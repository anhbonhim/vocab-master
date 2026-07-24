"""
Pydantic V2 schemas for the LLM-driven exercise pipeline (CONT-03, T-05-02).

This module is the single chokepoint between AI-generated content
(Opencode API today, potentially other providers later) and the SQLite
``Lesson.exercises_data`` JSON column. Per threat T-05-02 and the plan's
``must_haves.truths``:

    "Do not save raw LLM responses directly to the database without
     Pydantic validation."

Every consumer of an LLM response MUST route the raw string through
:func:`validate_llm_output`. ``ValueError`` is the single error type
callers should catch to trigger a retry / fallback.
"""
from __future__ import annotations

import re
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, ValidationError


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------


class ExerciseItem(BaseModel):
    """One exercise item as returned by the LLM.

    ``type`` is intentionally a free-form string so the schema stays
    forward-compatible with new exercise shapes (per D-01). Validators
    for individual types live in the consumer (Android client) and
    future per-type helpers; this module only guarantees the shared
    structural contract.
    """

    # Tolerate extra fields the LLM may invent (e.g. an ``explanation``
    # or ``difficulty`` field). We do not reject on unknown keys so
    # new exercise shapes can flow through without a schema bump.
    model_config = ConfigDict(extra="ignore")

    type: str
    question: str
    correct_answer: str
    # Only meaningful for multiple_choice / listening; null for fill_blank
    # and sentence_arrangement.
    options: Optional[List[str]] = None


class LLMResponse(BaseModel):
    """Top-level wrapper: the LLM returns an object with an ``exercises`` list."""

    model_config = ConfigDict(extra="ignore")

    exercises: List[ExerciseItem]


# ---------------------------------------------------------------------------
# Markdown stripping helpers
# ---------------------------------------------------------------------------


# Matches a single fenced code block at the start AND end of the string.
# Captures the inner payload, regardless of whether the fence is labeled
# ```json, ```JSON, or just ```.
_FENCED_CODE_BLOCK = re.compile(
    r"^\s*```(?:json|JSON)?\s*\n?(.*?)\n?\s*```\s*$",
    re.DOTALL,
)


def _strip_markdown_code_blocks(text: str) -> str:
    """Strip a leading/trailing ```` ```json ... ``` ```` wrapper if present.

    LLMs commonly wrap JSON in markdown code fences. Pydantic V2's
    ``model_validate_json`` does not tolerate the fence characters, so
    we strip them here BEFORE handing the payload to Pydantic.

    Args:
        text: The raw response body from the LLM.

    Returns:
        The cleaned JSON string, free of leading/trailing fences and
        surrounding whitespace. If no fence is present, the input is
        returned trimmed.
    """
    if not text:
        return text

    trimmed = text.strip()
    match = _FENCED_CODE_BLOCK.match(trimmed)
    if match:
        return match.group(1).strip()
    return trimmed


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------


def validate_llm_output(json_str: str) -> LLMResponse:
    """Validate a raw LLM response and return a strict :class:`LLMResponse`.

    This is the ONLY sanctioned way to turn an LLM response into
    persisted exercise data. The contract:

    1. Empty / whitespace-only input -> ``ValueError``.
    2. ```` ```json ... ``` ```` wrapper is stripped before parsing.
    3. Malformed JSON -> ``ValueError`` (the underlying Pydantic
       ``ValueError`` is re-raised so the message is preserved).
    4. Valid JSON that fails Pydantic schema validation (missing
       required fields, wrong types) -> ``ValueError`` wrapping the
       ``ValidationError`` for diagnostics.

    Args:
        json_str: The raw response body from the LLM. May be wrapped
            in markdown code fences.

    Returns:
        A fully validated :class:`LLMResponse`.

    Raises:
        ValueError: If the input is empty, malformed, or does not match
            the expected schema.
    """
    if not json_str or not json_str.strip():
        raise ValueError("LLM response is empty")

    cleaned = _strip_markdown_code_blocks(json_str)

    try:
        return LLMResponse.model_validate_json(cleaned)
    except ValidationError as exc:
        # Convert Pydantic's ValidationError into ValueError so callers
        # only have to catch one exception type.
        raise ValueError(
            f"LLM output failed Pydantic validation: {exc}"
        ) from exc
    except ValueError as exc:
        # model_validate_json raises ValueError for malformed JSON.
        # Re-raise as-is to preserve the original message but make the
        # failure observable to the caller.
        raise ValueError(f"LLM output is not valid JSON: {exc}") from exc
