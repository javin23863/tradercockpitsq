#!/usr/bin/env python3
"""Sign the packaged TraderCockpit desktop executable when operator material is configured."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys


def _bootstrap_imports() -> None:
    root = Path(__file__).resolve().parents[1]
    product = root / "product"
    if str(product) not in sys.path:
        sys.path.insert(0, str(product))


def main(argv: list[str] | None = None) -> int:
    _bootstrap_imports()
    from tradercockpit.windows_release import sign_executable

    parser = argparse.ArgumentParser(description="Sign TraderCockpit.exe with Authenticode")
    parser.add_argument(
        "--exe",
        type=Path,
        default=Path("dist/windows/TraderCockpit.exe"),
        help="Packaged TraderCockpit.exe to sign",
    )
    args = parser.parse_args(argv)
    result = sign_executable(args.exe.expanduser().resolve())
    print(json.dumps(result, indent=2, sort_keys=True))
    if result.get("status") == "failed":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
