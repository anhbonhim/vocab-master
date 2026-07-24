from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Optional
import json
import random
import uuid
from datetime import datetime

from app.database import get_db
from app.models.vocabulary import Vocabulary
from app.models.placement_session import PlacementSession
from app.models.user import User
from app.services.irt_engine import IRTEngine
from app.utils.firebase_auth import get_optional_user_uid

router = APIRouter(prefix="/api/v1/placement", tags=["placement_test"])

class AnswerItem(BaseModel):
    question_id: str
    is_correct: bool
    response_time_ms: int

class AnswerRequest(BaseModel):
    # For authenticated users, state can be read from DB.
    # For anonymous users, the full history of the current test session is sent.
    responses: Optional[List[AnswerItem]] = None
    # The latest answer being submitted
    latest_question_id: str
    latest_is_correct: bool
    latest_response_time_ms: int

def get_or_create_user(db: Session, firebase_uid: str) -> User:
    user = db.query(User).filter(User.firebase_uid == firebase_uid).first()
    if not user:
        user = User(firebase_uid=firebase_uid)
        db.add(user)
        db.commit()
        db.refresh(user)
    return user

def _build_options_from_word(vocab: Vocabulary):
    """
    Build the 4 multiple-choice options from the embedded scrambled_data JSON
    stored on the vocabulary record. This avoids generating distractors from
    the DB (which previously produced duplicate / wrong-language options).

    Returns (options_list, correct_option_id) where options_list is a list of
    {"id": int, "text": str} and correct_option_id is the id of the correct
    option. IDs are indices 0..n-1 so the Android client can compare equality.
    """
    raw = None
    if vocab.scrambled_data:
        try:
            raw = json.loads(vocab.scrambled_data)
        except (ValueError, TypeError):
            raw = None

    options_texts = []
    correct_idx = 0
    if raw and isinstance(raw, dict) and raw.get("options"):
        options_texts = raw["options"]
        correct_idx = int(raw.get("correctIndex", 0))
    else:
        # Fallback: word itself as the only option
        options_texts = [vocab.word]
        correct_idx = 0

    options = []
    for idx, text in enumerate(options_texts):
        options.append({"id": idx, "text": text})

    # Shuffle options while tracking the correct option's id
    correct_text = options_texts[correct_idx] if correct_idx < len(options_texts) else options_texts[0]
    random.shuffle(options)
    correct_option_id = next((opt["id"] for opt in options if opt["text"] == correct_text), options[0]["id"])
    return options, correct_option_id

@router.post("/start")
def start_placement_test(uid: Optional[str] = Depends(get_optional_user_uid), db: Session = Depends(get_db)):
    # 1. Handle authenticated user session in DB
    session_id = str(uuid.uuid4())
    current_theta = -1.0
    
    if uid:
        user = get_or_create_user(db, uid)
        # Close any existing active sessions
        existing_sessions = db.query(PlacementSession).filter(
            PlacementSession.user_id == user.id,
            PlacementSession.finished_at == None
        ).all()
        for s in existing_sessions:
            db.delete(s)
        db.commit()
        
        # Start session at A2 (theta = -1.0)
        session = PlacementSession(
            id=session_id,
            user_id=user.id,
            theta=-1.0,
            se=9.0,
            responses="[]"
        )
        db.add(session)
        db.commit()
    
    # 2. Pull a random A2 word to start
    start_word = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == "A2"
    ).order_by(Vocabulary.irt_difficulty).offset(random.randint(0, 50)).first()
    
    if not start_word:
        start_word = db.query(Vocabulary).first()
        if not start_word:
            raise HTTPException(status_code=500, detail="Vocabulary DB is empty!")

    options, correct_option_id = _build_options_from_word(start_word)
    
    return {
        "session_id": session_id,
        "current_theta": current_theta,
        "next_question": {
            "question_id": str(start_word.id),
            "correct_option_id": correct_option_id,
            "prompt": start_word.example or start_word.word,
            "options": options
        }
    }

