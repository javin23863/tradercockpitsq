from __future__ import annotations

from io import BytesIO
import subprocess
import unittest
from xml.etree import ElementTree
from zipfile import ZipFile


# Secondary discovery evidence only. This test never makes retained byte
# identity a production-validity requirement; the installed SQX project remains
# the executable runtime specification.
RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_RETESTER_PROJECT = "references/strategyquant-x-144.2953/user/projects/Retester/project.cfx"


def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


class RetainedMonteCarloProfileProbeTests(unittest.TestCase):
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

    def test_probe_monte_carlo_manipulation_native_subtree(self) -> None:
        with ZipFile(BytesIO(self._retained_project())) as archive:
            root = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
        cross_checks = [node for node in root.iter() if _local(node.tag) == "CrossChecks"]
        self.assertEqual(len(cross_checks), 1)
        profile_names = [_local(child.tag) for child in cross_checks[0]]
        profiles = [child for child in cross_checks[0] if _local(child.tag) == "MonteCarloManipulation"]
        self.assertEqual(
            len(profiles),
            1,
            f"retained direct CrossChecks profiles: {profile_names!r}",
        )

        def emit(node: ElementTree.Element, path: str) -> None:
            name = _local(node.tag)
            text = (node.text or "").strip()
            print(f"SQX_MC_NODE path={path}/{name} attrs={dict(node.attrib)!r} text={text!r}")
            for child in node:
                emit(child, f"{path}/{name}")

        emit(profiles[0], "CrossChecks")


if __name__ == "__main__":
    unittest.main()
