# TraderCockpit SQX Parity Implementation Map

This is the binding implementation and acceptance map for `tradercockpitsq`. Read it with `AGENTS.md` and `docs/product-architecture-v1.md`.

## Scope rule

TraderCockpit is reproducing the observed StrategyQuant X 144.2953 backend capabilities/workflows through TraderCockpit-owned code and a simpler TraderCockpit UI.

There is no Phase 0 intake and no Phase 1 intake in this product. Do not use the old Futures-repository workflow, `phase01_intake`, or equivalent phase terminology to plan or gate implementation.

`javin23863/futures` remains quarantined unless the user explicitly reverses that decision.

## Required authority before implementation

For an SQX-backed feature, inspect all applicable authority before editing:

- the relevant screenshot(s) from the 35-shot SQX panel set;
- the exact saved `.cfx` project/configuration and task XML where available;
- the exact preset/overlay/runtime evidence where available;
- native SQX execution evidence where behavior must be proved;
- the accepted TraderCockpit prototype/UI mapping for how the capability should be exposed.

Screenshots define visible capability and workflow structure. Project/configuration/runtime evidence defines what actually executes. The TraderCockpit prototype defines presentation. None of these licenses fabricated semantics.

## Product rules that must not regress

- TraderCockpit must remain simpler and lower-friction than SQX at the UI layer.
- Do not clone the SQX settings UI one-for-one.
- Do not invent an unrelated workflow in front of SQX.
- Saved projects/presets are wired only when exact evidence proves the mapping.
- Unsupported/unverified capability is unavailable or refused rather than approximated.
- Search/evolution fitness and ranking are discovery evidence, not validation/champion status by themselves.
- Native result statistics are not automatically trustworthy merely because fields exist; only producer outputs proved meaningful may be promoted into typed product metrics.
- Apollo guides through explicit user actions and cannot silently change strategy semantics, launch compute, certify/promote results, export, or delete evidence.
- Production code must not import recovered/reference trees or another repository at runtime.

## Verified product foundation

The following is already present on the canonical product line or merged product history:

- [x] TraderCockpit-owned production namespace under `product/tradercockpit`.
- [x] Deterministic canonical serialization and content-addressed custody.
- [x] Exact strategy/candidate/run/result/lifecycle/evidence identity handling.
- [x] Filesystem-backed persistence and verified read model.
- [x] Product server and browser acceptance harness.
- [x] Accepted TraderCockpit UI foundation/prototype direction with persistent Apollo.
- [x] Source-bound SQX preset/runtime control.
- [x] Native SQX output custody.
- [x] Native SQX 144.2953 Retester task-1 evaluator with exact `SQTradingLib.jar` build verification.
- [x] Isolated TraderCockpit-owned Retester execution.
- [x] Native GA variation evidence retained for Builder evolutionary-search implementation.

## SQX capability map

### Builder shell and configuration

Observed Builder shell:

`Progress | Full settings | Results`

Observed Full settings surfaces:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

Implementation status must be tracked by capability, not by invented product phases.

### What to build / strategy construction

Observed behavior to reproduce where supported:

- [ ] simple strategy configuration;
- [ ] multi-timeframe/multi-symbol configuration;
- [ ] strategy-from-template behavior;
- [ ] improve-existing-strategy behavior;
- [ ] trading direction/style/build-mode configuration;
- [ ] condition/period, stop-loss, and profit-target configuration;
- [ ] parts-to-improve semantics.

Do not expose an option until its exact SQX semantics can be represented and executed truthfully.

### Genetic evolution

Observed controls include:

- [ ] maximum generations;
- [ ] population size per island;
- [ ] crossover probability;
- [ ] mutation probability;
- [ ] island count/topology;
- [ ] migration interval/rate;
- [ ] initial-population databank behavior;
- [ ] generated decimation coefficient;
- [ ] initial-population filtering;
- [ ] fresh-blood duplicate replacement / weakest replacement behavior;
- [ ] restart-on-finish and stagnation restart behavior.

Native evidence already proves bounded execution for:

- [x] saved baseline configuration;
- [x] crossover-only variation;
- [x] mutation-only variation;
- [x] balanced crossover/mutation variation;
- [x] fresh-blood replacement variation;
- [x] four-island topology variation.

The retained native run generated 90 strategies across those bounded comparisons with 17 accepted native result archives and zero failed strategies. Restart combinations were deliberately disabled for deterministic short runs, so restart behavior remains separate/unproven evidence rather than something to infer.

### Data and trading context

Observed Builder Data/Trading surfaces include engine, symbol, timeframe, date range, precision, spread/slippage/commission context, IS/OOS ranges, session/trading options, and related execution settings.

