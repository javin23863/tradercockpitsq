from pathlib import Path

builder_path = Path("product/tradercockpit/sqx_builder_config.py")
text = builder_path.read_text(encoding="utf-8")

constant = '_NATIVE_GENERATION_TYPES = frozenset({"random-generation", "genetic-evolution"})\n'
if constant not in text:
    raise SystemExit("native generation allowlist anchor mismatch")
text = text.replace(constant, "", 1)

old_requirement = '''        _requirement(\n            "search_build_mode", "Search / build mode",\n            _state(native.generation_type in _NATIVE_GENERATION_TYPES), required=True,\n            detail="Native WhatToBuild recognizes random-generation or genetic-evolution and rejects unknown generation types.",\n            evidence_path=task_source,\n            values={"generation_type": native.generation_type},\n        ),\n'''
new_requirement = '''        _requirement(\n            "search_build_mode", "Search / build mode",\n            _state(_present(native.generation_type)), required=True,\n            detail="A native generationType selection is present in the exact current Builder task. TraderCockpit preserves the opaque producer value and SQX validates its semantics during loadconfig.",\n            evidence_path=task_source,\n            values={"generation_type": native.generation_type},\n        ),\n'''
if old_requirement not in text:
    raise SystemExit("search/build mode requirement anchor mismatch")
text = text.replace(old_requirement, new_requirement, 1)
builder_path.write_text(text, encoding="utf-8")

test_path = Path("tests/product/test_research_specification.py")
test_text = test_path.read_text(encoding="utf-8")
old_test = '''    def test_unknown_generation_type_stays_unresolved(self) -> None:\n        task = """\n        <Task>\n          <WhatToBuild><BuildMode generationType="future-or-typo"/></WhatToBuild>\n          <Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>\n          <InstrumentInfo instrument="EURUSD_dukascopy"/>\n        </Task>\n        """\n        with TemporaryDirectory() as tmp:\n            home = self._runtime(Path(tmp))\n            self._write_project(home, task)\n            record = builder_project_config_record(home)\n\n        search_mode = self._requirements(record)["search_build_mode"]\n        self.assertEqual(search_mode["state"], "unresolved")\n        self.assertEqual(search_mode["values"]["generation_type"], "future-or-typo")\n\n'''
new_test = '''    def test_current_native_generation_type_is_preserved_without_tradercockpit_allowlist(self) -> None:\n        task = """\n        <Task>\n          <WhatToBuild><BuildMode generationType="future-native-mode"/></WhatToBuild>\n          <Chart symbol="EURUSD_M1_dukas" timeframe="M30"/>\n          <InstrumentInfo instrument="EURUSD_dukascopy"/>\n        </Task>\n        """\n        with TemporaryDirectory() as tmp:\n            home = self._runtime(Path(tmp))\n            self._write_project(home, task)\n            record = builder_project_config_record(home)\n\n        search_mode = self._requirements(record)["search_build_mode"]\n        self.assertEqual(search_mode["state"], "producer_configured")\n        self.assertEqual(search_mode["values"]["generation_type"], "future-native-mode")\n        self.assertNotIn("unresolved:search_build_mode", record["specification"]["build_gate"]["reason_codes"])\n\n'''
if old_test not in test_text:
    raise SystemExit("generation type regression anchor mismatch")
test_path.write_text(test_text.replace(old_test, new_test, 1), encoding="utf-8")
