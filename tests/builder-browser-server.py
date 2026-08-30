"""Test-only SPA server exercising the real Builder HTTP adapter.

This is not a second product server. It exists only to prove the Candidates browser
flow before the canonical ``app_server.py`` seam is released by Recovery Vertical 1.
"""

from __future__ import annotations

import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from tradercockpit.builder.http import (
    builder_http_get_response,
    builder_http_post_response,
)


MAX_JSON_BODY = 1024 * 1024


def make_handler(web_root: Path, state_root: Path):
    directory = str(web_root.resolve())

    class Handler(SimpleHTTPRequestHandler):
        extensions_map = {
            **SimpleHTTPRequestHandler.extensions_map,
            ".js": "text/javascript; charset=utf-8",
            ".mjs": "text/javascript; charset=utf-8",
        }

        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=directory, **kwargs)

        def end_headers(self) -> None:
            self.send_header("cache-control", "no-store")
            super().end_headers()

        def _json(self, status: int, payload: dict[str, object]) -> None:
            body = json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
            self.send_response(status)
            self.send_header("content-type", "application/json; charset=utf-8")
            self.send_header("content-length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _json_body(self) -> object:
            raw_length = self.headers.get("content-length")
            if raw_length is None:
                raise ValueError("Content-Length is required")
            try:
                length = int(raw_length)
            except ValueError as exc:
                raise ValueError("Content-Length must be an integer") from exc
            if length <= 0 or length > MAX_JSON_BODY:
                raise ValueError("JSON request body length is invalid")
            content_type = self.headers.get("content-type", "").split(";", 1)[0].strip().lower()
            if content_type != "application/json":
                raise ValueError("Content-Type must be application/json")
            try:
                return json.loads(self.rfile.read(length).decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise ValueError("request body must be valid UTF-8 JSON") from exc

        def do_GET(self) -> None:  # noqa: N802
            parsed = urlsplit(self.path)
            builder = builder_http_get_response(
                state_root,
                parsed.path,
                parse_qs(parsed.query, keep_blank_values=True),
            )
            if builder is not None:
                self._json(*builder)
                return
            if parsed.path.startswith("/api/"):
                self._json(404, {"error": "not_found", "detail": "unknown API path"})
                return
            if parsed.path == "/" or not Path(parsed.path).suffix:
                self.path = "/index.html"
            super().do_GET()

        def do_POST(self) -> None:  # noqa: N802
            parsed = urlsplit(self.path)
            if parsed.path == "/api/builder-searches":
                try:
                    request = self._json_body()
                except ValueError as exc:
                    self._json(400, {"error": "invalid_request", "detail": str(exc)})
                    return
                builder = builder_http_post_response(state_root, parsed.path, request)
                if builder is None:
                    raise AssertionError("Builder start path was not claimed by adapter")
                self._json(*builder)
                return
            if parsed.path.startswith("/api/"):
                self._json(404, {"error": "not_found", "detail": "unknown API path"})
                return
            self._json(405, {"error": "method_not_allowed", "detail": "POST is only available for product API actions"})

    return Handler


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=4175)
    parser.add_argument("--web-root", type=Path, default=Path(__file__).resolve().parents[1] / "web")
    parser.add_argument("--state-root", type=Path, required=True)
    args = parser.parse_args()
    args.state_root.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(
        (args.host, args.port),
        make_handler(args.web_root, args.state_root),
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
