import unittest

from tradercockpit.builder import (
    SQX_GENERATION_CROSSOVER,
    SQX_GENERATION_INITIAL,
    SQX_GENERATION_MUTATION,
    SQX_GENERATION_UNKNOWN,
    SQX_LINEAGE_SOURCE_PROVENANCE,
    EvolutionExecutionContext,
    EvolutionLineage,
    LineageError,
    finalize_pipeline_lineage,
)


class BuilderLineageTests(unittest.TestCase):
    def test_unknown_matches_native_gpids_sentinel(self):
        lineage = EvolutionLineage.unknown()
        self.assertEqual(lineage.island_index, -1)
        self.assertEqual(lineage.generation_index, -1)
        self.assertEqual(lineage.node_index, -1)
        self.assertIs(lineage.generation_type, SQX_GENERATION_UNKNOWN)
        self.assertEqual(lineage.short_string(), "0.-1.-1")
        with self.assertRaisesRegex(LineageError, "not valid for Unknown"):
            lineage.display_string()

    def test_initial_lineage_uses_generation_zero_and_one_based_short_island(self):
        lineage = EvolutionLineage.initial(island_index=2, node_index=7)
        self.assertEqual(lineage.generation_type, SQX_GENERATION_INITIAL)
        self.assertEqual(lineage.identity_key, (2, 0, 7))
        self.assertEqual(lineage.short_string(), "3.0.7")
        self.assertEqual(lineage.display_string(), "3.0.7 (Initial)")

    def test_mutation_child_uses_context_parent_and_negative_prefinal_node(self):
        parent = EvolutionLineage.initial(island_index=0, node_index=3)
        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        child = EvolutionLineage.mutation(context=context, parent=parent)

        self.assertEqual(child.identity_key, (0, 1, -1))
        self.assertEqual(child.generation_type, SQX_GENERATION_MUTATION)
        self.assertEqual(child.parent1, "1.0.3")
        self.assertIsNone(child.parent2)
        self.assertEqual(child.display_string(), "1.1.-1 (Mutation from 1.0.3)")

    def test_crossover_child_records_both_parent_short_ids(self):
        parent1 = EvolutionLineage.initial(island_index=0, node_index=3)
        parent2 = EvolutionLineage.initial(island_index=0, node_index=9)
        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        child = EvolutionLineage.crossover(
            context=context,
            parent1=parent1,
            parent2=parent2,
        )

        self.assertEqual(child.identity_key, (0, 1, -1))
        self.assertEqual(child.generation_type, SQX_GENERATION_CROSSOVER)
        self.assertEqual(child.parent1, "1.0.3")
        self.assertEqual(child.parent2, "1.0.9")
        self.assertEqual(
            child.display_string(),
            "1.1.-1 (Crossover from 1.0.3+1.0.9)",
        )

    def test_pipeline_node_assignment_preserves_lineage_and_parents(self):
        parent = EvolutionLineage.initial(island_index=1, node_index=4)
        child = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=1, generation_index=2),
            parent=parent,
        )
        finalized = child.with_node_index(6)

        self.assertEqual(finalized.identity_key, (1, 2, 6))
        self.assertEqual(finalized.parent1, "2.0.4")
        self.assertEqual(finalized.display_string(), "2.2.6 (Mutation from 2.0.4)")
        self.assertEqual(child.node_index, -1)

    def test_pipeline_node_assignment_only_finalizes_negative_generated_children(self):
        parent = EvolutionLineage.initial(island_index=0, node_index=1)
        child = EvolutionLineage.crossover(
            context=EvolutionExecutionContext(island_index=0, generation_index=1),
            parent1=parent,
            parent2=EvolutionLineage.initial(island_index=0, node_index=2),
        )
        finalized = child.with_node_index(4)

        with self.assertRaisesRegex(LineageError, "node_index -1"):
            finalized.with_node_index(5)
        with self.assertRaisesRegex(LineageError, "node_index -1"):
            parent.with_node_index(5)
        with self.assertRaisesRegex(LineageError, "mutation/crossover"):
            EvolutionLineage.unknown().with_node_index(5)

    def test_pipeline_finalization_starts_at_final_population_size_and_is_sequential(self):
        initial = EvolutionLineage.initial(island_index=0, node_index=9)
        parent1 = EvolutionLineage.initial(island_index=0, node_index=1)
        parent2 = EvolutionLineage.initial(island_index=0, node_index=2)
        mutation = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=0, generation_index=2),
            parent=parent1,
        )
        crossover = EvolutionLineage.crossover(
            context=EvolutionExecutionContext(island_index=0, generation_index=2),
            parent1=parent1,
            parent2=parent2,
        )

        finalized = finalize_pipeline_lineage((initial, mutation, crossover))

        self.assertIs(finalized[0], initial)
        self.assertEqual(tuple(item.node_index for item in finalized), (9, 3, 4))
        self.assertEqual(finalized[1].parent1, "1.0.1")
        self.assertEqual(finalized[2].parent1, "1.0.1")
        self.assertEqual(finalized[2].parent2, "1.0.2")
        self.assertEqual(mutation.node_index, -1)
        self.assertEqual(crossover.node_index, -1)

    def test_pipeline_finalization_fails_closed_for_invalid_sequence_or_negative_unknown(self):
        with self.assertRaisesRegex(LineageError, "ordered sequence"):
            finalize_pipeline_lineage(iter(()))  # type: ignore[arg-type]
        with self.assertRaisesRegex(LineageError, "only EvolutionLineage"):
            finalize_pipeline_lineage((EvolutionLineage.initial(island_index=0, node_index=0), object()))  # type: ignore[arg-type]
        with self.assertRaisesRegex(LineageError, "mutation/crossover"):
            finalize_pipeline_lineage((EvolutionLineage.unknown(),))

    def test_is_same_matches_native_coordinate_only_identity(self):
        left = EvolutionLineage(
            island_index=0,
            generation_index=2,
            node_index=5,
            generation_type=SQX_GENERATION_CROSSOVER,
            parent1="1.0.1",
            parent2="1.0.2",
        )
        same_coordinates = EvolutionLineage(
            island_index=0,
            generation_index=2,
            node_index=5,
            generation_type=SQX_GENERATION_CROSSOVER,
            parent1="1.1.3",
            parent2="1.1.4",
        )
        different_node = EvolutionLineage(
            island_index=0,
            generation_index=2,
            node_index=6,
            generation_type=SQX_GENERATION_CROSSOVER,
            parent1="1.1.3",
            parent2="1.1.4",
        )

        self.assertTrue(left.is_same(same_coordinates))
        self.assertFalse(left.is_same(different_node))
        self.assertFalse(left.is_same(None))

    def test_mutation_and_crossover_require_assigned_parent_ids(self):
        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        with self.assertRaisesRegex(LineageError, "parent1"):
            EvolutionLineage.mutation(context=context, parent=EvolutionLineage.unknown())

        unfinished_parent = EvolutionLineage(
            island_index=0,
            generation_index=1,
            node_index=-1,
            generation_type=SQX_GENERATION_MUTATION,
            parent1="1.0.1",
        )
        with self.assertRaisesRegex(LineageError, "parent1"):
            EvolutionLineage.crossover(
                context=EvolutionExecutionContext(island_index=0, generation_index=2),
                parent1=unfinished_parent,
                parent2=EvolutionLineage.initial(island_index=0, node_index=2),
            )

    def test_generation_type_parent_shapes_fail_closed(self):
        cases = (
            (
                dict(
                    island_index=0,
                    generation_index=0,
                    node_index=1,
                    generation_type=SQX_GENERATION_MUTATION,
                    parent1="1.0.0",
                ),
                "positive generation",
            ),
            (
                dict(
                    island_index=0,
                    generation_index=1,
                    node_index=-1,
                    generation_type=SQX_GENERATION_MUTATION,
                    parent1="1.0.0",
                    parent2="1.0.1",
                ),
                "cannot declare parent2",
            ),
            (
                dict(
                    island_index=0,
                    generation_index=1,
                    node_index=-1,
                    generation_type=SQX_GENERATION_CROSSOVER,
                    parent1="1.0.0",
                    parent2=None,
                ),
                "parent2",
            ),
            (
                dict(
                    island_index=0,
                    generation_index=0,
                    node_index=-1,
                    generation_type=SQX_GENERATION_INITIAL,
                ),
                "assigned node index",
            ),
        )
        for kwargs, message in cases:
            with self.subTest(kwargs=kwargs):
                with self.assertRaisesRegex(LineageError, message):
                    EvolutionLineage(**kwargs)

    def test_coordinate_types_fail_closed_instead_of_python_integer_coercion(self):
        for field, value in (
            ("island_index", True),
            ("generation_index", 1.0),
            ("node_index", False),
        ):
            kwargs = dict(
                island_index=0,
                generation_index=0,
                node_index=0,
                generation_type=SQX_GENERATION_INITIAL,
            )
            kwargs[field] = value
            with self.subTest(field=field, value=value):
                with self.assertRaisesRegex(LineageError, f"{field} must be an integer"):
                    EvolutionLineage(**kwargs)

    def test_final_node_index_must_be_non_negative_exact_integer(self):
        child = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=0, generation_index=1),
            parent=EvolutionLineage.initial(island_index=0, node_index=0),
        )
        for value in (-1, True, 2.5):
            with self.subTest(value=value):
                with self.assertRaises(LineageError):
                    child.with_node_index(value)

    def test_provenance_names_exact_recovered_lineage_sources(self):
        classes = {item.class_name for item in SQX_LINEAGE_SOURCE_PROVENANCE}
        self.assertEqual(
            classes,
            {
                "GPIDs",
                "GPGenerationTypes",
                "GPGenerationalEngine",
                "NodeMutation",
                "NodeCrossover",
                "EvolutionPipeline",
            },
        )
        generational = next(
            item
            for item in SQX_LINEAGE_SOURCE_PROVENANCE
            if item.class_name == "GPGenerationalEngine"
        )
        self.assertIn("gpEvolution", generational.method)
        self.assertNotIn("runEvolution", generational.method)
        pipeline = next(
            item
            for item in SQX_LINEAGE_SOURCE_PROVENANCE
            if item.class_name == "EvolutionPipeline"
        )
        self.assertEqual(pipeline.method, "apply")
        self.assertIn("negative nodeIndex", pipeline.conclusion)
        self.assertTrue(all(item.blob_sha for item in SQX_LINEAGE_SOURCE_PROVENANCE))


if __name__ == "__main__":
    unittest.main()
