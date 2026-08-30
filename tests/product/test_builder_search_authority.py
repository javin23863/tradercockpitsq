import unittest

import tradercockpit.builder as builder
import tradercockpit.builder.search as search_module
from tradercockpit.builder.runtime import BuilderRuntimeSearchService


class BuilderSearchAuthorityTests(unittest.TestCase):
    def test_all_supported_builder_search_imports_resolve_to_runtime_authority(self):
        self.assertIs(builder.BuilderSearchService, BuilderRuntimeSearchService)
        self.assertIs(search_module.BuilderSearchService, BuilderRuntimeSearchService)


if __name__ == "__main__":
    unittest.main()
