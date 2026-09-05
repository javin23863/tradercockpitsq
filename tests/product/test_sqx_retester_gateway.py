from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from zipfile import ZipFile
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError


class SqxRetesterGatewayTests(unittest.TestCase):
    STDOUT = """Loaded 1 strategies to databank Results
Retest : Starting strategies retesting...
Batch size computed to: 5, SingleOptims: false, str to test: 1, totalCores: 15, Backtest mode: 1
Retest : All backtest data prepared
Retest : Task finished in 5.21 s.
All tasks completed
"""

    @staticmethod
    def _task_log(home, project_name, *, tested=1):
        folder = home / "user/projects" / project_name / "log"
        folder.mkdir(exist_ok=True)
        (folder / "global_log_20260905_020659.log").write_text(
            f"Project: {project_name}\nTASK STARTED at 2026.09.05 16:06:59.718\n"
            "Task: Retest, Type: Retest\nDatabanks before start: Results (1)\n"
            "TASK FINISHED at 2026.09.05 16:07:05.075 in 5 s.\n"
            f"Total tested: {tested}, Time per strategy: 0 ms., Passed: 0, Failed: {tested}\n"
            "Failed details:\nGlobal filter: Profit factor[Main data] > 1.30, Count: 1\n", encoding="utf-8")

    def test_exit_zero_requires_bound_actual_execution_not_serialization(self):
        for output, tested, stale in (("", 1, False), ("Project started\nProject finished", 1, False),
                                     (self.STDOUT.replace("Retest :", "Other :"), 1, False),
                                     (self.STDOUT, 0, False), (self.STDOUT, 1, True),
                                     (self.STDOUT + "Preventing multiple instances", 1, False),
                                     (self.STDOUT.replace("str to test: 1", "str to test: 0"), 1, False)):
            with self.subTest(output=output, tested=tested, stale=stale), TemporaryDirectory() as tmp:
                home, name, launcher, project, engine, _ = self._runtime(Path(tmp))
                if stale:
                    self._task_log(home, name)
                def runner(command, **kwargs):
                    if not stale:
                        self._task_log(home, name, tested=tested)
                    return subprocess.CompletedProcess(command, 0, output, "")
                with self.assertRaises(SqxNativeGatewayError) as caught:
                    SqxNativeControlGateway(home, launcher, runner=runner).launch_retester_task(
                        name, expected_project_sha256=project, expected_engine_sha256=engine)
                self.assertEqual(caught.exception.code, "retester_execution_unverified")

    def test_multiple_or_inactive_tasks_refuse_before_process(self):
        for tasks in ('<Task type="Retest" name="Retest" active="false" taskXMLFile="Retest-Task1.xml"/>',
                      '<Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/><Task type="Retest" name="Other" active="false" taskXMLFile="Retest-Task2.xml"/>'):
            with self.subTest(tasks=tasks), TemporaryDirectory() as tmp:
                home, name, launcher, _, engine, _ = self._runtime(Path(tmp))
                path = home / "user/projects" / name / "project.cfx"
                with ZipFile(path, "w") as archive:
                    archive.writestr("config.xml", '<Project name="Retester"><Tasks>' + tasks + '</Tasks></Project>')
                    archive.writestr("Retest-Task1.xml", '<Settings/>')
                def runner(*args, **kwargs):
                    self.fail("Unsafe task topology reached native process")
                with self.assertRaises(SqxNativeGatewayError) as caught:
                    SqxNativeControlGateway(home, launcher, runner=runner).launch_retester_task(
                        name, expected_project_sha256=sha256(path.read_bytes()).hexdigest(), expected_engine_sha256=engine)
                self.assertEqual(caught.exception.code, "retester_source_project_invalid")

    def _runtime(self, root: Path) -> tuple[Path, str, str, str, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        launcher = b"trusted launcher"
        (root / "sqcli.exe").write_bytes(launcher)
        (root / "internal/libs").mkdir(parents=True)
        engine = b"current installed engine"
        (root / "internal/libs/SQTradingLib.jar").write_bytes(engine)
        project_name = "TraderCockpit-Retester-11111111111111111111111111111111"
        project_root = root / "user/projects" / project_name
        project_root.mkdir(parents=True)
        stream = BytesIO()
        with ZipFile(stream, "w") as archive:
            archive.writestr("config.xml", '<Project name="Retester"><Tasks><Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks></Project>')
            archive.writestr("Retest-Task1.xml", '<Settings/>')
        project = stream.getvalue()
        (project_root / "project.cfx").write_bytes(project)
        results = project_root / "databanks/Results"
        results.mkdir(parents=True)
        baseline = b"exact staged baseline"
        (results / "Baseline.sqx").write_bytes(baseline)
        return (
            root,
            project_name,
            sha256(launcher).hexdigest(),
            sha256(project).hexdigest(),
            sha256(engine).hexdigest(),
            sha256(baseline).hexdigest(),
        )

    def test_success_uses_exact_start_only_task_one_command(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            calls: list[tuple[list[str], dict[str, object]]] = []

            def runner(command, **kwargs):
                calls.append((list(command), dict(kwargs)))
                self._task_log(home, project_name)
                return subprocess.CompletedProcess(command, 0, self.STDOUT, "")

            receipt = SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                project_name,
                expected_project_sha256=project_hash,
                expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
            )

        self.assertEqual(receipt["operation"], "retester_start_task")
        self.assertEqual(receipt["project"], project_name)
        self.assertEqual(receipt["task"], 1)
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["project_sha256"], project_hash)
        self.assertEqual(receipt["engine_sha256"], engine_hash)
        self.assertEqual(receipt["launcher_sha256"], launcher_hash)
        self.assertEqual(len(calls), 1)
        self.assertEqual(
            calls[0][0],
            [
                str(home / "sqcli.exe"),
                "-project",
                "action=start",
                f"name={project_name}",
            ],
        )
        kwargs = calls[0][1]
        self.assertEqual(kwargs["cwd"], str(home))
        self.assertEqual(kwargs["stdin"], subprocess.DEVNULL)
        self.assertTrue(kwargs["capture_output"])
        self.assertTrue(kwargs["text"])
        self.assertFalse(kwargs["check"])
        self.assertFalse(kwargs["shell"])
        self.assertEqual(receipt["receipts"][0]["state"], "completed")
        self.assertEqual(receipt["receipts"][0]["exit_code"], 0)
        self.assertEqual(receipt["receipts"][0]["execution_proof"]["tested_strategies"], 1)
        self.assertEqual(receipt["receipts"][0]["execution_proof"]["failed_strategies"], 1)
        self.assertEqual(receipt["receipts"][0]["engine_sha256"], engine_hash)

    def test_arbitrary_project_identity_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, _, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    "Retester",
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )

        self.assertEqual(caught.exception.code, "retester_project_invalid")
        self.assertEqual(calls, 0)

    def test_project_launcher_and_engine_hashes_are_verified_before_spawn(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as project_caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256="0" * 64,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )
            self.assertEqual(project_caught.exception.code, "retester_project_hash_mismatch")
            self.assertEqual(calls, 0)

            with self.assertRaises(SqxNativeGatewayError) as launcher_caught:
                SqxNativeControlGateway(home, "0" * 64, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )
            self.assertEqual(launcher_caught.exception.code, "sqx_launcher_hash_mismatch")
            self.assertEqual(calls, 0)

            with self.assertRaises(SqxNativeGatewayError) as engine_caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256="0" * 64,
                )
            self.assertEqual(engine_caught.exception.code, "retester_engine_hash_mismatch")
            self.assertEqual(calls, 0)

    def test_engine_change_after_provenance_capture_refuses_before_spawn(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            (home / "internal/libs/SQTradingLib.jar").write_bytes(b"replacement engine")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )

        self.assertEqual(caught.exception.code, "retester_engine_hash_mismatch")
        self.assertEqual(calls, 0)

    def test_nonzero_exit_preserves_exact_retester_receipt(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))

            def runner(command, **kwargs):
                return subprocess.CompletedProcess(command, 7)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_command_rejected")
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(model["receipts"][0]["project"], project_name)
        self.assertEqual(model["receipts"][0]["task"], 1)
        self.assertEqual(model["receipts"][0]["state"], "rejected")
        self.assertEqual(model["receipts"][0]["exit_code"], 7)
        self.assertEqual(model["receipts"][0]["engine_sha256"], engine_hash)

    def test_timeout_marks_launched_retester_as_possible_partial_side_effect(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))

            def runner(command, **kwargs):
                raise subprocess.TimeoutExpired(command, timeout=kwargs["timeout"])

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
            result_archive_name="Baseline.sqx",
            expected_result_archive_sha256=baseline_hash,
                )

        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(model["receipts"][0]["state"], "timeout")



    def test_results_databank_redirection_inside_sqx_home_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            project_root = home / "user/projects" / project_name
            results = project_root / "databanks/Results"
            alternate = home / "user/projects/OtherProject/databanks/Results"
            alternate.mkdir(parents=True)
            (alternate / "Baseline.sqx").write_bytes(b"exact staged baseline")
            (results / "Baseline.sqx").unlink()
            results.rmdir()
            try:
                results.symlink_to(alternate, target_is_directory=True)
            except OSError as exc:
                self.skipTest(f"directory symlink unavailable on this platform: {exc}")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
                    result_archive_name="Baseline.sqx",
                    expected_result_archive_sha256=baseline_hash,
                )
        self.assertEqual(caught.exception.code, "retester_result_archive_path_escape")
        self.assertEqual(calls, 0)

    def test_project_root_redirection_to_other_project_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            project_root = home / "user/projects" / project_name
            other = home / "user/projects/OtherProject"
            (other / "databanks/Results").mkdir(parents=True)
            (other / "project.cfx").write_bytes(b"exact retester project")
            (other / "databanks/Results/Baseline.sqx").write_bytes(b"exact staged baseline")
            (project_root / "databanks/Results/Baseline.sqx").unlink()
            (project_root / "databanks/Results").rmdir()
            (project_root / "databanks").rmdir()
            (project_root / "project.cfx").unlink()
            project_root.rmdir()
            try:
                project_root.symlink_to(other, target_is_directory=True)
            except OSError as exc:
                self.skipTest(f"directory symlink unavailable on this platform: {exc}")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
                    result_archive_name="Baseline.sqx",
                    expected_result_archive_sha256=baseline_hash,
                )
        self.assertEqual(caught.exception.code, "retester_project_invalid")
        self.assertEqual(calls, 0)

    def test_staged_baseline_file_redirection_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            results = home / "user/projects" / project_name / "databanks/Results"
            baseline = results / "Baseline.sqx"
            redirected = results / "Redirected.sqx"
            redirected.write_bytes(baseline.read_bytes())
            baseline.unlink()
            try:
                baseline.symlink_to(redirected)
            except OSError as exc:
                self.skipTest(f"file symlink unavailable on this platform: {exc}")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
                    result_archive_name="Baseline.sqx",
                    expected_result_archive_sha256=baseline_hash,
                )
        self.assertEqual(caught.exception.code, "retester_result_archive_path_escape")
        self.assertEqual(calls, 0)


    def test_staged_baseline_change_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash, engine_hash, baseline_hash = self._runtime(Path(tmp))
            (home / "user/projects" / project_name / "databanks/Results/Baseline.sqx").write_bytes(b"changed baseline")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                    expected_engine_sha256=engine_hash,
                    result_archive_name="Baseline.sqx",
                    expected_result_archive_sha256=baseline_hash,
                )
        self.assertEqual(caught.exception.code, "retester_result_archive_hash_mismatch")
        self.assertEqual(calls, 0)


if __name__ == "__main__":
    unittest.main()
