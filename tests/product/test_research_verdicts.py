from __future__ import annotations

from pathlib import Path
import random
from datetime import datetime, timezone
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_retester_http import historical_results_response
from tradercockpit.research_verdicts import (
    COCKPIT_VERDICT_SCHEMA,
    DEFAULT_VERDICT_POLICY,
    SAMPLE_FULL,
    SAMPLE_IN_SAMPLE,
    SAMPLE_OUT_OF_SAMPLE,
    VERDICT_POLICY_ENV,
    cockpit_verdict,
    evaluate_native_conditions,
    monte_carlo_trade_manipulation,
    native_chart_history_ms,
    native_task_sections,
    parse_native_conditions,
    select_additional_market_trades,
    select_sample,
    sqx_statistics,
    verdict_policy,
)


DAY = 86_400_000
START = 1_600_000_000_000


def _trade(index: int, pl: float, *, sample_type: int = SAMPLE_IN_SAMPLE, day_step: int = 7) -> dict[str, object]:
    open_time = START + index * day_step * DAY
    return {
        "PL": pl,
        "SampleType": sample_type,
        "OpenTime": open_time,
        "CloseTime": open_time + DAY,
        "Type": 1,
    }


def _profitable_series(count: int, *, sample_type: int = SAMPLE_IN_SAMPLE, offset: int = 0) -> list[dict[str, object]]:
    # Repeating +120 / +80 / -60 keeps every calendar quarter and year profitable.
    pattern = (120.0, 80.0, -60.0)
    return [_trade(offset + index, pattern[index % 3], sample_type=sample_type) for index in range(count)]


def _seeded_series(seed: int, count: int, *, sample_type: int, offset: int = 0) -> list[dict[str, object]]:
    # Deterministic pseudo-random edge (62% winners) with irregular sequencing so the
    # observed drawdown is representative for the Monte Carlo resample.
    rng = random.Random(seed)
    trades = []
    for index in range(count):
        pl = rng.uniform(90, 220) if rng.random() < 0.62 else -rng.uniform(60, 150)
        trades.append(_trade(offset + index, round(pl, 2), sample_type=sample_type, day_step=9))
    return trades


def _deployable_trades() -> list[dict[str, object]]:
    return _seeded_series(1, 90, sample_type=SAMPLE_IN_SAMPLE) + _seeded_series(101, 45, sample_type=SAMPLE_OUT_OF_SAMPLE, offset=90)


_TASK_XML = b"""<Task>
  <Rankings type="never">
    <Conditions>
      <Condition use="true"><Left-Side><Column-Value column="ProfitFactor" sampleType="10" direction="0" confidenceLevel="50"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="1.3"/></Right-Side></Condition>
      <Condition use="true"><Left-Side><Column-Value column="NumberOfTrades" sampleType="20" direction="0" confidenceLevel="50"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="5"/></Right-Side></Condition>
      <Condition use="false"><Left-Side><Column-Value column="NetProfit" sampleType="127"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="999999"/></Right-Side></Condition>
      <Condition use="true"><Left-Side><Column-Value column="WFPctOfProfitableRuns" sampleType="127"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="70"/></Right-Side></Condition>
    </Conditions>
  </Rankings>
  <CrossChecks use="true">
    <RetestWithHigherPrecision use="true">
      <Settings><Precision>2</Precision></Settings>
      <AcceptanceSettings><Conditions>
        <Condition use="true"><Left-Side><Column-Value column="ProfitFactor" sampleType="127"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="1.3"/></Right-Side></Condition>
      </Conditions></AcceptanceSettings>
    </RetestWithHigherPrecision>
    <RetestOnAdditionalMarkets use="true">
      <Settings/>
      <AcceptanceSettings><Conditions>
        <Condition use="true"><Left-Side><Column-Value column="ProfitFactor" sampleType="127"/></Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="1.1"/></Right-Side></Condition>
      </Conditions></AcceptanceSettings>
    </RetestOnAdditionalMarkets>
    <MonteCarloManipulation use="false">
      <AcceptanceSettings><Conditions>
        <Condition use="true"><Left-Side><Column-Value column="NetProfit" confidenceLevel="80"/></Left-Side><Comparator value=">="/><Right-Side><Column-Value column="NetProfit" confidenceLevel="50"/></Right-Side></Condition>
      </Conditions></AcceptanceSettings>
    </MonteCarloManipulation>
  </CrossChecks>
  <MoneyManagement><InitialCapital>25000</InitialCapital></MoneyManagement>
</Task>"""


