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
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tomllib


_EXECUTABLE_NAME = "TraderCockpit.exe"
_START_MENU_FOLDER = "TraderCockpitSQ"
_START_MENU_SHORTCUT_NAME = "TraderCockpitSQ.lnk"
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
        / _START_MENU_SHORTCUT_NAME
    )


def _other_product_data_root() -> Path | None:
    base = os.environ.get("LOCALAPPDATA") or os.environ.get("APPDATA")
    if not base:
        return None
    return (Path(base) / "TraderCockpit").resolve()


def _is_forbidden_identity_path(path: Path) -> bool:
    resolved = Path(path).expanduser().resolve()
    name = resolved.name.casefold()
    if name in _FORBIDDEN_ENTRYPOINT_NAMES or name == "tradercockpit.lnk":
        return True
    other = _other_product_data_root()
    if other is None:
        return False
    try:
        resolved.relative_to(other)
    except ValueError:
        return False
    return True


def _same_file(left: Path, right: Path) -> bool:
    try:
        return left.exists() and right.exists() and left.samefile(right)
    except OSError:
        return False


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


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _write_install_manifest(install_dir: Path, *, version: str, executable_sha256: str) -> None:
    # ponytail: inline so the zip/NSIS copy of this script does not need product/
    payload = {
        "schema": "tc.windows-install.v1",
        "version": version,
        "install_root": str(install_dir),
        "executable": _EXECUTABLE_NAME,
        "executable_sha256": executable_sha256,
    }
    (install_dir / "install-manifest.json").write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def install_windows_desktop(
    executable: Path,
    *,
    install_dir: Path | None = None,
    shortcut_path: Path | None = None,
    version: str | None = None,
    expected_sha256: str | None = None,
) -> Path:
    source = Path(executable).expanduser().resolve()
    if source.name.casefold() in _FORBIDDEN_ENTRYPOINT_NAMES:
        raise ValueError("refusing to install a native SQX or unrelated launcher as TraderCockpit")
    if source.name != _EXECUTABLE_NAME:
        raise ValueError(f"install accepts only {_EXECUTABLE_NAME}")

    destination_dir = Path(install_dir).expanduser().resolve() if install_dir else default_windows_install_dir()
    if _is_forbidden_identity_path(destination_dir) or destination_dir.name.casefold() == "tradercockpit":
        raise ValueError("refusing to install into the other TraderCockpit product identity")
    destination = destination_dir / _EXECUTABLE_NAME
    shortcut = Path(shortcut_path).expanduser().resolve() if shortcut_path else default_start_menu_shortcut()
    if _is_forbidden_identity_path(shortcut):
        raise ValueError("refusing to retarget the other TraderCockpit shortcut")

    if sys.platform != "win32":
        raise RuntimeError("TraderCockpit Windows install must run on Windows")
    if not source.is_file():
        raise FileNotFoundError(f"missing packaged executable: {source}")

    destination_dir.mkdir(parents=True, exist_ok=True)
    if not _same_file(source, destination):
        shutil.copy2(source, destination)
    _create_shortcut(shortcut, destination, destination_dir)

    digest = expected_sha256 or _sha256_file(destination)
    if expected_sha256 and _sha256_file(source).casefold() != expected_sha256.casefold():
        raise ValueError("executable SHA-256 does not match the trusted release manifest")
    selected_version = version or _default_release_version(source=source, install_dir=destination_dir)
    _write_install_manifest(destination_dir, version=selected_version, executable_sha256=digest)
    return destination


def _default_release_version(*, source: Path, install_dir: Path) -> str:
    for candidate in (source.parent / "release-manifest.json", install_dir / "release-manifest.json"):
        if candidate.is_file():
            payload = json.loads(candidate.read_text(encoding="utf-8"))
            version = payload.get("version")
            if isinstance(version, str) and version.strip():
                return version.strip()
    pyproject = Path(__file__).resolve().parents[1] / "pyproject.toml"
    if pyproject.is_file():
        payload = tomllib.loads(pyproject.read_text(encoding="utf-8"))
        return str(payload["project"]["version"])
    return "unknown"


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
    parser.add_argument("--version", default=None, help="Release version recorded in install-manifest.json")
    parser.add_argument(
        "--expected-sha256",
        default=None,
        help="Trusted SHA-256 for the packaged executable (fail closed on mismatch)",
    )
    args = parser.parse_args(argv)
    installed = install_windows_desktop(
        args.exe,
        install_dir=args.install_dir,
        shortcut_path=args.shortcut,
        version=args.version,
        expected_sha256=args.expected_sha256,
    )
    print(installed)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
