from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_configurations import (
    CONFIGURATION_ASSEMBLY_MODE,
    CONFIGURATION_SOURCE_ENTRY,
    ResearchConfigurationContent,
    ResearchConfigurationError,
    compile_current_builder_configuration,
    list_current_configurations,
    read_current_configuration,
)
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchKind
from tradercockpit.sqx_builder_config import (
    SQX_BUILDER_PROJECT_RELATIVE_PATH,
    SqxBuilderNativeSelections,
    SqxBuilderProjectConfig,
)
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError


class ResearchConfigurationReviewFixTests(unittest.TestCase):
    def _archive(self, root: Path, name: str, task: bytes) -> tuple[Path, bytes]:
        path = root / name
        with ZipFile(path, "w") as archive:
            archive.writestr("config.xml", b"<Project/>")
            archive.writestr(CONFIGURATION_SOURCE_ENTRY, task)
        return path, path.read_bytes()

    def _config(self, archive_path: Path, archive_bytes: bytes) -> SqxBuilderProjectConfig:
        return SqxBuilderProjectConfig(
            archive_path=archive_path,
            archive_sha256=sha256(archive_bytes).hexdigest(),
            charts=(),
            instruments=(),
            native=SqxBuilderNativeSelections(),
        )

    def _content(
        self,
        store: FileResearchCustodyStore,
        archive_bytes: bytes,
        task_bytes: bytes,
        *,
        state: str = "compiled",
        approved_from_revision: str | None = None,
    ) -> tuple[ResearchConfigurationContent, tuple]:
        archive_ref = store.put_evidence(archive_bytes)
        task_ref = store.put_evidence(task_bytes)
        content = ResearchConfigurationContent(
            state=state,
            sqx_build=SQX_BUILD,
            source_project_path=SQX_BUILDER_PROJECT_RELATIVE_PATH,
            source_project_sha256=archive_ref.digest,
            source_project_ref=archive_ref,
            source_entry=CONFIGURATION_SOURCE_ENTRY,
            source_entry_ref=task_ref,
            executable_xml_ref=task_ref,
            assembly_mode=CONFIGURATION_ASSEMBLY_MODE,
            approved_changes=(),
            review_summary="Exact native snapshot.",
            approved_from_revision=approved_from_revision,
        )
        return content, (archive_ref, task_ref)

    def test_read_rejects_xml_evidence_not_equal_to_bound_archive_member(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            _, archive_bytes = self._archive(root, "source.cfx", b"<native-a/>")
            store = FileResearchCustodyStore(root / "data")
            content, evidence = self._content(store, archive_bytes, b"<forged-b/>")
            entity = store.create_entity(ResearchKind.CONFIGURATION)
            revision = store.create_revision(entity, content.canonical_bytes(), evidence=evidence)
            store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)

            with self.assertRaises(ResearchConfigurationError) as caught:
                read_current_configuration(store, entity)
            self.assertEqual(caught.exception.code, "configuration_content_corrupt")

    def test_read_rejects_approved_child_that_changes_compiled_parent_custody(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            _, archive_a = self._archive(root, "a.cfx", b"<native-a/>")
            _, archive_b = self._archive(root, "b.cfx", b"<native-b/>")
            store = FileResearchCustodyStore(root / "data")
            entity = store.create_entity(ResearchKind.CONFIGURATION)

            compiled, compiled_evidence = self._content(store, archive_a, b"<native-a/>")
            parent = store.create_revision(entity, compiled.canonical_bytes(), evidence=compiled_evidence)
            store.compare_and_set_current(entity, expected_revision=None, target_revision=parent.revision)

            approved, approved_evidence = self._content(
                store,
                archive_b,
                b"<native-b/>",
                state="approved",
                approved_from_revision=str(parent.revision),
            )
            child = store.create_revision(
                entity,
                approved.canonical_bytes(),
                parent_revision=parent.revision,
                evidence=approved_evidence,
            )
            store.compare_and_set_current(
                entity,
                expected_revision=parent.revision,
                target_revision=child.revision,
            )

            with self.assertRaises(ResearchConfigurationError) as caught:
                read_current_configuration(store, entity)
            self.assertEqual(caught.exception.code, "configuration_content_corrupt")

    def test_catalog_ignores_only_store_atomic_pointer_temporaries(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            _, archive_bytes = self._archive(root, "source.cfx", b"<native/>")
            store = FileResearchCustodyStore(root / "data")
            content, evidence = self._content(store, archive_bytes, b"<native/>")
            entity = store.create_entity(ResearchKind.CONFIGURATION)
            revision = store.create_revision(entity, content.canonical_bytes(), evidence=evidence)
            store.compare_and_set_current(entity, expected_revision=None, target_revision=revision.revision)

            directory = store.base / "current" / ResearchKind.CONFIGURATION.value
            temporary = directory / f".{entity.value}.json.tmp-12345-{'a' * 32}"
            temporary.write_bytes(b"partial")
            catalog = list_current_configurations(store)
            self.assertEqual(len(catalog["configurations"]), 1)
            self.assertEqual(catalog["configurations"][0]["entity_id"], str(entity))

            unexpected = directory / ".unexpected.tmp"
            unexpected.write_bytes(b"partial")
            with self.assertRaises(Exception):
                list_current_configurations(store)

    def test_unsupported_archive_read_is_normalized_to_configuration_refusal(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            archive_path, archive_bytes = self._archive(root, "source.cfx", b"<native/>")
            config = self._config(archive_path, archive_bytes)
            store = FileResearchCustodyStore(root / "data")
            with (
                patch(
                    "tradercockpit.research_configurations.read_sqx_builder_project",
                    return_value=config,
                ),
                patch(
                    "tradercockpit.research_configurations.verified_sqx_home",
                    return_value=root,
                ),
                patch("tradercockpit.research_configurations.ZipFile.read", side_effect=RuntimeError("encrypted")),
            ):
                with self.assertRaises(ResearchConfigurationError) as caught:
                    compile_current_builder_configuration(store, None)
            self.assertEqual(caught.exception.code, "configuration_source_invalid")

    def test_runtime_identity_is_reverified_after_archive_capture(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            archive_path, archive_bytes = self._archive(root, "source.cfx", b"<native/>")
            config = self._config(archive_path, archive_bytes)
            store = FileResearchCustodyStore(root / "data")
            with (
                patch(
                    "tradercockpit.research_configurations.read_sqx_builder_project",
                    return_value=config,
                ),
                patch(
                    "tradercockpit.research_configurations.verified_sqx_home",
                    side_effect=SqxPresetRuntimeError("sqx_build_mismatch", "runtime changed"),
                ) as reverify,
            ):
                with self.assertRaises(SqxPresetRuntimeError) as caught:
                    compile_current_builder_configuration(store, root / "sqx")
            self.assertEqual(caught.exception.code, "sqx_build_mismatch")
            reverify.assert_called_once_with(root / "sqx")


if __name__ == "__main__":
    unittest.main()
