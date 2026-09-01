"""Canonical local application-data root for TraderCockpit.

Browser code never selects this path. The desktop/server may override it through
trusted process configuration for development or testing; otherwise the path follows
platform user-data conventions.
"""

from __future__ import annotations

import os
from pathlib import Path
import sys


TRADERCOCKPIT_DATA_ROOT_ENV = "TRADERCOCKPIT_DATA_ROOT"
# ponytail: keep this product out of tradercockpit-app's %LOCALAPPDATA%\TraderCockpit
_APPLICATION_DATA_DIR_NAME = "TraderCockpitSQ"


def default_application_data_root() -> Path:
    """Return the platform-specific per-user TraderCockpit data directory."""

    if sys.platform == "win32":
        base = os.environ.get("LOCALAPPDATA") or os.environ.get("APPDATA")
        if base:
            return (Path(base).expanduser() / _APPLICATION_DATA_DIR_NAME).resolve()
        return (Path.home() / "AppData" / "Local" / _APPLICATION_DATA_DIR_NAME).resolve()

    if sys.platform == "darwin":
        return (Path.home() / "Library" / "Application Support" / _APPLICATION_DATA_DIR_NAME).resolve()

    xdg_data_home = os.environ.get("XDG_DATA_HOME")
    base = Path(xdg_data_home).expanduser() if xdg_data_home else Path.home() / ".local" / "share"
    return (base / _APPLICATION_DATA_DIR_NAME).resolve()


def resolve_application_data_root(value: Path | str | None = None) -> Path:
    """Resolve a trusted process-side override or the canonical per-user default."""

    selected: Path | str | None = value
    if selected is None:
        selected = os.environ.get(TRADERCOCKPIT_DATA_ROOT_ENV)
    if selected is None:
        return default_application_data_root()
    if not isinstance(selected, (str, Path)) or not str(selected).strip():
        raise ValueError("application data root must be a non-empty filesystem path")
    return Path(selected).expanduser().resolve()
