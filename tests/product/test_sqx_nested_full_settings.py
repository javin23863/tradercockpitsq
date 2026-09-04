from __future__ import annotations

from functools import lru_cache
from hashlib import sha1
from io import BytesIO
from pathlib import Path
import subprocess
from tempfile import TemporaryDirectory
import unittest
from xml.etree import ElementTree
from zipfile import ZipFile

from tradercockpit.research_verdicts import native_condition_display_row, parse_native_conditions
from tradercockpit.sqx_custom_project import custom_project_topology_record, xml_node
from tradercockpit.sqx_custom_project_settings import update_custom_project_settings
from tradercockpit.sqx_custom_project import SqxCustomProjectTopologyError


RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_BUILDER_PROJECT_PATH = "references/strategyquant-x-144.2953/user/projects/Builder/project.cfx"
RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1 = "6194322a7a6feab40e02d9d9ed741401749a51d1"
RETAINED_BUILDER_PROJECT_SIZE = 47153
RETAINED_PROJECT_NAME = "RetainedBuildTask"


def _git_blob_sha1(value: bytes) -> str:
    return sha1(f"blob {len(value)}\0".encode("ascii") + value, usedforsecurity=False).hexdigest()


def _git(args: list[str], *, capture: bool = True) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(
        ["git", *args],
        check=False,
        capture_output=capture,
        cwd=Path(__file__).resolve().parents[2],
    )


@lru_cache(maxsize=1)
def retained_builder_archive() -> bytes:
    fetched = _git(["fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD])
    if fetched.returncode != 0:
        raise AssertionError(fetched.stderr.decode("utf-8", "replace") or "git fetch failed")
    shown = _git(["show", f"{RETAINED_REFERENCE_HEAD}:{RETAINED_BUILDER_PROJECT_PATH}"])
    if shown.returncode != 0 or not shown.stdout:
        raise AssertionError(shown.stderr.decode("utf-8", "replace") or "git show failed")
    archive = shown.stdout
    if len(archive) != RETAINED_BUILDER_PROJECT_SIZE or _git_blob_sha1(archive) != RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1:
        raise AssertionError(
            "retained SQX Builder archive identity mismatch: "
            f"expected {RETAINED_BUILDER_PROJECT_SIZE} bytes/{RETAINED_BUILDER_PROJECT_GIT_BLOB_SHA1}, "
            f"observed {len(archive)} bytes/{_git_blob_sha1(archive)}"
        )
    return archive


def _task_xml(archive: bytes) -> bytes:
    with ZipFile(BytesIO(archive)) as handle:
        return handle.read("Build-Task1.xml")


def _find(node: dict[str, object], tag: str) -> dict[str, object] | None:
    if node.get("tag") == tag:
        return node
    for child in node.get("children") or []:
        if isinstance(child, dict):
            found = _find(child, tag)
            if found is not None:
                return found
    return None


def _runtime(root: Path) -> Path:
    (root / "internal/web/SQUANT").mkdir(parents=True)
    (root / "internal/web/SQUANT/build.dat").write_text("2953", encoding="utf-8")
    (root / "internal/SQUANT.dat").write_bytes(b"144fixture")
    return root


def _install_retained_custom_project(home: Path) -> Path:
    path = home / "user" / "projects" / RETAINED_PROJECT_NAME / "project.cfx"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(retained_builder_archive())
    return path


class SqxNestedFullSettingsTests(unittest.TestCase):
    def test_retained_build_task_exposes_nested_rankings_and_cross_checks(self) -> None:
        root = ElementTree.fromstring(_task_xml(retained_builder_archive()))
        settings = root if root.tag.rsplit("}", 1)[-1] == "Settings" else root.find("Settings")
        self.assertIsNotNone(settings)
        rankings_el = next(child for child in settings if child.tag.rsplit("}", 1)[-1] == "Rankings")
        cross_el = next(child for child in settings if child.tag.rsplit("}", 1)[-1] == "CrossChecks")
        rankings = xml_node(rankings_el)
        cross_checks = xml_node(cross_el)

        conditions = parse_native_conditions(rankings)
        self.assertEqual(
            [(item["column"], item["comparator"], item["threshold"]) for item in conditions],
            [
                ("AvgTradesPerMonth", ">", 2.0),
                ("ProfitFactor", ">", 1.3),
                ("ReturnDDRatio", ">", 6.0),
                ("WinningPct", ">", 30.0),
                ("NumberOfTrades", ">", 50.0),
            ],
        )
        profit = next(
            child
            for child in _find(rankings, "Conditions")["children"]
            if child.get("display", {}).get("column") == "ProfitFactor"
        )
        self.assertEqual(profit["path"][-1], "Condition:2")
        display = native_condition_display_row(profit)
        self.assertEqual(display["label"], "ProfitFactor (in-sample) > 1.3")

        max_strategies = _find(rankings, "MaxStrategies")
        self.assertEqual(max_strategies["text"], "1000")

        wfo = _find(cross_checks, "WalkForwardOptimization")
        param1 = _find(wfo, "Param1")
        self.assertEqual(param1["attributes"]["value"], "20")
        max_tests = _find(wfo, "MaxTests")
        self.assertEqual(max_tests["text"], "1000")

        higher = _find(cross_checks, "RetestWithHigherPrecision")
        precision = _find(higher, "Precision")
        self.assertEqual(precision["text"], "2")

        what_if = _find(cross_checks, "WhatIf")
        self.assertEqual(what_if["attributes"].get("use"), "false")
        self.assertEqual(what_if["children"], [])

    def test_retained_nested_writes_and_refuses_invented_conditions(self) -> None:
        with TemporaryDirectory() as tmp:
            home = _runtime(Path(tmp))
            _install_retained_custom_project(home)
            written = update_custom_project_settings(
                home,
                RETAINED_PROJECT_NAME,
                1,
                [
                    {"path": ["Rankings", "MaxStrategies"], "text": "999"},
                    {
                        "path": ["Rankings", "Conditions", "Condition:2", "Right-Side", "Numeric-Value"],
                        "attribute": "value",
                        "value": "1.4",
                    },
                ],
            )
            record = custom_project_topology_record(home, RETAINED_PROJECT_NAME)
            with self.assertRaises(SqxCustomProjectTopologyError) as missing:
                update_custom_project_settings(
                    home,
                    RETAINED_PROJECT_NAME,
                    1,
                    [{"path": ["Rankings", "Conditions", "Condition:999"], "attribute": "use", "value": "true"}],
                )

        self.assertEqual(written["updated"], 2)
        rankings = next(item for item in record["tasks"][0]["settings"] if item["tag"] == "Rankings")
        self.assertEqual(_find(rankings, "MaxStrategies")["text"], "999")
        first = next(
            child
            for child in _find(rankings, "Conditions")["children"]
            if child.get("display", {}).get("column") == "ProfitFactor"
        )
        self.assertEqual(_find(first, "Numeric-Value")["attributes"]["value"], "1.4")
        self.assertEqual(missing.exception.code, "custom_project_settings_path_missing")
        names = [item["tag"] for item in record["tasks"][0]["settings"]]
        self.assertIn("Rankings", names)
        self.assertIn("CrossChecks", names)
        self.assertNotIn("GOLD", "".join(names))


if __name__ == "__main__":
    unittest.main()
