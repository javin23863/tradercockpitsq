"""Native StrategyQuant X Retester evaluator for exact SQX archive custody."""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import os
from pathlib import Path
import re
import shutil
import subprocess
from typing import Callable
from uuid import uuid4

from tradercockpit.domain import (
    EngineBuildSpecV1,
    NativeDataContextV1,
    NativeExecutionContextV1,
    ResultArtifactV1,
    StrategySpecV1,
)
from tradercockpit.engine.contracts import BacktestInputsV1, EngineContractError
from tradercockpit.engine.evaluator import EvaluatorDescriptorV1

from .sqx_outputs import (
    SQX_NATIVE_STRATEGY_SCHEMA,
    SqxOutputError,
    inspect_sqx_output,
    persist_sqx_custody_blob,
    sqx_custody_blob_path,
)
from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_RETESTER_RESULT_SCHEMA = "sqx.native-retester-result.v1"
SQX_RETESTER_CONTEXT_SCHEMA = "sqx.retester-task.v1"
SQX_TRADING_LIB_SHA256 = "9796578273f36ced388b977bf08ff67c149a8897805b0bce00f7b8d3de6241f3"
SQX_LAUNCHER_SHA256_ENV = "SQX_LAUNCHER_SHA256"
SQX_RETESTER_TASK = 1
SQX_RETESTER_SOURCE_PROJECT = "Retester"
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")


class SqxRetesterError(EngineContractError):
    """Raised when the native Retester cannot prove an exact execution."""


def _sha256_file(path: Path) -> str:
    digest = sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sqx_retester_engine_build() -> EngineBuildSpecV1:
    return EngineBuildSpecV1(
        implementation="strategyquant-x-retester",
        revision=SQX_BUILD,
        artifact_sha256=SQX_TRADING_LIB_SHA256,
    )


def _verify_engine_artifact(home: Path) -> None:
    path = home / "internal" / "libs" / "SQTradingLib.jar"
    if not path.is_file():
        raise SqxRetesterError("SQX trading engine artifact is missing")
    observed = _sha256_file(path)
    if observed != SQX_TRADING_LIB_SHA256:
        raise SqxRetesterError(
            f"SQX trading engine artifact hash mismatch: {observed}"
        )


def _trusted_launcher_sha256(value: str | None) -> str:
    candidate = value if value is not None else os.environ.get(SQX_LAUNCHER_SHA256_ENV)
    if candidate is None or not candidate.strip():
        raise SqxRetesterError(
            f"set {SQX_LAUNCHER_SHA256_ENV} to the separately trusted sqcli.exe SHA-256"
        )
    normalized = candidate.strip().lower()
    if not _DIGEST_RE.fullmatch(normalized):
        raise SqxRetesterError(
            f"{SQX_LAUNCHER_SHA256_ENV} must be exactly 64 hexadecimal characters"
        )
    return normalized


def _verify_launcher_artifact(home: Path, expected_sha256: str | None) -> str:
    launcher = home / "sqcli.exe"
    if not launcher.is_file():
        raise SqxRetesterError("SQX launcher is missing")
    expected = _trusted_launcher_sha256(expected_sha256)
    try:
        observed = _sha256_file(launcher)
    except OSError as exc:
        raise SqxRetesterError("SQX launcher cannot be read") from exc
    if observed != expected:
        raise SqxRetesterError(
            f"SQX launcher hash mismatch: {observed}"
        )
    return observed


def _strategy_hash(strategy: StrategySpecV1, key: str, detail: str) -> str:
    value = strategy.semantics.get(key)
    if not isinstance(value, str) or not _DIGEST_RE.fullmatch(value):
        raise SqxRetesterError(detail)
    return value


def _strategy_archive_hash(strategy: StrategySpecV1) -> str:
    if strategy.semantic_schema != SQX_NATIVE_STRATEGY_SCHEMA:
        raise SqxRetesterError(
            f"unsupported strategy semantic schema: {strategy.semantic_schema}"
        )
    semantics = strategy.semantics
    if (
        semantics.get("producer") != "strategyquant-x"
        or semantics.get("source_build") != SQX_BUILD
    ):
        raise SqxRetesterError("strategy is not bound to the verified SQX build")
    if semantics.get("native_version") != SQX_BUILD:
        raise SqxRetesterError(
            "strategy native producer version does not match the verified SQX build"
        )
    return _strategy_hash(
        strategy,
        "archive_sha256",
        "strategy does not contain an exact SQX archive hash",
    )


def _strategy_settings_hash(strategy: StrategySpecV1) -> str:
    return _strategy_hash(
        strategy,
        "settings_entry_sha256",
        "strategy does not contain an exact SQX settings hash",
    )


def _find_builder_source_archive(home: Path, expected_hash: str) -> Path:
    """Legacy source locator used only when no TraderCockpit custody root is supplied."""

    root = home / "user" / "projects" / "Builder" / "databanks" / "Results"
    if not root.is_dir():
        raise SqxRetesterError("SQX Builder Results databank is missing")
    matches = [
        path for path in root.glob("*.sqx") if _sha256_file(path) == expected_hash
    ]
    if len(matches) != 1:
        raise SqxRetesterError(
            "expected exactly one Builder result matching strategy custody, "
            f"found {len(matches)}"
        )
    return matches[0]


