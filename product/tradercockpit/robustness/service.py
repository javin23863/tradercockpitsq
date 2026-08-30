"""Canonical TraderCockpit robustness model, execution, custody, and readback.

The first executable method is deterministic Monte Carlo manipulation of an
*actual persisted* trade sequence.  TraderCockpit derives analysis metrics from
producer-owned trade P&L observations; it never invents trades or presents the
derived metrics as native-producer facts.
"""
from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
import hashlib
import os
from pathlib import Path
import random
import tempfile
from typing import Any, Mapping

from tradercockpit.domain import (
    BacktestRunSpecV1,
    ContentAddress,
    EngineBuildSpecV1,
    ResultArtifactV1,
    canonical_json_bytes,
    canonical_json_loads,
    content_address,
)
from tradercockpit.storage import FileObjectStore

from .system_parameter_permutation import SystemParameterPermutationSettings
from .trade_manipulation import RandomlySkipTradesConfig, apply_randomly_skip_trades
from .trade_order import apply_randomize_trades_order

ROBUSTNESS_RESULT_SCHEMA = "tradercockpit.robustness.trade-monte-carlo.v1"
ROBUSTNESS_IMPLEMENTATION_REVISION = "trade-monte-carlo.v1"
ROBUSTNESS_IMPLEMENTATION = "tradercockpit-robustness"
_ALLOWED_METRICS = frozenset({"net_pnl", "max_drawdown", "trade_count"})
_ALLOWED_OPERATORS = frozenset({"gt", "gte", "lt", "lte", "eq"})
_MAX_TRIALS = 10_000


class RobustnessError(ValueError):
    """Base error for malformed or unverifiable robustness state."""


class RobustnessExecutionUnavailable(RobustnessError):
    """Raised when a configured method requires a canonical executor not yet bound."""


@dataclass(frozen=True, slots=True)
class ObservedTradeV1:
    trade_id: str
    pnl: Decimal

    def __post_init__(self) -> None:
        if not isinstance(self.trade_id, str) or not self.trade_id.strip() or self.trade_id != self.trade_id.strip():
            raise RobustnessError("trade id must be non-empty text without surrounding whitespace")
        if not isinstance(self.pnl, Decimal) or not self.pnl.is_finite():
            raise RobustnessError("trade pnl must be a finite Decimal")


@dataclass(frozen=True, slots=True)
class RobustnessMetricGateV1:
    metric: str
    operator: str
    threshold: Decimal

    def __post_init__(self) -> None:
        if self.metric not in _ALLOWED_METRICS:
            raise RobustnessError(f"unsupported robustness metric {self.metric!r}")
        if self.operator not in _ALLOWED_OPERATORS:
            raise RobustnessError(f"unsupported robustness operator {self.operator!r}")
        if not isinstance(self.threshold, Decimal) or not self.threshold.is_finite():
            raise RobustnessError("robustness threshold must be a finite Decimal")

    def identity_payload(self) -> Mapping[str, Any]:
        return {"metric": self.metric, "operator": self.operator, "threshold": self.threshold}


