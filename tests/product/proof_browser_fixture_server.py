from __future__ import annotations

import argparse
from contextlib import ExitStack
from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from unittest.mock import patch

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_ideas import create_idea, list_current_ideas, read_current_idea
from tradercockpit.research_proof import create_research_proof


CONFIG_ENTITY = "tc-research:configuration:v1:11111111-1111-4111-8111-111111111111"
CONFIG_REVISION = f"tc-research-revision:configuration:sha256:{'1' * 64}"
JOB_ENTITY = "tc-research:native-job:v1:22222222-2222-4222-8222-222222222222"
JOB_REVISION = f"tc-research-revision:native-job:sha256:{'2' * 64}"
CANDIDATE_ENTITY = "tc-research:candidate:v1:33333333-3333-4333-8333-333333333333"
CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'3' * 64}"
HISTORICAL_ENTITY = "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444"
HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'4' * 64}"
INTERNAL_PROOF_ENTITY = "tc-research:proof:v1:55555555-5555-4555-8555-555555555555"
INTERNAL_PROOF_REVISION = f"tc-research-revision:proof:sha256:{'5' * 64}"
VALIDATION_REF = f"tc-evidence:sha256:{'6' * 64}"


def evidence(digit: str) -> str:
    return f"tc-evidence:sha256:{digit * 64}"


def source_records() -> dict[str, dict[str, object]]:
    native_project = "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    higher_project = "TraderCockpit-Retester-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    configuration = {
        "schema": "tc.research-configuration.v1",
        "entity_id": CONFIG_ENTITY,
        "revision": CONFIG_REVISION,
        "state": "approved",
        "sqx_build": "144.2953",
        "source_project_ref": evidence("a"),
        "source_project_sha256": "a" * 64,
        "executable_xml_ref": evidence("b"),
        "executable_xml_sha256": "b" * 64,
    }
    native_job = {
        "schema": "tc.research-native-job.v1",
        "entity_id": JOB_ENTITY,
        "revision": JOB_REVISION,
        "state": "submitted",
        "sqx_build": "144.2953",
        "configuration_entity_id": CONFIG_ENTITY,
        "configuration_revision": CONFIG_REVISION,
        "operation": "builder_loadconfig_start",
        "launcher_sha256": "c" * 64,
    }
    candidate = {
        "schema": "tc.research-candidate.v1",
        "entity_id": CANDIDATE_ENTITY,
        "revision": CANDIDATE_REVISION,
        "sqx_build": "144.2953",
        "native_job_entity_id": JOB_ENTITY,
        "native_job_revision": JOB_REVISION,
        "configuration_entity_id": CONFIG_ENTITY,
        "configuration_revision": CONFIG_REVISION,
        "archive_name": "candidate.sqx",
        "archive_ref": evidence("d"),
        "archive_sha256": "d" * 64,
    }
    historical = {
        "schema": "tc.research-historical-result.v1",
        "entity_id": HISTORICAL_ENTITY,
        "revision": HISTORICAL_REVISION,
        "state": "completed",
        "execution_completed": True,
        "candidate_entity_id": CANDIDATE_ENTITY,
        "candidate_revision": CANDIDATE_REVISION,
        "candidate_archive_name": "candidate.sqx",
        "candidate_archive_ref": candidate["archive_ref"],
        "candidate_archive_sha256": candidate["archive_sha256"],
        "sqx_build": "144.2953",
        "operation": "native_retester_task_1",
        "retester_task": 1,
        "native_project_name": native_project,
        "native_project_relative_path": f"user/projects/{native_project}/project.cfx",
        "source_project_ref": evidence("2"),
        "source_project_sha256": "2" * 64,
        "engine_ref": evidence("f"),
        "engine_sha256": "f" * 64,
        "launcher_sha256": "1" * 64,
        "result_archive_name": "result.sqx",
        "result_archive_relative_path": f"user/projects/{native_project}/databanks/Results/result.sqx",
        "result_archive_ref": evidence("e"),
        "result_archive_sha256": "e" * 64,
        "result_strategy_ref": evidence("7"),
        "result_strategy_sha256": "7" * 64,
        "result_settings_ref": evidence("9"),
        "result_settings_sha256": "9" * 64,
        "receipts": [{"state": "completed"}],
        "partial_side_effect": True,
        "validation_state": "not_run",
    }
    trades = {
        "schema": "tc.research-historical-trades.v1",
        "historical_result_entity_id": HISTORICAL_ENTITY,
        "historical_result_revision": HISTORICAL_REVISION,
        "candidate_entity_id": CANDIDATE_ENTITY,
        "candidate_revision": CANDIDATE_REVISION,
        "result_archive_ref": historical["result_archive_ref"],
        "result_archive_sha256": historical["result_archive_sha256"],
        "rows": [],
    }
    validation = {
        "schema": "tc.research-native-robustness.v1",
        "validation_ref": VALIDATION_REF,
        "proof_entity_id": INTERNAL_PROOF_ENTITY,
        "proof_revision": INTERNAL_PROOF_REVISION,
        "sqx_build": "144.2953",
        "operation": "native_retester_cross_check",
        "method": "RetestWithHigherPrecision",
        "execution_state": "completed",
        "producer_outcome_state": "producer_result_captured_outcome_unread",
        "source_historical_result_entity_id": HISTORICAL_ENTITY,
        "source_historical_result_revision": HISTORICAL_REVISION,
        "source_result_archive_ref": historical["result_archive_ref"],
        "source_result_archive_sha256": historical["result_archive_sha256"],
        "source_project_ref": evidence("2"),
        "source_project_sha256": "2" * 64,
        "compiled_project_ref": evidence("3"),
        "compiled_project_sha256": "3" * 64,
        "source_task_sha256": "4" * 64,
        "compiled_task_sha256": "5" * 64,
        "engine_ref": historical["engine_ref"],
        "engine_sha256": historical["engine_sha256"],
        "launcher_sha256": "1" * 64,
        "native_project_name": higher_project,
        "native_project_relative_path": f"user/projects/{higher_project}/project.cfx",
        "result_archive_name": "higher.sqx",
        "result_archive_ref": evidence("8"),
        "result_archive_sha256": "8" * 64,
        "result_strategy_ref": evidence("a"),
        "result_strategy_sha256": "a" * 64,
        "result_settings_ref": evidence("b"),
        "result_settings_sha256": "b" * 64,
        "native_settings": {"Precision": "1 Minute", "Spread": "Current"},
        "configuration_changed": False,
        "receipts": [{
            "action": "startOnlyTask",
            "task": 1,
            "state": "completed",
            "project": higher_project,
            "project_sha256": "3" * 64,
            "engine_sha256": historical["engine_sha256"],
            "launcher_sha256": "1" * 64,
            "result_archive_sha256": historical["result_archive_sha256"],
        }],
    }
    return {
        "configuration": configuration,
        "native_job": native_job,
        "candidate": candidate,
        "historical": historical,
        "trades": trades,
        "validation": validation,
    }


