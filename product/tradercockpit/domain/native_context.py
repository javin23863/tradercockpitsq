"""Opaque producer-owned data and execution contexts for native engines.

These specs are used when TraderCockpit can prove the exact producer configuration
identity but cannot truthfully expand that configuration into TraderCockpit-owned
data-window or execution-assumption fields.
"""

from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Any, ClassVar, Mapping

from .specs import _AddressedSpec, _require_schema, _require_text, _require_token


_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


def _digest(value: str, name: str) -> str:
    value = _require_text(value, name)
    if not _DIGEST_RE.fullmatch(value):
        raise ValueError(f"{name} must be 64 lowercase hex chars")
    return value


def _task(value: int) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
        raise ValueError("source_task must be a positive integer")
    return value


@dataclass(frozen=True, slots=True)
class NativeDataContextV1(_AddressedSpec):
    """Exact native producer configuration identity for run data semantics."""

    KIND: ClassVar[str] = "data"

    producer: str
    context_schema: str
    source_project: str
    source_task: int
    source_config_sha256: str
    candidate_archive_sha256: str
    candidate_settings_sha256: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "producer", _require_token(self.producer, "producer"))
        object.__setattr__(self, "context_schema", _require_schema(self.context_schema, "context_schema"))
        object.__setattr__(self, "source_project", _require_token(self.source_project, "source_project"))
        object.__setattr__(self, "source_task", _task(self.source_task))
        object.__setattr__(self, "source_config_sha256", _digest(self.source_config_sha256, "source_config_sha256"))
        object.__setattr__(self, "candidate_archive_sha256", _digest(self.candidate_archive_sha256, "candidate_archive_sha256"))
        object.__setattr__(self, "candidate_settings_sha256", _digest(self.candidate_settings_sha256, "candidate_settings_sha256"))

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "producer": self.producer,
            "context_schema": self.context_schema,
            "source_project": self.source_project,
            "source_task": self.source_task,
            "source_config_sha256": self.source_config_sha256,
            "candidate_archive_sha256": self.candidate_archive_sha256,
            "candidate_settings_sha256": self.candidate_settings_sha256,
        }


@dataclass(frozen=True, slots=True)
class NativeExecutionContextV1(_AddressedSpec):
    """Exact native producer configuration identity for execution semantics."""

    KIND: ClassVar[str] = "execution"

    producer: str
    context_schema: str
    source_project: str
    source_task: int
    source_config_sha256: str
    candidate_archive_sha256: str
    candidate_settings_sha256: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "producer", _require_token(self.producer, "producer"))
        object.__setattr__(self, "context_schema", _require_schema(self.context_schema, "context_schema"))
        object.__setattr__(self, "source_project", _require_token(self.source_project, "source_project"))
        object.__setattr__(self, "source_task", _task(self.source_task))
        object.__setattr__(self, "source_config_sha256", _digest(self.source_config_sha256, "source_config_sha256"))
        object.__setattr__(self, "candidate_archive_sha256", _digest(self.candidate_archive_sha256, "candidate_archive_sha256"))
        object.__setattr__(self, "candidate_settings_sha256", _digest(self.candidate_settings_sha256, "candidate_settings_sha256"))

    def identity_payload(self) -> Mapping[str, Any]:
        return {
            "producer": self.producer,
            "context_schema": self.context_schema,
            "source_project": self.source_project,
            "source_task": self.source_task,
            "source_config_sha256": self.source_config_sha256,
            "candidate_archive_sha256": self.candidate_archive_sha256,
            "candidate_settings_sha256": self.candidate_settings_sha256,
        }
