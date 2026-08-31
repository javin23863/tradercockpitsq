from inspect import signature
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

from tradercockpit.desktop import (
    _default_web_root,
    _pywebview_window,
    run_desktop,
    start_desktop_server,
)
from tradercockpit.desktop_lifecycle import DesktopLifecycleError


class DesktopRuntimeTests(unittest.TestCase):
    def web_root(self, root: str) -> Path:
        web = Path(root) / "web"
        web.mkdir()
        (web / "index.html").write_text(
            "<!doctype html><main data-product-shell='tradercockpit-desktop'>TraderCockpit</main>",
            encoding="utf-8",
        )
        return web

    def test_desktop_server_serves_canonical_spa_and_stops_cleanly(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            try:
                self.assertTrue(runtime.thread.is_alive())
                self.assertTrue(runtime.url.startswith("http://127.0.0.1:"))
                self.assertTrue(runtime.url.endswith("/home"))
                with urlopen(runtime.url, timeout=2) as response:
                    body = response.read().decode("utf-8")
                self.assertIn("tradercockpit-desktop", body)
            finally:
                runtime.close()
            self.assertFalse(runtime.thread.is_alive())
            self.assertTrue(runtime.closed)
            self.assertTrue(runtime.workers.sealed)
            runtime.close()  # idempotent after successful shutdown

    def test_desktop_server_has_no_network_bind_or_state_root_override(self):
        for function in (start_desktop_server, run_desktop):
            params = signature(function).parameters
            self.assertNotIn("host", params)
            self.assertNotIn("state_root", params)

    def test_frozen_desktop_resolves_bundled_canonical_web_tree(self):
        with tempfile.TemporaryDirectory() as tmp:
            with patch.object(sys, "_MEIPASS", tmp, create=True):
                self.assertEqual(_default_web_root(), Path(tmp) / "web")

    def test_windows_window_runner_forces_edgechromium_webview2(self):
        fake_webview = SimpleNamespace(
            create_window=MagicMock(),
            start=MagicMock(),
        )
        with patch.dict(sys.modules, {"webview": fake_webview}), patch(
            "tradercockpit.desktop.sys.platform",
            "win32",
        ):
            _pywebview_window("TraderCockpit Test", "http://127.0.0.1:4174/home", 1200, 760)

        fake_webview.create_window.assert_called_once_with(
            "TraderCockpit Test",
            "http://127.0.0.1:4174/home",
            width=1200,
            height=760,
            min_size=(960, 640),
        )
        fake_webview.start.assert_called_once_with(gui="edgechromium")

    def test_non_windows_window_runner_does_not_force_windows_renderer(self):
        fake_webview = SimpleNamespace(
            create_window=MagicMock(),
            start=MagicMock(),
        )
        with patch.dict(sys.modules, {"webview": fake_webview}), patch(
            "tradercockpit.desktop.sys.platform",
            "linux",
        ):
            _pywebview_window("TraderCockpit Test", "http://127.0.0.1:4174/home", 1200, 760)

        fake_webview.start.assert_called_once_with()

    def test_desktop_rejects_dns_rebinding_host(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            try:
                request = Request(runtime.url, headers={"Host": "attacker.invalid"})
                with self.assertRaises(HTTPError) as raised:
                    urlopen(request, timeout=2)
                self.assertEqual(raised.exception.code, 403)
                payload = json.loads(raised.exception.read().decode("utf-8"))
                self.assertEqual(payload["reason_code"], "invalid_desktop_host")
            finally:
                runtime.close()

    def test_desktop_rejects_cross_origin_browser_mutation(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            try:
                parsed = urlsplit(runtime.url)
                base = f"{parsed.scheme}://{parsed.netloc}"
                request = Request(
                    f"{base}/api/sqx-presets/foo/launch",
                    data=b"",
                    method="POST",
                    headers={"Origin": "https://example.invalid"},
                )
                with self.assertRaises(HTTPError) as raised:
                    urlopen(request, timeout=2)
                self.assertEqual(raised.exception.code, 403)
                payload = json.loads(raised.exception.read().decode("utf-8"))
                self.assertEqual(payload["reason_code"], "cross_origin_mutation")
            finally:
                runtime.close()

    def test_same_origin_post_reaches_read_only_canonical_router(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            try:
                parsed = urlsplit(runtime.url)
                base = f"{parsed.scheme}://{parsed.netloc}"
                request = Request(
                    f"{base}/api/sqx-presets/foo/launch",
                    data=b"",
                    method="POST",
                    headers={"Origin": base},
                )
                with self.assertRaises(HTTPError) as raised:
                    urlopen(request, timeout=2)
                self.assertEqual(raised.exception.code, 405)
                payload = json.loads(raised.exception.read().decode("utf-8"))
                self.assertEqual(payload["reason_code"], "read_only_baseline")
            finally:
                runtime.close()

    def test_run_desktop_uses_one_canonical_server_and_closes_it_when_window_returns(self):
        with tempfile.TemporaryDirectory() as tmp:
            calls = []

            def window_runner(title, url, width, height):
                calls.append((title, url, width, height))
                with urlopen(url, timeout=2) as response:
                    self.assertIn("TraderCockpit", response.read().decode("utf-8"))

            run_desktop(
                web_root=self.web_root(tmp),
                title="TraderCockpit Test",
                width=1200,
                height=760,
                window_runner=window_runner,
            )

            self.assertEqual(len(calls), 1)
            title, url, width, height = calls[0]
            self.assertEqual(title, "TraderCockpit Test")
            self.assertTrue(url.startswith("http://127.0.0.1:"))
            self.assertTrue(url.endswith("/home"))
            self.assertEqual((width, height), (1200, 760))
            with self.assertRaises(URLError):
                urlopen(url, timeout=0.5)

    def test_desktop_close_terminates_registered_real_worker(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            worker = subprocess.Popen(
                [sys.executable, "-c", "import time; time.sleep(60)"],
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            try:
                runtime.register_worker(
                    worker,
                    label="fixture-native-worker",
                    timeout_seconds=2,
                )
                self.assertEqual(runtime.workers.active_count, 1)

                runtime.close()

                self.assertIsNotNone(worker.poll())
                self.assertEqual(runtime.workers.active_count, 0)
                self.assertTrue(runtime.closed)
                with self.assertRaises(DesktopLifecycleError):
                    runtime.register_worker(worker, label="late-worker")
            finally:
                if worker.poll() is None:
                    worker.kill()
                    worker.wait(timeout=5)
                if not runtime.closed:
                    runtime.close()

    def test_invalid_start_path_port_and_missing_web_root_refuse(self):
        with tempfile.TemporaryDirectory() as tmp:
            web = self.web_root(tmp)
            with self.assertRaisesRegex(ValueError, "must begin"):
                start_desktop_server(web_root=web, start_path="home")
            for port in (-1, 65536, True):
                with self.subTest(port=port):
                    with self.assertRaises(ValueError):
                        start_desktop_server(web_root=web, port=port)
            with self.assertRaises(FileNotFoundError):
                start_desktop_server(web_root=Path(tmp) / "missing")


if __name__ == "__main__":
    unittest.main()
