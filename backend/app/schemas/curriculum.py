"""
Pydantic request/response schemas for the Curriculum API.

These schemas are the public contract exposed to the Android client and
any other downstream consumer. The Lesson exercises_data field is kept
as a flexible list so that new exercise types (per D-01) can flow through
without a schema migration on the API side either.
"""
from typing import Any, List, Optional
from pydantic import BaseModel, ConfigDict


class TopicResponse(BaseModel):
    """Public representation of a Topic for the curriculum list endpoint."""
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: Optional[str] = None
    difficulty_level: str
    order_index: int


class LessonResponse(BaseModel):
    """Public representation of a Lesson, including its JSON exercises blob."""
    model_config = ConfigDict(from_attributes=True)

    id: int
    topic_id: int
    title: str
    description: Optional[str] = None
    order_index: int
    exercises_data: List[Any] = []