- [ ] model these settings from the actual SQX configuration source used by the producer;
- [ ] do not ask the TraderCockpit UI for values that SQX will ignore;
- [ ] when execution uses a native `project.cfx`, bind exact project/configuration identity rather than fabricating expanded assumptions.

### Building blocks and order/exit behavior

Observed Builder controls include selected signals/indicators, weights, parameters, order types, exit types, and external indicators/timeframes.

- [ ] reproduce the supported block catalog/selection semantics;
- [ ] reproduce weights/parameter-generation semantics where evidenced;
- [ ] preserve supported order/exit type behavior;
- [ ] fail closed on unimplemented block semantics.

### Money management / ATM

- [ ] reproduce supported initial-capital and sizing-method semantics;
- [ ] reproduce supported ATM behavior only where exact SQX behavior is evidenced;
- [ ] do not substitute generic sizing logic for SQX behavior.

### Cross checks / robustness

Observed methods include:

- [ ] What If simulations;
- [ ] Monte Carlo trades manipulation;
- [ ] Higher backtest precision;
- [ ] Backtests on additional markets;
- [ ] Monte Carlo retest methods;
- [ ] Sequential Optimization;
- [ ] Optimization Profile / System Parameter Permutation;
- [ ] Walk-Forward Optimization;
- [ ] Walk-Forward Matrix.

Each method may have separate Settings and Filtering configuration. Preserve that distinction in the backend even if TraderCockpit presents it more simply.

### Ranking and filtering

Observed Ranking controls include databank capacity/stop-generation behavior, fitness objective, automatic/custom filters, and cross-check filters.

- [ ] implement exact supported ranking objectives;
- [ ] implement exact supported filtering semantics;
- [ ] keep search fitness/ranking distinct from strategy validation/promotion status.

### Custom Projects / workflow orchestration

SQX Custom Projects define ordered task graphs. The captured GOLD BREAKOUT M30 workflow visibly includes Build, Retest/OOS tasks, timeframe/slippage/parameter retests, Clear databanks, and Go To Task.

- [ ] parse/identify task order from the exact saved `.cfx` project;
- [ ] execute only the order/configuration actually proved by that project;
- [ ] preserve task/result databank relationships;
- [ ] support loops/go-to behavior only where runtime evidence proves semantics;
- [ ] never replace the task graph with a generic intake pipeline.

### Retester / results

Observed Retester Results include Overview, List of trades, Equity chart, Trade analysis, Portfolio correlation, Strategy config, Source Code, and additional analyses.

- [x] exact native Retester execution path exists for task 1;
- [x] result archive custody exists;
- [x] native trade CSV evidence exists;
- [ ] identify and type only result metrics whose native meaning is proved;
- [ ] expose trade/result analysis through the simplified TraderCockpit results/proof surface;
- [ ] keep `completed` execution separate from validation `passed` semantics.

## TraderCockpit UI mapping guardrail

The frontend should guide the user through intent and actions, not reproduce every SQX panel.

Use progressive disclosure:

- construction/strategy intent maps to Builder configuration;
- search/evolution maps to Genetic options and Builder progress;
- testing/robustness maps to cross-check/project tasks;
- workflow automation maps to Custom Projects;
- results/proof maps to Retester/native result custody;
- Apollo may explain and help configure these capabilities but backend authority remains deterministic.

The accepted prototype's compact construction/backtest/proof experience is the presentation direction. Detailed SQX settings belong behind the simplified controls when needed.

If Fast/Golden or other TraderCockpit product labels are retained, their actual stages must be generated from verified SQX-backed workflow/task plans, not frontend constants or an old repository pipeline.

## Acceptance gate for every capability slice

Before declaring a slice complete, require all applicable:

- [ ] relevant SQX screenshot inspected;
- [ ] relevant project/preset/configuration/runtime evidence inspected;
- [ ] exact behavior/control/task being reproduced named;
- [ ] exact input/output/configuration identities preserved;
- [ ] unsupported/malformed input fails closed;
- [ ] positive native/executable proof exists where execution is claimed;
- [ ] negative/refusal proof exists where applicable;
- [ ] stale/tampered/cross-run/cross-strategy substitution rejected where applicable;
- [ ] producer metrics are regression-covered before being shown as product truth;
- [ ] UI uses backend capability/configuration/results rather than hard-coded workflow fiction;
- [ ] no second generic pipeline or Futures-derived fallback was introduced;
- [ ] full product/browser acceptance remains green.

## Current active work

Continue the StrategyQuant X Builder/evolutionary-search implementation using the 35-shot SQX authority, saved project/configuration evidence, and the native GA variation run.

The GA evidence is input to the genetic-algorithm implementation—not a separate documentation or run-binding project. After the currently supported Builder/evolution behavior is implemented and accepted, continue through the adjacent SQX-backed configuration, project-task orchestration, robustness, and result/metric capabilities according to the observed SQX workflow and available executable evidence.
