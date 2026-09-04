"""Typed Idea identities for Recent work.

Lists current Ideas whose draft object_kind is indicator, strategy, or model.
This is not a last-route restore and not a Candidate or quantitative ranking.
"""

from __future__ import annotations

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_ideas import ResearchIdeaError, current_idea_entities, read_current_idea


RECENT_WORK_SCHEMA = "tc.recent-work.v1"
RESEARCH_RECENT_WORK_API_PATH = "/api/research/recent-work"
TYPED_OBJECT_KINDS = frozenset({"indicator", "strategy", "model"})
# ponytail: rail density; raise if a machine keeps more typed Ideas than this
RECENT_WORK_LIMIT = 12


def _object_kind(record: dict[str, object]) -> str | None:
    draft = record.get("draft")
    if not isinstance(draft, dict):
        return None
    kind = draft.get("object_kind")
    if kind in TYPED_OBJECT_KINDS:
        return str(kind)
    return None


def _summary(text: object) -> str:
    first_line = next((line.strip() for line in str(text).splitlines() if line.strip()), "Untitled idea")
    return first_line[:120]


def _idea_path(entity_id: str) -> str:
    return f"/research?workspace=signals&tab=overview&idea={entity_id}"


def list_recent_work(store: FileResearchCustodyStore) -> dict[str, object]:
    ranked: list[tuple[float, dict[str, object]]] = []
    for entity in current_idea_entities(store):
        record = read_current_idea(store, entity)
        kind = _object_kind(record)
        if kind is None:
            continue
        entity_id = str(record["entity_id"])
        ranked.append(
            (
                store._current_path(entity).stat().st_mtime,  # ponytail: pointer mtime is recency; do not leak it
                {
                    "entity_id": entity_id,
                    "revision": str(record["revision"]),
                    "object_kind": kind,
                    "summary": _summary(record["text"]),
                    "path": _idea_path(entity_id),
                },
            )
        )
    ranked.sort(key=lambda item: item[0], reverse=True)
    return {
        "schema": RECENT_WORK_SCHEMA,
        "items": [item for _, item in ranked[:RECENT_WORK_LIMIT]],
    }


def research_recent_work_response(
    research_store: FileResearchCustodyStore | None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        return 200, list_recent_work(research_store)
    except ResearchIdeaError as exc:
        status = 409 if exc.code == "idea_content_corrupt" else 400
        return status, {
            "error": "invalid_state" if status == 409 else "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
