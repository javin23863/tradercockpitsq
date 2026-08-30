"""Serve TraderCockpit with exact product reads and source-bound SQX control."""

from __future__ import annotations

import argparse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from tradercockpit.domain import ContentAddress
from tradercockpit.engine import EngineContractError, load_initial_run_read_model
from tradercockpit.sqx_presets import (
    SqxPresetRuntimeError,
    get_sqx_preset,
    launch_sqx_preset,
    preset_catalog,
    preset_record,
)
from tradercockpit.storage import (
    ContentStoreError,
    FileObjectStore,
    FileRunLifecycleStore,
    LifecycleStoreError,
)


RUN_READ_API_PATH = "/api/run-read"
SQX_PRESETS_API_PATH = "/api/sqx-presets"
SQX_PRESET_LAUNCH_SUFFIX = "/launch"
API_PATH = RUN_READ_API_PATH
_DEFAULT_WEB_ROOT = Path(__file__).resolve().parents[2] / "web"


def _state_root(value: Path | str | None) -> Path:
    if value is None:
        raise FileNotFoundError("TraderCockpit state root is not configured")
    root = Path(value).expanduser().resolve()
    if not root.is_dir():
        raise FileNotFoundError("TraderCockpit state root does not exist")
    if not (root / "objects").is_dir() or not (root / "lifecycle" / "heads").is_dir():
        raise FileNotFoundError("TraderCockpit state root does not contain durable run state")
    return root


def _input_detail(model) -> dict[str, object]:
    inputs = model.inputs
    return {
        "candidate": {
            "origin": inputs.candidate.origin,
            "parent_strategy_ref": (
                str(inputs.candidate.parent_strategy_ref)
                if inputs.candidate.parent_strategy_ref
                else None
            ),
            "origin_ref": str(inputs.candidate.origin_ref) if inputs.candidate.origin_ref else None,
        },
        "strategy": {
            "semantic_schema": inputs.strategy.semantic_schema,
        },
        "data": {
            "symbol": inputs.data.symbol,
            "timeframe": inputs.data.timeframe,
            "source": inputs.data.source,
            "dataset_revision": inputs.data.dataset_revision,
            "timezone_name": inputs.data.timezone_name,
            "session_calendar": inputs.data.session_calendar,
            "start": inputs.data.start,
            "end": inputs.data.end,
            "adjustment_policy": inputs.data.adjustment_policy,
        },
        "execution": {
            "starting_cash": str(inputs.execution.starting_cash),
            "currency": inputs.execution.currency,
            "models": [
                {"kind": item.kind, "model": item.model}
                for item in inputs.execution.models
            ],
        },
        "engine_build": {
            "implementation": inputs.engine_build.implementation,
            "revision": inputs.engine_build.revision,
            "artifact_sha256": inputs.engine_build.artifact_sha256,
        },
    }


def _result_detail(model) -> dict[str, object] | None:
    if model.result is None:
        return None
    return {
        "result_schema": model.result.result_schema,
        "producer_build_ref": str(model.result.producer_build_ref),
    }


def _validation_detail(model) -> dict[str, object] | None:
    if model.decision is None:
        return None
    return {
        "passed": model.decision.passed,
        "source_result_schema": model.plan.source_result_schema,
        "outcomes": [
            {
                "metric_path": outcome.metric_path,
                "operator": outcome.operator,
                "threshold": str(outcome.threshold),
                "actual": str(outcome.actual),
                "passed": outcome.passed,
            }
            for outcome in model.decision.outcomes
        ],
    }


def read_run_snapshot(state_root: Path | str, run_ref_text: str, invocation_id: str) -> dict[str, object]:
    """Return fields verified by the existing initial-run read model."""

    if not isinstance(run_ref_text, str) or not run_ref_text:
        raise ValueError("runRef must be a non-empty TraderCockpit content address")
    if not isinstance(invocation_id, str) or not invocation_id:
        raise ValueError("invocationId must be a non-empty string")
    run_ref = ContentAddress.parse(run_ref_text)
    if run_ref.kind != "backtest-run":
        raise ValueError("runRef must reference 'backtest-run'")

    root = _state_root(state_root)
    model = load_initial_run_read_model(
        run_ref,
        invocation_id,
        FileObjectStore(root),
        FileRunLifecycleStore(root),
    )
    event = model.lifecycle_event
    return {
        "schema": "tc.initial-run-read.v1",
        "run_ref": str(model.run.ref),
        "invocation_id": event.invocation_id,
        "status": model.status,
        "terminal": model.terminal,
        "occurred_at": event.occurred_at,
        "reason_code": event.reason_code,
        "lifecycle_event_ref": str(event.ref),
        "inputs": {
            "candidate_ref": str(model.inputs.candidate.ref),
            "strategy_ref": str(model.inputs.strategy.ref),
            "data_ref": str(model.inputs.data.ref),
            "execution_ref": str(model.inputs.execution.ref),
            "engine_build_ref": str(model.inputs.engine_build.ref),
            "random_seed": model.run.random_seed,
        },
        "input_detail": _input_detail(model),
        "artifacts": {
            "receipt_ref": str(model.receipt.ref) if model.receipt else None,
            "result_ref": str(model.result.ref) if model.result else None,
            "plan_ref": str(model.plan.ref) if model.plan else None,
            "decision_ref": str(model.decision.ref) if model.decision else None,
            "evidence_manifest_ref": str(model.evidence_manifest.ref) if model.evidence_manifest else None,
        },
        "result": _result_detail(model),
        "validation": _validation_detail(model),
    }


