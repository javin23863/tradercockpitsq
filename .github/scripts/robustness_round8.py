from __future__ import annotations

from pathlib import Path
import subprocess

EXPECTED_BASE = "aa2d166385bb1404b50a4ac371e1f2b40f73908b"
ALLOWED_PREEXISTING = {
    ".github/scripts/robustness_round8.py",
    ".github/workflows/robustness-round8.yml",
}


def run(*args: str) -> str:
    return subprocess.check_output(args, text=True).strip()


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, value: str) -> None:
    Path(path).write_text(value, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    first = text.find(start)
    if first < 0:
        raise SystemExit(f"{label}: start marker missing")
    last = text.find(end, first)
    if last < 0:
        raise SystemExit(f"{label}: end marker missing")
    return text[:first] + replacement + text[last:]


head = run("git", "rev-parse", "HEAD")
changed = set(filter(None, run("git", "diff", "--name-only", EXPECTED_BASE, head).splitlines()))
if changed - ALLOWED_PREEXISTING:
    raise SystemExit(f"unexpected drift from {EXPECTED_BASE}: {sorted(changed - ALLOWED_PREEXISTING)}")

# 1. Canonical Historical Result revision reader + archive error normalization.
path = "product/tradercockpit/research_retester.py"
text = read(path)
text = replace_once(
    text,
    '    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError) as exc:\n        raise ResearchRetesterError("retester_result_corrupt", f"result archive member {name} is unreadable") from exc\n',
    '    except (BadZipFile, RuntimeError, NotImplementedError, EOFError, OSError, zlib.error) as exc:\n        raise ResearchRetesterError("retester_result_corrupt", f"result archive member {name} is unreadable") from exc\n',
    "normalize compressed member errors",
)
text = replace_once(
    text,
    '    try:\n        record = inspect_sqx_output_bytes(snapshot, archive_name=path.name)\n    except SqxOutputError as exc:\n        raise ResearchRetesterError(exc.code, exc.detail) from exc\n',
    '    try:\n        record = inspect_sqx_output_bytes(snapshot, archive_name=path.name)\n    except SqxOutputError as exc:\n        raise ResearchRetesterError(exc.code, exc.detail) from exc\n    except zlib.error as exc:\n        raise ResearchRetesterError(\n            "retester_result_corrupt",\n            "Retester result archive contains unreadable compressed data",\n        ) from exc\n',
    "normalize capture decompression errors",
)
needle = '''def read_current_historical_result(store: FileResearchCustodyStore, entity_id: ResearchEntityId | str) -> dict[str, object]:
    entity = _historical_entity(entity_id)
    return _record(store, entity, store.current(entity))


'''
addition = needle + '''def read_historical_result_revision(
    store: FileResearchCustodyStore,
    entity_id: ResearchEntityId | str,
    revision: ResearchRevisionRef | str,
) -> dict[str, object]:
    """Read one exact immutable Historical Result through the canonical validator."""

    entity = _historical_entity(entity_id)
    if isinstance(revision, ResearchRevisionRef):
        if revision.kind != ResearchKind.HISTORICAL_RESULT:
            raise ResearchRetesterError(
                "historical_result_content_corrupt",
                "historical-result revision is not Historical Result custody",
            )
        selected = revision
    else:
        selected = _typed_revision(
            revision,
            ResearchKind.HISTORICAL_RESULT,
            "historical_result_content_corrupt",
        )
    return _record(store, entity, selected)


'''
text = replace_once(text, needle, addition, "add exact historical revision reader")
write(path, text)

# 2. Higher Precision source path and Proof source validation.
path = "product/tradercockpit/research_robustness.py"
text = read(path)
text = replace_once(
    text,
    '    _validate_retester_project,\n    read_current_historical_result,\n)',
    '    _validate_retester_project,\n    read_current_historical_result,\n    read_historical_result_revision,\n)',
    "import canonical historical revision reader",
)
helper = '''def _read_installed_retester_source(home: Path) -> tuple[bytes, Path, str]:
    """Capture only the exact physical installed Retester/project.cfx source."""

    relative = f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx"
    try:
        projects_root = (home / "user/projects").resolve(strict=True)
        projects_root.relative_to(home)
        expected_retester_root = projects_root / RETESTER_SOURCE_PROJECT
        resolved_retester_root = (home / "user/projects" / RETESTER_SOURCE_PROJECT).resolve(strict=True)
    except (OSError, RuntimeError, ValueError) as exc:
        raise ResearchRetesterError(
            "retester_source_project_path_escape",
            "installed Retester source path escapes verified SQX runtime",
        ) from exc
    if resolved_retester_root != expected_retester_root or not resolved_retester_root.is_dir():
        raise ResearchRetesterError(
            "retester_source_project_path_escape",
            "installed Retester project root is redirected from the exact user/projects/Retester path",
        )
    project_bytes, physical_path, project_sha = _read_exact_inside(
        home,
        relative,
        missing_code="retester_source_project_missing",
        escape_code="retester_source_project_path_escape",
    )
    expected_file = expected_retester_root / "project.cfx"
    if physical_path != expected_file:
        raise ResearchRetesterError(
            "retester_source_project_path_escape",
            "installed Retester project.cfx is redirected from its exact physical source path",
        )
    return project_bytes, physical_path, project_sha


'''
text = replace_once(text, "def _current_proof_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:\n", helper + "def _current_proof_entities(store: FileResearchCustodyStore) -> tuple[ResearchEntityId, ...]:\n", "add exact Retester source helper")
new_binding = '''def _validate_historical_source_binding(
    store: FileResearchCustodyStore,
    payload: dict[str, object],
) -> None:
    try:
        source_entity = ResearchEntityId.parse(payload["source_historical_result_entity_id"])
        source_revision = ResearchRevisionRef.parse(payload["source_historical_result_revision"])
        source_ref = EvidenceRef.parse(payload["source_result_archive_ref"])
    except (KeyError, TypeError, ResearchCustodyError) as exc:
        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source identities are invalid") from exc
    if source_entity.kind != ResearchKind.HISTORICAL_RESULT or source_revision.kind != ResearchKind.HISTORICAL_RESULT:
        raise ResearchRobustnessError("robustness_proof_catalog_corrupt", "robustness Proof source is not Historical Result custody")
    try:
        source = read_historical_result_revision(store, source_entity, source_revision)
    except (ResearchCustodyError, ResearchRetesterError) as exc:
        raise ResearchRobustnessError(
            "robustness_proof_catalog_corrupt",
            "robustness Proof source Historical Result revision is unavailable or producer-invalid",
        ) from exc
    if (
        source.get("entity_id") != str(source_entity)
        or source.get("revision") != str(source_revision)
        or source.get("state") != "completed"
        or source.get("execution_completed") is not True
        or source.get("result_archive_ref") != str(source_ref)
        or source.get("result_archive_sha256") != payload.get("source_result_archive_sha256")
        or payload.get("source_result_archive_ref") != source.get("result_archive_ref")
    ):
        raise ResearchRobustnessError(
            "robustness_proof_catalog_corrupt",
            "robustness Proof source archive does not match its canonical Historical Result revision",
        )


'''
text = replace_between(text, "def _validate_historical_source_binding(\n", "def _failed_successor(\n", new_binding, "replace partial Historical Result validator")
text = replace_once(
    text,
    '''        source_project_bytes, _, _ = _read_exact_inside(
            home,
            f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx",
            missing_code="retester_source_project_missing",
            escape_code="retester_source_project_path_escape",
        )
''',
    '        source_project_bytes, _, _ = _read_installed_retester_source(home)\n',
    "capability exact Retester source",
)
text = replace_once(
    text,
    '''        source_project_bytes, _, source_project_sha = _read_exact_inside(
            home,
            f"user/projects/{RETESTER_SOURCE_PROJECT}/project.cfx",
            missing_code="retester_source_project_missing",
            escape_code="retester_source_project_path_escape",
        )
''',
    '        source_project_bytes, _, source_project_sha = _read_installed_retester_source(home)\n',
    "execution exact Retester source",
)
write(path, text)

# 3. SPA mount lifecycle follows the already-established Research module pattern.
path = "web/research-backtest-robustness.mjs"
text = read(path)
old_tail = '''  document.addEventListener("locationchange", () => { if (robustnessRoute()) void load(); });
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => { if (robustnessRoute()) void load(); }, { once: true });
  } else if (robustnessRoute()) {
    queueMicrotask(load);
  }
}
'''
new_tail = '''  const observer = new MutationObserver(() => {
    const host = panel();
    if (robustnessRoute() && host && !host.querySelector("[data-robustness-workspace]")) void load();
    if (!robustnessRoute()) generation += 1;
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  void load();
}
'''
text = replace_once(text, old_tail, new_tail, "mount robustness after SPA route renders")
write(path, text)

# 4. Product regressions use a producer-valid Historical Result source chain.
path = "tests/product/test_research_robustness.py"
text = read(path)
text = replace_once(text, "import json\n", "import json\nimport zlib\n", "import zlib for corrupt compressed output regression")
text = replace_once(
    text,
    "from tradercockpit.research_robustness import (\n",
    "from tradercockpit.research_retester import NativeRetesterContent, ResearchRetesterError, read_historical_result_revision\nfrom tradercockpit.research_robustness import (\n",
    "import canonical historical validator",
)
new_historical = '''    def _historical(self, store: FileResearchCustodyStore, source: bytes) -> dict[str, object]:
        candidate = self._archive_bytes("historical-candidate")
        candidate_info = inspect_sqx_output_bytes(candidate, archive_name="Candidate.sqx")
        candidate_ref = store.put_evidence(candidate)
        source_project = self._project_bytes(self._task_xml())
        source_project_ref = store.put_evidence(source_project)
        engine = b"historical Retester engine"
        engine_ref = store.put_evidence(engine)
        result_ref = store.put_evidence(source)
        inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")
        with ZipFile(BytesIO(source)) as archive:
            strategy = archive.read("strategy_Portfolio.xml")
            settings = archive.read("settings.xml")
        strategy_ref = store.put_evidence(strategy)
        settings_ref = store.put_evidence(settings)

        entity = ResearchEntityId.parse(self.HISTORICAL_ENTITY)
        project_name = "TraderCockpit-Retester-22222222222242228222222222222222"
        candidate_entity = "tc-research:candidate:v1:33333333-3333-4333-8333-333333333333"
        candidate_revision = f"tc-research-revision:candidate:sha256:{'4' * 64}"
        prepared = NativeRetesterContent(
            state="prepared",
            candidate_entity_id=candidate_entity,
            candidate_revision=candidate_revision,
            candidate_archive_name="Candidate.sqx",
            candidate_archive_ref=candidate_ref,
            candidate_archive_sha256=candidate_info["archive_sha256"],
            sqx_build="144.2953",
            operation="native_retester_task_1",
            retester_task=1,
            native_project_name=project_name,
            native_project_relative_path=f"user/projects/{project_name}/project.cfx",
            source_project_ref=source_project_ref,
            source_project_sha256=sha256(source_project).hexdigest(),
            engine_ref=engine_ref,
            engine_sha256=sha256(engine).hexdigest(),
            launcher_sha256=None,
            receipts=(),
            partial_side_effect=False,
        )
        prepared_revision = store.create_revision(
            entity,
            prepared.canonical_bytes(),
            evidence=(candidate_ref, source_project_ref, engine_ref),
        )
        store.compare_and_set_current(entity, expected_revision=None, target_revision=prepared_revision.revision)

        completed = NativeRetesterContent(
            state="completed",
            candidate_entity_id=candidate_entity,
            candidate_revision=candidate_revision,
            candidate_archive_name="Candidate.sqx",
            candidate_archive_ref=candidate_ref,
            candidate_archive_sha256=candidate_info["archive_sha256"],
            sqx_build="144.2953",
            operation="native_retester_task_1",
            retester_task=1,
            native_project_name=project_name,
            native_project_relative_path=f"user/projects/{project_name}/project.cfx",
            source_project_ref=source_project_ref,
            source_project_sha256=sha256(source_project).hexdigest(),
            engine_ref=engine_ref,
            engine_sha256=sha256(engine).hexdigest(),
            launcher_sha256=self.LAUNCHER_SHA,
            receipts=({"action": "startOnlyTask", "task": 1, "state": "completed", "project": project_name},),
            partial_side_effect=False,
            result_archive_name="Baseline.sqx",
            result_archive_relative_path=f"user/projects/{project_name}/databanks/Results/Baseline.sqx",
            result_archive_ref=result_ref,
            result_archive_sha256=inspected["archive_sha256"],
            result_strategy_ref=strategy_ref,
            result_strategy_sha256=sha256(strategy).hexdigest(),
            result_settings_ref=settings_ref,
            result_settings_sha256=sha256(settings).hexdigest(),
        )
        completed_revision = store.create_revision(
            entity,
            completed.canonical_bytes(),
            parent_revision=prepared_revision.revision,
            evidence=(candidate_ref, source_project_ref, engine_ref, result_ref, strategy_ref, settings_ref),
        )
        store.compare_and_set_current(
            entity,
            expected_revision=prepared_revision.revision,
            target_revision=completed_revision.revision,
        )
        self.HISTORICAL_REVISION = str(completed_revision.revision)
        return read_historical_result_revision(store, entity, completed_revision.revision)

'''
text = replace_between(text, "    def _historical(self, store: FileResearchCustodyStore, source: bytes) -> dict[str, object]:\n", "    def _current_proof_payload", new_historical, "replace minimal Historical Result fixture")
new_tests = '''    def test_exact_historical_revision_reader_rejects_minimal_subset_content(self) -> None:
        source = self._archive_bytes("baseline")
        with TemporaryDirectory() as tmp:
            store = FileResearchCustodyStore(Path(tmp))
            ref = store.put_evidence(source)
            inspected = inspect_sqx_output_bytes(source, archive_name="Baseline.sqx")
            entity = ResearchEntityId.parse(self.HISTORICAL_ENTITY)
            content = json.dumps({
                "schema": "tc.research-historical-result-content.v1",
                "state": "completed",
                "result_archive_ref": str(ref),
                "result_archive_sha256": inspected["archive_sha256"],
            }, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
            revision = store.create_revision(entity, content, evidence=(ref,))
            with self.assertRaises(ResearchRetesterError) as caught:
                read_historical_result_revision(store, entity, revision.revision)
            self.assertEqual(caught.exception.code, "historical_result_content_corrupt")

    def test_installed_retester_source_redirect_is_refused_before_native_execution(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            alternate = home / "user/projects/Alternate"
            alternate.mkdir(parents=True)
            alternate_file = alternate / "project.cfx"
            alternate_file.write_bytes(project)
            source_file = home / "user/projects/Retester/project.cfx"
            source_file.unlink()
            try:
                source_file.symlink_to(alternate_file)
            except OSError as exc:
                self.skipTest(f"symlink creation unavailable: {exc}")

            capability = read_native_robustness_capabilities(home)
            self.assertEqual(capability["methods"][0]["state"], "unavailable")
            self.assertEqual(capability["methods"][0]["reason_code"], "retester_source_project_path_escape")
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with self.assertRaises(ResearchRobustnessError) as caught:
                    start_native_higher_precision(
                        store, home, self.LAUNCHER_SHA,
                        historical_result_entity_id=self.HISTORICAL_ENTITY,
                        expected_historical_result_revision=self.HISTORICAL_REVISION,
                        gateway_factory=lambda *args, **kwargs: self.fail("redirected source reached native gateway"),
                    )
            self.assertEqual(caught.exception.code, "retester_source_project_path_escape")

    def test_compressed_result_read_failure_persists_completed_native_receipt(self) -> None:
        source_result = self._archive_bytes("baseline")
        project = self._project_bytes(self._task_xml())
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            home = self._runtime(root / "sqx", project)
            store = FileResearchCustodyStore(root / "data")
            historical = self._historical(store, source_result)
            with patch("tradercockpit.research_robustness.read_current_historical_result", return_value=historical):
                with patch("tradercockpit.research_retester.inspect_sqx_output_bytes", side_effect=zlib.error("corrupt compressed member")):
                    with self.assertRaises(ResearchRobustnessError) as caught:
                        start_native_higher_precision(
                            store, home, self.LAUNCHER_SHA,
                            historical_result_entity_id=self.HISTORICAL_ENTITY,
                            expected_historical_result_revision=self.HISTORICAL_REVISION,
                            gateway_factory=self._gateway_factory(home, "higher-precision"),
                        )
            self.assertEqual(caught.exception.code, "retester_result_corrupt")
            self.assertRegex(caught.exception.attempt_ref or "", r"^tc-evidence:sha256:[0-9a-f]{64}$")
            failed = self._current_proof_payload(store)
            self.assertEqual(failed["state"], "failed")
            self.assertEqual(failed["failure_reason_code"], "retester_result_corrupt")
            self.assertTrue(failed["partial_side_effect"])
            self.assertEqual(failed["launcher_sha256"], self.LAUNCHER_SHA)
            self.assertEqual(failed["receipts"][0]["state"], "completed")
            reopened = read_native_robustness_result(store, caught.exception.attempt_ref)
            self.assertEqual(reopened["failure_reason_code"], "retester_result_corrupt")
            self.assertEqual(reopened["receipts"][0]["state"], "completed")

'''
text = replace_once(text, "    def test_invalid_validation_ref_is_typed(self) -> None:\n", new_tests + "    def test_invalid_validation_ref_is_typed(self) -> None:\n", "add round eight backend regressions")
write(path, text)

# 5. Browser acceptance proves the ordinary Home -> Research -> Backtest -> Robustness SPA path mounts.
path = "tests/run-browser-robustness-regression.mjs"
text = read(path)
old = '''  const page = await browser.newPage();
  const missingValidation = `tc-evidence:sha256:${"f".repeat(64)}`;
  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness&validationRef=${missingValidation}`, { waitUntil: "domcontentloaded" });

  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }

  assert.equal(
    await page.locator("[data-robustness-workspace]").count(),
    1,
    "Backtest Robustness must mount its producer-backed workspace",
  );
'''
new = '''  const page = await browser.newPage();
  const missingValidation = `tc-evidence:sha256:${"f".repeat(64)}`;

  await page.goto(`${baseUrl}/home`, { waitUntil: "domcontentloaded" });
  await page.getByRole("link", { name: "Open Research", exact: true }).click();
  await page.getByRole("link", { name: "Backtest", exact: true }).click();
  await page.getByRole("link", { name: "Robustness", exact: true }).click();
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }
  assert.equal(
    await page.locator("[data-robustness-workspace]").count(),
    1,
    "Backtest Robustness must mount after ordinary SPA navigation from Home",
  );

  await page.goto(`${baseUrl}/research?stage=backtest&tab=robustness&validationRef=${missingValidation}`, { waitUntil: "domcontentloaded" });
  for (let attempt = 0; attempt < 100; attempt += 1) {
    if (await page.locator("[data-robustness-workspace]").count()) break;
    await page.waitForTimeout(25);
  }
  assert.equal(
    await page.locator("[data-robustness-workspace]").count(),
    1,
    "Backtest Robustness must also mount on direct bookmarked entry",
  );
'''
text = replace_once(text, old, new, "browser SPA navigation regression")
write(path, text)

print("round eight patch applied")
