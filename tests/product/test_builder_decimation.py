import unittest

from tradercockpit.builder.decimation import (
    SQX_DECIMATION_SOURCE_PROVENANCE,
    InitialPopulationDecimationError,
    normalize_decimation_coefficient,
    plan_initial_population_decimation,
)


class BuilderInitialPopulationDecimationTests(unittest.TestCase):
    def test_displayed_baseline_coefficient_one_generates_one_population(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=0,
            decimation_coefficient=1,
        )
        self.assertEqual(plan.effective_decimation_coefficient, 1)
        self.assertEqual(plan.generated_survivor_capacity, 100)
        self.assertEqual(plan.filter_passing_generated_target, 100)
        self.assertEqual(plan.generated_candidates_removed_after_sort, 0)

    def test_coefficient_three_generates_three_times_remaining_slots(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=0,
            decimation_coefficient=3,
        )
        self.assertEqual(plan.filter_passing_generated_target, 300)
        self.assertEqual(plan.generated_survivor_capacity, 100)
        self.assertEqual(plan.generated_candidates_removed_after_sort, 200)

    def test_supplied_initial_population_reduces_generated_target_before_decimation(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=100,
            supplied_initial_count=40,
            decimation_coefficient=3,
        )
        self.assertEqual(plan.generated_survivor_capacity, 60)
        self.assertEqual(plan.filter_passing_generated_target, 180)
        self.assertEqual(plan.generated_candidates_removed_after_sort, 120)

    def test_supplied_initial_count_at_or_above_population_needs_no_generated_survivors(self):
        for supplied in (100, 125):
            with self.subTest(supplied=supplied):
                plan = plan_initial_population_decimation(
                    population_size_per_island=100,
                    supplied_initial_count=supplied,
                    decimation_coefficient=7,
                )
                self.assertEqual(plan.generated_survivor_capacity, 0)
                self.assertEqual(plan.filter_passing_generated_target, 0)
                self.assertEqual(plan.generated_candidates_removed_after_sort, 0)

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
                self.assertEqual(plan.filter_passing_generated_target, 15)
                self.assertEqual(plan.generated_candidates_removed_after_sort, 0)

    def test_no_unproved_upper_bound_is_invented(self):
        plan = plan_initial_population_decimation(
            population_size_per_island=4,
            supplied_initial_count=1,
            decimation_coefficient=1000,
        )
        self.assertEqual(plan.filter_passing_generated_target, 3000)
        self.assertEqual(plan.generated_candidates_removed_after_sort, 2997)

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
        self.assertIn(
            "decimationCoef",
            by_class["BuildMode"].method,
        )
        self.assertIn(
            "getGPSettings",
            by_class["GeneticBuildEngine"].method,
        )
        self.assertIn(
            "generateInitialPopulation",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertIn(
            "decimateInitialPopulation",
            by_class["GPGenerationalEngine"].method,
        )
        self.assertTrue(
            all(item.blob_sha for item in SQX_DECIMATION_SOURCE_PROVENANCE)
        )


if __name__ == "__main__":
    unittest.main()
