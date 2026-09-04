"""Optional process-local native SQX runtime configuration.

Environment variables remain the preferred trusted overrides. This file lets a
packaged desktop remember a user-configured SQX home without baking machine paths
into the application. It never launches SQX. The browser never chooses a path.
"""

from __future__ import annotations

from collections.abc import Sequence
from hashlib import sha256
import json
import os
from pathlib import Path
import re

from tradercockpit.sqx_presets import SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


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


def default_sqx_search_roots() -> tuple[Path, ...]:
    """Usual Windows install parents. Linux/mac stay empty so CI does not scan."""

    if os.name != "nt":
        return ()
    roots = [Path(r"C:\StrategyQuantX"), Path(r"C:\StrategyQuant X")]
    for key in ("ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"):
        raw = os.environ.get(key)
        if not raw:
            continue
        base = Path(raw)
        roots.append(base / "StrategyQuantX")
        roots.append(base / "StrategyQuant X")
    roots.append(Path.home() / "Downloads")
    return tuple(roots)


def _looks_like_sqx_install_name(name: str) -> bool:
    folded = name.casefold()
    return folded.startswith("sqx") or folded.startswith("strategyquant")


def _verified_homes_under(root: Path) -> tuple[Path, ...]:
    try:
        return (verified_sqx_home(root),)
    except SqxPresetRuntimeError:
        pass
    except OSError:
        return ()
    try:
        children = sorted(
            path
            for path in root.iterdir()
            if path.is_dir() and _looks_like_sqx_install_name(path.name)
        )
    except OSError:
        return ()
    found: list[Path] = []
    for child in children:
        try:
            found.append(verified_sqx_home(child))
        except (SqxPresetRuntimeError, OSError):
            continue
    return tuple(found)


def discover_verified_sqx_home(
    search_roots: Sequence[Path | str] | None = None,
) -> Path | None:
    """Return the unique verified 144.2953 install under the search roots, or None."""

    roots = (
        tuple(Path(item) for item in search_roots)
        if search_roots is not None
        else default_sqx_search_roots()
    )
    found: list[Path] = []
    seen: set[Path] = set()
    for root in roots:
        for home in _verified_homes_under(root):
            if home in seen:
                continue
            seen.add(home)
            found.append(home)
    if len(found) != 1:
        return None
    return found[0]


def observed_launcher_sha256(home: Path | str) -> str | None:
    launcher = Path(home) / SQX_LAUNCHER_RELATIVE_PATH
    try:
        if not launcher.is_file():
            return None
        return sha256(launcher.read_bytes()).hexdigest()
    except OSError:
        return None


def resolve_process_native_runtime(
    data_root: Path | str,
    *,
    sqx_home: Path | str | None = None,
    launcher_sha256: str | None = None,
    search_roots: Sequence[Path | str] | None = None,
) -> tuple[Path | None, str | None]:
    """Resolve home + launcher digest for this process. Never a browser input."""

    if sqx_home is not None:
        return Path(sqx_home), launcher_sha256
    configured_home, configured_digest = optional_native_runtime_config(data_root)
    if configured_home is not None:
        return configured_home, launcher_sha256 or configured_digest
    found = discover_verified_sqx_home(search_roots)
    if found is None:
        return None, launcher_sha256
    digest = launcher_sha256 or observed_launcher_sha256(found)
    if digest:
        try:
            write_native_runtime_config(data_root, sqx_home=found, launcher_sha256=digest)
        except (OSError, ValueError):
            pass
    return found, digest
