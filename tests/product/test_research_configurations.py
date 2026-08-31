from __future__ import annotations

from hashlib import sha256
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

import pytest

from tradercockpit.research_configurations import (
    CONFIGURATION_ASSEMBLY_MODE,
    CONFIGURATION_CONTENT_SCHEMA,
    ResearchConfigurationError,
    approve_configuration,
    compile_current_builder_configuration,
    list_current_configurations,
    read_current_configuration,
)
from tradercockpit.research_custody import (
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
)
from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderNativeSelections,
    SqxBuilderProjectConfig,
)
from tradercockpit.sqx_presets import SQX_BUILD


def _builder_config(tmp_path: Path, task_xml: bytes = b"<BuildTask><WhatToBuild/></BuildTask>") -> SqxBuilderProjectConfig:
    archive = tmp_path / "project.cfx"
    with ZipFile(archive, "w", compression=ZIP_DEFLATED) as handle:
        handle.writestr("config.xml", b"<Project/>")
        handle.writestr("Build-Task1.xml", task_xml)
    snapshot = archive.read_bytes()
    return SqxBuilderProjectConfig(
        archive_path=archive,
        archive_sha256=sha256(snapshot).hexdigest(),
        charts=(),
        instruments=(),
        native=SqxBuilderNativeSelections(),
    )


def test_compile_binds_exact_native_task_bytes_and_archive(monkeypatch, tmp_path: Path) -> None:
    task_xml = b"<BuildTask generation='native'><Data/></BuildTask>"
    config = _builder_config(tmp_path, task_xml)
    archive_bytes = config.archive_path.read_bytes()
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    store = FileResearchCustodyStore(tmp_path / "data")

    record = compile_current_builder_configuration(store, tmp_path / "sqx")

    assert record["state"] == "compiled"
    assert record["sqx_build"] == SQX_BUILD
    assert record["source_project_path"] == SQX_BUILDER_PROJECT_RELATIVE_PATH
    assert record["source_project_sha256"] == config.archive_sha256
    assert record["source_project_ref"] == str(EvidenceRef.from_bytes(archive_bytes))
    assert record["assembly_mode"] == CONFIGURATION_ASSEMBLY_MODE
    assert record["approved_changes"] == []
    assert record["review"]["changed"] is False
    assert record["approval"]["approved"] is False
    assert record["launch"] == {"enabled": False, "reason_code": "native_launch_not_in_this_slice"}
    assert record["executable_xml_sha256"] == sha256(task_xml).hexdigest()

    entity = ResearchEntityId.parse(record["entity_id"])
    revision = store.read_revision(store.current(entity))
    evidence_bytes = {store.read_evidence(ref) for ref in revision.evidence}
    assert evidence_bytes == {archive_bytes, task_xml}


def test_approval_creates_new_revision_without_changing_executable_identity(monkeypatch, tmp_path: Path) -> None:
    config = _builder_config(tmp_path)
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    store = FileResearchCustodyStore(tmp_path / "data")
    compiled = compile_current_builder_configuration(store, None)

    approved = approve_configuration(
        store,
        entity_id=compiled["entity_id"],
        expected_revision=compiled["revision"],
    )

    assert approved["state"] == "approved"
    assert approved["revision"] != compiled["revision"]
    assert approved["parent_revision"] == compiled["revision"]
    assert approved["executable_xml_ref"] == compiled["executable_xml_ref"]
    assert approved["source_project_ref"] == compiled["source_project_ref"]
    assert approved["source_project_sha256"] == compiled["source_project_sha256"]
    assert approved["approval"] == {
        "approved": True,
        "approved_from_revision": compiled["revision"],
    }
    assert read_current_configuration(store, compiled["entity_id"])["revision"] == approved["revision"]


def test_approval_rejects_stale_and_already_approved_revision(monkeypatch, tmp_path: Path) -> None:
    config = _builder_config(tmp_path)
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    store = FileResearchCustodyStore(tmp_path / "data")
    compiled = compile_current_builder_configuration(store, None)
    approved = approve_configuration(store, entity_id=compiled["entity_id"], expected_revision=compiled["revision"])

    with pytest.raises(ResearchCustodyError) as exc_info:
        approve_configuration(store, entity_id=compiled["entity_id"], expected_revision=compiled["revision"])
    assert exc_info.value.code == "current_conflict"

    with pytest.raises(ResearchConfigurationError) as exc_info:
        approve_configuration(store, entity_id=approved["entity_id"], expected_revision=approved["revision"])
    assert exc_info.value.code == "configuration_already_approved"


def test_catalog_and_reopen_preserve_approved_identity(monkeypatch, tmp_path: Path) -> None:
    config = _builder_config(tmp_path)
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    data_root = tmp_path / "data"
    store = FileResearchCustodyStore(data_root)
    compiled = compile_current_builder_configuration(store, None)
    approved = approve_configuration(store, entity_id=compiled["entity_id"], expected_revision=compiled["revision"])

    catalog = list_current_configurations(store)
    assert catalog["schema"] == "tc.research-configuration-catalog.v1"
    assert catalog["configurations"] == [{
        "entity_id": approved["entity_id"],
        "revision": approved["revision"],
        "state": "approved",
        "source_project_sha256": approved["source_project_sha256"],
        "executable_xml_sha256": approved["executable_xml_sha256"],
    }]

    reopened = FileResearchCustodyStore(data_root)
    assert read_current_configuration(reopened, approved["entity_id"]) == approved
    assert list_current_configurations(reopened) == catalog


def test_compile_refuses_archive_changed_after_inspection(monkeypatch, tmp_path: Path) -> None:
    config = _builder_config(tmp_path)
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    config.archive_path.write_bytes(b"moved")
    store = FileResearchCustodyStore(tmp_path / "data")

    with pytest.raises(ResearchConfigurationError) as exc_info:
        compile_current_builder_configuration(store, None)
    assert exc_info.value.code == "configuration_source_moved"


def test_read_rejects_wrong_entity_kind(tmp_path: Path) -> None:
    store = FileResearchCustodyStore(tmp_path / "data")
    idea = store.create_entity(ResearchKind.IDEA)
    with pytest.raises(ResearchConfigurationError) as exc_info:
        read_current_configuration(store, str(idea))
    assert exc_info.value.code == "configuration_entity_invalid"


def test_read_rejects_approved_provenance_that_does_not_match_parent(monkeypatch, tmp_path: Path) -> None:
    config = _builder_config(tmp_path)
    monkeypatch.setattr("tradercockpit.research_configurations.read_sqx_builder_project", lambda _home: config)
    store = FileResearchCustodyStore(tmp_path / "data")
    compiled = compile_current_builder_configuration(store, None)
    entity = ResearchEntityId.parse(compiled["entity_id"])
    parent = store.current(entity)
    parent_record = store.read_revision(parent)
    content = json.loads(store.read_revision_content(parent))
    content["state"] = "approved"
    other = store.create_entity(ResearchKind.CONFIGURATION)
    other_revision = store.create_revision(other, b"{}")
    content["approved_from_revision"] = str(other_revision.revision)
    corrupt = store.create_revision(
        entity,
        json.dumps(content, sort_keys=True, separators=(",", ":")).encode("utf-8"),
        parent_revision=parent,
        evidence=parent_record.evidence,
    )
    store.compare_and_set_current(entity, expected_revision=parent, target_revision=corrupt.revision)

    with pytest.raises(ResearchConfigurationError) as exc_info:
        read_current_configuration(store, entity)
    assert exc_info.value.code == "configuration_content_corrupt"
