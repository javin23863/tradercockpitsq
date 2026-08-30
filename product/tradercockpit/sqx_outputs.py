"""Import native StrategyQuant X Builder output into TraderCockpit custody.

SQX remains the producer. TraderCockpit reads only native ``.sqx`` archives from
the Builder ``Results`` databank, verifies the configured SQX build, hashes the
archive and its strategy/settings members, and then stores immutable
``StrategySpecV1`` / ``CandidateSpecV1`` objects through the existing content
store. No SQX strategy semantics are reimplemented here.
"""

from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from zipfile import BadZipFile, ZipFile

from tradercockpit.domain import CandidateSpecV1, StrategySpecV1
from tradercockpit.storage import FileObjectStore

from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_OUTPUT_LIST_SCHEMA = "tc.sqx-builder-output-list.v1"
SQX_OUTPUT_IMPORT_SCHEMA = "tc.sqx-builder-output-import.v1"
SQX_NATIVE_STRATEGY_SCHEMA = "sqx.native-archive.v1"
SQX_BUILDER_PROJECT = "Builder"
SQX_RESULTS_DATABANK = "Results"
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


def _sha256_file(path: Path) -> str:
    digest = sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _results_root(home: Path) -> Path:
    return home / "user" / "projects" / SQX_BUILDER_PROJECT / "databanks" / SQX_RESULTS_DATABANK


def _archive_name(value: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        raise SqxOutputError("invalid_archive_name", "archive must be a non-empty filename")
    if "/" in value or "\\" in value or Path(value).name != value or not value.lower().endswith(".sqx"):
        raise SqxOutputError("invalid_archive_name", "archive must name one .sqx file in the Builder Results databank")
    return value


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


def inspect_sqx_output(path: Path) -> dict[str, object]:
    """Return source-owned identity fields for one native SQX result archive."""

    if not path.is_file() or path.suffix.lower() != ".sqx":
        raise SqxOutputError("output_not_found", f"SQX output does not exist: {path.name}")
    try:
        with ZipFile(path) as archive:
            settings = _read_member(archive, "settings.xml")
            strategy = _read_member(archive, "strategy_Portfolio.xml")
            version_bytes = _read_member(archive, "version.txt")
            try:
                native_version = version_bytes.decode("utf-8-sig").strip()
            except UnicodeDecodeError as exc:
                raise SqxOutputError("invalid_sqx_archive", "SQX version.txt is not UTF-8 text") from exc
            if not native_version or len(native_version) > 256:
                raise SqxOutputError("invalid_sqx_archive", "SQX version.txt is empty or unreasonably long")
            entries = sorted(item.filename for item in archive.infolist())
    except BadZipFile as exc:
        raise SqxOutputError("invalid_sqx_archive", f"SQX output is not a valid archive: {path.name}") from exc

    stat = path.stat()
    return {
        "archive": path.name,
        "relative_path": f"user/projects/{SQX_BUILDER_PROJECT}/databanks/{SQX_RESULTS_DATABANK}/{path.name}",
        "bytes": stat.st_size,
        "archive_sha256": _sha256_file(path),
        "native_version": native_version,
        "strategy_entry_sha256": _sha256_bytes(strategy),
        "settings_entry_sha256": _sha256_bytes(settings),
        "archive_entries": entries,
        "importable": True,
    }


def discover_sqx_outputs(sqx_home: Path | str | None) -> dict[str, object]:
    """List only native Builder Results archives from a verified SQX runtime."""

    payload: dict[str, object] = {
        "schema": SQX_OUTPUT_LIST_SCHEMA,
        "sqx_build": SQX_BUILD,
        "project": SQX_BUILDER_PROJECT,
        "databank": SQX_RESULTS_DATABANK,
        "runtime": {"ready": False, "status": "runtime_not_configured", "detail": "SQX_HOME is not configured"},
        "outputs": [],
    }
    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        payload["runtime"] = {"ready": False, "status": exc.code, "detail": exc.detail}
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
    for path in sorted(results_root.glob("*.sqx"), key=lambda item: item.name.casefold()):
        try:
            outputs.append(inspect_sqx_output(path))
        except SqxOutputError as exc:
            outputs.append(
                {
                    "archive": path.name,
                    "relative_path": f"user/projects/{SQX_BUILDER_PROJECT}/databanks/{SQX_RESULTS_DATABANK}/{path.name}",
                    "bytes": path.stat().st_size,
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


def import_sqx_output(
    sqx_home: Path | str | None,
    state_root: Path | str | None,
    archive_name: str,
) -> dict[str, object]:
    """Persist exact native SQX strategy/candidate custody for one Builder output."""

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
    output = inspect_sqx_output(path)

    if state_root is None:
        raise SqxOutputError("state_root_not_configured", "TraderCockpit state root is not configured")
    root = Path(state_root).expanduser().resolve()
    if not root.is_dir():
        raise SqxOutputError("state_root_missing", f"TraderCockpit state root does not exist: {root}")

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
        raise SqxOutputError("custody_failed", "content store returned an unexpected immutable identity")

    return {
        "schema": SQX_OUTPUT_IMPORT_SCHEMA,
        "archive": output,
        "strategy_ref": str(strategy.ref),
        "candidate_ref": str(candidate.ref),
        "semantic_schema": strategy.semantic_schema,
        "candidate_origin": candidate.origin,
        "custody": "persisted",
        "run_binding": {
            "available": False,
            "reason_code": "evaluator_not_bound",
            "detail": "Native SQX strategy custody is established; no accepted evaluator is bound to sqx.native-archive.v1 yet.",
        },
    }
