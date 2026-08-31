"""Read-only custody for native StrategyQuant X Custom Project task topology.

Behavioral authority is the retained SQX 144.2953 saved-project archive. Native
numbered task identities are preserved generically instead of treating one
observed project's task set as a closed enum. Extra task semantics are extracted
only where retained XML evidence establishes a field contract. This module does
not execute tasks or infer hidden orchestration behavior.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import re
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

from .sqx_presets import SQX_BUILD, verified_sqx_home


SQX_CUSTOM_PROJECT_TOPOLOGY_SCHEMA = "tc.sqx-custom-project-topology.v1"
SQX_CUSTOM_PROJECTS_RELATIVE_ROOT = "user/projects"
SQX_CUSTOM_PROJECT_CONFIG_ENTRY = "config.xml"
# These are task kinds for which this module extracts additional XML semantics.
# Other canonically numbered native task kinds remain valid opaque topology.
SQX_CUSTOM_PROJECT_TYPED_TASK_KINDS = frozenset({"ClearDatabanks", "GoToTask"})
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
_TASK_ENTRY_PATTERN = re.compile(
    r"^(?P<kind>[A-Za-z][A-Za-z0-9]*)-Task(?P<index>[1-9][0-9]*)\.xml$"
)


class SqxCustomProjectTopologyError(RuntimeError):
    """Raised when saved-project topology cannot be read without inference."""

    def __init__(self, code: str, detail: str):
        super().__init__(detail)
        self.code = code
        self.detail = detail


@dataclass(frozen=True, slots=True)
class SqxCustomProjectTask:
    native_task_index: int
    kind: str
    entry_name: str
    clear_databanks: tuple[str, ...] = ()
    goto_target_label: str | None = None


@dataclass(frozen=True, slots=True)
class SqxCustomProjectTopology:
    project: str
    archive_path: Path
    archive_sha256: str
    internal_entries: tuple[str, ...]
    tasks: tuple[SqxCustomProjectTask, ...]
    source_build: str = SQX_BUILD


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


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

    # Lexical name validation alone is insufficient: a direct child can itself be
    # a symlink/junction. Require the resolved archive to remain the same named
    # direct project child and project.cfx within the verified projects root.
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
) -> SqxCustomProjectTask:
    """Preserve generic task identity and extract only source-established fields."""

    clear_databanks: tuple[str, ...] = ()
    goto_target_label: str | None = None

    if kind == "ClearDatabanks":
        sections = [
            item for item in root.iter() if _local_name(item.tag) == "ClearDatabanks"
        ]
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

            _parse_xml(
                archive.read(SQX_CUSTOM_PROJECT_CONFIG_ENTRY),
                SQX_CUSTOM_PROJECT_CONFIG_ENTRY,
            )

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
                by_index[index] = _task_from_xml(
                    kind=kind,
                    index=index,
                    entry_name=entry_name,
                    root=root,
                )

            # Empty task topology is valid: retained PortfolioComposer is a native
            # project.cfx containing config.xml with no numbered task entries.
            return entries, tuple(by_index[index] for index in sorted(by_index))
    except BadZipFile as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_archive_invalid",
            "SQX Custom Project project.cfx is not a readable native project archive",
        ) from exc


def read_sqx_custom_project_topology(
    sqx_home: Path | str | None,
    project: str,
) -> SqxCustomProjectTopology:
    """Read numbered native task identities from one immutable project snapshot."""

    home = verified_sqx_home(sqx_home)
    archive_path = _resolved_project_archive(home, project)
    archive_snapshot = _read_archive_snapshot(archive_path)
    entries, tasks = _read_topology(archive_snapshot)
    return SqxCustomProjectTopology(
        project=project,
        archive_path=archive_path,
        archive_sha256=sha256(archive_snapshot).hexdigest(),
        internal_entries=entries,
        tasks=tasks,
    )


def custom_project_topology_record(
    sqx_home: Path | str | None,
    project: str,
) -> dict[str, object]:
    """Return saved-project task topology as JSON-safe immutable custody."""

    topology = read_sqx_custom_project_topology(sqx_home, project)
    return {
        "schema": SQX_CUSTOM_PROJECT_TOPOLOGY_SCHEMA,
        "source_build": topology.source_build,
        "project": topology.project,
        "source_relative_path": _project_relative_path(topology.project),
        "archive_sha256": topology.archive_sha256,
        "internal_entries": list(topology.internal_entries),
        "tasks": [
            {
                "native_task_index": task.native_task_index,
                "kind": task.kind,
                "entry_name": task.entry_name,
                "clear_databanks": list(task.clear_databanks),
                "goto_target_label": task.goto_target_label,
            }
            for task in topology.tasks
        ],
        "execution": {
            "supported": False,
            "reason": "topology_custody_only",
        },
    }
