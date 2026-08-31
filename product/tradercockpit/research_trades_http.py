"""HTTP-neutral adapter for exact Backtest Trades readback."""

from __future__ import annotations

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_retester import ResearchRetesterError
from tradercockpit.research_trades import ResearchTradesError, read_historical_trades


RESEARCH_HISTORICAL_TRADES_API_PATH = "/api/research/historical-trades"


def historical_trades_response(
    research_store: FileResearchCustodyStore | None,
    *,
    historical_result_entity_id: str | None,
    expected_historical_result_revision: str | None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    if not historical_result_entity_id or not expected_historical_result_revision:
        return 400, {
            "error": "invalid_request",
            "reason_code": "historical_trades_selector_missing",
            "detail": "Trades readback requires exact Historical Result entity and expected revision selectors.",
        }
    try:
        return 200, read_historical_trades(
            research_store,
            historical_result_entity_id=historical_result_entity_id,
            expected_historical_result_revision=expected_historical_result_revision,
        )
    except ResearchTradesError as exc:
        status = 400 if exc.code in {
            "historical_trades_entity_invalid",
            "historical_trades_revision_invalid",
        } else 409
        return status, {
            "error": "invalid_request" if status == 400 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchRetesterError as exc:
        return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}
    except ResearchCustodyError as exc:
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
