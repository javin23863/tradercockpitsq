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

## Data, trading context, building blocks, and sizing

The runtime model must include the configuration actually needed for evaluation and candidate generation: market/timeframe/date context, cost assumptions, sessions, trading direction/style, building blocks, order/exit rules, stop/target behavior, and sizing/risk configuration.

SQX configuration is useful reference authority. TraderCockpit may supply its own validation/defaults/algorithms when evidence ends, provided external/producer truth is not fabricated and the rules are internally consistent.

## Ranking and filtering

Ranking is a discovery/search mechanism, not validation or champion certification.

TraderCockpit must define usable objectives, filtering, capacity/termination, deterministic ordering/tie behavior, and the connection between evaluated candidate metrics and ranked candidate sets. SQX objectives and filters should be reproduced where useful; the absence of a complete SQX objective catalog does not block a functioning ranking subsystem.

## Robustness and validation

SQX cross-check methods are references for the breadth of capability: What If, Monte Carlo variants, higher precision, additional markets, sequential optimization, parameter permutation, WFO, and WFM.

TraderCockpit should implement useful robustness methods progressively through one canonical test/result model. Native SQX execution may be used when required or valuable. TraderCockpit-owned deterministic implementations are also valid for Class C behavior.

Validation/promotion remains a separate authority from ranking. A completed run or high ranking score cannot silently become a validated/champion state.

## Workflow orchestration / Custom Projects

SQX Custom Projects demonstrate ordered task-graph workflows such as Build, Retest/OOS, timeframe/slippage/parameter retests, Clear databanks, and Go To Task.

TraderCockpit needs a general canonical task-orchestration model whether or not the exact original `.cfx` is available. The model should support ordered tasks, dependencies, task inputs/outputs, result/databank custody, branching/looping, failure/termination, and resumable status where useful.

When a saved SQX project exists, translate/reproduce it using Class A/B rules. When it does not, Class C TraderCockpit workflows may still be designed and implemented. Do not replace this with the quarantined Futures intake architecture.

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

Native producer facts must remain exact where represented as native facts. TraderCockpit-derived analysis is allowed when its formula and provenance are explicit.

## Persistence and custody

Content-addressed identity and custody are foundational, but custody is not the product itself. Persistence should support the real user flow: save configurations, strategies, candidates, runs, task/workflow state, results, evidence, validation decisions, and history needed by the UI.

Do not create identity-only slices that never connect to an operational capability.

## TraderCockpit UI mapping

The UI is intent-driven and progressively disclosed:

- **Construct / Candidates:** define strategy intent, configure generation, create/search candidates;
- **Test & Validate:** run backtests, robustness methods, comparison, and validation actions;
- **Results / Proof:** inspect trades, equity, metrics, evidence, configurations, and provenance;
- **Operate / automation:** run saved task workflows against canonical product capabilities;
- **Apollo:** explain, configure, and prepare explicit actions without silently changing semantics or fabricating execution.

The UI must consume real backend state. It cannot compensate for missing runtime behavior with hard-coded workflow fiction.

## Vertical implementation rule

Implementation should proceed by the highest-value non-overlapping vertical product gap, not by the smallest available SQX evidence fragment.

A vertical slice should normally include enough of configuration, domain behavior, runtime/orchestration, persistence/results, and UI/API integration to produce a usable outcome. Adjacent frontend/backend work may be split for concurrency, but the plan must show how the pieces join immediately.

Examples of useful product slices:

- configure construction → generate candidates → evaluate → rank → display candidate set;
- choose candidate → run robustness suite → persist evidence → show validation result;
- define workflow → execute ordered tasks → persist task/run state → inspect results;
- open completed run → inspect truthful trades/metrics/equity → compare or promote.

Examples of insufficient slices unless immediately consumed by adjacent work:

- a standalone dataclass that no runtime uses;
- a parser with no product action that invokes it;
- a UI control backed only by constants;
- a refusal boundary created solely because exact SQX internals are missing.

## Product rules that must not regress

- TraderCockpit owns the architecture and must become a complete functioning product.
- SQX evidence should be used heavily but is not the completeness gate.
- Class B/C behavior is expected where evidence is incomplete.
- Class D producer/external facts must remain truthful.
- One canonical domain/runtime pipeline; no duplicate generic pipeline or Futures-derived fallback.
- Ranking is distinct from validation/promotion.
- UI must use real backend state/actions.
- Production must not import reference/recovered trees at runtime.

## Reference anchors

Reference branches and artifacts remain useful evidence sources. Inspect current heads before relying on them. They are not production bases and do not prevent TraderCockpit-owned design where evidence is incomplete.
