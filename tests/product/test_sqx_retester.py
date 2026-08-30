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
from tradercockpit.sqx_outputs import (
    SQX_NATIVE_STRATEGY_SCHEMA,
    inspect_sqx_output,
    persist_sqx_custody_blob,
)
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

    def _strategy(self, source_info: dict[str, object]) -> StrategySpecV1:
        return StrategySpecV1(
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

    def _inputs(
        self,
        home: Path,
        strategy: StrategySpecV1,
        *,
        state_root: Path | None = None,
    ) -> BacktestInputsV1:
        candidate = CandidateSpecV1(strategy_ref=strategy.ref, origin="sqx-builder")
        data, execution = sqx_retester_native_contexts(
            home,
            strategy,
            state_root=state_root,
        )
        engine_build = sqx_retester_engine_build()
        run = BacktestRunSpecV1(
            candidate_ref=candidate.ref,
            data_ref=data.ref,
            execution_ref=execution.ref,
            engine_build_ref=engine_build.ref,
            random_seed=None,
        )
        return BacktestInputsV1(run, candidate, strategy, data, execution, engine_build)

    def test_native_retester_evaluator_uses_custody_and_persists_exact_result(self) -> None:
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
            source_info = inspect_sqx_output(source)
            persist_sqx_custody_blob(
                state,
                source.read_bytes(),
                expected_sha256=str(source_info["archive_sha256"]),
            )
            strategy = self._strategy(source_info)
            inputs = self._inputs(home, strategy, state_root=state)
            source.unlink()
            projects: list[str] = []

            def runner(command, **kwargs):
                project_name = next(item.split("=", 1)[1] for item in command if item.startswith("name="))
                projects.append(project_name)
                result = home / "user/projects" / project_name / "databanks/Results/Generated.sqx"
                self._archive(result, "retested")
                return subprocess.CompletedProcess(command, 0, "All tasks completed", "")

            evaluator = SqxRetesterEvaluator(home, custody_root=state, runner=runner)
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
            result_relative = result.payload["result"]["custody_relative_path"]
            self.assertIsInstance(result_relative, str)
            self.assertTrue((state / result_relative).is_file())
            self.assertEqual(len(projects), 1)
            self.assertFalse((home / "user/projects" / projects[0]).exists())

    def test_custodied_strategy_does_not_require_unique_builder_databank_match(self) -> None:
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
            source_info = inspect_sqx_output(source)
            persist_sqx_custody_blob(
                state,
                source.read_bytes(),
                expected_sha256=str(source_info["archive_sha256"]),
            )
            strategy = self._strategy(source_info)
            duplicate = source.with_name("Duplicate.sqx")
            duplicate.write_bytes(source.read_bytes())
            evaluator = SqxRetesterEvaluator(home, custody_root=state)

            evaluator.validate_strategy(strategy)

    def test_missing_custody_blob_fails_even_if_builder_source_still_exists(self) -> None:
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
            strategy = self._strategy(inspect_sqx_output(source))
            evaluator = SqxRetesterEvaluator(home, custody_root=state)

            with self.assertRaisesRegex(SqxRetesterError, "missing from TraderCockpit custody"):
                evaluator.validate_strategy(strategy)

    def test_engine_artifact_hash_mismatch_fails_before_launch(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp), b"wrong engine")
            source = home / "user/projects/Builder/databanks/Results/Generated.sqx"
            self._archive(source, "source")
            source_info = inspect_sqx_output(source)
            strategy = self._strategy(source_info)
            inputs = self._inputs(home, strategy)
            evaluator = SqxRetesterEvaluator(home)

            with self.assertRaises(SqxRetesterError):
                evaluator.validate_strategy(inputs.strategy)


if __name__ == "__main__":
    unittest.main()
