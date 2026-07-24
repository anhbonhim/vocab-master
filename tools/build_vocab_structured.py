#!/usr/bin/env python3
import os
import json
import re
import openpyxl
import wn

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
CEFRJ_PATH = f"{PROJECT_ROOT}/data/CEFR-J Wordlist Ver1.6.xlsx"
TATOEBA_DIR = f"{PROJECT_ROOT}/data/tatoeba_source"
OUTPUT_PATH = f"{PROJECT_ROOT}/data/src/main/assets/vocab_structured.json"
NGSL_PATH = f"{PROJECT_ROOT}/data/NGSL_500.txt"

def load_ngsl_500():
    try:
        with open(NGSL_PATH, "r", encoding="utf-8") as f:
            return set(line.strip().lower() for line in f if line.strip())
    except Exception:
        return set()

HIGH_FREQ_500 = load_ngsl_500()

def count_syllables(word):
    word = word.lower()
    vowels = "aeiouy"
    count = 0
    prev_vowel = False
    for char in word:
        is_vowel = char in vowels
        if is_vowel and not prev_vowel: count += 1
        prev_vowel = is_vowel
    if word.endswith("e"): count = max(1, count - 1)
    return max(1, count)

def difficulty_score(vocab_item):
    word = vocab_item["word"]
    score = min(len(word.split('/')[0]), 15) + count_syllables(word) * 2
    if word.lower() not in HIGH_FREQ_500: score += 8
    pos = vocab_item.get("pos", "")
    if "adverb" in pos: score += 4
    elif "verb" in pos: score += 3
    elif "adjective" in pos: score += 2
    elif "idiom" in pos or "phrasal" in pos: score += 8
    if ' ' in word or '/' in word: score += 5
    return score

def pos_to_wn(pos):
    pos = pos.lower()
    if "noun" in pos or "determiner" in pos: return "n"
    if "verb" in pos: return "v"
    if "adj" in pos: return "a"
    if "adv" in pos: return "r"
    return "n"

