# TraderCockpit Product Architecture v1

## Decision

`main` is the canonical TraderCockpit product line. Production implementation remains TraderCockpit-owned under `product/**` and `web/**`.

TraderCockpit is a complete strategy research and trading-system development product. StrategyQuant X 144.2953 is a major behavioral reference and compatibility source, not the owner of TraderCockpit's architecture and not a prerequisite for filling normal product behavior.

There is no Phase 0 intake or Phase 1 intake/product stage in this architecture. `javin23863/futures` is quarantined unless the user explicitly reverses that decision.

Reviewed but unmerged branches may prove implementation direction and executable behavior, but they do not change canonical product state until merged into `main`.

## Product authority model

```text
TRADERCOCKPIT PRODUCT GOALS + DOMAIN MODEL
construction → generation → evaluation → ranking → testing/robustness
→ workflow orchestration → results/proof → validation/promotion
                         |
                         | defines the coherent product path
                         v
TRADERCOCKPIT DOMAIN + RUNTIME + PERSISTENCE
                         ^
                         |
SQX REFERENCE EVIDENCE ---+--- native/recovered behavior, presets,
                              projects, screenshots, outputs, runs
                         |
                         v
TRADERCOCKPIT UI
simplified intent-driven operating surface + Apollo
```

The product path is authoritative. SQX evidence informs how individual capabilities should behave and provides compatibility targets, defaults, algorithms, terminology, workflow examples, and producer truth. It does not determine whether TraderCockpit is allowed to implement behavior that the product requires.

## Behavior authority classes

Implementation follows the four classes defined in `AGENTS.md`:

- **Class A — observed/recovered:** reproduce useful known SQX behavior.
- **Class B — reconstructed:** infer deterministic behavior consistent with available observations.
- **Class C — TraderCockpit-owned:** design missing behavior required by the product where SQX does not define it or evidence is unavailable.
- **Class D — producer/external truth:** never fabricate market observations, native producer output, custody, certification, or external side effects.

This distinction allows the product to keep moving without misrepresenting invented behavior as SQX fact.

## Canonical product flow

TraderCockpit should converge on one connected flow:

```text
Strategy intent/configuration
        ↓
Candidate construction / evolutionary generation
        ↓
Evaluation / backtest execution
        ↓
Ranking + filtering
        ↓
Robustness / validation workflows
        ↓
Results, proof, comparison, promotion decisions
        ↓
Saved strategy/run/result history
```

Custom/project workflows orchestrate the same canonical capabilities rather than forming a separate intake pipeline.

The frontend should expose this flow through the accepted compact TraderCockpit workspaces, not through a copy of SQX's settings hierarchy.

## Strategy construction / Builder

SQX Builder remains a strong reference. Its observed settings surfaces are:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks → Ranking → Notes`

These surfaces identify useful capabilities. TraderCockpit may simplify them, combine them, supply its own deterministic defaults, and define missing semantics so long as the runtime consumes the resulting configuration coherently.

Construction must ultimately produce canonical TraderCockpit strategy/candidate configuration that can enter the real execution path.

## Candidate generation and evolution

TraderCockpit requires a complete candidate-generation loop. SQX native/recovered behavior should be used where it is understood, including known crossover, mutation, fresh-blood, and multi-island observations. Missing hidden mechanics do not justify leaving the loop incomplete.

Where behavior is unresolved, TraderCockpit should define deterministic Class B/C semantics for selection, mutation/crossover details, replacement, migration, restart/stagnation, tie handling, termination, and reproducibility as needed by the product. Those choices must be tested and must not be falsely described as recovered SQX internals.

Generated candidates must have canonical identity/lineage and feed the real evaluation/ranking path.

## Candidate evaluation / backtest execution

Evaluation is an explicit product stage, not an implied helper inside generation or ranking.

For any candidate whose ranking or user decision depends on backtest/producer evidence, TraderCockpit must resolve the exact candidate and strategy semantics, select an evaluator that explicitly supports that semantic schema/build, bind the actual data/execution context consumed by that evaluator, and execute through the canonical run authority. Execution must produce durable run/receipt/result/lifecycle custody and a reopenable read model.

Native SQX candidates may use producer-derived native Retester contexts. TraderCockpit-owned strategies may use explicit Class C evaluator/data/execution semantics. In either case, the product may not synthesize trades, P&L, native metrics, or execution completion simply so a candidate can be ranked.

Construction-only objectives such as a deterministic structural/search fitness are valid discovery evidence when clearly labelled, but they are not substitutes for backtest or validation evidence. The joined Builder path therefore needs to make the distinction explicit:

```text
candidate construction/generation
        ↓
structural/search objective (optional, clearly scoped)
        ↓
real evaluator/backtest when the product decision requires execution evidence
        ↓
durable result/evidence
        ↓
