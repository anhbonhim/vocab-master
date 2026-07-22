"""
Curriculum API router.

Phase 5 / Plan 01 establishes the base surface for delivering gamified
curriculum content to the Android client. Endpoints:

  GET  /api/v1/curriculum/topics       -> list all Topics
  GET  /api/v1/curriculum/topics/{id}/lessons  -> list Lessons for a Topic

Exercises are returned as a JSON list (per D-01); no separate exercise
type tables are created.
"""
from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.curriculum import Topic, Lesson
from app.schemas.curriculum import TopicResponse, LessonResponse

router = APIRouter(prefix="/api/v1/curriculum", tags=["curriculum"])


@router.get("/topics", response_model=List[TopicResponse])
def list_topics(db: Session = Depends(get_db)) -> List[TopicResponse]:
    """Return all Topics, ordered by order_index then id for stability."""
    topics = (
        db.query(Topic)
        .order_by(Topic.order_index.asc(), Topic.id.asc())
        .all()
    )
    return [TopicResponse.model_validate(t) for t in topics]


@router.get("/topics/{topic_id}/lessons", response_model=List[LessonResponse])
def list_lessons_by_topic(
    topic_id: int,
    db: Session = Depends(get_db),
) -> List[LessonResponse]:
    """Return all Lessons belonging to a Topic, ordered by order_index."""
    topic = db.query(Topic).filter(Topic.id == topic_id).first()
    if topic is None:
        raise HTTPException(status_code=404, detail=f"Topic {topic_id} not found")

    lessons = (
        db.query(Lesson)
        .filter(Lesson.topic_id == topic_id)
        .order_by(Lesson.order_index.asc(), Lesson.id.asc())
        .all()
    )
    return [LessonResponse.model_validate(lesson) for lesson in lessons]
