from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchEntityId
from tradercockpit.research_ideas import create_idea
from tradercockpit.research_recent_work import (
    RECENT_WORK_SCHEMA,
    RESEARCH_RECENT_WORK_API_PATH,
    list_recent_work,
    research_recent_work_response,
)


def _draft(kind: str) -> dict[str, object]:
    return {
        "schema": "tc.research-source-draft.v1",
        "status": "bound",
        "object_kind": kind,
        "clauses": [{"span_id": "span-0001", "text": "quoted clause", "sha256": "ab"}],
        "reason_code": None,
        "detail": "Typed draft bound to hashed quoted spans.",
    }


def _pointer(store: FileResearchCustodyStore, entity_id: str) -> Path:
    entity = ResearchEntityId.parse(entity_id)
    return store.base / "current" / entity.kind.value / f"{entity.value}.json"


class ResearchRecentWorkTests(unittest.TestCase):
    def test_lists_typed_ideas_newest_first_and_skips_unresolved(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            create_idea(store, text="Plain note without a typed kind")
            create_idea(store, text="Unresolved draft", draft=_draft("unresolved"))
            indicator = create_idea(store, text="Range indicator\nmore", draft=_draft("indicator"))
            strategy = create_idea(store, text="Opening range strategy", draft=_draft("strategy"))
            model = create_idea(store, text="Regime model", draft=_draft("model"))
            os.utime(_pointer(store, str(indicator["entity_id"])), (10, 10))
            os.utime(_pointer(store, str(strategy["entity_id"])), (30, 30))
            os.utime(_pointer(store, str(model["entity_id"])), (20, 20))

            payload = list_recent_work(store)
            self.assertEqual(payload["schema"], RECENT_WORK_SCHEMA)
            kinds = [item["object_kind"] for item in payload["items"]]
            self.assertEqual(kinds, ["strategy", "model", "indicator"])
            self.assertEqual(payload["items"][0]["summary"], "Opening range strategy")
            self.assertEqual(
                payload["items"][0]["path"],
                f"/research?workspace=signals&tab=overview&idea={strategy['entity_id']}",
            )
            self.assertEqual(
                set(payload["items"][0]),
                {"entity_id", "revision", "object_kind", "summary", "path"},
            )

    def test_empty_store_is_an_empty_list(self) -> None:
        with TemporaryDirectory() as tmp:
            payload = list_recent_work(FileResearchCustodyStore(Path(tmp)))
            self.assertEqual(payload, {"schema": RECENT_WORK_SCHEMA, "items": []})

    def test_unbound_store_is_unavailable(self) -> None:
        status, payload = research_recent_work_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")


class ResearchRecentWorkServerTests(unittest.TestCase):
    def _start(self, root: Path, store: FileResearchCustodyStore | None):
        web = root / "web"
        web.mkdir()
        (web / "index.html").write_text("<title>TraderCockpit</title>", encoding="utf-8")
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, f"http://127.0.0.1:{server.server_port}"

    def _json(self, url: str, method: str = "GET") -> tuple[int, dict[str, object]]:
        request = Request(url, method=method)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_get_returns_typed_identities_and_refuses_query(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            store = FileResearchCustodyStore(root / "data")
            idea = create_idea(store, text="Opening range strategy", draft=_draft("strategy"))
            server, base = self._start(root, store)
            try:
                status, payload = self._json(base + RESEARCH_RECENT_WORK_API_PATH)
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], RECENT_WORK_SCHEMA)
                self.assertEqual(len(payload["items"]), 1)
                self.assertEqual(payload["items"][0]["entity_id"], idea["entity_id"])
                self.assertEqual(payload["items"][0]["object_kind"], "strategy")

                status, payload = self._json(base + RESEARCH_RECENT_WORK_API_PATH + "?entityId=1")
                self.assertEqual(status, 400)
                self.assertEqual(payload["error"], "invalid_request")
            finally:
                server.shutdown()
                server.server_close()
