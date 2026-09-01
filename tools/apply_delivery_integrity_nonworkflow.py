from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one replacement target, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


replace_once(
    "LIVING_IMPLEMENTATION_PLAN.md",
    "- `LIVING_IMPLEMENTATION_PLAN.md` — current implementation sequence and status.\n\n## Product shape\n",
    "- `LIVING_IMPLEMENTATION_PLAN.md` — current implementation sequence and status.\n\n"
    "## Active delivery gate\n\n"
    "<!-- delivery-integrity\n"
    "allowed-plan-items: workflow-correction-integrity-audit,robustness-higher-precision,research-proof,research-restart-reopen\n"
    "breadth-freeze: one-native-robustness-method-until-research-reopen\n"
    "-->\n\n"
    "The current product-completion lane is the **Research end-to-end vertical**. Work advances toward one usable desktop path rather than horizontally accumulating capabilities:\n\n"
    "1. `workflow-correction-integrity-audit` — exercise the already-landed Builder → Candidate → Retester → Trades/Configuration path against the installed SQX runtime and remove any remaining invalid static-reference prerequisite.\n"
    "2. `robustness-higher-precision` — connect and accept exactly one producer-backed native Robustness method through the common Retester execution/custody path.\n"
    "3. `research-proof` — bind the exact Idea/configuration/job/Candidate/Historical Result/trades/validation identities into the real Proof surface.\n"
    "4. `research-restart-reopen` — restart the desktop with the same data root and recover the exact complete Research chain.\n\n"
    "**Breadth freeze:** until step 4 is complete, do not add or merge a second Robustness method, optimization family, or adjacent Research capability. Additional SQX methods are reference material only until the first complete Research vertical works and reopens.\n\n"
    "This section is parsed by repository delivery-integrity automation. Product PRs may update completion/status in this same plan, but a PR cannot authorize a new product lane for itself; authorization is read from the base `main` version of this file.\n\n"
    "## Product shape\n",
)

replace_once(
    "LIVING_IMPLEMENTATION_PLAN.md",
    "- [ ] Backtest Robustness shows producer-backed native validation state only; no TraderCockpit validation algorithm or reconstructed outcome may substitute for the native producer.\n- [ ] Proof binds exact idea/config/runtime/job/artifact/result/validation identities.\n- [ ] Restart/reopen resolves the same identities across the complete Research path.\n\nNo platform-owned Builder, GA, historical backtester, robustness engine, optimizer, or workflow executor may substitute for the native producer.\n",
    "- [ ] Backtest Robustness shows producer-backed native validation state only; no TraderCockpit validation algorithm or reconstructed outcome may substitute for the native producer. Complete one method first; additional methods remain frozen until Proof + restart/reopen complete the vertical.\n- [ ] Proof binds exact idea/config/runtime/job/artifact/result/validation identities and is visible in the canonical desktop.\n- [ ] Restart/reopen resolves the same identities across the complete Research path using the same data root.\n- [ ] **Breadth unlock:** only after the complete Research chain reopens may additional native Robustness methods be added, and they must reuse the common Robustness execution/custody lifecycle rather than clone it.\n\nNo platform-owned Builder, GA, historical backtester, robustness engine, optimizer, or workflow executor may substitute for the native producer.\n",
)

replace_once(
    "LIVING_IMPLEMENTATION_PLAN.md",
    "8. During prototype construction, merge only after exact-head focused tests, Product Runtime Acceptance, relevant browser/desktop acceptance, and the applicable real-producer exercise are clean. Do not block each intermediate implementation slice on Codex review.\n9. Run the comprehensive adversarial Codex review/closure pass on the assembled end-of-plan prototype candidate, then fix findings before declaring the prototype complete.\n10. Delete/supersede implementation branches after merge; do not preserve parallel product branches as future authorities.\n",
    "8. During prototype construction, an **intermediate** slice merge requires exact-head focused tests, Product Runtime Acceptance, relevant browser/desktop acceptance, the applicable real-producer exercise, and one substantive exact-head adversarial review. Codex may provide that review, but Codex closure is not a mandatory intermediate-slice gate.\n9. Run the comprehensive adversarial **Codex review/closure** pass on the assembled end-of-plan prototype candidate. The repository label `final-prototype-review` activates that mandatory final closure gate.\n10. Production implementation PRs target `main` directly and must contain the current `main` head in their ancestry. Stacked production PRs are prohibited; later slices are replayed from current `main` after their dependencies merge.\n11. Delete/supersede implementation branches after merge; do not preserve parallel product branches as future authorities.\n",
)

