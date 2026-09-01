#!/usr/bin/env python3
"""Exercise the installed-SQX Research chain through TraderCockpit's loopback API.

This is acceptance tooling, not a production/evidence authority. It never reads
SQX files directly and never infers a native job->archive identifier.

Phases:
  start  - compile, approve, launch Builder, record pre/post output identities
  finish - operator selects one exact new/changed Builder output; run Retester/Trades
  verify - after desktop restart, verify the exact stored chain reopens unchanged
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from ipaddress import ip_address
import json
from pathlib import Path
import re
import sys
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit, urlunsplit
from urllib.request import Request, urlopen


SCHEMA = "tc.installed-sqx-research-acceptance.v1"
ASSOCIATION = "operator_selected_exact_native_output"
DIGEST = re.compile(r"^[0-9a-f]{64}$")

STATUS = "/api/status"
BUILDER = "/api/sqx-builder-config"
OUTPUTS = "/api/sqx-outputs"
CONFIGS = "/api/research/configurations"
JOBS = "/api/research/native-jobs"
CANDIDATES = "/api/research/candidates"
RESULTS = "/api/research/historical-results"


class AcceptanceError(RuntimeError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code, self.detail = code, detail


def _text(record: dict[str, Any], key: str, where: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value:
        raise AcceptanceError("response_invalid", f"{where} missing {key}")
    return value


def _digest(record: dict[str, Any], key: str, where: str) -> str:
    value = _text(record, key, where)
    if not DIGEST.fullmatch(value):
        raise AcceptanceError("response_invalid", f"{where} has invalid {key}")
    return value


def _same(actual: Any, expected: Any, detail: str) -> None:
    if actual != expected:
        raise AcceptanceError("identity_mismatch", detail)


def _validate_identities(value: object, *, complete: bool) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise AcceptanceError("transcript_invalid", "transcript identities are missing")
    text_fields = [
        "configuration_entity_id",
        "compiled_configuration_revision",
        "configuration_revision",
        "native_job_entity_id",
        "native_job_revision",
    ]
    digest_fields = ["source_project_sha256", "executable_xml_sha256", "builder_launcher_sha256"]
    if complete:
        text_fields.extend([
            "candidate_entity_id",
            "candidate_revision",
            "candidate_archive_name",
            "historical_result_entity_id",
            "historical_result_revision",
        ])
        digest_fields.extend([
            "candidate_archive_sha256",
            "historical_result_archive_sha256",
            "retester_source_project_sha256",
            "retester_engine_sha256",
            "orders_entry_sha256",
        ])
    for key in text_fields:
        _text(value, key, "transcript identities")
    for key in digest_fields:
        _digest(value, key, "transcript identities")
    return value


def _attestations(transcript: dict[str, Any], *required: str) -> dict[str, Any]:
    value = transcript.get("operator_attestations")
    if not isinstance(value, dict):
        raise AcceptanceError("transcript_invalid", "operator attestations are missing")
    for key in required:
        if value.get(key) is not True:
            raise AcceptanceError("transcript_invalid", f"required operator attestation is missing: {key}")
    return value


def _base_url(value: str) -> str:
    parsed = urlsplit(value)
    if parsed.scheme != "http" or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path.rstrip("/"):
        raise AcceptanceError("base_url_invalid", "use plain http://<loopback-ip>:<port>")
    if not parsed.hostname:
        raise AcceptanceError("base_url_invalid", "loopback host is required")
    try:
        port = parsed.port
        loopback = ip_address(parsed.hostname).is_loopback
    except ValueError as exc:
        raise AcceptanceError("base_url_invalid", "host/port is invalid") from exc
    if not loopback or port is None:
        raise AcceptanceError("base_url_invalid", "literal loopback host and explicit port are required")
    netloc = f"[{parsed.hostname}]:{port}" if ":" in parsed.hostname else f"{parsed.hostname}:{port}"
    return urlunsplit(("http", netloc, "", "", ""))


@dataclass(frozen=True)
class Client:
    base_url: str
    timeout: float = 1200.0

    def __post_init__(self) -> None:
        object.__setattr__(self, "base_url", _base_url(self.base_url))

    def request(self, method: str, path: str, *, payload=None, query=None) -> dict[str, Any]:
        url = self.base_url + path + (("?" + urlencode(query)) if query else "")
        body = None
        headers = {"Accept": "application/json"}
        if payload is not None:
            body = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()
            headers["Content-Type"] = "application/json"
        request = Request(url, data=body, method=method, headers=headers)
        try:
            with urlopen(request, timeout=self.timeout) as response:
                raw = response.read()
        except HTTPError as exc:
            raw = exc.read()
            try:
                error = json.loads(raw.decode())
            except (UnicodeDecodeError, json.JSONDecodeError):
                error = {}
            raise AcceptanceError(
                str(error.get("reason_code") or error.get("error") or f"http_{exc.code}"),
                str(error.get("detail") or f"HTTP {exc.code} from {path}"),
            ) from exc
        except URLError as exc:
            raise AcceptanceError("api_unreachable", f"could not reach {url}: {exc.reason}") from exc
        try:
            value = json.loads(raw.decode())
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise AcceptanceError("response_invalid", f"{path} did not return JSON") from exc
        if not isinstance(value, dict):
            raise AcceptanceError("response_invalid", f"{path} did not return an object")
        return value


def _outputs(payload: dict[str, Any]) -> dict[str, str]:
    items = payload.get("outputs")
    if not isinstance(items, list):
        raise AcceptanceError("response_invalid", "SQX outputs list is missing")
    found: dict[str, str] = {}
    for item in items:
        if not isinstance(item, dict) or item.get("inspectable") is not True:
            continue
        name, digest = item.get("archive"), item.get("archive_sha256")
        if not isinstance(name, str) or not isinstance(digest, str) or not DIGEST.fullmatch(digest):
            raise AcceptanceError("response_invalid", "inspectable SQX output has invalid identity")
        if name in found:
            raise AcceptanceError("response_invalid", f"duplicate SQX output name {name}")
        found[name] = digest
    return dict(sorted(found.items()))


def _delta(before: dict[str, str], after: dict[str, str]) -> list[dict[str, str]]:
    return [
        {"archive": name, "archive_sha256": digest, "change": "new" if name not in before else "changed"}
        for name, digest in after.items()
        if before.get(name) != digest
    ]


def start(client: Client, *, confirmed_current_builder_saved_in_sqx: bool = False) -> dict[str, Any]:
    if confirmed_current_builder_saved_in_sqx is not True:
        raise AcceptanceError(
            "operator_confirmation_required",
            "confirm that the current Builder project was saved/changed in the installed SQX UI for this run rather than selected because it matches a retained repository reference",
        )
    status = client.request("GET", STATUS)
    builder = client.request("GET", BUILDER)
    installed_source_sha = _digest(builder, "archive_sha256", "installed Builder project")
    before = _outputs(client.request("GET", OUTPUTS))

    compiled = client.request("POST", CONFIGS, payload={"action": "compile"})
    _same(compiled.get("state"), "compiled", "configuration did not compile")
    _same(compiled.get("source_project_sha256"), installed_source_sha, "compile did not bind current installed Builder project")
    config_id = _text(compiled, "entity_id", "compiled configuration")
    compiled_rev = _text(compiled, "revision", "compiled configuration")
    source_sha = _digest(compiled, "source_project_sha256", "compiled configuration")
    xml_sha = _digest(compiled, "executable_xml_sha256", "compiled configuration")

    reopened_compiled = client.request("GET", CONFIGS, query={"entityId": config_id})
    _same(reopened_compiled.get("entity_id"), config_id, "compiled reopen changed configuration entity")
    _same(reopened_compiled.get("revision"), compiled_rev, "compiled reopen changed configuration revision")
    _same(reopened_compiled.get("state"), "compiled", "compiled reopen changed configuration state")
    _same(reopened_compiled.get("source_project_sha256"), source_sha, "compiled reopen changed source project identity")
    _same(reopened_compiled.get("executable_xml_sha256"), xml_sha, "compiled reopen changed executable identity")

    approved = client.request("POST", CONFIGS, payload={
        "action": "approve",
        "entity_id": config_id,
        "expected_revision": compiled_rev,
    })
    _same(approved.get("state"), "approved", "configuration did not approve")
    _same(approved.get("entity_id"), config_id, "approval changed configuration entity")
    approved_rev = _text(approved, "revision", "approved configuration")
    _same(approved.get("source_project_sha256"), source_sha, "approval changed source project identity")
    _same(approved.get("executable_xml_sha256"), xml_sha, "approval changed executable identity")

    job = client.request("POST", JOBS, payload={
        "action": "launch-builder",
        "configuration_entity_id": config_id,
        "expected_configuration_revision": approved_rev,
    })
    _same(job.get("state"), "submitted", "Builder job was not submitted")
    _same(job.get("configuration_revision"), approved_rev, "Builder job does not bind approved configuration")
    job_id = _text(job, "entity_id", "Builder job")
    job_rev = _text(job, "revision", "Builder job")
    launcher_sha = _digest(job, "launcher_sha256", "Builder job")

    after = _outputs(client.request("GET", OUTPUTS))
    return {
        "schema": SCHEMA,
        "stage": "builder_submitted",
        "base_url": client.base_url,
        "operator_action_required": "Choose one exact new/changed archive produced by this Builder run; no deterministic job-to-archive seam is inferred.",
        "runtime_status_schema": status.get("schema"),
        "operator_attestations": {
            "current_builder_saved_in_installed_sqx": True,
        },
        "compiled_reopen_verified": True,
        "identities": {
            "configuration_entity_id": config_id,
            "compiled_configuration_revision": compiled_rev,
            "configuration_revision": approved_rev,
            "source_project_sha256": source_sha,
            "executable_xml_sha256": xml_sha,
            "native_job_entity_id": job_id,
            "native_job_revision": job_rev,
            "builder_launcher_sha256": launcher_sha,
        },
        "builder_outputs_before": before,
        "builder_outputs_after": after,
        "candidate_archive_options": _delta(before, after),
    }


def _chain(client: Client, identities: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        "configuration": client.request("GET", CONFIGS, query={"entityId": _text(identities, "configuration_entity_id", "transcript")}),
        "native_job": client.request("GET", JOBS, query={"entityId": _text(identities, "native_job_entity_id", "transcript")}),
        "candidate": client.request("GET", CANDIDATES, query={"entityId": _text(identities, "candidate_entity_id", "transcript")}),
        "historical_result": client.request("GET", RESULTS, query={"entityId": _text(identities, "historical_result_entity_id", "transcript")}),
    }


def _verify_chain(identities: dict[str, Any], chain: dict[str, dict[str, Any]]) -> None:
    identities = _validate_identities(identities, complete=True)
    config, job = chain["configuration"], chain["native_job"]
    candidate, result = chain["candidate"], chain["historical_result"]

    checks = (
        (config.get("entity_id"), identities["configuration_entity_id"], "configuration entity changed"),
        (config.get("revision"), identities["configuration_revision"], "configuration revision changed"),
        (config.get("source_project_sha256"), identities["source_project_sha256"], "configuration source identity changed"),
        (config.get("executable_xml_sha256"), identities["executable_xml_sha256"], "configuration executable identity changed"),
        (job.get("entity_id"), identities["native_job_entity_id"], "Builder job entity changed"),
        (job.get("revision"), identities["native_job_revision"], "Builder job revision changed"),
        (job.get("configuration_revision"), identities["configuration_revision"], "Builder job configuration binding changed"),
        (job.get("launcher_sha256"), identities["builder_launcher_sha256"], "Builder launcher identity changed"),
        (candidate.get("entity_id"), identities["candidate_entity_id"], "Candidate entity changed"),
        (candidate.get("revision"), identities["candidate_revision"], "Candidate revision changed"),
        (candidate.get("native_job_revision"), identities["native_job_revision"], "Candidate job binding changed"),
        (candidate.get("configuration_revision"), identities["configuration_revision"], "Candidate configuration binding changed"),
        (candidate.get("archive_sha256"), identities["candidate_archive_sha256"], "Candidate archive identity changed"),
        (candidate.get("association_mode"), ASSOCIATION, "Candidate association mode changed"),
        (result.get("entity_id"), identities["historical_result_entity_id"], "Historical Result entity changed"),
        (result.get("revision"), identities["historical_result_revision"], "Historical Result revision changed"),
        (result.get("candidate_revision"), identities["candidate_revision"], "Historical Result Candidate binding changed"),
        (result.get("result_archive_sha256"), identities["historical_result_archive_sha256"], "Historical Result archive changed"),
        (result.get("source_project_sha256"), identities["retester_source_project_sha256"], "Retester source project identity changed"),
        (result.get("engine_sha256"), identities["retester_engine_sha256"], "Retester engine identity changed"),
        (result.get("launcher_sha256"), identities["builder_launcher_sha256"], "Retester launcher identity changed"),
        (result.get("retester_task"), 1, "Retester task is not task 1"),
        (result.get("state"), "completed", "Historical Result is not completed"),
        (result.get("execution_completed"), True, "Historical Result execution is incomplete"),
    )
    for actual, expected, detail in checks:
        _same(actual, expected, detail)

    trades = result.get("trades_readback")
    if not isinstance(trades, dict) or trades.get("state") != "available" or not isinstance(trades.get("payload"), dict):
        raise AcceptanceError("trades_unavailable", "Backtest Trades readback is unavailable")
    payload = trades["payload"]
    _same(payload.get("historical_result_revision"), identities["historical_result_revision"], "Trades result binding changed")
    _same(payload.get("orders_entry_sha256"), identities["orders_entry_sha256"], "orders.bin identity changed")


def finish(
    client: Client,
    transcript: dict[str, Any],
    archive: str,
    *,
    confirmed_archive_from_builder_run: bool = False,
    confirmed_orders_bin_only_observed_trades_seam: bool = False,
) -> dict[str, Any]:
    if confirmed_archive_from_builder_run is not True:
        raise AcceptanceError(
            "operator_confirmation_required",
            "confirm that the selected exact archive was observed as output from this Builder run",
        )
    if confirmed_orders_bin_only_observed_trades_seam is not True:
        raise AcceptanceError(
            "operator_confirmation_required",
            "confirm that no more authoritative direct native trade-row seam was observed and orders.bin remains the exact producer seam",
        )
    if transcript.get("schema") != SCHEMA or transcript.get("stage") != "builder_submitted":
        raise AcceptanceError("transcript_invalid", "finish requires a builder_submitted transcript")
    identities = _validate_identities(transcript.get("identities"), complete=False)
    attestations = _attestations(transcript, "current_builder_saved_in_installed_sqx")
    if transcript.get("compiled_reopen_verified") is not True:
        raise AcceptanceError("transcript_invalid", "compiled configuration reopen was not verified")
    before = transcript.get("builder_outputs_before")
    if not isinstance(before, dict):
        raise AcceptanceError("transcript_invalid", "transcript output baseline is missing")
    if any(not isinstance(name, str) or not isinstance(value, str) or not DIGEST.fullmatch(value) for name, value in before.items()):
        raise AcceptanceError("transcript_invalid", "output baseline contains an invalid identity")

    current = _outputs(client.request("GET", OUTPUTS))
    archive_sha = current.get(archive)
    if not archive_sha or before.get(archive) == archive_sha:
        raise AcceptanceError("archive_not_observed", "selected archive is not an exact new/changed output from this Builder launch")

    candidate = client.request("POST", CANDIDATES, payload={
        "action": "import-native-output",
        "native_job_entity_id": identities["native_job_entity_id"],
        "expected_native_job_revision": identities["native_job_revision"],
        "archive": archive,
        "expected_archive_sha256": archive_sha,
    })
    _same(candidate.get("association_mode"), ASSOCIATION, "Candidate association is not operator-selected exact output")
    _same(candidate.get("native_job_revision"), identities["native_job_revision"], "Candidate job binding changed")
    _same(candidate.get("configuration_revision"), identities["configuration_revision"], "Candidate configuration binding changed")
    _same(candidate.get("archive_sha256"), archive_sha, "Candidate archive identity changed")
    candidate_id = _text(candidate, "entity_id", "Candidate")
    candidate_rev = _text(candidate, "revision", "Candidate")

    result = client.request("POST", RESULTS, payload={
        "action": "start-retester",
        "candidate_entity_id": candidate_id,
        "expected_candidate_revision": candidate_rev,
    })
    _same(result.get("state"), "completed", "Retester did not produce a completed Historical Result")
    _same(result.get("execution_completed"), True, "Retester execution did not complete")
    _same(result.get("candidate_revision"), candidate_rev, "Historical Result Candidate binding changed")
    _same(result.get("retester_task"), 1, "Historical Result does not record Retester task 1")
    result_id = _text(result, "entity_id", "Historical Result")
    result_rev = _text(result, "revision", "Historical Result")
    result_sha = _digest(result, "result_archive_sha256", "Historical Result")
    retester_source_sha = _digest(result, "source_project_sha256", "Historical Result")
    engine_sha = _digest(result, "engine_sha256", "Historical Result")
    _same(result.get("launcher_sha256"), identities["builder_launcher_sha256"], "Retester used a different launcher identity")

    readback = client.request("GET", RESULTS, query={"entityId": result_id})
    trades = readback.get("trades_readback")
    if not isinstance(trades, dict) or trades.get("state") != "available" or not isinstance(trades.get("payload"), dict):
        raise AcceptanceError("trades_unavailable", "Backtest Trades readback is unavailable")
    trade = trades["payload"]
    orders_sha = _digest(trade, "orders_entry_sha256", "Backtest Trades")
    _same(trade.get("historical_result_revision"), result_rev, "Trades does not bind exact Historical Result")

    complete = {
        **transcript,
        "stage": "completed",
        "operator_action_required": "Restart TraderCockpit with the same data root, inspect the real Candidates/Overview/Trades/Configuration surfaces, then run verify.",
        "operator_attestations": {
            **attestations,
            "selected_archive_observed_from_this_builder_run": True,
            "orders_bin_only_observed_trades_seam": True,
        },
        "trades_readback_mode": "strict_orders_bin_adapter",
        "builder_outputs_after": current,
        "candidate_archive_options": _delta(before, current),
        "identities": {
            **identities,
            "candidate_entity_id": candidate_id,
            "candidate_revision": candidate_rev,
            "candidate_archive_name": archive,
            "candidate_archive_sha256": archive_sha,
            "historical_result_entity_id": result_id,
            "historical_result_revision": result_rev,
            "historical_result_archive_sha256": result_sha,
            "retester_source_project_sha256": retester_source_sha,
            "retester_engine_sha256": engine_sha,
            "orders_entry_sha256": orders_sha,
        },
        "trades": {
            "schema": trade.get("schema"),
            "orders_format": trade.get("orders_format"),
            "orders_format_version": trade.get("orders_format_version"),
            "native_order_count": trade.get("native_order_count"),
            "trade_count": trade.get("trade_count"),
            "selection": trade.get("selection"),
            "orders_entry_sha256": orders_sha,
        },
    }
    _verify_chain(complete["identities"], _chain(client, complete["identities"]))
    return complete


def verify(
    client: Client,
    transcript: dict[str, Any],
    *,
    confirmed_desktop_reopen_reviewed: bool = False,
) -> dict[str, Any]:
    if confirmed_desktop_reopen_reviewed is not True:
        raise AcceptanceError(
            "operator_confirmation_required",
            "confirm that the restarted desktop showed the exact Candidate, Backtest Overview, Trades, and Configuration chain",
        )
    if transcript.get("schema") != SCHEMA or transcript.get("stage") not in {"completed", "reopen_verified"}:
        raise AcceptanceError("transcript_invalid", "verify requires a completed or reopen_verified transcript")
    identities = _validate_identities(transcript.get("identities"), complete=True)
    attestations = _attestations(
        transcript,
        "current_builder_saved_in_installed_sqx",
        "selected_archive_observed_from_this_builder_run",
        "orders_bin_only_observed_trades_seam",
    )
    if transcript.get("compiled_reopen_verified") is not True:
        raise AcceptanceError("transcript_invalid", "compiled configuration reopen was not verified")
    if transcript.get("trades_readback_mode") != "strict_orders_bin_adapter":
        raise AcceptanceError("transcript_invalid", "Trades readback mode is missing or changed")
    trades = transcript.get("trades")
    if not isinstance(trades, dict):
        raise AcceptanceError("transcript_invalid", "completed transcript is missing Trades summary")
    _verify_chain(identities, _chain(client, identities))
    return {
        **transcript,
        "schema": SCHEMA,
        "stage": "reopen_verified",
        "base_url": client.base_url,
        "operator_action_required": None,
        "operator_attestations": {
            **attestations,
            "desktop_reopen_exact_chain_reviewed": True,
        },
        "identities": identities,
        "trades": trades,
    }


def _read(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AcceptanceError("transcript_invalid", f"could not read {path}") from exc
    if not isinstance(value, dict):
        raise AcceptanceError("transcript_invalid", "transcript must be an object")
    return value


def _write(path: Path, value: dict[str, Any]) -> None:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    except OSError as exc:
        raise AcceptanceError("transcript_write_failed", f"could not write {path}") from exc


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:4173")
    commands = parser.add_subparsers(dest="command", required=True)
    start_parser = commands.add_parser("start")
    start_parser.add_argument("--transcript", type=Path, required=True)
    start_parser.add_argument("--confirm-current-builder-saved-in-sqx", action="store_true")
    finish_parser = commands.add_parser("finish")
    finish_parser.add_argument("--transcript", type=Path, required=True)
    finish_parser.add_argument("--archive", required=True)
    finish_parser.add_argument("--confirm-archive-from-builder-run", action="store_true")
    finish_parser.add_argument("--confirm-orders-bin-only-observed-trades-seam", action="store_true")
    verify_parser = commands.add_parser("verify")
    verify_parser.add_argument("--transcript", type=Path, required=True)
    verify_parser.add_argument("--confirm-desktop-reopen-reviewed", action="store_true")
    args = parser.parse_args(argv)
    client = Client(args.base_url)
    try:
        if args.command == "start":
            value = start(
                client,
                confirmed_current_builder_saved_in_sqx=args.confirm_current_builder_saved_in_sqx,
            )
            _write(args.transcript, value)
        elif args.command == "finish":
            value = finish(
                client,
                _read(args.transcript),
                args.archive,
                confirmed_archive_from_builder_run=args.confirm_archive_from_builder_run,
                confirmed_orders_bin_only_observed_trades_seam=args.confirm_orders_bin_only_observed_trades_seam,
            )
            _write(args.transcript, value)
        else:
            value = verify(
                client,
                _read(args.transcript),
                confirmed_desktop_reopen_reviewed=args.confirm_desktop_reopen_reviewed,
            )
            _write(args.transcript, value)
        print(json.dumps(value, indent=2, sort_keys=True))
        return 0
    except AcceptanceError as exc:
        print(json.dumps({"error": exc.code, "detail": exc.detail}, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
