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


def _validate_objective_token(value: object) -> str:
    if not isinstance(value, str) or not _OBJECTIVE_RE.fullmatch(value):
        raise SpecValidationError(
            "objective must be a non-empty lowercase objective token"
        )
    return value


@dataclass(frozen=True, slots=True)
class RankingObjectiveV1:
    """One opaque Builder ranking objective under the evidenced direction."""

    objective: str
    direction: str = "maximize"

    def __post_init__(self) -> None:
        _validate_objective_token(self.objective)
        if self.direction not in SQX_RANKING_SUPPORTED_DIRECTIONS:
            raise SpecValidationError(
                "ranking direction is not supported by the retained SQX evidence"
            )


@dataclass(frozen=True, slots=True)
class CandidateFitnessV1:
    """Discovery-only fitness evidence for one immutable candidate and objective."""

    candidate_ref: ContentAddress
    objective: str
    score: Decimal

    def __post_init__(self) -> None:
        if (
            not isinstance(self.candidate_ref, ContentAddress)
            or self.candidate_ref.kind != "candidate"
        ):
            raise SpecValidationError("candidate_ref must reference a candidate")
        _validate_objective_token(self.objective)
        if not isinstance(self.score, Decimal) or not self.score.is_finite():
            raise SpecValidationError("score must be a finite Decimal")

    @property
    def evidence_role(self) -> str:
        return SQX_RANKING_EVIDENCE_ROLE


def order_candidates(
    objective: RankingObjectiveV1,
    fitness: tuple[CandidateFitnessV1, ...],
) -> tuple[CandidateFitnessV1, ...]:
    """Order candidates by evidenced Builder fitness without promoting them.

    The retained parity evidence proves maximize ordering. Each score is bound to
    the opaque objective identity it measures; relabeling fitness from another
    objective therefore fails closed instead of silently changing evidence
    meaning. Equal scores are resolved by immutable candidate identity only to
    keep TraderCockpit output deterministic; no SQX tie-break algorithm is
    claimed.
    """

    if not isinstance(objective, RankingObjectiveV1):
        raise SpecValidationError("objective must be RankingObjectiveV1")
    if not isinstance(fitness, tuple):
        fitness = tuple(fitness)
    if not fitness:
        raise SpecValidationError("fitness must not be empty")
    if any(not isinstance(item, CandidateFitnessV1) for item in fitness):
        raise SpecValidationError(
            "fitness must contain only CandidateFitnessV1 values"
        )
    if any(item.objective != objective.objective for item in fitness):
        raise SpecValidationError(
            "fitness objective must match the ranking objective"
        )

    refs = [item.candidate_ref for item in fitness]
    if len(set(refs)) != len(refs):
        raise SpecValidationError("fitness must contain each candidate at most once")

    return tuple(
        sorted(fitness, key=lambda item: (-item.score, str(item.candidate_ref)))
    )
