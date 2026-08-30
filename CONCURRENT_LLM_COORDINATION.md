# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 06:36:40 ICT (UTC+07:00)

At this moment, the operator has confirmed that **three LLMs are operating concurrently in this repository**. Every assistant/agent must account for that concurrency before planning, editing, rebasing, merging, or handing off work.

This file is a timestamped coordination snapshot, not a substitute for checking live GitHub state. Before touching any slice, fetch the current PR/branch/issue ownership and changed-file surface. Do not rely on conversational memory alone, and do not assume an earlier head still belongs exclusively to the same session.

## Current concurrency findings

Recovery Vertical 2 remains **occupied by another concurrent LLM**. This session previously stopped with PR #25 at head `444fcdd8d3971ee753798b7853643a526ac4b6eb`; during the ownership check PR #25 advanced externally first to `ef9ba0b93fca2f5ab4004778b1eb042e93822891` and then to `ce74a1193b3d7d246b013ad4e2b09624a05b268d` without this session creating those commits. This session therefore remains excluded from PR #25.

A fresh live search found no integration branch/PR claiming Recovery Vertical 3. PRs #8, #14, and #16 remain explicitly bounded robustness ingredients rather than an assembled execution/results vertical. This session has therefore claimed **Recovery Vertical 3 — robustness execution/results** on isolated branch `codex/product-recovery-robustness`, with draft PR #27. The claim is also recorded in Issue #24.

## Current protected surfaces

| Surface | Current branch / PR | Coordination status |
| --- | --- | --- |
| Native candidate → Retester recovery | PR #23 — `codex/product-recovery-native-run` at `479003a59303de61db6115bcaab504f34473ce0d` | Protected Recovery Vertical 1. Owns shared `app_server.py` native-run/Retester routing and the canonical execution-only run-service changes until accepted/merged. |
| Builder/evolution candidate production | PR #25 — `codex/product-recovery-builder-evolution` | **Externally active/occupied.** This session must not edit, rebase, extend, or integrate this lane. |
| Repository policy/docs | PR #21 — `codex/product-completion-policy` | Protected documentation surface. `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, and the adversarial-review document must not be edited from an unrelated product lane. |
| Robustness execution/results | PR #27 — `codex/product-recovery-robustness` | **Owned by this session.** Scope is canonical robustness configuration/execution/result custody/filtering/readback, initially using real persisted trade evidence. Must not create a second Retester/run authority or modify #23/#25 behavior. |
| Coordination record | PR #26 — `codex/llm-concurrency-coordination` | Coordination-only documentation; no product/runtime behavior. |

The operator-declared concurrent LLM count is three. The table records protected repository surfaces; it does **not** assert that every open PR corresponds one-to-one with a currently running LLM. Live ownership must be established from branch movement, explicit coordination notes, and user assignments.

## Mandatory anti-overlap procedure

Before starting or resuming work, every LLM must:

1. Fetch current `main`, open PRs, and the coordination issue/PR relevant to the intended slice.
2. Record the exact current head of the intended branch before editing.
3. Compare that head with the last head produced by the current session. Unexpected advancement is treated as another agent's protected concurrent work until proven otherwise.
4. Identify the exact capability and files likely to be touched, then compare them against all protected lanes.
5. If another LLM already owns the same capability, branch, or shared file surface, **do not edit it**. Stop that slice or choose a genuinely non-overlapping one.
6. Keep each lane on its own branch/worktree. Never switch, reset, clean, stash, rewrite, or reuse another lane's mutable checkout.
7. Treat information from another lane as external state that must be re-fetched from GitHub before use. Do not mix stale conversational state from one lane into another.
8. Re-check ownership immediately before rebases, shared-server changes, UI integration, or merges; these are the highest-collision points.
9. When ownership changes, a PR merges, or one of the three LLMs changes slices, update this file with a new timestamp before overlapping work begins.

## Recovery Vertical 2 integration boundary

The intended completion sequence remains:

`PR #23 accepted/merged → re-fetch exact main → rebase PR #25 → minimal Builder route wiring without changing Retester behavior → operational Candidates UI → browser search/restart/reopen E2E → exact-head Product Runtime Acceptance → review closure`

Because PR #25 is demonstrably being changed by another concurrent LLM, this session must not execute that sequence unless ownership is explicitly reassigned.

## Recovery Vertical 3 boundary

This session's lane is:

`canonical robustness plan → real method execution over source-owned evidence/shared execution authority → immutable robustness result custody → filtering/readback → Stress & Robustness frontend`

Current draft PR #27 implements the backend checkpoint for real Monte Carlo trade manipulation over persisted source result trade evidence. It deliberately does not modify `app_server.py` or duplicate Retester execution. System-parameter-permutation execution, shared route wiring, and the operational Stress & Robustness UI must wait for the PR #23 shared execution/server seam or be implemented only in files that do not overlap it.

Two LLMs must never independently implement the same integration sequence.

## Snapshot maintenance

This record is intentionally timestamped because concurrent ownership is transient. A later agent must not assume these ownership conditions remain current without checking live repository state first.
