"""
Tests for the Curriculum API endpoints.

Covers:
- CONT-01: Curriculum content delivery API surface
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.database import Base, get_db


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


@pytest.fixture
def client(db_session):
    """Provide a FastAPI TestClient bound to the in-memory test database."""
    def _override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = _override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


def test_get_topics_returns_empty_list_when_no_topics(client):
    """
    GET /api/v1/curriculum/topics should return an empty list when no Topic
    rows exist in the database. This is the tracer happy-path for CONT-01.
    """
    response = client.get("/api/v1/curriculum/topics")
    assert response.status_code == 200
    assert response.json() == []


def test_list_lessons_by_topic_returns_empty_list(client, db_session):
    """
    GET /api/v1/curriculum/topics/{topic_id}/lessons should return [] for
    a Topic with no Lessons yet. Seeds a Topic to avoid the 404 branch.
    """
    from app.models.curriculum import Topic
    topic = Topic(title="Travel", difficulty_level="A2", order_index=1)
    db_session.add(topic)
    db_session.commit()
    db_session.refresh(topic)

    response = client.get(f"/api/v1/curriculum/topics/{topic.id}/lessons")
    assert response.status_code == 200
    assert response.json() == []


def test_list_lessons_by_topic_returns_exercises_data(client, db_session):
    """
    Lessons expose their exercises_data field as a parsed JSON list (D-01).
    This verifies the JSON column is correctly serialized through Pydantic.
    """
    from app.models.curriculum import Topic, Lesson
    topic = Topic(title="Food", difficulty_level="A2", order_index=2)
    db_session.add(topic)
    db_session.commit()
    db_session.refresh(topic)

    sample_exercises = [
        {
            "type": "multiple_choice",
            "question": "What is 'apple' in Vietnamese?",
            "options": ["Quả táo", "Quả cam", "Quả nho", "Quả chuối"],
            "correct_answer": "Quả táo",
        },
        {
            "type": "fill_blank",
            "question": "I ___ an apple.",
            "correct_answer": "eat",
        },
    ]
    lesson = Lesson(
        topic_id=topic.id,
        title="Fruits",
        order_index=1,
        exercises_data=sample_exercises,
    )
    db_session.add(lesson)
    db_session.commit()

    response = client.get(f"/api/v1/curriculum/topics/{topic.id}/lessons")
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1
    assert body[0]["title"] == "Fruits"
    assert body[0]["exercises_data"] == sample_exercises


def test_list_lessons_by_unknown_topic_returns_404(client):
    """
    Hitting lessons for a non-existent topic must surface a 404, not 200 [].
    """
    response = client.get("/api/v1/curriculum/topics/9999/lessons")
    assert response.status_code == 404
