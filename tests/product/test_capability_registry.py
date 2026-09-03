from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import capabilities_response, capabilities_stage_response, make_handler
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
    stage_addon,
)
from tradercockpit.native_plugins import load_native_plugin_catalog
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

    def _bundled_count(self) -> int:
        return len(load_native_plugin_catalog())

    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _request_json(
        self,
        url: str,
        *,
        method: str = "GET",
        payload: dict[str, object] | None = None,
    ) -> tuple[int, dict[str, object]]:
        data = json.dumps(payload).encode("utf-8") if payload is not None else (b"" if method == "POST" else None)
        headers = {"Content-Type": "application/json", "Accept": "application/json"} if payload is not None else {}
        request = Request(url, data=data, method=method, headers=headers)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_packaged_native_plugins_are_the_default_catalog(self) -> None:
        record = capability_registry_record(None)
        bundled = self._bundled_count()
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
        self.assertEqual(record["addon_count"], bundled)
        self.assertGreaterEqual(bundled, 7)
        ids = [addon["id"] for addon in record["addons"]]
        self.assertEqual(
            ids,
            [
                "native.sqx-lab",
                "native.custom-block",
                "native.runcompare",
                "native.lucidflex-prop",
                "native.edge-decay",
                "native.two-step-challenge",
                "native.source-translator",
            ],
        )
        runcompare = next(addon for addon in record["addons"] if addon["id"] == "native.runcompare")
        self.assertEqual(runcompare["producer"], "native_sqx")
        self.assertEqual(runcompare["kind"], "results_plugin")
        self.assertEqual(runcompare["presentation"]["title"], "RunCompare")
        self.assertEqual(runcompare["presentation"]["controls"][0]["label"], "Strategy name")
        self.assertFalse(runcompare["runtime"]["installed"])
        self.assertIn("cannot rewrite", record["detail"])
        self.assertIn("StrategyQuant X", record["detail"])

        compact = extensions_status_record(None)
        self.assertEqual(compact["status"], "ready")
        self.assertEqual(compact["nav_authority"], "platform")
        self.assertEqual(compact["slot_count"], 3)
        self.assertEqual(compact["addon_count"], bundled)
        self.assertEqual(compact["refused_count"], 0)
        self.assertEqual(compact["registry_schema"], REGISTRY_SCHEMA)

    def test_missing_addons_directory_keeps_packaged_plugins_not_unimplemented(self) -> None:
        with TemporaryDirectory() as tmp:
            record = capability_registry_record(Path(tmp))
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["addon_count"], self._bundled_count())
        self.assertNotIn("unimplemented", json.dumps(record))

    def test_well_formed_operator_addon_binds_explore_slot_beside_native_plugins(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._write_addon(root, "watch-note.json", self._addon())
            record = capability_registry_record(root)
        self.assertEqual(record["status"], "ready")
        self.assertEqual(record["addon_count"], self._bundled_count() + 1)
        self.assertEqual(record["refused_count"], 0)
        addon = next(item for item in record["addons"] if item["id"] == "operator.watch-note")
        self.assertEqual(addon["producer"], "operator")
        self.assertEqual(addon["kind"], "operator")
        self.assertEqual(addon["slot"], "explore.extensions")
        self.assertIsNone(addon["action_schema"])
        self.assertEqual(addon["presentation"]["title"], "Watch note")
        self.assertFalse(addon["runtime"]["stageable"])

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

    def test_extra_keys_operator_native_claim_and_actions_are_refused(self) -> None:
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
        self.assertEqual(record["addon_count"], self._bundled_count() + 1)
        ids = [item["id"] for item in record["addons"]]
        self.assertIn("operator.watch-note", ids)
        self.assertNotIn("operator.escaped", ids)
        codes = {item["reason_code"] for item in record["refused"]}
        self.assertIn("addon_identity_duplicate", codes)
        self.assertIn("addon_path_escape", codes)

    def test_addons_directory_symlink_unavailables_operator_store_keeps_packaged_plugins(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = Path(tmp) / "elsewhere"
            target.mkdir()
            (root / "addons").symlink_to(target)
            record = capability_registry_record(root)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "addon_store_path_escape")
        self.assertEqual(record["addon_count"], self._bundled_count())

    def test_runtime_status_reads_the_same_registry_authority(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self._write_addon(root, "watch-note.json", self._addon())
            payload = runtime_status_record(None, data_root=root)
        extensions = payload["extensions"]
        self.assertEqual(extensions["status"], "ready")
        self.assertEqual(extensions["addon_count"], self._bundled_count() + 1)
        self.assertEqual(extensions["slot_count"], 3)
        self.assertEqual(extensions["nav_authority"], "platform")
        self.assertNotEqual(extensions["status"], "current")

    def test_http_get_capabilities_lists_packaged_plugins_and_stage_requires_runtime(self) -> None:
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
                self.assertEqual(payload["addon_count"], self._bundled_count() + 1)
                ids = [item["id"] for item in payload["addons"]]
                self.assertIn("native.runcompare", ids)
                self.assertIn("operator.watch-note", ids)
                self.assertEqual(payload["surfaces"], list(PLATFORM_SURFACES))

                status, payload = self._request_json(base + "/api/capabilities?slot=explore.extensions")
                self.assertEqual(status, 400)
                self.assertEqual(payload["error"], "invalid_request")

                status, payload = self._request_json(base + REGISTRY_API_PATH, method="POST")
                self.assertEqual(status, 415)

                status, payload = self._request_json(
                    base + REGISTRY_API_PATH,
                    method="POST",
                    payload={"action": "stage", "id": "native.runcompare"},
                )
                self.assertEqual(status, 503)
                self.assertEqual(payload["reason_code"], "runtime_not_configured")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_http_stage_installs_runcompare_into_verified_runtime(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx")
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, home))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._request_json(
                    base + REGISTRY_API_PATH,
                    method="POST",
                    payload={"action": "stage", "id": "native.runcompare"},
                )
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], "tc.capability-addon-stage.v1")
                self.assertTrue(payload["installed"])
                self.assertTrue((home / "user/extend/ResultsPlugins/RunCompare/index.html").is_file())
                self.assertFalse((home / "start-sq.bat").exists())
                self.assertNotIn(str(home), json.dumps(payload))

                status, payload = self._request_json(base + REGISTRY_API_PATH)
                runcompare = next(item for item in payload["addons"] if item["id"] == "native.runcompare")
                self.assertTrue(runcompare["runtime"]["installed"])
                self.assertFalse(runcompare["runtime"]["stageable"])

                status, payload = self._request_json(
                    base + REGISTRY_API_PATH,
                    method="POST",
                    payload={"action": "stage", "id": "native.sqx-lab"},
                )
                self.assertEqual(status, 409)
                self.assertEqual(payload["reason_code"], "native_plugin_not_stageable")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_capabilities_response_without_store_includes_packaged_plugins(self) -> None:
        status, payload = capabilities_response(None)
        self.assertEqual(status, 200)
        self.assertEqual(payload["status"], "ready")
        self.assertEqual(payload["addon_count"], self._bundled_count())
        self.assertEqual(payload["slots"][0]["id"], "explore.extensions")

    def test_stage_without_runtime_and_unknown_plugin_fail_closed(self) -> None:
        with self.assertRaises(CapabilityRegistryError) as raised:
            stage_addon("native.runcompare", None)
        self.assertEqual(raised.exception.code, "runtime_not_configured")
        status, payload = capabilities_stage_response(None, {"action": "stage", "id": "native.missing"})
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "runtime_not_configured")
        status, payload = capabilities_stage_response(None, {"action": "mutate", "id": "native.runcompare"})
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "capability_action_invalid")
