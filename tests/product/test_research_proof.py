from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.research_custody import FileResearchCustodyStore, ResearchKind
from tradercockpit.research_ideas import create_idea, revise_idea
from tradercockpit.research_proof import (
    RESEARCH_PROOF_CATALOG_SCHEMA,
    RESEARCH_PROOF_READ_SCHEMA,
    ResearchProofError,
    create_research_proof,
    list_current_research_proofs,
    read_current_research_proof,
)


class ResearchProofTests(unittest.TestCase):
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

    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")
        self.idea = create_idea(self.store, text="Exact research idea", source="operator")

    def _records(self):
        configuration = {
            "schema": "tc.research-configuration.v1",
            "entity_id": self.CONFIG_ENTITY,
            "revision": self.CONFIG_REVISION,
            "state": "approved",
            "sqx_build": "144.2953",
            "source_project_ref": f"tc-evidence:sha256:{'a' * 64}",
            "source_project_sha256": "a" * 64,
            "executable_xml_ref": f"tc-evidence:sha256:{'b' * 64}",
            "executable_xml_sha256": "b" * 64,
        }
        native_job = {
            "schema": "tc.research-native-job.v1",
            "entity_id": self.JOB_ENTITY,
            "revision": self.JOB_REVISION,
            "state": "submitted",
            "sqx_build": "144.2953",
            "configuration_entity_id": self.CONFIG_ENTITY,
            "configuration_revision": self.CONFIG_REVISION,
            "operation": "builder_loadconfig_start",
            "launcher_sha256": "c" * 64,
        }
        candidate = {
            "schema": "tc.research-candidate.v1",
            "entity_id": self.CANDIDATE_ENTITY,
            "revision": self.CANDIDATE_REVISION,
            "sqx_build": "144.2953",
            "native_job_entity_id": self.JOB_ENTITY,
            "native_job_revision": self.JOB_REVISION,
            "configuration_entity_id": self.CONFIG_ENTITY,
            "configuration_revision": self.CONFIG_REVISION,
            "archive_ref": f"tc-evidence:sha256:{'d' * 64}",
            "archive_sha256": "d" * 64,
        }
        historical = {
            "schema": "tc.research-historical-result.v1",
            "entity_id": self.HISTORICAL_ENTITY,
            "revision": self.HISTORICAL_REVISION,
            "state": "completed",
            "execution_completed": True,
            "sqx_build": "144.2953",
            "candidate_entity_id": self.CANDIDATE_ENTITY,
            "candidate_revision": self.CANDIDATE_REVISION,
            "result_archive_ref": f"tc-evidence:sha256:{'e' * 64}",
            "result_archive_sha256": "e" * 64,
            "engine_ref": f"tc-evidence:sha256:{'f' * 64}",
            "engine_sha256": "f" * 64,
            "launcher_sha256": "1" * 64,
            "native_project_name": "TraderCockpit-Retester-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        }
        trades = {
            "schema": "tc.research-historical-trades.v1",
            "historical_result_entity_id": self.HISTORICAL_ENTITY,
            "historical_result_revision": self.HISTORICAL_REVISION,
            "candidate_entity_id": self.CANDIDATE_ENTITY,
            "candidate_revision": self.CANDIDATE_REVISION,
            "result_archive_ref": historical["result_archive_ref"],
            "result_archive_sha256": historical["result_archive_sha256"],
            "rows": [],
        }
        validation = {
            "schema": "tc.research-native-robustness.v1",
            "validation_ref": self.VALIDATION_REF,
            "proof_entity_id": self.INTERNAL_PROOF_ENTITY,
            "proof_revision": self.INTERNAL_PROOF_REVISION,
            "sqx_build": "144.2953",
            "operation": "native_retester_cross_check",
            "method": "RetestWithHigherPrecision",
            "execution_state": "completed",
            "producer_outcome_state": "producer_result_captured_outcome_unread",
            "source_historical_result_entity_id": self.HISTORICAL_ENTITY,
            "source_historical_result_revision": self.HISTORICAL_REVISION,
            "source_result_archive_ref": historical["result_archive_ref"],
            "source_result_archive_sha256": historical["result_archive_sha256"],
            "result_archive_ref": f"tc-evidence:sha256:{'8' * 64}",
            "result_archive_sha256": "8" * 64,
        }
        return configuration, native_job, candidate, historical, trades, validation

    def _patch_records(self, *, mutate=None):
        configuration, native_job, candidate, historical, trades, validation = self._records()
        records = {
            "configuration": configuration,
            "native_job": native_job,
            "candidate": candidate,
            "historical": historical,
            "trades": trades,
            "validation": validation,
        }
        if mutate is not None:
            mutate(records)
        return (
            patch("tradercockpit.research_proof.read_current_configuration", return_value=configuration),
            patch("tradercockpit.research_proof.read_current_native_job", return_value=native_job),
            patch("tradercockpit.research_proof.read_current_candidate", return_value=candidate),
            patch("tradercockpit.research_proof.read_historical_result_revision", return_value=historical),
            patch("tradercockpit.research_proof.read_historical_trades", return_value=trades),
            patch("tradercockpit.research_proof.read_native_robustness_result", return_value=validation),
        )

    def _with_records(self, callback, *, mutate=None):
        patches = self._patch_records(mutate=mutate)
        with patches[0], patches[1], patches[2], patches[3], patches[4], patches[5]:
            return callback()

    def _create(self):
        return create_research_proof(
            self.store,
            idea_entity_id=self.idea["entity_id"],
            idea_revision=self.idea["revision"],
            historical_result_entity_id=self.HISTORICAL_ENTITY,
            historical_result_revision=self.HISTORICAL_REVISION,
            validation_ref=self.VALIDATION_REF,
        )

    def test_proof_binds_exact_chain_without_inventing_validation_verdict(self):
        proof = self._with_records(self._create)
        self.assertEqual(proof["schema"], RESEARCH_PROOF_READ_SCHEMA)
        self.assertFalse(proof["reused"])
        self.assertEqual(proof["idea"]["revision"], self.idea["revision"])
        self.assertEqual(proof["configuration"]["revision"], self.CONFIG_REVISION)
        self.assertEqual(proof["native_job"]["revision"], self.JOB_REVISION)
        self.assertEqual(proof["candidate"]["revision"], self.CANDIDATE_REVISION)
        self.assertEqual(proof["historical_result"]["revision"], self.HISTORICAL_REVISION)
        self.assertEqual(proof["validation"]["validation_ref"], self.VALIDATION_REF)
        self.assertFalse(proof["truth"]["producer_verdict_available"])
        self.assertEqual(
            proof["truth"]["producer_validation_outcome"],
            "producer_result_captured_outcome_unread",
        )

    def test_exact_idea_revision_reopens_after_idea_current_advances(self):
        proof = self._with_records(self._create)
        revised = revise_idea(
            self.store,
            entity_id=self.idea["entity_id"],
            expected_revision=self.idea["revision"],
            text="Later idea revision",
            source="operator",
        )
        self.assertNotEqual(revised["revision"], self.idea["revision"])

        reopened = self._with_records(
            lambda: read_current_research_proof(self.store, proof["entity_id"])
        )
        self.assertEqual(reopened["idea"]["revision"], self.idea["revision"])
        self.assertEqual(reopened["idea"]["text"], "Exact research idea")

    def test_validation_from_another_historical_result_is_rejected(self):
        def mutate(records):
            records["validation"]["source_historical_result_revision"] = (
                f"tc-research-revision:historical-result:sha256:{'9' * 64}"
            )

        with self.assertRaises(ResearchProofError) as caught:
            self._with_records(self._create, mutate=mutate)
        self.assertEqual(caught.exception.code, "research_proof_validation_invalid")

    def test_native_job_configuration_substitution_is_rejected(self):
        def mutate(records):
            records["native_job"]["configuration_revision"] = (
                f"tc-research-revision:configuration:sha256:{'9' * 64}"
            )

        with self.assertRaises(ResearchProofError) as caught:
            self._with_records(self._create, mutate=mutate)
        self.assertEqual(caught.exception.code, "research_proof_chain_invalid")

    def test_catalog_skips_internal_robustness_proof_records(self):
        proof = self._with_records(self._create)
        foreign = self.store.create_entity(ResearchKind.PROOF)
        foreign_revision = self.store.create_revision(
            foreign,
            b'{"schema":"tc.research-native-robustness-attempt.v1","state":"failed"}',
        )
        self.store.compare_and_set_current(
            foreign,
            expected_revision=None,
            target_revision=foreign_revision.revision,
        )

        catalog = self._with_records(lambda: list_current_research_proofs(self.store))
        self.assertEqual(catalog["schema"], RESEARCH_PROOF_CATALOG_SCHEMA)
        self.assertEqual(len(catalog["proofs"]), 1)
        self.assertEqual(catalog["proofs"][0]["entity_id"], proof["entity_id"])

    def test_duplicate_exact_proof_is_reused(self):
        first = self._with_records(self._create)
        second = self._with_records(self._create)
        self.assertEqual(first["entity_id"], second["entity_id"])
        self.assertEqual(first["revision"], second["revision"])
        self.assertTrue(second["reused"])


if __name__ == "__main__":
    unittest.main()
