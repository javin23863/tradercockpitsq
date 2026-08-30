from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.domain import ContentAddress, NativeDataContextV1, NativeExecutionContextV1
from tradercockpit.engine import load_initial_run_read_model
from tradercockpit.sqx_outputs import import_sqx_output
from tradercockpit.sqx_retester import SqxRetesterEvaluator
from tradercockpit.sqx_runs import SqxRunRequestError, start_sqx_native_run
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class SqxNativeRunVerticalTests(unittest.TestCase):
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

    def test_imported_candidate_executes_with_producer_derived_context_and_reopens(self):
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
            custody = import_sqx_output(home, state, "Generated.sqx")

            def runner(command, **kwargs):
                project_name = next(item.split("=", 1)[1] for item in command if item.startswith("name="))
                result = home / "user/projects" / project_name / "databanks/Results/Generated.sqx"
                self._archive(result, "retested")
                return subprocess.CompletedProcess(command, 0, "All tasks completed", "")

            response = start_sqx_native_run(
                home,
                state,
                {"candidate_ref": custody["candidate_ref"]},
                evaluator_factory=lambda sqx_home: SqxRetesterEvaluator(sqx_home, runner=runner),
                invocation_id_factory=lambda: "sqx-vertical-001",
                clock=lambda: datetime(2026, 8, 31, tzinfo=timezone.utc),
            )

            self.assertEqual(response["status"], "completed")
            self.assertIsNone(response["inputs"]["random_seed"])
            self.assertEqual(response["native_context"]["source_project"], "retester")
            self.assertEqual(response["native_context"]["source_task"], 1)

            store = FileObjectStore(state)
            lifecycle = FileRunLifecycleStore(state)
            parsed_run_ref = ContentAddress.parse(response["run_ref"])
            model = load_initial_run_read_model(
                parsed_run_ref,
                response["invocation_id"],
                store,
                lifecycle,
            )
            self.assertEqual(model.status, "completed")
            self.assertIsInstance(model.inputs.data, NativeDataContextV1)
            self.assertIsInstance(model.inputs.execution, NativeExecutionContextV1)
            self.assertEqual(model.result.ref, ContentAddress.parse(response["result_ref"]))
            self.assertIsNone(model.decision)
            self.assertIsNone(model.evidence_manifest)

    def test_native_request_refuses_user_supplied_execution_fiction(self):
        with self.assertRaisesRegex(SqxRunRequestError, "exactly candidate_ref"):
            start_sqx_native_run(
                None,
                None,
                {
                    "candidate_ref": "tc:candidate:v1:sha256:" + "0" * 64,
                    "data": {"symbol": "ES"},
                },
            )


if __name__ == "__main__":
    unittest.main()
