"""Registered exact implementation hashes for durable robustness result custody.

When robustness semantics change, introduce a new revision and retain prior
revision hashes here so immutable historical results remain verifiable/readable.
This registry file is deliberately outside the hashed implementation set.
"""

KNOWN_ROBUSTNESS_ARTIFACT_SHA256: dict[str, str] = {
    "trade-monte-carlo.v1": "ac1ad97395638dbe7e5a376b916051849e68aecb61da0298b289351a879c0e20",
}
