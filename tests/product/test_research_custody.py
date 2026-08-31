from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Barrier
import unittest
from uuid import UUID

from tradercockpit.research_custody import (
    EVIDENCE_REF_SCHEMA,
    RESEARCH_CURRENT_SCHEMA,
    RESEARCH_ENTITY_ID_SCHEMA,
    RESEARCH_REVISION_SCHEMA,
    EvidenceRef,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
    ResearchRevisionRef,
    research_custody_capability_record,
)


class ResearchCustodyTests(unittest.TestCase):
    def assertCode(self, code: str, callback) -> ResearchCustodyError:
        with self.assertRaises(ResearchCustodyError) as raised:
            callback()
        self.assertEqual(raised.exception.code, code)
        return raised.exception

    def test_entity_identity_is_typed_namespaced_and_separate_from_account_identity(self) -> None:
        for kind in ResearchKind:
            identity = ResearchEntityId.new(kind)
            self.assertEqual(ResearchEntityId.parse(str(identity)), identity)
            self.assertTrue(str(identity).startswith(f"tc-research:{kind.value}:v1:"))

        self.assertCode(
            "entity_id_invalid",
            lambda: ResearchEntityId.parse("tc-account:google:v1:sha256:abcd"),
        )
        self.assertCode(
            "entity_id_invalid",
            lambda: ResearchEntityId.parse(
                "tc-research:idea:v2:00000000-0000-0000-0000-000000000001"
            ),
        )
        self.assertCode(
            "entity_kind_invalid",
            lambda: ResearchEntityId.new("idea"),  # type: ignore[arg-type]
        )
        self.assertCode(
            "entity_id_invalid",
            lambda: ResearchEntityId(ResearchKind.IDEA, "not-a-uuid"),  # type: ignore[arg-type]
        )

        canonical = ResearchEntityId(
            ResearchKind.IDEA,
            UUID("12345678-1234-5678-1234-567812345678"),
        )
        self.assertCode(
            "entity_id_invalid",
            lambda: ResearchEntityId.parse(str(canonical).upper()),
        )

    def test_evidence_and_revision_are_content_addressed_and_idempotent(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.CONFIGURATION)
            native_bytes = store.put_evidence(b"native-project-bytes")
            content = b'{"schema":"example.configuration.v1","approved":false}'

            first = store.create_revision(
                entity,
                content,
                evidence=(native_bytes, native_bytes),
            )
            second = store.create_revision(
                entity,
                content,
                evidence=(native_bytes,),
            )

            self.assertEqual(first.revision, second.revision)
            self.assertEqual(first.evidence, (native_bytes,))
            self.assertEqual(store.read_revision(first.revision), first)
            self.assertEqual(store.read_revision_content(first.revision), content)
            self.assertEqual(store.read_evidence(native_bytes), b"native-project-bytes")
            self.assertTrue(str(first.revision).startswith("tc-research-revision:configuration:sha256:"))

    def test_revision_parent_is_bound_to_one_entity(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            first_entity = store.create_entity(ResearchKind.IDEA)
            second_entity = store.create_entity(ResearchKind.IDEA)
            parent = store.create_revision(first_entity, b"revision-1")

            self.assertCode(
                "revision_parent_mismatch",
                lambda: store.create_revision(
                    second_entity,
                    b"revision-2",
                    parent_revision=parent.revision,
                ),
            )

    def test_missing_evidence_and_corrupt_immutable_content_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.CANDIDATE)
            missing = EvidenceRef("0" * 64)
            self.assertCode(
                "evidence_missing",
                lambda: store.create_revision(entity, b"candidate", evidence=(missing,)),
            )

            evidence = store.put_evidence(b"exact-native-archive")
            evidence_path = (
                store.base
                / "evidence"
                / "sha256"
                / evidence.digest[:2]
                / f"{evidence.digest}.bin"
            )
            evidence_path.write_bytes(b"tampered")
            self.assertCode(
                "immutable_evidence_corrupt",
                lambda: store.read_evidence(evidence),
            )

        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.PROOF)
            revision = store.create_revision(entity, b"proof-envelope")
            revision_path = (
                store.base
                / "revisions"
                / revision.revision.kind.value
                / revision.revision.digest[:2]
                / f"{revision.revision.digest}.json"
            )
            revision_path.write_text("{}", encoding="utf-8")
            self.assertCode(
                "immutable_revision_corrupt",
                lambda: store.read_revision(revision.revision),
            )

    def test_current_pointer_is_explicit_compare_and_set_and_survives_reopen(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.IDEA)
            first = store.create_revision(entity, b"first")
            second = store.create_revision(
                entity,
                b"second",
                parent_revision=first.revision,
            )

            self.assertCode("current_pointer_missing", lambda: store.current(entity))
            self.assertEqual(
                store.compare_and_set_current(
                    entity,
                    expected_revision=None,
                    target_revision=first.revision,
                ),
                first.revision,
            )
            self.assertCode(
                "current_conflict",
                lambda: store.compare_and_set_current(
                    entity,
                    expected_revision=None,
                    target_revision=second.revision,
                ),
            )
            store.compare_and_set_current(
                entity,
                expected_revision=first.revision,
                target_revision=second.revision,
            )

            reopened = FileResearchCustodyStore(tmp)
            self.assertEqual(reopened.current(entity), second.revision)

    def test_current_pointer_refuses_cross_entity_substitution(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            left = store.create_entity(ResearchKind.HISTORICAL_RESULT)
            right = store.create_entity(ResearchKind.HISTORICAL_RESULT)
            right_revision = store.create_revision(right, b"result")

            self.assertCode(
                "current_target_mismatch",
                lambda: store.compare_and_set_current(
                    left,
                    expected_revision=None,
                    target_revision=right_revision.revision,
                ),
            )

    def test_concurrent_compare_and_set_admits_exactly_one_successor(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.CONFIGURATION)
            base = store.create_revision(entity, b"base")
            left = store.create_revision(
                entity,
                b"left",
                parent_revision=base.revision,
            )
            right = store.create_revision(
                entity,
                b"right",
                parent_revision=base.revision,
            )
            store.compare_and_set_current(
                entity,
                expected_revision=None,
                target_revision=base.revision,
            )

            barrier = Barrier(2)

            def update(target: ResearchRevisionRef) -> tuple[str, str]:
                contender = FileResearchCustodyStore(tmp)
                barrier.wait(timeout=5)
                try:
                    contender.compare_and_set_current(
                        entity,
                        expected_revision=base.revision,
                        target_revision=target,
                    )
                    return ("success", str(target))
                except ResearchCustodyError as exc:
                    return (exc.code, str(target))

            with ThreadPoolExecutor(max_workers=2) as pool:
                outcomes = list(pool.map(update, (left.revision, right.revision)))

            self.assertEqual([item[0] for item in outcomes].count("success"), 1)
            self.assertEqual([item[0] for item in outcomes].count("current_conflict"), 1)
            current = store.current(entity)
            successful = next(item[1] for item in outcomes if item[0] == "success")
            self.assertEqual(str(current), successful)

    def test_corrupt_current_pointer_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.NATIVE_JOB)
            revision = store.create_revision(entity, b"job-binding")
            store.compare_and_set_current(
                entity,
                expected_revision=None,
                target_revision=revision.revision,
            )
            pointer_path = (
                store.base
                / "current"
                / entity.kind.value
                / f"{entity.value}.json"
            )
            pointer_path.write_text(
                json.dumps(
                    {
                        "schema": RESEARCH_CURRENT_SCHEMA,
                        "entity_id": str(entity),
                        "revision": "tc-research-revision:candidate:sha256:" + "0" * 64,
                    }
                ),
                encoding="utf-8",
            )
            self.assertCode("current_pointer_corrupt", lambda: store.current(entity))

    def test_public_capability_descriptor_is_bounded_and_not_active_subject_state(self) -> None:
        payload = research_custody_capability_record()
        self.assertEqual(payload["status"], "ready")
        self.assertEqual(payload["identity_schema"], RESEARCH_ENTITY_ID_SCHEMA)
        self.assertEqual(payload["revision_schema"], RESEARCH_REVISION_SCHEMA)
        self.assertEqual(payload["evidence_schema"], EVIDENCE_REF_SCHEMA)
        self.assertEqual(payload["current_schema"], RESEARCH_CURRENT_SCHEMA)
        self.assertEqual(payload["current_update"], "compare-and-set")
        self.assertFalse(payload["active_subject"])
        self.assertEqual(
            payload["record_kinds"],
            [kind.value for kind in ResearchKind],
        )


if __name__ == "__main__":
    unittest.main()
