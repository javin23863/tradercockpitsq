from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.native_plugins import (
    NativePluginError,
    load_native_plugin_catalog,
    plugin_package_path,
    plugin_runtime_state,
    stage_native_plugin,
)
from tradercockpit.sqx_presets import SqxPresetRuntimeError, verified_sqx_home


class NativePluginCatalogTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def test_catalog_packages_the_owner_supplied_sqx_plugins(self) -> None:
        catalog = load_native_plugin_catalog()
        ids = [item["id"] for item in catalog]
        self.assertEqual(
            ids,
            [
                "native.sqx-lab",
                "native.custom-block",
                "native.runcompare",
                "native.lucidflex-prop",
                "native.edge-decay",
                "native.two-step-challenge",
                "native.source-translator",
            ],
        )
        for entry in catalog:
            package = plugin_package_path(entry)
            self.assertIsNotNone(package, entry["id"])
            self.assertTrue(package.is_file(), entry["id"])
            self.assertTrue(str(entry["source_url"]).startswith("https://strategyquant.com/"))
            runtime = plugin_runtime_state(entry, None)
            if entry["kind"] == "authoring_skill":
                self.assertFalse(runtime["stageable"])
                self.assertEqual(runtime["status"], "packaged")
            else:
                self.assertTrue(runtime["stageable"])
                self.assertEqual(runtime["status"], "runtime_not_configured")

    def test_stage_runcompare_into_verified_runtime_skips_launcher_files(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            self.assertEqual(verified_sqx_home(home), home.resolve())
            result = stage_native_plugin("native.runcompare", home)
            dest = home / "user/extend/ResultsPlugins/RunCompare"
            self.assertTrue((dest / "index.html").is_file())
            self.assertTrue((dest / "server.ps1").is_file())
            self.assertFalse((home / "start-sq.bat").exists())
            self.assertFalse((dest / "start-sq.bat").exists())
            self.assertTrue(result["installed"])
            self.assertEqual(result["native_placement"], "user/extend/ResultsPlugins/RunCompare")
            runtime = plugin_runtime_state(
                next(item for item in load_native_plugin_catalog() if item["id"] == "native.runcompare"),
                home,
            )
            self.assertTrue(runtime["installed"])
            self.assertFalse(runtime["stageable"])

    def test_stage_source_translator_sxp_and_refuse_authoring_skills(self) -> None:
        with TemporaryDirectory() as tmp:
            home = self._runtime(Path(tmp))
            result = stage_native_plugin("native.source-translator", home)
            dest = home / "user/extend/ResultsPlugins/Source Code Translator/index.html"
            self.assertTrue(dest.is_file())
            self.assertEqual(result["native_placement"], "user/extend/ResultsPlugins/Source Code Translator")
            with self.assertRaises(NativePluginError) as raised:
                stage_native_plugin("native.sqx-lab", home)
            self.assertEqual(raised.exception.code, "native_plugin_not_stageable")
            with self.assertRaises(NativePluginError) as unknown:
                stage_native_plugin("native.does-not-exist", home)
            self.assertEqual(unknown.exception.code, "native_plugin_unknown")

    def test_stage_without_verified_runtime_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            with self.assertRaises(NativePluginError) as raised:
                stage_native_plugin("native.runcompare", Path(tmp))
            self.assertEqual(raised.exception.code, "sqx_build_markers_missing")
        with self.assertRaises(SqxPresetRuntimeError):
            verified_sqx_home(None)
