"""Read-only inspection of native SQX Builder output archives.

SQX remains the producer. This module verifies only the configured SQX runtime,
physically bounded Builder Results path, exact archive bytes, required native
members, and native version. It does not infer strategy semantics or a hidden
native job-to-archive identifier.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import os
from pathlib import Path
import re
from zipfile import BadZipFile, ZipFile

from .sqx_presets import (
    SQX_BUILD,
    SQX_RESULT_ARCHIVE_FORMAT_VERSION,
    SqxPresetRuntimeError,
    sqx_result_archive_build,
    verified_sqx_home,
)


SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1"
SQX_BUILDER_PROJECT = "Builder"
SQX_RESULTS_DATABANK = "Results"
_REQUIRED_ARCHIVE_MEMBERS = (
    "settings.xml",
    "strategy_Portfolio.xml",
    "version.txt",
)
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


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
    try:
        value = archive.read(matches[0])
    except (RuntimeError, NotImplementedError, EOFError, OSError) as exc:
        raise SqxOutputError("invalid_sqx_archive", f"SQX archive member {name!r} is unreadable") from exc
    if not value:
        raise SqxOutputError("invalid_sqx_archive", f"SQX archive member {name!r} is empty")
    return value


def inspect_sqx_output_bytes(snapshot: bytes, *, archive_name: str) -> dict[str, object]:
    """Inspect one already-captured immutable native archive snapshot."""

    if not isinstance(snapshot, bytes) or not snapshot:
        raise SqxOutputError("invalid_sqx_archive", "SQX output snapshot is empty")
    if not isinstance(archive_name, str) or not archive_name or Path(archive_name).name != archive_name or not archive_name.lower().endswith(".sqx"):
        raise SqxOutputError("output_name_invalid", "SQX output archive name must be one canonical .sqx filename")
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            settings = _read_member(archive, "settings.xml")
            strategy = _read_member(archive, "strategy_Portfolio.xml")
            version_bytes = _read_member(archive, "version.txt")
            try:
                archive_format = version_bytes.decode("utf-8-sig").strip()
            except UnicodeDecodeError as exc:
                raise SqxOutputError("invalid_sqx_archive", "SQX version.txt is not UTF-8 text") from exc
            if archive_format != SQX_RESULT_ARCHIVE_FORMAT_VERSION:
                raise SqxOutputError(
                    "invalid_sqx_archive",
                    f"expected SQX result archive format {SQX_RESULT_ARCHIVE_FORMAT_VERSION}, observed {archive_format!r}",
                )
            native_version = sqx_result_archive_build(strategy)
            if native_version != SQX_BUILD:
                raise SqxOutputError(
                    "sqx_output_build_mismatch",
                    f"expected SQX output build {SQX_BUILD}, observed {native_version!r}",
                )
            entries = sorted(item.filename for item in archive.infolist())
    except BadZipFile as exc:
        raise SqxOutputError("invalid_sqx_archive", f"SQX output is not a valid archive: {archive_name}") from exc

    return {
        "archive": archive_name,
        "bytes": len(snapshot),
        "archive_sha256": _sha256_bytes(snapshot),
        "native_version": native_version,
        "strategy_entry_sha256": _sha256_bytes(strategy),
        "settings_entry_sha256": _sha256_bytes(settings),
        "archive_entries": entries,
        "inspectable": True,
    }


def inspect_sqx_output(path: Path, *, results_root: Path | None = None) -> dict[str, object]:
    """Return source-owned identity fields from one immutable archive snapshot."""

    if results_root is not None:
        path = _safe_output_path(results_root, path)
    snapshot = _read_archive_snapshot(path)
    return inspect_sqx_output_bytes(snapshot, archive_name=path.name)


def capture_sqx_output_archive(
    sqx_home: Path | str | None,
    archive_name: str,
    *,
    expected_archive_sha256: str,
) -> tuple[bytes, dict[str, object]]:
    """Capture one exact Builder Results archive with pre/post physical identity checks."""

    if (
        not isinstance(archive_name, str)
        or not archive_name
        or Path(archive_name).name != archive_name
        or not archive_name.lower().endswith(".sqx")
    ):
        raise SqxOutputError("output_name_invalid", "archive must be one canonical .sqx filename")
    if not isinstance(expected_archive_sha256, str) or not _DIGEST_RE.fullmatch(expected_archive_sha256):
        raise SqxOutputError("output_digest_invalid", "expected archive SHA-256 is invalid")
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise SqxOutputError(exc.code, exc.detail) from exc
    results_root = _results_root(home)
    if not results_root.is_dir():
        raise SqxOutputError("results_databank_missing", "SQX Builder Results databank does not exist")

    expected = results_root / archive_name
    try:
        before = expected.resolve(strict=True)
        before.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxOutputError("output_path_escape", "SQX output does not resolve inside the verified runtime") from exc
    if before.parent != results_root or not before.is_file():
        raise SqxOutputError("output_not_found", f"SQX output does not exist: {archive_name}")

    try:
        with before.open("rb") as handle:
            opened = os.fstat(handle.fileno())
            snapshot = handle.read()
    except OSError as exc:
        raise SqxOutputError("output_unreadable", f"SQX output could not be read: {archive_name}") from exc
    if not snapshot:
        raise SqxOutputError("invalid_sqx_archive", "SQX output snapshot is empty")

    try:
        after = expected.resolve(strict=True)
        after_stat = after.stat()
        after.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxOutputError("output_changed_during_capture", "SQX output identity changed during capture") from exc
    if (
        after != before
        or after.parent != results_root
        or (opened.st_dev, opened.st_ino) != (after_stat.st_dev, after_stat.st_ino)
    ):
        raise SqxOutputError("output_changed_during_capture", "SQX output identity changed during capture")

    observed = _sha256_bytes(snapshot)
    if observed != expected_archive_sha256:
        raise SqxOutputError("output_digest_mismatch", "SQX output bytes do not match the selected archive identity")
    record = inspect_sqx_output_bytes(snapshot, archive_name=archive_name)
    record["relative_path"] = (
        f"user/projects/{SQX_BUILDER_PROJECT}/databanks/{SQX_RESULTS_DATABANK}/{archive_name}"
    )
    return snapshot, record


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
        "import_reason": "native_results_unavailable",
    }
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        payload["runtime"] = {"ready": False, "status": exc.code, "detail": exc.detail}
        payload["import_reason"] = exc.code
        return payload
    try:
        results_root = _results_root(home)
    except SqxOutputError as exc:
        payload["runtime"] = {"ready": False, "status": exc.code, "detail": exc.detail}
        payload["import_reason"] = exc.code
        return payload
    if not results_root.is_dir():
        payload["runtime"] = {
            "ready": False,
            "status": "results_databank_missing",
            "detail": "SQX Builder Results databank does not exist",
        }
        payload["import_reason"] = "results_databank_missing"
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
    payload["import_available"] = any(item.get("inspectable") is True for item in outputs)
    payload["import_reason"] = None if payload["import_available"] else "no_inspectable_native_outputs"
    return payload
