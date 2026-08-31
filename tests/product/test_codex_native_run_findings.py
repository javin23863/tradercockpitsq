from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.app_server import (
    run_read_response,
    sqx_imported_candidates_response,
    sqx_run_start_response,
)
from tradercockpit.domain import (
    BacktestRunSpecV1,
    CandidateSpecV1,
    DataSpecV1,
    EngineBuildSpecV1,
    ExecutionModelV1,
    ExecutionSpecV1,
    ResultArtifactV1,
    StrategySpecV1,
)
from tradercockpit.engine import EvaluatorDescriptorV1, execute_backtest
from tradercockpit.sqx_outputs import import_sqx_output
from tradercockpit.sqx_retester import SqxRetesterEvaluator
from tradercockpit.sqx_runs import start_sqx_native_run
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class _ExecutionEvaluator:
    def __init__(self, build):
        self._descriptor = EvaluatorDescriptorV1(
            build.ref,
            ("tc.strategy.rules.v1",),
            "tc.backtest.result.v1",
            True,
        )

    @property
    def descriptor(self):
        return self._descriptor

    def validate_strategy(self, strategy):
        return None

    def evaluate(self, inputs):
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            self._descriptor.result_schema,
            {"metrics": {"profit_factor": Decimal("1.25")}},
        )


class CodexNativeRunFindingTests(unittest.TestCase):
    def _archive(self, path: Path, marker: str) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr("settings.xml", f"<Settings>{marker}</Settings>".encode())
            archive.writestr("strategy_Portfolio.xml", f"<Strategy>{marker}</Strategy>".encode())
            archive.writestr("version.txt", b"144.2953")
            archive.writestr("orders.bin", marker.encode())

    def _runtime(self, root: Path, engine_bytes: bytes) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal").mkdir(exist_ok=True)
        (root / "internal/SQUANT.dat").write_bytes(b"144")
        (root / "internal/libs").mkdir(parents=True)
        (root / "internal/libs/SQTradingLib.jar").write_bytes(engine_bytes)
        (root / "sqcli.exe").write_bytes(b"fixture launcher")
        (root / "user/projects/Retester").mkdir(parents=True)
        (root / "user/projects/Retester/project.cfx").write_bytes(b"fixture retester project")
        return root

    def test_result_readback_reverifies_native_blob_hash_and_presence(self):
        engine_bytes = b"fixture trading engine"
        engine_hash = sha256(engine_bytes).hexdigest()
        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_retester.SQX_TRADING_LIB_SHA256", engine_hash
        ):
            root = Path(tmp)
            home = self._runtime(root / "sqx", engine_bytes)
            state = root / "state"
            state.mkdir()
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            custody = import_sqx_output(home, state, source.name)
            source.unlink()

            def runner(command, **kwargs):
                project_name = next(
                    item.split("=", 1)[1] for item in command if item.startswith("name=")
                )
                result = home / "user/projects" / project_name / "databanks/Results/Generated.sqx"
                self._archive(result, "retested")
                return subprocess.CompletedProcess(command, 0, "All tasks completed", "")

            response = start_sqx_native_run(
                home,
                state,
                {"candidate_ref": custody["candidate_ref"]},
                evaluator_factory=lambda sqx_home: SqxRetesterEvaluator(
                    sqx_home,
                    custody_root=state,
                    runner=runner,
                ),
                invocation_id_factory=lambda: "codex-custody-001",
                clock=lambda: datetime(2026, 8, 31, tzinfo=timezone.utc),
            )
            status, readback = run_read_response(
                state,
                response["run_ref"],
                response["invocation_id"],
            )
            self.assertEqual(status, 200)
            result_payload = readback["result"]["payload"]["result"]
            result_path = state / result_payload["custody_relative_path"]
            exact_bytes = result_path.read_bytes()

            result_path.write_bytes(b"corrupt")
            status, payload = run_read_response(
                state,
                response["run_ref"],
                response["invocation_id"],
            )
            self.assertEqual(status, 409)
            self.assertEqual(payload["reason_code"], "custody_failed")

            result_path.write_bytes(exact_bytes)
            self.assertEqual(
                run_read_response(state, response["run_ref"], response["invocation_id"])[0],
                200,
            )
            result_path.unlink()
            status, payload = run_read_response(
                state,
                response["run_ref"],
                response["invocation_id"],
            )
            self.assertEqual(status, 409)
            self.assertEqual(payload["reason_code"], "custody_failed")

    def test_imported_candidate_catalog_survives_builder_source_removal(self):
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as state_tmp:
            home = Path(runtime_tmp)
            (home / "internal/web/SQUANT").mkdir(parents=True)
            (home / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
            (home / "internal").mkdir(exist_ok=True)
            (home / "internal/SQUANT.dat").write_bytes(b"144")
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            imported = import_sqx_output(home, state_tmp, source.name)
            source.unlink()

            status, catalog = sqx_imported_candidates_response(state_tmp)
            self.assertEqual(status, 200)
            self.assertEqual(len(catalog["candidates"]), 1)
            candidate = catalog["candidates"][0]
            self.assertEqual(candidate["candidate_ref"], imported["candidate_ref"])
            self.assertEqual(candidate["strategy_ref"], imported["strategy_ref"])
            self.assertTrue(candidate["run_binding"]["available"])
            self.assertTrue((Path(state_tmp) / candidate["custody_relative_path"]).is_file())

    def test_execution_completion_uses_post_execution_timestamp(self):
        with TemporaryDirectory() as tmp:
            store = FileObjectStore(tmp)
            lifecycle = FileRunLifecycleStore(tmp)
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
                "UTC",
                "fixture",
                "2026-01-01T00:00:00Z",
                "2026-01-02T00:00:00Z",
                "none",
            )
            execution = ExecutionSpecV1(
                Decimal("100000"),
                "USD",
                (ExecutionModelV1("fill", "bar-close", {}),),
            )
            build = EngineBuildSpecV1("tradercockpit", "r1", "a" * 64)
            run = BacktestRunSpecV1(candidate.ref, data.ref, execution.ref, build.ref)
            for item in (strategy, candidate, data, execution, build, run):
                store.put(item)

            execute_backtest(
                run.ref,
                store,
                lifecycle,
                _ExecutionEvaluator(build),
                invocation_id="timing-001",
                issued_at="2026-01-02T00:00:00.000000Z",
                completion_clock=lambda: "2026-01-02T00:00:07.000000Z",
            )
            event = lifecycle.current(run.ref, "timing-001")
            self.assertEqual(event.status, "completed")
            self.assertEqual(event.occurred_at, "2026-01-02T00:00:07.000000Z")

    def test_missing_native_run_state_is_service_unavailable_not_bad_request(self):
        status, payload = sqx_run_start_response(
            None,
            None,
            {"candidate_ref": "tc:candidate:v1:sha256:" + "0" * 64},
        )
        self.assertEqual(status, 503)
        self.assertEqual(payload["error"], "producer_not_configured")


if __name__ == "__main__":
    unittest.main()
