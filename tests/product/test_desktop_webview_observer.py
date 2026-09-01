from __future__ import annotations

from types import SimpleNamespace
import sys
import unittest
from unittest.mock import MagicMock, patch

from tradercockpit.desktop import (
    DESKTOP_WINDOW_OBSERVATION_SCHEMA,
    _observe_webview_until_settled,
    _pywebview_window,
)


class DesktopWebViewObserverTests(unittest.TestCase):
    def settled_observation(self) -> dict[str, object]:
        return {
            "location_pathname": "/research",
            "location_search": "",
            "document_title": "TraderCockpit — Research / Construct / Idea",
            "product_shell": "tradercockpit-desktop",
            "surface_id": "research",
            "research_stage_id": "construct",
            "research_tab_id": "idea",
            "page_heading": "Idea",
            "idea_workspace": True,
            "idea_save_action": True,
        }

    def test_windows_observation_runs_through_pywebview_backend_worker_args_slot(self) -> None:
        window = SimpleNamespace(evaluate_js=MagicMock(return_value=self.settled_observation()))
        fake_webview = SimpleNamespace(
            create_window=MagicMock(return_value=window),
            start=MagicMock(),
        )
        sink = MagicMock()

        with patch.dict(sys.modules, {"webview": fake_webview}), patch(
            "tradercockpit.desktop.sys.platform",
            "win32",
        ):
            _pywebview_window(
                "TraderCockpit",
                "http://127.0.0.1:4174/research",
                1200,
                760,
                observation_sink=sink,
            )

        fake_webview.create_window.assert_called_once_with(
            "TraderCockpit",
            "http://127.0.0.1:4174/research",
            width=1200,
            height=760,
            min_size=(960, 640),
        )
        args, kwargs = fake_webview.start.call_args
        self.assertEqual(args, (_observe_webview_until_settled,))
        self.assertEqual(kwargs["args"], (window, sink))
        self.assertEqual(kwargs["gui"], "edgechromium")
        self.assertNotIn("localization", kwargs)

    def test_non_windows_observation_uses_the_same_backend_worker_args_slot(self) -> None:
        window = SimpleNamespace(evaluate_js=MagicMock(return_value=self.settled_observation()))
        fake_webview = SimpleNamespace(
            create_window=MagicMock(return_value=window),
            start=MagicMock(),
        )
        sink = MagicMock()

        with patch.dict(sys.modules, {"webview": fake_webview}), patch(
            "tradercockpit.desktop.sys.platform",
            "linux",
        ):
            _pywebview_window(
                "TraderCockpit",
                "http://127.0.0.1:4174/research",
                1200,
                760,
                observation_sink=sink,
            )

        args, kwargs = fake_webview.start.call_args
        self.assertEqual(args, (_observe_webview_until_settled,))
        self.assertEqual(kwargs, {"args": (window, sink)})

    def test_observer_publishes_the_actual_settled_research_dom(self) -> None:
        window = SimpleNamespace(evaluate_js=MagicMock(return_value=self.settled_observation()))
        observed: list[dict[str, object]] = []

        _observe_webview_until_settled(window, observed.append)

        self.assertEqual(len(observed), 1)
        self.assertEqual(observed[0]["schema"], DESKTOP_WINDOW_OBSERVATION_SCHEMA)
        self.assertEqual(observed[0]["location_pathname"], "/research")
        self.assertEqual(observed[0]["surface_id"], "research")
        self.assertEqual(observed[0]["research_stage_id"], "construct")
        self.assertEqual(observed[0]["research_tab_id"], "idea")
        self.assertIs(observed[0]["idea_workspace"], True)
        self.assertIs(observed[0]["idea_save_action"], True)


if __name__ == "__main__":
    unittest.main()
