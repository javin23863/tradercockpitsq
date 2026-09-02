#!/usr/bin/env python3
"""Build, optionally sign, and package the TraderCockpit Windows desktop installer."""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tomllib
import zipfile


def _bootstrap_imports() -> None:
    root = Path(__file__).resolve().parents[1]
    product = root / "product"
    if str(product) not in sys.path:
        sys.path.insert(0, str(product))


def _load_build_module():
    path = Path(__file__).resolve().parent / "build_windows_desktop.py"
    spec = importlib.util.spec_from_file_location("tradercockpit_build_windows_desktop", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _project_version(root: Path) -> str:
    payload = tomllib.loads((root / "pyproject.toml").read_text(encoding="utf-8"))
    return str(payload["project"]["version"])


def _render_setup_script(*, version: str, executable_sha256: str) -> str:
    return f"""# TraderCockpit Windows setup ({version})
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$exe = Join-Path $here "TraderCockpit.exe"
if (-not (Test-Path $exe)) {{
  throw "Missing TraderCockpit.exe beside this setup script."
}}
$installer = Join-Path $here "install_windows_desktop.py"
if (-not (Test-Path $installer)) {{
  throw "Missing install_windows_desktop.py beside this setup script."
}}
python $installer --exe $exe --version {version} --expected-sha256 {executable_sha256}
"""


def _maybe_build_nsis_installer(
    *,
    root: Path,
    staging: Path,
    version: str,
    out_dir: Path,
) -> Path | None:
    makensis = shutil.which("makensis")
    script = root / "packaging" / "windows" / "installer.nsi"
    if makensis is None or not script.is_file():
        return None

    out_dir.mkdir(parents=True, exist_ok=True)
    installer_exe = out_dir / f"TraderCockpit-{version}-Setup.exe"
    completed = subprocess.run(
        [
            makensis,
            f"/DPRODUCT_VERSION={version}",
            f"/DSTAGING_DIR={staging}",
            f"/DOUT_FILE={installer_exe}",
            str(script),
        ],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout or "makensis failed").strip()
        raise RuntimeError(detail)
    if not installer_exe.is_file():
        raise RuntimeError(f"NSIS did not produce installer: {installer_exe}")
    return installer_exe


def package_windows_desktop(
    root: Path,
    *,
    dist_dir: Path,
    work_dir: Path,
    release_dir: Path,
    version: str | None = None,
    sign: bool = True,
) -> dict[str, object]:
    _bootstrap_imports()
    from tradercockpit.windows_release import (
        build_install_manifest,
        sha256_file,
        sign_executable,
        signing_status,
    )

    selected_version = version or _project_version(root)
    builder = _load_build_module()
    executable = builder.build_windows_desktop(root, dist_dir=dist_dir, work_dir=work_dir)

    signing_result = {"status": "skipped", "reason_code": "signing_not_requested"}
    if sign:
        signing_result = sign_executable(executable)

    executable_sha256 = sha256_file(executable)
    release_dir.mkdir(parents=True, exist_ok=True)

    staged_exe = release_dir / "TraderCockpit.exe"
    shutil.copy2(executable, staged_exe)
    shutil.copy2(root / "tools" / "install_windows_desktop.py", release_dir / "install_windows_desktop.py")
    (release_dir / "TraderCockpit-Setup.ps1").write_text(
        _render_setup_script(version=selected_version, executable_sha256=executable_sha256),
        encoding="utf-8",
    )

    release_manifest = build_install_manifest(
        version=selected_version,
        install_root=Path("%LOCALAPPDATA%") / "Programs" / "TraderCockpitSQ",
        executable_sha256=executable_sha256,
    )
    release_manifest["payload"] = str(staged_exe)
    release_manifest_path = release_dir / "release-manifest.json"
    release_manifest_path.write_text(json.dumps(release_manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    zip_path = release_dir.parent / f"TraderCockpit-{selected_version}-win64.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for item in release_dir.iterdir():
            archive.write(item, arcname=item.name)

    nsis_installer = _maybe_build_nsis_installer(
        root=root,
        staging=release_dir,
        version=selected_version,
        out_dir=release_dir,
    )

    return {
        "version": selected_version,
        "executable": str(executable),
        "executable_sha256": executable_sha256,
        "release_dir": str(release_dir),
        "release_manifest": str(release_manifest_path),
        "zip": str(zip_path),
        "nsis_installer": str(nsis_installer) if nsis_installer else None,
        "signing": signing_result,
        "signing_readiness": signing_status(),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Package TraderCockpit for Windows release")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Repository root containing product/ and web/.",
    )
    parser.add_argument("--dist-dir", type=Path, default=Path("dist/windows"))
    parser.add_argument("--work-dir", type=Path, default=Path("build/windows"))
    parser.add_argument("--release-dir", type=Path, default=Path("dist/windows/release"))
    parser.add_argument("--version", default=None)
    parser.add_argument(
        "--no-sign",
        action="store_true",
        help="Skip Authenticode signing even when operator signing material is configured.",
    )
    args = parser.parse_args(argv)

    if sys.platform != "win32":
        print("TraderCockpit Windows packaging must run on Windows", file=sys.stderr)
        return 1

    root = args.root.expanduser().resolve()
    result = package_windows_desktop(
        root,
        dist_dir=args.dist_dir.expanduser().resolve(),
        work_dir=args.work_dir.expanduser().resolve(),
        release_dir=args.release_dir.expanduser().resolve(),
        version=args.version,
        sign=not args.no_sign,
    )
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
