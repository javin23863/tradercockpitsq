"""HTTP boundary for capability/add-on registry writes."""

from __future__ import annotations

from pathlib import Path

from tradercockpit.extensions import EXTENSIONS_MANIFEST_NAME, ExtensionsError, register_addon_slot
from tradercockpit.research_custody import FileResearchCustodyStore


EXTENSIONS_API_PATH = "/api/extensions"


def extensions_register_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Add-on registration requires the application data root.",
        }
    try:
        record = register_addon_slot(research_store.root, payload)
        return 201, record
    except ExtensionsError as exc:
        error = "invalid_state" if exc.status == 409 else "invalid_request"
        return exc.status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def extensions_manifest_relative_path() -> str:
    return EXTENSIONS_MANIFEST_NAME
