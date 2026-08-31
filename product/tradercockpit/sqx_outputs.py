"""Import native StrategyQuant X Builder output into TraderCockpit custody.

SQX remains the producer. TraderCockpit reads one immutable byte snapshot of a
native ``.sqx`` archive, verifies its members, persists that exact snapshot by
content hash, and stores immutable StrategySpecV1 / CandidateSpecV1 identities.
The imported candidate therefore remains runnable even if SQX later clears or
moves the Builder Results databank.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
import os
from pathlib import Path
import tempfile
from zipfile import BadZipFile, ZipFile

from tradercockpit.domain import CandidateSpecV1, ContentAddress, StrategySpecV1
from tradercockpit.storage import ContentStoreError, FileObjectStore

from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1"
SQX_OUTPUT_IMPORT_SCHEMA = "tc.sqx-builder-output-import.v1"
SQX_IMPORTED_CANDIDATE_LIST_SCHEMA = "tc.sqx-imported-candidate-list.v1"
SQX_NATIVE_STRATEGY_SCHEMA = "sqx.native-archive.v1"
SQX_BUILDER_PROJECT = "Builder"
SQX_RESULTS_DATABANK = "Results"
SQX_NATIVE_CUSTODY_ARCHIVES_ROOT = "native/sqx/archives"
SQX_NATIVE_CUSTODY_RESULTS_ROOT = "native/sqx/results"
_REQUIRED_ARCHIVE_MEMBERS = (
    "settings.xml",
    "strategy_Portfolio.xml",
    "version.txt",
)


class SqxOutputError(RuntimeError):
    """Raised when native SQX output cannot enter TraderCockpit custody."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


def _sha256_bytes(value: bytes) -> str:
    return sha256(value).hexdigest()


def _results_root(home: Path) -> Path:
    return (
        home
        / "user"
        / "projects"
        / SQX_BUILDER_PROJECT
        / "databanks"
        / SQX_RESULTS_DATABANK
    )


