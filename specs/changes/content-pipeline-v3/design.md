# Technical Design: content-pipeline-v3

## 1. Data Schema

### 1.1 `vocab_structured.json` (Intermediate Database)
This file represents the consolidated gold-standard dictionary database. Every word in the curriculum has a corresponding entry here.

```json
{
  "word": "string",
  "level": "A1.1 | A1.2",
  "pos": "noun | verb | adjective | adverb",
  "synset_id": "string (oewn-XXXX)",
  "definition": "string (from OEWN)",
  "examples": [
    {
      "text": "string (English example)",
      "translation": "string (Vietnamese translation)",
      "audio_url": "string (optional Tatoeba audio URL)",
      "source": "tatoeba:ID | oewn:ID"
    }
  ],
  "synonyms": ["string"],
  "antonyms": ["string"],
  "hypernyms": ["string (synset_id)"],
  "coordinate_terms": ["string (words sharing the same hypernym)"],
  "translations_vi": ["string"],
  "sources": ["string"]
}
```

### 1.2 `lessons_v3.json` Parity
Must strictly match the schema expected by `VocabularyRepositoryImpl.kt` and `QuizViewModel.kt` (identical to `lessons_v2.json` schema):
- **Structure:** `sections` -> `units` -> `nodes` (LESSON, REVIEW, CHECKPOINT) -> `sessions` -> `questions`
- **Question Schema:** `id`, `word`, `type` (1 of 7), `prompt`, `options` (exactly 4 for MC/Intro/Listening), `correctIndex`, `correctSentence`, `scrambledWords`, `translation`, `audioUrl`, `audioUrlSlow`, `matchingPairs`.

---

## 2. Extraction & Merge Logic

### 2.1 CEFR-J Wordlist Parsing
1. Parse the A1 sheet from `CEFRJ_wordlist_ver1.6.xlsx`.
2. Extract the word list, POS, and map Japanese school level to app levels (e.g., elementary/JHS 1 -> A1.1, JHS 2 -> A1.2).
3. Store as the base target dictionary.

### 2.2 Open English WordNet (OEWN) Integration
Using python `wn` library:
1. Download `oewn:2025`.
2. For each target word, look up matching synsets by word and POS.
3. Extract definitions (`synset.definition()`), examples (`synset.examples()`), synonyms, antonyms, hypernyms (`synset.hypernyms()`), and coordinate terms (words in synsets sharing the same hypernym).

### 2.3 Tatoeba En-Vn Mapping
1. Parse weekly Tatoeba dump files `sentences.csv`, `links.csv`, `tags.csv`.
2. Filter for sentences in English (`eng`) and Vietnamese (`vie`).
3. Retain only sentences with the `@ok` tag or belonging to CK's proofread list (ID: 907).
4. Match English sentences containing the target word with their linked Vietnamese translations.
5. If translation exists, add to `examples` array with source `tatoeba:ID`.
6. Backfill missing translations using Wiktionary data if no Tatoeba translations exist.

---

## 3. Question Generation Algorithms (Deterministic)

### 3.1 MULTIPLE_CHOICE
- **Prompt:** `"What is the meaning of: {word}"` (or `"What does '{word}' mean?"`)
- **Correct Option:** The `definition` from OEWN.
- **Distractors:** Definitions of 3 `coordinate_terms` (words sharing the same hypernym in OEWN) with the same POS. This guarantees distractors are semantically related (e.g., if word is "cat", distractors are definitions of "dog", "horse", "cow") but legally distinct and never duplicates.

### 3.2 FILL_IN_BLANK
- **Prompt:** A selected example sentence from OEWN/Tatoeba with the target word replaced by `"_____"` (5 underscores).
- **Correct Option:** The target word.
- **Distractors:** 3 synonyms or coordinate terms with the same POS.

### 3.3 INTRODUCTION
- **Prompt:** A full example sentence showing the word in context.
- **Options:** The target word + 3 coordinate terms. Correct Index = 0.

### 3.4 LISTENING
- **Prompt:** `"Listen and choose the word you hear"`
- **Options:** The target word + 3 homophones or words with identical syllable counts and POS.

### 3.5 MATCHING
- **Prompt:** `"Match the English words with their Vietnamese meanings"`
- **Pairs:** 4 target words from the current lesson module mapped to their primary translations.

### 3.6 SCRAMBLED
- **Correct Sentence:** An example sentence.
- **Scrambled Words:** Tokenized and shuffled words of the example sentence.

---

## 4. Audio Pipeline Integration

1. **Word-Level Audio:** Reuse `generate_audio_edge_tts_v2.py` logic (using Edge-TTS `en-US-AriaNeural`) to generate word pronunciations.
2. **Sentence-Level Audio:** 
   - Scan Tatoeba example sentences mapped to words.
   - Cross-reference with Tatoeba audio lists.
   - Filter out files with `CC BY-NC 4.0` or "no offsite license".
   - Only download audio from contributors licensing under `CC BY 4.0` or `CC0`.
   - Fall back to Edge-TTS generated sentence audio if no open-licensed Tatoeba audio exists.

---

## 5. Cleanup Execution Plan

```
Root/
├── generate_lessons_v2.py            ──► [DELETE] (Obsolete LLM gen)
├── generate_assets.py                ──► [DELETE] (Obsolete LLM TTS)
├── generate_audio_edge_tts.py        ──► [DELETE] (Use v2 instead)
├── update_listening_audio_urls.py    ──► [DELETE] (Merged to new generator)
├── repair_lessons_v2.py              ──► [DELETE] (Merged to new validator)
├── regenerate_broken.py              ──► [DELETE] (Obsolete selective gen)
├── validate_lessons_v2.py            ──► [DELETE] (Dup in tools/)
├── fix_jar.py                        ──► [DELETE] (Orphan utility)
│
├── logs/                             ──► [CREATE]
│   └── (Move all log files here)     ──► [MOVE] (*.log, *_log.txt, logcat*)
│
└── data/src/main/assets/
    ├── lessons_v2.json               ──► [DELETE] (After v3 validated)
    ├── lessons_v2_repaired.json      ──► [DELETE] (Orphan intermediate)
    ├── lessons_v2_checkpoint.json    ──► [DELETE] (Orphan checkpoint)
    ├── lessons_checkpoint.json       ──► [DELETE] (Orphan checkpoint)
    ├── regenerate_queue.json         ──► [DELETE] (Orphan queue)
    │
    ├── vocab_structured.json         ──► [NEW] (Intermediate DB)
    ├── lessons_v3.json               ──► [NEW] (New curriculum seed)
    └── THIRD_PARTY_LICENSES.md       ──► [NEW] (Attribution file)
```
