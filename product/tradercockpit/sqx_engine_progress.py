"""Read Custom Project engine stats and Progress charts from SQX WebSockets.

SQX Electron loads counts from ``engine`` / ``engine-channel``
(``strategies``, ``strategiesRejected``, ``strategiesAccepted``,
``strategiesPerHour``) and on-page Progress charts from ``engineCharts``
(``charts[].data.chart`` Chart.js configs). HTTP servlets here only expose
logs, not live counts. Fitness Evolution stays a native popup; this module
does not port it.
"""

from __future__ import annotations

import base64
import json
import math
import os
import re
import socket
import struct
import threading
import time

from .sqx_presets import SQX_BUILD

SQX_ENGINE_CHANNELS = frozenset({"engine", "engine-channel"})
SQX_ENGINE_CHART_CHANNEL = "engineCharts"
SQX_ENGINE_CHART_DATA_TYPE = "chart"
SQX_ENGINE_CHART_ROW_TYPES = frozenset({"grid", "rows"})
SQX_ENGINE_CHART_SLOT_COUNT = 2
SQX_ENGINE_CHART_MAX_POINTS = 256
SQX_ENGINE_CHART_MAX_ITEMS = 32
SQX_ENGINE_GET_TYPES_PATH = "/engine/getTypes"
SQX_ENGINE_SAVE_SELECTION_PATH = "/engine/saveSelection"
SQX_ENGINE_CHART_TYPES_SCHEMA = "tc.sqx-engine-chart-types.v1"
SQX_ENGINE_CHART_SELECTION_SCHEMA = "tc.sqx-engine-chart-selection.v1"
SQX_ENGINE_CHART_SELECTION_API_PATH = "/api/sqx-engine-chart-selection"
_ENGINE_TYPE_ID = re.compile(r"^[A-Za-z][A-Za-z0-9.]*$")
# Official EngineCtrl.js paints two slots; these are the 144.2953 snippet defaults
# shown on Progress (engine/getTypes settings fall back to types[0] when unset).
SQX_ENGINE_CHART_PREFERRED_TYPES = (
    "AverageStrategiesPerHourChart",
    "HeapMemoryChart",
)
# Constructor L.tsq / L.t strings from internal/extend/Snippets/SQ/EngineCharts.
SQX_ENGINE_CHART_TITLES = {
    "AcceptedStrategiesPerHour": "Accepted strategies per hour",
    "AverageStrategiesPerHourChart": "Average strategies per hour",
    "DatabankFitnessIS": "Databank Fitness - IS",
    "DatabankFitnessIST": "Databank Fitness - IS Training",
    "DatabankFitnessISV": "Databank Fitness - IS Validation",
    "DatabankFitnessOOS": "Databank Fitness - OOS",
    "GenEvoFitnessIS": "Island #1 - Fitness IS",
    "GenEvoFitnessIST": "Island #1 - Fitness IS Training",
    "GenEvoFitnessISV": "Island #1 - Fitness IS Validation",
    "GenEvoFitnessOOS": "Island #1 - Fitness Out of Sample",
    "GeneticEvolutionInfo": "Genetic Evolution info",
    "HeapMemoryChart": "Heap memory chart",
    "OffHeapMemoryChart": "Off-heap memory chart",
    "OffHeapMemoryInfoChart": "Off-heap memory info",
}
SQX_WS_SETUP_APPS = {
    "Builder": "BUILDER",
    "Retester": "RETESTER",
    "Optimizer": "OPTIMIZER",
}
SQX_WS_SETUP_APP_DEFAULT = "TASKMANAGER"
SQX_WS_PATH = "/websocket/updates"
SQX_WS_POLL_TIMEOUT_SECONDS = 0.8
SQX_ENGINE_CACHE_TTL_SECONDS = 2.0
# Official SQConstants.runningStatuses keys from 144.2953.
SQX_CUSTOM_PROJECT_STATUS_NAMES = {
    0: "beforeStart",
    1: "running",
    2: "paused",
    3: "finished",
    4: "stopped",
    5: "pausing",
    6: "stopping",
    50: "error",
    100: "loading",
}
SQX_CUSTOM_PROJECT_ACTIVE_STATUSES = frozenset(
    {"running", "paused", "pausing", "stopping", "loading"}
)

