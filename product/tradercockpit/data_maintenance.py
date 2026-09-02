"""Data-root manifest, backup/restore, and crash diagnostics.

Secrets stay outside the data-root (`keys.env` / `TRADERCOCKPIT_SECRETS_PATH`).
OAuth token files may exist on disk inside a backup zip; they never appear in
status or other read models. Restore accepts a backups/ basename only.
"""

from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import traceback
from typing import Any
from zipfile import ZIP_DEFLATED, ZipFile


DATA_MAINTENANCE_SCHEMA = "tc.data-maintenance.v1"
DATA_ROOT_MANIFEST_SCHEMA = "tc.data-root.v1"
CRASH_LOG_SCHEMA = "tc.crash-log.v1"
DATA_MAINTENANCE_API_PATH = "/api/desktop/maintenance"
MANIFEST_NAME = "data-root-manifest.json"
CRASH_LOG_NAME = "crash-log.json"
BACKUP_DIRNAME = "backups"
CURRENT_DATA_ROOT_VERSION = 1
SECRET_FILENAMES = frozenset({"keys.env"})
OAUTH_FILENAMES = frozenset({"google-oauth.json", "schwab-oauth.json"})
_SECRET_KEY_RE = re.compile(
    r"(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|secret|password|passwd|authorization|stripe|openrouter|google)"
)
_SECRET_VALUE_RE = re.compile(
    r"(?i)(sk_live_[A-Za-z0-9]+|sk_test_[A-Za-z0-9]+|sk-or-[A-Za-z0-9._-]+|whsec_[A-Za-z0-9]+|rk_live_[A-Za-z0-9]+|Bearer\s+\S+)"
)


class DataMaintenanceError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def _utc_now() -> datetime:
    return datetime.now(timezone.utc).replace(microsecond=0)


def _iso(moment: datetime) -> str:
    return moment.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _atomic_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    temporary = path.with_name(f".{path.name}.tmp-{os.getpid()}")
    try:
        temporary.write_text(text, encoding="utf-8")
        os.replace(temporary, path)
    finally:
        try:
            temporary.unlink()
        except FileNotFoundError:
            pass


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _read_json(path: Path) -> dict[str, Any] | None:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise DataMaintenanceError("manifest_invalid", "data-root manifest is not valid JSON") from exc
    if not isinstance(payload, dict):
        raise DataMaintenanceError("manifest_invalid", "data-root manifest must be a JSON object")
    return payload


def _require_manifest(payload: dict[str, Any]) -> int:
    schema = payload.get("schema")
    version = payload.get("version")
    if schema != DATA_ROOT_MANIFEST_SCHEMA or version != CURRENT_DATA_ROOT_VERSION:
        raise DataMaintenanceError(
            "unknown_schema",
            "data-root manifest schema is unknown; refusing to continue",
        )
    return CURRENT_DATA_ROOT_VERSION


def ensure_manifest(data_root: Path | str) -> dict[str, Any]:
    """Write `tc.data-root.v1` on first touch; refuse unknown schemas."""

    root = Path(data_root).expanduser().resolve()
    root.mkdir(parents=True, exist_ok=True)
    path = root / MANIFEST_NAME
    payload = _read_json(path)
    if payload is None:
        record = {"schema": DATA_ROOT_MANIFEST_SCHEMA, "version": CURRENT_DATA_ROOT_VERSION}
        _atomic_json(path, record)
        return record
    _require_manifest(payload)
    return payload


def _skip_backup_file(root: Path, path: Path) -> bool:
    try:
        relative = path.relative_to(root)
    except ValueError:
        return True
    if path.name.lower() in SECRET_FILENAMES:
        return True
    parts = relative.parts
    if parts and parts[0] == BACKUP_DIRNAME and path.suffix.lower() == ".zip":
        return True
    return False


def _backup_members(root: Path) -> list[Path]:
    files: list[Path] = []
    for dirpath, _dirnames, filenames in os.walk(root, followlinks=False):
        current = Path(dirpath)
        for name in filenames:
            path = current / name
            if _skip_backup_file(root, path):
                continue
            files.append(path)
    return files


