"""Custody contract for SQX system-parameter permutation settings."""
from __future__ import annotations
from dataclasses import dataclass

SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE = "OptProfileSysParamPermutation"
SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS = 1

class SystemParameterPermutationError(ValueError):
    pass

@dataclass(frozen=True, slots=True)
class SystemParameterPermutationSettings:
    max_tests: int = SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS
    optim_periods: bool = False
    optim_exit_types: bool = False
    enabled: bool = True
    def __post_init__(self) -> None:
        if type(self.max_tests) is not int or self.max_tests < 1:
            raise SystemParameterPermutationError("MaxTests must be a positive integer")
        if type(self.optim_periods) is not bool:
            raise SystemParameterPermutationError("OptimPeriods must be boolean")
        if type(self.optim_exit_types) is not bool:
            raise SystemParameterPermutationError("OptimExitTypes must be boolean")
        if type(self.enabled) is not bool:
            raise SystemParameterPermutationError("use must be boolean")
    def as_sqx_settings(self) -> dict[str, object]:
        return {"profile": SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE, "use": self.enabled, "Settings": {"OptimPeriods": self.optim_periods, "OptimExitTypes": self.optim_exit_types, "MaxTests": self.max_tests}}
    @property
    def hidden_execution_semantics_recovered(self) -> bool:
        return False
