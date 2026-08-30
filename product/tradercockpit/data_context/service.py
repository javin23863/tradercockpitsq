"""Canonical TraderCockpit data and trading-context configuration.

This package does not create a market-data producer or execution engine. It
persists the existing canonical ``DataSpecV1`` and ``ExecutionSpecV1`` objects
and records only a verified composite reference so those exact assumptions can
be reopened and handed to the canonical run contract later.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
import os
from pathlib import Path
import tempfile
from typing import Any, Mapping

from tradercockpit.domain import (
    ContentAddress,
    DataSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    canonical_json_bytes,
    canonical_json_loads,
    content_address,
)
from tradercockpit.storage import FileObjectStore

DATA_TRADING_CONTEXT_SCHEMA = "tc.data-trading-context.v1"
DATA_TRADING_CONTEXT_LIST_SCHEMA = "tc.data-trading-context-list.v1"
DATA_TRADING_CONTEXT_KIND = "data-trading-context"


class DataTradingContextError(ValueError):
    """Raised when a requested context is malformed."""


class DataTradingContextStateError(RuntimeError):
    """Raised when durable context custody is missing or inconsistent."""


def _decimal_from_request(value: object, name: str) -> Decimal:
    if isinstance(value, bool) or isinstance(value, float):
        raise DataTradingContextError(f"{name} must be an exact integer or decimal string")
    if isinstance(value, int):
        result = Decimal(value)
    elif isinstance(value, str) and value and value == value.strip():
        try:
            result = Decimal(value)
        except InvalidOperation as exc:
            raise DataTradingContextError(f"{name} must be an exact decimal string") from exc
    else:
        raise DataTradingContextError(f"{name} must be an exact integer or decimal string")
    if not result.is_finite() or result <= 0:
        raise DataTradingContextError(f"{name} must be finite and positive")
    return result


@dataclass(frozen=True, slots=True)
class DataTradingContextConfigV1:
    """TraderCockpit-owned research assumptions that resolve to canonical specs."""

    symbol: str
    timeframe: str
    source: str
    dataset_revision: str
    timezone_name: str
    session_calendar: str
    start: str
    end: str
    adjustment_policy: str = "none"
    starting_cash: Decimal = Decimal("100000")
    currency: str = "USD"
    fill_model: str = "bar-close"

    def build_specs(self) -> tuple[DataSpecV1, ExecutionSpecV1]:
        """Construct the existing canonical objects; their validators own syntax."""

        data = DataSpecV1(
            self.symbol,
            self.timeframe,
            self.source,
            self.dataset_revision,
            self.timezone_name,
            self.session_calendar,
            self.start,
            self.end,
            self.adjustment_policy,
        )
        execution = ExecutionSpecV1(
            self.starting_cash,
            self.currency,
            (ExecutionModelV1("fill", self.fill_model, {}),),
        )
        return data, execution

    @classmethod
    def from_request(cls, request: object) -> "DataTradingContextConfigV1":
        if not isinstance(request, Mapping):
            raise DataTradingContextError("request body must be an object")
        allowed = {
            "symbol",
            "timeframe",
            "source",
            "datasetRevision",
            "timezone",
            "sessionCalendar",
            "start",
            "end",
            "adjustmentPolicy",
            "startingCash",
            "currency",
            "fillModel",
        }
        unknown = sorted(set(request) - allowed)
        if unknown:
            raise DataTradingContextError("unknown request fields: " + ", ".join(unknown))

        required = (
            "symbol",
            "timeframe",
            "source",
            "datasetRevision",
            "timezone",
            "sessionCalendar",
            "start",
            "end",
        )
        values: dict[str, str] = {}
        for name in required:
            value = request.get(name)
            if not isinstance(value, str) or not value or value != value.strip():
                raise DataTradingContextError(f"{name} must be a non-empty trimmed string")
            values[name] = value

        def optional_text(name: str, default: str) -> str:
            value = request.get(name, default)
            if not isinstance(value, str) or not value or value != value.strip():
                raise DataTradingContextError(f"{name} must be a non-empty trimmed string")
            return value

        config = cls(
            symbol=values["symbol"],
            timeframe=values["timeframe"],
            source=values["source"],
            dataset_revision=values["datasetRevision"],
            timezone_name=values["timezone"],
            session_calendar=values["sessionCalendar"],
            start=values["start"],
            end=values["end"],
            adjustment_policy=optional_text("adjustmentPolicy", "none"),
            starting_cash=_decimal_from_request(request.get("startingCash", "100000"), "startingCash"),
            currency=optional_text("currency", "USD"),
            fill_model=optional_text("fillModel", "bar-close"),
        )
        config.build_specs()
        return config


@dataclass(frozen=True, slots=True)
class DataTradingContextV1:
    """Resolved pair of canonical data/execution specs."""

    data: DataSpecV1
    execution: ExecutionSpecV1

    @property
    def ref(self) -> ContentAddress:
        return content_address(
            DATA_TRADING_CONTEXT_KIND,
            1,
            {
                "data_ref": str(self.data.ref),
                "execution_ref": str(self.execution.ref),
            },
        )

    def record(self) -> dict[str, Any]:
        return {
            "schema": DATA_TRADING_CONTEXT_SCHEMA,
            "context_ref": str(self.ref),
            "authority": {
                "market_and_dataset_identity": "user-supplied",
                "execution_assumptions": "tradercockpit-owned",
                "native_sqx_binding": False,
            },
            "data": {
                "ref": str(self.data.ref),
                "symbol": self.data.symbol,
                "timeframe": self.data.timeframe,
                "source": self.data.source,
                "dataset_revision": self.data.dataset_revision,
                "timezone": self.data.timezone_name,
                "session_calendar": self.data.session_calendar,
                "start": self.data.start,
                "end": self.data.end,
                "adjustment_policy": self.data.adjustment_policy,
            },
            "execution": {
                "ref": str(self.execution.ref),
                "starting_cash": str(self.execution.starting_cash),
                "currency": self.execution.currency,
                "models": [
                    {
                        "kind": model.kind,
                        "model": model.model,
                        "parameters": dict(model.parameters),
                    }
                    for model in self.execution.models
                ],
            },
        }


class FileDataTradingContextCatalog:
    """Append-only composite index; immutable specs remain in FileObjectStore."""

    def __init__(self, root: Path | str):
        self.root = Path(root).resolve() / "data-trading-contexts"
        self.root.mkdir(parents=True, exist_ok=True)

    def _path(self, ref: ContentAddress) -> Path:
        if not isinstance(ref, ContentAddress) or ref.kind != DATA_TRADING_CONTEXT_KIND:
            raise DataTradingContextError("context_ref must reference data-trading-context")
        return self.root / f"{ref.sha256}.json"

    @staticmethod
    def _payload(context: DataTradingContextV1) -> dict[str, str]:
        return {
            "schema": DATA_TRADING_CONTEXT_SCHEMA,
            "context_ref": str(context.ref),
            "data_ref": str(context.data.ref),
            "execution_ref": str(context.execution.ref),
        }

    def put(self, context: DataTradingContextV1) -> ContentAddress:
        payload = self._payload(context)
        encoded = canonical_json_bytes(payload)
        target = self._path(context.ref)
        if target.exists():
            try:
                existing = target.read_bytes()
            except OSError as exc:
                raise DataTradingContextStateError("unable to read existing context record") from exc
            if existing != encoded:
                raise DataTradingContextStateError("existing context record disagrees with its content identity")
            return context.ref

        fd, temporary_name = tempfile.mkstemp(
            prefix=f".{context.ref.sha256}.",
            suffix=".tmp",
            dir=target.parent,
        )
        temporary = Path(temporary_name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(encoded)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temporary, target)
        finally:
            if temporary.exists():
                temporary.unlink()
        return context.ref

    def read_refs(self, ref: ContentAddress) -> tuple[ContentAddress, ContentAddress]:
        target = self._path(ref)
        try:
            payload = canonical_json_loads(target.read_bytes())
        except FileNotFoundError as exc:
            raise KeyError(ref) from exc
        except (OSError, ValueError) as exc:
            raise DataTradingContextStateError("invalid data/trading-context record") from exc
        if not isinstance(payload, Mapping):
            raise DataTradingContextStateError("invalid data/trading-context record")
        try:
            stored_ref = ContentAddress.parse(payload["context_ref"])
            data_ref = ContentAddress.parse(payload["data_ref"])
            execution_ref = ContentAddress.parse(payload["execution_ref"])
        except (KeyError, TypeError, ValueError) as exc:
            raise DataTradingContextStateError("invalid data/trading-context identity") from exc
        if payload.get("schema") != DATA_TRADING_CONTEXT_SCHEMA or stored_ref != ref:
            raise DataTradingContextStateError("data/trading-context record identity mismatch")
        if data_ref.kind != "data" or execution_ref.kind != "execution":
            raise DataTradingContextStateError("data/trading-context record has invalid child refs")
        expected = content_address(
            DATA_TRADING_CONTEXT_KIND,
            1,
            {"data_ref": str(data_ref), "execution_ref": str(execution_ref)},
        )
        if expected != ref:
            raise DataTradingContextStateError("data/trading-context composite identity mismatch")
        return data_ref, execution_ref

    def list_refs(self) -> tuple[ContentAddress, ...]:
        refs: list[ContentAddress] = []
        for path in sorted(self.root.glob("*.json")):
            digest = path.stem
            try:
                ref = ContentAddress(DATA_TRADING_CONTEXT_KIND, 1, digest)
            except ValueError as exc:
                raise DataTradingContextStateError("invalid data/trading-context filename") from exc
            self.read_refs(ref)
            refs.append(ref)
        return tuple(refs)


class DataTradingContextServiceV1:
    """Persist, reopen, and enumerate exact product-owned research contexts."""

    def __init__(self, state_root: Path | str):
        self.store = FileObjectStore(state_root)
        self.catalog = FileDataTradingContextCatalog(state_root)

    def create(self, config: DataTradingContextConfigV1) -> DataTradingContextV1:
        if not isinstance(config, DataTradingContextConfigV1):
            raise DataTradingContextError("config must be DataTradingContextConfigV1")
        data, execution = config.build_specs()
        if self.store.put(data) != data.ref:
            raise DataTradingContextStateError("store returned the wrong data ref")
        if self.store.put(execution) != execution.ref:
            raise DataTradingContextStateError("store returned the wrong execution ref")
        context = DataTradingContextV1(data, execution)
        self.catalog.put(context)
        return self.read(context.ref)

    def read(self, context_ref: ContentAddress) -> DataTradingContextV1:
        data_ref, execution_ref = self.catalog.read_refs(context_ref)
        try:
            data = self.store.resolve(data_ref)
            execution = self.store.resolve(execution_ref)
        except KeyError as exc:
            raise DataTradingContextStateError("data/trading-context child object is missing") from exc
        if not isinstance(data, DataSpecV1) or data.ref != data_ref:
            raise DataTradingContextStateError("data/trading-context data custody is invalid")
        if not isinstance(execution, ExecutionSpecV1) or execution.ref != execution_ref:
            raise DataTradingContextStateError("data/trading-context execution custody is invalid")
        context = DataTradingContextV1(data, execution)
        if context.ref != context_ref:
            raise DataTradingContextStateError("reopened data/trading-context identity changed")
        return context

    def list(self) -> tuple[DataTradingContextV1, ...]:
        return tuple(self.read(ref) for ref in self.catalog.list_refs())
