from decimal import Decimal
import json
from pathlib import Path
import tempfile
import unittest

from tradercockpit.account import (
    AccountStateEventV1,
    AccountStateV1,
    SpendAuthorityMetadataV1,
    VerifiedGoogleIdentity,
)
from tradercockpit.storage import AccountStateStoreError, FileAccountStateStore


class AccountStateStoreTests(unittest.TestCase):
    def account(self):
        return VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "stable-google-subject",
            "consumer@example.test",
        )

    def state(self, identity, *, used="0", signed_in=True, authority=None):
        return AccountStateV1(
            identity.account_subject,
            signed_in,
            "starter",
            Decimal("2.5"),
            Decimal(used),
            identity.email,
            authority,
        )

    def test_publishes_immutable_event_and_atomic_current_head(self):
        identity = self.account()
        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            created = AccountStateEventV1(
                "account_created",
                "2026-08-31T03:00:00Z",
                self.state(identity),
            )
            self.assertEqual(store.publish(created), created.event_id)
            current = store.current(identity.account_subject)
            self.assertEqual(current.event_id, created.event_id)
            self.assertEqual(current.state.subject, identity.account_subject)
            self.assertEqual(current.state.allowance_remaining, Decimal("2.5"))

            authority = SpendAuthorityMetadataV1(
                "provider-authority-opaque-id",
                "active",
                Decimal("2.5"),
                "monthly",
                "2026-09-30T00:00:00Z",
            )
            bound = AccountStateEventV1(
                "spend_authority_bound",
                "2026-08-31T03:01:00Z",
                self.state(identity, authority=authority),
                created.event_id,
            )
            store.publish(bound)
            current = store.current(identity.account_subject)
            self.assertEqual(current.event_id, bound.event_id)
            self.assertEqual(current.previous_event_id, created.event_id)
            self.assertEqual(current.state.spend_authority.authority_id, "provider-authority-opaque-id")

            event_path = Path(tmp, "accounts", "events", "v1", f"{created.event_id}.json")
            self.assertTrue(event_path.is_file())
            raw = event_path.read_text(encoding="utf-8")
            self.assertNotIn("OPENROUTER", raw)
            self.assertNotIn("management_key", raw)
            self.assertNotIn("secret", raw.lower())

    def test_rejects_stale_or_non_creation_first_event(self):
        identity = self.account()
        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            state = self.state(identity)
            with self.assertRaisesRegex(AccountStateStoreError, "first account event"):
                store.publish(
                    AccountStateEventV1(
                        "usage_reconciled",
                        "2026-08-31T03:00:00Z",
                        state,
                    )
                )

            created = AccountStateEventV1(
                "account_created",
                "2026-08-31T03:00:00Z",
                state,
            )
            store.publish(created)
            stale = AccountStateEventV1(
                "usage_reconciled",
                "2026-08-31T03:01:00Z",
                self.state(identity, used="0.25"),
                "a" * 64,
            )
            with self.assertRaisesRegex(AccountStateStoreError, "current head"):
                store.publish(stale)

    def test_corrupt_head_or_event_fails_closed(self):
        identity = self.account()
        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            created = AccountStateEventV1(
                "account_created",
                "2026-08-31T03:00:00Z",
                self.state(identity),
            )
            store.publish(created)
            event_path = Path(tmp, "accounts", "events", "v1", f"{created.event_id}.json")
            envelope = json.loads(event_path.read_text(encoding="utf-8"))
            envelope["payload"]["state"]["allowance_used"] = "1"
            event_path.write_text(
                json.dumps(envelope, sort_keys=True, separators=(",", ":")),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(AccountStateStoreError, "does not match"):
                store.current(identity.account_subject)

    def test_store_reopens_same_subject_and_state(self):
        identity = self.account()
        with tempfile.TemporaryDirectory() as tmp:
            first = FileAccountStateStore(tmp)
            created = AccountStateEventV1(
                "account_created",
                "2026-08-31T03:00:00Z",
                self.state(identity, used="0.5"),
            )
            first.publish(created)

            reopened = FileAccountStateStore(tmp)
            current = reopened.current(identity.account_subject)
            self.assertEqual(current.event_id, created.event_id)
            self.assertEqual(current.state.allowance_used, Decimal("0.5"))
            self.assertEqual(current.state.entitlement_id, "starter")


if __name__ == "__main__":
    unittest.main()
