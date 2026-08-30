# TraderCockpit Agent Execution Workflow

This file is repository-level operating policy for planning, review, implementation, correction, testing, and agent handoffs.

## 1. Canonical product direction

- `main` is the canonical TraderCockpit production branch.
- TraderCockpit is a TraderCockpit-owned product. StrategyQuant X 144.2953 is an important behavioral reference and compatibility target, but it is **not** the product specification, architecture, or completeness gate.
- The goal is a coherent end-to-end strategy research, construction, testing, robustness, workflow, and results product. SQX evidence should be used to reproduce useful observed behavior where available, while TraderCockpit must define the missing product behavior required to make the system actually usable.
- This is **not** a Phase 0 / Phase 1 / intake project. Do not invent, revive, or reuse Phase 0, Phase 1, `phase01_intake`, or equivalent workflow terminology for this product unless the user explicitly changes the product architecture.
- The old `javin23863/futures` repository is quarantined. Do not inspect, recover from, copy from, test against, depend on, or use it as an acceptance gate unless the user explicitly reverses this rule.
- SQX screenshots, saved `.cfx` projects, preset/configuration archives, runtime traces, native execution evidence, exported results/trades, recovered implementation, and user-supplied files are reference evidence. They inform behavior, defaults, naming, algorithms, workflows, and compatibility, but production must remain TraderCockpit-owned and may not depend on those artifacts at runtime.
- The accepted TraderCockpit prototype/UI authority defines the presentation direction. Do not clone SQX merely because an SQX capability is being used as a reference.
- Historical branches are evidence snapshots, not active implementation spines. Do not move or rewrite accepted checkpoints.

## 2. Product-first planning is mandatory

Before implementing a slice, first identify the user-visible TraderCockpit capability being completed and the end-to-end path required to make it function.

For each capability, inspect all useful evidence that exists:

1. current TraderCockpit domain/runtime/UI state;
2. relevant SQX screenshots and visible controls;
3. saved project/configuration/preset/runtime evidence where available;
4. recovered source or native runs where they materially clarify behavior;
5. accepted TraderCockpit UI mapping and surrounding product workflow.

Do not reverse this order by treating missing SQX evidence as proof that TraderCockpit cannot proceed.

The 35-shot SQX panel set remains a required reference for Builder, evolutionary search, robustness validation, custom-project orchestration, and Retester/results. The observed Builder surfaces include:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

These are useful capability references, not a restriction that TraderCockpit may only implement behavior directly proved by those screenshots.

## 3. Evidence classes and invention authority

Every material behavior should be treated as one of four classes. The class determines how implementation and tests are justified.

### Class A — observed/recovered behavior

SQX behavior, configuration, runtime effects, or recovered implementation is sufficiently clear. Reproduce the useful observable contract unless TraderCockpit intentionally simplifies or improves it.

### Class B — reconstructed behavior

The observable contract is clear enough but hidden implementation detail is missing. Infer the smallest deterministic semantics consistent with the available evidence and the TraderCockpit product model. Exact SQX internals are not required.

### Class C — TraderCockpit-owned product behavior

The product needs behavior that SQX evidence does not define, does not expose, or is not available for. **Design and implement it.** The implementation must be coherent with the surrounding domain model, deterministic where appropriate, covered by tests, and documented as TraderCockpit-owned behavior rather than falsely attributed to SQX.

This class is required to close product gaps. Missing SQX evidence is not, by itself, a reason to leave holes in workflows, orchestration, configuration, state transitions, ranking, validation, persistence, UI actions, or other normal product behavior.

### Class D — producer truth / external authority

Some facts cannot be invented because doing so would misrepresent reality. Never fabricate:

- market data or broker/exchange observations;
- native SQX output that was not actually produced;
- result statistics claimed to come from a producer when they did not;
- candidate/run/result identity or custody evidence;
- validation/champion certification without the actual governing validation logic and evidence;
- external side effects claimed to have occurred when they did not.

When a Class D fact is unavailable, fail closed for that fact while allowing the rest of the product workflow to remain functional.

## 4. Product-completion rule

The default response to a gap is **design**, not refusal.

