from decimal import Decimal
import tempfile
import unittest

from tradercockpit.account import VerifiedGoogleIdentity
from tradercockpit.account_service import resolve_google_account
from tradercockpit.storage import FileAccountStateStore


class SequenceVerifier:
    def __init__(self, identities):
        self.identities = list(identities)

    def verify(self, credential):
        return self.identities.pop(0)


class AccountServiceTests(unittest.TestCase):
    def test_repeated_verified_sign_in_does_not_duplicate_starter_allowance(self):
        first_identity = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "stable-subject",
            "first@example.test",
        )
        second_identity = VerifiedGoogleIdentity(
            "accounts.google.com",
            "stable-subject",
            "renamed@example.test",
        )
        verifier = SequenceVerifier([first_identity, second_identity])

        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            first = resolve_google_account(
                credential="token-1",
                verifier=verifier,
                store=store,
                entitlement_id="starter",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:00:00Z",
            )
            second = resolve_google_account(
                credential="token-2",
                verifier=verifier,
                store=store,
                entitlement_id="different-value-must-not-regrant",
                starter_allowance=Decimal("99"),
                occurred_at="2026-08-31T03:01:00Z",
            )

            self.assertTrue(first.created)
            self.assertFalse(second.created)
            self.assertEqual(first.event.state.subject, second.event.state.subject)
            self.assertEqual(second.event.state.allowance_limit, Decimal("2"))
            self.assertEqual(second.event.state.allowance_used, Decimal("0"))
            self.assertEqual(second.event.state.entitlement_id, "starter")
            self.assertEqual(second.event.state.email, "renamed@example.test")
            self.assertEqual(second.event.previous_event_id, first.event.event_id)
            self.assertEqual(store.current(first.event.state.subject).event_id, second.event.event_id)

    def test_repeated_unchanged_sign_in_returns_existing_head_without_new_event(self):
        identity = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "stable-subject",
            "consumer@example.test",
        )
        verifier = SequenceVerifier([identity, identity])

        with tempfile.TemporaryDirectory() as tmp:
            store = FileAccountStateStore(tmp)
            first = resolve_google_account(
                credential="token-1",
                verifier=verifier,
                store=store,
                entitlement_id="starter",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:00:00Z",
            )
            second = resolve_google_account(
                credential="token-2",
                verifier=verifier,
                store=store,
                entitlement_id="starter",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:01:00Z",
            )

            self.assertFalse(second.created)
            self.assertEqual(second.event.event_id, first.event.event_id)


if __name__ == "__main__":
    unittest.main()
