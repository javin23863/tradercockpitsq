from dataclasses import dataclass
import unittest

from tradercockpit.builder import (
    SQX_CROSSOVER_MAX_POINTS,
    SQX_CROSSOVER_PROBABILITY_SCOPE,
    SQX_GA_SOURCE_PROVENANCE,
    SQX_MUTATION_PROBABILITY_SCOPE,
    SQX_NATIVE_OPERATOR_PIPELINE,
    SQX_TOURNAMENT_RANK_PROBABILITY,
    SQX_TOURNAMENT_SIZE,
    EvolutionConfig,
    EvolutionConfigError,
    EvolutionKernel,
    TournamentSelection,
    plan_islands,
    sqx_probability_gate,
)


def native_settings(**overrides):
    settings = {
        "population": 4,
        "max_generations": 2,
        "crossover_probability": 93,
        "mutation_probability": 30,
        "islands": 1,
        "migration_modulo": 1,
        "migration_rate": 0,
        "fresh_blood_replace_similar": True,
        "fresh_blood_replace_weakest": False,
        "filter_initial_population": False,
        "restart_on_finish": False,
        "restart_on_stagnation": False,
    }
    settings.update(overrides)
    return settings


class StubRng:
    def __init__(self, *, indexes=(), draws=()):
        self._indexes = iter(indexes)
        self._draws = iter(draws)
        self.randrange_calls = 0
        self.random_calls = 0

    def randrange(self, stop):
        self.randrange_calls += 1
        value = next(self._indexes)
        if not 0 <= value < stop:
            raise AssertionError(f"stub index {value} out of range for stop={stop}")
        return value

    def random(self):
        self.random_calls += 1
        return next(self._draws)


@dataclass(frozen=True)
class Candidate:
    name: str
    fitness: float
    identity: str


def selector():
    return TournamentSelection(
        fitness=lambda candidate: candidate.fitness,
        identity=lambda candidate: candidate.identity,
    )