def _find_source_archive(
    home: Path,
    expected_hash: str,
    custody_root: Path | str | None,
) -> Path:
    if custody_root is None:
        return _find_builder_source_archive(home, expected_hash)
    root = Path(custody_root).expanduser().resolve()
    try:
        path = sqx_custody_blob_path(root, expected_hash)
    except SqxOutputError as exc:
        raise SqxRetesterError(exc.detail) from exc
    if not path.is_file():
        raise SqxRetesterError(
            "imported SQX candidate archive is missing from TraderCockpit custody"
        )
    if _sha256_file(path) != expected_hash:
        raise SqxRetesterError(
            "TraderCockpit SQX candidate custody hash mismatch"
        )
    return path


def _inspect_exact_source(
    source: Path,
    archive_hash: str,
    settings_hash: str,
) -> dict[str, object]:
    try:
        inspected = inspect_sqx_output(source)
    except SqxOutputError as exc:
        raise SqxRetesterError(exc.detail) from exc
    if inspected["archive_sha256"] != archive_hash:
        raise SqxRetesterError(
            "SQX source archive changed while binding native context"
        )
    if inspected["settings_entry_sha256"] != settings_hash:
        raise SqxRetesterError(
            "SQX source settings changed while binding native context"
        )
    return inspected


def sqx_retester_native_contexts(
    sqx_home: Path | str | None,
    strategy: StrategySpecV1,
    *,
    state_root: Path | str | None = None,
) -> tuple[NativeDataContextV1, NativeExecutionContextV1]:
    """Derive opaque native contexts from the exact files Retester will use."""

    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise SqxRetesterError(exc.detail) from exc
    archive_hash = _strategy_archive_hash(strategy)
    settings_hash = _strategy_settings_hash(strategy)
    source = _find_source_archive(home, archive_hash, state_root)
    _inspect_exact_source(source, archive_hash, settings_hash)
    source_project = (
        home
        / "user"
        / "projects"
        / SQX_RETESTER_SOURCE_PROJECT
        / "project.cfx"
    )
    if not source_project.is_file():
        raise SqxRetesterError("SQX Retester project.cfx is missing")
    config_hash = _sha256_file(source_project)
    common = {
        "producer": "strategyquant-x",
        "context_schema": SQX_RETESTER_CONTEXT_SCHEMA,
        "source_project": "retester",
        "source_task": SQX_RETESTER_TASK,
        "source_config_sha256": config_hash,
        "candidate_archive_sha256": archive_hash,
        "candidate_settings_sha256": settings_hash,
    }
    return NativeDataContextV1(**common), NativeExecutionContextV1(**common)


