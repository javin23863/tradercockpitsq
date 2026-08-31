"""Trusted native StrategyQuant X control gateway.

The gateway is intentionally narrow. It exposes only the two direct Builder controls
proven by retained SQX evidence: load one exact configuration, then start Builder.
It is not a generic command runner and it does not expose browser mutation routes.

Every native subprocess gets a fresh runtime/build/launcher/configuration preflight.
A read-only runtime-status snapshot is never accepted as launch authorization.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import re
import subprocess
from threading import Lock
from typing import Callable, Sequence

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1"
SQX_NATIVE_CONTROL_ERROR_SCHEMA = "tc.sqx-native-control-error.v1"
SQX_NATIVE_CONTROL_TIMEOUT_SECONDS = 60.0
_BUILDER_PROJECT = "Builder"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CONTROL_LOCK = Lock()


class SqxNativeGatewayError(RuntimeError):
    """Structured fail-closed native control refusal."""

    def __init__(
        self,
        code: str,
        detail: str,
        *,
        receipts: Sequence[dict[str, object]] = (),
    ) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail
        self.receipts = tuple(dict(item) for item in receipts)

    def read_model(self) -> dict[str, object]:
        completed = sum(item.get("state") == "completed" for item in self.receipts)
        return {
            "schema": SQX_NATIVE_CONTROL_ERROR_SCHEMA,
            "error": "native_control_refused",
            "reason_code": self.code,
            "detail": self.detail,
            "control_requests_completed": completed,
            "partial_side_effect": completed > 0,
            "receipts": [dict(item) for item in self.receipts],
        }


def _trusted_digest(value: str | None, *, missing_code: str, invalid_code: str) -> str:
    if value is None or not value.strip():
        raise SqxNativeGatewayError(missing_code, "trusted SHA-256 is not configured")
    normalized = value.strip().lower()
    if not _DIGEST_RE.fullmatch(normalized):
        raise SqxNativeGatewayError(invalid_code, "trusted SHA-256 must be 64 hexadecimal characters")
    return normalized


def _sha256_file(path: Path) -> str:
    digest = sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise SqxNativeGatewayError("native_file_unreadable", "native control file could not be read") from exc
    return digest.hexdigest()


def _resolve_inside(home: Path, value: Path | str, *, escape_code: str) -> tuple[Path, Path]:
    candidate = Path(value).expanduser()
    if not candidate.is_absolute():
        candidate = home / candidate
    try:
        resolved = candidate.resolve()
        relative = resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxNativeGatewayError(escape_code, "native control path resolves outside the verified runtime") from exc
    return resolved, relative


@dataclass(frozen=True, slots=True)
class _VerifiedControlContext:
    home: Path
    launcher: Path
    launcher_sha256: str
    config: Path
    config_relative_path: str
    config_sha256: str


@dataclass(slots=True)
class SqxNativeControlGateway:
    """Run the exact retained native Builder load/start sequence."""

    sqx_home: Path | str | None
    trusted_launcher_sha256: str | None
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run
    timeout_seconds: float = SQX_NATIVE_CONTROL_TIMEOUT_SECONDS

    def __post_init__(self) -> None:
        if (
            not isinstance(self.timeout_seconds, (int, float))
            or isinstance(self.timeout_seconds, bool)
            or self.timeout_seconds <= 0
        ):
            raise ValueError("timeout_seconds must be positive")
        if not callable(self.runner):
            raise TypeError("runner must be callable")

    def _preflight(
        self,
        config_path: Path | str,
        expected_config_sha256: str | None,
    ) -> _VerifiedControlContext:
        """Freshly verify all execution inputs immediately before one subprocess."""

        try:
            home = verified_sqx_home(self.sqx_home)
        except SqxPresetRuntimeError as exc:
            raise SqxNativeGatewayError(exc.code, exc.detail) from exc

        trusted_launcher = _trusted_digest(
            self.trusted_launcher_sha256,
            missing_code="trusted_launcher_not_configured",
            invalid_code="trusted_launcher_digest_invalid",
        )
        launcher, _ = _resolve_inside(
            home,
            SQX_LAUNCHER_RELATIVE_PATH,
            escape_code="sqx_launcher_path_escape",
        )
        if not launcher.is_file():
            raise SqxNativeGatewayError("sqx_launcher_missing", "trusted SQX launcher is missing")
        try:
            observed_launcher = _sha256_file(launcher)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError("sqx_launcher_unreadable", "trusted SQX launcher could not be read") from exc
        if observed_launcher != trusted_launcher:
            raise SqxNativeGatewayError(
                "sqx_launcher_hash_mismatch",
                "configured SQX launcher does not match the trusted identity",
            )

        expected_config = _trusted_digest(
            expected_config_sha256,
            missing_code="config_identity_not_configured",
            invalid_code="config_identity_invalid",
        )
        config, relative = _resolve_inside(
            home,
            config_path,
            escape_code="config_path_escape",
        )
        if config.suffix.lower() != ".xml":
            raise SqxNativeGatewayError(
                "config_type_unsupported",
                "native Builder loadconfig currently accepts only proven XML configuration files",
            )
        if not config.is_file():
            raise SqxNativeGatewayError("config_missing", "native Builder configuration is missing")
        try:
            observed_config = _sha256_file(config)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError("config_unreadable", "native Builder configuration could not be read") from exc
        if observed_config != expected_config:
            raise SqxNativeGatewayError(
                "config_hash_mismatch",
                "native Builder configuration does not match the approved identity",
            )

        return _VerifiedControlContext(
            home=home,
            launcher=launcher,
            launcher_sha256=observed_launcher,
            config=config,
            config_relative_path=relative.as_posix(),
            config_sha256=observed_config,
        )

    @staticmethod
    def _command(context: _VerifiedControlContext, action: str) -> tuple[str, ...]:
        if action == "loadconfig":
            return (
                str(context.launcher),
                "-project",
                "action=loadconfig",
                f"name={_BUILDER_PROJECT}",
                f"file={context.config}",
            )
        if action == "start":
            return (
                str(context.launcher),
                "-project",
                "action=start",
                f"name={_BUILDER_PROJECT}",
            )
        raise AssertionError("unsupported native control action")

    @staticmethod
    def _receipt(
        sequence: int,
        action: str,
        state: str,
        context: _VerifiedControlContext | None,
        *,
        exit_code: int | None,
        reason_code: str | None = None,
    ) -> dict[str, object]:
        return {
            "sequence": sequence,
            "action": action,
            "project": _BUILDER_PROJECT,
            "state": state,
            "exit_code": exit_code,
            "sqx_build": SQX_BUILD,
            "launcher_sha256": context.launcher_sha256 if context else None,
            "config_sha256": context.config_sha256 if context else None,
            "reason_code": reason_code,
        }

    def launch_builder(
        self,
        config_path: Path | str,
        *,
        expected_config_sha256: str | None,
    ) -> dict[str, object]:
        """Load one exact Builder config and submit native Builder start control.

        Success proves only that the two documented native CLI processes exited
        successfully. It does not claim candidate generation or any research result.
        """

        receipts: list[dict[str, object]] = []
        last_context: _VerifiedControlContext | None = None
        with _CONTROL_LOCK:
            for sequence, action in enumerate(("loadconfig", "start"), start=1):
                try:
                    context = self._preflight(config_path, expected_config_sha256)
                except SqxNativeGatewayError as exc:
                    failed = self._receipt(
                        sequence,
                        action,
                        "preflight_failed",
                        last_context,
                        exit_code=None,
                        reason_code=exc.code,
                    )
                    raise SqxNativeGatewayError(
                        exc.code,
                        exc.detail,
                        receipts=(*receipts, failed),
                    ) from exc

                last_context = context
                command = self._command(context, action)
                try:
                    completed = self.runner(
                        list(command),
                        cwd=str(context.home),
                        stdin=subprocess.DEVNULL,
                        capture_output=True,
                        text=True,
                        timeout=float(self.timeout_seconds),
                        check=False,
                        shell=False,
                    )
                except subprocess.TimeoutExpired as exc:
                    failed = self._receipt(
                        sequence,
                        action,
                        "timeout",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_timeout",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_timeout",
                        f"SQX {action} command timed out",
                        receipts=(*receipts, failed),
                    ) from exc
                except OSError as exc:
                    failed = self._receipt(
                        sequence,
                        action,
                        "launch_failed",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_failed",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_failed",
                        f"SQX {action} command could not be executed",
                        receipts=(*receipts, failed),
                    ) from exc

                if type(completed.returncode) is not int:
                    failed = self._receipt(
                        sequence,
                        action,
                        "invalid_receipt",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_failed",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_failed",
                        "SQX command runner returned an invalid exit code",
                        receipts=(*receipts, failed),
                    )
                if completed.returncode != 0:
                    failed = self._receipt(
                        sequence,
                        action,
                        "rejected",
                        context,
                        exit_code=int(completed.returncode),
                        reason_code="sqx_command_rejected",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_rejected",
                        f"SQX {action} command exited nonzero",
                        receipts=(*receipts, failed),
                    )

                receipts.append(
                    self._receipt(
                        sequence,
                        action,
                        "completed",
                        context,
                        exit_code=int(completed.returncode),
                    )
                )

        assert last_context is not None
        return {
            "schema": SQX_NATIVE_CONTROL_SCHEMA,
            "operation": "builder_loadconfig_start",
            "project": _BUILDER_PROJECT,
            "state": "submitted",
            "sqx_build": SQX_BUILD,
            "launcher_sha256": last_context.launcher_sha256,
            "config_relative_path": last_context.config_relative_path,
            "config_sha256": last_context.config_sha256,
            "control_requests_submitted": len(receipts),
            "control_requests_completed": len(receipts),
            "partial_side_effect": False,
            "receipts": [dict(item) for item in receipts],
        }
