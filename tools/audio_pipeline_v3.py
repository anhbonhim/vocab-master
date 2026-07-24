#!/usr/bin/env python3
"""
Audio Pipeline V3:
Updates LISTENING and sentence-based questions in lessons_v3.json to point
to appropriate audio CDN URLs (word-level vs sentence-level).
"""

import json
import os
import re

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
JSON_PATH = f"{PROJECT_ROOT}/data/src/main/assets/lessons_v3.json"
CDN_BASE = "https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2"

def get_safe_filename(text: str) -> str:
    first_part = text.split('/')[0]
    clean = re.sub(r'[^a-zA-Z0-9]', '', first_part).lower()
    return clean if clean else "audio_" + str(abs(hash(text)))

def process_audio_urls():
    if not os.path.exists(JSON_PATH):
        print(f"[!] File not found: {JSON_PATH}")
        return

    with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    updated_count = 0

    for sec in data.get("sections", []):
        for unit in sec.get("units", []):
            for node in unit.get("nodes", []):
                for session in node.get("sessions", []):
                    for q in session.get("questions", []):
                        qtype = q.get("type")
                        word = q.get("word")
                        
                        # Word-level audio (default)
                        if word:
                            safe_w = get_safe_filename(word)
                            q["audioUrl"] = f"{CDN_BASE}/words/{safe_w}.ogg"
                            q["audioUrlSlow"] = f"{CDN_BASE}/words/{safe_w}_slow.ogg"

                        # Sentence-level audio for LISTENING & SCRAMBLED & INTRODUCTION
                        if qtype == "LISTENING" and q.get("prompt"):
                            # If prompt is a full sentence, use sentence-level audio
                            prompt_text = q.get("prompt")
                            if len(prompt_text.split()) > 2 and "choose the word" not in prompt_text.lower():
                                safe_s = get_safe_filename(prompt_text)
                                q["audioUrl"] = f"{CDN_BASE}/sentences/{safe_s}.ogg"
                                q["audioUrlSlow"] = f"{CDN_BASE}/sentences/{safe_s}_slow.ogg"
                                updated_count += 1

    with open(JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"[+] Audio Pipeline V3 completed! Updated {updated_count} sentence audio URLs in {JSON_PATH}.")

if __name__ == "__main__":
    process_audio_urls()
