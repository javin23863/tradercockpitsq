"""Read-only native SQX runtime trust descriptor.

This module verifies runtime/build and launcher identity only. It never launches a
native process and does not implement a control gateway. Descriptor-time launcher
verification is informational only; any future gateway must reverify immediately
before process creation rather than treating this snapshot as launch authorization.
"""

from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import re

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_RUNTIME_SCHEMA = "tc.sqx-runtime.v1"
SQX_LAUNCHER_RELATIVE_PATH = "sqcli.exe"
SQX_LAUNCHER_SHA256_ENV = "SQX_LAUNCHER_SHA256"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


def _trust_configuration(value: str | None) -> tuple[bool, str | None]:
    if value is None or not value.strip():
        return False, None
    normalized = value.strip().lower()
    return True, normalized if _DIGEST_RE.fullmatch(normalized) else None


def _build_failure(
    value: Path | str | None,
    trusted_launcher_sha256: str | None,
    exc: SqxPresetRuntimeError,
) -> dict[str, object]:
    configured = value is not None
    launcher_configured, expected = _trust_configuration(trusted_launcher_sha256)
    return {
        "schema": SQX_RUNTIME_SCHEMA,
        "status": "invalid" if configured else "unavailable",
        "producer": "strategyquant-x",
        "verification_scope": "read-only-snapshot",
        "build": {
            "status": "invalid" if configured else "unavailable",
            "verified": False,
            "expected": SQX_BUILD,
            "observed": None,
            "reason_code": exc.code,
        },
        "launcher": {
            "status": "unavailable",
            "configured": launcher_configured,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": expected,
            "observed_sha256": None,
            "reason_code": "runtime_not_verified",
        },
        "inspection": {
            "available": False,
            "reason_code": exc.code,
        },
        "execution": {
            "available": False,
            "launcher_verified": False,
            "gateway_available": False,
            "launch_authorization": False,
            "requires_fresh_launcher_verification": True,
            "reason_code": exc.code,
        },
    }


def _launcher_descriptor(home: Path, trusted_launcher_sha256: str | None) -> dict[str, object]:
    if trusted_launcher_sha256 is None or not trusted_launcher_sha256.strip():
        return {
            "status": "unavailable",
            "configured": False,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": None,
            "observed_sha256": None,
            "reason_code": "trusted_launcher_not_configured",
        }

    expected = trusted_launcher_sha256.strip().lower()
    if not _DIGEST_RE.fullmatch(expected):
        return {
            "status": "invalid",
            "configured": True,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": None,
            "observed_sha256": None,
            "reason_code": "trusted_launcher_digest_invalid",
        }

    try:
        launcher = (home / SQX_LAUNCHER_RELATIVE_PATH).resolve()
        launcher.relative_to(home)
    except (OSError, RuntimeError, ValueError):
        return {
            "status": "invalid",
            "configured": True,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": expected,
            "observed_sha256": None,
            "reason_code": "sqx_launcher_path_escape",
        }

    if not launcher.is_file():
        return {
            "status": "invalid",
            "configured": True,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": expected,
            "observed_sha256": None,
            "reason_code": "sqx_launcher_missing",
        }

    try:
        snapshot = launcher.read_bytes()
    except OSError:
        return {
            "status": "invalid",
            "configured": True,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": expected,
            "observed_sha256": None,
            "reason_code": "sqx_launcher_unreadable",
        }

    observed = sha256(snapshot).hexdigest()
    if observed != expected:
        return {
            "status": "invalid",
            "configured": True,
            "verified": False,
            "verification_scope": "read-only-snapshot",
            "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
            "expected_sha256": expected,
            "observed_sha256": observed,
            "reason_code": "sqx_launcher_hash_mismatch",
        }

    return {
        "status": "ready",
        "configured": True,
        "verified": True,
        "verification_scope": "read-only-snapshot",
        "relative_path": SQX_LAUNCHER_RELATIVE_PATH,
        "expected_sha256": expected,
        "observed_sha256": observed,
        "reason_code": None,
    }


def sqx_runtime_descriptor(
    sqx_home: Path | str | None,
    trusted_launcher_sha256: str | None = None,
) -> dict[str, object]:
    """Return the secret-free read-only runtime/build/launcher trust descriptor."""

    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        return _build_failure(sqx_home, trusted_launcher_sha256, exc)

    launcher = _launcher_descriptor(home, trusted_launcher_sha256)
    launcher_verified = launcher["verified"] is True
    execution_reason = (
        "trusted_native_gateway_not_implemented"
        if launcher_verified
        else str(launcher["reason_code"])
    )
    return {
        "schema": SQX_RUNTIME_SCHEMA,
        "status": "ready",
        "producer": "strategyquant-x",
        "verification_scope": "read-only-snapshot",
        "build": {
            "status": "ready",
            "verified": True,
            "expected": SQX_BUILD,
            "observed": SQX_BUILD,
            "reason_code": None,
        },
        "launcher": launcher,
        "inspection": {
            "available": True,
            "reason_code": None,
        },
        "execution": {
            "available": False,
            "launcher_verified": launcher_verified,
            "gateway_available": False,
            "launch_authorization": False,
            "requires_fresh_launcher_verification": True,
            "reason_code": execution_reason,
        },
    }
