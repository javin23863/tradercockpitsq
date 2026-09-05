"""Trusted native StrategyQuant X control gateway.

The gateway is intentionally narrow. It exposes only product-bound SQX
controls:

- Builder: load one exact approved Task-rooted CFX, then supervise Builder;
- Retester: start task 1 for one TraderCockpit-created isolated Retester project.

It is not a generic command runner and browser code never supplies executable,
runtime, task, or arbitrary project paths.

Every native subprocess gets a fresh runtime/build/launcher and operation-specific
identity preflight. A read-only runtime-status snapshot is never accepted as launch
authorization.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import re
import os
import subprocess
import time
from threading import Lock
from typing import Callable, Sequence
from xml.etree import ElementTree
from zipfile import BadZipFile, ZIP_STORED, ZipFile, ZipInfo

from tradercockpit.desktop_lifecycle import DesktopWorkerSupervisor
from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1"
SQX_NATIVE_CONTROL_ERROR_SCHEMA = "tc.sqx-native-control-error.v1"
SQX_NATIVE_CONTROL_TIMEOUT_SECONDS = 60.0
_BUILDER_PROJECT = "Builder"
_RETESTER_TASK = 1
_RETESTER_ENGINE_RELATIVE_PATH = "internal/libs/SQTradingLib.jar"
_RETESTER_PROJECT_RE = re.compile(r"^TraderCockpit-Retester-[0-9a-f]{32}$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CONTROL_LOCK = Lock()
_BUILDER_START_WORKER_LABEL = "sqx-project-start:Builder"
_BUILDER_START_SETTLE_SECONDS = 1.0


def _native_xml(payload: bytes) -> ElementTree.Element:
    if b"<!DOCTYPE" in payload.upper() or b"<!ENTITY" in payload.upper():
        raise SqxNativeGatewayError("config_xml_invalid", "native configuration cannot declare entities")
    try:
        return ElementTree.fromstring(payload)
    except ElementTree.ParseError as exc:
        raise SqxNativeGatewayError("config_xml_invalid", "native configuration XML is invalid") from exc



def single_retester_task(snapshot: bytes) -> str:
    """Bound whole-project start to the sole native task, without rewriting it."""
    try:
        with ZipFile(BytesIO(snapshot)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise ValueError("duplicate members")
            root = _native_xml(archive.read("config.xml"))
            groups = root.findall("Tasks")
            tasks = groups[0].findall("Task") if len(groups) == 1 else []
            if root.tag != "Project" or len(tasks) != 1:
                raise ValueError("one task required")
            task = tasks[0]
            name = task.get("name", "")
            if (task.get("type") != "Retest" or task.get("active", "true").lower() != "true"
                    or task.get("taskXMLFile") != "Retest-Task1.xml" or not name.strip()
                    or len(name) > 200 or any(ord(char) < 32 for char in name)
                    or _native_xml(archive.read("Retest-Task1.xml")).tag != "Settings"):
                raise ValueError("one active Retest task required")
            return name
    except (BadZipFile, KeyError, ValueError, LookupError, SqxNativeGatewayError) as exc:
        raise SqxNativeGatewayError("retester_source_project_invalid", "Retester project must contain exactly one active Retest task bound to Retest-Task1.xml") from exc


def verified_retester_execution(receipt: object, task_name: str | None = None) -> bool:
    """Validate the small native-execution summary carried by current receipts."""
    if not isinstance(receipt, dict) or receipt.get("action") != "start" or receipt.get("task") != 1 or receipt.get("state") != "completed" or type(receipt.get("exit_code")) is not int or receipt.get("exit_code") != 0:
        return False
    proof = receipt.get("execution_proof")
    fields = {"schema", "task_name", "input_strategies", "tested_strategies", "passed_strategies", "failed_strategies", "stdout_sha256", "task_log_sha256"}
    if not isinstance(proof, dict) or set(proof) != fields or proof.get("schema") != "tc.sqx-retester-execution.v1":
        return False
    name = proof.get("task_name")
    if not isinstance(name, str) or not name.strip() or (task_name is not None and name != task_name):
        return False
    for field in ("input_strategies", "tested_strategies", "passed_strategies", "failed_strategies"):
        if type(proof.get(field)) is not int or proof[field] < (1 if field in {"input_strategies", "tested_strategies"} else 0):
            return False
    return (proof["passed_strategies"] + proof["failed_strategies"] == proof["tested_strategies"]
            and all(isinstance(proof.get(field), str) and _DIGEST_RE.fullmatch(proof[field]) for field in ("stdout_sha256", "task_log_sha256")))


def _retester_execution(stdout: str, stderr: str, task_name: str, project_name: str, task_log: bytes) -> dict[str, object]:
    """Require this process's task run and actual tested count, not ZIP saving."""
    try:
        log = task_log.decode("utf-8-sig")
        if not isinstance(stdout, str) or not isinstance(stderr, str) or len(stdout) + len(stderr) > 4_000_000:
            raise ValueError("unreadable output")
        # Quantitative 'Failed: N' / 'Failed details' are valid filter outcomes.
        if re.search(r"preventing multiple instances|\bexception\b|\berror\b|cannot (?:start|load|run)|task index out of range|no strategies to", stdout + "\n" + stderr + "\n" + log, re.I):
            raise ValueError("native refusal")
        prefix = r"^" + re.escape(task_name) + r" : "
        patterns = (prefix + r"Starting strategies retesting\.\.\.", r"str to test: ([1-9][0-9]*),",
                    prefix + r"All backtest data prepared", prefix + r"Task finished in [0-9.]+ s\.")
        offset = 0
        inputs = None
        for index, pattern in enumerate(patterns):
            match = re.search(pattern, stdout[offset:], re.MULTILINE)
            if match is None:
                raise ValueError("missing task progress")
            if index == 1:
                inputs = int(match.group(1))
            offset += match.end()
        if not log.startswith(f"Project: {project_name}\n") and not log.startswith(f"Project: {project_name}\r\n"):
            raise ValueError("wrong project log")
        pattern = (r"TASK STARTED at [^\r\n]+\r?\nTask: " + re.escape(task_name)
                   + r", Type: Retest\r?\n[\s\S]*?TASK FINISHED at [^\r\n]+"
                   + r"[\s\S]*?Total tested: ([1-9][0-9]*), Time per strategy: [^\r\n]*?, Passed: ([0-9]+), Failed: ([0-9]+)")
        match = re.search(pattern, log)
        if match is None or log.count("TASK STARTED at ") != 1 or log.count("TASK FINISHED at ") != 1:
            raise ValueError("missing task completion")
        tested, passed, failed = map(int, match.groups())
        if passed + failed != tested:
            raise ValueError("inconsistent native counts")
        return {"schema": "tc.sqx-retester-execution.v1", "task_name": task_name,
                "input_strategies": inputs, "tested_strategies": tested, "passed_strategies": passed,
                "failed_strategies": failed, "stdout_sha256": sha256(stdout.encode("utf-8")).hexdigest(),
                "task_log_sha256": sha256(task_log).hexdigest()}
    except (ValueError, UnicodeError, TypeError) as exc:
        raise SqxNativeGatewayError("retester_execution_unverified", "Native Retester did not prove that the bound task tested strategies and finished") from exc


