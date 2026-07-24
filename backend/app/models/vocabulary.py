from sqlalchemy import Column, Integer, String, Float, Text, JSON
from app.database import Base

class Vocabulary(Base):
    __tablename__ = "vocabulary"

    id = Column(Integer, primary_key=True, index=True)
    word = Column(String, unique=True, index=True, nullable=False)
    definition = Column(Text, nullable=False)
    part_of_speech = Column(String)
    difficulty_level = Column(String, index=True) # A1-C2
    ipa = Column(String, nullable=True)
    topic = Column(String, default="general", index=True)
    audio_url = Column(String, nullable=True)
    example = Column(Text, nullable=True)
    scrambled_data = Column(Text, nullable=True) # Store JSON string
    
    # IRT Parameters
    irt_difficulty = Column(Float, default=0.0)
    irt_discrimination = Column(Float, default=1.0)
