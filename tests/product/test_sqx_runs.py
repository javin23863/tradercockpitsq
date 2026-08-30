from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tradercockpit.app_server import sqx_run_start_response
from tradercockpit.domain import CandidateSpecV1, ResultArtifactV1, StrategySpecV1
from tradercockpit.engine.evaluator import EvaluatorDescriptorV1
from tradercockpit.sqx_outputs import SQX_NATIVE_STRATEGY_SCHEMA
from tradercockpit.sqx_retester import SQX_RETESTER_RESULT_SCHEMA, sqx_retester_engine_build
from tradercockpit.sqx_runs import SqxRunRequestError, start_sqx_native_run
from tradercockpit.storage import FileObjectStore, FileRunLifecycleStore


class FixtureEvaluator:
    @property
    def descriptor(self):
        build = sqx_retester_engine_build()
        return EvaluatorDescriptorV1(
            build.ref,
            (SQX_NATIVE_STRATEGY_SCHEMA,),
            SQX_RETESTER_RESULT_SCHEMA,
            False,
        )

    def validate_strategy(self, strategy):
        if strategy.semantic_schema != SQX_NATIVE_STRATEGY_SCHEMA:
            raise ValueError("wrong schema")

    def evaluate(self, inputs):
        return ResultArtifactV1(
            inputs.run.ref,
            inputs.engine_build.ref,
            SQX_RETESTER_RESULT_SCHEMA,
            {"producer": {"exit_code": 0}, "result": {"archive_bytes": 456}},
        )


class SqxRunBindingTests(unittest.TestCase):
    def _candidate(self, root: Path) -> CandidateSpecV1:
        strategy = StrategySpecV1(
            SQX_NATIVE_STRATEGY_SCHEMA,
            {
                "producer": "strategyquant-x",
                "source_build": "144.2953",
                "source_project": "Builder",
                "source_databank": "Results",
                "archive_sha256": "a" * 64,
                "native_version": "144.2953",
                "strategy_entry_sha256": "b" * 64,
                "settings_entry_sha256": "c" * 64,
            },
        )
        candidate = CandidateSpecV1(strategy.ref, "sqx-builder")
        store = FileObjectStore(root)
        store.put(strategy)
        store.put(candidate)
        return candidate

    def _request(self, candidate: CandidateSpecV1) -> dict[str, object]:
        return {
            "candidate_ref": str(candidate.ref),
            "data": {
                "symbol": "ES",
                "timeframe": "H1",
                "source": "sqx-native",
                "dataset_revision": "dataset-sha-001",
                "timezone_name": "America/Chicago",
                "session_calendar": "CME",
                "start": "2020-01-01T00:00:00Z",
                "end": "2020-02-01T00:00:00Z",
                "adjustment_policy": "none",
            },
            "execution": {
                "starting_cash": "100000.00",
                "currency": "USD",
                "models": [
                    {
                        "kind": "fills",
                        "model": "sqx-native",
                        "parameters": {},
                    }
                ],
            },
            "random_seed": None,
        }

    def test_binding_persists_exact_specs_and_completed_run(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            candidate = self._candidate(root)
            payload = start_sqx_native_run(
                None,
                root,
                self._request(candidate),
                evaluator_factory=lambda _home: FixtureEvaluator(),
                invocation_id_factory=lambda: "sqx-fixture-001",
                clock=lambda: datetime(2026, 8, 30, 12, 0, tzinfo=timezone.utc),
            )

            self.assertEqual(payload["schema"], "tc.sqx-native-run-start.v1")
            self.assertEqual(payload["status"], "completed")
            self.assertEqual(payload["invocation_id"], "sqx-fixture-001")
            self.assertFalse(payload["validation"]["available"])
            self.assertEqual(payload["inputs"]["candidate_ref"], str(candidate.ref))

            store = FileObjectStore(root)
            lifecycle = FileRunLifecycleStore(root)
            run = store.resolve(payload["run_ref"])
            self.assertEqual(run.candidate_ref, candidate.ref)
            self.assertEqual(store.resolve(run.data_ref).symbol, "ES")
            self.assertEqual(store.resolve(run.data_ref).dataset_revision, "dataset-sha-001")
            execution = store.resolve(run.execution_ref)
            self.assertEqual(execution.starting_cash, Decimal("100000.00"))
            self.assertEqual(execution.models[0].model, "sqx-native")
            self.assertEqual(run.engine_build_ref, sqx_retester_engine_build().ref)
            event = lifecycle.current(run.ref, "sqx-fixture-001")
            self.assertEqual(event.status, "completed")
            self.assertEqual(str(event.result_ref), payload["result_ref"])

    def test_missing_candidate_custody_is_rejected(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            candidate = self._candidate(root)
            request = self._request(candidate)
            request["candidate_ref"] = str(candidate.ref).replace("a", "b", 1)
            with self.assertRaises(SqxRunRequestError):
                start_sqx_native_run(
                    None,
                    root,
                    request,
                    evaluator_factory=lambda _home: FixtureEvaluator(),
                )

    def test_request_requires_explicit_data_and_execution_fields(self) -> None:
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            candidate = self._candidate(root)
            request = self._request(candidate)
            request["data"] = {"symbol": "ES"}
            status, payload = sqx_run_start_response(
                None,
                root,
                request,
                starter=lambda home, state, body: start_sqx_native_run(
                    home,
                    state,
                    body,
                    evaluator_factory=lambda _home: FixtureEvaluator(),
                ),
            )
            self.assertEqual(status, 400)
            self.assertEqual(payload["error"], "invalid_request")

    def test_server_response_preserves_completed_identity(self) -> None:
        expected = {
            "schema": "tc.sqx-native-run-start.v1",
            "status": "completed",
            "run_ref": "tc:backtest-run:v1:sha256:" + "a" * 64,
            "invocation_id": "sqx-123",
        }
        status, payload = sqx_run_start_response(
            None,
            None,
            {"candidate_ref": "opaque"},
            starter=lambda _home, _state, _request: expected,
        )
        self.assertEqual(status, 201)
        self.assertEqual(payload, expected)


if __name__ == "__main__":
    unittest.main()
