import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Vocab Master API"
    VERSION: str = "1.0.0"
    
    # Database
    DATABASE_URL: str = "sqlite:///./vocab.db"
    
    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "firebase-service-account.json"
    
    # Opencode LLM API (CONT-02)
    # Defaults to empty string so the app boots even when the secret is
    # absent; callers must check before issuing a real request.
    OPENCODE_API_KEY: str = ""
    OPENCODE_API_URL: str = "http://localhost:8080/v1/chat/completions"
    OPENCODE_MODEL: str = "gemini-3.1-pro-low(high)"
    OPENCODE_TIMEOUT_SECONDS: float = 60.0
    
    class Config:
        env_file = ".env"

settings = Settings()