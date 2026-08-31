from __future__ import annotations

import hashlib
import json
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.research_custody import (
    RESEARCH_REVISION_SCHEMA,
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchKind,
    ResearchRevisionRef,
)


class ResearchCustodyAdversarialTests(unittest.TestCase):
    def test_forged_same_kind_cross_entity_parent_is_rejected_on_read(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            parent_entity = store.create_entity(ResearchKind.IDEA)
            child_entity = store.create_entity(ResearchKind.IDEA)
            parent = store.create_revision(parent_entity, b"parent")
            content = store.put_evidence(b"forged-child")

            payload = {
                "schema": RESEARCH_REVISION_SCHEMA,
                "entity_id": str(child_entity),
                "kind": ResearchKind.IDEA.value,
                "parent_revision": str(parent.revision),
                "content": str(content),
                "evidence": [],
            }
            encoded = json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
            digest = hashlib.sha256(encoded).hexdigest()
            forged = ResearchRevisionRef(ResearchKind.IDEA, digest)
            path = (
                store.base
                / "revisions"
                / forged.kind.value
                / forged.digest[:2]
                / f"{forged.digest}.json"
            )
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(encoded)

            with self.assertRaises(ResearchCustodyError) as raised:
                store.read_revision(forged)
            self.assertEqual(raised.exception.code, "immutable_revision_corrupt")
            self.assertIn("parent entity binding", raised.exception.detail)


if __name__ == "__main__":
    unittest.main()
