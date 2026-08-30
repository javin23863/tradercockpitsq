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

    def test_mutation_accepts_prefinal_crossover_parent_from_same_pipeline_generation(self):
        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        source1 = EvolutionLineage.initial(island_index=0, node_index=3)
        source2 = EvolutionLineage.initial(island_index=0, node_index=9)
        crossover = EvolutionLineage.crossover(
            context=context,
            parent1=source1,
            parent2=source2,
        )

        mutation = EvolutionLineage.mutation(context=context, parent=crossover)

        self.assertEqual(crossover.short_string(), "1.1.-1")
        self.assertEqual(mutation.parent1, "1.1.-1")
        self.assertEqual(mutation.display_string(), "1.1.-1 (Mutation from 1.1.-1)")

        finalized = finalize_pipeline_lineage((crossover, mutation))
        self.assertEqual(tuple(item.node_index for item in finalized), (2, 3))
        self.assertEqual(finalized[1].parent1, "1.1.-1")

    def test_mutation_rejects_finalized_same_context_crossover_but_allows_older_one(self):
        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        source1 = EvolutionLineage.initial(island_index=0, node_index=3)
        source2 = EvolutionLineage.initial(island_index=0, node_index=9)
        crossover = EvolutionLineage.crossover(
            context=context,
            parent1=source1,
            parent2=source2,
        )
        finalized_same_context = crossover.with_node_index(5)

        with self.assertRaisesRegex(LineageError, "same-context mutation parent"):
            EvolutionLineage.mutation(
                context=context,
                parent=finalized_same_context,
            )

        older_crossover = EvolutionLineage(
            island_index=0,
            generation_index=1,
            node_index=5,
            generation_type=SQX_GENERATION_CROSSOVER,
            parent1="1.0.1",
            parent2="1.0.2",
        )
        later = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=0, generation_index=2),
            parent=older_crossover,
        )
        self.assertEqual(later.parent1, "1.1.5")

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

    def test_crossover_rejects_non_lineage_parent_objects(self):
        class SpoofParent:
            def short_string(self):
                return "1.0.7"

        context = EvolutionExecutionContext(island_index=0, generation_index=1)
        assigned = EvolutionLineage.initial(island_index=0, node_index=2)

        with self.assertRaisesRegex(LineageError, "parent1 must be EvolutionLineage"):
            EvolutionLineage.crossover(
                context=context,
                parent1=SpoofParent(),  # type: ignore[arg-type]
                parent2=assigned,
            )
        with self.assertRaisesRegex(LineageError, "parent2 must be EvolutionLineage"):
            EvolutionLineage.crossover(
                context=context,
                parent1=assigned,
                parent2=SpoofParent(),  # type: ignore[arg-type]
            )

    def test_parent_short_ids_require_native_canonical_integer_spelling(self):
        for parent1 in ("1.01.3", "1.1.003", "01.1.3"):
            with self.subTest(kind="crossover", parent1=parent1):
                with self.assertRaisesRegex(LineageError, "assigned SQX short lineage id"):
                    EvolutionLineage(
                        island_index=0,
                        generation_index=2,
                        node_index=-1,
                        generation_type=SQX_GENERATION_CROSSOVER,
                        parent1=parent1,
                        parent2="1.1.4",
                    )

        for parent1 in ("1.01.-1", "1.1.003", "01.1.-1"):
            with self.subTest(kind="mutation", parent1=parent1):
                with self.assertRaisesRegex(
                    LineageError,
                    "mutation-parent short lineage id",
                ):
                    EvolutionLineage(
                        island_index=0,
                        generation_index=1,
                        node_index=-1,
                        generation_type=SQX_GENERATION_MUTATION,
                        parent1=parent1,
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

    def test_pipeline_finalization_rejects_mixed_pending_execution_contexts(self):
        island_zero = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=0, generation_index=2),
            parent=EvolutionLineage.initial(island_index=0, node_index=1),
        )
        island_one = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=1, generation_index=2),
            parent=EvolutionLineage.initial(island_index=1, node_index=1),
        )
        later_generation = EvolutionLineage.mutation(
            context=EvolutionExecutionContext(island_index=0, generation_index=3),
            parent=EvolutionLineage.initial(island_index=0, node_index=2),
        )

        for mixed in ((island_zero, island_one), (island_zero, later_generation)):
            with self.subTest(mixed=tuple(item.identity_key for item in mixed)):
                with self.assertRaisesRegex(LineageError, "share one island/generation"):
                    finalize_pipeline_lineage(mixed)

    def test_pipeline_finalization_fails_closed_for_invalid_sequence_or_negative_unknown(self):
        with self.assertRaisesRegex(LineageError, "ordered sequence"):
            finalize_pipeline_lineage(iter(()))  # type: ignore[arg-type]
        with self.assertRaisesRegex(LineageError, "only EvolutionLineage"):
            finalize_pipeline_lineage(
                (EvolutionLineage.initial(island_index=0, node_index=0), object())
            )  # type: ignore[arg-type]
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

    def test_crossover_requires_assigned_parents_and_mutation_limits_prefinal_parent_shape(self):
        context1 = EvolutionExecutionContext(island_index=0, generation_index=1)
        initial1 = EvolutionLineage.initial(island_index=0, node_index=1)
        initial2 = EvolutionLineage.initial(island_index=0, node_index=2)
        prefinal_crossover = EvolutionLineage.crossover(
            context=context1,
            parent1=initial1,
            parent2=initial2,
        )

        with self.assertRaisesRegex(LineageError, "assigned SQX short lineage id"):
            EvolutionLineage.crossover(
                context=EvolutionExecutionContext(island_index=0, generation_index=2),
                parent1=prefinal_crossover,
                parent2=initial2,
            )

        unfinished_mutation = EvolutionLineage.mutation(context=context1, parent=initial1)
        with self.assertRaisesRegex(LineageError, "same-context mutation parent"):
            EvolutionLineage.mutation(context=context1, parent=unfinished_mutation)

        with self.assertRaisesRegex(LineageError, "preceding crossover"):
            EvolutionLineage.mutation(context=context1, parent=EvolutionLineage.unknown())

        with self.assertRaisesRegex(LineageError, "match child island/generation"):
            EvolutionLineage(
                island_index=0,
                generation_index=2,
                node_index=-1,
                generation_type=SQX_GENERATION_MUTATION,
                parent1="1.1.-1",
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
                "GeneticBuildEngine",
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
        self.assertIn("generateInitialPopulation", generational.method)
        self.assertIn("generateAdditionalCandidates", generational.method)
        self.assertIn("addExistingInitialPopulation", generational.method)
        self.assertNotIn("runEvolution", generational.method)
        builder = next(
            item
            for item in SQX_LINEAGE_SOURCE_PROVENANCE
            if item.class_name == "GeneticBuildEngine"
        )
        self.assertEqual(builder.method, "getGPSettings")
        self.assertEqual(builder.blob_sha, "bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61")
        self.assertIn("NodeCrossover immediately before NodeMutation", builder.conclusion)
        mutation = next(
            item
            for item in SQX_LINEAGE_SOURCE_PROVENANCE
            if item.class_name == "NodeMutation"
        )
        self.assertIn("nodeIndex -1", mutation.conclusion)
        pipeline = next(
            item
            for item in SQX_LINEAGE_SOURCE_PROVENANCE
            if item.class_name == "EvolutionPipeline"
        )
        self.assertEqual(pipeline.method, "apply")
        self.assertIn("one island/generation context", pipeline.conclusion)
        self.assertTrue(all(item.blob_sha for item in SQX_LINEAGE_SOURCE_PROVENANCE))


if __name__ == "__main__":
    unittest.main()