- Do not stop because an exact SQX class, method, random-number routine, hidden algorithm, project file, metric formula, or source implementation has not been recovered.
- Do not make “source-proven,” “exact SQX semantics,” or “native evidence exists” the prerequisite for ordinary TraderCockpit functionality.
- Use SQX parity where it improves compatibility or gives a strong behavioral model. Use reconstruction where observations are enough. Use TraderCockpit-owned design where evidence ends.
- Distinguish observed facts, reconstructed choices, and TraderCockpit-owned choices in tests/comments/docs when that distinction matters.
- Prefer simple deterministic behavior over speculative complexity.
- New behavior must connect to the canonical domain model and the real execution path; do not create isolated demo contracts, duplicate pipelines, fake evaluators, or UI-only simulations.
- A capability is not complete because a low-level primitive exists. It is complete only when the user-visible path can exercise it through real configuration, orchestration, persistence/custody where applicable, results, and UI or API surfaces required by the product.

## 5. Vertical-slice planning is mandatory

Work should advance through end-to-end product slices, not an accumulation of disconnected evidence primitives.

For every slice, identify:

- the user action or product outcome being enabled;
- domain/configuration objects required;
- runtime/orchestration path required;
- persistence/custody/result path required;
- frontend/API exposure required now or explicitly deferred to the immediately adjacent slice;
- tests that prove the path works end to end;
- which behaviors are Class A, B, C, or D.

A slice that only adds a tiny contract with no path toward a usable feature should normally be expanded or combined with the adjacent work needed to make it operational.

When multiple implementation lanes are active, choose the next non-overlapping vertical slice. Do not use lane isolation as a reason to stop overall product progress.

## 6. Assistant-first execution is mandatory

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

## 7. Mandatory pre-delegation capability check

Before sending any task to a desktop agent:

1. use connected repository tools for inspection, review, edits, commits, pushes, and remote verification;
2. when execution is required, attempt an isolated sandbox/checkout with available runtime tools;
3. use available test/build/browser/runtime tools directly when they can execute the acceptance proof;
4. delegate only the residual operation that is genuinely impossible here.

A desktop handoff is allowed only for a concrete environment limitation such as local-only uncommitted state, unavailable OS/GUI/hardware/credential/service, or a runtime that cannot be reproduced here. State the limitation specifically.

For frontend/UI work, the desktop agent is an implementation/runtime executor, not the product or information-architecture planner unless the user explicitly overrides this rule.

## 8. Concurrent worktree isolation is mandatory

Desktop or external-agent tasks that can overlap in time must not share one mutable checkout or worktree.

- Assign each concurrent lane its own dedicated checkout/worktree before it switches branches or edits files.
- Acceptance/verification must run in a separate clean worktree pinned to the exact commit under review.
- No lane may switch branches, edit, reset, clean, stash, overwrite, or otherwise mutate another lane's worktree.
- If a checkout changes branch, HEAD, index, or working-tree contents unexpectedly, stop and preserve it exactly as found.
- Treat unknown local modifications as protected concurrent work until provenance is established.
- Remote branch equality never justifies discarding local work.
- Acceptance evidence collected before an unexpected mutation may be used only when the exact tested HEAD and completed assertions are known; post-mutation cleanliness or diff claims must not be inferred.

The primary assistant must prevent checkout collisions in its handoff instructions rather than relying on agents to detect them afterward.

## 9. Review and correction ownership

The primary assistant owns acceptance review. After any external report:

1. inspect actual repository state and diff rather than trusting the report;
2. compare implementation against the current TraderCockpit product architecture and the applicable Class A/B/C/D authority;
3. fix directly any defect that can be corrected here;
4. strengthen tests directly when possible;
5. delegate only any remaining environment-bound proof;
6. do not mark a work slice complete without executable evidence for the product contract it claims.

## 10. Mandatory PR and Codex review closure loop

Creating or updating a pull request starts a mandatory **review-closure** loop. Codex review is a merge/closure gate, not a serialization gate for independent implementation work.

For every PR created or materially updated by an agent:

1. record the PR's current head SHA after implementation and acceptance;
2. run all applicable executable acceptance on that exact head;
3. a head with green required executable acceptance may be reported as **executable-complete** while its Codex review is still pending, but it must not be reported as review-closed or merge-ready;
4. wait for and explicitly inspect the Codex review for that exact current head before review closure or merge, including review submissions, inline review threads, and PR-level review comments;
5. inspect every actionable Codex finding against the actual diff, product architecture, evidence class, and acceptance criteria rather than applying review text mechanically;
6. fix every valid finding directly on the same PR branch when the current environment permits it, add or strengthen regression tests where appropriate, and rerun the relevant focused and full acceptance checks;
7. treat every corrective commit as invalidating review closure for the previous head SHA and require a fresh Codex review of the new current head before merge;
8. repeat `Codex review → inspect findings → correct → test → fresh Codex review` until the latest PR head has no unresolved actionable Codex findings and all required acceptance checks pass;
9. reply to or resolve review threads only after the underlying correction has been verified; do not resolve a thread merely to clear the UI;
10. before final handoff or merge readiness, re-check that the reviewed SHA equals the current PR head SHA and that no newer commit has bypassed Codex review.

