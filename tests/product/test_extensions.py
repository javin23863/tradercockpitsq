from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.extensions import (
    BUILTIN_CAPABILITIES,
    CAPABILITY_REGISTRY_SCHEMA,
    DESCRIPTOR_VERSION,
    EXTENSIONS_MANIFEST_SCHEMA,
    ExtensionsError,
    capability_registry_record,
    extensions_status_record,
    register_addon_slot,
    write_extensions_manifest,
)


class ExtensionsRegistryTests(unittest.TestCase):
    def test_builtin_registry_is_ready_without_data_root(self) -> None:
        record = extensions_status_record(None)
        self.assertEqual(record["status"], "ready")
        self.assertIsNone(record["reason_code"])
        registry = record["registry"]
        self.assertEqual(registry["schema"], CAPABILITY_REGISTRY_SCHEMA)
        self.assertEqual(len(registry["capabilities"]), len(BUILTIN_CAPABILITIES))
        self.assertEqual(registry["addons"], [])
        capability_ids = {item["id"] for item in registry["capabilities"]}
        self.assertEqual(
            capability_ids,
            {
                "runtime-status",
                "research-custody",
                "market-quotes",
                "operate-promotions",
                "sqx-custom-project-control",
                "assistant",
            },
        )
        for item in registry["capabilities"]:
            self.assertEqual(item["descriptor_version"], DESCRIPTOR_VERSION)
            self.assertEqual(item["kind"], "capability")
            self.assertEqual(item["owner"], "platform")

    def test_valid_manifest_adds_typed_addon_slots(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_extensions_manifest(
                root,
                [
                    {
                        "id": "sample-feed",
                        "descriptor_version": DESCRIPTOR_VERSION,
                        "owner": "operator",
                        "placement": ["explore"],
                        "read_schema": "tc.sample-feed-read.v1",
                    }
                ],
            )
            record = extensions_status_record(root)

        self.assertEqual(record["status"], "ready")
        self.assertEqual(len(record["registry"]["addons"]), 1)
        addon = record["registry"]["addons"][0]
        self.assertEqual(addon["id"], "sample-feed")
        self.assertEqual(addon["kind"], "addon")
        self.assertEqual(addon["availability"], "registered")

    def test_unknown_manifest_schema_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "extensions.json").write_text(
                json.dumps({"schema": "tc.legacy.v9", "addons": []}),
                encoding="utf-8",
            )
            record = extensions_status_record(root)

        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "manifest_invalid")
        self.assertEqual(record["registry"]["addons"], [])

    def test_unknown_descriptor_version_fails_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "extensions.json").write_text(
                json.dumps(
                    {
                        "schema": EXTENSIONS_MANIFEST_SCHEMA,
                        "addons": [
                            {
                                "id": "bad",
                                "descriptor_version": "99",
                                "owner": "operator",
                                "placement": ["explore"],
                                "read_schema": "tc.bad.v1",
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            record = extensions_status_record(root)

        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "manifest_invalid")

    def test_register_addon_persists_typed_slot(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            record = register_addon_slot(
                root,
                {
                    "action": "register",
                    "addon": {
                        "id": "macro-overlay",
                        "descriptor_version": DESCRIPTOR_VERSION,
                        "owner": "operator",
                        "placement": ["explore"],
                        "read_schema": "tc.macro-overlay-read.v1",
                    },
                },
            )
            self.assertEqual(record["schema"], CAPABILITY_REGISTRY_SCHEMA)
            self.assertEqual(record["registered"]["id"], "macro-overlay")
            payload = capability_registry_record(root)
            self.assertEqual(len(payload["addons"]), 1)

    def test_register_refuses_forbidden_script_slot(self) -> None:
        with TemporaryDirectory() as tmp:
            with self.assertRaises(ExtensionsError) as ctx:
                register_addon_slot(
                    Path(tmp),
                    {
                        "action": "register",
                        "addon": {
                            "id": "evil",
                            "descriptor_version": DESCRIPTOR_VERSION,
                            "owner": "operator",
                            "placement": ["explore"],
                            "read_schema": "tc.evil.v1",
                            "script": "alert(1)",
                        },
                    },
                )
        self.assertEqual(ctx.exception.code, "extension_registration_forbidden")

    def test_register_refuses_nav_rewrite_slot_type(self) -> None:
        with TemporaryDirectory() as tmp:
            with self.assertRaises(ExtensionsError) as ctx:
                register_addon_slot(
                    Path(tmp),
                    {
                        "action": "register",
                        "addon": {
                            "id": "nav",
                            "descriptor_version": DESCRIPTOR_VERSION,
                            "owner": "operator",
                            "placement": ["home"],
                            "read_schema": "tc.nav.v1",
                            "slot_type": "nav_rewrite",
                        },
                    },
                )
        self.assertEqual(ctx.exception.code, "extension_registration_forbidden")


if __name__ == "__main__":
    unittest.main()