replace_once(
    "LIVING_IMPLEMENTATION_PLAN.md",
    "**First complete the workflow-correction integrity audit of the already-landed Research path against the actual installed SQX program and the real-runtime observations already captured. Remove retained-byte/evidence gates that can block valid native state and identify any unnecessary reconstruction. Then continue directly into actual SQX Robustness configuration/execution/readback and connect it to Backtest Robustness. No preliminary `ICrossCheck` persistence/readback evidence gate is required.**\n\nDo not begin a separate feature roadmap. New work advances this file from top to bottom unless the architecture is explicitly changed first.\n",
    "**Finish the current Research vertical before adding breadth:** run the installed-SQX integrity audit; land one real Higher Precision Robustness seam; build the Proof surface over the exact existing identities; then prove full desktop restart/reopen on the same data root. Do not add System Parameter Permutation, Monte Carlo, Additional Markets, Walk-Forward, or another Research capability until those four steps are complete. No preliminary retained-evidence gate is required when the installed SQX runtime can answer the integration question directly.**\n\nDo not begin a separate feature roadmap. New work advances this file from top to bottom unless the architecture is explicitly changed first.\n",
)

replace_once(
    "AGENTS.md",
    "- Start every implementation branch from current `main`.\n- Select the first incomplete applicable item in `LIVING_IMPLEMENTATION_PLAN.md`.\n- Confirm no active branch owns the same product slice/files.\n- Keep one branch limited to one coherent slice.\n",
    "- Start every implementation branch from current `main`. A production PR must target `main` and contain the **current** `main` head in its ancestry before it can be merge-ready.\n- Stacked production PRs are prohibited. If a later slice was prototyped on another feature branch, salvage only its method-specific delta and replay it from current `main` after the dependency merges.\n- Select the first incomplete applicable item in `LIVING_IMPLEMENTATION_PLAN.md`; do not start a later blocking item merely because its code is convenient to write.\n- Respect the living plan's active delivery gate and breadth freeze. One working user vertical takes precedence over adding second/third methods in the same capability family.\n- Confirm no active branch owns the same product slice/files.\n- Keep one branch limited to one coherent slice.\n",
)

replace_once(
    "AGENTS.md",
    "- For native integrations, inspect/run the installed producer as part of the slice whenever it is available; do not defer that producer exercise into a separate evidence checkpoint.\n- Merge only after exact-head tests, applicable Product Runtime Acceptance, browser/desktop acceptance, and substantive review are clean.\n- Delete the implementation branch after merge. Closed or historical branches are not future architecture authorities.\n",
    "- For native integrations, inspect/run the installed producer as part of the slice whenever it is available; do not defer that producer exercise into a separate evidence checkpoint. Native-changing PRs carry an exact-head `Installed SQX Acceptance` status; a trusted collaborator may satisfy it only after the real installed producer has been exercised for that exact commit.\n- Robustness method/profile adapters may parse or minimally compile producer-owned profile settings, but they may not import the native control gateway or call `launch_retester_task`; native launch, receipts, failure/interruption handling, result capture, cataloging, and Proof custody belong to one common Robustness execution lifecycle.\n- Merge an intermediate slice only after exact-head tests, applicable Product Runtime Acceptance, browser/desktop acceptance, installed-producer acceptance when required, and one substantive exact-head adversarial review are clean. Codex is optional for intermediate slices.\n- Mandatory Codex closure is reserved for the assembled prototype candidate explicitly labeled `final-prototype-review`.\n- Delete the implementation branch after merge. Closed or historical branches are not future architecture authorities.\n",
)

replace_once(
    "docs/product-backbone-spec-v1.md",
    "### Backtest / Robustness\n\nSelected native cross-check/retest/optimization methods rendered dynamically from an exact producer-backed plan. Methods are capabilities, not permanent navigation tabs.\n",
    "### Backtest / Robustness\n\nSelected native cross-check/retest/optimization methods rendered dynamically from an exact producer-backed plan. Methods are capabilities, not permanent navigation tabs.\n\nRobustness uses one platform execution/custody lifecycle around producer-owned SQX methods:\n\n`native method/profile adapter -> common isolated-project compiler/stager -> common Retester task executor -> common receipt/failure/result/Proof custody -> method-specific producer outcome reader`\n\nMethod/profile adapters may identify the native profile, validate/preserve its current producer-owned fields, and make only the minimum producer-required profile-selection change. They must not duplicate Retester launch, runtime trust, receipt normalization, failure/interruption persistence, result capture, catalog/readback, or Proof custody. A second native method is added only after the first complete Research vertical (including Proof and restart/reopen) is working.\n",
)

readme = (ROOT / "README.md").read_text(encoding="utf-8")
start = readme.index("## Current repository shape\n")
end = readme.index("## Desktop\n")
replacement = """## Repository status authority\n\n`README.md` intentionally does **not** enumerate which product slices are currently complete. Current implementation state, sequencing, and the active delivery gate live only in `LIVING_IMPLEMENTATION_PLAN.md`.\n\nStable repository boundaries are:\n\n- `product/tradercockpit/` — the one canonical application, custody, native-runtime, and producer-integration family;\n- `web/` — the one product UI used by browser acceptance and the desktop host;\n- `tests/` — product/runtime/browser/desktop acceptance;\n- `docs/product-architecture-v1.md` and `docs/product-backbone-spec-v1.md` — stable architecture and detailed contract;\n- `LIVING_IMPLEMENTATION_PLAN.md` — the sole current sequencing/status authority;\n- `tools/check_production_boundary.py` — rejects prohibited duplicate/reference architecture.\n\nNative quantitative behavior remains producer-owned by the installed SQX runtime. TraderCockpit supplies trusted control, exact custody, readback, orchestration, and product presentation without recreating Builder, Retester, Robustness, optimization, or other SQX algorithms.\n\n"""
(ROOT / "README.md").write_text(readme[:start] + replacement + readme[end:], encoding="utf-8")

