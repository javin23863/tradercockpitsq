from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
import os
import subprocess
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import Mock, patch
from zipfile import ZipFile
from xml.etree import ElementTree

from tradercockpit.sqx_gateway import (
    SQX_NATIVE_CONTROL_ERROR_SCHEMA,
    SQX_NATIVE_CONTROL_SCHEMA,
    SqxNativeControlGateway,
    SqxNativeGatewayError,
    pack_task_rooted_cfx,
    task_document_from_cfx,
)


class SqxNativeControlGatewayTests(unittest.TestCase):
    def setUp(self):
        settle = patch("tradercockpit.sqx_gateway._BUILDER_START_SETTLE_SECONDS", 0)
        settle.start()
        self.addCleanup(settle.stop)

    def _gateway(self, *args, **kwargs):
        kwargs.setdefault("register_worker", Mock())
        kwargs.setdefault("process_factory", Mock(return_value=Mock(poll=Mock(return_value=None))))
        return SqxNativeControlGateway(*args, **kwargs)

    def _packed(self, settings=b"<Settings><Exact>approved</Exact></Settings>", *, config=None):
        source = BytesIO()
        with ZipFile(source, "w") as archive:
            archive.writestr("config.xml", config or b'<Project name="Builder"><Tasks><Task type="Build" name="Build" taskXMLFile="Build-Task1.xml"/></Tasks></Project>')
            archive.writestr("Build-Task1.xml", settings)
        return pack_task_rooted_cfx(settings, source.getvalue())

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
        config = config if config is not None else self._packed()
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
                return subprocess.CompletedProcess(command, 0, "Config loaded.", "")

            process = Mock(poll=Mock(return_value=None))
            factory = Mock(return_value=process)
            registrar = Mock()
            receipt = self._gateway(
                home,
                launcher_hash,
                runner=runner,
                process_factory=factory,
                register_worker=registrar,
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
            [("loadconfig", "completed", 0), ("start", "completed", None)],
        )
        self.assertEqual(len(calls), 1)
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
            factory.call_args.args[0],
            [str(home / "sqcli.exe"), "-project", "action=start", "name=Builder"],
        )
        registrar.assert_called_once_with(process, label="sqx-project-start:Builder")
        start_options = factory.call_args.kwargs
        self.assertFalse(start_options["shell"])
        self.assertEqual(start_options["stdout"], subprocess.DEVNULL)
        self.assertEqual(start_options["creationflags"], subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        self.assertNotIn("timeout", start_options)
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
                    self._gateway(home, trusted, runner=runner).launch_builder(
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
                self._gateway(home, "0" * 64, runner=runner).launch_builder(
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
                self._gateway(
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
                self._gateway(home, launcher_hash).launch_builder(
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
                self._gateway(home, launcher_hash).launch_builder(
                    linked,
                    expected_config_sha256=expected,
                )
            self.assertEqual(linked_caught.exception.code, "config_path_escape")

    def test_config_identity_and_type_are_bounded(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, _ = self._runtime(Path(tmp))

            with self.assertRaises(SqxNativeGatewayError) as missing:
                self._gateway(home, launcher_hash).launch_builder(
                    config,
                    expected_config_sha256=None,
                )
            self.assertEqual(missing.exception.code, "config_identity_not_configured")

            with self.assertRaises(SqxNativeGatewayError) as mismatch:
                self._gateway(home, launcher_hash).launch_builder(
                    config,
                    expected_config_sha256="0" * 64,
                )
            self.assertEqual(mismatch.exception.code, "config_hash_mismatch")

            unsupported = config.with_suffix(".xml")
            unsupported.write_bytes(config.read_bytes())
            with self.assertRaises(SqxNativeGatewayError) as wrong_type:
                self._gateway(home, launcher_hash).launch_builder(
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
                return subprocess.CompletedProcess(command, 0, "Config loaded.", "")

            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._gateway(home, launcher_hash, runner=runner).launch_builder(
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
                return subprocess.CompletedProcess(command, 0, "Config loaded.", "")

            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._gateway(home, launcher_hash, runner=runner).launch_builder(
                    config,
                    expected_config_sha256=config_hash,
                )

        self.assertEqual(calls, 1)
        self.assertEqual(caught.exception.code, "config_hash_mismatch")
        self.assertEqual(caught.exception.read_model()["control_requests_completed"], 1)
        self.assertTrue(caught.exception.read_model()["partial_side_effect"])

    def test_start_early_exit_including_zero_preserves_partial_side_effect_receipts(self) -> None:
        for exit_code in (0, 7):
            with self.subTest(exit_code=exit_code), TemporaryDirectory() as tmp:
                home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
                runner = Mock(return_value=subprocess.CompletedProcess([], 0, "Config loaded.", ""))
                process = Mock(poll=Mock(return_value=exit_code))
                with self.assertRaises(SqxNativeGatewayError) as caught:
                    self._gateway(home, launcher_hash, runner=runner, process_factory=Mock(return_value=process)).launch_builder(
                        config, expected_config_sha256=config_hash)
                self.assertEqual(caught.exception.code, "sqx_command_rejected")
                model = caught.exception.read_model()
                self.assertEqual(model["control_requests_completed"], 1)
                self.assertTrue(model["partial_side_effect"])
                self.assertEqual([item["state"] for item in model["receipts"]], ["completed", "rejected"])
                runner.assert_called_once()

    def test_unconfirmed_or_refused_load_at_exit_zero_never_starts_builder(self) -> None:
        for output in ("", "Unknown task", "Cannot load config missing.cfx", "Config loaded.\nFile not found"):
            with self.subTest(output=output), TemporaryDirectory() as tmp:
                home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
                factory = Mock()
                with self.assertRaises(SqxNativeGatewayError) as caught:
                    self._gateway(home, launcher_hash, runner=Mock(return_value=subprocess.CompletedProcess([], 0, output, "")),
                                  process_factory=factory).launch_builder(config, expected_config_sha256=config_hash)
                self.assertEqual(caught.exception.code, "sqx_loadconfig_failed")
                factory.assert_not_called()

    def test_missing_supervisor_and_active_builder_refuse_before_load(self) -> None:
        for options, reason in (({"register_worker": None}, "desktop_worker_unregistered"),
                                ({"worker_is_active": lambda label: label == "sqx-project-start:Builder"}, "native_project_already_running")):
            with self.subTest(reason=reason), TemporaryDirectory() as tmp:
                home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
                runner = Mock()
                with self.assertRaises(SqxNativeGatewayError) as caught:
                    self._gateway(home, launcher_hash, runner=runner, **options).launch_builder(config, expected_config_sha256=config_hash)
                self.assertEqual(caught.exception.code, reason)
                runner.assert_not_called()

    def test_registration_failure_stops_unregistered_process(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))
            process = Mock(poll=Mock(return_value=None))
            process.terminate.side_effect = lambda: setattr(process.poll, "return_value", 0)
            registrar = Mock(side_effect=RuntimeError("desktop closing"))
            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._gateway(home, launcher_hash, runner=Mock(return_value=subprocess.CompletedProcess([], 0, "Config loaded.", "")),
                              process_factory=Mock(return_value=process), register_worker=registrar).launch_builder(
                    config, expected_config_sha256=config_hash)
            self.assertEqual(caught.exception.code, "desktop_worker_unregistered")
            process.terminate.assert_called_once()
            self.assertTrue(caught.exception.read_model()["partial_side_effect"])

    def test_pack_preserves_settings_bytes_and_source_task_attributes(self) -> None:
        settings = b'<Settings>\r\n  <!-- preserve whitespace --> <Exact a="&amp;"/>\r\n</Settings>'
        config = b'<Project name="Builder"><Tasks><Task type="Build" name="Build" taskXMLFile="Build-Task1.xml" templateFile="A &amp; B.cfx" version="126.2189"/></Tasks></Project>'
        packed = self._packed(settings, config=config)
        self.assertEqual(packed, self._packed(settings, config=config))
        document = task_document_from_cfx(packed)
        self.assertTrue(document.endswith(settings + b"</Task>"))
        self.assertEqual(ElementTree.fromstring(document).attrib, ElementTree.fromstring(config).find("./Tasks/Task").attrib)

    def test_pack_rejects_mismatched_settings_and_ambiguous_or_wrong_source_task(self) -> None:
        source = BytesIO()
        with ZipFile(source, "w") as archive:
            archive.writestr("config.xml", b'<Project name="Builder"/>')
            archive.writestr("Build-Task1.xml", b'<Settings/>')
        with self.assertRaises(SqxNativeGatewayError) as mismatch:
            pack_task_rooted_cfx(b'<Settings>different</Settings>', source.getvalue())
        self.assertEqual(mismatch.exception.code, "config_settings_mismatch")
        for tasks in (b'', b'<Task type="Retest" taskXMLFile="Build-Task1.xml"/>',
                      b'<Task type="Build" taskXMLFile="Build-Task1.xml"/><Task type="Build" taskXMLFile="Build-Task1.xml"/>'):
            with self.subTest(tasks=tasks), self.assertRaises(SqxNativeGatewayError) as caught:
                self._packed(config=b'<Project name="Builder"><Tasks>'+tasks+b'</Tasks></Project>')
            self.assertEqual(caught.exception.code, "config_task_element_missing")

    def test_project_rooted_archive_refuses_before_native_control(self) -> None:
        packed = BytesIO()
        with ZipFile(packed, "w") as archive:
            archive.writestr("config.xml", b'<Project name="Builder"/>')
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp), config=packed.getvalue())
            runner = Mock()
            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._gateway(home, launcher_hash, runner=runner).launch_builder(config, expected_config_sha256=config_hash)
            self.assertEqual(caught.exception.code, "config_task_element_missing")
            runner.assert_not_called()

    def test_first_command_timeout_has_no_completed_control_but_possible_side_effect(self) -> None:
        with TemporaryDirectory() as tmp:
            home, config, launcher_hash, config_hash = self._runtime(Path(tmp))

            def runner(command, **kwargs):
                raise subprocess.TimeoutExpired(command, kwargs["timeout"])

            with self.assertRaises(SqxNativeGatewayError) as caught:
                self._gateway(home, launcher_hash, runner=runner).launch_builder(
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
                self._gateway(home, launcher_hash, runner=runner).launch_builder(
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


if __name__ == "__main__":
    unittest.main()