@router.post("/{session_id}/answer")
def submit_answer(
    session_id: str,
    request: AnswerRequest,
    uid: Optional[str] = Depends(get_optional_user_uid),
    db: Session = Depends(get_db)
):
    # 1. Resolve responses list
    responses_list = []
    
    # If user is authenticated, we can optionally rely on DB, but to support client-driven
    # stateless fallback seamlessly, we prioritize the request.responses if provided.
    # Otherwise we read/write to SQL database.
    user = None
    session = None
    
    if uid:
        user = get_or_create_user(db, uid)
        session = db.query(PlacementSession).filter(
            PlacementSession.id == session_id,
            PlacementSession.user_id == user.id
        ).first()
        
    if session and session.finished_at is not None:
        raise HTTPException(status_code=400, detail="Placement session already finished")

    # If client passed full responses history (Guest / Stateless mode)
    if request.responses is not None:
        # Load from client
        for r in request.responses:
            vocab_item = db.query(Vocabulary).filter(Vocabulary.id == int(r.question_id)).first()
            if vocab_item:
                responses_list.append({
                    "question_id": int(r.question_id),
                    "is_correct": r.is_correct,
                    "response_time_ms": r.response_time_ms,
                    "a": vocab_item.irt_discrimination,
                    "b": vocab_item.irt_difficulty
                })
    elif session:
        # Load from DB
        responses_db = json.loads(session.responses)
        for r in responses_db:
            responses_list.append({
                "question_id": r["question_id"],
                "is_correct": r["is_correct"],
                "response_time_ms": r["response_time_ms"],
                "a": r["a"],
                "b": r["b"]
            })
            
    # Add the latest answer
    latest_vocab = db.query(Vocabulary).filter(Vocabulary.id == int(request.latest_question_id)).first()
    if not latest_vocab:
        raise HTTPException(status_code=404, detail="Vocabulary item not found")
        
    responses_list.append({
        "question_id": latest_vocab.id,
        "is_correct": request.latest_is_correct,
        "response_time_ms": request.latest_response_time_ms,
        "a": latest_vocab.irt_discrimination,
        "b": latest_vocab.irt_difficulty
    })
    
    # Estimate theta and Standard Error
    new_theta, new_se = IRTEngine.estimate_theta(responses_list, current_theta=-1.0 if not session else session.theta)
    
    n_asked = len(responses_list)
    is_finished = new_se < 0.4 or n_asked >= 15
    
    # Save state if authenticated
    if session:
        # Keep DB structure updated
        # Map responses back to simple JSON list for storage
        db_responses = [{
            "question_id": r["question_id"],
            "is_correct": r["is_correct"],
            "response_time_ms": r["response_time_ms"],
            "a": r["a"],
            "b": r["b"]
        } for r in responses_list]
        
        session.theta = new_theta
        session.se = new_se
        session.responses = json.dumps(db_responses)
        
    if is_finished:
        final_level = IRTEngine.map_theta_to_cefr(new_theta)
        
        if session:
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
    next_level = IRTEngine.map_theta_to_cefr(new_theta)
    asked_question_ids = {r["question_id"] for r in responses_list}
    
    query = db.query(Vocabulary).filter(
        Vocabulary.difficulty_level == next_level,
        ~Vocabulary.id.in_(list(asked_question_ids))
    )
    
    if query.count() == 0:
        query = db.query(Vocabulary).filter(~Vocabulary.id.in_(list(asked_question_ids)))
        
    next_word = query.order_by(Vocabulary.irt_difficulty).offset(random.randint(0, min(10, max(0, query.count() - 1)))).first()
    if not next_word:
        next_word = db.query(Vocabulary).first()
        if not next_word:
            raise HTTPException(status_code=500, detail="Vocabulary DB is empty!")

    options, correct_option_id = _build_options_from_word(next_word)
    
    return {
        "status": "continue",
        "current_theta": new_theta,
        "standard_error": new_se,
        "estimated_level": next_level,
        "next_question": {
            "question_id": str(next_word.id),
            "correct_option_id": correct_option_id,
            "prompt": next_word.example or next_word.word,
            "options": options
        }
    }