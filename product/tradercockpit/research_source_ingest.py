"""URL and document ingest into Idea custody.

The owner pastes a URL or a document. This module fetches or accepts the bytes, hashes
the exact body, and splits it into quoted spans. Apollo may later draft indicator vs
strategy vs model meaning only from those spans. Clauses that are not verbatim span
substrings are refused. This is not a substitute quantitative engine.
"""

from __future__ import annotations

from html.parser import HTMLParser
from ipaddress import ip_address
import hashlib
import json
import re
import socket
from typing import Callable
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


INGEST_SCHEMA = "tc.research-source-ingest.v1"
DRAFT_SCHEMA = "tc.research-source-draft.v1"
RESEARCH_IDEA_INGEST_API_PATH = "/api/research/ideas/ingest"

MAX_INGEST_CHARS = 100_000
MAX_FETCH_BYTES = 1_000_000
MAX_SPANS = 200
MIN_SPAN_CHARS = 24
FETCH_TIMEOUT_SECONDS = 15
MAX_REDIRECTS = 3
ALLOWED_OBJECT_KINDS = frozenset({"indicator", "strategy", "model", "unresolved"})
ALLOWED_SCHEMES = frozenset({"http", "https"})

Fetch = Callable[[str], tuple[int, dict[str, str], bytes]]


class SourceIngestError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


