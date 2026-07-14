from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Optional
import json
import random

from app.database import get_db
from app.models.vocabulary import Vocabulary
from app.models.placement_session import PlacementSession
from app.models.user import User
from app.services.irt_engine import IRTEngine
from app.utils.firebase_auth import get_current_user_uid

router = APIRouter(prefix="/api/v1/placement", tags=["placement_test"])

class AnswerRequest(BaseModel):
    vocab_id: int
    is_correct: bool
    response_time_ms: int

def get_or_create_user(db: Session, firebase_uid: str) -> User:
    user = db.query(User).filter(User.firebase_uid == firebase_uid).first()
    if not user:
        user = User(firebase_uid=firebase_uid)
        db.add(user)
        db.commit()
        db.refresh(user)
    return user

@router.post("/start")
def start_placement_test(uid: str = Depends(get_current_user_uid), db: Session = Depends(get_db)):
    user = get_or_create_user(db, uid)
    
    # 1. Close any existing active sessions
    existing_sessions = db.query(PlacementSession).filter(
        PlacementSession.user_id == user.id,
        PlacementSession.finished_at == None
    ).all()
    for s in existing_sessions:
        db.delete(s)
    db.commit()
    
    # 2. Start session at A2 (theta = -1.0)
    session = PlacementSession(
        user_id=user.id,
        theta=-1.0,
        se=9.0,
        responses="[]"
    )
    db.add(session)
    db.commit()
    db.refresh(session)
    
    # 3. Pull a random A2 word to start
    start_word = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == "A2"
    ).order_by(Vocabulary.irt_difficulty).offset(random.randint(0, 50)).first()
    
    if not start_word:
        start_word = db.query(Vocabulary).first()
        if not start_word:
            raise HTTPException(status_code=500, detail="Vocabulary DB is empty!")
            
    # Generate distractors from the same level
    distractors = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == start_word.difficulty_level,
        Vocabulary.id != start_word.id
    ).order_by(Vocabulary.word).offset(random.randint(0, 100)).limit(3).all()
    
    options = [{"id": start_word.id, "text": start_word.definition}]
    for d in distractors:
        options.append({"id": d.id, "text": d.definition})
    random.shuffle(options)
    
    return {
        "session_id": session.id,
        "current_theta": session.theta,
        "next_question": {
            "vocab_id": start_word.id,
            "word": start_word.word,
            "options": options
        }
    }

@router.post("/{session_id}/answer")
def submit_answer(
    session_id: str,
    request: AnswerRequest,
    uid: str = Depends(get_current_user_uid),
    db: Session = Depends(get_db)
):
    user = get_or_create_user(db, uid)
    
    session = db.query(PlacementSession).filter(
        PlacementSession.id == session_id,
        PlacementSession.user_id == user.id
    ).first()
    
    if not session or session.finished_at is not None:
        raise HTTPException(status_code=404, detail="Active placement session not found")
        
    # Get the question word parameters
    vocab = db.query(Vocabulary).filter(Vocabulary.id == request.vocab_id).first()
    if not vocab:
        raise HTTPException(status_code=404, detail="Vocabulary item not found")
        
    # Parse existing responses
    responses = json.loads(session.responses)
    
    # Add new response
    responses.append({
        "vocab_id": vocab.id,
        "is_correct": request.is_correct,
        "response_time_ms": request.response_time_ms,
        "a": vocab.irt_discrimination,
        "b": vocab.irt_difficulty
    })
    
    # Calculate updated estimated ability (theta) and Standard Error
    new_theta, new_se = IRTEngine.estimate_theta(responses, current_theta=session.theta)
    
    # Check stopping conditions (SE < 0.4 or max 15 questions reached)
    n_asked = len(responses)
    is_finished = new_se < 0.4 or n_asked >= 15
    
    session.theta = new_theta
    session.se = new_se
    session.responses = json.dumps(responses)
    
    if is_finished:
        from datetime import datetime
        final_level = IRTEngine.map_theta_to_cefr(new_theta)
        session.finished_at = datetime.now()
        session.result_level = final_level
        
        # Save placement level to User Settings
        from app.models.user import UserSettings
        settings = db.query(UserSettings).filter(UserSettings.user_id == user.id).first()
        if not settings:
            settings = UserSettings(user_id=user.id)
            db.add(settings)
        settings.placement_level = final_level
        db.commit()
        
        return {
            "status": "finished",
            "current_theta": new_theta,
            "standard_error": new_se,
            "estimated_level": final_level,
            "result": {
                "final_level": final_level,
                "theta": new_theta,
                "confidence": 1.0 - min(new_se, 1.0),
                "questions_asked": n_asked
            }
        }
        
    db.commit()
    
    # Select next question based on current theta
    # Query vocabulary items of level closest to theta
    next_level = IRTEngine.map_theta_to_cefr(new_theta)
    
    # Exclude already asked questions
    asked_vocab_ids = {r["vocab_id"] for r in responses}
    
    # Find words at target difficulty level
    query = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == next_level,
        ~Vocabulary.id.in_(list(asked_vocab_ids))
    )
    
    # If no words left in that level (unlikely), pick from B1 or general
    if query.count() == 0:
        query = db.query(Vocabulary).filter(~Vocabulary.id.in_(list(asked_vocab_ids)))
        
    next_word = query.order_by(Vocabulary.irt_difficulty).offset(random.randint(0, min(10, max(0, query.count() - 1)))).first()
    
    if not next_word:
        # Emergency fallback
        next_word = db.query(Vocabulary).first()
        
    # Generate distractors from same level
    distractors = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == next_word.difficulty_level,
        Vocabulary.id != next_word.id
    ).order_by(Vocabulary.word).offset(random.randint(0, 100)).limit(3).all()
    
    options = [{"id": next_word.id, "text": next_word.definition}]
    for d in distractors:
        options.append({"id": d.id, "text": d.definition})
    random.shuffle(options)
    
    return {
        "status": "continue",
        "current_theta": new_theta,
        "standard_error": new_se,
        "estimated_level": next_level,
        "next_question": {
            "vocab_id": next_word.id,
            "word": next_word.word,
            "options": options
        }
    }