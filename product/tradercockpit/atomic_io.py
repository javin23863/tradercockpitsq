"""Atomic, durable text writes for data-root JSON stores.

A crash or a concurrent writer must never leave a torn or partially written store
file (these hold billing, account, credit, runtime and extension state). Each write
goes to a temp file in the same directory, is flushed and fsync'd, then atomically
renamed over the target with ``os.replace`` (atomic on POSIX and Windows). A per-path
in-process lock serializes writers within one process.

This is the single-node durability primitive for the personal/operator deployment.
Cross-process, multi-writer durability for the commercial multi-tenant deployment is
handled by the storage seam documented in ``docs/product-architecture-v1.md`` and is
intentionally out of scope for this file.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
from threading import Lock

_LOCKS: dict[str, Lock] = {}
_LOCKS_GUARD = Lock()


def _path_lock(path: Path) -> Lock:
    key = os.fspath(path)
    with _LOCKS_GUARD:
        lock = _LOCKS.get(key)
        if lock is None:
            lock = Lock()
            _LOCKS[key] = lock
        return lock


def atomic_write_text(path: Path | str, text: str, *, encoding: str = "utf-8") -> None:
    """Write ``text`` to ``path`` atomically and durably."""

    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    with _path_lock(target):
        temporary = target.with_name(f".{target.name}.{os.getpid()}.tmp")
        try:
            with open(temporary, "w", encoding=encoding) as handle:
                handle.write(text)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temporary, target)
        finally:
            try:
                temporary.unlink()
            except OSError:
                pass


def atomic_write_json(
    path: Path | str,
    payload: object,
    *,
    indent: int = 2,
    sort_keys: bool = True,
    ensure_ascii: bool = True,
) -> None:
    """Serialize ``payload`` as JSON (trailing newline) and write it atomically."""

    text = json.dumps(payload, indent=indent, sort_keys=sort_keys, ensure_ascii=ensure_ascii) + "\n"
    atomic_write_text(path, text)
