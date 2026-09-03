"""Trusted native StrategyQuant X control gateway.

The gateway is intentionally narrow. It exposes only product-bound SQX
controls:

- Builder: load one exact approved Task-rooted task config (.cfx), then start Builder;
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
import subprocess
from threading import Lock
from typing import Callable, NoReturn, Sequence
from xml.etree import ElementTree
from zipfile import ZIP_STORED, BadZipFile, ZipFile, ZipInfo

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1"
SQX_NATIVE_CONTROL_ERROR_SCHEMA = "tc.sqx-native-control-error.v1"
SQX_NATIVE_CONTROL_TIMEOUT_SECONDS = 60.0
_BUILDER_PROJECT = "Builder"
_BUILDER_START_WORKER_LABEL = "sqx-builder-start"
_RETESTER_TASK = 1
_RETESTER_ENGINE_RELATIVE_PATH = "internal/libs/SQTradingLib.jar"
_RETESTER_PROJECT_RE = re.compile(r"^TraderCockpit-Retester-[0-9a-f]{32}$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_LOADCONFIG_FAILURE_RE = re.compile(r"cannot load config|file not found", re.IGNORECASE)
_CONTROL_LOCK = Lock()


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


def _command_output(completed: object) -> str:
    parts: list[str] = []
    for attr in ("stdout", "stderr"):
        value = getattr(completed, attr, None)
        if isinstance(value, bytes):
            parts.append(value.decode("utf-8", "replace"))
        elif isinstance(value, str):
            parts.append(value)
    return "\n".join(parts)


def _loadconfig_output_failed(completed: object) -> bool:
    return bool(_LOADCONFIG_FAILURE_RE.search(_command_output(completed)))


def _loadconfig_file_arg(config: Path) -> str:
    """Return the loadconfig file= value SQX 144.2953 actually resolves.

    Native loadconfig appends ``.cfx`` to ``file=``. Passing a ``.cfx`` path
    would make SQX look for ``*.cfx.cfx``. Passing a ``.xml`` path makes it
    look for ``*.xml.cfx``. The staged Task-rooted archive is ``{digest}.cfx``;
    the argv value is that path without the ``.cfx`` suffix.
    """

    if config.suffix.lower() == ".cfx":
        return str(config.with_suffix(""))
    return str(config)


def _local_xml_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def task_root_xml(payload: bytes) -> bytes | None:
    """Return payload when it is a Task-rooted XML document."""

    try:
        root = ElementTree.fromstring(payload)
    except ElementTree.ParseError:
        return None
    if _local_xml_name(root.tag) != "Task":
        return None
    return payload


def task_document_from_cfx(data: bytes) -> bytes:
    """Return the XML document SQX 144.2953 loadconfig inspects inside a .cfx.

    Native loadconfig opens the resolved ``*.cfx`` and looks for a Task element
    in that document. For a zip archive it reads ``config.xml``. A copy of
    ``project.cfx`` fails because that ``config.xml`` is Project-rooted. A
    non-zip payload is inspected as the document itself.
    """

    try:
        with ZipFile(BytesIO(data)) as archive:
            names = archive.namelist()
            if "config.xml" in names:
                return archive.read("config.xml")
    except BadZipFile:
        return data
    return data


def is_task_rooted_cfx(data: bytes) -> bool:
    return task_root_xml(task_document_from_cfx(data)) is not None


def pack_task_rooted_cfx(task_xml: bytes) -> bytes:
    """Wrap exact approved Build-Task1.xml bytes as the Task-rooted CFX SQX loads.

    This is the native ``.cfx`` container, not a substitute task. The inner
    ``config.xml`` bytes are the approved Task document unchanged.
    """

    if task_root_xml(task_xml) is None:
        raise SqxNativeGatewayError(
            "config_task_element_missing",
            "native Builder loadconfig requires a Task-rooted task config",
        )
    buffer = BytesIO()
    info = ZipInfo("config.xml", date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = ZIP_STORED
    info.create_system = 0
    with ZipFile(buffer, "w") as archive:
        archive.writestr(info, task_xml)
    return buffer.getvalue()


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
        if self.register_worker is not None and not callable(self.register_worker):
            raise TypeError("register_worker must be callable")
        if not callable(self.process_factory):
            raise TypeError("process_factory must be callable")

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
                "native Builder loadconfig accepts only the exact approved Task-rooted .cfx",
            )
        if not config.is_file():
            raise SqxNativeGatewayError("config_missing", "native Builder configuration is missing")
        try:
            observed_config = _sha256_file(config)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError("config_unreadable", "native Builder configuration could not be read") from exc
        if observed_config != expected_config:
            raise SqxNativeGatewayError(
                "config_hash_mismatch",
                "native Builder configuration does not match the approved identity",
            )
        try:
            staged_bytes = config.read_bytes()
        except OSError as exc:
            raise SqxNativeGatewayError("config_unreadable", "native Builder configuration could not be read") from exc
        if not is_task_rooted_cfx(staged_bytes):
            raise SqxNativeGatewayError(
                "config_task_element_missing",
                "native Builder loadconfig requires a Task-rooted task config, not a project.cfx copy",
            )

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
            observed_project = _sha256_file(project_file)
        except SqxNativeGatewayError as exc:
            raise SqxNativeGatewayError("retester_project_unreadable", "isolated Retester project.cfx could not be read") from exc
        if observed_project != expected_project:
            raise SqxNativeGatewayError(
                "retester_project_hash_mismatch",
                "isolated Retester project does not match its staged identity",
            )

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
                f"file={_loadconfig_file_arg(context.config)}",
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
            "action": "startOnlyTask",
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

    def _builder_failure(
        self,
        receipts: Sequence[dict[str, object]],
        sequence: int,
        action: str,
        state: str,
        context: _VerifiedControlContext | None,
        *,
        code: str,
        detail: str,
        exit_code: int | None = None,
        cause: BaseException | None = None,
    ) -> NoReturn:
        failed = self._builder_receipt(
            sequence,
            action,
            state,
            context,
            exit_code=exit_code,
            reason_code=code,
        )
        raise SqxNativeGatewayError(code, detail, receipts=(*receipts, failed)) from cause

    def _run_builder_command(
        self,
        sequence: int,
        action: str,
        command: tuple[str, ...],
        context: _VerifiedControlContext,
        receipts: Sequence[dict[str, object]],
    ) -> subprocess.CompletedProcess[str]:
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
            )
        except subprocess.TimeoutExpired as exc:
            self._builder_failure(
                receipts,
                sequence,
                action,
                "timeout",
                context,
                code="sqx_command_timeout",
                detail=f"SQX {action} command timed out",
                cause=exc,
            )
        except OSError as exc:
            self._builder_failure(
                receipts,
                sequence,
                action,
                "launch_failed",
                context,
                code="sqx_command_failed",
                detail=f"SQX {action} command could not be executed",
                cause=exc,
            )
        if type(completed.returncode) is not int:
            self._builder_failure(
                receipts,
                sequence,
                action,
                "invalid_receipt",
                context,
                code="sqx_command_failed",
                detail="SQX command runner returned an invalid exit code",
            )
        if completed.returncode != 0:
            self._builder_failure(
                receipts,
                sequence,
                action,
                "rejected",
                context,
                code="sqx_command_rejected",
                detail=f"SQX {action} command exited nonzero",
                exit_code=int(completed.returncode),
            )
        return completed

    def _submit_builder_start(
        self,
        sequence: int,
        command: tuple[str, ...],
        context: _VerifiedControlContext,
        receipts: Sequence[dict[str, object]],
    ) -> dict[str, object]:
        assert self.register_worker is not None
        try:
            process = self.process_factory(
                list(command),
                cwd=str(context.home),
                stdin=subprocess.DEVNULL,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                shell=False,
            )
        except OSError as exc:
            self._builder_failure(
                receipts,
                sequence,
                "start",
                "launch_failed",
                context,
                code="sqx_command_failed",
                detail="SQX start command could not be executed",
                cause=exc,
            )
        try:
            self.register_worker(process, label=_BUILDER_START_WORKER_LABEL)
        except Exception as exc:
            terminate = getattr(process, "terminate", None)
            if callable(terminate):
                try:
                    terminate()
                except Exception:
                    pass
            self._builder_failure(
                receipts,
                sequence,
                "start",
                "launch_failed",
                context,
                code="desktop_worker_unregistered",
                detail="SQX start process could not be registered with the desktop worker supervisor",
                cause=exc,
            )
        poll = getattr(process, "poll", None)
        exit_code = poll() if callable(poll) else None
        if exit_code not in (None, 0):
            self._builder_failure(
                receipts,
                sequence,
                "start",
                "rejected",
                context,
                code="sqx_command_rejected",
                detail="SQX start command exited nonzero",
                exit_code=int(exit_code),
            )
        return self._builder_receipt(
            sequence,
            "start",
            "completed",
            context,
            exit_code=exit_code if type(exit_code) is int else None,
        )

    def launch_builder(
        self,
        config_path: Path | str,
        *,
        expected_config_sha256: str | None,
    ) -> dict[str, object]:
        """Load one exact Task-rooted Builder task config and submit native start.

        ``loadconfig`` must exit 0 and must not print a native load failure.
        ``start`` is a long-lived SQX process: when a desktop worker registrar is
        bound, the process is registered and left running. Success does not claim
        candidate generation or any research result.
        """

        receipts: list[dict[str, object]] = []
        last_context: _VerifiedControlContext | None = None
        with _CONTROL_LOCK:
            for sequence, action in enumerate(("loadconfig", "start"), start=1):
                try:
                    context = self._preflight(config_path, expected_config_sha256)
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
                if action == "start" and self.register_worker is not None:
                    receipts.append(
                        self._submit_builder_start(sequence, command, context, receipts)
                    )
                    continue
                completed = self._run_builder_command(sequence, action, command, context, receipts)
                if action == "loadconfig" and _loadconfig_output_failed(completed):
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "rejected",
                        context,
                        exit_code=int(completed.returncode),
                        reason_code="sqx_loadconfig_failed",
                    )
                    raise SqxNativeGatewayError(
                        "sqx_loadconfig_failed",
                        "SQX loadconfig did not load the approved Task-rooted task config",
                        receipts=(*receipts, failed),
                    )
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

            command = [
                str(context.launcher),
                "-project",
                "action=startOnlyTask",
                f"name={context.project_name}",
                f"task={_RETESTER_TASK}",
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
                    "SQX Retester startOnlyTask command timed out",
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
                    "SQX Retester startOnlyTask command could not be executed",
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
                    "SQX Retester startOnlyTask command exited nonzero",
                    receipts=(failed,),
                )

            receipt = self._retester_receipt(
                "completed",
                context,
                context.project_name,
                exit_code=int(completed.returncode),
            )

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
