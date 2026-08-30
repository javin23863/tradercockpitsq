import tempfile
import unittest

from tradercockpit.domain import ContentAddress, RunLifecycleEventV1, SpecValidationError
from tradercockpit.storage import FileRunLifecycleStore, LifecycleStoreError


def ref(kind, char):
    return ContentAddress(kind, 1, char * 64)


class LifecycleStatusTests(unittest.TestCase):
    def setUp(self):
        self.run_ref = ref("backtest-run", "a")
        self.receipt_ref = ref("run-receipt", "b")
        self.result_ref = ref("result", "c")
        self.decision_ref = ref("validation-decision", "d")
        self.evidence_ref = ref("evidence-manifest", "e")

    def ready(self):
        return RunLifecycleEventV1(
            self.run_ref,
            "initial-001",
            "ready",
            "2025-01-02T00:00:00Z",
        )

    def test_ready_running_passed_chain_survives_reopen(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileRunLifecycleStore(tmp)
            ready = self.ready()
            self.assertEqual(store.publish(ready), ready.ref)
            running = RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "running",
                "2025-01-02T00:00:01Z",
                previous_event_ref=ready.ref,
                receipt_ref=self.receipt_ref,
            )
            self.assertEqual(store.publish(running), running.ref)
            passed = RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "passed",
                "2025-01-02T00:00:02Z",
                previous_event_ref=running.ref,
                receipt_ref=self.receipt_ref,
                result_ref=self.result_ref,
                decision_ref=self.decision_ref,
                evidence_manifest_ref=self.evidence_ref,
            )
            self.assertEqual(store.publish(passed), passed.ref)
            self.assertTrue(store.current(self.run_ref, "initial-001").terminal)

            reopened = FileRunLifecycleStore(tmp)
            current = reopened.current(self.run_ref, "initial-001")
            self.assertEqual(current.ref, passed.ref)
            self.assertEqual(current.status, "passed")

    def test_refused_is_terminal_and_cannot_claim_launch_artifacts(self):
        ready = self.ready()
        refused = RunLifecycleEventV1(
            self.run_ref,
            "initial-001",
            "refused",
            "2025-01-02T00:00:01Z",
            previous_event_ref=ready.ref,
            reason_code="preflight_refused",
        )
        with tempfile.TemporaryDirectory() as tmp:
            store = FileRunLifecycleStore(tmp)
            store.publish(ready)
            store.publish(refused)
            with self.assertRaisesRegex(LifecycleStoreError, "terminal"):
                store.publish(
                    RunLifecycleEventV1(
                        self.run_ref,
                        "initial-001",
                        "running",
                        "2025-01-02T00:00:02Z",
                        previous_event_ref=refused.ref,
                        receipt_ref=self.receipt_ref,
                    )
                )
        with self.assertRaisesRegex(SpecValidationError, "must not claim launch"):
            RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "refused",
                "2025-01-02T00:00:01Z",
                previous_event_ref=ready.ref,
                receipt_ref=self.receipt_ref,
                reason_code="preflight_refused",
            )

    def test_failed_event_can_preserve_partial_durable_prefix(self):
        ready = self.ready()
        running = RunLifecycleEventV1(
            self.run_ref,
            "initial-001",
            "running",
            "2025-01-02T00:00:01Z",
            previous_event_ref=ready.ref,
            receipt_ref=self.receipt_ref,
        )
        failed = RunLifecycleEventV1(
            self.run_ref,
            "initial-001",
            "failed",
            "2025-01-02T00:00:02Z",
            previous_event_ref=running.ref,
            receipt_ref=self.receipt_ref,
            result_ref=self.result_ref,
            reason_code="validation_error",
        )
        self.assertEqual(failed.result_ref, self.result_ref)
        with self.assertRaisesRegex(SpecValidationError, "must form"):
            RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "failed",
                "2025-01-02T00:00:02Z",
                previous_event_ref=running.ref,
                receipt_ref=self.receipt_ref,
                decision_ref=self.decision_ref,
                reason_code="validation_error",
            )

    def test_stale_previous_event_cannot_replace_current_head(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileRunLifecycleStore(tmp)
            ready = self.ready()
            store.publish(ready)
            running = RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "running",
                "2025-01-02T00:00:01Z",
                previous_event_ref=ready.ref,
                receipt_ref=self.receipt_ref,
            )
            store.publish(running)
            stale = RunLifecycleEventV1(
                self.run_ref,
                "initial-001",
                "failed",
                "2025-01-02T00:00:02Z",
                previous_event_ref=ready.ref,
                receipt_ref=self.receipt_ref,
                reason_code="producer_failed",
            )
            with self.assertRaisesRegex(LifecycleStoreError, "does not extend"):
                store.publish(stale)
            self.assertEqual(store.current(self.run_ref, "initial-001").ref, running.ref)

    def test_first_event_must_be_ready(self):
        ready = self.ready()
        running = RunLifecycleEventV1(
            self.run_ref,
            "initial-001",
            "running",
            "2025-01-02T00:00:01Z",
            previous_event_ref=ready.ref,
            receipt_ref=self.receipt_ref,
        )
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(LifecycleStoreError, "first lifecycle event"):
                FileRunLifecycleStore(tmp).publish(running)

    def test_noncanonical_or_tampered_head_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            store = FileRunLifecycleStore(tmp)
            ready = self.ready()
            store.publish(ready)
            head = store._head_path(self.run_ref, "initial-001")
            head.write_bytes(b'{"wire_schema": "tc.run-lifecycle-head.v1"}')
            with self.assertRaises(LifecycleStoreError):
                store.current(self.run_ref, "initial-001")


if __name__ == "__main__":
    unittest.main()
