"""Thin desktop host for the canonical TraderCockpit application server and web UI.

The desktop layer owns only process/window lifecycle. Product state, SQX control,
account authority, and API behavior remain in the canonical TraderCockpit backend.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from http.server import ThreadingHTTPServer
import os
from pathlib import Path
from threading import Thread
from typing import Callable

from tradercockpit.app_server import make_handler


_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"
_DEFAULT_START_PATH = "/home"
WindowRunner = Callable[[str, str, int, int], None]


@dataclass
class DesktopRuntime:
    """Running canonical application server owned by one desktop lifecycle."""

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


def start_desktop_server(
    *,
    web_root: Path | str = _DEFAULT_WEB_ROOT,
    state_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
    host: str = "127.0.0.1",
    port: int = 0,
    start_path: str = _DEFAULT_START_PATH,
) -> DesktopRuntime:
    """Start the existing TraderCockpit server for one desktop window lifecycle."""

    root = Path(web_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError(f"web root does not exist: {root}")
    if not isinstance(host, str) or not host:
        raise ValueError("desktop host must be a non-empty string")
    if not isinstance(port, int) or isinstance(port, bool) or not 0 <= port <= 65535:
        raise ValueError("desktop port must be an integer from 0 through 65535")
    path = _normalized_start_path(start_path)

    server = ThreadingHTTPServer(
        (host, port),
        make_handler(root, state_root, sqx_home),
    )
    server.daemon_threads = True
    thread = Thread(
        target=server.serve_forever,
        name="tradercockpit-desktop-server",
        daemon=True,
    )
    thread.start()

    actual_host, actual_port = server.server_address[:2]
    display_host = "127.0.0.1" if actual_host in {"0.0.0.0", "::"} else actual_host
    return DesktopRuntime(
        server=server,
        thread=thread,
        url=f"http://{display_host}:{actual_port}{path}",
    )


def _pywebview_window(title: str, url: str, width: int, height: int) -> None:
    try:
        import webview
    except ImportError as exc:  # pragma: no cover - exercised only without desktop extra
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
    state_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
    host: str = "127.0.0.1",
    port: int = 0,
    start_path: str = _DEFAULT_START_PATH,
    title: str = "TraderCockpit — Development",
    width: int = 1440,
    height: int = 900,
    window_runner: WindowRunner = _pywebview_window,
) -> None:
    """Run one native window around the canonical TraderCockpit server/UI."""

    runtime = start_desktop_server(
        web_root=web_root,
        state_root=state_root,
        sqx_home=sqx_home,
        host=host,
        port=port,
        start_path=start_path,
    )
    try:
        window_runner(title, runtime.url, width, height)
    finally:
        runtime.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Launch the TraderCockpit development desktop")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--start-path", default=_DEFAULT_START_PATH)
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--state-root",
        type=Path,
        default=(
            Path(os.environ["TRADERCOCKPIT_STATE_ROOT"])
            if os.environ.get("TRADERCOCKPIT_STATE_ROOT")
            else None
        ),
    )
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
    )
    parser.add_argument("--title", default="TraderCockpit — Development")
    parser.add_argument("--width", type=int, default=1440)
    parser.add_argument("--height", type=int, default=900)
    args = parser.parse_args(argv)

    if args.width < 960 or args.height < 640:
        parser.error("desktop dimensions must be at least 960x640")

    run_desktop(
        web_root=args.web_root,
        state_root=args.state_root,
        sqx_home=args.sqx_home,
        host=args.host,
        port=args.port,
        start_path=args.start_path,
        title=args.title,
        width=args.width,
        height=args.height,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
