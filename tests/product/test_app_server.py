from decimal import Decimal
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
import tempfile
from threading import Thread
import unittest
from urllib.parse import urlencode
from urllib.request import urlopen

from tradercockpit.app_server import make_handler, read_run_snapshot, run_read_response
from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    RunLifecycleEventV1,
    StrategySpecV1,
)
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class AppServerReadTests(unittest.TestCase):
    def setup_state(self, root, *, publish_ready=True):
        store = FileObjectStore(root)
        lifecycle = FileRunLifecycleStore(root)
        strategy = StrategySpecV1(
            "tc.strategy.rules.v1",
            {"entry": {"kind": "always"}, "exit": {"bars": 1}},
        )
        candidate = CandidateSpecV1(strategy.ref, "manual")
        data = DataSpecV1(
            "ES",
            "1m",
            "fixture",
            "rev-1",
            "America/Chicago",
            "CME",
            "2025-01-01T00:00:00Z",
            "2025-01-02T00:00:00Z",
            "none",
        )
        execution = ExecutionSpecV1(
            Decimal("100000"),
            "USD",
            (ExecutionModelV1("fill", "bar-close", {}),),
        )
        build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
        run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
        for value in (strategy, candidate, data, execution, build, run):
            store.put(value)
        if publish_ready:
            lifecycle.publish(
                RunLifecycleEventV1(
                    run.ref,
                    "initial-001",
                    "ready",
                    "2025-01-02T00:00:00Z",
                )
            )
        return run, candidate, data, execution, build

    def test_exact_read_returns_only_verified_run_identity_and_lifecycle(self):
        with tempfile.TemporaryDirectory() as tmp:
            run, candidate, data, execution, build = self.setup_state(tmp)
            payload = read_run_snapshot(tmp, str(run.ref), "initial-001")

            self.assertEqual(payload["schema"], "tc.initial-run-read.v1")
            self.assertEqual(payload["run_ref"], str(run.ref))
            self.assertEqual(payload["invocation_id"], "initial-001")
            self.assertEqual(payload["status"], "ready")
            self.assertFalse(payload["terminal"])
            self.assertEqual(payload["inputs"]["candidate_ref"], str(candidate.ref))
            self.assertEqual(payload["inputs"]["data_ref"], str(data.ref))
            self.assertEqual(payload["inputs"]["execution_ref"], str(execution.ref))
            self.assertEqual(payload["inputs"]["engine_build_ref"], str(build.ref))
            self.assertIsNone(payload["artifacts"]["result_ref"])
            self.assertNotIn("metrics", payload)

    def test_http_boundary_serves_spa_and_exact_run_state(self):
        with tempfile.TemporaryDirectory() as state_tmp, tempfile.TemporaryDirectory() as web_tmp:
            run, *_ = self.setup_state(state_tmp)
            Path(web_tmp, "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(Path(web_tmp), state_tmp))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                with urlopen(f"{base}/validate/run") as response:
                    self.assertIn("TraderCockpit", response.read().decode())
                query = urlencode({"runRef": str(run.ref), "invocationId": "initial-001"})
                with urlopen(f"{base}/api/run-read?{query}") as response:
                    payload = json.loads(response.read())
                self.assertEqual(payload["run_ref"], str(run.ref))
                self.assertEqual(payload["status"], "ready")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_missing_lifecycle_is_not_inferred_from_existing_run_object(self):
        with tempfile.TemporaryDirectory() as tmp:
            run, *_ = self.setup_state(tmp, publish_ready=False)
            status, payload = run_read_response(tmp, str(run.ref), "initial-001")
            self.assertEqual(status, 404)
            self.assertEqual(payload["error"], "not_found")

    def test_non_run_content_address_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            run, candidate, *_ = self.setup_state(tmp)
            status, payload = run_read_response(tmp, str(candidate.ref), "initial-001")
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")
            self.assertNotEqual(str(run.ref), str(candidate.ref))

    def test_unconfigured_state_root_fails_closed(self):
        status, payload = run_read_response(None, "tc:backtest-run:v1:sha256:" + "a" * 64, "initial-001")
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")

    def test_invocation_id_is_not_normalized(self):
        with tempfile.TemporaryDirectory() as tmp:
            run, *_ = self.setup_state(tmp)
            status, payload = run_read_response(tmp, str(run.ref), " initial-001 ")
            self.assertEqual(status, 409)
            self.assertEqual(payload["error"], "invalid_state")


if __name__ == "__main__":
    unittest.main()
