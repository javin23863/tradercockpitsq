"""TraderCockpit-owned workflow/task-graph model.

Custom/project workflows orchestrate canonical TraderCockpit capabilities; they do
not form a second run/evaluation pipeline.  Task graph semantics in this module
are Class C product behavior.  SQX project topology may be translated into this
model by the adjacent import adapter without claiming hidden SQX orchestration
internals.
"""

from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Any, Mapping

from tradercockpit.domain import ContentAddress
from tradercockpit.domain.canonical import canonical_json_bytes, content_address


WORKFLOW_PLAN_SCHEMA = "tc.workflow-plan.v1"
WORKFLOW_IMPLEMENTATION = "tradercockpit.workflow.v1"
WORKFLOW_TASK_KINDS = frozenset({"action", "clear_databanks", "goto", "stop"})
_TOKEN_RE = re.compile(r"^[A-Za-z][A-Za-z0-9._:-]{0,127}$")


class WorkflowError(ValueError):
    """Raised when workflow configuration or durable state is invalid."""


def _token(value: object, name: str) -> str:
    if not isinstance(value, str) or not _TOKEN_RE.fullmatch(value):
        raise WorkflowError(f"{name} must be a non-empty workflow token")
    return value


def _exact_int(value: object, name: str, *, minimum: int, maximum: int) -> int:
    if type(value) is not int or not minimum <= value <= maximum:
        raise WorkflowError(f"{name} must be an integer from {minimum} to {maximum}")
    return value


def _string_tuple(value: object, name: str) -> tuple[str, ...]:
    if value is None:
        return ()
    if isinstance(value, str) or not isinstance(value, (list, tuple)):
        raise WorkflowError(f"{name} must be an array of workflow tokens")
    return tuple(_token(item, f"{name}[{index}]") for index, item in enumerate(value))


def _settings(value: object) -> dict[str, Any]:
    if value is None:
        return {}
    if not isinstance(value, Mapping):
        raise WorkflowError("task settings must be an object")
    copied = dict(value)
    try:
        canonical_json_bytes(copied)
    except ValueError as exc:
        raise WorkflowError(f"task settings are not canonical: {exc}") from exc
    return copied


