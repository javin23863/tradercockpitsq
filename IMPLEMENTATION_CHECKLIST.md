# TraderCockpit Product Implementation Map

This is the binding implementation and acceptance map for `tradercockpitsq`. Read it with `AGENTS.md` and `docs/product-architecture-v1.md`.

## Scope rule

TraderCockpit is a TraderCockpit-owned strategy research and trading-system development product. StrategyQuant X 144.2953 is a major behavioral reference and compatibility target, but it is not the completeness gate for the product.

There is no Phase 0 intake and no Phase 1 intake in this product. Do not use the old Futures-repository workflow, `phase01_intake`, or equivalent phase terminology to plan or gate implementation. `javin23863/futures` remains quarantined unless the user explicitly reverses that decision.

## Implementation authority

For each capability, classify material behavior using `AGENTS.md`:

- **Class A — observed/recovered:** reproduce useful SQX behavior that is known.
- **Class B — reconstructed:** implement deterministic behavior consistent with observations when hidden SQX detail is missing.
- **Class C — TraderCockpit-owned:** design the missing behavior required for a coherent product when SQX does not define it or the evidence is unavailable.
- **Class D — producer/external truth:** never fabricate market data, producer results, custody, certification, or external side effects.

Screenshots, `.cfx` projects, presets, configs, recovered code, native runs, exported results, and runtime traces should be inspected when relevant. Their absence does **not** block Class B or Class C behavior.

## Live-state rule

This file defines product direction and acceptance requirements. It is **not** an authority for transient GitHub state.

- Before acting on a PR head, CI result, Codex availability statement, ownership claim, or merge blocker recorded in prose, query the live repository state.
- Current PR head SHA, current workflow results, current review threads/comments, and the latest authoritative Codex activity supersede older receipts, issue text, handoffs, and historical quota notices.
- Historical Codex quota/unavailability text must not be carried forward after capacity returns. When Codex is available, request the required fresh exact-head reviews and continue the review/correction/merge path immediately.
- If a branch has moved, only the current head is actionable. Do not plan from a stale SHA merely because it is written below.
- Stale status prose must be corrected when encountered, but correcting prose is never a substitute for implementing, testing, reviewing, or merging product code.

## Product rules that must not regress

- TraderCockpit must remain simpler and lower-friction than SQX at the UI layer.
- Do not clone SQX settings one-for-one.
- Do not invent a second unrelated pipeline beside the canonical product path.
- Do not leave ordinary product gaps merely because exact SQX evidence is unavailable; reconstruct or design the missing behavior and label its authority correctly.
- Search/evolution fitness and ranking remain distinct from validation/champion status.
- Producer-native metrics must be truthful when represented as producer-native facts; TraderCockpit may also define its own clearly identified derived metrics.
- Apollo guides through explicit user actions and cannot silently launch compute, certify/promote results, export, delete evidence, or fabricate external truth.
- Production code must not import recovered/reference trees or another repository at runtime.
- A capability slice is not complete until its intended user path is operational end to end.
- Reviewed work on an open PR is not “present on main” until it is actually merged; status language must distinguish merged foundation from reviewed/unmerged recovery work.

## Verified product foundation on `main`

The following is already present on the canonical `main` product line or merged product history:

- [x] TraderCockpit-owned production namespace under `product/tradercockpit`.
- [x] Deterministic canonical serialization and content-addressed custody.
- [x] Strategy/candidate/run/result/lifecycle/evidence identity handling.
- [x] Filesystem-backed persistence and verified read model.
- [x] Product server and browser acceptance harness.
- [x] Accepted TraderCockpit UI foundation/prototype direction with persistent Apollo.
- [x] Source-bound SQX preset/runtime control foundation.
- [x] Native SQX Builder output import/candidate custody foundation.
- [x] Native SQX 144.2953 Retester task-1 evaluator with exact `SQTradingLib.jar` build verification.
- [x] Isolated native Retester evaluator execution foundation.
- [x] Native GA variation evidence retained for Builder evolutionary-search implementation.

These ingredients do not imply that all corresponding user-visible verticals are already complete on `main`.

## Reviewed recovery state outside `main`

Review status must remain separate from merge status. The entries below are historical exact-head receipts; always apply the live-state rule before acting on them.

