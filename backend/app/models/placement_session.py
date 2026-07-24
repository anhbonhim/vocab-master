from sqlalchemy import Column, String, Float, DateTime, Text
from sqlalchemy.sql import func
from app.database import Base
import uuid

class PlacementSession(Base):
    __tablename__ = "placement_sessions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, index=True)
    started_at = Column(DateTime(timezone=True), server_default=func.now())
    finished_at = Column(DateTime(timezone=True), nullable=True)
    result_level = Column(String, nullable=True)
    
    theta = Column(Float, default=0.0)
    se = Column(Float, default=9.0)
    
    # JSON string: list of dicts {"word": str, "is_correct": bool, "time_ms": int}
    responses = Column(Text, default="[]") 
