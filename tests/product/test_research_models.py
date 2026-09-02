from __future__ import annotations

from hashlib import sha256
from http.server import ThreadingHTTPServer
from io import BytesIO
import json
from pathlib import Path
import struct
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from zipfile import ZipFile

from tradercockpit.app_server import make_handler
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_models import (
    RESEARCH_MODELS_SCHEMA,
    fit_model,
    models_catalog,
    sklearn_available,
)


HISTORICAL_ENTITY = "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333"
HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'3' * 64}"
CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"


def _utf(value: str) -> bytes:
    encoded = value.encode("ascii")
    return struct.pack(">H", len(encoded)) + encoded


def _order(ticket: int, *, pl: float, mae: float, mfe: float, pips_pl: float) -> bytes:
    parts = [
        bytes((1, 2, 3, 4)),
        struct.pack(">ii", ticket, ticket),
        struct.pack(">bbb", 1, 0, 1),
        struct.pack(">q", 1_700_000_000_000),
        struct.pack(">b", 1),
        struct.pack(">ff", 1.0, 1.1000),
        struct.pack(">qf", 1_700_000_000_000, 1.1000),
        struct.pack(">qf", 1_700_003_600_000, 1.1100),
        struct.pack(">ffh", 1.0900, 1.1200, 12),
        struct.pack(">ffffffff", pl, 1.0, 1.0, pips_pl, -25.0, -0.25, -2.5, -1.5),
        struct.pack(">B", 1),
        struct.pack(">ffff", mae, -3.0, mfe, 12.0),
        struct.pack(">i", 123456),
        struct.pack(">fff", 10_100.0, 1.01, 1_010.0),
        struct.pack(">i", 42),
        struct.pack(">bffbf", 1, 0.0, 0.25, 2, 0.0015),
    ]
    return b"".join(parts)


def _orders_bin(*orders: bytes) -> bytes:
    primitive = bytearray()
    primitive += _utf("SQOrderFileFormat:11")
    for _ in range(7):
        primitive += struct.pack(">i", 0 if _ < 6 else 1)
    strings = ("EURUSD", "Setup A", "Strategy A", "producer comment")
    primitive += struct.pack(">i", len(strings))
    for value in strings:
        primitive += _utf(value)
    for order in orders:
        primitive += order
    return b"\xac\xed\x00\x05\x7a" + struct.pack(">I", len(primitive)) + bytes(primitive)


def _archive(orders_bin: bytes) -> bytes:
    buffer = BytesIO()
    with ZipFile(buffer, "w") as zip_file:
        zip_file.writestr("version.txt", b"144.2953")
        zip_file.writestr("orders.bin", orders_bin)
    return buffer.getvalue()


def _result(archive_ref: str, archive_sha: str) -> dict[str, object]:
    return {
        "entity_id": HISTORICAL_ENTITY,
        "revision": HISTORICAL_REVISION,
        "state": "completed",
        "execution_completed": True,
        "candidate_entity_id": CANDIDATE_ENTITY,
        "candidate_revision": CANDIDATE_REVISION,
        "result_archive_ref": archive_ref,
        "result_archive_sha256": archive_sha,
    }


class ResearchModelsTests(unittest.TestCase):
    def test_catalog_is_truthful_when_sklearn_is_missing(self) -> None:
        with patch("tradercockpit.research_models.sklearn_available", return_value=False):
            payload = models_catalog(None)
        self.assertEqual(payload["schema"], RESEARCH_MODELS_SCHEMA)
        self.assertFalse(payload["backend_available"])
        self.assertEqual(payload["reason_code"], "ml_backend_not_installed")
        self.assertEqual(len(payload["families"]), 3)

    def test_fit_refuses_unknown_family_and_extra_path_fields(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            from tradercockpit.research_models import models_write

            status, payload = models_write(
                store,
                {
                    "action": "fit",
                    "family_id": "sklearn.tree.DecisionTreeClassifier",
                    "historical_result_entity_id": HISTORICAL_ENTITY,
                    "expected_historical_result_revision": HISTORICAL_REVISION,
                    "path": "C:/outside/model.pkl",
                },
            )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "ml_fit_identity_invalid")

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_fit_writes_artifact_digest_from_native_trades(self) -> None:
        snapshot = _archive(
            _orders_bin(
                _order(1, pl=80.0, mae=-10.0, mfe=40.0, pips_pl=8.0),
                _order(2, pl=-40.0, mae=-30.0, mfe=5.0, pips_pl=-4.0),
                _order(3, pl=20.0, mae=-8.0, mfe=15.0, pips_pl=2.0),
                _order(4, pl=-10.0, mae=-12.0, mfe=3.0, pips_pl=-1.0),
            )
        )
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = _result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                payload = fit_model(
                    store,
                    family_id="sklearn.tree.DecisionTreeClassifier",
                    historical_result_entity_id=HISTORICAL_ENTITY,
                    expected_historical_result_revision=HISTORICAL_REVISION,
                )
            model = payload["models"][0]
            artifact = Path(tmp) / "ml-models" / f"{model['artifact_sha256']}.joblib"
            self.assertTrue(payload["backend_available"])
            self.assertEqual(model["trade_count"], 4)
            self.assertEqual(model["label_rule"], "producer_pl_positive")
            self.assertEqual(model["historical_result_revision"], HISTORICAL_REVISION)
            self.assertTrue(artifact.is_file())
            self.assertEqual(sha256(artifact.read_bytes()).hexdigest(), model["artifact_sha256"])


class ResearchModelsHttpTests(unittest.TestCase):
    def _server(self, root: Path):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        store = FileResearchCustodyStore(root / "data")
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        return server, thread

    def _json(self, url: str, *, method: str = "GET", payload: dict[str, object] | None = None):
        headers = {"Accept": "application/json"}
        data = None
        if payload is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(payload).encode("utf-8")
        request = Request(url, data=data, method=method, headers=headers)
        try:
            with urlopen(request, timeout=2) as response:
                return response.status, json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            return exc.code, json.loads(exc.read().decode("utf-8"))

    def test_http_catalog_and_query_fail_closed(self) -> None:
        with TemporaryDirectory() as tmp:
            server, thread = self._server(Path(tmp))
            base = f"http://127.0.0.1:{server.server_port}"
            try:
                status, payload = self._json(base + "/api/research/models")
                self.assertEqual(status, 200)
                self.assertEqual(payload["schema"], RESEARCH_MODELS_SCHEMA)
                status, payload = self._json(base + "/api/research/models?family=tree")
                self.assertEqual(status, 400)
                status, payload = self._json(
                    base + "/api/research/models",
                    method="POST",
                    payload={"action": "fit", "family_id": "nope", "historical_result_entity_id": HISTORICAL_ENTITY, "expected_historical_result_revision": HISTORICAL_REVISION},
                )
                self.assertEqual(status, 409)
                self.assertIn(payload["reason_code"], {"ml_family_unknown", "ml_backend_not_installed"})
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