@dataclass(frozen=True, slots=True)
class RobustnessPlanV1:
    """One versioned TraderCockpit robustness request over an exact source result."""

    source_result_ref: ContentAddress
    trials: int = 100
    random_seed: int = 0
    randomize_trades_order: bool = True
    randomly_skip_trades: RandomlySkipTradesConfig | None = None
    gates: tuple[RobustnessMetricGateV1, ...] = ()
    system_parameter_permutation: SystemParameterPermutationSettings | None = None

    def __post_init__(self) -> None:
        if not isinstance(self.source_result_ref, ContentAddress) or self.source_result_ref.kind != "result":
            raise RobustnessError("source_result_ref must reference a result")
        if type(self.trials) is not int or not 1 <= self.trials <= _MAX_TRIALS:
            raise RobustnessError(f"trials must be an integer from 1 to {_MAX_TRIALS}")
        if type(self.random_seed) is not int or self.random_seed < 0:
            raise RobustnessError("random_seed must be a non-negative integer")
        if type(self.randomize_trades_order) is not bool:
            raise RobustnessError("randomize_trades_order must be boolean")
        if self.randomly_skip_trades is not None and not isinstance(self.randomly_skip_trades, RandomlySkipTradesConfig):
            raise RobustnessError("randomly_skip_trades must be RandomlySkipTradesConfig")
        if not isinstance(self.gates, tuple):
            object.__setattr__(self, "gates", tuple(self.gates))
        if any(not isinstance(gate, RobustnessMetricGateV1) for gate in self.gates):
            raise RobustnessError("gates must contain only RobustnessMetricGateV1 values")
        ordered = tuple(sorted(self.gates, key=lambda gate: canonical_json_bytes(gate.identity_payload())))
        if len({canonical_json_bytes(gate.identity_payload()) for gate in ordered}) != len(ordered):
            raise RobustnessError("gates must not contain duplicates")
        object.__setattr__(self, "gates", ordered)
        if self.system_parameter_permutation is not None and not isinstance(
            self.system_parameter_permutation, SystemParameterPermutationSettings
        ):
            raise RobustnessError("system_parameter_permutation has the wrong type")
        permutation_enabled = self.system_parameter_permutation is not None and self.system_parameter_permutation.enabled
        if not self.randomize_trades_order and self.randomly_skip_trades is None and not permutation_enabled:
            raise RobustnessError("at least one enabled robustness method must be configured")

    def identity_payload(self) -> Mapping[str, Any]:
        permutation = self.system_parameter_permutation
        return {
            "source_result_ref": str(self.source_result_ref),
            "trials": self.trials,
            "random_seed": self.random_seed,
            "randomize_trades_order": self.randomize_trades_order,
            "randomly_skip_trades": None if self.randomly_skip_trades is None else {"probability_pct": self.randomly_skip_trades.probability_pct},
            "gates": tuple(gate.identity_payload() for gate in self.gates),
            "system_parameter_permutation": None if permutation is None else permutation.as_sqx_settings(),
        }

    @property
    def ref(self) -> ContentAddress:
        return content_address("robustness-plan", 1, self.identity_payload())


class _PythonBoundedRandom:
    """TraderCockpit-owned deterministic RNG adapter for reconstructed methods."""

    def __init__(self, seed: int):
        self._rng = random.Random(seed)

    def next_int(self, bound: int, /) -> int:
        if type(bound) is not int or bound <= 0:
            raise RobustnessError("random bound must be a positive integer")
        return self._rng.randrange(bound)


def _decimal(value: object, name: str) -> Decimal:
    if isinstance(value, bool):
        raise RobustnessError(f"{name} must be numeric, not bool")
    if isinstance(value, int):
        return Decimal(value)
    if isinstance(value, Decimal) and value.is_finite():
        return value
    raise RobustnessError(f"{name} must be an int or finite Decimal")


def extract_observed_trades(source: ResultArtifactV1) -> tuple[ObservedTradeV1, ...]:
    """Extract producer-owned trade observations without inventing missing identity."""

    if not isinstance(source, ResultArtifactV1):
        raise RobustnessError("source must be ResultArtifactV1")
    raw = source.payload.get("trades")
    if not isinstance(raw, tuple) or not raw:
        raise RobustnessError("source result must contain a non-empty canonical trades sequence")
    trades: list[ObservedTradeV1] = []
    seen: set[str] = set()
    for index, item in enumerate(raw):
        if not isinstance(item, Mapping):
            raise RobustnessError(f"source trade {index} must be a mapping")
        trade_id = item.get("id")
        if not isinstance(trade_id, str):
            raise RobustnessError(f"source trade {index} must carry producer-owned id")
        if trade_id in seen:
            raise RobustnessError(f"duplicate source trade id {trade_id!r}")
        seen.add(trade_id)
        trades.append(ObservedTradeV1(trade_id, _decimal(item.get("pnl"), f"source trade {trade_id} pnl")))
    return tuple(trades)


def _compare(actual: Decimal, operator: str, threshold: Decimal) -> bool:
    if operator == "gt": return actual > threshold
    if operator == "gte": return actual >= threshold
    if operator == "lt": return actual < threshold
    if operator == "lte": return actual <= threshold
    if operator == "eq": return actual == threshold
    raise AssertionError(operator)


