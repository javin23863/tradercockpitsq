from __future__ import annotations

import argparse
import base64
from hashlib import sha256
from http.server import ThreadingHTTPServer
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile, ZipInfo

from tradercockpit.app_server import make_handler
from tradercockpit.research_candidates import CANDIDATE_ASSOCIATION_MODE, CandidateContent
from tradercockpit.research_configurations import (
    CONFIGURATION_ASSEMBLY_MODE,
    CONFIGURATION_SOURCE_ENTRY,
    ResearchConfigurationContent,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchKind
from tradercockpit.research_ideas import create_idea, list_current_ideas, read_current_idea
from tradercockpit.research_native_jobs import (
    NATIVE_JOB_OPERATION,
    NATIVE_JOB_STAGE_RELATIVE_DIR,
    NativeBuilderJobContent,
)
from tradercockpit.research_proof import (
    create_research_proof,
    list_current_research_proofs,
    read_current_research_proof,
)
from tradercockpit.research_retester import NativeRetesterContent, RETESTER_OPERATION, RETESTER_TASK
from tradercockpit.research_robustness import (
    ROBUSTNESS_ATTEMPT_SCHEMA,
    ROBUSTNESS_METHOD_HIGHER_PRECISION,
    ROBUSTNESS_OPERATION,
    ROBUSTNESS_OUTCOME_UNREAD,
    ROBUSTNESS_RECORD_SCHEMA,
    compile_higher_precision_project,
)
from tradercockpit.sqx_builder_config import SQX_BUILDER_PROJECT_RELATIVE_PATH
from tradercockpit.sqx_outputs import inspect_sqx_output_bytes
from tradercockpit.sqx_presets import SQX_BUILD


_NATIVE_PORTFOLIO_ORDERS_BIN = base64.b64decode(
    "rO0ABXflABRTUU9yZGVyRmlsZUZvcm1hdDoxMQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAACAAZBQVBMLkQAEE5ldyBTdHJhdGVneSAoMSkBAgIAAAAAAQAAAAABBAsAAABr4plUAAFGKPgAPbhR7AAAAGvimVQAPbhR7AAAAX/8K8AAQyxcKQAAAADMvrwgJP5J42h/RpGKj0aRio9GhpYAgAAAAIAAAACAAAAAgAAAAAFD1QvvQHwsPUnxETZGjqtyRq57gEnkoP9GkYqPAAAAAAAAAAABwIhZjwAAAAD/AAAAAA=="
)


def _canonical(payload: dict[str, object]) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _zip(entries: list[tuple[str, bytes]]) -> bytes:
    buffer = BytesIO()
    with ZipFile(buffer, "w") as archive:
        for name, payload in entries:
            info = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = ZIP_STORED
            archive.writestr(info, payload)
    return buffer.getvalue()


def _builder_project() -> tuple[bytes, bytes]:
    task = b'''<Settings><WhatToBuild><StrategyType type="fixture"/><MarketSides type="both"/><BuildMode generationType="runtime-defined"/></WhatToBuild><Data><Setups><Setup dateFrom="2020.01.01" dateTo="2020.12.31" testPrecision="Selected Timeframe" engine="SQ"><Chart symbol="EURUSD" timeframe="H1"/></Setup></Setups></Data><InstrumentInfo instrument="EURUSD"/><Rankings><MaxStrategies>1</MaxStrategies><StopCondition type="count"/></Rankings><Options><BuildTradingOptions/></Options><Blocks/><MoneyManagement/><CrossChecks use="false"/></Settings>'''
    config = b'''<Project name="Builder"><Chart symbol="EURUSD" timeframe="H1"/><InstrumentInfo instrument="EURUSD"/></Project>'''
    return _zip([("config.xml", config), ("Build-Task1.xml", task)]), task


def _retester_project() -> bytes:
    config = b'''<Project name="Retester"><Tasks><Task type="Retest" taskXMLFile="Retest-Task1.xml"/></Tasks></Project>'''
    task = b'''<Settings><CrossChecks use="true"><RetestWithHigherPrecision use="true"><Settings><Precision>1 Minute</Precision><Spread>Current</Spread></Settings></RetestWithHigherPrecision></CrossChecks></Settings>'''
    return _zip([("config.xml", config), ("Retest-Task1.xml", task)])


def _output_archive(name: str, *, orders: bool) -> tuple[bytes, dict[str, object], bytes, bytes]:
    strategy = f"<Strategy name='{name}'/>".encode()
    settings = f"<Settings name='{name}'/>".encode()
    entries = [
        ("version.txt", SQX_BUILD.encode()),
        ("strategy_Portfolio.xml", strategy),
        ("settings.xml", settings),
    ]
    if orders:
        entries.append(("orders.bin", _NATIVE_PORTFOLIO_ORDERS_BIN))
    snapshot = _zip(entries)
    return snapshot, inspect_sqx_output_bytes(snapshot, archive_name=f"{name}.sqx"), strategy, settings


def _exact_idea(store: FileResearchCustodyStore) -> dict[str, object]:
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


def _persist_configuration(store: FileResearchCustodyStore):
    project_bytes, task_bytes = _builder_project()
    project_ref = store.put_evidence(project_bytes)
    task_ref = store.put_evidence(task_bytes)
    entity = store.create_entity(ResearchKind.CONFIGURATION)
    common = dict(
        sqx_build=SQX_BUILD,
        source_project_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
        source_project_sha256=sha256(project_bytes).hexdigest(),
        source_project_ref=project_ref,
        source_entry=CONFIGURATION_SOURCE_ENTRY,
        source_entry_ref=task_ref,
        executable_xml_ref=task_ref,
        assembly_mode=CONFIGURATION_ASSEMBLY_MODE,
        approved_changes=(),
        review_summary="Deterministic canonical Proof restart fixture",
    )
    compiled = ResearchConfigurationContent(state="compiled", approved_from_revision=None, **common)
    compiled_revision = store.create_revision(entity, compiled.canonical_bytes(), evidence=(project_ref, task_ref))
    store.compare_and_set_current(entity, expected_revision=None, target_revision=compiled_revision.revision)
    approved = ResearchConfigurationContent(
        state="approved",
        approved_from_revision=str(compiled_revision.revision),
        **common,
    )
    approved_revision = store.create_revision(
        entity,
        approved.canonical_bytes(),
        parent_revision=compiled_revision.revision,
        evidence=(project_ref, task_ref),
    )
    store.compare_and_set_current(
        entity,
        expected_revision=compiled_revision.revision,
        target_revision=approved_revision.revision,
    )
    return entity, approved_revision.revision, task_ref


def _persist_job(store: FileResearchCustodyStore, configuration_entity, configuration_revision, task_ref):
    entity = store.create_entity(ResearchKind.NATIVE_JOB)
    staged = f"{NATIVE_JOB_STAGE_RELATIVE_DIR}/{task_ref.digest[:2]}/{task_ref.digest}.cfx"
    prepared = NativeBuilderJobContent(
        state="prepared",
        configuration_entity_id=str(configuration_entity),
        configuration_revision=str(configuration_revision),
        executable_xml_ref=task_ref,
        executable_xml_sha256=task_ref.digest,
        sqx_build=SQX_BUILD,
        operation=NATIVE_JOB_OPERATION,
        staged_config_relative_path=staged,
        launcher_sha256=None,
        partial_side_effect=False,
        receipts=(),
    )
    prepared_revision = store.create_revision(entity, prepared.canonical_bytes(), evidence=(task_ref,))
    store.compare_and_set_current(entity, expected_revision=None, target_revision=prepared_revision.revision)
    launcher = "c" * 64
    receipts = tuple(
        {
            "sequence": sequence,
            "action": action,
            "project": "Builder",
            "state": "completed",
            "exit_code": 0,
            "sqx_build": SQX_BUILD,
            "launcher_sha256": launcher,
            "config_sha256": task_ref.digest,
            "reason_code": None,
        }
        for sequence, action in ((1, "loadconfig"), (2, "start"))
    )
    submitted = NativeBuilderJobContent(
        state="submitted",
        configuration_entity_id=str(configuration_entity),
        configuration_revision=str(configuration_revision),
        executable_xml_ref=task_ref,
        executable_xml_sha256=task_ref.digest,
        sqx_build=SQX_BUILD,
        operation=NATIVE_JOB_OPERATION,
        staged_config_relative_path=staged,
        launcher_sha256=launcher,
        partial_side_effect=False,
        receipts=receipts,
    )
    submitted_revision = store.create_revision(
        entity,
        submitted.canonical_bytes(),
        parent_revision=prepared_revision.revision,
        evidence=(task_ref,),
    )
    store.compare_and_set_current(
        entity,
        expected_revision=prepared_revision.revision,
        target_revision=submitted_revision.revision,
    )
    return entity, submitted_revision.revision


def _persist_candidate(store: FileResearchCustodyStore, job_entity, job_revision, configuration_entity, configuration_revision):
    archive, info, strategy, settings = _output_archive("candidate", orders=False)
    archive_ref = store.put_evidence(archive)
    strategy_ref = store.put_evidence(strategy)
    settings_ref = store.put_evidence(settings)
    entity = store.create_entity(ResearchKind.CANDIDATE)
    content = CandidateContent(
        native_job_entity_id=str(job_entity),
        native_job_revision=str(job_revision),
        configuration_entity_id=str(configuration_entity),
        configuration_revision=str(configuration_revision),
        association_mode=CANDIDATE_ASSOCIATION_MODE,
        archive_name="candidate.sqx",
        archive_relative_path="user/projects/Builder/databanks/Results/candidate.sqx",
        archive_ref=archive_ref,
        archive_sha256=info["archive_sha256"],
        strategy_ref=strategy_ref,
        strategy_sha256=info["strategy_entry_sha256"],
        settings_ref=settings_ref,
        settings_sha256=info["settings_entry_sha256"],
        sqx_build=SQX_BUILD,
    )
    revision = store.create_revision(
        entity,
        content.canonical_bytes(),
        evidence=(archive_ref, strategy_ref, settings_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)
    return entity, revision.revision, archive_ref, info


def _persist_historical(store: FileResearchCustodyStore, candidate_entity, candidate_revision, candidate_ref, candidate_info):
    project_bytes = _retester_project()
    project_ref = store.put_evidence(project_bytes)
    engine_bytes = b"fixture-sq-trading-lib"
    engine_ref = store.put_evidence(engine_bytes)
    entity = store.create_entity(ResearchKind.HISTORICAL_RESULT)
    project_name = f"TraderCockpit-Retester-{entity.value.hex}"
    common = dict(
        candidate_entity_id=str(candidate_entity),
        candidate_revision=str(candidate_revision),
        candidate_archive_name="candidate.sqx",
        candidate_archive_ref=candidate_ref,
        candidate_archive_sha256=candidate_info["archive_sha256"],
        sqx_build=SQX_BUILD,
        operation=RETESTER_OPERATION,
        retester_task=RETESTER_TASK,
        native_project_name=project_name,
        native_project_relative_path=f"user/projects/{project_name}/project.cfx",
        source_project_ref=project_ref,
        source_project_sha256=sha256(project_bytes).hexdigest(),
        engine_ref=engine_ref,
        engine_sha256=sha256(engine_bytes).hexdigest(),
    )
    prepared = NativeRetesterContent(
        state="prepared",
        launcher_sha256=None,
        receipts=(),
        partial_side_effect=False,
        **common,
    )
    prepared_revision = store.create_revision(
        entity,
        prepared.canonical_bytes(),
        evidence=(candidate_ref, project_ref, engine_ref),
    )
    store.compare_and_set_current(entity, expected_revision=None, target_revision=prepared_revision.revision)

    result, result_info, strategy, settings = _output_archive("result", orders=True)
    result_ref = store.put_evidence(result)
    strategy_ref = store.put_evidence(strategy)
    settings_ref = store.put_evidence(settings)
    launcher = "1" * 64
    completed = NativeRetesterContent(
        state="completed",
        launcher_sha256=launcher,
        receipts=({"state": "completed"},),
        partial_side_effect=False,
        result_archive_name="result.sqx",
        result_archive_relative_path=f"user/projects/{project_name}/databanks/Results/result.sqx",
        result_archive_ref=result_ref,
        result_archive_sha256=result_info["archive_sha256"],
        result_strategy_ref=strategy_ref,
        result_strategy_sha256=result_info["strategy_entry_sha256"],
        result_settings_ref=settings_ref,
        result_settings_sha256=result_info["settings_entry_sha256"],
        **common,
    )
    completed_revision = store.create_revision(
        entity,
        completed.canonical_bytes(),
        parent_revision=prepared_revision.revision,
        evidence=(candidate_ref, project_ref, engine_ref, result_ref, strategy_ref, settings_ref),
    )
    store.compare_and_set_current(
        entity,
        expected_revision=prepared_revision.revision,
        target_revision=completed_revision.revision,
    )
    return entity, completed_revision.revision, result_ref, result_info, project_ref, project_bytes, engine_ref, engine_bytes


def _persist_robustness(
    store: FileResearchCustodyStore,
    historical_entity,
    historical_revision,
    source_result_ref,
    source_result_info,
    source_project_ref,
    source_project_bytes,
    engine_ref,
    engine_bytes,
):
    compiled_project_bytes, plan = compile_higher_precision_project(source_project_bytes)
    compiled_project_ref = store.put_evidence(compiled_project_bytes)
    proof_entity = store.create_entity(ResearchKind.PROOF)
    project_name = f"TraderCockpit-Retester-{proof_entity.value.hex}"
    project_relative = f"user/projects/{project_name}/project.cfx"
    prepared = {
        "schema": ROBUSTNESS_ATTEMPT_SCHEMA,
        "state": "prepared",
        "sqx_build": SQX_BUILD,
        "operation": ROBUSTNESS_OPERATION,
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "source_historical_result_entity_id": str(historical_entity),
        "source_historical_result_revision": str(historical_revision),
        "source_result_archive_ref": str(source_result_ref),
        "source_result_archive_sha256": source_result_info["archive_sha256"],
        "source_project_ref": str(source_project_ref),
        "source_project_sha256": sha256(source_project_bytes).hexdigest(),
        "compiled_project_ref": str(compiled_project_ref),
        "compiled_project_sha256": plan["compiled_project_sha256"],
        "configuration_changed": plan["configuration_changed"],
        "source_task_sha256": plan["source_task_sha256"],
        "compiled_task_sha256": plan["compiled_task_sha256"],
        "native_settings": plan["native_settings"],
        "engine_ref": str(engine_ref),
        "engine_sha256": sha256(engine_bytes).hexdigest(),
        "native_project_name": project_name,
        "native_project_relative_path": project_relative,
        "launcher_sha256": None,
        "receipts": [],
        "partial_side_effect": False,
        "failure_reason_code": None,
    }
    prepared_evidence = tuple({source_result_ref, source_project_ref, compiled_project_ref, engine_ref})
    prepared_revision = store.create_revision(
        proof_entity,
        _canonical(prepared),
        evidence=prepared_evidence,
    )
    store.compare_and_set_current(proof_entity, expected_revision=None, target_revision=prepared_revision.revision)

    result, result_info, strategy, settings = _output_archive("higher", orders=True)
    result_ref = store.put_evidence(result)
    strategy_ref = store.put_evidence(strategy)
    settings_ref = store.put_evidence(settings)
    launcher = "2" * 64
    receipt = {
        "action": "startOnlyTask",
        "task": 1,
        "state": "completed",
        "sqx_build": SQX_BUILD,
        "launcher_sha256": launcher,
        "project": project_name,
        "project_sha256": plan["compiled_project_sha256"],
        "engine_sha256": sha256(engine_bytes).hexdigest(),
        "result_archive_sha256": source_result_info["archive_sha256"],
    }
    record = {
        "schema": ROBUSTNESS_RECORD_SCHEMA,
        "sqx_build": SQX_BUILD,
        "operation": ROBUSTNESS_OPERATION,
        "method": ROBUSTNESS_METHOD_HIGHER_PRECISION,
        "source_historical_result_entity_id": str(historical_entity),
        "source_historical_result_revision": str(historical_revision),
        "source_result_archive_ref": str(source_result_ref),
        "source_result_archive_sha256": source_result_info["archive_sha256"],
        "source_project_ref": str(source_project_ref),
        "source_project_sha256": sha256(source_project_bytes).hexdigest(),
        "compiled_project_ref": str(compiled_project_ref),
        "compiled_project_sha256": plan["compiled_project_sha256"],
        "configuration_changed": plan["configuration_changed"],
        "source_task_sha256": plan["source_task_sha256"],
        "compiled_task_sha256": plan["compiled_task_sha256"],
        "native_settings": plan["native_settings"],
        "engine_ref": str(engine_ref),
        "engine_sha256": sha256(engine_bytes).hexdigest(),
        "launcher_sha256": launcher,
        "native_project_name": project_name,
        "native_project_relative_path": project_relative,
        "receipts": [receipt],
        "result_archive_name": "higher.sqx",
        "result_archive_ref": str(result_ref),
        "result_archive_sha256": result_info["archive_sha256"],
        "result_strategy_ref": str(strategy_ref),
        "result_strategy_sha256": result_info["strategy_entry_sha256"],
        "result_settings_ref": str(settings_ref),
        "result_settings_sha256": result_info["settings_entry_sha256"],
        "execution_state": "completed",
        "producer_outcome_state": ROBUSTNESS_OUTCOME_UNREAD,
    }
    completed_revision = store.create_revision(
        proof_entity,
        _canonical(record),
        parent_revision=prepared_revision.revision,
        evidence=prepared_evidence + (result_ref, strategy_ref, settings_ref),
    )
    store.compare_and_set_current(
        proof_entity,
        expected_revision=prepared_revision.revision,
        target_revision=completed_revision.revision,
    )
    return completed_revision.content


def _seed_chain(store: FileResearchCustodyStore, idea: dict[str, object]) -> dict[str, object]:
    config_entity, config_revision, task_ref = _persist_configuration(store)
    job_entity, job_revision = _persist_job(store, config_entity, config_revision, task_ref)
    candidate_entity, candidate_revision, candidate_ref, candidate_info = _persist_candidate(
        store, job_entity, job_revision, config_entity, config_revision
    )
    (
        historical_entity,
        historical_revision,
        result_ref,
        result_info,
        project_ref,
        project_bytes,
        engine_ref,
        engine_bytes,
    ) = _persist_historical(store, candidate_entity, candidate_revision, candidate_ref, candidate_info)
    validation_ref = _persist_robustness(
        store,
        historical_entity,
        historical_revision,
        result_ref,
        result_info,
        project_ref,
        project_bytes,
        engine_ref,
        engine_bytes,
    )
    return create_research_proof(
        store,
        idea_entity_id=str(idea["entity_id"]),
        idea_revision=str(idea["revision"]),
        historical_result_entity_id=str(historical_entity),
        historical_result_revision=str(historical_revision),
        validation_ref=str(validation_ref),
    )


def _fixture_proof(store: FileResearchCustodyStore) -> tuple[dict[str, object], bool]:
    catalog = list_current_research_proofs(store)
    proofs = catalog.get("proofs")
    if not isinstance(proofs, list) or len(proofs) > 1:
        raise RuntimeError("Proof browser fixture requires zero or one user-facing Proof")
    if proofs:
        entity_id = proofs[0].get("entity_id")
        if not isinstance(entity_id, str):
            raise RuntimeError("Persisted Proof identity is invalid")
        return read_current_research_proof(store, entity_id), True
    return _seed_chain(store, _exact_idea(store)), False


def main() -> int:
    parser = argparse.ArgumentParser(description="Serve deterministic Research Proof browser fixture")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=4175)
    parser.add_argument("--data-root", type=Path, required=True)
    parser.add_argument("--web-root", type=Path, default=Path("web"))
    args = parser.parse_args()

    args.data_root.mkdir(parents=True, exist_ok=True)
    store = FileResearchCustodyStore(args.data_root)
    proof, reused = _fixture_proof(store)
    print(
        "PROOF_FIXTURE=" + json.dumps(
            {
                "entity_id": proof["entity_id"],
                "revision": proof["revision"],
                "idea_revision": proof["idea"]["revision"],
                "historical_result_revision": proof["historical_result"]["revision"],
                "validation_ref": proof["validation"]["validation_ref"],
                "reused": reused,
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
