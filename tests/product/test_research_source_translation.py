from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from urllib.parse import parse_qs
from urllib.request import Request, urlopen
from zipfile import ZipFile

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_source_translation import (
    DELIVERY_TARGETS,
    RESEARCH_SOURCE_TRANSLATION_API_PATH,
    RESEARCH_SOURCE_TRANSLATION_CATALOG_SCHEMA,
    RESEARCH_SOURCE_TRANSLATION_SCHEMA,
    ResearchSourceTranslationError,
    source_translation_catalog,
    source_translation_read_response,
    source_translation_write_response,
    translate_native_source,
)


PSEUDO = "LongEntryCondition = (CCI(14) > 0)\nStop Loss = 100 pips;\n"
PINE = "//@version=6\n// translated from StrategyQuant X native source, unverified\nstrategy('x')\n"
ENV_OK = {"OPENROUTER_API_KEY": "test-key", "TRADERCOCKPIT_ASSISTANT_MODEL": "z-ai/glm-5.3-flash"}


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    bank = root / "user/projects/Example/databanks/Results"
    bank.mkdir(parents=True)
    with ZipFile(bank / "Native.sqx", "w") as handle:
        handle.writestr("strategy_Portfolio.xml", "<StrategyFile><Strategy name='x'/></StrategyFile>")
        handle.writestr("settings.xml", "<Settings/>")
    (root / "user/settings").mkdir(parents=True)
    return root


def _bind_sqx_web(home: Path, port: int) -> None:
    (home / "user/settings/settings.xml").write_text(
        f"<Settings><WebServerPortUsed>{port}</WebServerPortUsed><BrowserToken>tok</BrowserToken></Settings>",
        encoding="utf-8",
    )


class _SqxWebHandler(BaseHTTPRequestHandler):
    def log_message(self, *_args) -> None:  # noqa: D401 - silence
        return

    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length") or 0)
        form = parse_qs(self.rfile.read(length).decode("utf-8"))
        if self.path != "/sourcecode/print" or self.headers.get("browserToken") != "tok":
            self.send_response(404)
            self.end_headers()
            return
        code_type = form.get("type", [""])[0]
        body = json.dumps({"code": PSEUDO if code_type.startswith("Pseudo") else form.get("strategyXML", [""])[0], "success": "ok"})
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(body.encode("utf-8"))


def _completion(model: str, content: str) -> bytes:
    return json.dumps({
        "id": "gen-1",
        "model": model,
        "choices": [{"message": {"role": "assistant", "content": content}}],
        "usage": {"prompt_tokens": 100, "completion_tokens": 50, "total_tokens": 150},
    }).encode("utf-8")


