from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.vocabulary import Vocabulary
from app.schemas.vocabulary import VocabularyCatalogResponse, VocabularyItemResponse
from typing import List

router = APIRouter(prefix="/api/v1/vocabulary", tags=["vocabulary"])

@router.get("/topics")
def get_topics(db: Session = Depends(get_db)):
    topics = db.query(Vocabulary.topic).distinct().all()
    # Flatten list of tuples
    topic_list = [t[0] for t in topics if t[0]]
    if not topic_list:
        topic_list = ["general", "business", "travel", "technology", "education"]
    return {"data": topic_list}

@router.get("/catalog", response_model=VocabularyCatalogResponse)
def get_catalog(
    topic: str = "general",
    level: str = "A1",
    page: int = Query(1, ge=1),
    size: int = Query(50, ge=1, le=100),
    db: Session = Depends(get_db)
):
    offset = (page - 1) * size
    query = db.query(Vocabulary).filter(
        Vocabulary.topic == topic,
        Vocabulary.difficulty_level == level
    )
    
    total = query.count()
    items = query.offset(offset).limit(size).all()
    
    item_responses = []
    for item in items:
        item_responses.append(VocabularyItemResponse(
            id=item.id,
            word=item.word,
            definition=item.definition,
            part_of_speech=item.part_of_speech or "",
            difficulty_level=item.difficulty_level,
            ipa=item.ipa,
            topic=item.topic,
            audio_url=item.audio_url,
            example=item.example,
            scrambled_data=item.scrambled_data
        ))
        
    return VocabularyCatalogResponse(
        topic=topic,
        level=level,
        page=page,
        size=size,
        total=total,
        items=item_responses
    )