def source_patches(records: dict[str, dict[str, object]]) -> ExitStack:
    stack = ExitStack()
    stack.enter_context(patch("tradercockpit.research_proof.read_configuration_revision", return_value=records["configuration"]))
    stack.enter_context(patch("tradercockpit.research_proof.read_native_job_revision", return_value=records["native_job"]))
    stack.enter_context(patch("tradercockpit.research_proof.read_candidate_revision", return_value=records["candidate"]))
    stack.enter_context(patch("tradercockpit.research_proof.read_historical_result_revision", return_value=records["historical"]))
    stack.enter_context(patch("tradercockpit.research_proof.read_historical_trades", return_value=records["trades"]))
    stack.enter_context(patch("tradercockpit.research_proof.read_native_robustness_result", return_value=records["validation"]))
    return stack


def exact_idea(store: FileResearchCustodyStore) -> dict[str, object]:
    catalog = list_current_ideas(store)
    ideas = catalog.get("ideas")
    if not isinstance(ideas, list) or len(ideas) > 1:
        raise RuntimeError("Proof browser fixture requires zero or one persisted Idea")
    if not ideas:
        return create_idea(store, text="Proof restart acceptance idea", source="Proof browser fixture")
    entity_id = ideas[0].get("entity_id")
    if not isinstance(entity_id, str):
        raise RuntimeError("Persisted fixture Idea identity is invalid")
    return read_current_idea(store, entity_id)


def main() -> int:
    parser = argparse.ArgumentParser(description="Serve deterministic Research Proof browser fixture")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=4175)
    parser.add_argument("--data-root", type=Path, required=True)
    parser.add_argument("--web-root", type=Path, default=Path("web"))
    args = parser.parse_args()

    args.data_root.mkdir(parents=True, exist_ok=True)
    store = FileResearchCustodyStore(args.data_root)
    idea = exact_idea(store)
    records = source_records()
    with source_patches(records):
        proof = create_research_proof(
            store,
            idea_entity_id=str(idea["entity_id"]),
            idea_revision=str(idea["revision"]),
            historical_result_entity_id=HISTORICAL_ENTITY,
            historical_result_revision=HISTORICAL_REVISION,
            validation_ref=VALIDATION_REF,
        )
        print(
            "PROOF_FIXTURE=" + json.dumps(
                {
                    "entity_id": proof["entity_id"],
                    "revision": proof["revision"],
                    "idea_revision": proof["idea"]["revision"],
                    "historical_result_revision": proof["historical_result"]["revision"],
                    "validation_ref": proof["validation"]["validation_ref"],
                    "reused": proof["reused"],
                },
                sort_keys=True,
                separators=(",", ":"),
            ),
            flush=True,
        )
        server = ThreadingHTTPServer(
            (args.host, args.port),
            make_handler(args.web_root, None, None, store),
        )
        try:
            server.serve_forever()
        finally:
            server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
