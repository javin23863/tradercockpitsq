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

## Stable reference evidence locators

These are **branch/path locators**, not frozen commit claims. Resolve each branch's current head before using it; do not replace these with historical SHAs in planning documents.

- **Accepted SQX/UI authority:** branch `codex/ui-reference-acceptance`, path `references/ui-authority/`. Use this for the retained screenshots/visual authority and UI reference evidence.
- **Recovered SQX 144.2953 source/reference archive:** branch `codex/sqx-reference-archive-20260830`. Start at `docs/module-map.md` and `docs/extraction-report.md`; recovered/decompiled source is under `sources/` (especially `sources/engine-core/` and `sources/plugins/`), with non-class assets/config/workflows under `references/`.
- **Native SQX 144.2953 runtime evidence:** branch `codex/sqx-runtime-evidence-144-2953`, path `references/strategyquant-x-144.2953/`, plus its adjacent `references/` and `docs/` inventory.
- **Capability/parity evidence branch:** `codex/sqx-capability-parity`. Use it as a discovery anchor for retained capability probes and then verify the live branch contents before relying on a claim.
- **Accepted compact TraderCockpit UI implementation checkpoints:** `checkpoint/ui-shell-accepted-2026-08-30` and `checkpoint/ui-cockpit-home-accepted-2026-08-30`; current production UI remains whatever is actually merged on `main`.

Reference branches are evidence only. Production must never import them at runtime.

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

Review status must remain separate from merge status. The entries below are useful coordination anchors; always apply the live-state rule before acting on them.

- **Recovery Vertical 1 — native candidate to Retester result:** executable-complete on PR #23 current reviewed/corrected line, exact product-acceptance head `426b5dffde96a0d9dbd584864551c4fa67d61500`. It connects immutable imported native archive/candidate custody → durable candidate rediscovery → producer-derived Retester context → shared run authority → execution `completed` → reverified durable native result archive → exact readback/results UI. It is not part of `main` until merged, and its fresh exact-head Codex review must be checked live.
- **SQX preset-control hardening:** reviewed on PR #2, stacked on PR #23. It removes the unauthenticated localhost command channel, requires explicit trusted launcher identity, stages verified preset bytes, preserves partial native side effects, and protects the product launch request boundary. Check its current head live before integration.
- **Recovery Vertical 2 — Builder/evolution candidate production:** actively owned by PR #25. Other lanes must not duplicate or overlap that implementation while it is active.
- **Recovery Vertical 3 — robustness execution/results:** actively owned by PR #27. Do not create a second robustness execution/result authority.
- **Recovery Vertical 4 — workflow orchestration/Custom Projects:** actively owned by PR #28. Do not create a second workflow engine/server; integrate its HTTP-neutral adapter into the canonical server after its dependencies land.

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

### Candidate evaluation / backtest execution

Generated candidates are not product results until they enter a real evaluator and produce truthful persisted evidence. This is a mandatory seam between candidate generation and ranking.

Required product behavior:

- [ ] resolve the exact candidate and strategy semantics from canonical custody before execution;
- [ ] select an evaluator/producer using explicit supported strategy semantic schema, engine/build identity, and requested product mode—not a generic fake evaluator;
- [ ] bind the actual data and execution context consumed by that evaluator; native contexts remain producer-derived where required, while TraderCockpit evaluators may use explicit Class C contexts;
- [ ] run through the canonical run authority (`engine/run_service.py` or its accepted successor), not a second execution pipeline;
- [ ] persist exact run identity, receipt, result, lifecycle state, and producer/build provenance;
- [ ] distinguish prelaunch refusal, producer/evaluation failure, persistence/custody failure, execution `completed`, and later validation outcomes;
- [ ] never synthesize P&L, trades, native metrics, or completed execution merely to make generated candidates rankable;
- [ ] provide an API/read model that can reopen the exact execution and surface truthful result applicability;
- [ ] connect the product UI from generated candidate → executable action/progress → durable result/readback;
- [ ] joined acceptance must prove at least one generated candidate becomes a real persisted result and that ranking consumes only the objective/evidence actually produced for that candidate.

A candidate-generation PR is not product-complete if its “evaluation” is only a local construction heuristic. A construction/search objective such as `construction_fit` may guide discovery, but it must be labeled as such and cannot impersonate backtest/validation evidence.

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
- [ ] bind each fitness/score to the exact objective/evidence that produced it and reject mixed/relabelled objective custody;
- [ ] support filtering and databank/candidate-capacity behavior;
- [ ] define deterministic tie behavior;
- [ ] integrate ranking into candidate flow after the relevant evaluation/discovery evidence exists;
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

