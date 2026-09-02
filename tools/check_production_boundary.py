#!/usr/bin/env python3
"""Fail if production code imports or embeds superseded product authority."""

from __future__ import annotations

import argparse
import ast
from dataclasses import dataclass
from pathlib import Path
import re
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
    # The bounded Assistant (Apollo identity) is an allowed platform surface; these markers
    # name the prohibited persistent Apollo product spine / top-level surface architecture.
    "APOLLO_SURFACE_ID",
    "apollo-persistent",
    "apollo-dock",
    "apollo_spine",
    "StrategySpecV1",
    "BacktestEvaluatorV1",
    "BacktestRunSpecV1",
    "evaluator_not_bound",
    "tradercockpit.engine",
    "SQX_RETAINED_BUILDER_PROJECT",
    "retained_native_reference",
    "exact_retained_git_blob_identity",
    "retained_native_validation_evidence_required",
    "RETESTER_ENGINE_SHA256",
)
_NATIVE_MUTABLE_VALIDITY_MODULES = frozenset(
    {
        "sqx_builder_config.py",
        "research_configurations.py",
        "research_native_jobs.py",
        "research_retester.py",
        "sqx_gateway.py",
    }
)
_SHA256_LITERAL_RE = re.compile(r"^[0-9a-fA-F]{64}$")
WEB_FORBIDDEN_MARKERS = (
    "secrets_store",
    "keys.env",
)
_WEB_SCAN_SUFFIXES = frozenset({".mjs", ".js", ".html"})


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


def _contains_sha256_literal(node: ast.AST | None) -> bool:
    if node is None:
        return False
    return any(
        isinstance(item, ast.Constant)
        and isinstance(item.value, str)
        and _SHA256_LITERAL_RE.fullmatch(item.value) is not None
        for item in ast.walk(node)
    )


def _assignment_names(node: ast.Assign | ast.AnnAssign) -> tuple[str, ...]:
    targets = node.targets if isinstance(node, ast.Assign) else (node.target,)
    names: list[str] = []
    for target in targets:
        names.extend(item.id for item in ast.walk(target) if isinstance(item, ast.Name))
    return tuple(names)


def _native_digest_literal_violations(path: Path, tree: ast.Module) -> list[Violation]:
    """Reject renamed hard-coded artifact allowlists in mutable native validity modules.

    These modules must derive mutable Builder/Retester identities from the current
    authorized runtime or immutable custody. A module-level SHA-256 literal here is
    therefore a stale-reference trust oracle regardless of the constant's spelling.
    Built-in preset hashes live in a separate read-only catalog and are intentionally
    outside this rule.
    """

    if path.name not in _NATIVE_MUTABLE_VALIDITY_MODULES:
        return []
    violations: list[Violation] = []
    for node in tree.body:
        if not isinstance(node, (ast.Assign, ast.AnnAssign)):
            continue
        value = node.value
        if not _contains_sha256_literal(value):
            continue
        names = _assignment_names(node) or ("<module-level-sha256>",)
        violations.extend(
            Violation(path, node.lineno, name, "native_digest_literal") for name in names
        )
    return violations


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
    violations.extend(_native_digest_literal_violations(path, tree))
    return violations


def _forbidden_path(relative: Path) -> bool:
    return any(relative == prefix or prefix in relative.parents for prefix in FORBIDDEN_PATH_PREFIXES)


def scan_web(root: Path) -> list[Violation]:
    web = root / "web"
    if not web.is_dir():
        return []
    violations: list[Violation] = []
    for path in sorted(web.rglob("*")):
        if not path.is_file() or path.suffix not in _WEB_SCAN_SUFFIXES:
            continue
        text = path.read_text(encoding="utf-8")
        for marker in WEB_FORBIDDEN_MARKERS:
            line = _marker_line(text, marker)
            if line is not None:
                violations.append(Violation(path, line, marker, "web_marker"))
    return violations


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
        violations = scan_product(args.root.resolve()) + scan_web(args.root.resolve())
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
