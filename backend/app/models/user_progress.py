from sqlalchemy import Column, Integer, String, Float, ForeignKey, BigInteger, DateTime
from sqlalchemy.sql import func
from app.database import Base

class UserCard(Base):
    __tablename__ = "user_cards"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(String, index=True) # References users.id
    questionId = Column(String, index=True)    # Natural key linking to vocabulary.word
    
    # FSRS fields
    due = Column(DateTime(timezone=True))
    stability = Column(Float, default=0.0)
    difficulty = Column(Float, default=0.0)
    interval = Column(Integer, default=0)
    reps = Column(Integer, default=0)
    lapses = Column(Integer, default=0)
    state = Column(Integer, default=0)
    last_review = Column(DateTime(timezone=True), nullable=True)
    
    last_modified = Column(BigInteger, default=0) # Epoch ms for sync

class ReviewLog(Base):
    __tablename__ = "review_logs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(String, index=True)
    questionId = Column(String, index=True)
    
    rating = Column(Integer)
    elapsed_days = Column(Integer)
    scheduled_days = Column(Integer)
    stability = Column(Float)
    difficulty = Column(Float)
    state = Column(Integer)
    timestamp = Column(DateTime(timezone=True))
