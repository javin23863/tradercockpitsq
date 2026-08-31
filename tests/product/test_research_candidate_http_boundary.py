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


class ResearchCandidateHttpBoundaryTests(unittest.TestCase):
    def _server(self, root: Path):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        store = FileResearchCustodyStore(root / "data")
        server = ThreadingHTTPServer(
            ("127.0.0.1", 0),
            make_handler(web, None, None, store),
        )
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread, store

    def _post(self, url: str, payload: dict[str, object]):
        body = json.dumps(payload).encode("utf-8")
        request = Request(
            url,
            data=body,
            method="POST",
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        try:
            response = urlopen(request, timeout=2)
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))
        with response:
            return response.status, json.loads(response.read().decode("utf-8"))

    def test_candidate_import_rejects_filesystem_path_and_forwards_only_exact_identity_tuple(self) -> None:
        native_job_entity = "tc-research:native-job:v1:11111111-1111-4111-8111-111111111111"
        native_job_revision = f"tc-research-revision:native-job:sha256:{'1' * 64}"
        archive_sha256 = "a" * 64
        allowed = {
            "action": "import-native-output",
            "native_job_entity_id": native_job_entity,
            "expected_native_job_revision": native_job_revision,
            "archive": "Survivor.sqx",
            "expected_archive_sha256": archive_sha256,
        }
        imported = {
            "schema": "tc.research-candidate.v1",
            "entity_id": "tc-research:candidate:v1:22222222-2222-4222-8222-222222222222",
            "revision": f"tc-research-revision:candidate:sha256:{'2' * 64}",
            "reused": False,
        }

        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/candidates"
            try:
                with patch("tradercockpit.app_server.import_native_candidate", return_value=imported) as importer:
                    status, payload = self._post(endpoint, {**allowed, "path": "C:/outside/Survivor.sqx"})
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "candidate_action_invalid")
                    importer.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["entity_id"], imported["entity_id"])
                    importer.assert_called_once_with(
                        store,
                        None,
                        native_job_entity_id=native_job_entity,
                        expected_native_job_revision=native_job_revision,
                        archive_name="Survivor.sqx",
                        expected_archive_sha256=archive_sha256,
                    )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