class SqxStatisticsTests(unittest.TestCase):
    def test_columns_follow_published_sqx_formulas(self):
        trades = [_trade(0, 100.0), _trade(1, -50.0), _trade(2, 30.0), _trade(3, -20.0)]
        stats = sqx_statistics(trades, initial_capital=10000)
        self.assertEqual(stats["NumberOfTrades"], 4)
        self.assertEqual(stats["NetProfit"], 60.0)
        self.assertEqual(stats["GrossProfit"], 130.0)
        self.assertEqual(stats["GrossLoss"], 70.0)
        self.assertEqual(stats["WinningPct"], 50.0)
        self.assertEqual(stats["ProfitFactor"], 1.86)
        # Equity walk: 10100 → 10050 (dd 50) → 10080 (dd 20) → 10060 (dd 40); max DD 50.
        self.assertEqual(stats["Drawdown"], 50.0)
        self.assertEqual(stats["ReturnDDRatio"], 1.2)
        self.assertEqual(stats["Expectancy"], 15.0)
        self.assertEqual(stats["MaxConsecLosses"], 1)
        self.assertEqual(stats["final_equity"], 10060.0)
        self.assertEqual(stats["months_basis"], "traded_span")

    def test_avg_trades_per_month_uses_native_chart_history_when_present(self):
        trades = [_trade(0, 10.0), _trade(1, -5.0)]
        chart_from, chart_to = native_chart_history_ms(
            b'<Settings><Data><Setups>'
            b'<Setup dateFrom="2020.01.01" dateTo="2022.01.01"><Chart symbol="ES" timeframe="H1"/></Setup>'
            b'</Setups></Data></Settings>'
        )
        self.assertEqual(chart_from, int(datetime(2020, 1, 1, tzinfo=timezone.utc).timestamp() * 1000))
        traded = sqx_statistics(trades, initial_capital=10000)
        chart = sqx_statistics(trades, initial_capital=10000, chart_from_ms=chart_from, chart_to_ms=chart_to)
        self.assertEqual(traded["months_basis"], "traded_span")
        self.assertEqual(chart["months_basis"], "native_chart_history")
        self.assertGreater(chart["TotalDataDays"], traded["TotalDataDays"])
        self.assertLess(chart["AvgTradesPerMonth"], traded["AvgTradesPerMonth"])

    def test_special_cases_match_sqx(self):
        self.assertEqual(sqx_statistics([])["ProfitFactor"], 0.0)
        winners = [_trade(0, 10.0), _trade(1, 20.0)]
        stats = sqx_statistics(winners)
        self.assertEqual(stats["ProfitFactor"], 5.0)
        self.assertEqual(stats["Drawdown"], 0.0)
        self.assertEqual(stats["ReturnDDRatio"], 10.0)

    def test_sample_selection_uses_native_sample_type_families(self):
        trades = [_trade(0, 1.0, sample_type=10), _trade(1, 1.0, sample_type=11), _trade(2, 1.0, sample_type=20), _trade(3, 1.0, sample_type=25)]
        self.assertEqual(len(select_sample(trades, SAMPLE_FULL)), 4)
        self.assertEqual(len(select_sample(trades, SAMPLE_IN_SAMPLE)), 2)
        self.assertEqual(len(select_sample(trades, SAMPLE_OUT_OF_SAMPLE)), 2)


