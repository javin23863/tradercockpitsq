"""Exact native Custom Project task XML as adjustable Full settings.

Tabs are the top-level Settings children in the saved task. This module does
not invent SQX MCP, default parameter sets, extra elements, or extra attributes.
Writes update only attributes or existing text on an existing element.
"""

from __future__ import annotations

from hashlib import sha256
from io import BytesIO
from pathlib import Path
import re
from xml.etree import ElementTree
from zipfile import ZipFile, ZIP_DEFLATED

from .sqx_custom_project import (
    SQX_BUILD,
    SQX_CUSTOM_PROJECT_CONFIG_ENTRY,
    SqxCustomProjectTopologyError,
    _local_name,
    _project_relative_path,
    _read_archive_snapshot,
    _resolved_project_archive,
    _verified_home,
    read_sqx_custom_project_topology,
    settings_sections,
)


SQX_CUSTOM_PROJECT_SETTINGS_SCHEMA = "tc.sqx-custom-project-settings.v1"
SQX_CUSTOM_PROJECT_SETTINGS_API_PATH = "/api/sqx-project-settings"
_PATH_STEP = re.compile(r"^([A-Za-z][A-Za-z0-9-]*)(?::([1-9][0-9]*))?$")


def _split_settings_path_step(step: str) -> tuple[str, int | None]:
    if not isinstance(step, str) or not step:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_path_invalid",
            "Settings path must use exact native element names from this task XML.",
        )
    match = _PATH_STEP.fullmatch(step)
    if match is None:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_path_invalid",
            "Settings path must use exact native element names from this task XML.",
        )
    occurrence = int(match.group(2)) if match.group(2) else None
    return match.group(1), occurrence


def _walk_path(root: ElementTree.Element, path: list[str]) -> ElementTree.Element:
    current = root
    for index, raw in enumerate(path):
        name, occurrence = _split_settings_path_step(raw)
        matches = [child for child in list(current) if _local_name(child.tag) == name]
        if occurrence is None:
            if len(matches) != 1:
                raise SqxCustomProjectTopologyError(
                    "custom_project_settings_path_missing",
                    f"Native settings path {path[: index + 1]!r} is not a unique element in this task.",
                )
            current = matches[0]
            continue
        if occurrence > len(matches):
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_path_missing",
                f"Native settings path {path[: index + 1]!r} is not a unique element in this task.",
            )
        current = matches[occurrence - 1]
    return current


def _apply_task_text(target: ElementTree.Element, label: str, text: object) -> None:
    if not isinstance(text, str):
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_value_invalid",
            "Settings text must be the exact native element string.",
        )
    current = (target.text or "").strip()
    if not current:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_text_missing",
            f"Native element {label!r} has no existing text. This desktop does not invent parameters.",
        )
    target.text = text


def _apply_attribute(target: ElementTree.Element, label: str, attribute: object, value: object) -> None:
    if not isinstance(attribute, str) or not attribute:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_attribute_invalid",
            "Settings attribute must be one existing native attribute name.",
        )
    if not isinstance(value, str):
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_value_invalid",
            "Settings value must be the exact native attribute string.",
        )
    if attribute not in target.attrib:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_attribute_missing",
            f"Native element {label!r} has no attribute {attribute!r}. This desktop does not invent parameters.",
        )
    current = target.attrib[attribute]
    if current in {"true", "false"} and value not in {"true", "false"}:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_value_invalid",
            f"Native attribute {attribute!r} is a boolean flag and accepts only true or false.",
        )
    target.attrib[attribute] = value


