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
    NativeWorkerRegistry,
    ResearchNativeJobError,
    builder_loadconfig_cfx,
    launch_approved_builder_configuration,
    list_current_native_jobs,
    refresh_native_job,
    stop_native_job,
)
from tradercockpit.sqx_gateway import SqxBuilderWorker, SqxNativeGatewayError


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
        revision = ResearchRevisionRef(ResearchKind.CONFIGURATION, sha256(b"approved-config-revision" + xml).hexdigest())
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
                def launch_builder(self, path, *, expected_config_sha256, worker_log_path=None):
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

    def _running_receipts(self, launcher_sha: str, config_sha: str, start_state: str) -> list[dict[str, object]]:
        base = {"project": "Builder", "sqx_build": "144.2953", "launcher_sha256": launcher_sha, "config_sha256": config_sha, "reason_code": None}
        return [
            {"sequence": 1, "action": "loadconfig", "state": "completed", "exit_code": 0, **base},
            {"sequence": 2, "action": "start", "state": start_state, "exit_code": None if start_state == "running" else 0, **base},
        ]

    def test_supervised_launch_runs_refreshes_with_live_status_and_stops_gracefully(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)
            outer = self

            class FakeProcess:
                pid = 777

                def __init__(self) -> None:
                    self.exit_code: int | None = None

                def poll(self):
                    return self.exit_code

                def wait(self, timeout=None):
                    return self.exit_code

                def terminate(self):
                    self.exit_code = 1

                def kill(self):
                    self.exit_code = -9

            http_calls: list[str] = []

            class FakeGateway:
                worker = None

                def launch_builder(self, path, *, expected_config_sha256, worker_log_path=None):
                    outer.assertIsNotNone(worker_log_path)
                    outer.assertEqual(Path(worker_log_path).parent, store.base / "native-worker-logs")
                    Path(worker_log_path).parent.mkdir(parents=True, exist_ok=True)
                    Path(worker_log_path).write_text("Server started on port 5050\n=========== Project started ===========\n", encoding="utf-8")
                    self.worker = SqxBuilderWorker(FakeProcess(), log_path=Path(worker_log_path), http_port=5050)
                    return {
                        "schema": "tc.sqx-native-control.v1",
                        "operation": "builder_loadconfig_start",
                        "project": "Builder",
                        "state": "running",
                        "sqx_build": "144.2953",
                        "launcher_sha256": launcher_sha,
                        "config_relative_path": Path(path).resolve().relative_to(home.resolve()).as_posix(),
                        "config_sha256": expected_config_sha256,
                        "control_requests_submitted": 2,
                        "control_requests_completed": 2,
                        "partial_side_effect": False,
                        "receipts": self._running_receipts(expected_config_sha256),
                        "worker": {"pid": 777, "http_port": 5050, "log_path": str(worker_log_path)},
                    }

                def _running_receipts(self, config_sha):
                    return outer._running_receipts(launcher_sha, config_sha, "running")

            supervised: list[tuple[object, str]] = []

            def supervisor_register(process, *, label, timeout_seconds):
                supervised.append((process, label))

            registry = NativeWorkerRegistry(supervisor_register)

            def fake_http(port, command, **kwargs):
                http_calls.append(command)
                if "action=status" in command:
                    return "14:27:45 Status of project Builder\n--------------------------------------------------\nStrategies generated                          1279\nIn databank                                      1\n"
                if "action=stop" in command:
                    worker = registry.get(launched["entity_id"])
                    worker.log_path.write_text(worker.read_log() + "Project stopped\nAll tasks completed\nBye\n", encoding="utf-8")
                    worker.process.exit_code = 0
                    return "Stopping project Builder\nProject execution stopped.\n"
                raise AssertionError(command)

            configuration_b, revision_b, _ = self._configuration(store, b"<Settings><Other>approved</Other></Settings>")
            configurations = {configuration["entity_id"]: configuration, configuration_b["entity_id"]: configuration_b}

            with patch(
                "tradercockpit.research_native_jobs.read_current_configuration",
                side_effect=lambda _store, entity_id: configurations[entity_id],
            ), patch("tradercockpit.sqx_gateway.sqx_http_command", side_effect=fake_http):
                launched = launch_approved_builder_configuration(
                    store, home, launcher_sha,
                    configuration_entity_id=configuration["entity_id"],
                    expected_configuration_revision=revision,
                    gateway_factory=lambda *args, **kwargs: FakeGateway(),
                    worker_registry=registry,
                )
                self.assertEqual(launched["state"], "running")
                self.assertEqual(launched["worker"]["pid"], 777)
                self.assertEqual(launched["worker"]["http_port"], 5050)
                self.assertIsNone(launched["completion"])
                self.assertEqual(len(supervised), 1)
                self.assertEqual(supervised[0][1], f"sqx-builder:{launched['entity_id']}")
                self.assertIs(supervised[0][0], registry.get(launched["entity_id"]))

                with self.assertRaises(ResearchNativeJobError) as busy:
                    launch_approved_builder_configuration(
                        store, home, launcher_sha,
                        configuration_entity_id=configuration_b["entity_id"],
                        expected_configuration_revision=revision_b,
                        gateway_factory=lambda *args, **kwargs: FakeGateway(),
                        worker_registry=registry,
                    )
                self.assertEqual(busy.exception.code, "native_job_busy")
                self.assertEqual(len(list_current_native_jobs(store)["jobs"]), 1, "busy refusal must not strand a prepared job")

                refreshed = refresh_native_job(store, launched["entity_id"], worker_registry=registry)
                self.assertEqual(refreshed["state"], "running")
                self.assertTrue(refreshed["supervised"])
                self.assertEqual(refreshed["live"]["rows"]["Strategies generated"], "1279")
                self.assertEqual(refreshed["live"]["rows"]["In databank"], "1")
                self.assertEqual(refreshed["revision"], launched["revision"])

                stopped = stop_native_job(store, launched["entity_id"], worker_registry=registry)
                self.assertEqual(stopped["state"], "stopped")
                self.assertEqual(stopped["receipts"][1]["state"], "completed")
                self.assertEqual(stopped["receipts"][1]["exit_code"], 0)
                self.assertTrue(stopped["completion"]["stop_requested"])
                self.assertEqual(stopped["completion"]["exit_code"], 0)
                self.assertEqual(stopped["parent_revision"], launched["revision"])
                log_ref = EvidenceRef.parse(stopped["completion"]["log_ref"])
                self.assertIn(b"All tasks completed", store.read_evidence(log_ref))
                self.assertIsNone(registry.get(launched["entity_id"]))
                self.assertEqual([c.split()[1] for c in http_calls], ["action=status", "action=stop"])

                again = refresh_native_job(store, launched["entity_id"], worker_registry=registry)
                self.assertEqual(again, stopped)
                with self.assertRaises(ResearchNativeJobError) as not_running:
                    stop_native_job(store, launched["entity_id"], worker_registry=registry)
                self.assertEqual(not_running.exception.code, "native_job_not_running")

            reopened = FileResearchCustodyStore(root / "data")
            self.assertEqual(list_current_native_jobs(reopened)["jobs"][0]["state"], "stopped")

    def test_refresh_finalises_worker_that_exited_without_finishing_as_failed(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)
            ref = EvidenceRef.parse(str(configuration["executable_xml_ref"]))
            log_path = store.base / "native-worker-logs" / "job.log"
            log_path.parent.mkdir(parents=True)
            log_path.write_text("=========== Project started ===========\nOutOfMemoryError\n", encoding="utf-8")
            entity = store.create_entity(ResearchKind.NATIVE_JOB)
            common = dict(
                configuration_entity_id=str(configuration["entity_id"]),
                configuration_revision=revision,
                executable_xml_ref=ref,
                executable_xml_sha256=ref.digest,
                sqx_build="144.2953",
                operation=NATIVE_JOB_OPERATION,
                staged_config_relative_path=f"user/TraderCockpit/approved-configurations/{ref.digest[:2]}/{ref.digest}.cfx",
            )
            prepared = store.create_revision(
                entity,
                NativeBuilderJobContent(state="prepared", launcher_sha256=None, partial_side_effect=False, receipts=(), **common).canonical_bytes(),
                evidence=(ref,),
            )
            worker = {"pid": 5, "http_port": 5050, "log_path": str(log_path), "started_at": "2026-09-02T07:00:00+00:00"}
            running = store.create_revision(
                entity,
                NativeBuilderJobContent(
                    state="running", launcher_sha256=launcher_sha, partial_side_effect=False,
                    receipts=tuple(self._running_receipts(launcher_sha, "c" * 64, "running")), worker=worker, **common,
                ).canonical_bytes(),
                parent_revision=prepared.revision,
                evidence=(ref,),
            )
            store.compare_and_set_current(entity, expected_revision=None, target_revision=running.revision)

            class DeadProcess:
                pid = 5

                def poll(self):
                    return 137

                def wait(self, timeout=None):
                    return 137

                def terminate(self):
                    pass

                def kill(self):
                    pass

            registry = NativeWorkerRegistry()
            registry.register(str(entity), SqxBuilderWorker(DeadProcess(), log_path=log_path, http_port=5050))
            failed = refresh_native_job(store, entity, worker_registry=registry)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "sqx_worker_exited")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["receipts"][1]["state"], "rejected")
            self.assertEqual(failed["completion"]["exit_code"], 137)
            self.assertIn(b"OutOfMemoryError", store.read_evidence(EvidenceRef.parse(failed["completion"]["log_ref"])))

            # A running record nobody in this process owns is reported unsupervised while SQX answers,
            # and finalised as failed once its HTTP API is gone.
            store2 = FileResearchCustodyStore(root / "data2")
            configuration2, revision2, _ = self._configuration(store2, xml)
            ref2 = EvidenceRef.parse(str(configuration2["executable_xml_ref"]))
            entity2 = store2.create_entity(ResearchKind.NATIVE_JOB)
            common2 = {**common, "configuration_entity_id": str(configuration2["entity_id"]), "configuration_revision": revision2, "executable_xml_ref": ref2, "executable_xml_sha256": ref2.digest, "staged_config_relative_path": f"user/TraderCockpit/approved-configurations/{ref2.digest[:2]}/{ref2.digest}.cfx"}
            prepared2 = store2.create_revision(entity2, NativeBuilderJobContent(state="prepared", launcher_sha256=None, partial_side_effect=False, receipts=(), **common2).canonical_bytes(), evidence=(ref2,))
            running2 = store2.create_revision(
                entity2,
                NativeBuilderJobContent(state="running", launcher_sha256=launcher_sha, partial_side_effect=False, receipts=tuple(self._running_receipts(launcher_sha, "c" * 64, "running")), worker=worker, **common2).canonical_bytes(),
                parent_revision=prepared2.revision, evidence=(ref2,),
            )
            store2.compare_and_set_current(entity2, expected_revision=None, target_revision=running2.revision)
            with patch("tradercockpit.sqx_gateway.sqx_http_command", return_value="Status of project Builder\nStrategies generated    9\n"):
                unsupervised = refresh_native_job(store2, entity2, worker_registry=NativeWorkerRegistry())
            self.assertEqual(unsupervised["state"], "running")
            self.assertFalse(unsupervised["supervised"])
            self.assertEqual(unsupervised["live"]["rows"]["Strategies generated"], "9")
            with self.assertRaises(ResearchNativeJobError) as cannot_stop:
                stop_native_job(store2, entity2, worker_registry=NativeWorkerRegistry())
            self.assertEqual(cannot_stop.exception.code, "native_job_unsupervised")
            with patch("tradercockpit.sqx_gateway.sqx_http_command", side_effect=SqxNativeGatewayError("sqx_http_unreachable", "gone")):
                lost = refresh_native_job(store2, entity2, worker_registry=NativeWorkerRegistry())
            self.assertEqual(lost["state"], "failed")
            self.assertEqual(lost["completion"]["exit_code"], -1)

    def test_gateway_refusal_is_preserved_as_failed_job(self) -> None:
        xml = b"<Settings><Exact>approved</Exact></Settings>"
        launcher_sha = sha256(b"trusted-launcher").hexdigest()
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            store = FileResearchCustodyStore(root / "data")
            configuration, revision, _source = self._configuration(store, xml)

            class FailingGateway:
                def launch_builder(self, path, *, expected_config_sha256, worker_log_path=None):
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
