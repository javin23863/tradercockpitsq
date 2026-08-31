"""Producer-backed Backtest Trades read model.

Trades are read only from the immutable native SQX result archive already bound to
one completed Historical Result revision.  The SQX orders contract remains the
producer authority; TraderCockpit adds exact custody binding and no trade synthesis.
"""

from __future__ import annotations

from hashlib import sha256

from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchKind,
    ResearchRevisionRef,
)
from tradercockpit.research_retester import ResearchRetesterError, read_current_historical_result
from tradercockpit.sqx_orders import SqxOrdersError, inspect_sqx_orders_bytes


RESEARCH_TRADES_SCHEMA = "tc.research-historical-trades.v1"


class ResearchTradesError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _expected_revision(value: str) -> ResearchRevisionRef:
    try:
        revision = ResearchRevisionRef.parse(value)
    except ResearchCustodyError as exc:
        raise ResearchTradesError("historical_trades_revision_invalid", "Historical Result revision identity is invalid") from exc
    if revision.kind != ResearchKind.HISTORICAL_RESULT:
        raise ResearchTradesError("historical_trades_revision_invalid", "Trades require a Historical Result revision")
    return revision


def read_historical_trades(
    store: FileResearchCustodyStore,
    *,
    historical_result_entity_id: str,
    expected_historical_result_revision: str,
) -> dict[str, object]:
    if not isinstance(store, FileResearchCustodyStore):
        raise ResearchTradesError("historical_trades_store_invalid", "canonical research custody store is required")
    if not isinstance(historical_result_entity_id, str) or not historical_result_entity_id:
        raise ResearchTradesError("historical_trades_entity_invalid", "Historical Result entity identity is required")
    if not isinstance(expected_historical_result_revision, str) or not expected_historical_result_revision:
        raise ResearchTradesError("historical_trades_revision_invalid", "expected Historical Result revision is required")

    expected = _expected_revision(expected_historical_result_revision)
    result = read_current_historical_result(store, historical_result_entity_id)
    if result.get("revision") != str(expected):
        raise ResearchTradesError(
            "historical_trades_revision_conflict",
            "Historical Result revision changed before Trades readback",
        )
    if result.get("state") != "completed" or result.get("execution_completed") is not True:
        raise ResearchTradesError(
            "historical_trades_result_incomplete",
            "Trades require one completed native Retester Historical Result",
        )

    archive_ref_value = result.get("result_archive_ref")
    archive_sha = result.get("result_archive_sha256")
    try:
        archive_ref = EvidenceRef.parse(archive_ref_value)  # type: ignore[arg-type]
    except ResearchCustodyError as exc:
        raise ResearchTradesError("historical_trades_archive_invalid", "Historical Result archive evidence ref is invalid") from exc
    if archive_ref.digest != archive_sha:
        raise ResearchTradesError("historical_trades_archive_invalid", "Historical Result archive evidence binding is inconsistent")

    snapshot = store.read_evidence(archive_ref)
    if sha256(snapshot).hexdigest() != archive_sha:
        raise ResearchTradesError("historical_trades_archive_invalid", "Historical Result archive bytes changed in custody")
    try:
        orders = inspect_sqx_orders_bytes(snapshot)
    except SqxOrdersError as exc:
        raise ResearchTradesError(exc.code, exc.detail) from exc

    return {
        "schema": RESEARCH_TRADES_SCHEMA,
        "historical_result_entity_id": result["entity_id"],
        "historical_result_revision": result["revision"],
        "candidate_entity_id": result["candidate_entity_id"],
        "candidate_revision": result["candidate_revision"],
        "result_archive_ref": result["result_archive_ref"],
        "result_archive_sha256": result["result_archive_sha256"],
        **orders,
    }
