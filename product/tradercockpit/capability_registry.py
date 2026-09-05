"""Backend capability/add-on registry.

One authority supplies typed descriptors. Add-ons bind registered slots only.
They cannot inject HTML/script, rewrite top-level navigation, or claim producer truth.
Unknown descriptor versions fail closed.
"""

from __future__ import annotations

from json import JSONDecodeError, loads
from pathlib import Path
from typing import Any

from tradercockpit.native_plugins import (
    NativePluginError,
    load_native_plugin_catalog,
    plugin_runtime_state,
    stage_native_plugin,
)


REGISTRY_SCHEMA = "tc.capability-addon-registry.v1"
ADDON_SCHEMA = "tc.capability-addon.v1"
ADDON_DESCRIPTOR_VERSION = 1
REGISTRY_API_PATH = "/api/capabilities"
ADDONS_DIR_NAME = "addons"
NONE_SCHEMA = "tc.capability-addon.none.v1"
NAV_AUTHORITY = "platform"

# Must match the browser's APP_SURFACES exactly: the registry payload is the nav
# authority the frontend validates against. Retester and Optimizer are native SQX
# module identities that redirect to Builder, not top-level surfaces.
PLATFORM_SURFACES = (
    "home", "builder", "data-manager", "custom-projects", "apollo", "operate", "settings",
)

REGISTERED_SLOTS = (
    {
        "id": "explore.extensions",
        "surface": "settings",
        "kind": "status_card",
        "label": "Settings plugin catalog",
    },
    {
        "id": "automation.extensions",
        "surface": "custom-projects",
        "kind": "status_card",
        "label": "Custom projects extensions",
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
        "kind",
        "package",
        "native_placement",
        "source_url",
        "runtime",
        "config_schema",
        "read_schema",
        "action_schema",
        "presentation",
    }
)
_PRESENTATION_KEYS = frozenset({"title", "detail", "job", "opens_in", "controls"})
_CONTROL_KEYS = frozenset({"label", "detail"})
_PRODUCERS = frozenset({"operator", "platform", "native_sqx"})
_BUNDLED_PRODUCERS = frozenset({"native_sqx", "platform"})
_AVAILABILITY = frozenset({"ready", "unavailable"})
_KINDS = frozenset({"results_plugin", "authoring_skill", "sxp_extension", "operator"})
_ID_CHARS = frozenset("abcdefghijklmnopqrstuvwxyz0123456789.-")
_ALLOWED_CONTRACT_SCHEMAS = frozenset({NONE_SCHEMA})


class CapabilityRegistryError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def registered_slots() -> list[dict[str, str]]:
    return [dict(slot) for slot in REGISTERED_SLOTS]


def _optional_home(sqx_home: Path | str | None) -> Path | None:
    if sqx_home is None:
        return None
    text = str(sqx_home).strip()
    return Path(text) if text else None


def capability_registry_record(
    data_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
) -> dict[str, Any]:
    """Return the typed add-on registry, including packaged native SQX plugins."""

    addons: list[dict[str, Any]] = []
    refused: list[dict[str, str]] = []
    seen: set[str] = set()
    sqx_home = _optional_home(sqx_home)
    try:
        for entry in load_native_plugin_catalog():
            addons.append(_bundled_addon(entry, sqx_home, seen))
    except Exception as exc:  # noqa: BLE001 - catalog must fail closed, not crash the desktop
        refused.append({"source": "native_plugins/catalog.json", "reason_code": "native_plugin_catalog_invalid", "detail": str(exc)})

    try:
        directory = _addons_dir(data_root)
    except CapabilityRegistryError as exc:
        return _record(addons, refused + [{"source": ADDONS_DIR_NAME, "reason_code": exc.code, "detail": exc.detail}], status="unavailable")

    if directory is None:
        return _record(addons, refused)

    for path in sorted(directory.iterdir(), key=lambda item: item.name):
        if path.name.startswith(".") or path.suffix.lower() != ".json":
            continue
        try:
            addons.append(_load_addon_file(directory, path, seen))
            seen.add(addons[-1]["id"])
        except CapabilityRegistryError as exc:
            refused.append({"source": path.name, "reason_code": exc.code, "detail": exc.detail})
    return _record(addons, refused)