@dataclass(slots=True)
class SqxRetesterEvaluator:
    """Execute one exact native SQX archive through isolated Retester task 1."""

    sqx_home: Path | str
    custody_root: Path | str | None = None
    expected_launcher_sha256: str | None = None
    timeout_seconds: float = 300.0
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run

    @property
    def descriptor(self) -> EvaluatorDescriptorV1:
        return EvaluatorDescriptorV1(
            engine_build_ref=sqx_retester_engine_build().ref,
            semantic_schemas=(SQX_NATIVE_STRATEGY_SCHEMA,),
            result_schema=SQX_RETESTER_RESULT_SCHEMA,
            deterministic=False,
        )

    def _home(self) -> Path:
        try:
            home = verified_sqx_home(self.sqx_home)
        except SqxPresetRuntimeError as exc:
            raise SqxRetesterError(exc.detail) from exc
        _verify_launcher_artifact(home, self.expected_launcher_sha256)
        _verify_engine_artifact(home)
        return home

    def validate_strategy(self, strategy: StrategySpecV1) -> None:
        if not isinstance(strategy, StrategySpecV1):
            raise SqxRetesterError("strategy must be StrategySpecV1")
        home = self._home()
        expected_hash = _strategy_archive_hash(strategy)
        expected_settings = _strategy_settings_hash(strategy)
        source = _find_source_archive(home, expected_hash, self.custody_root)
        _inspect_exact_source(source, expected_hash, expected_settings)

    def evaluate(self, inputs: BacktestInputsV1) -> ResultArtifactV1:
        if not isinstance(inputs, BacktestInputsV1):
            raise SqxRetesterError("inputs must be BacktestInputsV1")
        home = self._home()
        expected_data, expected_execution = sqx_retester_native_contexts(
            home,
            inputs.strategy,
            state_root=self.custody_root,
        )
        if (
            not isinstance(inputs.data, NativeDataContextV1)
            or inputs.data.ref != expected_data.ref
        ):
            raise SqxRetesterError(
                "run data context does not match the exact native Retester configuration"
            )
        if (
            not isinstance(inputs.execution, NativeExecutionContextV1)
            or inputs.execution.ref != expected_execution.ref
        ):
            raise SqxRetesterError(
                "run execution context does not match the exact native Retester configuration"
            )
        if inputs.run.random_seed is not None:
            raise SqxRetesterError(
                "native Retester task 1 does not bind a TraderCockpit random seed"
            )

        expected_hash = _strategy_archive_hash(inputs.strategy)
        expected_settings = _strategy_settings_hash(inputs.strategy)
        source = _find_source_archive(home, expected_hash, self.custody_root)
        _inspect_exact_source(source, expected_hash, expected_settings)
        source_project = (
            home
            / "user"
            / "projects"
            / SQX_RETESTER_SOURCE_PROJECT
            / "project.cfx"
        )

        project_name = f"TraderCockpit-Retester-{uuid4().hex[:16]}"
        project_root = home / "user" / "projects" / project_name
        results_root = project_root / "databanks" / "Results"
        if project_root.exists():
            raise SqxRetesterError(
                "isolated SQX Retester workspace already exists"
            )
        results_root.mkdir(parents=True)
        cleanup_workspace = self.custody_root is not None

        try:
            shutil.copy2(source_project, project_root / "project.cfx")
            if (
                _sha256_file(project_root / "project.cfx")
                != expected_data.source_config_sha256
            ):
                raise SqxRetesterError(
                    "staged SQX Retester configuration hash mismatch"
                )
            staged = results_root / source.name
            shutil.copy2(source, staged)
            if _sha256_file(staged) != expected_hash:
                raise SqxRetesterError("staged SQX candidate hash mismatch")
            before_outputs = {
                path.name: _sha256_file(path)
                for path in results_root.glob("*.sqx")
            }

            command = [
                str(home / "sqcli.exe"),
                "-project",
                "action=startOnlyTask",
                f"name={project_name}",
                f"task={SQX_RETESTER_TASK}",
            ]
            try:
                completed = self.runner(
                    command,
                    cwd=str(home),
                    capture_output=True,
                    text=True,
                    timeout=self.timeout_seconds,
                    check=False,
                )
            except subprocess.TimeoutExpired as exc:
                raise SqxRetesterError("SQX Retester task timed out") from exc
            except OSError as exc:
                raise SqxRetesterError(
                    "SQX Retester process could not be started"
                ) from exc
            if completed.returncode != 0:
                raise SqxRetesterError(
                    f"SQX Retester exited with code {completed.returncode}"
                )

            changed_outputs: list[Path] = []
            for path in sorted(results_root.glob("*.sqx")):
                observed_hash = _sha256_file(path)
                if (
                    path.name not in before_outputs
                    or before_outputs[path.name] != observed_hash
                ):
                    changed_outputs.append(path)
            if len(changed_outputs) != 1:
                raise SqxRetesterError(
                    "SQX Retester produced "
                    f"{len(changed_outputs)} new-or-changed result archives; "
                    "expected exactly one"
                )
            result_path = changed_outputs[0]
            try:
                result_snapshot = result_path.read_bytes()
            except OSError as exc:
                raise SqxRetesterError(
                    "SQX Retester result archive could not be read"
                ) from exc
            try:
                result_info = inspect_sqx_output(result_path)
            except SqxOutputError as exc:
                raise SqxRetesterError(exc.detail) from exc
            result_hash = str(result_info["archive_sha256"])
            if sha256(result_snapshot).hexdigest() != result_hash:
                raise SqxRetesterError(
                    "SQX Retester result changed while entering custody"
                )
            if result_hash == expected_hash:
                raise SqxRetesterError(
                    "SQX Retester did not produce a changed native result archive"
                )

            result_custody_relative: str | None = None
            if self.custody_root is not None:
                try:
                    persisted = persist_sqx_custody_blob(
                        Path(self.custody_root).expanduser().resolve(),
                        result_snapshot,
                        expected_sha256=result_hash,
                        result=True,
                    )
                except SqxOutputError as exc:
                    raise SqxRetesterError(exc.detail) from exc
                result_custody_relative = persisted.relative_to(
                    Path(self.custody_root).expanduser().resolve()
                ).as_posix()

            return ResultArtifactV1(
                run_ref=inputs.run.ref,
                producer_build_ref=inputs.engine_build.ref,
                result_schema=SQX_RETESTER_RESULT_SCHEMA,
                payload={
                    "producer": {
                        "exit_code": int(completed.returncode),
                        "task": SQX_RETESTER_TASK,
                    },
                    "source": {
                        "archive_sha256": expected_hash,
                        "settings_entry_sha256": (
                            expected_data.candidate_settings_sha256
                        ),
                        "project_config_sha256": expected_data.source_config_sha256,
                    },
                    "result": {
                        "archive_sha256": result_hash,
                        "archive_bytes": int(result_info["bytes"]),
                        "strategy_entry_sha256": result_info[
                            "strategy_entry_sha256"
                        ],
                        "settings_entry_sha256": result_info[
                            "settings_entry_sha256"
                        ],
                        "custody_relative_path": result_custody_relative,
                    },
                    "workspace": {
                        "project": project_name,
                        "ephemeral": cleanup_workspace,
                    },
                },
            )
        finally:
            if cleanup_workspace and project_root.exists():
                shutil.rmtree(project_root)
