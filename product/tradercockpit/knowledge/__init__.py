"""Curated Quant-Guild catalog retrieval for the bounded Assistant.

The knowledge library is reference data: public lecture titles, source URLs, and
platform-authored cockpit notes. Production must not import Quant-Guild-Library
(or any notebook/Python from that repository), and this catalog must not store
lecture transcripts or notebook text.
"""

from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any


KNOWLEDGE_SCHEMA = "tc.knowledge-catalog.v1"
KNOWLEDGE_LIBRARY = "quant-guild"
KNOWLEDGE_SOURCE = "https://github.com/romanmichaelpaolucci/Quant-Guild-Library"
KNOWLEDGE_ROOT_ENV = "TRADERCOCKPIT_KNOWLEDGE_ROOT"
DEFAULT_CATALOG_NAME = "quant_guild_catalog.json"
MAX_PASSAGES = 3
MAX_SUMMARY_CHARS = 400
MAX_GROUNDING_CHARS = 4000

_TOKEN_RE = re.compile(r"[a-z0-9]{3,}")
_STOP = frozenset(
    {
        "the",
        "and",
        "for",
        "with",
        "that",
        "this",
        "from",
        "you",
        "your",
        "are",
        "was",
        "were",
        "how",
        "why",
        "what",
        "when",
        "who",
        "can",
        "not",
        "dont",
        "into",
        "over",
        "under",
        "about",
        "just",
        "more",
        "most",
        "than",
        "then",
        "quant",
        "video",
        "lecture",
        "lectures",
        "watch",
        "until",
        "need",
        "does",
        "dont",
    }
)


def default_catalog_path() -> Path:
    return Path(__file__).resolve().parent / DEFAULT_CATALOG_NAME


def _environ(environ: dict[str, str] | None) -> dict[str, str]:
    return os.environ if environ is None else environ  # type: ignore[return-value]


def _tokens(text: str) -> set[str]:
    return {token for token in _TOKEN_RE.findall(text.lower()) if token not in _STOP}


def _as_entry(raw: object) -> dict[str, object] | None:
    if not isinstance(raw, dict):
        return None
    identity = raw.get("id")
    title = raw.get("title")
    summary = raw.get("summary")
    url = raw.get("source_url")
    if not isinstance(identity, str) or not identity.strip():
        return None
    if not isinstance(title, str) or not title.strip():
        return None
    if not isinstance(summary, str) or not summary.strip():
        return None
    if not isinstance(url, str) or not url.strip():
        return None
    tags = raw.get("tags")
    kept_tags = [item.strip() for item in tags if isinstance(item, str) and item.strip()] if isinstance(tags, list) else []
    year = raw.get("year") if isinstance(raw.get("year"), int) else None
    lecture = raw.get("lecture") if isinstance(raw.get("lecture"), int) else None
    return {
        "id": identity.strip(),
        "title": title.strip(),
        "summary": summary.strip()[:MAX_SUMMARY_CHARS],
        "source_url": url.strip(),
        "tags": kept_tags,
        "year": year,
        "lecture": lecture,
    }


def _empty_catalog(path: Path, reason_code: str) -> dict[str, Any]:
    return {
        "schema": KNOWLEDGE_SCHEMA,
        "library": KNOWLEDGE_LIBRARY,
        "source": KNOWLEDGE_SOURCE,
        "entries": [],
        "reason_code": reason_code,
        "path": str(path),
    }


def _read_catalog_file(path: Path) -> dict[str, Any]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError):
        return _empty_catalog(path, "knowledge_catalog_unavailable")
    if not isinstance(payload, dict) or payload.get("schema") != KNOWLEDGE_SCHEMA:
        return _empty_catalog(path, "knowledge_catalog_invalid")
    entries = [_as_entry(item) for item in payload.get("entries") or []]
    kept = [item for item in entries if item is not None]
    source = payload.get("source") if isinstance(payload.get("source"), str) and payload.get("source").strip() else KNOWLEDGE_SOURCE
    return {
        "schema": KNOWLEDGE_SCHEMA,
        "library": payload.get("library") if isinstance(payload.get("library"), str) else KNOWLEDGE_LIBRARY,
        "source": source,
        "entries": kept,
        "reason_code": None if kept else "knowledge_catalog_empty",
        "path": str(path),
    }


def _merge_entries(*groups: list[dict[str, object]]) -> list[dict[str, object]]:
    merged: dict[str, dict[str, object]] = {}
    for group in groups:
        for entry in group:
            merged[str(entry["id"])] = entry
    return [merged[key] for key in sorted(merged)]


def _extra_catalogs(root: Path) -> list[dict[str, object]]:
    if not root.is_dir():
        return []
    entries: list[dict[str, object]] = []
    for path in sorted(root.glob("*.json")):
        loaded = _read_catalog_file(path)
        entries.extend(loaded["entries"])
    return entries


def load_catalog(*, environ: dict[str, str] | None = None, catalog_path: Path | str | None = None) -> dict[str, Any]:
    """Load the packaged catalog plus optional operator JSON. Missing bytes fail closed."""

    if catalog_path is not None:
        return _read_catalog_file(Path(catalog_path))
    packaged_path = default_catalog_path()
    packaged = _read_catalog_file(packaged_path)
    extra_root = (_environ(environ).get(KNOWLEDGE_ROOT_ENV) or "").strip()
    extra_entries = _extra_catalogs(Path(extra_root)) if extra_root else []
    entries = _merge_entries(packaged["entries"], extra_entries)
    if not entries:
        return packaged if packaged["reason_code"] else _empty_catalog(packaged_path, "knowledge_catalog_empty")
    return {
        "schema": KNOWLEDGE_SCHEMA,
        "library": KNOWLEDGE_LIBRARY,
        "source": packaged.get("source") or KNOWLEDGE_SOURCE,
        "entries": entries,
        "reason_code": None,
        "path": str(packaged_path),
    }


