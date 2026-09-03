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
from threading import Lock
import time
from typing import Callable, Sequence
from urllib.error import URLError
from urllib.parse import quote
from urllib.request import urlopen

from tradercockpit.sqx_presets import SQX_BUILD, SqxPresetRuntimeError, verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


SQX_NATIVE_CONTROL_SCHEMA = "tc.sqx-native-control.v1"
SQX_NATIVE_CONTROL_ERROR_SCHEMA = "tc.sqx-native-control-error.v1"
SQX_NATIVE_CONTROL_TIMEOUT_SECONDS = 60.0
# Observed sqcli 144.2953: ``-project action=start`` runs the project inside the sqcli
# process until it finishes or is stopped, printing these markers to stdout on the way.
SQX_BUILDER_START_READY_TIMEOUT_SECONDS = 120.0
_START_RUNNING_MARKER = "=========== Project started ==========="
_START_HTTP_PORT_RE = re.compile(r"Server started on port (\d+)")
_START_FINISHED_MARKER = "All tasks completed"
_SQX_HTTP_TIMEOUT_SECONDS = 20.0
_BUILDER_PROJECT = "Builder"
_RETESTER_TASK = 1
_RETESTER_ENGINE_RELATIVE_PATH = "internal/libs/SQTradingLib.jar"
_RETESTER_PROJECT_RE = re.compile(r"^TraderCockpit-Retester-[0-9a-f]{32}$")
_DIGEST_RE = re.compile(r"^[0-9a-f]{64}$")
_CONTROL_LOCK = Lock()
# sqcli often exits 0 after printing a CLILogger refusal (observed 144.2953 loadconfig/start).
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
    """Return the producer CLI refusal text, or None when SQX did not print one."""

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
                break
        cleaned = " ".join(line.strip() for line in snippet.splitlines() if line.strip())
        return cleaned or marker
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


def sqx_http_command(http_port: int, command: str, *, timeout_seconds: float = _SQX_HTTP_TIMEOUT_SECONDS) -> str:
    """Send one fixed command to the HTTP API of an sqcli process the cockpit started.

    sqcli 144.2953 prints ``HTTP API started ... http://localhost:<port>/call?cmd=-h``
    and answers ``-project action=status|stop name=Builder`` with plain text.
    """

    if type(http_port) is not int or not 1 <= http_port <= 65535:
        raise SqxNativeGatewayError("sqx_http_port_invalid", "sqcli HTTP API port is unknown")
    url = f"http://127.0.0.1:{http_port}/call?cmd={quote(command, safe='=')}"
    try:
        with urlopen(url, timeout=timeout_seconds) as response:  # noqa: S310 - fixed loopback URL
            return response.read().decode("utf-8", "replace").replace("<br>", "\n")
    except (URLError, OSError, ValueError) as exc:
        raise SqxNativeGatewayError("sqx_http_unreachable", "running sqcli HTTP API did not answer") from exc


def builder_status_command() -> str:
    return f"-project action=status name={_BUILDER_PROJECT}"


def builder_stop_command() -> str:
    return f"-project action=stop name={_BUILDER_PROJECT}"


def parse_builder_status(text: str) -> dict[str, str]:
    """Split sqcli's two-column status table into ``{native label: native value}``."""

    rows: dict[str, str] = {}
    for line in text.splitlines():
        match = re.match(r"^(\S.*?\S)\s{2,}(\S.*)$", line.strip())
        if match:
            rows[match.group(1)] = match.group(2)
    return rows


