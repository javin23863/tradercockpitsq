#!/usr/bin/env python3
"""Install the packaged TraderCockpit desktop as its own Windows product identity.

This copies ``TraderCockpit.exe`` into a per-user Programs directory that is not
the SQX install, not the Git checkout, and not the TraderCockpit application-data
root. It creates a Start Menu shortcut that targets that executable.

It does not retarget unrelated launchers that happen to share the TraderCockpit
name, and it never installs StrategyQuantX.exe as the product entrypoint.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys


_EXECUTABLE_NAME = "TraderCockpit.exe"
_START_MENU_FOLDER = "TraderCockpitSQ"
_FORBIDDEN_ENTRYPOINT_NAMES = frozenset(
    {
        "strategyquantx.exe",
        "sqcli.exe",
        "strategyquantx_ui.exe",
        "launch-tradercockpit.cmd",
    }
)


def default_windows_install_dir() -> Path:
    base = os.environ.get("LOCALAPPDATA")
    if not base:
        raise RuntimeError("LOCALAPPDATA is required to install TraderCockpit")
    return Path(base) / "Programs" / "TraderCockpitSQ"


def default_start_menu_shortcut() -> Path:
    roaming = os.environ.get("APPDATA")
    if not roaming:
        raise RuntimeError("APPDATA is required to install the TraderCockpit Start Menu shortcut")
    return (
        Path(roaming)
        / "Microsoft"
        / "Windows"
        / "Start Menu"
        / "Programs"
        / _START_MENU_FOLDER
        / "TraderCockpit.lnk"
    )


def _create_shortcut(link_path: Path, target: Path, workdir: Path) -> None:
    link_path.parent.mkdir(parents=True, exist_ok=True)
    script = (
        "$s = (New-Object -ComObject WScript.Shell).CreateShortcut({link}); "
        "$s.TargetPath = {target}; "
        "$s.WorkingDirectory = {workdir}; "
        "$s.WindowStyle = 1; "
        "$s.Save()"
    ).format(
        link=json.dumps(str(link_path)),
        target=json.dumps(str(target)),
        workdir=json.dumps(str(workdir)),
    )
    completed = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout or "shortcut creation failed").strip()
        raise RuntimeError(detail)


def install_windows_desktop(
    executable: Path,
    *,
    install_dir: Path | None = None,
    shortcut_path: Path | None = None,
) -> Path:
    source = Path(executable).expanduser().resolve()
    if source.name.casefold() in _FORBIDDEN_ENTRYPOINT_NAMES:
        raise ValueError("refusing to install a native SQX or unrelated launcher as TraderCockpit")
    if source.name != _EXECUTABLE_NAME:
        raise ValueError(f"install accepts only {_EXECUTABLE_NAME}")
    if sys.platform != "win32":
        raise RuntimeError("TraderCockpit Windows install must run on Windows")
    if not source.is_file():
        raise FileNotFoundError(f"missing packaged executable: {source}")

    destination_dir = Path(install_dir).expanduser().resolve() if install_dir else default_windows_install_dir()
    destination_dir.mkdir(parents=True, exist_ok=True)
    destination = destination_dir / _EXECUTABLE_NAME
    if destination.resolve() != source:
        shutil.copy2(source, destination)

    shortcut = Path(shortcut_path).expanduser().resolve() if shortcut_path else default_start_menu_shortcut()
    _create_shortcut(shortcut, destination, destination_dir)
    return destination


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Install packaged TraderCockpit for Windows")
    parser.add_argument(
        "--exe",
        type=Path,
        default=Path("dist/windows") / _EXECUTABLE_NAME,
        help="Packaged TraderCockpit.exe produced by tools/build_windows_desktop.py",
    )
    parser.add_argument("--install-dir", type=Path, default=None)
    parser.add_argument("--shortcut", type=Path, default=None)
    args = parser.parse_args(argv)
    installed = install_windows_desktop(
        args.exe,
        install_dir=args.install_dir,
        shortcut_path=args.shortcut,
    )
    print(installed)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
