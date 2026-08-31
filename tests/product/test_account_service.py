from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from pathlib import Path
import tempfile
from threading import Barrier
import unittest

from tradercockpit.account import VerifiedGoogleIdentity
from tradercockpit.account_service import resolve_google_account
from tradercockpit.storage import FileAccountStateStore


class SequenceVerifier:
    def __init__(self, identities):
        self.identities = list(identities)

    def verify(self, credential):
        return self.identities.pop(0)


class BarrierVerifier:
    def __init__(self, identity, parties=2):
        self.identity = identity
        self.barrier = Barrier(parties)

    def verify(self, credential):
        self.barrier.wait(timeout=5)
        return self.identity


class AccountServiceTests(unittest.TestCase):
    def test_repeated_verified_sign_in_does_not_duplicate_or_rewrite_starter_grant(self):
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
                starter_grant_policy_id="starter-2026-08",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:00:00Z",
            )
            second = resolve_google_account(
                credential="token-2",
                verifier=verifier,
                store=store,
                entitlement_id="different-value-must-not-regrant",
                starter_grant_policy_id="different-policy-must-not-regrant",
                starter_allowance=Decimal("99"),
                occurred_at="2026-08-31T03:01:00Z",
            )

            self.assertTrue(first.created)
            self.assertFalse(second.created)
            self.assertEqual(first.event.state.subject, second.event.state.subject)
            self.assertEqual(second.event.state.allowance_limit, Decimal("2"))
            self.assertEqual(second.event.state.allowance_used, Decimal("0"))
            self.assertEqual(second.event.state.entitlement_id, "starter")
            self.assertEqual(second.event.state.starter_grant_policy_id, "starter-2026-08")
            self.assertEqual(second.event.state.starter_grant_id, first.event.state.starter_grant_id)
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
                starter_grant_policy_id="starter-2026-08",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:00:00Z",
            )
            second = resolve_google_account(
                credential="token-2",
                verifier=verifier,
                store=store,
                entitlement_id="starter",
                starter_grant_policy_id="starter-2026-08",
                starter_allowance=Decimal("2"),
                occurred_at="2026-08-31T03:01:00Z",
            )

            self.assertFalse(second.created)
            self.assertEqual(second.event.event_id, first.event.event_id)

    def test_concurrent_first_resolution_admits_only_one_creation_and_grant(self):
        identity = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "concurrent-subject",
            "consumer@example.test",
        )
        verifier = BarrierVerifier(identity)

        with tempfile.TemporaryDirectory() as tmp:
            stores = [FileAccountStateStore(tmp), FileAccountStateStore(tmp)]

            def resolve(index):
                return resolve_google_account(
                    credential=f"token-{index}",
                    verifier=verifier,
                    store=stores[index],
                    entitlement_id="starter",
                    starter_grant_policy_id="starter-2026-08",
                    starter_allowance=Decimal("2"),
                    occurred_at=f"2026-08-31T03:00:0{index}Z",
                )

            with ThreadPoolExecutor(max_workers=2) as pool:
                results = list(pool.map(resolve, (0, 1)))

            self.assertEqual(sorted(result.created for result in results), [False, True])
            self.assertEqual(results[0].event.state.subject, results[1].event.state.subject)
            self.assertEqual(
                results[0].event.state.starter_grant_id,
                results[1].event.state.starter_grant_id,
            )
            self.assertEqual(results[0].event.state.allowance_limit, Decimal("2"))
            self.assertEqual(results[1].event.state.allowance_limit, Decimal("2"))
            event_files = list(Path(tmp, "accounts", "events", "v1").glob("*.json"))
            self.assertEqual(len(event_files), 1)
            current = stores[0].current(identity.account_subject)
            self.assertEqual(current.event_kind, "account_created")
            self.assertEqual(current.state.starter_grant_policy_id, "starter-2026-08")


if __name__ == "__main__":
    unittest.main()
