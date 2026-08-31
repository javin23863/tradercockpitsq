# TraderCockpit Native-SQX Product Architecture

## Product decision

`main` remains the canonical TraderCockpit product line.

**StrategyQuant X 144.2953 is the strategy-research producer/backend authority. TraderCockpit is the application, desktop, guidance, configuration, custody, and presentation layer around that backend.**

TraderCockpit must not implement a second Builder, genetic algorithm, backtester, robustness engine, optimizer, or Custom Project execution engine when the corresponding operation belongs to SQX.

The production boundary is:

```text
TraderCockpit desktop/UI
        |
        | plain-English intent, explicit user choices, saved product state
        v
TraderCockpit application/runtime
        |
        | exact native configuration + job control + custody/read models
        v
StrategyQuant X 144.2953 producer
Builder / Retester / Optimizer / Cross checks / Custom Projects
        |
        | native databanks, .sqx strategies, native results/trades
        v
TraderCockpit custody + Backtest/Proof UI
```

The currently proven executable adapter uses a verified local SQX 144.2953 runtime and `sqcli.exe`. That is the first production spine. The adapter boundary must remain stable if deployment later packages or relocates the SQX worker differently.

Recovered/source/reference trees remain evidence and build-time research material; production code must not import those trees as ad-hoc Python runtime dependencies. This rule does **not** mean SQX itself is merely reference material. The verified SQX producer is the backend being controlled.

The old `javin23863/futures` architecture remains quarantined unless explicitly reinstated. There is no generic Phase 0/Phase 1 intake model in this product.

## Detailed implementation contract

`docs/product-backbone-spec-v1.md` is the binding detailed implementation contract for this architecture. It fixes the backbone information architecture, per-tab responsibilities, backend/read-model ownership, native SQX gateway/configuration compiler, revision/staleness rules, add-on capability manifest, extension slots, migration map, and anti-drift acceptance tests.

The core research navigation is intentionally stable:

- `Construct | Backtest | Proof`;
- Construct: `Idea | Specification | Build | Candidates`;
- Backtest: `Overview | Trades | Robustness | Configuration`.

Explore, Automation, Operate and add-ons are auxiliary/capability-driven surfaces. They do not expand the core stage bar by default.

## Authorities used to define the product

Implementation must reconcile four authorities before code changes:

1. **Observed SQX product behavior** — the 35-shot UI set, including Builder construction, Genetic options, Data, Trading options, Building blocks, Money management, Ranking, Cross checks, Progress, Retester results, and Custom Projects.
2. **Executable SQX evidence** — saved `.cfx` projects/task XML, preset/configuration files, native runtime traces, native Builder outputs, native `.sqx` archives, Retester results, and bounded native GA runs.
3. **Official SQX workflow semantics** — Builder creates/improves strategies; Retester retests existing strategies; Optimizer optimizes existing strategies; Custom Projects automate ordered task flows and databank filtering.
4. **TraderCockpit presentation authority** — the accepted prototype direction: Ideas/source intake, Apollo-guided construction, Candidate Lab, Backtest, robustness drilldown, Proof, and the persistent high-level stage model `Construct → Backtest → Proof`.

Screenshots prove visible workflow and configuration surfaces; they do not by themselves prove hidden implementation. Native execution/result evidence proves producer behavior. TraderCockpit simplifies presentation without changing which backend owns the operation.

## Actual SQX strategy-development lifecycle

The product is not Retester-first and it is not GA-first.

### 1. Idea or trading concept

A user begins with an idea, source, indicator, existing strategy, template, paper, notes, or explicit rules.

TraderCockpit owns this human-facing intake. Apollo may extract what is already stated and identify what is still required. It must not invent missing trading meaning.

The questions asked here must come from the requirements of the selected native construction path. For Builder this includes the same categories SQX exposes: strategy type, direction/style, conditions/periods, stop/profit behavior, data, trading/session rules, building blocks, sizing, search mode, ranking/filtering, and optional cross-checks. The UI asks only unresolved questions and presents them in plain English.

### 2. Construct the native strategy-search space

