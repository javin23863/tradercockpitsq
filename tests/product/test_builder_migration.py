import unittest

from tradercockpit.builder.evolution import EvolutionConfig
from tradercockpit.builder.migration import (
    SQX_MIGRATION_INBOX_FRACTION,
    SQX_MIGRATION_SOURCE_PROVENANCE,
    MigrationError,
    migration_inbox_capacity,
    plan_migration_inbox_add,
    plan_migration_receive,
    plan_migration_send,
)


def migration_config(**overrides):
    values = dict(
        population_size_per_island=100,
        maximum_generations=100,
        crossover_probability_pct=93,
        mutation_probability_pct=30,
        island_count=4,
        migration_interval=87,
        migration_rate_pct=6,
    )
    values.update(overrides)
    return EvolutionConfig(**values)


class BuilderMigrationTests(unittest.TestCase):
    def test_four_island_topology_routes_to_next_island_and_wraps(self):
        first = plan_migration_send(
            migration_config(),
            source_island_index=0,
            current_generation=87,
            current_population_size=100,
        )
        last = plan_migration_send(
            migration_config(),
            source_island_index=3,
            current_generation=87,
            current_population_size=100,
        )
        self.assertEqual(first.destination_island_index, 1)
        self.assertEqual(last.destination_island_index, 0)

    def test_observed_six_percent_interval_sends_prefix_positions(self):
        plan = plan_migration_send(
            migration_config(),
            source_island_index=0,
            current_generation=87,
            current_population_size=100,
        )
        self.assertTrue(plan.scheduled)
        self.assertEqual(plan.migration_count, 6)
        self.assertEqual(plan.source_positions, tuple(range(6)))

        before_interval = plan_migration_send(
            migration_config(),
            source_island_index=0,
            current_generation=86,
            current_population_size=100,
        )
        self.assertFalse(before_interval.scheduled)
        self.assertEqual(before_interval.migration_count, 0)

    def test_positive_rate_migrates_at_least_one_candidate(self):
        plan = plan_migration_send(
            migration_config(population_size_per_island=4),
            source_island_index=0,
            current_generation=87,
            current_population_size=4,
        )
        self.assertEqual(plan.migration_count, 1)
        self.assertEqual(plan.source_positions, (0,))

    def test_single_island_or_zero_rate_disables_send(self):
        single = plan_migration_send(
            migration_config(island_count=1),
            source_island_index=0,
            current_generation=87,
            current_population_size=100,
        )
        self.assertFalse(single.scheduled)
        self.assertIsNone(single.destination_island_index)

        zero_rate = plan_migration_send(
            migration_config(migration_rate_pct=0),
            source_island_index=0,
            current_generation=87,
            current_population_size=100,
        )
        self.assertFalse(zero_rate.scheduled)
        self.assertEqual(zero_rate.migration_count, 0)

    def test_inbox_capacity_is_twenty_percent_with_minimum_one(self):
        self.assertEqual(SQX_MIGRATION_INBOX_FRACTION, 0.2)
        self.assertEqual(migration_inbox_capacity(100), 20)
        self.assertEqual(migration_inbox_capacity(4), 1)

        plan = plan_migration_inbox_add(
            configured_population_size=100,
            existing_inbox_count=10,
            incoming_count=15,
        )
        self.assertEqual(plan.capacity, 20)
        self.assertEqual(plan.retained_count, 20)
        self.assertEqual(plan.discarded_count, 5)
        self.assertTrue(plan.requires_default_shuffle)

    def test_inbox_below_capacity_does_not_claim_shuffle(self):
        plan = plan_migration_inbox_add(
            configured_population_size=100,
            existing_inbox_count=2,
            incoming_count=3,
        )
        self.assertEqual(plan.retained_count, 5)
        self.assertEqual(plan.discarded_count, 0)
        self.assertFalse(plan.requires_default_shuffle)

    def test_receive_waits_until_interval_threshold_and_preserves_inbox(self):
        plan = plan_migration_receive(
            migration_config(),
            current_generation=86,
            current_population_size=100,
            inbox_count=20,
        )
        self.assertFalse(plan.eligible)
        self.assertEqual(plan.immigrants_applied, 0)
        self.assertEqual(plan.inbox_remaining_after, 20)

    def test_receive_replaces_at_most_half_then_clears_inbox(self):
        normal = plan_migration_receive(
            migration_config(),
            current_generation=87,
            current_population_size=100,
            inbox_count=20,
        )
        self.assertTrue(normal.eligible)
        self.assertEqual(normal.replacement_limit, 50)
        self.assertEqual(normal.immigrants_applied, 20)
        self.assertEqual(normal.inbox_discarded, 0)
        self.assertEqual(normal.inbox_remaining_after, 0)
        self.assertEqual(
            normal.removed_population_positions,
            tuple(range(99, 79, -1)),
        )

        oversized = plan_migration_receive(
            migration_config(),
            current_generation=87,
            current_population_size=100,
            inbox_count=60,
        )
        self.assertEqual(oversized.immigrants_applied, 50)
        self.assertEqual(oversized.inbox_discarded, 10)
        self.assertEqual(oversized.inbox_remaining_after, 0)

    def test_population_one_clears_eligible_inbox_without_applying_candidate(self):
        plan = plan_migration_receive(
            migration_config(population_size_per_island=1),
            current_generation=87,
            current_population_size=1,
            inbox_count=1,
        )
        self.assertTrue(plan.eligible)
        self.assertEqual(plan.replacement_limit, 0)
        self.assertEqual(plan.immigrants_applied, 0)
        self.assertEqual(plan.inbox_discarded, 1)
        self.assertEqual(plan.inbox_remaining_after, 0)

    def test_malformed_migration_state_fails_closed(self):
        with self.assertRaisesRegex(MigrationError, "outside configured islands"):
            plan_migration_send(
                migration_config(),
                source_island_index=4,
                current_generation=87,
                current_population_size=100,
            )
        with self.assertRaisesRegex(MigrationError, "non-empty population"):
            plan_migration_send(
                migration_config(),
                source_island_index=0,
                current_generation=87,
                current_population_size=0,
            )
        with self.assertRaisesRegex(MigrationError, "positive"):
            migration_inbox_capacity(0)
        with self.assertRaisesRegex(MigrationError, "existing_inbox_count"):
            plan_migration_inbox_add(
                configured_population_size=100,
                existing_inbox_count=-1,
                incoming_count=1,
            )
        with self.assertRaisesRegex(MigrationError, "inbox_count"):
            plan_migration_receive(
                migration_config(),
                current_generation=87,
                current_population_size=100,
                inbox_count=-1,
            )

    def test_provenance_records_exact_migration_authority(self):
        by_class = {
            item.class_name: item
            for item in SQX_MIGRATION_SOURCE_PROVENANCE
        }
        self.assertEqual(
            set(by_class),
            {"GeneticBuildEngine", "GPGenerationalEngine", "GPIslandJob"},
        )
        self.assertIn("migrateIndividuals", by_class["GPGenerationalEngine"].method)
        self.assertIn("receiveImmigrants", by_class["GPGenerationalEngine"].method)
        self.assertEqual(by_class["GPIslandJob"].method, "messageReceived/createJobID")
        self.assertTrue(
            all(item.blob_sha for item in SQX_MIGRATION_SOURCE_PROVENANCE)
        )


if __name__ == "__main__":
    unittest.main()
