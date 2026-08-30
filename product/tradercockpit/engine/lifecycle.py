"""Operational lifecycle producer contract for run status."""

from __future__ import annotations

from typing import Protocol, runtime_checkable

from tradercockpit.domain import ContentAddress, RunLifecycleEventV1


@runtime_checkable
class RunLifecycleStoreV1(Protocol):
    """Persist immutable lifecycle events and expose one exact current event."""

    def publish(self, event: RunLifecycleEventV1) -> ContentAddress:
        ...

    def current(
        self,
        run_ref: ContentAddress,
        invocation_id: str,
    ) -> RunLifecycleEventV1:
        ...
