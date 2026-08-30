from dataclasses import dataclass
import unittest

from tradercockpit.builder.fresh_blood import (
    SQX_FRESH_BLOOD_SOURCE_PROVENANCE,
    SQX_MAX_CANDIDATES_PER_FINGERPRINT,
    SQX_WEAKEST_REPLACEMENT_MAX_PCT,
    FreshBloodError,
    plan_weakest_replacement,
    prune_similar_population,
)


@dataclass(frozen=True)
class Candidate:
    name: str
    fitness: float
    fingerprint: int


class BuilderFreshBloodTests(unittest.TestCase):
    def test_similarity_pruning_removes_zero_fitness_and_third_fingerprint_copy(self):
        population = (
            Candidate("a1", 3.0, 7),
            Candidate("zero", 0.0, 7),
            Candidate("a2", 2.0, 7),
            Candidate("a3", 1.0, 7),
            Candidate("b1", 5.0, 9),
            Candidate("b2", 4.0, 9),
        )

        result = prune_similar_population(
            population,
            fitness=lambda candidate: candidate.fitness,
            fingerprint=lambda candidate: candidate.fingerprint,
        )

        self.assertEqual(
            [candidate.name for candidate in result.retained],
            ["a1", "a2", "b1", "b2"],
        )
        self.assertEqual(result.removed_zero_fitness, 1)
        self.assertEqual(result.removed_excess_fingerprint, 1)
        self.assertEqual(result.removed_count, 2)

    def test_zero_fitness_candidate_does_not_consume_fingerprint_allowance(self):
        population = (
            Candidate("zero", 0.0, 7),
            Candidate("a1", 1.0, 7),
            Candidate("a2", 2.0, 7),
            Candidate("a3", 3.0, 7),
        )

        result = prune_similar_population(
            population,
            fitness=lambda candidate: candidate.fitness,
            fingerprint=lambda candidate: candidate.fingerprint,
        )
        self.assertEqual(
            [candidate.name for candidate in result.retained],
            ["a1", "a2"],
        )
        self.assertEqual(result.removed_zero_fitness, 1)
        self.assertEqual(result.removed_excess_fingerprint, 1)

    def test_similarity_pruning_requires_numeric_fitness_and_integer_fingerprint(self):
        candidate = Candidate("a", 1.0, 7)
        with self.assertRaisesRegex(FreshBloodError, "fitness callback"):
            prune_similar_population(
                (candidate,),
                fitness=lambda _: True,
                fingerprint=lambda item: item.fingerprint,
            )
        with self.assertRaisesRegex(FreshBloodError, "fingerprint callback"):
            prune_similar_population(
                (candidate,),
                fitness=lambda item: item.fitness,
                fingerprint=lambda _: True,
            )

    def test_displayed_ten_percent_every_two_generations_replaces_ten_of_one_hundred(self):
        plan = plan_weakest_replacement(
            population_size=100,
            current_population_size=100,
            current_generation=2,
            replace_weakest_pct=10,
            replace_every_generations=2,
        )
        self.assertTrue(plan.scheduled)
        self.assertEqual(plan.effective_replace_pct, 10)
        self.assertEqual(plan.target_fresh_count, 10)
        self.assertEqual(plan.already_missing_count, 0)
        self.assertEqual(plan.weakest_to_remove, 10)
        self.assertEqual(plan.refill_count, 10)

    def test_existing_population_gaps_are_credited_before_weakest_removal(self):
        plan = plan_weakest_replacement(
            population_size=100,
            current_population_size=95,
            current_generation=2,
            replace_weakest_pct=10,
            replace_every_generations=2,
        )
        self.assertEqual(plan.already_missing_count, 5)
        self.assertEqual(plan.target_fresh_count, 10)
        self.assertEqual(plan.weakest_to_remove, 5)
        self.assertEqual(plan.refill_count, 10)

        already_below_target = plan_weakest_replacement(
            population_size=100,
            current_population_size=85,
            current_generation=2,
            replace_weakest_pct=10,
            replace_every_generations=2,
        )
        self.assertEqual(already_below_target.already_missing_count, 15)
        self.assertEqual(already_below_target.weakest_to_remove, 0)
        self.assertEqual(already_below_target.refill_count, 15)

    def test_unscheduled_generation_does_not_remove_weakest_but_still_refills_gap(self):
        plan = plan_weakest_replacement(
            population_size=100,
            current_population_size=95,
            current_generation=1,
            replace_weakest_pct=10,
            replace_every_generations=2,
        )
        self.assertFalse(plan.scheduled)
        self.assertEqual(plan.target_fresh_count, 0)
        self.assertEqual(plan.weakest_to_remove, 0)
        self.assertEqual(plan.refill_count, 5)

    def test_scheduled_zero_percent_still_targets_one_candidate(self):
        plan = plan_weakest_replacement(
            population_size=4,
            current_population_size=4,
            current_generation=2,
            replace_weakest_pct=0,
            replace_every_generations=2,
        )
        self.assertEqual(plan.target_fresh_count, 1)
        self.assertEqual(plan.weakest_to_remove, 1)
        self.assertEqual(plan.refill_count, 1)

    def test_weakest_percentage_is_source_clamped_to_fifty(self):
        plan = plan_weakest_replacement(
            population_size=100,
            current_population_size=100,
            current_generation=5,
            replace_weakest_pct=99,
            replace_every_generations=5,
        )
        self.assertEqual(plan.effective_replace_pct, 50)
        self.assertEqual(plan.target_fresh_count, 50)
        self.assertEqual(plan.weakest_to_remove, 50)

    def test_malformed_weakest_inputs_fail_closed(self):
        cases = (
            (
                dict(
                    population_size=0,
                    current_population_size=0,
                    current_generation=1,
                    replace_weakest_pct=10,
                    replace_every_generations=2,
                ),
                "population_size must be positive",
            ),
            (
                dict(
                    population_size=4,
                    current_population_size=5,
                    current_generation=1,
                    replace_weakest_pct=10,
                    replace_every_generations=2,
                ),
                "current_population_size",
            ),
            (
                dict(
                    population_size=4,
                    current_population_size=4,
                    current_generation=-1,
                    replace_weakest_pct=10,
                    replace_every_generations=2,
                ),
                "current_generation",
            ),
            (
                dict(
                    population_size=4,
                    current_population_size=4,
                    current_generation=1,
                    replace_weakest_pct=-1,
                    replace_every_generations=2,
                ),
                "replace_weakest_pct",
            ),
            (
                dict(
                    population_size=4,
                    current_population_size=4,
                    current_generation=1,
                    replace_weakest_pct=10,
                    replace_every_generations=0,
                ),
                "replace_every_generations",
            ),
            (
                dict(
                    population_size=True,
                    current_population_size=4,
                    current_generation=1,
                    replace_weakest_pct=10,
                    replace_every_generations=2,
                ),
                "population_size must be an integer",
            ),
        )
        for kwargs, message in cases:
            with self.subTest(kwargs=kwargs):
                with self.assertRaisesRegex(FreshBloodError, message):
                    plan_weakest_replacement(**kwargs)

    def test_provenance_records_exact_fresh_blood_authority(self):
        by_class = {
            item.class_name: item
            for item in SQX_FRESH_BLOOD_SOURCE_PROVENANCE
        }
        self.assertEqual(
            set(by_class),
            {"BuildMode", "GeneticBuildEngine", "GPGenerationalEngine"},
        )
        self.assertEqual(SQX_MAX_CANDIDATES_PER_FINGERPRINT, 2)
        self.assertEqual(SQX_WEAKEST_REPLACEMENT_MAX_PCT, 50)
        self.assertIn("freshBloodWeakestPct", by_class["BuildMode"].method)
        self.assertEqual(
            by_class["GeneticBuildEngine"].method,
            "getGPSettings",
        )
        self.assertIn(
            "removeTooSimilarStrategies",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertIn(
            "removeWeakestStrategies",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertTrue(
            all(item.blob_sha for item in SQX_FRESH_BLOOD_SOURCE_PROVENANCE)
        )


if __name__ == "__main__":
    unittest.main()
