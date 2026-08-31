"""Canonical read-only application server for the clean desktop baseline."""

from __future__ import annotations

import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from tradercockpit.sqx_builder_config import (
    SqxBuilderConfigError,
    builder_project_config_record,
)
from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTopologyError,
    custom_project_topology_record,
)
from tradercockpit.sqx_outputs import discover_sqx_outputs
from tradercockpit.sqx_presets import get_sqx_preset, preset_catalog, preset_record


SQX_PRESETS_API_PATH = "/api/sqx-presets"
SQX_OUTPUTS_API_PATH = "/api/sqx-outputs"
SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config"
SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology"
_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"


def sqx_preset_response(
    sqx_home: Path | str | None,
    preset_id: str | None = None,
) -> tuple[int, dict[str, object]]:
    if preset_id is None:
        return 200, preset_catalog(sqx_home)
    if not isinstance(preset_id, str) or not preset_id:
        return 400, {"error": "invalid_request", "detail": "presetId must be a non-empty string"}
    try:
        descriptor = get_sqx_preset(preset_id)
    except KeyError:
        return 404, {"error": "not_found", "detail": "unknown SQX preset"}
    return 200, {
        "schema": "tc.sqx-preset.v1",
        "preset": preset_record(descriptor, sqx_home),
    }


def sqx_builder_config_response(
    sqx_home: Path | str | None,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, builder_project_config_record(sqx_home)
    except SqxBuilderConfigError as exc:
        status = 503 if exc.code in {"runtime_not_configured", "builder_project_missing"} else 409
        return status, {
            "error": "producer_not_configured" if status == 503 else "invalid_state",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        # verified_sqx_home uses its own typed runtime error; keep the HTTP boundary fail closed.
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def sqx_project_topology_response(
    sqx_home: Path | str | None,
    project: str,
) -> tuple[int, dict[str, object]]:
    if not isinstance(project, str) or not project:
        return 400, {"error": "invalid_request", "detail": "project must be a non-empty string"}
    try:
        return 200, custom_project_topology_record(sqx_home, project)
    except SqxCustomProjectTopologyError as exc:
        if exc.code == "custom_project_missing":
            status, error = 404, "not_found"
        elif exc.code in {"custom_project_name_invalid"}:
            status, error = 400, "invalid_request"
        elif exc.code in {"runtime_not_configured"}:
            status, error = 503, "producer_not_configured"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except Exception as exc:
        code = getattr(exc, "code", "runtime_invalid")
        detail = getattr(exc, "detail", str(exc))
        return 503, {
            "error": "producer_not_configured",
            "reason_code": str(code),
            "detail": str(detail),
        }


def make_handler(
    web_root: Path,
    sqx_home: Path | str | None = None,
):
    """Create the one canonical HTTP handler used by server and desktop."""

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

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path == SQX_PRESETS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"presetId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                preset_ids = query.get("presetId", [])
                if len(preset_ids) > 1:
                    self._json(400, {"error": "invalid_request", "detail": "at most one presetId is allowed"})
                    return
                status, payload = sqx_preset_response(sqx_home, preset_ids[0] if preset_ids else None)
                self._json(status, payload)
                return

            if parsed.path == SQX_OUTPUTS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "SQX output discovery accepts no query parameters"})
                    return
                self._json(200, discover_sqx_outputs(sqx_home))
                return

            if parsed.path == SQX_BUILDER_CONFIG_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Builder config read accepts no query parameters"})
                    return
                status, payload = sqx_builder_config_response(sqx_home)
                self._json(status, payload)
                return

            if parsed.path == SQX_PROJECT_TOPOLOGY_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) != {"project"} or len(query.get("project", [])) != 1 or not query["project"][0]:
                    self._json(400, {"error": "invalid_request", "detail": "exactly one non-empty project parameter is required"})
                    return
                status, payload = sqx_project_topology_response(sqx_home, query["project"][0])
                self._json(status, payload)
                return

            if parsed.path.startswith("/api/"):
                self._json(404, {"error": "not_found", "detail": "unknown API path"})
                return

            if parsed.path == "/" or not Path(parsed.path).suffix:
                self.path = "/index.html"
            super().do_GET()

        def do_POST(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path.startswith("/api/"):
                self._json(
                    405,
                    {
                        "error": "method_not_allowed",
                        "reason_code": "read_only_baseline",
                        "detail": "native mutation is disabled until the trusted native gateway is implemented",
                    },
                )
                return
            self._json(405, {"error": "method_not_allowed", "detail": "POST is not supported"})

    return Handler


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Serve TraderCockpit")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "4173")))
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
        help="Authorized SQX installation used for read-only native inspection.",
    )
    args = parser.parse_args(argv)
    if not args.web_root.is_dir():
        parser.error(f"web root does not exist: {args.web_root}")

    server = ThreadingHTTPServer(
        (args.host, args.port),
        make_handler(args.web_root, args.sqx_home),
    )
    print(f"TraderCockpit listening on http://{args.host}:{args.port}")
    if args.sqx_home is None:
        print("Native SQX inspection unavailable: set SQX_HOME or --sqx-home")
    print("Native SQX mutation is disabled in the clean baseline")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
