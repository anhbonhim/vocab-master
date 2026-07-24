"""
Tests for the Pydantic V2 LLM Schemas (CONT-03, T-05-02).

These tests pin the public contract for the strict validator that sits
between any LLM response (Opencode API, future providers) and the SQLite
lesson payload. Per CONT-03 and threat T-05-02, raw LLM output MUST NOT
be persisted without Pydantic validation.

Coverage:
- Valid JSON payload parses into LLMResponse (a list of ExerciseItem).
- Missing required fields raise ValidationError.
- Malformed JSON string raises ValueError.
- Markdown ```json ... ``` wrappers are stripped before validation.
"""
import json
import pytest
from pydantic import ValidationError

from app.schemas.llm import (
    ExerciseItem,
    LLMResponse,
    validate_llm_output,
)


# ---------------------------------------------------------------------------
# Happy path: a clean JSON payload must parse into LLMResponse
# ---------------------------------------------------------------------------


def test_valid_json_parses_into_llm_response_with_exercise_list():
    """
    A minimal but complete valid payload must round-trip through
    LLMResponse and yield a populated list of ExerciseItem objects.
    """
    payload = {
        "exercises": [
            {
                "type": "multiple_choice",
                "question": "What is 'apple' in Vietnamese?",
                "options": ["Quả táo", "Quả cam", "Quả nho", "Quả chuối"],
                "correct_answer": "Quả táo",
            },
            {
                "type": "fill_blank",
                "question": "I ___ an apple.",
                "correct_answer": "eat",
            },
        ]
    }
    json_str = json.dumps(payload)

    response = validate_llm_output(json_str)

    assert isinstance(response, LLMResponse)
    assert len(response.exercises) == 2
    assert isinstance(response.exercises[0], ExerciseItem)
    assert response.exercises[0].type == "multiple_choice"
    assert response.exercises[0].options == ["Quả táo", "Quả cam", "Quả nho", "Quả chuối"]
    assert response.exercises[1].type == "fill_blank"
    assert response.exercises[1].options is None  # Optional field default


# ---------------------------------------------------------------------------
# Markdown stripping: LLM often wraps JSON in ```json ... ```
# ---------------------------------------------------------------------------


def test_markdown_json_code_block_is_stripped_before_validation():
    """
    LLM responses are commonly wrapped in ```json ... ``` markdown.
    The validator MUST strip the wrapper BEFORE handing the payload to
    Pydantic's model_validate_json, otherwise it raises a parse error
    on the first backtick.
    """
    payload = {
        "exercises": [
            {
                "type": "listening",
                "question": "Listen and choose the correct word.",
                "options": ["cat", "bat", "rat", "mat"],
                "correct_answer": "cat",
            }
        ]
    }
    wrapped = "```json\n" + json.dumps(payload) + "\n```"

    response = validate_llm_output(wrapped)

    assert isinstance(response, LLMResponse)
    assert len(response.exercises) == 1
    assert response.exercises[0].type == "listening"


def test_plain_markdown_fence_without_json_lang_is_stripped():
    """
    Some LLM responses use bare ``` fences without the 'json' hint.
    Those must also be tolerated.
    """
    payload = {
        "exercises": [
            {
                "type": "fill_blank",
                "question": "Sky is ___.",
                "correct_answer": "blue",
            }
        ]
    }
    wrapped = "```\n" + json.dumps(payload) + "\n```"

    response = validate_llm_output(wrapped)

    assert isinstance(response, LLMResponse)
    assert response.exercises[0].correct_answer == "blue"


# ---------------------------------------------------------------------------
# Failure modes: missing fields and malformed JSON
# ---------------------------------------------------------------------------


def test_missing_required_fields_raises_validation_error():
    """
    A payload missing the 'question' or 'correct_answer' or 'type' field
    must raise ValidationError, which validate_llm_output propagates
    as a ValueError so callers can catch a single error type.
    """
    # 'question' is missing
    bad_payload = {
        "exercises": [
            {
                "type": "multiple_choice",
                "options": ["a", "b"],
                "correct_answer": "a",
            }
        ]
    }
    with pytest.raises(ValueError):
        validate_llm_output(json.dumps(bad_payload))


def test_malformed_json_string_raises_value_error():
    """
    Pure non-JSON text or broken JSON (e.g. trailing comma, unterminated
    string) must raise ValueError, never silently return an empty model.
    """
    with pytest.raises(ValueError):
        validate_llm_output("not json at all")

    with pytest.raises(ValueError):
        validate_llm_output('{"exercises": [{"type": "x", ')


def test_empty_string_raises_value_error():
    """
    An empty string from a misbehaving LLM MUST be rejected, not
    silently treated as a valid empty model.
    """
    with pytest.raises(ValueError):
        validate_llm_output("")


def test_empty_exercises_list_is_valid_but_empty():
    """
    Edge case: a well-formed payload with an empty exercises list is
    valid. The validator returns a LLMResponse with an empty list,
    not an error. This is the contract the content script relies on
    when the LLM intentionally generates zero exercises.
    """
    response = validate_llm_output('{"exercises": []}')
    assert isinstance(response, LLMResponse)
    assert response.exercises == []


# ---------------------------------------------------------------------------
# Model-level invariants (belt-and-suspenders, exercised via the helper)
# ---------------------------------------------------------------------------


def test_exercise_item_options_must_be_list_when_provided():
    """
    If 'options' is provided it must be a list of strings, not a single
    scalar. The Pydantic schema enforces this.
    """
    bad = {
        "exercises": [
            {
                "type": "multiple_choice",
                "question": "Pick one.",
                "options": "not-a-list",  # wrong type
                "correct_answer": "a",
            }
        ]
    }
    with pytest.raises(ValueError):
        validate_llm_output(json.dumps(bad))