### Validation and promotion

Validation/promotion is its own governed vertical. It consumes exact run/result/robustness evidence; it is never inferred from ranking position, construction fitness, or execution completion.

Required product behavior:

- [ ] select or create an explicit validation plan whose source-result schema and gates are compatible with the exact evidence being judged;
- [ ] apply the canonical validation authority (`product/tradercockpit/domain/validation.py` and the accepted run/evidence service path) rather than creating a competing verdict engine;
- [ ] persist the validation plan, each gate outcome, validation decision, evidence manifest, and their exact run/result references;
- [ ] fail closed when required metrics/evidence are absent, incompatible, non-numeric, tampered, or from another run;
- [ ] expose a user action/read model that shows why a strategy passed or failed and which exact evidence was used;
- [ ] keep robustness outcomes visible as evidence without silently turning them into a pass/fail policy unless the selected validation plan explicitly consumes them;
- [ ] promotion/champion state must require an explicit persisted decision/action after validation; no high rank or completed run may auto-promote;
- [ ] preserve refusal/failure history and never overwrite a prior result to manufacture a passing decision;
- [ ] joined acceptance must prove `exact result/evidence → validation plan → persisted decision/evidence manifest → UI readback`, plus an explicit promotion action if promotion is claimed by the slice.

### Workflow orchestration / Custom Projects

SQX Custom Projects are useful reference task graphs, but TraderCockpit requires its own coherent orchestration model.

Required product behavior:

- [ ] represent ordered tasks, dependencies, databank/result custody, branching/looping, and termination;
- [ ] import/translate an SQX `.cfx` project when available and sufficiently understood;
- [ ] reconstruct task behavior from visible/config evidence when possible;
- [ ] define TraderCockpit-owned orchestration semantics when no authoritative SQX project is available;
- [ ] dispatch action tasks into registered canonical product capabilities rather than implementing duplicate Builder/Retester/robustness engines inside the workflow layer;
- [ ] persist resumable workflow/task state and exact output references;
- [ ] expose workflow start/list/read/reopen through the canonical product server and Operate UI;
- [ ] never leave workflow automation absent solely because a particular `.cfx` file was not recovered;
- [ ] do not revive the quarantined Futures intake pipeline.

### Retester / results / proof

Reference result areas include Overview, List of trades, Equity chart, Trade analysis, Portfolio correlation, Strategy config, Source Code, and additional analyses.

Merged foundation:

- [x] native Retester task-1 evaluator exists;
- [x] native Builder output can enter TraderCockpit candidate custody;
- [x] native trade CSV evidence exists as reference evidence.

Product-completion requirements:

