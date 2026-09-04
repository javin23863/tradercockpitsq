"""Serve installed StrategyQuant X Results plugins from the verified runtime.

These are the operator-installed HTML apps under ``user/extend/ResultsPlugins``.
TraderCockpit does not reimplement their math. It only path-jails and serves the
plugin files; the Results pane answers GET_STATS / GET_ORDERS from producer
``orders.bin``.
"""

from __future__ import annotations

from pathlib import Path
import re
from shutil import copytree
from urllib.parse import unquote

from .sqx_custom_project import SqxCustomProjectTopologyError, _verified_home


SQX_RESULTS_PLUGIN_API_PATH = "/api/sqx-results-plugin"
RESULTS_PLUGINS_RELATIVE = Path("user") / "extend" / "ResultsPlugins"
MAX_PLUGIN_FILE_BYTES = 8_000_000
PLUGIN_TAB_FOLDERS = (
    ("prop-mc", "Prop Monte Carlo"),
    ("prop-analytics", "Prop analytics"),
)
PLUGIN_TEMPLATE_FOLDER = "CustomPlugin"
SQX_RESULTS_PLUGIN_CREATE_API_PATH = "/api/sqx-results-plugins"
_PLUGIN_NAME_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 _.-]{0,79}$")
_ALLOWED_SUFFIXES = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".png": "image/png",
    ".svg": "image/svg+xml",
    ".map": "application/json; charset=utf-8",
}


def _plugin_folder(value: str) -> str:
    if (
        not isinstance(value, str)
        or not value
        or value != value.strip()
        or "/" in value
        or "\\" in value
        or "\0" in value
        or value in {".", ".."}
        or Path(value).name != value
    ):
        raise SqxCustomProjectTopologyError(
            "results_plugin_name_invalid",
            "Results plugin folder must be one exact directory name",
        )
    return value


def _plugin_relative(value: str) -> str:
    raw = (value or "").replace("\\", "/").strip("/")
    if not raw:
        return "index.html"
    parts = Path(raw).parts
    if not parts or any(part in {"", ".", ".."} for part in parts):
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin file path must stay inside the plugin folder",
        )
    return "/".join(parts)


def results_plugins_root(home: Path) -> Path:
    return home / RESULTS_PLUGINS_RELATIVE


def _plugin_tab_id(folder: str, used: set[str]) -> str:
    for tab_id, name in PLUGIN_TAB_FOLDERS:
        if name == folder:
            used.add(tab_id)
            return tab_id
    slug = re.sub(r"[^a-z0-9]+", "-", folder.casefold()).strip("-") or "plugin"
    candidate = slug
    n = 2
    while candidate in used:
        candidate = f"{slug}-{n}"
        n += 1
    used.add(candidate)
    return candidate


def _installed_plugin_folder(root: Path, folder: str) -> bool:
    index = root / folder / "index.html"
    return index.is_file() and not index.is_symlink() and not (root / folder).is_symlink()


def results_plugin_create_state(sqx_home: Path | str | None) -> dict[str, object]:
    try:
        home = _verified_home(sqx_home)
    except SqxCustomProjectTopologyError:
        return {"available": False, "template": PLUGIN_TEMPLATE_FOLDER}
    index = results_plugins_root(home) / PLUGIN_TEMPLATE_FOLDER / "index.html"
    return {
        "available": index.is_file() and not index.is_symlink(),
        "template": PLUGIN_TEMPLATE_FOLDER,
    }


def list_results_plugin_tabs(sqx_home: Path | str | None) -> list[dict[str, object]]:
    try:
        home = _verified_home(sqx_home)
    except SqxCustomProjectTopologyError:
        return [
            {
                "id": tab_id,
                "folder": folder,
                "title": folder,
                "installed": False,
            }
            for tab_id, folder in PLUGIN_TAB_FOLDERS
        ]
    root = results_plugins_root(home)
    used: set[str] = set()
    tabs: list[dict[str, object]] = []
    known = {folder for _tab_id, folder in PLUGIN_TAB_FOLDERS}
    for tab_id, folder in PLUGIN_TAB_FOLDERS:
        used.add(tab_id)
        tabs.append(
            {
                "id": tab_id,
                "folder": folder,
                "title": folder,
                "installed": _installed_plugin_folder(root, folder),
            }
        )
    if root.is_dir():
        extras = sorted(
            (
                child.name
                for child in root.iterdir()
                if child.is_dir()
                and child.name not in known
                and child.name != PLUGIN_TEMPLATE_FOLDER
                and _installed_plugin_folder(root, child.name)
            ),
            key=str.casefold,
        )
        for folder in extras:
            tabs.append(
                {
                    "id": _plugin_tab_id(folder, used),
                    "folder": folder,
                    "title": folder,
                    "installed": True,
                }
            )
    return tabs


