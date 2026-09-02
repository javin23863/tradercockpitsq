"""Discover installed SQX 144.2953 homes. Never launches a native process."""

from __future__ import annotations

from hashlib import sha256
import os
from pathlib import Path

from tradercockpit.native_runtime_config import (
    native_runtime_config_path,
    optional_native_runtime_config,
    write_native_runtime_config,
)
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH

SQX_RUNTIME_DISCOVERY_SCHEMA = "tc.sqx-runtime-discovery.v1"
NATIVE_RUNTIME_API_PATH = "/api/native-runtime"
_CANDIDATE_PREFIX = "tc-sqx-home:sha256:"


class NativeRuntimeDiscoveryError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def default_search_roots() -> tuple[Path, ...]:
    roots: list[Path] = []
    env_home = os.environ.get("SQX_HOME")
    if env_home and env_home.strip():
        roots.append(Path(env_home).expanduser())
    roots.append(Path("C:/StrategyQuantX"))
    program_files = os.environ.get("ProgramFiles") or r"C:\Program Files"
    program_files_x86 = os.environ.get("ProgramFiles(x86)") or r"C:\Program Files (x86)"
    roots.append(Path(program_files) / "StrategyQuantX")
    roots.append(Path(program_files_x86) / "StrategyQuantX")
    seen: set[Path] = set()
    unique: list[Path] = []
    for root in roots:
        try:
            resolved = root.expanduser().resolve()
        except OSError:
            continue
        if resolved in seen:
            continue
        seen.add(resolved)
        unique.append(resolved)
    return tuple(unique)


def candidate_id_for(home: Path) -> str:
    return _CANDIDATE_PREFIX + sha256(str(home.resolve()).encode("utf-8")).hexdigest()


def inspect_sqx_home(home: Path | str) -> dict[str, object]:
    resolved = Path(home).expanduser().resolve()
    launcher = resolved / SQX_LAUNCHER_RELATIVE_PATH
    observed_sha256 = None
    if launcher.is_file():
        try:
            observed_sha256 = sha256(launcher.read_bytes()).hexdigest()
        except OSError:
            observed_sha256 = None
    observed_build = None
    reason_code = None
    bindable = False
    try:
        verified_sqx_home(resolved)
        if observed_sha256:
            bindable = True
        else:
            reason_code = "sqx_launcher_missing"
    except SqxPresetRuntimeError as exc:
        reason_code = exc.code
    else:
        observed_build = SQX_BUILD
    if not launcher.is_file() and reason_code is None:
        reason_code = "sqx_launcher_missing"
    return {
        "candidate_id": candidate_id_for(resolved),
        "home_path": str(resolved),
        "label": resolved.name or str(resolved),
        "expected_build": SQX_BUILD,
        "observed_build": observed_build,
        "launcher_relative_path": SQX_LAUNCHER_RELATIVE_PATH,
        "launcher_sha256": observed_sha256,
        "bindable": bindable,
        "reason_code": reason_code,
    }


def discover_sqx_runtimes(search_roots: tuple[Path, ...] | list[Path] | None = None) -> list[dict[str, object]]:
    roots = tuple(search_roots) if search_roots is not None else default_search_roots()
    found: list[dict[str, object]] = []
    seen: set[str] = set()
    for root in roots:
        try:
            resolved = Path(root).expanduser().resolve()
        except OSError:
            continue
        if not resolved.is_dir() or not (resolved / SQX_LAUNCHER_RELATIVE_PATH).is_file():
            continue
        record = inspect_sqx_home(resolved)
        candidate_id = str(record["candidate_id"])
        if candidate_id in seen:
            continue
        seen.add(candidate_id)
        found.append(record)
    return found


