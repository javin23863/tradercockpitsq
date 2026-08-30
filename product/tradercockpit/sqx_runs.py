"""Bind imported SQX candidates to exact TraderCockpit runs and execute them natively."""

from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Callable, Mapping
from uuid import uuid4

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    ContentAddress,
    DataSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
)
from tradercockpit.engine.contracts import EngineContractError
from tradercockpit.engine.execution_service import execute_backtest
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore

from .sqx_retester import SqxRetesterEvaluator, sqx_retester_engine_build


SQX_RUN_START_SCHEMA = "tc.sqx-native-run-start.v1"


class SqxRunRequestError(ValueError):
    """Raised when a native SQX run request is incomplete or invalid."""


def _text(value: object, name: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        raise SqxRunRequestError(f"{name} must be a non-empty string without surrounding whitespace")
    return value


def _mapping(value: object, name: str) -> Mapping[str, object]:
    if not isinstance(value, Mapping):
        raise SqxRunRequestError(f"{name} must be an object")
    return value


def _decimal(value: object, name: str) -> Decimal:
    text = _text(value, name)
    try:
        parsed = Decimal(text)
    except InvalidOperation as exc:
        raise SqxRunRequestError(f"{name} must be an exact decimal string") from exc
    if not parsed.is_finite():
        raise SqxRunRequestError(f"{name} must be finite")
    return parsed


def _candidate_ref(value: object) -> ContentAddress:
    text = _text(value, "candidate_ref")
    try:
        ref = ContentAddress.parse(text)
    except ValueError as exc:
        raise SqxRunRequestError("candidate_ref must be a valid TraderCockpit content address") from exc
    if ref.kind != "candidate":
        raise SqxRunRequestError("candidate_ref must reference 'candidate'")
    return ref


def _data_spec(payload: object) -> DataSpecV1:
    data = _mapping(payload, "data")
    return DataSpecV1(
        symbol=_text(data.get("symbol"), "data.symbol"),
        timeframe=_text(data.get("timeframe"), "data.timeframe"),
        source=_text(data.get("source"), "data.source"),
        dataset_revision=_text(data.get("dataset_revision"), "data.dataset_revision"),
        timezone_name=_text(data.get("timezone_name"), "data.timezone_name"),
        session_calendar=_text(data.get("session_calendar"), "data.session_calendar"),
        start=_text(data.get("start"), "data.start"),
        end=_text(data.get("end"), "data.end"),
        adjustment_policy=_text(data.get("adjustment_policy"), "data.adjustment_policy"),
    )


def _execution_spec(payload: object) -> ExecutionSpecV1:
    execution = _mapping(payload, "execution")
    raw_models = execution.get("models")
    if not isinstance(raw_models, list) or not raw_models:
        raise SqxRunRequestError("execution.models must be a non-empty array")
    models: list[ExecutionModelV1] = []
    for index, raw in enumerate(raw_models):
        item = _mapping(raw, f"execution.models[{index}]")
        parameters = item.get("parameters", {})
        if not isinstance(parameters, Mapping):
            raise SqxRunRequestError(f"execution.models[{index}].parameters must be an object")
        models.append(
            ExecutionModelV1(
                kind=_text(item.get("kind"), f"execution.models[{index}].kind"),
                model=_text(item.get("model"), f"execution.models[{index}].model"),
                parameters=parameters,
            )
        )
    return ExecutionSpecV1(
        starting_cash=_decimal(execution.get("starting_cash"), "execution.starting_cash"),
        currency=_text(execution.get("currency"), "execution.currency"),
        models=tuple(models),
    )


def _random_seed(value: object) -> int | None:
    if value is None:
        return None
    if not isinstance(value, int) or isinstance(value, bool) or value < 0:
        raise SqxRunRequestError("random_seed must be a non-negative integer or null")
    return value


def start_sqx_native_run(
    sqx_home: Path | str | None,
    state_root: Path | str | None,
    request: object,
    *,
    evaluator_factory: Callable[[Path | str], object] = SqxRetesterEvaluator,
    invocation_id_factory: Callable[[], str] = lambda: f"sqx-{uuid4().hex}",
    clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
) -> dict[str, object]:
    """Persist exact run inputs, execute SQX Retester, and return durable identities."""

    body = _mapping(request, "request")
    candidate_ref = _candidate_ref(body.get("candidate_ref"))
    data = _data_spec(body.get("data"))
    execution = _execution_spec(body.get("execution"))
    random_seed = _random_seed(body.get("random_seed"))

    if state_root is None:
        raise SqxRunRequestError("TraderCockpit state root is not configured")
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise SqxRunRequestError(f"TraderCockpit state root does not exist: {root}")

    store = FileObjectStore(root)
    lifecycle = FileRunLifecycleStore(root)
    try:
        candidate = store.resolve(candidate_ref)
    except KeyError as exc:
        raise SqxRunRequestError(f"candidate_ref is not in TraderCockpit custody: {candidate_ref}") from exc
    if not isinstance(candidate, CandidateSpecV1) or candidate.ref != candidate_ref:
        raise SqxRunRequestError("candidate_ref did not resolve to the exact CandidateSpecV1")

    engine_build = sqx_retester_engine_build()
    run = BacktestRunSpecV1(
        candidate_ref=candidate.ref,
        data_ref=data.ref,
        execution_ref=execution.ref,
        engine_build_ref=engine_build.ref,
        random_seed=random_seed,
    )
    for item in (data, execution, engine_build, run):
        if store.put(item) != item.ref:
            raise EngineContractError("content store returned an unexpected immutable identity")

    invocation_id = _text(invocation_id_factory(), "invocation_id")
    issued_at = clock().astimezone(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")
    evaluator = evaluator_factory(sqx_home)
    result = execute_backtest(
        run.ref,
        store,
        lifecycle,
        evaluator,
        invocation_id=invocation_id,
        issued_at=issued_at,
    )
    return {
        "schema": SQX_RUN_START_SCHEMA,
        "status": "completed",
        "run_ref": str(result.run_ref),
        "invocation_id": invocation_id,
        "receipt_ref": str(result.receipt_ref),
        "result_ref": str(result.result_ref),
        "lifecycle_event_ref": str(result.lifecycle_event_ref),
        "inputs": {
            "candidate_ref": str(candidate.ref),
            "data_ref": str(data.ref),
            "execution_ref": str(execution.ref),
            "engine_build_ref": str(engine_build.ref),
            "random_seed": random_seed,
        },
        "validation": {
            "available": False,
            "detail": "Native SQX execution completed. Strategy-quality validation is not claimed by this execution receipt.",
        },
    }
