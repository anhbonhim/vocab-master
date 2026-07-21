#!/usr/bin/env python3
"""
Golden-vector generator for VocabMaster FSRS-6 Kotlin port.

Regen command (one-time; rerun manually only when bumping py-fsrs or DEFAULT_PARAMETERS):
    pip install fsrs==6.3.1 && python3 domain/scripts/generate_fsrs_golden_vectors.py

This script is dev-only and is NOT shipped in the APK.
"""

import json
import math
from datetime import datetime, timezone, timedelta
from fsrs import Scheduler, Card, Rating, State

PY_FSRS_VERSION = "6.3.1"
START = datetime(2022, 11, 29, 12, 30, 0, tzinfo=timezone.utc)


def iso(dt):
    """ISO-8601 UTC string with explicit +00:00 offset."""
    return dt.isoformat()


def interval_days(card):
    """Whole days between last_review and due (floor of delta/86400s)."""
    if card.last_review is None or card.due is None:
        return 0
    return (card.due - card.last_review).days


def card_snapshot(card):
    return {
        "state": card.state.name,
        "step": card.step,
        "stability": card.stability,
        "difficulty": card.difficulty,
        "due": iso(card.due),
        "last_review": iso(card.last_review) if card.last_review else None,
        "interval_days": interval_days(card),
    }


def run_reviews(scheduler, initial_card, ratings, datetimes):
    """Replay a list of ratings through a scheduler and return per-review snapshots."""
    card = initial_card
    snapshots = []
    for rating, dt in zip(ratings, datetimes):
        card, _ = scheduler.review_card(card, rating, dt)
        snapshots.append(card_snapshot(card))
    return snapshots


def make_vector(vector_id, initial_card, ratings, datetimes, scheduler):
    return {
        "id": vector_id,
        "initial_card": card_snapshot(initial_card),
        "reviews": [
            {"rating": r.name, "datetime": iso(dt)} for r, dt in zip(ratings, datetimes)
        ],
        "expected_after_each": run_reviews(scheduler, initial_card, ratings, datetimes),
    }


def pristine_card(due=None):
    """A brand-new py-fsrs card (Learning, step=0, S/D null)."""
    return Card(card_id=1, due=due if due else START)


def build_card(state, step, stability, difficulty, due, last_review):
    """Construct a pre-built card for targeted state coverage."""
    return Card(
        card_id=1,
        state=state,
        step=step,
        stability=stability,
        difficulty=difficulty,
        due=due,
        last_review=last_review,
    )


