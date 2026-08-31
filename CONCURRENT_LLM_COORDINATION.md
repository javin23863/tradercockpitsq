# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 07:30:16 ICT (UTC+07:00)

## THREE-LLM CONCURRENCY RULE — CURRENT REPOSITORY CONDITION

At this timestamp, **three LLMs are operating in this repository concurrently**. Every LLM must account for the other two before selecting or resuming work.

Before any implementation, rebase, shared-file edit, or integration step:

1. re-fetch live branches and open PRs;
2. identify the current capability owner, not merely a similarly named historical branch;
3. treat another active LLM's branch/PR as protected even if its work is not merged;
4. do not cherry-pick, duplicate, rewrite, or opportunistically extend another LLM's capability lane;
5. if a needed seam is owned by another active lane, stop that integration step and select a genuinely independent product gap instead;
6. re-check ownership again immediately before touching shared files such as `app_server.py`, `run_service.py`, `web/app.mjs`, `web/index.html`, `package.json`, or shared browser runners.

Transient ownership must be determined from live GitHub state. This file records the coordination snapshot but does not override a newer live branch/PR state.

## CURRENT THREAD STATUS

**Strategies → Overview / PR #32 implementation checkpoint is COMPLETE.**

Exact PR #32 head: `ed2236d71c12d6ff2d92d380a2cb557c6ced4775`.
Exact Product Runtime Acceptance: run #245 — **PASS**.

The bounded Overview capability is executable-complete on its branch and does not require the PR #23 shared server seam for its current user path because it consumes canonical `/api/run-read` already present on `main`. PR #32 remains draft for substantive review and merge-order reconciliation only. Mechanical Codex Review Loop #350 is not substantive review evidence; PR #32 currently has no substantive review submission.

## COMPLETED — STRATEGIES → OVERVIEW

- Before selecting this lane, rejected `Operate → Runs` as duplicate because existing `web/run-read.mjs` already operationalizes exact run/invocation lookup on the shared RunSurface.
- Claimed `codex/product-recovery-strategy-overview` only after live ownership checks showed PR #23 and PR #25 actively advancing and no competing Overview lane.
- Added independent `web/strategy-overview.mjs`; did not modify `web/app.mjs`, `app_server.py`, `run_service.py`, Retester, Builder, robustness, workflow, Data/Trading Context, Evidence, or Compare capability logic.
- Reuses only canonical `/api/run-read`; no second strategy/run/lifecycle backend or run index exists.
- Requires an exact canonical StrategySpec route plus exact run reference and invocation ID.
- Cross-checks the backend-returned strategy identity against the exact Overview route and refuses cross-strategy substitution.
- Verifies canonical run/candidate/strategy/data/execution/engine/lifecycle/result/evidence custody and result-producer build identity before replacing placeholder custody/activity state.
- Renders one exact linked invocation and explicitly does not claim latest activity or complete history.
- Does not infer strategy policy from run custody.
- Preserves exact run/invocation in the URL and reloads the same verification.
- Provides only supported next actions: the exact invocation in Operate and verified Results when exact result/evidence custody exists.
- Focused Strategy Overview tests pass.
- Browser acceptance proves verify → provenance/activity render → URL persistence → reload same custody → deliberate cross-strategy substitution → explicit refusal.
- Product Runtime Acceptance #245 passed every stage on exact head `ed2236d71c12d6ff2d92d380a2cb557c6ced4775`, including general browser regression and both SQX browser regressions.

## LIVE EXTERNAL OWNERSHIP SNAPSHOT

- PR #23 / `codex/product-recovery-native-run`: live head `cfefcd8611cd5486ab4f86477590258f3f1079c3` at this snapshot. It continued advancing during this thread and remains protected as Recovery Vertical 1 / shared server + canonical execution seam.
- PR #25 / `codex/product-recovery-builder-evolution`: live head `d53360f798db36e4a04bbf7823727bfef5d9fccd` at this snapshot. It continued advancing during this thread and remains protected as Recovery Vertical 2 / Builder-evolution lane.
- PR #28 / `codex/product-recovery-workflow-orchestration`: protected workflow/Custom Project lane; do not duplicate or opportunistically extend it without live reassignment.

The live `head_sha` from GitHub metadata is the ownership snapshot. PR body text can lag a moving branch and must not be used instead of live head metadata.

## PREVIOUS COMPLETED / PAUSED CHECKPOINTS OWNED BY THIS THREAD

- PR #27 / `codex/product-recovery-robustness`: backend robustness checkpoint complete at `e2181453d4b7ffc0879e6b9bb10e98014ea1f476`, Product Runtime Acceptance #186 green; paused at PR #23 integration seam.
- PR #29 / `codex/product-recovery-data-trading-context`: Data/Trading Context checkpoint complete at `8aa8d566fd1d565b6449fc0a8dde7655dda8a095`, Product Runtime Acceptance #209 green; paused at PR #23 integration seam.
- PR #30 / `codex/product-recovery-strategy-evidence`: Evidence/Proof implementation complete at `d6f0e7d244088cc5a156017f587b7234ef809b06`, Product Runtime Acceptance #225 green; substantive review/merge closure pending.
- PR #31 / `codex/product-recovery-result-compare`: Result Compare implementation complete at `11868084dd80734244815878b9a94c693a90b2e3`, Product Runtime Acceptance #233 green; substantive review/merge closure pending.
- PR #32 / `codex/product-recovery-strategy-overview`: Strategy Overview implementation complete at `ed2236d71c12d6ff2d92d380a2cb557c6ced4775`, Product Runtime Acceptance #245 green; substantive review/merge closure pending.

## PROTECTED CONCURRENT LANES

- PR #23 / `codex/product-recovery-native-run`: Recovery Vertical 1; owns shared `app_server.py` and canonical execution/run-service seam until accepted into `main`.
- PR #25 / `codex/product-recovery-builder-evolution`: Recovery Vertical 2; externally occupied by another LLM.
- PR #28 / `codex/product-recovery-workflow-orchestration`: workflow/Custom Project vertical; protected.
- PR #27 / `codex/product-recovery-robustness`: completed checkpoint owned by this thread; do not resume shared integration until PR #23 releases its seam.
- PR #29 / `codex/product-recovery-data-trading-context`: completed checkpoint owned by this thread; do not resume shared integration until PR #23 releases its seam.
- PR #30 / `codex/product-recovery-strategy-evidence`: implementation complete; avoid capability expansion unless review finds a defect or merge-time reconciliation is required.
- PR #31 / `codex/product-recovery-result-compare`: implementation complete; avoid capability expansion unless review finds a defect or merge-time reconciliation is required.
- PR #32 / `codex/product-recovery-strategy-overview`: implementation complete; avoid capability expansion unless review finds a defect or merge-time reconciliation is required.
- PR #21 / `codex/product-completion-policy`: protected policy/docs authority.

## NEXT FOR THIS LLM

Do not continue PR #32 unless a substantive review finds a defect or merge-time reconciliation is required.

On the next `next`, first re-fetch all live branch/PR ownership for the three concurrent LLMs, then select the highest-value **unoccupied** end-to-end product gap that can be completed without entering another LLM's protected lane. If no independent gap remains, report the exact blocker instead of overlapping another lane.
