"""Windows desktop release manifest, signing, updater, and rollback contracts.

Packaging installs the frozen ``TraderCockpit.exe`` product identity only. It never
retargets StrategyQuant X launchers or the other TraderCockpit product tree.
"""

from __future__ import annotations

from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from typing import Any


INSTALL_MANIFEST_SCHEMA = "tc.windows-install.v1"
SIGNING_STATUS_SCHEMA = "tc.windows-signing.v1"
UPDATE_RESULT_SCHEMA = "tc.windows-update.v1"
ROLLBACK_RESULT_SCHEMA = "tc.windows-rollback.v1"
MANIFEST_FILENAME = "install-manifest.json"
ROLLBACK_DIRNAME = ".rollback"
EXECUTABLE_NAME = "TraderCockpit.exe"

SIGNING_CERT_PATH_ENV = "TRADERCOCKPIT_SIGNING_CERT_PATH"
SIGNING_CERT_PASSWORD_ENV = "TRADERCOCKPIT_SIGNING_CERT_PASSWORD"
SIGNTOOL_PATH_ENV = "TRADERCOCKPIT_SIGNTOOL_PATH"
SIGNING_TIMESTAMP_URL_ENV = "TRADERCOCKPIT_SIGNING_TIMESTAMP_URL"


class WindowsReleaseError(Exception):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class SigningConfig:
    cert_path: Path
    cert_password: str
    signtool_path: Path
    timestamp_url: str | None


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def default_signtool_path() -> Path:
    windows_kits = os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")
    return Path(windows_kits) / "Windows Kits" / "10" / "bin" / "x64" / "signtool.exe"


def signing_config_from_environ(environ: dict[str, str] | None = None) -> SigningConfig | None:
    env = environ if environ is not None else os.environ
    cert_raw = env.get(SIGNING_CERT_PATH_ENV, "").strip()
    password = env.get(SIGNING_CERT_PASSWORD_ENV, "").strip()
    if not cert_raw or not password:
        return None
    cert_path = Path(cert_raw).expanduser()
    signtool_raw = env.get(SIGNTOOL_PATH_ENV, "").strip()
    signtool_path = Path(signtool_raw).expanduser() if signtool_raw else default_signtool_path()
    timestamp = env.get(SIGNING_TIMESTAMP_URL_ENV, "").strip() or None
    return SigningConfig(
        cert_path=cert_path,
        cert_password=password,
        signtool_path=signtool_path,
        timestamp_url=timestamp,
    )


def signing_status(*, environ: dict[str, str] | None = None) -> dict[str, Any]:
    env = environ if environ is not None else os.environ
    config = signing_config_from_environ(env)
    if config is None:
        missing = []
        if not env.get(SIGNING_CERT_PATH_ENV, "").strip():
            missing.append(SIGNING_CERT_PATH_ENV)
        if not env.get(SIGNING_CERT_PASSWORD_ENV, "").strip():
            missing.append(SIGNING_CERT_PASSWORD_ENV)
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "not_configured",
            "reason_code": "signing_not_configured",
            "detail": "Authenticode signing requires operator certificate material in the environment.",
            "missing_env": missing,
        }
    if sys.platform != "win32":
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "unavailable",
            "reason_code": "signing_host_not_windows",
            "detail": "Authenticode signing must run on Windows.",
        }
    if not config.cert_path.is_file():
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "not_configured",
            "reason_code": "signing_cert_missing",
            "detail": f"Signing certificate not found: {config.cert_path}",
        }
    if not config.signtool_path.is_file():
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "not_configured",
            "reason_code": "signtool_missing",
            "detail": f"signtool.exe not found: {config.signtool_path}",
        }
    return {
        "schema": SIGNING_STATUS_SCHEMA,
        "status": "ready",
        "reason_code": None,
        "detail": "Authenticode signing material is configured.",
        "cert_path": str(config.cert_path),
        "signtool_path": str(config.signtool_path),
    }


def sign_executable(
    executable: Path,
    *,
    config: SigningConfig | None = None,
    environ: dict[str, str] | None = None,
) -> dict[str, Any]:
    target = Path(executable).expanduser().resolve()
    if target.name != EXECUTABLE_NAME:
        raise WindowsReleaseError("signing_target_invalid", f"refusing to sign non-product executable: {target.name}")

    readiness = signing_status(environ=environ)
    if readiness["status"] != "ready":
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "skipped",
            "reason_code": readiness["reason_code"],
            "detail": readiness["detail"],
            "path": str(target),
        }

    selected = config or signing_config_from_environ(environ)
    assert selected is not None

    command = [
        str(selected.signtool_path),
        "sign",
        "/fd",
        "SHA256",
    ]
    if selected.timestamp_url:
        command.extend(["/tr", selected.timestamp_url, "/td", "SHA256"])
    command.extend(
        [
            "/f",
            str(selected.cert_path),
            "/p",
            selected.cert_password,
            str(target),
        ]
    )

    completed = subprocess.run(command, check=False, capture_output=True, text=True)
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout or "signtool sign failed").strip()
        return {
            "schema": SIGNING_STATUS_SCHEMA,
            "status": "failed",
            "reason_code": "signing_failed",
            "detail": detail,
            "path": str(target),
        }
    return {
        "schema": SIGNING_STATUS_SCHEMA,
        "status": "signed",
        "reason_code": None,
        "detail": "Executable signed with Authenticode.",
        "path": str(target),
        "sha256": sha256_file(target),
    }


