# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 06:57:35 ICT (UTC+07:00)

## CURRENT THREAD STATUS

**Data & Trading Context / PR #29 checkpoint is COMPLETE and PAUSED. It is NOT FINISHED.**

Exact PR #29 head: `8aa8d566fd1d565b6449fc0a8dde7655dda8a095`.
Exact Product Runtime Acceptance: run #209 — **PASS**.

The next work for PR #29 requires the shared canonical server/run seam currently owned by PR #23. PR #23 remains open/draft and has advanced externally to head `32c2d9b605a2e90ab26ad5e7463d3aed3cb875e0`; this LLM created none of that PR #23 movement and must not modify or duplicate it.

## COMPLETED — DATA & TRADING CONTEXT

- Claimed isolated branch `codex/product-recovery-data-trading-context` only after live ownership checks found no competing Data/Trading Context lane.
- Reused canonical `DataSpecV1` and `ExecutionSpecV1`; no second execution/run pipeline or duplicate domain authority.
- Added explicit Data/Trading Context configuration for user-supplied symbol, timeframe, source, dataset revision, timezone, session calendar, and exact date window.
- Kept starting cash, currency, and fill model clearly TraderCockpit-owned research assumptions rather than producer/native-SQX facts.
- Persisted exact DataSpec/ExecutionSpec objects through `FileObjectStore`.
- Added deterministic composite `data-trading-context` identity plus verified durable list/reopen custody.
- Added HTTP-neutral create/list/read contracts and a shared-server adapter that falls through for other product authorities.
- Added operational `/explore/data` enhancement without modifying `web/app.mjs`.
- Preserved the existing market-coverage placeholder so a saved research context never claims provider availability.
- Initial Product Runtime Acceptance run #205 passed the backend/product/frontend tests and correctly failed browser regression on false wording `Producer unavailable`.
- Corrected only the frontend wording to `Context data not available to this frontend`; the acceptance rule was not weakened.
- Exact-head Product Runtime Acceptance run #209 then passed every stage, including browser regression, SQX preset browser integration, and SQX output-custody browser integration.
- PR #27 robustness remains paused and untouched at its own PR #23 integration boundary.

## NEXT — DATA & TRADING CONTEXT

**Do not continue PR #29 into shared integration until PR #23 is accepted/merged.**

After PR #23 merges:

1. Re-fetch exact `main` and rebase PR #29.
2. Minimally register `/api/data-contexts` and `/api/data-contexts/read` in canonical `app_server.py` by delegating to the existing adapter; do not alter Retester behavior.
3. Prove canonical-server browser create → persisted DataSpec/ExecutionSpec → reload/reopen with identical refs.
4. Add explicit context selection to Run Setup only for compatible TraderCockpit-owned runs. Native SQX Retester runs retain producer-derived context from PR #23.
5. Prove the selected context's exact DataSpec/ExecutionSpec refs enter canonical run binding without frontend-generated identity or fallback assumptions.
6. Run full exact-head Product Runtime Acceptance and substantive review closure.

## FINISH CONDITION — DATA & TRADING CONTEXT

This vertical is completely finished only when this path is operational:

`Explore → Market Data → enter explicit research data/trading assumptions → save canonical DataSpec + ExecutionSpec → reload/reopen same refs → select exact context → compatible TraderCockpit run binds those exact refs`

Current state: **PAUSED AT VALID PR #23 SHARED-SEAM BLOCKER. NOT FINISHED.**

## PROTECTED CONCURRENT LANES

- PR #23 / `codex/product-recovery-native-run`: Recovery Vertical 1; owns shared `app_server.py` and canonical execution-only run-service seam until merged.
- PR #25 / `codex/product-recovery-builder-evolution`: Recovery Vertical 2; externally occupied by another LLM.
- `codex/product-recovery-workflow-orchestration`: externally active workflow/Custom Project vertical; do not edit or duplicate.
- PR #27 / `codex/product-recovery-robustness`: this LLM's completed robustness checkpoint, paused at PR #23 boundary.
- PR #29 / `codex/product-recovery-data-trading-context`: this LLM's completed Data/Trading Context checkpoint, paused at PR #23 boundary.
- PR #21 / `codex/product-completion-policy`: protected policy/docs authority.

Every LLM must re-fetch live GitHub state before resuming, rebasing, touching shared files, or integrating across lanes.
