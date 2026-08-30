from pathlib import Path
import tomllib
import unittest


class PackagingBoundaryTests(unittest.TestCase):
    def pyproject(self):
        root = Path(__file__).resolve().parents[2]
        return tomllib.loads((root / "pyproject.toml").read_text(encoding="utf-8"))

    def test_production_package_has_no_runtime_dependencies(self):
        project = self.pyproject()["project"]
        self.assertEqual(project.get("dependencies"), [])

    def test_only_product_namespace_is_packaged(self):
        config = self.pyproject()
        self.assertEqual(config["tool"]["setuptools"]["package-dir"], {"": "product"})
        finder = config["tool"]["setuptools"]["packages"]["find"]
        self.assertEqual(finder["where"], ["product"])
        self.assertEqual(finder["include"], ["tradercockpit*"])

    def test_legacy_and_reference_roots_are_not_packaging_inputs(self):
        text = (Path(__file__).resolve().parents[2] / "pyproject.toml").read_text(
            encoding="utf-8"
        )
        lowered = text.lower()
        self.assertNotIn("javin23863/futures", lowered)
        self.assertNotIn("sources/", lowered)
        self.assertNotIn("references/", lowered)


if __name__ == "__main__":
    unittest.main()
