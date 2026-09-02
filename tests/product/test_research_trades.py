from __future__ import annotations

import base64
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import struct
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_retester_http import historical_results_response
from tradercockpit.research_trades import RESEARCH_TRADES_SCHEMA, ResearchTradesError, read_historical_trades
from tradercockpit.sqx_orders import SQX_ORDERS_SCHEMA, SqxOrdersError, inspect_sqx_orders_bytes, parse_orders_bin


HISTORICAL_ENTITY = "tc-research:historical-result:v1:33333333-3333-4333-8333-333333333333"
HISTORICAL_REVISION = f"tc-research-revision:historical-result:sha256:{'3' * 64}"
CANDIDATE_ENTITY = "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111"
CANDIDATE_REVISION = f"tc-research-revision:candidate:sha256:{'1' * 64}"

# Exact orders.bin from the SQX 144.2953-produced StrSingleAsset.sqx fixture.
_NATIVE_PORTFOLIO_ORDERS_BIN = base64.b64decode(
    "rO0ABXflABRTUU9yZGVyRmlsZUZvcm1hdDoxMQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAACAAZBQVBMLkQAEE5ldyBTdHJhdGVneSAoMSkBAgIAAAAAAQAAAAABBAsAAABr4plUAAFGKPgAPbhR7AAAAGvimVQAPbhR7AAAAX/8K8AAQyxcKQAAAADMvrwgJP5J42h/RpGKj0aRio9GhpYAgAAAAIAAAACAAAAAgAAAAAFD1QvvQHwsPUnxETZGjqtyRq57gEnkoP9GkYqPAAAAAAAAAAABwIhZjwAAAAD/AAAAAA=="
)


def _utf(value: str) -> bytes:
    encoded = value.encode("ascii")
    return struct.pack(">H", len(encoded)) + encoded


def _order(
    ticket: int,
    *,
    order_type: int,
    close_type: int = 0,
    in_portfolio: int = 1,
    open_time: int = 1_700_000_000_000,
    close_time: int = 1_700_003_600_000,
) -> bytes:
    parts = [
        bytes((1, 2, 3, 4)),  # Symbol, SetupName, StrategyName, Comment cache refs
        struct.pack(">ii", ticket, ticket),
        struct.pack(">bbb", order_type, close_type, 1),
        struct.pack(">q", open_time),
        struct.pack(">b", order_type),
        struct.pack(">ff", 1.0, 1.1000),
        struct.pack(">qf", open_time, 1.1000),
        struct.pack(">qf", close_time, 1.1100),
        struct.pack(">ffh", 1.0900, 1.1200, 12),
        struct.pack(">ffffffff", 100.0, 1.0, 1.0, 10.0, -25.0, -0.25, -2.5, -1.5),
        struct.pack(">B", 1),
        struct.pack(">ffff", -30.0, -3.0, 120.0, 12.0),
        struct.pack(">i", 123456),  # ignored serialized Duration
        struct.pack(">fff", 10_100.0, 1.01, 1_010.0),
        struct.pack(">i", 42),
        struct.pack(">bffbf", in_portfolio, 0.0, 0.25, 2, 0.0015),
    ]
    return b"".join(parts)


def _orders_bin(*orders: bytes, layout: tuple[int, ...] = (0, 0, 0, 0, 0, 0, 1)) -> bytes:
    primitive = bytearray()
    primitive += _utf("SQOrderFileFormat:11")
    for value in layout:
        primitive += struct.pack(">i", value)
    strings = ("EURUSD", "Setup A", "Strategy A", "producer comment")
    primitive += struct.pack(">i", len(strings))
    for value in strings:
        primitive += _utf(value)
    for order in orders:
        primitive += order
    return b"\xac\xed\x00\x05\x7a" + struct.pack(">I", len(primitive)) + bytes(primitive)


def _archive(orders_bin: bytes) -> bytes:
    buffer = BytesIO()
    with ZipFile(buffer, "w") as archive:
        archive.writestr("version.txt", b"144.2953")
        archive.writestr("orders.bin", orders_bin)
    return buffer.getvalue()


