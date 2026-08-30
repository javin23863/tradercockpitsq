"""Discovery-only ranking contracts for SQX-backed candidate ordering.

The retained SQX parity evidence proves only that a configured objective orders
search/optimization candidates and that ranking alone is not validation or
promotion authority. This module intentionally does not implement SQX metric
formulas, filters, champion selection, or validation decisions.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
import re
from typing import Iterable

from .canonical import ContentAddress
from .specs import SpecValidationError, _require_ref, _require_text

_OBJECTIVE_RE = re.compile(r"^[a-z][a-z0-9_.-]*$")
_DIRECTIONS = frozenset({"maximize", "minimize"})


@dataclass(frozen=True, slots=True)
class RankingObjectiveV1:
    """Opaque producer metric plus ordering direction.

    Metric meaning remains producer-owned until separately proven.
    """

    metric_path: str
    direction: str

    def __post_init__(self) -> None:
        metric_path = _require_text(self.metric_path, "metric_path")
        if (
            not _OBJECTIVE_RE.fullmatch(metric_path)
            or ".." in metric_path
            or metric_path.endswith(".")
        ):
            raise SpecValidationError(
                "metric_path must be a dotted lowercase metric path"
            )
        if self.direction not in _DIRECTIONS:
            raise SpecValidationError(
                f"direction must be one of {sorted(_DIRECTIONS)}, got {self.direction!r}"
            )
        object.__setattr__(self, "metric_path", metric_path)


@dataclass(frozen=True, slots=True)
class RankedCandidateV1:
    """One candidate and its producer-supplied finite ranking score."""

    candidate_ref: ContentAddress
    score: Decimal

    def __post_init__(self) -> None:
        _require_ref(self.candidate_ref, "candidate", "candidate_ref")
        if not isinstance(self.score, Decimal) or not self.score.is_finite():
            raise SpecValidationError("score must be a finite Decimal")


def order_ranked_candidates(
    objective: RankingObjectiveV1,
    candidates: Iterable[RankedCandidateV1],
) -> tuple[RankedCandidateV1, ...]:
    """Order candidates for discovery without creating validation authority.

    Equal-score tie semantics are not proved by the retained SQX evidence, so
    ties fail closed instead of inventing a secondary ordering rule.
    """

    if not isinstance(objective, RankingObjectiveV1):
        raise SpecValidationError("objective must be RankingObjectiveV1")
    values = tuple(candidates)
    if any(not isinstance(item, RankedCandidateV1) for item in values):
        raise SpecValidationError(
            "candidates must contain only RankedCandidateV1 values"
        )
    refs = tuple(item.candidate_ref for item in values)
    if len(set(refs)) != len(refs):
        raise SpecValidationError("candidate_ref values must be unique")
    scores = tuple(item.score for item in values)
    if len(set(scores)) != len(scores):
        raise SpecValidationError("equal-score ranking tie semantics are unproven")

    reverse = objective.direction == "maximize"
    return tuple(sorted(values, key=lambda item: item.score, reverse=reverse))
