from __future__ import annotations

import unittest

from tradercockpit.sqx_engine_progress import (
    engine_progress_values,
    read_engine_progress,
    reset_engine_progress_cache_for_tests,
    setup_app_for_project,
)


class SqxEngineProgressTests(unittest.TestCase):
    def setUp(self) -> None:
        reset_engine_progress_cache_for_tests()

    def tearDown(self) -> None:
        reset_engine_progress_cache_for_tests()

    def test_engine_progress_values_map_sqx_field_names(self) -> None:
        payload = {
            "projectName": "Example Workflow",
            "strategies": 1200,
            "strategiesRejected": 980,
            "strategiesAccepted": 220,
            "strategiesPerHour": 450,
            "progressPercent": 37,
            "runningStatus": "paused",
        }
        mapped = engine_progress_values(payload)
        self.assertEqual(mapped["generated"], 1200)
        self.assertEqual(mapped["rejected"], 980)
        self.assertEqual(mapped["accepted"], 220)
        self.assertEqual(mapped["rate"], 450)
        self.assertEqual(mapped["percent"], 37)
        self.assertEqual(mapped["running_status"], "paused")

    def test_setup_app_matches_sqx_module_windows(self) -> None:
        self.assertEqual(setup_app_for_project("Builder"), "BUILDER")
        self.assertEqual(setup_app_for_project("Example Workflow"), "TASKMANAGER")

    def test_engine_payload_accepts_sqx_channel_names(self) -> None:
        from tradercockpit.sqx_engine_progress import _engine_payload

        message = {
            "projectData": {
                "name": "Builder",
                "channels": [{"name": "engine", "data": {"strategies": 3}}],
            }
        }
        self.assertEqual(_engine_payload(message, "Builder")["strategies"], 3)

    def test_engine_progress_values_leave_unknowns_none(self) -> None:
        mapped = engine_progress_values({"projectName": "Example Workflow"})
        self.assertIsNone(mapped["generated"])
        self.assertIsNone(mapped["rejected"])
        self.assertIsNone(mapped["accepted"])
        self.assertIsNone(mapped["rate"])

    def test_read_engine_progress_uses_poll_payload(self) -> None:
        calls: list[str] = []

        def fake_poll(_home, project, *, timeout=0.8):
            calls.append(project)
            return {
                "strategies": 10,
                "strategiesRejected": 4,
                "strategiesAccepted": 6,
                "strategiesPerHour": 30,
            }

        import tradercockpit.sqx_engine_progress as module

        original = module._poll_engine_channel
        module._poll_engine_channel = fake_poll
        try:
            first = read_engine_progress("/tmp/sqx", "Example Workflow")
            second = read_engine_progress("/tmp/sqx", "Example Workflow")
        finally:
            module._poll_engine_channel = original

        self.assertEqual(first["generated"], 10)
        self.assertEqual(first["rejected"], 4)
        self.assertEqual(first["accepted"], 6)
        self.assertEqual(first["rate"], 30)
        self.assertEqual(second["generated"], 10)
        self.assertEqual(calls, ["Example Workflow"])


if __name__ == "__main__":
    unittest.main()
