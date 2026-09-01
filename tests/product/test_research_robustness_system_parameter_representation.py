from __future__ import annotations

from io import BytesIO
import unittest
from zipfile import ZipFile

from tradercockpit.research_robustness import ResearchRobustnessError
from tradercockpit.research_robustness_system_parameter import compile_system_parameter_permutation_project


class SystemParameterPermutationRepresentationTests(unittest.TestCase):
    @staticmethod
    def _project(max_tests: str) -> bytes:
        task = (
            "<Settings><CrossChecks>"
            '<OptProfileSysParamPermutation use="false"><Settings>'
            "<OptimPeriods>false</OptimPeriods>"
            "<OptimExitTypes>false</OptimExitTypes>"
            f"<MaxTests>{max_tests}</MaxTests>"
            "</Settings></OptProfileSysParamPermutation>"
            '<RetestWithHigherPrecision use="false"><Settings><Precision>2</Precision><Spread>3</Spread></Settings></RetestWithHigherPrecision>'
            "</CrossChecks></Settings>"
        ).encode()
        config = (
            '<Project name="Retester" version="144.2953">'
            '<Tasks><Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks>'
            "</Project>"
        ).encode()
        stream = BytesIO()
        with ZipFile(stream, "w") as archive:
            archive.writestr("config.xml", config)
            archive.writestr("Retest-Task1.xml", task)
        return stream.getvalue()

    def test_positive_ascii_decimal_max_tests_is_preserved(self) -> None:
        _, plan = compile_system_parameter_permutation_project(self._project("17"))
        self.assertEqual(plan["native_settings"]["MaxTests"], "17")

    def test_noncanonical_or_nonpositive_max_tests_fails_closed(self) -> None:
        for value in ("+1", "-1", "0", "1.0", "abc", "١"):
            with self.subTest(value=value):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    compile_system_parameter_permutation_project(self._project(value))
                self.assertEqual(caught.exception.code, "robustness_system_parameter_invalid")


if __name__ == "__main__":
    unittest.main()
