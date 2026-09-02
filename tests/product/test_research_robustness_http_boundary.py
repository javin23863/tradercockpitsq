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
from tradercockpit.research_robustness import ResearchRobustnessError


class ResearchRobustnessHttpBoundaryTests(unittest.TestCase):
    HISTORICAL_ENTITY = "tc-research:historical-result:v1:11111111-1111-4111-8111-111111111111"
    HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'1' * 64}"
    VALIDATION_REF = f"tc-evidence:sha256:{'2' * 64}"
    PROJECT_SHA = "3" * 64
    ENGINE_SHA = "4" * 64
    LAUNCHER_SHA = "5" * 64
    PROJECT_NAME = "TraderCockpit-Retester-66666666666646668666666666666666"

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
            "proof_entity_id": "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",
            "proof_revision": f"tc-research-revision:proof:sha256:{'8' * 64}",
            "operation": "native_retester_cross_check",
            "method": "RetestWithHigherPrecision",
            "execution_state": "completed",
            "producer_outcome_state": "producer_result_captured_outcome_unread",
            "native_project_name": self.PROJECT_NAME,
            "compiled_project_sha256": self.PROJECT_SHA,
            "engine_sha256": self.ENGINE_SHA,
            "launcher_sha256": self.LAUNCHER_SHA,
            "source_result_archive_sha256": "6" * 64,
            "receipts": [{
                "action": "startOnlyTask",
                "project": self.PROJECT_NAME,
                "task": 1,
                "state": "completed",
                "project_sha256": self.PROJECT_SHA,
                "engine_sha256": self.ENGINE_SHA,
                "launcher_sha256": self.LAUNCHER_SHA,
                "result_archive_sha256": "6" * 64,
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

    def test_additional_markets_forwards_only_exact_historical_result_identity(self) -> None:
        allowed = {
            "action": "start-additional-markets",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "expected_historical_result_revision": self.HISTORICAL_REVISION,
        }
        result = self._robustness_record(method="RetestOnAdditionalMarkets")

        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch(
                    "tradercockpit.research_retester_http.start_native_additional_markets",
                    return_value=result,
                ) as starter:
                    for forbidden in (
                        {"task": 2},
                        {"path": "C:/outside/project.cfx"},
                        {"markets": [{"symbol": "EURUSD"}]},
                        {"method": "MonteCarloRetest"},
                    ):
                        status, payload = self._post(endpoint, {**allowed, **forbidden})
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["reason_code"], "robustness_action_invalid")
                        starter.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["validation_ref"], self.VALIDATION_REF)
                    self.assertEqual(payload["method"], "RetestOnAdditionalMarkets")
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

    def test_walk_forward_forwards_only_exact_historical_result_identity(self) -> None:
        allowed = {
            "action": "start-walk-forward",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "expected_historical_result_revision": self.HISTORICAL_REVISION,
        }
        result = self._robustness_record(method="WalkForwardOptimization")

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
                        {"period": "10"},
                        {"method": "MonteCarloRetest"},
                    ):
                        status, payload = self._post(endpoint, {**allowed, **forbidden})
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["reason_code"], "robustness_action_invalid")
                        starter.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["validation_ref"], self.VALIDATION_REF)
                    self.assertEqual(payload["method"], "WalkForwardOptimization")
                    starter.assert_called_once_with(
                        store,
                        None,
                        None,
                        method="WalkForwardOptimization",
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                    )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_monte_carlo_manipulation_forwards_only_exact_historical_result_identity(self) -> None:
        allowed = {
            "action": "start-monte-carlo-manipulation",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "expected_historical_result_revision": self.HISTORICAL_REVISION,
        }
        result = self._robustness_record(method="MonteCarloManipulation")

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
                        {"NumberOfSimulations": "30"},
                        {"method": "MonteCarloRetest"},
                    ):
                        status, payload = self._post(endpoint, {**allowed, **forbidden})
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["reason_code"], "robustness_action_invalid")
                        starter.assert_not_called()

                    status, payload = self._post(endpoint, allowed)
                    self.assertEqual(status, 201)
                    self.assertEqual(payload["validation_ref"], self.VALIDATION_REF)
                    self.assertEqual(payload["method"], "MonteCarloManipulation")
                    starter.assert_called_once_with(
                        store,
                        None,
                        None,
                        method="MonteCarloManipulation",
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                    )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_failed_start_returns_only_originating_attempt_identity_when_available(self) -> None:
        allowed = {
            "action": "start-higher-precision",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "expected_historical_result_revision": self.HISTORICAL_REVISION,
        }
        attempt_ref = f"tc-evidence:sha256:{'a' * 64}"
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch(
                    "tradercockpit.research_retester_http.start_native_higher_precision",
                    side_effect=ResearchRobustnessError("sqx_control_timeout", "timed out", attempt_ref=attempt_ref),
                ):
                    status, payload = self._post(endpoint, allowed)
                self.assertEqual(status, 409)
                self.assertEqual(payload["attempt_ref"], attempt_ref)

                with patch(
                    "tradercockpit.research_retester_http.start_native_higher_precision",
                    side_effect=ResearchRobustnessError("runtime_not_configured", "runtime unavailable"),
                ):
                    status, payload = self._post(endpoint, allowed)
                self.assertEqual(status, 503)
                self.assertNotIn("attempt_ref", payload)
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

    def test_public_readback_refuses_receipt_project_or_engine_substitution(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                for field, forged in (
                    ("project_sha256", "7" * 64),
                    ("engine_sha256", "8" * 64),
                    ("launcher_sha256", "9" * 64),
                    ("project", "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                    ("result_archive_sha256", "0" * 64),
                ):
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
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


    def test_public_readback_requires_registered_proof_identity_shape(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread, _store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                record = self._robustness_record()
                record.pop("proof_entity_id")
                record.pop("proof_revision")
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

    def test_capability_and_catalog_reads_accept_no_browser_injected_settings(self) -> None:
        capabilities = {
            "schema": "tc.research-native-robustness-capabilities.v1",
            "sqx_build": "144.2953",
            "methods": [{
                "method": "RetestWithHigherPrecision",
                "state": "unavailable",
                "reason_code": "runtime_not_configured",
                "detail": "runtime unavailable",
                "native_settings": None,
                "configuration_changed": None,
                "source_project_sha256": None,
                "compiled_project_sha256": None,
                "engine_sha256": None,
            }],
        }
        catalog = {"schema": "tc.research-native-robustness-catalog.v1", "results": [], "failed_attempts": []}
        with TemporaryDirectory() as tmp:
            server, thread, store = self._server(Path(tmp))
            endpoint = f"http://127.0.0.1:{server.server_port}/api/research/historical-results"
            try:
                with patch("tradercockpit.research_retester_http.read_native_robustness_capabilities", return_value=capabilities) as capability_reader:
                    status, payload = self._post(endpoint, {"action": "read-robustness-capabilities"})
                    self.assertEqual(status, 200)
                    self.assertEqual(payload, capabilities)
                    capability_reader.assert_called_once_with(None)

                    capability_reader.reset_mock()
                    status, payload = self._post(endpoint, {"action": "read-robustness-capabilities", "Precision": "2"})
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "robustness_capabilities_invalid")
                    capability_reader.assert_not_called()

                with patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value=catalog) as catalog_reader:
                    status, payload = self._post(endpoint, {"action": "list-robustness"})
                    self.assertEqual(status, 200)
                    self.assertEqual(payload, catalog)
                    catalog_reader.assert_called_once_with(store)

                    catalog_reader.reset_mock()
                    status, payload = self._post(endpoint, {"action": "list-robustness", "latest": True})
                    self.assertEqual(status, 400)
                    self.assertEqual(payload["reason_code"], "robustness_catalog_invalid")
                    catalog_reader.assert_not_called()

                passed_receipt = {
                    "schema": "tc.research-native-robustness-catalog.v1",
                    "results": [],
                    "failed_attempts": [{
                        "schema": "tc.research-native-robustness-attempt.v1",
                        "state": "failed",
                        "attempt_ref": self.VALIDATION_REF,
                        "proof_entity_id": "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",
                        "proof_revision": f"tc-research-revision:proof:sha256:{'8' * 64}",
                        "failure_reason_code": "sqx_control_timeout",
                        "partial_side_effect": False,
                        "native_project_name": self.PROJECT_NAME,
                        "compiled_project_sha256": self.PROJECT_SHA,
                        "engine_sha256": self.ENGINE_SHA,
                        "method": "MonteCarloRetest",
                        "operation": "invented_operation",
                        "sqx_build": "0",
                        "receipts": [{
                            "action": "startOnlyTask",
                            "project": self.PROJECT_NAME,
                            "task": 1,
                            "state": "passed",
                        }],
                    }],
                }
                with patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value=passed_receipt):
                    status, payload = self._post(endpoint, {"action": "list-robustness"})
                    self.assertEqual(status, 409)
                    self.assertEqual(payload["reason_code"], "robustness_record_corrupt")

                omitted = {"schema": "tc.research-native-robustness-catalog.v1", "results": []}
                with patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value=omitted):
                    status, payload = self._post(endpoint, {"action": "list-robustness"})
                    self.assertEqual(status, 409)
                    self.assertEqual(payload["reason_code"], "robustness_catalog_corrupt")

                interrupted = {
                    "schema": "tc.research-native-robustness-catalog.v1",
                    "results": [],
                    "failed_attempts": [{
                        "schema": "tc.research-native-robustness-attempt.v1",
                        "state": "interrupted",
                        "attempt_ref": self.VALIDATION_REF,
                        "proof_entity_id": "tc-research:proof:v1:77777777-7777-4777-8777-777777777777",
                        "proof_revision": f"tc-research-revision:proof:sha256:{'8' * 64}",
                        "failure_reason_code": "robustness_attempt_interrupted",
                        "partial_side_effect": True,
                        "launcher_sha256": None,
                        "method": "MonteCarloRetest",
                        "operation": "invented_operation",
                        "sqx_build": "0",
                        "receipts": [],
                    }],
                }
                with patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value=interrupted):
                    status, payload = self._post(endpoint, {"action": "list-robustness"})
                    self.assertEqual(status, 409)
                    self.assertEqual(payload["reason_code"], "robustness_record_corrupt")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