class SqxOrdersReadbackTests(unittest.TestCase):
    def test_real_producer_fixture_matches_the_native_portfolio_record(self) -> None:
        parsed = parse_orders_bin(_NATIVE_PORTFOLIO_ORDERS_BIN)

        self.assertEqual(parsed["orders_entry_sha256"], "99c93e3640febd03ef404817ff54e2328c12aabf41fe86c302715fb5013404da")
        self.assertEqual(parsed["native_order_count"], 1)
        self.assertEqual(parsed["trade_count"], 1)
        trade = parsed["trades"][0]
        self.assertEqual(trade["Symbol"], "AAPL.D")
        self.assertEqual(trade["SetupName"], "New Strategy (1)")
        self.assertEqual(trade["Ticket"], 1)
        self.assertEqual(trade["Type"], 1)
        self.assertEqual(trade["CloseType"], 4)
        self.assertEqual(trade["IsInPortfolio"], 1)
        self.assertEqual(trade["BarsInTrade"], 9470)
        self.assertEqual(trade["Duration"], 1_185_840_000)

    def test_format11_parser_matches_native_portfolio_trade_filter(self) -> None:
        orders_bin = _orders_bin(
            _order(1, order_type=1),
            _order(2, order_type=3),  # pending, not filled
            _order(3, order_type=2, close_type=18),  # native control order
            _order(4, order_type=11),
            _order(5, order_type=2, in_portfolio=0),
        )
        parsed = parse_orders_bin(orders_bin)

        self.assertEqual(parsed["schema"], SQX_ORDERS_SCHEMA)
        self.assertEqual(parsed["orders_format"], "SQOrderFileFormat:11")
        self.assertEqual(parsed["native_order_count"], 5)
        self.assertEqual(parsed["trade_count"], 2)
        self.assertEqual([row["Ticket"] for row in parsed["trades"]], [1, 4])
        self.assertEqual(parsed["trades"][0]["Duration"], 3600)
        self.assertEqual(parsed["trades"][0]["Symbol"], "EURUSD")
        self.assertEqual(parsed["selection"], {
            "result_key": "Portfolio",
            "direction": 0,
            "sample_type": 127,
            "filled_orders": True,
            "control_orders": False,
            "native_filter": "filterExcludingControlOrders",
        })

    def test_duration_matches_java_signed_long_arithmetic(self) -> None:
        parsed = parse_orders_bin(_orders_bin(_order(
            9,
            order_type=1,
            open_time=-(1 << 63),
            close_time=(1 << 63) - 1,
        )))
        # Java long subtraction wraps to -1, then / 1000 truncates toward zero.
        self.assertEqual(parsed["trades"][0]["Duration"], 0)

    def test_format11_layout_and_object_stream_fail_closed(self) -> None:
        with self.assertRaises(SqxOrdersError) as layout_error:
            parse_orders_bin(_orders_bin(_order(1, order_type=1), layout=(1, 0, 0, 0, 0, 0, 1)))
        self.assertEqual(layout_error.exception.code, "sqx_orders_layout_unsupported")

        with self.assertRaises(SqxOrdersError) as stream_error:
            parse_orders_bin(b"\xac\xed\x00\x05\x73")
        self.assertEqual(stream_error.exception.code, "sqx_orders_stream_unsupported")

    def test_archive_requires_exact_build_and_orders_member(self) -> None:
        parsed = inspect_sqx_orders_bytes(_archive(_orders_bin(_order(1, order_type=1))))
        self.assertEqual(parsed["sqx_build"], "144.2953")
        self.assertEqual(parsed["orders_entry"], "orders.bin")
        self.assertEqual(parsed["trade_count"], 1)

        stamped = BytesIO()
        with ZipFile(stamped, "w") as archive:
            archive.writestr("version.txt", b"1")
            archive.writestr("orders.bin", _orders_bin(_order(1, order_type=1)))
        stamped_parsed = inspect_sqx_orders_bytes(stamped.getvalue())
        self.assertEqual(stamped_parsed["sqx_build"], "144.2953")
        self.assertEqual(stamped_parsed["trade_count"], 1)

        buffer = BytesIO()
        with ZipFile(buffer, "w") as archive:
            archive.writestr("version.txt", b"145.0")
            archive.writestr("orders.bin", _orders_bin(_order(1, order_type=1)))
        with self.assertRaises(SqxOrdersError) as mismatch:
            inspect_sqx_orders_bytes(buffer.getvalue())
        self.assertEqual(mismatch.exception.code, "sqx_orders_build_mismatch")