class BuilderEvolutionTests(unittest.TestCase):
    def test_saved_native_baseline_maps_proved_controls(self):
        config = EvolutionConfig.from_native_settings(native_settings())

        self.assertEqual(config.population_size_per_island, 4)
        self.assertEqual(config.maximum_generations, 2)
        self.assertEqual(config.crossover_probability_pct, 93)
        self.assertEqual(config.mutation_probability_pct, 30)
        self.assertEqual(config.island_count, 1)
        self.assertEqual(config.planned_population_capacity, 4)
        self.assertIs(config.fresh_blood_replace_similar, True)
        self.assertIs(config.fresh_blood_replace_weakest, False)

    def test_native_operator_variants_preserve_serialized_probabilities(self):
        for crossover, mutation in ((93, 30), (100, 0), (0, 100), (50, 50)):
            with self.subTest(crossover=crossover, mutation=mutation):
                config = EvolutionConfig.from_native_settings(
                    native_settings(
                        crossover_probability=crossover,
                        mutation_probability=mutation,
                    )
                )
                self.assertEqual(config.crossover_probability_pct, crossover)
                self.assertEqual(config.mutation_probability_pct, mutation)

    def test_four_island_variant_is_four_island_local_population_targets(self):
        config = EvolutionConfig.from_native_settings(
            native_settings(islands=4, migration_modulo=87, migration_rate=6)
        )

        plans = plan_islands(config)
        self.assertEqual([plan.island_index for plan in plans], [0, 1, 2, 3])
        self.assertEqual([plan.population_size for plan in plans], [4, 4, 4, 4])
        self.assertEqual(config.planned_population_capacity, 16)
        self.assertEqual(config.migration_interval, 87)
        self.assertEqual(config.migration_rate_pct, 6)

    def test_fresh_weak_variant_is_preserved_as_configuration_only(self):
        config = EvolutionConfig.from_native_settings(
            native_settings(fresh_blood_replace_weakest=True)
        )
        self.assertIs(config.fresh_blood_replace_weakest, True)

    def test_restart_modes_still_fail_closed(self):
        with self.assertRaisesRegex(
            EvolutionConfigError,
            "restart behavior is not yet supported",
        ):
            EvolutionConfig.from_native_settings(
                native_settings(restart_on_finish=True)
            )

    def test_missing_native_settings_fail_closed(self):
        settings = native_settings()
        settings.pop("mutation_probability")
        with self.assertRaisesRegex(EvolutionConfigError, "mutation_probability"):
            EvolutionConfig.from_native_settings(settings)

    def test_invalid_probabilities_fail_closed(self):
        with self.assertRaisesRegex(EvolutionConfigError, "crossover probability"):
            EvolutionConfig.from_native_settings(
                native_settings(crossover_probability=101)
            )

    def test_native_setting_types_fail_closed_instead_of_python_truthiness(self):
        cases = (
            ("restart_on_finish", "false", "restart_on_finish must be a boolean"),
            ("population", "4", "population must be an integer"),
            ("population", True, "population must be an integer"),
        )
        for key, value, message in cases:
            with self.subTest(key=key, value=value):
                with self.assertRaisesRegex(EvolutionConfigError, message):
                    EvolutionConfig.from_native_settings(
                        native_settings(**{key: value})
                    )

    def test_source_proven_operator_contract_replaces_two_draw_model(self):
        self.assertEqual(SQX_TOURNAMENT_SIZE, 3)
        self.assertEqual(SQX_TOURNAMENT_RANK_PROBABILITY, 0.8)
        self.assertEqual(SQX_CROSSOVER_MAX_POINTS, 2)
        self.assertEqual(SQX_CROSSOVER_PROBABILITY_SCOPE, "shuffled-pair")
        self.assertEqual(SQX_MUTATION_PROBABILITY_SCOPE, "generated-object")
        self.assertEqual(
            SQX_NATIVE_OPERATOR_PIPELINE,
            (
                "TournamentSelection",
                "NodeCrossover(max-points=2)",
                "NodeMutation",
                "FixNonRandomBlocks",
                "FixUnusedDependentFormulas",
                "FixCustomBlocks",
                "FixStockpickerBlocks",
                "FixNumberOfExitTypes",
            ),
        )

    def test_source_provenance_records_exact_recovered_classes_without_runtime_imports(self):
        classes = {item.class_name for item in SQX_GA_SOURCE_PROVENANCE}
        self.assertTrue(
            {
                "GeneticBuildEngine",
                "TournamentSelection",
                "EvolutionPipeline",
                "NodeCrossover",
                "NodeMutation",
                "GPEngine",
                "GPGenerationalEngine",
                "MersenneTwisterRng",
            }.issubset(classes)
        )
        self.assertTrue(all(item.blob_sha for item in SQX_GA_SOURCE_PROVENANCE))

    def test_probability_one_succeeds_without_consuming_random_draw(self):
        rng = StubRng(draws=[])
        self.assertIs(sqx_probability_gate(1.0, rng), True)
        self.assertEqual(rng.random_calls, 0)

    def test_probability_zero_consumes_draw_and_fails(self):
        rng = StubRng(draws=[0.0])
        self.assertIs(sqx_probability_gate(0.0, rng), False)
        self.assertEqual(rng.random_calls, 1)

    def test_probability_gate_uses_strict_less_than(self):
        rng = StubRng(draws=[0.49, 0.50])
        self.assertIs(sqx_probability_gate(0.50, rng), True)
        self.assertIs(sqx_probability_gate(0.50, rng), False)

    def test_tournament_selection_samples_three_with_replacement_and_prefers_best_rank(self):
        population = [
            Candidate("low", 1.0, "low"),
            Candidate("mid", 2.0, "mid"),
            Candidate("high", 3.0, "high"),
        ]
        rng = StubRng(indexes=[0, 1, 2], draws=[0.79])

        selected = selector().select(population, 1, rng)

        self.assertEqual(selected, (population[2],))
        self.assertEqual(rng.randrange_calls, 3)
        self.assertEqual(rng.random_calls, 1)

    def test_tournament_selection_can_take_second_rank_after_best_rank_misses(self):
        population = [
            Candidate("low", 1.0, "low"),
            Candidate("mid", 2.0, "mid"),
            Candidate("high", 3.0, "high"),
        ]
        rng = StubRng(indexes=[0, 1, 2], draws=[0.90, 0.10])

        selected = selector().select(population, 1, rng)

        self.assertEqual(selected, (population[1],))

    def test_tournament_selection_duplicate_identity_culling_affects_later_tournaments(self):
        population = [
            Candidate("a1", 10.0, "a"),
            Candidate("a2", 9.0, "a"),
            Candidate("b", 8.0, "b"),
            Candidate("c", 7.0, "c"),
        ]
        rng = StubRng(
            indexes=[0, 0, 0, 0, 0, 0],
            draws=[0.10, 0.10],
        )

        selected = selector().select(population, 2, rng)

        self.assertEqual(
            [candidate.identity for candidate in selected],
            ["a", "b"],
        )

    def test_population_kernel_selects_first_then_runs_whole_population_pipeline_in_source_order(self):
        population = [
            Candidate("a", 1.0, "a"),
            Candidate("b", 2.0, "b"),
            Candidate("c", 3.0, "c"),
        ]
        config = EvolutionConfig.from_native_settings(
            native_settings(crossover_probability=50, mutation_probability=50)
        )
        calls = []

        def operator(name):
            def apply(candidates, actual_config, rng):
                calls.append((name, tuple(candidate.name for candidate in candidates)))
                self.assertIs(actual_config, config)
                return candidates

            return apply

        kernel = EvolutionKernel(
            selector=selector(),
            crossover=operator("NodeCrossover(max-points=2)"),
            mutate=operator("NodeMutation"),
            fix_non_random_blocks=operator("FixNonRandomBlocks"),
            fix_unused_dependent_formulas=operator("FixUnusedDependentFormulas"),
            fix_custom_blocks=operator("FixCustomBlocks"),
            fix_stockpicker_blocks=operator("FixStockpickerBlocks"),
            fix_number_of_exit_types=operator("FixNumberOfExitTypes"),
        )
        rng = StubRng(
            indexes=[0, 1, 2, 0, 1, 0],
            draws=[0.10, 0.10],
        )

        result = kernel.evolve_selected_population(
            population,
            config,
            rng,
            selection_count=2,
        )

        self.assertEqual(
            [name for name, _ in calls],
            list(SQX_NATIVE_OPERATOR_PIPELINE[1:]),
        )
        self.assertTrue(all(names == ("c", "b") for _, names in calls))
        self.assertEqual(result.selected_count, 2)
        self.assertEqual(result.population, (population[2], population[1]))
        # Only TournamentSelection consumes RNG here. The kernel does not invent
        # global crossover or mutation draws; those gates belong to their operators.
        self.assertEqual(rng.randrange_calls, 6)
        self.assertEqual(rng.random_calls, 2)

    def test_kernel_requires_selection_count_explicit_and_rejects_empty_positive_selection(self):
        config = EvolutionConfig.from_native_settings(native_settings())

        def identity_operator(candidates, config, rng):
            return candidates

        kernel = EvolutionKernel(
            selector=selector(),
            crossover=identity_operator,
            mutate=identity_operator,
            fix_non_random_blocks=identity_operator,
            fix_unused_dependent_formulas=identity_operator,
            fix_custom_blocks=identity_operator,
            fix_stockpicker_blocks=identity_operator,
            fix_number_of_exit_types=identity_operator,
        )

        with self.assertRaisesRegex(EvolutionConfigError, "population must not be empty"):
            kernel.evolve_selected_population(
                [],
                config,
                StubRng(),
                selection_count=1,
            )


if __name__ == "__main__":
    unittest.main()
