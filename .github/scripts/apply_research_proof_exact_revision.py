from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"{label} anchor mismatch")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


config_path = Path("product/tradercockpit/research_configurations.py")
replace_once(
    config_path,
    '''def read_current_configuration(\n    store: FileResearchCustodyStore,\n    entity_id: ResearchEntityId | str,\n) -> dict[str, object]:\n    entity = _configuration_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\n''',
    '''def read_current_configuration(\n    store: FileResearchCustodyStore,\n    entity_id: ResearchEntityId | str,\n) -> dict[str, object]:\n    entity = _configuration_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\ndef read_configuration_revision(\n    store: FileResearchCustodyStore,\n    entity_id: ResearchEntityId | str,\n    revision: ResearchRevisionRef | str,\n) -> dict[str, object]:\n    entity = _configuration_entity(entity_id)\n    exact_revision = _configuration_revision(revision)\n    return _record(store, entity, exact_revision)\n\n\n''',
    "configuration exact-revision helper",
)

job_path = Path("product/tradercockpit/research_native_jobs.py")
replace_once(
    job_path,
    '''def read_current_native_job(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:\n    entity = _job_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\n''',
    '''def read_current_native_job(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:\n    entity = _job_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\ndef read_native_job_revision(\n    store: FileResearchCustodyStore,\n    entity_id: ResearchEntityId | str,\n    revision: ResearchRevisionRef | str,\n) -> dict[str, object]:\n    entity = _job_entity(entity_id)\n    exact_revision = _job_revision(revision)\n    return _record(store, entity, exact_revision)\n\n\n''',
    "native-job exact-revision helper",
)

candidate_path = Path("product/tradercockpit/research_candidates.py")
replace_once(
    candidate_path,
    '''def read_current_candidate(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:\n    entity = _candidate_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\n''',
    '''def read_current_candidate(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:\n    entity = _candidate_entity(entity_id)\n    return _record(store, entity, store.current(entity))\n\n\ndef read_candidate_revision(\n    store: FileResearchCustodyStore,\n    entity_id: ResearchEntityId | str,\n    revision: ResearchRevisionRef | str,\n) -> dict[str, object]:\n    entity = _candidate_entity(entity_id)\n    try:\n        exact_revision = revision if isinstance(revision, ResearchRevisionRef) else ResearchRevisionRef.parse(revision)\n    except ResearchCustodyError as exc:\n        raise ResearchCandidateError("candidate_revision_invalid", "candidate revision identity is invalid") from exc\n    if exact_revision.kind != ResearchKind.CANDIDATE:\n        raise ResearchCandidateError("candidate_revision_invalid", "research revision is not a candidate revision")\n    return _record(store, entity, exact_revision)\n\n\n''',
    "candidate exact-revision helper",
)

proof_path = Path("product/tradercockpit/research_proof.py")
text = proof_path.read_text(encoding="utf-8")
replacements = (
    (
        "from tradercockpit.research_candidates import ResearchCandidateError, read_current_candidate\n",
        "from tradercockpit.research_candidates import ResearchCandidateError, read_candidate_revision\n",
        "Proof Candidate exact import",
    ),
    (
        "from tradercockpit.research_configurations import ResearchConfigurationError, read_current_configuration\n",
        "from tradercockpit.research_configurations import ResearchConfigurationError, read_configuration_revision\n",
        "Proof Configuration exact import",
    ),
    (
        "from tradercockpit.research_native_jobs import ResearchNativeJobError, read_current_native_job\n",
        "from tradercockpit.research_native_jobs import ResearchNativeJobError, read_native_job_revision\n",
        "Proof native-job exact import",
    ),
    (
        "        candidate = read_current_candidate(store, candidate_entity_id)\n",
        "        candidate = read_candidate_revision(store, candidate_entity_id, candidate_revision)\n",
        "Proof Candidate exact read",
    ),
    (
        "        native_job = read_current_native_job(store, native_job_entity_id)\n",
        "        native_job = read_native_job_revision(store, native_job_entity_id, native_job_revision)\n",
        "Proof native-job exact read",
    ),
    (
        "        configuration = read_current_configuration(store, configuration_entity_id)\n",
        "        configuration = read_configuration_revision(store, configuration_entity_id, configuration_revision)\n",
        "Proof Configuration exact read",
    ),
)
for old, new, label in replacements:
    if old not in text:
        raise SystemExit(f"{label} anchor mismatch")
    text = text.replace(old, new, 1)
proof_path.write_text(text, encoding="utf-8")

test_path = Path("tests/product/test_research_proof.py")
test_text = test_path.read_text(encoding="utf-8")
for old, new, label in (
    (
        'patch("tradercockpit.research_proof.read_current_configuration", return_value=configuration)',
        'patch("tradercockpit.research_proof.read_configuration_revision", return_value=configuration)',
        "Proof test Configuration patch",
    ),
    (
        'patch("tradercockpit.research_proof.read_current_native_job", return_value=native_job)',
        'patch("tradercockpit.research_proof.read_native_job_revision", return_value=native_job)',
        "Proof test native-job patch",
    ),
    (
        'patch("tradercockpit.research_proof.read_current_candidate", return_value=candidate)',
        'patch("tradercockpit.research_proof.read_candidate_revision", return_value=candidate)',
        "Proof test Candidate patch",
    ),
):
    if old not in test_text:
        raise SystemExit(f"{label} anchor mismatch")
    test_text = test_text.replace(old, new, 1)

anchor = '''    def test_validation_from_another_historical_result_is_rejected(self):\n'''
regression = '''    def test_proof_reads_exact_bound_revisions_instead_of_current_pointers(self):\n        patches = self._patch_records()\n        with (\n            patches[0] as configuration_read,\n            patches[1] as native_job_read,\n            patches[2] as candidate_read,\n            patches[3],\n            patches[4],\n            patches[5],\n        ):\n            self._create()\n\n        configuration_read.assert_called_once_with(self.store, self.CONFIG_ENTITY, self.CONFIG_REVISION)\n        native_job_read.assert_called_once_with(self.store, self.JOB_ENTITY, self.JOB_REVISION)\n        candidate_read.assert_called_once_with(self.store, self.CANDIDATE_ENTITY, self.CANDIDATE_REVISION)\n\n''' + anchor
if anchor not in test_text:
    raise SystemExit("Proof exact-revision regression anchor mismatch")
test_text = test_text.replace(anchor, regression, 1)
test_path.write_text(test_text, encoding="utf-8")
