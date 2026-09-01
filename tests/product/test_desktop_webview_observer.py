from __future__ import annotations

from types import SimpleNamespace
import sys
import unittest
from unittest.mock import MagicMock, patch

from tradercockpit.desktop import (
    DESKTOP_WINDOW_OBSERVATION_SCHEMA,
    DESKTOP_WINDOW_OBSERVATION_STATE_KEY,
    _install_webview_observer,
    _pywebview_window,
)


class FakeState:
    def __init__(self) -> None:
        self.handlers = []

    def __iadd__(self, handler):
        self.handlers.append(handler)
        return self

    def fire(self, event_type: str, key: str, value: object) -> None:
        for handler in tuple(self.handlers):
            handler(event_type, key, value)


class FakeWindow:
    def __init__(self) -> None:
        self.state = FakeState()


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
        return window, SimpleNamespace(
            create_window=MagicMock(return_value=window),
            start=MagicMock(),
        )

    def test_observer_accepts_only_valid_window_observation_state_changes(self) -> None:
        window = FakeWindow()
        sink = MagicMock()

        _install_webview_observer(window, sink)

        self.assertEqual(len(window.state.handlers), 1)
        window.state.fire("delete", DESKTOP_WINDOW_OBSERVATION_STATE_KEY, self.settled_observation())
        window.state.fire("change", "unrelated", self.settled_observation())
        window.state.fire("change", DESKTOP_WINDOW_OBSERVATION_STATE_KEY, {"location_pathname": "/research"})
        sink.assert_not_called()

        window.state.fire("change", DESKTOP_WINDOW_OBSERVATION_STATE_KEY, self.settled_observation())
        sink.assert_called_once()
        observed = sink.call_args.args[0]
        self.assertEqual(observed["schema"], DESKTOP_WINDOW_OBSERVATION_SCHEMA)
        self.assertEqual(observed["surface_id"], "research")
        self.assertEqual(observed["research_stage_id"], "construct")
        self.assertEqual(observed["research_tab_id"], "idea")

    def test_windows_window_starts_normally_after_registering_state_observer(self) -> None:
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
        self.assertEqual(len(window.state.handlers), 1)

    def test_non_windows_uses_same_state_observer_contract(self) -> None:
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
        self.assertEqual(len(window.state.handlers), 1)


if __name__ == "__main__":
    unittest.main()
