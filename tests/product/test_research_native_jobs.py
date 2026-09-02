from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from tradercockpit.research_custody import EvidenceRef, FileResearchCustodyStore, ResearchKind, ResearchRevisionRef
from tradercockpit.research_native_jobs import (
    NATIVE_JOB_CATALOG_SCHEMA,
    NATIVE_JOB_READ_SCHEMA,
    ResearchNativeJobError,
    builder_loadconfig_cfx,
    launch_approved_builder_configuration,
    list_current_native_jobs,
)
from tradercockpit.sqx_gateway import SqxNativeGatewayError


def _project_cfx(task_xml: bytes) -> bytes:
    config = b'<Task taskXMLFile="Build-Task1.xml" />'
    buffer = BytesIO()
    with ZipFile(buffer, "w") as archive:
        for name, payload in (("config.xml", config), ("Build-Task1.xml", task_xml)):
            info = ZipInfo(name)
            info.date_time = (1980, 1, 1, 0, 0, 0)
            info.compress_type = ZIP_DEFLATED
            archive.writestr(info, payload)
    return buffer.getvalue()


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
        source = store.put_evidence(_project_cfx(xml))
        record = {
            "schema": "tc.research-configuration.v1",
            "entity_id": str(entity),
            "revision": str(revision),
            "state": "approved",
            "sqx_build": "144.2953",
            "executable_xml_ref": str(evidence),
            "executable_xml_sha256": evidence.digest,
            "source_project_ref": str(source),
            "source_project_sha256": source.digest,
            "approval": {"approved": True},
        }
        return record, str(revision)

    def test_launch_stages_exact_bytes_creates_job_and_retry_is_idempotent(self) -> None:
        xml = b"<Task><Exact>approved</Exact></Task>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, xml)
            expected_cfx = builder_loadconfig_cfx(_project_cfx(xml), xml)
            calls: list[Path] = []

            class FakeGateway:
                def launch_builder(self, path, *, expected_config_sha256):
                    config = Path(path).resolve()
                    calls.append(config)
                    relative = config.relative_to(home.resolve()).as_posix()
                    self_outer.assertEqual(config.read_bytes(), expected_cfx)
                    self_outer.assertEqual(expected_config_sha256, sha256(expected_cfx).hexdigest())
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

            catalog = list_current_native_jobs(store, revision)
            self.assertEqual(catalog["schema"], NATIVE_JOB_CATALOG_SCHEMA)
            self.assertEqual(len(catalog["jobs"]), 1)
            self.assertEqual(catalog["jobs"][0]["entity_id"], launched["entity_id"])

    def test_gateway_refusal_is_preserved_as_failed_job(self) -> None:
        xml = b"<Task><Exact>approved</Exact></Task>"
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
        xml = b"<Task><Exact>approved</Exact></Task>"
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision = self._configuration(store, xml)
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


if __name__ == "__main__":
    unittest.main()
