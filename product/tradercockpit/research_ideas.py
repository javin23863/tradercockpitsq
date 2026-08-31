"""Research Construct/Idea custody on the canonical research store.

Idea content is product-authored source/provenance text only. This module does not
parse strategy meaning, mint candidates, or authorize native execution.
"""

from __future__ import annotations

from dataclasses import dataclass
import json

from tradercockpit.research_custody import (
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
)


IDEA_CONTENT_SCHEMA = "tc.research-idea-content.v1"
IDEA_READ_SCHEMA = "tc.research-idea.v1"
IDEA_CATALOG_SCHEMA = "tc.research-idea-catalog.v1"
MAX_IDEA_TEXT_CHARS = 100_000
MAX_IDEA_SOURCE_CHARS = 20_000


class ResearchIdeaError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class ResearchIdeaContent:
    text: str
    source: str

    def __post_init__(self) -> None:
        if not isinstance(self.text, str) or not self.text.strip():
            raise ResearchIdeaError("idea_text_invalid", "idea text must be a non-empty string")
        if len(self.text) > MAX_IDEA_TEXT_CHARS:
            raise ResearchIdeaError("idea_text_too_large", "idea text exceeds the supported size")
        if not isinstance(self.source, str):
            raise ResearchIdeaError("idea_source_invalid", "idea source must be a string")
        if len(self.source) > MAX_IDEA_SOURCE_CHARS:
            raise ResearchIdeaError("idea_source_too_large", "idea source exceeds the supported size")

    def canonical_bytes(self) -> bytes:
        return json.dumps(
            {
                "schema": IDEA_CONTENT_SCHEMA,
                "source": self.source,
                "text": self.text,
            },
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")

    @classmethod
    def from_bytes(cls, data: bytes) -> "ResearchIdeaContent":
        try:
            payload = json.loads(data)
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ResearchIdeaError("idea_content_corrupt", "idea content is not valid JSON") from exc
        if (
            not isinstance(payload, dict)
            or set(payload) != {"schema", "source", "text"}
            or payload.get("schema") != IDEA_CONTENT_SCHEMA
        ):
            raise ResearchIdeaError("idea_content_corrupt", "idea content schema is invalid")
        try:
            return cls(text=payload["text"], source=payload["source"])
        except ResearchIdeaError as exc:
            raise ResearchIdeaError("idea_content_corrupt", exc.detail) from exc


def _idea_entity(value: ResearchEntityId | str) -> ResearchEntityId:
    entity = value if isinstance(value, ResearchEntityId) else ResearchEntityId.parse(value)
    if entity.kind != ResearchKind.IDEA:
        raise ResearchIdeaError("idea_entity_invalid", "research entity is not an Idea")
    return entity


def _idea_revision(value: ResearchRevisionRef | str) -> ResearchRevisionRef:
    revision = value if isinstance(value, ResearchRevisionRef) else ResearchRevisionRef.parse(value)
    if revision.kind != ResearchKind.IDEA:
        raise ResearchIdeaError("idea_revision_invalid", "research revision is not an Idea revision")
    return revision


def _record(
    store: FileResearchCustodyStore,
    entity: ResearchEntityId,
    revision: ResearchRevisionRef,
) -> dict[str, object]:
    stored_revision = store.read_revision(revision)
    if stored_revision.entity_id != entity:
        raise ResearchIdeaError("idea_revision_invalid", "Idea revision belongs to another entity")
    content = ResearchIdeaContent.from_bytes(store.read_revision_content(revision))
    return {
        "schema": IDEA_READ_SCHEMA,
        "entity_id": str(entity),
        "revision": str(revision),
        "parent_revision": str(stored_revision.parent_revision) if stored_revision.parent_revision else None,
        "content_ref": str(stored_revision.content),
        "text": content.text,
        "source": content.source,
    }


def read_current_idea(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
) -> dict[str, object]:
    entity = _idea_entity(entity_id)
    return _record(store, entity, store.current(entity))


def list_current_ideas(store: FileResearchCustodyStore) -> dict[str, object]:
    ideas: list[dict[str, object]] = []
    for entity in store.list_entities(ResearchKind.IDEA):
        record = read_current_idea(store, entity)
        text = str(record["text"])
        first_line = next((line.strip() for line in text.splitlines() if line.strip()), "Untitled idea")
        ideas.append(
            {
                "entity_id": record["entity_id"],
                "revision": record["revision"],
                "summary": first_line[:120],
            }
        )
    return {
        "schema": IDEA_CATALOG_SCHEMA,
        "ideas": ideas,
    }


def create_idea(
    store: FileResearchCustodyStore,
    *,
    text: str,
    source: str = "",
) -> dict[str, object]:
    content = ResearchIdeaContent(text=text, source=source)
    entity = store.create_entity(ResearchKind.IDEA)
    revision = store.create_revision(entity, content.canonical_bytes())
    store.compare_and_set_current(
        entity,
        expected_revision=None,
        target_revision=revision.revision,
    )
    return _record(store, entity, revision.revision)


def revise_idea(
    store: FileResearchCustodyStore,
    *,
    entity_id: ResearchEntityId | str,
    expected_revision: ResearchRevisionRef | str,
    text: str,
    source: str = "",
) -> dict[str, object]:
    entity = _idea_entity(entity_id)
    expected = _idea_revision(expected_revision)
    current = store.current(entity)
    if current != expected:
        raise ResearchCustodyError("current_conflict", "Idea revision changed before save")

    content = ResearchIdeaContent(text=text, source=source)
    revision = store.create_revision(
        entity,
        content.canonical_bytes(),
        parent_revision=expected,
    )
    store.compare_and_set_current(
        entity,
        expected_revision=expected,
        target_revision=revision.revision,
    )
    return _record(store, entity, revision.revision)
