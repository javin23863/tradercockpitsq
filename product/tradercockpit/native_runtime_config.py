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
from typing import NamedTuple

from tradercockpit.sqx_presets import SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


NATIVE_RUNTIME_CONFIG_SCHEMA = "tc.native-runtime-config.v1"
NATIVE_RUNTIME_CONFIG_NAME = "native-runtime.json"
RUNTIME_BINDING_ENVIRONMENT = "environment"
RUNTIME_BINDING_REMEMBERED = "remembered"
RUNTIME_BINDING_DISCOVERED = "discovered"
RUNTIME_BINDING_NONE = "none"
SQX_INSTALL_AMBIGUOUS = "sqx_install_ambiguous"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class NativeRuntimeResolution(NamedTuple):
    sqx_home: Path | None
    launcher_sha256: str | None
    source: str = RUNTIME_BINDING_NONE
    reason_code: str | None = None


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


def clear_native_runtime_config(data_root: Path | str) -> None:
    """Drop a remembered runtime pin. Does not accept a browser-chosen path."""

    path = native_runtime_config_path(data_root)
    try:
        path.unlink(missing_ok=True)
    except OSError:
        pass


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
    home = Path.home()
    roots.extend((home / "Downloads", home / "Desktop", home / "Documents"))
    return tuple(roots)


def _looks_like_sqx_install_name(name: str) -> bool:
    folded = name.casefold()
    return folded.startswith("sqx") or folded.startswith("strategyquant")


def observed_launcher_sha256(home: Path | str) -> str | None:
    launcher = Path(home) / SQX_LAUNCHER_RELATIVE_PATH
    try:
        if not launcher.is_file():
            return None
        return sha256(launcher.read_bytes()).hexdigest()
    except OSError:
        return None


def _complete_verified_home(value: Path | str) -> Path | None:
    try:
        home = verified_sqx_home(value)
    except (SqxPresetRuntimeError, OSError):
        return None
    if observed_launcher_sha256(home) is None:
        return None
    return home


def _verified_homes_under(root: Path) -> tuple[Path, ...]:
    complete = _complete_verified_home(root)
    if complete is not None:
        return (complete,)
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
        complete = _complete_verified_home(child)
        if complete is not None:
            found.append(complete)
    return tuple(found)


def list_verified_sqx_homes(
    search_roots: Sequence[Path | str] | None = None,
) -> tuple[Path, ...]:
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
    return tuple(found)


def discover_verified_sqx_home(
    search_roots: Sequence[Path | str] | None = None,
) -> Path | None:
    """Return the unique complete 144.2953 install under the search roots, or None."""

    homes = list_verified_sqx_homes(search_roots)
    if len(homes) != 1:
        return None
    return homes[0]


def _persist_runtime(
    data_root: Path | str,
    *,
    sqx_home: Path,
    launcher_sha256: str,
) -> bool:
    try:
        write_native_runtime_config(
            data_root,
            sqx_home=sqx_home,
            launcher_sha256=launcher_sha256,
        )
        return True
    except (OSError, ValueError):
        return False


def resolve_process_native_runtime(
    data_root: Path | str,
    *,
    sqx_home: Path | str | None = None,
    launcher_sha256: str | None = None,
    search_roots: Sequence[Path | str] | None = None,
) -> NativeRuntimeResolution:
    """Resolve home + launcher digest for this process. Never a browser input."""

    if sqx_home is not None:
        home_path = Path(sqx_home)
        complete = _complete_verified_home(home_path)
        digest = launcher_sha256 or (observed_launcher_sha256(complete) if complete else None)
        if complete is not None and digest:
            _persist_runtime(
                data_root,
                sqx_home=complete,
                launcher_sha256=digest,
            )
        return NativeRuntimeResolution(
            home_path,
            digest,
            RUNTIME_BINDING_ENVIRONMENT,
            None,
        )

    configured_home, configured_digest = optional_native_runtime_config(data_root)
    if configured_home is not None:
        complete = _complete_verified_home(configured_home)
        if complete is None:
            clear_native_runtime_config(data_root)
        else:
            observed = observed_launcher_sha256(complete)
            if (
                launcher_sha256 is None
                and configured_digest
                and observed
                and configured_digest != observed
            ):
                return NativeRuntimeResolution(
                    complete,
                    configured_digest,
                    RUNTIME_BINDING_REMEMBERED,
                    None,
                )
            digest = launcher_sha256 or configured_digest or observed
            return NativeRuntimeResolution(
                complete,
                digest,
                RUNTIME_BINDING_REMEMBERED,
                None,
            )

    homes = list_verified_sqx_homes(search_roots)
    if len(homes) > 1:
        return NativeRuntimeResolution(
            None,
            launcher_sha256,
            RUNTIME_BINDING_NONE,
            SQX_INSTALL_AMBIGUOUS,
        )
    if len(homes) == 0:
        return NativeRuntimeResolution(
            None,
            launcher_sha256,
            RUNTIME_BINDING_NONE,
            "runtime_not_configured",
        )

    found = homes[0]
    digest = launcher_sha256 or observed_launcher_sha256(found)
    if digest:
        _persist_runtime(
            data_root,
            sqx_home=found,
            launcher_sha256=digest,
        )
    return NativeRuntimeResolution(
        found,
        digest,
        RUNTIME_BINDING_DISCOVERED,
        None,
    )
