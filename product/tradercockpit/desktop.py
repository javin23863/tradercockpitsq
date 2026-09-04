"""Thin desktop host for the canonical TraderCockpit server and web UI.

The desktop owns local server/window/worker lifecycle and browser-local security.
Product state, native integration, accounts, and feature logic remain in the
canonical backend.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from http.server import ThreadingHTTPServer
import json
import os
from pathlib import Path
import socket
import sys
from threading import Lock, Thread
import time
from typing import Callable
from urllib.parse import urlsplit

from tradercockpit.app_data import resolve_application_data_root
from tradercockpit.app_server import TraderCockpitHTTPServer, make_handler
from tradercockpit.desktop_session import (
    DesktopSessionError,
    canonicalize_desktop_path,
    read_desktop_session,
)
from tradercockpit.desktop_lifecycle import (
    DEFAULT_WORKER_STOP_TIMEOUT_SECONDS,
    DesktopLifecycleError,
    DesktopWorkerSupervisor,
    OwnedProcess,
)
from tradercockpit.native_runtime_config import optional_native_runtime_config
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.sqx_runtime import SQX_LAUNCHER_SHA256_ENV


def _default_web_root() -> Path:
    """Resolve the one canonical web tree in source and frozen desktop layouts."""

    frozen_root = getattr(sys, "_MEIPASS", None)
    if isinstance(frozen_root, str) and frozen_root:
        return Path(frozen_root) / "web"
    return Path(__file__).resolve().parents[2] / "web"


_DEFAULT_WEB_ROOT = _default_web_root()
_DESKTOP_LOOPBACK_HOST = "127.0.0.1"
_WINDOWS_WEBVIEW_GUI = "edgechromium"
DESKTOP_LOOPBACK_ADVERT_NAME = "desktop-loopback.json"
DESKTOP_LOOPBACK_ADVERT_SCHEMA = "tc.desktop-loopback.v1"
WindowRunner = Callable[[str, str, int, int], None]


def _frozen_desktop() -> bool:
    return bool(getattr(sys, "frozen", False))


def default_window_title() -> str:
    return "TraderCockpit" if _frozen_desktop() else "TraderCockpit — Development"


def _loopback_ready_timeout_seconds() -> float:
    return 20.0 if _frozen_desktop() else 5.0


def wait_until_loopback_ready(url: str, *, timeout_seconds: float | None = None) -> None:
    """Block until the desktop loopback port accepts connections before opening WebView2.

    The readiness check is TCP-only. An in-process HTTP GET to this same
    ThreadingHTTPServer is reset on frozen Windows (RemoteDisconnected) and
    prevents the TraderCockpit window from opening.
    """

    if timeout_seconds is None:
        timeout_seconds = _loopback_ready_timeout_seconds()
    if not isinstance(timeout_seconds, (int, float)) or isinstance(timeout_seconds, bool) or timeout_seconds <= 0:
        raise ValueError("loopback ready timeout must be a positive number")
    parsed = urlsplit(url)
    host = parsed.hostname or _DESKTOP_LOOPBACK_HOST
    if parsed.port is None:
        raise ValueError("desktop loopback URL must include an explicit port")
    port = parsed.port
    deadline = time.monotonic() + float(timeout_seconds)
    last: Exception | None = None
    while time.monotonic() < deadline:
        try:
            with socket.create_connection((host, port), timeout=1):
                return
        except OSError as exc:
            last = exc
        time.sleep(0.05)
    raise RuntimeError("TraderCockpit loopback server did not become ready") from last


def _write_loopback_advert(data_root: Path, url: str) -> Path:
    path = data_root / DESKTOP_LOOPBACK_ADVERT_NAME
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(
            {
                "schema": DESKTOP_LOOPBACK_ADVERT_SCHEMA,
                "product": "tradercockpit",
                "url": url,
                "pid": os.getpid(),
            },
            indent=2,
            sort_keys=True,
        )
        + "\n",
        encoding="utf-8",
    )
    return path


@dataclass
class DesktopRuntime:
    server: ThreadingHTTPServer
    thread: Thread
    url: str
    workers: DesktopWorkerSupervisor = field(default_factory=DesktopWorkerSupervisor)
    loopback_advert_path: Path | None = None
    _close_lock: Lock = field(default_factory=Lock, init=False, repr=False)
    _server_closed: bool = field(default=False, init=False, repr=False)
    _closed: bool = field(default=False, init=False, repr=False)

    @property
    def closed(self) -> bool:
        with self._close_lock:
            return self._closed

    def register_worker(
        self,
        process: OwnedProcess,
        *,
        label: str,
        timeout_seconds: float = DEFAULT_WORKER_STOP_TIMEOUT_SECONDS,
    ) -> None:
        """Register a long-lived process that must not outlive this desktop."""

        self.workers.register(
            process,
            label=label,
            timeout_seconds=timeout_seconds,
        )

    def close(self) -> None:
        """Seal the lifecycle, stop the local server, then stop all owned workers."""

        with self._close_lock:
            if self._closed:
                return

            self.workers.seal()
            failures: list[str] = []

            if not self._server_closed:
                try:
                    self.server.shutdown()
                    self.server.server_close()
                    self.thread.join(timeout=5)
                    if self.thread.is_alive():
                        raise DesktopLifecycleError(
                            "TraderCockpit desktop server did not stop cleanly"
                        )
                    self._server_closed = True
                except Exception as exc:
                    failures.append(f"server: {exc}")

            try:
                self.workers.stop_all()
            except DesktopLifecycleError as exc:
                failures.append(f"workers: {exc}")

            advert = self.loopback_advert_path
            if advert is not None:
                try:
                    advert.unlink(missing_ok=True)
                except OSError:
                    pass
                self.loopback_advert_path = None

            if failures:
                raise DesktopLifecycleError(
                    "desktop shutdown incomplete: " + "; ".join(failures)
                )

            self._closed = True


def _normalized_start_path(value: str) -> str:
    try:
        return canonicalize_desktop_path(value)
    except DesktopSessionError as exc:
        raise ValueError("desktop start path must be a registered product route") from exc


def _desktop_handler(
    web_root: Path,
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None,
    research_store: FileResearchCustodyStore,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
):
    """Wrap the canonical handler with desktop browser-local protections."""

    canonical_handler = make_handler(
        web_root,
        sqx_home,
        trusted_launcher_sha256,
        research_store,
        register_worker=register_worker,
        worker_is_active=worker_is_active,
    )

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

        def log_message(self, format: str, *args) -> None:  # noqa: A002 - stdlib handler API
            # --windowed frozen EXEs have no console; stdlib logs to stderr during
            # send_response, which drops the client connection if stderr is missing.
            stream = getattr(sys, "stderr", None)
            if stream is None:
                return
            try:
                stream.write(
                    "%s - - [%s] %s\n"
                    % (self.address_string(), self.log_date_time_string(), format % args)
                )
            except (OSError, ValueError):
                return

        def log_error(self, format: str, *args) -> None:  # noqa: A002 - stdlib handler API
            self.log_message(format, *args)

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
    data_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    port: int = 0,
    start_path: str | None = None,
) -> DesktopRuntime:
    """Start the canonical app server on loopback for one desktop lifecycle."""

    root = Path(web_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError(f"web root does not exist: {root}")
    if not isinstance(port, int) or isinstance(port, bool) or not 0 <= port <= 65535:
        raise ValueError("desktop port must be an integer from 0 through 65535")
    resolved_data_root = resolve_application_data_root(data_root)
    path = (
        _normalized_start_path(start_path)
        if start_path is not None
        else str(read_desktop_session(resolved_data_root)["path"])
    )
    research_store = FileResearchCustodyStore(resolved_data_root)
    workers = DesktopWorkerSupervisor()

    def register_worker(
        process: OwnedProcess,
        *,
        label: str,
        timeout_seconds: float = DEFAULT_WORKER_STOP_TIMEOUT_SECONDS,
    ) -> None:
        workers.register(process, label=label, timeout_seconds=timeout_seconds)

    def worker_is_active(label: str) -> bool:
        return workers.is_active(label)

    server = TraderCockpitHTTPServer(
        (_DESKTOP_LOOPBACK_HOST, port),
        _desktop_handler(
            root,
            sqx_home,
            trusted_launcher_sha256,
            research_store,
            register_worker=register_worker,
            worker_is_active=worker_is_active,
        ),
    )
    server.daemon_threads = True
    thread = Thread(
        target=server.serve_forever,
        name="tradercockpit-desktop-server",
        daemon=True,
    )
    thread.start()

    _actual_host, actual_port = server.server_address[:2]
    url = f"http://{_DESKTOP_LOOPBACK_HOST}:{actual_port}{path}"
    return DesktopRuntime(
        server=server,
        thread=thread,
        url=url,
        workers=workers,
        loopback_advert_path=_write_loopback_advert(resolved_data_root, url),
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
    if sys.platform == "win32":
        webview.start(gui=_WINDOWS_WEBVIEW_GUI)
    else:
        webview.start()


def run_desktop(
    *,
    web_root: Path | str = _DEFAULT_WEB_ROOT,
    data_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    port: int = 0,
    start_path: str | None = None,
    title: str | None = None,
    width: int = 1440,
    height: int = 900,
    window_runner: WindowRunner = _pywebview_window,
) -> None:
    runtime = start_desktop_server(
        web_root=web_root,
        data_root=data_root,
        sqx_home=sqx_home,
        trusted_launcher_sha256=trusted_launcher_sha256,
        port=port,
        start_path=start_path,
    )
    try:
        wait_until_loopback_ready(runtime.url)
        window_runner(title or default_window_title(), runtime.url, width, height)
    finally:
        runtime.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Launch the TraderCockpit desktop")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument(
        "--start-path",
        default=None,
        help="Registered product route. When omitted, the last saved desktop session is restored.",
    )
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--data-root",
        type=Path,
        default=None,
        help="Trusted process-side application data-root override.",
    )
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
    parser.add_argument("--title", default=None)
    parser.add_argument("--width", type=int, default=1440)
    parser.add_argument("--height", type=int, default=900)
    args = parser.parse_args(argv)

    if args.width < 960 or args.height < 640:
        parser.error("desktop dimensions must be at least 960x640")

    data_root = resolve_application_data_root(args.data_root)
    configured_home, configured_sha256 = optional_native_runtime_config(data_root)

    run_desktop(
        web_root=args.web_root,
        data_root=data_root,
        sqx_home=args.sqx_home if args.sqx_home is not None else configured_home,
        trusted_launcher_sha256=(
            args.sqx_launcher_sha256
            if args.sqx_launcher_sha256
            else configured_sha256
        ),
        port=args.port,
        start_path=args.start_path,
        title=args.title or default_window_title(),
        width=args.width,
        height=args.height,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
