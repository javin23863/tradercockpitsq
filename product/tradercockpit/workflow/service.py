"""Durable execution service for TraderCockpit workflow graphs."""

from __future__ import annotations

import os
from pathlib import Path
import tempfile
from typing import Any, Callable, Mapping

from tradercockpit.domain import ContentAddress
from tradercockpit.domain.canonical import canonical_json_bytes, canonical_json_loads, content_address
from tradercockpit.storage import ContentStoreError, FileObjectStore

from .model import (
    WORKFLOW_IMPLEMENTATION,
    WorkflowError,
    WorkflowPlanV1,
    WorkflowTaskOutcome,
    WorkflowTaskV1,
)


WORKFLOW_RUN_SCHEMA = "tc.workflow-run.v1"
WorkflowActionHandler = Callable[[WorkflowTaskV1, Mapping[str, Any]], WorkflowTaskOutcome]


class FileWorkflowRunStore:
    """Atomic mutable run-state catalog; immutable outputs stay in FileObjectStore."""

    def __init__(self, root: Path | str):
        self.root = Path(root).expanduser().resolve()
        self.runs_root = self.root / "workflow" / "runs"
        self.runs_root.mkdir(parents=True, exist_ok=True)

    def _path(self, run_ref: ContentAddress) -> Path:
        if not isinstance(run_ref, ContentAddress) or run_ref.kind != "workflow-run":
            raise WorkflowError("run_ref must reference workflow-run")
        return self.runs_root / f"{run_ref.sha256}.json"

    def write(self, run_ref: ContentAddress, state: Mapping[str, Any]) -> None:
        payload = canonical_json_bytes(state)
        target = self._path(run_ref)
        fd, temp_name = tempfile.mkstemp(prefix=f".{run_ref.sha256}.", suffix=".tmp", dir=target.parent)
        temp = Path(temp_name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(payload)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temp, target)
        finally:
            if temp.exists():
                temp.unlink()
        if target.read_bytes() != payload:
            raise WorkflowError("durable workflow state changed after write")

    def read(self, run_ref: ContentAddress) -> dict[str, Any]:
        try:
            payload = self._path(run_ref).read_bytes()
        except FileNotFoundError as exc:
            raise KeyError(run_ref) from exc
        except OSError as exc:
            raise WorkflowError("workflow run state could not be read") from exc
        value = canonical_json_loads(payload)
        if not isinstance(value, dict):
            raise WorkflowError("workflow run state must be an object")
        return value

    def list(self) -> tuple[dict[str, Any], ...]:
        found: list[dict[str, Any]] = []
        for path in sorted(self.runs_root.glob("*.json")):
            value = canonical_json_loads(path.read_bytes())
            if not isinstance(value, dict):
                raise WorkflowError(f"workflow run state is invalid: {path.name}")
            found.append(value)
        return tuple(found)


