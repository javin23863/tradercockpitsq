"""Optional process-local native SQX runtime configuration.

Environment variables remain the preferred trusted overrides. This file lets a
packaged desktop remember a user-configured SQX home without baking machine paths
into the application. It never launches SQX.
"""

from __future__ import annotations

from pathlib import Path
import json
import re


NATIVE_RUNTIME_CONFIG_SCHEMA = "tc.native-runtime-config.v1"
NATIVE_RUNTIME_CONFIG_NAME = "native-runtime.json"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


def native_runtime_config_path(data_root: Path | str) -> Path:
    return Path(data_root) / NATIVE_RUNTIME_CONFIG_NAME


def load_native_runtime_config(data_root: Path | str) -> tuple[Path | None, str | None]:
    """Return ``(sqx_home, launcher_sha256)`` from the data-root config file."""

    path = native_runtime_config_path(data_root)
    if not path.is_file():
        return None, None
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError("native runtime config is unreadable") from exc
    if not isinstance(payload, dict) or payload.get("schema") != NATIVE_RUNTIME_CONFIG_SCHEMA:
        raise ValueError("native runtime config schema is invalid")

    raw_home = payload.get("sqx_home")
    home: Path | None = None
    if raw_home is not None:
        if not isinstance(raw_home, str) or not raw_home.strip():
            raise ValueError("native runtime config sqx_home must be a non-empty path")
        home = Path(raw_home).expanduser().resolve()

    raw_digest = payload.get("launcher_sha256")
    digest: str | None = None
    if raw_digest is not None:
        if not isinstance(raw_digest, str) or not _DIGEST_RE.fullmatch(raw_digest.strip().lower()):
            raise ValueError("native runtime config launcher_sha256 must be a SHA-256 hex digest")
        digest = raw_digest.strip().lower()
    return home, digest


def optional_native_runtime_config(data_root: Path | str) -> tuple[Path | None, str | None]:
    """Load native runtime config, treating a corrupt file as absent so the UI can open."""

    try:
        return load_native_runtime_config(data_root)
    except ValueError:
        return None, None


def write_native_runtime_config(
    data_root: Path | str,
    *,
    sqx_home: Path | str,
    launcher_sha256: str,
) -> Path:
    """Persist a user-local native runtime pointer under the application data root."""

    digest = launcher_sha256.strip().lower()
    if not _DIGEST_RE.fullmatch(digest):
        raise ValueError("native runtime config launcher_sha256 must be a SHA-256 hex digest")
    home = Path(sqx_home).expanduser().resolve()
    path = native_runtime_config_path(data_root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(
            {
                "schema": NATIVE_RUNTIME_CONFIG_SCHEMA,
                "sqx_home": str(home),
                "launcher_sha256": digest,
            },
            indent=2,
            sort_keys=True,
        )
        + "\n",
        encoding="utf-8",
    )
    return path
