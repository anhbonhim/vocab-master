"""
Content generation script (CONT-02, D-01, D-02, T-05-02, T-05-03).

Orchestrates the LLM-driven exercise pipeline:

  1. Fetch a list of Vocabulary rows for a topic.
  2. Call :func:`app.services.llm_service.generate_exercises_from_llm`
     to get the raw assistant text from the Opencode API.
  3. Pass the raw text through
     :func:`app.schemas.llm.validate_llm_output` so a strict
     ``LLMResponse`` comes out (T-05-02: single chokepoint).
  4. Persist the validated list of exercise dicts onto
     ``Lesson.exercises_data`` (D-01: flexible JSON column).

This module is a CLI entry point. It is meant to be run by an admin
or a cron job — NOT from inside the FastAPI request cycle (that would
block the event loop for tens of seconds waiting on the LLM).

Usage::

    python -m app.services.content_gen --lesson-id 1 --topic Travel

Exit code:
  * 0 = success (exercises generated and persisted)
  * 1 = failure (LLM error, validation error, missing lesson, etc.)

The script is deliberately side-effect-aware: it logs every step to
stdout/stderr at INFO/WARNING/ERROR level so an operator can audit
what happened.
"""
from __future__ import annotations

import argparse
import asyncio
import logging
import sys
from typing import List, Sequence

from sqlalchemy.orm import Session

from app.database import SessionLocal
from app.models.curriculum import Lesson
from app.models.vocabulary import Vocabulary
from app.schemas.llm import validate_llm_output
from app.services.llm_service import (
    LLMServiceError,
    generate_exercises_from_llm,
)

logger = logging.getLogger("content_gen")


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------


def fetch_vocabulary_words(db: Session, topic: str) -> List[str]:
    """Return the ``word`` column for every Vocabulary row with the given topic.

    The list is ordered by ``Vocabulary.id`` so the order is stable across
    runs. The LLM is asked to produce exercises for these words, and the
    caller (the script operator) can pair the LLM output back to the
    source list via the order.
    """
    rows = (
        db.query(Vocabulary)
        .filter(Vocabulary.topic == topic)
        .order_by(Vocabulary.id.asc())
        .all()
    )
    return [row.word for row in rows]


def save_exercises_to_lesson(
    db: Session, lesson_id: int, exercises: List[dict]
) -> Lesson:
    """Persist a list of exercise dicts onto ``Lesson.exercises_data``.

    Returns the updated :class:`Lesson` so callers can re-read it for
    logging / verification. D-01: the column is a flexible JSON field;
    we store plain dicts and let SQLAlchemy serialize.
    """
    lesson = db.query(Lesson).filter(Lesson.id == lesson_id).first()
    if lesson is None:
        raise ValueError(f"Lesson {lesson_id} not found")
    lesson.exercises_data = exercises
    db.commit()
    db.refresh(lesson)
    return lesson


# ---------------------------------------------------------------------------
# Pipeline
# ---------------------------------------------------------------------------


async def generate_for_lesson(lesson_id: int, topic: str) -> int:
    """Run the LLM pipeline for a single Lesson.

    Returns the number of exercises written to ``exercises_data``.

    Raises:
        LLMServiceError: API key missing, timeout, non-2xx, or
            malformed response envelope from the Opencode API.
        ValueError: Pydantic validation failed, or the Lesson was not
            found.
    """
    db = SessionLocal()
    try:
        words = fetch_vocabulary_words(db, topic)
        if not words:
            logger.warning(
                "No vocabulary rows found for topic %r; aborting without "
                "calling the LLM (lesson %d left untouched).",
                topic,
                lesson_id,
            )
            return 0
        logger.info(
            "Fetched %d vocabulary words for topic %r; calling LLM.",
            len(words),
            topic,
        )

        # 1) Call the LLM service. Errors are caught + re-raised so the
        # caller (the CLI) can log + abort.
        raw_text = await generate_exercises_from_llm(words)

        # 2) Validate the raw text through the single chokepoint
        # (T-05-02). validate_llm_output is the only sanctioned way to
        # turn an LLM string into persisted exercise data.
        validated = validate_llm_output(raw_text)
        if not validated.exercises:
            logger.warning(
                "LLM returned an empty exercises list for lesson %d; "
                "saving an empty list rather than overwriting with junk.",
                lesson_id,
            )

        # 3) Convert Pydantic models to plain dicts so SQLAlchemy's
        # JSON column serializes them cleanly.
        exercises = [item.model_dump() for item in validated.exercises]

        # 4) Persist (D-01).
        save_exercises_to_lesson(db, lesson_id, exercises)
        logger.info(
            "Saved %d exercises to Lesson %d.", len(exercises), lesson_id
        )
        return len(exercises)
    finally:
        db.close()


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="content_gen",
        description=(
            "Generate Lesson exercises via the Opencode LLM and persist "
            "the validated result into the SQLite JSON column."
        ),
    )
    parser.add_argument(
        "--lesson-id",
        type=int,
        required=True,
        help="ID of the Lesson row to populate.",
    )
    parser.add_argument(
        "--topic",
        type=str,
        required=True,
        help=(
            "Topic name used to filter the Vocabulary table for source "
            "words."
        ),
    )
    parser.add_argument(
        "--log-level",
        type=str,
        default="INFO",
        help="Python logging level (default: INFO).",
    )
    return parser


def main(argv: Sequence[str]) -> int:
    """CLI entry point. Returns the process exit code."""
    parser = _build_arg_parser()
    args = parser.parse_args(list(argv))

    logging.basicConfig(
        level=getattr(logging, args.log_level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )

    try:
        count = asyncio.run(generate_for_lesson(args.lesson_id, args.topic))
    except LLMServiceError as exc:
        logger.error("LLM call failed: %s", exc)
        return 1
    except ValueError as exc:
        logger.error("Pipeline failed: %s", exc)
        return 1

    logger.info("Done: %d exercises generated for Lesson %d.", count, args.lesson_id)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
