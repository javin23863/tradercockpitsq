#!/usr/bin/env python3
"""Fail if production code imports or embeds superseded product authority."""

from __future__ import annotations

import argparse
import ast
from dataclasses import dataclass
from pathlib import Path
import sys
from typing import Iterable


FORBIDDEN_ROOTS = frozenset({"sources", "references", "futures"})
FORBIDDEN_PATH_PREFIXES = (
    Path("tradercockpit") / "builder",
    Path("tradercockpit") / "engine",
)
FORBIDDEN_MARKERS = (
    "phase01_intake",
    "tradercockpit.builder-strategy.v1",
    "javin23863/futures",
    "Apollo",
    "StrategySpecV1",
    "BacktestEvaluatorV1",
    "BacktestRunSpecV1",
    "evaluator_not_bound",
    "tradercockpit.engine",
)


@dataclass(frozen=True, slots=True)
class Violation:
    path: Path
    line: int
    module: str
    kind: str = "import"


def _forbidden(module: str | None) -> bool:
    if not module:
        return False
    root = module.split(".", 1)[0]
    return root in FORBIDDEN_ROOTS


def _marker_line(text: str, marker: str) -> int | None:
    for index, line in enumerate(text.splitlines(), start=1):
        if marker in line:
            return index
    return None


def scan_file(path: Path) -> list[Violation]:
    text = path.read_text(encoding="utf-8")
    tree = ast.parse(text, filename=str(path))
    violations: list[Violation] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if _forbidden(alias.name):
                    violations.append(Violation(path, node.lineno, alias.name))
        elif isinstance(node, ast.ImportFrom) and _forbidden(node.module):
            violations.append(Violation(path, node.lineno, node.module or ""))

    for marker in FORBIDDEN_MARKERS:
        line = _marker_line(text, marker)
        if line is not None:
            violations.append(Violation(path, line, marker, "marker"))
    return violations


def _forbidden_path(relative: Path) -> bool:
    return any(relative == prefix or prefix in relative.parents for prefix in FORBIDDEN_PATH_PREFIXES)


def scan_product(root: Path) -> list[Violation]:
    product = root / "product"
    if not product.is_dir():
        raise FileNotFoundError(f"production root does not exist: {product}")
    violations: list[Violation] = []
    for path in sorted(product.rglob("*.py")):
        relative = path.relative_to(product)
        if _forbidden_path(relative):
            violations.append(Violation(path, 1, relative.as_posix(), "path"))
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
                f"{violation.path}:{violation.line}: forbidden production "
                f"{violation.kind} {violation.module}",
                file=sys.stderr,
            )
        print(f"production-boundary: FAIL ({len(violations)} violation(s))", file=sys.stderr)
        return 1

    print("production-boundary: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