class _HTMLText(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self._chunks: list[str] = []
        self._skip = False

    def handle_starttag(self, tag: str, attrs) -> None:
        if tag in {"script", "style", "noscript"}:
            self._skip = True
        if tag in {"p", "div", "br", "li", "h1", "h2", "h3", "tr"}:
            self._chunks.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in {"script", "style", "noscript"}:
            self._skip = False
        if tag in {"p", "div", "li", "h1", "h2", "h3"}:
            self._chunks.append("\n\n")

    def handle_data(self, data: str) -> None:
        if not self._skip:
            self._chunks.append(data)

    def text(self) -> str:
        return "".join(self._chunks)


def normalize_source_text(raw: str, *, media_type: str = "text/plain") -> str:
    if not isinstance(raw, str):
        raise SourceIngestError("source_text_invalid", "source text must be a string")
    text = raw.replace("\r\n", "\n").replace("\r", "\n")
    if media_type.startswith("text/html") or "<html" in text[:400].lower() or "<p" in text[:400].lower():
        parser = _HTMLText()
        parser.feed(text)
        parser.close()
        text = parser.text()
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    if not text:
        raise SourceIngestError("source_empty", "ingested source produced no quoted text")
    if len(text) > MAX_INGEST_CHARS:
        raise SourceIngestError("source_too_large", "ingested source exceeds the supported size")
    return text


def quoted_spans_from_text(text: str) -> list[dict[str, object]]:
    """Split normalized text into hashed quoted spans. No semantic inference."""

    if not isinstance(text, str) or not text.strip():
        raise SourceIngestError("source_empty", "ingested source produced no quoted text")
    blocks = [block.strip() for block in re.split(r"\n\s*\n", text) if block.strip()]
    pieces: list[str] = []
    for block in blocks:
        if len(block) <= 800:
            pieces.append(block)
            continue
        sentences = [part.strip() for part in re.split(r"(?<=[.!?])\s+", block) if part.strip()]
        pieces.extend(sentences or [block])

    spans: list[dict[str, object]] = []
    cursor = 0
    for piece in pieces:
        if len(piece) < MIN_SPAN_CHARS and spans:
            continue
        start = text.find(piece, cursor)
        if start < 0:
            start = text.find(piece)
        if start < 0:
            continue
        end = start + len(piece)
        cursor = end
        span_id = f"span-{len(spans) + 1:04d}"
        spans.append(
            {
                "id": span_id,
                "start": start,
                "end": end,
                "sha256": _sha256_text(piece),
                "text": piece,
            }
        )
        if len(spans) >= MAX_SPANS:
            break
    if not spans:
        raise SourceIngestError("source_empty", "ingested source produced no quoted spans")
    return spans


def source_ingest_record(
    *,
    kind: str,
    text: str,
    media_type: str,
    uri: str | None = None,
    filename: str | None = None,
) -> dict[str, object]:
    if kind not in {"url", "document"}:
        raise SourceIngestError("ingest_kind_invalid", "ingest kind must be url or document")
    normalized = normalize_source_text(text, media_type=media_type)
    spans = quoted_spans_from_text(normalized)
    return {
        "schema": INGEST_SCHEMA,
        "kind": kind,
        "uri": uri,
        "filename": filename,
        "media_type": media_type,
        "content_sha256": _sha256_text(normalized),
        "character_count": len(normalized),
        "text": normalized,
        "quoted_spans": spans,
    }


def _host_is_blocked(host: str) -> bool:
    hostname = host.split("%", 1)[0]
    try:
        address = ip_address(hostname)
    except ValueError:
        address = None
    if address is not None:
        return bool(
            address.is_private
            or address.is_loopback
            or address.is_link_local
            or address.is_multicast
            or address.is_reserved
            or address.is_unspecified
        )
    try:
        infos = socket.getaddrinfo(hostname, None)
    except socket.gaierror as exc:
        raise SourceIngestError("url_unresolved", f"source URL host could not be resolved: {hostname}") from exc
    for info in infos:
        raw = info[4][0]
        try:
            address = ip_address(raw.split("%", 1)[0])
        except ValueError:
            continue
        if (
            address.is_private
            or address.is_loopback
            or address.is_link_local
            or address.is_multicast
            or address.is_reserved
            or address.is_unspecified
        ):
            return True
    return False


def validate_public_http_url(value: str, *, resolve_host: bool = True) -> str:
    if not isinstance(value, str) or not value.strip():
        raise SourceIngestError("url_invalid", "source URL must be a non-empty string")
    parsed = urlparse(value.strip())
    if parsed.scheme not in ALLOWED_SCHEMES or not parsed.netloc or parsed.username or parsed.password:
        raise SourceIngestError("url_invalid", "source URL must be a public http(s) URL")
    host = parsed.hostname
    if not host:
        raise SourceIngestError("url_invalid", "source URL must be a public http(s) URL")
    try:
        address = ip_address(host.split("%", 1)[0])
    except ValueError:
        address = None
    if address is not None and (
        address.is_private
        or address.is_loopback
        or address.is_link_local
        or address.is_multicast
        or address.is_reserved
        or address.is_unspecified
    ):
        raise SourceIngestError("url_blocked", "source URL must not target a private or local address")
    if resolve_host and _host_is_blocked(host):
        raise SourceIngestError("url_blocked", "source URL must not target a private or local address")
    return value.strip()


def _default_fetch(url: str) -> tuple[int, dict[str, str], bytes]:
    request = Request(url, method="GET", headers={"user-agent": "TraderCockpit-source-ingest/1.0", "accept": "text/*,application/xhtml+xml"})
    try:
        with urlopen(request, timeout=FETCH_TIMEOUT_SECONDS) as response:
            status = int(getattr(response, "status", 200) or 200)
            headers = {str(key).lower(): str(value) for key, value in response.headers.items()}
            body = response.read(MAX_FETCH_BYTES + 1)
    except HTTPError as exc:
        raise SourceIngestError("url_fetch_failed", f"source URL returned HTTP {exc.code}") from exc
    except URLError as exc:
        raise SourceIngestError("url_fetch_failed", f"source URL could not be fetched: {exc.reason}") from exc
    except TimeoutError as exc:
        raise SourceIngestError("url_fetch_failed", "source URL fetch timed out") from exc
    return status, headers, body


def fetch_url_text(url: str, *, fetch: Fetch | None = None) -> tuple[str, str, str]:
    """Return ``(final_url, media_type, text)`` for a public URL. Fail closed on escape."""

    current = validate_public_http_url(url, resolve_host=fetch is None)
    opener = fetch or _default_fetch
    seen: set[str] = set()
    for _ in range(MAX_REDIRECTS + 1):
        if current in seen:
            raise SourceIngestError("url_fetch_failed", "source URL redirect loop")
        seen.add(current)
        status, headers, body = opener(current)
        location = headers.get("location")
        if status in {301, 302, 303, 307, 308} and location:
            next_url = location if location.startswith("http") else f"{urlparse(current).scheme}://{urlparse(current).netloc}{location}"
            current = validate_public_http_url(next_url, resolve_host=fetch is None)
            continue
        if status >= 400:
            raise SourceIngestError("url_fetch_failed", f"source URL returned HTTP {status}")
        if len(body) > MAX_FETCH_BYTES:
            raise SourceIngestError("source_too_large", "ingested source exceeds the supported size")
        media_type = (headers.get("content-type") or "text/plain").split(";", 1)[0].strip().lower() or "text/plain"
        if media_type not in {"text/plain", "text/markdown", "text/html", "application/xhtml+xml", "text/csv"}:
            raise SourceIngestError("document_type_unsupported", f"media type {media_type} is not an ingestible text document")
        try:
            text = body.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise SourceIngestError("source_text_invalid", "source URL body is not valid UTF-8") from exc
        return current, media_type, text
    raise SourceIngestError("url_fetch_failed", "source URL exceeded redirect limit")


def unavailable_draft(*, reason_code: str, detail: str) -> dict[str, object]:
    return {
        "schema": DRAFT_SCHEMA,
        "status": "unavailable",
        "object_kind": "unresolved",
        "clauses": [],
        "reason_code": reason_code,
        "detail": detail,
    }


def bind_draft_to_spans(
    spans: list[dict[str, object]],
    proposal: dict[str, object] | None,
) -> dict[str, object]:
    """Accept a typed draft only when every clause is a verbatim span substring."""

    if not proposal:
        return unavailable_draft(
            reason_code="draft_not_proposed",
            detail="No typed draft was proposed from the quoted spans.",
        )
    kind = proposal.get("object_kind")
    if kind not in ALLOWED_OBJECT_KINDS:
        raise SourceIngestError("draft_kind_invalid", "draft object kind must be indicator, strategy, model, or unresolved")
    raw_clauses = proposal.get("clauses")
    if raw_clauses is None:
        raw_clauses = []
    if not isinstance(raw_clauses, list):
        raise SourceIngestError("draft_clause_invalid", "draft clauses must be a list")
    by_id = {str(span.get("id")): span for span in spans}
    clauses: list[dict[str, object]] = []
    for item in raw_clauses:
        if not isinstance(item, dict):
            raise SourceIngestError("draft_clause_invalid", "each draft clause must be an object")
        span_id = item.get("span_id")
        text = item.get("text")
        if not isinstance(span_id, str) or span_id not in by_id:
            raise SourceIngestError("draft_span_unknown", "draft clause must cite a quoted span")
        if not isinstance(text, str) or not text.strip():
            raise SourceIngestError("draft_clause_invalid", "draft clause text must be a non-empty string")
        span_text = str(by_id[span_id]["text"])
        if text not in span_text:
            raise SourceIngestError("draft_clause_invented", "draft clause is not a verbatim substring of its quoted span")
        clauses.append(
            {
                "span_id": span_id,
                "text": text,
                "sha256": _sha256_text(text),
            }
        )
    if kind != "unresolved" and not clauses:
        raise SourceIngestError("draft_clause_invalid", "a typed object kind requires at least one quoted clause")
    return {
        "schema": DRAFT_SCHEMA,
        "status": "bound" if clauses else "unavailable",
        "object_kind": kind if clauses else "unresolved",
        "clauses": clauses,
        "reason_code": None if clauses else "no_quoted_clauses",
        "detail": (
            "Typed draft bound to hashed quoted spans."
            if clauses
            else "No quoted clauses were bound; object kind stays unresolved."
        ),
    }


def parse_model_draft_json(raw: str) -> dict[str, object]:
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise SourceIngestError("draft_unreadable", "Apollo draft was not valid JSON") from exc
    if not isinstance(payload, dict):
        raise SourceIngestError("draft_unreadable", "Apollo draft must be a JSON object")
    return payload


def ingest_from_document(*, filename: str, text: str) -> dict[str, object]:
    if not isinstance(filename, str) or not filename.strip():
        raise SourceIngestError("filename_invalid", "document filename must be a non-empty string")
    name = filename.strip()
    lower = name.lower()
    if lower.endswith((".pdf", ".docx", ".doc", ".xlsx", ".xls")):
        raise SourceIngestError("document_type_unsupported", "binary documents are not ingested; paste or supply UTF-8 text")
    media = "text/html" if lower.endswith((".html", ".htm")) else "text/plain"
    return source_ingest_record(kind="document", text=text, media_type=media, filename=name)


def ingest_from_url(url: str, *, fetch: Fetch | None = None) -> dict[str, object]:
    final_url, media_type, text = fetch_url_text(url, fetch=fetch)
    return source_ingest_record(kind="url", text=text, media_type=media_type, uri=final_url)


def persistable_ingest(record: dict[str, object]) -> dict[str, object]:
    """Store hash, spans, and provenance. The full body lives on the Idea text field."""

    return {key: value for key, value in record.items() if key != "text"}


def persist_ingested_idea(
    store,
    ingest: dict[str, object],
    *,
    entity_id: str | None = None,
    expected_revision: str | None = None,
    draft: dict[str, object] | None = None,
) -> dict[str, object]:
    from tradercockpit.research_ideas import create_idea, revise_idea

    text = str(ingest["text"])
    source = str(ingest.get("uri") or ingest.get("filename") or "ingested source")
    stored = persistable_ingest(ingest)
    bound_draft = draft or unavailable_draft(
        reason_code="assistant_not_invoked",
        detail="Quoted spans are stored. Apollo has not bound a typed draft to those spans.",
    )
    if entity_id:
        if not expected_revision:
            raise SourceIngestError("idea_revision_required", "revising an ingested Idea requires expected_revision")
        return revise_idea(
            store,
            entity_id=entity_id,
            expected_revision=expected_revision,
            text=text,
            source=source,
            ingest=stored,
            draft=bound_draft,
        )
    return create_idea(store, text=text, source=source, ingest=stored, draft=bound_draft)


def research_idea_ingest_write(
    research_store,
    payload: dict[str, object],
    *,
    fetch: Fetch | None = None,
) -> tuple[int, dict[str, object]]:
    from tradercockpit.research_custody import ResearchCustodyError
    from tradercockpit.research_ideas import ResearchIdeaError

    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    keys = set(payload)
    allowed = {"url", "filename", "text", "entity_id", "expected_revision", "draft"}
    if not keys or keys - allowed:
        return 400, {"error": "invalid_request", "detail": "Idea ingest accepts url, or filename+text, and optional entity_id/expected_revision/draft."}
    has_url = "url" in payload
    has_document = "filename" in payload or "text" in payload
    if has_url == has_document:
        return 400, {"error": "invalid_request", "detail": "Idea ingest requires either url or filename+text, not both."}
    try:
        if has_url:
            ingest = ingest_from_url(str(payload.get("url") or ""), fetch=fetch)
        else:
            ingest = ingest_from_document(filename=str(payload.get("filename") or ""), text=str(payload.get("text") or ""))
        proposal = payload.get("draft") if isinstance(payload.get("draft"), dict) else None
        draft = bind_draft_to_spans(list(ingest["quoted_spans"]), proposal) if proposal else None
        record = persist_ingested_idea(
            research_store,
            ingest,
            entity_id=str(payload["entity_id"]) if payload.get("entity_id") else None,
            expected_revision=str(payload["expected_revision"]) if payload.get("expected_revision") else None,
            draft=draft,
        )
        return (200 if payload.get("entity_id") else 201), record
    except SourceIngestError as exc:
        return 400, {"error": "invalid_request", "reason_code": exc.code, "detail": exc.detail}
    except ResearchIdeaError as exc:
        return 400, {"error": "invalid_request", "reason_code": exc.code, "detail": exc.detail}
    except ResearchCustodyError as exc:
        if exc.code == "current_conflict":
            return 409, {"error": "conflict", "reason_code": exc.code, "detail": exc.detail}
        if exc.code == "current_pointer_missing":
            return 404, {"error": "not_found", "reason_code": exc.code, "detail": exc.detail}
        return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}
