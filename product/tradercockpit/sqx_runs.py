"""Bind imported SQX candidates to exact native Retester executions."""

from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, Mapping
from uuid import uuid4

from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    ContentAddress,
    StrategySpecV1,
)
from tradercockpit.engine import EngineContractError, execute_backtest
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore

from .sqx_retester import (
    SqxRetesterEvaluator,
    sqx_retester_engine_build,
    sqx_retester_native_contexts,
)


SQX_RUN_START_SCHEMA = "tc.sqx-native-run-start.v1"


class SqxRunRequestError(ValueError):
    """Raised when a native SQX run request is incomplete or invalid."""


class SqxRunUnavailableError(RuntimeError):
    """Raised when a valid native run request cannot use server-side state."""


def _text(value: object, name: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        raise SqxRunRequestError(
            f"{name} must be a non-empty string without surrounding whitespace"
        )
    return value


def _mapping(value: object, name: str) -> Mapping[str, object]:
    if not isinstance(value, Mapping):
        raise SqxRunRequestError(f"{name} must be an object")
    return value


def _candidate_ref(value: object) -> ContentAddress:
    text = _text(value, "candidate_ref")
    try:
        ref = ContentAddress.parse(text)
    except ValueError as exc:
        raise SqxRunRequestError(
            "candidate_ref must be a valid TraderCockpit content address"
        ) from exc
    if ref.kind != "candidate":
        raise SqxRunRequestError("candidate_ref must reference 'candidate'")
    return ref


def _resolve_candidate_and_strategy(
    store: FileObjectStore,
    candidate_ref: ContentAddress,
) -> tuple[CandidateSpecV1, StrategySpecV1]:
    try:
        candidate = store.resolve(candidate_ref)
    except KeyError as exc:
        raise SqxRunRequestError(
            f"candidate_ref is not in TraderCockpit custody: {candidate_ref}"
        ) from exc
    if not isinstance(candidate, CandidateSpecV1) or candidate.ref != candidate_ref:
        raise SqxRunRequestError(
            "candidate_ref did not resolve to the exact CandidateSpecV1"
        )
    try:
        strategy = store.resolve(candidate.strategy_ref)
    except KeyError as exc:
        raise SqxRunRequestError(
            "candidate strategy is missing from TraderCockpit custody"
        ) from exc
    if not isinstance(strategy, StrategySpecV1) or strategy.ref != candidate.strategy_ref:
        raise SqxRunRequestError(
            "candidate strategy did not resolve to the exact StrategySpecV1"
        )
    return candidate, strategy


def start_sqx_native_run(
    sqx_home: Path | str | None,
    state_root: Path | str | None,
    request: object,
    *,
    evaluator_factory: Callable[[Path | str | None], object] | None = None,
    invocation_id_factory: Callable[[], str] = lambda: f"sqx-{uuid4().hex}",
    clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
) -> dict[str, object]:
    """Derive producer-owned context, execute Retester, and return durable refs.

    The caller supplies only the candidate identity. Data/execution context and
    the candidate bytes come from TraderCockpit's exact imported archive custody
    plus the verified native Retester project. User-entered execution fiction is
    not accepted.
    """

    body = _mapping(request, "request")
    if set(body) != {"candidate_ref"}:
        raise SqxRunRequestError(
            "native SQX run request must contain exactly candidate_ref"
        )
    candidate_ref = _candidate_ref(body.get("candidate_ref"))

    if state_root is None:
        raise SqxRunUnavailableError("TraderCockpit state root is not configured")
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise SqxRunUnavailableError(
            f"TraderCockpit state root does not exist: {root}"
        )

    store = FileObjectStore(root)
    lifecycle = FileRunLifecycleStore(root)
    candidate, strategy = _resolve_candidate_and_strategy(store, candidate_ref)

    data_context, execution_context = sqx_retester_native_contexts(
        sqx_home,
        strategy,
        state_root=root,
    )
    engine_build = sqx_retester_engine_build()
    run = BacktestRunSpecV1(
        candidate_ref=candidate.ref,
        data_ref=data_context.ref,
        execution_ref=execution_context.ref,
        engine_build_ref=engine_build.ref,
        random_seed=None,
    )
    for item in (data_context, execution_context, engine_build, run):
        if store.put(item) != item.ref:
            raise EngineContractError(
                "content store returned an unexpected immutable identity"
            )

    invocation_id = _text(invocation_id_factory(), "invocation_id")
    issued_at = (
        clock()
        .astimezone(timezone.utc)
        .isoformat(timespec="microseconds")
        .replace("+00:00", "Z")
    )
    if evaluator_factory is None:
        evaluator = SqxRetesterEvaluator(sqx_home, custody_root=root)
    else:
        evaluator = evaluator_factory(sqx_home)
    execution = execute_backtest(
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
        "run_ref": str(execution.run_ref),
        "invocation_id": invocation_id,
        "receipt_ref": str(execution.receipt_ref),
        "result_ref": str(execution.result_ref),
        "lifecycle_event_ref": str(execution.lifecycle_event_ref),
        "inputs": {
            "candidate_ref": str(candidate.ref),
            "strategy_ref": str(strategy.ref),
            "data_ref": str(data_context.ref),
            "execution_ref": str(execution_context.ref),
            "engine_build_ref": str(engine_build.ref),
            "random_seed": None,
        },
        "native_context": {
            "schema": data_context.context_schema,
            "source_project": data_context.source_project,
            "source_task": data_context.source_task,
            "source_config_sha256": data_context.source_config_sha256,
            "candidate_archive_sha256": data_context.candidate_archive_sha256,
            "candidate_settings_sha256": data_context.candidate_settings_sha256,
        },
        "validation": {
            "available": False,
            "detail": (
                "Native SQX execution completed. Strategy-quality validation "
                "is a separate governed product action."
            ),
        },
    }