class NativeConditionTests(unittest.TestCase):
    def test_parse_and_evaluate_exact_native_conditions(self):
        sections = native_task_sections(_TASK_XML)
        conditions = parse_native_conditions(sections["rankings"])
        self.assertEqual([condition["column"] for condition in conditions], ["ProfitFactor", "NumberOfTrades", "WFPctOfProfitableRuns"])
        self.assertEqual(conditions[1]["sample_type"], 20)

        trades = _profitable_series(9) + _profitable_series(6, sample_type=SAMPLE_OUT_OF_SAMPLE, offset=9)
        checks = evaluate_native_conditions(conditions, trades, initial_capital=25000)
        self.assertEqual([check["state"] for check in checks], ["pass", "pass", "unevaluated"])
        self.assertEqual(checks[0]["sample"], "in-sample")
        self.assertEqual(checks[1]["value"], 6)
        self.assertIn("native producer run", checks[2]["detail"])

        unused = [{
            "result_key": "Main: DJ_M1_dukas/H1",
            "sample": SAMPLE_FULL,
            "direction": 0,
            "confidence_level": 50,
            "columns": {"WFPctOfProfitableRuns": 0.0},
        }]
        unused_checks = evaluate_native_conditions(conditions, trades, initial_capital=25000, native_columns=unused)
        self.assertEqual([check["state"] for check in unused_checks], ["pass", "pass", "unevaluated"])

        databank = [{
            "result_key": "CrossCheck_WalkForwardOptimization",
            "sample": SAMPLE_FULL,
            "direction": 0,
            "confidence_level": 50,
            "columns": {"WFPctOfProfitableRuns": 82.5},
        }]
        produced = evaluate_native_conditions(conditions, trades, initial_capital=25000, native_columns=databank)
        self.assertEqual([check["state"] for check in produced], ["pass", "pass", "pass"])
        self.assertEqual(produced[2]["value"], 82.5)

        monte_carlo_node = next(child for child in sections["cross_checks"]["children"] if child["tag"] == "MonteCarloManipulation")
        monte_carlo = parse_native_conditions(monte_carlo_node)
        mc_checks = evaluate_native_conditions(monte_carlo, trades, initial_capital=25000)
        self.assertEqual(mc_checks[0]["state"], "unevaluated")
        cl_rows = [
            {"result_key": "Portfolio", "sample": SAMPLE_FULL, "direction": 0, "confidence_level": 50, "columns": {"NetProfit": 1000.0}},
            {"result_key": "CrossCheck_MonteCarloManipulation", "sample": SAMPLE_FULL, "direction": 0, "confidence_level": 50, "columns": {"NetProfit": 1100.0}},
        ]
        missing_cl = evaluate_native_conditions(monte_carlo, trades, initial_capital=25000, native_columns=cl_rows)
        self.assertEqual(missing_cl[0]["state"], "unevaluated")
        cl_rows_exact = [
            {"result_key": "Portfolio", "sample": SAMPLE_FULL, "direction": 0, "confidence_level": 50, "columns": {"NetProfit": 1000.0}},
            {"result_key": "CrossCheck_MonteCarloManipulation", "sample": SAMPLE_FULL, "direction": 0, "confidence_level": 80, "columns": {"NetProfit": 1100.0}},
        ]
        cl_checks = evaluate_native_conditions(monte_carlo, trades, initial_capital=25000, native_columns=cl_rows_exact)
        self.assertEqual(cl_checks[0]["state"], "pass")
        self.assertEqual(cl_checks[0]["value"], 1100.0)
        self.assertEqual(cl_checks[0]["threshold"], 1000.0)

    def test_monte_carlo_is_deterministic_for_one_seed(self):
        trades = _profitable_series(30)
        first = monte_carlo_trade_manipulation(trades, initial_capital=10000, simulations=50, skip_trades_pct=10, seed=42)
        second = monte_carlo_trade_manipulation(trades, initial_capital=10000, simulations=50, skip_trades_pct=10, seed=42)
        self.assertEqual(first, second)
        self.assertEqual(first["simulations"], 50)
        self.assertGreater(first["net_profit_p5"], 0)


class VerdictPolicyTests(unittest.TestCase):
    def test_defaults_and_environment_override(self):
        policy = verdict_policy({})
        self.assertEqual(policy["values"], DEFAULT_VERDICT_POLICY)
        self.assertEqual(policy["source"], "default")

        override = verdict_policy({VERDICT_POLICY_ENV: '{"oos_min_trades": 3, "unknown": 1, "stress_simulations": -5}'})
        self.assertEqual(override["values"]["oos_min_trades"], 3)
        self.assertEqual(override["values"]["stress_simulations"], DEFAULT_VERDICT_POLICY["stress_simulations"])
        self.assertEqual(override["source"], "environment")
        self.assertEqual(len(override["warnings"]), 2)

        broken = verdict_policy({VERDICT_POLICY_ENV: "not json"})
        self.assertEqual(broken["values"], DEFAULT_VERDICT_POLICY)
        self.assertTrue(broken["warnings"])