def native_runtime_discovery_record(
    data_root: Path | str | None,
    *,
    process_home: Path | str | None = None,
    process_launcher_sha256: str | None = None,
    search_roots: tuple[Path, ...] | list[Path] | None = None,
) -> dict[str, object]:
    candidates = discover_sqx_runtimes(search_roots)
    by_id = {str(item["candidate_id"]): item for item in candidates}
    saved_home, saved_digest = (None, None)
    if data_root is not None:
        saved_home, saved_digest = optional_native_runtime_config(data_root)
    saved = None
    if saved_home is not None:
        inspected = inspect_sqx_home(saved_home)
        saved = {
            "present": True,
            "candidate_id": inspected["candidate_id"],
            "home_path": inspected["home_path"],
            "launcher_sha256": saved_digest,
            "matches_observed_launcher": (
                saved_digest == inspected["launcher_sha256"] if saved_digest and inspected["launcher_sha256"] else False
            ),
            "reason_code": inspected["reason_code"]
            if inspected["reason_code"]
            else (None if saved_digest == inspected["launcher_sha256"] else "sqx_launcher_hash_mismatch"),
        }
        if inspected["candidate_id"] not in by_id:
            candidates.append(inspected)
    process_pinned = process_home is not None or bool(process_launcher_sha256)
    recovery = _recovery(process_pinned, saved, candidates)
    return {
        "schema": SQX_RUNTIME_DISCOVERY_SCHEMA,
        "expected_build": SQX_BUILD,
        "process_pinned": process_pinned,
        "saved": saved,
        "candidates": candidates,
        "recovery": recovery,
    }


def bind_discovered_runtime(
    data_root: Path | str,
    candidate_id: str,
    *,
    process_home: Path | str | None = None,
    process_launcher_sha256: str | None = None,
    search_roots: tuple[Path, ...] | list[Path] | None = None,
) -> dict[str, object]:
    if process_home is not None or bool(process_launcher_sha256):
        raise NativeRuntimeDiscoveryError(
            "process_runtime_pinned",
            "SQX_HOME or SQX_LAUNCHER_SHA256 already pins this process; unset them before binding a discovered runtime",
        )
    if not isinstance(candidate_id, str) or not candidate_id.startswith(_CANDIDATE_PREFIX):
        raise NativeRuntimeDiscoveryError("candidate_id_invalid", "bind requires a discovered candidate_id")
    record = native_runtime_discovery_record(data_root, search_roots=search_roots)
    match = next((item for item in record["candidates"] if item["candidate_id"] == candidate_id), None)
    if match is None:
        raise NativeRuntimeDiscoveryError("candidate_not_found", "that SQX home is not in the discovery set")
    if match["bindable"] is not True or not match["launcher_sha256"]:
        raise NativeRuntimeDiscoveryError(
            str(match["reason_code"] or "runtime_not_verified"),
            "only a verified SQX 144.2953 home with sqcli.exe can be bound",
        )
    write_native_runtime_config(
        data_root,
        sqx_home=str(match["home_path"]),
        launcher_sha256=str(match["launcher_sha256"]),
    )
    return native_runtime_discovery_record(data_root, search_roots=search_roots)


def clear_saved_runtime(
    data_root: Path | str,
    *,
    process_home: Path | str | None = None,
    process_launcher_sha256: str | None = None,
    search_roots: tuple[Path, ...] | list[Path] | None = None,
) -> dict[str, object]:
    path = native_runtime_config_path(data_root)
    try:
        path.unlink()
    except FileNotFoundError:
        pass
    except OSError as exc:
        raise NativeRuntimeDiscoveryError("session_store_unreadable", "native-runtime.json could not be cleared") from exc
    return native_runtime_discovery_record(
        data_root,
        process_home=process_home,
        process_launcher_sha256=process_launcher_sha256,
        search_roots=search_roots,
    )


def _recovery(
    process_pinned: bool,
    saved: dict[str, object] | None,
    candidates: list[dict[str, object]],
) -> dict[str, object]:
    if process_pinned:
        return {
            "action": "none",
            "reason_code": "process_runtime_pinned",
            "detail": "This process is pinned by SQX_HOME / SQX_LAUNCHER_SHA256. Discovery is read-only until those are unset.",
        }
    bindable = [item for item in candidates if item.get("bindable") is True]
    if saved and saved.get("reason_code"):
        return {
            "action": "bind" if bindable else "clear",
            "reason_code": saved["reason_code"],
            "detail": "The saved runtime failed verification. Bind a discovered 144.2953 home or clear the saved pointer.",
        }
    if not saved and bindable:
        return {
            "action": "bind",
            "reason_code": "runtime_not_configured",
            "detail": "No saved runtime. Bind a discovered SQX 144.2953 home, then reopen the desktop.",
        }
    if not saved and not bindable:
        return {
            "action": "none",
            "reason_code": "runtime_not_configured",
            "detail": "No verified SQX 144.2953 home was found. Install StrategyQuant X or set SQX_HOME.",
        }
    return {
        "action": "none",
        "reason_code": None,
        "detail": "Saved runtime is present. Reopen the desktop if this process started before the bind.",
    }
