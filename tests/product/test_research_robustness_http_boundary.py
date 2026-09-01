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


class ResearchRobustnessHttpBoundaryTests(unittest.TestCase):
    HISTORICAL_ENTITY = "tc-research:historical-result:v1:11111111-1111-4111-8111-111111111111"
    HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'1' * 64}"
    VALIDATION_REF = f"tc-evidence:sha256:{'2' * 64}"
    PROJECT_SHA = "3" * 64
    ENGINE_SHA = "4" * 64
    LAUNCHER_SHA = "5" * 64
    PROJECT_NAME = "TraderCockpit-Retester-66666666666646668666666666666666"
    PROJECT_RELATIVE = f"user/projects/{PROJECT_NAME}/project.cfx"

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

    def _robustness_record(self, **overrides: object) -> dict[str, object]:
        result = {
            "schema": "tc.research-native-robustness.v1",
            "validation_ref": self.VALIDATION_REF,
            "operation": "native_retester_cross_check",
            "method": "RetestWithHigherPrecision",
            "execution_state": "completed",
            "producer_outcome_state": "producer_result_captured_outcome_unread",
            "native_project_name": self.PROJECT_NAME,
            "native_project_relative_path": self.PROJECT_RELATIVE,
            "compiled_project_sha256": self.PROJECT_SHA,
            "engine_sha256": self.ENGINE_SHA,
            "launcher_sha256": self.LAUNCHER_SHA,
            "receipts": [{
                "action": "startOnlyTask",
                "project": self.PROJECT_NAME,
                "task": 1,
                "state": "completed",
                "project_sha256": self.PROJECT_SHA,
                "engine_sha256": self.ENGINE_SHA,
                "launcher_sha256": self.LAUNCHER_SHA,
            }],
        }
        result.update(overrides)
        return result

    def test_higher_precision_forwards_only_exact_historical_result_identity(self) -> None:
        allowed = {
            "action": "start-higher-precision",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "expected_historical_result_revision": self.HISTORICAL_REVISION,
        }
        result = self._robustness_record()

        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch(
                    "tradercockpit.research_retester_http.start_native_higher_precision",
                    return_value=result,
                ) as starter:
                    for forbidden in (
                        {"task": 2},
                        {"path": "C:/outside/project.cfx"},
                        {"precision": "2"},
                        {"spread": "3"},
                        {"method": "MonteCarloRetest"},
                    ):
                        status, payload = self._post(endpoint, {**allowed, **forbidden})
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["reason_code"], "robustness_action_invalid")
                        starter.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["validation_ref"], self.VALIDATION_REF)
                    starter.assert_called_once_with(
                        store,
                        None,
                        None,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                    )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_robustness_reopen_forwards_only_exact_validation_evidence_ref(self) -> None:
        result = self._robustness_record()
        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch(
                    "tradercockpit.research_retester_http.read_native_robustness_result",
                    return_value=result,
                ) as reader:
                    status, payload = self._post(
                        endpoint,
                        {"action": "read-robustness", "validation_ref": self.VALIDATION_REF},
                    )
                    self.assertEqual(status, 200)
                    self.assertEqual(payload["validation_ref"], self.VALIDATION_REF)
                    reader.assert_called_once_with(store, self.VALIDATION_REF)

                    reader.reset_mock()
                    status, payload = self._post(
                        endpoint,
                        {
                            "action": "read-robustness",
                            "validation_ref": self.VALIDATION_REF,
                            "latest": True,
                        },
                    )
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "robustness_read_invalid")
                    reader.assert_not_called()
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_public_readback_refuses_receipt_project_engine_or_path_substitution(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                receipt_forgeries = (
                    ("project_sha256", "7" * 64),
                    ("engine_sha256", "8" * 64),
                    ("launcher_sha256", "9" * 64),
                    ("project", "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                )
                for field, forged in receipt_forgeries:
                    record = self._robustness_record()
                    record["receipts"] = [{**record["receipts"][0], field: forged}]
                    with patch(
                        "tradercockpit.research_retester_http.read_native_robustness_result",
                        return_value=record,
                    ):
                        status, payload = self._post(
                            endpoint,
                            {"action": "read-robustness", "validation_ref": self.VALIDATION_REF},
                        )
                    self.assertEqual(status, 409)
                    self.assertEqual(payload["reason_code"], "robustness_record_corrupt")

                for overrides in (
                    {"native_project_name": "Retester"},
                    {"native_project_name": "TraderCockpit-Retester-gggggggggggggggggggggggggggggggg"},
                    {"native_project_relative_path": "user/projects/Retester/project.cfx"},
                ):
                    record = self._robustness_record(**overrides)
                    with patch(
                        "tradercockpit.research_retester_http.read_native_robustness_result",
                        return_value=record,
                    ):
                        status, payload = self._post(
                            endpoint,
                            {"action": "read-robustness", "validation_ref": self.VALIDATION_REF},
                        )
                    self.assertEqual(status, 409)
                    self.assertEqual(payload["reason_code"], "robustness_record_corrupt")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
