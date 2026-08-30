from random import Random

import pytest

from tradercockpit.builder import (
    SQX_NATIVE_OPERATOR_PIPELINE,
    EvolutionConfig,
    EvolutionConfigError,
    EvolutionKernel,
    SelectedParents,
    decide_variation,
    plan_islands,
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


def test_saved_native_baseline_maps_exact_proved_controls():
    config = EvolutionConfig.from_native_settings(native_settings())

    assert config.population_size_per_island == 4
    assert config.maximum_generations == 2
    assert config.crossover_probability_pct == 93
    assert config.mutation_probability_pct == 30
    assert config.island_count == 1
    assert config.planned_population_capacity == 4
    assert config.fresh_blood_replace_similar is True
    assert config.fresh_blood_replace_weakest is False


def test_native_crossover_only_and_mutation_only_variants_gate_independently():
    crossover_only = EvolutionConfig.from_native_settings(
        native_settings(crossover_probability=100, mutation_probability=0)
    )
    mutation_only = EvolutionConfig.from_native_settings(
        native_settings(crossover_probability=0, mutation_probability=100)
    )

    assert decide_variation(crossover_only, crossover_draw=0.999, mutation_draw=0.0).crossover_applied
    assert not decide_variation(crossover_only, crossover_draw=0.999, mutation_draw=0.0).mutation_applied
    assert not decide_variation(mutation_only, crossover_draw=0.0, mutation_draw=0.999).crossover_applied
    assert decide_variation(mutation_only, crossover_draw=0.0, mutation_draw=0.999).mutation_applied


def test_balanced_variant_uses_independent_probability_gates():
    config = EvolutionConfig.from_native_settings(
        native_settings(crossover_probability=50, mutation_probability=50)
    )

    decision = decide_variation(config, crossover_draw=0.49, mutation_draw=0.50)
    assert decision.crossover_applied is True
    assert decision.mutation_applied is False
    assert decision.operator_pipeline == SQX_NATIVE_OPERATOR_PIPELINE


def test_four_island_variant_preserves_per_island_population_topology():
    config = EvolutionConfig.from_native_settings(
        native_settings(islands=4, migration_modulo=87, migration_rate=6)
    )

    plans = plan_islands(config)
    assert [plan.island_index for plan in plans] == [0, 1, 2, 3]
    assert [plan.population_size for plan in plans] == [4, 4, 4, 4]
    assert config.planned_population_capacity == 16
    assert config.migration_interval == 87
    assert config.migration_rate_pct == 6


def test_fresh_weak_variant_is_preserved_as_configuration_not_invented_replacement_logic():
    config = EvolutionConfig.from_native_settings(
        native_settings(fresh_blood_replace_weakest=True)
    )
    assert config.fresh_blood_replace_weakest is True


def test_restart_modes_fail_closed_until_native_behavior_is_proved():
    with pytest.raises(EvolutionConfigError, match="restart behavior is not yet supported"):
        EvolutionConfig.from_native_settings(native_settings(restart_on_finish=True))


def test_missing_native_settings_fail_closed():
    settings = native_settings()
    settings.pop("mutation_probability")
    with pytest.raises(EvolutionConfigError, match="mutation_probability"):
        EvolutionConfig.from_native_settings(settings)


def test_invalid_probabilities_fail_closed():
    with pytest.raises(EvolutionConfigError, match="crossover probability"):
        EvolutionConfig.from_native_settings(native_settings(crossover_probability=101))


def test_kernel_invokes_in_proved_order_and_always_runs_postprocess():
    calls = []

    def select(population, rng):
        calls.append("TournamentSelection")
        return SelectedParents(population[0], population[1])

    def crossover(left, right, rng):
        calls.append("NodeCrossover")
        return f"cross({left},{right})"

    def mutate(candidate, rng):
        calls.append("NodeMutation")
        return f"mut({candidate})"

    def postprocess(candidate, rng):
        calls.append("post")
        return f"post({candidate})"

    kernel = EvolutionKernel(
        select_parents=select,
        crossover=crossover,
        mutate=mutate,
        postprocess=postprocess,
    )
    config = EvolutionConfig.from_native_settings(
        native_settings(crossover_probability=100, mutation_probability=100)
    )

    result = kernel.vary_one(["a", "b"], config, Random(7))

    assert calls == ["TournamentSelection", "NodeCrossover", "NodeMutation", "post"]
    assert result.candidate == "post(mut(cross(a,b)))"
    assert result.decision.crossover_applied is True
    assert result.decision.mutation_applied is True
