# TraderCockpit Agent Execution Workflow

This file is repository-level operating policy for planning, review, implementation, correction, testing, and agent handoffs.

## 1. Canonical product direction

- `main` is the canonical TraderCockpit production branch.
- TraderCockpit is implementing TraderCockpit-owned backend/runtime contracts that reproduce the observed capabilities, presets, projects, workflows, and execution behavior of StrategyQuant X 144.2953 while presenting them through a simpler TraderCockpit interface.
- This is **not** a Phase 0 / Phase 1 / intake project. Do not invent, revive, or reuse Phase 0, Phase 1, `phase01_intake`, or equivalent workflow terminology for this product unless StrategyQuant X itself supplies that exact concept for the capability being implemented.
- The old `javin23863/futures` repository is not product architecture. It is quarantined. Do not inspect, recover from, copy from, test against, depend on, or use it as an acceptance gate unless the user explicitly reverses this rule.
- The observed SQX UI captures, saved `.cfx` projects, preset/configuration archives, runtime traces, native execution evidence, and other user-supplied SQX files are behavioral authority for what SQX actually exposes and does. They are not production runtime dependencies and must not be wholesale-imported.
- The accepted TraderCockpit prototype/UI authority defines how those SQX capabilities are simplified and presented to the user. Do not clone the SQX interface merely because its backend workflow is authoritative.
- Historical product/UI branches are evidence snapshots, not active implementation spines. Do not move or rewrite accepted checkpoints.

## 2. Mandatory SQX grounding before planning or editing

Before changing product architecture, backend workflow, or frontend behavior for an SQX-backed capability:

1. inspect the relevant SQX screenshot(s), not only prose summaries;
2. inspect the corresponding saved project/configuration/runtime evidence when backend behavior is involved;
3. inspect the current TraderCockpit prototype or accepted UI mapping for how the capability should be exposed;
4. identify the exact SQX control, task, preset, project step, result surface, or producer being reproduced;
5. only then plan or edit implementation files.

The 35-shot SQX panel set is a required visual reference for Builder, evolutionary search, robustness validation, custom-project orchestration, and Retester results. The observed Builder configuration surfaces include:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

These are SQX capability/configuration surfaces, not an assistant-created product phase sequence. Likewise, Custom Projects define their own ordered task graph. Preserve the order proved by the project configuration/runtime evidence rather than inserting a generic intake pipeline.

### 2A. Behavioral reconstruction from incomplete evidence is required

The goal is behavioral reconstruction and product adaptation, not source-code archaeology. Exact recovered/decompiled implementation is useful evidence when available, but it is **not** a prerequisite for implementing an SQX-backed capability.

- Treat screenshots, user-uploaded files, saved `.cfx`/project archives, XML/config values, presets, labels, control ordering, defaults, ranges, observed outputs, runtime traces, and native runs as a combined behavioral evidence set.
- A reference artifact being excluded from production runtime means only that production must not depend on it. It does **not** mean the artifact is unusable for reconstructing behavior.
- Do not stop merely because the exact SQX class, method, random-number routine, hidden algorithm, or source implementation has not been recovered.
- When direct implementation evidence is incomplete, infer the smallest semantics consistent with all available observations. Preserve observable contracts such as input meaning, defaults, ranges, ordering, state transitions, output shape, custody, and user-visible consequences.
- Separate **observed/proven facts** from **reconstructed implementation choices** in tests, comments, or implementation notes where the distinction matters. Do not present an inferred internal detail as recovered fact.
- When several hidden implementations could explain the same evidence, choose the simplest deterministic TraderCockpit-owned implementation that satisfies the observed contract and current product use case. Do not invent extra knobs, phases, data, or claims that the evidence does not require.
- Reconstruct SQX capability semantics, then adapt them to the accepted TraderCockpit interface. Preserve behavior and authority boundaries; do not preserve SQX UI complexity merely for visual parity.
- Convert evidence into executable tests wherever possible: control/default parsing, ordering, allowed ranges, boundary behavior, deterministic invariants, output structure, and observed before/after effects.
- Additional native runs or recovered source should refine a reconstruction when they materially distinguish behavior, but absence of that evidence alone is not a blocker.
- Stop or fail closed only when proceeding would require fabricating market data, producer results, identity/custody evidence, validation/champion status, unsupported external side effects, or a behavior whose meaningful contract cannot be resolved from the available evidence. Missing hidden implementation detail by itself is not such a stop condition.

