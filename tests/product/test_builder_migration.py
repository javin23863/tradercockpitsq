from dataclasses import dataclass
import unittest

from tradercockpit.builder.evolution import EvolutionConfig
from tradercockpit.builder.migration import (
    JAVA_INT_MAX,
    SQX_MIGRATION_CLONE_CONTRACT,
    SQX_MIGRATION_INBOX_FRACTION,
    SQX_MIGRATION_SOURCE_PROVENANCE,
    MigrationError,
    materialize_migration_candidates,
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


@dataclass
class Candidate:
    name: str


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
        self.assertIs(plan.clone_contract, SQX_MIGRATION_CLONE_CONTRACT)

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

    def test_send_materializes_distinct_prefix_clones_and_keeps_sender_population(self):
        population = tuple(Candidate(f"c{index}") for index in range(4))
        plan = plan_migration_send(
            migration_config(population_size_per_island=4),
            source_island_index=0,
            current_generation=87,
            current_population_size=4,
        )
        result = materialize_migration_candidates(
            population,
            plan,
            clone_for_migration=lambda candidate: Candidate(candidate.name),
        )
        self.assertEqual(result.source_positions, (0,))
        self.assertEqual([item.name for item in result.migrants], ["c0"])
        self.assertIsNot(result.migrants[0], population[0])
        self.assertTrue(result.sender_population_unchanged)
        self.assertEqual([item.name for item in population], ["c0", "c1", "c2", "c3"])

    def test_send_rejects_alias_instead_of_fake_migration_clone(self):
        population = tuple(Candidate(f"c{index}") for index in range(4))
        plan = plan_migration_send(
            migration_config(population_size_per_island=4),
            source_island_index=0,
            current_generation=87,
            current_population_size=4,
        )
        with self.assertRaisesRegex(MigrationError, "distinct migration candidate"):
            materialize_migration_candidates(
                population,
                plan,
                clone_for_migration=lambda candidate: candidate,
            )

    def test_recovered_clone_contract_preserves_lineage_and_drops_results_group(self):
        contract = SQX_MIGRATION_CLONE_CONTRACT
        self.assertTrue(contract.creates_distinct_candidate)
        self.assertTrue(contract.strategy_xml_deep_cloned)
        self.assertTrue(contract.fitness_preserved)
        self.assertTrue(contract.gpids_lineage_preserved)
        self.assertFalse(contract.results_group_preserved)
        self.assertTrue(contract.sender_retains_source_candidate)

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
        self.assertFalse(plan.retained_identity_known)
        self.assertFalse(plan.native_trim_calls_destroy)

    def test_inbox_below_capacity_preserves_arrival_identity_without_shuffle(self):
        plan = plan_migration_inbox_add(
            configured_population_size=100,
            existing_inbox_count=2,
            incoming_count=3,
        )
        self.assertEqual(plan.retained_count, 5)
        self.assertEqual(plan.discarded_count, 0)
        self.assertFalse(plan.requires_default_shuffle)
        self.assertTrue(plan.retained_identity_known)

    def test_receive_waits_until_interval_threshold_and_preserves_inbox(self):
        plan = plan_migration_receive(
            migration_config(),
            current_generation=86,
            current_population_size=100,
            inbox_count=20,
        )
        self.assertFalse(plan.eligible)
        self.assertEqual(plan.source_inbox_capacity, 20)
        self.assertEqual(plan.immigrants_applied, 0)
        self.assertEqual(plan.inbox_remaining_after, 20)
        self.assertFalse(plan.requires_population_resort)

    def test_receive_is_threshold_gated_not_modulo_gated(self):
        plan = plan_migration_receive(
            migration_config(),
            current_generation=88,
            current_population_size=100,
            inbox_count=1,
        )
        self.assertTrue(plan.eligible)
        self.assertEqual(plan.immigrants_applied, 1)
        self.assertTrue(plan.requires_population_resort)

    def test_receive_replaces_capped_inbox_then_clears_and_resorts(self):
        normal = plan_migration_receive(
            migration_config(),
            current_generation=87,
            current_population_size=100,
            inbox_count=20,
        )
        self.assertTrue(normal.eligible)
        self.assertEqual(normal.source_inbox_capacity, 20)
        self.assertEqual(normal.replacement_limit, 50)
        self.assertEqual(normal.immigrants_applied, 20)
        self.assertEqual(normal.inbox_discarded, 0)
        self.assertEqual(normal.inbox_remaining_after, 0)
        self.assertEqual(normal.removed_population_candidates_destroyed, 20)
        self.assertEqual(normal.native_unapplied_inbox_candidates_destroyed, 0)
        self.assertTrue(normal.requires_population_resort)
        self.assertTrue(normal.immigrant_clone_contract.gpids_lineage_preserved)
        self.assertFalse(normal.immigrant_clone_contract.results_group_preserved)
        self.assertEqual(
            normal.removed_population_positions,
            tuple(range(99, 79, -1)),
        )

    def test_receive_rejects_inbox_state_upstream_add_could_not_produce(self):
        with self.assertRaisesRegex(MigrationError, "addToInbox capacity"):
            plan_migration_receive(
                migration_config(),
                current_generation=87,
                current_population_size=100,
                inbox_count=21,
            )

    def test_population_one_clears_eligible_inbox_without_applying_candidate(self):
        plan = plan_migration_receive(
            migration_config(population_size_per_island=1),
            current_generation=87,
            current_population_size=1,
            inbox_count=1,
        )
        self.assertTrue(plan.eligible)
        self.assertEqual(plan.source_inbox_capacity, 1)
        self.assertEqual(plan.replacement_limit, 0)
        self.assertEqual(plan.immigrants_applied, 0)
        self.assertEqual(plan.inbox_discarded, 1)
        self.assertEqual(plan.inbox_remaining_after, 0)
        self.assertEqual(plan.native_unapplied_inbox_candidates_destroyed, 0)
        self.assertTrue(plan.requires_population_resort)

    def test_malformed_migration_state_fails_closed(self):
        with self.assertRaisesRegex(MigrationError, "outside configured islands"):
            plan_migration_send(
                migration_config(),
                source_island_index=4,
                current_generation=87,
                current_population_size=100,
            )
        with self.assertRaisesRegex(MigrationError, "TraderCockpit safety boundary"):
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
        with self.assertRaisesRegex(MigrationError, "signed Java int"):
            plan_migration_inbox_add(
                configured_population_size=100,
                existing_inbox_count=JAVA_INT_MAX,
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
            {"GeneticBuildEngine", "GPGenerationalEngine", "GPIslandJob", "Node"},
        )
        self.assertIn("migrateIndividuals", by_class["GPGenerationalEngine"].method)
        self.assertIn("receiveImmigrants", by_class["GPGenerationalEngine"].method)
        self.assertEqual(by_class["GPIslandJob"].method, "messageReceived/createJobID")
        self.assertEqual(by_class["Node"].method, "cloneForMigration")
        self.assertIn("ResultsGroup null", by_class["Node"].conclusion)
        self.assertTrue(all(item.blob_sha for item in SQX_MIGRATION_SOURCE_PROVENANCE))


if __name__ == "__main__":
    unittest.main()
