from __future__ import annotations

import base64
import struct
import unittest

from tradercockpit.sqx_databank import decode_sqstats, lookup_databank_column, parse_sqx_databank


def _java_utf(name: str) -> bytes:
    encoded = name.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def _pack_v2(*records: bytes) -> bytes:
    return b"".join(records)


def _extra_float(name: str, value: float) -> bytes:
    return bytes([103]) + _java_utf(name) + struct.pack(">f", value)


def _numbered_float(field_id: int, value: float) -> bytes:
    return bytes([3, field_id]) + struct.pack(">f", value)


class SqxDatabankTests(unittest.TestCase):
    def test_named_extras_and_numbered_wf_net_profit(self) -> None:
        payload = _pack_v2(
            _numbered_float(10, 16966.0),
            _numbered_float(73, 82.5),
            _extra_float("BestWF", 0.0),
        )
        columns = decode_sqstats(payload, version=2)
        self.assertEqual(columns["NetProfit"], 16966.0)
        self.assertEqual(columns["WFPctOfProfitableRuns"], 82.5)
        self.assertEqual(columns["BestWF"], 0.0)

    def test_parse_results_group_and_cl_lookup(self) -> None:
        main = base64.b64encode(_pack_v2(_numbered_float(10, 1000.0), _numbered_float(21, 12.5))).decode("ascii")
        cross = base64.b64encode(_pack_v2(
            _numbered_float(10, 800.0),
            _numbered_float(21, 18.0),
            _extra_float("WFPctOfProfitableRuns", 91.0),
        )).decode("ascii")
        xml = f"""<Settings>
          <ResultsGroup>
            <ResultsMap>
              <Result resultKey="Portfolio">
                <ValuesMap>
                  <stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                    <SQStats version="2" e="b64">{main}</SQStats>
                  </stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                </ValuesMap>
              </Result>
              <Result resultKey="CrossCheck_MonteCarloManipulation">
                <ValuesMap>
                  <stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                    <SQStats version="2" e="b64">{cross}</SQStats>
                  </stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                </ValuesMap>
              </Result>
            </ResultsMap>
          </ResultsGroup>
        </Settings>""".encode()
        rows = parse_sqx_databank(xml)
        self.assertEqual(len(rows), 2)
        self.assertEqual(lookup_databank_column(rows, "NetProfit", sample_type=127, confidence_level=50), 1000.0)
        self.assertIsNone(lookup_databank_column(rows, "NetProfit", sample_type=127, confidence_level=80))
        self.assertEqual(lookup_databank_column(rows, "WFPctOfProfitableRuns", sample_type=127), 91.0)
        self.assertIsNone(lookup_databank_column(rows, "NetProfit", sample_type=10, confidence_level=80))

        cl80 = base64.b64encode(_pack_v2(_numbered_float(10, 800.0))).decode("ascii")
        xml_cl = xml.decode().replace(
            'resultKey="CrossCheck_MonteCarloManipulation"',
            'resultKey="CrossCheck_MonteCarloManipulation_CL80"',
        ).replace(cross, cl80, 1).encode()
        cl_rows = parse_sqx_databank(xml_cl)
        self.assertEqual(lookup_databank_column(cl_rows, "NetProfit", sample_type=127, confidence_level=80), 800.0)

    def test_truncated_sqstats_are_omitted(self) -> None:
        truncated = bytes([3, 10])
        with self.assertRaises(ValueError):
            decode_sqstats(truncated, version=2)
        with self.assertRaises(ValueError):
            decode_sqstats(bytes([99]), version=2)
        payload = _pack_v2(_numbered_float(10, 1.0), bytes([0]))
        self.assertEqual(decode_sqstats(payload, version=2)["NetProfit"], 1.0)
        blob = base64.b64encode(truncated).decode("ascii")
        xml = f"""<Settings>
          <ResultsGroup>
            <ResultsMap>
              <Result resultKey="Portfolio">
                <ValuesMap>
                  <stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                    <SQStats version="2" e="b64">{blob}</SQStats>
                  </stats_LQ1_direction_DD_0_L1_pl_DD_0_L1_sample_DD_127_L1__RQ1_>
                </ValuesMap>
              </Result>
            </ResultsMap>
          </ResultsGroup>
        </Settings>""".encode()
        self.assertEqual(parse_sqx_databank(xml), [])