@dataclass(frozen=True, slots=True)
class WorkflowTaskV1:
    """One task in a bounded TraderCockpit workflow graph."""

    task_id: str
    kind: str
    action: str | None = None
    depends_on: tuple[str, ...] = ()
    successors: tuple[str, ...] = ()
    goto_target: str | None = None
    clear_databanks: tuple[str, ...] = ()
    settings: Mapping[str, Any] | None = None
    max_visits: int = 1

    def __post_init__(self) -> None:
        object.__setattr__(self, "task_id", _token(self.task_id, "task_id"))
        if self.kind not in WORKFLOW_TASK_KINDS:
            raise WorkflowError(f"unsupported workflow task kind: {self.kind!r}")
        object.__setattr__(self, "depends_on", _string_tuple(self.depends_on, "depends_on"))
        object.__setattr__(self, "successors", _string_tuple(self.successors, "successors"))
        object.__setattr__(
            self,
            "clear_databanks",
            _string_tuple(self.clear_databanks, "clear_databanks"),
        )
        object.__setattr__(self, "settings", _settings(self.settings))
        object.__setattr__(
            self,
            "max_visits",
            _exact_int(self.max_visits, "max_visits", minimum=1, maximum=10000),
        )

        if self.kind == "action":
            object.__setattr__(self, "action", _token(self.action, "action"))
            if self.goto_target is not None or self.clear_databanks:
                raise WorkflowError("action task cannot own goto/clear-databank fields")
        elif self.action is not None:
            raise WorkflowError(f"{self.kind} task cannot declare action")

        if self.kind == "clear_databanks":
            if not self.clear_databanks:
                raise WorkflowError("clear_databanks task requires at least one databank")
            if self.goto_target is not None:
                raise WorkflowError("clear_databanks task cannot declare goto_target")
        elif self.clear_databanks:
            raise WorkflowError(f"{self.kind} task cannot declare clear_databanks")

        if self.kind == "goto":
            object.__setattr__(self, "goto_target", _token(self.goto_target, "goto_target"))
            if self.successors:
                raise WorkflowError("goto task uses goto_target, not successors")
        elif self.goto_target is not None:
            raise WorkflowError(f"{self.kind} task cannot declare goto_target")

        if self.kind == "stop" and self.successors:
            raise WorkflowError("stop task cannot declare successors")

    def identity_payload(self) -> dict[str, Any]:
        return {
            "task_id": self.task_id,
            "kind": self.kind,
            "action": self.action,
            "depends_on": self.depends_on,
            "successors": self.successors,
            "goto_target": self.goto_target,
            "clear_databanks": self.clear_databanks,
            "settings": dict(self.settings or {}),
            "max_visits": self.max_visits,
        }

    @classmethod
    def from_payload(cls, value: Mapping[str, Any]) -> "WorkflowTaskV1":
        if not isinstance(value, Mapping):
            raise WorkflowError("workflow task must be an object")
        allowed = {
            "task_id", "kind", "action", "depends_on", "successors",
            "goto_target", "clear_databanks", "settings", "max_visits",
        }
        unknown = sorted(set(value) - allowed)
        if unknown:
            raise WorkflowError("unknown workflow task fields: " + ", ".join(unknown))
        if "task_id" not in value or "kind" not in value:
            raise WorkflowError("workflow task requires task_id and kind")
        return cls(
            task_id=value["task_id"],
            kind=value["kind"],
            action=value.get("action"),
            depends_on=_string_tuple(value.get("depends_on"), "depends_on"),
            successors=_string_tuple(value.get("successors"), "successors"),
            goto_target=value.get("goto_target"),
            clear_databanks=_string_tuple(value.get("clear_databanks"), "clear_databanks"),
            settings=value.get("settings"),
            max_visits=value.get("max_visits", 1),
        )