class CockpitVerdictTests(unittest.TestCase):
    def _sections(self):
        return native_task_sections(_TASK_XML)

    def test_native_walk_forward_column_keeps_native_stages_incomplete(self):
        sections = self._sections()
        trades = _deployable_trades()
        verdict = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            rankings=sections["rankings"],
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="a" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        self.assertEqual(verdict["schema"], COCKPIT_VERDICT_SCHEMA)
        self.assertEqual(verdict["authority"], "tradercockpit")
        self.assertEqual(verdict["initial_capital"], 25000.0)
        self.assertEqual(verdict["initial_capital_source"], "native_money_management")
        states = {stage["id"]: stage["state"] for stage in verdict["stages"]}
        # The native Rankings include a walk-forward column the cockpit cannot recompute, so the
        # stages that evaluate those exact native conditions stay explicitly incomplete.
        self.assertEqual(states["initial-test"], "incomplete")
        self.assertEqual(states["fast-validation"], "pass")
        self.assertEqual(states["golden-validation"], "incomplete")
        self.assertEqual(states["scenario-tests"], "pass")
        self.assertEqual(states["stress-tests"], "pass")
        self.assertEqual(states["out-of-sample"], "pass")
        self.assertEqual(states["evidence"], "pass")
        self.assertEqual(verdict["verdict"]["state"], "incomplete")
        self.assertIsNotNone(verdict["monte_carlo"])
        self.assertEqual(len(verdict["equity"]), 135)
        self.assertEqual(verdict["statistics"]["out_of_sample"]["NumberOfTrades"], 45)

    def test_all_stages_pass_when_every_native_column_is_computable(self):
        sections = self._sections()
        rankings = sections["rankings"]
        rankings["children"][0]["children"] = rankings["children"][0]["children"][:2]
        trades = _deployable_trades()
        verdict = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            rankings=rankings,
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="b" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        self.assertEqual(verdict["verdict"], {"state": "pass", "label": "Robust & Deployable", "stages_passed": 7, "stages_total": 7})
        stress = next(stage for stage in verdict["stages"] if stage["id"] == "stress-tests")
        self.assertEqual([check["state"] for check in stress["checks"]], ["pass"] * 4)
        scenario = next(stage for stage in verdict["stages"] if stage["id"] == "scenario-tests")
        self.assertEqual(scenario["checks"][2]["detail"], "5 calendar years")

    def test_additional_market_trades_feed_golden_validation_without_blocking_when_absent(self):
        sections = self._sections()
        rankings = sections["rankings"]
        rankings["children"][0]["children"] = rankings["children"][0]["children"][:2]
        trades = _deployable_trades()
        without_am = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            rankings=rankings,
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="b" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        golden = next(stage for stage in without_am["stages"] if stage["id"] == "golden-validation")
        self.assertFalse(any(str(check["label"]).startswith("Additional Markets") for check in golden["checks"]))
        self.assertIsNone(without_am["statistics"]["additional_markets"])

        winners = [{**trade, "SetupName": "AdditionalMarket: EURUSD_M1_dukas/H1"} for trade in trades]
        passing = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            additional_market_trades=select_additional_market_trades(winners),
            rankings=rankings,
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="b" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        passing_golden = next(stage for stage in passing["stages"] if stage["id"] == "golden-validation")
        am_checks = [check for check in passing_golden["checks"] if str(check["label"]).startswith("Additional Markets")]
        self.assertEqual([check["state"] for check in am_checks], ["pass"])
        self.assertGreater(passing["statistics"]["additional_markets"]["ProfitFactor"], 1.1)

        losers = [{**_trade(0, -50.0), "SetupName": "AdditionalMarket: EURUSD_M1_dukas/H1"}]
        failing = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            additional_market_trades=select_additional_market_trades(losers + [{"PL": 10.0, "SampleType": 10, "OpenTime": START, "CloseTime": START + DAY, "Type": 1, "SetupName": "Main: GBPUSD"}]),
            rankings=rankings,
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="d" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        failing_golden = next(stage for stage in failing["stages"] if stage["id"] == "golden-validation")
        self.assertEqual(failing_golden["state"], "fail")
        self.assertEqual(len(select_additional_market_trades(losers + [{"SetupName": "Main: GBPUSD"}])), 1)

    def test_bound_cross_check_runs_append_to_scenario_stress_and_oos(self):
        sections = self._sections()
        rankings = sections["rankings"]
        rankings["children"][0]["children"] = rankings["children"][0]["children"][:2]
        trades = _deployable_trades()

        def _profile(tag: str, acceptance: str) -> dict[str, object]:
            xml = (
                f"<Task><CrossChecks><{tag} use=\"true\">{acceptance}</{tag}></CrossChecks></Task>"
            ).encode()
            return native_task_sections(xml)["cross_checks"]  # type: ignore[return-value]

        empty = "<AcceptanceSettings/>"
        pf = (
            "<AcceptanceSettings><Conditions>"
            '<Condition use="true"><Left-Side><Column-Value column="ProfitFactor" sampleType="127"/>'
            '</Left-Side><Comparator value=">"/><Right-Side><Numeric-Value value="1.1"/></Right-Side></Condition>'
            "</Conditions></AcceptanceSettings>"
        )
        verdict = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=trades,
            rankings=rankings,
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=1,
            seed_digest="e" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
            cross_check_runs={
                "WhatIf": {"trades": trades, "detail": None, "cross_checks": _profile("WhatIf", empty)},
                "OptProfileSysParamPermutation": {"trades": trades, "detail": None, "cross_checks": _profile("OptProfileSysParamPermutation", empty)},
                "MonteCarloRetest": {"trades": trades, "detail": None, "cross_checks": _profile("MonteCarloRetest", pf)},
                "MonteCarloManipulation": {"trades": trades, "detail": None, "cross_checks": _profile("MonteCarloManipulation", empty)},
                "WalkForwardOptimization": {"trades": trades, "detail": None, "cross_checks": _profile("WalkForwardOptimization", pf)},
                "SequentialOptimization": {"trades": trades, "detail": None, "cross_checks": _profile("SequentialOptimization", empty)},
            },
        )
        scenario = next(stage for stage in verdict["stages"] if stage["id"] == "scenario-tests")
        self.assertTrue(any(str(check["label"]).startswith("What-If") and check["state"] == "unevaluated" for check in scenario["checks"]))
        self.assertTrue(any(str(check["label"]).startswith("System Parameter Permutation") and check["state"] == "unevaluated" for check in scenario["checks"]))
        stress = next(stage for stage in verdict["stages"] if stage["id"] == "stress-tests")
        mc_checks = [check for check in stress["checks"] if str(check["label"]).startswith("Monte Carlo retest")]
        self.assertEqual([check["state"] for check in mc_checks], ["pass"])
        self.assertTrue(any(str(check["label"]).startswith("Monte Carlo manipulation") and check["state"] == "unevaluated" for check in stress["checks"]))
        oos = next(stage for stage in verdict["stages"] if stage["id"] == "out-of-sample")
        wf_checks = [check for check in oos["checks"] if str(check["label"]).startswith("Walk-Forward")]
        self.assertEqual([check["state"] for check in wf_checks], ["pass"])
        self.assertTrue(any(str(check["label"]).startswith("Sequential Optimization") and check["state"] == "unevaluated" for check in oos["checks"]))

    def test_short_histories_leave_concentration_unevaluated(self):
        sections = self._sections()
        trades = _profitable_series(30)
        verdict = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=None,
            rankings=sections["rankings"],
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=0,
            seed_digest="c" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        scenario = next(stage for stage in verdict["stages"] if stage["id"] == "scenario-tests")
        self.assertEqual(scenario["state"], "incomplete")
        self.assertEqual(scenario["checks"][2]["state"], "unevaluated")
        self.assertIn("at least 3 calendar years", scenario["checks"][2]["detail"])

    def test_missing_inputs_are_truthful_not_run_states(self):
        sections = self._sections()
        trades = _profitable_series(30)
        verdict = cockpit_verdict(
            historical_trades=trades,
            higher_precision_trades=None,
            rankings=sections["rankings"],
            cross_checks=sections["cross_checks"],
            money_management=None,
            proof_count=0,
            seed_digest="c" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        states = {stage["id"]: stage["state"] for stage in verdict["stages"]}
        self.assertEqual(states["fast-validation"], "not_run")
        self.assertEqual(states["golden-validation"], "not_run")
        self.assertEqual(states["out-of-sample"], "not_run")
        self.assertEqual(states["evidence"], "not_run")
        self.assertEqual(verdict["initial_capital"], 10000.0)
        self.assertEqual(verdict["initial_capital_source"], "sqx_default")
        # Rankings require > 5 out-of-sample trades and this result has none.
        initial = next(stage for stage in verdict["stages"] if stage["id"] == "initial-test")
        self.assertEqual(initial["state"], "fail")
        self.assertEqual(verdict["verdict"]["state"], "fail")

    def test_unreadable_native_conditions_stay_incomplete(self):
        verdict = cockpit_verdict(
            historical_trades=_profitable_series(30),
            higher_precision_trades=None,
            rankings=None,
            cross_checks=None,
            money_management=None,
            proof_count=0,
            seed_digest="d" * 64,
            native_conditions_state="unavailable",
            native_conditions_detail="configuration custody unreadable",
            policy=verdict_policy({}),
        )
        initial = verdict["stages"][0]
        self.assertEqual(initial["state"], "incomplete")
        self.assertEqual(initial["detail"], "configuration custody unreadable")
        self.assertEqual(verdict["native_conditions"]["state"], "unavailable")

    def test_losing_out_of_sample_fails_the_cockpit_verdict(self):
        sections = self._sections()
        oos = [_trade(30 + index, -40.0, sample_type=SAMPLE_OUT_OF_SAMPLE) for index in range(12)]
        verdict = cockpit_verdict(
            historical_trades=_profitable_series(30) + oos,
            higher_precision_trades=None,
            rankings=sections["rankings"],
            cross_checks=sections["cross_checks"],
            money_management=sections["money_management"],
            proof_count=0,
            seed_digest="e" * 64,
            native_conditions_state="available",
            policy=verdict_policy({}),
        )
        oos_stage = next(stage for stage in verdict["stages"] if stage["id"] == "out-of-sample")
        self.assertEqual(oos_stage["state"], "fail")
        failed = [check["label"] for check in oos_stage["checks"] if check["state"] == "fail"]
        self.assertIn("Out-of-sample net profit", failed)
        self.assertEqual(verdict["verdict"]["label"], "Rejected")


class CockpitVerdictHttpTests(unittest.TestCase):
    RESULT = {
        "schema": "tc.research-historical-result.v1",
        "entity_id": "tc-research:historical-result:v1:22222222-2222-4222-8222-222222222222",
        "revision": f"tc-research-revision:historical-result:sha256:{'2' * 64}",
        "candidate_entity_id": "tc-research:candidate:v1:11111111-1111-4111-8111-111111111111",
        "candidate_revision": f"tc-research-revision:candidate:sha256:{'1' * 64}",
        "state": "completed",
        "execution_completed": True,
        "result_archive_sha256": "f" * 64,
    }

    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_detail_response_attaches_cockpit_verdict_from_custody_chain(self):
        xml_ref = self.store.put_evidence(_TASK_XML)
        trades = {"schema": "tc.research-historical-trades.v1", "trades": _profitable_series(30)}
        with patch("tradercockpit.research_retester_http.read_current_historical_result", return_value=dict(self.RESULT)), \
             patch("tradercockpit.research_retester_http.read_historical_trades", return_value=trades), \
             patch("tradercockpit.research_retester_http.read_candidate_revision", return_value={"configuration_entity_id": "cfg", "configuration_revision": "cfg-rev"}), \
             patch("tradercockpit.research_retester_http.read_configuration_revision", return_value={"executable_xml_ref": str(xml_ref)}), \
             patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value={"results": []}), \
             patch("tradercockpit.research_retester_http.list_current_research_proofs", return_value={"proofs": [{"historical_result_revision": self.RESULT["revision"]}]}):
            status, payload = historical_results_response(self.store, entity_id=self.RESULT["entity_id"])
        self.assertEqual(status, 200)
        verdict = payload["cockpit_verdict"]
        self.assertEqual(verdict["state"], "available")
        body = verdict["payload"]
        self.assertEqual(body["schema"], COCKPIT_VERDICT_SCHEMA)
        self.assertEqual(body["native_conditions"]["state"], "available")
        self.assertEqual(body["initial_capital"], 25000.0)
        self.assertEqual(body["historical_result_revision"], self.RESULT["revision"])
        states = {stage["id"]: stage["state"] for stage in body["stages"]}
        self.assertEqual(states["evidence"], "pass")
        self.assertEqual(states["fast-validation"], "not_run")

    def test_detail_response_reports_unreadable_native_conditions(self):
        trades = {"schema": "tc.research-historical-trades.v1", "trades": _profitable_series(30)}
        with patch("tradercockpit.research_retester_http.read_current_historical_result", return_value=dict(self.RESULT)), \
             patch("tradercockpit.research_retester_http.read_historical_trades", return_value=trades), \
             patch("tradercockpit.research_retester_http.read_candidate_revision", return_value={}), \
             patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value={"results": []}), \
             patch("tradercockpit.research_retester_http.list_current_research_proofs", return_value={"proofs": []}):
            status, payload = historical_results_response(self.store, entity_id=self.RESULT["entity_id"])
        self.assertEqual(status, 200)
        body = payload["cockpit_verdict"]["payload"]
        self.assertEqual(body["native_conditions"]["state"], "unavailable")
        self.assertEqual(body["stages"][0]["state"], "incomplete")

    def test_incomplete_result_has_no_verdict(self):
        incomplete = {**self.RESULT, "state": "running", "execution_completed": False}
        with patch("tradercockpit.research_retester_http.read_current_historical_result", return_value=incomplete):
            status, payload = historical_results_response(self.store, entity_id=self.RESULT["entity_id"])
        self.assertEqual(status, 200)
        self.assertEqual(payload["cockpit_verdict"]["state"], "unavailable")
        self.assertEqual(payload["cockpit_verdict"]["reason_code"], "historical_trades_result_incomplete")

    def test_avg_trades_per_month_uses_result_settings_chart_range(self):
        xml_ref = self.store.put_evidence(_TASK_XML)
        settings_ref = self.store.put_evidence(
            b'<Settings><Data><Setups>'
            b'<Setup dateFrom="2018.01.01" dateTo="2024.01.01"><Chart symbol="ES" timeframe="H1"/></Setup>'
            b'</Setups></Data></Settings>'
        )
        result = {**self.RESULT, "result_settings_ref": str(settings_ref)}
        trades = {"schema": "tc.research-historical-trades.v1", "trades": _profitable_series(30)}
        with patch("tradercockpit.research_retester_http.read_current_historical_result", return_value=result), \
             patch("tradercockpit.research_retester_http.read_historical_trades", return_value=trades), \
             patch("tradercockpit.research_retester_http.read_candidate_revision", return_value={"configuration_entity_id": "cfg", "configuration_revision": "cfg-rev"}), \
             patch("tradercockpit.research_retester_http.read_configuration_revision", return_value={"executable_xml_ref": str(xml_ref)}), \
             patch("tradercockpit.research_retester_http.list_native_robustness_results", return_value={"results": []}), \
             patch("tradercockpit.research_retester_http.list_current_research_proofs", return_value={"proofs": []}):
            status, payload = historical_results_response(self.store, entity_id=result["entity_id"])
        self.assertEqual(status, 200)
        stats = payload["cockpit_verdict"]["payload"]["statistics"]["full"]
        self.assertEqual(stats["months_basis"], "native_chart_history")
        self.assertGreater(stats["TotalDataDays"], 1000)


if __name__ == "__main__":
    unittest.main()
