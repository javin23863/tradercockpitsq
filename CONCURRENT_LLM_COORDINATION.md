# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 07:21:00 ICT (UTC+07:00)

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

**Test & Validate → Compare / PR #31 implementation checkpoint is COMPLETE.**

Exact PR #31 head: `11868084dd80734244815878b9a94c693a90b2e3`.
Exact Product Runtime Acceptance: run #233 — **PASS**.

The bounded Compare capability is executable-complete on its branch and does not require the PR #23 shared server seam for its current user path because it consumes canonical `/api/run-read` already present on `main`. PR #31 remains draft for substantive review and merge-order reconciliation only. Mechanical Codex Review Loop #334 is not substantive review evidence; the PR currently has no substantive review submission.

## COMPLETED — TEST & VALIDATE → COMPARE

- Claimed `codex/product-recovery-result-compare` only after live branch/PR checks found no competing Compare lane.
- Added independent `web/result-compare.mjs`; did not modify `web/app.mjs`, `app_server.py`, `run_service.py`, Retester, Builder, robustness, workflow, Data/Trading Context, or Strategies Evidence capability logic.
- Reuses only canonical `/api/run-read`; no second comparison/result/run backend exists.
- Requires two exact terminal run/invocation identities with durable result custody.
- Verifies canonical run/candidate/strategy/data/execution/engine/result/validation/evidence identities and cross-checks result producer build against the run engine build.
- Requires matching result schemas; incompatible schemas fail closed.
- Contextual comparison requires both runs to resolve to the requested strategy exactly.
- Displays exact identity differences and backend-owned validation gate outcomes without reading hidden result payloads or generating a winner, rank, score, recommendation, promotion, or superiority claim.
- Persists both exact run/invocation pairs in the URL and automatically reloads the same comparison.
- Focused comparison tests pass.
- Browser acceptance proves compare → exact result/gate rendering → URL persistence → reload same comparison → incompatible-schema refusal.
- Initial Product Runtime Acceptance #231 exposed only a contradictory browser assertion that rejected the word `superior` inside the UI's explicit no-superiority disclaimer. The acceptance assertion was corrected without changing product behavior or weakening the boundary.
- Product Runtime Acceptance #233 passed every stage on exact head `11868084dd80734244815878b9a94c693a90b2e3`, including general browser regression, the comparison proof, and both SQX browser regressions.

## PREVIOUS COMPLETED / PAUSED CHECKPOINTS OWNED BY THIS THREAD

- PR #27 / `codex/product-recovery-robustness`: backend robustness checkpoint complete at `e2181453d4b7ffc0879e6b9bb10e98014ea1f476`, Product Runtime Acceptance #186 green; paused at PR #23 integration seam.
- PR #29 / `codex/product-recovery-data-trading-context`: Data/Trading Context checkpoint complete at `8aa8d566fd1d565b6449fc0a8dde7655dda8a095`, Product Runtime Acceptance #209 green; paused at PR #23 integration seam.
- PR #30 / `codex/product-recovery-strategy-evidence`: Evidence/Proof implementation complete at `d6f0e7d244088cc5a156017f587b7234ef809b06`, Product Runtime Acceptance #225 green; substantive review/merge closure pending.
- PR #31 / `codex/product-recovery-result-compare`: Result Compare implementation complete at `11868084dd80734244815878b9a94c693a90b2e3`, Product Runtime Acceptance #233 green; substantive review/merge closure pending.

## PROTECTED CONCURRENT LANES

- PR #23 / `codex/product-recovery-native-run`: Recovery Vertical 1; owns shared `app_server.py` and canonical execution-only `run_service.py` seam until merged.
- PR #25 / `codex/product-recovery-builder-evolution`: Recovery Vertical 2; externally occupied by another LLM.
- PR #28 / `codex/product-recovery-workflow-orchestration`: externally active workflow/Custom Project vertical; do not edit or duplicate.
- PR #27 / `codex/product-recovery-robustness`: completed checkpoint owned by this thread; do not resume until PR #23 releases its shared seam.
- PR #29 / `codex/product-recovery-data-trading-context`: completed checkpoint owned by this thread; do not resume shared integration until PR #23 releases its shared seam.
- PR #30 / `codex/product-recovery-strategy-evidence`: implementation complete; avoid capability expansion unless review finds a defect or merge-time reconciliation is required.
- PR #31 / `codex/product-recovery-result-compare`: implementation complete; avoid capability expansion unless review finds a defect or merge-time reconciliation is required.
- PR #21 / `codex/product-completion-policy`: protected policy/docs authority.

## NEXT FOR THIS LLM

Do not continue PR #31 unless a substantive review finds a defect or merge-time reconciliation is required.

On the next `next`, first re-fetch all live branch/PR ownership for the three concurrent LLMs, then select the highest-value **unoccupied** end-to-end product gap that can be completed without entering another LLM's protected lane. If no independent gap remains, report the exact blocker instead of overlapping another lane.
