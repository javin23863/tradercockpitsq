from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.sqx_gateway import (
    SQX_NATIVE_CONTROL_ERROR_SCHEMA,
    SQX_NATIVE_CONTROL_SCHEMA,
    SqxNativeControlGateway,
    SqxNativeGatewayError,
    pack_task_rooted_cfx,
)


class SqxNativeControlGatewayTests(unittest.TestCase):
    def _runtime(
        self,
        root: Path,
        *,
        launcher: bytes = b"trusted launcher",
        config: bytes | None = None,
    ) -> tuple[Path, Path, str, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        config_bytes = config if config is not None else pack_task_rooted_cfx(b"<Task><Exact>approved</Exact></Task>")
        config_path = root / "user/settings/Builder/Approved.cfx"
        config_path.parent.mkdir(parents=True)
        config_path.write_bytes(config_bytes)
        return (
            root,
            config_path,
            sha256(launcher).hexdigest(),
            sha256(config_bytes).hexdigest(),
        )

    def test_success_uses_exact_direct_cli_argv_and_structured_receipts(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls: list[tuple[list[str], dict[str, object]]] = []

            def runner(command, **kwargs):
                calls.append((list(command), dict(kwargs)))
                return subprocess.CompletedProcess(command, 0, "ignored stdout", "ignored stderr")

            receipt = SqxNativeControlGateway(
                home,
                launcher_hash,
                runner=runner,
            ).launch_builder(config, expected_config_sha256=config_hash)

        self.assertEqual(receipt["schema"], SQX_NATIVE_CONTROL_SCHEMA)
        self.assertEqual(receipt["operation"], "builder_loadconfig_start")
        self.assertEqual(receipt["project"], "Builder")
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["sqx_build"], "144.2953")
        self.assertEqual(receipt["launcher_sha256"], launcher_hash)
        self.assertEqual(receipt["config_sha256"], config_hash)
        self.assertEqual(receipt["config_relative_path"], "user/settings/Builder/Approved.cfx")
        self.assertEqual(receipt["control_requests_submitted"], 2)
        self.assertEqual(receipt["control_requests_completed"], 2)
        self.assertFalse(receipt["partial_side_effect"])
        self.assertEqual(
            [(item["action"], item["state"], item["exit_code"]) for item in receipt["receipts"]],
            [("loadconfig", "completed", 0), ("start", "completed", 0)],
        )
        self.assertEqual(len(calls), 2)
        self.assertEqual(
            calls[0][0],
            [
                str(home / "sqcli.exe"),
                "-project",
                "action=loadconfig",
                "name=Builder",
                f"file={config.with_suffix('')}",
            ],
        )
        self.assertEqual(
            calls[1][0],
            [str(home / "sqcli.exe"), "-project", "action=start", "name=Builder"],
        )
        for _, kwargs in calls:
            self.assertEqual(kwargs["cwd"], str(home))
            self.assertEqual(kwargs["stdin"], subprocess.DEVNULL)
            self.assertTrue(kwargs["capture_output"])
            self.assertTrue(kwargs["text"])
            self.assertEqual(kwargs["timeout"], 60.0)
            self.assertFalse(kwargs["check"])
            self.assertFalse(kwargs["shell"])

    def test_missing_or_malformed_launcher_trust_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, _, config_hash = self._runtime(Path(tmp))
            for trusted, code in (
                (None, "trusted_launcher_not_configured"),
                ("not-a-digest", "trusted_launcher_digest_invalid"),
            ):
                calls = 0

                def runner(*args, **kwargs):
                    nonlocal calls
                    calls += 1
                    return subprocess.CompletedProcess(args, 0)

                with self.assertRaises(SqxNativeGatewayError) as caught:
                    SqxNativeControlGateway(home, trusted, runner=runner).launch_builder(
                        config,
                        expected_config_sha256=config_hash,
                    )
                self.assertEqual(caught.exception.code, code)
                self.assertEqual(calls, 0)

    def test_launcher_hash_mismatch_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, _, config_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, "0" * 64, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_launcher_hash_mismatch")
        self.assertEqual(calls, 0)

    def test_launcher_symlink_escape_is_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home, config, _, config_hash = self._runtime(root / "sqx")
            external = root / "external.exe"
            external.write_bytes(b"outside")
            launcher = home / "sqcli.exe"
            launcher.unlink()
            try:
                launcher.symlink_to(external)
            except (OSError, NotImplementedError) as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(
                    home,
                    sha256(external.read_bytes()).hexdigest(),
                ).launch_builder(config, expected_config_sha256=config_hash)

        self.assertEqual(caught.exception.code, "sqx_launcher_path_escape")

    def test_config_path_escape_and_symlink_escape_are_refused(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home, _, launcher_hash, _ = self._runtime(root / "sqx")
            external = root / "outside.xml"
            external.write_bytes(b"<outside />")
            expected = sha256(external.read_bytes()).hexdigest()

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    external,
                    expected_config_sha256=expected,
                )
            self.assertEqual(caught.exception.code, "config_path_escape")

            linked = home / "user/settings/Builder/Linked.xml"
            try:
                linked.symlink_to(external)
            except (OSError, NotImplementedError) as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")
            with self.assertRaises(SqxNativeGatewayError) as linked_caught:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    linked,
                    expected_config_sha256=expected,
                )
            self.assertEqual(linked_caught.exception.code, "config_path_escape")

    def test_config_identity_and_type_are_bounded(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, _ = self._runtime(Path(tmp))

            with self.assertRaises(SqxNativeGatewayError) as missing:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    config,
                    expected_config_sha256=None,
                )
            self.assertEqual(missing.exception.code, "config_identity_not_configured")

            with self.assertRaises(SqxNativeGatewayError) as mismatch:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    config,
                    expected_config_sha256="0" * 64,
                )
            self.assertEqual(mismatch.exception.code, "config_hash_mismatch")

            unsupported = config.with_suffix(".xml")
            unsupported.write_bytes(config.read_bytes())
            with self.assertRaises(SqxNativeGatewayError) as wrong_type:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    unsupported,
                    expected_config_sha256=sha256(unsupported.read_bytes()).hexdigest(),
                )
            self.assertEqual(wrong_type.exception.code, "config_type_unsupported")

    def test_project_shaped_cfx_is_refused_as_missing_task_element(self) -> None:
        with TemporaryDirectory() as tmp:
            home, _, launcher_hash, _ = self._runtime(Path(tmp))
            from io import BytesIO
            from zipfile import ZipFile

            buffer = BytesIO()
            with ZipFile(buffer, "w") as archive:
                archive.writestr("config.xml", b"<Project><Tasks/></Project>")
                archive.writestr("Build-Task1.xml", b"<Task><Exact>approved</Exact></Task>")
            project_shaped = home / "user/settings/Builder/ProjectCopy.cfx"
            project_shaped.write_bytes(buffer.getvalue())

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash).launch_builder(
                    project_shaped,
                    expected_config_sha256=sha256(project_shaped.read_bytes()).hexdigest(),
                )
        self.assertEqual(caught.exception.code, "config_task_element_missing")

    def test_launcher_is_reverified_before_second_subprocess(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(command, **kwargs):
                nonlocal calls
                calls += 1
                if calls == 1:
                    (home / "sqcli.exe").write_bytes(b"replaced launcher")
                return subprocess.CompletedProcess(command, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(calls, 1)
        self.assertEqual(caught.exception.code, "sqx_launcher_hash_mismatch")
        model = caught.exception.read_model()
        self.assertEqual(model["schema"], SQX_NATIVE_CONTROL_ERROR_SCHEMA)
        self.assertEqual(model["control_requests_completed"], 1)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(
            [(item["action"], item["state"]) for item in model["receipts"]],
            [("loadconfig", "completed"), ("start", "preflight_failed")],
        )

    def test_config_is_reverified_before_second_subprocess(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(command, **kwargs):
                nonlocal calls
                calls += 1
                if calls == 1:
                    config.write_bytes(b"<changed />")
                return subprocess.CompletedProcess(command, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(calls, 1)
        self.assertEqual(caught.exception.code, "config_hash_mismatch")
        self.assertEqual(caught.exception.read_model()["control_requests_completed"], 1)
        self.assertTrue(caught.exception.read_model()["partial_side_effect"])

    def test_second_command_nonzero_preserves_partial_side_effect_receipts(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(command, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(command, 0 if calls == 1 else 7)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_command_rejected")
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 1)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(
            [(item["state"], item["exit_code"]) for item in model["receipts"]],
            [("completed", 0), ("rejected", 7)],
        )

    def test_first_command_timeout_has_no_completed_control_but_possible_side_effect(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))

            def runner(command, **kwargs):
                raise subprocess.TimeoutExpired(command, kwargs["timeout"])

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_command_timeout")
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(model["receipts"][0]["state"], "timeout")

    def test_runtime_build_is_freshly_verified_before_native_control(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            (home / "internal/web/SQUANT/build.dat").write_text("9999", encoding="utf-8")
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_build_mismatch")
        self.assertEqual(calls, 0)

    def test_timeout_configuration_must_be_positive(self) -> None:
        with self.assertRaises(ValueError):
            SqxNativeControlGateway(None, None, timeout_seconds=0)
        with self.assertRaises(ValueError):
            SqxNativeControlGateway(None, None, timeout_seconds=True)

    def test_loadconfig_native_failure_text_is_rejected_even_when_exit_is_zero(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls: list[list[str]] = []

            def runner(command, **kwargs):
                calls.append(list(command))
                return subprocess.CompletedProcess(
                    command,
                    0,
                    "",
                    "Cannot load config. /tmp/approved.xml.cfx (file not found)",
                )

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_loadconfig_failed")
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][2], "action=loadconfig")
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(model["receipts"][0]["action"], "loadconfig")
        self.assertEqual(model["receipts"][0]["state"], "rejected")
        self.assertEqual(model["receipts"][0]["reason_code"], "sqx_loadconfig_failed")

    def test_start_registers_long_lived_process_and_does_not_wait_for_exit(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            registered: list[object] = []
            started: list[list[str]] = []

            class AliveProcess:
                def poll(self):
                    return None

                def terminate(self):
                    raise AssertionError("start process must stay running")

            def runner(command, **kwargs):
                if "action=start" in command:
                    raise AssertionError("registered start must not use the waited runner")
                return subprocess.CompletedProcess(command, 0, "load ok", "")

            def process_factory(command, **kwargs):
                started.append(list(command))
                return AliveProcess()

            def register_worker(process, *, label):
                registered.append((process, label))

            receipt = SqxNativeControlGateway(
                home,
                launcher_hash,
                runner=runner,
                register_worker=register_worker,
                process_factory=process_factory,
            ).launch_builder(config, expected_config_sha256=config_hash)

        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(len(started), 1)
        self.assertEqual(started[0][2], "action=start")
        self.assertEqual(len(registered), 1)
        self.assertEqual(registered[0][1], "sqx-builder-start")
        self.assertEqual(receipt["receipts"][1]["action"], "start")
        self.assertEqual(receipt["receipts"][1]["state"], "completed")
        self.assertIsNone(receipt["receipts"][1]["exit_code"])


if __name__ == "__main__":
    unittest.main()
