# Adversarial implementation review — 2026-08-31

This review treats a passing contract test as insufficient unless the product capability can be exercised through the canonical TraderCockpit path. It records both the stop-the-line findings that triggered recovery and their current disposition. A reviewed open PR is evidence of a corrected implementation, but it does not become canonical `main` state until merged.

## Findings at the recovery checkpoint

1. **Acceptance rewarded incompletion.** Browser regression asserted broad pending/unavailable states, so making a real capability operational could break the suite.
2. **Native output custody and native Retester execution were disconnected.** Imported SQX Builder output had candidate custody while the native Retester evaluator existed separately, but there was no truthful end-to-end user path joining them.
3. **The narrower native-run work had contradictory input authority and duplicate orchestration risk.** Request-supplied generic data/execution assumptions conflicted with the producer-derived context required by native Retester, and a separate execution service risked becoming a second run authority.
4. **Several open PRs were research/implementation primitives rather than product slices.** Ranking, lineage, decimation, fresh blood, migration, topology, and robustness contracts were useful ingredients but did not by themselves create usable product outcomes.
5. **UI authority intentionally disabled backend paths that were becoming available.** Candidates and Run Setup contained pending/disabled states rather than joined product actions.
6. **Builder/evolution was not a complete candidate-generation system.** The existing GA kernel and adjacent contracts did not yet own construction → generation → evaluation → ranking/filtering → candidate custody → usable Candidates UI.

## Current disposition

### Recovery Vertical 1 — native candidate to Retester result

Reviewed implementation: PR #23 exact head `479003a59303de61db6115bcaab504f34473ce0d`.

It now proves:

`native Builder output -> immutable TraderCockpit archive/candidate custody -> producer-derived Retester context -> shared run authority -> execution completed -> durable native result archive -> exact product readback/results UI`

Corrections include single-snapshot import identity, durable native archive custody independent of the mutable Builder databank, producer-derived native contexts, one shared run authority, execution-only `completed` lifecycle truth, durable native result custody, temporary SQX workspace cleanup, native result readback, and browser acceptance that expects the real action when the capability is available.

PR #6, the earlier narrower execution-lifecycle branch, was closed unmerged as superseded after confirming that its useful `completed` lifecycle behavior was subsumed by PR #23 and its duplicate `execution_service.py` authority should not survive.

This reviewed vertical is not canonical `main` behavior until merged.

### SQX preset-control hardening adjacent to Recovery Vertical 1

Reviewed implementation: PR #2 exact head `48ce8992fea12412dd2505c04ced0d32f73b6896`, stacked on PR #23.

The original localhost-command-channel design inherited multiple control/custody defects. The corrected canonical preset path removes unauthenticated listener reuse, requires a separately trusted launcher SHA-256 because the retained readable archive contains no authoritative launcher binary/hash, stages the exact verified preset snapshot, invokes direct native project commands, preserves partial native side effects, rejects ordinary cross-site form POSTs, and avoids claiming that command-process success proves generated strategies or validation results.

This reviewed correction is not canonical `main` behavior until merged.

### Ingredient PRs

Lineage, Custom Project topology, system-parameter permutation settings, trade-skip manipulation, decimation, fresh-blood, migration, and other bounded contracts must continue to be described as **implementation ingredients** unless and until they are consumed by a complete vertical path. Green focused/full tests on an isolated ingredient do not change this rule.

### Recovery Vertical 2 — Builder/evolution candidate production

Active owner: PR #25.

The intended path remains:

`strategy construction -> candidate generation -> evaluation -> ranking/filtering -> candidate custody -> usable Candidates UI`

PR #25 is the active non-overlapping implementation lane for that recovery vertical. Existing GA/ranking/lineage/decimation/fresh-blood/migration work should be integrated into that path rather than recreated in parallel.

## Recovery rule

No isolated SQX parity primitive should be treated as a completed product slice merely because its local contract is correct. Preserve useful branches as implementation ingredients and advance the canonical product vertically.

Normal product gaps should be closed with the Class A/B/C authority model in `AGENTS.md`. Fail closed only for Class D producer/external facts that cannot be truthfully supplied.

## Acceptance correction

Product acceptance must distinguish three things:

- **shell integrity** — routes, navigation, identity preservation, and truthful unavailable states;
- **capability integration** — when a fixture/producer is intentionally available, the corresponding action becomes enabled and completes its real backend path;
- **truth boundaries** — unavailable Class D producer facts are never fabricated.

Global assertions that every canonical route is pending are prohibited. Pending/unavailable assertions must be capability-specific and must disappear when the tested producer/capability is intentionally available.

Every capability integration must include at least one joined test spanning the seam that previously separated its components. Unit tests that mock both sides of the seam are not sufficient acceptance.

Acceptance receipts must name the exact tested head. A reviewed/unmerged branch must not be described as canonical product state, and a mechanical review-monitor workflow success must not be substituted for an actual substantive Codex review when repository policy requires one.
