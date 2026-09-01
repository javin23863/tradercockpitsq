from __future__ import annotations

from io import BytesIO
import subprocess
import unittest
from xml.etree import ElementTree
from zipfile import ZipFile


# Secondary discovery evidence only. These assertions document one observed
# SQX 144.2953 Retester project. Production validity is defined by the user's
# current installed SQX project, not byte/value equality with this archive.
RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_RETESTER_PROJECT = "references/strategyquant-x-144.2953/user/projects/Retester/project.cfx"


def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _children(node: ElementTree.Element, tag: str) -> list[ElementTree.Element]:
    return [child for child in node if _local(child.tag) == tag]


def _one(node: ElementTree.Element, tag: str) -> ElementTree.Element:
    matches = _children(node, tag)
    if len(matches) != 1:
        raise AssertionError(f"expected exactly one direct {tag}, found {len(matches)}")
    return matches[0]


class RetainedMonteCarloTopologyTests(unittest.TestCase):
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

    @classmethod
    def _profile(cls) -> ElementTree.Element:
        with ZipFile(BytesIO(cls._retained_project())) as archive:
            root = ElementTree.fromstring(archive.read("Retest-Task1.xml"))
        cross_checks = [node for node in root.iter() if _local(node.tag) == "CrossChecks"]
        if len(cross_checks) != 1:
            raise AssertionError(f"expected one CrossChecks section, found {len(cross_checks)}")
        profile_names = [_local(child.tag) for child in cross_checks[0]]
        cls.assertEqual(
            cls,
            profile_names,
            [
                "RetestOnAdditionalMarkets",
                "WalkForwardOptimization",
                "RetestWithHigherPrecision",
                "MonteCarloRetest",
                "WalkForwardMatrix",
                "MonteCarloManipulation",
                "OptProfileSysParamPermutation",
                "WhatIf",
                "SequentialOptimization",
            ],
        )
        profiles = _children(cross_checks[0], "MonteCarloManipulation")
        if len(profiles) != 1:
            raise AssertionError(f"expected one direct MonteCarloManipulation profile, found {len(profiles)}")
        return profiles[0]

    def test_retained_profile_has_observed_trade_manipulation_settings(self) -> None:
        profile = self._profile()
        self.assertEqual(profile.attrib, {"use": "false"})
        self.assertEqual([_local(child.tag) for child in profile], ["Settings", "AcceptanceSettings"])

        settings = _one(profile, "Settings")
        self.assertEqual([_local(child.tag) for child in settings], ["Methods", "NumberOfSimulations"])
        self.assertEqual((_one(settings, "NumberOfSimulations").text or "").strip(), "30")

        methods = _one(settings, "Methods")
        self.assertEqual([_local(child.tag) for child in methods], ["Method", "Method"])
        first, second = list(methods)
        self.assertEqual(first.attrib, {"type": "RandomizeTradesOrder", "use": "true"})
        self.assertEqual(second.attrib, {"type": "RandomlySkipTrades", "use": "true"})

        first_param = _one(_one(first, "Params"), "Param")
        self.assertEqual(first_param.attrib, {"key": "Method", "type": "String"})
        self.assertEqual((first_param.text or "").strip(), "resampling")

        second_param = _one(_one(second, "Params"), "Param")
        self.assertEqual(second_param.attrib, {"key": "Probability", "type": "Integer"})
        self.assertEqual((second_param.text or "").strip(), "10")

    def test_retained_profile_has_observed_native_acceptance_conditions(self) -> None:
        profile = self._profile()
        acceptance = _one(profile, "AcceptanceSettings")
        conditions = _one(acceptance, "Conditions")
        self.assertEqual(conditions.attrib, {"CrossCheck": "MonteCarloManipulation"})
        condition_nodes = _children(conditions, "Condition")
        self.assertEqual(len(condition_nodes), 2)
        self.assertEqual([node.attrib for node in condition_nodes], [{"use": "true"}, {"use": "true"}])

        expected = [
            (
                ">=",
                {
                    "class": "NetProfit", "column": "NetProfit", "columnType": "0", "confidenceLevel": "80",
                    "direction": "0", "format": "Decimal2PL", "market": "1", "name": "Net profit",
                    "pctRatio": "0", "plType": "10", "resultType": "MonteCarloManipulation",
                    "sampleType": "10", "subresult": "30",
                },
                {
                    "class": "NetProfit", "column": "NetProfit", "columnType": "0", "confidenceLevel": "50",
                    "direction": "0", "format": "Decimal2PL", "market": "undefined", "name": "Net profit",
                    "pctRatio": "60", "plType": "10", "resultType": "main", "sampleType": "127", "subresult": "30",
                },
            ),
            (
                "<=",
                {
                    "class": "DrawdownPct", "column": "DrawdownPct", "columnType": "0", "confidenceLevel": "80",
                    "direction": "0", "format": "Decimal2Pct", "market": "1", "name": "Max DD %",
                    "pctRatio": "0", "plType": "10", "resultType": "MonteCarloManipulation",
                    "sampleType": "10", "subresult": "30",
                },
                {
                    "class": "DrawdownPct", "column": "DrawdownPct", "columnType": "0", "confidenceLevel": "50",
                    "direction": "0", "format": "Decimal2Pct", "market": "undefined", "name": "Max DD %",
                    "pctRatio": "175", "plType": "10", "resultType": "main", "sampleType": "127", "subresult": "30",
                },
            ),
        ]
        for condition, (comparator, left_attrs, right_attrs) in zip(condition_nodes, expected, strict=True):
            self.assertEqual([_local(child.tag) for child in condition], ["Left-Side", "Comparator", "Right-Side"])
            self.assertEqual(_one(condition, "Comparator").attrib, {"value": comparator})
            left = _one(_one(condition, "Left-Side"), "Column-Value")
            right = _one(_one(condition, "Right-Side"), "Column-Value")
            self.assertEqual(_one(condition, "Left-Side").attrib, {"valueType": "column"})
            self.assertEqual(_one(condition, "Right-Side").attrib, {"valueType": "column"})
            self.assertEqual(left.attrib, left_attrs)
            self.assertEqual(right.attrib, right_attrs)


if __name__ == "__main__":
    unittest.main()
