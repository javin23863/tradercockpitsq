"""Packaged native StrategyQuant X plugins. Not a substitute engine.

Results plugins install into the authorized SQX runtime. Authoring skills stay
packaged for native-block work. Browser code never chooses the install path.
"""

from __future__ import annotations

from json import loads
from pathlib import Path
from zipfile import ZipFile
from typing import Any


CATALOG_SCHEMA = "tc.native-plugin-catalog.v1"
PACKAGES_DIR_NAME = "packages"
RESULTS_PLUGINS_RELATIVE = Path("user") / "extend" / "ResultsPlugins"
_KINDS = frozenset({"results_plugin", "authoring_skill", "sxp_extension"})


class NativePluginError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def native_plugins_root() -> Path:
    return Path(__file__).resolve().parent


def packages_dir() -> Path:
    return native_plugins_root() / PACKAGES_DIR_NAME


def load_native_plugin_catalog() -> list[dict[str, Any]]:
    path = native_plugins_root() / "catalog.json"
    payload = loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict) or payload.get("schema") != CATALOG_SCHEMA:
        raise NativePluginError("native_plugin_catalog_invalid", "Native plugin catalog schema is not registered.")
    plugins = payload.get("plugins")
    if not isinstance(plugins, list) or not plugins:
        raise NativePluginError("native_plugin_catalog_invalid", "Native plugin catalog has no plugins.")
    seen: set[str] = set()
    catalog: list[dict[str, Any]] = []
    for item in plugins:
        catalog.append(_plugin_entry(item, seen))
    return catalog


def plugin_package_path(entry: dict[str, Any]) -> Path | None:
    package = entry.get("package")
    if not isinstance(package, str) or not package:
        return None
    path = (packages_dir() / package).resolve()
    try:
        path.relative_to(packages_dir().resolve())
    except ValueError as exc:
        raise NativePluginError("native_plugin_path_escape", "Plugin package resolved outside the package store.") from exc
    return path if path.is_file() else None


def plugin_runtime_state(entry: dict[str, Any], sqx_home: Path | str | None) -> dict[str, Any]:
    package = plugin_package_path(entry)
    packaged = package is not None
    stageable = entry["kind"] in {"results_plugin", "sxp_extension"} and packaged and bool(entry.get("native_placement"))
    if not packaged:
        return {
            "status": "unavailable",
            "installed": False,
            "stageable": False,
            "detail": "The packaged plugin file is missing from the desktop.",
        }
    if entry["kind"] == "authoring_skill":
        return {
            "status": "packaged",
            "installed": False,
            "stageable": False,
            "detail": "Packaged with TraderCockpit for native authoring. It does not install as a Results tab.",
        }
    if sqx_home is None:
        return {
            "status": "runtime_not_configured",
            "installed": False,
            "stageable": stageable,
            "detail": "Install into StrategyQuant X after the authorized runtime is verified. Plugin settings stay in SQX Results.",
        }
    installed = _plugin_installed(entry, Path(sqx_home))
    if installed:
        return {
            "status": "installed",
            "installed": True,
            "stageable": False,
            "detail": f"Installed at {entry['native_placement']}. Adjust settings in StrategyQuant X Results.",
        }
    return {
        "status": "packaged",
        "installed": False,
        "stageable": stageable,
        "detail": "Packaged. Install into the authorized StrategyQuant X runtime to use it on Results.",
    }


def stage_native_plugin(plugin_id: str, sqx_home: Path | str) -> dict[str, Any]:
    from tradercockpit.sqx_presets import SqxPresetRuntimeError, verified_sqx_home

    try:
        home = verified_sqx_home(sqx_home)
    except SqxPresetRuntimeError as exc:
        raise NativePluginError(exc.code, str(exc)) from exc
    entry = next((item for item in load_native_plugin_catalog() if item["id"] == plugin_id), None)
    if entry is None:
        raise NativePluginError("native_plugin_unknown", "That plugin is not in the packaged catalog.")
    if entry["kind"] == "authoring_skill":
        raise NativePluginError("native_plugin_not_stageable", "Authoring skills are not Results plugins.")
    package = plugin_package_path(entry)
    if package is None:
        raise NativePluginError("native_plugin_package_missing", "The packaged plugin file is missing.")
    dest = _placement_dir(home, entry["native_placement"])
    _extract_plugin(package, dest, entry.get("archive_root"))
    return {
        "id": plugin_id,
        "installed": True,
        "native_placement": entry["native_placement"],
        "detail": f"Installed at {entry['native_placement']}. Restart StrategyQuant X or reload Results.",
    }


def _plugin_entry(item: object, seen: set[str]) -> dict[str, Any]:
    if not isinstance(item, dict):
        raise NativePluginError("native_plugin_catalog_invalid", "Plugin catalog entry must be an object.")
    identity = item.get("id")
    kind = item.get("kind")
    if not isinstance(identity, str) or identity in seen:
        raise NativePluginError("native_plugin_catalog_invalid", "Plugin identity is missing or duplicated.")
    if kind not in _KINDS:
        raise NativePluginError("native_plugin_catalog_invalid", "Plugin kind is not registered.")
    seen.add(identity)
    return item


def _plugin_installed(entry: dict[str, Any], home: Path) -> bool:
    placement = entry.get("native_placement")
    if not isinstance(placement, str) or not placement:
        return False
    try:
        dest = _placement_dir(home, placement)
    except NativePluginError:
        return False
    marker = dest / "index.html"
    if marker.is_file():
        return True
    return dest.is_dir() and any(dest.iterdir())


def _placement_dir(home: Path, relative: str) -> Path:
    if Path(relative).is_absolute() or ".." in Path(relative).parts:
        raise NativePluginError("native_plugin_path_escape", "Plugin placement must stay inside the authorized runtime.")
    dest = (home / relative).resolve()
    try:
        dest.relative_to(home.resolve())
    except ValueError as exc:
        raise NativePluginError("native_plugin_path_escape", "Plugin placement resolved outside the authorized runtime.") from exc
    if dest.exists() and dest.is_symlink():
        raise NativePluginError("native_plugin_path_escape", "Plugin placement must not be a symlink.")
    return dest


def _extract_plugin(package: Path, dest: Path, archive_root: str | None) -> None:
    dest.mkdir(parents=True, exist_ok=True)
    with ZipFile(package) as archive:
        for info in archive.infolist():
            name = info.filename.replace("\\", "/")
            if not name or name.endswith("/"):
                continue
            relative = _member_relative(name, archive_root)
            if relative is None:
                continue
            target = (dest / relative).resolve()
            try:
                target.relative_to(dest.resolve())
            except ValueError as exc:
                raise NativePluginError("native_plugin_path_escape", "Archive member escaped the plugin folder.") from exc
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(info.filename))


def _member_relative(name: str, archive_root: str | None) -> str | None:
    parts = [part for part in name.split("/") if part]
    if any(part == ".." for part in parts):
        return None
    if not archive_root:
        return "/".join(parts)
    root_parts = [part for part in archive_root.replace("\\", "/").split("/") if part]
    if parts[: len(root_parts)] != root_parts:
        return None
    rest = parts[len(root_parts) :]
    return "/".join(rest) if rest else None
