from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore


class ResearchRetesterHttpBoundaryTests(unittest.TestCase):
    CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
    CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"

    def _server(self, root: Path):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        store = FileResearchCustodyStore(root / "data")
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread, store

    def _post(self, url: str, payload: dict[str, object]):
        request = Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            method="POST",
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        try:
            response = urlopen(request, timeout=2)
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))
        with response:
            return response.status, json.loads(response.read().decode("utf-8"))

    def test_retester_rejects_native_control_fields_and_forwards_only_candidate_identity(self) -> None:
        allowed = {
            "action": "start-retester",
            "candidate_entity_id": self.CANDIDATE_ENTITY,
            "expected_candidate_revision": self.CANDIDATE_REVISION,
        }
        result = {
            "schema": "tc.research-historical-result.v1",
            "entity_id": "tc-research:historical-result:v1:22222222-2222-4222-8222-222222222222",
            "revision": f"tc-research-revision:historical-result:sha256:{'2' * 64}",
            "reused": False,
        }

        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch("tradercockpit.research_retester_http.start_native_retester", return_value=result) as starter:
                    for forbidden in (
                        {"path": "C:/outside/project.cfx"},
                        {"project": "Retester"},
                        {"task": 2},
                        {"launcher": "C:/outside/sqcli.exe"},
                    ):
                        status, payload = self._post(endpoint, {**allowed, **forbidden})
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["reason_code"], "historical_result_action_invalid")
                        starter.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["entity_id"], result["entity_id"])
                    starter.assert_called_once_with(
                        store,
                        None,
                        None,
                        candidate_entity_id=self.CANDIDATE_ENTITY,
                        expected_candidate_revision=self.CANDIDATE_REVISION,
                    )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
