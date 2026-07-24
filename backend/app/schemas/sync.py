from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

class UserSettingsSchema(BaseModel):
    dailyGoalMinutes: int
    currentStreak: int
    longestStreak: int
    availableFreezes: int
    lastStudyDate: int
    xpTotal: int
    desiredRetention: float
    theme: str
    language: str
    placementLevel: Optional[str] = None
    selectedTopic: str

class VocabularyCardSchema(BaseModel):
    questionId: str
    due: str
    stability: float
    difficulty: float
    interval: int
    reps: int
    lapses: int
    state: int
    lastReview: Optional[str] = None
    lastModified: int

class ReviewLogSchema(BaseModel):
    questionId: str
    rating: int
    elapsed_days: int
    scheduled_days: int
    stability: float
    difficulty: float
    state: int
    timestamp: str

class SyncPayload(BaseModel):
    userSettings: UserSettingsSchema
    vocabularyCards: List[VocabularyCardSchema]
    reviewLogs: List[ReviewLogSchema]
    lastSyncTimestamp: int
