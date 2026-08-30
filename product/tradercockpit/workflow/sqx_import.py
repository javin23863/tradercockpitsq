"""Translate reviewed SQX Custom Project topology into canonical workflows."""

from __future__ import annotations

from typing import Mapping

from tradercockpit.sqx_custom_project import SqxCustomProjectTopology

from .model import WorkflowError, WorkflowPlanV1, WorkflowTaskV1


SQX_WORKFLOW_ACTION_PREFIX = "sqx."


def workflow_plan_from_sqx_topology(
    topology: SqxCustomProjectTopology,
    *,
    goto_targets: Mapping[str, str] | None = None,
    max_steps: int = 1000,
) -> WorkflowPlanV1:
    """Translate source-visible topology without inventing hidden task behavior.

    Native Build/Retest/Optimize/etc. become action-dispatch tasks whose handler
    names preserve the native kind.  They execute only when the canonical product
    registers a real handler for that capability.  ClearDatabanks maps directly
    to workflow-owned databank membership clearing.  Native GoToTask proves a
    control-flow jump but the retained evidence does not prove target-label to
    task-index resolution, so that identity mapping is supplied explicitly by the
    importer rather than guessed.
    """

    if not isinstance(topology, SqxCustomProjectTopology):
        raise WorkflowError("topology must be SqxCustomProjectTopology")
    if not topology.tasks:
        raise WorkflowError("SQX project contains no executable numbered tasks")

    mapping = dict(goto_targets or {})
    task_ids = {
        task.native_task_index: f"task-{task.native_task_index}"
        for task in topology.tasks
    }
    known_ids = set(task_ids.values())
    translated: list[WorkflowTaskV1] = []

    for position, native in enumerate(topology.tasks):
        task_id = task_ids[native.native_task_index]
        next_task_id = (
            task_ids[topology.tasks[position + 1].native_task_index]
            if position + 1 < len(topology.tasks)
            else None
        )
        common = {
            "task_id": task_id,
            "settings": {
                "native_task_index": native.native_task_index,
                "native_kind": native.kind,
                "native_entry_name": native.entry_name,
            },
            # A bounded loop is useful product behavior for imported GoTo graphs;
            # the plan-wide max_steps cap remains the hard stop.
            "max_visits": 100,
        }

        if native.kind == "ClearDatabanks":
            translated.append(
                WorkflowTaskV1(
                    **common,
                    kind="clear_databanks",
                    clear_databanks=native.clear_databanks,
                    successors=(next_task_id,) if next_task_id else (),
                )
            )
            continue

        if native.kind == "GoToTask":
            label = native.goto_target_label
            if not label or label not in mapping:
                raise WorkflowError(
                    f"SQX GoToTask target label {label!r} requires an explicit task-id mapping"
                )
            target = mapping[label]
            if target not in known_ids:
                raise WorkflowError(
                    f"SQX GoToTask mapping for {label!r} references unknown task {target!r}"
                )
            translated.append(
                WorkflowTaskV1(
                    **common,
                    kind="goto",
                    goto_target=target,
                )
            )
            continue

        translated.append(
            WorkflowTaskV1(
                **common,
                kind="action",
                action=f"{SQX_WORKFLOW_ACTION_PREFIX}{native.kind}",
                successors=(next_task_id,) if next_task_id else (),
            )
        )

    return WorkflowPlanV1(
        name=f"sqx-{topology.archive_sha256[:16]}",
        tasks=tuple(translated),
        start_task_id=translated[0].task_id,
        max_steps=max_steps,
        source="sqx-import",
        source_ref=f"sha256:{topology.archive_sha256}",
    )
