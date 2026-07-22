"""
Pydantic schemas for user-submitted content reports (D-03).
"""
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class ReportCreate(BaseModel):
    """Request body for POST /api/v1/reports."""
    lesson_id: Optional[int] = Field(
        default=None,
        description="Optional Lesson the report is about.",
    )
    category: str = Field(
        default="other",
        max_length=50,
        description="Short tag such as 'wrong_answer', 'typo', 'audio_issue', 'other'.",
    )
    message: str = Field(
        ...,
        min_length=1,
        max_length=2000,
        description="User-supplied description of the problem.",
    )


class ReportResponse(BaseModel):
    """Returned after a successful POST so the client can confirm persistence."""
    model_config = ConfigDict(from_attributes=True)

    id: int
    firebase_uid: str
    lesson_id: Optional[int]
    category: str
    message: str
    status: str
    created_at: datetime