@dataclass(frozen=True, slots=True)
class WorkflowPlanV1:
    """Immutable executable graph definition with deterministic identity."""

    name: str
    tasks: tuple[WorkflowTaskV1, ...]
    start_task_id: str
    max_steps: int = 1000
    source: str = "tradercockpit"
    source_ref: str | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "name", _token(self.name, "name"))
        if not isinstance(self.tasks, tuple):
            object.__setattr__(self, "tasks", tuple(self.tasks))
        if not self.tasks or any(not isinstance(task, WorkflowTaskV1) for task in self.tasks):
            raise WorkflowError("workflow requires one or more WorkflowTaskV1 tasks")
        object.__setattr__(self, "start_task_id", _token(self.start_task_id, "start_task_id"))
        object.__setattr__(
            self,
            "max_steps",
            _exact_int(self.max_steps, "max_steps", minimum=1, maximum=10000),
        )
        if self.source not in {"tradercockpit", "sqx-import"}:
            raise WorkflowError("workflow source must be tradercockpit or sqx-import")
        if self.source == "sqx-import":
            if not isinstance(self.source_ref, str) or not self.source_ref.strip():
                raise WorkflowError("sqx-import workflow requires source_ref")
        elif self.source_ref is not None:
            raise WorkflowError("source_ref is reserved for sqx-import workflows")

        by_id = {task.task_id: task for task in self.tasks}
        if len(by_id) != len(self.tasks):
            raise WorkflowError("workflow task_id values must be unique")
        if self.start_task_id not in by_id:
            raise WorkflowError("start_task_id does not exist in workflow")
        for task in self.tasks:
            if task.task_id in task.depends_on:
                raise WorkflowError(f"task {task.task_id!r} cannot depend on itself")
            for dependency in task.depends_on:
                if dependency not in by_id:
                    raise WorkflowError(
                        f"task {task.task_id!r} depends on unknown task {dependency!r}"
                    )
            for successor in task.successors:
                if successor not in by_id:
                    raise WorkflowError(
                        f"task {task.task_id!r} has unknown successor {successor!r}"
                    )
            if task.goto_target is not None and task.goto_target not in by_id:
                raise WorkflowError(
                    f"task {task.task_id!r} has unknown goto target {task.goto_target!r}"
                )

    @property
    def ref(self) -> ContentAddress:
        return content_address("workflow-plan", 1, self.identity_payload())

    def identity_payload(self) -> dict[str, Any]:
        return {
            "schema": WORKFLOW_PLAN_SCHEMA,
            "name": self.name,
            "tasks": tuple(task.identity_payload() for task in self.tasks),
            "start_task_id": self.start_task_id,
            "max_steps": self.max_steps,
            "source": self.source,
            "source_ref": self.source_ref,
        }

    def task(self, task_id: str) -> WorkflowTaskV1:
        for task in self.tasks:
            if task.task_id == task_id:
                return task
        raise WorkflowError(f"workflow task not found: {task_id}")

    def sequential_successor(self, task_id: str) -> str | None:
        for index, task in enumerate(self.tasks):
            if task.task_id == task_id:
                return self.tasks[index + 1].task_id if index + 1 < len(self.tasks) else None
        raise WorkflowError(f"workflow task not found: {task_id}")

    @classmethod
    def from_payload(cls, value: Mapping[str, Any]) -> "WorkflowPlanV1":
        if not isinstance(value, Mapping):
            raise WorkflowError("workflow plan must be an object")
        allowed = {"schema", "name", "tasks", "start_task_id", "max_steps", "source", "source_ref"}
        unknown = sorted(set(value) - allowed)
        if unknown:
            raise WorkflowError("unknown workflow plan fields: " + ", ".join(unknown))
        if value.get("schema", WORKFLOW_PLAN_SCHEMA) != WORKFLOW_PLAN_SCHEMA:
            raise WorkflowError("workflow plan schema is not supported")
        raw_tasks = value.get("tasks")
        if isinstance(raw_tasks, (str, bytes)) or not isinstance(raw_tasks, (list, tuple)):
            raise WorkflowError("workflow plan tasks must be an array")
        return cls(
            name=value.get("name"),
            tasks=tuple(WorkflowTaskV1.from_payload(task) for task in raw_tasks),
            start_task_id=value.get("start_task_id"),
            max_steps=value.get("max_steps", 1000),
            source=value.get("source", "tradercockpit"),
            source_ref=value.get("source_ref"),
        )


@dataclass(frozen=True, slots=True)
class WorkflowTaskOutcome:
    """Truthful output returned by one canonical capability handler."""

    output_refs: tuple[ContentAddress, ...] = ()
    databank_additions: Mapping[str, tuple[ContentAddress, ...]] | None = None
    next_task_id: str | None = None
    detail: Mapping[str, Any] | None = None

    def __post_init__(self) -> None:
        if not isinstance(self.output_refs, tuple):
            object.__setattr__(self, "output_refs", tuple(self.output_refs))
        if any(not isinstance(ref, ContentAddress) for ref in self.output_refs):
            raise WorkflowError("task output_refs must contain ContentAddress values")
        additions: dict[str, tuple[ContentAddress, ...]] = {}
        for name, refs in dict(self.databank_additions or {}).items():
            key = _token(name, "databank name")
            values = refs if isinstance(refs, tuple) else tuple(refs)
            if any(not isinstance(ref, ContentAddress) for ref in values):
                raise WorkflowError("databank additions must contain ContentAddress values")
            additions[key] = values
        object.__setattr__(self, "databank_additions", additions)
        if self.next_task_id is not None:
            object.__setattr__(self, "next_task_id", _token(self.next_task_id, "next_task_id"))
        detail = dict(self.detail or {})
        try:
            canonical_json_bytes(detail)
        except ValueError as exc:
            raise WorkflowError(f"task outcome detail is not canonical: {exc}") from exc
        object.__setattr__(self, "detail", detail)
