"""Read native SQX program-layout modules as module archives, not Custom Projects.

Builder, Retester, and Optimizer reuse the same project.cfx / task XML custody as
Custom Projects. Data manager and AlgoWizard are inspect-only: this module reports
whether a native archive exists and never invents an editor, downloader, or tasks.
"""

from __future__ import annotations

from hashlib import sha256
from pathlib import Path

from .sqx_custom_project import (
    SQX_CUSTOM_PROJECTS_RELATIVE_ROOT,
    SqxCustomProjectTopologyError,
    _catalog_item_from_topology,
    _project_relative_path,
    _resolved_project_archive,
    _verified_home,
    custom_project_control_record,
    read_sqx_custom_project_topology,
)
from .sqx_presets import SQX_BUILD


SQX_RUN_MODULE_SCHEMA = "tc.sqx-run-module.v1"
SQX_RUN_MODULE_API_PATH = "/api/sqx-module"
SQX_RUN_MODULE_NAMES = ("Builder", "Retester", "Optimizer")
SQX_INSPECT_MODULE_NAMES = ("Data manager", "AlgoWizard")
SQX_MODULE_ALIASES = {
    "builder": "Builder",
    "retester": "Retester",
    "optimizer": "Optimizer",
    "data-manager": "Data manager",
    "data manager": "Data manager",
    "datamanager": "Data manager",
    "algowizard": "AlgoWizard",
}
SQX_INSPECT_FOLDERS = {
    "Data manager": ("DataManager", "Data Manager"),
    "AlgoWizard": ("AlgoWizard",),
}
SQX_INSPECT_JOB = {
    "Data manager": "data downloader or manager",
    "AlgoWizard": "block editor",
}


class SqxRunModuleError(RuntimeError):
    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


def canonical_sqx_module_name(value: object) -> str:
    if not isinstance(value, str) or not value.strip():
        raise SqxRunModuleError("sqx_module_name_invalid", "SQX module name must be one program-layout module")
    raw = value.strip()
    if raw in SQX_RUN_MODULE_NAMES or raw in SQX_INSPECT_MODULE_NAMES:
        return raw
    mapped = SQX_MODULE_ALIASES.get(raw.casefold())
    if mapped:
        return mapped
    raise SqxRunModuleError("sqx_module_name_invalid", "SQX module name must be one program-layout module")


def _control_record(
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    control = custom_project_control_record(sqx_home, trusted_launcher_sha256, register_worker)
    if control["available"] is True:
        return control
    return {
        **control,
        "detail": (
            "Module start uses the verified StrategyQuant X runtime and trusted launcher. "
            f"{control['detail']}"
        ),
    }


def _base_record(
    module: str,
    kind: str,
    *,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    return {
        "schema": SQX_RUN_MODULE_SCHEMA,
        "source_build": SQX_BUILD,
        "module": module,
        "kind": kind,
        "status": "unavailable",
        "reason_code": None,
        "detail": None,
        "project": None,
        "source_relative_path": None,
        "archive_sha256": None,
        "task_count": None,
        "databank_count": None,
        "strategy_count": None,
        "editor_wired": False,
        "control": _control_record(sqx_home, trusted_launcher_sha256, register_worker),
    }


def _missing_run_module(
    module: str,
    exc: SqxCustomProjectTopologyError,
    *,
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    record = _base_record(
        module,
        "run",
        sqx_home=sqx_home,
        trusted_launcher_sha256=trusted_launcher_sha256,
        register_worker=register_worker,
    )
    record["project"] = module
    record["source_relative_path"] = f"{SQX_CUSTOM_PROJECTS_RELATIVE_ROOT}/{module}/project.cfx"
    record["reason_code"] = exc.code
    record["detail"] = exc.detail
    return record


def _read_run_module(
    home: Path,
    module: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    try:
        topology = read_sqx_custom_project_topology(home, module)
    except SqxCustomProjectTopologyError as exc:
        return _missing_run_module(
            module,
            exc,
            sqx_home=home,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
        )
    item = _catalog_item_from_topology(home, topology)
    record = _base_record(
        module,
        "run",
        sqx_home=home,
        trusted_launcher_sha256=trusted_launcher_sha256,
        register_worker=register_worker,
    )
    record.update(
        {
            "status": "ready",
            "reason_code": None,
            "detail": (
                f"Native {module} archive from verified user/projects. "
                "Full settings and Results bind this module's project.cfx and databanks."
            ),
            "project": module,
            "source_relative_path": item["source_relative_path"],
            "archive_sha256": item["archive_sha256"],
            "task_count": item["task_count"],
            "databank_count": item["databank_count"],
            "strategy_count": item["strategy_count"],
        }
    )
    return record


def _inspect_archive(home: Path, folder: str) -> Path | None:
    try:
        path = _resolved_project_archive(home, folder)
    except SqxCustomProjectTopologyError:
        return None
    return path if path.is_file() else None


def _read_inspect_module(
    home: Path,
    module: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    job = SQX_INSPECT_JOB[module]
    record = _base_record(
        module,
        "inspect",
        sqx_home=home,
        trusted_launcher_sha256=trusted_launcher_sha256,
        register_worker=register_worker,
    )
    for folder in SQX_INSPECT_FOLDERS[module]:
        archive = _inspect_archive(home, folder)
        if archive is None:
            continue
        record.update(
            {
                "reason_code": "native_module_editor_unwired",
                "detail": (
                    f"Native {module} archive is present at "
                    f"{SQX_CUSTOM_PROJECTS_RELATIVE_ROOT}/{folder}/project.cfx. "
                    f"This desktop does not invent a {job}."
                ),
                "project": folder,
                "source_relative_path": _project_relative_path(folder),
                "archive_sha256": sha256(archive.read_bytes()).hexdigest(),
            }
        )
        return record
    record.update(
        {
            "reason_code": "native_module_archive_missing",
            "detail": (
                f"Verified StrategyQuant X has no {module} archive under user/projects. "
                f"This desktop does not invent a {job}."
            ),
        }
    )
    return record


def read_sqx_run_module(
    sqx_home: Path | str | None,
    module: object,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    """Read one official SQX program-layout module from the verified runtime."""

    name = canonical_sqx_module_name(module)
    try:
        home = _verified_home(sqx_home)
    except SqxCustomProjectTopologyError as exc:
        raise SqxRunModuleError(exc.code, exc.detail) from exc
    if name in SQX_RUN_MODULE_NAMES:
        return _read_run_module(
            home,
            name,
            trusted_launcher_sha256=trusted_launcher_sha256,
            register_worker=register_worker,
        )
    return _read_inspect_module(
        home,
        name,
        trusted_launcher_sha256=trusted_launcher_sha256,
        register_worker=register_worker,
    )
