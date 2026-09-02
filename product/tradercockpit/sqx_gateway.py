"""Trusted native StrategyQuant X control gateway.

The gateway is intentionally narrow. It exposes only product-bound SQX
controls:

- Builder: load one exact approved Task-rooted ``.cfx`` configuration, then start Builder;
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
from pathlib import Path
import re
import subprocess
import tempfile
from threading import Lock
import time
from typing import Callable, Sequence

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1"
SQX_NATIVE_CONTROL_ERROR_SCHEMA = "tc.sqx-native-control-error.v1"
SQX_NATIVE_CONTROL_TIMEOUT_SECONDS = 60.0
# sqcli ``action=start`` runs Builder in-process until it finishes. Wait only for
# the producer marker, then stop the process so launch can return submitted.
SQX_BUILDER_START_READY_TIMEOUT_SECONDS = 120.0
# Retester ``startOnlyTask`` stays in-process until that CrossCheck finishes.
# 60s is enough for loadconfig, not for Additional Markets / Walk-Forward.
SQX_RETESTER_CONTROL_TIMEOUT_SECONDS = 1800.0
_START_RUNNING_MARKER = "=========== Project started ==========="
_BUILDER_PROJECT = "Builder"
_RETESTER_TASK = 1
_RETESTER_ENGINE_RELATIVE_PATH = "internal/libs/SQTradingLib.jar"
_RETESTER_PROJECT_RE = re.compile(r"^TraderCockpit-Retester-[0-9a-f]{32}$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CONTROL_LOCK = Lock()
# sqcli 144.2953 often exits 0 after printing a CLILogger refusal.
_CLI_REFUSAL_MARKERS = ("Cannot load config", "Cannot start project")


def _cli_text(completed: object) -> str:
    stdout = getattr(completed, "stdout", None) or ""
    stderr = getattr(completed, "stderr", None) or ""
    if isinstance(stdout, bytes):
        stdout = stdout.decode("utf-8", "replace")
    if isinstance(stderr, bytes):
        stderr = stderr.decode("utf-8", "replace")
    return f"{stdout}\n{stderr}".replace("<br>", "\n")


def _producer_cli_refusal(completed: object) -> str | None:
    return _refusal_in_text(_cli_text(completed))


def _refusal_in_text(text: str) -> str | None:
    text = text.replace("<br>", "\n")
    for marker in _CLI_REFUSAL_MARKERS:
        index = text.find(marker)
        if index < 0:
            continue
        snippet = text[index:]
        for stop in ("\n--------------------------------------------------", "\nBye"):
            cut = snippet.find(stop)
            if 0 <= cut:
                snippet = snippet[:cut]
        return snippet.strip()
    return None


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
    start_ready_timeout_seconds: float = SQX_BUILDER_START_READY_TIMEOUT_SECONDS
    spawner: Callable[..., subprocess.Popen] = subprocess.Popen

    def __post_init__(self) -> None:
        if (
            not isinstance(self.timeout_seconds, (int, float))
            or isinstance(self.timeout_seconds, bool)
            or self.timeout_seconds <= 0
        ):
            raise ValueError("timeout_seconds must be positive")
        if (
            not isinstance(self.start_ready_timeout_seconds, (int, float))
            or isinstance(self.start_ready_timeout_seconds, bool)
            or self.start_ready_timeout_seconds <= 0
        ):
            raise ValueError("start_ready_timeout_seconds must be positive")
        if not callable(self.runner):
            raise TypeError("runner must be callable")
        if not callable(self.spawner):
            raise TypeError("spawner must be callable")

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
                "native Builder loadconfig accepts only Task-rooted .cfx configuration archives",
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
                f"file={context.config}",
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

    def launch_builder(
        self,
        config_path: Path | str,
        *,
        expected_config_sha256: str | None,
    ) -> dict[str, object]:
        """Load one exact Builder config and submit native Builder start control.

        ``loadconfig`` must exit 0 with no producer refusal. Production ``start``
        waits only for SQX's ``Project started`` marker, then stops the in-process
        Builder run. That is not candidate generation.
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
                if action == "start" and self.runner is subprocess.run:
                    receipts.append(self._start_until_ready(sequence, context, command, receipts))
                    break
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
                refusal = _producer_cli_refusal(completed)
                if completed.returncode != 0 or refusal is not None:
                    reason = "sqx_command_rejected" if completed.returncode != 0 else "sqx_cli_refused"
                    failed = self._builder_receipt(
                        sequence,
                        action,
                        "rejected",
                        context,
                        exit_code=int(completed.returncode),
                        reason_code=reason,
                    )
                    raise SqxNativeGatewayError(
                        reason,
                        refusal or f"SQX {action} command exited nonzero",
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

    def _start_until_ready(
        self,
        sequence: int,
        context: _VerifiedControlContext,
        command: Sequence[str],
        prior: Sequence[dict[str, object]],
    ) -> dict[str, object]:
        def refuse(state: str, code: str, detail: str, exit_code: int | None) -> SqxNativeGatewayError:
            failed = self._builder_receipt(sequence, "start", state, context, exit_code=exit_code, reason_code=code)
            return SqxNativeGatewayError(code, detail, receipts=(*prior, failed))

        log_handle = tempfile.NamedTemporaryFile("wb", delete=False)
        log_path = Path(log_handle.name)
        try:
            process = self.spawner(
                list(command),
                cwd=str(context.home),
                stdin=subprocess.DEVNULL,
                stdout=log_handle,
                stderr=subprocess.STDOUT,
                shell=False,
            )
        except OSError as exc:
            log_handle.close()
            log_path.unlink(missing_ok=True)
            raise refuse("launch_failed", "sqx_command_failed", "SQX start command could not be executed", None) from exc
        log_handle.close()
        deadline = time.monotonic() + float(self.start_ready_timeout_seconds)
        try:
            while True:
                try:
                    text = log_path.read_text(encoding="utf-8", errors="replace")
                except OSError:
                    text = ""
                exit_code = process.poll()
                refusal = _refusal_in_text(text)
                if exit_code is not None:
                    if type(exit_code) is not int:
                        raise refuse("invalid_receipt", "sqx_command_failed", "SQX process returned an invalid exit code", None)
                    if exit_code != 0 or refusal is not None:
                        reason = "sqx_command_rejected" if exit_code != 0 else "sqx_cli_refused"
                        raise refuse("rejected", reason, refusal or "SQX start command exited nonzero", int(exit_code))
                    return self._builder_receipt(sequence, "start", "completed", context, exit_code=int(exit_code))
                if refusal is not None:
                    process.kill()
                    process.wait(timeout=5)
                    raise refuse("rejected", "sqx_cli_refused", refusal, None)
                if _START_RUNNING_MARKER in text:
                    process.kill()
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        pass
                    return self._builder_receipt(sequence, "start", "completed", context, exit_code=0)
                if time.monotonic() >= deadline:
                    process.kill()
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        pass
                    raise refuse("timeout", "sqx_command_timeout", "SQX start did not report Project started in time", None)
                time.sleep(0.2)
        finally:
            if process.poll() is None:
                process.kill()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    pass
            log_path.unlink(missing_ok=True)

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
                    timeout=float(SQX_RETESTER_CONTROL_TIMEOUT_SECONDS),
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
            refusal = _producer_cli_refusal(completed)
            if completed.returncode != 0 or refusal is not None:
                reason = "sqx_command_rejected" if completed.returncode != 0 else "sqx_cli_refused"
                failed = self._retester_receipt(
                    "rejected",
                    context,
                    context.project_name,
                    exit_code=int(completed.returncode),
                    reason_code=reason,
                )
                raise SqxNativeGatewayError(
                    reason,
                    refusal or "SQX Retester startOnlyTask command exited nonzero",
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