def knowledge_status(*, environ: dict[str, str] | None = None, catalog_path: Path | str | None = None) -> dict[str, object]:
    """Secret-free readiness for `/api/status` and `/api/assistant`."""

    catalog = load_catalog(environ=environ, catalog_path=catalog_path)
    ready = bool(catalog["entries"])
    return {
        "library": KNOWLEDGE_LIBRARY,
        "status": "ready" if ready else "unavailable",
        "reason_code": None if ready else catalog["reason_code"],
        "entry_count": len(catalog["entries"]),
        "source": catalog["source"],
        "detail": (
            f"Quant-Guild catalog ready ({len(catalog['entries'])} lecture references)."
            if ready
            else "Quant-Guild catalog is not connected; do not invent library content."
        ),
    }


def _query_text(query: str, history: object) -> str:
    parts = [query]
    if isinstance(history, list):
        for item in reversed(history):
            if isinstance(item, dict) and item.get("role") == "user" and isinstance(item.get("content"), str):
                parts.append(item["content"])
                break
    return " ".join(parts)


def retrieve_passages(
    query: str,
    *,
    history: object = None,
    environ: dict[str, str] | None = None,
    catalog_path: Path | str | None = None,
    limit: int = MAX_PASSAGES,
) -> list[dict[str, object]]:
    """Return the highest-overlap catalog notes for a user message."""

    if not isinstance(query, str) or not query.strip():
        return []
    catalog = load_catalog(environ=environ, catalog_path=catalog_path)
    query_tokens = _tokens(_query_text(query, history))
    if not query_tokens or not catalog["entries"]:
        return []
    ranked: list[tuple[int, str, dict[str, object]]] = []
    for entry in catalog["entries"]:
        title_tokens = _tokens(str(entry["title"]))
        tag_tokens = _tokens(" ".join(str(tag) for tag in entry["tags"]))
        summary_tokens = _tokens(str(entry["summary"]))
        title_hits = query_tokens & title_tokens
        tag_hits = query_tokens & tag_tokens
        summary_hits = query_tokens & summary_tokens
        if not (title_hits or tag_hits):
            continue
        score = (3 * len(title_hits)) + (2 * len(tag_hits)) + len(summary_hits)
        ranked.append((score, str(entry["id"]), entry))
    ranked.sort(key=lambda item: (-item[0], item[1]))
    return [item[2] for item in ranked[: max(1, min(limit, MAX_PASSAGES))]]


def citation_record(entry: dict[str, object]) -> dict[str, object]:
    return {
        "id": entry["id"],
        "title": entry["title"],
        "year": entry.get("year"),
        "lecture": entry.get("lecture"),
        "source_url": entry["source_url"],
    }


def retrieve_knowledge(
    query: str,
    *,
    history: object = None,
    environ: dict[str, str] | None = None,
    catalog_path: Path | str | None = None,
    limit: int = MAX_PASSAGES,
) -> dict[str, object]:
    """Typed retrieval record attached to assistant replies."""

    catalog = load_catalog(environ=environ, catalog_path=catalog_path)
    if catalog["reason_code"]:
        return {
            "state": "unavailable",
            "reason_code": catalog["reason_code"],
            "library": KNOWLEDGE_LIBRARY,
            "passages": [],
            "citations": [],
        }
    passages = retrieve_passages(
        query,
        history=history,
        environ=environ,
        catalog_path=catalog_path,
        limit=limit,
    )
    if not passages:
        return {
            "state": "idle",
            "reason_code": "no_matching_passages",
            "library": KNOWLEDGE_LIBRARY,
            "passages": [],
            "citations": [],
        }
    return {
        "state": "grounded",
        "reason_code": None,
        "library": KNOWLEDGE_LIBRARY,
        "passages": passages,
        "citations": [citation_record(item) for item in passages],
    }


def format_grounding(retrieval: dict[str, object]) -> str:
    if retrieval.get("state") == "unavailable":
        return "Quant-Guild knowledge library is not connected; do not invent library content."
    passages = retrieval.get("passages") if isinstance(retrieval.get("passages"), list) else []
    if not passages:
        return (
            "Quant-Guild knowledge: no matching catalog note for this message. "
            "Do not invent Quant-Guild formulas, proofs, or lecture claims."
        )
    lines = [
        "Quant-Guild catalog notes (platform-authored cockpit notes, not lecture transcripts, not producer truth; cite the lecture title and URL if used):",
    ]
    used = 0
    for index, passage in enumerate(passages, start=1):
        if not isinstance(passage, dict):
            continue
        block = f"[{index}] {passage.get('title')} ({passage.get('source_url')})\n{passage.get('summary')}"
        if used + len(block) > MAX_GROUNDING_CHARS:
            break
        lines.append(block)
        used += len(block)
    return "\n\n".join(lines)


def knowledge_reply_record(retrieval: dict[str, object]) -> dict[str, object]:
    return {
        "state": retrieval.get("state"),
        "reason_code": retrieval.get("reason_code"),
        "library": retrieval.get("library") or KNOWLEDGE_LIBRARY,
        "citations": retrieval.get("citations") if isinstance(retrieval.get("citations"), list) else [],
    }
