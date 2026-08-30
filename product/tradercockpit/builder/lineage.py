"""StrategyQuant X 144.2953 genetic-programming lineage semantics.

This module reproduces the source-proven GPIDs identity/lineage contract needed
by Builder evolution. It deliberately does not model strategy XML or tree-edit
semantics, but it does preserve the Java-int representation boundary because
lineage IDs are externally observable in candidate names, job IDs, migration,
and parent strings.
"""

from __future__ import annotations

from dataclasses import dataclass, replace
import re
from typing import Protocol, Sequence

from .evolution import SourceProvenance


JAVA_INT_MIN = -(2**31)
JAVA_INT_MAX = 2**31 - 1
SQX_GENERATION_INITIAL = "Initial"
SQX_GENERATION_MUTATION = "Mutation"
SQX_GENERATION_CROSSOVER = "Crossover"
SQX_GENERATION_UNKNOWN: None = None

_CANONICAL_NONNEGATIVE = r"(?:0|[1-9][0-9]*)"
_ASSIGNED_PARENT_ID_RE = re.compile(
    rf"^([1-9][0-9]*)\.({_CANONICAL_NONNEGATIVE})\.({_CANONICAL_NONNEGATIVE})$"
)
_MUTATION_PARENT_ID_RE = re.compile(
    rf"^([1-9][0-9]*)\.({_CANONICAL_NONNEGATIVE})\.(-1|{_CANONICAL_NONNEGATIVE})$"
)

SQX_LINEAGE_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="GPIDs",
        method="toString/toShortString/set/getClone/isSame",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPIDs.java",
        blob_sha="e32b7d2e65dea5be9f3ead3d2eee659afed277a5",
        conclusion=(
            "Lineage identity is stored in Java int island/generation/node coordinates; "
            "display uses islandIndex+1 and mutation/crossover parents are stored as "
            "short-ID strings. isSame compares coordinates only."
        ),
    ),
    SourceProvenance(
        class_name="GPGenerationTypes",
        method="constants",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPGenerationTypes.java",
        blob_sha="4644e04c56d12f241c9079f77a85803c1f3bf2f1",
        conclusion="Generation types are Initial, Mutation, Crossover, and null Unknown.",
    ),
    SourceProvenance(
        class_name="GPGenerationalEngine",
        method=(
            "gpEvolution/generateInitialPopulation/generateAdditionalCandidates/"
            "addExistingInitialPopulation"
        ),
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPGenerationalEngine.java",
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "Initial population uses generation 0/Initial; currentGeneration increments "
            "before each evolutionary step."
        ),
    ),
    SourceProvenance(
        class_name="GeneticBuildEngine",
        method="getGPSettings",
        path="sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/GeneticBuildEngine.java",
        blob_sha="bfb72b3e0d9b72c4a989ae32bb62f33cacce1d61",
        conclusion=(
            "Constructs the Builder EvolutionPipeline with NodeCrossover immediately "
            "before NodeMutation, so mutation can consume an unfinalized crossover child."
        ),
    ),
    SourceProvenance(
        class_name="NodeMutation",
        method="apply",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeMutation.java",
        blob_sha="ff36748ba1baa17d00a104f768a0d0e4d95d772a",
        conclusion=(
            "A changed mutation child receives current island/generation, type Mutation, "
            "and parent1 as its input candidate's short ID. Because mutation follows "
            "crossover in the same EvolutionPipeline, that parent short ID can still "
            "carry nodeIndex -1 until pipeline finalization."
        ),
    ),
    SourceProvenance(
        class_name="NodeCrossover",
        method="mate",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeCrossover.java",
        blob_sha="68a928465858f1fd211b431d6a9da919b5f9244a",
        conclusion=(
            "A changed crossover child receives current island/generation, type Crossover, "
            "both source short IDs, and the GPIDs default node index -1."
        ),
    ),
    SourceProvenance(
        class_name="EvolutionPipeline",
        method="apply",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/EvolutionPipeline.java",
        blob_sha="ed5cc26702e1a31841e6f746839259ee4ee40267",
        conclusion=(
            "Forwards one island/generation context through every operator, then initializes "
            "the next node index to final population size and assigns consecutive Java-int "
            "indices to outputs whose nodeIndex remains negative."
        ),
    ),
)


class LineageError(ValueError):
    """Raised when an SQX lineage state is malformed or internally inconsistent."""


class ExecutionCoordinates(Protocol):
    island_index: int
    generation_index: int


def _java_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise LineageError(f"{name} must be an integer")
    if not JAVA_INT_MIN <= value <= JAVA_INT_MAX:
        raise LineageError(f"{name} must fit a signed Java int")
    return value