This reconstruction rule applies to backend and frontend work, including Builder, evolutionary search, robustness tests, Retester behavior, and custom-project orchestration.

## 3. Assistant-first execution is mandatory

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

## 4. Mandatory pre-delegation capability check

Before sending any task to a desktop agent:

1. use connected repository tools for inspection, review, edits, commits, pushes, and remote verification;
2. when execution is required, attempt an isolated sandbox/checkout with available runtime tools;
3. use available test/build/browser/runtime tools directly when they can execute the acceptance proof;
4. delegate only the residual operation that is genuinely impossible here.

A desktop handoff is allowed only for a concrete environment limitation such as local-only uncommitted state, unavailable OS/GUI/hardware/credential/service, or a runtime that cannot be reproduced here. State the limitation specifically.

For frontend/UI work, the desktop agent is an implementation/runtime executor, not the product or information-architecture planner unless the user explicitly overrides this rule.

## 5. Concurrent worktree isolation is mandatory

Desktop or external-agent tasks that can overlap in time must not share one mutable checkout or worktree.

- Assign each concurrent lane its own dedicated checkout/worktree before it switches branches or edits files.
- Acceptance/verification must run in a separate clean worktree pinned to the exact commit under review.
- No lane may switch branches, edit, reset, clean, stash, overwrite, or otherwise mutate another lane's worktree.
- If a checkout changes branch, HEAD, index, or working-tree contents unexpectedly, stop and preserve it exactly as found.
- Treat unknown local modifications as protected concurrent work until provenance is established.
- Remote branch equality never justifies discarding local work.
- Acceptance evidence collected before an unexpected mutation may be used only when the exact tested HEAD and completed assertions are known; post-mutation cleanliness or diff claims must not be inferred.

The primary assistant must prevent checkout collisions in its handoff instructions rather than relying on agents to detect them afterward.

## 6. Review and correction ownership

The primary assistant owns acceptance review. After any external report:

1. inspect actual repository state and diff rather than trusting the report;
2. compare implementation against the current SQX behavior authority and TraderCockpit UI authority;
3. fix directly any defect that can be corrected here;
4. strengthen tests directly when possible;
5. delegate only any remaining environment-bound proof;
6. do not mark a work slice complete without executable evidence.

## 7. Mandatory PR and Codex review closure loop

Creating or updating a pull request starts a mandatory **review-closure** loop. Codex review is a merge/closure gate, not a serialization gate for independent implementation work.

For every PR created or materially updated by an agent:

1. record the PR's current head SHA after implementation and acceptance;
2. run all applicable executable acceptance on that exact head;
3. a head with green required executable acceptance may be reported as **executable-complete** while its Codex review is still pending, but it must not be reported as review-closed or merge-ready;
4. wait for and explicitly inspect the Codex review for that exact current head before review closure or merge, including review submissions, inline review threads, and PR-level review comments;
5. inspect every actionable Codex finding against the actual diff, source authority, and acceptance criteria rather than applying review text mechanically;
6. fix every valid finding directly on the same PR branch when the current environment permits it, add or strengthen regression tests where appropriate, and rerun the relevant focused and full acceptance checks;
7. treat every corrective commit as invalidating review closure for the previous head SHA and require a fresh Codex review of the new current head before merge;
8. repeat `Codex review → inspect findings → correct → test → fresh Codex review` until the latest PR head has no unresolved actionable Codex findings and all required acceptance checks pass;
9. reply to or resolve review threads only after the underlying correction has been verified; do not resolve a thread merely to clear the UI;
10. before final handoff or merge readiness, re-check that the reviewed SHA equals the current PR head SHA and that no newer commit has bypassed Codex review.

