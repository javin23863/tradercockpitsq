"""Read Custom Project engine stats from the SQX WebSocket engine-channel.

SQX Electron loads counts from WebSocket ``engine-channel`` payloads
(``strategies``, ``strategiesRejected``, ``strategiesAccepted``,
``strategiesPerHour``). HTTP servlets here only expose logs, not live counts.
"""

from __future__ import annotations

import base64
import json
import os
import socket
import struct
import threading
import time
from typing import Any

SQX_ENGINE_CHANNELS = frozenset({"engine", "engine-channel"})
SQX_WS_SETUP_APPS = {
    "Builder": "BUILDER",
    "Retester": "RETESTER",
    "Optimizer": "OPTIMIZER",
}
SQX_WS_SETUP_APP_DEFAULT = "TASKMANAGER"
SQX_WS_PATH = "/websocket/updates"
SQX_WS_POLL_TIMEOUT_SECONDS = 0.8
SQX_ENGINE_CACHE_TTL_SECONDS = 2.0

_cache: dict[str, dict[str, object]] = {}
_last_poll: dict[str, float] = {}
_state_lock = threading.Lock()


def _optional_count(value: object) -> int | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float) and value.is_integer():
        return int(value)
    if isinstance(value, str):
        stripped = value.strip()
        if stripped.isdigit() or (stripped.startswith("-") and stripped[1:].isdigit()):
            return int(stripped)
    return None


def setup_app_for_project(project: str) -> str:
    return SQX_WS_SETUP_APPS.get(project, SQX_WS_SETUP_APP_DEFAULT)


def engine_progress_values(payload: dict[str, object] | None) -> dict[str, int | str | None]:
    """Map SQX engine-channel field names to cockpit progress stats."""

    empty: dict[str, int | str | None] = {
        "generated": None,
        "rejected": None,
        "accepted": None,
        "rate": None,
        "percent": None,
        "running_status": None,
    }
    if not isinstance(payload, dict):
        return empty
    status = payload.get("runningStatus")
    return {
        "generated": _optional_count(payload.get("strategies")),
        "rejected": _optional_count(payload.get("strategiesRejected")),
        "accepted": _optional_count(payload.get("strategiesAccepted")),
        "rate": _optional_count(payload.get("strategiesPerHour")),
        "percent": _optional_count(payload.get("progressPercent")),
        "running_status": status.strip() if isinstance(status, str) and status.strip() else None,
    }


def cached_engine_progress(project: str) -> dict[str, int | None]:
    with _state_lock:
        return engine_progress_values(_cache.get(project))


def _engine_payload(message: dict[str, object], project: str) -> dict[str, object] | None:
    project_data = message.get("projectData")
    if not isinstance(project_data, dict):
        return None
    if project_data.get("name") != project:
        return None
    channels = project_data.get("channels")
    if not isinstance(channels, list):
        return None
    for channel in channels:
        if not isinstance(channel, dict) or channel.get("name") not in SQX_ENGINE_CHANNELS:
            continue
        data = channel.get("data")
        if isinstance(data, dict):
            return data
    return None


def _recv_exact(sock: socket.socket, size: int) -> bytes:
    chunks: list[bytes] = []
    remaining = size
    while remaining > 0:
        part = sock.recv(remaining)
        if not part:
            raise OSError("SQX websocket closed before a full frame arrived")
        chunks.append(part)
        remaining -= len(part)
    return b"".join(chunks)


def _read_ws_text(sock: socket.socket) -> str | None:
    header = _recv_exact(sock, 2)
    fin = header[0] & 0x80
    opcode = header[0] & 0x0F
    masked = header[1] & 0x80
    length = header[1] & 0x7F
    if length == 126:
        length = struct.unpack("!H", _recv_exact(sock, 2))[0]
    elif length == 127:
        length = struct.unpack("!Q", _recv_exact(sock, 8))[0]
    if masked:
        raise OSError("SQX websocket server frames must not be masked")
    payload = _recv_exact(sock, length) if length else b""
    if opcode == 0x8:
        return None
    if opcode == 0x9:
        sock.send(b"\x8a\x00")
        return _read_ws_text(sock)
    if opcode == 0x1 or (opcode == 0x0 and fin):
        return payload.decode("utf-8")
    if opcode == 0x0:
        return payload.decode("utf-8")
    return None


