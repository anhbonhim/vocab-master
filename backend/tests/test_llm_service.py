"""
Tests for the LLM Service (CONT-02, threat T-05-03).

These tests pin the contract of the Opencode API client used by the
content generation pipeline. Per D-02 and the plan's must_haves:

  - Backend trigger calls Opencode API using an asynchronous httpx client.
  - Do not use synchronous libraries like `requests` for the Opencode
    API call (use httpx.AsyncClient).
  - API timeout or failure when calling Opencode: httpx timeout triggers
    exception; script logs error and aborts exercise generation
    (threat T-05-03 mitigation).

Coverage:
- Async LLM call is made without blocking (AsyncClient, not Client).
- The Opencode endpoint is hit with the configured URL + auth header +
  model name.
- A 60-second timeout is configured on the client (threat T-05-03).
- The raw assistant text is returned as a string for downstream
  validate_llm_output() to consume.
- httpx.TimeoutException is caught and re-raised as a clean
  LLMServiceError so callers only catch one exception type.
- httpx.HTTPStatusError (non-2xx) is caught and re-raised the same way.
- A missing API key is detected before any HTTP call is issued.
"""
import json
from typing import Any, Dict, List, Optional

import httpx
import pytest

from app.services.llm_service import (
    LLMServiceError,
    generate_exercises_from_llm,
)


# ---------------------------------------------------------------------------
# Test fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(autouse=True)
def _default_opencode_api_key(monkeypatch):
    """Provide a non-empty OPENCODE_API_KEY for every test by default.

    The service refuses to make an HTTP call when the key is empty
    (T-05-03 mitigation + a good safety net for the content script).
    Tests that want to assert the empty-key branch clear this env var
    explicitly inside the test.
    """
    monkeypatch.setenv("OPENCODE_API_KEY", "test-api-key")
    # Drop the cached settings instance so the env re-read takes effect.
    # We must patch BOTH the canonical binding in app.config AND the
    # imported binding inside app.services.llm_service, because the
    # service did `from app.config import settings` at import time
    # (separate name in the service module's namespace).
    from app.config import Settings
    from app.services import llm_service as llm_service_module

    fresh_settings = Settings()
    monkeypatch.setattr("app.config.settings", fresh_settings)
    monkeypatch.setattr(llm_service_module, "settings", fresh_settings)
    yield


def _make_json_response(payload: Dict[str, Any]) -> httpx.Response:
    """Build an httpx.Response that looks like an Opencode chat-completions reply."""
    return httpx.Response(
        200,
        json={
            "choices": [
                {
                    "message": {
                        "content": json.dumps(payload),
                    }
                }
            ]
        },
    )


def _make_text_response(text: str) -> httpx.Response:
    """Build an httpx.Response whose .text matches the LLM raw text output."""
    return httpx.Response(
        200,
        json={
            "choices": [
                {
                    "message": {
                        "content": text,
                    }
                }
            ]
        },
    )


class _RecordingTransport(httpx.MockTransport):
    """MockTransport that records the last request it handled.

    We use a class (rather than a closure) so tests can introspect the
    captured URL, headers, method, and body to assert that the service
    is wired up correctly.
    """

    def __init__(self, handler):
        super().__init__(handler)
        self.requests: List[httpx.Request] = []

    def handle_request(self, request: httpx.Request) -> httpx.Response:
        self.requests.append(request)
        return super().handle_request(request)


# ---------------------------------------------------------------------------
# Happy path
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_returns_raw_text_for_validator(
    monkeypatch,
):
    """
    Successful HTTP 200 response: the service must extract the assistant
    message text and return it as a plain string. Downstream
    validate_llm_output() is what parses the JSON, so the service itself
    should NOT try to parse — it must return the raw text.
    """
    raw_text = '{"exercises": [{"type": "fill_blank", "question": "Sky is ___.", "correct_answer": "blue"}]}'

    transport = _RecordingTransport(lambda req: _make_text_response(raw_text))

    # Patch AsyncClient so the service uses our transport. We also patch
    # settings to give a known API key.
    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", lambda **kwargs: _StubAsyncClient(transport, kwargs))

    result = await generate_exercises_from_llm(["apple", "banana"])

    assert result == raw_text


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_uses_configured_url_and_model(
    monkeypatch,
):
    """
    The service must read URL + model + key from settings (CONT-02
    foundation) and pass them to the Opencode API. This test wires a
    custom Settings via env vars and asserts the request was made to
    the configured endpoint with the configured model in the body.
    """
    # Force a known URL + key + model + short timeout by overriding env
    # vars. pydantic-settings reads these on Settings() instantiation.
    monkeypatch.setenv("OPENCODE_API_URL", "http://test-opencode.example/v1/chat/completions")
    monkeypatch.setenv("OPENCODE_API_KEY", "secret-key-xyz")
    monkeypatch.setenv("OPENCODE_MODEL", "fake-model-id")
    monkeypatch.setenv("OPENCODE_TIMEOUT_SECONDS", "12.5")
    # Drop the cached settings instance so the env re-read takes effect.
    # We must patch BOTH the canonical binding in app.config AND the
    # imported binding inside app.services.llm_service (the service
    # did `from app.config import settings` at import time).
    from app.config import Settings
    from app.services import llm_service as llm_service_module

    fresh_settings = Settings()
    monkeypatch.setattr("app.config.settings", fresh_settings)
    monkeypatch.setattr(llm_service_module, "settings", fresh_settings)

    raw_text = "ok"
    transport = _RecordingTransport(lambda req: _make_text_response(raw_text))
    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", lambda **kwargs: _StubAsyncClient(transport, kwargs))

    await generate_exercises_from_llm(["word"])

    assert len(transport.requests) == 1
    req = transport.requests[0]
    assert req.method == "POST"
    assert str(req.url) == "http://test-opencode.example/v1/chat/completions"
    assert req.headers.get("Authorization") == "Bearer secret-key-xyz"
    body = json.loads(req.content.decode("utf-8"))
    assert body["model"] == "fake-model-id"
    # The vocabulary list must appear in the user message
    assert "word" in body["messages"][0]["content"]


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_uses_async_client(monkeypatch):
    """
    Plan must_haves: 'Do not use synchronous libraries like requests for
    the Opencode API call (use httpx.AsyncClient).' This test asserts the
    client passed into the call is httpx.AsyncClient (async), not the
    sync httpx.Client.
    """
    transport = _RecordingTransport(lambda req: _make_text_response("ok"))
    captured_kwargs: Dict[str, Any] = {}

    def _factory(**kwargs):
        captured_kwargs.update(kwargs)
        return _StubAsyncClient(transport, kwargs)

    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", _factory)

    await generate_exercises_from_llm(["x"])

    # Timeout MUST be present (T-05-03 mitigation)
    assert "timeout" in captured_kwargs, "AsyncClient must be created with an explicit timeout"
    # The timeout value must be a number > 0
    assert float(captured_kwargs["timeout"]) > 0