def build_structured_vocab():
    print("[1/4] Loading CEFR-J Wordlist A1...")
    wb = openpyxl.load_workbook(CEFRJ_PATH, read_only=True)
    ws = wb['A1']
    
    raw_vocab = []
    seen_words = set()
    
    for row in ws.iter_rows(min_row=2, values_only=True):
        if not row or not row[0]: continue
        headword = str(row[0]).strip()
        pos = str(row[1]).strip() if row[1] else "noun"
        
        clean_word = headword.split('/')[0].strip()
        key = (clean_word.lower(), pos)
        if key in seen_words: continue
        seen_words.add(key)
        
        raw_vocab.append({
            "word": clean_word,
            "original_headword": headword,
            "pos": pos
        })

    print(f"Total A1 raw words extracted from CEFR-J: {len(raw_vocab)}")
    
    raw_vocab.sort(key=difficulty_score)
    mid = len(raw_vocab) // 2
    for i, item in enumerate(raw_vocab):
        item["level"] = "A1.1" if i < mid else "A1.2"

    print("[2/4] Initializing Open English WordNet 2025...")
    ewn = wn.Wordnet("oewn:2025")

    print("[3/4] Lightning fast Reverse Lookup on Tatoeba En-Vn pairs...")
    # Step A: Load all Vietnamese sentences (only 2MB)
    vie_sentences = {}
    with open(f"{TATOEBA_DIR}/vie_sentences.tsv", "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) >= 3:
                sid, lang, text = parts[0], parts[1], parts[2]
                vie_sentences[sid] = text
    
    vie_sids = set(vie_sentences.keys())
    print(f"Loaded {len(vie_sentences)} Vietnamese sentences.")

    # Step B: Fast scan links.csv for any link connected to vie_sids
    eng_to_vie_links = {}
    needed_eng_sids = set()
    
    with open(f"{TATOEBA_DIR}/links.csv", "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) >= 2:
                id1, id2 = parts[0], parts[1]
                if id1 in vie_sids:
                    eng_to_vie_links.setdefault(id2, []).append(id1)
                    needed_eng_sids.add(id2)
                elif id2 in vie_sids:
                    eng_to_vie_links.setdefault(id1, []).append(id2)
                    needed_eng_sids.add(id1)

    print(f"Found {len(needed_eng_sids)} English sentence IDs linked to Vietnamese.")

    # Step C: Filter eng_sentences.tsv for only needed_eng_sids
    eng_sentences = {}
    with open(f"{TATOEBA_DIR}/eng_sentences.tsv", "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) >= 3:
                sid, lang, text = parts[0], parts[1], parts[2]
                if sid in needed_eng_sids:
                    eng_sentences[sid] = text

    print(f"Loaded {len(eng_sentences)} matching English sentences.")

    print("[4/4] Merging structured dataset...")
    structured_db = []

    for item in raw_vocab:
        word = item["word"]
        pos = item["pos"]
        level = item["level"]
        wn_pos = pos_to_wn(pos)
        
        synset_id = None
        definition = f"Basic {pos} meaning."
        synonyms = []
        antonyms = []
        hypernyms = []
        coordinate_terms = []
        oewn_examples = []
        
        synsets = ewn.synsets(word, pos=wn_pos)
        if not synsets:
            synsets = ewn.synsets(word)
            
        if synsets:
            primary_synset = synsets[0]
            synset_id = primary_synset.id
            definition = primary_synset.definition()
            
            for w in primary_synset.words():
                w_lemma = w.lemma()
                if w_lemma.lower() != word.lower() and w_lemma not in synonyms:
                    synonyms.append(w_lemma)
            
            for ex in primary_synset.examples():
                oewn_examples.append(ex)
                
            for hyp in primary_synset.hypernyms():
                hypernyms.append(hyp.id)
                for coord_syn in hyp.hyponyms():
                    if coord_syn.id != primary_synset.id:
                        for cw in coord_syn.words():
                            clemma = cw.lemma()
                            if clemma.lower() != word.lower() and clemma not in coordinate_terms:
                                coordinate_terms.append(clemma)

        if not coordinate_terms:
            coordinate_terms = ["item", "thing", "object", "concept"]

        max_words = 8 if level == "A1.1" else 12
        matched_examples = []
        pattern = re.compile(rf"\b{re.escape(word)}\b", re.IGNORECASE)
        
        for eng_id, eng_text in eng_sentences.items():
            if len(matched_examples) >= 3: break
            word_count = len(eng_text.split())
            if word_count > max_words: continue
            
            if pattern.search(eng_text):
                vie_ids = eng_to_vie_links.get(eng_id, [])
                for vid in vie_ids:
                    if vid in vie_sentences:
                        vie_text = vie_sentences[vid]
                        matched_examples.append({
                            "text": eng_text,
                            "translation": vie_text,
                            "source": f"tatoeba:{eng_id}"
                        })
                        break

        if not matched_examples and oewn_examples:
            matched_examples.append({
                "text": oewn_examples[0],
                "translation": f"Nghĩa của từ '{word}'.",
                "source": f"oewn:{synset_id}"
            })
        elif not matched_examples:
            matched_examples.append({
                "text": f"This is a {word}.",
                "translation": f"Đây là một {word}.",
                "source": "template:a1"
            })

        sources = ["cefrj:1.6"]
        if synset_id: sources.append(f"oewn:{synset_id}")
        if matched_examples and "tatoeba" in matched_examples[0]["source"]:
            sources.append(matched_examples[0]["source"])

        vi_trans = matched_examples[0]["translation"] if matched_examples else word

        structured_db.append({
            "word": word,
            "original_headword": item["original_headword"],
            "level": level,
            "pos": pos,
            "synset_id": synset_id,
            "definition": definition,
            "examples": matched_examples,
            "synonyms": synonyms[:4],
            "antonyms": antonyms[:4],
            "hypernyms": hypernyms[:2],
            "coordinate_terms": coordinate_terms[:10],
            "translations_vi": [vi_trans],
            "sources": sources
        })

    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(structured_db, f, ensure_ascii=False, indent=2)

    print(f"Successfully generated {OUTPUT_PATH} with {len(structured_db)} items!")

if __name__ == "__main__":
    build_structured_vocab()
