from __future__ import annotations

from types import SimpleNamespace
import sys
import unittest
from unittest.mock import MagicMock, patch

from tradercockpit.desktop import (
    DESKTOP_WINDOW_OBSERVATION_SCHEMA,
    _WEBVIEW_OBSERVATION_SCRIPT,
    _install_webview_observer,
    _pywebview_window,
)


class FakeEvent:
    def __init__(self) -> None:
        self.handlers = []

    def __iadd__(self, handler):
        self.handlers.append(handler)
        return self

    def fire(self, window) -> None:
        for handler in tuple(self.handlers):
            handler(window)


class FakeWindow:
    def __init__(self) -> None:
        self.events = SimpleNamespace(loaded=FakeEvent())
        self.run_js = MagicMock()
        self.exposed = []

    def expose(self, *functions) -> None:
        self.exposed.extend(functions)


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

    def fake_webview(self):
        window = FakeWindow()
        start = MagicMock(side_effect=lambda *args, **kwargs: window.events.loaded.fire(window))
        return window, SimpleNamespace(
            create_window=MagicMock(return_value=window),
            start=start,
        )

    def test_observer_exposes_validated_python_bridge_and_injects_csp_safe_script(self) -> None:
        window = FakeWindow()
        sink = MagicMock()

        _install_webview_observer(window, sink)

        self.assertEqual(len(window.exposed), 1)
        report = window.exposed[0]
        self.assertEqual(report.__name__, "report_window_observation")
        self.assertIs(report(self.settled_observation()), True)
        sink.assert_called_once()
        observed = sink.call_args.args[0]
        self.assertEqual(observed["schema"], DESKTOP_WINDOW_OBSERVATION_SCHEMA)
        self.assertEqual(observed["surface_id"], "research")

        sink.reset_mock()
        self.assertIs(report({"location_pathname": "/research"}), False)
        sink.assert_not_called()

        window.events.loaded.fire(window)
        window.run_js.assert_called_once_with(_WEBVIEW_OBSERVATION_SCRIPT)
        script = window.run_js.call_args.args[0]
        self.assertIn("pywebview?.api?.report_window_observation", script)
        self.assertIn("data-product-shell", script)
        self.assertIn("data-research-idea-workspace", script)
        self.assertNotIn("evaluate_js", script)

    def test_windows_window_starts_normally_after_registering_loaded_bridge(self) -> None:
        window, fake_webview = self.fake_webview()
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
        fake_webview.start.assert_called_once_with(gui="edgechromium")
        self.assertEqual(len(window.exposed), 1)
        window.run_js.assert_called_once_with(_WEBVIEW_OBSERVATION_SCRIPT)

    def test_non_windows_uses_same_loaded_bridge_contract(self) -> None:
        window, fake_webview = self.fake_webview()
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

        fake_webview.start.assert_called_once_with()
        self.assertEqual(len(window.exposed), 1)
        window.run_js.assert_called_once_with(_WEBVIEW_OBSERVATION_SCRIPT)


if __name__ == "__main__":
    unittest.main()
