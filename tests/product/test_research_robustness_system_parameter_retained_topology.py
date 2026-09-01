from __future__ import annotations

from io import BytesIO
from pathlib import Path
import subprocess
import unittest
from xml.etree import ElementTree
from zipfile import ZipFile


# Secondary structural evidence only. Production validity remains defined by the
# user's current installed Retester project, not equality to this retained blob.
RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_RETESTER_PROJECT = "references/strategyquant-x-144.2953/user/projects/Retester/project.cfx"


class RetainedSystemParameterPermutationTopologyTests(unittest.TestCase):
    @staticmethod
    def _retained_project() -> bytes:
        fetched = subprocess.run(
            ["git", "fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD],
            capture_output=True,
            check=False,
        )
        if fetched.returncode != 0:
            raise AssertionError(f"could not fetch retained SQX evidence commit: {fetched.stderr.decode(errors='replace')}")
        shown = subprocess.run(
            ["git", "show", f"{RETAINED_REFERENCE_HEAD}:{RETAINED_RETESTER_PROJECT}"],
            capture_output=True,
            check=False,
        )
        if shown.returncode != 0 or not shown.stdout:
            raise AssertionError(f"could not materialize retained Retester project: {shown.stderr.decode(errors='replace')}")
        return shown.stdout

    def test_retained_retester_places_system_parameter_profile_under_crosschecks(self) -> None:
        project = self._retained_project()
        with ZipFile(BytesIO(project)) as archive:
            task = ElementTree.fromstring(archive.read("Retest-Task1.xml"))

        cross_checks = [node for node in task.iter() if node.tag.rsplit("}", 1)[-1] == "CrossChecks"]
        self.assertEqual(len(cross_checks), 1)
        direct_profiles = [
            child for child in cross_checks[0]
            if child.tag.rsplit("}", 1)[-1] == "OptProfileSysParamPermutation"
        ]
        self.assertEqual(len(direct_profiles), 1)
        profile = direct_profiles[0]
        self.assertIn(profile.attrib.get("use"), {"true", "false"})

        settings = [child for child in profile if child.tag.rsplit("}", 1)[-1] == "Settings"]
        self.assertEqual(len(settings), 1)
        names = [child.tag.rsplit("}", 1)[-1] for child in settings[0]]
        self.assertEqual(names.count("OptimPeriods"), 1)
        self.assertEqual(names.count("OptimExitTypes"), 1)
        self.assertEqual(names.count("MaxTests"), 1)


if __name__ == "__main__":
    unittest.main()
