import firebase_admin
from firebase_admin import credentials, auth
from fastapi import Request, HTTPException, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.config import settings
from typing import Optional

# Initialize Firebase Admin
try:
    cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
    firebase_admin.initialize_app(cred)
    print("Firebase Admin initialized successfully.")
except Exception as e:
    print(f"Error initializing Firebase Admin: {e}")
    # Don't crash immediately, allows app to start for testing without auth
    pass

security = HTTPBearer(auto_error=False)

def verify_token(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> Optional[dict]:
    """
    Verify the Firebase ID token from the Authorization header if present.
    Returns the decoded token dictionary if valid, or None if missing/invalid (if optional).
    """
    if not credentials:
        return None
    token = credentials.credentials
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token
    except Exception as e:
        # If credentials are provided but invalid, we still want to raise an error
        raise HTTPException(
            status_code=401,
            detail=f"Invalid authentication credentials: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )

def get_current_user_uid(decoded_token: Optional[dict] = Security(verify_token)) -> str:
    """
    Dependency to extract the Firebase UID from the verified token. Requires auth.
    """
    if not decoded_token:
        raise HTTPException(
            status_code=401,
            detail="Authentication required",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return decoded_token.get("uid")

def get_optional_user_uid(decoded_token: Optional[dict] = Security(verify_token)) -> Optional[str]:
    """
    Dependency to optionally extract the Firebase UID if user is authenticated.
    """
    if not decoded_token:
        return None
    return decoded_token.get("uid")