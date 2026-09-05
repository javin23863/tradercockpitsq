from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.parse import parse_qs, urlsplit
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.desktop_session import (
    DESKTOP_SESSION_API_PATH,
    DESKTOP_SESSION_SCHEMA,
    DesktopSessionError,
    canonicalize_desktop_path,
    read_desktop_session,
    write_desktop_session,
)
from tradercockpit.research_custody import FileResearchCustodyStore


IDEA = "tc-research:idea:v1:cccccccc-cccc-4ccc-8ccc-cccccccccccc"
CONFIGURATION = "tc-research:configuration:v1:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
PROOF = "tc-research:proof:v1:11111111-1111-4111-8111-111111111111"
HISTORICAL_RESULT = "tc-research:historical-result:v1:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
VALIDATION = "tc-evidence:sha256:" + ("ab" * 32)


class DesktopSessionTests(unittest.TestCase):
    def test_workflow_selection_survives_desktop_reopen(self) -> None:
        with TemporaryDirectory() as tmp:
            for surface in ("builder", "custom-projects"):
                path = f"/{surface}?project=Saved+Project&tab=results&task=2&databank=Review&archive=Strategy+1.sqx&resultView=trade-analysis&sample=oos&direction=short&period_by=open_time"
                saved = write_desktop_session(tmp, path)
                self.assertEqual(read_desktop_session(tmp), saved)
                self.assertEqual(saved["path"], path)
            path = "/builder?tab=settings&task=1&section=Blocks&block=Blocks%2FBuildingBlocks%2FBlock%3A1"
            self.assertEqual(write_desktop_session(tmp, path)["path"], path)

    def test_workflow_session_rejects_unregistered_or_unsafe_selection(self) -> None:
        for query in ("task=0", "task=-1", "task=1&task=2", "tab=execute", "sample=unknown", "direction=buy", "period_by=now", "archive=../a.sqx", "archive=a.exe", "project=C%3A", "databank=a%00b", "token=secret", "resultView=unknown", "section=%3Cscript%3E", "block=Blocks/../Block:1", "block=Blocks//Block:1", "block=Blocks/Block:0"):
            with self.subTest(query=query), self.assertRaises(DesktopSessionError):
                canonicalize_desktop_path("/builder?" + query)

    def test_canonicalize_keeps_registered_research_identity(self) -> None:
        path = canonicalize_desktop_path(
            f"/research?workspace=validate&tab=trades&configuration={CONFIGURATION}"
            f"&proofEntity={PROOF}&validationRef={VALIDATION}"
        )
        params = parse_qs(urlsplit(path).query)
        self.assertEqual(urlsplit(path).path, "/research")
        self.assertEqual(params["workspace"], ["validate"])
        self.assertEqual(params["tab"], ["trades"])
        self.assertEqual(params["configuration"], [CONFIGURATION])
        self.assertEqual(params["proofEntity"], [PROOF])
        self.assertEqual(params["validationRef"], [VALIDATION])
        overlay = canonicalize_desktop_path(
            f"/research?workspace=signals&tab=signals&historicalResult={HISTORICAL_RESULT}"
        )
        self.assertEqual(parse_qs(urlsplit(overlay).query)["historicalResult"], [HISTORICAL_RESULT])
        idea = canonicalize_desktop_path(
            f"/research?workspace=signals&tab=overview&idea={IDEA}"
        )
        self.assertEqual(parse_qs(urlsplit(idea).query)["idea"], [IDEA])
        self.assertEqual(canonicalize_desktop_path("/research?workspace=evolution"), "/research?workspace=evolution")
        self.assertEqual(canonicalize_desktop_path("/home"), "/home")
        self.assertEqual(canonicalize_desktop_path("/apollo"), "/apollo")
        self.assertEqual(canonicalize_desktop_path("/algowizard"), "/apollo")

    def test_canonicalize_refuses_unknown_or_malformed_routes(self) -> None:
        cases = (
            "home",
            "/unknown",
            "/home?workspace=signals",
            "/research?workspace=nonsense",
            "/research?workspace=evolution&tab=overview",
            "/research?workspace=validate&tab=made-up",
            "/research?evil=1",
            f"/research?workspace=evolution&configuration={PROOF}",
            "/research?workspace=evolution&configuration=not-an-id",
            f"/research?workspace=signals&tab=overview&idea={CONFIGURATION}",
            "/research?workspace=signals&tab=overview&idea=not-an-id",
            f"/research?workspace=signals&tab=signals&historicalResult={PROOF}",
            "/research?workspace=signals&tab=signals&historicalResult=not-an-id",
            "https://example.invalid/home",
        )
        for value in cases:
            with self.subTest(value=value):
                with self.assertRaises(DesktopSessionError):
                    canonicalize_desktop_path(value)

    def test_corrupt_or_missing_session_falls_back_to_home(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertEqual(read_desktop_session(root)["path"], "/home")
            (root / "desktop-session.json").write_text("{not json", encoding="utf-8")
            self.assertEqual(read_desktop_session(root)["path"], "/home")
            (root / "desktop-session.json").write_text(
                '{"schema":"tc.desktop-session.v1","path":"/etc/passwd"}',
                encoding="utf-8",
            )
            self.assertEqual(read_desktop_session(root)["path"], "/home")

    def test_write_round_trips_registered_path(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            saved = write_desktop_session(root, f"/research?workspace=evolution&configuration={CONFIGURATION}")
            self.assertEqual(saved["schema"], DESKTOP_SESSION_SCHEMA)
            self.assertEqual(read_desktop_session(root)["path"], saved["path"])
            self.assertEqual(parse_qs(urlsplit(saved["path"]).query)["configuration"], [CONFIGURATION])

    def test_http_session_write_is_what_the_next_read_returns(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            web = root / "web"
            web.mkdir()
            (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
            store = FileResearchCustodyStore(root / "data")
            server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                request = Request(
                    base + DESKTOP_SESSION_API_PATH,
                    data=json.dumps({"path": f"/research?workspace=evolution&configuration={CONFIGURATION}"}).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                with urlopen(request, timeout=2) as response:
                    written = json.loads(response.read().decode("utf-8"))
                self.assertEqual(written["schema"], DESKTOP_SESSION_SCHEMA)
                with urlopen(base + DESKTOP_SESSION_API_PATH, timeout=2) as response:
                    read = json.loads(response.read().decode("utf-8"))
                self.assertEqual(read["path"], written["path"])
            finally:
                server.shutdown()
                server.server_close()
                thread.join()
