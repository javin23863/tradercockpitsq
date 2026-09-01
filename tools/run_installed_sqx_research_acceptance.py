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


def start(client: Client) -> dict[str, Any]:
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

    after = _outputs(client.request("GET", OUTPUTS))
    return {
        "schema": SCHEMA,
        "stage": "builder_submitted",
        "base_url": client.base_url,
        "operator_action_required": "Choose one exact new/changed archive produced by this Builder run; no deterministic job-to-archive seam is inferred.",
        "runtime_status_schema": status.get("schema"),
        "identities": {
            "configuration_entity_id": config_id,
            "compiled_configuration_revision": compiled_rev,
            "configuration_revision": approved_rev,
            "source_project_sha256": source_sha,
            "executable_xml_sha256": xml_sha,
            "native_job_entity_id": job_id,
            "native_job_revision": job_rev,
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
    config, job = chain["configuration"], chain["native_job"]
    candidate, result = chain["candidate"], chain["historical_result"]

    checks = (
        (config.get("revision"), identities["configuration_revision"], "configuration revision changed"),
        (config.get("source_project_sha256"), identities["source_project_sha256"], "configuration source identity changed"),
        (config.get("executable_xml_sha256"), identities["executable_xml_sha256"], "configuration executable identity changed"),
        (job.get("revision"), identities["native_job_revision"], "Builder job revision changed"),
        (job.get("configuration_revision"), identities["configuration_revision"], "Builder job configuration binding changed"),
        (candidate.get("revision"), identities["candidate_revision"], "Candidate revision changed"),
        (candidate.get("native_job_revision"), identities["native_job_revision"], "Candidate job binding changed"),
        (candidate.get("configuration_revision"), identities["configuration_revision"], "Candidate configuration binding changed"),
        (candidate.get("archive_sha256"), identities["candidate_archive_sha256"], "Candidate archive identity changed"),
        (candidate.get("association_mode"), ASSOCIATION, "Candidate association mode changed"),
        (result.get("revision"), identities["historical_result_revision"], "Historical Result revision changed"),
        (result.get("candidate_revision"), identities["candidate_revision"], "Historical Result Candidate binding changed"),
        (result.get("result_archive_sha256"), identities["historical_result_archive_sha256"], "Historical Result archive changed"),
        (result.get("engine_sha256"), identities["retester_engine_sha256"], "Retester engine identity changed"),
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


def finish(client: Client, transcript: dict[str, Any], archive: str) -> dict[str, Any]:
    if transcript.get("schema") != SCHEMA or transcript.get("stage") != "builder_submitted":
        raise AcceptanceError("transcript_invalid", "finish requires a builder_submitted transcript")
    identities = transcript.get("identities")
    before = transcript.get("builder_outputs_before")
    if not isinstance(identities, dict) or not isinstance(before, dict):
        raise AcceptanceError("transcript_invalid", "transcript identities/output baseline are missing")
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
    engine_sha = _digest(result, "engine_sha256", "Historical Result")

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
        "operator_action_required": "Restart TraderCockpit with the same data root, then run verify.",
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


def verify(client: Client, transcript: dict[str, Any]) -> dict[str, Any]:
    if transcript.get("schema") != SCHEMA or transcript.get("stage") != "completed" or not isinstance(transcript.get("identities"), dict):
        raise AcceptanceError("transcript_invalid", "verify requires a completed transcript")
    _verify_chain(transcript["identities"], _chain(client, transcript["identities"]))
    return {"schema": SCHEMA, "stage": "reopen_verified", "base_url": client.base_url, "identities": transcript["identities"], "trades": transcript["trades"]}


def _read(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise AcceptanceError("transcript_invalid", f"could not read {path}") from exc
    if not isinstance(value, dict):
        raise AcceptanceError("transcript_invalid", "transcript must be an object")
    return value


def _write(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:4173")
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("start", "verify"):
        sub = commands.add_parser(name)
        sub.add_argument("--transcript", type=Path, required=True)
    finish_parser = commands.add_parser("finish")
    finish_parser.add_argument("--transcript", type=Path, required=True)
    finish_parser.add_argument("--archive", required=True)
    args = parser.parse_args(argv)
    client = Client(args.base_url)
    try:
        if args.command == "start":
            value = start(client)
            _write(args.transcript, value)
        elif args.command == "finish":
            value = finish(client, _read(args.transcript), args.archive)
            _write(args.transcript, value)
        else:
            value = verify(client, _read(args.transcript))
        print(json.dumps(value, indent=2, sort_keys=True))
        return 0
    except AcceptanceError as exc:
        print(json.dumps({"error": exc.code, "detail": exc.detail}, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
