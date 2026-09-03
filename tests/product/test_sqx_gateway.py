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
)


class SqxNativeControlGatewayTests(unittest.TestCase):
    def _runtime(
        self,
        root: Path,
        *,
        launcher: bytes = b"trusted launcher",
        config: bytes = b"<builder-config />",
    ) -> tuple[Path, Path, str, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        (root / "sqcli.exe").write_bytes(launcher)
        config_path = root / "user/settings/Builder/Approved.cfx"
        config_path.parent.mkdir(parents=True)
        config_path.write_bytes(config)
        return (
            root,
            config_path,
            sha256(launcher).hexdigest(),
            sha256(config).hexdigest(),
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
                f"file={config}",
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

    def _supervised(self, home, config, launcher_hash, config_hash, script, *, runner_ok=True):
        """Run launch_builder with a fake spawner whose process writes ``script`` lines to the log.

        ``script`` is a list of (text, exit_code_after_or_None); each poll advances one step.
        """

        def runner(command, **kwargs):
            return subprocess.CompletedProcess(command, 0 if runner_ok else 1, "loadconfig ok", "")

        class FakeProcess:
            def __init__(self, log):
                self.log = log
                self.step = 0
                self.pid = 4242
                self.exit_code = None
                self.killed = False

            def poll(self):
                if self.step < len(script):
                    text, exit_code = script[self.step]
                    self.log.write(text.encode("utf-8"))
                    self.log.flush()
                    self.step += 1
                    if exit_code is not None:
                        self.exit_code = exit_code
                return self.exit_code

            def wait(self, timeout=None):
                return self.exit_code

            def terminate(self):
                self.exit_code = 1

            def kill(self):
                self.killed = True
                self.exit_code = -9

        spawned = {}

        def spawner(command, **kwargs):
            spawned["command"] = list(command)
            spawned["kwargs"] = kwargs
            # keep the log open for the fake process like the real child would
            process = FakeProcess(open(kwargs["stdout"].name, "ab"))
            spawned["process"] = process
            return process

        gateway = SqxNativeControlGateway(
            home,
            launcher_hash,
            runner=runner,
            spawner=spawner,
            start_ready_timeout_seconds=2.0,
            poll_interval_seconds=0.01,
            running_grace_seconds=0.02,
        )
        try:
            result = gateway.launch_builder(
                config,
                expected_config_sha256=config_hash,
                worker_log_path=home / "worker" / "job.log",
            )
        finally:
            if "process" in spawned:
                spawned["process"].log.close()
        return gateway, result, spawned

    def test_supervised_start_reports_running_worker_with_http_port(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            script = [
                ("Server started on port 5050\nHTTP API started\n", None),
                ("21:27:17 Starting project 'Builder'\n=========== Project started ===========\n", None),
                ("All backtest data prepared\n", None),
                ("", None),
                ("", None),
                ("", None),
            ]
            gateway, result, spawned = self._supervised(home, config, launcher_hash, config_hash, script)
            self.assertEqual(result["state"], "running")
            self.assertEqual(result["receipts"][0]["state"], "completed")
            self.assertEqual(result["receipts"][1]["state"], "running")
            self.assertIsNone(result["receipts"][1]["exit_code"])
            self.assertEqual(result["worker"]["pid"], 4242)
            self.assertEqual(result["worker"]["http_port"], 5050)
            self.assertEqual(result["worker"]["log_path"], str(home / "worker" / "job.log"))
            self.assertIs(gateway.worker.process, spawned["process"])
            self.assertEqual(spawned["command"], [str(home / "sqcli.exe"), "-project", "action=start", "name=Builder"])
            self.assertEqual(spawned["kwargs"]["cwd"], str(home))
            self.assertEqual(spawned["kwargs"]["stderr"], subprocess.STDOUT)
            self.assertFalse(spawned["process"].killed)

    def test_supervised_start_refusal_after_project_started_is_rejected_not_running(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            script = [
                ("Server started on port 5050\n=========== Project started ===========\n", None),
                ("Cannot start project. Cannot start project 'Builder', it has config errors. Error: Strategy file 'X' doesn't exist\n--------------------------------------------------\n", None),
                ("All tasks completed\nBye\n", 0),
            ]
            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._supervised(home, config, launcher_hash, config_hash, script)
            self.assertEqual(caught.exception.code, "sqx_cli_refused")
            self.assertIn("config errors", caught.exception.detail)
            self.assertEqual([item["state"] for item in caught.exception.receipts], ["completed", "rejected"])

    def test_supervised_start_kills_worker_that_never_reports_project_started(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            script = [("Server started on port 5050\n", None)] + [("", None)] * 1000
            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._supervised(home, config, launcher_hash, config_hash, script)
            self.assertEqual(caught.exception.code, "sqx_command_timeout")
            self.assertEqual(caught.exception.receipts[-1]["state"], "timeout")

    def test_supervised_start_quick_clean_exit_is_completed(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            script = [("=========== Project started ===========\nAll tasks completed\nBye\n", 0)]
            gateway, result, _ = self._supervised(home, config, launcher_hash, config_hash, script)
            self.assertEqual(result["state"], "submitted")
            self.assertEqual(result["receipts"][1]["state"], "completed")
            self.assertIsNone(gateway.worker)

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

    def test_loadconfig_exit_zero_with_cannot_load_config_is_refused(self) -> None:
        # Observed sqcli 144.2953 stdout: exit 0, looks for file.xml.cfx as a zip.
        stdout = (
            "Starting StrategyQuant X in command line mode.\n"
            "Params: -project action=loadconfig name=Builder file=C:\\sqx\\approved.xml \n"
            "Loading config of project Builder\n"
            "--------------------------------------------------\n"
            "Cannot load config. \n"
            "C:\\sqx\\approved.xml.cfx (The system cannot find the file specified)\n"
            "--------------------------------------------------\n"
            "All tasks completed\n"
            "Bye\n"
        )
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls: list[list[str]] = []

            def runner(command, **kwargs):
                calls.append(list(command))
                return subprocess.CompletedProcess(command, 0, stdout, "")

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_cli_refused")
        self.assertIn("Cannot load config", caught.exception.detail)
        self.assertIn("approved.xml.cfx", caught.exception.detail)
        self.assertEqual(len(calls), 1)
        self.assertIn("action=loadconfig", calls[0])
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(
            [(item["action"], item["state"], item["exit_code"], item["reason_code"]) for item in model["receipts"]],
            [("loadconfig", "rejected", 0, "sqx_cli_refused")],
        )

    def test_start_exit_zero_with_cannot_start_project_preserves_loadconfig_receipt(self) -> None:
        start_stdout = (
            "Cannot start project.\n"
            "Cannot start project 'Builder', it has config errors.\n"
            "Error: Strategy file 'D:\\missing.sq4' doesn't exist in field: Strategy file, in setting: What to build\n"
            "--------------------------------------------------\n"
            "Bye\n"
        )
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(command, **kwargs):
                nonlocal calls
                calls += 1
                if calls == 1:
                    return subprocess.CompletedProcess(command, 0, "Loading config of project Builder\nBye\n", "")
                return subprocess.CompletedProcess(command, 0, start_stdout, "")

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_cli_refused")
        self.assertIn("Cannot start project", caught.exception.detail)
        self.assertIn("Strategy file", caught.exception.detail)
        self.assertEqual(calls, 2)
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 1)
        self.assertTrue(model["partial_side_effect"])
        self.assertEqual(
            [(item["action"], item["state"], item["reason_code"]) for item in model["receipts"]],
            [("loadconfig", "completed", None), ("start", "rejected", "sqx_cli_refused")],
        )

    def test_timeout_configuration_must_be_positive(self) -> None:
        with self.assertRaises(ValueError):
            SqxNativeControlGateway(None, None, timeout_seconds=0)
        with self.assertRaises(ValueError):
            SqxNativeControlGateway(None, None, timeout_seconds=True)


if __name__ == "__main__":
    unittest.main()
