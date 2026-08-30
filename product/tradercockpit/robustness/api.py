"""HTTP-neutral API responses for the canonical robustness capability.

Shared server route registration is intentionally deferred while Recovery
Vertical 1 owns ``app_server.py``. These functions are the stable service/API
contract that can be wired after that occupied server seam is released.
"""
from __future__ import annotations

from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Mapping

from tradercockpit.domain import ContentAddress

from .service import (
    RobustnessError,
    RobustnessExecutionUnavailable,
    RobustnessMetricGateV1,
    RobustnessPlanV1,
    RobustnessServiceV1,
)
from .system_parameter_permutation import SystemParameterPermutationSettings
from .trade_manipulation import RandomlySkipTradesConfig

ROBUSTNESS_START_PATH = "/api/robustness-runs"
ROBUSTNESS_READ_PATH = "/api/robustness-runs/read"
ROBUSTNESS_LIST_PATH = "/api/robustness-results"


def _decimal(value: object, name: str) -> Decimal:
    if isinstance(value, bool):
        raise RobustnessError(f"{name} must be numeric")
    if isinstance(value, int):
        return Decimal(value)
    if isinstance(value, str):
        try:
            result = Decimal(value)
        except InvalidOperation as exc:
            raise RobustnessError(f"{name} must be a decimal string") from exc
        if result.is_finite():
            return result
    raise RobustnessError(f"{name} must be an integer or finite decimal string")


def _parse_plan(request: Mapping[str, Any]) -> RobustnessPlanV1:
    try:
        source_ref = ContentAddress.parse(request["sourceResultRef"])
        config = request["config"]
        if not isinstance(config, Mapping):
            raise RobustnessError("config must be an object")

        skip_value = config.get("randomlySkipTradesProbabilityPct")
        skip = None if skip_value is None else RandomlySkipTradesConfig(skip_value)

        gates_raw = config.get("filters", ())
        if not isinstance(gates_raw, (list, tuple)):
            raise RobustnessError("filters must be an array")
        gates = tuple(
            RobustnessMetricGateV1(
                item["metric"],
                item["operator"],
                _decimal(item["threshold"], "filter threshold"),
            )
            for item in gates_raw
        )

        permutation = None
        permutation_raw = config.get("systemParameterPermutation")
        if permutation_raw is not None:
            if not isinstance(permutation_raw, Mapping):
                raise RobustnessError("systemParameterPermutation must be an object")
            permutation = SystemParameterPermutationSettings(
                max_tests=permutation_raw.get("maxTests", 1),
                optim_periods=permutation_raw.get("optimPeriods", False),
                optim_exit_types=permutation_raw.get("optimExitTypes", False),
                enabled=permutation_raw.get("enabled", True),
            )

        return RobustnessPlanV1(
            source_ref,
            trials=config.get("trials", 100),
            random_seed=config.get("randomSeed", 0),
            randomize_trades_order=config.get("randomizeTradesOrder", True),
            randomly_skip_trades=skip,
            gates=gates,
            system_parameter_permutation=permutation,
        )
    except (KeyError, TypeError, ValueError, RobustnessError) as exc:
        if isinstance(exc, RobustnessError):
            raise
        raise RobustnessError("invalid robustness request") from exc


def _json_ready(value: object) -> object:
    if isinstance(value, Decimal):
        return str(value)
    if isinstance(value, Mapping):
        return {key: _json_ready(item) for key, item in value.items()}
    if isinstance(value, (tuple, list)):
        return [_json_ready(item) for item in value]
    if value is None or isinstance(value, (bool, int, str)):
        return value
    raise RobustnessError(f"value of type {type(value).__name__} is not JSON-ready")


def _result_payload(result) -> dict[str, Any]:
    return {
        "resultRef": str(result.ref),
        "runRef": str(result.run_ref),
        "producerBuildRef": str(result.producer_build_ref),
        "resultSchema": result.result_schema,
        "payload": _json_ready(result.payload),
    }


def robustness_start_response(state_root: Path | str | None, request: Mapping[str, Any]) -> tuple[int, dict[str, Any]]:
    if state_root is None:
        return 503, {"error": "state_root_unavailable"}
    try:
        plan = _parse_plan(request)
        result = RobustnessServiceV1(state_root).run(plan)
    except RobustnessExecutionUnavailable as exc:
        return 409, {"error": "execution_unavailable", "detail": str(exc)}
    except RobustnessError as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    return 201, _result_payload(result)


def robustness_read_response(state_root: Path | str | None, result_ref: str) -> tuple[int, dict[str, Any]]:
    if state_root is None:
        return 503, {"error": "state_root_unavailable"}
    try:
        ref = ContentAddress.parse(result_ref)
        result = RobustnessServiceV1(state_root).read(ref)
    except RobustnessError as exc:
        return 404, {"error": "robustness_result_not_found", "detail": str(exc)}
    except ValueError as exc:
        return 400, {"error": "invalid_result_ref", "detail": str(exc)}
    return 200, _result_payload(result)


def robustness_list_response(state_root: Path | str | None, source_result_ref: str) -> tuple[int, dict[str, Any]]:
    if state_root is None:
        return 503, {"error": "state_root_unavailable"}
    try:
        source_ref = ContentAddress.parse(source_result_ref)
        results = RobustnessServiceV1(state_root).list_for_source(source_ref)
    except (RobustnessError, ValueError) as exc:
        return 400, {"error": "invalid_source_result", "detail": str(exc)}
    return 200, {"sourceResultRef": str(source_ref), "results": tuple(_result_payload(result) for result in results)}
