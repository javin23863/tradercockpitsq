"""Deterministic serialization and content-addressed identity.

The production domain deliberately refuses ambiguous JSON values instead of
silently stringifying them. In particular, binary floating-point values are
not accepted in identity-bearing payloads; callers must use exact integers,
Decimals, or strings according to the owning spec.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, is_dataclass
from decimal import Decimal
from enum import Enum
import hashlib
import json
import re
from typing import Any, Mapping


_KIND_RE = re.compile(r"^[a-z][a-z0-9._-]*$")
_VERSION_RE = re.compile(r"^[1-9][0-9]*$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class CanonicalizationError(ValueError):
    """Raised when a payload cannot be represented canonically."""


def _decimal_text(value: Decimal) -> str:
    if not value.is_finite():
        raise CanonicalizationError("non-finite Decimal values are not canonical")
    if value == 0:
        return "0"
    normalized = value.normalize()
    text = format(normalized, "f")
    if "." in text:
        text = text.rstrip("0").rstrip(".")
    return text


def _normalize(value: Any, path: str = "$") -> Any:
    if value is None or isinstance(value, (bool, int, str)):
        return value

    if isinstance(value, float):
        raise CanonicalizationError(
            f"{path}: float is not permitted in identity-bearing payloads"
        )

    if isinstance(value, Decimal):
        return {"$decimal": _decimal_text(value)}

    if isinstance(value, Enum):
        return _normalize(value.value, path)

    if is_dataclass(value) and not isinstance(value, type):
        return _normalize(asdict(value), path)

    if isinstance(value, Mapping):
        normalized: dict[str, Any] = {}
        for key, item in value.items():
            if not isinstance(key, str):
                raise CanonicalizationError(f"{path}: mapping keys must be strings")
            normalized[key] = _normalize(item, f"{path}.{key}")
        return normalized

    if isinstance(value, (list, tuple)):
        return [_normalize(item, f"{path}[{index}]") for index, item in enumerate(value)]

    raise CanonicalizationError(
        f"{path}: unsupported canonical type {type(value).__name__}"
    )


def canonical_json_bytes(payload: Any) -> bytes:
    """Return the unique UTF-8 JSON representation used for identity."""

    normalized = _normalize(payload)
    return json.dumps(
        normalized,
        ensure_ascii=False,
        allow_nan=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def canonical_sha256(payload: Any) -> str:
    return hashlib.sha256(canonical_json_bytes(payload)).hexdigest()


@dataclass(frozen=True, slots=True)
class ContentAddress:
    """Schema-scoped content address for an immutable production payload."""

    kind: str
    version: int
    sha256: str

    def __post_init__(self) -> None:
        if not _KIND_RE.fullmatch(self.kind):
            raise ValueError(f"invalid content-address kind: {self.kind!r}")
        if self.version < 1:
            raise ValueError("content-address version must be >= 1")
        if not _DIGEST_RE.fullmatch(self.sha256):
            raise ValueError("content-address sha256 must be 64 lowercase hex chars")

    def __str__(self) -> str:
        return f"tc:{self.kind}:v{self.version}:sha256:{self.sha256}"

    @classmethod
    def parse(cls, value: str) -> "ContentAddress":
        parts = value.split(":")
        if len(parts) != 5 or parts[0] != "tc" or parts[3] != "sha256":
            raise ValueError("invalid TraderCockpit content address")
        kind = parts[1]
        version_token = parts[2]
        if not version_token.startswith("v") or not _VERSION_RE.fullmatch(version_token[1:]):
            raise ValueError("invalid TraderCockpit content-address version")
        return cls(kind=kind, version=int(version_token[1:]), sha256=parts[4])

    def verify(self, payload: Any) -> bool:
        return self == content_address(self.kind, self.version, payload)


def content_address(kind: str, version: int, payload: Any) -> ContentAddress:
    """Hash payload with its schema kind/version to prevent cross-schema aliasing."""

    if not _KIND_RE.fullmatch(kind):
        raise ValueError(f"invalid content-address kind: {kind!r}")
    if version < 1:
        raise ValueError("content-address version must be >= 1")

    envelope = {
        "kind": kind,
        "version": version,
        "payload": payload,
    }
    return ContentAddress(kind=kind, version=version, sha256=canonical_sha256(envelope))
