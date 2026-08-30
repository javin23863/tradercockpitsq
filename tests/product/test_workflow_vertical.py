from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.domain.specs import StrategySpecV1
from tradercockpit.sqx_custom_project import (
    SqxCustomProjectTask,
    SqxCustomProjectTopology,
)
from tradercockpit.storage import FileObjectStore
from tradercockpit.workflow import (
    WorkflowError,
    WorkflowPlanV1,
    WorkflowRunService,
    WorkflowTaskOutcome,
    WorkflowTaskV1,
    workflow_list_response,
    workflow_plan_from_sqx_topology,
    workflow_read_response,
    workflow_start_response,
)


class WorkflowVerticalTests(unittest.TestCase):
    def _plan(self):
        return WorkflowPlanV1(
            name="candidate-cycle",
            start_task_id="produce",
            max_steps=20,
            tasks=(
                WorkflowTaskV1(
                    "produce",
                    "action",
                    action="test.produce",
                    successors=("clear-scratch",),
                ),
                WorkflowTaskV1(
                    "clear-scratch",
                    "clear_databanks",
                    clear_databanks=("Scratch",),
                    successors=("decide",),
                    max_visits=2,
                ),
                WorkflowTaskV1(
                    "decide",
                    "action",
                    action="test.decide",
                    depends_on=("produce",),
                    successors=("finish", "clear-scratch"),
                    max_visits=2,
                ),
                WorkflowTaskV1("finish", "stop"),
            ),
        )

    def test_action_branch_loop_custody_and_durable_reopen(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            object_store = FileObjectStore(root)
            strategy = StrategySpecV1(
                semantic_schema="test.workflow-strategy.v1",
                semantics={"entry": "ema", "period": 20},
            )
            object_store.put(strategy)

            def produce(task, context):
                self.assertEqual(task.action, "test.produce")
                self.assertEqual(context["visit"], 1)
                return WorkflowTaskOutcome(
                    output_refs=(strategy.ref,),
                    databank_additions={
                        "Candidates": (strategy.ref,),
                        "Scratch": (strategy.ref,),
                    },
                    detail={"producer": "test.produce"},
                )

            def decide(task, context):
                return WorkflowTaskOutcome(
                    next_task_id="clear-scratch" if context["visit"] == 1 else "finish",
                    detail={"visit": context["visit"]},
                )

            service = WorkflowRunService(
                root,
                handlers={"test.produce": produce, "test.decide": decide},
            )
            completed = service.start(
                self._plan(),
                run_key="run-1",
                inputs={"strategyRef": "opaque"},
            )

            self.assertEqual(completed["status"], "completed")
            self.assertEqual(completed["step_count"], 6)
            self.assertEqual(completed["current_task_id"], None)
            self.assertEqual(completed["visits"]["clear-scratch"], 2)
            self.assertEqual(completed["visits"]["decide"], 2)
            self.assertEqual(completed["databanks"]["Candidates"], [str(strategy.ref)])
            self.assertNotIn("Scratch", completed["databanks"])
            self.assertEqual(completed["output_refs"], [str(strategy.ref)])
            self.assertEqual(
                [event["task_id"] for event in completed["events"]],
                ["produce", "clear-scratch", "decide", "clear-scratch", "decide", "finish"],
            )

            reopened = WorkflowRunService(root).read(completed["run_ref"])
            self.assertEqual(reopened, completed)

            list_status, catalog = workflow_list_response(root)
            self.assertEqual(list_status, 200)
            self.assertEqual(catalog["runs"], [completed])
            read_status, api_reopened = workflow_read_response(root, completed["run_ref"])
            self.assertEqual(read_status, 200)
            self.assertEqual(api_reopened, completed)

    def test_unstored_handler_output_fails_without_fabricating_custody(self):
        with TemporaryDirectory() as directory:
            orphan = StrategySpecV1(
                semantic_schema="test.workflow-strategy.v1",
                semantics={"entry": "rsi"},
            )
            plan = WorkflowPlanV1(
                name="orphan-output",
                start_task_id="produce",
                tasks=(
                    WorkflowTaskV1("produce", "action", action="test.orphan"),
                ),
            )
            service = WorkflowRunService(
                directory,
                handlers={
                    "test.orphan": lambda task, context: WorkflowTaskOutcome(
                        output_refs=(orphan.ref,)
                    )
                },
            )
            result = service.start(plan, run_key="run-1")
            self.assertEqual(result["status"], "failed")
            self.assertEqual(result["output_refs"], [])
            self.assertIn("task output", result["failure"]["code"])

    def test_unbounded_control_flow_is_stopped_by_task_visit_limit(self):
        with TemporaryDirectory() as directory:
            plan = WorkflowPlanV1(
                name="bounded-loop",
                start_task_id="again",
                max_steps=10,
                tasks=(
                    WorkflowTaskV1(
                        "again",
                        "goto",
                        goto_target="again",
                        max_visits=2,
                    ),
                ),
            )
            result = WorkflowRunService(directory).start(plan, run_key="run-1")
            self.assertEqual(result["status"], "failed")
            self.assertEqual(result["step_count"], 2)
            self.assertIn("max_visits", result["failure"]["detail"])

    def test_missing_dependency_fails_before_action_handler(self):
        with TemporaryDirectory() as directory:
            calls = []
            plan = WorkflowPlanV1(
                name="dependency-check",
                start_task_id="second",
                tasks=(
                    WorkflowTaskV1("first", "stop"),
                    WorkflowTaskV1(
                        "second",
                        "action",
                        action="test.should-not-run",
                        depends_on=("first",),
                    ),
                ),
            )
            result = WorkflowRunService(
                directory,
                handlers={
                    "test.should-not-run": lambda task, context: calls.append(task)
                },
            ).start(plan, run_key="run-1")
            self.assertEqual(result["status"], "failed")
            self.assertEqual(calls, [])
            self.assertIn("dependencies", result["failure"]["detail"])

    def test_durable_plan_identity_tampering_is_refused_on_read(self):
        with TemporaryDirectory() as directory:
            service = WorkflowRunService(directory)
            plan = WorkflowPlanV1(
                name="stop-only",
                start_task_id="finish",
                tasks=(WorkflowTaskV1("finish", "stop"),),
            )
            result = service.start(plan, run_key="run-1")
            run_ref = service.run_ref(plan, "run-1", {})
            path = service.runs._path(run_ref)
            state = service.runs.read(run_ref)
            state["plan"]["name"] = "changed-plan"
            from tradercockpit.domain.canonical import canonical_json_bytes
            path.write_bytes(canonical_json_bytes(state))
            with self.assertRaisesRegex(WorkflowError, "plan identity"):
                WorkflowRunService(directory).read(result["run_ref"])

    def test_sqx_topology_becomes_executable_graph_without_task_kind_enum_lock(self):
        topology = SqxCustomProjectTopology(
            project="Example",
            archive_path=Path("/evidence/project.cfx"),
            archive_sha256="a" * 64,
            internal_entries=("config.xml", "Build-Task1.xml", "ClearDatabanks-Task2.xml", "GoToTask-Task3.xml"),
            tasks=(
                SqxCustomProjectTask(1, "Build", "Build-Task1.xml"),
                SqxCustomProjectTask(
                    2,
                    "ClearDatabanks",
                    "ClearDatabanks-Task2.xml",
                    clear_databanks=("Results",),
                ),
                SqxCustomProjectTask(
                    3,
                    "GoToTask",
                    "GoToTask-Task3.xml",
                    goto_target_label="Build again",
                ),
            ),
        )
        plan = workflow_plan_from_sqx_topology(
            topology,
            goto_targets={"Build again": "task-1"},
            max_steps=12,
        )
        self.assertEqual(plan.source, "sqx-import")
        self.assertEqual(plan.source_ref, "sha256:" + "a" * 64)
        self.assertEqual(plan.tasks[0].action, "sqx.Build")
        self.assertEqual(plan.tasks[1].clear_databanks, ("Results",))
        self.assertEqual(plan.tasks[2].goto_target, "task-1")

    def test_sqx_goto_requires_explicit_target_identity_mapping(self):
        topology = SqxCustomProjectTopology(
            project="Example",
            archive_path=Path("/evidence/project.cfx"),
            archive_sha256="b" * 64,
            internal_entries=("config.xml", "GoToTask-Task1.xml"),
            tasks=(
                SqxCustomProjectTask(
                    1,
                    "GoToTask",
                    "GoToTask-Task1.xml",
                    goto_target_label="Unknown label",
                ),
            ),
        )
        with self.assertRaisesRegex(WorkflowError, "requires an explicit"):
            workflow_plan_from_sqx_topology(topology)

    def test_api_starts_real_workflow_and_rejects_unknown_fields(self):
        with TemporaryDirectory() as directory:
            plan = WorkflowPlanV1(
                name="api-stop",
                start_task_id="finish",
                tasks=(WorkflowTaskV1("finish", "stop"),),
            )
            status, result = workflow_start_response(
                directory,
                {"plan": plan.identity_payload(), "runKey": "api-1", "inputs": {}},
            )
            self.assertEqual(status, 201)
            self.assertEqual(result["status"], "completed")

            status, payload = workflow_start_response(
                directory,
                {"plan": plan.identity_payload(), "runKey": "api-2", "fake": True},
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")


if __name__ == "__main__":
    unittest.main()
