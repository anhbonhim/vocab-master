# Plan 01-01: Port py-fsrs 6.3.1 engine to Kotlin

## Context & Objectives
- Ported canonical py-fsrs 6.3.1 (FSRS-6, 21 parameters) scheduler to Kotlin in `domain/fsrs/v6/`.
- Produced mathematically identical schedule outcomes validated against an independent golden-vector JSON dataset without using string conversions.

## Accomplished
- **Script:** Developed and committed Python generator `domain/scripts/generate_fsrs_golden_vectors.py` (explicit instructions in header: requires `pip install fsrs==6.3.1`).
- **Data:** Wrote `domain/src/test/resources/fsrs/golden_vectors.json`, covering 36 vectors from pristine to full paths (short intervals, lapse sequences, consecutive easy/again blocks), along with edge cases (min stability, min/max difficulty, fuzz intervals).
- **Domain Layer:** Ported `State`, `Rating`, `Card`, `ReviewLog`, and `Scheduler` natively (using Kotlin Banker's rounding via `kotlin.math.round`).
- **Validation:** Implemented JUnit test suite `GoldenVectorTest` evaluating Kotlin math directly against the deterministic JSON output per-review. Tolerance bound: `1e-6` for Double comparisons, exact equality for Enums and Longs. `:domain:test` succeeds, and `detekt` verifies zero usage of `String.format` and `Locale`.

## Ground Truth Alignments
- **Parameter Conflict (D-06):** Resolved the difference noted in CONTEXT.md (`w[0] = 0.2172`) against py-fsrs 6.3.1 itself, selecting `0.212` for `w[0]` (matching the installed Python ground truth to pass golden tests).
- **State Semantics:** Mapped `State.New` functionally onto py-fsrs pristine card (where Python dictates `state=Learning` with `step=0`). After the initial review, cards traverse between Learning and Review as dictated by core logic, avoiding any further mapping misalignments.
- **Banker's Rounding:** Enforced `kotlin.math.round()` over `roundToInt()` for fuzzing calculation accuracy.

This implementation acts as a stable drop-in target for subsequent Room database and UI consumption steps.