def _archive_name(value: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        raise SqxOutputError(
            "invalid_archive_name",
            "archive must be a non-empty filename",
        )
    if (
        "/" in value
        or "\\" in value
        or Path(value).name != value
        or not value.lower().endswith(".sqx")
    ):
        raise SqxOutputError(
            "invalid_archive_name",
            "archive must name one .sqx file in the Builder Results databank",
        )
    return value


def _digest(value: str, name: str = "archive_sha256") -> str:
    if not isinstance(value, str) or len(value) != 64:
        raise SqxOutputError(
            "invalid_digest",
            f"{name} must be 64 lowercase hex chars",
        )
    try:
        int(value, 16)
    except ValueError as exc:
        raise SqxOutputError(
            "invalid_digest",
            f"{name} must be 64 lowercase hex chars",
        ) from exc
    if value != value.lower():
        raise SqxOutputError(
            "invalid_digest",
            f"{name} must be 64 lowercase hex chars",
        )
    return value


def _read_archive_snapshot(path: Path) -> bytes:
    if not path.is_file() or path.suffix.lower() != ".sqx":
        raise SqxOutputError(
            "output_not_found",
            f"SQX output does not exist: {path.name}",
        )
    try:
        snapshot = path.read_bytes()
    except OSError as exc:
        raise SqxOutputError(
            "output_unreadable",
            f"SQX output cannot be read: {path.name}",
        ) from exc
    if not snapshot:
        raise SqxOutputError(
            "invalid_sqx_archive",
            f"SQX output is empty: {path.name}",
        )
    return snapshot


def _read_member(archive: ZipFile, name: str) -> bytes:
    matches = [item for item in archive.infolist() if item.filename == name]
    if len(matches) != 1:
        raise SqxOutputError(
            "invalid_sqx_archive",
            f"SQX archive must contain exactly one {name!r} member",
        )
    value = archive.read(matches[0])
    if not value:
        raise SqxOutputError(
            "invalid_sqx_archive",
            f"SQX archive member {name!r} is empty",
        )
    return value


def _inspect_sqx_snapshot(snapshot: bytes, archive_name: str) -> dict[str, object]:
    """Derive all identity fields from one immutable archive byte snapshot."""

    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            settings = _read_member(archive, "settings.xml")
            strategy = _read_member(archive, "strategy_Portfolio.xml")
            version_bytes = _read_member(archive, "version.txt")
            try:
                native_version = version_bytes.decode("utf-8-sig").strip()
            except UnicodeDecodeError as exc:
                raise SqxOutputError(
                    "invalid_sqx_archive",
                    "SQX version.txt is not UTF-8 text",
                ) from exc
            if not native_version or len(native_version) > 256:
                raise SqxOutputError(
                    "invalid_sqx_archive",
                    "SQX version.txt is empty or unreasonably long",
                )
            if native_version != SQX_BUILD:
                raise SqxOutputError(
                    "sqx_version_mismatch",
                    (
                        "SQX archive producer version does not match the supported "
                        f"build: expected {SQX_BUILD}, got {native_version}"
                    ),
                )
            entries = sorted(item.filename for item in archive.infolist())
    except BadZipFile as exc:
        raise SqxOutputError(
            "invalid_sqx_archive",
            f"SQX output is not a valid archive: {archive_name}",
        ) from exc

    return {
        "archive": archive_name,
        "relative_path": (
            f"user/projects/{SQX_BUILDER_PROJECT}/databanks/"
            f"{SQX_RESULTS_DATABANK}/{archive_name}"
        ),
        "bytes": len(snapshot),
        "archive_sha256": _sha256_bytes(snapshot),
        "native_version": native_version,
        "strategy_entry_sha256": _sha256_bytes(strategy),
        "settings_entry_sha256": _sha256_bytes(settings),
        "archive_entries": entries,
        "importable": True,
    }


def inspect_sqx_output(path: Path) -> dict[str, object]:
    """Return source-owned identity fields from one immutable file snapshot."""

    snapshot = _read_archive_snapshot(path)
    return _inspect_sqx_snapshot(snapshot, path.name)


def _state_root(state_root: Path | str | None) -> Path:
    if state_root is None:
        raise SqxOutputError(
            "state_root_not_configured",
            "TraderCockpit state root is not configured",
        )
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise SqxOutputError(
            "state_root_missing",
            f"TraderCockpit state root does not exist: {root}",
        )
    return root


def sqx_custody_blob_path(
    state_root: Path | str,
    archive_sha256: str,
    *,
    result: bool = False,
) -> Path:
    """Return the deterministic custody path for one content-addressed SQX blob."""

    root = Path(state_root).expanduser().resolve()
    digest = _digest(archive_sha256)
    relative_root = (
        SQX_NATIVE_CUSTODY_RESULTS_ROOT
        if result
        else SQX_NATIVE_CUSTODY_ARCHIVES_ROOT
    )
    return root / relative_root / digest[:2] / f"{digest}.sqx"


def verify_sqx_custody_blob(
    state_root: Path | str | None,
    archive_sha256: str,
    *,
    result: bool = False,
    expected_relative_path: str | None = None,
) -> Path:
    """Re-read and hash one claimed custody blob before exposing durable truth."""

    root = _state_root(state_root)
    digest = _digest(archive_sha256)
    target = sqx_custody_blob_path(root, digest, result=result)
    relative = target.relative_to(root).as_posix()
    if expected_relative_path is not None and expected_relative_path != relative:
        raise SqxOutputError(
            "custody_failed",
            "SQX custody path does not match the content-addressed archive identity",
        )
    try:
        snapshot = target.read_bytes()
    except FileNotFoundError as exc:
        raise SqxOutputError(
            "custody_failed",
            f"SQX custody blob is missing: {relative}",
        ) from exc
    except OSError as exc:
        raise SqxOutputError(
            "custody_failed",
            f"SQX custody blob cannot be read: {relative}",
        ) from exc
    if not snapshot or _sha256_bytes(snapshot) != digest:
        raise SqxOutputError(
            "custody_failed",
            f"SQX custody blob hash does not match durable identity: {relative}",
        )
    return target


def persist_sqx_custody_blob(
    state_root: Path | str,
    snapshot: bytes,
    *,
    expected_sha256: str | None = None,
    result: bool = False,
) -> Path:
    """Atomically persist one exact native archive snapshot by SHA-256."""

    if not isinstance(snapshot, bytes) or not snapshot:
        raise SqxOutputError(
            "custody_failed",
            "SQX custody snapshot must be non-empty bytes",
        )
    observed = _sha256_bytes(snapshot)
    if expected_sha256 is not None and observed != _digest(expected_sha256):
        raise SqxOutputError(
            "custody_failed",
            "SQX custody snapshot hash does not match expected identity",
        )
    target = sqx_custody_blob_path(state_root, observed, result=result)
    target.parent.mkdir(parents=True, exist_ok=True)

    if target.exists():
        try:
            existing = target.read_bytes()
        except OSError as exc:
            raise SqxOutputError(
                "custody_failed",
                f"unable to read existing SQX custody blob: {target}",
            ) from exc
        if existing != snapshot:
            raise SqxOutputError(
                "custody_failed",
                "existing SQX custody bytes disagree with content hash",
            )
        return target

    fd, temporary_name = tempfile.mkstemp(
        prefix=f".{observed}.",
        suffix=".tmp",
        dir=target.parent,
    )
    temporary = Path(temporary_name)
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(snapshot)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
    finally:
        if temporary.exists():
            temporary.unlink()

    try:
        stored = target.read_bytes()
    except OSError as exc:
        raise SqxOutputError(
            "custody_failed",
            f"unable to verify stored SQX custody blob: {target}",
        ) from exc
    if stored != snapshot:
        raise SqxOutputError(
            "custody_failed",
            "stored SQX custody bytes changed after persistence",
        )
    return target


def discover_sqx_outputs(sqx_home: Path | str | None) -> dict[str, object]:
    """List only native Builder Results archives from a verified SQX runtime."""

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
    }
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        payload["runtime"] = {
            "ready": False,
            "status": exc.code,
            "detail": exc.detail,
        }
        return payload

    results_root = _results_root(home)
    if not results_root.is_dir():
        payload["runtime"] = {
            "ready": False,
            "status": "results_databank_missing",
            "detail": f"SQX Builder Results databank does not exist: {results_root}",
        }
        return payload

    outputs: list[dict[str, object]] = []
    for path in sorted(
        results_root.glob("*.sqx"),
        key=lambda item: item.name.casefold(),
    ):
        try:
            outputs.append(inspect_sqx_output(path))
        except SqxOutputError as exc:
            try:
                size = path.stat().st_size
            except OSError:
                size = None
            outputs.append(
                {
                    "archive": path.name,
                    "relative_path": (
                        f"user/projects/{SQX_BUILDER_PROJECT}/databanks/"
                        f"{SQX_RESULTS_DATABANK}/{path.name}"
                    ),
                    "bytes": size,
                    "importable": False,
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


def _run_binding(candidate_ref: ContentAddress) -> dict[str, object]:
    return {
        "available": True,
        "mode": "sqx-native-retester",
        "request": {"candidate_ref": str(candidate_ref)},
        "detail": (
            "Candidate archive is persisted in TraderCockpit custody and eligible "
            "for native SQX Retester execution. Launch still verifies the exact "
            "SQX runtime, Retester project, engine artifact, archive, settings, "
            "and producer version identity."
        ),
    }


def imported_sqx_candidates(
    state_root: Path | str | None,
) -> dict[str, object]:
    """List durable imported SQX candidates independently of the live Builder databank."""

    root = _state_root(state_root)
    store = FileObjectStore(root)
    candidate_root = store.objects_root / "candidate" / "v1"
    candidates: list[dict[str, object]] = []
    if not candidate_root.is_dir():
        return {
            "schema": SQX_IMPORTED_CANDIDATE_LIST_SCHEMA,
            "candidates": candidates,
        }

    for path in sorted(candidate_root.glob("*.json"), key=lambda item: item.name):
        try:
            candidate_ref = ContentAddress("candidate", 1, path.stem)
        except ValueError as exc:
            raise SqxOutputError(
                "custody_failed",
                f"invalid candidate object filename in durable custody: {path.name}",
            ) from exc
        try:
            candidate = store.resolve(candidate_ref)
        except (KeyError, ContentStoreError) as exc:
            raise SqxOutputError(
                "custody_failed",
                f"durable candidate object cannot be resolved: {candidate_ref}",
            ) from exc
        if not isinstance(candidate, CandidateSpecV1):
            raise SqxOutputError(
                "custody_failed",
                f"candidate path resolved to the wrong object type: {candidate_ref}",
            )
        if candidate.origin != "sqx-builder":
            continue
        try:
            strategy = store.resolve(candidate.strategy_ref)
        except (KeyError, ContentStoreError) as exc:
            raise SqxOutputError(
                "custody_failed",
                f"imported candidate strategy is missing: {candidate.strategy_ref}",
            ) from exc
        if (
            not isinstance(strategy, StrategySpecV1)
            or strategy.ref != candidate.strategy_ref
        ):
            raise SqxOutputError(
                "custody_failed",
                f"imported candidate strategy custody is invalid: {candidate.strategy_ref}",
            )
        if strategy.semantic_schema != SQX_NATIVE_STRATEGY_SCHEMA:
            raise SqxOutputError(
                "custody_failed",
                "sqx-builder candidate does not reference the native SQX strategy schema",
            )
        native_version = strategy.semantics.get("native_version")
        if native_version != SQX_BUILD:
            raise SqxOutputError(
                "sqx_version_mismatch",
                (
                    "imported SQX candidate producer version does not match the "
                    f"supported build: expected {SQX_BUILD}, got {native_version!r}"
                ),
            )
        archive_sha256 = strategy.semantics.get("archive_sha256")
        if not isinstance(archive_sha256, str):
            raise SqxOutputError(
                "custody_failed",
                "imported SQX strategy is missing archive_sha256 identity",
            )
        custody_path = verify_sqx_custody_blob(root, archive_sha256)
        candidates.append(
            {
                "candidate_ref": str(candidate.ref),
                "strategy_ref": str(strategy.ref),
                "candidate_origin": candidate.origin,
                "semantic_schema": strategy.semantic_schema,
                "archive_sha256": archive_sha256,
                "custody_relative_path": custody_path.relative_to(root).as_posix(),
                "native_version": native_version,
                "strategy_entry_sha256": strategy.semantics.get(
                    "strategy_entry_sha256"
                ),
                "settings_entry_sha256": strategy.semantics.get(
                    "settings_entry_sha256"
                ),
                "run_binding": _run_binding(candidate.ref),
            }
        )

    return {
        "schema": SQX_IMPORTED_CANDIDATE_LIST_SCHEMA,
        "candidates": candidates,
    }


def import_sqx_output(
    sqx_home: Path | str | None,
    state_root: Path | str | None,
    archive_name: str,
) -> dict[str, object]:
    """Persist exact native SQX archive plus strategy/candidate custody."""

    name = _archive_name(archive_name)
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise SqxOutputError(exc.code, exc.detail) from exc

    results_root = _results_root(home)
    if not results_root.is_dir():
        raise SqxOutputError(
            "results_databank_missing",
            f"SQX Builder Results databank does not exist: {results_root}",
        )
    path = results_root / name
    snapshot = _read_archive_snapshot(path)
    output = _inspect_sqx_snapshot(snapshot, name)
    root = _state_root(state_root)
    custody_path = persist_sqx_custody_blob(
        root,
        snapshot,
        expected_sha256=str(output["archive_sha256"]),
    )

    strategy = StrategySpecV1(
        semantic_schema=SQX_NATIVE_STRATEGY_SCHEMA,
        semantics={
            "producer": "strategyquant-x",
            "source_build": SQX_BUILD,
            "source_project": SQX_BUILDER_PROJECT,
            "source_databank": SQX_RESULTS_DATABANK,
            "archive_sha256": output["archive_sha256"],
            "native_version": output["native_version"],
            "strategy_entry_sha256": output["strategy_entry_sha256"],
            "settings_entry_sha256": output["settings_entry_sha256"],
        },
    )
    candidate = CandidateSpecV1(
        strategy_ref=strategy.ref,
        origin="sqx-builder",
    )

    store = FileObjectStore(root)
    strategy_ref = store.put(strategy)
    candidate_ref = store.put(candidate)
    if strategy_ref != strategy.ref or candidate_ref != candidate.ref:
        raise SqxOutputError(
            "custody_failed",
            "content store returned an unexpected immutable identity",
        )

    custody_relative = custody_path.relative_to(root).as_posix()
    archive_payload = dict(output)
    archive_payload["custody_relative_path"] = custody_relative

    return {
        "schema": SQX_OUTPUT_IMPORT_SCHEMA,
        "archive": archive_payload,
        "strategy_ref": str(strategy.ref),
        "candidate_ref": str(candidate.ref),
        "semantic_schema": strategy.semantic_schema,
        "candidate_origin": candidate.origin,
        "custody": "persisted",
        "run_binding": _run_binding(candidate.ref),
    }
