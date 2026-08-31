"""Canonical application server for the TraderCockpit desktop."""

from __future__ import annotations

import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from ipaddress import ip_address
import json
import os
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from tradercockpit.app_data import resolve_application_data_root
from tradercockpit.research_configurations import (
    ResearchConfigurationError,
    approve_configuration,
    compile_current_builder_configuration,
    list_current_configurations,
    read_current_configuration,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_ideas import (
    ResearchIdeaError,
    create_idea,
    list_current_ideas,
    read_current_idea,
    revise_idea,
)
from tradercockpit.runtime_status import runtime_status_record
from tradercockpit.sqx_builder_config import (
    SqxBuilderConfigError,
    builder_project_config_record,
)
from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTopologyError,
    custom_project_topology_record,
)
from tradercockpit.sqx_outputs import discover_sqx_outputs
from tradercockpit.sqx_presets import (
    SqxPresetRuntimeError,
    get_sqx_preset,
    preset_catalog,
    preset_record,
)
from tradercockpit.sqx_runtime import SQX_LAUNCHER_SHA256_ENV


STATUS_API_PATH = "/api/status"
RESEARCH_IDEAS_API_PATH = "/api/research/ideas"
RESEARCH_CONFIGURATIONS_API_PATH = "/api/research/configurations"
SQX_PRESETS_API_PATH = "/api/sqx-presets"
SQX_OUTPUTS_API_PATH = "/api/sqx-outputs"
SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config"
SQX_PROJECT_TOPOLOGY_API_PATH = "/api/sqx-project-topology"
MAX_JSON_BODY_BYTES = 256_000
_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"


def _is_loopback_address(value: str) -> bool:
    try:
        return ip_address(value.split("%", 1)[0]).is_loopback
    except (AttributeError, ValueError):
        return False


def status_response(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None = None,
    research_store: FileResearchCustodyStore | None = None,
) -> tuple[int, dict[str, object]]:
    return 200, runtime_status_record(
        sqx_home,
        trusted_launcher_sha256,
        research_store_bound=research_store is not None,
    )


