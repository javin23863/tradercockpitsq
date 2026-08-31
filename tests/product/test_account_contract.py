from decimal import Decimal
import unittest

from tradercockpit.account import (
    AccountContractError,
    AccountIntegrationUnavailable,
    AccountStateV1,
    ModelPolicyV1,
    SpendAuthorityMetadataV1,
    VerifiedGoogleIdentity,
    provision_openrouter_spend_authority,
    verify_google_identity,
)


class FakeVerifier:
    def __init__(self, identity):
        self.identity = identity
        self.seen = []

    def verify(self, credential):
        self.seen.append(credential)
        return self.identity


class FakeProvisioner:
    def __init__(self, authority):
        self.authority = authority
        self.seen = []

    def provision(self, **kwargs):
        self.seen.append(kwargs)
        return self.authority


class AccountContractTests(unittest.TestCase):
    def test_google_subject_is_stable_across_accepted_issuer_forms_and_email_changes(self):
        first = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "google-subject-123",
            "first@example.test",
        )
        alternate_issuer = VerifiedGoogleIdentity(
            "accounts.google.com",
            "google-subject-123",
            "renamed@example.test",
        )
        other = VerifiedGoogleIdentity(
            "https://accounts.google.com",
            "google-subject-456",
            "first@example.test",
        )

        self.assertEqual(first.account_subject, alternate_issuer.account_subject)
        self.assertEqual(first.issuer, "https://accounts.google.com")
        self.assertEqual(alternate_issuer.issuer, "https://accounts.google.com")
        self.assertNotEqual(first.account_subject, other.account_subject)
        self.assertTrue(first.account_subject.startswith("tc-account:google:v1:sha256:"))
        self.assertNotIn("first@example.test", first.account_subject)
        self.assertNotIn("google-subject-123", first.account_subject)

        with self.assertRaisesRegex(AccountContractError, "accepted Google"):
            VerifiedGoogleIdentity("https://example.test", "google-subject-123")

    def test_identity_verification_fails_closed_without_collaborator(self):
        with self.assertRaisesRegex(AccountIntegrationUnavailable, "not configured"):
            verify_google_identity("credential", None)

    def test_identity_verification_accepts_only_verified_identity_object(self):
        identity = VerifiedGoogleIdentity("https://accounts.google.com", "sub-1")
        verifier = FakeVerifier(identity)
        self.assertIs(verify_google_identity("token-1", verifier), identity)
        self.assertEqual(verifier.seen, ["token-1"])

        class BadVerifier:
            def verify(self, credential):
                return {"sub": "sub-1"}

        with self.assertRaises(AccountContractError):
            verify_google_identity("token-2", BadVerifier())

    def test_model_policy_is_current_backend_data_not_durable_account_identity(self):
        identity = VerifiedGoogleIdentity("https://accounts.google.com", "sub-1")
        first_policy = ModelPolicyV1("workhorse-v1", "z-ai/glm-5.3-flash")
        authority = SpendAuthorityMetadataV1(
            "or-authority-opaque-id",
            "active",
            Decimal("2.50"),
            "monthly",
            "2026-09-30T00:00:00Z",
        )
        state = AccountStateV1(
            identity.account_subject,
            True,
            "starter",
            Decimal("2.50"),
            Decimal("0.75"),
            identity.email,
            authority,
        )
        read = state.read_model(first_policy)

        self.assertEqual(read["allowance"]["limit"], "2.5")
        self.assertEqual(read["allowance"]["used"], "0.75")
        self.assertEqual(read["allowance"]["remaining"], "1.75")
        self.assertEqual(read["model_policy"]["default_model"], "z-ai/glm-5.3-flash")
        self.assertEqual(read["spend_authority"]["hard_limit"], "2.5")
        self.assertNotIn("credential", read["spend_authority"])
        self.assertNotIn("secret", read["spend_authority"])

        replacement = ModelPolicyV1("workhorse-v2", "next/efficient-model")
        replaced_read = state.read_model(replacement)
        self.assertEqual(replaced_read["subject"], read["subject"])
        self.assertEqual(replaced_read["allowance"], read["allowance"])
        self.assertEqual(replaced_read["model_policy"]["default_model"], "next/efficient-model")

    def test_spend_authority_metadata_requires_real_provider_bound(self):
        with self.assertRaisesRegex(AccountContractError, "not a credential"):
            SpendAuthorityMetadataV1("sk-or-v1-secret", "active", Decimal("1"))
        with self.assertRaisesRegex(AccountContractError, "explicit hard_limit"):
            SpendAuthorityMetadataV1("authority-without-limit", "active")

        identity = VerifiedGoogleIdentity("https://accounts.google.com", "sub-bound")
        with self.assertRaisesRegex(AccountContractError, "must not exceed"):
            AccountStateV1(
                identity.account_subject,
                True,
                "starter",
                Decimal("1"),
                Decimal("0"),
                identity.email,
                SpendAuthorityMetadataV1("authority-too-large", "active", Decimal("2")),
            )

    def test_spend_provisioning_fails_closed_without_collaborator(self):
        identity = VerifiedGoogleIdentity("https://accounts.google.com", "sub-1")
        with self.assertRaisesRegex(AccountIntegrationUnavailable, "not configured"):
            provision_openrouter_spend_authority(
                account_subject=identity.account_subject,
                hard_limit=Decimal("1"),
                limit_reset=None,
                expires_at=None,
                provisioner=None,
            )

    def test_spend_provisioning_verifies_returned_limit(self):
        identity = VerifiedGoogleIdentity("https://accounts.google.com", "sub-1")
        authority = SpendAuthorityMetadataV1("authority-1", "active", Decimal("1"))
        provisioner = FakeProvisioner(authority)
        result = provision_openrouter_spend_authority(
            account_subject=identity.account_subject,
            hard_limit=Decimal("1"),
            limit_reset="monthly",
            expires_at="2026-09-30T00:00:00Z",
            provisioner=provisioner,
        )
        self.assertIs(result, authority)
        self.assertEqual(provisioner.seen[0]["account_subject"], identity.account_subject)
        self.assertEqual(provisioner.seen[0]["hard_limit"], Decimal("1"))

        wrong = FakeProvisioner(SpendAuthorityMetadataV1("authority-2", "active", Decimal("9")))
        with self.assertRaisesRegex(AccountContractError, "does not match"):
            provision_openrouter_spend_authority(
                account_subject=identity.account_subject,
                hard_limit=Decimal("1"),
                limit_reset=None,
                expires_at=None,
                provisioner=wrong,
            )


if __name__ == "__main__":
    unittest.main()
