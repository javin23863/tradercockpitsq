from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.assistant import build_messages, assistant_status_record
from tradercockpit.assistant_knowledge import (
    CORPUS_ENV,
    KNOWLEDGE_SCHEMA,
    format_grounding,
    knowledge_status,
    load_corpus,
    retrieve_passages,
)


def _write_corpus(root: Path, documents: list[dict[str, str]]) -> Path:
    path = root / "quant_guild.json"
    path.write_text(
        json.dumps(
            {
                "schema": KNOWLEDGE_SCHEMA,
                "library": "quant-guild",
                "source": "https://github.com/romanmichaelpaolucci/Quant-Guild-Library",
                "source_revision": "abc123",
                "documents": documents,
            }
        ),
        encoding="utf-8",
    )
    return path


class AssistantKnowledgeTests(unittest.TestCase):
    def test_missing_corpus_is_unavailable(self):
        status = knowledge_status(corpus_path=Path("C:/missing/quant_guild.json"))
        self.assertEqual(status["status"], "unavailable")
        self.assertEqual(status["reason_code"], "knowledge_corpus_unavailable")
        self.assertEqual(status["document_count"], 0)

    def test_retrieve_ranks_title_overlap_and_skips_unrelated(self):
        with TemporaryDirectory() as tmp:
            path = _write_corpus(
                Path(tmp),
                [
                    {"id": "2025-033", "title": "Why Monte Carlo Simulation Works", "text": "Monte Carlo estimates an expectation by sampling.", "url": "https://example.test/mc"},
                    {"id": "2025-036", "title": "How to Trade with the Kelly Criterion", "text": "Kelly sizes bets from edge and odds.", "url": "https://example.test/kelly"},
                    {"id": "2026-097", "title": "3 Backtesting Pitfalls That Ruin Your Strategy", "text": "Look-ahead bias and overfitting ruin backtests.", "url": "https://example.test/bt"},
                ],
            )
            hits = retrieve_passages("explain monte carlo option pricing", corpus_path=path)
            self.assertEqual([item["id"] for item in hits], ["2025-033"])
            self.assertEqual(retrieve_passages("unrelated gardening tips", corpus_path=path), [])

    def test_messages_attach_passages_or_explicit_no_hit(self):
        with TemporaryDirectory() as tmp:
            path = _write_corpus(
                Path(tmp),
                [{"id": "2025-029", "title": "Ito's Lemma Clearly and Visually Explained", "text": "Ito's lemma is the chain rule for stochastic calculus.", "url": "https://example.test/ito"}],
            )
            grounded = build_messages("What is Ito's lemma?", None, None, corpus_path=path)
            self.assertIn("Ito's lemma is the chain rule", grounded[0]["content"])
            self.assertIn("https://example.test/ito", grounded[0]["content"])
            missed = build_messages("What is the weather?", None, None, corpus_path=path)
            self.assertIn("no matching lecture excerpt", missed[0]["content"])

    def test_status_reports_ready_corpus_without_secrets(self):
        with TemporaryDirectory() as tmp:
            path = _write_corpus(Path(tmp), [{"id": "2025-001", "title": "Inverse Transform", "text": "Sample from a CDF.", "url": "https://example.test/inv"}])
            record = assistant_status_record({CORPUS_ENV: str(path), "OPENROUTER_API_KEY": "sk-or-test"})
            self.assertEqual(record["knowledge"]["status"], "ready")
            self.assertEqual(record["knowledge"]["document_count"], 1)
            self.assertNotIn("sk-or-test", json.dumps(record))
            self.assertEqual(load_corpus(corpus_path=path)["source_revision"], "abc123")
            self.assertIn("Inverse Transform", format_grounding(retrieve_passages("inverse transform", corpus_path=path)))

    def test_packaged_corpus_retrieves_a_real_lecture(self):
        status = knowledge_status()
        self.assertEqual(status["status"], "ready")
        self.assertGreaterEqual(status["document_count"], 100)
        hits = retrieve_passages("Kelly criterion bet sizing")
        self.assertTrue(hits)
        self.assertTrue(any("Kelly" in item["title"] for item in hits))


if __name__ == "__main__":
    unittest.main()
