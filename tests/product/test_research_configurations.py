from __future__ import annotations

from hashlib import sha256
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZIP_DEFLATED, ZipFile

from tradercockpit.research_configurations import (
    CONFIGURATION_ASSEMBLY_MODE,
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


class ResearchConfigurationTests(unittest.TestCase):
    def _builder_config(
        self,
        root: Path,
        task_xml: bytes = b"<BuildTask><WhatToBuild/></BuildTask>",
    ) -> SqxBuilderProjectConfig:
        sqx = root / "sqx"
        (sqx / "internal/web/SQUANT").mkdir(parents=True, exist_ok=True)
        (sqx / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (sqx / "internal/SQUANT.dat").parent.mkdir(parents=True, exist_ok=True)
        (sqx / "internal/SQUANT.dat").write_bytes(b"144fixture")
        archive = sqx / "user/projects/Builder/project.cfx"
        archive.parent.mkdir(parents=True, exist_ok=True)
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

    def test_compile_binds_exact_native_task_bytes_and_archive(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            task_xml = b"<BuildTask generation='native'><Data/></BuildTask>"
            config = self._builder_config(root, task_xml)
            archive_bytes = config.archive_path.read_bytes()
            store = FileResearchCustodyStore(root / "data")
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                record = compile_current_builder_configuration(store, root / "sqx")

            self.assertEqual(record["state"], "compiled")
            self.assertEqual(record["sqx_build"], SQX_BUILD)
            self.assertEqual(record["source_project_path"], SQX_BUILDER_PROJECT_RELATIVE_PATH)
            self.assertEqual(record["source_project_sha256"], config.archive_sha256)
            self.assertEqual(record["source_project_ref"], str(EvidenceRef.from_bytes(archive_bytes)))
            self.assertEqual(record["assembly_mode"], CONFIGURATION_ASSEMBLY_MODE)
            self.assertEqual(record["approved_changes"], [])
            self.assertFalse(record["review"]["changed"])
            self.assertFalse(record["approval"]["approved"])
            self.assertEqual(
                record["launch"],
                {"enabled": False, "reason_code": "native_launch_not_in_this_slice"},
            )
            self.assertEqual(record["executable_xml_sha256"], sha256(task_xml).hexdigest())

            entity = ResearchEntityId.parse(record["entity_id"])
            revision = store.read_revision(store.current(entity))
            evidence_bytes = {store.read_evidence(ref) for ref in revision.evidence}
            self.assertEqual(evidence_bytes, {archive_bytes, task_xml})

    def test_approval_creates_new_revision_without_changing_executable_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            config = self._builder_config(root)
            store = FileResearchCustodyStore(root / "data")
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                compiled = compile_current_builder_configuration(store, root / "sqx")

            approved = approve_configuration(
                store,
                entity_id=compiled["entity_id"],
                expected_revision=compiled["revision"],
            )

            self.assertEqual(approved["state"], "approved")
            self.assertNotEqual(approved["revision"], compiled["revision"])
            self.assertEqual(approved["parent_revision"], compiled["revision"])
            self.assertEqual(approved["executable_xml_ref"], compiled["executable_xml_ref"])
            self.assertEqual(approved["source_project_ref"], compiled["source_project_ref"])
            self.assertEqual(approved["source_project_sha256"], compiled["source_project_sha256"])
            self.assertEqual(
                approved["approval"],
                {"approved": True, "approved_from_revision": compiled["revision"]},
            )
            self.assertEqual(
                read_current_configuration(store, compiled["entity_id"])["revision"],
                approved["revision"],
            )

    def test_approval_rejects_stale_and_already_approved_revision(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            config = self._builder_config(root)
            store = FileResearchCustodyStore(root / "data")
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                compiled = compile_current_builder_configuration(store, root / "sqx")
            approved = approve_configuration(
                store,
                entity_id=compiled["entity_id"],
                expected_revision=compiled["revision"],
            )

            with self.assertRaises(ResearchCustodyError) as stale:
                approve_configuration(
                    store,
                    entity_id=compiled["entity_id"],
                    expected_revision=compiled["revision"],
                )
            self.assertEqual(stale.exception.code, "current_conflict")

            with self.assertRaises(ResearchConfigurationError) as repeated:
                approve_configuration(
                    store,
                    entity_id=approved["entity_id"],
                    expected_revision=approved["revision"],
                )
            self.assertEqual(repeated.exception.code, "configuration_already_approved")

    def test_catalog_and_reopen_preserve_approved_identity(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            config = self._builder_config(root)
            data_root = root / "data"
            store = FileResearchCustodyStore(data_root)
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                compiled = compile_current_builder_configuration(store, root / "sqx")
            approved = approve_configuration(
                store,
                entity_id=compiled["entity_id"],
                expected_revision=compiled["revision"],
            )

            catalog = list_current_configurations(store)
            self.assertEqual(catalog["schema"], "tc.research-configuration-catalog.v1")
            self.assertEqual(
                catalog["configurations"],
                [{
                    "entity_id": approved["entity_id"],
                    "revision": approved["revision"],
                    "state": "approved",
                    "source_project_sha256": approved["source_project_sha256"],
                    "executable_xml_sha256": approved["executable_xml_sha256"],
                }],
            )

            reopened = FileResearchCustodyStore(data_root)
            self.assertEqual(read_current_configuration(reopened, approved["entity_id"]), approved)
            self.assertEqual(list_current_configurations(reopened), catalog)

    def test_compile_refuses_archive_changed_after_inspection(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            config = self._builder_config(root)
            config.archive_path.write_bytes(b"moved")
            store = FileResearchCustodyStore(root / "data")
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                with self.assertRaises(ResearchConfigurationError) as raised:
                    compile_current_builder_configuration(store, root / "sqx")
            self.assertEqual(raised.exception.code, "configuration_source_moved")

    def test_read_rejects_wrong_entity_kind(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp) / "data")
            idea = store.create_entity(ResearchKind.IDEA)
            with self.assertRaises(ResearchConfigurationError) as raised:
                read_current_configuration(store, str(idea))
            self.assertEqual(raised.exception.code, "configuration_entity_invalid")

    def test_read_rejects_approved_provenance_that_does_not_match_parent(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            config = self._builder_config(root)
            store = FileResearchCustodyStore(root / "data")
            with patch(
                "tradercockpit.research_configurations.read_sqx_builder_project",
                return_value=config,
            ):
                compiled = compile_current_builder_configuration(store, root / "sqx")
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
            store.compare_and_set_current(
                entity,
                expected_revision=parent,
                target_revision=corrupt.revision,
            )

            with self.assertRaises(ResearchConfigurationError) as raised:
                read_current_configuration(store, entity)
            self.assertEqual(raised.exception.code, "configuration_content_corrupt")


if __name__ == "__main__":
    unittest.main()