def _last_backup(root: Path) -> dict[str, str] | None:
    backup_dir = root / BACKUP_DIRNAME
    if not backup_dir.is_dir():
        return None
    zips = sorted(
        (
            path
            for path in backup_dir.iterdir()
            if path.is_file()
            and path.name.startswith("tradercockpit-data-")
            and path.suffix.lower() == ".zip"
        ),
        key=lambda path: path.name,
    )
    if not zips:
        return None
    latest = zips[-1]
    stamp = latest.stem.removeprefix("tradercockpit-data-")
    created_at = None
    if re.fullmatch(r"\d{8}T\d{6}Z", stamp):
        created_at = datetime.strptime(stamp, "%Y%m%dT%H%M%SZ").replace(tzinfo=timezone.utc)
    else:
        created_at = datetime.fromtimestamp(latest.stat().st_mtime, timezone.utc).replace(microsecond=0)
    return {
        "name": latest.name,
        "sha256": _sha256_file(latest),
        "created_at": _iso(created_at),
    }


def _crash_status(root: Path) -> dict[str, object] | None:
    path = root / CRASH_LOG_NAME
    if not path.is_file():
        return None
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError):
        return {"present": True, "recorded_at": None}
    recorded_at = payload.get("recorded_at") if isinstance(payload, dict) else None
    return {
        "present": True,
        "recorded_at": recorded_at if isinstance(recorded_at, str) else None,
    }


