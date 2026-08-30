"""Filesystem content store for immutable TraderCockpit execution objects."""

from __future__ import annotations

import os
from pathlib import Path
import tempfile

from tradercockpit.domain import ContentAddress

from .wire import WireFormatError, decode_addressed_object, encode_addressed_object


class ContentStoreError(RuntimeError):
    """Raised when durable immutable-object custody is missing or corrupted."""


class FileObjectStore:
    """Store immutable objects at paths derived only from validated content refs."""

    def __init__(self, root: Path | str):
        self.root = Path(root).resolve()
        self.objects_root = self.root / "objects"
        self.objects_root.mkdir(parents=True, exist_ok=True)

    def _path(self, ref: ContentAddress) -> Path:
        if not isinstance(ref, ContentAddress):
            raise ContentStoreError("ref must be a ContentAddress")
        return (
            self.objects_root
            / ref.kind
            / f"v{ref.version}"
            / f"{ref.sha256}.json"
        )

    def put(self, value: object) -> ContentAddress:
        try:
            encoded = encode_addressed_object(value)
        except WireFormatError as exc:
            raise ContentStoreError(str(exc)) from exc
        ref = getattr(value, "ref", None)
        if not isinstance(ref, ContentAddress):
            raise ContentStoreError("stored value did not expose a ContentAddress")
        target = self._path(ref)
        target.parent.mkdir(parents=True, exist_ok=True)

        if target.exists():
            existing = target.read_bytes()
            if existing != encoded:
                raise ContentStoreError(
                    f"existing object bytes disagree with immutable ref {ref}"
                )
            try:
                existing_value = decode_addressed_object(existing)
            except WireFormatError as exc:
                raise ContentStoreError(
                    f"existing object for {ref} is corrupt: {exc}"
                ) from exc
            if getattr(existing_value, "ref", None) != ref:
                raise ContentStoreError(f"existing object for {ref} resolves to another ref")
            return ref

        fd, temporary_name = tempfile.mkstemp(
            prefix=f".{ref.sha256}.",
            suffix=".tmp",
            dir=target.parent,
        )
        temporary = Path(temporary_name)
        try:
            with os.fdopen(fd, "wb") as handle:
                handle.write(encoded)
                handle.flush()
                os.fsync(handle.fileno())
            os.replace(temporary, target)
        finally:
            if temporary.exists():
                temporary.unlink()

        # Never trust the write merely because rename succeeded.
        try:
            stored = target.read_bytes()
        except OSError as exc:
            raise ContentStoreError(f"unable to read stored object {ref}") from exc
        if stored != encoded:
            raise ContentStoreError(f"stored bytes changed for immutable ref {ref}")
        return ref

    def resolve(self, ref: ContentAddress) -> object:
        target = self._path(ref)
        try:
            encoded = target.read_bytes()
        except FileNotFoundError as exc:
            raise KeyError(ref) from exc
        except OSError as exc:
            raise ContentStoreError(f"unable to read object {ref}") from exc

        try:
            value = decode_addressed_object(encoded)
        except WireFormatError as exc:
            raise ContentStoreError(f"corrupt object {ref}: {exc}") from exc
        actual_ref = getattr(value, "ref", None)
        if actual_ref != ref:
            raise ContentStoreError(
                f"object stored at {ref} resolved to different ref {actual_ref}"
            )
        return value

    def contains(self, ref: ContentAddress) -> bool:
        return self._path(ref).is_file()
