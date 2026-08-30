"""Native StrategyQuant X Retester evaluator for exact SQX archive custody."""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import shutil
import subprocess
from typing import Callable
from uuid import uuid4

from tradercockpit.domain import EngineBuildSpecV1, ResultArtifactV1, StrategySpecV1
from tradercockpit.engine.contracts import BacktestInputsV1, EngineContractError
from tradercockpit.engine.evaluator import EvaluatorDescriptorV1

from .sqx_outputs import SQX_NATIVE_STRATEGY_SCHEMA, inspect_sqx_output
from .sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home


SQX_RETESTER_RESULT_SCHEMA = "sqx.native-retester-result.v1"
SQX_TRADING_LIB_SHA256 = "9796578273f36ced388b977bf08ff67c149a8897805b0bce00f7b8d3de6241f3"
SQX_RETESTER_TASK = 1
SQX_RETESTER_SOURCE_PROJECT = "Retester"


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


def _strategy_archive_hash(strategy: StrategySpecV1) -> str:
    if strategy.semantic_schema != SQX_NATIVE_STRATEGY_SCHEMA:
        raise SqxRetesterError(
            f"unsupported strategy semantic schema: {strategy.semantic_schema}"
        )
    semantics = strategy.semantics
    if semantics.get("producer") != "strategyquant-x" or semantics.get("source_build") != SQX_BUILD:
        raise SqxRetesterError("strategy is not bound to the verified SQX build")
    value = semantics.get("archive_sha256")
    if not isinstance(value, str) or len(value) != 64:
        raise SqxRetesterError("strategy does not contain an exact SQX archive hash")
    return value


def _find_source_archive(home: Path, expected_hash: str) -> Path:
    root = home / "user" / "projects" / "Builder" / "databanks" / "Results"
    if not root.is_dir():
        raise SqxRetesterError("SQX Builder Results databank is missing")
    matches = [path for path in root.glob("*.sqx") if _sha256_file(path) == expected_hash]
    if len(matches) != 1:
        raise SqxRetesterError(
            f"expected exactly one Builder result matching strategy custody, found {len(matches)}"
        )
    return matches[0]


@dataclass(slots=True)
class SqxRetesterEvaluator:
    """Execute one exact native SQX archive through isolated Retester task 1."""

    sqx_home: Path | str
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
        launcher = home / "sqcli.exe"
        if not launcher.is_file():
            raise SqxRetesterError("SQX launcher is missing")
        _verify_engine_artifact(home)
        return home

    def validate_strategy(self, strategy: StrategySpecV1) -> None:
        if not isinstance(strategy, StrategySpecV1):
            raise SqxRetesterError("strategy must be StrategySpecV1")
        home = self._home()
        expected_hash = _strategy_archive_hash(strategy)
        source = _find_source_archive(home, expected_hash)
        inspected = inspect_sqx_output(source)
        if inspected["archive_sha256"] != expected_hash:
            raise SqxRetesterError("SQX source archive changed during validation")

    def evaluate(self, inputs: BacktestInputsV1) -> ResultArtifactV1:
        if not isinstance(inputs, BacktestInputsV1):
            raise SqxRetesterError("inputs must be BacktestInputsV1")
        home = self._home()
        expected_hash = _strategy_archive_hash(inputs.strategy)
        source = _find_source_archive(home, expected_hash)

        source_project = home / "user" / "projects" / SQX_RETESTER_SOURCE_PROJECT / "project.cfx"
        if not source_project.is_file():
            raise SqxRetesterError("SQX Retester project.cfx is missing")

        project_name = f"TraderCockpit-Retester-{uuid4().hex[:16]}"
        project_root = home / "user" / "projects" / project_name
        results_root = project_root / "databanks" / "Results"
        if project_root.exists():
            raise SqxRetesterError("isolated SQX Retester workspace already exists")
        results_root.mkdir(parents=True)
        shutil.copy2(source_project, project_root / "project.cfx")
        staged = results_root / source.name
        shutil.copy2(source, staged)
        if _sha256_file(staged) != expected_hash:
            raise SqxRetesterError("staged SQX candidate hash mismatch")

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
        if completed.returncode != 0:
            raise SqxRetesterError(
                f"SQX Retester exited with code {completed.returncode}"
            )

        outputs = sorted(results_root.glob("*.sqx"))
        if len(outputs) != 1:
            raise SqxRetesterError(
                f"SQX Retester produced {len(outputs)} result archives; expected exactly one"
            )
        result_info = inspect_sqx_output(outputs[0])
        if result_info["archive_sha256"] == expected_hash:
            raise SqxRetesterError("SQX Retester did not produce a changed native result archive")

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
                },
                "result": {
                    "archive_sha256": result_info["archive_sha256"],
                    "archive_bytes": int(result_info["bytes"]),
                    "strategy_entry_sha256": result_info["strategy_entry_sha256"],
                    "settings_entry_sha256": result_info["settings_entry_sha256"],
                },
                "workspace": {
                    "project": project_name,
                },
            },
        )
