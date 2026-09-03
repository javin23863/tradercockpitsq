"""Backend capability/add-on registry.

One authority supplies typed descriptors. Add-ons bind registered slots only.
They cannot inject HTML/script, rewrite top-level navigation, or claim producer truth.
Unknown descriptor versions fail closed.
"""

from __future__ import annotations

from json import JSONDecodeError, loads
from pathlib import Path
from typing import Any


REGISTRY_SCHEMA = "tc.capability-addon-registry.v1"
ADDON_SCHEMA = "tc.capability-addon.v1"
ADDON_DESCRIPTOR_VERSION = 1
REGISTRY_API_PATH = "/api/capabilities"
ADDONS_DIR_NAME = "addons"
NONE_SCHEMA = "tc.capability-addon.none.v1"
NAV_AUTHORITY = "platform"

PLATFORM_SURFACES = ("home", "research", "explore", "automation", "operate", "settings")

REGISTERED_SLOTS = (
    {
        "id": "explore.extensions",
        "surface": "explore",
        "kind": "status_card",
        "label": "Explore extensions",
    },
    {
        "id": "automation.extensions",
        "surface": "automation",
        "kind": "status_card",
        "label": "Automation extensions",
    },
    {
        "id": "settings.extensions",
        "surface": "settings",
        "kind": "status_card",
        "label": "Settings extensions",
    },
)

_SLOT_IDS = frozenset(slot["id"] for slot in REGISTERED_SLOTS)
_SLOT_SURFACES = frozenset(slot["surface"] for slot in REGISTERED_SLOTS)
_ADDON_KEYS = frozenset(
    {
        "schema",
        "descriptor_version",
        "id",
        "version",
        "producer",
        "availability",
        "slot",
        "config_schema",
        "read_schema",
        "action_schema",
        "presentation",
    }
)
_PRESENTATION_KEYS = frozenset({"title", "detail"})
_PRODUCERS = frozenset({"operator", "platform"})
_AVAILABILITY = frozenset({"ready", "unavailable"})
_ID_CHARS = frozenset("abcdefghijklmnopqrstuvwxyz0123456789.-")
_ALLOWED_CONTRACT_SCHEMAS = frozenset({NONE_SCHEMA})


class CapabilityRegistryError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def registered_slots() -> list[dict[str, str]]:
    return [dict(slot) for slot in REGISTERED_SLOTS]


def capability_registry_record(data_root: Path | str | None = None) -> dict[str, Any]:
    """Return the typed add-on registry. Missing add-on files means zero add-ons, not unimplemented."""

    addons: list[dict[str, Any]] = []
    refused: list[dict[str, str]] = []
    try:
        directory = _addons_dir(data_root)
    except CapabilityRegistryError as exc:
        return _record([], [{"source": ADDONS_DIR_NAME, "reason_code": exc.code, "detail": exc.detail}], status="unavailable")

    if directory is None:
        return _record([], [])

    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if path.name.startswith(".") or path.suffix.lower() != ".json":
            continue
        try:
            addons.append(_load_addon_file(directory, path, {item["id"] for item in addons}))
        except CapabilityRegistryError as exc:
            refused.append({"source": path.name, "reason_code": exc.code, "detail": exc.detail})
    return _record(addons, refused)


def extensions_status_record(data_root: Path | str | None = None) -> dict[str, Any]:
    """Compact /api/status view of the same registry authority."""

    registry = capability_registry_record(data_root)
    return {
        "status": registry["status"],
        "reason_code": registry["reason_code"],
        "detail": registry["detail"],
        "registry_schema": REGISTRY_SCHEMA,
        "nav_authority": NAV_AUTHORITY,
        "slot_count": len(REGISTERED_SLOTS),
        "addon_count": registry["addon_count"],
        "refused_count": registry["refused_count"],
    }


def _record(addons: list[dict[str, Any]], refused: list[dict[str, str]], *, status: str = "ready") -> dict[str, Any]:
    reason = None if status == "ready" else (refused[0]["reason_code"] if refused else "addon_store_unreadable")
    detail = (
        "Typed add-on registry is ready. Add-ons bind registered slots only and cannot rewrite "
        "top-level navigation or inject script/HTML."
        if status == "ready"
        else (refused[0]["detail"] if refused else "The add-on store could not be read.")
    )
    if status == "ready" and refused:
        detail = (
            f"{detail} {len(refused)} descriptor"
            f"{'' if len(refused) == 1 else 's'} failed closed and "
            "were not bound."
        )
    return {
        "schema": REGISTRY_SCHEMA,
        "status": status,
        "reason_code": reason,
        "detail": detail,
        "nav_authority": NAV_AUTHORITY,
        "surfaces": list(PLATFORM_SURFACES),
        "slots": registered_slots(),
        "addons": addons,
        "refused": refused,
        "addon_count": len(addons),
        "refused_count": len(refused),
    }


def _addons_dir(data_root: Path | str | None) -> Path | None:
    if data_root is None:
        return None
    root = Path(data_root).expanduser().resolve()
    candidate = root / ADDONS_DIR_NAME
    if not candidate.exists():
        return None
    if candidate.is_symlink():
        raise CapabilityRegistryError(
            "addon_store_path_escape",
            "Add-on store must be a real directory inside the application data root.",
        )
    if not candidate.is_dir():
        raise CapabilityRegistryError(
            "addon_store_unreadable",
            "Add-on store must be a directory of JSON descriptors.",
        )
    try:
        resolved = candidate.resolve()
        resolved.relative_to(root)
    except (OSError, RuntimeError, ValueError) as exc:
        raise CapabilityRegistryError(
            "addon_store_path_escape",
            "Add-on store resolved outside the application data root.",
        ) from exc
    return resolved