class ResearchTradesBindingTests(unittest.TestCase):
    def _result(self, archive_ref: str, archive_sha: str) -> dict[str, object]:
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

    def test_readback_preserves_product_schema_and_exact_archive_binding(self) -> None:
        snapshot = _archive(_orders_bin(_order(7, order_type=1)))
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = self._result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                payload = read_historical_trades(
                    store,
                    historical_result_entity_id=HISTORICAL_ENTITY,
                    expected_historical_result_revision=HISTORICAL_REVISION,
                )

        self.assertEqual(payload["schema"], RESEARCH_TRADES_SCHEMA)
        self.assertEqual(payload["historical_result_revision"], HISTORICAL_REVISION)
        self.assertEqual(payload["result_archive_sha256"], sha256(snapshot).hexdigest())
        self.assertEqual(payload["trade_count"], 1)
        self.assertEqual(payload["trades"][0]["Ticket"], 7)

    def test_readback_refuses_stale_result_revision_and_archive_binding(self) -> None:
        snapshot = _archive(_orders_bin(_order(7, order_type=1)))
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(snapshot)
            result = self._result(str(ref), sha256(snapshot).hexdigest())
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=result):
                with self.assertRaises(ResearchTradesError) as stale:
                    read_historical_trades(
                        store,
                        historical_result_entity_id=HISTORICAL_ENTITY,
                        expected_historical_result_revision=f"tc-research-revision:historical-result:sha256:{'8' * 64}",
                    )
            self.assertEqual(stale.exception.code, "historical_trades_revision_conflict")

            bad = self._result(str(ref), "9" * 64)
            with patch("tradercockpit.research_trades.read_current_historical_result", return_value=bad):
                with self.assertRaises(ResearchTradesError) as binding:
                    read_historical_trades(
                        store,
                        historical_result_entity_id=HISTORICAL_ENTITY,
                        expected_historical_result_revision=HISTORICAL_REVISION,
                    )
            self.assertEqual(binding.exception.code, "historical_trades_archive_invalid")

    def test_catalog_read_never_parses_orders_and_detail_read_does(self) -> None:
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            catalog = {"schema": "tc.research-historical-result-catalog.v1", "results": []}
            with patch("tradercockpit.research_retester_http.list_current_historical_results", return_value=catalog), patch(
                "tradercockpit.research_retester_http.read_historical_trades"
            ) as trades:
                status, payload = historical_results_response(store)
            self.assertEqual(status, 200)
            self.assertEqual(payload, catalog)
            trades.assert_not_called()

            result = self._result("tc-evidence:sha256:" + "a" * 64, "a" * 64)
            trade_payload = {"schema": RESEARCH_TRADES_SCHEMA, "trade_count": 0, "trades": []}
            with patch("tradercockpit.research_retester_http.read_current_historical_result", return_value=result), patch(
                "tradercockpit.research_retester_http.read_historical_trades", return_value=trade_payload
            ) as trades:
                status, payload = historical_results_response(store, entity_id=HISTORICAL_ENTITY)
            self.assertEqual(status, 200)
            self.assertEqual(payload["trades_readback"], {"state": "available", "payload": trade_payload})
            trades.assert_called_once_with(
                store,
                historical_result_entity_id=HISTORICAL_ENTITY,
                expected_historical_result_revision=HISTORICAL_REVISION,
            )


if __name__ == "__main__":
    unittest.main()
