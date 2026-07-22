"""
SQLAlchemy ORM models for the Curriculum domain.

Curriculum structure (D-01):
    Topic  (1) ─── (N) Lesson
    Lesson (1) ─── (1) exercises_data (JSON column)

Exercises are stored as a flexible JSON string on the Lesson row
(see D-01 in 05-CONTEXT.md). This avoids a schema migration every time
we add a new exercise type (multiple_choice, fill_blank, listening, etc.).
"""
from sqlalchemy import Column, Integer, String, Text, ForeignKey, JSON
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.database import Base


class Topic(Base):
    __tablename__ = "topics"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    # CEFR difficulty band, e.g. "A1", "A2", ..., "C2"
    difficulty_level = Column(String, index=True, default="A1")
    # Order index for stable client-side rendering
    order_index = Column(Integer, default=0)

    lessons = relationship(
        "Lesson",
        back_populates="topic",
        cascade="all, delete-orphan",
        order_by="Lesson.order_index",
    )


class Lesson(Base):
    __tablename__ = "lessons"

    id = Column(Integer, primary_key=True, index=True)
    topic_id = Column(Integer, ForeignKey("topics.id"), nullable=False, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    order_index = Column(Integer, default=0)

    # D-01: Exercises stored as JSON string. SQLAlchemy's JSON type handles
    # serialization for us; clients receive a parsed list/dict via the
    # Pydantic response schema.
    exercises_data = Column(JSON, default=list, nullable=False)

    topic = relationship("Topic", back_populates="lessons")
