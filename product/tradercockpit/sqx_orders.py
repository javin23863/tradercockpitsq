"""Strict readback of StrategyQuant X 144.2953 native ``orders.bin`` data.

The observed SQX 144.2953 producer writes ``orders.bin`` through ``ObjectOutputStream`` and
``OrdersList.serialize()`` using ``SQOrderFileFormat:11``.  Its SQ4/SQX loader opens
that same member through ``ObjectInputStream`` and calls ``OrdersList.deserialize()``.
This module implements only that exact observed producer contract.  It does not infer trades
from strategy XML, invent order meanings, or accept other SQX order formats.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import math
import struct
from zipfile import BadZipFile, ZipFile

from tradercockpit.sqx_presets import SQX_BUILD


SQX_ORDERS_MEMBER = "orders.bin"
SQX_ORDERS_FORMAT = "SQOrderFileFormat:11"
SQX_ORDERS_FORMAT_VERSION = 11
SQX_ORDERS_SCHEMA = "tc.sqx-orders.v1"
_OBJECT_STREAM_HEADER = b"\xac\xed\x00\x05"
_TC_BLOCKDATA = 0x77
_TC_BLOCKDATALONG = 0x7A
_EXPECTED_FORMAT11_LAYOUT = (0, 0, 0, 0, 0, 0, 1)


class SqxOrdersError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _sha256(value: bytes) -> str:
    return sha256(value).hexdigest()


def _member(archive: ZipFile, name: str) -> bytes:
    matches = [entry for entry in archive.infolist() if entry.filename == name]
    if len(matches) != 1:
        raise SqxOrdersError("sqx_orders_member_invalid", f"SQX archive must contain exactly one {name}")
    try:
        value = archive.read(matches[0])
    except (RuntimeError, NotImplementedError, EOFError, OSError) as exc:
        raise SqxOrdersError("sqx_orders_member_invalid", f"SQX archive member {name} is unreadable") from exc
    if not value:
        raise SqxOrdersError("sqx_orders_member_invalid", f"SQX archive member {name} is empty")
    return value


def _object_stream_primitive_data(value: bytes) -> bytes:
    """Return only legal primitive block data from one ObjectOutputStream.

    OrdersList.serialize writes primitives only.  Object/class/reset/end-block tokens
    therefore indicate a different Java serialization shape and are rejected.
    """

    if not isinstance(value, bytes) or not value.startswith(_OBJECT_STREAM_HEADER):
        raise SqxOrdersError("sqx_orders_stream_invalid", "orders.bin is not a Java ObjectOutputStream")
    cursor = len(_OBJECT_STREAM_HEADER)
    result = bytearray()
    while cursor < len(value):
        token = value[cursor]
        cursor += 1
        if token == _TC_BLOCKDATA:
            if cursor >= len(value):
                raise SqxOrdersError("sqx_orders_stream_invalid", "truncated ObjectOutputStream block length")
            size = value[cursor]
            cursor += 1
        elif token == _TC_BLOCKDATALONG:
            if cursor + 4 > len(value):
                raise SqxOrdersError("sqx_orders_stream_invalid", "truncated ObjectOutputStream long block length")
            size = struct.unpack_from(">I", value, cursor)[0]
            cursor += 4
        else:
            raise SqxOrdersError(
                "sqx_orders_stream_unsupported",
                f"orders.bin contains unsupported ObjectOutputStream token 0x{token:02x}",
            )
        end = cursor + size
        if end > len(value):
            raise SqxOrdersError("sqx_orders_stream_invalid", "truncated ObjectOutputStream block data")
        result.extend(value[cursor:end])
        cursor = end
    if not result:
        raise SqxOrdersError("sqx_orders_stream_invalid", "orders.bin contains no primitive block data")
    return bytes(result)


def _decode_modified_utf8(value: bytes) -> str:
    units: list[int] = []
    cursor = 0
    while cursor < len(value):
        first = value[cursor]
        cursor += 1
        if 0x01 <= first <= 0x7F:
            units.append(first)
            continue
        if first & 0xE0 == 0xC0:
            if cursor >= len(value):
                raise SqxOrdersError("sqx_orders_utf_invalid", "truncated modified UTF-8 sequence")
            second = value[cursor]
            cursor += 1
            if second & 0xC0 != 0x80:
                raise SqxOrdersError("sqx_orders_utf_invalid", "invalid modified UTF-8 continuation")
            unit = ((first & 0x1F) << 6) | (second & 0x3F)
            if unit != 0 and unit < 0x80:
                raise SqxOrdersError("sqx_orders_utf_invalid", "non-canonical modified UTF-8 sequence")
            units.append(unit)
            continue
        if first & 0xF0 == 0xE0:
            if cursor + 2 > len(value):
                raise SqxOrdersError("sqx_orders_utf_invalid", "truncated modified UTF-8 sequence")
            second, third = value[cursor], value[cursor + 1]
            cursor += 2
            if second & 0xC0 != 0x80 or third & 0xC0 != 0x80:
                raise SqxOrdersError("sqx_orders_utf_invalid", "invalid modified UTF-8 continuation")
            unit = ((first & 0x0F) << 12) | ((second & 0x3F) << 6) | (third & 0x3F)
            if unit < 0x800:
                raise SqxOrdersError("sqx_orders_utf_invalid", "non-canonical modified UTF-8 sequence")
            units.append(unit)
            continue
        raise SqxOrdersError("sqx_orders_utf_invalid", "invalid modified UTF-8 lead byte")

    chars: list[str] = []
    index = 0
    while index < len(units):
        unit = units[index]
        if 0xD800 <= unit <= 0xDBFF:
            if index + 1 >= len(units) or not 0xDC00 <= units[index + 1] <= 0xDFFF:
                raise SqxOrdersError("sqx_orders_utf_invalid", "unpaired modified UTF-8 surrogate")
            low = units[index + 1]
            chars.append(chr(0x10000 + ((unit - 0xD800) << 10) + (low - 0xDC00)))
            index += 2
            continue
        if 0xDC00 <= unit <= 0xDFFF:
            raise SqxOrdersError("sqx_orders_utf_invalid", "unpaired modified UTF-8 surrogate")
        chars.append(chr(unit))
        index += 1
    return "".join(chars)


class _PrimitiveReader:
    def __init__(self, value: bytes) -> None:
        self.value = value
        self.cursor = 0

    @property
    def remaining(self) -> int:
        return len(self.value) - self.cursor

    def _take(self, size: int) -> bytes:
        if size < 0 or self.cursor + size > len(self.value):
            raise SqxOrdersError("sqx_orders_truncated", "orders.bin ended inside a native order record")
        result = self.value[self.cursor:self.cursor + size]
        self.cursor += size
        return result

    def u8(self) -> int:
        return self._take(1)[0]

    def i8(self) -> int:
        return struct.unpack(">b", self._take(1))[0]

    def boolean(self) -> bool:
        value = self.u8()
        if value not in (0, 1):
            raise SqxOrdersError("sqx_orders_value_invalid", "native boolean is not encoded as 0 or 1")
        return value == 1

    def i16(self) -> int:
        return struct.unpack(">h", self._take(2))[0]

    def u16(self) -> int:
        return struct.unpack(">H", self._take(2))[0]

    def i32(self) -> int:
        return struct.unpack(">i", self._take(4))[0]

    def i64(self) -> int:
        return struct.unpack(">q", self._take(8))[0]

    def f32(self) -> float:
        value = struct.unpack(">f", self._take(4))[0]
        if not math.isfinite(value):
            raise SqxOrdersError("sqx_orders_value_invalid", "native order contains a non-finite float")
        return value

    def utf(self) -> str:
        size = self.u16()
        return _decode_modified_utf8(self._take(size))


def _read_cached_string(reader: _PrimitiveReader, cache: dict[int, str], *, byte_keys: bool) -> str | None:
    key = reader.u8() if byte_keys else reader.i32()
    if key == 0:
        return None
    try:
        return cache[key]
    except KeyError as exc:
        raise SqxOrdersError("sqx_orders_string_cache_invalid", "order references a missing native string-cache entry") from exc


def _duration_seconds(open_time: int, close_time: int) -> int:
    # Match OrdersList.getDuration exactly: Java signed-long subtraction, long
    # division truncated toward zero, upper clamp, then narrowing cast to int.
    delta = ((close_time - open_time + (1 << 63)) % (1 << 64)) - (1 << 63)
    seconds = delta // 1000 if delta >= 0 else -((-delta) // 1000)
    if seconds >= 2_147_483_647:
        return 2_147_483_647
    narrowed = seconds & 0xFFFF_FFFF
    return narrowed - (1 << 32) if narrowed >= (1 << 31) else narrowed


def _read_format11_order(reader: _PrimitiveReader, cache: dict[int, str], *, byte_keys: bool) -> dict[str, object]:
    order: dict[str, object] = {
        "Symbol": _read_cached_string(reader, cache, byte_keys=byte_keys),
        "SetupName": _read_cached_string(reader, cache, byte_keys=byte_keys),
        "StrategyName": _read_cached_string(reader, cache, byte_keys=byte_keys),
        "Comment": _read_cached_string(reader, cache, byte_keys=byte_keys),
        "Ticket": reader.i32(),
        "Order": reader.i32(),
        "Type": reader.i8(),
        "CloseType": reader.i8(),
        "SampleType": reader.i8(),
        "OriginalOpenTime": reader.i64(),
        "OriginalType": reader.i8(),
        "Size": reader.f32(),
        "OriginalPrice": reader.f32(),
        "OpenTime": reader.i64(),
        "OpenPrice": reader.f32(),
        "CloseTime": reader.i64(),
        "ClosePrice": reader.f32(),
        "StopLoss": reader.f32(),
        "TakeProfit": reader.f32(),
        "BarsInTrade": reader.i16(),
        "PL": reader.f32(),
        "PctPL": reader.f32(),
        "PctPL_TWR": reader.f32(),
        "PipsPL": reader.f32(),
        "DD": reader.f32(),
        "PctDD": reader.f32(),
        "PipsDD": reader.f32(),
        "CommSwap": reader.f32(),
        "CommSwapApplied": reader.boolean(),
        "MAE": reader.f32(),
        "PipsMAE": reader.f32(),
        "MFE": reader.f32(),
        "PipsMFE": reader.f32(),
    }
    reader.i32()  # serialized Duration is deliberately ignored by the native loader
    order["Duration"] = _duration_seconds(int(order["OpenTime"]), int(order["CloseTime"]))
    order.update(
        {
            "AccountBalance": reader.f32(),
            "PctAccountBalance": reader.f32(),
            "PipsAccountBalance": reader.f32(),
            "MagicNumber": reader.i32(),
            "IsInPortfolio": reader.i8(),
            "Extra1": reader.f32(),
            "SlippageInMoney": reader.f32(),
            "ExitIndex": reader.i8(),
            "ATROnOpen": reader.f32(),
        }
    )
    return order


def parse_orders_bin(value: bytes) -> dict[str, object]:
    primitive = _object_stream_primitive_data(value)
    reader = _PrimitiveReader(primitive)
    format_name = reader.utf()
    if format_name != SQX_ORDERS_FORMAT:
        raise SqxOrdersError(
            "sqx_orders_format_unsupported",
            f"expected {SQX_ORDERS_FORMAT}, observed {format_name!r}",
        )
    layout = tuple(reader.i32() for _ in range(7))
    if layout != _EXPECTED_FORMAT11_LAYOUT:
        raise SqxOrdersError("sqx_orders_layout_unsupported", "SQOrderFileFormat:11 layout flags do not match observed SQX 144.2953")

    cache_count = reader.i32()
    if cache_count < 0:
        raise SqxOrdersError("sqx_orders_string_cache_invalid", "native string-cache count is negative")
    byte_keys = cache_count < 120
    cache: dict[int, str] = {}
    if byte_keys:
        if cache_count > reader.remaining:
            raise SqxOrdersError("sqx_orders_string_cache_invalid", "native string-cache count exceeds available bytes")
        for index in range(cache_count):
            cache[index + 1] = reader.utf()
    else:
        if cache_count > reader.remaining // 6:
            raise SqxOrdersError("sqx_orders_string_cache_invalid", "native string-cache count exceeds available bytes")
        for _ in range(cache_count):
            key = reader.i32()
            if key == 0 or key in cache:
                raise SqxOrdersError("sqx_orders_string_cache_invalid", "native string-cache key is invalid or duplicated")
            cache[key] = reader.utf()

    orders: list[dict[str, object]] = []
    while reader.remaining:
        start = reader.cursor
        order = _read_format11_order(reader, cache, byte_keys=byte_keys)
        if reader.cursor <= start:
            raise SqxOrdersError("sqx_orders_stream_invalid", "native order parser made no progress")
        orders.append(order)

    trades = [
        order
        for order in orders
        if int(order["IsInPortfolio"]) != 0
        and int(order["Type"]) in {1, 2, 9, 11}
        and int(order["CloseType"]) != 18
    ]
    return {
        "schema": SQX_ORDERS_SCHEMA,
        "orders_format": SQX_ORDERS_FORMAT,
        "orders_format_version": SQX_ORDERS_FORMAT_VERSION,
        "orders_entry_sha256": _sha256(value),
        "native_order_count": len(orders),
        "trade_count": len(trades),
        "selection": {
            "result_key": "Portfolio",
            "direction": 0,
            "sample_type": 127,
            "filled_orders": True,
            "control_orders": False,
            "native_filter": "filterExcludingControlOrders",
        },
        "trades": trades,
    }


def inspect_sqx_orders_bytes(snapshot: bytes) -> dict[str, object]:
    """Read the exact producer-owned Portfolio trade list from one SQX archive."""

    if not isinstance(snapshot, bytes) or not snapshot:
        raise SqxOrdersError("sqx_orders_archive_invalid", "SQX result archive snapshot is empty")
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            version = _member(archive, "version.txt")
            try:
                build = version.decode("utf-8-sig").strip()
            except UnicodeDecodeError as exc:
                raise SqxOrdersError("sqx_orders_archive_invalid", "SQX version.txt is not UTF-8 text") from exc
            if build != SQX_BUILD:
                raise SqxOrdersError("sqx_orders_build_mismatch", f"expected SQX {SQX_BUILD}, observed {build!r}")
            orders = _member(archive, SQX_ORDERS_MEMBER)
    except BadZipFile as exc:
        raise SqxOrdersError("sqx_orders_archive_invalid", "historical result is not a valid SQX archive") from exc

    result = parse_orders_bin(orders)
    return {**result, "sqx_build": build, "orders_entry": SQX_ORDERS_MEMBER}