def data_maintenance_status(data_root: Path | str | None) -> dict[str, Any]:
    """Readiness-only record. Never includes zip members, tokens, or crash text."""

    if data_root is None:
        return {
            "schema": DATA_MAINTENANCE_SCHEMA,
            "status": "unavailable",
            "data_root_version": None,
            "last_backup": None,
            "crash_log": None,
            "reason_code": "data_root_unbound",
            "detail": "Data-root maintenance requires the application data root.",
        }
    root = Path(data_root).expanduser().resolve()
    try:
        manifest = ensure_manifest(root)
        version = _require_manifest(manifest)
    except DataMaintenanceError as exc:
        return {
            "schema": DATA_MAINTENANCE_SCHEMA,
            "status": "unavailable",
            "data_root_version": None,
            "last_backup": None,
            "crash_log": _crash_status(root) if root.is_dir() else None,
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    return {
        "schema": DATA_MAINTENANCE_SCHEMA,
        "status": "ready",
        "data_root_version": version,
        "last_backup": _last_backup(root),
        "crash_log": _crash_status(root),
        "reason_code": None,
        "detail": "Data-root backup, restore, and crash diagnostics are available.",
    }


def backup_data_root(data_root: Path | str) -> dict[str, Any]:
    root = Path(data_root).expanduser().resolve()
    ensure_manifest(root)
    created = _utc_now()
    name = f"tradercockpit-data-{created.strftime('%Y%m%dT%H%M%SZ')}.zip"
    backup_dir = root / BACKUP_DIRNAME
    backup_dir.mkdir(parents=True, exist_ok=True)
    archive = backup_dir / name
    members = _backup_members(root)
    with ZipFile(archive, "w", compression=ZIP_DEFLATED) as handle:
        for path in members:
            handle.write(path, arcname=path.relative_to(root).as_posix())
    return {
        "schema": DATA_MAINTENANCE_SCHEMA,
        "status": "ready",
        "name": name,
        "sha256": _sha256_file(archive),
        "created_at": _iso(created),
        "file_count": len(members),
        "reason_code": None,
        "detail": "Data-root backup written under backups/.",
    }


def _backup_basename(value: object) -> str:
    if not isinstance(value, str) or not value.strip():
        raise DataMaintenanceError("restore_path_escape", "restore archive must be a backups/ basename")
    name = value.strip()
    if name != Path(name).name or "/" in name or "\\" in name or ".." in name:
        raise DataMaintenanceError("restore_path_escape", "restore archive must be a backups/ basename")
    if not name.lower().endswith(".zip"):
        raise DataMaintenanceError("restore_path_escape", "restore archive must be a .zip basename")
    return name


def _unsafe_zip_member(name: str) -> bool:
    relative = name.replace("\\", "/").strip("/")
    if not relative or relative.startswith("/") or relative.startswith("\\"):
        return True
    first = relative.split("/", 1)[0]
    if ":" in first:
        return True
    return any(part in {"", ".."} for part in relative.split("/"))


def _is_backups_member(name: str) -> bool:
    relative = name.replace("\\", "/").lstrip("/")
    return relative == BACKUP_DIRNAME or relative.startswith(f"{BACKUP_DIRNAME}/")


def restore_data_root(data_root: Path | str, archive: object) -> dict[str, Any]:
    root = Path(data_root).expanduser().resolve()
    root.mkdir(parents=True, exist_ok=True)
    name = _backup_basename(archive)
    backup_dir = (root / BACKUP_DIRNAME).resolve()
    backup_dir.mkdir(parents=True, exist_ok=True)
    zip_path = (backup_dir / name).resolve()
    try:
        zip_path.relative_to(backup_dir)
    except ValueError as exc:
        raise DataMaintenanceError("restore_path_escape", "restore archive must live under backups/") from exc
    if not zip_path.is_file():
        raise DataMaintenanceError("backup_not_found", f"backup archive not found: {name}")

    with ZipFile(zip_path, "r") as handle:
        names = handle.namelist()
        if any(_unsafe_zip_member(member) for member in names):
            raise DataMaintenanceError("restore_path_escape", "backup zip contains a path-escaping member")
        manifest_name = next(
            (member for member in names if member.replace("\\", "/").lstrip("/") == MANIFEST_NAME),
            None,
        )
        if manifest_name is None:
            raise DataMaintenanceError("manifest_missing", "backup zip has no data-root manifest")
        try:
            payload = json.loads(handle.read(manifest_name).decode("utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise DataMaintenanceError("manifest_invalid", "backup manifest is not valid JSON") from exc
        if not isinstance(payload, dict):
            raise DataMaintenanceError("manifest_invalid", "backup manifest must be a JSON object")
        _require_manifest(payload)

        extracted = 0
        for member in names:
            if _is_backups_member(member) or member.endswith("/"):
                continue
            relative = member.replace("\\", "/").lstrip("/")
            target = (root / relative).resolve()
            try:
                target.relative_to(root)
            except ValueError as exc:
                raise DataMaintenanceError("restore_path_escape", "backup zip contains a path-escaping member") from exc
            target.parent.mkdir(parents=True, exist_ok=True)
            with handle.open(member) as source, target.open("wb") as dest:
                dest.write(source.read())
            extracted += 1

    return {
        "schema": DATA_MAINTENANCE_SCHEMA,
        "status": "ready",
        "name": name,
        "file_count": extracted,
        "reason_code": None,
        "detail": "Data-root restored from backups/ basename; backups/ preserved.",
    }


def _redact_secrets(text: str, environ: dict[str, str] | None = None) -> str:
    env = environ if environ is not None else os.environ
    redacted = text
    for key, value in env.items():
        if not value or len(value) < 6:
            continue
        if _SECRET_KEY_RE.search(key) or _SECRET_VALUE_RE.search(value):
            redacted = redacted.replace(value, "[redacted]")
    return _SECRET_VALUE_RE.sub("[redacted]", redacted)


def record_crash(data_root: Path | str | None, exc: BaseException, *, environ: dict[str, str] | None = None) -> None:
    if data_root is None:
        return
    root = Path(data_root).expanduser().resolve()
    root.mkdir(parents=True, exist_ok=True)
    formatted = "".join(traceback.format_exception(type(exc), exc, exc.__traceback__))
    payload = {
        "schema": CRASH_LOG_SCHEMA,
        "recorded_at": _iso(_utc_now()),
        "exception_type": type(exc).__name__,
        "traceback": _redact_secrets(formatted, environ),
    }
    _atomic_json(root / CRASH_LOG_NAME, payload)


def data_maintenance_write_response(
    research_store: Any,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if research_store is None:
        return 503, {
            "error": "unavailable",
            "reason_code": "data_root_unbound",
            "detail": "Data-root maintenance requires the application data root.",
        }
    if not isinstance(payload, dict):
        return 400, {
            "error": "invalid_request",
            "reason_code": "maintenance_action_invalid",
            "detail": "maintenance writes accept action=backup or action=restore",
        }
    action = payload.get("action")
    if action == "backup":
        if set(payload) != {"action"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "maintenance_action_invalid",
                "detail": "backup accepts only action=backup",
            }
        try:
            return 200, backup_data_root(research_store.root)
        except DataMaintenanceError as exc:
            return 400, {"error": "invalid_request", "reason_code": exc.code, "detail": exc.detail}
    if action == "restore":
        if set(payload) != {"action", "archive"}:
            return 400, {
                "error": "invalid_request",
                "reason_code": "maintenance_action_invalid",
                "detail": "restore accepts action=restore and a backups/ basename",
            }
        try:
            return 200, restore_data_root(research_store.root, payload.get("archive"))
        except DataMaintenanceError as exc:
            code = 404 if exc.code == "backup_not_found" else 400
            return code, {"error": "invalid_request", "reason_code": exc.code, "detail": exc.detail}
    return 400, {
        "error": "invalid_request",
        "reason_code": "maintenance_action_invalid",
        "detail": "maintenance writes accept action=backup or action=restore",
    }
