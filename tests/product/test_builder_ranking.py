from decimal import Decimal
import unittest

from product.tradercockpit.builder.ranking import (
    SQX_RANKING_EVIDENCE_ROLE,
    CandidateFitnessV1,
    RankingObjectiveV1,
    order_candidates,
)
from product.tradercockpit.domain.canonical import content_address
from product.tradercockpit.domain.specs import SpecValidationError


def candidate_ref(name: str):
    return content_address("candidate", 1, {"name": name})


class BuilderRankingAuthorityTests(unittest.TestCase):
    def test_maximize_orders_search_candidates_without_validation_state(self):
        objective = RankingObjectiveV1("example_metric", "maximize")
        low = CandidateFitnessV1(candidate_ref("low"), Decimal("1.25"))
        high = CandidateFitnessV1(candidate_ref("high"), Decimal("9.50"))

        ordered = order_candidates(objective, (low, high))

        self.assertEqual([item.score for item in ordered], [Decimal("9.50"), Decimal("1.25")])
        self.assertTrue(all(item.evidence_role == SQX_RANKING_EVIDENCE_ROLE for item in ordered))
        self.assertTrue(all(not hasattr(item, "passed") for item in ordered))
        self.assertTrue(all(not hasattr(item, "champion") for item in ordered))
        self.assertTrue(all(not hasattr(item, "validation_decision_ref") for item in ordered))

    def test_equal_scores_are_deterministic_by_immutable_candidate_identity(self):
        objective = RankingObjectiveV1("example_metric")
        first = CandidateFitnessV1(candidate_ref("first"), Decimal("4"))
        second = CandidateFitnessV1(candidate_ref("second"), Decimal("4"))

        ordered = order_candidates(objective, (second, first))

        self.assertEqual(
            [str(item.candidate_ref) for item in ordered],
            sorted([str(first.candidate_ref), str(second.candidate_ref)]),
        )

    def test_unproved_direction_fails_closed(self):
        with self.assertRaisesRegex(SpecValidationError, "not supported"):
            RankingObjectiveV1("example_metric", "minimize")

    def test_objective_is_opaque_not_a_claimed_metric_catalog(self):
        self.assertEqual(
            RankingObjectiveV1("source_bound_metric").objective,
            "source_bound_metric",
        )
        with self.assertRaises(SpecValidationError):
            RankingObjectiveV1("Net Profit")

    def test_fitness_requires_finite_decimal_and_candidate_identity(self):
        with self.assertRaisesRegex(SpecValidationError, "finite Decimal"):
            CandidateFitnessV1(candidate_ref("bad"), Decimal("NaN"))
        with self.assertRaisesRegex(SpecValidationError, "candidate"):
            CandidateFitnessV1(
                content_address("strategy", 1, {"name": "wrong-kind"}),
                Decimal("1"),
            )

    def test_duplicate_candidate_fitness_fails_closed(self):
        objective = RankingObjectiveV1("example_metric")
        ref = candidate_ref("duplicate")
        with self.assertRaisesRegex(SpecValidationError, "at most once"):
            order_candidates(
                objective,
                (
                    CandidateFitnessV1(ref, Decimal("1")),
                    CandidateFitnessV1(ref, Decimal("2")),
                ),
            )

    def test_empty_ranking_fails_closed(self):
        with self.assertRaisesRegex(SpecValidationError, "must not be empty"):
            order_candidates(RankingObjectiveV1("example_metric"), ())


if __name__ == "__main__":
    unittest.main()
