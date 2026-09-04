from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError
from tradercockpit.sqx_results_plugins import (
    create_results_plugin,
    list_results_plugin_tabs,
    parse_results_plugin_request_path,
    read_results_plugin_file,
    results_plugin_create_state,
)


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


class SqxResultsPluginTests(unittest.TestCase):
    def test_serves_installed_plugin_index_and_refuses_escape(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            plugin = home / "user/extend/ResultsPlugins/Prop Monte Carlo"
            plugin.mkdir(parents=True)
            (plugin / "index.html").write_text("<html>prop-mc</html>", encoding="utf-8")
            (plugin / "locales").mkdir()
            (plugin / "locales/en.json").write_text('{"ok":true}', encoding="utf-8")
            tabs = list_results_plugin_tabs(home)
            body, content_type = read_results_plugin_file(home, "/api/sqx-results-plugin/Prop%20Monte%20Carlo/index.html")
            locale, locale_type = read_results_plugin_file(
                home, "/api/sqx-results-plugin/Prop Monte Carlo/locales/en.json"
            )
            with self.assertRaises(SqxCustomProjectTopologyError) as escaped:
                read_results_plugin_file(home, "/api/sqx-results-plugin/Prop%20Monte%20Carlo/../index.html")
        self.assertEqual(tabs[0]["id"], "prop-mc")
        self.assertTrue(tabs[0]["installed"])
        self.assertFalse(tabs[1]["installed"])
        self.assertIn(b"prop-mc", body)
        self.assertTrue(content_type.startswith("text/html"))
        self.assertEqual(locale, b'{"ok":true}')
        self.assertTrue(locale_type.startswith("application/json"))
        self.assertEqual(escaped.exception.code, "results_plugin_path_escape")

    def test_lists_extra_installed_plugins_and_copies_customplugin(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            root = home / "user/extend/ResultsPlugins"
            template = root / "CustomPlugin"
            template.mkdir(parents=True)
            (template / "index.html").write_text("<html>template</html>", encoding="utf-8")
            extra = root / "Edge Decay"
            extra.mkdir()
            (extra / "index.html").write_text("<html>edge</html>", encoding="utf-8")
            tabs = list_results_plugin_tabs(home)
            created = create_results_plugin(home, "My Analysis")
            again = list_results_plugin_tabs(home)
            with self.assertRaises(SqxCustomProjectTopologyError) as exists:
                create_results_plugin(home, "My Analysis")
            with self.assertRaises(SqxCustomProjectTopologyError) as template_name:
                create_results_plugin(home, "CustomPlugin")
            ids = [tab["id"] for tab in tabs]
            self.assertEqual(ids[:2], ["prop-mc", "prop-analytics"])
            self.assertIn("edge-decay", ids)
            self.assertTrue(results_plugin_create_state(home)["available"])
            self.assertEqual(created["folder"], "My Analysis")
            self.assertEqual(created["id"], "my-analysis")
            self.assertTrue((home / "user/extend/ResultsPlugins/My Analysis/index.html").is_file())
            self.assertIn("my-analysis", [tab["id"] for tab in again])
            self.assertEqual(exists.exception.code, "results_plugin_exists")
            self.assertEqual(template_name.exception.code, "results_plugin_name_invalid")

    def test_request_path_requires_a_folder(self) -> None:
        with self.assertRaises(SqxCustomProjectTopologyError) as raised:
            parse_results_plugin_request_path("/api/sqx-results-plugin")
        self.assertEqual(raised.exception.code, "results_plugin_name_invalid")
