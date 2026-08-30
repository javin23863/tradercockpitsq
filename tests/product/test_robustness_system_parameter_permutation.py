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

    def test_positive_test_cap_is_preserved_without_invented_upper_bound(self):
        for value in (2, 10, 100):
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
        for kwargs in ({"optim_periods": 0}, {"optim_exit_types": 0}):
            with self.subTest(kwargs=kwargs):
                with self.assertRaises(SystemParameterPermutationError):
                    SystemParameterPermutationSettings(**kwargs)

    def test_unproven_period_optimization_is_rejected(self):
        with self.assertRaises(SystemParameterPermutationError):
            SystemParameterPermutationSettings(optim_periods=True)

    def test_unproven_exit_type_optimization_is_rejected(self):
        with self.assertRaises(SystemParameterPermutationError):
            SystemParameterPermutationSettings(optim_exit_types=True)

    def test_serialization_result_is_not_shared_between_calls(self):
        settings = SystemParameterPermutationSettings()
        first = settings.as_sqx_settings()
        first["Settings"]["MaxTests"] = 999

        self.assertEqual(settings.as_sqx_settings()["Settings"]["MaxTests"], 1)


if __name__ == "__main__":
    unittest.main()