def _load_addon_file(directory: Path, path: Path, seen_ids: set[str]) -> dict[str, Any]:
    if path.is_symlink():
        raise CapabilityRegistryError("addon_path_escape", f"{path.name} is a symlink and was refused.")
    try:
        resolved = path.resolve()
        resolved.relative_to(directory)
    except (OSError, RuntimeError, ValueError) as exc:
        raise CapabilityRegistryError("addon_path_escape", f"{path.name} resolved outside the add-on store.") from exc
    if not resolved.is_file():
        raise CapabilityRegistryError("addon_unreadable", f"{path.name} is not a descriptor file.")
    try:
        payload = loads(resolved.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError) as exc:
        raise CapabilityRegistryError("addon_unreadable", f"{path.name} could not be read.") from exc
    except JSONDecodeError as exc:
        raise CapabilityRegistryError("addon_descriptor_invalid", f"{path.name} is not JSON.") from exc
    return addon_from_payload(payload, seen_ids)


def addon_from_payload(payload: object, seen_ids: set[str] | None = None) -> dict[str, Any]:
    if not isinstance(payload, dict):
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on descriptor must be an object.")
    extra = set(payload) - _ADDON_KEYS
    if extra:
        raise CapabilityRegistryError(
            "addon_descriptor_invalid",
            "Add-on descriptor has unsupported keys. Add-ons cannot rewrite navigation or inject markup.",
        )
    if payload.get("schema") != ADDON_SCHEMA:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on descriptor schema is not registered.")
    if payload.get("descriptor_version") != ADDON_DESCRIPTOR_VERSION:
        raise CapabilityRegistryError("addon_descriptor_version_unsupported", "Unknown add-on descriptor version failed closed.")

    identity = payload.get("id")
    version = payload.get("version")
    producer = payload.get("producer")
    availability = payload.get("availability")
    slot = payload.get("slot")
    config_schema = payload.get("config_schema")
    read_schema = payload.get("read_schema")
    action_schema = payload.get("action_schema")
    presentation = payload.get("presentation")

    if not _addon_id(identity):
        raise CapabilityRegistryError("addon_identity_invalid", "Add-on identity is not a registered slug.")
    if seen_ids is not None and identity in seen_ids:
        raise CapabilityRegistryError("addon_identity_duplicate", f"Add-on identity {identity} is already bound.")
    if not isinstance(version, str) or not version or not _plain_text(version) or len(version) > 32:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on version is invalid.")
    if producer not in _PRODUCERS:
        raise CapabilityRegistryError(
            "addon_producer_refused",
            "Add-ons cannot claim native producer truth. Producer must be operator or platform.",
        )
    if availability not in _AVAILABILITY:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on availability is invalid.")
    if slot not in _SLOT_IDS:
        raise CapabilityRegistryError(
            "addon_slot_unregistered",
            "Add-on placement is not a registered typed slot. Top-level navigation cannot be extended.",
        )
    if config_schema not in _ALLOWED_CONTRACT_SCHEMAS or read_schema not in _ALLOWED_CONTRACT_SCHEMAS:
        raise CapabilityRegistryError("addon_contract_invalid", "Add-on config/read schemas must be the registered none contract.")
    if action_schema is not None:
        raise CapabilityRegistryError(
            "addon_action_refused",
            "Add-ons have no mutation contract. Actions stay on canonical product APIs.",
        )
    title, detail = _presentation(presentation)
    return {
        "schema": ADDON_SCHEMA,
        "descriptor_version": ADDON_DESCRIPTOR_VERSION,
        "id": identity,
        "version": version,
        "producer": producer,
        "availability": availability,
        "slot": slot,
        "config_schema": config_schema,
        "read_schema": read_schema,
        "action_schema": None,
        "presentation": {"title": title, "detail": detail},
    }


def _presentation(value: object) -> tuple[str, str]:
    if not isinstance(value, dict):
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on presentation must be a typed object.")
    if set(value) - _PRESENTATION_KEYS:
        raise CapabilityRegistryError(
            "addon_presentation_invalid",
            "Add-on presentation may only include title and detail. HTML/script are refused.",
        )
    title = value.get("title")
    detail = value.get("detail")
    if not isinstance(title, str) or not _plain_text(title) or not title.strip() or len(title) > 80:
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on title must be plain text.")
    if not isinstance(detail, str) or not _plain_text(detail) or not detail.strip() or len(detail) > 400:
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on detail must be plain text.")
    return title.strip(), detail.strip()


def _addon_id(value: object) -> bool:
    if not isinstance(value, str) or len(value) < 3 or len(value) > 80:
        return False
    if value[0] not in "abcdefghijklmnopqrstuvwxyz":
        return False
    if any(char not in _ID_CHARS for char in value):
        return False
    if ".." in value or value.startswith(".") or value.endswith("."):
        return False
    return "." in value


def _plain_text(value: str) -> bool:
    if any(ord(char) < 32 for char in value):
        return False
    lowered = value.lower()
    if "<" in value or ">" in value or "</" in value:
        return False
    if "javascript:" in lowered or "data:text/html" in lowered:
        return False
    return True


def slot_surface(slot_id: str) -> str | None:
    if slot_id not in _SLOT_IDS:
        return None
    return next(slot["surface"] for slot in REGISTERED_SLOTS if slot["id"] == slot_id)


# Imported by tests to prove nav slots do not exist.
assert "home" in PLATFORM_SURFACES
assert all(slot["surface"] in _SLOT_SURFACES for slot in REGISTERED_SLOTS)
assert not any(slot["kind"] == "navigation" for slot in REGISTERED_SLOTS)
assert set(PLATFORM_SURFACES) == {"home", "research", "explore", "automation", "operate", "settings"}