- **Recovery Vertical 1 — native candidate to Retester result:** reviewed and executable-complete on PR #23 exact head `479003a59303de61db6115bcaab504f34473ce0d`. It connects immutable imported native archive/candidate custody → producer-derived Retester context → shared run authority → execution `completed` → durable native result archive → exact readback/results UI. It is not part of `main` until merged.
- **SQX preset-control hardening:** reviewed and executable-complete on PR #2 exact head `48ce8992fea12412dd2505c04ced0d32f73b6896`, stacked on PR #23. It removes the unauthenticated localhost command channel, requires explicit trusted launcher identity, stages verified preset bytes, preserves partial native side effects, and protects the product launch request boundary. It is not part of `main` until merged.
- **Recovery Vertical 2 — Builder/evolution candidate production:** actively owned by PR #25. Other lanes must not duplicate or overlap that implementation while it is active.

## Product capability map

### Strategy construction / Builder

Reference surfaces from SQX include:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks → Ranking → Notes`

TraderCockpit should expose these capabilities through a simpler construction experience.

Required product behavior:

- [ ] create a strategy configuration from user intent;
- [ ] support single and multi-timeframe/symbol construction where the runtime supports it;
- [ ] support template-based construction and improvement of existing strategies;
- [ ] model trading direction/style/build mode;
- [ ] model condition/period, stop-loss, profit-target, and parts-to-improve behavior;
- [ ] persist configuration and hand it to the canonical generation/evaluation path;
- [ ] expose a usable construction flow in the TraderCockpit UI.

Use Class A behavior where SQX is known, Class B where observable semantics are sufficient, and Class C to close ordinary product gaps.

### Genetic evolution / candidate generation

Reference controls include generations, population, crossover, mutation, islands, migration, initial population, decimation, fresh blood, restart, and stagnation behavior.

Native evidence covers bounded baseline, crossover-only, mutation-only, balanced crossover/mutation, fresh-blood replacement, and four-island topology runs. Reviewed ingredient PRs additionally preserve/reconstruct ranking, lineage, decimation, fresh-blood, and migration contracts. They remain ingredients until consumed by the canonical vertical.

Required product behavior:

- [ ] complete a working candidate-generation loop;
- [ ] define parent selection, crossover, mutation, replacement, island behavior, termination, and reproducibility;
- [ ] use recovered/native SQX semantics where available;
- [ ] reconstruct or design deterministic semantics for unresolved internals instead of leaving the loop incomplete;
- [ ] record lineage and generation metadata;
- [ ] feed generated candidates into the canonical evaluation/ranking path;
- [ ] expose progress and usable results to the product UI.

Restart/stagnation behavior may be TraderCockpit-owned until better SQX evidence arrives; it must not be falsely described as recovered SQX behavior.

### Data and trading context

Required product behavior:

- [ ] model engine, symbol, timeframe, date range, precision, spread, slippage, commission, IS/OOS segmentation, and session/trading options actually used by TraderCockpit runtimes;
- [ ] bind exact producer configuration identity where native execution depends on it;
- [ ] provide TraderCockpit-owned defaults/validation where SQX evidence does not define a needed product rule;
- [ ] avoid asking the UI for values that no runtime consumes.

### Building blocks and order/exit behavior

Required product behavior:

- [ ] provide a supported block catalog;
- [ ] implement block selection, weighting, and parameter generation;
- [ ] implement supported order/exit behavior;
- [ ] clearly distinguish reconstructed/TraderCockpit-owned block semantics from recovered SQX behavior;
- [ ] integrate the blocks into actual candidate generation rather than leaving them as configuration-only objects.

### Money management / ATM

Required product behavior:

- [ ] model initial capital and position sizing;
- [ ] implement a coherent TraderCockpit sizing path;
- [ ] reproduce SQX behavior where known, reconstruct where possible, and use explicit TraderCockpit-owned rules where necessary;
- [ ] keep risk and result calculations internally consistent.

### Ranking and filtering

Required product behavior:

- [ ] support ranking objectives used by the product;
- [ ] support filtering and databank/candidate-capacity behavior;
- [ ] define deterministic tie behavior;
- [ ] integrate ranking into candidate flow;
- [ ] keep ranking distinct from validation/promotion;
- [ ] do not block ranking merely because the complete SQX objective catalog or tie algorithm is not recovered.

### Testing / robustness

Reference methods include What If, Monte Carlo trade manipulation, higher precision, additional markets, Monte Carlo retest, sequential optimization, parameter permutation, Walk-Forward Optimization, and Walk-Forward Matrix.

Required product behavior:

- [ ] provide a canonical robustness-test model;
- [ ] implement useful methods progressively through real execution paths;
- [ ] preserve producer truth for externally generated/native results;
- [ ] allow TraderCockpit-owned deterministic implementations where native SQX behavior is not required for truthfulness;
- [ ] persist outcomes and connect them to validation/results surfaces.

Reviewed robustness primitives/settings contracts are ingredients only until they are consumed by this real execution/results path.

### Workflow orchestration / Custom Projects

SQX Custom Projects are useful reference task graphs, but TraderCockpit requires its own coherent orchestration model.

Required product behavior:

- [ ] represent ordered tasks, dependencies, databank/result custody, branching/looping, and termination;
- [ ] import/translate an SQX `.cfx` project when available and sufficiently understood;
- [ ] reconstruct task behavior from visible/config evidence when possible;
- [ ] define TraderCockpit-owned orchestration semantics when no authoritative SQX project is available;
- [ ] never leave workflow automation absent solely because a particular `.cfx` file was not recovered;
- [ ] do not revive the quarantined Futures intake pipeline.

### Retester / results / proof

Reference result areas include Overview, List of trades, Equity chart, Trade analysis, Portfolio correlation, Strategy config, Source Code, and additional analyses.

Merged foundation:

- [x] native Retester task-1 evaluator exists;
- [x] native Builder output can enter TraderCockpit candidate custody;
- [x] native trade CSV evidence exists as reference evidence.

Product-completion requirements:

- [ ] merge one connected imported-candidate → producer-derived Retester context → shared run authority → durable result → exact readback/results UI vertical into the canonical product line (reviewed implementation exists on PR #23);
- [ ] type truthful native producer metrics where their meaning is known;
- [ ] define clearly identified TraderCockpit-derived metrics where useful and mathematically defined;
- [ ] expose trades, equity, analysis, configuration, and proof through the simplified results surface;
- [ ] keep execution `completed`, robustness/validation decisions, and champion/promotion state separate;
- [ ] connect results back to the strategy/candidate/run identities that produced them.

## UI mapping guardrail

The frontend should guide the user through intent and actions rather than reproduce every SQX panel.

Use progressive disclosure:

- strategy intent → construction/configuration;
- candidate search → generation/evolution and ranking;
- testing → backtest/robustness/validation;
- workflow automation → canonical TraderCockpit task orchestration;
- results/proof → run/result/trade evidence;
- Apollo → explanation and explicit configuration/action assistance.

The UI must consume real backend state and actions. It may not use hard-coded workflow fiction to hide missing backend functionality.

## Acceptance gate for every vertical slice

Before declaring a slice complete, require all applicable:

- [ ] user-visible outcome and end-to-end path are named;
- [ ] each material behavior is identified as Class A, B, C, or D where the distinction matters;
- [ ] relevant SQX/reference evidence was inspected when available;
- [ ] canonical input/output/configuration identities are preserved where applicable;
- [ ] malformed input fails safely;
- [ ] Class D facts are never fabricated;
- [ ] reconstructed or TraderCockpit-owned behavior has deterministic tests;
- [ ] runtime execution is tested where execution is claimed;
- [ ] persistence/custody is tested where state crosses process/user actions;
- [ ] UI/API uses canonical backend behavior rather than constants or placeholders;
- [ ] no duplicate generic pipeline or Futures-derived fallback was introduced;
- [ ] focused and full product/browser acceptance remain green on the exact current head;
- [ ] reviewed/unmerged work is not misrepresented as merged product state.

## Current implementation direction

Stop planning as a sequence of microscopic “source-proven parity” primitives. Continue by closing the highest-value non-overlapping **vertical product gap** on the canonical path.

Recovery Vertical 1 has an executable-complete reviewed implementation on PR #23; do not create a competing native-run authority. The adjacent preset-control correction is reviewed on PR #2. Recovery Vertical 2 is actively owned by PR #25; do not duplicate that lane.

For workflow, robustness, and results, apply the same rule: each lane should produce an operational product capability, not wait indefinitely for perfect SQX evidence and not treat an isolated ingredient as a completed product slice.
