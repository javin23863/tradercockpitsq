"""Read producer-stored SQX databank columns from a result ``settings.xml``.

StrategyQuant X writes ``SQStats`` blobs under ``ResultsGroup``. TraderCockpit does not
recompute walk-forward or confidence-level Monte Carlo from ``orders.bin``; it reads the
values SQX already stored. Numbered default-key slots are the SQX 144.2953
``StatsKeyCache`` indexes observed in the authorized ``SQTradingLib.jar``.
"""

from __future__ import annotations

import base64
import re
import struct
from xml.etree import ElementTree


# SQX 144.2953 StatsKeyCache StatInfo.index within type-3 (float) default keys.
_DOUBLE_INDEX_NAMES = {
    10: "NetProfit",
    21: "DrawdownPct",
    67: "WFMaxDDbyRun",
    68: "WFMaxPctDDbyRun",
    69: "WFMaxProfitByRun",
    70: "WFMaxProfitByRunInPct",
    71: "WFMaxStagnationInPct",
    72: "WFMinTradesInRun",
    73: "WFPctOfProfitableRuns",
    77: "WFScore",
}

_STATS_SAMPLE_RE = re.compile(
    r"direction(?:_DD_)?(\d+).*?pl(?:_DD_)?(\d+).*?sample(?:_DD_)?(\d+)",
    re.IGNORECASE | re.DOTALL,
)
_CL_KEY_RE = re.compile(r"(?:^|[_-])CL[_-]?(\d{1,3})(?:$|[_-])", re.IGNORECASE)


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _read_java_utf(data: bytes, offset: int) -> tuple[str, int]:
    if offset + 2 > len(data):
        raise ValueError("truncated UTF length")
    size = struct.unpack_from(">H", data, offset)[0]
    start = offset + 2
    end = start + size
    if end > len(data):
        raise ValueError("truncated UTF payload")
    return data[start:end].decode("utf-8"), end


def decode_sqstats(payload: bytes, *, version: int = 2) -> dict[str, float]:
    """Decode one optimized-format SQStats blob to named numeric columns."""

    columns: dict[str, float] = {}
    offset = 0
    use_float = version > 1
    while offset < len(payload):
        kind = payload[offset]
        offset += 1
        if kind == 0:
            break
        if kind in {1, 2, 3}:
            if offset >= len(payload):
                raise ValueError("truncated SQStats field id")
            field_id = payload[offset]
            offset += 1
            if kind == 1:
                width, fmt = 4, ">i"
            elif kind == 2:
                width, fmt = 8, ">q"
            elif use_float:
                width, fmt = 4, ">f"
            else:
                width, fmt = 8, ">d"
            if offset + width > len(payload):
                raise ValueError("truncated SQStats numeric payload")
            value = float(struct.unpack_from(fmt, payload, offset)[0])
            offset += width
            name = _DOUBLE_INDEX_NAMES.get(field_id) if kind == 3 else None
            if name:
                columns[name] = value
            continue
        if kind not in {101, 102, 103}:
            raise ValueError(f"unknown SQStats record type {kind}")
        name, offset = _read_java_utf(payload, offset)
        if kind == 101:
            width, fmt = 4, ">i"
        elif kind == 102:
            width, fmt = 8, ">q"
        elif use_float:
            width, fmt = 4, ">f"
        else:
            width, fmt = 8, ">d"
        if offset + width > len(payload):
            raise ValueError("truncated SQStats named payload")
        columns[name] = float(struct.unpack_from(fmt, payload, offset)[0])
        offset += width
    return columns


def _stats_coords(node: ElementTree.Element) -> tuple[int, int, int] | None:
    sample = node.attrib.get("sample") or node.attrib.get("sampleType")
    direction = node.attrib.get("direction")
    pl = node.attrib.get("pl")
    if sample is not None:
        try:
            return int(direction or 0), int(pl or 0), int(sample)
        except ValueError:
            return None
    match = _STATS_SAMPLE_RE.search(_local_name(node.tag))
    if match is None:
        return None
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def _confidence_level(result_key: str, node: ElementTree.Element) -> int:
    raw = node.attrib.get("confidenceLevel") or node.attrib.get("confidence_level")
    if raw:
        try:
            return int(raw)
        except ValueError:
            pass
    match = _CL_KEY_RE.search(result_key)
    if match is not None:
        return int(match.group(1))
    return 50


def _sqstats_nodes(parent: ElementTree.Element) -> list[ElementTree.Element]:
    return [child for child in parent.iter() if _local_name(child.tag) == "SQStats"]


def _columns_from_sqstats(node: ElementTree.Element) -> dict[str, float]:
    text = (node.text or "").strip()
    if not text:
        return {}
    try:
        payload = base64.b64decode(text, validate=False)
    except (ValueError, TypeError):
        return {}
    version_raw = node.attrib.get("version") or "2"
    try:
        version = int(version_raw)
    except ValueError:
        version = 2
    try:
        return decode_sqstats(payload, version=version)
    except (struct.error, ValueError, UnicodeDecodeError):
        return {}


def parse_sqx_databank(settings_xml: bytes | None) -> list[dict[str, object]]:
    """Return producer databank rows: result_key, sample, direction, confidence_level, columns."""

    if not settings_xml:
        return []
    try:
        root = ElementTree.fromstring(settings_xml)
    except (ElementTree.ParseError, LookupError, ValueError):
        return []
    rows: list[dict[str, object]] = []
    for result in root.iter():
        if _local_name(result.tag) != "Result":
            continue
        result_key = str(result.attrib.get("resultKey") or result.attrib.get("key") or "").strip()
        if not result_key:
            key_child = next((child for child in result if _local_name(child.tag) == "resultKey"), None)
            result_key = ((key_child.text or "").strip() if key_child is not None else "")
        for stats in result.iter():
            coords = _stats_coords(stats)
            if coords is None:
                continue
            direction, _pl, sample = coords
            columns: dict[str, float] = {}
            for blob in _sqstats_nodes(stats):
                columns.update(_columns_from_sqstats(blob))
            if not columns:
                continue
            rows.append({
                "result_key": result_key,
                "sample": sample,
                "direction": direction,
                "confidence_level": _confidence_level(result_key, stats),
                "columns": columns,
            })
    return rows


def lookup_databank_column(
    rows: list[dict[str, object]] | None,
    column: str,
    *,
    sample_type: int,
    direction: int = 0,
    confidence_level: int = 50,
) -> float | None:
    """Pick one producer column value for an acceptance-condition coordinate."""

    if not rows or not column:
        return None
    matched = [
        row for row in rows
        if row.get("sample") == sample_type
        and int(row.get("direction") or 0) == direction
        and isinstance(row.get("columns"), dict)
        and column in row["columns"]  # type: ignore[operator]
    ]
    if not matched:
        return None

    def value(row: dict[str, object]) -> float:
        return float(row["columns"][column])  # type: ignore[index]

    exact = [
        row for row in matched
        if int(row.get("confidence_level") or 50) == confidence_level
    ]
    if not exact:
        return None
    want_cross = confidence_level != 50
    preferred = [
        row for row in exact
        if str(row.get("result_key") or "").startswith("CrossCheck_") is want_cross
    ]
    return value((preferred or exact)[0])
