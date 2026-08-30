"""Canonical wire codec for immutable execution-input objects."""

from __future__ import annotations

from decimal import Decimal
from typing import Any, Callable, Mapping

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    ContentAddress,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    StrategySpecV1,
    canonical_json_bytes,
    canonical_json_loads,
    content_address,
)


_WIRE_SCHEMA = "tc.addressed-object.v1"
_SUPPORTED_KINDS = frozenset(
    {"strategy", "candidate", "data", "execution", "engine-build", "backtest-run"}
)


class WireFormatError(ValueError):
    """Raised when persisted object bytes do not satisfy the production wire contract."""


def _object(value: Any, name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise WireFormatError(f"{name} must be an object")
    return value


def _exact_keys(value: Mapping[str, Any], expected: set[str], name: str) -> None:
    actual = set(value)
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        raise WireFormatError(
            f"{name} fields mismatch; missing={missing}, extra={extra}"
        )


def _text(value: Any, name: str) -> str:
    if not isinstance(value, str):
        raise WireFormatError(f"{name} must be a string")
    return value


def _integer(value: Any, name: str) -> int:
    if not isinstance(value, int) or isinstance(value, bool):
        raise WireFormatError(f"{name} must be an integer")
    return value


def _ref(value: Any, name: str) -> ContentAddress:
    try:
        return ContentAddress.parse(_text(value, name))
    except ValueError as exc:
        raise WireFormatError(f"{name} is not a valid content address") from exc


def _nullable_ref(value: Any, name: str) -> ContentAddress | None:
    return None if value is None else _ref(value, name)


def _decode_strategy(payload: Mapping[str, Any]) -> StrategySpecV1:
    _exact_keys(payload, {"semantic_schema", "semantics"}, "strategy payload")
    return StrategySpecV1(
        _text(payload["semantic_schema"], "semantic_schema"),
        _object(payload["semantics"], "semantics"),
    )


def _decode_candidate(payload: Mapping[str, Any]) -> CandidateSpecV1:
    _exact_keys(
        payload,
        {"strategy_ref", "origin", "parent_strategy_ref", "origin_ref"},
        "candidate payload",
    )
    return CandidateSpecV1(
        _ref(payload["strategy_ref"], "strategy_ref"),
        _text(payload["origin"], "origin"),
        parent_strategy_ref=_nullable_ref(
            payload["parent_strategy_ref"], "parent_strategy_ref"
        ),
        origin_ref=_nullable_ref(payload["origin_ref"], "origin_ref"),
    )


def _decode_data(payload: Mapping[str, Any]) -> DataSpecV1:
    fields = {
        "symbol",
        "timeframe",
        "source",
        "dataset_revision",
        "timezone_name",
        "session_calendar",
        "start",
        "end",
        "adjustment_policy",
    }
    _exact_keys(payload, fields, "data payload")
    return DataSpecV1(
        _text(payload["symbol"], "symbol"),
        _text(payload["timeframe"], "timeframe"),
        _text(payload["source"], "source"),
        _text(payload["dataset_revision"], "dataset_revision"),
        _text(payload["timezone_name"], "timezone_name"),
        _text(payload["session_calendar"], "session_calendar"),
        _text(payload["start"], "start"),
        _text(payload["end"], "end"),
        _text(payload["adjustment_policy"], "adjustment_policy"),
    )


def _decode_execution(payload: Mapping[str, Any]) -> ExecutionSpecV1:
    _exact_keys(payload, {"starting_cash", "currency", "models"}, "execution payload")
    cash = payload["starting_cash"]
    if not isinstance(cash, Decimal):
        raise WireFormatError("starting_cash must be a canonical Decimal")
    models_value = payload["models"]
    if not isinstance(models_value, list):
        raise WireFormatError("models must be an array")
    models: list[ExecutionModelV1] = []
    for index, raw in enumerate(models_value):
        model = _object(raw, f"models[{index}]")
        _exact_keys(model, {"kind", "model", "parameters"}, f"models[{index}]")
        models.append(
            ExecutionModelV1(
                _text(model["kind"], f"models[{index}].kind"),
                _text(model["model"], f"models[{index}].model"),
                _object(model["parameters"], f"models[{index}].parameters"),
            )
        )
    return ExecutionSpecV1(cash, _text(payload["currency"], "currency"), tuple(models))


def _decode_engine_build(payload: Mapping[str, Any]) -> EngineBuildSpecV1:
    _exact_keys(
        payload,
        {"implementation", "revision", "artifact_sha256"},
        "engine-build payload",
    )
    return EngineBuildSpecV1(
        _text(payload["implementation"], "implementation"),
        _text(payload["revision"], "revision"),
        _text(payload["artifact_sha256"], "artifact_sha256"),
    )


def _decode_backtest_run(payload: Mapping[str, Any]) -> BacktestRunSpecV1:
    _exact_keys(
        payload,
        {
            "candidate_ref",
            "data_ref",
            "execution_ref",
            "engine_build_ref",
            "random_seed",
        },
        "backtest-run payload",
    )
    seed = payload["random_seed"]
    if seed is not None:
        seed = _integer(seed, "random_seed")
    return BacktestRunSpecV1(
        _ref(payload["candidate_ref"], "candidate_ref"),
        _ref(payload["data_ref"], "data_ref"),
        _ref(payload["execution_ref"], "execution_ref"),
        _ref(payload["engine_build_ref"], "engine_build_ref"),
        random_seed=seed,
    )


_DECODERS: dict[str, Callable[[Mapping[str, Any]], object]] = {
    "strategy": _decode_strategy,
    "candidate": _decode_candidate,
    "data": _decode_data,
    "execution": _decode_execution,
    "engine-build": _decode_engine_build,
    "backtest-run": _decode_backtest_run,
}


def encode_addressed_object(value: object) -> bytes:
    """Encode one supported immutable object with an explicit self-verifying ref."""

    kind = getattr(value, "KIND", None)
    version = getattr(value, "VERSION", None)
    ref = getattr(value, "ref", None)
    identity_payload = getattr(value, "identity_payload", None)
    if kind not in _SUPPORTED_KINDS or version != 1 or not isinstance(ref, ContentAddress):
        raise WireFormatError(
            f"unsupported addressed object type: {type(value).__name__}"
        )
    if not callable(identity_payload):
        raise WireFormatError("addressed object must expose identity_payload()")
    payload = identity_payload()
    expected = content_address(kind, version, payload)
    if expected != ref:
        raise WireFormatError("object ref does not match its identity payload")
    return canonical_json_bytes(
        {
            "wire_schema": _WIRE_SCHEMA,
            "ref": str(ref),
            "kind": kind,
            "version": version,
            "payload": payload,
        }
    )


def decode_addressed_object(data: bytes | str) -> object:
    """Decode, reconstruct, and re-verify one supported immutable object."""

    try:
        raw = canonical_json_loads(data)
    except ValueError as exc:
        raise WireFormatError(str(exc)) from exc
    envelope = _object(raw, "wire envelope")
    _exact_keys(
        envelope,
        {"wire_schema", "ref", "kind", "version", "payload"},
        "wire envelope",
    )
    if envelope["wire_schema"] != _WIRE_SCHEMA:
        raise WireFormatError("unsupported wire_schema")
    kind = _text(envelope["kind"], "kind")
    version = _integer(envelope["version"], "version")
    if kind not in _DECODERS or version != 1:
        raise WireFormatError(f"unsupported object kind/version: {kind} v{version}")
    declared_ref = _ref(envelope["ref"], "ref")
    if declared_ref.kind != kind or declared_ref.version != version:
        raise WireFormatError("declared ref kind/version does not match envelope")
    payload = _object(envelope["payload"], "payload")
    expected_ref = content_address(kind, version, payload)
    if expected_ref != declared_ref:
        raise WireFormatError("declared ref does not match envelope payload")

    try:
        value = _DECODERS[kind](payload)
    except WireFormatError:
        raise
    except (TypeError, ValueError) as exc:
        raise WireFormatError(f"invalid {kind} payload: {exc}") from exc

    actual_ref = getattr(value, "ref", None)
    if actual_ref != declared_ref:
        raise WireFormatError(
            "reconstructed object identity differs from persisted payload"
        )
    return value
