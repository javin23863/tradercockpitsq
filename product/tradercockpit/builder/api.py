"""HTTP-neutral Builder/evolution API contract.

This module intentionally does not register routes in ``app_server.py``. Recovery
Vertical 1 currently occupies that shared server surface; the functions here are
the stable Builder contract to wire after rebasing onto the accepted server head.
"""

from __future__ import annotations

from decimal import Decimal
from pathlib import Path
from threading import Lock
from typing import Any, Mapping

from tradercockpit.domain import ContentAddress, content_address
from tradercockpit.storage import ContentStoreError

from .runtime import BUILDER_SEARCH_IMPLEMENTATION, BuilderRuntimeSearchService
from .search import BuilderSearchConfigV1, BuilderSearchError


BUILDER_SEARCH_START_API_PATH = "/api/builder-searches"
BUILDER_SEARCH_READ_API_PATH = "/api/builder-searches/read"
BUILDER_CANDIDATES_API_PATH = "/api/builder-candidates"

# The canonical product server is threaded. Deterministic searches with identical
# implementation/request/config identity must not interleave writes to one mutable
# search-state record. A bounded stripe set avoids an unbounded lock registry while
# still serializing every identical search within the process that owns the server.
_SEARCH_START_LOCKS = tuple(Lock() for _ in range(64))


def _service(state_root: Path | str | None) -> BuilderRuntimeSearchService:
    if state_root is None:
        raise FileNotFoundError("TraderCockpit state root is not configured")
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError("TraderCockpit state root does not exist")
    return BuilderRuntimeSearchService(root)


def _search_ref(requested_strategy_ref: str, config: BuilderSearchConfigV1) -> ContentAddress:
    return content_address(
        "builder-search",
        1,
        {
            "implementation": BUILDER_SEARCH_IMPLEMENTATION,
            "requested_strategy_ref": requested_strategy_ref,
            "config_ref": str(config.ref),
        },
    )


def _start_lock(search_ref: ContentAddress) -> Lock:
    index = int(search_ref.sha256[:8], 16) % len(_SEARCH_START_LOCKS)
    return _SEARCH_START_LOCKS[index]


def _run_or_reuse_search(
    service: BuilderRuntimeSearchService,
    requested_strategy_ref: str,
    config: BuilderSearchConfigV1,
) -> dict[str, Any]:
    search_ref = _search_ref(requested_strategy_ref, config)
    with _start_lock(search_ref):
        try:
            existing = service.read(search_ref)
        except KeyError:
            return service.run(requested_strategy_ref, config)

        if existing.get("status") == "complete":
            return existing
        raise ContentStoreError(
            "Builder search has durable incomplete state; refusing to overwrite or restart it implicitly"
        )


def builder_search_start_response(
    state_root: Path | str | None,
    request: object,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(request, Mapping):
            raise BuilderSearchError("request body must be an object")
        allowed = {"strategyRef", "config"}
        unknown = sorted(set(request) - allowed)
        if unknown:
            raise BuilderSearchError("unknown request fields: " + ", ".join(unknown))
        strategy_ref = request.get("strategyRef")
        if not isinstance(strategy_ref, str) or not strategy_ref.strip():
            raise BuilderSearchError("strategyRef must be a non-empty string")
        config = BuilderSearchConfigV1.from_request(request.get("config"))
        service = _service(state_root)
        return 201, _run_or_reuse_search(service, strategy_ref, config)
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (BuilderSearchError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except ContentStoreError as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def builder_search_read_response(
    state_root: Path | str | None,
    search_ref_text: str,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(search_ref_text, str) or not search_ref_text:
            raise BuilderSearchError("searchRef must be a non-empty content address")
        search_ref = ContentAddress.parse(search_ref_text)
        if search_ref.kind != "builder-search":
            raise BuilderSearchError("searchRef must reference builder-search")
        return 200, _service(state_root).read(search_ref)
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except KeyError:
        return 404, {"error": "not_found", "detail": "Builder search was not found"}
    except (BuilderSearchError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except ContentStoreError as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


def builder_candidates_response(
    state_root: Path | str | None,
    requested_strategy_ref: str,
) -> tuple[int, dict[str, Any]]:
    try:
        if not isinstance(requested_strategy_ref, str) or not requested_strategy_ref.strip():
            raise BuilderSearchError("strategyRef must be a non-empty string")
        searches = _service(state_root).list_for_strategy(requested_strategy_ref)
        candidates: dict[str, dict[str, Any]] = {}
        for search in searches:
            for candidate in search["candidates"]:
                candidates[candidate["candidate_ref"]] = {
                    **candidate,
                    "search_ref": search["search_ref"],
                    "search_status": search["status"],
                    "config_ref": search["config_ref"],
                }
        ordered = sorted(
            candidates.values(),
            key=lambda item: (
                -Decimal(item["objective_values"]["construction_fit"]),
                item["candidate_ref"],
            ),
        )
        return 200, {
            "schema": "tc.builder-candidates.v1",
            "requested_strategy_ref": requested_strategy_ref,
            "searches": list(searches),
            "candidates": ordered,
        }
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except (BuilderSearchError, ValueError, TypeError) as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except ContentStoreError as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}
