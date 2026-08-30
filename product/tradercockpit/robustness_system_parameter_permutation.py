"""TraderCockpit custody contract for SQX system-parameter permutation settings.

StrategyQuant X 144.2953 evidence establishes the native profile identifier and
settings field names ``use``, ``OptimPeriods``, ``OptimExitTypes`` and
``MaxTests``.  A retained native probe exercised the enabled profile with both
optimization switches disabled and ``MaxTests=1``.

The hidden SQX permutation generator is not recovered.  That limits claims
about candidate generation, parameter ranges, workload size and result
semantics; it does *not* make the native boolean settings themselves invalid.
This module therefore preserves settings values without pretending to implement
the hidden generator or native Retester execution.
"""

from __future__ import annotations

from dataclasses import dataclass


SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE = "OptProfileSysParamPermutation"
SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS = 1


class SystemParameterPermutationError(ValueError):
    """Raised when system-parameter-permutation settings are malformed."""


@dataclass(frozen=True, slots=True)
class SystemParameterPermutationSettings:
    """Observable SQX profile settings, separate from hidden execution mechanics.

    ``enabled``, ``optim_periods`` and ``optim_exit_types`` are ordinary native
    configuration switches and are preserved exactly.  Allowing ``True`` does
    not claim to know which hidden permutations SQX will generate; that behavior
    remains an execution-engine boundary.

    ``max_tests`` is retained as a positive TraderCockpit test-count contract.
    The captured native probe proves ``1``; no native upper bound or special zero
    meaning is claimed here.  Native execution/mutation code must perform any
    additional representation validation required by its recovered parser.
    """

    max_tests: int = SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS
    optim_periods: bool = False
    optim_exit_types: bool = False
    enabled: bool = True

    def __post_init__(self) -> None:
        if type(self.max_tests) is not int or self.max_tests < 1:
            raise SystemParameterPermutationError(
                "MaxTests must be a positive integer"
            )
        if type(self.optim_periods) is not bool:
            raise SystemParameterPermutationError("OptimPeriods must be boolean")
        if type(self.optim_exit_types) is not bool:
            raise SystemParameterPermutationError("OptimExitTypes must be boolean")
        if type(self.enabled) is not bool:
            raise SystemParameterPermutationError("use must be boolean")

    def as_sqx_settings(self) -> dict[str, object]:
        """Return the evidenced SQX field names without executing or mutating SQX."""

        return {
            "profile": SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE,
            "use": self.enabled,
            "Settings": {
                "OptimPeriods": self.optim_periods,
                "OptimExitTypes": self.optim_exit_types,
                "MaxTests": self.max_tests,
            },
        }

    @property
    def hidden_execution_semantics_recovered(self) -> bool:
        """Make the evidence boundary machine-readable for downstream wiring."""

        return False