class _Transport:
    def __init__(self, content: str = PINE) -> None:
        self.requests: list[dict[str, object]] = []
        self.content = content

    def __call__(self, url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
        request = json.loads(body.decode("utf-8"))
        self.requests.append(request)
        return 200, _completion(str(request["model"]), self.content)


class SourceTranslationTests(unittest.TestCase):
    def _with_sqx_web(self):
        server = ThreadingHTTPServer(("127.0.0.1", 0), _SqxWebHandler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread

    def test_translation_is_bound_to_native_source_and_stored_unverified(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _runtime(root / "sqx")
            data_root = root / "data"
            server, thread = self._with_sqx_web()
            try:
                _bind_sqx_web(home, server.server_port)
                transport = _Transport("```pine\n" + PINE.rstrip() + "\n```")
                record = translate_native_source(
                    home,
                    data_root,
                    {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "pine_v6"},
                    environ=ENV_OK,
                    transport=transport,
                )
            finally:
                server.shutdown()
                server.server_close()
                thread.join()
            self.assertEqual(record["schema"], RESEARCH_SOURCE_TRANSLATION_SCHEMA)
            self.assertEqual(record["status"], "unverified_translation")
            self.assertEqual(record["verification"]["state"], "not_verified")
            self.assertEqual(record["target"]["id"], "pine_v6")
            self.assertEqual(record["native"]["format"], "pseudo")
            self.assertEqual(record["native"]["producer"], "sqx_local_web")
            self.assertEqual(record["native"]["strategy_name"], "Native")
            from hashlib import sha256
            self.assertEqual(record["native"]["pseudo_sha256"], sha256(PSEUDO.encode("utf-8")).hexdigest())
            self.assertEqual(record["code"], PINE.rstrip())
            self.assertEqual(record["code_sha256"], sha256(PINE.rstrip().encode("utf-8")).hexdigest())
            self.assertEqual(record["model"]["requested"], "z-ai/glm-5.3-flash")
            self.assertFalse(record["model"]["fallback_used"])
            request = transport.requests[0]
            self.assertNotIn("tools", request)
            self.assertEqual(request["temperature"], 0.0)
            self.assertIn(PSEUDO, request["messages"][1]["content"])
            self.assertIn("<StrategyFile>", request["messages"][1]["content"])
            self.assertIn("TC-UNTRANSLATABLE", request["messages"][0]["content"])
            stored = list((data_root / "research/source-translations").glob("*.json"))
            self.assertEqual(len(stored), 1)
            self.assertNotIn("test-key", stored[0].read_text(encoding="utf-8"))
            catalog = source_translation_catalog(data_root, project="Example", databank="Results", archive="Native.sqx", environ=ENV_OK)
            self.assertEqual(catalog["schema"], RESEARCH_SOURCE_TRANSLATION_CATALOG_SCHEMA)
            self.assertEqual([item["id"] for item in catalog["translation_targets"]], [item["id"] for item in DELIVERY_TARGETS])
            self.assertEqual([item["id"] for item in catalog["native_targets"]], ["mq4", "mq5"])
            self.assertEqual(catalog["translations"][0]["id"], record["id"])
            other = source_translation_catalog(data_root, project="Other", environ=ENV_OK)
            self.assertEqual(other["translations"], [])

    def test_translation_fails_closed_without_running_sqx_web(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _runtime(root / "sqx")
            _bind_sqx_web(home, 1)
            with self.assertRaises(ResearchSourceTranslationError) as raised:
                translate_native_source(
                    home,
                    root / "data",
                    {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "pine_v6"},
                    environ=ENV_OK,
                    transport=_Transport(),
                )
            self.assertEqual(raised.exception.code, "source_translation_native_unavailable")
            self.assertEqual(raised.exception.status, 503)
            self.assertFalse((root / "data/research/source-translations").exists())

    def test_translation_rejects_unknown_target_missing_provider_and_unbound_root(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = _runtime(root / "sqx")
            server, thread = self._with_sqx_web()
            try:
                _bind_sqx_web(home, server.server_port)
                with self.assertRaises(ResearchSourceTranslationError) as target:
                    translate_native_source(home, root / "data", {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "ninja"}, environ=ENV_OK, transport=_Transport())
                self.assertEqual(target.exception.code, "source_translation_target_invalid")
                with self.assertRaises(ResearchSourceTranslationError) as fields:
                    translate_native_source(home, root / "data", {"project": "Example", "target": "pine_v6"}, environ=ENV_OK, transport=_Transport())
                self.assertEqual(fields.exception.code, "source_translation_fields_invalid")
                with self.assertRaises(ResearchSourceTranslationError) as provider:
                    translate_native_source(home, root / "data", {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "python_backtrader"}, environ={}, transport=_Transport())
                self.assertEqual(provider.exception.code, "provider_not_configured")
                with self.assertRaises(ResearchSourceTranslationError) as unbound:
                    translate_native_source(home, None, {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "pine_v6"}, environ=ENV_OK, transport=_Transport())
                self.assertEqual(unbound.exception.code, "research_store_not_bound")
            finally:
                server.shutdown()
                server.server_close()
                thread.join()
            self.assertFalse((root / "data/research/source-translations").exists())

    def test_http_responses_map_errors_and_reject_bad_queries(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            status, payload = source_translation_read_response(root, {"project": ["Example"], "other": ["x"]}, environ=ENV_OK)
            self.assertEqual(status, 400)
            status, payload = source_translation_read_response(root, {"project": ["../x"]}, environ=ENV_OK)
            self.assertEqual(status, 400)
            self.assertEqual(payload["reason_code"], "source_translation_identity_invalid")
            status, payload = source_translation_read_response(root, {}, environ={})
            self.assertEqual(status, 200)
            self.assertFalse(payload["assistant"]["configured"])
            self.assertEqual(payload["translations"], [])
            status, payload = source_translation_write_response(None, root, ["x"], environ=ENV_OK)
            self.assertEqual(status, 400)
            status, payload = source_translation_write_response(None, None, {"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "pine_v6"}, environ=ENV_OK)
            self.assertEqual((status, payload["error"], payload["reason_code"]), (503, "unavailable", "research_store_not_bound"))


class SourceTranslationServerTests(unittest.TestCase):
    def _request(self, url: str, *, method: str = "GET", body: dict[str, object] | None = None, origin: str | None = None):
        data = json.dumps(body).encode("utf-8") if body is not None else (b"" if method == "POST" else None)
        headers = {"Content-Type": "application/json"} if body is not None else {}
        if origin:
            headers["Origin"] = origin
        request = Request(url, data=data, method=method, headers=headers)
        try:
            with urlopen(request, timeout=3) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_routes_are_loopback_only_and_fail_closed_without_sqx_web(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main/>", encoding="utf-8")
            home = _runtime(root / "sqx")
            _bind_sqx_web(home, 1)
            store = FileResearchCustodyStore(root / "data")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, home, research_store=store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._request(base + RESEARCH_SOURCE_TRANSLATION_API_PATH + "?project=Example&databank=Results&archive=Native.sqx")
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], RESEARCH_SOURCE_TRANSLATION_CATALOG_SCHEMA)
                self.assertTrue(payload["data_root_bound"])
                self.assertEqual(payload["translations"], [])
                status, payload = self._request(base + RESEARCH_SOURCE_TRANSLATION_API_PATH + "?bogus=1")
                self.assertEqual(status, 400)
                status, payload = self._request(
                    base + RESEARCH_SOURCE_TRANSLATION_API_PATH,
                    method="POST",
                    body={"project": "Example", "databank": "Results", "archive": "Native.sqx", "target": "pine_v6"},
                )
                self.assertEqual(status, 503)
                self.assertEqual(payload["error"], "unavailable")
                self.assertIn(payload["reason_code"], {"source_translation_native_unavailable", "provider_not_configured"})
                self.assertFalse((root / "data/research/source-translations").exists())
                status, payload = self._request(base + RESEARCH_SOURCE_TRANSLATION_API_PATH + "?x=1", method="POST", body={})
                self.assertEqual(status, 400)
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
