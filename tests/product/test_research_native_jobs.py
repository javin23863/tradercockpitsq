from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore, ResearchKind, ResearchRevisionRef
from tradercockpit.research_native_jobs import (
    NATIVE_JOB_CATALOG_SCHEMA,
    NATIVE_JOB_OPERATION,
    NATIVE_JOB_READ_SCHEMA,
    NativeBuilderJobContent,
    ResearchNativeJobError,
    builder_loadconfig_cfx,
    launch_approved_builder_configuration,
    list_current_native_jobs,
)
from tradercockpit.sqx_gateway import SqxNativeGatewayError


def _project_archive(task_xml: bytes) -> bytes:
    buffer = BytesIO()
    with ZipFile(buffer, "w") as archive:
        archive.writestr(
            "config.xml",
            b'<Project name="Builder" version="144.2953">'
            b"<Tasks>"
            b'<Task type="Build" name="Build" showSettingsOverview="false" sampleName="Custom" '
            b'active="true" taskXMLFile="Build-Task1.xml" />'
            b"</Tasks></Project>",
        )
        archive.writestr("Build-Task1.xml", task_xml)
    return buffer.getvalue()


class ResearchNativeJobTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _configuration(self, store: FileResearchCustodyStore, xml: bytes) -> tuple[dict[str, object], str, bytes]:
        entity = store.create_entity(ResearchKind.CONFIGURATION)
        revision = ResearchRevisionRef(ResearchKind.CONFIGURATION, sha256(b"approved-config-revision").hexdigest())
        source_bytes = _project_archive(xml)
        source = store.put_evidence(source_bytes)
        evidence = store.put_evidence(xml)
        record = {
            "schema": "tc.research-configuration.v1",
            "entity_id": str(entity),
            "revision": str(revision),
            "state": "approved",
            "sqx_build": "144.2953",
            "source_project_ref": str(source),
            "source_project_sha256": source.digest,
            "executable_xml_ref": str(evidence),
            "executable_xml_sha256": evidence.digest,
            "approval": {"approved": True},
        }
        return record, str(revision), source_bytes

    def test_loadconfig_cfx_wraps_task_settings_in_saveconfig_envelope(self) -> None:
        task_xml = b"<Settings><Options/></Settings>"
        cfx = builder_loadconfig_cfx(_project_archive(task_xml), task_xml)
        with ZipFile(BytesIO(cfx)) as archive:
            self.assertEqual(archive.namelist(), ["config.xml"])
            envelope = archive.read("config.xml")
        self.assertTrue(envelope.startswith(b'<Task type="Build" name="Build"'))
        self.assertIn(b'taskXMLFile="Build-Task1.xml"', envelope)
        self.assertTrue(envelope.endswith(b"</Task>"))
        self.assertIn(task_xml, envelope)
        self.assertEqual(builder_loadconfig_cfx(_project_archive(task_xml), task_xml), cfx)

    def test_loadconfig_cfx_wraps_approved_executable_bytes_not_archive_task(self) -> None:
        cfx = builder_loadconfig_cfx(_project_archive(b"<Settings>a</Settings>"), b"<Settings>b</Settings>")
        with ZipFile(BytesIO(cfx)) as archive:
            self.assertIn(b"<Settings>b</Settings></Task>", archive.read("config.xml"))
        with self.assertRaises(ResearchNativeJobError) as caught:
            builder_loadconfig_cfx(_project_archive(b"<Settings>a</Settings>"), b"   ")
        self.assertEqual(caught.exception.code, "native_job_loadconfig_task_mismatch")

    def test_launch_stages_exact_bytes_creates_job_and_retry_is_idempotent(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, source_bytes = self._configuration(store, xml)
            expected_cfx = builder_loadconfig_cfx(source_bytes, xml)
            expected_cfx_sha = sha256(expected_cfx).hexdigest()
            calls: list[Path] = []

            class FakeGateway:
                def launch_builder(self, path, *, expected_config_sha256):
                    config = Path(path).resolve()
                    calls.append(config)
                    relative = config.relative_to(home.resolve()).as_posix()
                    self_outer.assertEqual(config.read_bytes(), expected_cfx)
                    self_outer.assertEqual(expected_config_sha256, expected_cfx_sha)
                    receipts = [
                        {
                            "sequence": 1,
                            "action": "loadconfig",
                            "project": "Builder",
                            "state": "completed",
                            "exit_code": 0,
                            "sqx_build": "144.2953",
                            "launcher_sha256": launcher_sha,
                            "config_sha256": expected_config_sha256,
                            "reason_code": None,
                        },
                        {
                            "sequence": 2,
                            "action": "start",
                            "project": "Builder",
                            "state": "completed",
                            "exit_code": 0,
                            "sqx_build": "144.2953",
                            "launcher_sha256": launcher_sha,
                            "config_sha256": expected_config_sha256,
                            "reason_code": None,
                        },
                    ]
                    return {
                        "schema": "tc.sqx-native-control.v1",
                        "operation": "builder_loadconfig_start",
                        "project": "Builder",
                        "state": "submitted",
                        "sqx_build": "144.2953",
                        "launcher_sha256": launcher_sha,
                        "config_relative_path": relative,
                        "config_sha256": expected_config_sha256,
                        "control_requests_submitted": 2,
                        "control_requests_completed": 2,
                        "partial_side_effect": False,
                        "receipts": receipts,
                    }

            self_outer = self

            def gateway_factory(*args, **kwargs):
                self.assertEqual(args, (home, launcher_sha))
                return FakeGateway()

            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration):
                launched = launch_approved_builder_configuration(
                    store,
                    home,
                    launcher_sha,
                    configuration_entity_id=configuration["entity_id"],
                    expected_configuration_revision=revision,
                    gateway_factory=gateway_factory,
                )
                reused = launch_approved_builder_configuration(
                    store,
                    home,
                    launcher_sha,
                    configuration_entity_id=configuration["entity_id"],
                    expected_configuration_revision=revision,
                    gateway_factory=gateway_factory,
                )

            self.assertEqual(launched["schema"], NATIVE_JOB_READ_SCHEMA)
            self.assertEqual(launched["state"], "submitted")
            self.assertFalse(launched["reused"])
            self.assertEqual(launched["configuration_revision"], revision)
            self.assertEqual(launched["executable_xml_sha256"], sha256(xml).hexdigest())
            self.assertEqual(len(launched["receipts"]), 2)
            self.assertEqual(len(calls), 1)
            self.assertTrue(reused["reused"])
            self.assertEqual(reused["entity_id"], launched["entity_id"])
            self.assertEqual(reused["revision"], launched["revision"])
            self.assertEqual(calls[0].read_bytes(), expected_cfx)
            self.assertEqual(calls[0].name, f"{sha256(xml).hexdigest()}.cfx")
            with ZipFile(calls[0]) as archive:
                self.assertEqual(archive.namelist(), ["config.xml"])
                self.assertIn(xml, archive.read("config.xml"))

            catalog = list_current_native_jobs(store, revision)
            self.assertEqual(catalog["schema"], NATIVE_JOB_CATALOG_SCHEMA)
            self.assertEqual(len(catalog["jobs"]), 1)
            self.assertEqual(catalog["jobs"][0]["entity_id"], launched["entity_id"])

    def test_gateway_refusal_is_preserved_as_failed_job(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)

            class FailingGateway:
                def launch_builder(self, path, *, expected_config_sha256):
                    receipt = {
                        "sequence": 1,
                        "action": "loadconfig",
                        "project": "Builder",
                        "state": "preflight_failed",
                        "exit_code": None,
                        "sqx_build": "144.2953",
                        "launcher_sha256": None,
                        "config_sha256": None,
                        "reason_code": "sqx_launcher_hash_mismatch",
                    }
                    raise SqxNativeGatewayError(
                        "sqx_launcher_hash_mismatch",
                        "launcher changed",
                        receipts=(receipt,),
                    )

            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration):
                with self.assertRaises(ResearchNativeJobError) as caught:
                    launch_approved_builder_configuration(
                        store,
                        home,
                        launcher_sha,
                        configuration_entity_id=configuration["entity_id"],
                        expected_configuration_revision=revision,
                        gateway_factory=lambda *args, **kwargs: FailingGateway(),
                    )

            self.assertEqual(caught.exception.code, "sqx_launcher_hash_mismatch")
            catalog = list_current_native_jobs(store, revision)
            self.assertEqual(len(catalog["jobs"]), 1)
            failed = catalog["jobs"][0]
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "sqx_launcher_hash_mismatch")
            self.assertFalse(failed["partial_side_effect"])
            self.assertEqual(failed["receipts"][0]["state"], "preflight_failed")

    def test_unapproved_configuration_refuses_before_native_staging(self) -> None:
        xml = b"<Settings/>"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)
            configuration = {**configuration, "state": "compiled", "approval": {"approved": False}}

            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration):
                with self.assertRaises(ResearchNativeJobError) as caught:
                    launch_approved_builder_configuration(
                        store,
                        home,
                        "0" * 64,
                        configuration_entity_id=configuration["entity_id"],
                        expected_configuration_revision=revision,
                    )

            self.assertEqual(caught.exception.code, "native_job_configuration_unapproved")
            self.assertFalse((home / "user/TraderCockpit").exists())

    def test_existing_stage_file_must_match_exact_approved_bytes(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)
            digest = sha256(xml).hexdigest()
            target = home / "user/TraderCockpit/approved-configurations" / digest[:2] / f"{digest}.cfx"
            target.parent.mkdir(parents=True)
            target.write_bytes(b"different")

            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration):
                with self.assertRaises(ResearchNativeJobError) as caught:
                    launch_approved_builder_configuration(
                        store,
                        home,
                        "0" * 64,
                        configuration_entity_id=configuration["entity_id"],
                        expected_configuration_revision=revision,
                    )

            self.assertEqual(caught.exception.code, "native_job_stage_conflict")
            self.assertEqual(target.read_bytes(), b"different")

    def test_list_accepts_pre_cfx_xml_staged_path(self) -> None:
        xml = b"<Settings><legacy>xml</legacy></Settings>"
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp) / "data")
            configuration, revision, _source = self._configuration(store, xml)
            ref = EvidenceRef.parse(str(configuration["executable_xml_ref"]))
            digest = ref.digest
            entity = store.create_entity(ResearchKind.NATIVE_JOB)
            content = NativeBuilderJobContent(
                state="prepared",
                configuration_entity_id=str(configuration["entity_id"]),
                configuration_revision=revision,
                executable_xml_ref=ref,
                executable_xml_sha256=digest,
                sqx_build="144.2953",
                operation=NATIVE_JOB_OPERATION,
                staged_config_relative_path=f"user/TraderCockpit/approved-configurations/{digest[:2]}/{digest}.xml",
                launcher_sha256=None,
                partial_side_effect=False,
                receipts=(),
            )
            stored = store.create_revision(entity, content.canonical_bytes(), evidence=(ref,))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
            catalog = list_current_native_jobs(store)
            self.assertEqual(len(catalog["jobs"]), 1)
            self.assertTrue(catalog["jobs"][0]["staged_config_relative_path"].endswith(f"/{digest}.xml"))


if __name__ == "__main__":
    unittest.main()
