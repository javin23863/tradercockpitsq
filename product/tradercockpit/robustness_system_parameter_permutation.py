"""Bounded TraderCockpit contract for SQX system-parameter permutation.

StrategyQuant X 144.2953 runtime evidence proves a Retester profile named
``OptProfileSysParamPermutation``.  The captured native probe enabled that
profile with ``OptimPeriods=false``, ``OptimExitTypes=false`` and
``MaxTests=1`` and completed successfully.

The full hidden SQX permutation generator is not recovered.  This module
therefore owns only the observable settings contract.  It deliberately does
not invent parameter ranges, candidate-generation rules, acceptance-condition
semantics, or native project mutation.
"""

from __future__ import annotations

from dataclasses import dataclass


SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE = "OptProfileSysParamPermutation"
SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS = 1


class SystemParameterPermutationError(ValueError):
    """Raised when a request exceeds the reconstructed permutation contract."""


@dataclass(frozen=True, slots=True)
class SystemParameterPermutationSettings:
    """Observable settings for the bounded SQX permutation profile.

    ``max_tests`` is modeled as a positive test-count cap because SQX exposes
    that exact field and the native probe demonstrates the value ``1``.  No
    upper bound is claimed by this reconstruction.

    Period and exit-type optimization are represented so callers cannot lose
    custody of the native switches, but this slice only proves them disabled.
    Requests to enable either switch fail closed until separate evidence
    establishes their semantics.
    """

    max_tests: int = SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS
    optim_periods: bool = False
    optim_exit_types: bool = False

    def __post_init__(self) -> None:
        if type(self.max_tests) is not int or self.max_tests < 1:
            raise SystemParameterPermutationError(
                "MaxTests must be a positive integer"
            )
        if type(self.optim_periods) is not bool:
            raise SystemParameterPermutationError("OptimPeriods must be boolean")
        if type(self.optim_exit_types) is not bool:
            raise SystemParameterPermutationError("OptimExitTypes must be boolean")
        if self.optim_periods:
            raise SystemParameterPermutationError(
                "OptimPeriods=true is outside the reconstructed evidence boundary"
            )
        if self.optim_exit_types:
            raise SystemParameterPermutationError(
                "OptimExitTypes=true is outside the reconstructed evidence boundary"
            )

    def as_sqx_settings(self) -> dict[str, object]:
        """Return the exact evidenced SQX field names without mutating a project."""

        return {
            "profile": SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE,
            "use": True,
            "Settings": {
                "OptimPeriods": self.optim_periods,
                "OptimExitTypes": self.optim_exit_types,
                "MaxTests": self.max_tests,
            },
        }
