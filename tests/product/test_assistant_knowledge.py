from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.assistant import OPENROUTER_API_KEY_ENV, assistant_reply, assistant_status_record, build_messages
from tradercockpit.knowledge import (
    KNOWLEDGE_ROOT_ENV,
    KNOWLEDGE_SCHEMA,
    format_grounding,
    knowledge_status,
    load_catalog,
    retrieve_knowledge,
    retrieve_passages,
)


def _write_catalog(root: Path, entries: list[dict[str, object]], name: str = "quant_guild_catalog.json") -> Path:
    path = root / name
    path.write_text(
        json.dumps(
            {
                "schema": KNOWLEDGE_SCHEMA,
                "library": "quant-guild",
                "source": "https://github.com/romanmichaelpaolucci/Quant-Guild-Library",
                "entries": entries,
            }
        ),
        encoding="utf-8",
    )
    return path


def _entry(identity: str, title: str, summary: str, **extra: object) -> dict[str, object]:
    return {
        "id": identity,
        "title": title,
        "summary": summary,
        "source_url": extra.get("source_url") or f"https://example.test/{identity}",
        "tags": extra.get("tags") or [],
        "year": extra.get("year", 2025),
        "lecture": extra.get("lecture", 1),
    }


class AssistantKnowledgeTests(unittest.TestCase):
    def test_missing_catalog_is_unavailable(self):
        status = knowledge_status(catalog_path=Path("/missing/quant_guild_catalog.json"))
        self.assertEqual(status["status"], "unavailable")
        self.assertEqual(status["reason_code"], "knowledge_catalog_unavailable")
        self.assertEqual(status["entry_count"], 0)

    def test_invalid_schema_fails_closed(self):
        with TemporaryDirectory() as tmp:
            path = Path(tmp) / "bad.json"
            path.write_text(json.dumps({"schema": "other", "entries": [{"id": "x"}]}), encoding="utf-8")
            loaded = load_catalog(catalog_path=path)
            self.assertEqual(loaded["reason_code"], "knowledge_catalog_invalid")
            self.assertEqual(retrieve_knowledge("sharpe", catalog_path=path)["state"], "unavailable")

    def test_retrieve_ranks_title_and_tags_and_skips_unrelated(self):
        with TemporaryDirectory() as tmp:
            path = _write_catalog(
                Path(tmp),
                [
                    _entry("qg-mc", "Why Monte Carlo Simulation Works", "Sampling estimates an expectation.", tags=["monte-carlo"]),
                    _entry("qg-kelly", "How to Trade with the Kelly Criterion", "Sizing stays with native money management.", tags=["kelly"]),
                    _entry("qg-bt", "3 Backtesting Pitfalls That Ruin Your Strategy", "Do not invent a platform backtester.", tags=["backtest"]),
                ],
            )
            hits = retrieve_passages("explain monte carlo option pricing", catalog_path=path)
            self.assertEqual([item["id"] for item in hits], ["qg-mc"])
            self.assertEqual(retrieve_passages("unrelated gardening tips", catalog_path=path), [])
            self.assertEqual(retrieve_knowledge("unrelated gardening tips", catalog_path=path)["state"], "idle")

    def test_messages_attach_notes_or_explicit_no_hit(self):
        with TemporaryDirectory() as tmp:
            path = _write_catalog(
                Path(tmp),
                [_entry("qg-sharpe", "Stop Using the Sharpe Ratio Until You Watch This", "Sharpe is a producer-supplied column.", tags=["sharpe"])],
            )
            grounded = build_messages("What does Sharpe mean here?", None, None, catalog_path=path)
            self.assertIn("Sharpe is a producer-supplied column.", grounded[0]["content"])
            self.assertIn("https://example.test/qg-sharpe", grounded[0]["content"])
            missed = build_messages("What is the weather?", None, None, catalog_path=path)
            self.assertIn("no matching catalog note", missed[0]["content"])

    def test_status_and_reply_report_citations_without_secrets(self):
        with TemporaryDirectory() as tmp:
            path = _write_catalog(
                Path(tmp),
                [_entry("qg-kelly", "How to Trade with the Kelly Criterion", "The cockpit does not compute a Kelly fraction.", tags=["kelly"])],
            )
            record = assistant_status_record({
                KNOWLEDGE_ROOT_ENV: str(Path(tmp)),
                OPENROUTER_API_KEY_ENV: "sk-or-test",
            })
            self.assertEqual(record["knowledge"]["status"], "ready")
            self.assertGreaterEqual(record["knowledge"]["entry_count"], 1)
            self.assertNotIn("sk-or-test", json.dumps(record))

            status, payload = assistant_reply(
                {"message": "How should I size with Kelly?"},
                environ={OPENROUTER_API_KEY_ENV: "sk-or-test", KNOWLEDGE_ROOT_ENV: str(Path(tmp))},
                transport=lambda *_: (200, json.dumps({
                    "id": "gen-1",
                    "model": "z-ai/glm-5.3-flash",
                    "choices": [{"message": {"role": "assistant", "content": "Native money management owns sizing."}}],
                    "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
                }).encode()),
            )
            self.assertEqual(status, 200)
            self.assertEqual(payload["knowledge"]["state"], "grounded")
            self.assertTrue(any("Kelly" in item["title"] for item in payload["knowledge"]["citations"]))
            self.assertNotIn("sk-or-test", json.dumps(payload))
            self.assertIn("Kelly", format_grounding(retrieve_knowledge("kelly criterion", catalog_path=path)))

    def test_packaged_catalog_retrieves_cockpit_relevant_lectures(self):
        status = knowledge_status()
        self.assertEqual(status["status"], "ready")
        self.assertGreaterEqual(status["entry_count"], 20)
        self.assertLess(status["entry_count"], 80)
        kelly = retrieve_passages("Kelly criterion bet sizing")
        self.assertTrue(any("Kelly" in str(item["title"]) for item in kelly))
        sharpe = retrieve_passages("What does the Sharpe ratio mean on this result?")
        self.assertTrue(any("Sharpe" in str(item["title"]) for item in sharpe))
        for passage in [*kelly, *sharpe]:
            self.assertNotIn("```", str(passage["summary"]))
            self.assertLessEqual(len(str(passage["summary"])), 400)


if __name__ == "__main__":
    unittest.main()
