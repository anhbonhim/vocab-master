from fastapi import FastAPI, Depends
from app.config import settings
from app.utils.firebase_auth import get_current_user_uid
from app.database import engine, Base
from app.routers import vocabulary, placement, sync, curriculum, report

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION
)

# Include routers
app.include_router(vocabulary.router)
app.include_router(placement.router)
app.include_router(sync.router)
app.include_router(curriculum.router)
app.include_router(report.router)

@app.get("/")
def read_root():
    return {"message": "Welcome to Vocab Master API. Visit /docs for Swagger UI."}

@app.get("/api/v1/health")
def health_check():
    return {"status": "ok"}

@app.get("/api/v1/me")
def get_my_profile(uid: str = Depends(get_current_user_uid)):
    return {
        "status": "success",
        "message": "You are authenticated",
        "firebase_uid": uid
    }