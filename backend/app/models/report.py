"""
SQLAlchemy ORM model for user-submitted reports on curriculum content
(D-03). Admins can review these later; the Android client only needs
to POST them through the secured `/api/v1/reports` endpoint.
"""
from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.sql import func

from app.database import Base


class UserReport(Base):
    __tablename__ = "user_reports"

    id = Column(Integer, primary_key=True, index=True)
    # The Firebase UID of the reporting user. Required (T-05-01: Tampering
    # mitigation — only authenticated users can submit).
    firebase_uid = Column(String, nullable=False, index=True)
    # Optional reference to the Lesson the report is about; we don't
    # enforce a FK because lessons are content-managed separately and
    # reports must survive even if a lesson is retired.
    lesson_id = Column(Integer, nullable=True, index=True)
    # Free-form category so the client can pick from a small enum
    # ("wrong_answer", "typo", "audio_issue", "other") without a server
    # round-trip to validate.
    category = Column(String, nullable=False, default="other")
    # The user's own description of the issue.
    message = Column(Text, nullable=False)

    # Lifecycle: defaults to "open" so admin tooling can filter easily.
    status = Column(String, nullable=False, default="open")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
