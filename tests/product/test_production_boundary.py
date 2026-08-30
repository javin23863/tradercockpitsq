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


if __name__ == "__main__":
    unittest.main()
