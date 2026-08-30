# TraderCockpit Agent Execution Workflow

This file is repository-level operating policy for planning, review, implementation, correction, testing, and agent handoffs.

## 1. Canonical production line

- `main` is the canonical TraderCockpit production branch.
- If GitHub's repository default branch is not `main`, do not treat the default checkout as product authority; explicitly pin `main` before inspection, planning, editing, or acceptance.
- `codex/repo-consolidation` and the earlier product/UI implementation branches are historical evidence, not active development spines.
- Accepted checkpoint branches are evidence snapshots. Do not move or rewrite them.
- SQX extraction, parity, runtime-smoke, and plugin branches are reference/experimental lanes only. Their branch names, recovered classes, and runtime experiments are not product capability authority.
- `javin23863/futures` is quarantined. Do not inspect, recover from, copy from, test against, depend on, or use it as an acceptance gate unless the user explicitly reverses this rule.

## 2. Assistant-first execution is mandatory

The primary assistant must do every task it can perform in its current environment before delegating any work to a desktop or other external agent.

This includes, when available through connected tools or a sandbox:

- inspect repository state, branches, commits, diffs, files, PRs, CI, and history;
- review implementation work and identify defects;
- edit, create, delete, commit, and push repository files;
- correct code directly rather than returning a correction prompt;
- provision an isolated sandbox/checkout and run tests, linters, syntax checks, scripts, builds, browsers, or runtimes;
- inspect produced artifacts and compare executable evidence against acceptance criteria;
- perform product/architecture planning and define exact implementation boundaries.

Do not delegate merely because a desktop agent is available or because a task was previously assigned to one.

## 3. Mandatory pre-delegation capability check

Before sending any task to a desktop agent:

1. use connected repository tools for inspection, review, edits, commits, pushes, and remote verification;
2. when execution is required, attempt an isolated sandbox/checkout with available runtime tools;
3. use available test/build/browser/runtime tools directly when they can execute the acceptance proof;
4. delegate only the residual operation that is genuinely impossible here.

A desktop handoff is allowed only for a concrete environment limitation such as local-only uncommitted state, unavailable OS/GUI/hardware/credential/service, or a runtime that cannot be reproduced here. State the limitation specifically.

For frontend/UI work, the desktop agent is an implementation/runtime executor, not the product or information-architecture planner unless the user explicitly overrides this rule.

## 4. Concurrent worktree isolation is mandatory

Desktop or external-agent tasks that can overlap in time must not share one mutable checkout or worktree.

- Assign each concurrent lane its own dedicated checkout/worktree before it switches branches or edits files.
- Acceptance/verification must run in a separate clean worktree pinned to the exact commit under review.
- No lane may switch branches, edit, reset, clean, stash, overwrite, or otherwise mutate another lane's worktree.
- If a checkout changes branch, HEAD, index, or working-tree contents unexpectedly, stop and preserve it exactly as found.
- Treat unknown local modifications as protected concurrent work until provenance is established.
- Remote branch equality never justifies discarding local work.
- Acceptance evidence collected before an unexpected mutation may be used only when the exact tested HEAD and completed assertions are known; post-mutation cleanliness or diff claims must not be inferred.

The primary assistant must prevent checkout collisions in its handoff instructions rather than relying on agents to detect them afterward.

## 5. Review and correction ownership

The primary assistant owns acceptance review. After any external report:

1. inspect actual repository state and diff rather than trusting the report;
2. compare implementation against the current authority and acceptance criteria;
3. fix directly any defect that can be corrected here;
4. strengthen tests directly when possible;
5. delegate only any remaining environment-bound proof;
6. do not mark a phase complete without executable evidence.

## 6. Product and reference boundary

- Production code lives under `product/**` and `web/**` and must not import recovered/reference trees.
- Reference evidence may inform a deliberately reviewed TraderCockpit-owned contract or implementation, but may not be wholesale-merged into production.
- Do not create replacement engines, fake evaluators, synthetic pass results, fabricated market data, substitute identity objects, second pipelines, or speculative fallback systems.
- Unsupported or unconnected capability must fail closed and remain visibly unavailable rather than being simulated.
- Prefer the smallest change that connects or corrects an existing implementation.

## 7. Relationship to product authority

`IMPLEMENTATION_CHECKLIST.md` is the binding implementation and acceptance index. `docs/product-architecture-v1.md` defines the current clean product architecture. This file governs who performs the work and when delegation is permitted.

Documentation is not executable proof, but deleting the authority documents is not a substitute for maintaining a clean production boundary.
