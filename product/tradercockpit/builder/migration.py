"""SQX 144.2953 island-migration topology, count, and custody mechanics.

The contract below preserves source-visible send scheduling, ring routing,
clone custody, inbox capacity/shuffle behavior, receive replacement, and re-sort
requirements. Java default-shuffle survivor identity remains intentionally
unknown because SQX does not use the configured GA RNG for that shuffle.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Generic, Sequence, TypeVar

from .evolution import EvolutionConfig, SourceProvenance


CandidateT = TypeVar("CandidateT")
JAVA_INT_MIN = -(2**31)
JAVA_INT_MAX = 2**31 - 1
SQX_MIGRATION_INBOX_FRACTION = 0.2


class MigrationError(ValueError):
    """Raised when migration inputs are malformed or internally inconsistent."""


@dataclass(frozen=True, slots=True)
class MigrationCloneContract:
    """Recovered `Node.cloneForMigration()` payload/custody semantics."""

    creates_distinct_candidate: bool = True
    strategy_xml_deep_cloned: bool = True
    fitness_preserved: bool = True
    gpids_lineage_preserved: bool = True
    error_and_exception_preserved: bool = True
    duration_stats_preserved: bool = True
    modified_flag_preserved: bool = True
    results_group_preserved: bool = False
    sender_retains_source_candidate: bool = True


SQX_MIGRATION_CLONE_CONTRACT = MigrationCloneContract()


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
            "Builder maps island count and migration modulo into GPSettings and "
            "converts the displayed integer migration percentage with pctToDouble."
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
            "Positive-rate migration sends cadence-gated cloneForMigration copies of "
            "a population prefix to the next island, caps inboxes at 20% of configured "
            "population with default Collections.shuffle on overflow, receives once the "
            "generation reaches the interval, replaces at most half of current population, "
            "clears the inbox, and sorts the resulting population."
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
    SourceProvenance(
        class_name="Node",
        method="cloneForMigration",
        path=(
            "sources/engine-core/com/strategyquant/tradinglib/gp/strategies/Node.java"
        ),
        blob_sha="7439f1858207a481877481e3711e4f0392e2b7e3",
        conclusion=(
            "Migration cloning deep-clones strategy XML, preserves fitness/GPIDs/error/"
            "duration/modified state, and deliberately leaves ResultsGroup null."
        ),
    ),
)


def _java_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise MigrationError(f"{name} must be an integer")
    if not JAVA_INT_MIN <= value <= JAVA_INT_MAX:
        raise MigrationError(f"{name} must fit a signed Java int")
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
    clone_contract: MigrationCloneContract


def plan_migration_send(
    config: EvolutionConfig,
    *,
    source_island_index: int,
    current_generation: int,
    current_population_size: int,
) -> MigrationSendPlan:
    """Plan native ``migrateIndividuals`` and next-island ring routing."""

    config = _require_config(config)
    island_index = _java_int(source_island_index, "source_island_index")
    generation = _java_int(current_generation, "current_generation")
    population_size = _java_int(
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
        # Native Java would attempt population.get(0) after its minimum-one rule
        # and fail for an empty population. TraderCockpit refuses that impossible
        # normal GA state explicitly instead.
        if population_size == 0:
            raise MigrationError(
                "scheduled positive-rate migration requires a non-empty population "
                "(TraderCockpit safety boundary)"
            )
        migration_count = int(
            population_size * (config.migration_rate_pct / 100.0)
        )
        if config.migration_rate_pct > 0 and migration_count == 0:
            migration_count = 1
        if migration_count > population_size:
            raise MigrationError("migration_count exceeds current population")

    return MigrationSendPlan(
        source_island_index=island_index,
        destination_island_index=destination,
        current_generation=generation,
        current_population_size=population_size,
        scheduled=scheduled,
        migration_count=migration_count,
        source_positions=tuple(range(migration_count)),
        clone_contract=SQX_MIGRATION_CLONE_CONTRACT,
    )


@dataclass(frozen=True, slots=True)
class MigrationMaterializationResult(Generic[CandidateT]):
    source_positions: tuple[int, ...]
    migrants: tuple[CandidateT, ...]
    sender_population_unchanged: bool


def materialize_migration_candidates(
    population: Sequence[CandidateT],
    plan: MigrationSendPlan,
    *,
    clone_for_migration: Callable[[CandidateT], CandidateT],
) -> MigrationMaterializationResult[CandidateT]:
    """Materialize source-prefix migration clones through an injected node adapter.

    The callback is the TraderCockpit boundary that must implement the recovered
    ``Node.cloneForMigration`` payload contract. This function verifies the
    source-position/count custody and that each migrant is a distinct object.
    """

    if not isinstance(population, Sequence) or isinstance(
        population, (str, bytes, bytearray)
    ):
        raise MigrationError("population must be an ordered candidate sequence")
    if not isinstance(plan, MigrationSendPlan):
        raise MigrationError("plan must be MigrationSendPlan")
    if len(population) != plan.current_population_size:
        raise MigrationError("population length does not match migration send plan")
    if not callable(clone_for_migration):
        raise MigrationError("clone_for_migration must be callable")

    migrants: list[CandidateT] = []
    for position in plan.source_positions:
        source = population[position]
        clone = clone_for_migration(source)
        if clone is None or clone is source:
            raise MigrationError(
                "clone_for_migration must return a distinct migration candidate"
            )
        migrants.append(clone)

    return MigrationMaterializationResult(
        source_positions=plan.source_positions,
        migrants=tuple(migrants),
        sender_population_unchanged=True,
    )


def migration_inbox_capacity(population_size_per_island: int) -> int:
    """Reproduce ``addToInbox``'s configured-population 20% cap."""

    population_size = _java_int(
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
    retained_identity_known: bool
    native_trim_calls_destroy: bool


def plan_migration_inbox_add(
    *,
    configured_population_size: int,
    existing_inbox_count: int,
    incoming_count: int,
) -> MigrationInboxPlan:
    """Plan native inbox custody without inventing Java default-shuffle identity."""

    configured = _java_int(
        configured_population_size, "configured_population_size"
    )
    existing = _java_int(existing_inbox_count, "existing_inbox_count")
    incoming = _java_int(incoming_count, "incoming_count")
    if configured <= 0:
        raise MigrationError("configured_population_size must be positive")
    if existing < 0:
        raise MigrationError("existing_inbox_count must not be negative")
    if incoming < 0:
        raise MigrationError("incoming_count must not be negative")
    total = existing + incoming
    if total > JAVA_INT_MAX:
        raise MigrationError("migration inbox count exceeds signed Java int capacity")

    capacity = migration_inbox_capacity(configured)
    retained = min(total, capacity)
    requires_shuffle = total > capacity
    return MigrationInboxPlan(
        configured_population_size=configured,
        existing_inbox_count=existing,
        incoming_count=incoming,
        capacity=capacity,
        retained_count=retained,
        discarded_count=total - retained,
        requires_default_shuffle=requires_shuffle,
        retained_identity_known=not requires_shuffle,
        native_trim_calls_destroy=False,
    )


@dataclass(frozen=True, slots=True)
class MigrationReceivePlan:
    current_generation: int
    migration_interval: int
    current_population_size: int
    inbox_count: int
    source_inbox_capacity: int
    eligible: bool
    replacement_limit: int
    immigrants_applied: int
    inbox_discarded: int
    inbox_remaining_after: int
    removed_population_positions: tuple[int, ...]
    removed_population_candidates_destroyed: int
    native_unapplied_inbox_candidates_destroyed: int
    requires_population_resort: bool
    immigrant_clone_contract: MigrationCloneContract


def plan_migration_receive(
    config: EvolutionConfig,
    *,
    current_generation: int,
    current_population_size: int,
    inbox_count: int,
) -> MigrationReceivePlan:
    """Plan native ``receiveImmigrants`` count/custody effects.

    The canonical path enforces the upstream ``addToInbox`` cap. Accepting a
    larger inbox here would model a private method in an impossible state rather
    than the SQX migration workflow actually wired by ``GPIslandJob``.
    """

    config = _require_config(config)
    generation = _java_int(current_generation, "current_generation")
    population_size = _java_int(
        current_population_size, "current_population_size"
    )
    inbox = _java_int(inbox_count, "inbox_count")

    if generation < 0:
        raise MigrationError("current_generation must not be negative")
    if population_size < 0:
        raise MigrationError("current_population_size must not be negative")
    if inbox < 0:
        raise MigrationError("inbox_count must not be negative")

    inbox_capacity = migration_inbox_capacity(config.population_size_per_island)
    if inbox > inbox_capacity:
        raise MigrationError(
            "inbox_count exceeds source-proven addToInbox capacity"
        )

    # Native receive is threshold-gated, not modulo-gated. It can consume an
    # asynchronously delivered inbox on any generation at/after the interval.
    eligible = generation >= config.migration_interval and inbox > 0
    replacement_limit = population_size // 2

    if not eligible:
        return MigrationReceivePlan(
            current_generation=generation,
            migration_interval=config.migration_interval,
            current_population_size=population_size,
            inbox_count=inbox,
            source_inbox_capacity=inbox_capacity,
            eligible=False,
            replacement_limit=replacement_limit,
            immigrants_applied=0,
            inbox_discarded=0,
            inbox_remaining_after=inbox,
            removed_population_positions=(),
            removed_population_candidates_destroyed=0,
            native_unapplied_inbox_candidates_destroyed=0,
            requires_population_resort=False,
            immigrant_clone_contract=SQX_MIGRATION_CLONE_CONTRACT,
        )

    applied = min(inbox, replacement_limit)
    unapplied = inbox - applied
    return MigrationReceivePlan(
        current_generation=generation,
        migration_interval=config.migration_interval,
        current_population_size=population_size,
        inbox_count=inbox,
        source_inbox_capacity=inbox_capacity,
        eligible=True,
        replacement_limit=replacement_limit,
        immigrants_applied=applied,
        inbox_discarded=unapplied,
        inbox_remaining_after=0,
        removed_population_positions=tuple(
            population_size - 1 - index for index in range(applied)
        ),
        removed_population_candidates_destroyed=applied,
        # Source clears unapplied inbox entries without calling node.destroy().
        # TraderCockpit need not reproduce that resource-management quirk, but it
        # must not falsely claim native destruction occurred.
        native_unapplied_inbox_candidates_destroyed=0,
        requires_population_resort=True,
        immigrant_clone_contract=SQX_MIGRATION_CLONE_CONTRACT,
    )
