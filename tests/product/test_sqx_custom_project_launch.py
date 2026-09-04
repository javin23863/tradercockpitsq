from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.desktop_lifecycle import DesktopWorkerSupervisor
from tradercockpit.sqx_custom_project import (
    SqxCustomProjectControlError,
    custom_project_control,
    custom_project_progress_record,
    custom_project_topology_record,
    list_custom_projects,
)
from tradercockpit.sqx_custom_project_launch import (
    custom_project_worker_label,
    launch_readiness,
    project_command,
)


class _FakeProcess:
    def __init__(self, returncode: int | None = None) -> None:
        self.returncode = returncode
        self.terminated = False

    def poll(self) -> int | None:
        return self.returncode

    def terminate(self) -> None:
        self.terminated = True
        self.returncode = 0

    def kill(self) -> None:
        self.returncode = 0

    def wait(self, timeout: float | None = None) -> int:
        return 0 if self.returncode is not None else 0


class SqxCustomProjectLaunchTests(unittest.TestCase):
    def _runtime(self, root: Path, *, launcher: bytes = b"trusted-launcher") -> tuple[Path, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        return root, sha256(launcher).hexdigest()

    def _write_project(self, home: Path, project: str = "Example Workflow") -> Path:
        path = home / "user" / "projects" / project / "project.cfx"
        path.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(path, "w") as archive:
            archive.writestr("config.xml", "<Settings><Project/></Settings>")
            archive.writestr("Build-Task1.xml", "<Settings><Build/></Settings>")
        return path

    def test_start_uses_official_cli_argv_and_registers_the_worker(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            archive = self._write_project(home)
            expected_project = sha256(archive.read_bytes()).hexdigest()
            calls: list[list[str]] = []
            registered: list[tuple[object, str]] = []

            def factory(command, **_kwargs):
                calls.append(list(command))
                return _FakeProcess()

            def register(process, *, label, timeout_seconds=5.0):
                registered.append((process, label))

            receipt = custom_project_control(
                home,
                "Example Workflow",
                "run_project",
                trusted_launcher_sha256=digest,
                register_worker=register,
                process_factory=factory,
            )

        self.assertEqual(receipt["schema"], "tc.sqx-custom-project-control.v1")
        self.assertTrue(receipt["available"])
        self.assertEqual(receipt["native_action"], "start")
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["project_sha256"], expected_project)
        self.assertEqual(receipt["worker_label"], "sqx-project-start:Example Workflow")
        self.assertEqual(
            calls[0],
            [str(Path(tmp) / "sqcli.exe"), "-project", "action=start", "name=Example Workflow"],
        )
        self.assertEqual(registered[0][1], "sqx-project-start:Example Workflow")

    def test_stop_uses_official_cli_argv(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            calls: list[list[str]] = []

            def runner(command, **_kwargs):
                calls.append(list(command))
                return subprocess.CompletedProcess(command, 0, "", "")

            receipt = custom_project_control(
                home,
                "Example Workflow",
                "stop_project",
                trusted_launcher_sha256=digest,
                register_worker=lambda *_args, **_kwargs: None,
                runner=runner,
            )

        self.assertEqual(receipt["native_action"], "stop")
        self.assertEqual(
            calls[0],
            [str(home / "sqcli.exe"), "-project", "action=stop", "name=Example Workflow"],
        )

    def test_start_fails_closed_without_supervisor_registration(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256=digest,
                )
        self.assertEqual(caught.exception.code, "desktop_worker_unregistered")

    def test_start_fails_closed_on_launcher_hash_mismatch(self) -> None:
        with TemporaryDirectory() as tmp:
            home, _digest = self._runtime(Path(tmp))
            self._write_project(home)
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256="ab" * 32,
                    register_worker=lambda *_args, **_kwargs: None,
                )
        self.assertEqual(caught.exception.code, "sqx_launcher_hash_mismatch")

    def test_already_running_project_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256=digest,
                    register_worker=lambda *_args, **_kwargs: None,
                    worker_is_active=lambda label: label == custom_project_worker_label("Example Workflow"),
                    process_factory=lambda *_args, **_kwargs: _FakeProcess(),
                )
        self.assertEqual(caught.exception.code, "native_project_already_running")

    def test_cli_start_rejects_process_that_exits_immediately(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256=digest,
                    register_worker=lambda *_args, **_kwargs: None,
                    process_factory=lambda *_args, **_kwargs: _FakeProcess(0),
                )
        self.assertEqual(caught.exception.code, "sqx_command_rejected")

    def test_cli_start_rejects_process_that_dies_during_settle(self) -> None:
        class _DyingProcess(_FakeProcess):
            def __init__(self) -> None:
                super().__init__(None)
                self.checks = 0

            def poll(self) -> int | None:
                self.checks += 1
                if self.checks > 2:
                    self.returncode = 0
                return self.returncode

        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            with self.assertRaises(SqxCustomProjectControlError) as caught:
                custom_project_control(
                    home,
                    "Example Workflow",
                    "run_project",
                    trusted_launcher_sha256=digest,
                    register_worker=lambda *_args, **_kwargs: None,
                    process_factory=lambda *_args, **_kwargs: _DyingProcess(),
                )
        self.assertEqual(caught.exception.code, "sqx_command_rejected")

    def test_progress_streams_producer_logs_and_keeps_stats_unknown(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            (home / "log").mkdir()
            (home / "log" / "sqcli.log").write_text("Task 1 running\nAccepted stays unknown\n", encoding="utf-8")
            supervisor = DesktopWorkerSupervisor()
            process = _FakeProcess()
            supervisor.register(process, label=custom_project_worker_label("Example Workflow"))
            import tradercockpit.sqx_engine_progress as engine_progress

            original = engine_progress.read_engine_progress
            engine_progress.read_engine_progress = lambda *_args, **_kwargs: {
                "generated": None,
                "rejected": None,
                "accepted": None,
                "rate": None,
                "percent": None,
            }
            try:
                record = custom_project_progress_record(
                    home,
                    "Example Workflow",
                    trusted_launcher_sha256=digest,
                    register_worker=supervisor.register,
                    worker_is_active=supervisor.is_active,
                )
            finally:
                engine_progress.read_engine_progress = original
            catalog = list_custom_projects(
                home,
                trusted_launcher_sha256=digest,
                register_worker=supervisor.register,
            )
            topology = custom_project_topology_record(
                home,
                "Example Workflow",
                trusted_launcher_sha256=digest,
                register_worker=supervisor.register,
            )

        self.assertEqual(record["schema"], "tc.sqx-custom-project-progress.v1")
        self.assertTrue(record["running"])
        self.assertIsNone(record["generated"])
        self.assertIsNone(record["rejected"])
        self.assertIsNone(record["accepted"])
        self.assertIsNone(record["rate"])
        self.assertEqual(record["log_lines"][0]["relative_path"], "log/sqcli.log")
        self.assertEqual(record["log_lines"][0]["text"], "Task 1 running")
        self.assertTrue(catalog["control"]["available"])
        self.assertTrue(topology["execution"]["supported"])
        self.assertEqual(topology["execution"]["reason"], "native_cli")

    def test_progress_surfaces_sqx_engine_channel_stats(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home)
            supervisor = DesktopWorkerSupervisor()
            import tradercockpit.sqx_engine_progress as engine_progress

            original = engine_progress.read_engine_progress
            engine_progress.read_engine_progress = lambda *_args, **_kwargs: {
                "generated": 1200,
                "rejected": 980,
                "accepted": 220,
                "rate": 450,
                "percent": 37,
                "charts": [{
                    "type": "HeapMemoryChart",
                    "title": "Heap memory chart",
                    "series": [{"label": "Memory Usage", "values": [1.0, 2.0]}],
                }],
            }
            try:
                record = custom_project_progress_record(
                    home,
                    "Example Workflow",
                    trusted_launcher_sha256=digest,
                    register_worker=supervisor.register,
                    worker_is_active=supervisor.is_active,
                )
            finally:
                engine_progress.read_engine_progress = original

        self.assertEqual(record["generated"], 1200)
        self.assertEqual(record["rejected"], 980)
        self.assertEqual(record["accepted"], 220)
        self.assertEqual(record["rate"], 450)
        self.assertEqual(record["percent"], 37)
        self.assertEqual(record["charts"][0]["title"], "Heap memory chart")
        self.assertIn("engine-channel", record["detail"])
        self.assertIn("engineCharts", record["detail"])

    def test_readiness_is_not_launch_authorization_without_a_project(self) -> None:
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            ready = launch_readiness(home, digest, lambda *_args, **_kwargs: None)
            missing = launch_readiness(home, digest, None)
        self.assertTrue(ready["available"])
        self.assertEqual(missing["reason_code"], "desktop_worker_unregistered")
        context = type("Ctx", (), {"launcher": Path("/tmp/sqcli.exe"), "project": "Builder"})()
        self.assertEqual(
            project_command(context, "start"),
            ("/tmp/sqcli.exe", "-project", "action=start", "name=Builder"),
        )


if __name__ == "__main__":
    unittest.main()