_cache: dict[str, dict[str, object]] = {}
_charts_raw: dict[str, dict[str, object]] = {}
_types_cache: dict[str, dict[str, object]] = {}
_last_poll: dict[str, float] = {}
_stats_cache: dict[str, object] = {"at": 0.0, "rows": {}}
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


def custom_project_stat_fields(row: object) -> dict[str, object] | None:
    """Map one official customProjectStats row. Unknown status codes stay unused."""

    if not isinstance(row, dict):
        return None
    name = row.get("projectName")
    if not isinstance(name, str) or not name.strip():
        return None
    raw = row.get("runningStatus")
    status: str | None = None
    if type(raw) is int:
        status = SQX_CUSTOM_PROJECT_STATUS_NAMES.get(raw)
    elif isinstance(raw, str) and raw.strip().isdigit():
        status = SQX_CUSTOM_PROJECT_STATUS_NAMES.get(int(raw.strip()))
    elif isinstance(raw, str) and raw.strip() in SQX_CUSTOM_PROJECT_STATUS_NAMES.values():
        status = raw.strip()
    if status is None:
        return None
    fields: dict[str, object] = {
        "project": name.strip(),
        "running_status": status,
        "running": status in SQX_CUSTOM_PROJECT_ACTIVE_STATUSES,
    }
    percent = _optional_count(row.get("progressPercent"))
    if percent is not None and 0 <= percent <= 100:
        fields["percent"] = percent
    return fields


def _custom_project_stats_rows(payload: object) -> dict[str, dict[str, object]]:
    rows = payload.get("customProjectStats") if isinstance(payload, dict) else None
    if not isinstance(rows, list):
        return {}
    mapped: dict[str, dict[str, object]] = {}
    for row in rows:
        fields = custom_project_stat_fields(row)
        if fields is None:
            continue
        mapped[str(fields["project"])] = fields
    return mapped


def _named_payloads(
    message: dict[str, object],
    project: str,
    names: frozenset[str],
) -> dict[str, dict[str, object]]:
    project_data = message.get("projectData")
    if not isinstance(project_data, dict) or project_data.get("name") != project:
        return {}
    channels = project_data.get("channels")
    if not isinstance(channels, list):
        return {}
    found: dict[str, dict[str, object]] = {}
    for channel in channels:
        if not isinstance(channel, dict):
            continue
        name = channel.get("name")
        data = channel.get("data")
        if name in names and name not in found and isinstance(data, dict):
            found[str(name)] = data
    return found


def _engine_payload(message: dict[str, object], project: str) -> dict[str, object] | None:
    named = _named_payloads(message, project, SQX_ENGINE_CHANNELS)
    return named.get("engine") or named.get("engine-channel")


def _finite_number(value: object) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    number = float(value)
    if not math.isfinite(number):
        return None
    return number


def _point_y(point: object) -> float | None:
    number = _finite_number(point)
    if number is not None:
        return number
    if isinstance(point, dict):
        for key in ("y", "Y"):
            number = _finite_number(point.get(key))
            if number is not None:
                return number
    return None


def _chart_type_key(value: object) -> str:
    if not isinstance(value, str):
        return ""
    name = value.strip()
    if not name or "/" in name or "\\" in name or "\0" in name:
        return ""
    return name.rsplit(".", 1)[-1]


def _chartjs_series(config: object) -> list[dict[str, object]]:
    if not isinstance(config, dict):
        return []
    data = config.get("data")
    if not isinstance(data, dict):
        return []
    datasets = data.get("datasets")
    if not isinstance(datasets, list):
        return []
    series: list[dict[str, object]] = []
    for dataset in datasets[:8]:
        if not isinstance(dataset, dict):
            continue
        raw = dataset.get("data")
        if not isinstance(raw, list):
            continue
        values = [y for y in (_point_y(point) for point in raw) if y is not None]
        if len(values) < 2:
            continue
        if len(values) > SQX_ENGINE_CHART_MAX_POINTS:
            values = values[-SQX_ENGINE_CHART_MAX_POINTS:]
        label = dataset.get("label")
        item: dict[str, object] = {"values": values}
        if isinstance(label, str) and label.strip():
            item["label"] = label.strip()
        series.append(item)
    return series


def _engine_type_id(value: object) -> str:
    if not isinstance(value, str):
        return ""
    name = value.strip()
    if not name or len(name) > 128 or not _ENGINE_TYPE_ID.fullmatch(name):
        return ""
    return name