objective-bound ranking/filtering
```

Prelaunch refusal, producer failure, custody/persistence failure, execution `completed`, validation pass/fail, and promotion are separate states.

## Data, trading context, building blocks, and sizing

The runtime model must include the configuration actually needed for evaluation and candidate generation: market/timeframe/date context, cost assumptions, sessions, trading direction/style, building blocks, order/exit rules, stop/target behavior, and sizing/risk configuration.

SQX configuration is useful reference authority. TraderCockpit may supply its own validation/defaults/algorithms when evidence ends, provided external/producer truth is not fabricated and the rules are internally consistent.

## Ranking and filtering

Ranking is a discovery/search mechanism, not validation or champion certification.

TraderCockpit must define usable objectives, filtering, capacity/termination, deterministic ordering/tie behavior, and the connection between evaluated candidate metrics and ranked candidate sets. Every score must remain bound to the objective/evidence that produced it; mixed or relabelled objective custody must fail closed. SQX objectives and filters should be reproduced where useful; the absence of a complete SQX objective catalog does not block a functioning ranking subsystem.

## Robustness

SQX cross-check methods are references for the breadth of capability: What If, Monte Carlo variants, higher precision, additional markets, sequential optimization, parameter permutation, WFO, and WFM.

TraderCockpit should implement useful robustness methods progressively through one canonical test/result model. Native SQX execution may be used when required or valuable. TraderCockpit-owned deterministic implementations are also valid for Class C behavior.

Robustness outputs are evidence. They do not become a validation decision merely because the test completed or produced favorable metrics.

## Validation and promotion

Validation/promotion is a governed vertical distinct from ranking, robustness execution, and ordinary run completion.

TraderCockpit already has low-level validation/evidence primitives; product completion requires connecting them end to end rather than leaving them as domain ingredients. A validation action must bind an explicit plan to an exact compatible result/evidence set, evaluate gates against those exact facts, persist plan/outcomes/decision/evidence manifest, and expose the rationale through the product read model/UI.

A completed run or high ranking score cannot silently become validated/champion state. Promotion requires a separate explicit persisted action/decision after whatever validation policy the product requires. Missing/incompatible/tampered evidence fails closed.

Canonical shape:

```text
exact result + robustness/other evidence
        ↓
selected validation plan
        ↓
gate evaluation
        ↓
persisted decision + evidence manifest
        ↓
user-visible rationale
        ↓