def research_ideas_response(
    research_store: FileResearchCustodyStore | None,
    entity_id: str | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        if entity_id is None:
            return 200, list_current_ideas(research_store)
        return 200, read_current_idea(research_store, entity_id)
    except ResearchIdeaError as exc:
        status = 409 if exc.code == "idea_content_corrupt" else 400
        return status, {
            "error": "invalid_state" if status == 409 else "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def research_idea_write_response(
    research_store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }

    keys = set(payload)
    is_create = "entity_id" not in payload and "expected_revision" not in payload
    if is_create:
        if not {"text"} <= keys or keys - {"text", "source"}:
            return 400, {
                "error": "invalid_request",
                "detail": "Idea creation accepts only text and optional source.",
            }
    else:
        required = {"entity_id", "expected_revision", "text"}
        if not required <= keys or keys - (required | {"source"}):
            return 400, {
                "error": "invalid_request",
                "detail": "Idea revision requires entity_id, expected_revision, text, and optional source.",
            }

    source = payload.get("source", "")
    try:
        if is_create:
            return 201, create_idea(
                research_store,
                text=payload["text"],  # type: ignore[arg-type]
                source=source,  # type: ignore[arg-type]
            )
        return 200, revise_idea(
            research_store,
            entity_id=payload["entity_id"],  # type: ignore[arg-type]
            expected_revision=payload["expected_revision"],  # type: ignore[arg-type]
            text=payload["text"],  # type: ignore[arg-type]
            source=source,  # type: ignore[arg-type]
        )
    except ResearchIdeaError as exc:
        return 400, {
            "error": "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_conflict":
            status, error = 409, "conflict"
        elif exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def research_configurations_response(
    research_store: FileResearchCustodyStore | None,
    entity_id: str | None = None,
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }
    try:
        if entity_id is None:
            return 200, list_current_configurations(research_store)
        return 200, read_current_configuration(research_store, entity_id)
    except ResearchConfigurationError as exc:
        status = 409 if exc.code == "configuration_content_corrupt" else 400
        return status, {
            "error": "invalid_state" if status == 409 else "invalid_request",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    except ResearchCustodyError as exc:
        if exc.code == "current_pointer_missing":
            status, error = 404, "not_found"
        elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
            status, error = 400, "invalid_request"
        else:
            status, error = 409, "invalid_state"
        return status, {
            "error": error,
            "reason_code": exc.code,
            "detail": exc.detail,
        }


def research_configuration_write_response(
    research_store: FileResearchCustodyStore | None,
    sqx_home: Path | str | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "store_not_configured",
            "reason_code": "research_store_not_bound",
            "detail": "Canonical research custody store is not bound.",
        }

    action = payload.get("action")
    if action == "compile":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "detail": "Configuration compile accepts only action=compile.",
            }
        try:
            return 201, compile_current_builder_configuration(research_store, sqx_home)
        except (SqxBuilderConfigError, SqxPresetRuntimeError) as exc:
            status = 503 if exc.code in {
                "runtime_not_configured",
                "builder_project_missing",
                "runtime_build_mismatch",
                "runtime_identity_missing",
            } else 409
            return status, {
                "error": "producer_not_configured" if status == 503 else "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        except ResearchConfigurationError as exc:
            return 409, {
                "error": "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        except ResearchCustodyError as exc:
            return 409, {
                "error": "invalid_state",
                "reason_code": exc.code,
                "detail": exc.detail,
            }

    if action == "approve":
        required = {"action", "entity_id", "expected_revision"}
        if set(payload) != required:
            return 400, {
                "error": "invalid_request",
                "detail": "Configuration approval requires action, entity_id, and expected_revision only.",
            }
        try:
            return 200, approve_configuration(
                research_store,
                entity_id=payload["entity_id"],  # type: ignore[arg-type]
                expected_revision=payload["expected_revision"],  # type: ignore[arg-type]
            )
        except ResearchConfigurationError as exc:
            status = 409 if exc.code in {
                "configuration_already_approved",
                "configuration_content_corrupt",
            } else 400
            return status, {
                "error": "invalid_state" if status == 409 else "invalid_request",
                "reason_code": exc.code,
                "detail": exc.detail,
            }
        except ResearchCustodyError as exc:
            if exc.code == "current_conflict":
                status, error = 409, "conflict"
            elif exc.code == "current_pointer_missing":
                status, error = 404, "not_found"
            elif exc.code in {"entity_id_invalid", "entity_kind_invalid", "revision_ref_invalid"}:
                status, error = 400, "invalid_request"
            else:
                status, error = 409, "invalid_state"
            return status, {
                "error": error,
                "reason_code": exc.code,
                "detail": exc.detail,
            }

    return 400, {
        "error": "invalid_request",
        "reason_code": "configuration_action_invalid",
        "detail": "Configuration action must be compile or approve.",
    }


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
    trusted_launcher_sha256: str | None = None,
    research_store: FileResearchCustodyStore | None = None,
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

        def _research_client_is_loopback(self) -> bool:
            return _is_loopback_address(str(self.client_address[0]))

        def _reject_non_loopback_research_request(self) -> None:
            self._json(
                403,
                {
                    "error": "forbidden",
                    "reason_code": "local_custody_only",
                    "detail": "Research custody is available only to loopback clients.",
                },
            )

        def _request_json(self) -> dict[str, object] | None:
            content_type = (self.headers.get("Content-Type") or "").split(";", 1)[0].strip().lower()
            if content_type != "application/json":
                self._json(415, {"error": "unsupported_media_type", "detail": "application/json is required"})
                return None
            raw_length = self.headers.get("Content-Length")
            try:
                length = int(raw_length) if raw_length is not None else -1
            except ValueError:
                length = -1
            if length <= 0 or length > MAX_JSON_BODY_BYTES:
                self._json(400, {"error": "invalid_request", "detail": "JSON body length is missing, empty, or too large"})
                return None
            raw = self.rfile.read(length)
            if len(raw) != length:
                self._json(400, {"error": "invalid_request", "detail": "JSON request body is incomplete"})
                return None
            try:
                payload = json.loads(raw)
            except (UnicodeDecodeError, json.JSONDecodeError):
                self._json(400, {"error": "invalid_request", "detail": "request body must be valid JSON"})
                return None
            if not isinstance(payload, dict):
                self._json(400, {"error": "invalid_request", "detail": "request body must be a JSON object"})
                return None
            return payload

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path == STATUS_API_PATH:
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "runtime status accepts no query parameters"})
                    return
                status, payload = status_response(sqx_home, trusted_launcher_sha256, research_store)
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_IDEAS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_ideas_response(research_store, entity_ids[0] if entity_ids else None)
                self._json(status, payload)
                return

            if parsed.path == RESEARCH_CONFIGURATIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                query = parse_qs(parsed.query, keep_blank_values=True)
                if set(query) - {"entityId"}:
                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})
                    return
                entity_ids = query.get("entityId", [])
                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):
                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})
                    return
                status, payload = research_configurations_response(
                    research_store,
                    entity_ids[0] if entity_ids else None,
                )
                self._json(status, payload)
                return

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
            if parsed.path == RESEARCH_IDEAS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Idea writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_idea_write_response(research_store, payload)
                self._json(status, response)
                return

            if parsed.path == RESEARCH_CONFIGURATIONS_API_PATH:
                if not self._research_client_is_loopback():
                    self._reject_non_loopback_research_request()
                    return
                if parsed.query:
                    self._json(400, {"error": "invalid_request", "detail": "Configuration writes accept no query parameters"})
                    return
                payload = self._request_json()
                if payload is None:
                    return
                status, response = research_configuration_write_response(research_store, sqx_home, payload)
                self._json(status, response)
                return

            if parsed.path.startswith("/api/"):
                self._json(
                    405,
                    {
                        "error": "method_not_allowed",
                        "reason_code": "read_only_baseline",
                        "detail": "This API route has no approved mutation contract.",
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
        "--data-root",
        type=Path,
        default=None,
        help="Trusted process-side application data-root override.",
    )
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
        help="Authorized SQX installation used for read-only native inspection.",
    )
    parser.add_argument(
        "--sqx-launcher-sha256",
        default=os.environ.get(SQX_LAUNCHER_SHA256_ENV),
        help="Server-side trusted SHA-256 for the installed sqcli.exe launcher.",
    )
    args = parser.parse_args(argv)
    if not args.web_root.is_dir():
        parser.error(f"web root does not exist: {args.web_root}")

    data_root = resolve_application_data_root(args.data_root)
    research_store = FileResearchCustodyStore(data_root)
    server = ThreadingHTTPServer(
        (args.host, args.port),
        make_handler(
            args.web_root,
            args.sqx_home,
            args.sqx_launcher_sha256,
            research_store,
        ),
    )
    print(f"TraderCockpit listening on http://{args.host}:{args.port}")
    print("Research custody ready: canonical local application store is bound")
    if args.sqx_home is None:
        print("Native SQX inspection unavailable: set SQX_HOME or --sqx-home")
    if args.sqx_launcher_sha256 is None:
        print(f"Native SQX launcher trust unavailable: set {SQX_LAUNCHER_SHA256_ENV}")
    print("Native SQX launch has no product-bound HTTP route yet")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
