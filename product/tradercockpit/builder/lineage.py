"""StrategyQuant X 144.2953 genetic-programming lineage semantics.

This module reproduces the small, source-proven GPIDs identity/lineage contract
needed by Builder evolution. It deliberately does not model strategy XML,
mutation, crossover, migration, evaluation, or result semantics.
"""

from __future__ import annotations

from dataclasses import dataclass, replace
import re
from typing import Protocol

from .evolution import SourceProvenance


SQX_GENERATION_INITIAL = "Initial"
SQX_GENERATION_MUTATION = "Mutation"
SQX_GENERATION_CROSSOVER = "Crossover"
SQX_GENERATION_UNKNOWN: None = None

_PARENT_ID_RE = re.compile(r"^[1-9][0-9]*\.[0-9]+\.[0-9]+$")

SQX_LINEAGE_SOURCE_PROVENANCE: tuple[SourceProvenance, ...] = (
    SourceProvenance(
        class_name="GPIDs",
        method="toString/toShortString/set/getClone/isSame",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPIDs.java",
        blob_sha="e32b7d2e65dea5be9f3ead3d2eee659afed277a5",
        conclusion=(
            "Lineage identity is island/generation/node coordinates; display uses a "
            "one-based island number and records mutation/crossover parents as short IDs."
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
        method="gpEvolution/generateAdditionalCandidates/addExistingInitialPopulation",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/GPGenerationalEngine.java",
        blob_sha="c5ff3193354a7168c5b5da11428a552cb1bbdc45",
        conclusion=(
            "Initial population uses generation 0/Initial; currentGeneration increments "
            "before each evolutionary step."
        ),
    ),
    SourceProvenance(
        class_name="NodeMutation",
        method="apply",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeMutation.java",
        blob_sha="ff36748ba1baa17d00a104f768a0d0e4d95d772a",
        conclusion=(
            "A changed mutation child receives current island/generation, type Mutation, "
            "parent1 as the source short ID, and an initially negative node index."
        ),
    ),
    SourceProvenance(
        class_name="NodeCrossover",
        method="mate",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/strategies/NodeCrossover.java",
        blob_sha="68a928465858f1fd211b431d6a9da919b5f9244a",
        conclusion=(
            "A changed crossover child receives current island/generation, type Crossover, "
            "both source short IDs, and an initially negative node index."
        ),
    ),
    SourceProvenance(
        class_name="EvolutionPipeline",
        method="apply",
        path="sources/engine-core/com/strategyquant/tradinglib/gp/EvolutionPipeline.java",
        blob_sha="ed5cc26702e1a31841e6f746839259ee4ee40267",
        conclusion=(
            "After all evolutionary operators, candidates with negative nodeIndex receive "
            "sequential node indices beginning at the final population size."
        ),
    ),
)


class LineageError(ValueError):
    """Raised when an SQX lineage state is malformed or internally inconsistent."""


class ExecutionCoordinates(Protocol):
    island_index: int
    generation_index: int


def _exact_int(value: int, name: str) -> int:
    if type(value) is not int:
        raise LineageError(f"{name} must be an integer")
    return value


def _parent_id(value: str | None, name: str) -> str:
    if not isinstance(value, str) or not _PARENT_ID_RE.fullmatch(value):
        raise LineageError(f"{name} must be an assigned SQX short lineage id")
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
        _exact_int(self.island_index, "island_index")
        _exact_int(self.generation_index, "generation_index")
        _exact_int(self.node_index, "node_index")

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

        _parent_id(self.parent1, "parent1")
        if self.generation_type == SQX_GENERATION_MUTATION:
            if self.parent2 is not None:
                raise LineageError("mutation lineage cannot declare parent2")
            return

        _parent_id(self.parent2, "parent2")

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
        """Reproduce ``GPIDs.toShortString`` including the one-based island display."""

        return f"{self.island_index + 1}.{self.generation_index}.{self.node_index}"

    def display_string(self) -> str:
        """Reproduce the valid assigned-state forms of ``GPIDs.toString``."""

        if self.generation_type is SQX_GENERATION_UNKNOWN:
            raise LineageError("native GPIDs.toString is not valid for Unknown/null generation type")
        short = self.short_string()
        if self.generation_type == SQX_GENERATION_MUTATION:
            return f"{short} ({self.generation_type} from {self.parent1})"
        if self.generation_type == SQX_GENERATION_CROSSOVER:
            return f"{short} ({self.generation_type} from {self.parent1}+{self.parent2})"
        return f"{short} ({self.generation_type})"

    def with_node_index(self, node_index: int) -> "EvolutionLineage":
        """Finalize a native generated child whose current node index is ``-1``."""

        _exact_int(node_index, "node_index")
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
