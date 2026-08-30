# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 06:54:01 ICT (UTC+07:00)

## CURRENT THREAD STATUS

**Recovery Vertical 3 / PR #27 checkpoint is COMPLETE and PAUSED at the PR #23 server seam. This LLM has moved to the next unoccupied vertical: Data & Trading Context / PR #29.**

PR #29 is currently **ACTIVE, NOT FINISHED**. Its backend/frontend checkpoint is implemented and exact-head Product Runtime Acceptance is running.

## COMPLETED BEFORE THE LANE CHANGE

- PR #27 / `codex/product-recovery-robustness` reached a green backend robustness checkpoint at exact head `e2181453d4b7ffc0879e6b9bb10e98014ea1f476` with Product Runtime Acceptance run #186 green.
- PR #27 remains paused and untouched until PR #23 releases the shared `app_server.py` / execution-only run seam.
- The externally active workflow branch `codex/product-recovery-workflow-orchestration` was detected before claiming a next vertical and is protected from this LLM.

## ACTIVE LANE — DATA & TRADING CONTEXT

Branch: `codex/product-recovery-data-trading-context`
Draft PR: #29
Base: exact `main` `2a258d201a0575785382af42e779a452786d21fe`

Completed in the current checkpoint:

- reuses canonical `DataSpecV1` and `ExecutionSpecV1` rather than inventing another run/execution model;
- requires explicit user-supplied symbol, timeframe, source, dataset revision, timezone, session calendar, and exact date window;
- labels starting cash/currency/fill model as TraderCockpit research assumptions rather than producer or native-SQX facts;
- persists canonical DataSpec/ExecutionSpec objects through `FileObjectStore`;
- derives a deterministic composite `data-trading-context` identity from those exact refs and stores only a verified reopen/list catalog outside immutable object custody;
- adds HTTP-neutral create/list/read contracts and an adapter that cleanly falls through for other product authorities;
- adds an operational `/explore/data` enhancer using the repository's existing MutationObserver integration pattern without modifying `web/app.mjs`;
- preserves the existing Market Data coverage placeholder so provider availability remains producer-owned rather than inferred from a saved research context;
- focused frontend tests passed 5/5 locally;
- exact-head CI has already passed clean production install, production boundary, product tests, frontend syntax, and UI tests; browser/SQX regression stages remain in progress at this snapshot.

## NEXT — DATA & TRADING CONTEXT

1. Finish exact-head Product Runtime Acceptance and correct any failure if one appears.
2. After PR #23 is accepted/merged, rebase PR #29.
3. Minimally register `/api/data-contexts` and `/api/data-contexts/read` in the canonical `app_server.py` by delegating to the existing adapter; do not change Retester behavior.
4. Prove browser create → canonical DataSpec/ExecutionSpec custody → reload/reopen with identical refs against the canonical TraderCockpit server.
5. Connect Run Setup to an explicitly selected Data/Trading Context only for compatible TraderCockpit-owned runs. Native SQX Retester runs continue using producer-derived context from PR #23.
6. Run exact-head full Product Runtime Acceptance and substantive review closure.

## FINISH CONDITION — DATA & TRADING CONTEXT

This active vertical is finished only when this path is operational on the canonical server:

`Explore → Market Data → enter explicit research data/trading assumptions → save canonical DataSpec + ExecutionSpec → reload/reopen same refs → select exact context for a compatible TraderCockpit run`

Current state: **ACTIVE, NOT FINISHED.**

## PROTECTED CONCURRENT LANES

- PR #23 / `codex/product-recovery-native-run`: Recovery Vertical 1; owns shared `app_server.py` and canonical execution-only run-service seam until merged.
- PR #25 / `codex/product-recovery-builder-evolution`: Recovery Vertical 2; externally occupied by another LLM.
- `codex/product-recovery-workflow-orchestration`: externally active workflow/Custom Project vertical; do not edit or duplicate.
- PR #27 / `codex/product-recovery-robustness`: this LLM's completed checkpoint, paused at PR #23 boundary and not being modified while PR #29 is active.
- PR #29 / `codex/product-recovery-data-trading-context`: current active lane for this LLM.
- PR #21 / `codex/product-completion-policy`: protected policy/docs authority.

Every LLM must re-fetch live GitHub state before resuming, rebasing, touching shared files, or integrating across lanes.
