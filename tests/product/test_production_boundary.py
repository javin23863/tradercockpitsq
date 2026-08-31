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
    def test_current_product_tree_is_clean(self):
        root = Path(__file__).resolve().parents[2]
        self.assertEqual(checker.scan_product(root), [])

    def test_direct_reference_import_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("from references.vendor import thing\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].module, "references.vendor")

    def test_direct_sources_import_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("import sources.engine_core\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].module, "sources.engine_core")

    def test_legacy_futures_import_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("from futures.engine import run\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].module, "futures.engine")

    def test_stdlib_concurrent_futures_remains_allowed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "ok.py"
            target.write_text("from concurrent.futures import Future\n", encoding="utf-8")
            self.assertEqual(checker.scan_product(root), [])

    def test_duplicate_tradercockpit_builder_package_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "tradercockpit" / "builder"
            package.mkdir(parents=True)
            target = package / "evolution.py"
            target.write_text("VALUE = 1\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "path")
            self.assertEqual(violations[0].module, "tradercockpit/builder/evolution.py")

    def test_phase01_architecture_marker_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("LEGACY = 'phase01_intake'\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "marker")
            self.assertEqual(violations[0].module, "phase01_intake")

    def test_duplicate_builder_schema_marker_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("SCHEMA = 'tradercockpit.builder-strategy.v1'\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "marker")
            self.assertEqual(violations[0].module, "tradercockpit.builder-strategy.v1")

    def test_apollo_product_spine_marker_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("ASSISTANT = 'Apollo'\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "marker")
            self.assertEqual(violations[0].module, "Apollo")

    def test_copied_futures_repo_marker_is_rejected_without_import(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "product" / "example"
            package.mkdir(parents=True)
            target = package / "bad.py"
            target.write_text("SOURCE = 'javin23863/futures'\n", encoding="utf-8")
            violations = checker.scan_product(root)
            self.assertEqual(len(violations), 1)
            self.assertEqual(violations[0].kind, "marker")
            self.assertEqual(violations[0].module, "javin23863/futures")


if __name__ == "__main__":
    unittest.main()
