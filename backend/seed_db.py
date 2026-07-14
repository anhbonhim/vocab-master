import json
import sqlite3
import sys

def seed_db():
    conn = sqlite3.connect('vocab.db')
    cursor = conn.cursor()
    
    # Create vocabulary table matching model definition
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS vocabulary (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        word TEXT UNIQUE NOT NULL,
        definition TEXT NOT NULL,
        part_of_speech TEXT,
        difficulty_level TEXT,
        ipa TEXT,
        topic TEXT DEFAULT 'general',
        audio_url TEXT,
        example TEXT,
        scrambled_data TEXT,
        irt_difficulty REAL DEFAULT 0.0,
        irt_discrimination REAL DEFAULT 1.0
    )
    ''')
    
    # Check if vocabulary is already populated
    cursor.execute("SELECT COUNT(*) FROM vocabulary")
    count = cursor.fetchone()[0]
    if count > 0:
        print("Database already seeded.")
        conn.close()
        return

    # Map CEFR levels to initial IRT difficulty parameters (b)
    # A1: -2.0, A2: -1.0, B1: 0.0, B2: 1.0, C1: 2.0, C2: 3.0
    level_difficulty_map = {
        "A1": -2.0,
        "A2": -1.0,
        "B1": 0.0,
        "B2": 1.0,
        "C1": 2.0,
        "C2": 3.0
    }

    print("Reading vocabulary JSON...")
    # Read the assets file from the Android project directory
    assets_path = '../data/src/main/assets/vocabulary.json'
    try:
        with open(assets_path, 'r', encoding='utf-8') as f:
            items = json.load(f)
    except FileNotFoundError:
        print(f"Error: assets file not found at {assets_path}")
        conn.close()
        return

    print("Inserting vocabulary records into SQLite...")
    inserted_count = 0
    for item in items:
        word = item.get("word")
        level = item.get("level", "A2")
        pos = item.get("type", "")
        definition = item.get("translation", "")
        ipa = item.get("phonetic", "")
        topic = item.get("topic", "general")
        audio_url = item.get("audioUrl", "")
        
        # Determine example
        example = item.get("exampleBeginner") or item.get("exampleIntermediate") or item.get("exampleAdvanced") or ""
        scrambled_data = item.get("scrambledSentenceData", "[]")
        
        # Standard IRT parameters based on level
        b = level_difficulty_map.get(level, -1.0)
        a = 1.0  # Default discrimination
        
        try:
            cursor.execute('''
            INSERT INTO vocabulary (word, definition, part_of_speech, difficulty_level, ipa, topic, audio_url, example, scrambled_data, irt_difficulty, irt_discrimination)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (word, definition, pos, level, ipa, topic, audio_url, example, scrambled_data, b, a))
            inserted_count += 1
        except sqlite3.IntegrityError:
            # Word already exists, skip
            pass
            
    conn.commit()
    print(f"Successfully seeded {inserted_count} vocabulary items.")
    conn.close()

if __name__ == '__main__':
    seed_db()