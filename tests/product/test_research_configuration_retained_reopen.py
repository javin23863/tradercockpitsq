from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from tradercockpit.research_configurations import (
    CONFIGURATION_ASSEMBLY_MODE,
    CONFIGURATION_SOURCE_ENTRY,
    ResearchConfigurationContent,
    ResearchConfigurationError,
    approve_configuration,
    read_current_configuration,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchKind
from tradercockpit.sqx_builder_config import SQX_BUILDER_PROJECT_RELATIVE_PATH
from tradercockpit.sqx_presets import SQX_BUILD


class ResearchConfigurationRetainedReopenTests(unittest.TestCase):
    def test_structurally_valid_non_retained_archive_cannot_reopen_or_approve(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            task = b"<BuildTask><WhatToBuild/></BuildTask>"
            archive_path = root / "synthetic.cfx"
            with ZipFile(archive_path, "w") as archive:
                archive.writestr(
                    "config.xml",
                    b'<Project><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Project>',
                )
                archive.writestr(CONFIGURATION_SOURCE_ENTRY, task)
            archive_bytes = archive_path.read_bytes()

            store = FileResearchCustodyStore(root / "data")
            archive_ref = store.put_evidence(archive_bytes)
            task_ref = store.put_evidence(task)
            content = ResearchConfigurationContent(
                state="compiled",
                sqx_build=SQX_BUILD,
                source_project_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
                source_project_sha256=archive_ref.digest,
                source_project_ref=archive_ref,
                source_entry=CONFIGURATION_SOURCE_ENTRY,
                source_entry_ref=task_ref,
                executable_xml_ref=task_ref,
                assembly_mode=CONFIGURATION_ASSEMBLY_MODE,
                approved_changes=(),
                review_summary="Structurally valid synthetic native archive.",
            )
            entity = store.create_entity(ResearchKind.CONFIGURATION)
            revision = store.create_revision(
                entity,
                content.canonical_bytes(),
                evidence=(archive_ref, task_ref),
            )
            store.compare_and_set_current(
                entity,
                expected_revision=None,
                target_revision=revision.revision,
            )

            with self.assertRaises(ResearchConfigurationError) as reopened:
                read_current_configuration(store, entity)
            self.assertEqual(reopened.exception.code, "configuration_content_corrupt")
            self.assertIn("retained SQX 144.2953 Builder reference", reopened.exception.detail)

            with self.assertRaises(ResearchConfigurationError) as approval:
                approve_configuration(
                    store,
                    entity_id=entity,
                    expected_revision=revision.revision,
                )
            self.assertEqual(approval.exception.code, "configuration_content_corrupt")
            self.assertIn("retained SQX 144.2953 Builder reference", approval.exception.detail)


if __name__ == "__main__":
    unittest.main()