def _chart_items(raw: object) -> list[dict[str, str]]:
    if not isinstance(raw, list):
        return []
    items: list[dict[str, str]] = []
    for item in raw[:SQX_ENGINE_CHART_MAX_ITEMS]:
        if not isinstance(item, dict):
            continue
        name = item.get("name")
        value = item.get("value")
        if not isinstance(name, str) or not name.strip():
            continue
        if isinstance(value, bool):
            text = "true" if value else "false"
        elif isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(float(value)):
            text = str(value)
        elif isinstance(value, str):
            text = value
        else:
            continue
        items.append({"name": name.strip(), "value": text})
    return items


def _chart_title(type_id: str, type_names: dict[str, str] | None) -> str:
    key = _chart_type_key(type_id)
    if type_names:
        for candidate in (type_id, key):
            name = type_names.get(candidate)
            if name:
                return name
    return SQX_ENGINE_CHART_TITLES.get(key) or type_id


def _frame_for_type(
    item: dict[str, object] | None,
    type_id: str,
    type_names: dict[str, str] | None,
) -> dict[str, object]:
    frame: dict[str, object] = {
        "type": type_id,
        "title": _chart_title(type_id, type_names),
        "series": [],
    }
    if not isinstance(item, dict):
        return frame
    data = item.get("data")
    if not isinstance(data, dict):
        return frame
    kind = data.get("type")
    if kind == SQX_ENGINE_CHART_DATA_TYPE:
        frame["series"] = _chartjs_series(data.get("chart"))
        return frame
    if kind in SQX_ENGINE_CHART_ROW_TYPES:
        rows = _chart_items(data.get("items"))
        if rows:
            frame["kind"] = str(kind)
            frame["items"] = rows
    return frame


def engine_chart_frames(
    payload: dict[str, object] | None,
    settings: list[str] | None = None,
    type_names: dict[str, str] | None = None,
) -> list[dict[str, object]]:
    """Map official engineCharts.charts[] to the two Progress slots."""

    by_key: dict[str, dict[str, object]] = {}
    if isinstance(payload, dict) and isinstance(payload.get("charts"), list):
        for item in payload["charts"]:
            if not isinstance(item, dict):
                continue
            key = _chart_type_key(item.get("type"))
            if key and key not in by_key:
                by_key[key] = item
    selected: list[str] = []
    if isinstance(settings, list):
        for raw in settings:
            type_id = _engine_type_id(raw) or (_chart_type_key(raw) if isinstance(raw, str) else "")
            if type_id:
                selected.append(type_id)
            if len(selected) >= SQX_ENGINE_CHART_SLOT_COUNT:
                break
    if selected:
        return [
            _frame_for_type(by_key.get(_chart_type_key(type_id)), type_id, type_names)
            for type_id in selected[:SQX_ENGINE_CHART_SLOT_COUNT]
        ]
    extracted: list[dict[str, object]] = []
    for item in by_key.values():
        type_id = _engine_type_id(item.get("type")) or _chart_type_key(item.get("type"))
        if not type_id:
            continue
        frame = _frame_for_type(item, type_id, type_names)
        if frame.get("series") or frame.get("items"):
            extracted.append(frame)
    preferred = [frame for frame in extracted if _chart_type_key(frame["type"]) in SQX_ENGINE_CHART_PREFERRED_TYPES]
    preferred.sort(key=lambda frame: SQX_ENGINE_CHART_PREFERRED_TYPES.index(_chart_type_key(str(frame["type"]))))
    rest = [frame for frame in extracted if frame not in preferred]
    return (preferred + rest)[:SQX_ENGINE_CHART_SLOT_COUNT]


