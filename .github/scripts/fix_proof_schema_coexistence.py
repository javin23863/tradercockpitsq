from pathlib import Path

robustness = Path("product/tradercockpit/research_robustness.py")
text = robustness.read_text(encoding="utf-8")
anchor = 'ROBUSTNESS_CAPABILITIES_SCHEMA = "tc.research-native-robustness-capabilities.v1"\n'
replacement = anchor + '_USER_RESEARCH_PROOF_CONTENT_SCHEMA = "tc.research-proof-content.v1"\n'
if anchor not in text:
    raise SystemExit("robustness schema anchor mismatch")
text = text.replace(anchor, replacement, 1)
old_completed = '''        if raw.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA:\n            continue\n        if raw.get("schema") != ROBUSTNESS_RECORD_SCHEMA:\n'''
new_completed = '''        if raw.get("schema") == _USER_RESEARCH_PROOF_CONTENT_SCHEMA:\n            # Research Proof shares ResearchKind.PROOF custody. Its own strict\n            # reader owns this registered sibling schema.\n            continue\n        if raw.get("schema") == ROBUSTNESS_ATTEMPT_SCHEMA:\n            continue\n        if raw.get("schema") != ROBUSTNESS_RECORD_SCHEMA:\n'''
if old_completed not in text:
    raise SystemExit("completed Proof catalog anchor mismatch")
text = text.replace(old_completed, new_completed, 1)
old_failed = '''        if raw.get("schema") == ROBUSTNESS_RECORD_SCHEMA:\n            continue\n        if raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA:\n'''
new_failed = '''        if raw.get("schema") == _USER_RESEARCH_PROOF_CONTENT_SCHEMA:\n            # Registered user-facing Proof custody is foreign to robustness.\n            continue\n        if raw.get("schema") == ROBUSTNESS_RECORD_SCHEMA:\n            continue\n        if raw.get("schema") != ROBUSTNESS_ATTEMPT_SCHEMA:\n'''
if old_failed not in text:
    raise SystemExit("failed Proof catalog anchor mismatch")
text = text.replace(old_failed, new_failed, 1)
robustness.write_text(text, encoding="utf-8")

tests = Path("tests/product/test_research_robustness.py")
test_text = tests.read_text(encoding="utf-8")
old_import = 'from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchRevisionRef\n'
new_import = 'from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError, ResearchEntityId, ResearchKind, ResearchRevisionRef\n'
if old_import not in test_text:
    raise SystemExit("ResearchKind import anchor mismatch")
test_text = test_text.replace(old_import, new_import, 1)
anchor_test = '''    def test_proof_readback_requires_existing_historical_source_revision(self) -> None:\n'''
regression = '''    def test_catalog_ignores_registered_user_research_proof_schema(self) -> None:\n        with TemporaryDirectory() as tmp:\n            store = FileResearchCustodyStore(Path(tmp) / "data")\n            entity = store.create_entity(ResearchKind.PROOF)\n            sibling = store.create_revision(\n                entity,\n                json.dumps(\n                    {"schema": "tc.research-proof-content.v1"},\n                    sort_keys=True,\n                    separators=(",", ":"),\n                ).encode("utf-8"),\n            )\n            store.compare_and_set_current(entity, expected_revision=None, target_revision=sibling.revision)\n\n            catalog = list_native_robustness_results(store)\n\n        self.assertEqual(catalog["results"], [])\n        self.assertEqual(catalog["failed_attempts"], [])\n\n''' + anchor_test
if anchor_test not in test_text:
    raise SystemExit("Proof coexistence regression anchor mismatch")
test_text = test_text.replace(anchor_test, regression, 1)
tests.write_text(test_text, encoding="utf-8")
