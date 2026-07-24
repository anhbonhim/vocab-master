"""
Integration tests for the content generation script (CONT-02, D-01, T-05-02).

These tests pin the wiring of the LLM-driven exercise pipeline:

    fetch vocabulary  ->  call LLM service  ->  validate_llm_output
    ->  save to Lesson.exercises_data

Per the plan's acceptance_criteria for task 2: "Pipeline correctly wires
LLM fetch, validation, and DB save." This is the integration-level proof
that the four steps connect end-to-end.

Coverage:
- Successful run: vocabulary is fetched, LLM is called, response is
  validated, exercises_data is persisted (D-01 JSON column).
- Empty vocabulary: the script aborts BEFORE calling the LLM (no
  wasted LLM tokens, no junk in the DB).
- Missing lesson: ValueError surfaces to the operator (CLI maps to
  non-zero exit).
- LLMServiceError propagates so the CLI can log + abort (T-05-03).
- Pydantic ValueError from validate_llm_output propagates (T-05-02:
  the chokepoint).
"""
import json
from typing import Any, Dict, List

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base, get_db
from app.services.content_gen import (
    generate_for_lesson,
    fetch_vocabulary_words,
    save_exercises_to_lesson,
)
from app.services.llm_service import LLMServiceError


# ---------------------------------------------------------------------------
# Fixtures: isolated in-memory SQLite + per-test DB session
# ---------------------------------------------------------------------------


@pytest.fixture
def db_session():
    """Provide an isolated in-memory SQLite session for tests."""
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)


def _seed_topic_and_lesson(db, words: List[str], lesson_title: str = "Fruits"):
    """Insert one Topic, one Lesson, and one Vocabulary row per word."""
    from app.models.curriculum import Topic, Lesson
    from app.models.vocabulary import Vocabulary

    topic = Topic(title=lesson_title, difficulty_level="A2", order_index=1)
    db.add(topic)
    db.commit()
    db.refresh(topic)

    lesson = Lesson(
        topic_id=topic.id,
        title=lesson_title,
        order_index=1,
        exercises_data=[],
    )
    db.add(lesson)
    db.commit()
    db.refresh(lesson)

    for i, word in enumerate(words, start=1):
        db.add(
            Vocabulary(
                word=word,
                definition=f"definition of {word}",
                topic=lesson_title,
            )
        )
    db.commit()
    return topic, lesson


# ---------------------------------------------------------------------------
# Helpers: monkey-patch the LLM service to return canned text
# ---------------------------------------------------------------------------


def _patch_llm(monkeypatch, raw_text: str):
    """Replace generate_exercises_from_llm with a coroutine returning raw_text."""

    async def _fake_generate_exercises_from_llm(words):
        return raw_text

    monkeypatch.setattr(
        "app.services.content_gen.generate_exercises_from_llm",
        _fake_generate_exercises_from_llm,
    )


def _patch_llm_raises(monkeypatch, exc: Exception):
    """Replace generate_exercises_from_llm with a coroutine that raises."""
    async def _fake_generate_exercises_from_llm(words):
        raise exc

    monkeypatch.setattr(
        "app.services.content_gen.generate_exercises_from_llm",
        _fake_generate_exercises_from_llm,
    )


# ---------------------------------------------------------------------------
# fetch_vocabulary_words
# ---------------------------------------------------------------------------


def test_fetch_vocabulary_words_returns_words_for_topic_in_id_order(db_session):
    """
    The helper must return the words for the given topic, ordered by id
    (so callers can pair LLM output back to the source list).
    """
    _seed_topic_and_lesson(db_session, ["banana", "apple", "cherry"])

    words = fetch_vocabulary_words(db_session, "Fruits")

    assert words == ["banana", "apple", "cherry"]


def test_fetch_vocabulary_words_returns_empty_list_for_unknown_topic(db_session):
    """No rows -> empty list (NOT a raise), so the script can abort early."""
    assert fetch_vocabulary_words(db_session, "Mystery") == []


# ---------------------------------------------------------------------------
# generate_for_lesson: happy path (full pipeline)
# ---------------------------------------------------------------------------


def test_generate_for_lesson_persists_validated_exercises(monkeypatch, db_session):
    """
    The full pipeline: fetch vocab -> LLM call -> validate -> save to
    Lesson.exercises_data. After running, the Lesson row should have
    the validated dicts in its JSON column.
    """
    _seed_topic_and_lesson(db_session, ["apple", "banana"])

    raw_text = json.dumps(
        {
            "exercises": [
                {
                    "type": "fill_blank",
                    "question": "I eat an ___.",
                    "correct_answer": "apple",
                },
                {
                    "type": "multiple_choice",
                    "question": "Pick the fruit.",
                    "options": ["apple", "car", "dog"],
                    "correct_answer": "apple",
                },
            ]
        }
    )
    _patch_llm(monkeypatch, raw_text)

    # Patch the SessionLocal inside content_gen to use the test DB.
    monkeypatch.setattr("app.services.content_gen.SessionLocal", lambda: _SessionHolder(db_session))

    # Run the pipeline (it's async but no actual await happens because
    # we replaced the LLM call with a coroutine that returns immediately).
    from app.services.content_gen import generate_for_lesson as _gen

    # asyncio.run the coroutine
    import asyncio
    count = asyncio.run(_gen(lesson_id=1, topic="Fruits"))

    assert count == 2

    # Re-query the lesson from the test DB
    from app.models.curriculum import Lesson

    lesson = db_session.query(Lesson).filter(Lesson.id == 1).first()
    assert lesson is not None
    assert isinstance(lesson.exercises_data, list)
    assert len(lesson.exercises_data) == 2
    assert lesson.exercises_data[0]["type"] == "fill_blank"
    assert lesson.exercises_data[0]["correct_answer"] == "apple"
    assert lesson.exercises_data[1]["type"] == "multiple_choice"
    assert lesson.exercises_data[1]["options"] == ["apple", "car", "dog"]


