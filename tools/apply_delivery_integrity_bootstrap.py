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


# Living plan: keep it as the sole mutable roadmap, but make the current vertical and breadth freeze machine-readable.
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

# Agent policy: turn the sequencing and common-executor intent into explicit implementation discipline.
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

# Backbone: specify the common Robustness decomposition instead of method-by-method execution clones.
replace_once(
    "docs/product-backbone-spec-v1.md",
    "### Backtest / Robustness\n\nSelected native cross-check/retest/optimization methods rendered dynamically from an exact producer-backed plan. Methods are capabilities, not permanent navigation tabs.\n",
    "### Backtest / Robustness\n\nSelected native cross-check/retest/optimization methods rendered dynamically from an exact producer-backed plan. Methods are capabilities, not permanent navigation tabs.\n\nRobustness uses one platform execution/custody lifecycle around producer-owned SQX methods:\n\n`native method/profile adapter -> common isolated-project compiler/stager -> common Retester task executor -> common receipt/failure/result/Proof custody -> method-specific producer outcome reader`\n\nMethod/profile adapters may identify the native profile, validate/preserve its current producer-owned fields, and make only the minimum producer-required profile-selection change. They must not duplicate Retester launch, runtime trust, receipt normalization, failure/interruption persistence, result capture, catalog/readback, or Proof custody. A second native method is added only after the first complete Research vertical (including Proof and restart/reopen) is working.\n",
)

# README: remove mutable implementation-state claims so it cannot compete with the living plan.
readme = (ROOT / "README.md").read_text(encoding="utf-8")
start = readme.index("## Current repository shape\n")
end = readme.index("## Desktop\n")
replacement = """## Repository status authority\n\n`README.md` intentionally does **not** enumerate which product slices are currently complete. Current implementation state, sequencing, and the active delivery gate live only in `LIVING_IMPLEMENTATION_PLAN.md`.\n\nStable repository boundaries are:\n\n- `product/tradercockpit/` — the one canonical application, custody, native-runtime, and producer-integration family;\n- `web/` — the one product UI used by browser acceptance and the desktop host;\n- `tests/` — product/runtime/browser/desktop acceptance;\n- `docs/product-architecture-v1.md` and `docs/product-backbone-spec-v1.md` — stable architecture and detailed contract;\n- `LIVING_IMPLEMENTATION_PLAN.md` — the sole current sequencing/status authority;\n- `tools/check_production_boundary.py` — rejects prohibited duplicate/reference architecture.\n\nNative quantitative behavior remains producer-owned by the installed SQX runtime. TraderCockpit supplies trusted control, exact custody, readback, orchestration, and product presentation without recreating Builder, Retester, Robustness, optimization, or other SQX algorithms.\n\n"""
(ROOT / "README.md").write_text(readme[:start] + replacement + readme[end:], encoding="utf-8")

# Production boundary: prohibit later method adapters from becoming cloned native executors.
replace_once(
    "tools/check_production_boundary.py",
    "_SHA256_LITERAL_RE = re.compile(r\"^[0-9a-fA-F]{64}$\")\n",
    "_SHA256_LITERAL_RE = re.compile(r\"^[0-9a-fA-F]{64}$\")\n_ROBUSTNESS_METHOD_ADAPTER_RE = re.compile(r\"^research_robustness_.+\\.py$\")\n",
)

