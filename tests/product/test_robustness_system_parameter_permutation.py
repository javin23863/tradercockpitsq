import unittest

from tradercockpit.robustness_system_parameter_permutation import (
    SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS,
    SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE,
    SystemParameterPermutationError,
    SystemParameterPermutationSettings,
)


class SystemParameterPermutationTests(unittest.TestCase):
    def test_native_profile_identifier_is_preserved(self):
        self.assertEqual(
            SQX_SYSTEM_PARAMETER_PERMUTATION_PROFILE,
            "OptProfileSysParamPermutation",
        )

    def test_defaults_reproduce_bounded_native_probe(self):
        settings = SystemParameterPermutationSettings()

        self.assertEqual(SQX_SYSTEM_PARAMETER_PERMUTATION_PROBE_MAX_TESTS, 1)
        self.assertEqual(
            settings.as_sqx_settings(),
            {
                "profile": "OptProfileSysParamPermutation",
                "use": True,
                "Settings": {
                    "OptimPeriods": False,
                    "OptimExitTypes": False,
                    "MaxTests": 1,
                },
            },
        )
        self.assertFalse(settings.hidden_execution_semantics_recovered)

    def test_positive_test_cap_is_preserved_without_invented_upper_bound(self):
        for value in (2, 10, 100, 2**31):
            with self.subTest(value=value):
                settings = SystemParameterPermutationSettings(max_tests=value)
                self.assertEqual(
                    settings.as_sqx_settings()["Settings"]["MaxTests"],
                    value,
                )

    def test_invalid_max_tests_fail_closed(self):
        for value in (0, -1, True, 1.0, "1"):
            with self.subTest(value=value):
                with self.assertRaises(SystemParameterPermutationError):
                    SystemParameterPermutationSettings(max_tests=value)

    def test_non_boolean_native_switch_values_fail_closed(self):
        for kwargs in (
            {"optim_periods": 0},
            {"optim_exit_types": 0},
            {"enabled": 1},
        ):
            with self.subTest(kwargs=kwargs):
                with self.assertRaises(SystemParameterPermutationError):
                    SystemParameterPermutationSettings(**kwargs)

    def test_period_and_exit_type_switches_are_settings_not_hidden_algorithm_claims(self):
        settings = SystemParameterPermutationSettings(
            optim_periods=True,
            optim_exit_types=True,
        )
        serialized = settings.as_sqx_settings()

        self.assertIs(serialized["Settings"]["OptimPeriods"], True)
        self.assertIs(serialized["Settings"]["OptimExitTypes"], True)
        self.assertFalse(settings.hidden_execution_semantics_recovered)

    def test_profile_enablement_is_preserved_instead_of_hard_coded(self):
        disabled = SystemParameterPermutationSettings(enabled=False)
        enabled = SystemParameterPermutationSettings(enabled=True)

        self.assertIs(disabled.as_sqx_settings()["use"], False)
        self.assertIs(enabled.as_sqx_settings()["use"], True)

    def test_serialization_result_is_not_shared_between_calls(self):
        settings = SystemParameterPermutationSettings()
        first = settings.as_sqx_settings()
        first["Settings"]["MaxTests"] = 999
        first["use"] = False

        second = settings.as_sqx_settings()
        self.assertEqual(second["Settings"]["MaxTests"], 1)
        self.assertIs(second["use"], True)


if __name__ == "__main__":
    unittest.main()
