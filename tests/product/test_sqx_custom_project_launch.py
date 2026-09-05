from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import Mock, patch
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
    launch_custom_project,
    SqxCustomProjectLaunchError,
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

    def test_owned_stop_checks_pid_and_requires_native_confirmation(self) -> None:
        from tradercockpit.sqx_native_web import SqxNativeWebError
        from tradercockpit import sqx_custom_project_launch as launch
        cases = (
            (None, 42, b"", "sqx_web_unavailable"),
            (42, 99, b"", "sqx_command_owner_mismatch"),
            (42, 42, b"Preventing multiple instances", "sqx_command_stop_unconfirmed"),
            (42, 42, b"", "sqx_command_stop_unconfirmed"),
            (42, 42, b"Stopping project Other\nProject execution stopped.", "sqx_command_stop_unconfirmed"),
            (42, 42, b"Stopping project Builder\nERROR failed\nProject execution stopped.", "sqx_command_stop_unconfirmed"),
            (42, 42, b"x" * 8193, "sqx_command_stop_unconfirmed"),
            (42, 42, b"-------\n07:56:15 Stopping project Builder\nProject execution stopped.", None),
        )
        for owned_pid, listener_pid, output, error in cases:
            with self.subTest(error=error, output=output[:40]), TemporaryDirectory() as tmp:
                home, digest = self._runtime(Path(tmp))
                self._write_project(home, "Builder")
                supervisor = DesktopWorkerSupervisor()
                process = _FakeProcess()
                process.pid = owned_pid
                process.args = [str((home / "sqcli.exe").resolve()), "-project", "action=start", "name=Builder"]
                if owned_pid is not None:
                    supervisor.register(process, label=custom_project_worker_label("Builder"))
                response = Mock(status=200)
                response.read.return_value = output
                response.__enter__ = Mock(return_value=response)
                response.__exit__ = Mock(return_value=False)
                opener = Mock()
                opener.open.return_value = response
                with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=SqxNativeWebError("sqx_web_unavailable", "unavailable")), \
                     patch.object(launch, "_command_service_listener_pid", return_value=listener_pid) as inspect, \
                     patch.object(launch, "build_opener", return_value=opener) as build, \
                     patch("tradercockpit.sqx_engine_progress.invalidate_custom_project_stats") as invalidate:
                    kwargs = dict(trusted_launcher_sha256=digest, worker_process=supervisor.active_process)
                    if error:
                        with self.assertRaises(SqxCustomProjectControlError) as caught:
                            custom_project_control(home, "Builder", "stop_project", **kwargs)
                        self.assertEqual(caught.exception.code, error)
                        invalidate.assert_not_called()
                    else:
                        record = custom_project_control(home, "Builder", "stop_project", **kwargs)
                        self.assertEqual(record["schema"], "tc.sqx-custom-project-control.v1")
                        self.assertEqual(record["native_action"], "stop")
                        self.assertNotIn("receipts", record)
                        invalidate.assert_called_once()
                        opener.open.assert_called_once_with("http://127.0.0.1:5050/call?cmd=-project%20action=stop%20name=Builder", timeout=5)
                        self.assertEqual(build.call_args.args[0].proxies, {})
                    if owned_pid is None or listener_pid != owned_pid:
                        build.assert_not_called()
                    if owned_pid is None:
                        inspect.assert_not_called()

    def test_owned_stop_never_bypasses_native_web_refusal(self) -> None:
        from tradercockpit.sqx_native_web import SqxNativeWebError
        from tradercockpit import sqx_custom_project_launch as launch
        with TemporaryDirectory() as tmp:
            home, digest = self._runtime(Path(tmp))
            self._write_project(home, "Builder")
            lookup = Mock(return_value=_FakeProcess())
            with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=SqxNativeWebError("sqx_web_control_refused", "refused")), \
                 patch.object(launch, "build_opener") as build:
                with self.assertRaises(SqxCustomProjectControlError) as caught:
                    custom_project_control(home, "Builder", "stop_project", trusted_launcher_sha256=digest, worker_process=lookup)
                self.assertEqual(caught.exception.code, "sqx_web_control_refused")
                lookup.assert_not_called()
                build.assert_not_called()

    def test_owned_stop_refuses_untrusted_or_ambiguous_worker_before_network(self) -> None:
        from tradercockpit.sqx_native_web import SqxNativeWebError
        from tradercockpit import sqx_custom_project_launch as launch
        for defect in ("launcher", "exited", "duplicate", "sealed", "changed_after_query", "unsafe_name"):
            with self.subTest(defect=defect), TemporaryDirectory() as tmp:
                home, digest = self._runtime(Path(tmp))
                project = "Example Workflow" if defect == "unsafe_name" else "Builder"
                self._write_project(home, project)
                supervisor = DesktopWorkerSupervisor()
                process = _FakeProcess()
                process.pid = 42
                process.args = [str((home / "sqcli.exe").resolve()), "-project", "action=start", f"name={project}"]
                if defect == "launcher":
                    process.args[0] = str(home / "other.exe")
                supervisor.register(process, label=custom_project_worker_label(project))
                if defect == "duplicate":
                    supervisor.register(_FakeProcess(), label=custom_project_worker_label(project))
                if defect == "sealed":
                    supervisor.seal()
                if defect == "exited":
                    process.returncode = 0
                def inspect():
                    if defect == "changed_after_query":
                        supervisor.seal()
                    return 42
                with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=SqxNativeWebError("sqx_web_unavailable", "unavailable")), \
                     patch.object(launch, "_command_service_listener_pid", side_effect=inspect), \
                     patch.object(launch, "build_opener") as build:
                    with self.assertRaises(SqxCustomProjectControlError):
                        custom_project_control(home, project, "stop_project", trusted_launcher_sha256=digest, worker_process=supervisor.active_process)
                    build.assert_not_called()

    @unittest.skipUnless(__import__("os").name == "nt", "Windows listener query")
    def test_listener_query_is_hidden_fixed_and_rejects_ambiguous_rows(self) -> None:
        from tradercockpit import sqx_custom_project_launch as launch
        import json
        row = {"LocalAddress": "0.0.0.0", "LocalPort": 5050, "OwningProcess": 42}
        for rows in (row, [], [row, row], dict(row, OwningProcess=0), dict(row, LocalAddress="192.168.1.10")):
            with self.subTest(rows=rows), patch.object(launch.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, json.dumps(rows), "")) as run:
                if rows == row:
                    self.assertEqual(launch._command_service_listener_pid(), 42)
                else:
                    with self.assertRaises(SqxCustomProjectLaunchError):
                        launch._command_service_listener_pid()
                self.assertEqual(run.call_args.kwargs["creationflags"], subprocess.CREATE_NO_WINDOW)
                self.assertEqual(run.call_args.kwargs["timeout"], 5)
                self.assertIn("-LocalPort 5050", run.call_args.args[0][-1])
                self.assertIn("Hidden", run.call_args.args[0])

    def test_command_transport_refuses_redirects_without_contacting_target(self) -> None:
        from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
        from threading import Thread
        from urllib.error import HTTPError
        from urllib.request import ProxyHandler, build_opener
        from tradercockpit.sqx_custom_project_launch import _NoCommandRedirect
        hits = []
        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                hits.append(self.path)
                self.send_response(302)
                self.send_header("Location", "/must-not-send-stop")
                self.end_headers()
            def log_message(self, *_args):
                pass
        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with self.assertRaises(HTTPError):
                build_opener(ProxyHandler({}), _NoCommandRedirect()).open(f"http://127.0.0.1:{server.server_port}/call", timeout=2)
            self.assertEqual(hits, ["/call"])
        finally:
            server.shutdown()
            server.server_close()
            thread.join()

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

    def test_stop_without_native_web_refuses_without_touching_the_live_worker(self) -> None:
        from tradercockpit.sqx_native_web import SqxNativeWebError
        for reason in ("sqx_web_unavailable", "sqx_web_settings_missing", "sqx_web_refused"):
            with self.subTest(reason=reason), TemporaryDirectory() as tmp:
                home, digest = self._runtime(Path(tmp))
                self._write_project(home)
                process = _FakeProcess()
                supervisor = DesktopWorkerSupervisor()
                supervisor.register(process, label=custom_project_worker_label("Example Workflow"))
                runner = Mock(return_value=subprocess.CompletedProcess([], 0, "Preventing multiple instances: The app is already running on port 5050", ""))
                factory = Mock()
                with patch("tradercockpit.sqx_native_web.sqx_local_json", side_effect=SqxNativeWebError(reason, "native web unavailable")), \
                     patch("tradercockpit.sqx_engine_progress.invalidate_custom_project_stats") as invalidate:
                    with self.assertRaises(SqxCustomProjectControlError) as caught:
                        custom_project_control(home, "Example Workflow", "stop_project",
                            trusted_launcher_sha256=digest, register_worker=supervisor.register,
                            worker_is_active=supervisor.is_active, runner=runner, process_factory=factory)
                self.assertEqual(caught.exception.code, reason)
                runner.assert_not_called()
                factory.assert_not_called()
                invalidate.assert_not_called()
                self.assertTrue(supervisor.is_active(custom_project_worker_label("Example Workflow")))
                self.assertFalse(process.terminated)

    def test_direct_stop_rejects_native_second_instance_refusal_at_exit_zero(self) -> None:
        refusal = "Preventing multiple instances: The app is already running on port 5050"
        for stdout, stderr in ((refusal, ""), ("", refusal)):
            with self.subTest(stderr=bool(stderr)), TemporaryDirectory() as tmp:
                home, digest = self._runtime(Path(tmp))
                archive = self._write_project(home)
                runner = Mock(return_value=subprocess.CompletedProcess([], 0, stdout, stderr))
                with self.assertRaises(SqxCustomProjectLaunchError) as caught:
                    launch_custom_project(home, "Example Workflow", "stop_project",
                        trusted_launcher_sha256=digest,
                        project_relative_path=archive.relative_to(home).as_posix(),
                        expected_project_sha256=sha256(archive.read_bytes()).hexdigest(),
                        register_worker=None, runner=runner)
                self.assertEqual(caught.exception.code, "sqx_command_rejected")
                self.assertIn("not stopped", caught.exception.detail)
                runner.assert_called_once()

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
            (str(Path("/tmp/sqcli.exe")), "-project", "action=start", "name=Builder"),
        )


if __name__ == "__main__":
    unittest.main()
