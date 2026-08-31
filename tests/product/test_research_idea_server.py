from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from tradercockpit.app_server import _is_loopback_address, make_handler
from tradercockpit.research_custody import FileResearchCustodyStore


class ResearchIdeaServerTests(unittest.TestCase):
    def _web_root(self, root: Path) -> Path:
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<title>TraderCockpit</title>", encoding="utf-8")
        return web

    def _start(self, web: Path, store: FileResearchCustodyStore | None):
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread, f"http://127.0.0.1:{server.server_port}"

    def _json(
        self,
        url: str,
        *,
        method: str = "GET",
        payload: dict[str, object] | None = None,
        content_type: str = "application/json",
    ) -> tuple[int, dict[str, object]]:
        data = json.dumps(payload).encode("utf-8") if payload is not None else None
        request = Request(
            url,
            data=data,
            method=method,
            headers={"Content-Type": content_type} if data is not None else {},
        )
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_idea_custody_accepts_only_loopback_client_addresses(self) -> None:
        for address in ("127.0.0.1", "127.0.0.42", "::1", "::1%1"):
            with self.subTest(address=address):
                self.assertTrue(_is_loopback_address(address))
        for address in ("0.0.0.0", "192.168.1.10", "8.8.8.8", "::", "2001:4860:4860::8888", "localhost", ""):
            with self.subTest(address=address):
                self.assertFalse(_is_loopback_address(address))

    def test_create_read_revise_catalog_and_restart_use_same_custody(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            data_root = root / "data"
            web = self._web_root(root)
            store = FileResearchCustodyStore(data_root)
            server, thread, base = self._start(web, store)
            try:
                status, created = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={"text": "Opening range concept", "source": "Research notes"},
                )
                self.assertEqual(status, 201)
                entity_id = str(created["entity_id"])
                first_revision = str(created["revision"])

                status, catalog = self._json(base + "/api/research/ideas")
                self.assertEqual(status, 200)
                self.assertEqual(len(catalog["ideas"]), 1)
                self.assertEqual(catalog["ideas"][0]["entity_id"], entity_id)

                status, current = self._json(
                    base + "/api/research/ideas?" + urlencode({"entityId": entity_id})
                )
                self.assertEqual(status, 200)
                self.assertEqual(current, created)

                status, revised = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={
                        "entity_id": entity_id,
                        "expected_revision": first_revision,
                        "text": "Opening range concept with unresolved exit rules",
                        "source": "Research notes",
                    },
                )
                self.assertEqual(status, 200)
                self.assertEqual(revised["parent_revision"], first_revision)
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

            reopened = FileResearchCustodyStore(data_root)
            server, thread, base = self._start(web, reopened)
            try:
                status, current = self._json(
                    base + "/api/research/ideas?" + urlencode({"entityId": entity_id})
                )
                self.assertEqual(status, 200)
                self.assertEqual(current["revision"], revised["revision"])
                self.assertEqual(current["text"], "Opening range concept with unresolved exit rules")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_stale_revision_and_invalid_write_shapes_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store)
            try:
                _, created = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={"text": "first"},
                )
                _, revised = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={
                        "entity_id": created["entity_id"],
                        "expected_revision": created["revision"],
                        "text": "second",
                    },
                )
                status, conflict = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={
                        "entity_id": created["entity_id"],
                        "expected_revision": created["revision"],
                        "text": "stale",
                    },
                )
                self.assertEqual(status, 409)
                self.assertEqual(conflict["reason_code"], "current_conflict")

                cases = (
                    {"source": "missing text"},
                    {"text": "x", "unexpected": True},
                    {"entity_id": created["entity_id"], "text": "missing revision"},
                    {
                        "entity_id": created["entity_id"],
                        "expected_revision": revised["revision"],
                        "text": "   ",
                    },
                )
                for payload in cases:
                    with self.subTest(payload=payload):
                        status, response = self._json(
                            base + "/api/research/ideas",
                            method="POST",
                            payload=payload,
                        )
                        self.assertEqual(status, 400)
                        self.assertEqual(response["error"], "invalid_request")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_idea_route_rejects_arbitrary_selectors_and_non_json(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store)
            try:
                for path in (
                    "/api/research/ideas?path=C:/other",
                    "/api/research/ideas?entityId=",
                    "/api/research/ideas?entityId=a&entityId=b",
                ):
                    status, payload = self._json(base + path)
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["error"], "invalid_request")

                status, payload = self._json(
                    base + "/api/research/ideas",
                    method="POST",
                    payload={"text": "x"},
                    content_type="text/plain",
                )
                self.assertEqual(status, 415)
                self.assertEqual(payload["error"], "unsupported_media_type")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_unbound_store_refuses_and_status_never_exposes_data_root(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            data_root = root / "private-custody-root"
            store = FileResearchCustodyStore(data_root)
            server, thread, base = self._start(web, store)
            try:
                status, payload = self._json(base + "/api/status")
                self.assertEqual(status, 200)
                self.assertEqual(payload["research_custody"]["status"], "ready")
                self.assertNotIn(str(data_root), json.dumps(payload))
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

            server, thread, base = self._start(web, None)
            try:
                status, payload = self._json(base + "/api/research/ideas")
                self.assertEqual(status, 503)
                self.assertEqual(payload["reason_code"], "research_store_not_bound")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
