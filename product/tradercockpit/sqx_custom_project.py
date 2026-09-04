"""Read-only custody for native StrategyQuant X Custom Project task topology.

Behavioral authority is the retained SQX 144.2953 saved-project archive. Native
numbered task identities are preserved generically instead of treating one
observed project's task set as a closed enum. Extra task semantics are extracted
only where retained XML evidence establishes a field contract. Task execution is native: start/stop go through the trusted StrategyQuant X
launcher. This module does not infer hidden orchestration behavior.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import re
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from .sqx_custom_project_launch import (
    SQX_CUSTOM_PROJECT_PROGRESS_SCHEMA,
    SqxCustomProjectLaunchError,
    launch_custom_project,
    launch_readiness,
    custom_project_worker_label,
    read_producer_log_lines,
)
from .sqx_outputs import SqxOutputError, inspect_sqx_output_bytes
from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_CUSTOM_PROJECT_TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1"
SQX_CUSTOM_PROJECTS_CATALOG_SCHEMA = "tc.sqx-custom-projects.v1"
SQX_CUSTOM_PROJECT_CONTROL_SCHEMA = "tc.sqx-custom-project-control.v1"
SQX_CUSTOM_PROJECT_RESULTS_SCHEMA = "tc.sqx-custom-project-results.v1"
SQX_CUSTOM_PROJECTS_API_PATH = "/api/sqx-projects"
SQX_CUSTOM_PROJECT_CONTROL_API_PATH = "/api/sqx-project-control"
SQX_CUSTOM_PROJECT_RESULTS_API_PATH = "/api/sqx-project-results"
SQX_CUSTOM_PROJECTS_RELATIVE_ROOT = "user/projects"
SQX_CUSTOM_PROJECT_CONFIG_ENTRY = "config.xml"
SQX_CUSTOM_PROJECT_TYPED_TASK_KINDS = frozenset({"ClearDatabanks", "GoToTask"})
SQX_CUSTOM_PROJECT_MODULE_NAMES = frozenset(
    {
        "Builder",
        "Retester",
        "Optimizer",
        "PortfolioMaster",
        "Portfolio Master",
        "PortfolioComposer",
        "Portfolio Composer",
        "DataManager",
        "Data Manager",
        "AlgoWizard",
    }
)
SQX_CUSTOM_PROJECT_OBSERVED_TASK_KINDS = frozenset(
    {
        "Build",
        "Retest",
        "ClearDatabanks",
        "GoToTask",
        "Optimize",
        "AutomaticPortfolioBuilder",
    }
)
SQX_CUSTOM_PROJECT_CONTROL_ACTIONS = frozenset({"run_project", "stop_project"})
_TASK_ENTRY_PATTERN = re.compile(
    r"^(?P<kind>[A-Za-z][A-Za-z0-9]*)-Task(?P<index>[1-9][0-9]*)\.xml$"
)


class SqxCustomProjectTopologyError(RuntimeError):
    """Raised when saved-project topology cannot be read without inference."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