- [ ] merge one connected imported-candidate → durable rediscovery → producer-derived Retester context → shared run authority → reverified durable result → exact readback/results UI vertical into the canonical product line (current implementation is PR #23; check live state);
- [ ] type truthful native producer metrics where their meaning is known;
- [ ] define clearly identified TraderCockpit-derived metrics where useful and mathematically defined;
- [ ] expose trades, equity, analysis, configuration, and proof through the simplified results surface;
- [ ] keep execution `completed`, robustness/validation decisions, and champion/promotion state separate;
- [ ] connect results back to the strategy/candidate/run identities that produced them.

## Accepted UI workspace completion map

The accepted shell is a product contract, not permission to leave routes permanently in placeholder state. Current route truth is defined by `web/model.mjs` on `main`; if the route model changes, update this map intentionally rather than silently abandoning a workspace.

### Strategies

- **Overview:** resolve exact strategy/candidate identity, provenance, linked runs/results, and actionable next steps.
- **Build:** configuration → construction/generation → candidate custody through the canonical Builder vertical.
- **Signals & Models:** real supported indicator/model catalog bindings, strategy attachments, signal history/confluence, and market context. If live signal production is not yet supported, implement truthful research/backtest signal readback rather than fabricated live state.
- **Candidates:** actual Builder/evolution actions, durable candidate list, ranking/objective/lineage, and handoff into real evaluation/testing.
- **Evidence:** exact run/result/config/provenance/validation evidence chain and export/read actions where supported.

### Explore

- **Catalog:** real supported indicators/models/building blocks and their requirements; no fabricated catalog entries.
- **Market Workspace:** real market investigation context and chart/derived observations only from available market data producers.
- **Market Data:** source/coverage/timeframe/session capability and data requirements; product must either connect a real producer or explicitly change/remove the accepted route.

### Test & Validate

- **Run Setup:** canonical execution configuration/action for supported strategy/candidate types.
- **Results:** durable result/trade/equity/metrics/provenance readback.
- **Stress & Robustness:** canonical robustness execution and persisted results.
- **Compare:** compatible persisted-result selection/comparison with explicit metric applicability.
- **Prop Simulation:** explicit rule-set/account simulation using real configured rules/evidence. It is optional and separate from Golden/validation; do not fabricate prop-firm rules.

### Operate

- **Runs:** current/recent canonical run/workflow lifecycle state and explicit control actions supported by the runtime.
- **Performance:** truthful scope-labelled run/strategy/account performance; research metrics must not be presented as live account metrics.
- **Execution & Risk:** broker/order/position/exposure/deployment state only when a real operational producer exists; otherwise implement the supported non-live risk capability or intentionally migrate/remove the route.
- **Automation:** saved canonical workflow definitions/runs, task progress, resumable state, and outputs through the workflow orchestration vertical.

### Apollo

Apollo remains one persistent assistant surface. It may explain context, prepare configuration, and present explicit actions, but cannot silently mutate semantics, launch compute, promote/certify, delete evidence, or fabricate external truth.

For every accepted route above, the implementation plan must name either (a) its concrete backend/API/UI completion vertical or (b) an explicit approved migration/removal. “Producer integration pending” is a temporary truthful state, not a permanent completion strategy.

## UI mapping guardrail

The frontend should guide the user through intent and actions rather than reproduce every SQX panel.

Use progressive disclosure:

- strategy intent → construction/configuration;
- candidate search → generation/evolution, evaluation, and ranking;
- testing → backtest/robustness/validation;
- workflow automation → canonical TraderCockpit task orchestration;
- results/proof → run/result/trade/evidence/promotion decisions;
- Explore → catalog plus real market/data investigation capabilities;
- Operate → real run/workflow/performance/risk scope without implying live trading;
- Apollo → explanation and explicit configuration/action assistance.

The UI must consume real backend state and actions. It may not use hard-coded workflow fiction to hide missing backend functionality.

## Acceptance gate for every vertical slice

Before declaring a slice complete, require all applicable:

- [ ] user-visible outcome and end-to-end path are named;
- [ ] each material behavior is identified as Class A, B, C, or D where the distinction matters;
- [ ] relevant SQX/reference evidence was inspected from the stable branch/path locators when available;
- [ ] canonical input/output/configuration identities are preserved where applicable;
- [ ] malformed input fails safely;
- [ ] Class D facts are never fabricated;
- [ ] reconstructed or TraderCockpit-owned behavior has deterministic tests;
- [ ] runtime execution is tested where execution is claimed;
- [ ] persistence/custody is tested where state crosses process/user actions;
- [ ] UI/API uses canonical backend behavior rather than constants or placeholders;
- [ ] no duplicate generic pipeline or Futures-derived fallback was introduced;
- [ ] every accepted UI route affected by the slice either has a real backing path or an explicit approved migration/removal;
- [ ] focused and full product/browser acceptance remain green on the exact current head;
- [ ] reviewed/unmerged work is not misrepresented as merged product state.

## Current implementation direction

Stop planning as a sequence of microscopic “source-proven parity” primitives. Continue by closing the highest-value non-overlapping **vertical product gap** on the canonical path.

Recovery Vertical 1 is executable-complete on PR #23's corrected line and is undergoing exact-head Codex closure; do not create a competing native-run authority. PR #2 is the adjacent preset-control hardening layer. Recovery Vertical 2 is actively owned by PR #25, robustness by PR #27, and workflow orchestration by PR #28; do not duplicate those lanes.

The central generated-candidate seam is explicitly `construction/generation → real evaluation/backtest (or clearly labelled discovery objective where appropriate) → ranking/filtering → durable candidate/result readback`. A Builder lane must not stop at locally computed fitness if the claimed user path requires backtest metrics.

Validation/promotion is explicitly `exact persisted result/evidence → selected validation plan → gate evaluation → persisted decision/evidence manifest → user-visible rationale → explicit promotion action when supported`. Do not infer it from ranking or execution completion.

For Signals & Models, Explore, Compare, Prop Simulation, Operate Performance/Execution Risk, and other accepted routes not owned by the current recovery PRs, select future non-overlapping verticals from the accepted UI workspace completion map instead of leaving them indefinitely pending.
