"""
Tests for the User Report API endpoint (D-03, T-05-01).

These tests use FastAPI's dependency_overrides to bypass the real
Firebase auth flow so we can exercise the endpoint logic in isolation.
The actual JWT verification is exercised separately by the
firebase_auth helper; this suite focuses on the wiring: requires
auth, accepts valid payload, persists, returns 201 + body.
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.database import Base, get_db
from app.utils.firebase_auth import get_current_user_uid


# Stable fake UID for all "authenticated" test calls
FAKE_UID = "test-firebase-uid-123"


@pytest.fixture
def db_session():
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
    def _override_get_db():
        try:
            yield db_session
        finally:
            pass

    def _fake_uid():
        return FAKE_UID

    app.dependency_overrides[get_db] = _override_get_db
    app.dependency_overrides[get_current_user_uid] = _fake_uid
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


def test_create_report_requires_auth(db_session):
    """
    Posting without overriding get_current_user_uid should bubble up the
    401 from the real dependency (Firebase returns 401 when no token).
    We use a fresh TestClient that does NOT override auth.
    """
    def _override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = _override_get_db
    try:
        with TestClient(app) as c:
            response = c.post(
                "/api/v1/reports",
                json={"category": "typo", "message": "missing article"},
            )
        assert response.status_code == 401
    finally:
        app.dependency_overrides.clear()


def test_create_report_persists_with_authenticated_user(client):
    """A valid authenticated POST persists the report and returns 201."""
    response = client.post(
        "/api/v1/reports",
        json={
            "lesson_id": 42,
            "category": "wrong_answer",
            "message": "The correct answer is B, not A.",
        },
    )
    assert response.status_code == 201
    body = response.json()
    assert body["firebase_uid"] == FAKE_UID
    assert body["lesson_id"] == 42
    assert body["category"] == "wrong_answer"
    assert body["message"] == "The correct answer is B, not A."
    assert body["status"] == "open"
    assert "id" in body
    assert "created_at" in body


def test_create_report_rejects_empty_message(client):
    """An empty message must fail Pydantic validation (min_length=1)."""
    response = client.post(
        "/api/v1/reports",
        json={"category": "typo", "message": ""},
    )
    assert response.status_code == 422


def test_create_report_uses_auth_uid_not_body(client):
    """
    Even if the body tried to claim a different uid, the persisted row
    must use the authenticated uid from the token (T-05-01).
    """
    response = client.post(
        "/api/v1/reports",
        json={
            "firebase_uid": "spoofed-uid",
            "category": "other",
            "message": "trying to spoof",
        },
    )
    assert response.status_code == 201
    # The Pydantic schema doesn't even declare firebase_uid, so the body
    # key is ignored, and the response uses the token-derived uid.
    assert response.json()["firebase_uid"] == FAKE_UID
