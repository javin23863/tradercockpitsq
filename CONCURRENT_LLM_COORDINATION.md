# Concurrent LLM Coordination

**Coordination snapshot:** 2026-08-31 06:18 ICT (UTC+07:00)

At this moment, the operator has confirmed that **three LLMs are operating concurrently in this repository**. Every assistant/agent must account for that concurrency before planning, editing, rebasing, merging, or handing off work.

This file is a coordination snapshot, not a substitute for checking live GitHub state. Before touching a slice, verify the current PR/branch/issue ownership and changed-file surface. Do not rely on conversational memory alone.

## Current protected lanes

| Lane | Current owner surface | Protected scope |
| --- | --- | --- |
| Recovery Vertical 1 | PR #23 — `codex/product-recovery-native-run` | Native Builder candidate custody → native Retester execution → durable result/readback. Owns the shared `app_server.py` native-run/Retester routing surface until accepted/merged. |
| Recovery Vertical 2 | PR #25 — `codex/product-recovery-builder-evolution` | TraderCockpit Builder/evolution candidate production: construction config → population/evolution → objective/ranking → canonical persistence → Candidates API/read model → operational Candidates UI after the PR #23 integration boundary clears. |
| Repository policy/docs | PR #21 — `codex/product-completion-policy` | Governing documentation/policy, including `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, and the adversarial-review document. |

The table reflects the live ownership observed at this timestamp. It does **not** assert the human/model identity behind each branch. If the actual third LLM assignment differs from this snapshot, the first agent that detects the mismatch must update this file before editing any overlapping surface.

## Mandatory anti-overlap procedure

Before starting or resuming work, every LLM must:

1. Fetch current `main`, open PRs, and the coordination issue/PR relevant to the intended slice.
2. Identify the exact branch/PR that owns the capability and the files likely to be touched.
3. Compare that file surface against the other two active lanes.
4. If another LLM already owns the same capability or shared file surface, do not edit it. Either remain blocked at the declared integration boundary or select a genuinely non-overlapping slice.
5. Keep each lane on its own branch/worktree. Never switch, reset, clean, stash, rewrite, or reuse another lane's mutable checkout.
6. Treat information from another lane as external state that must be re-fetched from GitHub before use. Do not mix stale conversational state from one lane into another.
7. Re-check ownership immediately before rebases, shared-server changes, UI integration, or merges because those are the highest-collision points.
8. When ownership changes, a PR merges, or one of the three LLMs changes slices, update this file with a new timestamp before overlapping work begins.

## Current integration boundary for Recovery Vertical 2

PR #25 must not perform the final shared-server integration while PR #23 still owns the unmerged `app_server.py`/native-run surface. The legal continuation is:

`PR #23 accepted/merged → re-fetch exact main → rebase PR #25 → minimal Builder route wiring without changing Retester behavior → operational Candidates UI → browser search/restart/reopen E2E → exact-head Product Runtime Acceptance → review closure`

If another LLM starts working on any part of that Recovery Vertical 2 sequence, PR #25's owner must stop before editing and resolve ownership first. Two LLMs must never independently implement the same integration slice.

## Snapshot maintenance

This record is intentionally timestamped because concurrent ownership is transient. A later agent must not assume the three lanes above are still current without checking live repository state.