def run_read_response(
    state_root: Path | str | None,
    run_ref_text: str,
    invocation_id: str,
) -> tuple[int, dict[str, object]]:
    try:
        return 200, read_run_snapshot(state_root, run_ref_text, invocation_id)
    except FileNotFoundError as exc:
        return 503, {"error": "producer_not_configured", "detail": str(exc)}
    except EngineContractError as exc:
        detail = str(exc)
        status = 404 if detail.startswith("missing run") or "no lifecycle state" in detail else 409
        return status, {"error": "not_found" if status == 404 else "invalid_state", "detail": detail}
    except ValueError as exc:
        return 400, {"error": "invalid_request", "detail": str(exc)}
    except (ContentStoreError, LifecycleStoreError) as exc:
        return 409, {"error": "invalid_state", "detail": str(exc)}


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


def _sqx_launch_error_response(exc: SqxPresetRuntimeError) -> tuple[int, dict[str, object]]:
    if exc.code == "unknown_preset":
        status = 404
        error = "not_found"
    elif exc.code in {
        "runtime_not_configured",
        "preset_missing",
        "sqx_launcher_missing",
        "sqx_build_markers_missing",
    }:
        status = 503
        error = "producer_not_configured"
    elif exc.code in {
        "hash_mismatch",
        "sqx_build_invalid",
        "sqx_build_mismatch",
        "invalid_runtime_path",
    }:
        status = 409
        error = "invalid_runtime"
    else:
        status = 502
        error = "producer_error"
    return status, {"error": error, "reason_code": exc.code, "detail": exc.detail}


def sqx_preset_launch_response(
    sqx_home: Path | str | None,
    preset_id: str,
    *,
    launcher=launch_sqx_preset,
) -> tuple[int, dict[str, object]]:
    if not isinstance(preset_id, str) or not preset_id or "/" in preset_id:
        return 404, {"error": "not_found", "detail": "unknown SQX preset launch path"}
    try:
        return 202, launcher(preset_id, sqx_home)
    except SqxPresetRuntimeError as exc:
        return _sqx_launch_error_response(exc)


def make_handler(
    web_root: Path,
    state_root: Path | str | None,
    sqx_home: Path | str | None = None,
    *,
    sqx_launcher=launch_sqx_preset,
):
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
            body = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
            self.send_response(status)
            self.send_header("content-type", "application/json; charset=utf-8")
            self.send_header("content-length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler API
            parsed = urlsplit(self.path)
            if parsed.path == RUN_READ_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                run_refs = query.get("runRef", [])
                invocation_ids = query.get("invocationId", [])
                if len(run_refs) != 1 or len(invocation_ids) != 1:
                    self._json(400, {"error": "invalid_request", "detail": "exactly one runRef and invocationId are required"})
                    return
                status, payload = run_read_response(state_root, run_refs[0], invocation_ids[0])
                self._json(status, payload)
                return
            if parsed.path == SQX_PRESETS_API_PATH:
                query = parse_qs(parsed.query, keep_blank_values=True)
                preset_ids = query.get("presetId", [])
                if len(preset_ids) > 1:
                    self._json(400, {"error": "invalid_request", "detail": "at most one presetId is allowed"})
                    return
                status, payload = sqx_preset_response(sqx_home, preset_ids[0] if preset_ids else None)
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
            prefix = SQX_PRESETS_API_PATH + "/"
            if parsed.path.startswith(prefix) and parsed.path.endswith(SQX_PRESET_LAUNCH_SUFFIX):
                preset_id = parsed.path[len(prefix) : -len(SQX_PRESET_LAUNCH_SUFFIX)]
                status, payload = sqx_preset_launch_response(
                    sqx_home,
                    preset_id,
                    launcher=sqx_launcher,
                )
                self._json(status, payload)
                return
            if parsed.path.startswith("/api/"):
                self._json(404, {"error": "not_found", "detail": "unknown API path"})
                return
            self._json(405, {"error": "method_not_allowed", "detail": "POST is only available for product API actions"})

    return Handler


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Serve TraderCockpit")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "4173")))
    parser.add_argument("--web-root", type=Path, default=_DEFAULT_WEB_ROOT)
    parser.add_argument(
        "--state-root",
        type=Path,
        default=Path(os.environ["TRADERCOCKPIT_STATE_ROOT"]) if os.environ.get("TRADERCOCKPIT_STATE_ROOT") else None,
    )
    parser.add_argument(
        "--sqx-home",
        type=Path,
        default=Path(os.environ["SQX_HOME"]) if os.environ.get("SQX_HOME") else None,
        help="Authorized StrategyQuant X installation used for source-bound preset control.",
    )
    args = parser.parse_args(argv)
    if not args.web_root.is_dir():
        parser.error(f"web root does not exist: {args.web_root}")

    server = ThreadingHTTPServer(
        (args.host, args.port),
        make_handler(args.web_root, args.state_root, args.sqx_home),
    )
    print(f"TraderCockpit listening on http://{args.host}:{args.port}")
    if args.state_root is None:
        print("Exact run lookup disabled: set TRADERCOCKPIT_STATE_ROOT or --state-root")
    if args.sqx_home is None:
        print("SQX preset actions disabled: set SQX_HOME or --sqx-home")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
