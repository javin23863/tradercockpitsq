"""Read-only inspection of native SQX Builder output archives.

SQX remains the producer. This module verifies only the configured SQX runtime,
physically bounded Builder Results path, exact archive bytes, required native
members, and native version. It does not create platform strategy/candidate/run
identity and it never mutates native or platform state.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
from zipfile import BadZipFile, ZipFile

from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1"
SQX_BUILDER_PROJECT = "Builder"
SQX_RESULTS_DATABANK = "Results"
_REQUIRED_ARCHIVE_MEMBERS = (
    "settings.xml",
    "strategy_Portfolio.xml",
    "version.txt",
)


class SqxOutputError(RuntimeError):
    """Raised when native SQX output cannot be inspected without ambiguity."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


def _sha256_bytes(value: bytes) -> str:
    return sha256(value).hexdigest()


def _results_root(home: Path) -> Path:
    candidate = home / "user" / "projects" / SQX_BUILDER_PROJECT / "databanks" / SQX_RESULTS_DATABANK
    try:
        resolved = candidate.resolve()
        resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxOutputError(
            "results_databank_path_escape",
            "SQX Builder Results databank resolves outside the verified runtime",
        ) from exc
    return resolved


def _safe_output_path(results_root: Path, path: Path) -> Path:
    try:
        resolved = path.resolve()
    except (OSError, RuntimeError) as exc:
        raise SqxOutputError("output_unreadable", f"SQX output path could not be resolved: {path.name}") from exc
    if resolved.parent != results_root:
        raise SqxOutputError(
            "output_path_escape",
            "SQX output resolves outside the exact Builder Results databank",
        )
    return resolved


def _read_archive_snapshot(path: Path) -> bytes:
    if not path.is_file() or path.suffix.lower() != ".sqx":
        raise SqxOutputError("output_not_found", f"SQX output does not exist: {path.name}")
    try:
        return path.read_bytes()
    except OSError as exc:
        raise SqxOutputError("output_unreadable", f"SQX output could not be read: {path.name}") from exc


def _read_member(archive: ZipFile, name: str) -> bytes:
    matches = [item for item in archive.infolist() if item.filename == name]
    if len(matches) != 1:
        raise SqxOutputError(
            "invalid_sqx_archive",
            f"SQX archive must contain exactly one {name!r} member",
        )
    value = archive.read(matches[0])
    if not value:
        raise SqxOutputError("invalid_sqx_archive", f"SQX archive member {name!r} is empty")
    return value


def inspect_sqx_output(path: Path, *, results_root: Path | None = None) -> dict[str, object]:
    """Return source-owned identity fields from one immutable archive snapshot."""

    if results_root is not None:
        path = _safe_output_path(results_root, path)
    snapshot = _read_archive_snapshot(path)
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            settings = _read_member(archive, "settings.xml")
            strategy = _read_member(archive, "strategy_Portfolio.xml")
            version_bytes = _read_member(archive, "version.txt")
            try:
                native_version = version_bytes.decode("utf-8-sig").strip()
            except UnicodeDecodeError as exc:
                raise SqxOutputError("invalid_sqx_archive", "SQX version.txt is not UTF-8 text") from exc
            if native_version != SQX_BUILD:
                raise SqxOutputError(
                    "sqx_output_build_mismatch",
                    f"expected SQX output build {SQX_BUILD}, observed {native_version!r}",
                )
            entries = sorted(item.filename for item in archive.infolist())
    except BadZipFile as exc:
        raise SqxOutputError("invalid_sqx_archive", f"SQX output is not a valid archive: {path.name}") from exc

    return {
        "archive": path.name,
        "bytes": len(snapshot),
        "archive_sha256": _sha256_bytes(snapshot),
        "native_version": native_version,
        "strategy_entry_sha256": _sha256_bytes(strategy),
        "settings_entry_sha256": _sha256_bytes(settings),
        "archive_entries": entries,
        "inspectable": True,
    }


def discover_sqx_outputs(sqx_home: Path | str | None) -> dict[str, object]:
    """List only exact native Builder Results archives from a verified runtime."""

    payload: dict[str, object] = {
        "schema": SQX_OUTPUT_LIST_SCHEMA,
        "sqx_build": SQX_BUILD,
        "project": SQX_BUILDER_PROJECT,
        "databank": SQX_RESULTS_DATABANK,
        "runtime": {
            "ready": False,
            "status": "runtime_not_configured",
            "detail": "SQX_HOME is not configured",
        },
        "outputs": [],
        "import_available": False,
        "import_reason": "candidate_custody_not_implemented",
    }
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        payload["runtime"] = {"ready": False, "status": exc.code, "detail": exc.detail}
        return payload
    try:
        results_root = _results_root(home)
    except SqxOutputError as exc:
        payload["runtime"] = {"ready": False, "status": exc.code, "detail": exc.detail}
        return payload
    if not results_root.is_dir():
        payload["runtime"] = {
            "ready": False,
            "status": "results_databank_missing",
            "detail": "SQX Builder Results databank does not exist",
        }
        return payload

    outputs: list[dict[str, object]] = []
    for path in sorted(results_root.glob("*.sqx"), key=lambda item: item.name.casefold()):
        try:
            record = inspect_sqx_output(path, results_root=results_root)
            record["relative_path"] = (
                f"user/projects/{SQX_BUILDER_PROJECT}/databanks/{SQX_RESULTS_DATABANK}/{path.name}"
            )
            outputs.append(record)
        except SqxOutputError as exc:
            outputs.append(
                {
                    "archive": path.name,
                    "inspectable": False,
                    "reason_code": exc.code,
                    "detail": exc.detail,
                }
            )

    payload["runtime"] = {
        "ready": True,
        "status": "verified",
        "detail": f"Verified SQX {SQX_BUILD} Builder Results databank.",
    }
    payload["outputs"] = outputs
    return payload
