"""SQX 144.2953 island-migration count and topology semantics.

This module reproduces only source-proven migration scheduling, ring routing,
inbox capacity, and receive counts. Candidate cloning, lineage mutation, random
inbox identity selection, and fitness sorting remain outside this bounded slice.
"""

from __future__ import annotations

from dataclasses import dataclass

from .evolution import EvolutionConfig, SourceProvenance


SQX_MIGRATION_INBOX_FRACTION = 0.2


class MigrationError(ValueError):
    """Raised when migration inputs are malformed or internally inconsistent."""


SQX_MIGRATION_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="GeneticBuildEngine",
        method="getGPSettings",
        path=(
            "sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/"
            "GeneticBuildEngine.java"
        ),
        blob_sha="bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61",
        conclusion=(
            "Builder maps island count, migration rate, and migration generation "
            "modulo into GPSettings."
        ),
    ),
    SourceProvenance(
        class_name="GPGenerationalEngine",
        method=(
            "migrateIndividuals/sendMigrationCandidates/addToInbox/"
            "receiveImmigrants/shrinkTo"
        ),
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/gp/"
            "GPGenerationalEngine.java"
        ),
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "Positive-rate migration sends a cadence-gated prefix to the next island "
            "in a ring, caps inboxes at 20% of configured population (minimum one), "
            "and later replaces at most half of the current population before clearing "
            "the inbox and sorting again."
        ),
    ),
    SourceProvenance(
        class_name="GPIslandJob",
        method="messageReceived/createJobID",
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/gp/GPIslandJob.java"
        ),
        blob_sha="f80f5731873cb41f5b36770d8fd6ac242bfd6fb8",
        conclusion=(
            "GBIslandExchange messages are addressed to the destination island job "
            "and delivered directly to GPGenerationalEngine.addToInbox."
        ),
    ),
)


def _exact_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise MigrationError(f"{name} must be an integer")
    return value


def _require_config(config: EvolutionConfig) -> EvolutionConfig:
    if not isinstance(config, EvolutionConfig):
        raise MigrationError("config must be EvolutionConfig")
    return config


@dataclass(frozen=True, slots=True)
class MigrationSendPlan:
    source_island_index: int
    destination_island_index: int | None
    current_generation: int
    current_population_size: int
    scheduled: bool
    migration_count: int
    source_positions: tuple[int, ...]


def plan_migration_send(
    config: EvolutionConfig,
    *,
    source_island_index: int,
    current_generation: int,
    current_population_size: int,
) -> MigrationSendPlan:
    """Plan native ``migrateIndividuals`` and next-island ring routing."""

    config = _require_config(config)
    island_index = _exact_int(source_island_index, "source_island_index")
    generation = _exact_int(current_generation, "current_generation")
    population_size = _exact_int(
        current_population_size, "current_population_size"
    )

    if not 0 <= island_index < config.island_count:
        raise MigrationError("source_island_index is outside configured islands")
    if generation < 0:
        raise MigrationError("current_generation must not be negative")
    if population_size < 0:
        raise MigrationError("current_population_size must not be negative")

    destination = (
        None
        if config.island_count == 1
        else (island_index + 1) % config.island_count
    )
    scheduled = (
        config.island_count != 1
        and config.migration_rate_pct != 0
        and generation != 0
        and generation % config.migration_interval == 0
    )

    migration_count = 0
    if scheduled:
        if population_size == 0:
            raise MigrationError(
                "scheduled positive-rate migration requires a non-empty population"
            )
        migration_count = int(
            population_size * (config.migration_rate_pct / 100.0)
        )
        if config.migration_rate_pct > 0 and migration_count == 0:
            migration_count = 1

    return MigrationSendPlan(
        source_island_index=island_index,
        destination_island_index=destination,
        current_generation=generation,
        current_population_size=population_size,
        scheduled=scheduled,
        migration_count=migration_count,
        source_positions=tuple(range(migration_count)),
    )


def migration_inbox_capacity(population_size_per_island: int) -> int:
    """Reproduce ``addToInbox``'s configured-population 20% cap."""

    population_size = _exact_int(
        population_size_per_island, "population_size_per_island"
    )
    if population_size <= 0:
        raise MigrationError("population_size_per_island must be positive")
    capacity = int(SQX_MIGRATION_INBOX_FRACTION * population_size)
    return 1 if capacity == 0 else capacity


@dataclass(frozen=True, slots=True)
class MigrationInboxPlan:
    configured_population_size: int
    existing_inbox_count: int
    incoming_count: int
    capacity: int
    retained_count: int
    discarded_count: int
    requires_default_shuffle: bool


def plan_migration_inbox_add(
    *,
    configured_population_size: int,
    existing_inbox_count: int,
    incoming_count: int,
) -> MigrationInboxPlan:
    """Plan native inbox size effects without inventing Java shuffle identity."""

    existing = _exact_int(existing_inbox_count, "existing_inbox_count")
    incoming = _exact_int(incoming_count, "incoming_count")
    if existing < 0:
        raise MigrationError("existing_inbox_count must not be negative")
    if incoming < 0:
        raise MigrationError("incoming_count must not be negative")

    capacity = migration_inbox_capacity(configured_population_size)
    total = existing + incoming
    retained = min(total, capacity)
    return MigrationInboxPlan(
        configured_population_size=configured_population_size,
        existing_inbox_count=existing,
        incoming_count=incoming,
        capacity=capacity,
        retained_count=retained,
        discarded_count=total - retained,
        requires_default_shuffle=total > capacity,
    )


@dataclass(frozen=True, slots=True)
class MigrationReceivePlan:
    current_generation: int
    migration_interval: int
    current_population_size: int
    inbox_count: int
    eligible: bool
    replacement_limit: int
    immigrants_applied: int
    inbox_discarded: int
    inbox_remaining_after: int
    removed_population_positions: tuple[int, ...]


def plan_migration_receive(
    config: EvolutionConfig,
    *,
    current_generation: int,
    current_population_size: int,
    inbox_count: int,
) -> MigrationReceivePlan:
    """Plan native ``receiveImmigrants`` count/custody effects."""

    config = _require_config(config)
    generation = _exact_int(current_generation, "current_generation")
    population_size = _exact_int(
        current_population_size, "current_population_size"
    )
    inbox = _exact_int(inbox_count, "inbox_count")

    if generation < 0:
        raise MigrationError("current_generation must not be negative")
    if population_size < 0:
        raise MigrationError("current_population_size must not be negative")
    if inbox < 0:
        raise MigrationError("inbox_count must not be negative")

    eligible = generation >= config.migration_interval and inbox > 0
    replacement_limit = population_size // 2

    if not eligible:
        return MigrationReceivePlan(
            current_generation=generation,
            migration_interval=config.migration_interval,
            current_population_size=population_size,
            inbox_count=inbox,
            eligible=False,
            replacement_limit=replacement_limit,
            immigrants_applied=0,
            inbox_discarded=0,
            inbox_remaining_after=inbox,
            removed_population_positions=(),
        )

    applied = min(inbox, replacement_limit)
    return MigrationReceivePlan(
        current_generation=generation,
        migration_interval=config.migration_interval,
        current_population_size=population_size,
        inbox_count=inbox,
        eligible=True,
        replacement_limit=replacement_limit,
        immigrants_applied=applied,
        inbox_discarded=inbox - applied,
        inbox_remaining_after=0,
        removed_population_positions=tuple(
            population_size - 1 - index for index in range(applied)
        ),
    )
