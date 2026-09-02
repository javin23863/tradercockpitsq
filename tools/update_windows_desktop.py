#!/usr/bin/env python3
"""Apply a trusted TraderCockpit desktop update and retain rollback custody."""

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
    from tradercockpit.windows_release import WindowsReleaseError, apply_trusted_update

    parser = argparse.ArgumentParser(description="Apply a trusted TraderCockpit desktop update")
    parser.add_argument("--payload", type=Path, required=True, help="Trusted TraderCockpit.exe payload")
    parser.add_argument("--expected-sha256", required=True, help="Expected SHA-256 of the payload")
    parser.add_argument("--version", required=True, help="Release version being applied")
    parser.add_argument("--install-dir", type=Path, default=None)
    args = parser.parse_args(argv)

    installer = _load_install_module()
    install_dir = args.install_dir.expanduser().resolve() if args.install_dir else installer.default_windows_install_dir()
    try:
        result = apply_trusted_update(
            install_dir,
            args.payload,
            expected_sha256=args.expected_sha256,
            version=args.version,
        )
    except WindowsReleaseError as exc:
        print(json.dumps({"error": exc.code, "detail": exc.detail}, indent=2))
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