def _validate_parent_components(match: re.Match[str], name: str) -> tuple[int, int, int]:
    display_island = int(match.group(1))
    generation = int(match.group(2))
    node = int(match.group(3))
    # Assigned native parent IDs originate from nonnegative islandIndex with a
    # one-based display. TraderCockpit refuses islandIndex==Integer.MAX_VALUE,
    # because native islandIndex+1 would overflow to a negative displayed ID.
    if display_island > JAVA_INT_MAX:
        raise LineageError(f"{name} contains an island id outside native Java-int display range")
    if generation > JAVA_INT_MAX:
        raise LineageError(f"{name} contains a generation outside native Java-int range")
    if node > JAVA_INT_MAX:
        raise LineageError(f"{name} contains a node index outside native Java-int range")
    return display_island, generation, node


def _assigned_parent_id(value: str | None, name: str) -> str:
    if not isinstance(value, str):
        raise LineageError(f"{name} must be an assigned SQX short lineage id")
    match = _ASSIGNED_PARENT_ID_RE.fullmatch(value)
    if match is None:
        raise LineageError(f"{name} must be an assigned SQX short lineage id")
    _validate_parent_components(match, name)
    return value


def _mutation_parent_id(
    value: str | None,
    name: str,
    *,
    island_index: int,
    generation_index: int,
) -> str:
    if not isinstance(value, str):
        raise LineageError(f"{name} must be an SQX mutation-parent short lineage id")
    match = _MUTATION_PARENT_ID_RE.fullmatch(value)
    if match is None:
        raise LineageError(f"{name} must be an SQX mutation-parent short lineage id")

    parent_display_island, parent_generation, node_index = _validate_parent_components(
        match, name
    )
    if node_index == -1:
        if (
            parent_display_island != island_index + 1
            or parent_generation != generation_index
        ):
            raise LineageError(
                f"{name} pre-final SQX lineage id must match child island/generation"
            )
    return value


@dataclass(frozen=True, slots=True)
class EvolutionLineage:
    """TraderCockpit-owned equivalent of SQX ``GPIDs`` for GA candidates."""

    island_index: int = -1
    generation_index: int = -1
    node_index: int = -1
    generation_type: str | None = SQX_GENERATION_UNKNOWN
    parent1: str | None = None
    parent2: str | None = None

    def __post_init__(self) -> None:
        _java_int(self.island_index, "island_index")
        _java_int(self.generation_index, "generation_index")
        _java_int(self.node_index, "node_index")

        allowed = {
            SQX_GENERATION_UNKNOWN,
            SQX_GENERATION_INITIAL,
            SQX_GENERATION_MUTATION,
            SQX_GENERATION_CROSSOVER,
        }
        if self.generation_type not in allowed:
            raise LineageError("generation_type is not an SQX 144.2953 generation type")

        if self.generation_type is SQX_GENERATION_UNKNOWN:
            if (self.island_index, self.generation_index, self.node_index) != (-1, -1, -1):
                raise LineageError("unknown lineage must retain SQX's -1 coordinate sentinels")
            if self.parent1 is not None or self.parent2 is not None:
                raise LineageError("unknown lineage cannot declare parents")
            return

        if self.island_index < 0:
            raise LineageError("assigned lineage island_index must not be negative")
        if self.island_index >= JAVA_INT_MAX:
            raise LineageError(
                "assigned lineage island_index must keep one-based SQX display within Java int"
            )
        if self.generation_index < 0:
            raise LineageError("assigned lineage generation_index must not be negative")
        if self.node_index < -1:
            raise LineageError("assigned lineage node_index must be -1 or non-negative")

        if self.generation_type == SQX_GENERATION_INITIAL:
            if self.generation_index != 0:
                raise LineageError("initial lineage must use generation 0")
            if self.node_index < 0:
                raise LineageError("initial lineage must have an assigned node index")
            if self.parent1 is not None or self.parent2 is not None:
                raise LineageError("initial lineage cannot declare parents")
            return

        if self.generation_index == 0:
            raise LineageError("mutation/crossover lineage must use a positive generation")

        if self.generation_type == SQX_GENERATION_MUTATION:
            _mutation_parent_id(
                self.parent1,
                "parent1",
                island_index=self.island_index,
                generation_index=self.generation_index,
            )
            if self.parent2 is not None:
                raise LineageError("mutation lineage cannot declare parent2")
            return

        _assigned_parent_id(self.parent1, "parent1")
        _assigned_parent_id(self.parent2, "parent2")

    @classmethod
    def unknown(cls) -> "EvolutionLineage":
        return cls()

    @classmethod
    def initial(cls, *, island_index: int, node_index: int) -> "EvolutionLineage":
        return cls(
            island_index=island_index,
            generation_index=0,
            node_index=node_index,
            generation_type=SQX_GENERATION_INITIAL,
        )

    @classmethod
    def mutation(
        cls,
        *,
        context: ExecutionCoordinates,
        parent: "EvolutionLineage",
    ) -> "EvolutionLineage":
        if not isinstance(parent, EvolutionLineage):
            raise LineageError("parent must be EvolutionLineage")

        same_context = (
            parent.island_index == context.island_index
            and parent.generation_index == context.generation_index
        )
        if same_context:
            if (
                parent.generation_type != SQX_GENERATION_CROSSOVER
                or parent.node_index != -1
            ):
                raise LineageError(
                    "same-context mutation parent must be the pre-final crossover output"
                )
        elif parent.node_index == -1:
            raise LineageError(
                "pre-final mutation parent must be the preceding crossover output"
            )

        return cls(
            island_index=context.island_index,
            generation_index=context.generation_index,
            node_index=-1,
            generation_type=SQX_GENERATION_MUTATION,
            parent1=parent.short_string(),
        )

    @classmethod
    def crossover(
        cls,
        *,
        context: ExecutionCoordinates,
        parent1: "EvolutionLineage",
        parent2: "EvolutionLineage",
    ) -> "EvolutionLineage":
        if not isinstance(parent1, EvolutionLineage):
            raise LineageError("parent1 must be EvolutionLineage")
        if not isinstance(parent2, EvolutionLineage):
            raise LineageError("parent2 must be EvolutionLineage")
        return cls(
            island_index=context.island_index,
            generation_index=context.generation_index,
            node_index=-1,
            generation_type=SQX_GENERATION_CROSSOVER,
            parent1=parent1.short_string(),
            parent2=parent2.short_string(),
        )

    @property
    def identity_key(self) -> tuple[int, int, int]:
        """Coordinates used by native ``GPIDs.isSame``."""

        return (self.island_index, self.generation_index, self.node_index)

    def is_same(self, other: object) -> bool:
        return isinstance(other, EvolutionLineage) and self.identity_key == other.identity_key

    def short_string(self) -> str:
        """Reproduce valid ``GPIDs.toShortString`` one-based island display."""

        return f"{self.island_index + 1}.{self.generation_index}.{self.node_index}"

    def display_string(self) -> str:
        """Reproduce the valid assigned-state forms of ``GPIDs.toString``."""

        if self.generation_type is SQX_GENERATION_UNKNOWN:
            # Native calls generationType.equals(...) and would NPE for Unknown.
            # TraderCockpit exposes an explicit refusal instead of reproducing the crash.
            raise LineageError("native GPIDs.toString is not valid for Unknown/null generation type")
        short = self.short_string()
        if self.generation_type == SQX_GENERATION_MUTATION:
            return f"{short} ({self.generation_type} from {self.parent1})"
        if self.generation_type == SQX_GENERATION_CROSSOVER:
            return f"{short} ({self.generation_type} from {self.parent1}+{self.parent2})"
        return f"{short} ({self.generation_type})"

    def with_node_index(self, node_index: int) -> "EvolutionLineage":
        """Finalize a native generated child whose current node index is ``-1``."""

        _java_int(node_index, "node_index")
        if node_index < 0:
            raise LineageError("final node_index must not be negative")
        if self.node_index != -1:
            raise LineageError("only pre-final lineage with node_index -1 can be finalized")
        if self.generation_type not in {
            SQX_GENERATION_MUTATION,
            SQX_GENERATION_CROSSOVER,
        }:
            raise LineageError("only mutation/crossover lineage can be finalized")
        return replace(self, node_index=node_index)