class SqxCustomProjectControlError(RuntimeError):
    """Raised when Custom Project start/stop cannot use the native launcher."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class SqxCustomProjectCrossCheck:
    name: str
    use: bool | None


@dataclass(frozen=True, slots=True)
class SqxCustomProjectSetup:
    engine: str | None = None
    symbol: str | None = None
    timeframe: str | None = None
    date_from: str | None = None
    date_to: str | None = None
    generation_type: str | None = None
    money_management_type: str | None = None
    money_management_size: str | None = None
    cross_checks_use: bool | None = None
    cross_checks: tuple[SqxCustomProjectCrossCheck, ...] = ()
    source_member: str | None = None


@dataclass(frozen=True, slots=True)
class SqxCustomProjectTask:
    native_task_index: int
    kind: str
    entry_name: str
    clear_databanks: tuple[str, ...] = ()
    goto_target_label: str | None = None
    name: str | None = None
    active: bool | None = None
    setup: SqxCustomProjectSetup | None = None
    settings: tuple[object, ...] = ()


@dataclass(frozen=True, slots=True)
class SqxCustomProjectTopology:
    project: str
    archive_path: Path
    archive_sha256: str
    internal_entries: tuple[str, ...]
    tasks: tuple[SqxCustomProjectTask, ...]
    native_setup: SqxCustomProjectSetup | None = None
    source_build: str = SQX_BUILD


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _child_path_steps(element: ElementTree.Element) -> list[tuple[ElementTree.Element, str]]:
    children = list(element)
    counts: dict[str, int] = {}
    for child in children:
        tag = _local_name(child.tag)
        counts[tag] = counts.get(tag, 0) + 1
    seen: dict[str, int] = {}
    steps: list[tuple[ElementTree.Element, str]] = []
    for child in children:
        tag = _local_name(child.tag)
        seen[tag] = seen.get(tag, 0) + 1
        step = f"{tag}:{seen[tag]}" if counts[tag] > 1 else tag
        steps.append((child, step))
    return steps


def xml_node(
    element: ElementTree.Element,
    path: tuple[str, ...] = (),
    step: str | None = None,
) -> dict[str, object]:
    name = _local_name(element.tag)
    current = (*path, step or name)
    text = (element.text or "").strip() or None
    payload: dict[str, object] = {
        "tag": name,
        "path": list(current),
        "attributes": {str(key): str(value) for key, value in element.attrib.items()},
        "text": text,
        "children": [xml_node(child, current, child_step) for child, child_step in _child_path_steps(element)],
    }
    if name == "Condition":
        from .research_verdicts import native_condition_display_row

        display = native_condition_display_row(payload)
        if display is not None:
            payload["display"] = display
    return payload


def settings_sections(root: ElementTree.Element) -> tuple[dict[str, object], ...]:
    if _local_name(root.tag) == "Settings":
        return tuple(xml_node(child, (), child_step) for child, child_step in _child_path_steps(root))
    nested = _child_named(root, "Settings")
    if nested is not None:
        return tuple(
            xml_node(child, ("Settings",), child_step)
            for child, child_step in _child_path_steps(nested)
        )
    return tuple(xml_node(child, (), child_step) for child, child_step in _child_path_steps(root))


def _first_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    for element in root.iter():
        if _local_name(element.tag) == name:
            return element
    return None


def _child_named(root: ElementTree.Element | None, name: str) -> ElementTree.Element | None:
    if root is None:
        return None
    return next((child for child in root if _local_name(child.tag) == name), None)


def _optional_text(value: str | None) -> str | None:
    if not isinstance(value, str) or not value.strip():
        return None
    return value.strip()


def _optional_bool(value: str | None) -> bool | None:
    if not isinstance(value, str) or not value.strip():
        return None
    lowered = value.strip().lower()
    if lowered == "true":
        return True
    if lowered == "false":
        return False
    return None


def _task_config_bindings(config_root: ElementTree.Element) -> dict[str, tuple[str | None, bool | None]]:
    bindings: dict[str, tuple[str | None, bool | None]] = {}
    for element in config_root.iter():
        if _local_name(element.tag) != "Task":
            continue
        entry_name = _optional_text(element.attrib.get("taskXMLFile") or element.attrib.get("file"))
        if not entry_name:
            continue
        bindings[entry_name] = (
            _optional_text(element.attrib.get("name")),
            _optional_bool(element.attrib.get("active")),
        )
    return bindings


def _setup_from_task_xml(root: ElementTree.Element, entry_name: str) -> SqxCustomProjectSetup | None:
    data = _first_named(root, "Data")
    setups = _child_named(data, "Setups")
    setup_elements = (
        [child for child in setups if _local_name(child.tag) == "Setup"]
        if setups is not None
        else []
    )
    setup = setup_elements[0] if len(setup_elements) == 1 else _first_named(root, "Setup")
    chart = _child_named(setup, "Chart") if setup is not None else _first_named(root, "Chart")
    money = _first_named(root, "MoneyManagement")
    cross_checks_root = _first_named(root, "CrossChecks")
    what_to_build = _first_named(root, "WhatToBuild")
    build_mode = _child_named(what_to_build, "BuildMode") if what_to_build is not None else _first_named(root, "BuildMode")
    cross_checks = tuple(
        SqxCustomProjectCrossCheck(
            name=_local_name(child.tag),
            use=_optional_bool(child.attrib.get("use")),
        )
        for child in (list(cross_checks_root) if cross_checks_root is not None else [])
        if _local_name(child.tag)
    )
    record = SqxCustomProjectSetup(
        engine=_optional_text(setup.attrib.get("engine") if setup is not None else None),
        symbol=_optional_text(chart.attrib.get("symbol") if chart is not None else None),
        timeframe=_optional_text(chart.attrib.get("timeframe") if chart is not None else None),
        date_from=_optional_text(setup.attrib.get("dateFrom") if setup is not None else None),
        date_to=_optional_text(setup.attrib.get("dateTo") if setup is not None else None),
        generation_type=_optional_text(build_mode.attrib.get("generationType") if build_mode is not None else None),
        money_management_type=_optional_text(
            money.attrib.get("type") or money.attrib.get("method") if money is not None else None
        ),
        money_management_size=_optional_text(
            money.attrib.get("size") or money.attrib.get("lots") if money is not None else None
        ),
        cross_checks_use=_optional_bool(cross_checks_root.attrib.get("use") if cross_checks_root is not None else None),
        cross_checks=cross_checks,
        source_member=entry_name,
    )
    if not any(
        (
            record.engine,
            record.symbol,
            record.timeframe,
            record.date_from,
            record.date_to,
            record.generation_type,
            record.money_management_type,
            record.money_management_size,
            record.cross_checks_use is not None,
            record.cross_checks,
        )
    ):
        return None
    return record


def _setup_record(setup: SqxCustomProjectSetup | None) -> dict[str, object] | None:
    if setup is None:
        return None
    return {
        "engine": setup.engine,
        "symbol": setup.symbol,
        "timeframe": setup.timeframe,
        "date_from": setup.date_from,
        "date_to": setup.date_to,
        "generation_type": setup.generation_type,
        "money_management_type": setup.money_management_type,
        "money_management_size": setup.money_management_size,
        "cross_checks_use": setup.cross_checks_use,
        "cross_checks": [{"name": item.name, "use": item.use} for item in setup.cross_checks],
        "source_member": setup.source_member,
    }


def _parse_xml(payload: bytes, entry_name: str) -> ElementTree.Element:
    try:
        return ElementTree.fromstring(payload)
    except ElementTree.ParseError as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_xml_invalid",
            f"SQX Custom Project entry {entry_name!r} is not valid XML",
        ) from exc


def _project_relative_path(project: str) -> str:
    if (
        not isinstance(project, str)
        or not project.strip()
        or project != project.strip()
        or project in {".", ".."}
        or "/" in project
        or "\\" in project
        or "\x00" in project
    ):
        raise SqxCustomProjectTopologyError(
            "custom_project_name_invalid",
            "SQX Custom Project name must be one exact direct user/projects child",
        )
    return f"{SQX_CUSTOM_PROJECTS_RELATIVE_ROOT}/{project}/project.cfx"


def _resolved_project_archive(home: Path, project: str) -> Path:
    """Resolve the logical direct child without permitting symlink/junction escape."""

    relative_path = _project_relative_path(project)
    projects_root = (home / SQX_CUSTOM_PROJECTS_RELATIVE_ROOT).resolve()
    try:
        projects_root.relative_to(home)
    except ValueError as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX user/projects root resolves outside the verified runtime",
        ) from exc

    candidate = home / relative_path
    try:
        resolved = candidate.resolve()
    except (OSError, RuntimeError) as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_unreadable",
            f"SQX Custom Project path could not be resolved: {candidate}",
        ) from exc

    if (
        resolved.name != "project.cfx"
        or resolved.parent.name != project
        or resolved.parent.parent != projects_root
    ):
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX Custom Project resolves outside its exact direct user/projects child",
        )
    return resolved


def _read_archive_snapshot(path: Path) -> bytes:
    if not path.is_file():
        raise SqxCustomProjectTopologyError(
            "custom_project_missing",
            f"SQX Custom Project is missing: {path}",
        )
    try:
        return path.read_bytes()
    except OSError as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_unreadable",
            f"SQX Custom Project could not be read: {path}",
        ) from exc


def _task_from_xml(
    *,
    kind: str,
    index: int,
    entry_name: str,
    root: ElementTree.Element,
    name: str | None = None,
    active: bool | None = None,
) -> SqxCustomProjectTask:
    """Preserve generic task identity and extract only source-established fields."""

    clear_databanks: tuple[str, ...] = ()
    goto_target_label: str | None = None
    setup = _setup_from_task_xml(root, entry_name) if kind in {"Build", "Retest", "Optimize"} else None

    if kind == "ClearDatabanks":
        sections = [item for item in root.iter() if _local_name(item.tag) == "ClearDatabanks"]
        if len(sections) != 1:
            raise SqxCustomProjectTopologyError(
                "custom_project_clear_databanks_ambiguous",
                f"{entry_name!r} must contain exactly one ClearDatabanks section",
            )
        names = tuple(
            item.attrib["name"]
            for item in sections[0].iter()
            if _local_name(item.tag) == "Databank" and item.attrib.get("name")
        )
        if not names:
            raise SqxCustomProjectTopologyError(
                "custom_project_clear_databanks_missing",
                f"{entry_name!r} contains no named Databank custody",
            )
        clear_databanks = names

    if kind == "GoToTask":
        targets = [item for item in root.iter() if _local_name(item.tag) == "GoToTask"]
        if len(targets) != 1 or not targets[0].attrib.get("task"):
            raise SqxCustomProjectTopologyError(
                "custom_project_goto_target_missing",
                f"{entry_name!r} must contain one named GoToTask target",
            )
        goto_target_label = targets[0].attrib["task"]

    return SqxCustomProjectTask(
        native_task_index=index,
        kind=kind,
        entry_name=entry_name,
        clear_databanks=clear_databanks,
        goto_target_label=goto_target_label,
        name=name,
        active=active,
        setup=setup,
        settings=settings_sections(root),
    )


def _read_topology(
    archive_snapshot: bytes,
) -> tuple[tuple[str, ...], tuple[SqxCustomProjectTask, ...]]:
    try:
        with ZipFile(BytesIO(archive_snapshot)) as archive:
            entries = tuple(info.filename for info in archive.infolist())
            if len(entries) != len(set(entries)):
                raise SqxCustomProjectTopologyError(
                    "custom_project_duplicate_entries",
                    "SQX Custom Project contains duplicate archive member names",
                )
            if SQX_CUSTOM_PROJECT_CONFIG_ENTRY not in entries:
                raise SqxCustomProjectTopologyError(
                    "custom_project_config_missing",
                    "SQX Custom Project is missing required config.xml",
                )

            config_root = _parse_xml(
                archive.read(SQX_CUSTOM_PROJECT_CONFIG_ENTRY),
                SQX_CUSTOM_PROJECT_CONFIG_ENTRY,
            )
            bindings = _task_config_bindings(config_root)

            by_index: dict[int, SqxCustomProjectTask] = {}
            for entry_name in entries:
                if entry_name == SQX_CUSTOM_PROJECT_CONFIG_ENTRY:
                    continue
                match = _TASK_ENTRY_PATTERN.fullmatch(entry_name)
                if match is None:
                    if "-Task" in entry_name and entry_name.endswith(".xml"):
                        raise SqxCustomProjectTopologyError(
                            "custom_project_task_identity_invalid",
                            f"SQX Custom Project task identity is malformed: {entry_name!r}",
                        )
                    continue

                kind = match.group("kind")
                index = int(match.group("index"))
                if index in by_index:
                    raise SqxCustomProjectTopologyError(
                        "custom_project_task_index_ambiguous",
                        f"SQX Custom Project contains multiple task entries for native index {index}",
                    )
                root = _parse_xml(archive.read(entry_name), entry_name)
                name, active = bindings.get(entry_name, (None, None))
                by_index[index] = _task_from_xml(
                    kind=kind,
                    index=index,
                    entry_name=entry_name,
                    root=root,
                    name=name,
                    active=active,
                )

            return entries, tuple(by_index[index] for index in sorted(by_index))
    except BadZipFile as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_archive_invalid",
            "SQX Custom Project project.cfx is not a readable native project archive",
        ) from exc


def _verified_home(sqx_home: Path | str | None) -> Path:
    try:
        return verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise SqxCustomProjectTopologyError(exc.code, str(exc.detail)) from exc


def _project_native_setup(tasks: tuple[SqxCustomProjectTask, ...]) -> SqxCustomProjectSetup | None:
    for kind in ("Build", "Retest", "Optimize"):
        for task in tasks:
            if task.kind == kind and task.setup is not None:
                return task.setup
    return None


def read_sqx_custom_project_topology(
    sqx_home: Path | str | None,
    project: str,
) -> SqxCustomProjectTopology:
    """Read numbered native task identities from one immutable project snapshot."""

    home = _verified_home(sqx_home)
    archive_path = _resolved_project_archive(home, project)
    archive_snapshot = _read_archive_snapshot(archive_path)
    entries, tasks = _read_topology(archive_snapshot)
    return SqxCustomProjectTopology(
        project=project,
        archive_path=archive_path,
        archive_sha256=sha256(archive_snapshot).hexdigest(),
        internal_entries=entries,
        tasks=tasks,
        native_setup=_project_native_setup(tasks),
    )


def _task_record(task: SqxCustomProjectTask) -> dict[str, object]:
    return {
        "native_task_index": task.native_task_index,
        "kind": task.kind,
        "entry_name": task.entry_name,
        "name": task.name,
        "active": task.active,
        "clear_databanks": list(task.clear_databanks),
        "goto_target_label": task.goto_target_label,
        "setup": _setup_record(task.setup),
        "settings": [dict(item) if isinstance(item, dict) else item for item in task.settings],
    }


def custom_project_control_record(
    sqx_home: Path | str | None = None,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    readiness = launch_readiness(sqx_home, trusted_launcher_sha256, register_worker)
    available = readiness["available"] is True
    return {
        "schema": SQX_CUSTOM_PROJECT_CONTROL_SCHEMA,
        "available": available,
        "reason_code": readiness["reason_code"],
        "detail": readiness["detail"],
        "endpoint_configured": True,
        "credential_configured": False,
        "native_action_map": {
            "run_project": "start",
            "stop_project": "stop",
        },
    }


def custom_project_topology_record(
    sqx_home: Path | str | None,
    project: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    """Return saved-project task topology as JSON-safe immutable custody."""

    topology = read_sqx_custom_project_topology(sqx_home, project)
    control = custom_project_control_record(sqx_home, trusted_launcher_sha256, register_worker)
    supported = control["available"] is True
    return {
        "schema": SQX_CUSTOM_PROJECT_TOPOLOGY_SCHEMA,
        "source_build": topology.source_build,
        "project": topology.project,
        "source_relative_path": _project_relative_path(topology.project),
        "archive_sha256": topology.archive_sha256,
        "internal_entries": list(topology.internal_entries),
        "tasks": [_task_record(task) for task in topology.tasks],
        "native_setup": _setup_record(topology.native_setup),
        "execution": {
            "supported": supported,
            "reason": "native_cli" if supported else "topology_custody_only",
            "control": control,
        },
    }


def _databank_name(value: str) -> str:
    if (
        not isinstance(value, str)
        or not value
        or value != value.strip()
        or "/" in value
        or "\\" in value
        or "\0" in value
        or value in {".", ".."}
    ):
        raise SqxCustomProjectTopologyError(
            "custom_project_databank_name_invalid",
            "SQX databank name must be one exact folder name",
        )
    return value


def _project_databanks_root(home: Path, project: str) -> Path:
    root = (home / SQX_CUSTOM_PROJECTS_RELATIVE_ROOT / project / "databanks").resolve()
    try:
        root.relative_to(home.resolve())
    except ValueError as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX project databanks resolve outside the verified runtime",
        ) from exc
    return root


def _count_project_artifacts(home: Path, project: str) -> tuple[int, int]:
    root = _project_databanks_root(home, project)
    if not root.is_dir():
        return 0, 0
    databanks = 0
    strategies = 0
    for child in root.iterdir():
        if not child.is_dir():
            continue
        try:
            _databank_name(child.name)
        except SqxCustomProjectTopologyError:
            continue
        databanks += 1
        strategies += sum(1 for path in child.glob("*.sqx") if path.is_file())
    return databanks, strategies


def _catalog_item_from_topology(home: Path, topology: SqxCustomProjectTopology) -> dict[str, object]:
    setup = topology.native_setup
    databank_count, strategy_count = _count_project_artifacts(home, topology.project)
    return {
        "name": topology.project,
        "status": "ready",
        "reason_code": None,
        "detail": None,
        "task_count": len(topology.tasks),
        "databank_count": databank_count,
        "strategy_count": strategy_count,
        "engine": setup.engine if setup is not None else None,
        "symbol": setup.symbol if setup is not None else None,
        "timeframe": setup.timeframe if setup is not None else None,
        "archive_sha256": topology.archive_sha256,
        "source_relative_path": _project_relative_path(topology.project),
    }


def _unresolved_catalog_item(project: str, exc: SqxCustomProjectTopologyError) -> dict[str, object]:
    return {
        "name": project,
        "status": "unresolved",
        "reason_code": exc.code,
        "detail": exc.detail,
        "task_count": None,
        "databank_count": None,
        "strategy_count": None,
        "engine": None,
        "symbol": None,
        "timeframe": None,
        "archive_sha256": None,
        "source_relative_path": _project_relative_path(project),
    }


def list_custom_projects(
    sqx_home: Path | str | None,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
) -> dict[str, object]:
    """List real Custom Project archives under the verified runtime."""

    home = _verified_home(sqx_home)
    projects_root = (home / SQX_CUSTOM_PROJECTS_RELATIVE_ROOT).resolve()
    try:
        projects_root.relative_to(home.resolve())
    except ValueError as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX user/projects root resolves outside the verified runtime",
        ) from exc

    items: list[dict[str, object]] = []
    if projects_root.is_dir():
        for child in sorted(projects_root.iterdir(), key=lambda path: path.name.casefold()):
            if not child.is_dir() or child.name in SQX_CUSTOM_PROJECT_MODULE_NAMES:
                continue
            try:
                _project_relative_path(child.name)
            except SqxCustomProjectTopologyError:
                continue
            try:
                topology = read_sqx_custom_project_topology(home, child.name)
            except SqxCustomProjectTopologyError as exc:
                if exc.code == "custom_project_missing":
                    continue
                items.append(_unresolved_catalog_item(child.name, exc))
                continue
            items.append(_catalog_item_from_topology(home, topology))

    return {
        "schema": SQX_CUSTOM_PROJECTS_CATALOG_SCHEMA,
        "source_build": SQX_BUILD,
        "status": "ready",
        "reason_code": None,
        "detail": (
            "Native Custom Project workflows from verified user/projects. "
            "Task execution stays native. There is no StrategyQuant X MCP."
        ),
        "projects": items,
        "control": custom_project_control_record(home, trusted_launcher_sha256, register_worker),
    }


def list_custom_project_results(
    sqx_home: Path | str | None,
    project: str | None = None,
) -> dict[str, object]:
    """List native databanks and .sqx archives for one or every saved Custom Project."""

    home = _verified_home(sqx_home)
    names: list[str]
    if project is None:
        names = [item["name"] for item in list_custom_projects(home)["projects"] if item["status"] == "ready"]
    else:
        read_sqx_custom_project_topology(home, project)
        names = [project]

    projects: list[dict[str, object]] = []
    for name in names:
        root = _project_databanks_root(home, name)
        databanks: list[dict[str, object]] = []
        if root.is_dir():
            for child in sorted(root.iterdir(), key=lambda path: path.name.casefold()):
                if not child.is_dir():
                    continue
                try:
                    bank = _databank_name(child.name)
                except SqxCustomProjectTopologyError:
                    continue
                strategies: list[dict[str, object]] = []
                for archive in sorted(child.glob("*.sqx"), key=lambda path: path.name.casefold()):
                    if not archive.is_file():
                        continue
                    relative = f"user/projects/{name}/databanks/{bank}/{archive.name}"
                    try:
                        snapshot = archive.read_bytes()
                        record = inspect_sqx_output_bytes(
                            snapshot,
                            archive_name=archive.name,
                            require_runtime_build=False,
                        )
                        record["relative_path"] = relative
                        strategies.append(record)
                    except (OSError, SqxOutputError) as exc:
                        code = getattr(exc, "code", "output_unreadable")
                        detail = getattr(exc, "detail", str(exc))
                        strategies.append(
                            {
                                "archive": archive.name,
                                "relative_path": relative,
                                "inspectable": False,
                                "reason_code": code,
                                "detail": detail,
                            }
                        )
                databanks.append(
                    {
                        "name": bank,
                        "strategy_count": len(strategies),
                        "strategies": strategies,
                    }
                )
        projects.append(
            {
                "name": name,
                "source_relative_path": _project_relative_path(name),
                "databank_count": len(databanks),
                "strategy_count": sum(int(item["strategy_count"]) for item in databanks),
                "databanks": databanks,
            }
        )

    return {
        "schema": SQX_CUSTOM_PROJECT_RESULTS_SCHEMA,
        "source_build": SQX_BUILD,
        "status": "ready",
        "reason_code": None,
        "detail": (
            "Native Custom Project databanks and strategy archives from the verified runtime. "
            "These are producer files, not a platform backtester."
        ),
        "project": project,
        "projects": projects,
        "databank_count": sum(int(item["databank_count"]) for item in projects),
        "strategy_count": sum(int(item["strategy_count"]) for item in projects),
    }


def custom_project_progress_record(
    sqx_home: Path | str | None,
    project: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
) -> dict[str, object]:
    """Stream producer-written logs and databank counts. Stats stay unknown until SQX writes them."""

    topology = read_sqx_custom_project_topology(sqx_home, project)
    home = _verified_home(sqx_home)
    databank_count, strategy_count = _count_project_artifacts(home, topology.project)
    control = custom_project_control_record(home, trusted_launcher_sha256, register_worker)
    label = custom_project_worker_label(topology.project)
    running = bool(callable(worker_is_active) and worker_is_active(label))
    try:
        log_lines = read_producer_log_lines(home, topology.project)
    except SqxCustomProjectLaunchError:
        log_lines = []
    return {
        "schema": SQX_CUSTOM_PROJECT_PROGRESS_SCHEMA,
        "source_build": SQX_BUILD,
        "project": topology.project,
        "source_relative_path": _project_relative_path(topology.project),
        "archive_sha256": topology.archive_sha256,
        "running": running,
        "worker_label": label,
        "generated": None,
        "rejected": None,
        "accepted": None,
        "rate": None,
        "databank_count": databank_count,
        "strategy_count": strategy_count,
        "log_lines": log_lines,
        "control": control,
        "detail": (
            "Live log lines come from producer files under the verified runtime. "
            "Generated, rejected, accepted, and rate stay unknown until StrategyQuant X "
            "writes those values."
        ),
    }


def custom_project_control(
    sqx_home: Path | str | None,
    project: str,
    action: str,
    *,
    trusted_launcher_sha256: str | None = None,
    register_worker: object | None = None,
    worker_is_active: object | None = None,
    process_factory: object | None = None,
    runner: object | None = None,
) -> dict[str, object]:
    """Native Custom Project start/stop through the trusted launcher. There is no SQX MCP."""

    if not isinstance(action, str) or action not in SQX_CUSTOM_PROJECT_CONTROL_ACTIONS:
        raise SqxCustomProjectControlError(
            "custom_project_action_invalid",
            "Custom Project control accepts only native start (run_project) or stop (stop_project).",
        )
    topology = read_sqx_custom_project_topology(sqx_home, project)
    kwargs: dict[str, object] = {
        "trusted_launcher_sha256": trusted_launcher_sha256,
        "project_relative_path": _project_relative_path(topology.project),
        "expected_project_sha256": topology.archive_sha256,
        "register_worker": register_worker,
        "worker_is_active": worker_is_active,
    }
    if process_factory is not None:
        kwargs["process_factory"] = process_factory
    if runner is not None:
        kwargs["runner"] = runner
    try:
        return launch_custom_project(sqx_home, topology.project, action, **kwargs)
    except SqxCustomProjectLaunchError as exc:
        raise SqxCustomProjectControlError(exc.code, exc.detail) from exc
