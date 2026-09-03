#!/usr/bin/env python3
"""Restore the previous TraderCockpit desktop payload from rollback custody."""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path
import sys


def _bootstrap_imports() -> None:
    root = Path(__file__).resolve().parents[1]
    product = root / "product"
    if str(product) not in sys.path:
        sys.path.insert(0, str(product))


def _load_install_module():
    path = Path(__file__).resolve().parent / "install_windows_desktop.py"
    spec = importlib.util.spec_from_file_location("tradercockpit_install_windows_desktop", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(argv: list[str] | None = None) -> int:
    _bootstrap_imports()
    from tradercockpit.windows_release import WindowsReleaseError, rollback_install

    parser = argparse.ArgumentParser(description="Rollback TraderCockpit desktop to the previous payload")
    parser.add_argument("--install-dir", type=Path, default=None)
    args = parser.parse_args(argv)

    installer = _load_install_module()
    install_dir = args.install_dir.expanduser().resolve() if args.install_dir else installer.default_windows_install_dir()
    try:
        result = rollback_install(install_dir)
    except WindowsReleaseError as exc:
        print(json.dumps({"error": exc.code, "detail": exc.detail}, indent=2))
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