def create_results_plugin(sqx_home: Path | str | None, name: str) -> dict[str, object]:
    home = _verified_home(sqx_home)
    if not isinstance(name, str) or not _PLUGIN_NAME_RE.fullmatch(name.strip()):
        raise SqxCustomProjectTopologyError(
            "results_plugin_name_invalid",
            "Results plugin name must be a unique folder name.",
        )
    folder = _plugin_folder(name.strip())
    if folder.casefold() == PLUGIN_TEMPLATE_FOLDER.casefold():
        raise SqxCustomProjectTopologyError(
            "results_plugin_name_invalid",
            "CustomPlugin is the template, not a Results tab.",
        )
    root = results_plugins_root(home)
    source = root / PLUGIN_TEMPLATE_FOLDER
    dest = root / folder
    if not (source / "index.html").is_file() or source.is_symlink():
        raise SqxCustomProjectTopologyError(
            "results_plugin_template_missing",
            "Native CustomPlugin template is missing under user/extend/ResultsPlugins.",
        )
    existing = []
    if root.is_dir():
        existing = [child.name for child in root.iterdir() if child.is_dir()]
    if dest.exists() or any(item.casefold() == folder.casefold() for item in existing):
        raise SqxCustomProjectTopologyError(
            "results_plugin_exists",
            "A Results plugin with that name already exists.",
        )
    copytree(source, dest)
    created = next((tab for tab in list_results_plugin_tabs(home) if tab.get("folder") == folder), None)
    if not isinstance(created, dict):
        raise SqxCustomProjectTopologyError(
            "results_plugin_missing",
            "Results plugin folder was copied but could not be listed.",
        )
    return {
        **created,
        "detail": f"Copied {PLUGIN_TEMPLATE_FOLDER} to user/extend/ResultsPlugins/{folder}.",
    }


def resolve_results_plugin_file(
    sqx_home: Path | str | None,
    plugin_folder: str,
    relative: str,
) -> Path:
    home = _verified_home(sqx_home)
    folder = _plugin_folder(plugin_folder)
    rel = _plugin_relative(relative)
    root = results_plugins_root(home).resolve()
    try:
        root.relative_to(home.resolve())
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin root resolves outside the verified runtime",
        ) from exc
    if root.is_symlink():
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin root must not be a symlink",
        )
    target_dir = (root / folder).resolve()
    try:
        target_dir.relative_to(root)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin folder resolves outside user/extend/ResultsPlugins",
        ) from exc
    if target_dir.name != folder or target_dir.is_symlink():
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin folder resolves outside user/extend/ResultsPlugins",
        )
    path = (target_dir / Path(*rel.split("/"))).resolve()
    try:
        path.relative_to(target_dir)
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "results_plugin_path_escape",
            "Results plugin file resolves outside the plugin folder",
        ) from exc
    if path.is_symlink() or not path.is_file():
        raise SqxCustomProjectTopologyError(
            "results_plugin_missing",
            f"Results plugin file is missing: {folder}/{rel}",
        )
    suffix = path.suffix.lower()
    if suffix not in _ALLOWED_SUFFIXES:
        raise SqxCustomProjectTopologyError(
            "results_plugin_type_unsupported",
            "Results plugin file type is not served",
        )
    return path


def results_plugin_content_type(path: Path) -> str:
    return _ALLOWED_SUFFIXES[path.suffix.lower()]


def parse_results_plugin_request_path(path: str) -> tuple[str, str]:
    prefix = SQX_RESULTS_PLUGIN_API_PATH
    if path != prefix and not path.startswith(prefix + "/"):
        raise SqxCustomProjectTopologyError(
            "results_plugin_name_invalid",
            "Results plugin URL is not a plugin file path",
        )
    rest = unquote(path[len(prefix) :].lstrip("/"))
    if not rest:
        raise SqxCustomProjectTopologyError(
            "results_plugin_name_invalid",
            "Results plugin folder is required",
        )
    folder, _, relative = rest.partition("/")
    return _plugin_folder(folder), _plugin_relative(relative)


def read_results_plugin_file(sqx_home: Path | str | None, path: str) -> tuple[bytes, str]:
    folder, relative = parse_results_plugin_request_path(path)
    target = resolve_results_plugin_file(sqx_home, folder, relative)
    size = target.stat().st_size
    if size > MAX_PLUGIN_FILE_BYTES:
        raise SqxCustomProjectTopologyError(
            "results_plugin_too_large",
            "Results plugin file exceeds the desktop size limit",
        )
    return target.read_bytes(), results_plugin_content_type(target)
