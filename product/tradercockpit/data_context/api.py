"""HTTP-neutral API contract for canonical data/trading contexts."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from tradercockpit.domain import ContentAddress
from tradercockpit.storage import ContentStoreError

from .service import (
    DATA_TRADING_CONTEXT_LIST_SCHEMA,
    DataTradingContextConfigV1,
    DataTradingContextError,
    DataTradingContextServiceV1,
    DataTradingContextStateError,
)

DATA_CONTEXTS_API_PATH = "/api/data-contexts"
DATA_CONTEXT_READ_API_PATH = "/api/data-contexts/read"


def _service(state_root: Path | str | None) -> DataTradingContextServiceV1:
    if state_root is None:
        raise FileNotFoundError("TraderCockpit state root is not configured")
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError("TraderCockpit state root does not exist")
    return DataTradingContextServiceV1(root)


def data_context_create_response(
    state_root: Path | str | None,
    request: object,
) -> tuple[int, dict[str, Any]]:
    try:
        config = DataTradingContextConfigV1.from_request(request)
        context = _service(state_root).create(config)
        return 201, context.record()
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (DataTradingContextError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except (DataTradingContextStateError, ContentStoreError) as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def data_context_read_response(
    state_root: Path | str | None,
    context_ref_text: str,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(context_ref_text, str) or not context_ref_text:
            raise DataTradingContextError("contextRef must be a non-empty content address")
        ref = ContentAddress.parse(context_ref_text)
        if ref.kind != "data-trading-context":
            raise DataTradingContextError("contextRef must reference data-trading-context")
        return 200, _service(state_root).read(ref).record()
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except KeyError:
        return 404, {"error": "not_found", "detail": "Data/trading context was not found"}
    except (DataTradingContextError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except (DataTradingContextStateError, ContentStoreError) as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def data_context_list_response(
    state_root: Path | str | None,
) -> tuple[int, dict[str, Any]]:
    try:
        contexts = _service(state_root).list()
        return 200, {
            "schema": DATA_TRADING_CONTEXT_LIST_SCHEMA,
            "contexts": [context.record() for context in contexts],
        }
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (DataTradingContextStateError, ContentStoreError) as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}
