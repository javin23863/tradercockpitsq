#!/usr/bin/env python3
"""Build the one canonical TraderCockpit Windows desktop executable.

The executable freezes the existing ``tradercockpit.desktop`` host and bundles the
repository's canonical ``web/`` tree as runtime data. It does not create a second
server or UI source tree.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import sys


_EXECUTABLE_NAME = "TraderCockpit"


def pyinstaller_arguments(
    root: Path,
    *,
    dist_dir: Path,
    work_dir: Path,
) -> list[str]:
    product = root / "product"
    web = root / "web"
    entry = product / "tradercockpit" / "desktop.py"
    for path, label in ((product, "product"), (web, "web"), (entry, "desktop entrypoint")):
        if not path.exists():
            raise FileNotFoundError(f"missing {label}: {path}")

    return [
        "--noconfirm",
        "--clean",
        "--onefile",
        "--windowed",
        f"--name={_EXECUTABLE_NAME}",
        f"--distpath={dist_dir}",
        f"--workpath={work_dir}",
        f"--specpath={work_dir}",
        f"--paths={product}",
        f"--add-data={web}{os.pathsep}web",
        str(entry),
    ]


def build_windows_desktop(
    root: Path,
    *,
    dist_dir: Path,
    work_dir: Path,
) -> Path:
    if sys.platform != "win32":
        raise RuntimeError("TraderCockpit Windows desktop packaging must run on Windows")

    try:
        from PyInstaller.__main__ import run as pyinstaller_run
    except ImportError as exc:
        raise RuntimeError(
            "Windows desktop packaging is not installed. Install the 'desktop-build' extra."
        ) from exc

    shutil.rmtree(dist_dir, ignore_errors=True)
    shutil.rmtree(work_dir, ignore_errors=True)
    dist_dir.mkdir(parents=True, exist_ok=True)
    work_dir.mkdir(parents=True, exist_ok=True)

    pyinstaller_run(
        pyinstaller_arguments(
            root,
            dist_dir=dist_dir,
            work_dir=work_dir,
        )
    )
    executable = dist_dir / f"{_EXECUTABLE_NAME}.exe"
    if not executable.is_file():
        raise RuntimeError(f"PyInstaller did not produce the expected executable: {executable}")
    return executable


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build TraderCockpit for Windows")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Repository root containing product/ and web/.",
    )
    parser.add_argument("--dist-dir", type=Path, default=Path("dist/windows"))
    parser.add_argument("--work-dir", type=Path, default=Path("build/windows"))
    args = parser.parse_args(argv)

    root = args.root.expanduser().resolve()
    executable = build_windows_desktop(
        root,
        dist_dir=args.dist_dir.expanduser().resolve(),
        work_dir=args.work_dir.expanduser().resolve(),
    )
    print(executable)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