### Codex unavailability / quota exhaustion

Codex being unavailable, rate-limited, or quota-exhausted must not stall unrelated product progress.

- When the authoritative Codex actor explicitly reports that code-review capacity is unavailable or exhausted, record the affected PR/head as **review-deferred**.
- Keep the PR draft or otherwise review-open, keep `Codex Review Closure` pending, and do not merge it or call it review-closed/merge-ready.
- Continue directly with the next logically adjacent, non-overlapping implementation slice once executable acceptance for the current head is green.
- Do not repeatedly post `@codex review` requests while an explicit quota/unavailability response is current. Retry only after capacity is known to be available again or the user explicitly directs a retry.
- When capacity returns, request a fresh review of each still-current exact head. If a branch moved while review was unavailable, only the new current head needs review closure.
- A deferred Codex review never converts pending review state into success and never weakens executable acceptance requirements.

A PR is review-closed only when **both** conditions are true on the same current head SHA: required executable acceptance is green, and the latest Codex review has no unresolved actionable findings. Draft status, previous review approval, review outage, or review of an older SHA does not waive this merge gate.

### GitHub-native Codex monitoring

The primary monitoring mechanism is event-driven GitHub state, not scheduled polling. `.github/workflows/codex-review-loop.yml` owns the mechanical review state for every PR.

- `Codex Review Closure` is the current-head status context. A new review-ready PR head is `pending` until Codex closes review on that exact SHA.
- Opening a review-ready PR, reopening it, or marking a draft ready uses Codex's native automatic review trigger when review capacity is available.
- A `synchronize` event marks the new head pending but must not generate repeated bot-authored `@codex review` requests. An authenticated user/assistant requests a fresh review when Codex capacity is available.
- The authoritative Codex GitHub actor is `chatgpt-codex-connector[bot]`. Do not infer Codex review state from other bot or user comments.
- An authoritative Codex usage-limit/unavailability response keeps `Codex Review Closure` pending with a deferred description; it is not a clean review and not a review failure finding.
- A clean Codex review is accepted only when its `Reviewed commit` value matches the current PR head. A review for an older head remains stale/pending.
- Codex inline feedback, change-request review state, or a Codex review summary containing findings makes the status fail until corrected. A corrective commit automatically starts a fresh pending cycle.
- Scheduled polling may exist only as a fallback for notification. It is not the primary monitor and cannot establish review closure.
- Before declaring a PR merge-ready, inspect the underlying Codex comments/threads and confirm both required executable acceptance and `Codex Review Closure` are green on the same current head.

## 8. Product and reference boundary

- Production code lives under `product/**` and `web/**` and must not import recovered/reference trees.
- SQX evidence may define the behavior that TraderCockpit must reproduce, but the production implementation remains TraderCockpit-owned.
- An SQX capability is not considered unsupported merely because its original internal implementation has not been recovered. Apply the reconstruction rule in Section 2A first.
- Do not create replacement engines, fake evaluators, synthetic pass results, fabricated market data, substitute identity objects, second pipelines, or speculative fallback systems.
- Unsupported or genuinely unresolvable capability must fail closed and remain visibly unavailable rather than being simulated.
- Prefer the smallest change that connects, reconstructs, or corrects an existing SQX-backed implementation.
- Saved SQX projects and presets may be wired when their archived configuration, screenshots, observed behavior, or runtime evidence establishes the relationship being claimed; exact original source code is not required.

## 9. Relationship to product authority

`IMPLEMENTATION_CHECKLIST.md` is the binding implementation/acceptance map. `docs/product-architecture-v1.md` defines the product architecture and SQX-to-TraderCockpit mapping. The accepted SQX screenshot manifest and TraderCockpit prototype are visual/interaction authority and must be inspected for relevant UI/backend work.

Documentation alone is not executable proof, but executable reconstruction may be grounded in the combined SQX evidence set defined above. Acceptance should test the observable contract TraderCockpit is claiming; native SQX runtime or recovered source is required only when that additional evidence is necessary to distinguish materially different behaviors.
