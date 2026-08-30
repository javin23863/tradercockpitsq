# TraderCockpit SQX-Backed Product Architecture v1

## Decision

`main` is the canonical TraderCockpit product line. Production implementation remains TraderCockpit-owned under `product/**` and `web/**`.

The product objective is to reproduce the observed backend capabilities and workflows of StrategyQuant X 144.2953, then expose them through a simpler TraderCockpit interface. TraderCockpit is not cloning the SQX UI and is not reviving the old Futures-repository architecture.

There is no Phase 0 intake or Phase 1 intake/product stage in this architecture. Do not use those concepts to sequence work.

`javin23863/futures` is quarantined unless the user explicitly reverses that decision.

## Behavioral and interface authority

```text
OBSERVED SQX BEHAVIOR AUTHORITY
35-shot SQX UI set + saved .cfx projects + presets/configuration
+ native runtime evidence + exported native results/trades
                         |
                         | behavior / task / configuration mapping
                         v
TRADERCOCKPIT DOMAIN + RUNTIME
TraderCockpit-owned identities, configuration contracts,
workflow/task orchestration, persistence, producer adapters
                         |
                         | simplified presentation
                         v
TRADERCOCKPIT UI AUTHORITY
accepted prototype/workspaces + persistent Apollo + results/proof surfaces
```

SQX evidence is authoritative for what SQX actually does. It is not a runtime dependency. A screenshot alone does not prove execution, but a screenshot plus saved project/configuration/runtime evidence can define the behavior TraderCockpit must reproduce.

The TraderCockpit prototype is authoritative for presentation. The goal is lower friction and progressive disclosure, not a one-for-one copy of SQX's dense settings screens.

## Observed SQX workflow model

### Builder

The Builder has three primary states:

`Progress | Full settings | Results`

The observed Full settings surfaces are:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

These surfaces define configuration capabilities. They are not an assistant-created mandatory funnel.

Observed examples include:

- **What to build:** simple/multi-timeframe/template/improve-existing strategy choices plus build configuration.
- **Genetic options:** generations, population per island, crossover probability, mutation probability, island topology/migration, initial-population behavior, decimation, fresh-blood replacement, and restart/stagnation controls.
- **Data:** engine, symbol, timeframe, date range, precision, spread/slippage/commission context, and IS/OOS segmentation.
- **Building blocks:** signals/indicators, order types, exit types, weights, and parameter-generation choices.
- **Money management:** initial capital and sizing method.
- **Cross checks:** What If simulations, Monte Carlo trade manipulation, higher backtest precision, additional-market backtests, Monte Carlo retest methods, sequential optimization, parameter permutation, Walk-Forward Optimization, and Walk-Forward Matrix, each with method settings/filtering where shown.
- **Ranking:** datastore capacity/termination, fitness/ranking objective, and filtering conditions.

### Custom Projects

SQX Custom Projects are ordered task graphs. The captured GOLD BREAKOUT M30 project visibly sequences tasks such as Build, multiple Retest/OOS tasks, timeframe/slippage/parameter retests, Clear databanks, and Go To Task.

TraderCockpit must preserve the exact task order proved by the saved `.cfx` project/configuration. It must not insert a generic intake stage or rewrite the project as a different pipeline.

### Retester and results

Retester also exposes:

`Progress | Full settings | Results`

The captured Results surface includes Overview, List of trades, Equity chart, Trade analysis, Portfolio correlation, Strategy config, Source Code, and additional analyses. TraderCockpit should expose the useful result/proof information in its own simplified results UI while retaining exact producer/result identity.

### Presets and workflows

SQX already ships with presets/configurations and project workflows. TraderCockpit should use these as backend behavior/configuration evidence and create TraderCockpit-owned identities around the exact executable configuration.

A saved project may be wired to a product preset only where archived configuration or runtime evidence proves that exact relationship. Unproven mappings remain unavailable rather than inferred from names.

## TraderCockpit UI mapping

TraderCockpit does not need to expose every SQX control at once.

