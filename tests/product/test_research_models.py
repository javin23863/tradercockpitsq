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
    FAMILIES,
    RESEARCH_MODELS_SCHEMA,
    fit_model,
    models_catalog,
    sklearn_available,
)


HISTORICAL_ENTITY = "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333"
HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'3' * 64}"
CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"
LOGISTIC_FAMILY = "sklearn.linear_model.LogisticRegression"
TREE_FAMILY = "sklearn.tree.DecisionTreeClassifier"
BOOST_FAMILY = "sklearn.ensemble.GradientBoostingClassifier"


def _utf(value: str) -> bytes:
    encoded = value.encode("ascii")
    return struct.pack(">H", len(encoded)) + encoded


def _order(
    ticket: int,
    *,
    pl: float,
    mae: float,
    mfe: float,
    pips_pl: float,
    open_ms: int = 1_700_000_000_000,
    close_ms: int = 1_700_003_600_000,
) -> bytes:
    parts = [
        bytes((1, 2, 3, 4)),
        struct.pack(">ii", ticket, ticket),
        struct.pack(">bbb", 1, 0, 1),
        struct.pack(">q", open_ms),
        struct.pack(">b", 1),
        struct.pack(">ff", 1.0, 1.1000),
        struct.pack(">qf", open_ms, 1.1000),
        struct.pack(">qf", close_ms, 1.1100),
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
        zip_file.writestr("version.txt", b"1")
        zip_file.writestr("strategy_Portfolio.xml", b'<StrategyFile AppVersion="SQX Build 144.2953"><Strategy/></StrategyFile>')
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


def _staggered_orders(*, count: int, pl_for) -> tuple[bytes, ...]:
    hour = 3_600_000
    orders = []
    for index in range(count):
        open_ms = 1_700_000_000_000 + index * 2 * hour
        orders.append(
            _order(
                index + 1,
                pl=pl_for(index),
                mae=-10.0 - index,
                mfe=20.0 + index,
                pips_pl=2.0 if pl_for(index) > 0 else -1.5,
                open_ms=open_ms,
                close_ms=open_ms + hour,
            )
        )
    return tuple(orders)


def _mixed_pl(index: int) -> float:
    return 80.0 - (index * 15.0) if index % 2 == 0 else -20.0 - index


class ResearchModelsTests(unittest.TestCase):
    def test_catalog_is_truthful_when_sklearn_is_missing(self) -> None:
        with patch("tradercockpit.research_models.sklearn_available", return_value=False):
            payload = models_catalog(None)
        self.assertEqual(payload["schema"], RESEARCH_MODELS_SCHEMA)
        self.assertEqual(payload["scope"], "historical_research")
        self.assertFalse(payload["backend_available"])
        self.assertEqual(payload["reason_code"], "ml_backend_not_installed")
        self.assertEqual(len(payload["families"]), 4)
        self.assertEqual(payload["families"][0]["family_id"], LOGISTIC_FAMILY)
        self.assertTrue(all(item["enabled"] is True for item in payload["families"]))
        self.assertTrue(all("estimator" not in item for item in payload["families"]))

    def test_allowlist_includes_logistic_regression(self) -> None:
        family_ids = [item["family_id"] for item in FAMILIES]
        self.assertEqual(
            family_ids,
            [
                LOGISTIC_FAMILY,
                TREE_FAMILY,
                "sklearn.ensemble.RandomForestClassifier",
                BOOST_FAMILY,
            ],
        )

    def test_fit_refuses_unknown_family_and_extra_path_fields(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            from tradercockpit.research_models import models_write

            status, payload = models_write(
                store,
                {
                    "action": "fit",
                    "family_id": TREE_FAMILY,
                    "historical_result_entity_id": HISTORICAL_ENTITY,
                    "expected_historical_result_revision": HISTORICAL_REVISION,
                    "path": "C:/outside/model.pkl",
                },
            )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "ml_fit_identity_invalid")

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_fit_refuses_fewer_than_eight_trades_for_oos(self) -> None:
        snapshot = _archive(
            _orders_bin(
                *_staggered_orders(count=4, pl_for=_mixed_pl),
            )
        )
        from tradercockpit.research_models import ResearchModelsError

        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = _result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                with self.assertRaises(ResearchModelsError) as caught:
                    fit_model(
                        store,
                        family_id=TREE_FAMILY,
                        historical_result_entity_id=HISTORICAL_ENTITY,
                        expected_historical_result_revision=HISTORICAL_REVISION,
                    )
        self.assertEqual(caught.exception.code, "ml_trades_insufficient_for_oos")

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_fit_writes_artifact_digest_and_mandatory_metrics(self) -> None:
        snapshot = _archive(_orders_bin(*_staggered_orders(count=8, pl_for=_mixed_pl)))
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = _result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                payload = fit_model(
                    store,
                    family_id=TREE_FAMILY,
                    historical_result_entity_id=HISTORICAL_ENTITY,
                    expected_historical_result_revision=HISTORICAL_REVISION,
                )
            model = payload["models"][0]
            artifact = Path(tmp) / "ml-models" / f"{model['artifact_sha256']}.joblib"
            self.assertTrue(payload["backend_available"])
            self.assertEqual(model["trade_count"], 8)
            self.assertEqual(model["label_rule"], "producer_pl_positive")
            self.assertEqual(model["scope"], "historical_explanatory")
            self.assertEqual(model["historical_result_revision"], HISTORICAL_REVISION)
            self.assertIsInstance(model["train_accuracy"], float)
            self.assertIsInstance(model["oos_accuracy"], float)
            self.assertIn("cv", model)
            self.assertEqual(model["cv"]["n_splits"], 2)
            self.assertIn("embargo", model["cv"])
            self.assertEqual(model["expected_value"]["status"], "available")
            self.assertEqual(model["expected_value"]["n"], 8)
            self.assertTrue(model["expected_value"]["identity_ok"])
            self.assertIn(model["sharpe"]["status"], {"available", "unavailable"})
            self.assertIn("sharpe", model["sharpe"])
            self.assertEqual(model["selection"]["trial_index"], 1)
            self.assertEqual(model["selection"]["trial_count_on_result"], 1)
            self.assertIn(model["selection"]["deflated_sharpe_status"], {"computed", "selection_count_unknown"})
            self.assertTrue(artifact.is_file())
            self.assertEqual(sha256(artifact.read_bytes()).hexdigest(), model["artifact_sha256"])
            catalog = json.loads((Path(tmp) / "ml-models.json").read_text(encoding="utf-8"))
            self.assertEqual(catalog["schema"], RESEARCH_MODELS_SCHEMA)

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_logistic_regression_fits_on_native_trades(self) -> None:
        snapshot = _archive(_orders_bin(*_staggered_orders(count=8, pl_for=_mixed_pl)))
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = _result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                payload = fit_model(
                    store,
                    family_id=LOGISTIC_FAMILY,
                    historical_result_entity_id=HISTORICAL_ENTITY,
                    expected_historical_result_revision=HISTORICAL_REVISION,
                )
            model = payload["models"][0]
            self.assertEqual(model["family_id"], LOGISTIC_FAMILY)
            self.assertIsInstance(model["oos_accuracy"], float)
            self.assertGreaterEqual(model["oos_accuracy"], 0.0)
            self.assertLessEqual(model["oos_accuracy"], 1.0)

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_fit_missing_historical_result_fails_closed(self) -> None:
        from tradercockpit.research_models import models_write

        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            status, payload = models_write(
                store,
                {
                    "action": "fit",
                    "family_id": TREE_FAMILY,
                    "historical_result_entity_id": HISTORICAL_ENTITY,
                    "expected_historical_result_revision": HISTORICAL_REVISION,
                },
            )
        self.assertEqual(status, 409)
        self.assertEqual(payload["error"], "invalid_state")
        self.assertEqual(payload["reason_code"], "current_pointer_missing")

    @unittest.skipUnless(sklearn_available(), "scikit-learn extra is not installed")
    def test_fit_single_class_trades_fail_closed(self) -> None:
        snapshot = _archive(_orders_bin(*_staggered_orders(count=8, pl_for=lambda _index: 20.0)))
        from tradercockpit.research_models import ResearchModelsError

        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = _result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                with self.assertRaises(ResearchModelsError) as caught:
                    fit_model(
                        store,
                        family_id=BOOST_FAMILY,
                        historical_result_entity_id=HISTORICAL_ENTITY,
                        expected_historical_result_revision=HISTORICAL_REVISION,
                    )
        self.assertEqual(caught.exception.code, "ml_fit_failed")


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
                self.assertEqual(payload["scope"], "historical_research")
                self.assertEqual(len(payload["families"]), 4)
                status, payload = self._json(base + "/api/research/models?family=tree")
                self.assertEqual(status, 400)
                status, payload = self._json(
                    base + "/api/research/models",
                    method="POST",
                    payload={
                        "action": "fit",
                        "family_id": "nope",
                        "historical_result_entity_id": HISTORICAL_ENTITY,
                        "expected_historical_result_revision": HISTORICAL_REVISION,
                    },
                )
                self.assertEqual(status, 409)
                self.assertIn(payload["reason_code"], {"ml_family_unknown", "ml_backend_not_installed"})
            finally:
                server.shutdown()
                server.server_close()
                thread.join()


if __name__ == "__main__":
    unittest.main()