def build_install_manifest(
    *,
    version: str,
    install_root: Path,
    executable_sha256: str,
    previous: dict[str, Any] | None = None,
) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "schema": INSTALL_MANIFEST_SCHEMA,
        "version": version,
        "install_root": str(Path(install_root).expanduser().resolve()),
        "executable": EXECUTABLE_NAME,
        "executable_sha256": executable_sha256,
    }
    if previous is not None:
        payload["previous"] = previous
    return payload


def manifest_path(install_root: Path) -> Path:
    return Path(install_root).expanduser().resolve() / MANIFEST_FILENAME


def rollback_dir(install_root: Path) -> Path:
    return Path(install_root).expanduser().resolve() / ROLLBACK_DIRNAME


def write_install_manifest(install_root: Path, manifest: dict[str, Any]) -> Path:
    if manifest.get("schema") != INSTALL_MANIFEST_SCHEMA:
        raise WindowsReleaseError("install_manifest_invalid", "install manifest schema mismatch")
    destination = manifest_path(install_root)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return destination


def read_install_manifest(install_root: Path) -> dict[str, Any]:
    source = manifest_path(install_root)
    if not source.is_file():
        raise WindowsReleaseError("install_manifest_missing", f"missing install manifest: {source}")
    payload = json.loads(source.read_text(encoding="utf-8"))
    if payload.get("schema") != INSTALL_MANIFEST_SCHEMA:
        raise WindowsReleaseError("install_manifest_invalid", "install manifest schema mismatch")
    return payload


def _backup_current_executable(install_root: Path, manifest: dict[str, Any]) -> dict[str, Any]:
    installed = Path(install_root).expanduser().resolve() / EXECUTABLE_NAME
    if not installed.is_file():
        raise WindowsReleaseError("installed_executable_missing", f"missing installed executable: {installed}")

    backup_root = rollback_dir(install_root)
    backup_root.mkdir(parents=True, exist_ok=True)
    backup_exe = backup_root / EXECUTABLE_NAME
    shutil.copy2(installed, backup_exe)
    previous = {
        "version": manifest.get("version"),
        "executable_sha256": manifest.get("executable_sha256") or sha256_file(installed),
        "backup_path": str(backup_exe),
    }
    return previous


def apply_trusted_update(
    install_root: Path,
    payload: Path,
    *,
    expected_sha256: str,
    version: str,
) -> dict[str, Any]:
    root = Path(install_root).expanduser().resolve()
    source = Path(payload).expanduser().resolve()
    if source.name != EXECUTABLE_NAME:
        raise WindowsReleaseError("update_payload_invalid", f"update payload must be {EXECUTABLE_NAME}")

    actual_sha256 = sha256_file(source)
    expected = expected_sha256.strip().casefold()
    if actual_sha256.casefold() != expected:
        raise WindowsReleaseError(
            "update_payload_untrusted",
            "update payload SHA-256 does not match the trusted release manifest",
        )

    manifest = read_install_manifest(root)
    previous = _backup_current_executable(root, manifest)

    destination = root / EXECUTABLE_NAME
    shutil.copy2(source, destination)

    updated = build_install_manifest(
        version=version,
        install_root=root,
        executable_sha256=actual_sha256,
        previous=previous,
    )
    write_install_manifest(root, updated)
    return {
        "schema": UPDATE_RESULT_SCHEMA,
        "status": "applied",
        "reason_code": None,
        "detail": "Trusted update applied; previous payload retained for rollback.",
        "version": version,
        "executable_sha256": actual_sha256,
        "install_root": str(root),
    }


def rollback_install(install_root: Path) -> dict[str, Any]:
    root = Path(install_root).expanduser().resolve()
    manifest = read_install_manifest(root)
    previous = manifest.get("previous")
    if not isinstance(previous, dict):
        raise WindowsReleaseError("rollback_unavailable", "install manifest has no previous payload")

    backup_path = previous.get("backup_path")
    if not isinstance(backup_path, str) or not backup_path.strip():
        raise WindowsReleaseError("rollback_unavailable", "previous payload backup path is missing")

    backup = Path(backup_path).expanduser().resolve()
    if not backup.is_file():
        raise WindowsReleaseError("rollback_backup_missing", f"rollback backup missing: {backup}")

    backup_sha256 = sha256_file(backup)
    expected = str(previous.get("executable_sha256") or "").casefold()
    if expected and backup_sha256.casefold() != expected:
        raise WindowsReleaseError(
            "rollback_backup_untrusted",
            "rollback backup SHA-256 does not match the install manifest",
        )

    destination = root / EXECUTABLE_NAME
    shutil.copy2(backup, destination)

    rolled_back = build_install_manifest(
        version=str(previous.get("version") or manifest.get("version") or "unknown"),
        install_root=root,
        executable_sha256=backup_sha256,
    )
    write_install_manifest(root, rolled_back)
    return {
        "schema": ROLLBACK_RESULT_SCHEMA,
        "status": "restored",
        "reason_code": None,
        "detail": "Previous TraderCockpit payload restored from rollback backup.",
        "version": rolled_back["version"],
        "executable_sha256": backup_sha256,
        "install_root": str(root),
    }