def extensions_status_record(
    data_root: Path | str | None = None,
    sqx_home: Path | str | None = None,
) -> dict[str, Any]:
    """Compact /api/status view of the same registry authority."""

    registry = capability_registry_record(data_root, sqx_home)
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


def stage_addon(plugin_id: str, sqx_home: Path | str | None) -> dict[str, Any]:
    home = _optional_home(sqx_home)
    if home is None:
        raise CapabilityRegistryError(
            "runtime_not_configured",
            "Install requires a verified StrategyQuant X 144.2953 runtime. The browser cannot choose this path.",
        )
    try:
        return stage_native_plugin(plugin_id, home)
    except NativePluginError as exc:
        raise CapabilityRegistryError(exc.code, exc.detail) from exc


def _record(addons: list[dict[str, Any]], refused: list[dict[str, str]], *, status: str = "ready") -> dict[str, Any]:
    reason = None if status == "ready" else (refused[0]["reason_code"] if refused else "addon_store_unreadable")
    detail = (
        "Native StrategyQuant X plugins are packaged. Results plugins install into SQX; "
        "their settings stay in StrategyQuant X. Add-ons cannot rewrite top-level navigation "
        "or inject script/HTML."
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
    if candidate.is_symlink() or candidate.is_junction():
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
    if path.is_symlink() or path.is_junction():
        raise CapabilityRegistryError("addon_path_escape", f"{path.name} is a symlink or junction and was refused.")
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
    return addon_from_payload(payload, seen_ids, allow_native=False)


def _bundled_addon(entry: dict[str, Any], sqx_home: Path | str | None, seen: set[str]) -> dict[str, Any]:
    runtime = plugin_runtime_state(entry, sqx_home)
    payload = {
        "schema": ADDON_SCHEMA,
        "descriptor_version": ADDON_DESCRIPTOR_VERSION,
        "id": entry["id"],
        "version": entry["version"],
        "producer": "native_sqx",
        "availability": "ready" if runtime["status"] != "unavailable" else "unavailable",
        "slot": entry["slot"],
        "kind": entry["kind"],
        "package": entry.get("package"),
        "native_placement": entry.get("native_placement"),
        "source_url": entry.get("source_url"),
        "runtime": runtime,
        "config_schema": NONE_SCHEMA,
        "read_schema": NONE_SCHEMA,
        "action_schema": None,
        "presentation": entry["presentation"],
    }
    addon = addon_from_payload(payload, seen, allow_native=True)
    seen.add(addon["id"])
    return addon


def addon_from_payload(payload: object, seen_ids: set[str] | None = None, *, allow_native: bool = False) -> dict[str, Any]:
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
    kind = payload.get("kind", "operator")
    package = payload.get("package")
    native_placement = payload.get("native_placement")
    source_url = payload.get("source_url")
    runtime = payload.get("runtime")
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
    if producer == "native_sqx" and not allow_native:
        raise CapabilityRegistryError(
            "addon_producer_refused",
            "Operator add-ons cannot claim native producer truth. Packaged SQX plugins come from the desktop catalog.",
        )
    if producer not in _PRODUCERS:
        raise CapabilityRegistryError(
            "addon_producer_refused",
            "Add-on producer must be operator, platform, or a packaged native SQX plugin.",
        )
    if availability not in _AVAILABILITY:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on availability is invalid.")
    if slot not in _SLOT_IDS:
        raise CapabilityRegistryError(
            "addon_slot_unregistered",
            "Add-on placement is not a registered typed slot. Top-level navigation cannot be extended.",
        )
    if kind not in _KINDS:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on kind is not registered.")
    if package is not None and (not isinstance(package, str) or not _plain_text(package) or "/" in package or "\\" in package or len(package) > 80):
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on package name is invalid.")
    if native_placement is not None and (
        not isinstance(native_placement, str)
        or not _plain_text(native_placement)
        or native_placement.startswith("/")
        or ".." in Path(native_placement).parts
        or len(native_placement) > 160
    ):
        raise CapabilityRegistryError("addon_descriptor_invalid", "Native placement must be a relative runtime path.")
    if source_url is not None and (
        not isinstance(source_url, str)
        or not source_url.startswith("https://strategyquant.com/")
        or not _plain_text(source_url)
        or len(source_url) > 240
    ):
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on source URL is not a StrategyQuant codebase URL.")
    if runtime is None:
        runtime = {
            "status": "packaged",
            "installed": False,
            "stageable": False,
            "detail": "Operator add-on. No native Results install.",
        }
    runtime = _runtime(runtime)
    if config_schema not in _ALLOWED_CONTRACT_SCHEMAS or read_schema not in _ALLOWED_CONTRACT_SCHEMAS:
        raise CapabilityRegistryError("addon_contract_invalid", "Add-on config/read schemas must be the registered none contract.")
    if action_schema is not None:
        raise CapabilityRegistryError(
            "addon_action_refused",
            "Add-ons have no mutation contract. Install uses the canonical capability API. Plugin settings stay in StrategyQuant X.",
        )
    return {
        "schema": ADDON_SCHEMA,
        "descriptor_version": ADDON_DESCRIPTOR_VERSION,
        "id": identity,
        "version": version,
        "producer": producer,
        "availability": availability,
        "slot": slot,
        "kind": kind,
        "package": package,
        "native_placement": native_placement,
        "source_url": source_url,
        "runtime": runtime,
        "config_schema": config_schema,
        "read_schema": read_schema,
        "action_schema": None,
        "presentation": _presentation(presentation),
    }


def _runtime(value: object) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on runtime state must be an object.")
    status = value.get("status")
    if status not in {"packaged", "installed", "runtime_not_configured", "unavailable"}:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on runtime status is invalid.")
    detail = value.get("detail")
    if not isinstance(detail, str) or not _plain_text(detail) or len(detail) > 240:
        raise CapabilityRegistryError("addon_descriptor_invalid", "Add-on runtime detail must be plain text.")
    installed = value.get("installed") is True
    stageable = value.get("stageable") is True
    return {"status": status, "installed": installed, "stageable": stageable, "detail": detail}


def _presentation(value: object) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on presentation must be a typed object.")
    if set(value) - _PRESENTATION_KEYS:
        raise CapabilityRegistryError(
            "addon_presentation_invalid",
            "Add-on presentation may only include title, detail, job, opens_in, and controls. HTML/script are refused.",
        )
    title = value.get("title")
    detail = value.get("detail")
    job = value.get("job", "")
    opens_in = value.get("opens_in", "")
    if not isinstance(title, str) or not _plain_text(title) or not title.strip() or len(title) > 80:
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on title must be plain text.")
    if not isinstance(detail, str) or not _plain_text(detail) or not detail.strip() or len(detail) > 400:
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on detail must be plain text.")
    if job and (not isinstance(job, str) or not _plain_text(job) or len(job) > 160):
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on job must be plain text.")
    if opens_in and (not isinstance(opens_in, str) or not _plain_text(opens_in) or len(opens_in) > 80):
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on opens_in must be plain text.")
    controls = _controls(value.get("controls", []))
    return {
        "title": title.strip(),
        "detail": detail.strip(),
        "job": job.strip() if isinstance(job, str) else "",
        "opens_in": opens_in.strip() if isinstance(opens_in, str) else "",
        "controls": controls,
    }


def _controls(value: object) -> list[dict[str, str]]:
    if value in (None, []):
        return []
    if not isinstance(value, list) or len(value) > 6:
        raise CapabilityRegistryError("addon_presentation_invalid", "Add-on controls must be a short typed list.")
    controls: list[dict[str, str]] = []
    for item in value:
        if not isinstance(item, dict) or set(item) - _CONTROL_KEYS:
            raise CapabilityRegistryError("addon_presentation_invalid", "Each control may only include label and detail.")
        label = item.get("label")
        detail = item.get("detail")
        if not isinstance(label, str) or not _plain_text(label) or not label.strip() or len(label) > 40:
            raise CapabilityRegistryError("addon_presentation_invalid", "Control label must be plain text.")
        if not isinstance(detail, str) or not _plain_text(detail) or not detail.strip() or len(detail) > 160:
            raise CapabilityRegistryError("addon_presentation_invalid", "Control detail must be plain text.")
        controls.append({"label": label.strip(), "detail": detail.strip()})
    return controls



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


assert "home" in PLATFORM_SURFACES
assert all(slot["surface"] in _SLOT_SURFACES for slot in REGISTERED_SLOTS)
assert not any(slot["kind"] == "navigation" for slot in REGISTERED_SLOTS)
assert set(PLATFORM_SURFACES) == {
    "home",
    "builder",
    "data-manager",
    "custom-projects",
    "apollo",
    "operate",
    "settings",
}
