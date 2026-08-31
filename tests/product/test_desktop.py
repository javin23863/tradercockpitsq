from pathlib import Path
import tempfile
import unittest
from urllib.request import urlopen

from tradercockpit.desktop import run_desktop, start_desktop_server


class DesktopRuntimeTests(unittest.TestCase):
    def web_root(self, root: str) -> Path:
        web = Path(root) / "web"
        web.mkdir()
        (web / "index.html").write_text(
            "<!doctype html><main data-product-shell='construct-backtest-proof'>TraderCockpit</main>",
            encoding="utf-8",
        )
        return web

    def test_desktop_server_serves_canonical_spa_and_stops_cleanly(self):
        with tempfile.TemporaryDirectory() as tmp:
            runtime = start_desktop_server(web_root=self.web_root(tmp))
            try:
                self.assertTrue(runtime.thread.is_alive())
                self.assertTrue(runtime.url.endswith("/home"))
                with urlopen(runtime.url, timeout=2) as response:
                    body = response.read().decode("utf-8")
                self.assertIn("construct-backtest-proof", body)
            finally:
                runtime.close()
            self.assertFalse(runtime.thread.is_alive())

    def test_run_desktop_uses_one_canonical_server_and_injected_window_runner(self):
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

    def test_invalid_start_path_refuses_before_server_start(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, "must begin"):
                start_desktop_server(web_root=self.web_root(tmp), start_path="home")

    def test_missing_web_root_refuses(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(FileNotFoundError):
                start_desktop_server(web_root=Path(tmp) / "missing")


if __name__ == "__main__":
    unittest.main()
