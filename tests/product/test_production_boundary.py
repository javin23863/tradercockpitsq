import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


CHECKER_PATH = Path(__file__).resolve().parents[2] / "tools" / "check_production_boundary.py"
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
        root = Path(__file__).resolve().parents[2]
        self.assertEqual(checker.scan_product(root), [])

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


    def test_method_specific_robustness_module_cannot_import_native_gateway(self):
        violations = self._violations_for(
            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\n",
            "tradercockpit/research_robustness_monte_carlo.py",
        )
        self.assertTrue(any(item.kind == "robustness_method_executor" for item in violations))

    def test_method_specific_robustness_module_cannot_launch_retester(self):
        violations = self._violations_for(
            "def run(gateway):\n    return gateway.launch_retester_task('x')\n",
            "tradercockpit/research_robustness_system_parameter.py",
        )
        self.assertTrue(any(item.module == "launch_retester_task" for item in violations))

    def test_common_robustness_module_may_own_native_execution(self):
        violations = self._violations_for(
            "from tradercockpit.sqx_gateway import SqxNativeControlGateway\n"
            "def run(gateway):\n    return gateway.launch_retester_task('x')\n",
            "tradercockpit/research_robustness.py",
        )
        self.assertFalse(any(item.kind == "robustness_method_executor" for item in violations))


if __name__ == "__main__":
    unittest.main()
