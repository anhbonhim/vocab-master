"""
User Report API router (D-03).

All endpoints require an authenticated Firebase user. Reports are stored
in the `user_reports` table so admins can review them later.
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.report import UserReport
from app.schemas.report import ReportCreate, ReportResponse
from app.utils.firebase_auth import get_current_user_uid

router = APIRouter(prefix="/api/v1/reports", tags=["reports"])


@router.post(
    "",
    response_model=ReportResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_report(
    payload: ReportCreate,
    uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db),
) -> ReportResponse:
    """
    Persist a user report. The Firebase UID is taken from the verified
    token, not the request body, so clients cannot impersonate other
    users (T-05-01).
    """
    report = UserReport(
        firebase_uid=uid,
        lesson_id=payload.lesson_id,
        category=payload.category,
        message=payload.message,
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return ReportResponse.model_validate(report)
