from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import capabilities_response, make_handler
from tradercockpit.capability_registry import (
    ADDON_DESCRIPTOR_VERSION,
    ADDON_SCHEMA,
    NAV_AUTHORITY,
    NONE_SCHEMA,
    PLATFORM_SURFACES,
    REGISTERED_SLOTS,
    REGISTRY_API_PATH,
    REGISTRY_SCHEMA,
    CapabilityRegistryError,
    addon_from_payload,
    capability_registry_record,
    extensions_status_record,
    registered_slots,
)
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.runtime_status import runtime_status_record


class CapabilityRegistryTests(unittest.TestCase):
    def _addon(self, **overrides: object) -> dict[str, object]:
        payload: dict[str, object] = {
            "schema": ADDON_SCHEMA,
            "descriptor_version": ADDON_DESCRIPTOR_VERSION,
            "id": "operator.watch-note",
            "version": "1.0.0",
            "producer": "operator",
            "availability": "ready",
            "slot": "explore.extensions",
            "config_schema": NONE_SCHEMA,
            "read_schema": NONE_SCHEMA,
            "action_schema": None,
            "presentation": {
                "title": "Watch note",
                "detail": "Operator reminder for this slot. Plain text only.",
            },
        }
        payload.update(overrides)
        return payload

    def _write_addon(self, root: Path, name: str, payload: dict[str, object]) -> Path:
        directory = root / "addons"
        directory.mkdir(parents=True, exist_ok=True)
        path = directory / name
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def _request_json(self, url: str, *, method: str = "GET") -> tuple[int, dict[str, object]]:
        request = Request(url, data=b"" if method == "POST" else None, method=method)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_empty_registry_is_ready_with_frozen_surfaces_and_typed_slots(self) -> None:
        record = capability_registry_record(None)
        self.assertEqual(record["schema"], REGISTRY_SCHEMA)
        self.assertEqual(record["status"], "ready")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["nav_authority"], NAV_AUTHORITY)
        self.assertEqual(record["surfaces"], list(PLATFORM_SURFACES))
        self.assertEqual(record["surfaces"], ["home", "research", "explore", "automation", "operate", "settings"])
        self.assertEqual(record["slots"], registered_slots())
        self.assertEqual(len(record["slots"]), 3)
        self.assertEqual({slot["id"] for slot in record["slots"]}, {"explore.extensions", "automation.extensions", "settings.extensions"})
        self.assertTrue(all(slot["kind"] == "status_card" for slot in record["slots"]))
        self.assertFalse(any(slot["kind"] == "navigation" for slot in REGISTERED_SLOTS))
        self.assertEqual(record["addons"], [])
        self.assertEqual(record["refused"], [])
        self.assertEqual(record["addon_count"], 0)
        self.assertEqual(record["refused_count"], 0)
        self.assertIn("cannot rewrite", record["detail"])

        compact = extensions_status_record(None)
        self.assertEqual(compact["status"], "ready")
        self.assertEqual(compact["nav_authority"], "platform")
        self.assertEqual(compact["slot_count"], 3)
        self.assertEqual(compact["addon_count"], 0)
        self.assertEqual(compact["refused_count"], 0)
        self.assertEqual(compact["registry_schema"], REGISTRY_SCHEMA)

    def test_missing_addons_directory_is_ready_zero_not_unimplemented(self) -> None:
        with TemporaryDirectory() as tmp:
            record = capability_registry_record(Path(tmp))
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["addon_count"], 0)
        self.assertNotIn("unimplemented", json.dumps(record))

    def test_well_formed_operator_addon_binds_explore_slot(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._write_addon(root, "watch-note.json", self._addon())
            record = capability_registry_record(root)
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["addon_count"], 1)
        self.assertEqual(record["refused_count"], 0)
        addon = record["addons"][0]
        self.assertEqual(addon["id"], "operator.watch-note")
        self.assertEqual(addon["producer"], "operator")
        self.assertEqual(addon["slot"], "explore.extensions")
        self.assertIsNone(addon["action_schema"])
        self.assertEqual(addon["presentation"]["title"], "Watch note")

    def test_html_and_script_presentation_fail_closed(self) -> None:
        cases = (
            {"title": "<script>alert(1)</script>", "detail": "plain"},
            {"title": "Watch", "detail": "click <a href='x'>"},
            {"title": "Watch", "detail": "javascript:alert(1)"},
            {"title": "Watch\nnote", "detail": "plain"},
        )
        for presentation in cases:
            with self.subTest(presentation=presentation):
                with self.assertRaises(CapabilityRegistryError) as raised:
                    addon_from_payload(self._addon(presentation=presentation))
                self.assertEqual(raised.exception.code, "addon_presentation_invalid")

    def test_unknown_descriptor_version_fails_closed(self) -> None:
        with self.assertRaises(CapabilityRegistryError) as raised:
            addon_from_payload(self._addon(descriptor_version=2))
        self.assertEqual(raised.exception.code, "addon_descriptor_version_unsupported")

    def test_unregistered_and_navigation_slots_are_refused(self) -> None:
        for slot in ("home.navigation", "research.stages", "explore.nav", "settings.tabs"):
            with self.subTest(slot=slot):
                with self.assertRaises(CapabilityRegistryError) as raised:
                    addon_from_payload(self._addon(slot=slot))
                self.assertEqual(raised.exception.code, "addon_slot_unregistered")
                self.assertIn("navigation", raised.exception.detail)

    def test_extra_keys_native_producer_and_actions_are_refused(self) -> None:
        with self.assertRaises(CapabilityRegistryError) as extra:
            addon_from_payload(self._addon(route="/addons"))
        self.assertEqual(extra.exception.code, "addon_descriptor_invalid")

        with self.assertRaises(CapabilityRegistryError) as producer:
            addon_from_payload(self._addon(producer="native_sqx"))
        self.assertEqual(producer.exception.code, "addon_producer_refused")

        with self.assertRaises(CapabilityRegistryError) as action:
            addon_from_payload(self._addon(action_schema="tc.capability-addon.mutate.v1"))
        self.assertEqual(action.exception.code, "addon_action_refused")

    def test_duplicate_identities_and_symlink_escape_fail_closed_without_binding(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._write_addon(root, "a.json", self._addon())
            self._write_addon(root, "b.json", self._addon())
            outside = Path(tmp) / "outside.json"
            outside.write_text(json.dumps(self._addon(id="operator.escaped")), encoding="utf-8")
            (root / "addons" / "link.json").symlink_to(outside)
            record = capability_registry_record(root)
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["addon_count"], 1)
        self.assertEqual(record["addons"][0]["id"], "operator.watch-note")
        codes = {item["reason_code"] for item in record["refused"]}
        self.assertIn("addon_identity_duplicate", codes)
        self.assertIn("addon_path_escape", codes)

    def test_addons_directory_symlink_unavailables_the_registry(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = Path(tmp) / "elsewhere"
            target.mkdir()
            (root / "addons").symlink_to(target)
            record = capability_registry_record(root)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "addon_store_path_escape")
        self.assertEqual(record["addon_count"], 0)

    def test_runtime_status_reads_the_same_registry_authority(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._write_addon(root, "watch-note.json", self._addon())
            payload = runtime_status_record(None, data_root=root)
        extensions = payload["extensions"]
        self.assertEqual(extensions["status"], "ready")
        self.assertEqual(extensions["addon_count"], 1)
        self.assertEqual(extensions["slot_count"], 3)
        self.assertEqual(extensions["nav_authority"], "platform")
        self.assertNotEqual(extensions["status"], "current")

    def test_http_get_capabilities_is_read_only_and_accepts_no_query(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            store = FileResearchCustodyStore(root)
            self._write_addon(root, "watch-note.json", self._addon())
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, research_store=store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._request_json(base + REGISTRY_API_PATH)
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], REGISTRY_SCHEMA)
                self.assertEqual(payload["nav_authority"], "platform")
                self.assertEqual(payload["addon_count"], 1)
                self.assertEqual(payload["addons"][0]["id"], "operator.watch-note")
                self.assertEqual(payload["surfaces"], list(PLATFORM_SURFACES))

                status, payload = self._request_json(base + "/api/capabilities?slot=explore.extensions")
                self.assertEqual(status, 400)
                self.assertEqual(payload["error"], "invalid_request")

                status, payload = self._request_json(base + REGISTRY_API_PATH, method="POST")
                self.assertEqual(status, 405)
                self.assertEqual(payload["reason_code"], "read_only_baseline")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_capabilities_response_without_store_is_empty_ready(self) -> None:
        status, payload = capabilities_response(None)
        self.assertEqual(status, 200)
        self.assertEqual(payload["status"], "ready")
        self.assertEqual(payload["addon_count"], 0)
        self.assertEqual(payload["slots"][0]["id"], "explore.extensions")
