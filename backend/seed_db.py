import json
import sqlite3
import sys
import os

def seed_db():
    conn = sqlite3.connect('vocab.db')
    cursor = conn.cursor()

    # Drop and recreate vocabulary table to clear old bad data
    cursor.execute('DROP TABLE IF EXISTS vocabulary')
    cursor.execute('''
    CREATE TABLE vocabulary (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        word TEXT NOT NULL,
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

    # Map CEFR levels to initial IRT difficulty parameters (b)
    level_difficulty_map = {
        "A1": -2.0,
        "A2": -1.0,
        "B1": 0.0,
        "B2": 1.0,
        "C1": 2.0,
        "C2": 3.0
    }

    print("Reading lessons_v3.json...")
    assets_path = os.path.join(os.path.dirname(__file__), '..', 'data', 'src', 'main', 'assets', 'lessons_v3.json')
    try:
        with open(assets_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: assets file not found at {assets_path}")
        conn.close()
        return

    print("Inserting UNIQUE vocabulary records (FILL_IN_BLANK & MULTIPLE_CHOICE)...")
    inserted_count = 0
    skipped_duplicates = 0
    seen_words = set()

    for section in data.get('sections', []):
        level = section.get('cefrSublevel', 'A2').split('.')[0]
        for unit in section.get('units', []):
            topic = unit.get('topic', 'general')
            for node in unit.get('nodes', []):
                for session in node.get('sessions', []):
                    for q in session.get('questions', []):
                        q_type = q.get('type')
                        if q_type not in ['MULTIPLE_CHOICE', 'FILL_IN_BLANK']:
                            continue

                        word = q.get('word') or q.get('prompt', '').strip()
                        if not word:
                            continue

                        # Deduplicate by word: keep the FIRST occurrence only
                        if word in seen_words:
                            skipped_duplicates += 1
                            continue
                        seen_words.add(word)

                        # Vietnamese sentence translation (for hint/reference)
                        definition = q.get('translation', '') or word

                        # English fill-in-the-blank prompt
                        example = q.get('prompt', '')

                        # Embed the 4 options + correctIndex so placement.py
                        # doesn't need to generate distractors (avoids duplicates)
                        options_list = q.get('options') or []
                        correct_idx = q.get('correctIndex', 0)
                        scrambled_data = json.dumps({
                            "options": options_list,
                            "correctIndex": correct_idx
                        }, ensure_ascii=False)

                        audio_url = q.get('audioUrl', '')
                        b = level_difficulty_map.get(level, -1.0)
                        a = 1.0

                        try:
                            cursor.execute('''
                            INSERT INTO vocabulary (word, definition, part_of_speech, difficulty_level, ipa, topic, audio_url, example, scrambled_data, irt_difficulty, irt_discrimination)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ''', (word, definition, "noun", level, "", topic, audio_url, example, scrambled_data, b, a))
                            inserted_count += 1
                        except sqlite3.IntegrityError:
                            pass

    conn.commit()
    print(f"Inserted {inserted_count} unique vocabulary items. Skipped {skipped_duplicates} duplicates.")
    conn.close()

if __name__ == '__main__':
    seed_db()