def update_custom_project_settings(
    sqx_home: Path | str | None,
    project: str,
    task_index: int,
    updates: list[dict[str, object]],
) -> dict[str, object]:
    """Write existing native attributes or existing text back into one saved task member."""

    if not isinstance(task_index, int) or task_index < 1:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_index_invalid",
            "Settings updates require one exact native task index.",
        )
    if not isinstance(updates, list) or not updates:
        raise SqxCustomProjectTopologyError(
            "custom_project_settings_updates_invalid",
            "Settings updates must be a non-empty list of existing attribute or text writes.",
        )

    topology = read_sqx_custom_project_topology(sqx_home, project)
    task = next((item for item in topology.tasks if item.native_task_index == task_index), None)
    if task is None:
        raise SqxCustomProjectTopologyError(
            "custom_project_task_missing",
            f"This project has no native task {task_index}.",
        )

    home = _verified_home(sqx_home)
    archive_path = _resolved_project_archive(home, project)
    snapshot = _read_archive_snapshot(archive_path)
    with ZipFile(BytesIO(snapshot)) as archive:
        try:
            payload = archive.read(task.entry_name)
        except KeyError as exc:
            raise SqxCustomProjectTopologyError(
                "custom_project_task_missing",
                f"SQX Custom Project is missing {task.entry_name}",
            ) from exc
        members = {info.filename: archive.read(info.filename) for info in archive.infolist()}

    root = ElementTree.fromstring(payload)
    config_root = None
    if SQX_CUSTOM_PROJECT_CONFIG_ENTRY in members:
        config_root = ElementTree.fromstring(members[SQX_CUSTOM_PROJECT_CONFIG_ENTRY])
    for item in updates:
        if not isinstance(item, dict):
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Each settings update must be an object.",
            )
        extra = set(item) - {"path", "attribute", "value", "target", "text"}
        if extra:
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Settings updates accept only path, attribute, value, text, and optional target.",
            )
        path = item.get("path")
        target_kind = item.get("target", "task")
        has_text = "text" in item
        has_attribute = "attribute" in item or "value" in item
        if has_text and has_attribute:
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Each settings update writes either an existing attribute or existing text, not both.",
            )
        if not has_text and not has_attribute:
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Each settings update must write an existing attribute or existing text.",
            )
        if target_kind not in {"task", "config"}:
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Settings target must be the task XML or the saved project config.",
            )
        if has_text and target_kind != "task":
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_updates_invalid",
                "Text writes apply to the saved task XML, not the project config.",
            )
        if target_kind == "config":
            if path is not None:
                raise SqxCustomProjectTopologyError(
                    "custom_project_settings_updates_invalid",
                    "Config Task updates use the selected task identity, not an invented path.",
                )
            if config_root is None:
                raise SqxCustomProjectTopologyError(
                    "custom_project_config_missing",
                    "SQX Custom Project is missing required config.xml",
                )
            matches = [
                element
                for element in config_root.iter()
                if _local_name(element.tag) == "Task"
                and element.attrib.get("taskXMLFile") == task.entry_name
            ]
            if len(matches) != 1:
                raise SqxCustomProjectTopologyError(
                    "custom_project_settings_path_missing",
                    f"config.xml has no unique Task for {task.entry_name}.",
                )
            _apply_attribute(matches[0], "Task", item.get("attribute"), item.get("value"))
            continue
        if not isinstance(path, list) or not path or any(not isinstance(part, str) or not part for part in path):
            raise SqxCustomProjectTopologyError(
                "custom_project_settings_path_invalid",
                "Settings path must be the exact native element chain.",
            )
        target = _walk_path(root, path)
        label = path[-1]
        if has_text:
            _apply_task_text(target, label, item.get("text"))
            continue
        _apply_attribute(target, label, item.get("attribute"), item.get("value"))

    updated = ElementTree.tostring(root, encoding="utf-8")
    members[task.entry_name] = updated
    if config_root is not None:
        members[SQX_CUSTOM_PROJECT_CONFIG_ENTRY] = ElementTree.tostring(config_root, encoding="utf-8")
    buffer = BytesIO()
    with ZipFile(buffer, "w", compression=ZIP_DEFLATED) as archive:
        for name, body in members.items():
            archive.writestr(name, body)
    written = buffer.getvalue()
    archive_path.write_bytes(written)

    return {
        "schema": SQX_CUSTOM_PROJECT_SETTINGS_SCHEMA,
        "source_build": SQX_BUILD,
        "project": project,
        "task_index": task_index,
        "entry_name": task.entry_name,
        "source_relative_path": _project_relative_path(project),
        "archive_sha256": sha256(written).hexdigest(),
        "previous_archive_sha256": topology.archive_sha256,
        "updated": len(updates),
        "settings": list(settings_sections(ElementTree.fromstring(updated))),
        "detail": (
            "Wrote existing native attributes or text into the saved Custom Project task. "
            "This desktop does not invent extra SQX parameters."
        ),
    }
