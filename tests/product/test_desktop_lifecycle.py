from __future__ import annotations

import subprocess
import sys
import unittest

from tradercockpit.desktop_lifecycle import (
    DesktopLifecycleError,
    DesktopWorkerSupervisor,
)


class _StubbornProcess:
    def __init__(self, *, kill_stops: bool = True) -> None:
        self.alive = True
        self.terminate_calls = 0
        self.kill_calls = 0
        self.kill_stops = kill_stops

    def poll(self) -> int | None:
        return None if self.alive else 0

    def terminate(self) -> None:
        self.terminate_calls += 1

    def kill(self) -> None:
        self.kill_calls += 1
        if self.kill_stops:
            self.alive = False

    def wait(self, timeout: float | None = None) -> int:
        if self.alive:
            raise subprocess.TimeoutExpired(cmd="fixture-worker", timeout=timeout)
        return 0


class DesktopWorkerSupervisorTests(unittest.TestCase):
    def test_active_process_requires_unique_live_ownership_and_open_lifecycle(self) -> None:
        supervisor = DesktopWorkerSupervisor()
        self.assertIsNone(supervisor.active_process("project"))
        first, second = _StubbornProcess(), _StubbornProcess()
        supervisor.register(first, label="project")
        self.assertIs(supervisor.active_process("project"), first)
        supervisor.register(second, label="project")
        with self.assertRaises(DesktopLifecycleError):
            supervisor.active_process("project")
        second.alive = False
        self.assertIs(supervisor.active_process("project"), first)
        first.alive = False
        self.assertIsNone(supervisor.active_process("project"))
        supervisor.seal()
        with self.assertRaises(DesktopLifecycleError):
            supervisor.active_process("project")

    def test_real_registered_process_is_terminated_and_reaped(self) -> None:
        process = subprocess.Popen(
            [sys.executable, "-c", "import time; time.sleep(60)"],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        supervisor = DesktopWorkerSupervisor()
        try:
            supervisor.register(process, label="fixture-worker", timeout_seconds=2)
            self.assertEqual(supervisor.active_count, 1)

            supervisor.stop_all()

            self.assertIsNotNone(process.poll())
            self.assertEqual(supervisor.active_count, 0)
            self.assertTrue(supervisor.sealed)
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=5)

    def test_stubborn_worker_escalates_from_terminate_to_kill(self) -> None:
        process = _StubbornProcess()
        supervisor = DesktopWorkerSupervisor()
        supervisor.register(process, label="stubborn", timeout_seconds=0.01)

        supervisor.stop_all()

        self.assertEqual(process.terminate_calls, 1)
        self.assertEqual(process.kill_calls, 1)
        self.assertFalse(process.alive)
        self.assertEqual(supervisor.active_count, 0)

    def test_worker_that_survives_kill_is_reported_and_retained(self) -> None:
        process = _StubbornProcess(kill_stops=False)
        supervisor = DesktopWorkerSupervisor()
        supervisor.register(process, label="unkillable", timeout_seconds=0.01)

        with self.assertRaisesRegex(DesktopLifecycleError, "shutdown incomplete"):
            supervisor.stop_all()

        self.assertTrue(process.alive)
        self.assertEqual(process.terminate_calls, 1)
        self.assertEqual(process.kill_calls, 1)
        self.assertEqual(supervisor.active_count, 1)
        self.assertTrue(supervisor.sealed)

    def test_sealed_supervisor_refuses_new_worker_registration(self) -> None:
        supervisor = DesktopWorkerSupervisor()
        supervisor.seal()

        with self.assertRaisesRegex(DesktopLifecycleError, "registration is refused"):
            supervisor.register(_StubbornProcess(), label="late-worker")

    def test_is_active_tracks_registered_label_until_the_process_exits(self) -> None:
        process = _StubbornProcess()
        supervisor = DesktopWorkerSupervisor()
        supervisor.register(process, label="sqx-project-start:Example")
        self.assertTrue(supervisor.is_active("sqx-project-start:Example"))
        self.assertFalse(supervisor.is_active("other"))
        process.alive = False
        self.assertFalse(supervisor.is_active("sqx-project-start:Example"))

    def test_duplicate_process_registration_is_refused(self) -> None:
        process = _StubbornProcess()
        supervisor = DesktopWorkerSupervisor()
        supervisor.register(process, label="first")

        with self.assertRaisesRegex(DesktopLifecycleError, "already registered"):
            supervisor.register(process, label="duplicate")

    def test_worker_registration_requires_process_contract(self) -> None:
        supervisor = DesktopWorkerSupervisor()

        with self.assertRaisesRegex(TypeError, "missing required callable"):
            supervisor.register(object(), label="invalid-worker")  # type: ignore[arg-type]

    def test_worker_registration_validates_label_and_timeout(self) -> None:
        supervisor = DesktopWorkerSupervisor()
        for label in ("", "   ", "bad\nlabel"):
            with self.subTest(label=label):
                with self.assertRaises(ValueError):
                    supervisor.register(_StubbornProcess(), label=label)

        for timeout in (0, -1, True, float("nan"), float("inf")):
            with self.subTest(timeout=timeout):
                with self.assertRaises(ValueError):
                    supervisor.register(
                        _StubbornProcess(),
                        label="worker",
                        timeout_seconds=timeout,
                    )


if __name__ == "__main__":
    unittest.main()
