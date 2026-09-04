from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_source_ingest import (
    SourceIngestError,
    bind_draft_to_spans,
    ingest_from_document,
    ingest_from_url,
    quoted_spans_from_text,
    research_idea_ingest_write,
    validate_public_http_url,
)


PAPER = (
    "A mean-reversion strategy buys when the 14-period RSI is below 30.\n\n"
    "It sells when the 14-period RSI is above 70. Costs and session remain unresolved."
)


class SourceIngestTests(unittest.TestCase):
    def test_quoted_spans_are_hashed_verbatim_blocks(self) -> None:
        spans = quoted_spans_from_text(PAPER)
        self.assertGreaterEqual(len(spans), 2)
        self.assertTrue(all(item["id"].startswith("span-") for item in spans))
        self.assertIn("RSI is below 30", spans[0]["text"])
        self.assertEqual(len(spans[0]["sha256"]), 64)

    def test_document_ingest_stores_hash_and_refuses_pdf(self) -> None:
        record = ingest_from_document(filename="note.md", text=PAPER)
        self.assertEqual(record["kind"], "document")
        self.assertEqual(record["filename"], "note.md")
        self.assertEqual(len(record["content_sha256"]), 64)
        self.assertEqual(record["text"], PAPER)
        with self.assertRaises(SourceIngestError) as raised:
            ingest_from_document(filename="paper.pdf", text=PAPER)
        self.assertEqual(raised.exception.code, "document_type_unsupported")

    def test_private_and_local_urls_are_blocked(self) -> None:
        for url in ("http://127.0.0.1/secret", "http://192.168.1.10/x", "http://10.1.2.3/", "file:///etc/passwd", "ftp://example.com/a"):
            with self.subTest(url=url):
                with self.assertRaises(SourceIngestError):
                    validate_public_http_url(url)

    def test_url_ingest_uses_injected_fetch_and_never_invents_text(self) -> None:
        def fetch(url: str):
            self.assertEqual(url, "https://example.com/rsi")
            return 200, {"content-type": "text/plain; charset=utf-8"}, PAPER.encode("utf-8")

        record = ingest_from_url("https://example.com/rsi", fetch=fetch)
        self.assertEqual(record["kind"], "url")
        self.assertEqual(record["uri"], "https://example.com/rsi")
        self.assertEqual(record["text"], PAPER)

    def test_draft_must_quote_spans_and_refuses_invention(self) -> None:
        spans = quoted_spans_from_text(PAPER)
        bound = bind_draft_to_spans(
            spans,
            {
                "object_kind": "strategy",
                "clauses": [{"span_id": spans[0]["id"], "text": "buys when the 14-period RSI is below 30"}],
            },
        )
        self.assertEqual(bound["status"], "bound")
        self.assertEqual(bound["object_kind"], "strategy")
        self.assertEqual(bound["clauses"][0]["span_id"], spans[0]["id"])

        with self.assertRaises(SourceIngestError) as invented:
            bind_draft_to_spans(
                spans,
                {
                    "object_kind": "strategy",
                    "clauses": [{"span_id": spans[0]["id"], "text": "this edge will work live"}],
                },
            )
        self.assertEqual(invented.exception.code, "draft_clause_invented")

        with self.assertRaises(SourceIngestError) as untyped:
            bind_draft_to_spans(spans, {"object_kind": "strategy", "clauses": []})
        self.assertEqual(untyped.exception.code, "draft_clause_invalid")

    def test_http_document_ingest_mints_idea_with_spans(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp) / "data")
            status, payload = research_idea_ingest_write(
                store,
                {"filename": "rsi.txt", "text": PAPER},
            )
            self.assertEqual(status, 201)
            self.assertEqual(payload["schema"], "tc.research-idea.v1")
            self.assertEqual(payload["text"], PAPER)
            self.assertEqual(payload["ingest"]["kind"], "document")
            self.assertGreaterEqual(len(payload["ingest"]["quoted_spans"]), 2)
            self.assertNotIn("text", payload["ingest"])
            self.assertEqual(payload["draft"]["object_kind"], "unresolved")
            self.assertEqual(payload["draft"]["reason_code"], "assistant_not_invoked")

            status, refused = research_idea_ingest_write(store, {"url": "http://127.0.0.1/x", "filename": "x.txt"})
            self.assertEqual(status, 400)

            status, bound = research_idea_ingest_write(
                store,
                {
                    "filename": "rsi.txt",
                    "text": PAPER,
                    "entity_id": payload["entity_id"],
                    "expected_revision": payload["revision"],
                    "draft": {
                        "object_kind": "strategy",
                        "clauses": [
                            {
                                "span_id": payload["ingest"]["quoted_spans"][0]["id"],
                                "text": "buys when the 14-period RSI is below 30",
                            }
                        ],
                    },
                },
            )
            self.assertEqual(status, 200)
            self.assertEqual(bound["draft"]["status"], "bound")
            self.assertEqual(bound["draft"]["object_kind"], "strategy")
            self.assertEqual(bound["parent_revision"], payload["revision"])


if __name__ == "__main__":
    unittest.main()
