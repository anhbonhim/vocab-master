# Delta Spec: content-pipeline-v3

## ADDED Requirements

### Requirement: Deterministic Exercise Generation
- **Scenario: Generating Multiple Choice Distractors**
  Given a target word from CEFR-J A1 list,
  When generating a MULTIPLE_CHOICE question,
  Then the distractor options MUST be selected from the definitions of coordinate terms (sharing the same hypernym in Open English WordNet 2025), ensuring options are distinct and semantically relevant without any duplicates.

- **Scenario: Generating Sentence-Based Exercises**
  Given a target word,
  When generating FILL_IN_BLANK or SCRAMBLED questions,
  Then the prompt sentence MUST be drawn directly from proofread Open English WordNet 2025 examples or Tatoeba OK-tagged sentences, and MUST NOT be fabricated by an LLM.

- **Scenario: Sentence Length Control for CEFR Levels**
  Given a target word assigned to sublevel A1.1 or A1.2,
  When selecting an example sentence,
  Then the sentence length MUST NOT exceed 8 words for A1.1 and 12 words for A1.2.

### Requirement: License Attribution and Data Provenance
- **Scenario: Tracking Entry Sources**
  Given any vocabulary entry in `vocab_structured.json`,
  Then it MUST contain a `sources` array explicitly listing all attribution references (e.g., `["cefrj:1.6", "oewn:2025", "tatoeba:12345"]`).

- **Scenario: Displaying Open Licenses in App Settings**
  Given the user navigates to Settings Screen in the app,
  Then a "Data Sources & Licenses" section MUST be visible, displaying full attribution for CEFR-J Wordlist, Open English WordNet, Tatoeba, and Wiktionary.

### Requirement: LLM Scope Restriction to Narrative
- **Scenario: Generating Story Context**
  Given a unit curriculum config,
  When `generate_lessons_v3.py` runs,
  Then the LLM (qwen3-235b) MUST ONLY be invoked to generate `storySummary`, `scenarioContext`, and `keyPhrases`, and MUST NOT generate exercise prompts, options, or correct answers.

### Requirement: Legacy Asset and Script Cleanup
- **Scenario: Removing Orphan Asset JSON Files**
  Given `lessons_v3.json` has been generated and validated,
  When the build pipeline finishes,
  Then orphan asset files (`lessons_v2_repaired.json`, `lessons_v2_checkpoint.json`, `lessons_checkpoint.json`, `regenerate_queue.json`) MUST be permanently deleted from `data/src/main/assets/` to prevent APK bloating.

- **Scenario: Deleting Obsolete Pipeline Scripts**
  Given the new pipeline `tools/generate_lessons_v3.py` is operational,
  Then 9 legacy scripts (`generate_lessons_v2.py`, `generate_assets.py`, `generate_audio_edge_tts.py`, `update_listening_audio_urls.py`, `repair_lessons_v2.py`, `regenerate_broken.py`, root `validate_lessons_v2.py`, `fix_jar.py`) MUST be removed from the project root.

## MODIFIED Requirements

### Requirement: Seed Data Loading in Repository
- **Scenario: Loading Lessons V3 Asset**
  Given the Room database is empty on first launch,
  When `VocabularyRepositoryImpl` initializes pre-population,
  Then it MUST open and parse `lessons_v3.json` from assets instead of `lessons_v2.json`.
