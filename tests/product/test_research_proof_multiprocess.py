from __future__ import annotations

import multiprocessing
from pathlib import Path
from queue import Empty
from tempfile import TemporaryDirectory
import unittest

import proof_browser_fixture_server as fixture
from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_proof import create_research_proof, list_current_research_proofs


def _create_proof_worker(data_root: str, source: dict[str, str], barrier, queue) -> None:
    store = FileResearchCustodyStore(Path(data_root))
    try:
        barrier.wait(timeout=30)
        proof = create_research_proof(
            store,
            idea_entity_id=source["idea_entity_id"],
            idea_revision=source["idea_revision"],
            historical_result_entity_id=source["historical_result_entity_id"],
            historical_result_revision=source["historical_result_revision"],
            validation_ref=source["validation_ref"],
        )
        queue.put(
            {
                "entity_id": proof["entity_id"],
                "revision": proof["revision"],
                "reused": proof["reused"],
            }
        )
    except BaseException as exc:  # pragma: no cover - surfaced through the parent assertion
        queue.put({"error": f"{type(exc).__name__}: {exc}"})
        raise


class ResearchProofMultiprocessTests(unittest.TestCase):
    def test_identical_proof_creation_is_serialized_across_processes(self) -> None:
        with TemporaryDirectory() as tmp:
            data_root = Path(tmp) / "data"
            store = FileResearchCustodyStore(data_root)
            idea = fixture._exact_idea(store)
            config_entity, config_revision, task_ref = fixture._persist_configuration(store)
            job_entity, job_revision = fixture._persist_job(store, config_entity, config_revision, task_ref)
            candidate_entity, candidate_revision, candidate_ref, candidate_info = fixture._persist_candidate(
                store,
                job_entity,
                job_revision,
                config_entity,
                config_revision,
            )
            (
                historical_entity,
                historical_revision,
                result_ref,
                result_info,
                project_ref,
                project_bytes,
                engine_ref,
                engine_bytes,
            ) = fixture._persist_historical(
                store,
                candidate_entity,
                candidate_revision,
                candidate_ref,
                candidate_info,
            )
            validation_ref = fixture._persist_robustness(
                store,
                historical_entity,
                historical_revision,
                result_ref,
                result_info,
                project_ref,
                project_bytes,
                engine_ref,
                engine_bytes,
            )
            source = {
                "idea_entity_id": str(idea["entity_id"]),
                "idea_revision": str(idea["revision"]),
                "historical_result_entity_id": str(historical_entity),
                "historical_result_revision": str(historical_revision),
                "validation_ref": str(validation_ref),
            }

            context = multiprocessing.get_context("spawn")
            process_count = 4
            barrier = context.Barrier(process_count)
            queue = context.Queue()
            processes = [
                context.Process(
                    target=_create_proof_worker,
                    args=(str(data_root), source, barrier, queue),
                )
                for _ in range(process_count)
            ]
            for process in processes:
                process.start()
            for process in processes:
                process.join(45)
                self.assertEqual(process.exitcode, 0)

            results = []
            for _ in range(process_count):
                try:
                    results.append(queue.get(timeout=10))
                except Empty as exc:
                    self.fail(f"missing child Proof creation result: {exc}")
            errors = [result["error"] for result in results if "error" in result]
            self.assertEqual(errors, [])
            self.assertEqual(len({result["entity_id"] for result in results}), 1)
            self.assertEqual(len({result["revision"] for result in results}), 1)
            self.assertEqual(sum(result["reused"] is False for result in results), 1)
            self.assertEqual(sum(result["reused"] is True for result in results), process_count - 1)

            catalog = list_current_research_proofs(FileResearchCustodyStore(data_root))
            self.assertEqual(len(catalog["proofs"]), 1)
            self.assertEqual(catalog["proofs"][0]["entity_id"], results[0]["entity_id"])


if __name__ == "__main__":
    unittest.main()