def build_vector_set():
    scheduler = Scheduler(enable_fuzzing=False)
    vectors = []

    # --- Single-review from pristine ---
    for rating in (Rating.Again, Rating.Hard, Rating.Good, Rating.Easy):
        vectors.append(
            make_vector(
                f"pristine_{rating.name.lower()}",
                pristine_card(),
                [rating],
                [START],
                scheduler,
            )
        )

    # --- Single-review from pre-built Learning (step=1, S=2.5, D=5.0) ---
    learning_card = build_card(
        State.Learning,
        1,
        2.5,
        5.0,
        START,
        START - timedelta(days=1),
    )
    for rating in (Rating.Again, Rating.Hard, Rating.Good, Rating.Easy):
        vectors.append(
            make_vector(
                f"learning_{rating.name.lower()}",
                learning_card,
                [rating],
                [START],
                scheduler,
            )
        )

    # --- Single-review from pre-built Review (S=12.5, D=5.6, last_review 10 days ago) ---
    review_card = build_card(
        State.Review,
        None,
        12.5,
        5.6,
        START,
        START - timedelta(days=10),
    )
    for rating in (Rating.Again, Rating.Hard, Rating.Good, Rating.Easy):
        vectors.append(
            make_vector(
                f"review_{rating.name.lower()}",
                review_card,
                [rating],
                [START],
                scheduler,
            )
        )

    # --- Single-review from pre-built Relearning (step=0, S=0.8, D=6.0) ---
    relearning_card = build_card(
        State.Relearning,
        0,
        0.8,
        6.0,
        START,
        START - timedelta(days=1),
    )
    for rating in (Rating.Again, Rating.Hard, Rating.Good, Rating.Easy):
        vectors.append(
            make_vector(
                f"relearning_{rating.name.lower()}",
                relearning_card,
                [rating],
                [START],
                scheduler,
            )
        )

    # --- Sequences: Good x 1, 3, 5, 10, 30 ---
    for length in (1, 3, 5, 10, 30):
        card = pristine_card()
        datetimes = []
        dt = START
        for _ in range(length):
            dt = card.due
            datetimes.append(dt)
            card, _ = scheduler.review_card(card, Rating.Good, dt)
        # Reset card for the vector, datetimes already follow card.due progression
        vectors.append(
            make_vector(
                f"good_sequence_{length}",
                pristine_card(),
                [Rating.Good] * length,
                datetimes,
                scheduler,
            )
        )

    # --- Mixed 13-rating sequence: Good x 6, Again x 2, Good x 5 ---
    mixed_ratings = (
        [Rating.Good] * 6
        + [Rating.Again] * 2
        + [Rating.Good] * 5
    )
    card = pristine_card()
    datetimes = []
    dt = START
    for _ in range(len(mixed_ratings)):
        dt = card.due
        datetimes.append(dt)
        card, _ = scheduler.review_card(card, mixed_ratings[len(datetimes) - 1], dt)
    vectors.append(
        make_vector(
            "mixed_good6_again2_good5",
            pristine_card(),
            mixed_ratings,
            datetimes,
            scheduler,
        )
    )

    # --- Lapse path: Good, Good, Again, Good ---
    lapse_ratings = [Rating.Good, Rating.Good, Rating.Again, Rating.Good]
    card = pristine_card()
    datetimes = []
    dt = START
    for r in lapse_ratings:
        dt = card.due
        datetimes.append(dt)
        card, _ = scheduler.review_card(card, r, dt)
    vectors.append(
        make_vector(
            "lapse_good_good_again_good",
            pristine_card(),
            lapse_ratings,
            datetimes,
            scheduler,
        )
    )

    # --- Additional sequences: Easy x 5, Again x 5, Hard x 5 ---
    for rating, label in ((Rating.Easy, "easy"), (Rating.Again, "again"), (Rating.Hard, "hard")):
        for length in (3, 5):
            card = pristine_card()
            datetimes = []
            dt = START
            for _ in range(length):
                dt = card.due
                datetimes.append(dt)
                card, _ = scheduler.review_card(card, rating, dt)
            vectors.append(
                make_vector(
                    f"{label}_sequence_{length}",
                    pristine_card(),
                    [rating] * length,
                    datetimes,
                    scheduler,
                )
            )

    # --- Mixed Hard/Easy: Good, Hard, Easy, Good ---
    mixed_he_ratings = [Rating.Good, Rating.Hard, Rating.Easy, Rating.Good]
    card = pristine_card()
    datetimes = []
    dt = START
    for r in mixed_he_ratings:
        dt = card.due
        datetimes.append(dt)
        card, _ = scheduler.review_card(card, r, dt)
    vectors.append(
        make_vector(
            "mixed_good_hard_easy_good",
            pristine_card(),
            mixed_he_ratings,
            datetimes,
            scheduler,
        )
    )

    # --- Relearning recovery: Good, Good, Again, Good, Good ---
    relearn_ratings = [Rating.Good, Rating.Good, Rating.Again, Rating.Good, Rating.Good]
    card = pristine_card()
    datetimes = []
    dt = START
    for r in relearn_ratings:
        dt = card.due
        datetimes.append(dt)
        card, _ = scheduler.review_card(card, r, dt)
    vectors.append(
        make_vector(
            "relearning_recovery",
            pristine_card(),
            relearn_ratings,
            datetimes,
            scheduler,
        )
    )

    # --- Same-day: pristine + Good, then Good 5 minutes later ---
    vectors.append(
        make_vector(
            "same_day_learning_good_good",
            pristine_card(),
            [Rating.Good, Rating.Good],
            [START, START + timedelta(minutes=5)],
            scheduler,
        )
    )

    # --- Same-day: Review-state card reviewed Good 1 hour later ---
    same_day_review_card = build_card(
        State.Review,
        None,
        12.5,
        5.6,
        START,
        START - timedelta(hours=2),
    )
    vectors.append(
        make_vector(
            "same_day_review_good",
            same_day_review_card,
            [Rating.Good],
            [START],
            scheduler,
        )
    )

    # --- Edge case: 200 Again reviews one day apart -> stability floor ---
    again200_ratings = [Rating.Again] * 200
    again200_datetimes = [START + timedelta(days=i) for i in range(200)]
    vectors.append(
        make_vector(
            "again_200_days",
            pristine_card(),
            again200_ratings,
            again200_datetimes,
            scheduler,
        )
    )

    # --- Edge case: 10 Easy same-second reviews -> difficulty floor ---
    easy10_datetimes = [START + timedelta(seconds=i) for i in range(10)]
    vectors.append(
        make_vector(
            "easy_10_same_second",
            pristine_card(),
            [Rating.Easy] * 10,
            easy10_datetimes,
            scheduler,
        )
    )

    # --- Edge case: Review-state card with S=40000, Good 1 day later -> interval cap ---
    huge_s_card = build_card(
        State.Review,
        None,
        40000.0,
        5.0,
        START,
        START - timedelta(days=1),
    )
    vectors.append(
        make_vector(
            "huge_stability_interval_cap",
            huge_s_card,
            [Rating.Good],
            [START],
            scheduler,
        )
    )

    return vectors


def main():
    vectors = build_vector_set()
    output = {
        "py_fsrs_version": PY_FSRS_VERSION,
        "enable_fuzzing": False,
        "vectors": vectors,
    }
    with open("domain/src/test/resources/fsrs/golden_vectors.json", "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2)
    print(f"Generated {len(vectors)} golden vectors.")


if __name__ == "__main__":
    main()
