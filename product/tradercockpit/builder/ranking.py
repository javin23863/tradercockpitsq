"""Bounded StrategyQuant X Builder ranking reconstruction.

Observed SQX evidence establishes that Builder ranking orders search/optimization
candidates and that ranking/fitness alone is discovery evidence, never a
validation or champion decision. Concrete SQX objective semantics beyond the
retained reference objective are not claimed here.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
import re

from ..domain.canonical import ContentAddress
from ..domain.specs import SpecValidationError


_OBJECTIVE_RE = re.compile(r"^[a-z][a-z0-9_.-]*$")
SQX_RANKING_EVIDENCE_ROLE = "discovery"
SQX_RANKING_SUPPORTED_DIRECTIONS = frozenset({"maximize"})
DEFAULT_BUILDER_RANKING_OBJECTIVE = "construction_fit"


def _objective_token(value: str, name: str) -> str:
    if not isinstance(value, str) or not _OBJECTIVE_RE.fullmatch(value):
        raise SpecValidationError(
            f"{name} must be a non-empty lowercase objective token"
        )
    return value


@dataclass(frozen=True, slots=True)
class RankingObjectiveV1:
    """One opaque Builder ranking objective under the evidenced direction."""

    objective: str
    direction: str = "maximize"

    def __post_init__(self) -> None:
        _objective_token(self.objective, "objective")
        if self.direction not in SQX_RANKING_SUPPORTED_DIRECTIONS:
            raise SpecValidationError(
                "ranking direction is not supported by the retained SQX evidence"
            )


@dataclass(frozen=True, slots=True)
class CandidateFitnessV1:
    """Discovery-only fitness evidence bound to one immutable objective identity.

    ``construction_fit`` is the current canonical Builder product objective, so it
    remains the default for the existing search path. Any other objective must be
    supplied explicitly; :func:`order_candidates` rejects relabeling or mixed
    objective evidence before ranking.
    """

    candidate_ref: ContentAddress
    score: Decimal
    objective: str = DEFAULT_BUILDER_RANKING_OBJECTIVE

    def __post_init__(self) -> None:
        if not isinstance(self.candidate_ref, ContentAddress) or self.candidate_ref.kind != "candidate":
            raise SpecValidationError("candidate_ref must reference a candidate")
        if not isinstance(self.score, Decimal) or not self.score.is_finite():
            raise SpecValidationError("score must be a finite Decimal")
        _objective_token(self.objective, "fitness objective")

    @property
    def evidence_role(self) -> str:
        return SQX_RANKING_EVIDENCE_ROLE


def order_candidates(
    objective: RankingObjectiveV1,
    fitness: tuple[CandidateFitnessV1, ...],
) -> tuple[CandidateFitnessV1, ...]:
    """Order candidates by objective-bound Builder fitness without promoting them.

    The retained parity evidence proves maximize ordering. Equal scores are
    resolved by immutable candidate identity only to keep TraderCockpit output
    deterministic; no SQX tie-break algorithm is claimed.
    """

    if not isinstance(objective, RankingObjectiveV1):
        raise SpecValidationError("objective must be RankingObjectiveV1")
    if not isinstance(fitness, tuple):
        fitness = tuple(fitness)
    if not fitness:
        raise SpecValidationError("fitness must not be empty")
    if any(not isinstance(item, CandidateFitnessV1) for item in fitness):
        raise SpecValidationError("fitness must contain only CandidateFitnessV1 values")

    refs = [item.candidate_ref for item in fitness]
    if len(set(refs)) != len(refs):
        raise SpecValidationError("fitness must contain each candidate at most once")

    if any(item.objective != objective.objective for item in fitness):
        raise SpecValidationError(
            "fitness objective must match the ranking objective for every candidate"
        )

    return tuple(sorted(fitness, key=lambda item: (-item.score, str(item.candidate_ref))))