def task_document_from_cfx(data: bytes) -> bytes:
    """Read the one Task/Settings document consumed by native loadconfig."""
    try:
        with ZipFile(BytesIO(data)) as archive:
            if archive.namelist() != ["config.xml"]:
                raise SqxNativeGatewayError("config_task_element_missing", "Builder CFX must contain one config.xml Task document")
            document = archive.read("config.xml")
    except SqxNativeGatewayError:
        raise
    except (BadZipFile, OSError, RuntimeError, EOFError, NotImplementedError) as exc:
        raise SqxNativeGatewayError("config_archive_invalid", "native Builder CFX is unreadable") from exc
    task = _native_xml(document)
    if (task.tag != "Task" or task.get("type") != "Build"
            or task.get("taskXMLFile") != "Build-Task1.xml"
            or len(task) != 1 or task[0].tag != "Settings"):
        raise SqxNativeGatewayError("config_task_element_missing", "native Builder loadconfig requires the approved Build Task with one Settings body")
    return document


def pack_task_rooted_cfx(settings_xml: bytes, source_project: bytes) -> bytes:
    """Package native Task metadata around its exact approved Settings bytes.

    SQX project.cfx stores the Task declaration separately from Build-Task1.xml.
    loadconfig consumes those as one Task-rooted config.xml; Settings is never
    parsed and reserialized into a different executable configuration.
    """
    try:
        with ZipFile(BytesIO(source_project)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)) or not {"config.xml", "Build-Task1.xml"}.issubset(names):
                raise SqxNativeGatewayError("config_source_invalid", "source Builder project requires unique config.xml and Build-Task1.xml")
            project = _native_xml(archive.read("config.xml"))
            if archive.read("Build-Task1.xml") != settings_xml:
                raise SqxNativeGatewayError("config_settings_mismatch", "approved Settings differ from the bound source project task")
    except SqxNativeGatewayError:
        raise
    except (BadZipFile, OSError, RuntimeError, EOFError, NotImplementedError) as exc:
        raise SqxNativeGatewayError("config_source_invalid", "source Builder project is unreadable") from exc
    tasks = project.findall("./Tasks/Task")
    selected = [task for task in tasks if task.get("taskXMLFile") == "Build-Task1.xml"]
    if (project.tag != "Project" or project.get("name") != "Builder"
            or len(selected) != 1 or selected[0].get("type") != "Build"
            or len(selected[0]) != 0 or _native_xml(settings_xml).tag != "Settings"):
        raise SqxNativeGatewayError("config_task_element_missing", "approved source must identify one Build Task and its Settings body")
    wrapper = ElementTree.tostring(ElementTree.Element("Task", selected[0].attrib), encoding="utf-8", short_empty_elements=False)
    document = wrapper[:-len(b"</Task>")] + settings_xml + b"</Task>"
    buffer = BytesIO()
    info = ZipInfo("config.xml", date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = ZIP_STORED
    info.create_system = 0
    with ZipFile(buffer, "w") as archive:
        archive.writestr(info, document)
    packed = buffer.getvalue()
    task_document_from_cfx(packed)
    return packed


class SqxNativeGatewayError(RuntimeError):
    """Structured fail-closed native control refusal."""

    def __init__(
        self,
        code: str,
        detail: str,
        *,
        receipts: Sequence[dict[str, object]] = (),
    ) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail
        self.receipts = tuple(dict(item) for item in receipts)

    def read_model(self) -> dict[str, object]:
        completed = sum(item.get("state") == "completed" for item in self.receipts)
        launched_states = {"completed", "timeout", "rejected", "invalid_receipt"}
        partial_side_effect = any(item.get("state") in launched_states for item in self.receipts)
        return {
            "schema": SQX_NATIVE_CONTROL_ERROR_SCHEMA,
            "error": "native_control_refused",
            "reason_code": self.code,
            "detail": self.detail,
            "control_requests_completed": completed,
            "partial_side_effect": partial_side_effect,
            "receipts": [dict(item) for item in self.receipts],
        }


def _trusted_digest(value: str | None, *, missing_code: str, invalid_code: str) -> str:
    if value is None or not value.strip():
        raise SqxNativeGatewayError(missing_code, "trusted SHA-256 is not configured")
    normalized = value.strip().lower()
    if not _DIGEST_RE.fullmatch(normalized):
        raise SqxNativeGatewayError(invalid_code, "trusted SHA-256 must be 64 hexadecimal characters")
    return normalized


def _sha256_file(path: Path) -> str:
    digest = sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise SqxNativeGatewayError("native_file_unreadable", "native control file could not be read") from exc
    return digest.hexdigest()


def _resolve_inside(home: Path, value: Path | str, *, escape_code: str) -> tuple[Path, Path]:
    candidate = Path(value).expanduser()
    if not candidate.is_absolute():
        candidate = home / candidate
    try:
        resolved = candidate.resolve()
        relative = resolved.relative_to(home)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxNativeGatewayError(escape_code, "native control path resolves outside the verified runtime") from exc
    return resolved, relative


@dataclass(frozen=True, slots=True)
class _VerifiedLauncherContext:
    home: Path
    launcher: Path
    launcher_sha256: str


@dataclass(frozen=True, slots=True)
class _VerifiedControlContext:
    home: Path
    launcher: Path
    launcher_sha256: str
    config: Path
    config_relative_path: str
    config_sha256: str


@dataclass(frozen=True, slots=True)
class _VerifiedRetesterContext:
    home: Path
    launcher: Path
    launcher_sha256: str
    project_name: str
    project_file: Path
    project_relative_path: str
    project_sha256: str
    task_name: str
    engine_sha256: str
    result_archive_name: str | None
    result_archive_relative_path: str | None
    result_archive_sha256: str | None


@dataclass(slots=True)
class SqxNativeControlGateway:
    """Run only the bounded native Builder and Retester controls."""

    sqx_home: Path | str | None
    trusted_launcher_sha256: str | None
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run
    timeout_seconds: float = SQX_NATIVE_CONTROL_TIMEOUT_SECONDS
    register_worker: Callable[..., None] | None = None
    worker_is_active: Callable[[str], bool] | None = None
    process_factory: Callable[..., object] = subprocess.Popen

    def __post_init__(self) -> None:
        if (
            not isinstance(self.timeout_seconds, (int, float))
            or isinstance(self.timeout_seconds, bool)
            or self.timeout_seconds <= 0
        ):
            raise ValueError("timeout_seconds must be positive")
        if not callable(self.runner):
            raise TypeError("runner must be callable")

    def _preflight_launcher(self) -> _VerifiedLauncherContext:
        try:
            home = verified_sqx_home(self.sqx_home)
        except SqxPresetRuntimeError as exc:
            raise SqxNativeGatewayError(exc.code, exc.detail) from exc

        trusted_launcher = _trusted_digest(
            self.trusted_launcher_sha256,
            missing_code="trusted_launcher_not_configured",
            invalid_code="trusted_launcher_digest_invalid",
        )
        launcher, _ = _resolve_inside(
            home,
            SQX_LAUNCHER_RELATIVE_PATH,
            escape_code="sqx_launcher_path_escape",
        )
        if not launcher.is_file():
            raise SqxNativeGatewayError("sqx_launcher_missing", "trusted SQX launcher is missing")
        try:
            observed_launcher = _sha256_file(launcher)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError("sqx_launcher_unreadable", "trusted SQX launcher could not be read") from exc
        if observed_launcher != trusted_launcher:
            raise SqxNativeGatewayError(
                "sqx_launcher_hash_mismatch",
                "configured SQX launcher does not match the trusted identity",
            )
        return _VerifiedLauncherContext(home, launcher, observed_launcher)

    def _preflight(
        self,
        config_path: Path | str,
        expected_config_sha256: str | None,
    ) -> _VerifiedControlContext:
        launcher = self._preflight_launcher()
        expected_config = _trusted_digest(
            expected_config_sha256,
            missing_code="config_identity_not_configured",
            invalid_code="config_identity_invalid",
        )
        config, relative = _resolve_inside(
            launcher.home,
            config_path,
            escape_code="config_path_escape",
        )
        if config.suffix.lower() != ".cfx":
            raise SqxNativeGatewayError(
                "config_type_unsupported",
                "native Builder loadconfig accepts only the approved Task-rooted CFX",
            )
        if not config.is_file():
            raise SqxNativeGatewayError("config_missing", "native Builder configuration is missing")
        try:
            staged_bytes = config.read_bytes()
            observed_config = sha256(staged_bytes).hexdigest()
        except OSError as exc:
            raise SqxNativeGatewayError("config_unreadable", "native Builder configuration could not be read") from exc
        if observed_config != expected_config:
            raise SqxNativeGatewayError(
                "config_hash_mismatch",
                "native Builder configuration does not match the approved identity",
            )
        task_document_from_cfx(staged_bytes)

        return _VerifiedControlContext(
            home=launcher.home,
            launcher=launcher.launcher,
            launcher_sha256=launcher.launcher_sha256,
            config=config,
            config_relative_path=relative.as_posix(),
            config_sha256=observed_config,
        )

    def _preflight_retester(
        self,
        project_name: str,
        expected_project_sha256: str | None,
        expected_engine_sha256: str | None,
        result_archive_name: str | None = None,
        expected_result_archive_sha256: str | None = None,
    ) -> _VerifiedRetesterContext:
        if not isinstance(project_name, str) or not _RETESTER_PROJECT_RE.fullmatch(project_name):
            raise SqxNativeGatewayError(
                "retester_project_invalid",
                "native Retester control accepts only TraderCockpit-generated isolated project identities",
            )
        launcher = self._preflight_launcher()
        expected_project = _trusted_digest(
            expected_project_sha256,
            missing_code="retester_project_identity_not_configured",
            invalid_code="retester_project_identity_invalid",
        )
        expected_engine = _trusted_digest(
            expected_engine_sha256,
            missing_code="retester_engine_identity_not_configured",
            invalid_code="retester_engine_identity_invalid",
        )
        projects_root, _ = _resolve_inside(
            launcher.home,
            "user/projects",
            escape_code="retester_project_path_escape",
        )
        if not projects_root.is_dir():
            raise SqxNativeGatewayError("retester_projects_missing", "SQX project directory is missing")
        expected_project_root = projects_root / project_name
        project_root, _ = _resolve_inside(
            launcher.home,
            expected_project_root,
            escape_code="retester_project_path_escape",
        )
        if project_root != expected_project_root or project_root.parent != projects_root or not project_root.is_dir():
            raise SqxNativeGatewayError(
                "retester_project_invalid",
                "isolated Retester project is not the exact generated SQX project child",
            )
        project_file, relative = _resolve_inside(
            launcher.home,
            project_root / "project.cfx",
            escape_code="retester_project_path_escape",
        )
        if project_file.parent != project_root or not project_file.is_file():
            raise SqxNativeGatewayError("retester_project_missing", "isolated Retester project.cfx is missing")
        try:
            project_bytes = project_file.read_bytes()
            observed_project = sha256(project_bytes).hexdigest()
        except (OSError, SqxNativeGatewayError) as exc:
            raise SqxNativeGatewayError("retester_project_unreadable", "isolated Retester project.cfx could not be read") from exc
        if observed_project != expected_project:
            raise SqxNativeGatewayError(
                "retester_project_hash_mismatch",
                "isolated Retester project does not match its staged identity",
            )

        task_name = single_retester_task(project_bytes)

        observed_result_name: str | None = None
        observed_result_relative: str | None = None
        observed_result_sha: str | None = None
        if result_archive_name is not None or expected_result_archive_sha256 is not None:
            if (
                not isinstance(result_archive_name, str)
                or not result_archive_name
                or Path(result_archive_name).name != result_archive_name
                or "/" in result_archive_name
                or "\\" in result_archive_name
                or not result_archive_name.lower().endswith(".sqx")
            ):
                raise SqxNativeGatewayError(
                    "retester_result_archive_invalid",
                    "native Retester control requires one exact staged SQX result filename",
                )
            expected_result = _trusted_digest(
                expected_result_archive_sha256,
                missing_code="retester_result_archive_identity_not_configured",
                invalid_code="retester_result_archive_identity_invalid",
            )
            databanks_root, _ = _resolve_inside(
                launcher.home,
                project_root / "databanks",
                escape_code="retester_result_archive_path_escape",
            )
            if databanks_root != project_root / "databanks" or databanks_root.parent != project_root or not databanks_root.is_dir():
                raise SqxNativeGatewayError(
                    "retester_result_archive_path_escape",
                    "isolated Retester databanks directory was redirected outside the generated project",
                )
            results_root, _ = _resolve_inside(
                launcher.home,
                databanks_root / "Results",
                escape_code="retester_result_archive_path_escape",
            )
            if results_root != databanks_root / "Results" or results_root.parent != databanks_root or not results_root.is_dir():
                raise SqxNativeGatewayError(
                    "retester_result_archive_path_escape",
                    "isolated Retester Results databank was redirected outside the generated project",
                )
            expected_result_file = results_root / result_archive_name
            result_file, result_relative = _resolve_inside(
                launcher.home,
                expected_result_file,
                escape_code="retester_result_archive_path_escape",
            )
            if result_file != expected_result_file:
                raise SqxNativeGatewayError(
                    "retester_result_archive_path_escape",
                    "exact staged Retester result archive was redirected away from its generated path",
                )
            if result_file.parent != results_root or not result_file.is_file():
                raise SqxNativeGatewayError(
                    "retester_result_archive_missing",
                    "exact staged Retester result archive is missing",
                )
            observed_result_sha = _sha256_file(result_file)
            if observed_result_sha != expected_result:
                raise SqxNativeGatewayError(
                    "retester_result_archive_hash_mismatch",
                    "staged Retester result archive changed before native launch",
                )
            observed_result_name = result_archive_name
            observed_result_relative = result_relative.as_posix()

        engine, engine_relative = _resolve_inside(
            launcher.home,
            _RETESTER_ENGINE_RELATIVE_PATH,
            escape_code="retester_engine_path_escape",
        )
        if engine_relative.as_posix() != _RETESTER_ENGINE_RELATIVE_PATH or not engine.is_file():
            raise SqxNativeGatewayError(
                "retester_engine_missing",
                "installed SQTradingLib.jar is missing from the verified runtime",
            )
        try:
            observed_engine = _sha256_file(engine)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError(
                "retester_engine_unreadable",
                "installed SQTradingLib.jar could not be read before native execution",
            ) from exc
        if observed_engine != expected_engine:
            raise SqxNativeGatewayError(
                "retester_engine_hash_mismatch",
                "installed SQTradingLib.jar changed after execution provenance was captured",
            )

        return _VerifiedRetesterContext(
            home=launcher.home,
            launcher=launcher.launcher,
            launcher_sha256=launcher.launcher_sha256,
            project_name=project_name,
            project_file=project_file,
            project_relative_path=relative.as_posix(),
            project_sha256=observed_project,
            task_name=task_name,
            engine_sha256=observed_engine,
            result_archive_name=observed_result_name,
            result_archive_relative_path=observed_result_relative,
            result_archive_sha256=observed_result_sha,
        )

    @staticmethod
    def _builder_command(context: _VerifiedControlContext, action: str) -> tuple[str, ...]:
        if action == "loadconfig":
            return (
                str(context.launcher),
                "-project",
                "action=loadconfig",
                f"name={_BUILDER_PROJECT}",
                f"file={context.config.with_suffix('')}",
            )
        if action == "start":
            return (
                str(context.launcher),
                "-project",
                "action=start",
                f"name={_BUILDER_PROJECT}",
            )
        raise AssertionError("unsupported native control action")

    @staticmethod
    def _builder_receipt(
        sequence: int,
        action: str,
        state: str,
        context: _VerifiedControlContext | None,
        *,
        exit_code: int | None,
        reason_code: str | None = None,
    ) -> dict[str, object]:
        return {
            "sequence": sequence,
            "action": action,
            "project": _BUILDER_PROJECT,
            "state": state,
            "exit_code": exit_code,
            "sqx_build": SQX_BUILD,
            "launcher_sha256": context.launcher_sha256 if context else None,
            "config_sha256": context.config_sha256 if context else None,
            "reason_code": reason_code,
        }

    @staticmethod
    def _retester_receipt(
        state: str,
        context: _VerifiedRetesterContext | None,
        project_name: str,
        *,
        exit_code: int | None,
        reason_code: str | None = None,
    ) -> dict[str, object]:
        return {
            "sequence": 1,
            "action": "start",
            "project": project_name,
            "task": _RETESTER_TASK,
            "state": state,
            "exit_code": exit_code,
            "sqx_build": SQX_BUILD,
            "launcher_sha256": context.launcher_sha256 if context else None,
            "project_sha256": context.project_sha256 if context else None,
            "engine_sha256": context.engine_sha256 if context else None,
            "result_archive_name": context.result_archive_name if context else None,
            "result_archive_relative_path": context.result_archive_relative_path if context else None,
            "result_archive_sha256": context.result_archive_sha256 if context else None,
            "reason_code": reason_code,
        }

    def launch_builder(
        self,
        config_path: Path | str,
        *,
        expected_config_sha256: str | None,
    ) -> dict[str, object]:
        """Load one exact Builder config and submit native Builder start control.

        The load must not report a native refusal. Start is registered before
        returning; submission is not candidate generation or a research result.
        """

        receipts: list[dict[str, object]] = []
        last_context: _VerifiedControlContext | None = None
        with _CONTROL_LOCK:
            for sequence, action in enumerate(("loadconfig", "start"), start=1):
                try:
                    context = self._preflight(config_path, expected_config_sha256)
                    if not callable(self.register_worker):
                        raise SqxNativeGatewayError("desktop_worker_unregistered", "Builder start requires the desktop worker supervisor")
                    if callable(self.worker_is_active) and self.worker_is_active(_BUILDER_START_WORKER_LABEL):
                        raise SqxNativeGatewayError("native_project_already_running", "Builder already has a registered native start process")
                except SqxNativeGatewayError as exc:
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "preflight_failed",
                        last_context,
                        exit_code=None,
                        reason_code=exc.code,
                    )
                    raise SqxNativeGatewayError(
                        exc.code,
                        exc.detail,
                        receipts=(*receipts, failed),
                    ) from exc

                last_context = context
                command = self._builder_command(context, action)
                if action == "start":
                    try:
                        self._submit_builder_start(command, context)
                    except SqxNativeGatewayError as exc:
                        failed = self._builder_receipt(sequence, action, "rejected", context, exit_code=None, reason_code=exc.code)
                        raise SqxNativeGatewayError(exc.code, exc.detail, receipts=(*receipts, failed)) from exc
                    receipts.append(self._builder_receipt(sequence, action, "completed", context, exit_code=None))
                    continue
                try:
                    completed = self.runner(
                        list(command),
                        cwd=str(context.home),
                        stdin=subprocess.DEVNULL,
                        capture_output=True,
                        text=True,
                        timeout=float(self.timeout_seconds),
                        check=False,
                        shell=False,
                        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
                    )
                except subprocess.TimeoutExpired as exc:
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "timeout",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_timeout",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_timeout",
                        f"SQX {action} command timed out",
                        receipts=(*receipts, failed),
                    ) from exc
                except OSError as exc:
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "launch_failed",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_failed",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_failed",
                        f"SQX {action} command could not be executed",
                        receipts=(*receipts, failed),
                    ) from exc

                if type(completed.returncode) is not int:
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "invalid_receipt",
                        context,
                        exit_code=None,
                        reason_code="sqx_command_failed",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_failed",
                        "SQX command runner returned an invalid exit code",
                        receipts=(*receipts, failed),
                    )
                if completed.returncode != 0:
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "rejected",
                        context,
                        exit_code=int(completed.returncode),
                        reason_code="sqx_command_rejected",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_command_rejected",
                        f"SQX {action} command exited nonzero",
                        receipts=(*receipts, failed),
                    )

                output = f"{completed.stdout or ''}\n{completed.stderr or ''}"
                if (re.search(r"cannot\s+load\s+config|file\s+not\s+found|invalid\s+task\s+config", output, re.IGNORECASE)
                        or not re.search(r"\bConfig loaded\.", output)):
                    failed = self._builder_receipt(sequence, action, "rejected", context, exit_code=0, reason_code="sqx_loadconfig_failed")
                    raise SqxNativeGatewayError("sqx_loadconfig_failed", "SQX did not confirm loading the approved Builder configuration; start was not submitted", receipts=(*receipts, failed))

                receipts.append(
                    self._builder_receipt(
                        sequence,
                        action,
                        "completed",
                        context,
                        exit_code=int(completed.returncode),
                    )
                )

        assert last_context is not None
        return {
            "schema": SQX_NATIVE_CONTROL_SCHEMA,
            "operation": "builder_loadconfig_start",
            "project": _BUILDER_PROJECT,
            "state": "submitted",
            "sqx_build": SQX_BUILD,
            "launcher_sha256": last_context.launcher_sha256,
            "config_relative_path": last_context.config_relative_path,
            "config_sha256": last_context.config_sha256,
            "control_requests_submitted": len(receipts),
            "control_requests_completed": len(receipts),
            "partial_side_effect": False,
            "receipts": [dict(item) for item in receipts],
        }

    def _submit_builder_start(self, command: tuple[str, ...], context: _VerifiedControlContext) -> None:
        try:
            process = self.process_factory(list(command), cwd=str(context.home), stdin=subprocess.DEVNULL,
                                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, shell=False,
                                           creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        except OSError as exc:
            raise SqxNativeGatewayError("sqx_command_failed", "SQX start command could not be executed") from exc
        try:
            self.register_worker(process, label=_BUILDER_START_WORKER_LABEL)
        except Exception as exc:
            # Reuse bounded terminate/kill cleanup if the desktop closed during registration.
            cleanup = DesktopWorkerSupervisor()
            try:
                cleanup.register(process, label=_BUILDER_START_WORKER_LABEL)
                cleanup.stop_all()
            except Exception as cleanup_error:
                raise SqxNativeGatewayError("desktop_worker_cleanup_failed", "Unregistered Builder process could not be stopped") from cleanup_error
            raise SqxNativeGatewayError("desktop_worker_unregistered", "Builder process registration failed; process was stopped") from exc
        deadline = time.monotonic() + _BUILDER_START_SETTLE_SECONDS
        while process.poll() is None and time.monotonic() < deadline:
            time.sleep(0.05)
        if process.poll() is not None:
            raise SqxNativeGatewayError("sqx_command_rejected", "SQX start exited before Builder stayed running")

    def launch_retester_task(
        self,
        project_name: str,
        *,
        expected_project_sha256: str | None,
        expected_engine_sha256: str | None,
        result_archive_name: str | None = None,
        expected_result_archive_sha256: str | None = None,
    ) -> dict[str, object]:
        """Submit fixed native Retester task 1 for one isolated product project."""

        with _CONTROL_LOCK:
            try:
                context = self._preflight_retester(
                    project_name,
                    expected_project_sha256,
                    expected_engine_sha256,
                    result_archive_name,
                    expected_result_archive_sha256,
                )
            except SqxNativeGatewayError as exc:
                failed = self._retester_receipt(
                    "preflight_failed",
                    None,
                    project_name if isinstance(project_name, str) else "",
                    exit_code=None,
                    reason_code=exc.code,
                )
                raise SqxNativeGatewayError(exc.code, exc.detail, receipts=(failed,)) from exc

            log_folder = context.home / "user/projects" / context.project_name / "log"
            if log_folder.is_symlink() or log_folder.resolve() != log_folder:
                raise SqxNativeGatewayError("retester_execution_unverified", "Retester log directory was redirected")
            before_logs = set(log_folder.glob("global_log_*.log"))
            command = [
                str(context.launcher),
                "-project",
                "action=start",
                f"name={context.project_name}",
            ]
            try:
                completed = self.runner(
                    command,
                    cwd=str(context.home),
                    stdin=subprocess.DEVNULL,
                    capture_output=True,
                    text=True,
                    timeout=float(self.timeout_seconds),
                    check=False,
                    shell=False,
                )
            except subprocess.TimeoutExpired as exc:
                failed = self._retester_receipt(
                    "timeout",
                    context,
                    context.project_name,
                    exit_code=None,
                    reason_code="sqx_command_timeout",
                )
                raise SqxNativeGatewayError(
                    "sqx_command_timeout",
                    "SQX Retester start command timed out",
                    receipts=(failed,),
                ) from exc
            except OSError as exc:
                failed = self._retester_receipt(
                    "launch_failed",
                    context,
                    context.project_name,
                    exit_code=None,
                    reason_code="sqx_command_failed",
                )
                raise SqxNativeGatewayError(
                    "sqx_command_failed",
                    "SQX Retester start command could not be executed",
                    receipts=(failed,),
                ) from exc

            if type(completed.returncode) is not int:
                failed = self._retester_receipt(
                    "invalid_receipt",
                    context,
                    context.project_name,
                    exit_code=None,
                    reason_code="sqx_command_failed",
                )
                raise SqxNativeGatewayError(
                    "sqx_command_failed",
                    "SQX command runner returned an invalid exit code",
                    receipts=(failed,),
                )
            if completed.returncode != 0:
                failed = self._retester_receipt(
                    "rejected",
                    context,
                    context.project_name,
                    exit_code=int(completed.returncode),
                    reason_code="sqx_command_rejected",
                )
                raise SqxNativeGatewayError(
                    "sqx_command_rejected",
                    "SQX Retester start command exited nonzero",
                    receipts=(failed,),
                )

            try:
                fresh_logs = set(log_folder.glob("global_log_*.log")) - before_logs
                if len(fresh_logs) != 1 or log_folder.resolve() != log_folder:
                    raise ValueError("one fresh task log required")
                log_path = fresh_logs.pop()
                if log_path.is_symlink() or log_path.resolve() != log_path or log_path.stat().st_size > 1_000_000:
                    raise ValueError("invalid task log")
                before_stat = log_path.stat()
                with log_path.open("rb") as stream:
                    task_log = stream.read(1_000_001)
                after_stat = log_path.stat()
                if len(task_log) > 1_000_000 or (before_stat.st_ino, before_stat.st_size, before_stat.st_mtime_ns) != (after_stat.st_ino, after_stat.st_size, after_stat.st_mtime_ns):
                    raise ValueError("task log changed during capture")
                proof = _retester_execution(completed.stdout or "", completed.stderr or "", context.task_name, context.project_name, task_log)
            except (OSError, ValueError, SqxNativeGatewayError) as exc:
                failed = self._retester_receipt("rejected", context, context.project_name, exit_code=0, reason_code="retester_execution_unverified")
                raise SqxNativeGatewayError("retester_execution_unverified", "Native Retester execution could not be verified from this run's task progress and tested count", receipts=(failed,)) from exc
            receipt = self._retester_receipt(
                "completed",
                context,
                context.project_name,
                exit_code=int(completed.returncode),
            )
            receipt["execution_proof"] = proof

        return {
            "schema": SQX_NATIVE_CONTROL_SCHEMA,
            "operation": "retester_start_task",
            "project": context.project_name,
            "task": _RETESTER_TASK,
            "state": "submitted",
            "sqx_build": SQX_BUILD,
            "launcher_sha256": context.launcher_sha256,
            "project_relative_path": context.project_relative_path,
            "project_sha256": context.project_sha256,
            "engine_sha256": context.engine_sha256,
            "result_archive_name": context.result_archive_name,
            "result_archive_relative_path": context.result_archive_relative_path,
            "result_archive_sha256": context.result_archive_sha256,
            "control_requests_submitted": 1,
            "control_requests_completed": 1,
            "partial_side_effect": False,
            "receipts": [receipt],
        }
