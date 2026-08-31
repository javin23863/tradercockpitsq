from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.research_custody import (
    FileResearchCustodyStore,
    ResearchCustodyError,
    ResearchEntityId,
    ResearchKind,
)
from tradercockpit.research_ideas import (
    IDEA_CATALOG_SCHEMA,
    IDEA_CONTENT_SCHEMA,
    IDEA_READ_SCHEMA,
    ResearchIdeaContent,
    ResearchIdeaError,
    create_idea,
    list_current_ideas,
    read_current_idea,
    revise_idea,
)


class ResearchIdeaTests(unittest.TestCase):
    def test_create_revise_and_reopen_preserve_exact_revision_chain(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            first = create_idea(
                store,
                text="Mean reversion around a native volatility filter.",
                source="Notebook observation 2026-08-31",
            )
            self.assertEqual(first["schema"], IDEA_READ_SCHEMA)
            self.assertTrue(str(first["entity_id"]).startswith("tc-research:idea:v1:"))
            self.assertIsNone(first["parent_revision"])

            second = revise_idea(
                store,
                entity_id=str(first["entity_id"]),
                expected_revision=str(first["revision"]),
                text="Mean reversion around a native volatility filter; long and short remain unresolved.",
                source="Notebook observation 2026-08-31",
            )
            self.assertEqual(second["entity_id"], first["entity_id"])
            self.assertEqual(second["parent_revision"], first["revision"])
            self.assertNotEqual(second["revision"], first["revision"])

            reopened = FileResearchCustodyStore(tmp)
            current = read_current_idea(reopened, str(first["entity_id"]))
            self.assertEqual(current, second)
            self.assertEqual(
                reopened.read_revision_content(
                    reopened.current(ResearchEntityId.parse(str(first["entity_id"])))
                ),
                ResearchIdeaContent(
                    text="Mean reversion around a native volatility filter; long and short remain unresolved.",
                    source="Notebook observation 2026-08-31",
                ).canonical_bytes(),
            )

    def test_stale_revision_update_is_rejected_without_moving_current_pointer(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            first = create_idea(store, text="first")
            second = revise_idea(
                store,
                entity_id=str(first["entity_id"]),
                expected_revision=str(first["revision"]),
                text="second",
            )

            with self.assertRaises(ResearchCustodyError) as raised:
                revise_idea(
                    FileResearchCustodyStore(tmp),
                    entity_id=str(first["entity_id"]),
                    expected_revision=str(first["revision"]),
                    text="stale third",
                )
            self.assertEqual(raised.exception.code, "current_conflict")
            self.assertEqual(read_current_idea(store, str(first["entity_id"]))["revision"], second["revision"])

    def test_catalog_lists_only_valid_current_idea_entities(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            left = create_idea(store, text="Alpha idea\nmore detail")
            right = create_idea(store, text="Beta idea")
            catalog = list_current_ideas(store)
            self.assertEqual(catalog["schema"], IDEA_CATALOG_SCHEMA)
            self.assertEqual(
                {item["entity_id"] for item in catalog["ideas"]},
                {left["entity_id"], right["entity_id"]},
            )
            self.assertEqual(
                {item["summary"] for item in catalog["ideas"]},
                {"Alpha idea", "Beta idea"},
            )

    def test_catalog_fails_closed_on_unexpected_current_directory_entry(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            create_idea(store, text="valid")
            unexpected = store.base / "current" / "idea" / "unexpected.txt"
            unexpected.write_text("not a pointer", encoding="utf-8")
            with self.assertRaises(ResearchCustodyError) as raised:
                list_current_ideas(store)
            self.assertEqual(raised.exception.code, "current_pointer_corrupt")

    def test_wrong_kind_identity_and_revision_are_rejected(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            config = store.create_entity(ResearchKind.CONFIGURATION)
            config_revision = store.create_revision(config, b"config")
            store.compare_and_set_current(
                config,
                expected_revision=None,
                target_revision=config_revision.revision,
            )
            with self.assertRaises(ResearchIdeaError):
                read_current_idea(store, str(config))

            idea = create_idea(store, text="idea")
            with self.assertRaises(ResearchIdeaError):
                revise_idea(
                    store,
                    entity_id=str(idea["entity_id"]),
                    expected_revision=str(config_revision.revision),
                    text="updated",
                )

    def test_content_validation_and_corruption_fail_closed(self) -> None:
        with self.assertRaises(ResearchIdeaError):
            ResearchIdeaContent(text="   ", source="")
        with self.assertRaises(ResearchIdeaError):
            ResearchIdeaContent(text="x", source=1)  # type: ignore[arg-type]

        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(tmp)
            entity = store.create_entity(ResearchKind.IDEA)
            bad = json.dumps(
                {"schema": IDEA_CONTENT_SCHEMA, "text": "idea", "source": "", "extra": True}
            ).encode("utf-8")
            revision = store.create_revision(entity, bad)
            store.compare_and_set_current(
                entity,
                expected_revision=None,
                target_revision=revision.revision,
            )
            with self.assertRaises(ResearchIdeaError) as raised:
                read_current_idea(store, entity)
            self.assertEqual(raised.exception.code, "idea_content_corrupt")


if __name__ == "__main__":
    unittest.main()