def _metrics(trades: list[ObservedTradeV1]) -> dict[str, Decimal | int]:
    equity = Decimal(0)
    peak = Decimal(0)
    max_drawdown = Decimal(0)
    for trade in trades:
        equity += trade.pnl
        if equity > peak:
            peak = equity
        drawdown = peak - equity
        if drawdown > max_drawdown:
            max_drawdown = drawdown
    return {"net_pnl": equity, "max_drawdown": max_drawdown, "trade_count": len(trades)}


def _gate_passed(metrics: Mapping[str, Decimal | int], gates: tuple[RobustnessMetricGateV1, ...]) -> bool | None:
    if not gates:
        return None
    return all(_compare(_decimal(metrics[gate.metric], gate.metric), gate.operator, gate.threshold) for gate in gates)


def _implementation_artifact_sha256() -> str:
    """Hash the exact implementation files that own this revision's semantics."""

    digest = hashlib.sha256()
    root = Path(__file__).resolve().parent
    for name in ("service.py", "trade_manipulation.py", "trade_order.py", "system_parameter_permutation.py"):
        data = (root / name).read_bytes()
        digest.update(name.encode("utf-8"))
        digest.update(b"\0")
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)
    return digest.hexdigest()


def _implementation_build() -> EngineBuildSpecV1:
    from .builds import KNOWN_ROBUSTNESS_ARTIFACT_SHA256

    actual_sha = _implementation_artifact_sha256()
    expected_sha = KNOWN_ROBUSTNESS_ARTIFACT_SHA256.get(ROBUSTNESS_IMPLEMENTATION_REVISION)
    if expected_sha != actual_sha:
        raise RobustnessExecutionUnavailable(
            "robustness implementation bytes do not match the registered revision; bump/register the revision before execution"
        )
    return EngineBuildSpecV1(
        ROBUSTNESS_IMPLEMENTATION,
        ROBUSTNESS_IMPLEMENTATION_REVISION,
        actual_sha,
    )


def _verify_registered_build(store: FileObjectStore, ref: ContentAddress) -> EngineBuildSpecV1:
    from .builds import KNOWN_ROBUSTNESS_ARTIFACT_SHA256

    try:
        build = store.resolve(ref)
    except KeyError as exc:
        raise RobustnessError("robustness implementation build is missing") from exc
    if not isinstance(build, EngineBuildSpecV1) or build.ref != ref:
        raise RobustnessError("robustness implementation build custody is invalid")
    if build.implementation != ROBUSTNESS_IMPLEMENTATION:
        raise RobustnessError("robustness result producer is not TraderCockpit robustness")
    expected_sha = KNOWN_ROBUSTNESS_ARTIFACT_SHA256.get(build.revision)
    if expected_sha is None or build.artifact_sha256 != expected_sha:
        raise RobustnessError("robustness implementation revision is not registered")
    return build


def _trial_payloads(source_trades: tuple[ObservedTradeV1, ...], plan: RobustnessPlanV1) -> tuple[Mapping[str, Any], ...]:
    rng = _PythonBoundedRandom(plan.random_seed)
    trials: list[Mapping[str, Any]] = []
    for trial_index in range(plan.trials):
        working = list(source_trades)
        if plan.randomize_trades_order:
            apply_randomize_trades_order(working, rng)
        if plan.randomly_skip_trades is not None:
            apply_randomly_skip_trades(working, plan.randomly_skip_trades, rng)
        metrics = _metrics(working)
        trials.append({
            "trial_index": trial_index,
            "trade_ids": tuple(trade.trade_id for trade in working),
            "net_pnl": metrics["net_pnl"],
            "max_drawdown": metrics["max_drawdown"],
            "trade_count": metrics["trade_count"],
            "filter_passed": _gate_passed(metrics, plan.gates),
        })
    return tuple(trials)