SQX Builder Full settings establishes the search space:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

TraderCockpit maps the user's approved intent to an **exact native SQX configuration snapshot**. It does not translate the idea into a new TraderCockpit strategy language.

The mapping must preserve, where applicable:

- simple / multi-timeframe / template / improve-existing construction mode;
- direction, style and allowed structural complexity;
- conditions, indicator periods, stop loss and profit target behavior;
- symbol, timeframe, date ranges, IS/OOS, precision and trading costs;
- session/trading constraints;
- allowed indicators/signals/building blocks, weights and parameter ranges;
- order and exit types;
- money management / sizing;
- build/search mode including genetic evolution where chosen;
- ranking, basic filters and cross-check filters.

If a required native field cannot yet be mapped truthfully, the Construct surface exposes that gap and the run stays locked.

### 3. Native Builder generation and initial backtest

Builder is the core strategy-generation producer. Genetic evolution is one Builder build mode, not a separate TraderCockpit backend.

TraderCockpit starts the verified native Builder job, observes progress, and reads native databanks/output. SQX owns:

- initial strategy generation;
- genetic selection/crossover/mutation/island mechanics;
- strategy-tree/block semantics;
- initial backtest/evaluation;
- native fitness/ranking/filter decisions;
- native Builder databank output.

TraderCockpit owns job identity, exact configuration custody, process/error handling, progress projection, and durable import of the exact native output.

A surviving strategy enters TraderCockpit as the exact native `.sqx` artifact plus canonical product identity/custody. It must not be converted into a substitute strategy schema that the native backend did not execute.

### 4. Candidate Lab

Candidate Lab is the user-facing view of real native Builder survivors.

It may show only producer-backed fields and TraderCockpit-owned custody/provenance. It must preserve the exact relationship:

`idea/spec → native Builder configuration → native Builder job → native .sqx strategy → candidate identity`.

Candidate Lab is not a second generator.

### 5. Backtest and validation funnel

SQX Cross checks are part of the strategy-testing funnel. They run after the strategy is generated and passes initial filters, and can dismiss a strategy so more expensive checks are never run.

TraderCockpit should expose this as a progressive funnel rather than a wall of SQX settings:

- **Fast/basic:** inexpensive checks such as What If, Monte Carlo trade manipulation, higher backtest precision where configured.
- **Standard:** additional markets, Monte Carlo retest methods, sequential optimization and other repeated-backtest checks.
- **Extensive:** optimization profile/system-parameter permutation, Walk-Forward Optimization, Walk-Forward Matrix, and other expensive checks.

The exact enabled methods, order, settings and filters come from a native SQX-backed validation profile or Custom Project task plan. Product labels such as `Fast` and `Golden` are allowed only as saved TraderCockpit profiles that compile to an inspectable native plan. They are not hard-coded phase counts.

### 6. Retester and Optimizer

Retester is downstream of strategy creation. It retests **existing** strategies with the same or deliberately different data/settings/cross-checks. Optimizer operates on existing strategies for parameter optimization and Walk-Forward work.

TraderCockpit uses these native modules when the requested operation requires them. Retester must never be presented as the beginning of the strategy lifecycle.

### 7. Custom Projects / automation

Custom Projects automate the native strategy-development workflow.

Saved evidence shows native projects containing ordered combinations such as:

`Build → Retest → Retest → ... → ClearDatabanks → GoToTask`

and other projects with `Optimize` or portfolio tasks.

TraderCockpit must drive/observe the native Custom Project and its native databank/task state. It must not translate a `.cfx` project into a separate TraderCockpit branch/loop engine and then claim that engine is SQX execution.

The Automation UI may simplify task presentation, but source/target databanks, task order, conditions, loops and producer outcomes remain native authority.

### 8. Results and Proof

Backtest shows useful strategy performance/trade analysis from native producer artifacts. Proof binds the complete chain of identities and evidence used to make a decision.

Proof must be able to answer:

- what idea/spec was approved;
- what native Builder configuration actually ran;
- which SQX build/worker executed it;
- which market/data/settings were used;
- which native strategy archive survived;
- what initial/native result was produced;
- which cross-check/retest/optimization plan ran;
- which checks passed or failed and why;
- what exact artifact is being retained/exported/promoted.

TraderCockpit may add custody/evidence receipts. It may not manufacture producer results or replace a failed native gate with its own verdict.

## TraderCockpit user experience

The persistent top-level workflow is:

```text
Ideas / sources
      ↓
CONSTRUCT
  guided intent → exact native SQX configuration → Candidate Lab
      ↓
BACKTEST
  native initial results → progressive cross-check/retest/optimization funnel
      ↓
PROOF
  exact native artifacts + configuration + results + validation evidence
```

Custom Project automation and later Operate/export surfaces sit around this lifecycle; they do not replace it.

Apollo is a guide, not a producer. It may:

- parse user sources and identify stated rules;
- find unresolved fields required by the native SQX construction path;
- explain choices and evidence;
- prepare a deterministic configuration diff for user approval;
- explain candidate results and failures;
- navigate to the next valid action.

Apollo may not silently choose ambiguous strategy meaning, change native configuration after approval, start compute without an explicit action, waive a failed gate, promote a candidate, or claim proof that the backend did not produce.

## Runtime/application ownership

### TraderCockpit owns

- desktop shell and worker supervision;
- runtime discovery/health and exact SQX build verification;
- secure local application API;
- idea/source records and approved intent;
- deterministic mapping from approved intent to exact native SQX configuration;
- configuration snapshots/diffs and user approval;
- job lifecycle/control around native modules;
- content-addressed custody of native strategy/result artifacts;
- product identities and provenance links;
- progress/read models and frontend state;
- Candidate Lab, Backtest, Proof and Automation presentation;
- explicit refusal when a native capability/config mapping is not available.

### SQX owns

- Builder strategy generation;
- genetic/evolutionary producer algorithms;
- indicator/building-block strategy semantics;
- backtest engine behavior;
- native ranking/filter calculations;
- cross-check/robustness producer algorithms;
- Retester execution;
- optimization/Walk-Forward producer behavior;
- Custom Project task execution/databank semantics;
- native strategy/result artifacts.

## Foundation vertical — must pass before feature expansion

The first product-completion gate is one simple indicator concept executed end to end through the real backend.

Required proof:

1. Start the actual TraderCockpit application/runtime and verify SQX 144.2953 worker health.
2. Create/open one simple indicator-based idea from the UI.
3. Resolve only the native SQX-required missing fields and explicitly approve the resulting Construct plan.
4. Produce and persist the exact native Builder configuration snapshot used for that idea. The first indicator/block must be selected from a retained native configuration whose mapping can be proved; do not hard-code EMA/RSI merely because the name appears in a screenshot.
5. Start a bounded native Builder job from TraderCockpit.
6. Show real native progress and produce at least one native `.sqx` Builder survivor.
7. Import that exact archive into TraderCockpit custody and show it in Candidate Lab with its real native identity/result fields.
8. Run one downstream native validation/retest operation on the selected candidate using an explicit native profile.
9. Display the resulting Backtest/validation state and exact trade/result evidence.
10. Display a Proof view linking idea → configuration → SQX build/job → candidate archive → result/validation evidence.
11. Restart/reload the product and recover the same durable identities/artifacts.
12. Prove a malformed/unavailable native configuration fails visibly rather than falling back to a TraderCockpit-generated strategy or synthetic result.

**Until this vertical passes through the real application, the product foundation is not complete and no additional backend feature lane outranks it.**

## Implementation order

### A. Correct the authority boundary

- Remove production use of TraderCockpit-owned replacements for SQX producer algorithms.
- Preserve useful parity tests/source analysis as non-production evidence where appropriate.
- Keep canonical TraderCockpit custody/server/read-model infrastructure.

### B. Native runtime + desktop spine

- one supervised SQX worker boundary;
- exact build/launcher verification;
- explicit runtime health and structured producer errors;
- desktop opens the product only after the application/runtime is healthy;
- application shutdown must not orphan worker processes.