class WorkflowRunService:
    """Execute bounded workflows through registered canonical capability handlers."""

    def __init__(
        self,
        state_root: Path | str,
        *,
        handlers: Mapping[str, WorkflowActionHandler] | None = None,
    ) -> None:
        self.root = Path(state_root).expanduser().resolve()
        self.root.mkdir(parents=True, exist_ok=True)
        self.objects = FileObjectStore(self.root)
        self.runs = FileWorkflowRunStore(self.root)
        self.handlers = dict(handlers or {})
        for name, handler in self.handlers.items():
            if not isinstance(name, str) or not name or not callable(handler):
                raise WorkflowError("workflow handler registry is invalid")

    @staticmethod
    def run_ref(plan: WorkflowPlanV1, run_key: str, inputs: Mapping[str, Any]) -> ContentAddress:
        if not isinstance(plan, WorkflowPlanV1):
            raise WorkflowError("plan must be WorkflowPlanV1")
        if not isinstance(run_key, str) or not run_key.strip() or run_key != run_key.strip():
            raise WorkflowError("run_key must be a non-empty trimmed string")
        if not isinstance(inputs, Mapping):
            raise WorkflowError("workflow inputs must be an object")
        canonical_json_bytes(dict(inputs))
        return content_address(
            "workflow-run",
            1,
            {
                "implementation": WORKFLOW_IMPLEMENTATION,
                "plan_ref": str(plan.ref),
                "run_key": run_key,
                "inputs": dict(inputs),
            },
        )

    def start(
        self,
        plan: WorkflowPlanV1,
        *,
        run_key: str,
        inputs: Mapping[str, Any] | None = None,
    ) -> dict[str, Any]:
        inputs = dict(inputs or {})
        run_ref = self.run_ref(plan, run_key, inputs)
        try:
            existing = self.runs.read(run_ref)
        except KeyError:
            existing = None
        if existing is not None:
            return self._verified_read(run_ref, existing)

        state: dict[str, Any] = {
            "schema": WORKFLOW_RUN_SCHEMA,
            "implementation": WORKFLOW_IMPLEMENTATION,
            "run_ref": str(run_ref),
            "run_key": run_key,
            "plan_ref": str(plan.ref),
            "plan": plan.identity_payload(),
            "inputs": inputs,
            "status": "created",
            "current_task_id": plan.start_task_id,
            "step_count": 0,
            "visits": {},
            "completed_task_ids": [],
            "events": [],
            "databanks": {},
            "output_refs": [],
            "failure": None,
        }
        self.runs.write(run_ref, state)
        return self.resume(run_ref)

    def resume(self, run_ref: ContentAddress | str) -> dict[str, Any]:
        ref = ContentAddress.parse(run_ref) if isinstance(run_ref, str) else run_ref
        if not isinstance(ref, ContentAddress) or ref.kind != "workflow-run":
            raise WorkflowError("run_ref must reference workflow-run")
        state = self._verified_read(ref, self.runs.read(ref), read_model=False)
        if state["status"] in {"completed", "failed"}:
            return self._read_model(state)
        plan = WorkflowPlanV1.from_payload(state["plan"])
        state["status"] = "running"
        self.runs.write(ref, state)

        while state["status"] == "running":
            try:
                self._execute_current(plan, state)
            except (WorkflowError, ContentStoreError, KeyError, ValueError, TypeError) as exc:
                state["status"] = "failed"
                state["failure"] = {
                    "code": "workflow_task_failed",
                    "detail": str(exc),
                    "task_id": state.get("current_task_id"),
                }
            self.runs.write(ref, state)
        return self._read_model(state)

    def read(self, run_ref: ContentAddress | str) -> dict[str, Any]:
        ref = ContentAddress.parse(run_ref) if isinstance(run_ref, str) else run_ref
        if not isinstance(ref, ContentAddress) or ref.kind != "workflow-run":
            raise WorkflowError("run_ref must reference workflow-run")
        return self._verified_read(ref, self.runs.read(ref))

    def list_runs(self) -> tuple[dict[str, Any], ...]:
        values: list[dict[str, Any]] = []
        for state in self.runs.list():
            ref = ContentAddress.parse(state.get("run_ref", ""))
            values.append(self._verified_read(ref, state))
        return tuple(values)

    def _execute_current(self, plan: WorkflowPlanV1, state: dict[str, Any]) -> None:
        if state["step_count"] >= plan.max_steps:
            raise WorkflowError("workflow exceeded max_steps safety limit")
        task_id = state.get("current_task_id")
        if not isinstance(task_id, str):
            state["status"] = "completed"
            return
        task = plan.task(task_id)
        visits = dict(state["visits"])
        visit_count = int(visits.get(task.task_id, 0)) + 1
        if visit_count > task.max_visits:
            raise WorkflowError(f"task {task.task_id!r} exceeded max_visits")
        visits[task.task_id] = visit_count
        state["visits"] = visits

        completed = set(state["completed_task_ids"])
        missing = [dependency for dependency in task.depends_on if dependency not in completed]
        if missing:
            raise WorkflowError(
                f"task {task.task_id!r} dependencies are incomplete: {', '.join(missing)}"
            )

        before_databanks = {key: list(value) for key, value in state["databanks"].items()}
        outcome = WorkflowTaskOutcome()
        next_task_id: str | None

        if task.kind == "action":
            handler = self.handlers.get(task.action or "")
            if handler is None:
                raise WorkflowError(f"workflow action handler is not registered: {task.action}")
            context = {
                "run_ref": state["run_ref"],
                "plan_ref": state["plan_ref"],
                "run_key": state["run_key"],
                "inputs": dict(state["inputs"]),
                "databanks": before_databanks,
                "step_count": state["step_count"],
                "visit": visit_count,
            }
            outcome = handler(task, context)
            if not isinstance(outcome, WorkflowTaskOutcome):
                raise WorkflowError("workflow action handler must return WorkflowTaskOutcome")
            self._verify_outcome_custody(outcome)
            next_task_id = outcome.next_task_id
            if next_task_id is not None:
                if not task.successors or next_task_id not in task.successors:
                    raise WorkflowError(
                        f"handler selected undeclared successor {next_task_id!r} for {task.task_id!r}"
                    )
            elif task.successors:
                next_task_id = task.successors[0]
            else:
                next_task_id = plan.sequential_successor(task.task_id)
            self._apply_outcome(state, outcome)
        elif task.kind == "clear_databanks":
            databanks = {key: list(value) for key, value in state["databanks"].items()}
            for name in task.clear_databanks:
                databanks.pop(name, None)
            state["databanks"] = databanks
            next_task_id = task.successors[0] if task.successors else plan.sequential_successor(task.task_id)
        elif task.kind == "goto":
            next_task_id = task.goto_target
        else:
            next_task_id = None

        state["step_count"] += 1
        if task.task_id not in completed:
            state["completed_task_ids"].append(task.task_id)
        state["events"].append(
            {
                "step": state["step_count"],
                "task_id": task.task_id,
                "kind": task.kind,
                "action": task.action,
                "visit": visit_count,
                "output_refs": tuple(str(ref) for ref in outcome.output_refs),
                "detail": dict(outcome.detail or {}),
                "next_task_id": next_task_id,
            }
        )
        state["current_task_id"] = next_task_id
        if task.kind == "stop" or next_task_id is None:
            state["status"] = "completed"

    def _verify_outcome_custody(self, outcome: WorkflowTaskOutcome) -> None:
        refs = list(outcome.output_refs)
        for values in dict(outcome.databank_additions or {}).values():
            refs.extend(values)
        for ref in refs:
            self.objects.resolve(ref)

    def _apply_outcome(self, state: dict[str, Any], outcome: WorkflowTaskOutcome) -> None:
        outputs = list(state["output_refs"])
        for ref in outcome.output_refs:
            text = str(ref)
            if text not in outputs:
                outputs.append(text)
        state["output_refs"] = outputs

        databanks = {key: list(value) for key, value in state["databanks"].items()}
        for name, refs in dict(outcome.databank_additions or {}).items():
            bucket = databanks.setdefault(name, [])
            for ref in refs:
                text = str(ref)
                if text not in bucket:
                    bucket.append(text)
        state["databanks"] = databanks

    def _verified_read(
        self,
        run_ref: ContentAddress,
        state: Mapping[str, Any],
        *,
        read_model: bool = True,
    ) -> dict[str, Any]:
        if state.get("schema") != WORKFLOW_RUN_SCHEMA:
            raise WorkflowError("workflow run schema is not supported")
        if state.get("implementation") != WORKFLOW_IMPLEMENTATION:
            raise WorkflowError("workflow run implementation revision is not supported")
        if state.get("run_ref") != str(run_ref):
            raise WorkflowError("workflow run ref does not match durable path")
        plan = WorkflowPlanV1.from_payload(state.get("plan", {}))
        if state.get("plan_ref") != str(plan.ref):
            raise WorkflowError("durable workflow plan identity is corrupt")
        expected_ref = self.run_ref(plan, state.get("run_key"), state.get("inputs", {}))
        if expected_ref != run_ref:
            raise WorkflowError("workflow run identity does not match plan/run inputs")
        if state.get("status") not in {"created", "running", "completed", "failed"}:
            raise WorkflowError("workflow run status is invalid")
        if type(state.get("step_count")) is not int or state["step_count"] < 0:
            raise WorkflowError("workflow step_count is invalid")
        if state["step_count"] > plan.max_steps:
            raise WorkflowError("workflow step_count exceeds plan max_steps")
        current = state.get("current_task_id")
        if current is not None:
            plan.task(current)
        for ref_text in state.get("output_refs", ()):
            self.objects.resolve(ContentAddress.parse(ref_text))
        for refs in dict(state.get("databanks", {})).values():
            for ref_text in refs:
                self.objects.resolve(ContentAddress.parse(ref_text))
        copied = dict(state)
        return self._read_model(copied) if read_model else copied

    @staticmethod
    def _read_model(state: Mapping[str, Any]) -> dict[str, Any]:
        return {
            "schema": WORKFLOW_RUN_SCHEMA,
            "run_ref": state["run_ref"],
            "plan_ref": state["plan_ref"],
            "run_key": state["run_key"],
            "plan": state["plan"],
            "inputs": state["inputs"],
            "status": state["status"],
            "current_task_id": state["current_task_id"],
            "step_count": state["step_count"],
            "visits": state["visits"],
            "completed_task_ids": list(state["completed_task_ids"]),
            "events": list(state["events"]),
            "databanks": {key: list(value) for key, value in state["databanks"].items()},
            "output_refs": list(state["output_refs"]),
            "failure": state["failure"],
        }
