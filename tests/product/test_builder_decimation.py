import unittest

from tradercockpit.builder.decimation import (
    JAVA_INT_MAX,
    SQX_DECIMATION_SOURCE_PROVENANCE,
    InitialPopulationDecimationError,
    generated_candidates_removed_after_sort,
    initial_generation_batch_size,
    normalize_decimation_coefficient,
    plan_initial_population_decimation,
)


class BuilderInitialPopulationDecimationTests(unittest.TestCase):
    def test_displayed_baseline_coefficient_one_sets_minimum_threshold(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )
        self.assertEqual(plan.effective_decimation_coefficient, 1)
        self.assertEqual(plan.generated_survivor_capacity, 100)
        self.assertEqual(plan.native_acceptance_threshold, 100)
        self.assertEqual(plan.minimum_filter_passing_generated_count, 100)
        self.assertEqual(plan.minimum_generated_candidates_removed_after_sort, 0)
        self.assertEqual(plan.normal_completion_discarded_candidate_factory_calls, 1)

    def test_coefficient_three_sets_three_times_remaining_slot_threshold(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=0,
            decimation_coefficient=3,
        )
        self.assertEqual(plan.native_acceptance_threshold, 300)
        self.assertEqual(plan.minimum_filter_passing_generated_count, 300)
        self.assertEqual(plan.generated_survivor_capacity, 100)
        self.assertEqual(plan.minimum_generated_candidates_removed_after_sort, 200)

    def test_supplied_initial_population_reduces_generated_threshold(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=40,
            decimation_coefficient=3,
        )
        self.assertEqual(plan.generated_survivor_capacity, 60)
        self.assertEqual(plan.native_acceptance_threshold, 180)
        self.assertEqual(plan.minimum_filter_passing_generated_count, 180)
        self.assertEqual(plan.minimum_generated_candidates_removed_after_sort, 120)
        self.assertIs(plan.supplied_initial_population_shuffle_invoked, True)

    def test_supplied_initial_count_at_or_above_population_needs_no_generated_accepts(self):
        equal = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=100,
            decimation_coefficient=7,
        )
        self.assertEqual(equal.native_acceptance_threshold, 0)
        self.assertEqual(equal.minimum_filter_passing_generated_count, 0)
        self.assertEqual(equal.generated_survivor_capacity, 0)
        self.assertEqual(equal.minimum_generated_candidates_removed_after_sort, 0)
        self.assertEqual(equal.normal_completion_discarded_candidate_factory_calls, 1)

        above = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=125,
            decimation_coefficient=7,
        )
        self.assertEqual(above.native_acceptance_threshold, -175)
        self.assertEqual(above.minimum_filter_passing_generated_count, 0)
        self.assertEqual(above.generated_survivor_capacity, 0)
        self.assertEqual(above.minimum_generated_candidates_removed_after_sort, 0)
        self.assertIs(above.supplied_initial_population_shuffle_invoked, True)

    def test_nonpositive_native_coefficient_normalizes_to_one(self):
        for coefficient in (0, -1, -100):
            with self.subTest(coefficient=coefficient):
                self.assertEqual(normalize_decimation_coefficient(coefficient), 1)
                plan = plan_initial_population_decimation(
                    population_size_per_island=20,
                    supplied_initial_count=5,
                    decimation_coefficient=coefficient,
                )
                self.assertEqual(plan.requested_decimation_coefficient, coefficient)
                self.assertEqual(plan.effective_decimation_coefficient, 1)
                self.assertEqual(plan.native_acceptance_threshold, 15)
                self.assertEqual(plan.minimum_filter_passing_generated_count, 15)

    def test_final_native_batch_can_overshoot_acceptance_threshold(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=4,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )

        self.assertEqual(plan.native_acceptance_threshold, 4)
        self.assertEqual(
            initial_generation_batch_size(
                plan,
                accepted_generated_count=0,
                computed_threads=10,
            ),
            8,
        )

        # If all eight candidates in that final batch pass, SQX sorts all eight
        # and decimates four. The previous implementation incorrectly predicted
        # an exact accepted count of four and zero removals.
        self.assertEqual(
            generated_candidates_removed_after_sort(
                plan,
                accepted_generated_count=8,
            ),
            4,
        )

    def test_native_batch_size_is_capped_by_computed_threads(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )
        self.assertEqual(
            initial_generation_batch_size(
                plan,
                accepted_generated_count=0,
                computed_threads=3,
            ),
            6,
        )

    def test_threshold_completion_is_separate_from_discarded_factory_call(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=4,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )
        self.assertEqual(
            initial_generation_batch_size(
                plan,
                accepted_generated_count=4,
                computed_threads=2,
            ),
            0,
        )
        self.assertEqual(plan.normal_completion_discarded_candidate_factory_calls, 1)

    def test_java_int_storage_and_overflow_are_not_silently_reinterpreted(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=1,
            supplied_initial_count=0,
            decimation_coefficient=JAVA_INT_MAX,
        )
        self.assertEqual(plan.native_acceptance_threshold, JAVA_INT_MAX)

        with self.assertRaisesRegex(
            InitialPopulationDecimationError,
            "fit a signed Java int",
        ):
            plan_initial_population_decimation(
                population_size_per_island=1,
                supplied_initial_count=0,
                decimation_coefficient=JAVA_INT_MAX + 1,
            )

        with self.assertRaisesRegex(
            InitialPopulationDecimationError,
            "overflow SQX Java int arithmetic",
        ):
            plan_initial_population_decimation(
                population_size_per_island=4,
                supplied_initial_count=1,
                decimation_coefficient=JAVA_INT_MAX,
            )

    def test_integer_types_and_ranges_fail_closed(self):
        cases = (
            (
                dict(
                    population_size_per_island=True,
                    supplied_initial_count=0,
                    decimation_coefficient=1,
                ),
                "population_size_per_island must be an integer",
            ),
            (
                dict(
                    population_size_per_island=4,
                    supplied_initial_count=1.5,
                    decimation_coefficient=1,
                ),
                "supplied_initial_count must be an integer",
            ),
            (
                dict(
                    population_size_per_island=4,
                    supplied_initial_count=0,
                    decimation_coefficient=False,
                ),
                "decimation_coefficient must be an integer",
            ),
            (
                dict(
                    population_size_per_island=0,
                    supplied_initial_count=0,
                    decimation_coefficient=1,
                ),
                "population_size_per_island must be positive",
            ),
            (
                dict(
                    population_size_per_island=4,
                    supplied_initial_count=-1,
                    decimation_coefficient=1,
                ),
                "supplied_initial_count must not be negative",
            ),
        )
        for kwargs, message in cases:
            with self.subTest(kwargs=kwargs):
                with self.assertRaisesRegex(
                    InitialPopulationDecimationError,
                    message,
                ):
                    plan_initial_population_decimation(**kwargs)

    def test_batch_inputs_fail_closed(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=4,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )
        with self.assertRaisesRegex(
            InitialPopulationDecimationError,
            "accepted_generated_count must not be negative",
        ):
            initial_generation_batch_size(
                plan,
                accepted_generated_count=-1,
                computed_threads=1,
            )
        with self.assertRaisesRegex(
            InitialPopulationDecimationError,
            "computed_threads must be positive",
        ):
            initial_generation_batch_size(
                plan,
                accepted_generated_count=0,
                computed_threads=0,
            )

    def test_provenance_names_exact_decimation_authority(self):
        by_class = {
            item.class_name: item
            for item in SQX_DECIMATION_SOURCE_PROVENANCE
        }
        self.assertEqual(
            set(by_class),
            {"BuildMode", "GeneticBuildEngine", "GPGenerationalEngine"},
        )
        self.assertEqual(
            by_class["BuildMode"].blob_sha,
            "92a1596c49a71a7444166cb1a30e9468cbf27b00",
        )
        self.assertIn("decimationCoef", by_class["BuildMode"].method)
        self.assertIn("getGPSettings", by_class["GeneticBuildEngine"].method)
        self.assertIn(
            "generateInitialPopulation",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertIn(
            "decimateInitialPopulation",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertIn("threshold", by_class["GPGenerationalEngine"].conclusion)
        self.assertIn("overshoot", by_class["GPGenerationalEngine"].conclusion)
        self.assertTrue(
            all(item.blob_sha for item in SQX_DECIMATION_SOURCE_PROVENANCE)
        )


if __name__ == "__main__":
    unittest.main()