The accepted design direction uses a compact operating surface with guided construction, testing/backtesting, and proof/results, plus persistent Apollo. Detailed SQX capabilities should appear through progressive disclosure only when they are relevant.

The mapping principle is:

- user intent and strategy construction map to Builder configuration;
- search/evolution maps to SQX Builder genetic evolution;
- market/data assumptions map to the exact SQX project/preset configuration actually executed;
- robustness/testing maps to SQX cross-check/task workflows;
- workflow automation maps to SQX Custom Project task order;
- result/trade analysis maps to native SQX result artifacts and exported trades;
- TraderCockpit custody/evidence adds exact identities around those native operations without pretending to change what SQX executed.

If product labels such as Fast or Golden are used, they must compile to a verified SQX-backed workflow/task plan. Their stages cannot be hard-coded from an unrelated architecture.

## Current implemented product foundation

The current product line already contains:

- TraderCockpit-owned canonical/content-addressed custody objects and lifecycle/evidence persistence;
- source-bound SQX preset/runtime control and native output custody;
- a verified native SQX 144.2953 Retester task-1 evaluator using the exact `SQTradingLib.jar` build identity;
- isolated TraderCockpit-owned Retester execution so unrelated SQX databank strategies are not processed accidentally;
- a product UI shell and accepted prototype direction that can present SQX-backed capability without cloning SQX.

The native GA variation evidence at `codex/sqx-runtime-evidence-144-2953@48ffdee5e24fd9b222ccaf0ee46a6e3235ea6430` is implementation evidence for Builder evolutionary-search semantics. It is not a separate product workflow.

## Current implementation direction

Continue SQX parity from observed behavior rather than from generic architecture gates.

The active backend work is to implement/verify the Builder evolutionary-search behavior and adjacent Builder configuration using the real SQX controls and native evidence, then continue across the observed project/task and result surfaces as executable evidence permits.

The GA implementation must be constrained by the observed controls and native runs, including crossover-only, mutation-only, balanced crossover/mutation, fresh-blood replacement, and multi-island topology behavior. Restart combinations remain unproven where the evidence run deliberately disabled them.

Do not block this work behind an invented Phase 0/Phase 1 intake layer. Do not copy the old Futures backend. Do not create a second generic pipeline beside the SQX-backed one.

## Product rules that must not regress

- SQX behavior/configuration evidence is inspected before planning SQX-backed product behavior.
- TraderCockpit stays simpler than SQX at the UI layer; backend parity does not require UI duplication.
- Exact saved project/preset/runtime identities are preserved where execution depends on them.
- Unsupported or unverified semantics fail closed rather than being approximated.
- Evolution fitness/ranking is discovery evidence, not validation/champion status by itself.
- Numeric results shown as producer results must come from native/verified producer artifacts, not UI defaults.
- Apollo may guide and configure through explicit user actions but may not silently alter strategy semantics, launch compute, certify results, or fabricate capability.
- Production code must not import reference/recovered trees or the Futures repository at runtime.

## Reference anchors

Inspect current heads before relying on branch evidence:

- UI product authority: `codex/ui-prototype-authority@53645acfff750672805efd6b20623a0abf36dff1`.
- UI behavior acceptance: `codex/ui-reference-acceptance@26221dccee1541c1fc672f24b75a380cf4371c32`.
- SQX capability/parity evidence: `codex/sqx-capability-parity@6f6fb81b450844024e8585503845c4a3316472de`.
- SQX runtime smoke/evidence: `codex/sqx-runtime-smoke@766cdc6e8c2f42e6dee86fd59b38e2862ef235a6` and `codex/sqx-runtime-evidence-144-2953@48ffdee5e24fd9b222ccaf0ee46a6e3235ea6430`.
- Preserved SQX extraction/workflow history: `archive/sqx-engine-extract-2026-08-30@c1ae24d2e62acfb8ae8be1aea318a82225490c4b`.

Reference branches are evidence sources, not production bases. Deliberate implementation must remain TraderCockpit-owned while reproducing the behavior proven by that evidence.