### Codex unavailability / quota exhaustion

Codex being unavailable, rate-limited, or quota-exhausted must not stall unrelated product progress.

- When the authoritative Codex actor explicitly reports that code-review capacity is unavailable or exhausted, record the affected PR/head as **review-deferred**.
- Keep the PR draft or otherwise review-open, keep `Codex Review Closure` pending, and do not merge it or call it review-closed/merge-ready.
- Continue directly with the next logically adjacent, non-overlapping product slice once executable acceptance for the current head is green.
- Do not repeatedly post `@codex review` requests while an explicit quota/unavailability response is current. Retry only after capacity is known to be available again or the user explicitly directs a retry.
- When capacity returns, request a fresh review of each still-current exact head. If a branch moved while review was unavailable, only the new current head needs review closure.
- A deferred Codex review never converts pending review state into success and never weakens executable acceptance requirements.

A PR is review-closed only when **both** conditions are true on the same current head SHA: required executable acceptance is green, and the latest substantive Codex review has no unresolved actionable findings. Draft status, previous review approval, review outage, or review of an older SHA does not waive this merge gate.

### GitHub-native Codex monitoring

The primary monitoring mechanism is event-driven GitHub state, not scheduled polling. `.github/workflows/codex-review-loop.yml` owns the mechanical review state for every PR.

- `Codex Review Closure` is the current-head status context. A new review-ready PR head is `pending` until Codex closes review on that exact SHA.
- Opening a review-ready PR, reopening it, or marking a draft ready uses Codex's native automatic review trigger when review capacity is available.
- A `synchronize` event marks the new head pending but must not generate repeated bot-authored `@codex review` requests. An authenticated user/assistant requests a fresh review when Codex capacity is available.
- The authoritative Codex GitHub actor is `chatgpt-codex-connector[bot]`. Do not infer substantive Codex review evidence from other bot or user comments, or from the mechanical workflow completing successfully by itself.
- An authoritative Codex usage-limit/unavailability response keeps `Codex Review Closure` pending with a deferred description; it is not a clean review and not a review failure finding.
- A clean Codex review is accepted only when its `Reviewed commit` value matches the current PR head. A review for an older head remains stale/pending.
- Codex inline feedback, change-request review state, or a Codex review summary containing findings makes the status fail until corrected. A corrective commit automatically starts a fresh pending cycle.
- Scheduled polling may exist only as a fallback for notification. It is not the primary monitor and cannot establish review closure.
- Before declaring a PR merge-ready, inspect the underlying Codex comments/threads and confirm both required executable acceptance and substantive `Codex Review Closure` evidence are current on the same head.

## 11. Product and reference boundary

- Production code lives under `product/**` and `web/**` and must not import recovered/reference trees.
- SQX is a reference implementation and compatibility source, not the owner of TraderCockpit's architecture.
- Class A and B behavior may reproduce SQX. Class C behavior is intentionally TraderCockpit-owned. Class D facts must remain truthful and externally grounded.
- Do not create fake producer results, fabricated market data, substitute identity/custody evidence, or false validation/champion state.
- Do create the missing deterministic product behavior needed to connect real user workflows when no external truth is being fabricated.
- Prefer one coherent canonical pipeline. Do not create replacement engines, second generic pipelines, or disconnected fallback systems beside the canonical product path.

## 12. Relationship to product authority

`IMPLEMENTATION_CHECKLIST.md` is the binding implementation/acceptance map and must follow this product-completion policy. `docs/product-architecture-v1.md` defines the canonical product architecture and must also follow it. Neither document may reintroduce an “exact SQX evidence or unavailable” rule for ordinary Class B or Class C product behavior.

SQX evidence remains valuable and should be inspected whenever relevant, but absence of exact SQX evidence is a blocker only for claims that depend on Class D external/producer truth or when no meaningful product contract can be designed without fabricating such truth.
