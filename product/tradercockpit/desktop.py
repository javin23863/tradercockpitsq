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
from tradercockpit.app_server import make_handler
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
_DEFAULT_START_PATH = "/home"
_DESKTOP_LOOPBACK_HOST = "127.0.0.1"
_WINDOWS_WEBVIEW_GUI = "edgechromium"
_WEBVIEW_OBSERVATION_TIMEOUT_SECONDS = 20.0
DESKTOP_LOOPBACK_ADVERT_NAME = "desktop-loopback.json"
DESKTOP_LOOPBACK_ADVERT_SCHEMA = "tc.desktop-loopback.v1"
DESKTOP_WINDOW_OBSERVATION_SCHEMA = "tc.desktop-window-observation.v1"
WindowRunner = Callable[[str, str, int, int], None]
WindowObservationSink = Callable[[dict[str, object]], None]


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


def _normalized_window_observation(value: object) -> dict[str, object] | None:
    if not isinstance(value, dict):
        return None
    strings = {
        "location_pathname": value.get("location_pathname"),
        "location_search": value.get("location_search"),
        "document_title": value.get("document_title"),
        "product_shell": value.get("product_shell"),
        "surface_id": value.get("surface_id"),
        "research_stage_id": value.get("research_stage_id"),
        "research_tab_id": value.get("research_tab_id"),
        "page_heading": value.get("page_heading"),
    }
    if any(not isinstance(item, str) for item in strings.values()):
        return None
    if not strings["location_pathname"].startswith("/"):
        return None
    idea_workspace = value.get("idea_workspace")
    idea_save_action = value.get("idea_save_action")
    if not isinstance(idea_workspace, bool) or not isinstance(idea_save_action, bool):
        return None
    return {
        "schema": DESKTOP_WINDOW_OBSERVATION_SCHEMA,
        **strings,
        "idea_workspace": idea_workspace,
        "idea_save_action": idea_save_action,
    }


def _record_window_observation(path: Path, observation: object) -> bool:
    """Atomically attach an actual WebView DOM observation to the desktop advert."""

    normalized = _normalized_window_observation(observation)
    if normalized is None or not path.is_file():
        return False
    try:
        advert = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError):
        return False
    if (
        not isinstance(advert, dict)
        or advert.get("schema") != DESKTOP_LOOPBACK_ADVERT_SCHEMA
        or advert.get("product") != "tradercockpit"
        or not isinstance(advert.get("url"), str)
    ):
        return False
    advert["window_observation"] = normalized
    temporary = path.with_name(path.name + ".window-observation.tmp")
    try:
        temporary.write_text(
            json.dumps(advert, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        os.replace(temporary, path)
    except OSError:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass
        return False
    return True


def _webview_observation(window) -> dict[str, object] | None:
    script = r"""
(() => {
  const shell = document.querySelector('[data-product-shell="tradercockpit-desktop"]');
  return {
    location_pathname: window.location.pathname || '',
    location_search: window.location.search || '',
    document_title: document.title || '',
    product_shell: shell?.getAttribute('data-product-shell') || '',
    surface_id: shell?.getAttribute('data-surface-id') || '',
    research_stage_id: shell?.getAttribute('data-research-stage-id') || '',
    research_tab_id: shell?.getAttribute('data-research-tab-id') || '',
    page_heading: document.querySelector('.content-inner h1')?.textContent?.trim() || '',
    idea_workspace: Boolean(document.querySelector('[data-research-idea-workspace]')),
    idea_save_action: Boolean(document.querySelector('[data-idea-action="save"]')),
  };
})()
"""
    try:
        return _normalized_window_observation(window.evaluate_js(script))
    except Exception:
        return None


def _observe_webview_until_settled(window, sink: WindowObservationSink) -> None:
    """Observe the actual WebView DOM from pywebview's backend worker thread."""

    last: dict[str, object] | None = None
    deadline = time.monotonic() + _WEBVIEW_OBSERVATION_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        observation = _webview_observation(window)
        if observation is not None:
            last = observation
            sink(observation)
            surface = observation["surface_id"]
            if observation["product_shell"] == "tradercockpit-desktop" and surface:
                if surface != "research":
                    return
                stage = observation["research_stage_id"]
                tab = observation["research_tab_id"]
                if stage == "proof":
                    return
                if stage and tab:
                    if not (stage == "construct" and tab == "idea"):
                        return
                    if observation["idea_workspace"] is True and observation["idea_save_action"] is True:
                        return
        time.sleep(0.05)
    if last is not None:
        sink(last)


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

    def record_window_observation(self, observation: object) -> bool:
        advert = self.loopback_advert_path
        return bool(advert is not None and _record_window_observation(advert, observation))

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
                    advert.with_name(advert.name + ".window-observation.tmp").unlink(missing_ok=True)
                except OSError:
                    pass
                self.loopback_advert_path = None

            if failures:
                raise DesktopLifecycleError(
                    "desktop shutdown incomplete: " + "; ".join(failures)
                )

            self._closed = True


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
    research_store: FileResearchCustodyStore,
):
    """Wrap the canonical handler with desktop browser-local protections."""

    canonical_handler = make_handler(
        web_root,
        sqx_home,
        trusted_launcher_sha256,
        research_store,
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
    start_path: str = _DEFAULT_START_PATH,
) -> DesktopRuntime:
    """Start the canonical app server on loopback for one desktop lifecycle."""

    root = Path(web_root).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError(f"web root does not exist: {root}")
    if not isinstance(port, int) or isinstance(port, bool) or not 0 <= port <= 65535:
        raise ValueError("desktop port must be an integer from 0 through 65535")
    path = _normalized_start_path(start_path)
    resolved_data_root = resolve_application_data_root(data_root)
    research_store = FileResearchCustodyStore(resolved_data_root)

    server = ThreadingHTTPServer(
        (_DESKTOP_LOOPBACK_HOST, port),
        _desktop_handler(
            root,
            sqx_home,
            trusted_launcher_sha256,
            research_store,
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
        loopback_advert_path=_write_loopback_advert(resolved_data_root, url),
    )


def _pywebview_window(
    title: str,
    url: str,
    width: int,
    height: int,
    *,
    observation_sink: WindowObservationSink | None = None,
) -> None:
    try:
        import webview
    except ImportError as exc:  # pragma: no cover - depends on optional desktop extra
        raise RuntimeError(
            "Desktop support is not installed. Install TraderCockpit with the 'desktop' extra."
        ) from exc

    window = webview.create_window(
        title,
        url,
        width=width,
        height=height,
        min_size=(960, 640),
    )
    if sys.platform == "win32":
        if observation_sink is None:
            webview.start(gui=_WINDOWS_WEBVIEW_GUI)
        else:
            webview.start(
                _observe_webview_until_settled,
                window,
                observation_sink,
                gui=_WINDOWS_WEBVIEW_GUI,
            )
    elif observation_sink is None:
        webview.start()
    else:
        webview.start(_observe_webview_until_settled, window, observation_sink)


def run_desktop(
    *,
    web_root: Path | str = _DEFAULT_WEB_ROOT,
    data_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    port: int = 0,
    start_path: str = _DEFAULT_START_PATH,
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
        window_title = title or default_window_title()
        if window_runner is _pywebview_window:
            _pywebview_window(
                window_title,
                runtime.url,
                width,
                height,
                observation_sink=runtime.record_window_observation,
            )
        else:
            window_runner(window_title, runtime.url, width, height)
    finally:
        runtime.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Launch the TraderCockpit desktop")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--start-path", default=_DEFAULT_START_PATH)
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