def _parse_engine_chart_types(values: object) -> list[dict[str, str]]:
    from .sqx_native_web import SqxNativeWebError

    if not isinstance(values, list) or not values:
        raise SqxNativeWebError(
            "engine_chart_types_invalid",
            "StrategyQuant X engine/getTypes omitted types.",
        )
    types: list[dict[str, str]] = []
    seen: set[str] = set()
    for item in values:
        if not isinstance(item, dict):
            raise SqxNativeWebError(
                "engine_chart_types_invalid",
                "StrategyQuant X engine chart type is not an object.",
            )
        type_id = _engine_type_id(item.get("type"))
        name = item.get("name")
        if not type_id:
            raise SqxNativeWebError(
                "engine_chart_types_invalid",
                "StrategyQuant X engine chart type omitted type.",
            )
        if not isinstance(name, str) or not name.strip() or "\0" in name or len(name) > 256:
            raise SqxNativeWebError(
                "engine_chart_types_invalid",
                "StrategyQuant X engine chart type omitted name.",
            )
        if type_id in seen:
            continue
        seen.add(type_id)
        types.append({"type": type_id, "name": name.strip()})
    if not types:
        raise SqxNativeWebError(
            "engine_chart_types_invalid",
            "StrategyQuant X engine/getTypes omitted types.",
        )
    return types


def _parse_engine_chart_settings(values: object, types: list[dict[str, str]]) -> list[str]:
    allowed = {item["type"]: item["type"] for item in types}
    allowed.update({_chart_type_key(item["type"]): item["type"] for item in types})
    selected: list[str] = []
    if isinstance(values, list):
        for raw in values:
            type_id = _engine_type_id(raw)
            matched = allowed.get(type_id) or allowed.get(_chart_type_key(type_id))
            if matched:
                selected.append(matched)
            if len(selected) >= SQX_ENGINE_CHART_SLOT_COUNT:
                break
    fallback = types[0]["type"]
    while len(selected) < SQX_ENGINE_CHART_SLOT_COUNT:
        selected.append(fallback)
    return selected


def list_engine_chart_types(
    sqx_home: object,
    project: str,
    *,
    opener=None,
) -> dict[str, object]:
    from .sqx_native_web import sqx_local_json

    kwargs: dict[str, object] = {
        "method": "GET",
        "timeout": 5.0,
        "fields": {"projectName": project},
    }
    if opener is not None:
        kwargs["opener"] = opener
    producer = sqx_local_json(sqx_home, SQX_ENGINE_GET_TYPES_PATH, **kwargs)
    types = _parse_engine_chart_types(producer.get("types"))
    return {
        "schema": SQX_ENGINE_CHART_TYPES_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "types": types,
        "settings": _parse_engine_chart_settings(producer.get("settings"), types),
        "detail": "Official StrategyQuant X engine/getTypes chart slots.",
    }


def save_engine_chart_selection(
    sqx_home: object,
    project: str,
    number: object,
    type_id: object,
    *,
    opener=None,
) -> dict[str, object]:
    from .sqx_native_web import SqxNativeWebError, sqx_local_json

    if number not in (0, 1):
        raise SqxNativeWebError(
            "engine_chart_selection_invalid",
            "Chart slot number must be 0 or 1.",
        )
    catalog = list_engine_chart_types(sqx_home, project, opener=opener)
    allowed = {item["type"]: item["type"] for item in catalog["types"]}
    allowed.update({_chart_type_key(item["type"]): item["type"] for item in catalog["types"]})
    exact = _engine_type_id(type_id)
    matched = allowed.get(exact) or allowed.get(_chart_type_key(exact))
    if not matched:
        raise SqxNativeWebError(
            "engine_chart_selection_invalid",
            "Chart type is not in the official engine/getTypes list.",
        )
    kwargs: dict[str, object] = {
        "method": "GET",
        "timeout": 5.0,
        "fields": {"projectName": project, "number": str(number), "type": matched},
    }
    if opener is not None:
        kwargs["opener"] = opener
    sqx_local_json(sqx_home, SQX_ENGINE_SAVE_SELECTION_PATH, **kwargs)
    with _state_lock:
        _types_cache.pop(project, None)
        _charts_raw.pop(project, None)
        _last_poll.pop(project, None)
    return {
        "schema": SQX_ENGINE_CHART_SELECTION_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "number": number,
        "type": matched,
        "detail": "Requested StrategyQuant X engine/saveSelection.",
    }


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
) -> tuple[dict[str, object] | None, dict[str, object] | None]:
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
        subscribe = ("engine", "engine-channel", SQX_ENGINE_CHART_CHANNEL)
        for channel in subscribe:
            _send_ws_text(
                sock,
                json.dumps(
                    {"action": "subscribe", "projectName": project, "channel": channel},
                    separators=(",", ":"),
                ),
            )
        deadline = time.monotonic() + timeout
        stats: dict[str, object] | None = None
        charts: dict[str, object] | None = None
        wanted = SQX_ENGINE_CHANNELS | {SQX_ENGINE_CHART_CHANNEL}
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
            named = _named_payloads(message, project, wanted)
            if stats is None:
                stats = named.get("engine") or named.get("engine-channel")
            if charts is None and SQX_ENGINE_CHART_CHANNEL in named:
                charts = named[SQX_ENGINE_CHART_CHANNEL]
            if stats is not None and charts is not None:
                break
        return stats, charts
    finally:
        sock.close()


