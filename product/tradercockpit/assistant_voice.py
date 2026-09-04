"""Desktop microphone speech-to-text into the same Apollo message path.

Capture stays in the desktop webview. Transcription uses the operator OpenRouter
credential on the backend. The transcript is ordinary assistant message text;
mutations still require confirmation. Missing capture or STT is unavailable, not
a second assistant.
"""

from __future__ import annotations

import base64
import json
import os
from typing import Callable
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ASSISTANT_TRANSCRIBE_API_PATH = "/api/assistant/transcribe"
ASSISTANT_TRANSCRIPT_SCHEMA = "tc.assistant-transcript.v1"
ASSISTANT_VOICE_STATUS_SCHEMA = "tc.assistant-voice.v1"

OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY"
STT_MODEL_ENV = "TRADERCOCKPIT_STT_MODEL"
DEFAULT_STT_MODEL = "openai/whisper-1"
OPENROUTER_AUDIO_TRANSCRIPTIONS_URL = "https://openrouter.ai/api/v1/audio/transcriptions"

MAX_AUDIO_BYTES = 4_000_000
REQUEST_TIMEOUT_SECONDS = 45

ALLOWED_AUDIO_TYPES = {
    "audio/webm": "webm",
    "audio/wav": "wav",
    "audio/x-wav": "wav",
    "audio/wave": "wav",
    "audio/mpeg": "mp3",
    "audio/mp3": "mp3",
    "audio/ogg": "ogg",
    "audio/flac": "flac",
    "audio/mp4": "m4a",
    "audio/m4a": "m4a",
    "audio/x-m4a": "m4a",
}

Transport = Callable[[str, bytes, dict[str, str]], tuple[int, bytes]]


class AssistantVoiceError(ValueError):
    def __init__(self, code: str, detail: str, *, status: int = 400) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail
        self.status = status


def _environ(environ: dict[str, str] | None) -> dict[str, str]:
    return os.environ if environ is None else environ  # type: ignore[return-value]


def stt_model(environ: dict[str, str] | None = None) -> str:
    env = _environ(environ)
    return (env.get(STT_MODEL_ENV) or "").strip() or DEFAULT_STT_MODEL


def voice_status_record(environ: dict[str, str] | None = None) -> dict[str, object]:
    """Secret-free STT readiness. Capture permission is a desktop/webview concern."""

    env = _environ(environ)
    configured = bool((env.get(OPENROUTER_API_KEY_ENV) or "").strip())
    model = stt_model(env)
    return {
        "schema": ASSISTANT_VOICE_STATUS_SCHEMA,
        "status": "ready" if configured else "unavailable",
        "reason_code": None if configured else "provider_not_configured",
        "detail": (
            f"Desktop microphone transcribes through OpenRouter ({model}) into the same /api/assistant message path."
            if configured
            else f"Set {OPENROUTER_API_KEY_ENV} in the operator environment to enable speech-to-text."
        ),
        "capture": "desktop_microphone",
        "stt_provider": "openrouter",
        "stt_transport": "openrouter-audio-transcriptions",
        "stt_model": model,
        "native_mutation": False,
    }


def audio_format_for_content_type(content_type: str | None) -> str | None:
    if not isinstance(content_type, str) or not content_type.strip():
        return None
    media = content_type.split(";", 1)[0].strip().lower()
    return ALLOWED_AUDIO_TYPES.get(media)


def _urllib_transport(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
    request = Request(url, data=body, headers=headers, method="POST")
    try:
        with urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:  # noqa: S310 - fixed provider URL
            return int(response.status), response.read()
    except HTTPError as exc:
        return int(exc.code), exc.read()
    except URLError as exc:
        raise AssistantVoiceError("stt_provider_unreachable", f"OpenRouter STT is unreachable: {exc.reason}", status=502) from exc
    except TimeoutError as exc:
        raise AssistantVoiceError("stt_provider_timeout", "OpenRouter STT did not answer in time", status=504) from exc


def transcribe_audio(
    audio: bytes,
    *,
    content_type: str | None,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
) -> dict[str, object]:
    """Call the configured OpenRouter transcription model. Never mutates custody."""

    if not isinstance(audio, (bytes, bytearray)) or not audio:
        raise AssistantVoiceError("audio_invalid", "audio body must be non-empty")
    if len(audio) > MAX_AUDIO_BYTES:
        raise AssistantVoiceError("audio_too_large", f"audio exceeds {MAX_AUDIO_BYTES} bytes")
    fmt = audio_format_for_content_type(content_type)
    if fmt is None:
        raise AssistantVoiceError("audio_type_unsupported", "audio content type is not an allowed capture format")
    env = _environ(environ)
    key = (env.get(OPENROUTER_API_KEY_ENV) or "").strip()
    if not key:
        raise AssistantVoiceError(
            "provider_not_configured",
            f"Set {OPENROUTER_API_KEY_ENV} in the operator environment to enable speech-to-text.",
            status=503,
        )
    model = stt_model(env)
    payload = json.dumps({
        "model": model,
        "input_audio": {
            "data": base64.b64encode(bytes(audio)).decode("ascii"),
            "format": fmt,
        },
    }).encode("utf-8")
    send = transport or _urllib_transport
    headers = {
        "Authorization": f"Bearer {key}",
        "Content-Type": "application/json",
        "Accept": "application/json",
        "HTTP-Referer": "https://tradercockpit.local/",
        "X-Title": "TraderCockpit",
    }
    status, raw = send(OPENROUTER_AUDIO_TRANSCRIPTIONS_URL, payload, headers)
    try:
        body = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AssistantVoiceError("stt_provider_invalid", f"OpenRouter STT returned a non-JSON response ({status})", status=502) from exc
    if status >= 400 or not isinstance(body, dict):
        error = body.get("error") if isinstance(body, dict) else None
        detail = error.get("message") if isinstance(error, dict) and isinstance(error.get("message"), str) else f"OpenRouter STT request failed ({status})"
        code = "stt_provider_rejected" if status in {401, 402, 403} else "stt_provider_error"
        raise AssistantVoiceError(code, detail, status=502 if status >= 500 else 503)
    text = body.get("text")
    if not isinstance(text, str) or not text.strip():
        raise AssistantVoiceError("empty_transcript", "speech-to-text returned no text", status=502)
    usage = body.get("usage") if isinstance(body.get("usage"), dict) else {}
    return {
        "schema": ASSISTANT_TRANSCRIPT_SCHEMA,
        "transcript": text.strip(),
        "model": body.get("model") if isinstance(body.get("model"), str) else model,
        "requested_model": model,
        "format": fmt,
        "usage": {
            "prompt_tokens": usage.get("prompt_tokens"),
            "completion_tokens": usage.get("completion_tokens"),
            "total_tokens": usage.get("total_tokens"),
        },
        "native_mutation": False,
    }


def transcribe_response(
    audio: bytes,
    *,
    content_type: str | None,
    environ: dict[str, str] | None = None,
    transport: Transport | None = None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, transcribe_audio(audio, content_type=content_type, environ=environ, transport=transport)
    except AssistantVoiceError as exc:
        error = "invalid_request" if exc.status == 400 else "producer_not_configured" if exc.status == 503 else "provider_failed"
        return exc.status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