def finalize_pipeline_lineage(
    lineages: Sequence[EvolutionLineage],
) -> tuple[EvolutionLineage, ...]:
    """Apply SQX ``EvolutionPipeline.apply`` node-index finalization immutably.

    SQX initializes the next generated node index to the final pipeline population
    size, then walks one island/generation pipeline population in order and assigns
    consecutive indices to entries whose ``nodeIndex`` is still negative.
    TraderCockpit keeps lineage immutable, so finalized children are returned as
    replacements while already assigned entries are returned unchanged.
    """

    if not isinstance(lineages, Sequence) or isinstance(lineages, (str, bytes, bytearray)):
        raise LineageError("lineages must be an ordered sequence of EvolutionLineage values")
    if any(not isinstance(lineage, EvolutionLineage) for lineage in lineages):
        raise LineageError("lineages must contain only EvolutionLineage values")
    if len(lineages) > JAVA_INT_MAX:
        raise LineageError("final pipeline population size exceeds SQX Java-int capacity")

    pending_contexts = {
        (lineage.island_index, lineage.generation_index)
        for lineage in lineages
        if lineage.node_index < 0
    }
    if len(pending_contexts) > 1:
        raise LineageError(
            "pre-final pipeline lineage must share one island/generation context"
        )

    pending_count = sum(1 for lineage in lineages if lineage.node_index < 0)
    if pending_count and len(lineages) + pending_count - 1 > JAVA_INT_MAX:
        raise LineageError("pipeline node-index finalization would overflow SQX Java int")

    next_node_index = len(lineages)
    finalized: list[EvolutionLineage] = []
    for lineage in lineages:
        if lineage.node_index < 0:
            lineage = lineage.with_node_index(next_node_index)
            next_node_index += 1
        finalized.append(lineage)
    return tuple(finalized)
