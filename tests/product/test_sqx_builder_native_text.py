from __future__ import annotations

import unittest
from xml.etree import ElementTree

from tradercockpit.sqx_builder_config import _native_node, _native_node_record


class SqxBuilderNativeTextTests(unittest.TestCase):
    def test_meaningful_native_text_preserves_leading_and_trailing_whitespace(self) -> None:
        element = ElementTree.fromstring(
            '<BuildTradingOptions><FutureTradingValue>  17 \n</FutureTradingValue></BuildTradingOptions>'
        )

        record = _native_node_record(_native_node(element))

        self.assertEqual(record["children"][0]["text"], "  17 \n")

    def test_formatting_only_indentation_is_not_promoted_to_a_native_value(self) -> None:
        element = ElementTree.fromstring(
            "<BuildTradingOptions>\n  <FutureTradingValue>17</FutureTradingValue>\n</BuildTradingOptions>"
        )

        record = _native_node_record(_native_node(element))

        self.assertIsNone(record["text"])
        self.assertEqual(record["children"][0]["text"], "17")


if __name__ == "__main__":
    unittest.main()
