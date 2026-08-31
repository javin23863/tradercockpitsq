"""Thin desktop host for the canonical TraderCockpit server and web UI.

The desktop owns only local server/window lifecycle and browser-local security.
Product state, native integration, accounts, and future mutation logic remain in
the canonical backend.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from http.server import ThreadingHTTPServer
import os
from pathlib import Path
from threading import Thread
from typing import Callable
from urllib.parse import urlsplit

from tradercockpit.app_server import make_handler
from tradercockpit.sqx_runtime import SQX_LAUNCHER_SHA256_ENV


_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"
_DEFAULT_START_PATH = "/home"
_DESKTOP_LOOPBACK_HOST = "127.0.0.1"
WindowRunner = Callable[[str, str, int, int], None]


@dataclass
class DesktopRuntime:
    server: ThreadingHTTPServer
    thread: Thread
    url: str

    def close(self) -> None:
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)
        if self.thread.is_alive():
            raise RuntimeError("TraderCockpit desktop server did not stop cleanly")


def _normalized_start_path(value: str) -> str:
    if not isinstance(value, str) or not value.startswith("/"):
        raise ValueError("desktop start path must begin with '/'")
    if "?" in value or "#" in value:
        raise ValueError("desktop start path must not contain query or fragment data")
    return value


def _desktop_handler(
    web_root: Path,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
):
    """Wrap the canonical handler with desktop browser-local protections."""

    canonical_handler = make_handler(web_root, sqx_home, trusted_launcher_sha256)

    class DesktopHandler(canonical_handler):
        def _expected_host(self) -> str:
            return f"{_DESKTOP_LOOPBACK_HOST}:{self.server.server_port}"

        def _desktop_host_is_valid(self) -> bool:
            host = self.headers.get("Host") or ""
            return host.casefold() == self._expected_host().casefold()

        def _browser_mutation_is_same_origin(self) -> bool:
            if not self._desktop_host_is_valid():
                return False
            origin = self.headers.get("Origin")
            if origin is None:
                return True
            parsed = urlsplit(origin)
            return (
                parsed.scheme == "http"
                and bool(parsed.netloc)
                and parsed.netloc.casefold() == self._expected_host().casefold()
            )

        def _reject_desktop_request(self, reason_code: str, detail: str) -> None:
            self._json(
                403,
                {
                    "error": "forbidden",
                    "reason_code": reason_code,
                    "detail": detail,
                },
            )

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            if not self._desktop_host_is_valid():
                self._reject_desktop_request(
                    "invalid_desktop_host",
                    "desktop requests require the exact loopback Host",
                )
                return
            super().do_GET()

        def do_POST(self) -> None:  # noqa: N802 - stdlib handler API
            if not self._desktop_host_is_valid():
                self._reject_desktop_request(
                    "invalid_desktop_host",
                    "desktop requests require the exact loopback Host",
                )
                return
            if not self._browser_mutation_is_same_origin():
                self._reject_desktop_request(
                    "cross_origin_mutation",
                    "desktop browser mutations require the TraderCockpit same origin",
                )
                return
            super().do_POST()

    return DesktopHandler


def start_desktop_server(
    *,
    web_root: Path | str = _DEFAULT_WEB_ROOT,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    port: int = 0,
    start_path: str = _DEFAULT_START_PATH,
) -> DesktopRuntime:
    """Start the canonical app server on loopback for one desktop lifecycle."""

    root = Path(web_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError(f"web root does not exist: {root}")
    if not isinstance(port, int) or isinstance(port, bool) or not 0 <= port <= 65535:
        raise ValueError("desktop port must be an integer from 0 through 65535")
    path = _normalized_start_path(start_path)

    server = ThreadingHTTPServer(
        (_DESKTOP_LOOPBACK_HOST, port),
        _desktop_handler(root, sqx_home, trusted_launcher_sha256),
    )
    server.daemon_threads = True
    thread = Thread(
        target=server.serve_forever,
        name="tradercockpit-desktop-server",
        daemon=True,
    )
    thread.start()

    _actual_host, actual_port = server.server_address[:2]
    return DesktopRuntime(
        server=server,
        thread=thread,
        url=f"http://{_DESKTOP_LOOPBACK_HOST}:{actual_port}{path}",
    )


def _pywebview_window(title: str, url: str, width: int, height: int) -> None:
    try:
        import webview
    except ImportError as exc:  # pragma: no cover - depends on optional desktop extra
        raise RuntimeError(
            "Desktop support is not installed. Install TraderCockpit with the 'desktop' extra."
        ) from exc

    webview.create_window(
        title,
        url,
        width=width,
        height=height,
        min_size=(960, 640),
    )
    webview.start()


def run_desktop(
    *,
    web_root: Path | str = _DEFAULT_WEB_ROOT,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    port: int = 0,
    start_path: str = _DEFAULT_START_PATH,
    title: str = "TraderCockpit — Development",
    width: int = 1440,
    height: int = 900,
    window_runner: WindowRunner = _pywebview_window,
) -> None:
    runtime = start_desktop_server(
        web_root=web_root,
        sqx_home=sqx_home,
        trusted_launcher_sha256=trusted_launcher_sha256,
        port=port,
        start_path=start_path,
    )
    try:
        window_runner(title, runtime.url, width, height)
    finally:
        runtime.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Launch the TraderCockpit development desktop")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--start-path", default=_DEFAULT_START_PATH)
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
    )
    parser.add_argument(
        "--sqx-launcher-sha256",
        default=os.environ.get(SQX_LAUNCHER_SHA256_ENV),
        help="Server-side trusted SHA-256 for the installed sqcli.exe launcher.",
    )
    parser.add_argument("--title", default="TraderCockpit — Development")
    parser.add_argument("--width", type=int, default=1440)
    parser.add_argument("--height", type=int, default=900)
    args = parser.parse_args(argv)

    if args.width < 960 or args.height < 640:
        parser.error("desktop dimensions must be at least 960x640")

    run_desktop(
        web_root=args.web_root,
        sqx_home=args.sqx_home,
        trusted_launcher_sha256=args.sqx_launcher_sha256,
        port=args.port,
        start_path=args.start_path,
        title=args.title,
        width=args.width,
        height=args.height,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
