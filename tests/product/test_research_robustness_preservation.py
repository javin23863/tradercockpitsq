from __future__ import annotations

from io import BytesIO
import unittest
from zipfile import ZipFile

from tradercockpit.research_robustness import compile_higher_precision_project


class ResearchRobustnessPreservationTests(unittest.TestCase):
    @staticmethod
    def _project_bytes() -> bytes:
        stream = BytesIO()
        config = (
            '<Project name="Retester" version="144.2953">'
            '<Tasks><Task type="Retest" name="Retest" active="true" taskXMLFile="Retest-Task1.xml"/></Tasks>'
            '</Project>'
        ).encode()
        # Intentionally retain whitespace/comment/noncanonical formatting. The
        # compiler must not reserialize a producer-valid profile that is already
        # enabled for the requested native method.
        task = b'''<?xml version="1.0" encoding="UTF-8"?>
<Settings>
  <!-- keep exact native bytes -->
  <CrossChecks>
    <RetestWithHigherPrecision use="true">
      <Settings><Precision>2</Precision><Spread>3</Spread></Settings>
      <AcceptanceSettings />
    </RetestWithHigherPrecision>
    <MonteCarloManipulation use="false"><Settings /></MonteCarloManipulation>
  </CrossChecks>
</Settings>
'''
        with ZipFile(stream, "w") as archive:
            archive.comment = b"native-project-comment"
            archive.writestr("config.xml", config)
            archive.writestr("Retest-Task1.xml", task)
            archive.writestr("opaque-native-member.bin", b"opaque\x00native\xffbytes")
        return stream.getvalue()

    def test_enabled_profile_returns_exact_source_project_bytes(self) -> None:
        source = self._project_bytes()
        compiled, plan = compile_higher_precision_project(source)

        self.assertEqual(compiled, source)
        self.assertIs(plan["configuration_changed"], False)
        self.assertEqual(plan["source_project_sha256"], plan["compiled_project_sha256"])
        self.assertEqual(plan["source_task_sha256"], plan["compiled_task_sha256"])
        self.assertEqual(plan["native_settings"], {"Precision": "2", "Spread": "3"})


if __name__ == "__main__":
    unittest.main()
