from __future__ import annotations

import unittest

from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.parse import parse_qs, urlparse
import json

from tradercockpit.sqx_engine_progress import (
    custom_project_stat_fields,
    engine_chart_frames,
    engine_progress_values,
    list_engine_chart_types,
    read_engine_progress,
    reset_engine_progress_cache_for_tests,
    save_engine_chart_selection,
    setup_app_for_project,
)
from tradercockpit.sqx_native_web import SqxNativeWebError


class SqxEngineProgressTests(unittest.TestCase):
    def setUp(self) -> None:
        reset_engine_progress_cache_for_tests()

    def tearDown(self) -> None:
        reset_engine_progress_cache_for_tests()

    def test_custom_project_stat_fields_use_official_status_numbers(self) -> None:
        idle = custom_project_stat_fields({"projectName": "Example Workflow", "runningStatus": 0})
        running = custom_project_stat_fields(
            {"projectName": "Example Workflow", "runningStatus": 1, "progressPercent": 40}
        )
        paused = custom_project_stat_fields({"projectName": "Example Workflow", "runningStatus": 2})
        unknown = custom_project_stat_fields({"projectName": "Example Workflow", "runningStatus": 99})
        self.assertEqual(idle["running_status"], "beforeStart")
        self.assertFalse(idle["running"])
        self.assertTrue(running["running"])
        self.assertEqual(running["running_status"], "running")
        self.assertEqual(running["percent"], 40)
        self.assertTrue(paused["running"])
        self.assertEqual(paused["running_status"], "paused")
        self.assertIsNone(unknown)

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
            }, {
                "charts": [{
                    "type": "SQ.EngineCharts.AverageStrategiesPerHourChart",
                    "data": {
                        "type": "chart",
                        "chart": {
                            "data": {
                                "datasets": [{
                                    "label": "Avg. strategies per hour",
                                    "data": [10, 20, 30],
                                }],
                            },
                        },
                    },
                }],
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
        self.assertEqual(first["charts"][0]["title"], "Average strategies per hour")
        self.assertEqual(first["charts"][0]["series"][0]["values"], [10.0, 20.0, 30.0])
        self.assertEqual(second["generated"], 10)
        self.assertEqual(second["charts"][0]["type"], "SQ.EngineCharts.AverageStrategiesPerHourChart")
        self.assertEqual(calls, ["Example Workflow"])


    def test_engine_chart_frames_take_official_chartjs_series(self) -> None:
        frames = engine_chart_frames({
            "charts": [
                {
                    "type": "SQ.EngineCharts.GeneticEvolutionInfo",
                    "data": {"type": "rows", "items": [{"name": "No data"}]},
                },
                {
                    "type": "SQ.EngineCharts.HeapMemoryChart",
                    "data": {
                        "type": "chart",
                        "chart": {
                            "data": {
                                "datasets": [
                                    {"label": "Memory Usage", "data": [1, 2, 3]},
                                    {"label": "Heap Size", "data": [{"y": 4}, {"y": 5}, {"y": 6}]},
                                ],
                            },
                        },
                    },
                },
                {
                    "type": "SQ.EngineCharts.DatabankFitnessIS",
                    "data": {
                        "type": "chart",
                        "chart": {
                            "data": {
                                "datasets": [{"label": "Top Strategy", "data": [0.1, 0.2]}],
                            },
                        },
                    },
                },
                {
                    "type": "SQ.EngineCharts.AverageStrategiesPerHourChart",
                    "data": {
                        "type": "chart",
                        "chart": {
                            "data": {
                                "datasets": [{"label": "Avg. strategies per hour", "data": [7, 8]}],
                            },
                        },
                    },
                },
            ],
        })
        self.assertEqual(
            [frame["title"] for frame in frames],
            ["Average strategies per hour", "Heap memory chart"],
        )
        self.assertEqual(frames[1]["series"][1]["values"], [4.0, 5.0, 6.0])
        self.assertEqual(engine_chart_frames({"charts": []}), [])
        self.assertEqual(engine_chart_frames(None), [])

    def test_engine_chart_frames_follow_official_settings(self) -> None:
        payload = {
            "charts": [
                {
                    "type": "HeapMemoryChart",
                    "data": {
                        "type": "chart",
                        "chart": {"data": {"datasets": [{"label": "Memory Usage", "data": [1, 2]}]}},
                    },
                },
                {
                    "type": "GeneticEvolutionInfo",
                    "data": {
                        "type": "rows",
                        "items": [{"name": "No data", "value": "No genetic evolution running"}],
                    },
                },
            ],
        }
        frames = engine_chart_frames(
            payload,
            settings=["GeneticEvolutionInfo", "DatabankFitnessIS"],
            type_names={"GeneticEvolutionInfo": "Genetic Evolution info", "DatabankFitnessIS": "Databank Fitness - IS"},
        )
        self.assertEqual(frames[0]["kind"], "rows")
        self.assertEqual(frames[0]["items"][0]["value"], "No genetic evolution running")
        self.assertEqual(frames[1]["title"], "Databank Fitness - IS")
        self.assertEqual(frames[1]["series"], [])

    def test_list_and_save_engine_chart_types_use_official_servlets(self) -> None:
        types = [
            {"type": "AverageStrategiesPerHourChart", "name": "Average strategies per hour"},
            {"type": "HeapMemoryChart", "name": "Heap memory chart"},
        ]
        seen: list[str] = []

        def opener(request, timeout=None):
            parsed = urlparse(request.full_url)
            seen.append(f"{parsed.path}?{parsed.query}")
            if parsed.path == "/engine/getTypes":
                payload = {"types": types, "settings": ["HeapMemoryChart"]}
            elif parsed.path == "/engine/saveSelection":
                payload = {"ok": True}
            else:
                raise AssertionError(parsed.path)
            class _Resp:
                status = 200
                def read(self):
                    return json.dumps(payload).encode("utf-8")
                def __enter__(self):
                    return self
                def __exit__(self, *_args):
                    return None
            return _Resp()

        with TemporaryDirectory() as tmp:
            home = Path(tmp)
            (home / "internal/web/SQUANT").mkdir(parents=True)
            (home / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
            (home / "internal/SQUANT.dat").write_bytes(b"144fixture")
            settings = home / "user/settings/settings.xml"
            settings.parent.mkdir(parents=True)
            settings.write_text(
                "<Settings><WebServerPortUsed>8080</WebServerPortUsed><BrowserToken>tokentokentoken12</BrowserToken></Settings>",
                encoding="utf-8",
            )
            catalog = list_engine_chart_types(home, "Builder", opener=opener)
            self.assertEqual(catalog["settings"], ["HeapMemoryChart", "AverageStrategiesPerHourChart"])
            saved = save_engine_chart_selection(home, "Builder", 1, "HeapMemoryChart", opener=opener)
            self.assertEqual(saved["type"], "HeapMemoryChart")
            self.assertEqual(saved["number"], 1)
            with self.assertRaises(SqxNativeWebError):
                save_engine_chart_selection(home, "Builder", 0, "InventedChart", opener=opener)
            self.assertTrue(any(path.startswith("/engine/saveSelection") for path in seen))
            query = parse_qs(urlparse("http://x" + [path for path in seen if "saveSelection" in path][0]).query)
            self.assertEqual(query["type"], ["HeapMemoryChart"])
            self.assertEqual(query["number"], ["1"])


if __name__ == "__main__":
    unittest.main()
