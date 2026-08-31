from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.sqx_gateway import SqxNativeControlGateway, SqxNativeGatewayError


class SqxRetesterGatewayTests(unittest.TestCase):
    def _runtime(self, root: Path) -> tuple[Path, str, str, str]:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        launcher = b"trusted launcher"
        (root / "sqcli.exe").write_bytes(launcher)
        project_name = "TraderCockpit-Retester-11111111111111111111111111111111"
        project_root = root / "user/projects" / project_name
        project_root.mkdir(parents=True)
        project = b"exact retester project"
        (project_root / "project.cfx").write_bytes(project)
        return root, project_name, sha256(launcher).hexdigest(), sha256(project).hexdigest()

    def test_success_uses_exact_start_only_task_one_command(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash = self._runtime(Path(tmp))
            calls: list[tuple[list[str], dict[str, object]]] = []

            def runner(command, **kwargs):
                calls.append((list(command), dict(kwargs)))
                return subprocess.CompletedProcess(command, 0, "ignored", "ignored")

            receipt = SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                project_name,
                expected_project_sha256=project_hash,
            )

        self.assertEqual(receipt["operation"], "retester_start_task")
        self.assertEqual(receipt["project"], project_name)
        self.assertEqual(receipt["task"], 1)
        self.assertEqual(receipt["state"], "submitted")
        self.assertEqual(receipt["project_sha256"], project_hash)
        self.assertEqual(receipt["launcher_sha256"], launcher_hash)
        self.assertEqual(len(calls), 1)
        self.assertEqual(
            calls[0][0],
            [
                str(home / "sqcli.exe"),
                "-project",
                "action=startOnlyTask",
                f"name={project_name}",
                "task=1",
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

    def test_arbitrary_project_identity_refuses_before_runner(self) -> None:
        with TemporaryDirectory() as tmp:
            home, _, launcher_hash, project_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    "Retester",
                    expected_project_sha256=project_hash,
                )

        self.assertEqual(caught.exception.code, "retester_project_invalid")
        self.assertEqual(calls, 0)

    def test_project_hash_and_launcher_hash_are_verified_before_spawn(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash = self._runtime(Path(tmp))
            calls = 0

            def runner(*args, **kwargs):
                nonlocal calls
                calls += 1
                return subprocess.CompletedProcess(args, 0)

            with self.assertRaises(SqxNativeGatewayError) as project_caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256="0" * 64,
                )
            self.assertEqual(project_caught.exception.code, "retester_project_hash_mismatch")
            self.assertEqual(calls, 0)

            with self.assertRaises(SqxNativeGatewayError) as launcher_caught:
                SqxNativeControlGateway(home, "0" * 64, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                )
            self.assertEqual(launcher_caught.exception.code, "sqx_launcher_hash_mismatch")
            self.assertEqual(calls, 0)

    def test_nonzero_exit_preserves_exact_retester_receipt(self) -> None:
        with TemporaryDirectory() as tmp:
            home, project_name, launcher_hash, project_hash = self._runtime(Path(tmp))

            def runner(command, **kwargs):
                return subprocess.CompletedProcess(command, 7)

            with self.assertRaises(SqxNativeGatewayError) as caught:
                SqxNativeControlGateway(home, launcher_hash, runner=runner).launch_retester_task(
                    project_name,
                    expected_project_sha256=project_hash,
                )

        self.assertEqual(caught.exception.code, "sqx_command_rejected")
        model = caught.exception.read_model()
        self.assertEqual(model["control_requests_completed"], 0)
        self.assertFalse(model["partial_side_effect"])
        self.assertEqual(model["receipts"][0]["project"], project_name)
        self.assertEqual(model["receipts"][0]["task"], 1)
        self.assertEqual(model["receipts"][0]["state"], "rejected")
        self.assertEqual(model["receipts"][0]["exit_code"], 7)


if __name__ == "__main__":
    unittest.main()