def _result_payload(source: ResultArtifactV1, plan: RobustnessPlanV1) -> Mapping[str, Any]:
    source_trades = extract_observed_trades(source)
    trials = _trial_payloads(source_trades, plan)
    net_values = tuple(row["net_pnl"] for row in trials)
    drawdowns = tuple(row["max_drawdown"] for row in trials)
    counts = tuple(row["trade_count"] for row in trials)
    passed = tuple(row["filter_passed"] for row in trials if row["filter_passed"] is not None)
    return {
        "authority": "tradercockpit-derived",
        "analysis": "monte-carlo-trade-manipulation",
        "implementation_revision": ROBUSTNESS_IMPLEMENTATION_REVISION,
        "source_result_ref": str(source.ref),
        "source_result_schema": source.result_schema,
        "plan_ref": str(plan.ref),
        "plan": plan.identity_payload(),
        "metric_semantics": {
            "net_pnl": "sum of source trade pnl values in the retained trial sequence",
            "max_drawdown": "maximum peak-to-current decline over cumulative retained trade pnl",
            "trade_count": "count of retained producer-owned trades",
        },
        "trials": trials,
        "summary": {
            "source_trade_count": len(source_trades),
            "trial_count": len(trials),
            "minimum_trade_count": min(counts),
            "worst_net_pnl": min(net_values),
            "worst_max_drawdown": max(drawdowns),
            "filter_pass_count": None if not passed else sum(1 for value in passed if value),
        },
    }


def _plan_from_payload(payload: Mapping[str, Any]) -> RobustnessPlanV1:
    try:
        source_ref = ContentAddress.parse(payload["source_result_ref"])
        skip_raw = payload.get("randomly_skip_trades")
        skip = None if skip_raw is None else RandomlySkipTradesConfig(skip_raw["probability_pct"])
        gates = tuple(RobustnessMetricGateV1(item["metric"], item["operator"], _decimal(item["threshold"], "threshold")) for item in payload.get("gates", ()))
        permutation_raw = payload.get("system_parameter_permutation")
        permutation = None
        if permutation_raw is not None:
            settings = permutation_raw["Settings"]
            permutation = SystemParameterPermutationSettings(
                max_tests=settings["MaxTests"],
                optim_periods=settings["OptimPeriods"],
                optim_exit_types=settings["OptimExitTypes"],
                enabled=permutation_raw["use"],
            )
        return RobustnessPlanV1(
            source_ref,
            trials=payload["trials"],
            random_seed=payload["random_seed"],
            randomize_trades_order=payload["randomize_trades_order"],
            randomly_skip_trades=skip,
            gates=gates,
            system_parameter_permutation=permutation,
        )
    except (KeyError, TypeError, ValueError, RobustnessError) as exc:
        raise RobustnessError("invalid persisted robustness plan") from exc


class FileRobustnessCatalog:
    """Mutable index only; immutable source/results remain in FileObjectStore."""

    def __init__(self, root: Path | str):
        self.root = Path(root).resolve() / "robustness" / "catalog"
        self.root.mkdir(parents=True, exist_ok=True)

    def _path(self, source_ref: ContentAddress) -> Path:
        if not isinstance(source_ref, ContentAddress) or source_ref.kind != "result":
            raise RobustnessError("source_ref must reference result")
        return self.root / f"{source_ref.sha256}.json"

    def list(self, source_ref: ContentAddress) -> tuple[ContentAddress, ...]:
        path = self._path(source_ref)
        if not path.exists():
            return ()
        try:
            payload = canonical_json_loads(path.read_bytes())
            if payload.get("source_result_ref") != str(source_ref):
                raise RobustnessError("robustness catalog source identity mismatch")
            refs = tuple(ContentAddress.parse(value) for value in payload.get("result_refs", ()))
        except (OSError, AttributeError, TypeError, ValueError) as exc:
            raise RobustnessError("invalid robustness catalog") from exc
        if any(ref.kind != "result" for ref in refs) or len(set(refs)) != len(refs):
            raise RobustnessError("invalid robustness catalog result refs")
        return tuple(sorted(refs, key=str))

    def add(self, source_ref: ContentAddress, result_ref: ContentAddress) -> None:
        refs = set(self.list(source_ref))
        refs.add(result_ref)
        data = canonical_json_bytes({"source_result_ref": str(source_ref), "result_refs": tuple(sorted((str(ref) for ref in refs)))})
        target = self._path(source_ref)
        fd, name = tempfile.mkstemp(prefix=".catalog.", suffix=".tmp", dir=target.parent)
        temp = Path(name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(data)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temp, target)
        finally:
            if temp.exists():
                temp.unlink()