replace_once(
    "tools/check_production_boundary.py",
    "_SHA256_LITERAL_RE = re.compile(r\"^[0-9a-fA-F]{64}$\")\n",
    "_SHA256_LITERAL_RE = re.compile(r\"^[0-9a-fA-F]{64}$\")\n_ROBUSTNESS_METHOD_ADAPTER_RE = re.compile(r\"^research_robustness_.+\\.py$\")\n",
)
replace_once(
    "tools/check_production_boundary.py",
    "def scan_file(path: Path) -> list[Violation]:\n",
    "def _robustness_method_executor_violations(path: Path, tree: ast.Module) -> list[Violation]:\n"
    "    \"\"\"Keep SQX Robustness execution/custody in one common lifecycle.\"\"\"\n"
    "    if _ROBUSTNESS_METHOD_ADAPTER_RE.fullmatch(path.name) is None:\n"
    "        return []\n"
    "    violations: list[Violation] = []\n"
    "    for node in ast.walk(tree):\n"
    "        if isinstance(node, ast.ImportFrom) and node.module == \"tradercockpit.sqx_gateway\":\n"
    "            for alias in node.names:\n"
    "                if alias.name in {\"SqxNativeControlGateway\", \"SqxNativeGatewayError\"}:\n"
    "                    violations.append(Violation(path, node.lineno, alias.name, \"robustness_method_executor\"))\n"
    "        elif isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute) and node.func.attr == \"launch_retester_task\":\n"
    "            violations.append(Violation(path, node.lineno, \"launch_retester_task\", \"robustness_method_executor\"))\n"
    "    return violations\n\n\n"
    "def scan_file(path: Path) -> list[Violation]:\n",
)
replace_once(
    "tools/check_production_boundary.py",
    "    violations.extend(_native_digest_literal_violations(path, tree))\n    return violations\n",
    "    violations.extend(_native_digest_literal_violations(path, tree))\n    violations.extend(_robustness_method_executor_violations(path, tree))\n    return violations\n",
)

replace_once(
    "tests/product/test_production_boundary.py",
    "\n\nif __name__ == \"__main__\":\n    unittest.main()\n",
    '''\n\n    def test_method_specific_robustness_module_cannot_import_native_gateway(self):\n        violations = self._violations_for(\n            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\\n",\n            "tradercockpit/research_robustness_monte_carlo.py",\n        )\n        self.assertTrue(any(item.kind == "robustness_method_executor" for item in violations))\n\n    def test_method_specific_robustness_module_cannot_launch_retester(self):\n        violations = self._violations_for(\n            "def run(gateway):\\n    return gateway.launch_retester_task('x')\\n",\n            "tradercockpit/research_robustness_system_parameter.py",\n        )\n        self.assertTrue(any(item.module == "launch_retester_task" for item in violations))\n\n    def test_common_robustness_module_may_own_native_execution(self):\n        violations = self._violations_for(\n            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\\n"\n            "def run(gateway):\\n    return gateway.launch_retester_task('x')\\n",\n            "tradercockpit/research_robustness.py",\n        )\n        self.assertFalse(any(item.kind == "robustness_method_executor" for item in violations))\n\n\nif __name__ == "__main__":\n    unittest.main()\n''',
)

write(
    ".gitignore",
    """# Python\n__pycache__/\n*.py[cod]\n*.egg-info/\n.pytest_cache/\n.mypy_cache/\n.coverage\n\n# Node / browser tooling\nnode_modules/\nplaywright-report/\ntest-results/\n\n# Build/package output\nbuild/\ndist/\n*.spec\n\n# OS/editor/transient files\n.DS_Store\nThumbs.db\n*.tmp\n*.log\n""",
)

write(
    ".github/pull_request_template.md",
    """## Delivery authority\n\nLiving plan item: `replace-with-plan-item-id`\n\nUser path: describe the exact desktop path this slice makes work or advances\n\nNative producer seam: `not-applicable` or name the installed SQX executable/project/task/artifact seam actually used\n\nNative producer acceptance: `not-applicable` | `pending` | `required`\n\nReview class: `intermediate` | `final-prototype`\n\n## Scope\n\nDescribe the bounded change. Production implementation PRs target current `main` directly; stacked production PRs are not allowed.\n\n## Truth boundary\n\nState what the producer owns and what TraderCockpit owns. Do not claim native outcome truth that the producer seam does not expose.\n\n## Acceptance\n\n- [ ] Focused tests\n- [ ] Product Runtime Acceptance on exact head\n- [ ] Browser/desktop acceptance where applicable\n- [ ] Installed SQX Acceptance on exact head where native behavior changes\n- [ ] Substantive exact-head adversarial review\n""",
)

print("non-workflow delivery integrity patch applied")
