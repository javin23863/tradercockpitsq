from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from zipfile import ZipFile

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore


class ResearchConfigurationServerTests(unittest.TestCase):
    def _web_root(self, root: Path) -> Path:
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<title>TraderCockpit</title>", encoding="utf-8")
        return web

    def _sqx_root(self, root: Path) -> Path:
        sqx = root / "sqx"
        (sqx / "internal/web/SQUANT").mkdir(parents=True)
        (sqx / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (sqx / "internal/SQUANT.dat").write_bytes(b"144fixture")
        project = sqx / "user/projects/Builder/project.cfx"
        project.parent.mkdir(parents=True)
        with ZipFile(project, "w") as archive:
            archive.writestr(
                "config.xml",
                '<Project><Chart symbol="EURUSD_M1_dukas" timeframe="M30"/><InstrumentInfo instrument="EURUSD_dukascopy"/></Project>',
            )
            archive.writestr(
                "Build-Task1.xml",
                '''<Task>
                  <WhatToBuild><StrategyType type="simple"/><MarketSides type="both"/><BuildMode generationType="random-generation"/></WhatToBuild>
                  <Data><Setups><Setup dateFrom="2020.01.01" dateTo="2024.01.01" testPrecision="2" engine="0" slippage="1" minDist="0"><Chart symbol="EURUSD_M1_dukas" timeframe="M30" spread="2"/><Commissions><Method use="true"/></Commissions></Setup></Setups></Data>
                  <Options><BuildTradingOptions><Option/></BuildTradingOptions></Options>
                  <Blocks><BuildingBlocks/><OrderTypes/><ExitTypes/><CustomData/></Blocks>
                  <RiskMoneyManagement><MoneyManagement><Method type="FixedSize" use="true"/><InitialCapital>10000</InitialCapital></MoneyManagement></RiskMoneyManagement>
                  <Rankings><MaxStrategies>500</MaxStrategies><StopCondition type="passed-count" passedStrategies="10"/></Rankings>
                  <CrossChecks use="true"/>
                  <InstrumentInfo instrument="EURUSD_dukascopy"/>
                </Task>''',
            )
        return sqx

    def _start(self, web: Path, store: FileResearchCustodyStore | None, sqx_home: Path | None):
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, sqx_home, None, store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread, f"http://127.0.0.1:{server.server_port}"

    def _json(
        self,
        url: str,
        *,
        method: str = "GET",
        payload: dict[str, object] | None = None,
        content_type: str = "application/json",
    ) -> tuple[int, dict[str, object]]:
        data = json.dumps(payload).encode("utf-8") if payload is not None else None
        request = Request(
            url,
            data=data,
            method=method,
            headers={"Content-Type": content_type} if data is not None else {},
        )
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def _compile(self, base: str) -> tuple[int, dict[str, object]]:
        return self._json(
            base + "/api/research/configurations",
            method="POST",
            payload={"action": "compile"},
        )

    def test_compile_read_catalog_approve_and_restart_reopen(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            sqx = self._sqx_root(root)
            data_root = root / "data"
            store = FileResearchCustodyStore(data_root)
            server, thread, base = self._start(web, store, sqx)
            try:
                status, compiled = self._compile(base)
                self.assertEqual(status, 201)
                self.assertEqual(compiled["schema"], "tc.research-configuration.v1")
                self.assertEqual(compiled["state"], "compiled")
                self.assertFalse(compiled["launch"]["enabled"])

                status, catalog = self._json(base + "/api/research/configurations")
                self.assertEqual(status, 200)
                self.assertEqual(catalog["schema"], "tc.research-configuration-catalog.v1")
                self.assertEqual(len(catalog["configurations"]), 1)
                self.assertEqual(catalog["configurations"][0]["entity_id"], compiled["entity_id"])

                status, current = self._json(
                    base + "/api/research/configurations?" + urlencode({"entityId": compiled["entity_id"]})
                )
                self.assertEqual(status, 200)
                self.assertEqual(current, compiled)

                status, approved = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={
                        "action": "approve",
                        "entity_id": compiled["entity_id"],
                        "expected_revision": compiled["revision"],
                    },
                )
                self.assertEqual(status, 200)
                self.assertEqual(approved["state"], "approved")
                self.assertEqual(approved["parent_revision"], compiled["revision"])
                self.assertEqual(approved["executable_xml_sha256"], compiled["executable_xml_sha256"])
                self.assertFalse(approved["launch"]["enabled"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

            reopened = FileResearchCustodyStore(data_root)
            server, thread, base = self._start(web, reopened, sqx)
            try:
                status, current = self._json(
                    base + "/api/research/configurations?" + urlencode({"entityId": compiled["entity_id"]})
                )
                self.assertEqual(status, 200)
                self.assertEqual(current["revision"], approved["revision"])
                self.assertEqual(current["state"], "approved")
                self.assertEqual(current["executable_xml_sha256"], approved["executable_xml_sha256"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_stale_revision_and_reapproval_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            sqx = self._sqx_root(root)
            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store, sqx)
            try:
                status, compiled = self._compile(base)
                self.assertEqual(status, 201)
                _, approved = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={
                        "action": "approve",
                        "entity_id": compiled["entity_id"],
                        "expected_revision": compiled["revision"],
                    },
                )
                status, conflict = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={
                        "action": "approve",
                        "entity_id": compiled["entity_id"],
                        "expected_revision": compiled["revision"],
                    },
                )
                self.assertEqual(status, 409)
                self.assertEqual(conflict["reason_code"], "current_conflict")

                status, already = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={
                        "action": "approve",
                        "entity_id": approved["entity_id"],
                        "expected_revision": approved["revision"],
                    },
                )
                self.assertEqual(status, 409)
                self.assertEqual(already["reason_code"], "configuration_already_approved")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_invalid_shapes_selectors_and_non_json_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            sqx = self._sqx_root(root)
            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store, sqx)
            try:
                for path in (
                    "/api/research/configurations?path=C:/other",
                    "/api/research/configurations?entityId=",
                    "/api/research/configurations?entityId=a&entityId=b",
                ):
                    status, response = self._json(base + path)
                    self.assertEqual(status, 400)
                    self.assertEqual(response["error"], "invalid_request")

                cases = (
                    {},
                    {"action": "unknown"},
                    {"action": "compile", "extra": True},
                    {"action": "compile", "changes": "what_to_build.strategy_type=simple"},
                    {"action": "compile", "changes": [1]},
                    {"action": "compile", "changes": ["blocks.rsi=on"]},
                    {"action": "compile", "changes": ["what_to_build.strategy_type=generate"]},
                    {"action": "approve"},
                    {"action": "approve", "entity_id": "x", "expected_revision": "y", "extra": True},
                )
                for payload in cases:
                    with self.subTest(payload=payload):
                        status, response = self._json(
                            base + "/api/research/configurations",
                            method="POST",
                            payload=payload,
                        )
                        self.assertEqual(status, 400)
                        self.assertEqual(response["error"], "invalid_request")

                status, response = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={"action": "compile"},
                    content_type="text/plain",
                )
                self.assertEqual(status, 415)
                self.assertEqual(response["error"], "unsupported_media_type")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_unbound_store_and_missing_producer_are_distinct(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)

            server, thread, base = self._start(web, None, None)
            try:
                status, response = self._json(base + "/api/research/configurations")
                self.assertEqual(status, 503)
                self.assertEqual(response["reason_code"], "research_store_not_bound")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store, None)
            try:
                status, response = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={"action": "compile"},
                )
                self.assertEqual(status, 503)
                self.assertEqual(response["error"], "producer_not_configured")
                self.assertEqual(response["reason_code"], "runtime_not_configured")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_wrong_sqx_build_is_reported_as_invalid_producer_state(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            sqx = self._sqx_root(root)
            (sqx / "internal/web/SQUANT/build.dat").write_text("2952", encoding="utf-8")
            store = FileResearchCustodyStore(root / "data")
            server, thread, base = self._start(web, store, sqx)
            try:
                status, response = self._json(
                    base + "/api/research/configurations",
                    method="POST",
                    payload={"action": "compile"},
                )
                self.assertEqual(status, 409)
                self.assertEqual(response["error"], "invalid_state")
                self.assertEqual(response["reason_code"], "sqx_build_mismatch")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
