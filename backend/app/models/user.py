from sqlalchemy import Column, String, DateTime, Integer, Float
from sqlalchemy.sql import func
from app.database import Base
import uuid

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    firebase_uid = Column(String, unique=True, index=True, nullable=False)
    display_name = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class UserSettings(Base):
    __tablename__ = "user_settings"

    user_id = Column(String, primary_key=True) # References users.id
    daily_goal_min = Column(Integer, default=5)
    current_streak = Column(Integer, default=0)
    longest_streak = Column(Integer, default=0)
    available_freezes = Column(Integer, default=1)
    last_study_date = Column(Integer, default=0)
    xp_total = Column(Integer, default=0)
    desired_retention = Column(Float, default=0.9)
    theme = Column(String, default="SYSTEM")
    language = Column(String, default="VI")
    placement_level = Column(String, nullable=True)
    selected_topic = Column(String, default="general")
