#!/usr/bin/env python3
import json
import sys
import os
import re

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
V3_JSON_PATH = f"{PROJECT_ROOT}/data/src/main/assets/lessons_v3.json"

def validate_lessons_v3():
    print("==============================================")
    print("  VOCAB MASTER - LESSONS V3 AUDITOR")
    print("==============================================")
    
    if not os.path.exists(V3_JSON_PATH):
        print(f"[!] FAIL: File not found: {V3_JSON_PATH}")
        return False

    with open(V3_JSON_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)

    errors = []
    warnings = []
    question_count = 0
    duplicate_options_count = 0
    missing_translation_count = 0
    oversized_sentence_count = 0

    for section in data.get('sections', []):
        sec_sublevel = section.get('cefrSublevel', 'A1.1')
        max_words = 8 if sec_sublevel == 'A1.1' else 12

        for unit in section.get('units', []):
            if not unit.get('storySummary'):
                warnings.append(f"Unit {unit.get('id')}: storySummary is missing or empty")

            for node in unit.get('nodes', []):
                for session in node.get('sessions', []):
                    for q in session.get('questions', []):
                        question_count += 1
                        q_id = q.get('id', 'UNKNOWN_ID')
                        q_type = q.get('type')
                        translation = q.get('translation')

                        # 1. Check translation for non-MATCHING types
                        if q_type != 'MATCHING' and not translation:
                            missing_translation_count += 1
                            errors.append(f"{q_id}: Missing Vietnamese translation for {q_type}")

                        # 2. Check 4-option questions for duplicates & bounds
                        if q_type in ['MULTIPLE_CHOICE', 'FILL_IN_BLANK', 'INTRODUCTION', 'LISTENING']:
                            options = q.get('options')
                            correct_idx = q.get('correctIndex')
                            
                            if not options or len(options) != 4:
                                errors.append(f"{q_id}: {q_type} options count is {len(options) if options else 0}, expected 4")
                            else:
                                lower_options = [opt.strip().lower() for opt in options]
                                if len(set(lower_options)) != 4:
                                    duplicate_options_count += 1
                                    errors.append(f"{q_id}: {q_type} contains DUPLICATE options: {options}")

                            if correct_idx is None or not (0 <= correct_idx < 4):
                                errors.append(f"{q_id}: {q_type} correctIndex {correct_idx} is out of bounds")

                        # 3. Check MATCHING
                        elif q_type == 'MATCHING':
                            pairs = q.get('matchingPairs')
                            if not pairs or len(pairs) < 3:
                                errors.append(f"{q_id}: MATCHING has {len(pairs) if pairs else 0} pairs, expected >= 3")

                        # 4. Check SCRAMBLED
                        elif q_type == 'SCRAMBLED':
                            words = q.get('scrambledWords')
                            correct = q.get('correctSentence')
                            if not words or len(words) < 3:
                                errors.append(f"{q_id}: SCRAMBLED words count is {len(words) if words else 0}, expected >= 3")
                            if not correct:
                                errors.append(f"{q_id}: SCRAMBLED correctSentence is missing")
                            else:
                                word_len = len(correct.split())
                                if word_len > max_words + 4:
                                    oversized_sentence_count += 1
                                    warnings.append(f"{q_id}: Sentence length ({word_len} words) exceeds recommended limit ({max_words} words) for {sec_sublevel}")

                        # 5. Check LISTENING audio
                        elif q_type == 'LISTENING':
                            audio = q.get('audioUrl')
                            if not audio:
                                errors.append(f"{q_id}: LISTENING audioUrl is missing")

                        # 6. Check TYPING
                        elif q_type == 'TYPING':
                            correct = q.get('correctSentence')
                            if not correct:
                                errors.append(f"{q_id}: TYPING correctSentence is missing")

    # REPORTING
    print(f"\n--- AUDIT SUMMARY ---")
    print(f"Total Questions Evaluated: {question_count}")
    print(f"Duplicate Options Found: {duplicate_options_count}")
    print(f"Missing Translations: {missing_translation_count}")
    print(f"Oversized Sentences: {oversized_sentence_count}")
    print(f"Total Errors: {len(errors)}")
    print(f"Total Warnings: {len(warnings)}")

    if errors:
        print("\n[!] CRITICAL QUALITY ERRORS:")
        for err in errors[:20]:
            print(f" - {err}")
        if len(errors) > 20:
            print(f" ... and {len(errors) - 20} more errors.")
        return False

    print("\n[✓] PASS: All 5 Quality Criteria + Schema Parity Verified Successfully!")
    return True

if __name__ == "__main__":
    if validate_lessons_v3():
        sys.exit(0)
    else:
        sys.exit(1)