def test_generate_for_lesson_aborts_before_llm_when_no_vocab(monkeypatch, db_session):
    """
    If there are no vocabulary rows for the topic, the script must NOT
    call the LLM (no wasted tokens, no junk exercises_data). The
    LLMService is patched to raise if it gets called.
    """
    _seed_topic_and_lesson(db_session, words=[])  # no vocabulary rows

    called = {"yes": False}

    async def _must_not_be_called(words):
        called["yes"] = True
        return "{}"

    monkeypatch.setattr(
        "app.services.content_gen.generate_exercises_from_llm",
        _must_not_be_called,
    )

    monkeypatch.setattr("app.services.content_gen.SessionLocal", lambda: _SessionHolder(db_session))

    import asyncio
    from app.services.content_gen import generate_for_lesson as _gen

    count = asyncio.run(_gen(lesson_id=1, topic="Fruits"))

    assert count == 0
    assert called["yes"] is False, "LLM must not be called when no vocab rows exist"


def test_generate_for_lesson_propagates_llm_service_error(monkeypatch, db_session):
    """
    LLMServiceError from the LLM service must propagate to the caller
    (the CLI), which logs + aborts with a non-zero exit code.
    """
    _seed_topic_and_lesson(db_session, ["apple"])

    _patch_llm_raises(monkeypatch, LLMServiceError("simulated timeout"))

    monkeypatch.setattr("app.services.content_gen.SessionLocal", lambda: _SessionHolder(db_session))

    import asyncio
    from app.services.content_gen import generate_for_lesson as _gen

    with pytest.raises(LLMServiceError) as excinfo:
        asyncio.run(_gen(lesson_id=1, topic="Fruits"))
    assert "simulated timeout" in str(excinfo.value)


def test_generate_for_lesson_propagates_pydantic_value_error(monkeypatch, db_session):
    """
    When the LLM returns malformed JSON, validate_llm_output raises
    ValueError (T-05-02). That ValueError MUST propagate so the CLI
    can log + abort — the script must NEVER silently save junk.
    """
    _seed_topic_and_lesson(db_session, ["apple"])

    # Return text that is not valid JSON at all -> validate_llm_output
    # raises ValueError.
    _patch_llm(monkeypatch, "not json at all")

    monkeypatch.setattr("app.services.content_gen.SessionLocal", lambda: _SessionHolder(db_session))

    import asyncio
    from app.services.content_gen import generate_for_lesson as _gen

    with pytest.raises(ValueError):
        asyncio.run(_gen(lesson_id=1, topic="Fruits"))


def test_generate_for_lesson_raises_value_error_for_missing_lesson(monkeypatch, db_session):
    """
    If the operator passes a lesson_id that does not exist,
    save_exercises_to_lesson raises ValueError so the CLI can surface
    a clear error to the operator.
    """
    _seed_topic_and_lesson(db_session, ["apple"])
    # Intentionally do NOT create a lesson with id=999

    _patch_llm(monkeypatch, json.dumps({"exercises": []}))

    monkeypatch.setattr("app.services.content_gen.SessionLocal", lambda: _SessionHolder(db_session))

    import asyncio
    from app.services.content_gen import generate_for_lesson as _gen

    with pytest.raises(ValueError) as excinfo:
        asyncio.run(_gen(lesson_id=999, topic="Fruits"))
    assert "999" in str(excinfo.value)


# ---------------------------------------------------------------------------
# save_exercises_to_lesson
# ---------------------------------------------------------------------------


def test_save_exercises_to_lesson_persists_to_json_column(db_session):
    """
    D-01: exercises_data is a SQLAlchemy JSON column. Saving a list of
    dicts round-trips back as a list of dicts.
    """
    _seed_topic_and_lesson(db_session, words=[])  # no vocab, just the lesson

    exercises = [
        {"type": "fill_blank", "question": "Sky is ___.", "correct_answer": "blue"},
    ]
    updated = save_exercises_to_lesson(db_session, lesson_id=1, exercises=exercises)

    assert updated.exercises_data == exercises


def test_save_exercises_to_lesson_raises_for_missing_lesson(db_session):
    with pytest.raises(ValueError) as excinfo:
        save_exercises_to_lesson(db_session, lesson_id=999, exercises=[])
    assert "999" in str(excinfo.value)


# ---------------------------------------------------------------------------
# Helper: minimal SessionLocal stand-in for tests
# ---------------------------------------------------------------------------


class _SessionHolder:
    """Tiny stand-in for SessionLocal so the script uses the test DB.

    The script does ``db = SessionLocal()`` and then ``db.close()`` in a
    finally block. We mimic that by making the SessionLocal-returned
    object BE the session (since we only need a single connection per
    test). The test owns the session lifetime, so ``close()`` is a no-op.
    """

    def __init__(self, session):
        self._session = session

    def __call__(self):
        # When monkey-patched, the script does ``db = SessionLocal()``,
        # so the callable must return an object that has .query(),
        # .commit(), .refresh(), and .close().
        return self._session

    def query(self, *args, **kwargs):
        return self._session.query(*args, **kwargs)

    def commit(self):
        return self._session.commit()

    def refresh(self, obj):
        return self._session.refresh(obj)

    def close(self):
        # The test owns the session lifetime; do nothing here so the
        # test can keep using the session after the pipeline returns.
        pass
