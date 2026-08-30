from decimal import Decimal
import unittest

from tradercockpit.domain.canonical import ContentAddress
from tradercockpit.domain.ranking import (
    RankedCandidateV1,
    RankingObjectiveV1,
    order_ranked_candidates,
)
from tradercockpit.domain.specs import SpecValidationError


def candidate_ref(hex_digit: str) -> ContentAddress:
    return ContentAddress("candidate", 1, hex_digit * 64)


class RankingBoundaryTests(unittest.TestCase):
    def test_maximize_orders_discovery_candidates(self) -> None:
        objective = RankingObjectiveV1("example_metric", "maximize")
        low = RankedCandidateV1(candidate_ref("1"), Decimal("1.5"))
        high = RankedCandidateV1(candidate_ref("2"), Decimal("3.0"))

        ordered = order_ranked_candidates(objective, (low, high))

        self.assertEqual(tuple(item.candidate_ref for item in ordered), (high.candidate_ref, low.candidate_ref))

    def test_minimize_orders_discovery_candidates(self) -> None:
        objective = RankingObjectiveV1("example_metric", "minimize")
        low = RankedCandidateV1(candidate_ref("1"), Decimal("1.5"))
        high = RankedCandidateV1(candidate_ref("2"), Decimal("3.0"))

        ordered = order_ranked_candidates(objective, (high, low))

        self.assertEqual(tuple(item.candidate_ref for item in ordered), (low.candidate_ref, high.candidate_ref))

    def test_invalid_direction_fails_closed(self) -> None:
        with self.assertRaises(SpecValidationError):
            RankingObjectiveV1("example_metric", "best")

    def test_non_finite_score_fails_closed(self) -> None:
        with self.assertRaises(SpecValidationError):
            RankedCandidateV1(candidate_ref("1"), Decimal("NaN"))

    def test_duplicate_candidate_identity_fails_closed(self) -> None:
        objective = RankingObjectiveV1("example_metric", "maximize")
        ref = candidate_ref("1")
        with self.assertRaises(SpecValidationError):
            order_ranked_candidates(
                objective,
                (RankedCandidateV1(ref, Decimal("1")), RankedCandidateV1(ref, Decimal("2"))),
            )

    def test_unproven_tie_breaking_fails_closed(self) -> None:
        objective = RankingObjectiveV1("example_metric", "maximize")
        with self.assertRaises(SpecValidationError):
            order_ranked_candidates(
                objective,
                (
                    RankedCandidateV1(candidate_ref("1"), Decimal("2")),
                    RankedCandidateV1(candidate_ref("2"), Decimal("2")),
                ),
            )

    def test_ranking_objects_cannot_encode_validation_or_champion_state(self) -> None:
        objective = RankingObjectiveV1("example_metric", "maximize")
        ranked = RankedCandidateV1(candidate_ref("1"), Decimal("2"))

        self.assertEqual(set(objective.__dataclass_fields__), {"metric_path", "direction"})
        self.assertEqual(set(ranked.__dataclass_fields__), {"candidate_ref", "score"})
        self.assertFalse(hasattr(ranked, "passed"))
        self.assertFalse(hasattr(ranked, "validated"))
        self.assertFalse(hasattr(ranked, "champion"))


if __name__ == "__main__":
    unittest.main()