explicit promotion action (only when supported)
```

## Workflow orchestration / Custom Projects

SQX Custom Projects demonstrate ordered task-graph workflows such as Build, Retest/OOS, timeframe/slippage/parameter retests, Clear databanks, and Go To Task.

TraderCockpit needs a general canonical task-orchestration model whether or not the exact original `.cfx` is available. The model should support ordered tasks, dependencies, task inputs/outputs, result/databank custody, branching/looping, failure/termination, and resumable status where useful.

When a saved SQX project exists, translate/reproduce it using Class A/B rules. When it does not, Class C TraderCockpit workflows may still be designed and implemented. Workflow action tasks must dispatch into registered canonical Builder/Retester/robustness/etc. capabilities rather than reimplementing those engines inside the workflow layer. Do not replace this with the quarantined Futures intake architecture.

## Retester, results, and proof

Native SQX Retester output is one producer source, not the entirety of the results architecture.

TraderCockpit results should connect canonical strategy/candidate/run identities to:

- execution status;
- trades;
- equity/performance series;
- truthful producer-native metrics;
- clearly identified TraderCockpit-derived metrics;
- robustness/validation evidence;
- configuration/provenance;
- comparisons and promotion decisions.

Native producer facts must remain exact where represented as native facts. Content-addressed native custody that is presented as durable must be re-verifiable on read; a missing or altered blob is invalid state, not a successful historical claim. TraderCockpit-derived analysis is allowed when its formula and provenance are explicit.

## Persistence and custody

Content-addressed identity and custody are foundational, but custody is not the product itself. Persistence should support the real user flow: save configurations, strategies, candidates, runs, task/workflow state, results, evidence, validation decisions, promotion state, and history needed by the UI.

Durable candidate/result identity must also be rediscoverable through product read models where the user is expected to resume work after reload or after a native producer moves its source artifact.

Do not create identity-only slices that never connect to an operational capability.

## Accepted TraderCockpit UI workspace contract

The accepted compact shell remains a product contract unless an explicit product decision migrates/removes a route. Current route truth must be checked in `web/model.mjs` on `main` before implementation.

### Strategies

- **Overview:** exact strategy/candidate identity, provenance, linked activity, and real next actions.
- **Build:** intent/configuration into the canonical construction/generation path.
- **Signals & Models:** supported catalog bindings, attached models/indicators, truthful signal/history/confluence and market context. If only historical/research signals are supported, say so; never fabricate live signals.
- **Candidates:** real Builder/evolution actions, durable candidate/ranking/lineage custody, and evaluation/testing handoff.
- **Evidence:** exact strategy/run/result/config/validation provenance and evidence chain.

### Explore

- **Catalog:** real supported indicators/models/building blocks and requirements.
- **Market Workspace:** market investigation/chart/observations backed by available market data.
- **Market Data:** source, coverage, timeframe/session capability and requirements.

If no real data producer supports an accepted Explore route, the product must either implement a truthful supported data capability or explicitly approve a route migration/removal. Permanent placeholder state is not completion.

### Test & Validate

- **Run Setup:** canonical supported execution action/configuration.
- **Results:** durable run/result/trade/equity/metric/provenance readback.
- **Stress & Robustness:** canonical robustness execution/results.
- **Compare:** comparison of compatible persisted results with explicit metric applicability.
- **Prop Simulation:** optional explicit configured rule-set/account simulation using real rules/evidence; never invent prop-firm rules and never make it an automatic validation funnel stage.

### Operate

- **Runs:** canonical run/workflow lifecycle and supported explicit controls.
- **Performance:** scope-labelled run/strategy/account performance; research result metrics cannot be silently presented as live account performance.
- **Execution & Risk:** broker/order/position/exposure/deployment truth only from real operational producers; otherwise provide only the supported non-live risk capability or explicitly migrate/remove the route.
- **Automation:** saved workflow definitions/runs, task progress, resumable state, and exact outputs through canonical workflow orchestration.

### Apollo

Apollo is one persistent assistant surface. It may explain, configure, and prepare explicit actions. It cannot silently launch compute, mutate semantics, promote/certify, export/delete evidence, or fabricate external truth.

For every accepted route, future implementation planning must identify either its concrete backend/API/UI completion vertical or an explicit approved migration/removal. “Producer integration pending” is a temporary truthful state, not a permanent product strategy.

## Vertical implementation rule

Implementation should proceed by the highest-value non-overlapping vertical product gap, not by the smallest available SQX evidence fragment.

A vertical slice should normally include enough of configuration, domain behavior, runtime/orchestration, persistence/results, and UI/API integration to produce a usable outcome. Adjacent frontend/backend work may be split for concurrency, but the plan must show how the pieces join immediately.

Examples of useful product slices:

- configure construction → generate candidates → evaluate through the required real evaluator → rank → display/reopen candidate/result set;
- choose candidate → run robustness suite → persist evidence → apply an explicit validation plan → show decision/rationale;
- define workflow → execute ordered tasks through canonical capability handlers → persist task/run state → inspect/reopen results;
- open completed run → inspect truthful trades/metrics/equity → compare → explicitly validate/promote when requested.

Examples of insufficient slices unless immediately consumed by adjacent work:

- a standalone dataclass that no runtime uses;
- a parser with no product action that invokes it;
- a UI control backed only by constants;
- a locally computed “fitness” presented as if it were a backtest result;
- a validation primitive with no plan/action/readback path;
- a refusal boundary created solely because exact SQX internals are missing.

## Product rules that must not regress

- TraderCockpit owns the architecture and must become a complete functioning product.
- SQX evidence should be used heavily but is not the completeness gate.
- Class B/C behavior is expected where evidence is incomplete.
- Class D producer/external facts must remain truthful.
- One canonical domain/runtime pipeline; no duplicate generic pipeline or Futures-derived fallback.
- Evaluation is an explicit seam between generation and evidence-based ranking.
- Ranking is distinct from validation/promotion.
- Validation/promotion must consume explicit exact evidence and persist its decision.
- Every accepted UI route must have a completion vertical or an explicit approved migration/removal.
- UI must use real backend state/actions.
- Production must not import reference/recovered trees at runtime.

## Stable reference anchors

Resolve the current head of these branches before using them. The branch/path names are stable discovery locators; historical SHAs are not policy authority.

- **UI authority / retained screenshots:** `codex/ui-reference-acceptance` → `references/ui-authority/`.
- **Recovered SQX 144.2953 source/archive:** `codex/sqx-reference-archive-20260830` → `docs/module-map.md`, `docs/extraction-report.md`, `sources/engine-core/`, `sources/plugins/`, and `references/`.
- **Native SQX 144.2953 runtime evidence:** `codex/sqx-runtime-evidence-144-2953` → `references/strategyquant-x-144.2953/` plus adjacent `references/`/`docs/`.
- **Capability/parity probes:** `codex/sqx-capability-parity`; inspect its live contents before relying on a capability claim.
- **Accepted compact UI checkpoints:** `checkpoint/ui-shell-accepted-2026-08-30` and `checkpoint/ui-cockpit-home-accepted-2026-08-30`; production truth remains the current `main` files.

These refs are evidence sources, not production bases. Production code must not import them at runtime, and missing exact hidden SQX behavior never prevents a justified Class B/C product implementation.