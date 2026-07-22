"""
LLM service for the content generation pipeline (CONT-02, threat T-05-03).

Per D-02, the backend talks to the Opencode go API directly. This module
is the only sanctioned caller — every other consumer (content_gen.py,
admin scripts) must go through :func:`generate_exercises_from_llm`.

Key invariants (from plan 05-03 must_haves):

* The HTTP call MUST be made via ``httpx.AsyncClient``. Synchronous
  libraries like ``requests`` (or ``httpx.Client``) are forbidden because
  they would block the FastAPI event loop while waiting for the LLM.
* A timeout MUST be configured on the client. Without it, a stuck
  LLM provider would hang the worker indefinitely (threat T-05-03).
* ``httpx.TimeoutException`` and ``httpx.HTTPStatusError`` are caught
  and re-raised as :class:`LLMServiceError` so callers can catch one
  exception type, log it, and abort generation without crashing.
* The function returns the raw assistant message text — NOT a parsed
  JSON object. Parsing is the responsibility of
  :func:`app.schemas.llm.validate_llm_output` (single chokepoint,
  T-05-02 enforcement).

Note on testability: this module references ``httpx.AsyncClient`` as an
attribute lookup (``httpx.AsyncClient(...)``) rather than a top-level
import alias. This lets the test suite swap the client via
``monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", ...)``
without monkey-patching the global ``httpx`` module.
"""
from __future__ import annotations

import json
from typing import List

import httpx

from app.config import settings


# ---------------------------------------------------------------------------
# Public error type
# ---------------------------------------------------------------------------


class LLMServiceError(Exception):
    """Raised when the Opencode API call cannot be completed.

    Every LLM-service failure (missing API key, timeout, non-2xx
    response, malformed response envelope) is funneled into this single
    exception type. Content scripts only need to catch ``LLMServiceError``
    to log and abort the exercise generation step.
    """


# ---------------------------------------------------------------------------
# Prompt construction
# ---------------------------------------------------------------------------


# The prompt asks the LLM to return a strict JSON object matching the
# Pydantic LLMResponse shape, and explicitly forbids markdown code fences
# (the validator strips them defensively, but a clean prompt avoids the
# round-trip).
_PROMPT_TEMPLATE = (
    "You are a vocabulary teacher for an English-Vietnamese learning app.\n"
    "Generate exercises for the following vocabulary list:\n{vocab}\n\n"
    "Return ONLY a JSON object with this exact structure (no markdown, no "
    "code fences, no extra prose):\n"
    '{{"exercises": [{{"type": "fill_blank|multiple_choice|listening", '
    '"question": "...", "options": ["..."], "correct_answer": "..."}}]}}\n'
    "Rules:\n"
    "- Each exercise has a 'type', 'question', and 'correct_answer'.\n"
    "- 'options' is a list of strings for multiple_choice/listening; "
    "omit it for fill_blank.\n"
    "- Output must be raw JSON parseable by Python json.loads."
)


def _build_prompt(vocabulary_list: List[str]) -> str:
    """Compose the user prompt sent to the Opencode chat-completions API."""
    return _PROMPT_TEMPLATE.format(vocab=", ".join(vocabulary_list))


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------


async def generate_exercises_from_llm(vocabulary_list: List[str]) -> str:
    """Call the Opencode go API and return the raw assistant text.

    Args:
        vocabulary_list: The words the LLM should produce exercises for.
            Order is preserved in the prompt so callers can pair LLM
            output back to the source list.

    Returns:
        The raw text content of the LLM's ``choices[0].message.content``.
        This is NOT yet validated JSON — the caller is expected to pass
        the result through :func:`app.schemas.llm.validate_llm_output`
        before persisting.

    Raises:
        LLMServiceError: If the API key is empty, the request times out,
            the server returns a non-2xx status, or the response envelope
            does not contain the expected ``choices[0].message.content``
            field.
    """
    api_key = settings.OPENCODE_API_KEY
    if not api_key:
        raise LLMServiceError(
            "OPENCODE_API_KEY is empty; refusing to call the Opencode API. "
            "Set the env var (or backend/.env) to a real key before running "
            "the content generation script."
        )

    url = settings.OPENCODE_API_URL
    model = settings.OPENCODE_MODEL
    timeout = settings.OPENCODE_TIMEOUT_SECONDS

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": model,
        "messages": [
            {"role": "user", "content": _build_prompt(vocabulary_list)},
        ],
    }

    # Threat T-05-03: 60s timeout (configurable via OPENCODE_TIMEOUT_SECONDS)
    # — prevents a stuck LLM provider from hanging the worker forever.
    # Per plan must_haves: AsyncClient (not sync) so we don't block the
    # FastAPI event loop.
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
    except httpx.TimeoutException as exc:
        raise LLMServiceError(
            f"Opencode API call timed out after {timeout}s: {exc}"
        ) from exc
    except httpx.HTTPStatusError as exc:
        raise LLMServiceError(
            f"Opencode API returned HTTP {exc.response.status_code}: "
            f"{exc.response.text[:200] if exc.response.text else '<no body>'}"
        ) from exc

    # The Opencode go API mirrors the OpenAI chat-completions envelope,
    # but we defensively unpack the message text so a missing/renamed
    # field surfaces as a clean LLMServiceError instead of a KeyError.
    try:
        data = response.json()
        return data["choices"][0]["message"]["content"]
    except (json.JSONDecodeError, KeyError, IndexError, TypeError) as exc:
        raise LLMServiceError(
            f"Opencode API response envelope is malformed: {exc}"
        ) from exc