class RobustnessServiceV1:
    def __init__(self, state_root: Path | str):
        self.store = FileObjectStore(state_root)
        self.catalog = FileRobustnessCatalog(state_root)

    def _source(self, ref: ContentAddress) -> ResultArtifactV1:
        try:
            source = self.store.resolve(ref)
        except KeyError as exc:
            raise RobustnessError(f"missing source result {ref}") from exc
        if not isinstance(source, ResultArtifactV1) or source.ref != ref:
            raise RobustnessError("source result resolved to the wrong object")
        try:
            run = self.store.resolve(source.run_ref)
        except KeyError as exc:
            raise RobustnessError("source result run is missing") from exc
        if not isinstance(run, BacktestRunSpecV1) or run.ref != source.run_ref:
            raise RobustnessError("source result run custody is invalid")
        if source.producer_build_ref != run.engine_build_ref:
            raise RobustnessError("source result producer build does not match source run")
        extract_observed_trades(source)
        return source

    def run(self, plan: RobustnessPlanV1) -> ResultArtifactV1:
        if not isinstance(plan, RobustnessPlanV1):
            raise RobustnessError("plan must be RobustnessPlanV1")
        if plan.system_parameter_permutation is not None and plan.system_parameter_permutation.enabled:
            raise RobustnessExecutionUnavailable(
                "system-parameter permutation requires the canonical execution-only backtest seam; no alternate evaluator pipeline is created here"
            )
        source = self._source(plan.source_result_ref)
        build = _implementation_build()
        self.store.put(build)
        result = ResultArtifactV1(source.run_ref, build.ref, ROBUSTNESS_RESULT_SCHEMA, _result_payload(source, plan))
        self.store.put(result)
        self.catalog.add(source.ref, result.ref)
        return self.read(result.ref)

    def read(self, result_ref: ContentAddress) -> ResultArtifactV1:
        if not isinstance(result_ref, ContentAddress) or result_ref.kind != "result":
            raise RobustnessError("result_ref must reference result")
        try:
            result = self.store.resolve(result_ref)
        except KeyError as exc:
            raise RobustnessError(f"missing robustness result {result_ref}") from exc
        if not isinstance(result, ResultArtifactV1) or result.ref != result_ref:
            raise RobustnessError("robustness result resolved to the wrong object")
        if result.result_schema != ROBUSTNESS_RESULT_SCHEMA:
            raise RobustnessError("result is not a TraderCockpit robustness result")
        payload = result.payload
        try:
            source_ref = ContentAddress.parse(payload["source_result_ref"])
            plan = _plan_from_payload(payload["plan"])
            plan_ref = ContentAddress.parse(payload["plan_ref"])
        except (KeyError, TypeError, ValueError) as exc:
            raise RobustnessError("invalid robustness result payload") from exc
        if plan.ref != plan_ref or plan.source_result_ref != source_ref:
            raise RobustnessError("robustness plan identity mismatch")
        source = self._source(source_ref)
        build = _verify_registered_build(self.store, result.producer_build_ref)
        if result.run_ref != source.run_ref:
            raise RobustnessError("robustness result run identity mismatch")
        if payload.get("implementation_revision") != build.revision:
            raise RobustnessError("robustness result implementation revision mismatch")
        if build.revision == ROBUSTNESS_IMPLEMENTATION_REVISION:
            expected = _result_payload(source, plan)
            if canonical_json_bytes(payload) != canonical_json_bytes(expected):
                raise RobustnessError("robustness result does not reproduce from source evidence and plan")
        return result

    def list_for_source(self, source_ref: ContentAddress) -> tuple[ResultArtifactV1, ...]:
        self._source(source_ref)
        return tuple(self.read(ref) for ref in self.catalog.list(source_ref))
