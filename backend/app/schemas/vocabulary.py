from pydantic import BaseModel
from typing import Optional, List

class VocabularyItemResponse(BaseModel):
    id: int
    word: str
    definition: str
    part_of_speech: str
    difficulty_level: str
    ipa: Optional[str] = None
    topic: str
    audio_url: Optional[str] = None
    example: Optional[str] = None
    scrambled_data: Optional[str] = None

class VocabularyCatalogResponse(BaseModel):
    topic: str
    level: str
    page: int
    size: int
    total: int
    items: List[VocabularyItemResponse]
