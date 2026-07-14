import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Vocab Master API"
    VERSION: str = "1.0.0"
    
    # Database
    DATABASE_URL: str = "sqlite:///./vocab.db"
    
    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "firebase-service-account.json"
    
    class Config:
        env_file = ".env"

settings = Settings()