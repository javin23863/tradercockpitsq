"""Desktop-owned process lifecycle for the one TraderCockpit application runtime.

This module does not launch native work. It owns termination of long-lived process
handles that future application features explicitly register with the desktop.
Closing the desktop seals registration first, then the desktop host shuts its local
server and stops every registered process, escalating from terminate to kill when a
worker does not exit within the bounded grace period.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite
import subprocess
from threading import Lock
from typing import Protocol


DEFAULT_WORKER_STOP_TIMEOUT_SECONDS = 5.0


class DesktopLifecycleError(RuntimeError):
    """Raised when desktop-owned resources cannot be shut down cleanly."""


class OwnedProcess(Protocol):
    """Minimal subprocess contract accepted by the desktop worker supervisor."""

    def poll(self) -> int | None: ...

    def terminate(self) -> None: ...

    def kill(self) -> None: ...

    def wait(self, timeout: float | None = None) -> int: ...


@dataclass(frozen=True, slots=True)
class _OwnedWorker:
    label: str
    process: OwnedProcess
    timeout_seconds: float


def _validated_label(value: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("desktop worker label must be a non-empty string")
    label = value.strip()
    if any(token in label for token in ("\r", "\n", "\x00")):
        raise ValueError("desktop worker label contains unsupported control characters")
    return label


def _validated_timeout(value: float) -> float:
    if not isinstance(value, (int, float)) or isinstance(value, bool):
        raise ValueError("desktop worker stop timeout must be a finite positive number")
    timeout = float(value)
    if not isfinite(timeout) or timeout <= 0:
        raise ValueError("desktop worker stop timeout must be a finite positive number")
    return timeout


def _validated_process(process: OwnedProcess) -> OwnedProcess:
    required = ("poll", "terminate", "kill", "wait")
    missing = [name for name in required if not callable(getattr(process, name, None))]
    if missing:
        raise TypeError(
            "desktop worker process is missing required callable(s): " + ", ".join(missing)
        )
    return process


def _alive(process: OwnedProcess) -> bool:
    return process.poll() is None


def _bounded_wait(process: OwnedProcess, timeout_seconds: float) -> bool:
    try:
        process.wait(timeout=timeout_seconds)
    except subprocess.TimeoutExpired:
        return False
    return not _alive(process)


def _stop_worker(worker: _OwnedWorker) -> None:
    process = worker.process
    if not _alive(process):
        return

    terminate_error: Exception | None = None
    try:
        process.terminate()
    except Exception as exc:  # cleanup must still attempt the bounded kill fallback
        terminate_error = exc

    try:
        stopped = not _alive(process) or _bounded_wait(process, worker.timeout_seconds)
    except Exception as exc:  # cleanup must still attempt the bounded kill fallback
        stopped = False
        if terminate_error is None:
            terminate_error = exc

    if stopped:
        return

    kill_error: Exception | None = None
    try:
        process.kill()
    except Exception as exc:  # surfaced below with worker identity
        kill_error = exc

    try:
        stopped = not _alive(process) or _bounded_wait(process, worker.timeout_seconds)
    except Exception as exc:  # surfaced below with worker identity
        stopped = False
        if kill_error is None:
            kill_error = exc

    if stopped:
        return

    detail = f"desktop-owned worker {worker.label!r} did not stop"
    cause = kill_error or terminate_error
    if cause is not None:
        raise DesktopLifecycleError(f"{detail}: {cause}") from cause
    raise DesktopLifecycleError(detail)


class DesktopWorkerSupervisor:
    """Own registered long-lived processes for one desktop lifecycle.

    Registration is intentionally explicit. Future native-worker code must hand the
    actual process handle to this supervisor instead of spawning detached work. Once
    desktop shutdown begins, the supervisor is sealed and rejects new workers.
    """

    def __init__(self) -> None:
        self._lock = Lock()
        self._sealed = False
        self._workers: list[_OwnedWorker] = []

    @property
    def sealed(self) -> bool:
        with self._lock:
            return self._sealed

    @property
    def active_count(self) -> int:
        with self._lock:
            return sum(1 for worker in self._workers if _alive(worker.process))

    def register(
        self,
        process: OwnedProcess,
        *,
        label: str,
        timeout_seconds: float = DEFAULT_WORKER_STOP_TIMEOUT_SECONDS,
    ) -> None:
        worker = _OwnedWorker(
            label=_validated_label(label),
            process=_validated_process(process),
            timeout_seconds=_validated_timeout(timeout_seconds),
        )
        with self._lock:
            if self._sealed:
                raise DesktopLifecycleError(
                    "desktop lifecycle is closing; new worker registration is refused"
                )
            if any(item.process is process for item in self._workers):
                raise DesktopLifecycleError("desktop worker process is already registered")
            self._workers.append(worker)

    def seal(self) -> None:
        """Refuse future worker registration without stopping existing workers yet."""

        with self._lock:
            self._sealed = True

    def stop_all(self) -> None:
        """Stop every registered worker, preserving failed handles for explicit retry."""

        with self._lock:
            self._sealed = True
            workers = tuple(reversed(self._workers))

        failures: list[tuple[_OwnedWorker, str]] = []
        stopped_ids: set[int] = set()
        for worker in workers:
            try:
                _stop_worker(worker)
            except Exception as exc:
                failures.append((worker, str(exc)))
            else:
                stopped_ids.add(id(worker.process))

        with self._lock:
            self._workers = [
                worker
                for worker in self._workers
                if id(worker.process) not in stopped_ids
            ]

        if failures:
            detail = "; ".join(
                f"{worker.label}: {message}" for worker, message in failures
            )
            raise DesktopLifecycleError(
                f"desktop worker shutdown incomplete ({len(failures)} failure(s)): {detail}"
            )
