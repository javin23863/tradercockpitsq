from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from zipfile import ZipFile

from tradercockpit.app_server import make_handler, sqx_preset_response, status_response


class AppServerTests(unittest.TestCase):
    def _web_root(self, root: Path) -> Path:
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        return web

    def _runtime(self, root: Path) -> Path:
        (root / "internal/web/SQUANT").mkdir(parents=True)
        (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
        (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
        return root

    def _request_json(self, url: str, *, method: str = "GET") -> tuple[int, dict[str, object]]:
        request = Request(url, data=b"" if method == "POST" else None, method=method)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_status_response_keeps_application_ready_when_research_backend_is_unconfigured(self) -> None:
        status, payload = status_response(None)
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.runtime-status.v1")
        self.assertEqual(payload["application"]["status"], "ready")
        self.assertEqual(payload["research_backend"]["status"], "unavailable")
        self.assertEqual(payload["research_backend"]["reason_code"], "runtime_not_configured")
        self.assertFalse(payload["research_backend"]["execution"]["available"])

    def test_preset_catalog_is_read_only_when_runtime_is_unconfigured(self) -> None:
        status, payload = sqx_preset_response(None)
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.sqx-preset-catalog.v1")
        self.assertFalse(payload["execution_available"])
        self.assertEqual(payload["execution_reason"], "trusted_native_gateway_not_implemented")
        self.assertEqual(len(payload["presets"]), 3)

    def test_unknown_preset_is_not_found(self) -> None:
        status, payload = sqx_preset_response(None, "does-not-exist")
        self.assertEqual(status, 404)
        self.assertEqual(payload["error"], "not_found")

    def test_http_boundary_serves_canonical_status_and_current_spa_routes(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                for route in ("/home", "/research", "/explore"):
                    with urlopen(base + route, timeout=2) as response:
                        self.assertIn("TraderCockpit", response.read().decode("utf-8"))

                status, payload = self._request_json(base + "/api/status")
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], "tc.runtime-status.v1")
                self.assertEqual(payload["application"]["status"], "ready")
                self.assertEqual(payload["research_backend"]["status"], "unavailable")

                status, payload = self._request_json(base + "/api/sqx-presets")
                self.assertEqual(status, 200)
                self.assertFalse(payload["execution_available"])

                status, payload = self._request_json(base + "/api/sqx-outputs")
                self.assertEqual(status, 200)
                self.assertFalse(payload["import_available"])

                status, payload = self._request_json(base + "/api/market/quotes")
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], "tc.market-quotes.v1")
                self.assertEqual(payload["status"], "unavailable")
                self.assertEqual(payload["reason_code"], "provider_not_configured")
                self.assertEqual(payload["quotes"], [])

                status, payload = self._request_json(base + "/api/market/quotes?symbol=ES")
                self.assertEqual(status, 400)
                self.assertEqual(payload["error"], "invalid_request")

                status, payload = self._request_json(base + "/api/sqx-presets/foo/launch", method="POST")
                self.assertEqual(status, 405)
                self.assertEqual(payload["reason_code"], "read_only_baseline")

                status, payload = self._request_json(base + "/api/status", method="POST")
                self.assertEqual(status, 405)
                self.assertEqual(payload["reason_code"], "read_only_baseline")

                status, payload = self._request_json(base + "/api/unknown")
                self.assertEqual(status, 404)
                self.assertEqual(payload["error"], "not_found")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_verified_runtime_status_exposes_build_without_path(self) -> None:
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as web_tmp:
            home = self._runtime(Path(runtime_tmp))
            web = self._web_root(Path(web_tmp))
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, home))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._request_json(base + "/api/status")
                self.assertEqual(status, 200)
                research = payload["research_backend"]
                self.assertEqual(research["status"], "ready")
                self.assertEqual(research["build"], "144.2953")
                self.assertFalse(research["execution"]["available"])
                self.assertNotIn(str(home), json.dumps(payload))
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_api_query_shapes_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = self._web_root(root)
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                cases = (
                    "/api/status?refresh=true",
                    "/api/desktop/session?refresh=true",
                    "/api/native-runtime?refresh=true",
                    "/api/sqx-presets?other=value",
                    "/api/sqx-presets?presetId=a&presetId=b",
                    "/api/sqx-outputs?archive=x",
                    "/api/sqx-builder-config?project=Builder",
                    "/api/sqx-project-topology",
                    "/api/sqx-project-topology?project=",
                    "/api/sqx-project-topology?project=A&project=B",
                )
                for path in cases:
                    with self.subTest(path=path):
                        status, payload = self._request_json(base + path)
                        self.assertEqual(status, 400)
                        self.assertEqual(payload["error"], "invalid_request")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_builder_config_and_project_topology_are_real_read_only_native_reads(self) -> None:
        with TemporaryDirectory() as runtime_tmp, TemporaryDirectory() as web_tmp:
            home = self._runtime(Path(runtime_tmp))
            builder = home / "user/projects/Builder/project.cfx"
            builder.parent.mkdir(parents=True)
            with ZipFile(builder, "w") as archive:
                archive.writestr(
                    "config.xml",
                    '<Project><Chart symbol="ES" timeframe="M30"/><InstrumentInfo instrument="ES"/></Project>',
                )
                archive.writestr(
                    "Build-Task1.xml",
                    '<Task><Chart symbol="ES" timeframe="M30"/><InstrumentInfo instrument="ES"/></Task>',
                )
            project = home / "user/projects/Example/project.cfx"
            project.parent.mkdir(parents=True)
            with ZipFile(project, "w") as archive:
                archive.writestr("config.xml", "<Settings/>")

            web = self._web_root(Path(web_tmp))
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, home))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._request_json(base + "/api/sqx-builder-config")
                self.assertEqual(status, 200)
                self.assertEqual(payload["project"], "Builder")
                self.assertFalse(payload["execution"]["available"])

                status, payload = self._request_json(base + "/api/sqx-project-topology?project=Example")
                self.assertEqual(status, 200)
                self.assertEqual(payload["project"], "Example")
                self.assertFalse(payload["execution"]["supported"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