class SqxBuilderWorker:
    """Desktop-owned handle for one in-process sqcli Builder run.

    Implements the desktop worker supervisor protocol. ``terminate`` asks SQX to
    stop gracefully over its HTTP API (SQX then saves Results and exits itself);
    ``kill`` is the hard fallback the supervisor uses when that does not finish.
    """

    def __init__(self, process: subprocess.Popen, *, log_path: Path, http_port: int | None) -> None:
        self.process = process
        self.log_path = log_path
        self.http_port = http_port
        self.stop_requested = False

    @property
    def pid(self) -> int:
        return int(self.process.pid)

    def poll(self) -> int | None:
        return self.process.poll()

    def wait(self, timeout: float | None = None) -> int:
        return self.process.wait(timeout=timeout)

    def read_log(self) -> str:
        try:
            return self.log_path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            return ""

    def status(self) -> dict[str, object]:
        if self.http_port is None:
            raise SqxNativeGatewayError("sqx_http_port_invalid", "sqcli HTTP API port is unknown")
        text = sqx_http_command(self.http_port, builder_status_command())
        return {"raw": text, "rows": parse_builder_status(text)}

    def request_stop(self) -> str:
        if self.http_port is None:
            raise SqxNativeGatewayError("sqx_http_port_invalid", "sqcli HTTP API port is unknown")
        text = sqx_http_command(self.http_port, builder_stop_command())
        self.stop_requested = True
        return text

    def terminate(self) -> None:
        if self.poll() is not None:
            return
        try:
            self.request_stop()
        except SqxNativeGatewayError:
            self.process.terminate()

    def kill(self) -> None:
        self.process.kill()


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
    spawner: Callable[..., subprocess.Popen] = subprocess.Popen
    start_ready_timeout_seconds: float = SQX_BUILDER_START_READY_TIMEOUT_SECONDS
    poll_interval_seconds: float = 0.5
    running_grace_seconds: float = 5.0
    worker: SqxBuilderWorker | None = None

    def __post_init__(self) -> None:
        for value in (
            self.timeout_seconds,
            self.start_ready_timeout_seconds,
            self.poll_interval_seconds,
            self.running_grace_seconds,
        ):
            if not isinstance(value, (int, float)) or isinstance(value, bool) or value <= 0:
                raise ValueError("timeout_seconds must be positive")
        if not callable(self.runner) or not callable(self.spawner):
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
        worker_log_path: Path | str | None = None,
    ) -> dict[str, object]:
        """Load one exact Builder config and submit native Builder start control.

        ``loadconfig`` is a bounded CLI process that must exit 0 *and* print no
        producer refusal (sqcli 144.2953 exits 0 after ``Cannot load config`` /
        ``Cannot start project``). With ``worker_log_path`` the ``start`` process is
        spawned as a supervised worker: sqcli runs the Builder in-process, so the
        gateway waits only for SQX's ``Project started`` marker (state ``running``)
        or an early refusal/exit, and exposes the handle as ``self.worker``.
        Without it the legacy bounded synchronous start is used. Neither claims
        candidate generation.
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
                if action == "start" and worker_log_path is not None:
                    receipts.append(self._start_supervised(sequence, context, command, Path(worker_log_path), receipts))
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
        running = self.worker is not None and receipts[-1].get("state") == "running"
        result: dict[str, object] = {
            "schema": SQX_NATIVE_CONTROL_SCHEMA,
            "operation": "builder_loadconfig_start",
            "project": _BUILDER_PROJECT,
            "state": "running" if running else "submitted",
            "sqx_build": SQX_BUILD,
            "launcher_sha256": last_context.launcher_sha256,
            "config_relative_path": last_context.config_relative_path,
            "config_sha256": last_context.config_sha256,
            "control_requests_submitted": len(receipts),
            "control_requests_completed": len(receipts),
            "partial_side_effect": False,
            "receipts": [dict(item) for item in receipts],
        }
        if running:
            assert self.worker is not None
            result["worker"] = {
                "pid": self.worker.pid,
                "http_port": self.worker.http_port,
                "log_path": str(self.worker.log_path),
            }
        return result

    def _start_supervised(
        self,
        sequence: int,
        context: _VerifiedControlContext,
        command: Sequence[str],
        log_path: Path,
        prior: Sequence[dict[str, object]],
    ) -> dict[str, object]:
        def refuse(state: str, code: str, detail: str, exit_code: int | None) -> SqxNativeGatewayError:
            failed = self._builder_receipt(sequence, "start", state, context, exit_code=exit_code, reason_code=code)
            return SqxNativeGatewayError(code, detail, receipts=(*prior, failed))

        try:
            log_path.parent.mkdir(parents=True, exist_ok=True)
            log_handle = log_path.open("wb")
        except OSError as exc:
            raise refuse("launch_failed", "sqx_worker_log_unwritable", "Builder worker log could not be created", None) from exc
        try:
            with log_handle:
                process = self.spawner(
                    list(command),
                    cwd=str(context.home),
                    stdin=subprocess.DEVNULL,
                    stdout=log_handle,
                    stderr=subprocess.STDOUT,
                    shell=False,
                )
        except OSError as exc:
            raise refuse("launch_failed", "sqx_command_failed", "SQX start command could not be executed", None) from exc

        worker = SqxBuilderWorker(process, log_path=log_path, http_port=None)
        deadline = time.monotonic() + float(self.start_ready_timeout_seconds)
        # Observed: on a config error SQX prints "Project started" and the refusal in the
        # same millisecond, then exits a few seconds later. Hold before declaring running.
        running_since: float | None = None
        while True:
            text = worker.read_log()
            if worker.http_port is None:
                port = _START_HTTP_PORT_RE.search(text)
                if port:
                    worker.http_port = int(port.group(1))
            exit_code = worker.poll()
            refusal = _refusal_in_text(text)
            if exit_code is not None:
                if type(exit_code) is not int:
                    raise refuse("invalid_receipt", "sqx_command_failed", "SQX process returned an invalid exit code", None)
                if exit_code != 0 or refusal is not None:
                    reason = "sqx_command_rejected" if exit_code != 0 else "sqx_cli_refused"
                    raise refuse("rejected", reason, refusal or "SQX start command exited nonzero", int(exit_code))
                return self._builder_receipt(sequence, "start", "completed", context, exit_code=int(exit_code))
            if refusal is None and _START_RUNNING_MARKER in text:
                now = time.monotonic()
                if running_since is None:
                    running_since = now
                elif now - running_since >= float(self.running_grace_seconds):
                    self.worker = worker
                    return self._builder_receipt(sequence, "start", "running", context, exit_code=None)
            if time.monotonic() >= deadline:
                if refusal is not None:
                    worker.kill()
                    raise refuse("rejected", "sqx_cli_refused", refusal, None)
                worker.kill()
                raise refuse("timeout", "sqx_command_timeout", "SQX start did not report Project started in time", None)
            time.sleep(float(self.poll_interval_seconds))

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
