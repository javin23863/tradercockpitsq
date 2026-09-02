"""Canonical capability/add-on registry for TraderCockpit.

One backend authority supplies typed descriptors for platform-owned capabilities and
optional operator-registered add-on slots. Add-ons may not inject script/HTML, rewrite
navigation, or maintain a competing capability catalog.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tradercockpit.assistant import ASSISTANT_STATUS_SCHEMA
from tradercockpit.market_data import MARKET_QUOTES_SCHEMA
from tradercockpit.operate_promotions import PROMOTION_READ_SCHEMA
from tradercockpit.research_custody import RESEARCH_CURRENT_SCHEMA
from tradercockpit.sqx_custom_project_control import SQX_CUSTOM_PROJECT_CONTROL_SCHEMA


CAPABILITY_REGISTRY_SCHEMA = "tc.capability-registry.v1"
EXTENSIONS_MANIFEST_SCHEMA = "tc.extensions-manifest.v1"
RUNTIME_STATUS_READ_SCHEMA = "tc.runtime-status.v1"
DESCRIPTOR_VERSION = "1"
EXTENSIONS_MANIFEST_NAME = "extensions.json"

FORBIDDEN_REGISTRATION_KEYS = frozenset(
    {
        "script",
        "html",
        "javascript",
        "js",
        "nav",
        "navigation",
        "top_level_nav",
        "frontend_inject",
        "inject",
        "rewrite_navigation",
        "rewrite_research",
        "frontend_html",
        "frontend_js",
    }
)
FORBIDDEN_SLOT_TYPES = frozenset(
    {
        "script_inject",
        "html_inject",
        "nav_rewrite",
        "research_rewrite",
        "frontend_script",
        "frontend_html",
    }
)
ADDON_ALLOWED_KEYS = frozenset({"id", "descriptor_version", "owner", "placement", "read_schema", "action_schema", "config_schema"})
MANIFEST_ALLOWED_KEYS = frozenset({"schema", "addons"})


class ExtensionsError(ValueError):
    def __init__(self, code: str, detail: str, *, status: int = 400) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail
        self.status = status


def _capability(
    capability_id: str,
    *,
    owner: str,
    placement: list[str],
    read_schema: str,
    action_schema: str | None = None,
    config_schema: str | None = None,
) -> dict[str, object]:
    record: dict[str, object] = {
        "id": capability_id,
        "kind": "capability",
        "descriptor_version": DESCRIPTOR_VERSION,
        "owner": owner,
        "availability": "ready",
        "placement": placement,
        "read_schema": read_schema,
    }
    if action_schema is not None:
        record["action_schema"] = action_schema
    if config_schema is not None:
        record["config_schema"] = config_schema
    return record


BUILTIN_CAPABILITIES: tuple[dict[str, object], ...] = (
    _capability(
        "runtime-status",
        owner="platform",
        placement=["home", "settings"],
        read_schema=RUNTIME_STATUS_READ_SCHEMA,
    ),
    _capability(
        "research-custody",
        owner="platform",
        placement=["research", "settings"],
        read_schema=RESEARCH_CURRENT_SCHEMA,
        config_schema="tc.research-custody.v1",
    ),
    _capability(
        "market-quotes",
        owner="platform",
        placement=["home", "explore", "settings"],
        read_schema=MARKET_QUOTES_SCHEMA,
    ),
    _capability(
        "operate-promotions",
        owner="platform",
        placement=["operate"],
        read_schema=PROMOTION_READ_SCHEMA,
        action_schema="tc.operate-promotion-content.v1",
    ),
    _capability(
        "sqx-custom-project-control",
        owner="platform",
        placement=["automation"],
        read_schema=SQX_CUSTOM_PROJECT_CONTROL_SCHEMA,
        action_schema=SQX_CUSTOM_PROJECT_CONTROL_SCHEMA,
    ),
    _capability(
        "assistant",
        owner="platform",
        placement=["home", "research", "settings"],
        read_schema=ASSISTANT_STATUS_SCHEMA,
        action_schema="tc.assistant-reply.v1",
    ),
)


def extensions_manifest_path(data_root: Path | str) -> Path:
    return Path(data_root) / EXTENSIONS_MANIFEST_NAME


def _contains_forbidden_registration(payload: object) -> str | None:
    if isinstance(payload, dict):
        for key, value in payload.items():
            normalized = str(key).lower().replace("-", "_")
            if normalized in FORBIDDEN_REGISTRATION_KEYS:
                return normalized
            if normalized == "slot_type" and isinstance(value, str):
                slot = value.lower().replace("-", "_")
                if slot in FORBIDDEN_SLOT_TYPES:
                    return value
            found = _contains_forbidden_registration(value)
            if found:
                return found
    elif isinstance(payload, list):
        for item in payload:
            found = _contains_forbidden_registration(item)
            if found:
                return found
    return None


def _require_string(record: dict[str, object], key: str, code: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ExtensionsError(code, f"{key} must be a non-empty string")
    return value.strip()


def _addon_manifest_entry(record: dict[str, object]) -> dict[str, object]:
    entry: dict[str, object] = {
        "id": record["id"],
        "descriptor_version": record["descriptor_version"],
        "owner": record["owner"],
        "placement": list(record["placement"]),
        "read_schema": record["read_schema"],
    }
    for optional in ("action_schema", "config_schema"):
        if optional in record:
            entry[optional] = record[optional]
    return entry


def _validate_addon_slot(record: object, *, code: str) -> dict[str, object]:
    if not isinstance(record, dict):
        raise ExtensionsError(code, "add-on slot must be a JSON object")
    unknown = set(record) - ADDON_ALLOWED_KEYS
    if unknown:
        raise ExtensionsError(
            "extension_descriptor_unknown",
            f"unsupported add-on keys: {', '.join(sorted(unknown))}",
            status=409,
        )
    descriptor_version = _require_string(record, "descriptor_version", code)
    if descriptor_version != DESCRIPTOR_VERSION:
        raise ExtensionsError(
            code,
            f"unsupported descriptor_version {descriptor_version!r}",
            status=409,
        )
    addon_id = _require_string(record, "id", code)
    owner = _require_string(record, "owner", code)
    read_schema = _require_string(record, "read_schema", code)
    placement = record.get("placement")
    if not isinstance(placement, list) or not placement or not all(isinstance(item, str) and item for item in placement):
        raise ExtensionsError(code, "placement must be a non-empty string array")
    validated: dict[str, object] = {
        "id": addon_id,
        "kind": "addon",
        "descriptor_version": descriptor_version,
        "owner": owner,
        "availability": "registered",
        "placement": list(placement),
        "read_schema": read_schema,
    }
    for optional in ("action_schema", "config_schema"):
        if optional in record:
            validated[optional] = _require_string(record, optional, code)
    return validated


def _load_manifest_addons(data_root: Path | str | None) -> tuple[list[dict[str, object]], str | None, str | None]:
    if data_root is None:
        return [], None, None
    path = extensions_manifest_path(data_root)
    if not path.is_file():
        return [], None, None
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError):
        return [], "manifest_invalid", "extensions.json is not valid JSON."
    if not isinstance(payload, dict):
        return [], "manifest_invalid", "extensions.json must be a JSON object."
    unknown = set(payload) - MANIFEST_ALLOWED_KEYS
    if unknown:
        return [], "manifest_invalid", f"unsupported manifest keys: {', '.join(sorted(unknown))}"
    schema = payload.get("schema")
    if schema != EXTENSIONS_MANIFEST_SCHEMA:
        return [], "manifest_invalid", f"unsupported manifest schema {schema!r}"
    addons_raw = payload.get("addons")
    if addons_raw is None:
        return [], None, None
    if not isinstance(addons_raw, list):
        return [], "manifest_invalid", "addons must be an array"
    addons: list[dict[str, object]] = []
    for item in addons_raw:
        try:
            addons.append(_validate_addon_slot(item, code="manifest_invalid"))
        except ExtensionsError as exc:
            return [], exc.code, exc.detail
    return addons, None, None


def capability_registry_record(
    data_root: Path | str | None = None,
) -> dict[str, object]:
    capabilities = [dict(item) for item in BUILTIN_CAPABILITIES]
    addons, reason_code, detail = _load_manifest_addons(data_root)
    return {
        "schema": CAPABILITY_REGISTRY_SCHEMA,
        "capabilities": capabilities,
        "addons": addons,
        "manifest_reason_code": reason_code,
        "manifest_detail": detail,
    }


def extensions_status_record(data_root: Path | str | None = None) -> dict[str, object]:
    registry = capability_registry_record(data_root)
    reason_code = registry.pop("manifest_reason_code")
    detail = registry.pop("manifest_detail")
    if reason_code is not None:
        return {
            "status": "unavailable",
            "reason_code": reason_code,
            "detail": detail or "Capability/add-on manifest failed closed.",
            "registry": registry,
        }
    addon_count = len(registry["addons"])
    capability_count = len(registry["capabilities"])
    return {
        "status": "ready",
        "reason_code": None,
        "detail": (
            f"Platform capability registry is authoritative ({capability_count} built-in capabilities; "
            f"{addon_count} registered add-on{'s' if addon_count != 1 else ''})."
        ),
        "registry": registry,
    }


def register_addon_slot(data_root: Path | str, payload: dict[str, object]) -> dict[str, object]:
    forbidden = _contains_forbidden_registration(payload)
    if forbidden is not None:
        raise ExtensionsError(
            "extension_registration_forbidden",
            f"registration refuses forbidden slot key or type: {forbidden}",
            status=400,
        )
    if set(payload) != {"action", "addon"} or payload.get("action") != "register":
        raise ExtensionsError(
            "extension_registration_invalid",
            "registration accepts only action=register and a typed addon object",
        )
    addon = payload.get("addon")
    validated = _validate_addon_slot(addon, code="extension_registration_invalid")
    path = extensions_manifest_path(data_root)
    existing, reason_code, _ = _load_manifest_addons(data_root)
    if reason_code is not None:
        raise ExtensionsError(reason_code, "existing extensions.json is invalid", status=409)
    if any(item["id"] == validated["id"] for item in existing):
        raise ExtensionsError(
            "extension_already_registered",
            f"add-on id {validated['id']!r} is already registered",
            status=409,
        )
    merged = existing + [validated]
    manifest = {
        "schema": EXTENSIONS_MANIFEST_SCHEMA,
        "addons": [_addon_manifest_entry(item) for item in merged],
    }
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return {
        "schema": CAPABILITY_REGISTRY_SCHEMA,
        "registered": validated,
        "addons": merged,
    }


def write_extensions_manifest(data_root: Path | str, addons: list[dict[str, object]]) -> None:
    """Test/helper hook to persist a validated manifest."""

    validated = [_validate_addon_slot(item, code="manifest_invalid") for item in addons]
    path = extensions_manifest_path(data_root)
    manifest = {
        "schema": EXTENSIONS_MANIFEST_SCHEMA,
        "addons": [_addon_manifest_entry(item) for item in validated],
    }
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
