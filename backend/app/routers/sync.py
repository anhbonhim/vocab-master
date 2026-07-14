from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user_progress import UserCard, ReviewLog
from app.models.user import User, UserSettings
from app.schemas.sync import SyncPayload, UserSettingsSchema, VocabularyCardSchema, ReviewLogSchema
from app.utils.firebase_auth import get_current_user_uid
from datetime import datetime
import time

router = APIRouter(prefix="/api/v1/sync", tags=["sync"])

def get_or_create_user(db: Session, firebase_uid: str) -> User:
    user = db.query(User).filter(User.firebase_uid == firebase_uid).first()
    if not user:
        user = User(firebase_uid=firebase_uid)
        db.add(user)
        db.commit()
        db.refresh(user)
    return user

@router.post("/push")
def sync_push(
    payload: SyncPayload,
    uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    user = get_or_create_user(db, uid)
    
    # 1. Sync User Settings (Last-write-wins based on sync action)
    settings = db.query(UserSettings).filter(UserSettings.user_id == user.id).first()
    if not settings:
        settings = UserSettings(user_id=user.id)
        db.add(settings)
        
    s = payload.userSettings
    settings.daily_goal_min = s.dailyGoalMinutes
    settings.current_streak = s.currentStreak
    settings.longest_streak = s.longestStreak
    settings.available_freezes = s.availableFreezes
    settings.last_study_date = s.lastStudyDate
    settings.xp_total = s.xpTotal
    settings.desired_retention = s.desiredRetention
    settings.theme = s.theme
    settings.language = s.language
    settings.placement_level = s.placementLevel
    settings.selected_topic = s.selectedTopic
    
    # 2. Sync Vocabulary Cards (Merge using modified timestamp)
    for card_schema in payload.vocabularyCards:
        existing = db.query(UserCard).filter(
            UserCard.user_id == user.id,
            UserCard.word == card_schema.word
        ).first()
        
        due_dt = datetime.fromisoformat(card_schema.due)
        last_review_dt = datetime.fromisoformat(card_schema.lastReview) if card_schema.lastReview else None
        
        if not existing:
            # Create new record
            new_card = UserCard(
                user_id=user.id,
                word=card_schema.word,
                due=due_dt,
                stability=card_schema.stability,
                difficulty=card_schema.difficulty,
                interval=card_schema.interval,
                reps=card_schema.reps,
                lapses=card_schema.lapses,
                state=card_schema.state,
                last_review=last_review_dt,
                last_modified=card_schema.lastModified
            )
            db.add(new_card)
        else:
            # Overwrite only if payload's card is newer than DB card
            if card_schema.lastModified > existing.last_modified:
                existing.due = due_dt
                existing.stability = card_schema.stability
                existing.difficulty = card_schema.difficulty
                existing.interval = card_schema.interval
                existing.reps = card_schema.reps
                existing.lapses = card_schema.lapses
                existing.state = card_schema.state
                existing.last_review = last_review_dt
                existing.last_modified = card_schema.lastModified
                
    # 3. Sync Review Logs (Append-only using uniqueness of word + timestamp)
    for log_schema in payload.reviewLogs:
        log_time = datetime.fromisoformat(log_schema.timestamp)
        existing_log = db.query(ReviewLog).filter(
            ReviewLog.user_id == user.id,
            ReviewLog.word == log_schema.word,
            ReviewLog.timestamp == log_time
        ).first()
        
        if not existing_log:
            new_log = ReviewLog(
                user_id=user.id,
                word=log_schema.word,
                rating=log_schema.rating,
                elapsed_days=log_schema.elapsed_days,
                scheduled_days=log_schema.scheduled_days,
                stability=log_schema.stability,
                difficulty=log_schema.difficulty,
                state=log_schema.state,
                timestamp=log_time
            )
            db.add(new_log)
            
    db.commit()
    return {"status": "success", "message": "Synchronization push completed successfully"}

@router.get("/pull", response_model=SyncPayload)
def sync_pull(
    since: int = 0,
    uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    user = get_or_create_user(db, uid)
    
    # 1. Query settings
    settings = db.query(UserSettings).filter(UserSettings.user_id == user.id).first()
    if not settings:
        settings = UserSettings(user_id=user.id)
        db.add(settings)
        db.commit()
        db.refresh(settings)
        
    settings_schema = UserSettingsSchema(
        dailyGoalMinutes=settings.daily_goal_min,
        currentStreak=settings.current_streak,
        longestStreak=settings.longest_streak,
        availableFreezes=settings.available_freezes,
        lastStudyDate=settings.last_study_date,
        xpTotal=settings.xp_total,
        desiredRetention=settings.desired_retention,
        theme=settings.theme,
        language=settings.language,
        placementLevel=settings.placement_level,
        selectedTopic=settings.selected_topic
    )
    
    # 2. Query updated user cards
    cards = db.query(UserCard).filter(
        UserCard.user_id == user.id,
        UserCard.last_modified > since
    ).all()
    
    cards_schemas = []
    for c in cards:
        cards_schemas.append(VocabularyCardSchema(
            word=c.word,
            due=c.due.isoformat(),
            stability=c.stability,
            difficulty=c.difficulty,
            interval=c.interval,
            reps=c.reps,
            lapses=c.lapses,
            state=c.state,
            lastReview=c.last_review.isoformat() if c.last_review else None,
            lastModified=c.last_modified
        ))
        
    # 3. Query review logs
    # Note: Since review_logs are append-only, we pull logs created since 'since' timestamp
    # For SQLite, we map timestamp directly. We convert since (epoch ms) to datetime.
    since_dt = datetime.fromtimestamp(since / 1000.0)
    logs = db.query(ReviewLog).filter(
        ReviewLog.user_id == user.id,
        ReviewLog.timestamp > since_dt
    ).all()
    
    logs_schemas = []
    for l in logs:
        logs_schemas.append(ReviewLogSchema(
            word=l.word,
            rating=l.rating,
            elapsed_days=l.elapsed_days,
            scheduled_days=l.scheduled_days,
            stability=l.stability,
            difficulty=l.difficulty,
            state=l.state,
            timestamp=l.timestamp.isoformat()
        ))
        
    return SyncPayload(
        userSettings=settings_schema,
        vocabularyCards=cards_schemas,
        reviewLogs=logs_schemas,
        lastSyncTimestamp=int(time.time() * 1000)
    )