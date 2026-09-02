from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest import mock
from zipfile import ZipFile

import tradercockpit.sqx_custom_project_control as control
from tradercockpit.sqx_custom_project_control import (
    SQX_CUSTOM_PROJECT_CONTROL_SCHEMA,
    custom_project_control_record,
    submit_custom_project_control,
)
from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError


class SqxCustomProjectGatewayTests(unittest.TestCase):
    PROJECT = "PortfolioComposer"

    def _runtime(self, root: Path) -> tuple[Path, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        launcher = b"trusted launcher"
        (root / "sqcli.exe").write_bytes(launcher)
        project_root = root / "user/projects" / self.PROJECT
        project_root.mkdir(parents=True)
        with ZipFile(project_root / "project.cfx", "w") as archive:
            archive.writestr("config.xml", "<Settings/>")
            archive.writestr("Build-Task1.xml", "<Settings/>")
        return root, sha256(launcher).hexdigest()

    def setUp(self) -> None:
        control._ACTIVE_HANDLES.clear()
        control.bind_worker_register(None)

    def test_run_uses_exact_start_command_and_returns_process_handle(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            calls: list[list[str]] = []

            class FakeProcess:
                pid = 4242

                def poll(self):
                    return None

            def popen_factory(command, **kwargs):
                calls.append(list(command))
                return FakeProcess()

            receipt = SqxNativeControlGateway(home, launcher_hash).control_custom_project(
                self.PROJECT,
                "run",
                process_factory=popen_factory,
            )
            self.assertEqual(receipt["operation"], "custom_project_run")
            self.assertEqual(receipt["project"], self.PROJECT)
            self.assertEqual(receipt["pid"], 4242)
            self.assertEqual(
                calls[0],
                [str(home / "sqcli.exe"), "-project", "action=start", f"name={self.PROJECT}"],
            )

    def test_stop_uses_exact_stop_command(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            calls: list[list[str]] = []

            def runner(command, **kwargs):
                calls.append(list(command))
                return subprocess.CompletedProcess(command, 0, "", "")

            receipt = SqxNativeControlGateway(home, launcher_hash, runner=runner).control_custom_project(
                self.PROJECT,
                "stop",
            )
            self.assertEqual(receipt["operation"], "custom_project_stop")
            self.assertEqual(
                calls[0],
                [str(home / "sqcli.exe"), "-project", "action=stop", f"name={self.PROJECT}"],
            )

    def test_builder_identity_is_rejected(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            builder = home / "user/projects/Builder/project.cfx"
            builder.parent.mkdir(parents=True)
            with ZipFile(builder, "w") as archive:
                archive.writestr("config.xml", "<Settings/>")
            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash).control_custom_project("Builder", "run")
            self.assertEqual(caught.exception.code, "custom_project_identity_reserved")


class SqxCustomProjectControlHttpTests(unittest.TestCase):
    PROJECT = "PortfolioComposer"

    def _runtime(self, root: Path) -> tuple[Path, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        launcher = b"trusted launcher"
        (root / "sqcli.exe").write_bytes(launcher)
        project_root = root / "user/projects" / self.PROJECT
        project_root.mkdir(parents=True)
        with ZipFile(project_root / "project.cfx", "w") as archive:
            archive.writestr("config.xml", "<Settings/>")
        return root, sha256(launcher).hexdigest()

    def setUp(self) -> None:
        control._ACTIVE_HANDLES.clear()
        control.bind_worker_register(None)

    def test_control_record_reports_run_and_stop_availability(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            record = custom_project_control_record(home, launcher_hash, self.PROJECT)
            self.assertEqual(record["schema"], SQX_CUSTOM_PROJECT_CONTROL_SCHEMA)
            self.assertTrue(record["execution"]["available"])
            self.assertTrue(record["control"]["run_enabled"])
            self.assertFalse(record["control"]["stop_enabled"])

    def test_submit_run_registers_handle_and_stop_clears_it(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            registered: list[str] = []

            class FakeProcess:
                pid = 9001

                def poll(self):
                    return None

                def terminate(self) -> None:
                    return None

            def registrar(process, label: str) -> None:
                registered.append(label)

            control.bind_worker_register(registrar)

            original = SqxNativeControlGateway.control_custom_project

            def fake_run(self, project_name, action, *, process_factory=None):
                if action == "run":
                    process = FakeProcess()
                    return {
                        "schema": "tc.sqx-native-control.v1",
                        "operation": "custom_project_run",
                        "project": project_name,
                        "state": "running",
                        "pid": process.pid,
                        "receipts": [],
                        "process": process,
                    }
                return original(self, project_name, action, process_factory=process_factory)

            with mock.patch.object(SqxNativeControlGateway, "control_custom_project", fake_run):
                run = submit_custom_project_control(home, launcher_hash, self.PROJECT, "run")
            self.assertEqual(run["operation"], "custom_project_run")
            self.assertEqual(registered, [f"sqx-custom-project:{self.PROJECT}"])
            ready = custom_project_control_record(home, launcher_hash, self.PROJECT)
            self.assertTrue(ready["control"]["live"])
            self.assertFalse(ready["control"]["run_enabled"])
            self.assertTrue(ready["control"]["stop_enabled"])

            with mock.patch.object(
                SqxNativeControlGateway,
                "control_custom_project",
                lambda self, project_name, action, *, process_factory=None: {
                    "schema": "tc.sqx-native-control.v1",
                    "operation": "custom_project_stop",
                    "project": project_name,
                    "state": "submitted",
                    "receipts": [],
                },
            ):
                stop = submit_custom_project_control(home, launcher_hash, self.PROJECT, "stop")
            self.assertEqual(stop["operation"], "custom_project_stop")
            cleared = custom_project_control_record(home, launcher_hash, self.PROJECT)
            self.assertFalse(cleared["control"]["live"])

    def test_unknown_action_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            home, launcher_hash = self._runtime(Path(tmp))
            with self.assertRaises(SqxNativeGatewayError) as caught:
                submit_custom_project_control(home, launcher_hash, self.PROJECT, "pause")
            self.assertEqual(caught.exception.code, "custom_project_action_invalid")