replace_once(
    "tools/check_production_boundary.py",
    "def scan_file(path: Path) -> list[Violation]:\n",
    "def _robustness_method_executor_violations(path: Path, tree: ast.Module) -> list[Violation]:\n"
    "    \"\"\"Keep SQX Robustness execution/custody in one common lifecycle.\n\n"
    "    Method-specific modules named ``research_robustness_<method>.py`` may parse\n"
    "    or minimally compile producer-owned profiles, but they must not own the native\n"
    "    gateway or invoke Retester directly.\n"
    "    \"\"\"\n\n"
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

# Test the new static guard independently from the future Robustness implementation.
write(
    "tests/product/test_production_boundary.py",
    '''from pathlib import Path\nimport tempfile\nimport unittest\n\nfrom tools.check_production_boundary import scan_file\n\n\nclass ProductionBoundaryTests(unittest.TestCase):\n    def test_method_specific_robustness_module_cannot_import_native_gateway(self):\n        with tempfile.TemporaryDirectory() as directory:\n            path = Path(directory) / "research_robustness_monte_carlo.py"\n            path.write_text(\n                "from tradercockpit.sqx_gateway import SqxNativeControlGateway\\n",\n                encoding="utf-8",\n            )\n            violations = scan_file(path)\n        self.assertTrue(any(item.kind == "robustness_method_executor" for item in violations))\n\n    def test_method_specific_robustness_module_cannot_launch_retester(self):\n        with tempfile.TemporaryDirectory() as directory:\n            path = Path(directory) / "research_robustness_system_parameter.py"\n            path.write_text(\n                "def run(gateway):\\n    return gateway.launch_retester_task('x')\\n",\n                encoding="utf-8",\n            )\n            violations = scan_file(path)\n        self.assertTrue(any(item.module == "launch_retester_task" for item in violations))\n\n    def test_common_robustness_module_may_own_native_execution(self):\n        with tempfile.TemporaryDirectory() as directory:\n            path = Path(directory) / "research_robustness.py"\n            path.write_text(\n                "from tradercockpit.sqx_gateway import SqxNativeControlGateway\\n"\n                "def run(gateway):\\n    return gateway.launch_retester_task('x')\\n",\n                encoding="utf-8",\n            )\n            violations = scan_file(path)\n        self.assertFalse(any(item.kind == "robustness_method_executor" for item in violations))\n\n\nif __name__ == "__main__":\n    unittest.main()\n''',
)

write(
    ".gitignore",
    '''# Python\n__pycache__/\n*.py[cod]\n*.egg-info/\n.pytest_cache/\n.mypy_cache/\n.coverage\n\n# Node / browser tooling\nnode_modules/\nplaywright-report/\ntest-results/\n\n# Build/package output\nbuild/\ndist/\n*.spec\n\n# OS/editor/transient files\n.DS_Store\nThumbs.db\n*.tmp\n*.log\n''',
)

write(
    ".github/pull_request_template.md",
    '''## Delivery authority\n\nLiving plan item: `replace-with-plan-item-id`\n\nUser path: describe the exact desktop path this slice makes work or advances\n\nNative producer seam: `not-applicable` or name the installed SQX executable/project/task/artifact seam actually used\n\nNative producer acceptance: `not-applicable` | `pending` | `required`\n\nReview class: `intermediate` | `final-prototype`\n\n## Scope\n\nDescribe the bounded change. Production implementation PRs target current `main` directly; stacked production PRs are not allowed.\n\n## Truth boundary\n\nState what the producer owns and what TraderCockpit owns. Do not claim native outcome truth that the producer seam does not expose.\n\n## Acceptance\n\n- [ ] Focused tests\n- [ ] Product Runtime Acceptance on exact head\n- [ ] Browser/desktop acceptance where applicable\n- [ ] Installed SQX Acceptance on exact head where native behavior changes\n- [ ] Substantive exact-head adversarial review\n''',
)

write(
    ".github/workflows/delivery-integrity.yml",
    '''name: Delivery Integrity\n\non:\n  pull_request:\n    types: [opened, reopened, synchronize, edited, ready_for_review, labeled, unlabeled]\n\npermissions:\n  contents: read\n  pull-requests: read\n\njobs:\n  delivery-integrity:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n        with:\n          ref: ${{ github.event.pull_request.head.sha }}\n          fetch-depth: 0\n      - name: Require current main ancestry\n        shell: bash\n        run: |\n          set -euo pipefail\n          if [ "${{ github.event.pull_request.base.ref }}" != "main" ]; then\n            echo "Production delivery PRs must target main directly." >&2\n            exit 1\n          fi\n          git fetch origin main --no-tags\n          current_main="$(git rev-parse origin/main)"\n          merge_base="$(git merge-base HEAD origin/main)"\n          if [ "$merge_base" != "$current_main" ]; then\n            echo "PR head is stale/stacked: merge-base $merge_base does not equal current main $current_main." >&2\n            echo "Replay/rebase this slice from current main before review/merge." >&2\n            exit 1\n          fi\n      - name: Validate plan authority and PR contract\n        uses: actions/github-script@v7\n        with:\n          script: |\n            const owner = context.repo.owner;\n            const repo = context.repo.repo;\n            const pr = context.payload.pull_request;\n            const body = pr.body || '';\n            const field = (label) => {\n              const match = body.match(new RegExp(`^${label}:\\\\s*(.+)$`, 'mi'));\n              return match ? match[1].trim().replace(/^`|`$/g, '') : null;\n            };\n            const planItem = field('Living plan item');\n            const userPath = field('User path');\n            const nativeSeam = field('Native producer seam');\n            const nativeAcceptance = field('Native producer acceptance');\n            const reviewClass = field('Review class');\n            const failures = [];\n            if (!planItem || planItem === 'replace-with-plan-item-id') failures.push('Living plan item is missing.');\n            if (!userPath) failures.push('User path is missing.');\n            if (!nativeSeam) failures.push('Native producer seam is missing.');\n            if (!['not-applicable', 'pending', 'required'].includes(nativeAcceptance || '')) failures.push('Native producer acceptance must be not-applicable, pending, or required.');\n            if (!['intermediate', 'final-prototype'].includes(reviewClass || '')) failures.push('Review class must be intermediate or final-prototype.');\n\n            const {data: basePlanFile} = await github.rest.repos.getContent({\n              owner, repo, path: 'LIVING_IMPLEMENTATION_PLAN.md', ref: pr.base.sha,\n            });\n            const basePlan = Buffer.from(basePlanFile.content, basePlanFile.encoding).toString('utf8');\n            const gate = basePlan.match(/<!-- delivery-integrity\\s+allowed-plan-items:\\s*([^\\n]+)\\s+breadth-freeze:\\s*([^\\n]+)\\s*-->/m);\n            const bootstrap = !gate && planItem === 'delivery-governance';\n            if (gate) {\n              const allowed = new Set(gate[1].split(',').map((value) => value.trim()).filter(Boolean));\n              if (!allowed.has(planItem)) {\n                failures.push(`Plan item ${planItem} is not authorized by base main. Allowed now: ${[...allowed].join(', ')}`);\n              }\n            } else if (!bootstrap) {\n              failures.push('Base main has no delivery-integrity marker; only the delivery-governance bootstrap may add it.');\n            }\n\n            const files = await github.paginate(github.rest.pulls.listFiles, {owner, repo, pull_number: pr.number, per_page: 100});\n            const nativePattern = /^product\\/tradercockpit\\/(?:sqx_|research_(?:configurations|native_jobs|candidates|retester|trades|robustness|backtest_configuration))/;\n            const nativeChanged = files.some((item) => nativePattern.test(item.filename));\n            if (nativeChanged && nativeAcceptance === 'not-applicable') {\n              failures.push('Native integration files changed, so installed-SQX acceptance cannot be marked not-applicable.');\n            }\n            const finalLabel = (pr.labels || []).some((label) => label.name === 'final-prototype-review');\n            if (reviewClass === 'final-prototype' && !finalLabel) failures.push('final-prototype review class requires the final-prototype-review label.');\n            if (reviewClass === 'intermediate' && finalLabel) failures.push('final-prototype-review label is reserved for the assembled prototype candidate.');\n\n            const {data: repoInfo} = await github.rest.repos.get({owner, repo});\n            if (repoInfo.default_branch !== 'main') {\n              const message = `Repository default branch is ${repoInfo.default_branch}; it must be main.`;\n              if (bootstrap) core.warning(message + ' Fix repository settings before the next product PR.');\n              else failures.push(message);\n            }\n            let protectedMain = true;\n            try {\n              await github.rest.repos.getBranchProtection({owner, repo, branch: 'main'});\n            } catch (error) {\n              if (error.status === 404) protectedMain = false;\n              else core.warning(`Could not verify main protection: ${error.message}`);\n            }\n            if (!protectedMain) {\n              const message = 'main is not branch-protected.';\n              if (bootstrap) core.warning(message + ' Enable protection before the next product PR.');\n              else failures.push(message);\n            }\n            if (failures.length) core.setFailed(failures.join('\\n'));\n''',
)

write(
    ".github/workflows/installed-sqx-acceptance.yml",
    '''name: Installed SQX Acceptance\n\non:\n  pull_request:\n    types: [opened, reopened, synchronize]\n  issue_comment:\n    types: [created]\n\npermissions:\n  contents: read\n  issues: read\n  pull-requests: read\n  statuses: write\n\njobs:\n  installed-sqx-acceptance:\n    if: github.event_name == 'pull_request' || github.event.issue.pull_request != null\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/github-script@v7\n        with:\n          script: |\n            const owner = context.repo.owner;\n            const repo = context.repo.repo;\n            const trusted = new Set(['OWNER', 'MEMBER', 'COLLABORATOR']);\n            const statusContext = 'Installed SQX Acceptance';\n            let number;\n            if (context.eventName === 'pull_request') number = context.payload.pull_request.number;\n            else number = context.payload.issue.number;\n            const {data: pr} = await github.rest.pulls.get({owner, repo, pull_number: number});\n            const head = pr.head.sha;\n            const files = await github.paginate(github.rest.pulls.listFiles, {owner, repo, pull_number: number, per_page: 100});\n            const nativePattern = /^product\\/tradercockpit\\/(?:sqx_|research_(?:configurations|native_jobs|candidates|retester|trades|robustness|backtest_configuration))/;\n            const required = files.some((item) => nativePattern.test(item.filename));\n            const setStatus = async (state, description) => github.rest.repos.createCommitStatus({\n              owner, repo, sha: head, state, context: statusContext, description: description.slice(0, 140), target_url: pr.html_url,\n            });\n            if (!required) {\n              await setStatus('success', 'Installed SQX exercise not applicable');\n              return;\n            }\n            if (context.eventName === 'pull_request') {\n              await setStatus('pending', 'Real installed SQX exercise required for exact head');\n              return;\n            }\n            const comment = context.payload.comment;\n            if (!trusted.has(comment.author_association)) return;\n            const match = (comment.body || '').trim().match(/^\\/installed-sqx-accept\\s+([0-9a-f]{40})\\s+(.+)$/i);\n            if (!match) return;\n            const acceptedHead = match[1].toLowerCase();\n            const transcript = match[2].trim();\n            if (acceptedHead !== head) {\n              core.setFailed(`Acceptance targets ${acceptedHead}, but current PR head is ${head}.`);\n              return;\n            }\n            await setStatus('success', `Installed SQX exercised: ${transcript}`);\n''',
)

write(
    ".github/workflows/substantive-review.yml",
    '''name: Substantive Review\n\non:\n  pull_request:\n    types: [opened, reopened, synchronize]\n  issue_comment:\n    types: [created]\n  pull_request_review:\n    types: [submitted]\n\npermissions:\n  contents: read\n  issues: read\n  pull-requests: read\n  statuses: write\n\njobs:\n  substantive-review:\n    if: github.event_name == 'pull_request' || github.event_name == 'pull_request_review' || github.event.issue.pull_request != null\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/github-script@v7\n        with:\n          script: |\n            const owner = context.repo.owner;\n            const repo = context.repo.repo;\n            const trusted = new Set(['OWNER', 'MEMBER', 'COLLABORATOR']);\n            const statusContext = 'Substantive Review';\n            let number;\n            if (context.eventName === 'pull_request') number = context.payload.pull_request.number;\n            else if (context.eventName === 'pull_request_review') number = context.payload.pull_request.number;\n            else number = context.payload.issue.number;\n            const {data: pr} = await github.rest.pulls.get({owner, repo, pull_number: number});\n            const head = pr.head.sha;\n            const setStatus = async (state, description) => github.rest.repos.createCommitStatus({\n              owner, repo, sha: head, state, context: statusContext, description: description.slice(0, 140), target_url: pr.html_url,\n            });\n            if (context.eventName === 'pull_request') {\n              await setStatus('pending', 'Exact-head adversarial review required');\n              return;\n            }\n            if (context.eventName === 'pull_request_review') {\n              const review = context.payload.review;\n              if (\n                review.state === 'approved' &&\n                trusted.has(review.author_association) &&\n                (review.commit_id === head || context.payload.pull_request.head.sha === head)\n              ) {\n                await setStatus('success', `Approved exact-head review by ${review.user.login}`);\n              }\n              return;\n            }\n            const comment = context.payload.comment;\n            if (!trusted.has(comment.author_association)) return;\n            const accept = (comment.body || '').trim().match(/^\\/substantive-review\\s+accept\\s+([0-9a-f]{40})\\s+(.+)$/i);\n            const reject = (comment.body || '').trim().match(/^\\/substantive-review\\s+reject\\s+([0-9a-f]{40})\\s+(.+)$/i);\n            const command = accept || reject;\n            if (!command) return;\n            const reviewedHead = command[1].toLowerCase();\n            if (reviewedHead !== head) {\n              core.setFailed(`Review attestation targets ${reviewedHead}, but current PR head is ${head}.`);\n              return;\n            }\n            if (accept) await setStatus('success', `External adversarial review accepted: ${accept[2].trim()}`);\n            else await setStatus('failure', `External adversarial review has blockers: ${reject[2].trim()}`);\n''',
)

# Codex closure becomes mandatory only on the assembled prototype candidate.
replace_once(
    ".github/workflows/codex-review-loop.yml",
    "    types: [opened, reopened, ready_for_review, synchronize]\n",
    "    types: [opened, reopened, ready_for_review, synchronize, labeled, unlabeled]\n",
)
replace_once(
    ".github/workflows/codex-review-loop.yml",
    "            const closureHistory = async () => {\n",
    "            const finalPrototypeReview = (pr.labels || []).some(\n"
    "              (label) => label.name === 'final-prototype-review',\n"
    "            );\n"
    "            if (!finalPrototypeReview) {\n"
    "              await setStatus('success', 'Intermediate slice: Codex closure not required');\n"
    "              return;\n"
    "            }\n\n"
    "            const closureHistory = async () => {\n",
)

# Product Runtime Acceptance watches the new policy files and refuses tracked/unignored test pollution.
replace_once(
    ".github/workflows/product-runtime-acceptance.yml",
    "      - \".github/workflows/product-runtime-acceptance.yml\"\n",
    "      - \".github/workflows/product-runtime-acceptance.yml\"\n"
    "      - \".github/workflows/delivery-integrity.yml\"\n"
    "      - \".github/workflows/installed-sqx-acceptance.yml\"\n"
    "      - \".github/workflows/substantive-review.yml\"\n"
    "      - \".github/workflows/codex-review-loop.yml\"\n"
    "      - \".github/pull_request_template.md\"\n"
    "      - \".gitignore\"\n"
    "      - \"LIVING_IMPLEMENTATION_PLAN.md\"\n"
    "      - \"AGENTS.md\"\n"
    "      - \"docs/product-backbone-spec-v1.md\"\n",
)
replace_once(
    ".github/workflows/product-runtime-acceptance.yml",
    "      - name: Stop TraderCockpit server\n        if: always()\n",
    "      - name: Verify acceptance did not dirty the source tree\n"
    "        if: always()\n"
    "        shell: bash\n"
    "        run: |\n"
    "          dirty=\"$(git status --porcelain --untracked-files=all)\"\n"
    "          if [ -n \"$dirty\" ]; then\n"
    "            echo \"Acceptance dirtied the repository:\" >&2\n"
    "            echo \"$dirty\" >&2\n"
    "            exit 1\n"
    "          fi\n"
    "      - name: Stop TraderCockpit server\n        if: always()\n",
)
replace_once(
    ".github/workflows/product-runtime-acceptance.yml",
    "      - name: Build frozen Windows desktop\n        run: python tools/build_windows_desktop.py\n",
    "      - name: Verify Windows tests did not dirty the source tree\n"
    "        shell: pwsh\n"
    "        run: |\n"
    "          $dirty = @(git status --porcelain --untracked-files=all)\n"
    "          if ($dirty.Count -gt 0) {\n"
    "            $dirty | ForEach-Object { Write-Error $_ }\n"
    "            throw \"Windows acceptance dirtied the repository\"\n"
    "          }\n"
    "      - name: Build frozen Windows desktop\n        run: python tools/build_windows_desktop.py\n",
)

print("delivery integrity bootstrap applied")
