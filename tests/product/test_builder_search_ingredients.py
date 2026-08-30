from decimal import Decimal
import unittest

from tradercockpit.builder.decimation import (
    generated_candidates_removed_after_sort,
    normalize_decimation_coefficient,
    plan_initial_population_decimation,
)
from tradercockpit.builder.evolution import EvolutionConfig
from tradercockpit.builder.fresh_blood import plan_weakest_replacement, prune_similar_population
from tradercockpit.builder.migration import (
    migration_inbox_capacity,
    plan_migration_receive,
    plan_migration_send,
)
from tradercockpit.builder.ranking import CandidateFitnessV1, RankingObjectiveV1, order_candidates
from tradercockpit.domain import content_address
from tradercockpit.domain.specs import SpecValidationError


class BuilderSearchIngredientTests(unittest.TestCase):
    def test_sqx_initial_decimation_normalizes_and_removes_actual_excess(self):
        self.assertEqual(normalize_decimation_coefficient(0), 1)
        plan = plan_initial_population_decimation(
            population_size_per_island=4,
            supplied_initial_count=0,
            decimation_coefficient=2,
        )
        self.assertEqual(plan.native_acceptance_threshold, 8)
        self.assertEqual(
            generated_candidates_removed_after_sort(plan, accepted_generated_count=9),
            5,
        )

    def test_sqx_fresh_blood_keeps_two_per_fingerprint_and_clamps_weakest_pct(self):
        population = [1, 2, 3, 4]
        pruned = prune_similar_population(
            population,
            fitness=lambda value: 1.0,
            fingerprint=lambda value: 7,
        )
        self.assertEqual(pruned.retained, (1, 2))
        plan = plan_weakest_replacement(
            population_size=10,
            current_population_size=10,
            current_generation=2,
            replace_weakest_pct=90,
            replace_every_generations=2,
        )
        self.assertEqual(plan.effective_replace_pct, 50)
        self.assertEqual(plan.weakest_to_remove, 5)
        self.assertEqual(plan.refill_count, 5)

    def test_sqx_migration_uses_ring_routing_cap_and_half_population_receive_limit(self):
        config = EvolutionConfig(
            population_size_per_island=10,
            maximum_generations=3,
            crossover_probability_pct=50,
            mutation_probability_pct=50,
            island_count=3,
            migration_interval=1,
            migration_rate_pct=20,
        )
        send = plan_migration_send(
            config,
            source_island_index=2,
            current_generation=1,
            current_population_size=10,
        )
        self.assertEqual(send.destination_island_index, 0)
        self.assertEqual(send.source_positions, (0, 1))
        self.assertEqual(migration_inbox_capacity(10), 2)
        receive = plan_migration_receive(
            config,
            current_generation=1,
            current_population_size=10,
            inbox_count=2,
        )
        self.assertEqual(receive.replacement_limit, 5)
        self.assertEqual(receive.immigrants_applied, 2)

    def test_ranking_consumes_actual_decimal_objective_and_ties_by_candidate_ref(self):
        refs = (
            content_address("candidate", 1, {"id": "b"}),
            content_address("candidate", 1, {"id": "a"}),
        )
        ordered = order_candidates(
            RankingObjectiveV1("construction_fit"),
            (
                CandidateFitnessV1(refs[0], Decimal("9")),
                CandidateFitnessV1(refs[1], Decimal("9")),
            ),
        )
        self.assertEqual(
            [str(item.candidate_ref) for item in ordered],
            sorted(str(ref) for ref in refs),
        )
        self.assertTrue(all(item.objective == "construction_fit" for item in ordered))

    def test_ranking_refuses_relabelled_or_mixed_objective_evidence(self):
        first = content_address("candidate", 1, {"id": "first"})
        second = content_address("candidate", 1, {"id": "second"})

        with self.assertRaisesRegex(SpecValidationError, "must match"):
            order_candidates(
                RankingObjectiveV1("other_metric"),
                (CandidateFitnessV1(first, Decimal("1")),),
            )

        with self.assertRaisesRegex(SpecValidationError, "must match"):
            order_candidates(
                RankingObjectiveV1("construction_fit"),
                (
                    CandidateFitnessV1(first, Decimal("2")),
                    CandidateFitnessV1(second, Decimal("1"), "other_metric"),
                ),
            )

    def test_nondefault_ranking_objective_requires_explicit_fitness_binding(self):
        ref = content_address("candidate", 1, {"id": "bound"})
        ordered = order_candidates(
            RankingObjectiveV1("other_metric"),
            (CandidateFitnessV1(ref, Decimal("3"), "other_metric"),),
        )
        self.assertEqual(ordered[0].objective, "other_metric")


if __name__ == "__main__":
    unittest.main()
