from inspect import signature
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlsplit
from urllib.request import Request, urlopen

from tradercockpit.desktop import (
    DESKTOP_LOOPBACK_ADVERT_NAME,
    _default_web_root,
    _pywebview_window,
    default_window_title,
    main as desktop_main,
    run_desktop,
    start_desktop_server,
    wait_until_loopback_ready,
)
from tradercockpit.desktop_session import write_desktop_session
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

    def start(self, tmp: str, **kwargs):
        return start_desktop_server(
            web_root=self.web_root(tmp),
            data_root=Path(tmp) / "data",
            **kwargs,
        )

    def test_desktop_server_serves_canonical_spa_and_stops_cleanly(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = self.start(tmp)
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
            runtime = self.start(tmp)
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
            runtime = self.start(tmp)
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
            runtime = self.start(tmp)
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
                data_root=Path(tmp) / "data",
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
            runtime = self.start(tmp)
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

    def test_default_launch_restores_saved_research_session(self):
        configuration = "tc-research:configuration:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        with tempfile.TemporaryDirectory() as tmp:
            data_root = Path(tmp) / "data"
            write_desktop_session(
                data_root,
                f"/research?workspace=evolution&configuration={configuration}",
            )
            runtime = self.start(tmp)
            try:
                parsed = urlsplit(runtime.url)
                self.assertEqual(parsed.path, "/research")
                self.assertEqual(parse_qs(parsed.query)["workspace"], ["evolution"])
                self.assertEqual(parse_qs(parsed.query)["configuration"], [configuration])
            finally:
                runtime.close()

    def test_explicit_start_path_wins_over_saved_session(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_desktop_session(Path(tmp) / "data", "/research?workspace=evolution")
            runtime = start_desktop_server(
                web_root=self.web_root(tmp),
                data_root=Path(tmp) / "data",
                start_path="/home",
            )
            try:
                self.assertTrue(runtime.url.endswith("/home"))
            finally:
                runtime.close()

    def test_invalid_start_path_port_and_missing_web_root_refuse(self):
        with tempfile.TemporaryDirectory() as tmp:
            web = self.web_root(tmp)
            with self.assertRaisesRegex(ValueError, "registered product route"):
                start_desktop_server(web_root=web, start_path="home")
            for port in (-1, 65536, True):
                with self.subTest(port=port):
                    with self.assertRaises(ValueError):
                        start_desktop_server(web_root=web, port=port)
            with self.assertRaises(FileNotFoundError):
                start_desktop_server(web_root=Path(tmp) / "missing")

    def test_ordinary_startup_does_not_invoke_native_sqx_launcher(self):
        with tempfile.TemporaryDirectory() as tmp, patch(
            "tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_builder",
            side_effect=AssertionError("native SQX builder launched during ordinary startup"),
        ), patch(
            "tradercockpit.sqx_gateway.SqxNativeControlGateway.launch_retester_task",
            side_effect=AssertionError("native SQX retester launched during ordinary startup"),
        ):
            runtime = self.start(tmp)
            try:
                advert = Path(tmp) / "data" / DESKTOP_LOOPBACK_ADVERT_NAME
                self.assertTrue(advert.is_file())
                payload = json.loads(advert.read_text(encoding="utf-8"))
                self.assertEqual(payload["schema"], "tc.desktop-loopback.v1")
                self.assertEqual(payload["url"], runtime.url)
                self.assertTrue(runtime.url.endswith("/home"))
                with urlopen(runtime.url, timeout=2) as response:
                    self.assertIn("tradercockpit-desktop", response.read().decode("utf-8"))
                parsed = urlsplit(runtime.url)
                with urlopen(f"{parsed.scheme}://{parsed.netloc}/api/status", timeout=2) as response:
                    status = json.loads(response.read().decode("utf-8"))
                self.assertEqual(status["schema"], "tc.runtime-status.v1")
                self.assertEqual(status["application"]["desktop"], "canonical-server-ui")
            finally:
                runtime.close()
            self.assertFalse((Path(tmp) / "data" / DESKTOP_LOOPBACK_ADVERT_NAME).exists())

    def test_run_desktop_waits_for_loopback_before_opening_window(self):
        with tempfile.TemporaryDirectory() as tmp:
            order: list[str] = []

            def window_runner(title, url, width, height):
                order.append("window")
                with urlopen(url, timeout=2) as response:
                    self.assertEqual(response.status, 200)

            original = start_desktop_server

            def wrapped(**kwargs):
                order.append("server")
                runtime = original(**kwargs)
                order.append("server-ready")
                return runtime

            with patch("tradercockpit.desktop.start_desktop_server", side_effect=wrapped):
                run_desktop(
                    web_root=self.web_root(tmp),
                    data_root=Path(tmp) / "data",
                    window_runner=window_runner,
                )
            self.assertEqual(order, ["server", "server-ready", "window"])

    def test_frozen_window_title_is_tradercockpit_product_name(self):
        with patch("tradercockpit.desktop.sys.frozen", True, create=True):
            self.assertEqual(default_window_title(), "TraderCockpit")
        with patch("tradercockpit.desktop.sys.frozen", False, create=True):
            self.assertEqual(default_window_title(), "TraderCockpit — Development")

    def test_shutdown_does_not_stop_unregistered_sqx_like_process(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = self.start(tmp)
            independent = subprocess.Popen(
                [sys.executable, "-c", "import time; time.sleep(60)"],
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            try:
                runtime.close()
                self.assertIsNone(independent.poll())
            finally:
                if independent.poll() is None:
                    independent.kill()
                    independent.wait(timeout=5)
                if not runtime.closed:
                    runtime.close()

    def test_desktop_serves_home_when_stderr_is_missing(self):
        with tempfile.TemporaryDirectory() as tmp, patch("sys.stderr", None):
            runtime = self.start(tmp)
            try:
                with urlopen(runtime.url, timeout=2) as response:
                    self.assertIn("tradercockpit-desktop", response.read().decode("utf-8"))
            finally:
                runtime.close()

    def test_readiness_check_uses_the_local_server_even_when_http_proxy_is_set(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = self.start(tmp)
            try:
                with patch.dict(
                    os.environ,
                    {"HTTP_PROXY": "http://127.0.0.1:9", "http_proxy": "http://127.0.0.1:9"},
                    clear=False,
                ):
                    wait_until_loopback_ready(runtime.url, timeout_seconds=2)
            finally:
                runtime.close()

    def test_desktop_main_uses_process_runtime_resolution(self):
        home = Path("C:/discovered")
        digest = "a" * 64
        with tempfile.TemporaryDirectory() as tmp, patch(
            "tradercockpit.desktop.resolve_process_native_runtime",
            return_value=(home, digest),
        ) as resolve, patch("tradercockpit.desktop.run_desktop") as run:
            web = self.web_root(tmp)
            data = Path(tmp) / "data"
            data.mkdir()
            desktop_main(["--web-root", str(web), "--data-root", str(data)])
        resolve.assert_called_once()
        self.assertEqual(run.call_args.kwargs["sqx_home"], home)
        self.assertEqual(run.call_args.kwargs["trusted_launcher_sha256"], digest)


if __name__ == "__main__":
    unittest.main()
