from __future__ import annotations

from io import BytesIO
import subprocess
import unittest
from xml.etree import ElementTree
from zipfile import ZipFile


# Secondary discovery evidence only. Production validity is defined by the
# user's current installed SQX project, never equality with these retained
# values or bytes.
RETAINED_REFERENCE_HEAD = "958e2fe2910cbf71d51ae29e4951484a86fc4ab6"
RETAINED_RETESTER_PROJECT = "references/strategyquant-x-144.2953/user/projects/Retester/project.cfx"


def local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def one(parent: ElementTree.Element, tag: str) -> ElementTree.Element:
    matches = [child for child in parent if local(child.tag) == tag]
    if len(matches) != 1:
        raise AssertionError(f"expected one direct {tag}, found {len(matches)}")
    return matches[0]


class RetainedMonteCarloTopologyTests(unittest.TestCase):
    @staticmethod
    def retained_task() -> ElementTree.Element:
        fetched = subprocess.run(
            ["git", "fetch", "--no-tags", "--depth=1", "origin", RETAINED_REFERENCE_HEAD],
            capture_output=True,
            check=False,
        )
        if fetched.returncode != 0:
            raise AssertionError(f"could not fetch retained SQX evidence: {fetched.stderr.decode(errors='replace')}")
        shown = subprocess.run(
            ["git", "show", f"{RETAINED_REFERENCE_HEAD}:{RETAINED_RETESTER_PROJECT}"],
            capture_output=True,
            check=False,
        )
        if shown.returncode != 0 or not shown.stdout:
            raise AssertionError(f"could not materialize retained Retester project: {shown.stderr.decode(errors='replace')}")
        with ZipFile(BytesIO(shown.stdout)) as archive:
            return ElementTree.fromstring(archive.read("Retest-Task1.xml"))

    def profile(self) -> ElementTree.Element:
        root = self.retained_task()
        cross_checks = [node for node in root.iter() if local(node.tag) == "CrossChecks"]
        self.assertEqual(len(cross_checks), 1)
        self.assertEqual(
            [local(child.tag) for child in cross_checks[0]],
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
        matches = [child for child in cross_checks[0] if local(child.tag) == "MonteCarloManipulation"]
        self.assertEqual(len(matches), 1)
        return matches[0]

    def test_observed_settings_and_methods(self) -> None:
        profile = self.profile()
        self.assertEqual(profile.attrib, {"use": "false"})
        self.assertEqual([local(child.tag) for child in profile], ["Settings", "AcceptanceSettings"])
        settings = one(profile, "Settings")
        self.assertEqual([local(child.tag) for child in settings], ["Methods", "NumberOfSimulations"])
        self.assertEqual((one(settings, "NumberOfSimulations").text or "").strip(), "30")

        methods = list(one(settings, "Methods"))
        self.assertEqual(len(methods), 2)
        self.assertEqual(methods[0].attrib, {"type": "RandomizeTradesOrder", "use": "true"})
        self.assertEqual(methods[1].attrib, {"type": "RandomlySkipTrades", "use": "true"})
        first = one(one(methods[0], "Params"), "Param")
        second = one(one(methods[1], "Params"), "Param")
        self.assertEqual((first.attrib, (first.text or "").strip()), ({"key": "Method", "type": "String"}, "resampling"))
        self.assertEqual((second.attrib, (second.text or "").strip()), ({"key": "Probability", "type": "Integer"}, "10"))

    def test_observed_acceptance_conditions(self) -> None:
        conditions = one(one(self.profile(), "AcceptanceSettings"), "Conditions")
        self.assertEqual(conditions.attrib, {"CrossCheck": "MonteCarloManipulation"})
        items = list(conditions)
        self.assertEqual([local(item.tag) for item in items], ["Condition", "Condition"])
        self.assertEqual([item.attrib for item in items], [{"use": "true"}, {"use": "true"}])

        expected = [
            (">=", "NetProfit", "Net profit", "Decimal2PL", "80", "1", "MonteCarloManipulation", "0", "50", "undefined", "main", "60"),
            ("<=", "DrawdownPct", "Max DD %", "Decimal2Pct", "80", "1", "MonteCarloManipulation", "0", "50", "undefined", "main", "175"),
        ]
        for condition, values in zip(items, expected, strict=True):
            comparator, metric, name, fmt, left_conf, left_market, left_result, left_pct, right_conf, right_market, right_result, right_pct = values
            self.assertEqual([local(child.tag) for child in condition], ["Left-Side", "Comparator", "Right-Side"])
            self.assertEqual(one(condition, "Comparator").attrib, {"value": comparator})
            left_side, right_side = one(condition, "Left-Side"), one(condition, "Right-Side")
            self.assertEqual(left_side.attrib, {"valueType": "column"})
            self.assertEqual(right_side.attrib, {"valueType": "column"})
            left, right = one(left_side, "Column-Value"), one(right_side, "Column-Value")
            common = {"class": metric, "column": metric, "columnType": "0", "direction": "0", "format": fmt, "name": name, "plType": "10", "subresult": "30"}
            self.assertEqual(left.attrib, {**common, "confidenceLevel": left_conf, "market": left_market, "pctRatio": left_pct, "resultType": left_result, "sampleType": "10"})
            self.assertEqual(right.attrib, {**common, "confidenceLevel": right_conf, "market": right_market, "pctRatio": right_pct, "resultType": right_result, "sampleType": "127"})


if __name__ == "__main__":
    unittest.main()
