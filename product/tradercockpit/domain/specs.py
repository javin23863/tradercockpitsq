"""Immutable, TraderCockpit-owned execution specification envelopes.

These contracts deliberately define identity and custody before freezing the
full strategy-rule vocabulary. ``semantic_schema`` identifies the exact rule
language that owns a StrategySpec payload; merely constructing a StrategySpec
does not mean an engine supports that semantic schema.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
import re
from types import MappingProxyType
from typing import Any, ClassVar, Mapping

from .canonical import CanonicalizationError, ContentAddress, canonical_json_bytes, content_address


_TOKEN_RE = re.compile(r"^[a-z][a-z0-9._-]*$")
_SCHEMA_RE = re.compile(r"^[a-z][a-z0-9._-]*\.v[1-9][0-9]*$")
_CURRENCY_RE = re.compile(r"^[A-Z][A-Z0-9]{2,7}$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class SpecValidationError(ValueError):
    """Raised when an immutable production specification is invalid."""


def _require_text(value: str, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise SpecValidationError(f"{name} must be a non-empty string")
    if value != value.strip():
        raise SpecValidationError(f"{name} must not contain surrounding whitespace")
    return value


def _require_token(value: str, name: str) -> str:
    value = _require_text(value, name)
    if not _TOKEN_RE.fullmatch(value):
        raise SpecValidationError(f"{name} must match {_TOKEN_RE.pattern}")
    return value


def _require_schema(value: str, name: str) -> str:
    value = _require_text(value, name)
    if not _SCHEMA_RE.fullmatch(value):
        raise SpecValidationError(f"{name} must be an explicit versioned schema id")
    return value


def _require_ref(value: ContentAddress, kind: str, name: str) -> ContentAddress:
    if not isinstance(value, ContentAddress):
        raise SpecValidationError(f"{name} must be a ContentAddress")
    if value.kind != kind:
        raise SpecValidationError(f"{name} must reference {kind!r}, got {value.kind!r}")
    return value


def _freeze_json(value: Any, path: str = "$") -> Any:
    """Deep-freeze canonical JSON-domain values without changing exact decimals."""

    if value is None or isinstance(value, (bool, int, str, Decimal)):
        canonical_json_bytes(value)
        return value
    if isinstance(value, float):
        raise SpecValidationError(f"{path}: float is not permitted")
    if isinstance(value, Mapping):
        frozen: dict[str, Any] = {}
        for key, item in value.items():
            if not isinstance(key, str):
                raise SpecValidationError(f"{path}: mapping keys must be strings")
            frozen[key] = _freeze_json(item, f"{path}.{key}")
        canonical_json_bytes(frozen)
        return MappingProxyType(frozen)
    if isinstance(value, (list, tuple)):
        frozen_items = tuple(
            _freeze_json(item, f"{path}[{idx}]") for idx, item in enumerate(value)
        )
        canonical_json_bytes(frozen_items)
        return frozen_items
    try:
        canonical_json_bytes(value)
    except CanonicalizationError as exc:
        raise SpecValidationError(str(exc)) from exc
    raise SpecValidationError(f"{path}: unsupported value type {type(value).__name__}")


def _freeze_object(
    value: Mapping[str, Any], name: str, *, allow_empty: bool = False
) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise SpecValidationError(f"{name} must be a mapping")
    if not allow_empty and not value:
        raise SpecValidationError(f"{name} must not be empty")
    frozen = _freeze_json(value, f"$.{name}")
    assert isinstance(frozen, Mapping)
    return frozen


def _utc_timestamp(value: str, name: str) -> str:
    value = _require_text(value, name)
    candidate = value[:-1] + "+00:00" if value.endswith("Z") else value
    try:
        parsed = datetime.fromisoformat(candidate)
    except ValueError as exc:
        raise SpecValidationError(f"{name} must be an ISO-8601 timestamp") from exc
    if parsed.tzinfo is None or parsed.utcoffset() is None:
        raise SpecValidationError(f"{name} must include an explicit timezone offset")
    parsed = parsed.astimezone(timezone.utc)
    return parsed.isoformat(timespec="microseconds").replace("+00:00", "Z")


class _AddressedSpec:
    KIND: ClassVar[str]
    VERSION: ClassVar[int] = 1

    def identity_payload(self) -> Mapping[str, Any]:
        raise NotImplementedError

    @property
    def ref(self) -> ContentAddress:
        return content_address(self.KIND, self.VERSION, self.identity_payload())


@dataclass(frozen=True, slots=True)
class StrategySpecV1(_AddressedSpec):
    """Immutable strategy meaning under an explicitly versioned semantic schema.

    Engine support is separate: an engine must explicitly declare and validate
    ``semantic_schema`` before this specification is executable.
    """

    KIND: ClassVar[str] = "strategy"

    semantic_schema: str
    semantics: Mapping[str, Any]

    def __post_init__(self) -> None:
        object.__setattr__(
            self, "semantic_schema", _require_schema(self.semantic_schema, "semantic_schema")
        )
        object.__setattr__(self, "semantics", _freeze_object(self.semantics, "semantics"))

    def identity_payload(self) -> Mapping[str, Any]:
        return {"semantic_schema": self.semantic_schema, "semantics": self.semantics}


@dataclass(frozen=True, slots=True)
class CandidateSpecV1(_AddressedSpec):
    """One fully resolved strategy occurrence plus immutable lineage."""

    KIND: ClassVar[str] = "candidate"

    strategy_ref: ContentAddress
    origin: str
    parent_strategy_ref: ContentAddress | None = None
    origin_ref: ContentAddress | None = None

    def __post_init__(self) -> None:
        _require_ref(self.strategy_ref, "strategy", "strategy_ref")
        object.__setattr__(self, "origin", _require_token(self.origin, "origin"))
        if self.parent_strategy_ref is not None:
            _require_ref(self.parent_strategy_ref, "strategy", "parent_strategy_ref")
        if self.origin_ref is not None and not isinstance(self.origin_ref, ContentAddress):
            raise SpecValidationError("origin_ref must be a ContentAddress when supplied")

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "strategy_ref": str(self.strategy_ref),
            "origin": self.origin,
            "parent_strategy_ref": (
                None if self.parent_strategy_ref is None else str(self.parent_strategy_ref)
            ),
            "origin_ref": None if self.origin_ref is None else str(self.origin_ref),
        }


@dataclass(frozen=True, slots=True)
class DataSpecV1(_AddressedSpec):
    """Exact data window and custody assumptions for one evaluation."""

    KIND: ClassVar[str] = "data"

    symbol: str
    timeframe: str
    source: str
    dataset_revision: str
    timezone_name: str
    session_calendar: str
    start: str
    end: str
    adjustment_policy: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "symbol", _require_text(self.symbol, "symbol"))
        object.__setattr__(self, "timeframe", _require_text(self.timeframe, "timeframe"))
        object.__setattr__(self, "source", _require_token(self.source, "source"))
        object.__setattr__(
            self, "dataset_revision", _require_text(self.dataset_revision, "dataset_revision")
        )
        object.__setattr__(
            self, "timezone_name", _require_text(self.timezone_name, "timezone_name")
        )
        object.__setattr__(
            self, "session_calendar", _require_text(self.session_calendar, "session_calendar")
        )
        object.__setattr__(self, "start", _utc_timestamp(self.start, "start"))
        object.__setattr__(self, "end", _utc_timestamp(self.end, "end"))
        start_dt = datetime.fromisoformat(self.start[:-1] + "+00:00")
        end_dt = datetime.fromisoformat(self.end[:-1] + "+00:00")
        if start_dt >= end_dt:
            raise SpecValidationError("start must be before end")
        object.__setattr__(
            self,
            "adjustment_policy",
            _require_token(self.adjustment_policy, "adjustment_policy"),
        )

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "symbol": self.symbol,
            "timeframe": self.timeframe,
            "source": self.source,
            "dataset_revision": self.dataset_revision,
            "timezone_name": self.timezone_name,
            "session_calendar": self.session_calendar,
            "start": self.start,
            "end": self.end,
            "adjustment_policy": self.adjustment_policy,
        }


@dataclass(frozen=True, slots=True)
class ExecutionModelV1:
    """One explicitly named execution assumption model and its parameters."""

    kind: str
    model: str
    parameters: Mapping[str, Any]

    def __post_init__(self) -> None:
        object.__setattr__(self, "kind", _require_token(self.kind, "execution model kind"))
        object.__setattr__(self, "model", _require_token(self.model, "execution model"))
        object.__setattr__(
            self,
            "parameters",
            _freeze_object(self.parameters, "parameters", allow_empty=True),
        )

    def identity_payload(self) -> Mapping[str, Any]:
        return {"kind": self.kind, "model": self.model, "parameters": self.parameters}


@dataclass(frozen=True, slots=True)
class ExecutionSpecV1(_AddressedSpec):
    """Exact capital/account assumptions and named execution models."""

    KIND: ClassVar[str] = "execution"

    starting_cash: Decimal
    currency: str
    models: tuple[ExecutionModelV1, ...]

    def __post_init__(self) -> None:
        if (
            not isinstance(self.starting_cash, Decimal)
            or not self.starting_cash.is_finite()
            or self.starting_cash <= 0
        ):
            raise SpecValidationError("starting_cash must be a finite positive Decimal")
        if not isinstance(self.currency, str) or not _CURRENCY_RE.fullmatch(self.currency):
            raise SpecValidationError(
                "currency must be an uppercase currency/account unit token"
            )
        if not isinstance(self.models, tuple):
            object.__setattr__(self, "models", tuple(self.models))
        if not self.models:
            raise SpecValidationError("models must contain at least one execution assumption")
        seen: set[str] = set()
        for model in self.models:
            if not isinstance(model, ExecutionModelV1):
                raise SpecValidationError("models must contain only ExecutionModelV1 values")
            if model.kind in seen:
                raise SpecValidationError(f"duplicate execution model kind: {model.kind}")
            seen.add(model.kind)

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "starting_cash": self.starting_cash,
            "currency": self.currency,
            "models": tuple(model.identity_payload() for model in self.models),
        }


@dataclass(frozen=True, slots=True)
class EngineBuildSpecV1(_AddressedSpec):
    """Identity of the exact production engine build used for evaluation."""

    KIND: ClassVar[str] = "engine-build"

    implementation: str
    revision: str
    artifact_sha256: str

    def __post_init__(self) -> None:
        object.__setattr__(
            self, "implementation", _require_token(self.implementation, "implementation")
        )
        object.__setattr__(self, "revision", _require_text(self.revision, "revision"))
        if not isinstance(self.artifact_sha256, str) or not _DIGEST_RE.fullmatch(
            self.artifact_sha256
        ):
            raise SpecValidationError("artifact_sha256 must be 64 lowercase hex chars")

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "implementation": self.implementation,
            "revision": self.revision,
            "artifact_sha256": self.artifact_sha256,
        }


@dataclass(frozen=True, slots=True)
class BacktestRunSpecV1(_AddressedSpec):
    """Reproducible binding of candidate, data, execution, and engine build."""

    KIND: ClassVar[str] = "backtest-run"

    candidate_ref: ContentAddress
    data_ref: ContentAddress
    execution_ref: ContentAddress
    engine_build_ref: ContentAddress
    random_seed: int | None = None

    def __post_init__(self) -> None:
        _require_ref(self.candidate_ref, "candidate", "candidate_ref")
        _require_ref(self.data_ref, "data", "data_ref")
        _require_ref(self.execution_ref, "execution", "execution_ref")
        _require_ref(self.engine_build_ref, "engine-build", "engine_build_ref")
        if self.random_seed is not None and (
            not isinstance(self.random_seed, int)
            or isinstance(self.random_seed, bool)
            or self.random_seed < 0
        ):
            raise SpecValidationError("random_seed must be a non-negative integer or None")

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "candidate_ref": str(self.candidate_ref),
            "data_ref": str(self.data_ref),
            "execution_ref": str(self.execution_ref),
            "engine_build_ref": str(self.engine_build_ref),
            "random_seed": self.random_seed,
        }
