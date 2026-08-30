from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.domain import BacktestRunSpecV1, CandidateSpecV1, StrategySpecV1
from tradercockpit.engine import BacktestInputsV1, evaluate_backtest
from tradercockpit.sqx_outputs import SQX_NATIVE_STRATEGY_SCHEMA, inspect_sqx_output
from tradercockpit.sqx_retester import (
    SQX_RETESTER_RESULT_SCHEMA,
    SqxRetesterError,
    SqxRetesterEvaluator,
    sqx_retester_engine_build,
    sqx_retester_native_contexts,
)


class SqxRetesterEvaluatorTests(unittest.TestCase):
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

    def _inputs(self, home: Path, source_info: dict[str, object]) -> BacktestInputsV1:
        strategy = StrategySpecV1(
            semantic_schema=SQX_NATIVE_STRATEGY_SCHEMA,
            semantics={
                "producer": "strategyquant-x",
                "source_build": "144.2953",
                "source_project": "Builder",
                "source_databank": "Results",
                "archive_sha256": source_info["archive_sha256"],
                "native_version": source_info["native_version"],
                "strategy_entry_sha256": source_info["strategy_entry_sha256"],
                "settings_entry_sha256": source_info["settings_entry_sha256"],
            },
        )
        candidate = CandidateSpecV1(strategy_ref=strategy.ref, origin="sqx-builder")
        data, execution = sqx_retester_native_contexts(home, strategy)
        engine_build = sqx_retester_engine_build()
        run = BacktestRunSpecV1(
            candidate_ref=candidate.ref,
            data_ref=data.ref,
            execution_ref=execution.ref,
            engine_build_ref=engine_build.ref,
            random_seed=None,
        )
        return BacktestInputsV1(run, candidate, strategy, data, execution, engine_build)

    def test_native_retester_evaluator_produces_exact_result_receipt(self) -> None:
        engine_bytes = b"fixture trading engine"
        engine_hash = sha256(engine_bytes).hexdigest()
        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_retester.SQX_TRADING_LIB_SHA256", engine_hash
        ):
            home = self._runtime(Path(tmp), engine_bytes)
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            source_info = inspect_sqx_output(source)
            inputs = self._inputs(home, source_info)

            def runner(command, **kwargs):
                project_name = next(item.split("=", 1)[1] for item in command if item.startswith("name="))
                result = home / "user/projects" / project_name / "databanks/Results/Generated.sqx"
                self._archive(result, "retested")
                return subprocess.CompletedProcess(command, 0, "All tasks completed", "")

            evaluator = SqxRetesterEvaluator(home, runner=runner)
            result = evaluate_backtest(inputs, evaluator)

            self.assertEqual(result.result_schema, SQX_RETESTER_RESULT_SCHEMA)
            self.assertEqual(result.run_ref, inputs.run.ref)
            self.assertEqual(result.producer_build_ref, inputs.engine_build.ref)
            self.assertEqual(result.payload["producer"]["exit_code"], 0)
            self.assertEqual(result.payload["source"]["archive_sha256"], source_info["archive_sha256"])
            self.assertEqual(
                result.payload["source"]["project_config_sha256"],
                inputs.data.source_config_sha256,
            )
            self.assertNotEqual(result.payload["result"]["archive_sha256"], source_info["archive_sha256"])
            workspace = result.payload["workspace"]["project"]
            self.assertTrue((home / "user/projects" / workspace / "databanks/Results/Generated.sqx").is_file())

    def test_strategy_hash_must_resolve_to_exactly_one_builder_archive(self) -> None:
        engine_bytes = b"fixture trading engine"
        engine_hash = sha256(engine_bytes).hexdigest()
        with TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_retester.SQX_TRADING_LIB_SHA256", engine_hash
        ):
            home = self._runtime(Path(tmp), engine_bytes)
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            source_info = inspect_sqx_output(source)
            inputs = self._inputs(home, source_info)
            duplicate = source.with_name("Duplicate.sqx")
            duplicate.write_bytes(source.read_bytes())
            evaluator = SqxRetesterEvaluator(home)

            with self.assertRaises(SqxRetesterError):
                evaluator.validate_strategy(inputs.strategy)

    def test_engine_artifact_hash_mismatch_fails_before_launch(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), b"wrong engine")
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            inputs = self._inputs(home, inspect_sqx_output(source))
            evaluator = SqxRetesterEvaluator(home)

            with self.assertRaises(SqxRetesterError):
                evaluator.validate_strategy(inputs.strategy)


if __name__ == "__main__":
    unittest.main()