def _cached_chart_catalog(sqx_home: object, project: str) -> dict[str, object] | None:
    from .sqx_custom_project import SqxCustomProjectTopologyError
    from .sqx_native_web import SqxNativeWebError
    from .sqx_presets import SqxPresetRuntimeError

    now = time.monotonic()
    with _state_lock:
        cached = _types_cache.get(project)
        if isinstance(cached, dict) and now - float(cached.get("at", 0.0)) < SQX_ENGINE_CACHE_TTL_SECONDS:
            record = cached.get("record")
            return record if isinstance(record, dict) else None
    try:
        record = list_engine_chart_types(sqx_home, project)
    except (SqxNativeWebError, SqxCustomProjectTopologyError, SqxPresetRuntimeError):
        record = None
    with _state_lock:
        _types_cache[project] = {"at": now, "record": record}
    return record


def _poll_custom_project_stats(
    sqx_home: object,
    *,
    timeout: float = SQX_WS_POLL_TIMEOUT_SECONDS,
) -> dict[str, dict[str, object]]:
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
        _send_ws_text(
            sock,
            json.dumps({"action": "setup", "app": SQX_WS_SETUP_APP_DEFAULT}, separators=(",", ":")),
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
            rows = _custom_project_stats_rows(message)
            if rows:
                return rows
        return {}
    finally:
        sock.close()


def read_custom_project_stats(sqx_home: object) -> dict[str, dict[str, object]]:
    """Official TASKMANAGER customProjectStats. Idle rows do not open a per-project socket."""

    from .sqx_native_web import SqxNativeWebError
    from .sqx_custom_project import SqxCustomProjectTopologyError
    from .sqx_presets import SqxPresetRuntimeError

    now = time.monotonic()
    with _state_lock:
        cached_at = float(_stats_cache.get("at") or 0.0)
        cached_rows = _stats_cache.get("rows")
        if now - cached_at < SQX_ENGINE_CACHE_TTL_SECONDS and isinstance(cached_rows, dict):
            return dict(cached_rows)
    try:
        rows = _poll_custom_project_stats(sqx_home)
    except (SqxNativeWebError, SqxCustomProjectTopologyError, SqxPresetRuntimeError, OSError):
        rows = {}
    with _state_lock:
        _stats_cache["at"] = now
        _stats_cache["rows"] = rows
    return dict(rows)


def read_engine_progress(sqx_home: object, project: str) -> dict[str, object]:
    """Return cached or freshly polled SQX engine stats and official chart frames."""

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
            stats, charts = _poll_engine_channel(sqx_home, project)
        except SqxNativeWebError:
            stats, charts = None, None
        with _state_lock:
            if stats is not None:
                _cache[project] = stats
            if charts is not None:
                _charts_raw[project] = charts
    catalog = _cached_chart_catalog(sqx_home, project)
    settings = catalog.get("settings") if isinstance(catalog, dict) else None
    names = None
    if isinstance(catalog, dict) and isinstance(catalog.get("types"), list):
        names = {
            str(item["type"]): str(item["name"])
            for item in catalog["types"]
            if isinstance(item, dict) and item.get("type") and item.get("name")
        }
    with _state_lock:
        raw = _charts_raw.get(project)
    frames = engine_chart_frames(
        raw,
        settings=settings if isinstance(settings, list) else None,
        type_names=names,
    )
    values: dict[str, object] = dict(cached_engine_progress(project))
    if isinstance(catalog, dict):
        values["chart_types"] = catalog["types"]
        values["chart_settings"] = catalog["settings"]
    if frames:
        values["charts"] = frames
    return values


def reset_engine_progress_cache_for_tests() -> None:
    with _state_lock:
        _cache.clear()
        _charts_raw.clear()
        _types_cache.clear()
        _last_poll.clear()
        _stats_cache["at"] = 0.0
        _stats_cache["rows"] = {}
