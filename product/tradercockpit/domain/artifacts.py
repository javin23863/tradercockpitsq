"""Immutable result artifacts produced by TraderCockpit engines."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, ClassVar, Mapping

from .canonical import ContentAddress
from .specs import _AddressedSpec, _freeze_object, _require_ref, _require_schema


@dataclass(frozen=True, slots=True)
class ResultArtifactV1(_AddressedSpec):
    """Content-addressed result for one exact run and producer build.

    ``result_schema`` owns the payload vocabulary. Runtime timestamps and
    mutable lifecycle state intentionally do not participate in this artifact,
    allowing deterministic evaluators to reproduce the same result identity.
    """

    KIND: ClassVar[str] = "result"

    run_ref: ContentAddress
    producer_build_ref: ContentAddress
    result_schema: str
    payload: Mapping[str, Any]

    def __post_init__(self) -> None:
        _require_ref(self.run_ref, "backtest-run", "run_ref")
        _require_ref(
            self.producer_build_ref,
            "engine-build",
            "producer_build_ref",
        )
        object.__setattr__(
            self,
            "result_schema",
            _require_schema(self.result_schema, "result_schema"),
        )
        object.__setattr__(
            self,
            "payload",
            _freeze_object(self.payload, "payload"),
        )

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "run_ref": str(self.run_ref),
            "producer_build_ref": str(self.producer_build_ref),
            "result_schema": self.result_schema,
            "payload": self.payload,
        }