### C. Construct compiler

- model the required native Builder configuration fields;
- read exact native project/task/config templates;
- map approved user intent into a complete native snapshot;
- expose unresolved/unsupported fields before launch;
- preserve exact snapshot custody and a human-readable diff.

### D. Builder job and Candidate Lab

- start/stop/status native Builder jobs;
- observe native progress/databanks;
- import native survivors automatically and idempotently;
- render real candidates and initial results;
- reload/reopen from durable state.

### E. Backtest/validation funnel

- map Fast/Golden or custom product profiles to exact native cross-check/Retester/Optimizer plans;
- run methods in the real producer;
- short-circuit failed candidates according to native filter semantics;
- render method-specific results without invented normalization.

### F. Proof

- bind configuration, engine, candidate, result, validation plan/outcomes and native artifacts;
- support exact comparison and export only from verified custody.

### G. Native Custom Project automation

- import/create supported native `.cfx` workflows;
- run them through SQX;
- observe task/databank progress and loops;
- expose simplified automation controls without a duplicate workflow runtime.

### H. Desktop/product hardening

- consolidate the approved TraderCockpit UI into the desktop shell;
- replace fixtures/placeholders with canonical backend reads;
- package/configure the SQX worker according to deployment constraints;
- full clean-machine install/start/stop/reopen/browser/desktop acceptance.

## Disposition of current work

- **PR #23 native candidate/Retester/custody:** retain useful native adapter, custody, lifecycle and readback work; reposition Retester downstream in the full lifecycle.
- **PR #2 native Builder control:** retain the verified native launch/control direction; expand from preset launch into exact Construct-plan/native Builder job custody.
- **PR #25 TraderCockpit Builder engine:** do not merge the producer/search engine. Salvage only application/UI/custody pieces that remain valid after the native Builder job exists.
- **`product/tradercockpit/builder/evolution.py` on `main`:** quarantine from production producer authority; it may remain temporarily only as evidence/test support until references are moved/removed cleanly.
- **PR #27 TraderCockpit Monte Carlo robustness:** do not merge TraderCockpit-owned robustness production algorithms where SQX owns the corresponding cross-check. Reuse only generic custody/UI pieces if they remain applicable.
- **PR #28 TraderCockpit workflow engine:** do not merge the duplicate task/loop executor. Replace it with native Custom Project control/readback.
- **PR #29 data/trading context:** retain only fields that map to actual native SQX configuration or clearly separate TraderCockpit-only product state.
- **PRs #30–#33 read-only UI/proof surfaces:** reassess after the native spine is green; retain where they consume canonical real backend truth without inventing producer state.

Ingredient PRs that reconstructed isolated SQX algorithms are evidence/test donors, not production producer modules.

## Acceptance rules

A green unit suite is not product completion.

For any producer claim, acceptance must traverse the real application and the real SQX backend. The final product acceptance suite must distinguish:

- application plumbing tests;
- native adapter/configuration tests;
- real SQX runtime integration tests;
- browser/UI tests against the canonical server;
- desktop launch/supervision tests;
- full vertical product tests using native strategy/result artifacts.

No test may substitute a TraderCockpit-generated result for a claim that SQX executed it.

## Required references for implementers

Before changing a stage, inspect the matching artifacts rather than relying on names:

- `docs/product-backbone-spec-v1.md` for the exact application/UI/backend/add-on contract;
- 35-shot SQX panel manifest and original images;
- saved SQX `.cfx` projects and task XML;
- native Builder/Retester result archives and runtime evidence;
- official SQX workflow/program-layout/cross-check/Custom Project behavior;
- TraderCockpit prototype authority, especially Ideas, Apollo guidance, Candidate Lab, Backtest, robustness and Proof;
- `TraderCockpit Assessment.pdf` for the requirement that the application actually complete the generation/validation path rather than accumulate disconnected features.

This document is the controlling architecture. `docs/product-backbone-spec-v1.md` is its binding detailed application contract. Any older instruction that says production must reimplement SQX producer behavior in TraderCockpit-owned algorithms or use a different core research navigation model is superseded.