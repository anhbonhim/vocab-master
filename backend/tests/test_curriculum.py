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
