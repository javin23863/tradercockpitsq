import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
CHECKER_PATH = ROOT / "tools" / "check_production_boundary.py"
spec = importlib.util.spec_from_file_location("check_production_boundary", CHECKER_PATH)
checker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = checker
assert spec.loader is not None
spec.loader.exec_module(checker)


class ProductionBoundaryTests(unittest.TestCase):
    def _violations_for(self, source: str, relative: str = "example/bad.py"):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "product" / relative
            target.parent.mkdir(parents=True)
            target.write_text(source, encoding="utf-8")
            return checker.scan_product(root)

    def test_current_product_tree_is_clean(self):
        self.assertEqual(checker.scan_product(ROOT), [])

    def test_reference_source_and_legacy_futures_imports_are_rejected(self):
        for source, module in (
            ("from references.vendor import thing\n", "references.vendor"),
            ("import sources.engine_core\n", "sources.engine_core"),
            ("from futures.engine import run\n", "futures.engine"),
        ):
            with self.subTest(module=module):
                violations = self._violations_for(source)
                self.assertEqual(len(violations), 1)
                self.assertEqual(violations[0].module, module)

    def test_stdlib_concurrent_futures_remains_allowed(self):
        self.assertEqual(self._violations_for("from concurrent.futures import Future\n"), [])

    def test_duplicate_builder_and_generic_engine_paths_are_rejected(self):
        for relative in (
            "tradercockpit/builder/evolution.py",
            "tradercockpit/engine/evaluator.py",
        ):
            with self.subTest(relative=relative):
                violations = self._violations_for("VALUE = 1\n", relative)
                self.assertEqual(len(violations), 1)
                self.assertEqual(violations[0].kind, "path")
                self.assertEqual(violations[0].module, relative)

    def test_superseded_architecture_markers_are_rejected(self):
        for marker in (
            "phase01_intake",
            "tradercockpit.builder-strategy.v1",
            "javin23863/futures",
            "Apollo",
            "StrategySpecV1",
            "BacktestEvaluatorV1",
            "BacktestRunSpecV1",
            "evaluator_not_bound",
            "tradercockpit.engine",
        ):
            with self.subTest(marker=marker):
                violations = self._violations_for(f"VALUE = {marker!r}\n")
                self.assertEqual(len(violations), 1)
                self.assertEqual(violations[0].kind, "marker")
                self.assertEqual(violations[0].module, marker)

    def test_archived_native_identity_cannot_become_a_production_validity_oracle(self):
        for marker in (
            "SQX_RETAINED_BUILDER_PROJECT",
            "retained_native_reference",
            "exact_retained_git_blob_identity",
            "retained_native_validation_evidence_required",
            "RETESTER_ENGINE_SHA256",
        ):
            with self.subTest(marker=marker):
                violations = self._violations_for(
                    f"VALUE = {marker!r}\n",
                    "tradercockpit/research_configurations.py",
                )
                self.assertEqual(len(violations), 1)
                self.assertEqual(violations[0].kind, "marker")
                self.assertEqual(violations[0].module, marker)

    def test_renamed_hard_coded_native_digest_is_rejected_by_ast_policy(self):
        digest = "a" * 64
        for relative, name in (
            ("tradercockpit/research_configurations.py", "BUILDER_GOLDEN_SHA256"),
            ("tradercockpit/research_retester.py", "TRUSTED_RETESTER_JAR_DIGEST"),
            ("tradercockpit/sqx_gateway.py", "NATIVE_ARTIFACT_IDENTITY"),
        ):
            with self.subTest(relative=relative, name=name):
                violations = self._violations_for(f"{name} = {digest!r}\n", relative)
                self.assertEqual(len(violations), 1)
                self.assertEqual(violations[0].kind, "native_digest_literal")
                self.assertEqual(violations[0].module, name)

    def test_read_only_preset_catalog_may_keep_exact_custody_hashes(self):
        digest = "b" * 64
        self.assertEqual(
            self._violations_for(
                f"PRESET_SHA256 = {digest!r}\n",
                "tradercockpit/sqx_presets.py",
            ),
            [],
        )

    def test_unapproved_module_cannot_import_native_gateway_under_any_name(self):
        for source in (
            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\n",
            "from tradercockpit.sqx_gateway import SqxNativeGatewayError as Error\n",
            "import tradercockpit.sqx_gateway as gateway\n",
            "from tradercockpit import sqx_gateway\n",
        ):
            with self.subTest(source=source):
                violations = self._violations_for(
                    source,
                    "tradercockpit/crosscheck_higher_precision.py",
                )
                self.assertTrue(
                    any(item.kind == "native_gateway_owner" for item in violations)
                )

    def test_unapproved_module_cannot_invoke_native_launch_methods(self):
        for method in ("launch_builder", "launch_retester_task"):
            with self.subTest(method=method):
                violations = self._violations_for(
                    f"def run(gateway):\n    return gateway.{method}('x')\n",
                    "tradercockpit/native_method_adapter.py",
                )
                self.assertTrue(
                    any(
                        item.kind == "native_gateway_owner" and item.module == method
                        for item in violations
                    )
                )

    def test_gateway_owner_allowlist_is_exact_product_relative_path(self):
        source = (
            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\n"
            "def run(gateway):\n    return gateway.launch_retester_task('x')\n"
        )
        for relative in (
            "tradercockpit/sqx_gateway.py",
            "tradercockpit/research_native_jobs.py",
            "tradercockpit/research_retester.py",
            "tradercockpit/research_robustness.py",
        ):
            with self.subTest(relative=relative):
                violations = self._violations_for(source, relative)
                self.assertFalse(
                    any(item.kind == "native_gateway_owner" for item in violations)
                )
        shadow = self._violations_for(
            source,
            "shadow/tradercockpit/research_retester.py",
        )
        self.assertTrue(any(item.kind == "native_gateway_owner" for item in shadow))

    def test_unapproved_module_cannot_bypass_gateway_with_subprocess_launch(self):
        for source in (
            "import subprocess\nsubprocess.run(['sqx'])\n",
            "import subprocess as sp\nsp.Popen(['sqx'])\n",
            "from subprocess import check_call as launch\nlaunch(['sqx'])\n",
        ):
            with self.subTest(source=source):
                violations = self._violations_for(
                    source,
                    "tradercockpit/native_method_adapter.py",
                )
                self.assertTrue(
                    any(
                        item.kind == "native_gateway_owner"
                        and item.module.startswith("subprocess.")
                        for item in violations
                    )
                )
        self.assertEqual(
            self._violations_for(
                "import subprocess\nVALUE = subprocess.TimeoutExpired\n",
                "tradercockpit/desktop_lifecycle.py",
            ),
            [],
        )

    def test_delivery_authority_workflows_do_not_execute_pr_code(self):
        for name in (
            "delivery-integrity.yml",
            "installed-sqx-acceptance.yml",
            "substantive-review.yml",
            "codex-review-loop.yml",
        ):
            with self.subTest(name=name):
                text = (ROOT / ".github" / "workflows" / name).read_text(encoding="utf-8")
                self.assertIn("pull_request_target:", text)
                self.assertNotIn("\n  pull_request:\n", text)
                self.assertNotIn("actions/checkout", text)

    def test_delivery_queue_marker_and_parser_are_the_same_authority(self):
        plan = (ROOT / "LIVING_IMPLEMENTATION_PLAN.md").read_text(encoding="utf-8")
        workflow = (ROOT / ".github" / "workflows" / "delivery-integrity.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("delivery-queue:", plan)
        self.assertIn(
            "installed-sqx-required-items: workflow-correction-integrity-audit",
            plan,
        )
        self.assertIn("delivery-queue", workflow)
        self.assertIn("installed-sqx-required-items", workflow)
        self.assertIn("queueField === 'none'", workflow)
        self.assertIn("nativeField === 'none'", workflow)
        self.assertIn("installed-SQX-required items are outside delivery queue", workflow)
        self.assertIn("removing only the completed active item", workflow)
        self.assertIn("Governance changes must preserve one parseable delivery-integrity gate", workflow)
        self.assertNotIn("allowed-plan-items", workflow)
        self.assertIn("pull_request_target:", workflow)
        self.assertIn("branches: [main]", workflow)

    def test_native_acceptance_is_explicitly_operator_attested_and_boundary_triggered(self):
        delivery = (ROOT / ".github" / "workflows" / "delivery-integrity.yml").read_text(
            encoding="utf-8"
        )
        workflow = (
            ROOT / ".github" / "workflows" / "installed-sqx-acceptance.yml"
        ).read_text(encoding="utf-8")
        self.assertIn("receipt=([0-9a-f]{64})", workflow)
        self.assertIn("scenario=", workflow)
        self.assertIn("value !== 'none'", workflow)
        self.assertIn("nativeBoundaryChanged", workflow)
        self.assertIn("declaredRequired", workflow)
        self.assertIn("nativeBoundaryChanged", delivery)
        self.assertIn("Native integration boundary changed", delivery)
        self.assertIn("Operator-attested installed SQX receipt", workflow)

    def test_substantive_review_consumes_exact_head_github_reviews(self):
        workflow = (ROOT / ".github" / "workflows" / "substantive-review.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("pull_request_review:", workflow)
        self.assertIn("review.commit_id !== head", workflow)
        self.assertIn("Exact-head GitHub review approved", workflow)
        self.assertIn("Exact-head Codex review has findings", workflow)

    def test_codex_closure_is_reserved_for_final_prototype(self):
        workflow = (ROOT / ".github" / "workflows" / "codex-review-loop.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("final-prototype-review", workflow)
        self.assertIn("pull_request_review:", workflow)
        self.assertIn("review.commit_id !== head", workflow)
        self.assertIn("Codex closure reserved for final prototype review", workflow)
        self.assertIn("Unresolved Codex inline feedback", workflow)

    def test_product_runtime_acceptance_rejects_repository_dirt(self):
        workflow = (
            ROOT / ".github" / "workflows" / "product-runtime-acceptance.yml"
        ).read_text(encoding="utf-8")
        self.assertGreaterEqual(workflow.count("git status --porcelain --untracked-files=all"), 2)
        self.assertIn("--package-lock=false", workflow)


if __name__ == "__main__":
    unittest.main()
