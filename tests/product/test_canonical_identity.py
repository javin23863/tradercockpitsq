from dataclasses import dataclass
from decimal import Decimal
import unittest

from tradercockpit.domain.canonical import (
    CanonicalizationError,
    ContentAddress,
    canonical_json_bytes,
    content_address,
)


@dataclass(frozen=True)
class Example:
    name: str
    count: int


class CanonicalIdentityTests(unittest.TestCase):
    def test_mapping_order_does_not_change_identity(self):
        left = {"symbol": "ES", "timeframe": "1h", "nested": {"b": 2, "a": 1}}
        right = {"nested": {"a": 1, "b": 2}, "timeframe": "1h", "symbol": "ES"}
        self.assertEqual(canonical_json_bytes(left), canonical_json_bytes(right))
        self.assertEqual(
            content_address("strategy", 1, left),
            content_address("strategy", 1, right),
        )

    def test_kind_and_version_are_part_of_identity(self):
        payload = {"value": 7}
        self.assertNotEqual(
            content_address("strategy", 1, payload),
            content_address("candidate", 1, payload),
        )
        self.assertNotEqual(
            content_address("strategy", 1, payload),
            content_address("strategy", 2, payload),
        )

    def test_dataclass_and_exact_decimal_are_supported(self):
        payload = {"example": Example("x", 2), "price": Decimal("100.5000")}
        encoded = canonical_json_bytes(payload)
        self.assertIn(b'"$tc.decimal":"100.5"', encoded)
        address = content_address("example", 1, payload)
        self.assertTrue(address.verify(payload))
        self.assertEqual(ContentAddress.parse(str(address)), address)

    def test_reserved_tag_mapping_cannot_alias_decimal(self):
        decimal_bytes = canonical_json_bytes(Decimal("1.25"))
        self.assertEqual(decimal_bytes, b'{"$tc.decimal":"1.25"}')
        with self.assertRaisesRegex(
            CanonicalizationError,
            "reserved canonical namespace",
        ):
            canonical_json_bytes({"$tc.decimal": "1.25"})

    def test_reserved_namespace_is_refused_at_any_depth(self):
        with self.assertRaisesRegex(
            CanonicalizationError,
            "reserved canonical namespace",
        ):
            canonical_json_bytes({"nested": {"$tc.future": "x"}})

    def test_tamper_changes_address(self):
        original = {"entry": {"period": 5}}
        changed = {"entry": {"period": 6}}
        address = content_address("strategy", 1, original)
        self.assertFalse(address.verify(changed))

    def test_float_is_refused(self):
        with self.assertRaisesRegex(CanonicalizationError, "float is not permitted"):
            canonical_json_bytes({"price": 1.1})

    def test_non_string_mapping_key_is_refused(self):
        with self.assertRaisesRegex(
            CanonicalizationError,
            "mapping keys must be strings",
        ):
            canonical_json_bytes({1: "bad"})

    def test_sets_are_refused_instead_of_silently_reordered(self):
        with self.assertRaisesRegex(
            CanonicalizationError,
            "unsupported canonical type set",
        ):
            canonical_json_bytes({"values": {1, 2}})

    def test_invalid_address_is_refused(self):
        with self.assertRaises(ValueError):
            ContentAddress.parse("tc:strategy:v0:sha256:" + "0" * 64)
        with self.assertRaises(ValueError):
            content_address("Strategy", 1, {})


if __name__ == "__main__":
    unittest.main()
