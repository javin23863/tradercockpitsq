from __future__ import annotations

from hashlib import sha256
from concurrent.futures import ThreadPoolExecutor
from dataclasses import replace
from threading import Event
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore, ResearchKind, ResearchRevisionRef
from tradercockpit.research_native_jobs import (
    NATIVE_JOB_CATALOG_SCHEMA,
    NativeBuilderJobContent,
    NATIVE_JOB_OPERATION,
    read_current_native_job,
    NATIVE_JOB_READ_SCHEMA,
    ResearchNativeJobError,
    launch_approved_builder_configuration,
    list_current_native_jobs,
)
from tradercockpit.sqx_gateway import SqxNativeGatewayError, pack_task_rooted_cfx, task_document_from_cfx


class ResearchNativeJobTests(unittest.TestCase):
    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _configuration(self, store: FileResearchCustodyStore, xml: bytes) -> tuple[dict[str, object], str]:
        entity = store.create_entity(ResearchKind.CONFIGURATION)
        revision = ResearchRevisionRef(ResearchKind.CONFIGURATION, sha256(b"approved-config-revision").hexdigest())
        evidence = store.put_evidence(xml)
        project = BytesIO()
        with ZipFile(project, "w") as archive:
            archive.writestr("config.xml", b'<Project name="Builder"><Tasks><Task type="Build" name="Build" version="126.2189" taskXMLFile="Build-Task1.xml" templateFile="native &amp; template.cfx"/></Tasks></Project>')
            archive.writestr("Build-Task1.xml", xml)
        project_ref = store.put_evidence(project.getvalue())
        record = {
            "schema": "tc.research-configuration.v1",
            "entity_id": str(entity),
            "revision": str(revision),
            "state": "approved",
            "sqx_build": "144.2953",
            "executable_xml_ref": str(evidence),
            "executable_xml_sha256": evidence.digest,
            "approval": {"approved": True},
            "source_project_ref": str(project_ref),
            "source_project_sha256": project_ref.digest,
        }
        reader = patch("tradercockpit.research_native_jobs.read_configuration_revision", return_value=record)
        reader.start()
        self.addCleanup(reader.stop)
        return record, str(revision)

    def test_launch_stages_exact_bytes_creates_job_and_retry_is_idempotent(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, xml)
            calls: list[Path] = []

            class FakeGateway:
                def launch_builder(self, path, *, expected_config_sha256):
                    config = Path(path).resolve()
                    calls.append(config)
                    relative = config.relative_to(home.resolve()).as_posix()
                    self_outer.assertTrue(task_document_from_cfx(config.read_bytes()).endswith(xml + b"</Task>"))
                    self_outer.assertEqual(expected_config_sha256, sha256(config.read_bytes()).hexdigest())
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
            packed = calls[0].read_bytes()
            self.assertTrue(task_document_from_cfx(packed).endswith(xml + b"</Task>"))
            self.assertEqual(calls[0].name, f"{sha256(packed).hexdigest()}.cfx")
            self.assertEqual(store.read_evidence(EvidenceRef.parse(launched["staged_config_ref"])), packed)
            self.assertEqual(launched["staged_config_sha256"], sha256(packed).hexdigest())

            catalog = list_current_native_jobs(store, revision)
            self.assertEqual(catalog["schema"], NATIVE_JOB_CATALOG_SCHEMA)
            self.assertEqual(len(catalog["jobs"]), 1)
            self.assertEqual(catalog["jobs"][0]["entity_id"], launched["entity_id"])

    def test_legacy_xml_jobs_remain_readable_without_archive_fields(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            configuration, revision = self._configuration(store, b"<Settings/>")
            ref = EvidenceRef.parse(configuration["executable_xml_ref"])
            entity = store.create_entity(ResearchKind.NATIVE_JOB)
            content = self._prepared_content(configuration, revision, ref.digest + ".xml")
            stored = store.create_revision(entity, content.canonical_bytes(), evidence=(ref,))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
            record = read_current_native_job(store, entity)
            self.assertEqual(record["state"], "prepared")
            self.assertNotIn("staged_config_ref", record)
            self.assertNotIn("staged_config_sha256", record)

    def _prepared_content(self, configuration, revision, filename):
        return NativeBuilderJobContent(
            state="prepared", configuration_entity_id=configuration["entity_id"], configuration_revision=revision,
            executable_xml_ref=EvidenceRef.parse(configuration["executable_xml_ref"]),
            executable_xml_sha256=configuration["executable_xml_sha256"], sqx_build="144.2953",
            operation=NATIVE_JOB_OPERATION, staged_config_relative_path=f"user/TraderCockpit/approved-configurations/{filename[:2]}/{filename}",
            launcher_sha256=None, partial_side_effect=False, receipts=(),
        )

    def test_changed_task_wrapper_cannot_claim_the_original_approved_configuration(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            settings = b"<Settings/>"
            configuration, revision = self._configuration(store, settings)
            project = store.read_evidence(EvidenceRef.parse(configuration["source_project_ref"]))
            document = task_document_from_cfx(pack_task_rooted_cfx(settings, project))
            changed = BytesIO()
            with ZipFile(changed, "w") as archive:
                archive.writestr("config.xml", document.replace(b'version="126.2189"', b'version="999.999"'))
            archive_ref = store.put_evidence(changed.getvalue())
            entity = store.create_entity(ResearchKind.NATIVE_JOB)
            content = self._prepared_content(configuration, revision, archive_ref.digest + ".cfx")
            stored = store.create_revision(entity, content.canonical_bytes(), evidence=(content.executable_xml_ref, archive_ref))
            store.compare_and_set_current(entity, expected_revision=None, target_revision=stored.revision)
            with self.assertRaises(ResearchNativeJobError) as caught:
                read_current_native_job(store, entity)
            self.assertEqual(caught.exception.code, "native_job_content_corrupt")

    def test_submitted_receipts_must_bind_archive_identity_and_exact_control_sequence(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            configuration, revision = self._configuration(store, b"<Settings/>")
            archive_digest, launcher = "a" * 64, "b" * 64
            prepared = self._prepared_content(configuration, revision, archive_digest + ".cfx")
            receipts = tuple({"sequence": i, "action": action, "project": "Builder", "state": "completed",
                              "exit_code": 0 if i == 1 else None, "sqx_build": "144.2953", "launcher_sha256": launcher,
                              "config_sha256": archive_digest, "reason_code": None} for i, action in enumerate(("loadconfig", "start"), 1))
            submitted = replace(prepared, state="submitted", launcher_sha256=launcher, receipts=receipts)
            self.assertEqual(NativeBuilderJobContent.from_bytes(submitted.canonical_bytes()), submitted)
            for field, value in (("sequence", 1), ("action", "loadconfig"), ("project", "Other"),
                                 ("config_sha256", configuration["executable_xml_sha256"]), ("launcher_sha256", "c" * 64),
                                 ("exit_code", 7), ("exit_code", False), ("sqx_build", "other")):
                with self.subTest(field=field, value=value), self.assertRaises(ResearchNativeJobError):
                    replace(submitted, receipts=(receipts[0], {**receipts[1], field: value}))

    def test_concurrent_retry_reserves_one_job_and_submits_native_control_once(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, b"<Settings/>")
            entered, release = Event(), Event()
            calls = []
            class Gateway:
                def launch_builder(self, path, *, expected_config_sha256):
                    calls.append(path)
                    entered.set()
                    if not release.wait(5):
                        raise AssertionError("test did not release native launch")
                    receipts = [{"sequence": i, "action": action, "project": "Builder", "state": "completed",
                                 "exit_code": 0 if i == 1 else None, "sqx_build": "144.2953", "launcher_sha256": "b" * 64,
                                 "config_sha256": expected_config_sha256, "reason_code": None} for i, action in enumerate(("loadconfig", "start"), 1)]
                    return {"schema": "tc.sqx-native-control.v1", "operation": NATIVE_JOB_OPERATION, "state": "submitted",
                            "sqx_build": "144.2953", "launcher_sha256": "b" * 64, "config_sha256": expected_config_sha256,
                            "config_relative_path": Path(path).relative_to(home).as_posix(), "receipts": receipts}
            def launch():
                return launch_approved_builder_configuration(store, home, "b" * 64,
                    configuration_entity_id=configuration["entity_id"], expected_configuration_revision=revision,
                    gateway_factory=lambda *args, **kwargs: Gateway())
            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration), ThreadPoolExecutor(max_workers=2) as pool:
                first = pool.submit(launch)
                self.assertTrue(entered.wait(5))
                second = pool.submit(launch)
                release.set()
                records = first.result(timeout=5), second.result(timeout=5)
            self.assertEqual(len(calls), 1)
            self.assertEqual(records[0]["entity_id"], records[1]["entity_id"])
            self.assertEqual(records[0]["revision"], records[1]["revision"])
            self.assertTrue(records[1]["reused"])
            self.assertEqual(len(list_current_native_jobs(store)["jobs"]), 1)

    def test_gateway_refusal_is_preserved_as_failed_job(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, xml)

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
            with patch("tradercockpit.research_native_jobs.read_current_configuration", return_value=configuration):
                retried = launch_approved_builder_configuration(
                    store, home, launcher_sha, configuration_entity_id=configuration["entity_id"],
                    expected_configuration_revision=revision,
                    gateway_factory=lambda *args, **kwargs: self.fail("failed launch must not be resubmitted"),
                )
            self.assertTrue(retried["reused"])
            self.assertEqual(retried["state"], "failed")
            catalog = list_current_native_jobs(store, revision)
            self.assertEqual(len(catalog["jobs"]), 1)
            failed = catalog["jobs"][0]
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "sqx_launcher_hash_mismatch")
            self.assertFalse(failed["partial_side_effect"])
            self.assertEqual(failed["receipts"][0]["state"], "preflight_failed")

    def test_unapproved_configuration_refuses_before_native_staging(self) -> None:
        xml = b"<Task/>"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, xml)
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
            configuration, revision = self._configuration(store, xml)
            digest = sha256(pack_task_rooted_cfx(xml, store.read_evidence(EvidenceRef.parse(configuration["source_project_ref"])))).hexdigest()
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


if __name__ == "__main__":
    unittest.main()