# ---------------------------------------------------------------------------
# Failure modes
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_raises_llm_service_error_on_timeout(
    monkeypatch,
):
    """
    Threat T-05-03: httpx timeout triggers an exception; the script
    logs the error and aborts exercise generation.

    The service must convert httpx.TimeoutException into the canonical
    LLMServiceError so callers only need to catch one type. They then
    can log + abort.
    """
    def _hang(request: httpx.Request) -> httpx.Response:
        raise httpx.TimeoutException("simulated timeout", request=request)

    transport = _RecordingTransport(_hang)
    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", lambda **kwargs: _StubAsyncClient(transport, kwargs))

    with pytest.raises(LLMServiceError) as excinfo:
        await generate_exercises_from_llm(["x"])
    assert "timeout" in str(excinfo.value).lower()


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_raises_llm_service_error_on_http_status_error(
    monkeypatch,
):
    """
    Non-2xx response (e.g. 401, 500) from Opencode must be caught and
    converted to LLMServiceError so the content script can log and
    abort without crashing the FastAPI worker.
    """
    def _server_error(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, text="upstream broken")

    transport = _RecordingTransport(_server_error)
    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", lambda **kwargs: _StubAsyncClient(transport, kwargs))

    with pytest.raises(LLMServiceError) as excinfo:
        await generate_exercises_from_llm(["x"])
    assert "500" in str(excinfo.value)


@pytest.mark.asyncio
async def test_generate_exercises_from_llm_raises_when_api_key_missing(monkeypatch):
    """
    With OPENCODE_API_KEY unset (empty string default), the service must
    refuse to issue a real HTTP call and raise LLMServiceError. This
    keeps callers from accidentally firing an unauthorized request
    against the Opencode API.
    """
    # Force a known empty key. pydantic-settings empty string default
    # would already satisfy this, but be explicit to make the test
    # resilient to a future 'no default' change. We also need to
    # refresh the cached settings instance in both app.config AND
    # app.services.llm_service (the service imported the settings
    # object by name at module load time).
    monkeypatch.setenv("OPENCODE_API_KEY", "")
    from app.config import Settings
    from app.services import llm_service as llm_service_module

    fresh_settings = Settings()
    monkeypatch.setattr("app.config.settings", fresh_settings)
    monkeypatch.setattr(llm_service_module, "settings", fresh_settings)

    # If the service actually tries to make the call, the transport
    # would record a request and the test would fail. We assert no
    # request happened.
    transport = _RecordingTransport(lambda req: _make_text_response("ok"))
    monkeypatch.setattr("app.services.llm_service.httpx.AsyncClient", lambda **kwargs: _StubAsyncClient(transport, kwargs))

    with pytest.raises(LLMServiceError) as excinfo:
        await generate_exercises_from_llm(["x"])
    assert "api key" in str(excinfo.value).lower() or "opencode" in str(excinfo.value).lower()
    assert transport.requests == [], "service must not issue HTTP when API key is empty"


# ---------------------------------------------------------------------------
# Helper: a minimal AsyncClient stand-in that only implements the
# surface the service uses (async with + .post(...)).
# ---------------------------------------------------------------------------


class _StubAsyncClient:
    """A minimal async-context-manager wrapper around httpx.MockTransport.

    httpx.MockTransport is normally consumed via httpx.AsyncClient in the
    service code; we re-implement the very small slice of that surface
    so the test can swap the client in via monkeypatch without
    touching the real httpx.AsyncClient.
    """

    def __init__(self, transport: httpx.MockTransport, ctor_kwargs: Optional[Dict[str, Any]] = None):
        self._transport = transport
        self._ctor_kwargs = ctor_kwargs or {}

    async def __aenter__(self) -> "_StubAsyncClient":
        return self

    async def __aexit__(self, exc_type, exc, tb) -> None:
        return None

    async def post(self, url: str, **kwargs) -> httpx.Response:
        request = httpx.Request("POST", url, **kwargs)
        response = self._transport.handle_request(request)
        # httpx.Response.raise_for_status() requires the originating
        # request to be set on the response — without it, it raises
        # "Cannot call `raise_for_status` as the request instance has
        # not been set on this response."
        response._request = request
        return response