def _send_ws_text(sock: socket.socket, text: str) -> None:
    payload = text.encode("utf-8")
    length = len(payload)
    header = bytearray([0x81])
    mask_key = os.urandom(4)
    if length < 126:
        header.append(0x80 | length)
    elif length < 65536:
        header.append(0x80 | 126)
        header.extend(struct.pack("!H", length))
    else:
        header.append(0x80 | 127)
        header.extend(struct.pack("!Q", length))
    header.extend(mask_key)
    masked = bytes(b ^ mask_key[i % 4] for i, b in enumerate(payload))
    sock.send(header + masked)


def _poll_engine_channel(
    sqx_home: object,
    project: str,
    *,
    timeout: float = SQX_WS_POLL_TIMEOUT_SECONDS,
) -> dict[str, object] | None:
    from .sqx_native_web import SqxNativeWebError, sqx_local_json

    port_payload = sqx_local_json(sqx_home, "/main/getWebSocketPort")
    port = _optional_count(port_payload.get("port"))
    if port is None:
        raise SqxNativeWebError(
            "sqx_web_invalid_response",
            "StrategyQuant X did not publish a WebSocket port.",
        )
    sock = socket.create_connection(("127.0.0.1", port), timeout=timeout)
    sock.settimeout(timeout)
    try:
        key = base64.b64encode(os.urandom(16)).decode("ascii")
        request = (
            f"GET {SQX_WS_PATH} HTTP/1.1\r\n"
            f"Host: 127.0.0.1:{port}\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Key: {key}\r\n"
            "Sec-WebSocket-Version: 13\r\n"
            "\r\n"
        ).encode("ascii")
        sock.sendall(request)
        response = b""
        while b"\r\n\r\n" not in response:
            chunk = sock.recv(4096)
            if not chunk:
                raise OSError("SQX websocket handshake failed")
            response += chunk
        if b" 101 " not in response.split(b"\r\n", 1)[0]:
            raise SqxNativeWebError(
                "sqx_web_unavailable",
                "StrategyQuant X WebSocket handshake was refused.",
            )
        _send_ws_text(sock, json.dumps({"action": "setup", "app": setup_app_for_project(project)}, separators=(",", ":")))
        for channel in ("engine", "engine-channel"):
            _send_ws_text(
                sock,
                json.dumps(
                    {"action": "subscribe", "projectName": project, "channel": channel},
                    separators=(",", ":"),
                ),
            )
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                raw = _read_ws_text(sock)
            except TimeoutError:
                break
            except OSError:
                break
            if raw is None:
                break
            if not raw.strip() or raw.strip() == "{}":
                continue
            try:
                message = json.loads(raw)
            except json.JSONDecodeError:
                continue
            if not isinstance(message, dict):
                continue
            payload = _engine_payload(message, project)
            if payload is not None:
                return payload
        return None
    finally:
        sock.close()


def read_engine_progress(sqx_home: object, project: str) -> dict[str, int | None]:
    """Return cached or freshly polled SQX engine stats; None fields when unavailable."""

    from .sqx_native_web import SqxNativeWebError

    now = time.monotonic()
    should_poll = False
    with _state_lock:
        last = _last_poll.get(project, 0.0)
        if now - last >= SQX_ENGINE_CACHE_TTL_SECONDS:
            should_poll = True
            _last_poll[project] = now
    if should_poll:
        try:
            payload = _poll_engine_channel(sqx_home, project)
        except SqxNativeWebError:
            payload = None
        if payload is not None:
            with _state_lock:
                _cache[project] = payload
    return cached_engine_progress(project)


def reset_engine_progress_cache_for_tests() -> None:
    with _state_lock:
        _cache.clear()
        _last_poll.clear()
