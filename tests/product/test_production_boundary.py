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

    def test_bounded_assistant_identity_is_allowed(self):
        # The Assistant card keeps the Apollo identity; only the persistent spine/surface
        # architecture is prohibited.
        self.assertEqual(self._violations_for('ASSISTANT_IDENTITY = "Apollo"\n', "tradercockpit/assistant.py"), [])

    def test_superseded_architecture_markers_are_rejected(self):
        for marker in (
            "phase01_intake",
            "tradercockpit.builder-strategy.v1",
            "javin23863/futures",
            "APOLLO_SURFACE_ID",
            "apollo-persistent",
            "apollo-dock",
            "apollo_spine",
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

    def test_web_tree_rejects_secrets_store_imports(self):
        root = Path(__file__).resolve().parents[2]
        self.assertEqual(checker.scan_web(root), [])

    def test_web_secrets_store_marker_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "web" / "bad.mjs"
            target.parent.mkdir(parents=True)
            target.write_text('import { x } from "../product/tradercockpit/secrets_store.py";\n', encoding="utf-8")
            violations = checker.scan_web(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "web_marker")
            self.assertEqual(violations[0].module, "secrets_store")

    def test_read_only_preset_catalog_may_keep_exact_custody_hashes(self):
        digest = "b" * 64
        self.assertEqual(
            self._violations_for(
                f"PRESET_SHA256 = {digest!r}\n",
                "tradercockpit/sqx_presets.py",
            ),
            [],
        )


if __name__ == "__main__":
    unittest.main()
