#!/usr/bin/env python3
"""Fail if production Python imports reference-only repository namespaces."""

from __future__ import annotations

import argparse
import ast
from dataclasses import dataclass
from pathlib import Path
import sys
from typing import Iterable


FORBIDDEN_ROOTS = frozenset({"sources", "references"})


@dataclass(frozen=True, slots=True)
class Violation:
    path: Path
    line: int
    module: str


def _forbidden(module: str | None) -> bool:
    if not module:
        return False
    root = module.split(".", 1)[0]
    return root in FORBIDDEN_ROOTS


def scan_file(path: Path) -> list[Violation]:
    tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
    violations: list[Violation] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if _forbidden(alias.name):
                    violations.append(Violation(path, node.lineno, alias.name))
        elif isinstance(node, ast.ImportFrom) and _forbidden(node.module):
            violations.append(Violation(path, node.lineno, node.module or ""))
    return violations


def scan_product(root: Path) -> list[Violation]:
    product = root / "product"
    if not product.is_dir():
        raise FileNotFoundError(f"production root does not exist: {product}")
    violations: list[Violation] = []
    for path in sorted(product.rglob("*.py")):
        violations.extend(scan_file(path))
    return violations


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args(list(argv) if argv is not None else None)

    try:
        violations = scan_product(args.root.resolve())
    except (FileNotFoundError, SyntaxError) as exc:
        print(f"production-boundary: FAIL: {exc}", file=sys.stderr)
        return 2

    if violations:
        for violation in violations:
            print(
                f"{violation.path}:{violation.line}: forbidden production import "
                f"{violation.module}",
                file=sys.stderr,
            )
        print(f"production-boundary: FAIL ({len(violations)} violation(s))", file=sys.stderr)
        return 1

    print("production-boundary: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
