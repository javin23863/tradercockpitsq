# TraderCockpit Product Architecture v1

## 1. Product decision

`main` is the canonical TraderCockpit product line.

TraderCockpit is one desktop application with multiple product surfaces. **StrategyQuant X 144.2953 is the historical strategy-research producer/backend, not the TraderCockpit Home screen and not the source of unrelated live market, account, signal, execution, risk, or performance truth.**

The production boundary is:

```text
Consumer
  |
  | Google identity + product entitlement
  v
TraderCockpit desktop/application
  |\
  | \ bounded external-LLM transport
  |  \------------------------------> OpenRouter
  |
  +--> Home / live-current read models
  |      market · system · signals · risk · performance · pipeline
  |
  +--> StrategyQuant X screen
         |
         | exact native configuration/control/readback
         v
       StrategyQuant X 144.2953
       AI/AlgoWizard · Builder · Backtest · Cross checks
       Retester · Optimizer/Walk-Forward · Custom Projects
         |
         | native databanks · .sqx · historical results/trades
         v
       TraderCockpit custody · Candidate Lab · Backtest · Proof
```

TraderCockpit must not implement a second Builder, genetic algorithm, strategy language, historical backtester, robustness engine, optimizer, or Custom Project execution engine where SQX owns the operation.

Recovered SQX source/configuration/runtime material is evidence for the installed producer. The older Futures quantitative architecture remains quarantined. Earlier Futures/TraderCockpit Google/OpenRouter work may be used only as authorized consumer-account design lineage.

There is no generic Phase 0 / Phase 1 intake architecture in this product. Apollo is deferred and is not a persistent product spine.

## 2. Binding companion documents

- `docs/product-backbone-spec-v1.md` — detailed application/UI/API/custody contract;
- `docs/home-strategyquant-surface-authority-v1.md` — focused Home/SQX placement record;
- `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/optional `sqx-lab` hierarchy;
- `docs/consumer-openrouter-account-authority-v1.md` — Google account/OpenRouter spend/model-routing authority;
- `docs/repository-consolidation-v1.md` — cleanup and development-desktop delivery rules;
- `IMPLEMENTATION_CHECKLIST.md` — executable implementation and release gates;
- `AGENTS.md` — repository implementation policy.

These documents now use the same product-surface hierarchy. No older route or prototype branch overrides them.

---

# 3. Top-level desktop product surfaces

The top-level application surfaces are exactly:

```text
Home | StrategyQuant X | Explore | Automation | Operate | Settings
```

These surfaces have different authorities.

## 3.1 Home — current/live cockpit

Home is the default desktop surface and preserves the accepted eight-zone Cockpit Home:

1. **Market Overview** — current market context from the live market-data authority;
2. **System Status** — application/runtime/worker/provider health and alerts;
3. **Alpha Stack** — current strategy/candidate/champion/deployed identity context;
4. **Pipeline Overview** — current research/validation/deployment attention state;
5. **Signals** — current signal/confluence state when live strategy and market producers exist;
6. **Risk** — current portfolio/exposure/loss/deployment risk;
7. **Performance** — explicitly scoped current account/deployed-strategy performance, with historical summaries labeled when shown;
8. **Quick Actions** — navigation to owning surfaces without hidden workflows.

Home is intentionally not a copy of SQX. Historical Builder/Retester results do not become live prices, signals, account risk, or execution state merely because SQX produced them.

A Home zone with no configured producer remains visibly unavailable, stale, or pending. The UI never fills an empty zone with synthetic/demo truth.

## 3.2 StrategyQuant X — historical research

StrategyQuant X is one dedicated top-level screen at `/strategyquant`.

Inside that screen the persistent research workflow is:

```text
Construct -> Backtest -> Proof
```

Construct tabs:

```text
Idea | Specification | Build | Candidates
```

Backtest tabs:

```text
Overview | Trades | Robustness | Configuration
```

Proof is the exact evidence/provenance chain.

These are internal SQX research states, not top-level TraderCockpit workspaces. Compatibility redirects from old `/construct/*`, `/backtest/*`, or `/proof` paths may exist, but they do not define navigation authority.

Do not add Optimizer, Monte Carlo, Walk-Forward, MCP, LLM, or individual add-ons as permanent research tabs. Native methods appear contextually inside the appropriate stage from backend capability/configuration descriptors.

## 3.3 Explore

Explore is the searchable capability/catalog surface for backend-registered markets, data, indicators, native strategies/templates, validation methods, delivery targets, and installed add-ons.

The frontend does not maintain a competing master catalog.

## 3.4 Automation

Automation presents native SQX Custom Projects and other explicitly registered automation capabilities. TraderCockpit may inspect/configure/control/read native workflows but must not translate them into a second product-owned task engine.

## 3.5 Operate

Operate owns live/deployed runs, execution, risk, and performance where those capabilities actually exist. Historical SQX results remain historical unless explicitly linked to a deployed/live identity.

## 3.6 Settings

Settings owns account/allowance, model policy, native runtime, provider, add-on, and application configuration.

---

# 4. Producer ownership

## 4.1 StrategyQuant X owns historical strategy/research computation

SQX 144.2953 owns:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy generation/search;
- genetic/evolutionary mechanics;
- native strategy/block semantics;
- historical backtest engine behavior;
- native fitness/ranking/filter calculations;
- cross-check/robustness algorithms;
- Retester execution;
- Optimizer and Walk-Forward execution;
- Custom Project task/databank execution;
- native `.sqx` strategy/result artifacts.

A missing programmable seam means inspect/wire more native evidence or expose the capability as unavailable. It does not authorize a TraderCockpit substitute producer.

## 4.2 TraderCockpit owns application mechanics

TraderCockpit owns:

- desktop lifecycle and product navigation;
- live/current Home presentation from correct producers;
- Google consumer identity and stable account subject;
- entitlement/allowance/read models;
- OpenRouter provisioning custody and model-routing policy;
- idea/source intake and revisioning;
- native-requirement gap detection;
- exact configuration editing/review/approval;
- exact native configuration snapshot custody;
- native runtime verification and job control/readback;
- immutable product identities around native artifacts;
- Candidate Lab presentation;
- historical Backtest and Proof presentation;
- capability discovery/add-on registration;
- structured refusal when a required native/live/external producer is unavailable.

TraderCockpit may maintain producer-neutral lifecycle/custody envelopes. Those envelopes must not become hidden alternative quantitative engines.

---

# 5. Native SQX authoring hierarchy

Use this order:

1. **Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder** — primary native strategy authoring/generation authority.
2. **Native SQX MCP (`ServletMCP`)** — first-party inspection/control surface. Retained 144.2953 publishes exactly `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, and `stop_project`.
3. **`sqx-lab`** — optional external-LLM/custom-artifact extension for install-derived blocks, groups, `.sqx` templates, and `project.cfx` projects.
4. **TraderCockpit** — orchestration, custody, approval, control/readback, and presentation.

Do not invent MCP authoring methods absent from the retained registry. Do not route every idea through `sqx-lab`. Any `sqx-lab` artifact must be derived from and accepted by the target SQX installation.

---

# 6. Consumer account and OpenRouter architecture

Google authenticates the consumer to TraderCockpit; it is not an OpenRouter login.

The intended path is:

```text
verified Google sign-in
  -> stable TraderCockpit subject
  -> configured starter/plan entitlement
  -> provider-bounded per-consumer OpenRouter spend authority
  -> backend-selected workhorse/model policy
  -> account-attributed usage/readback
```

Rules:

- the operator/application keeps the OpenRouter provisioning/management credential;
- browser/consumer code never receives that management credential;
- provider-enforced limit/reset/expiry is the hard spend boundary, with internal accounting/readback for product state;
- starter amount, renewal cadence, and paid-plan values are configuration, not source-code guesses;
- current workhorse policy is `z-ai/glm-5.3-flash` but model/provider/fallback policy is backend-configurable;
- OpenRouter may support bounded language assistance and authorized tools, but it never replaces SQX strategy/backtest/robustness/optimization authority.

---

# 7. Historical StrategyQuant X lifecycle

## 7.1 Idea/source

A user may begin with an idea, source, indicator, existing native strategy, template, notes, paper, or explicit rules. TraderCockpit owns the user-facing record and provenance.

## 7.2 Native authoring when needed

Use the smallest proven native authoring capability. Prefer native SQX AI/AlgoWizard where a supported invocation exists. MCP is limited to its published control/read tools. `sqx-lab` is optional custom-artifact tooling only.

## 7.3 Specification / exact native construction plan

TraderCockpit resolves only information required to create an inspectable native SQX configuration. Relevant native Builder families include:

`What to build -> Parts to improve -> Genetic options -> Data -> Trading options -> Building blocks -> ATM -> Money management -> Cross checks -> Ranking -> Notes`.

Required native fields that cannot be mapped truthfully remain unresolved and lock compute.

## 7.4 Build

TraderCockpit compiles an approved exact native configuration snapshot and launches the native Builder only after runtime/configuration verification. SQX owns generation, GA behavior, initial backtests, native ranking/filtering, and Builder databank output.

## 7.5 Candidates

Candidate Lab consumes real native Builder survivors. It preserves:

`idea/source revision -> exact native configuration -> SQX job -> native .sqx artifact -> product candidate identity`.

Candidate Lab is not another strategy generator.

## 7.6 Backtest

Backtest shows historical producer-backed analysis:

- Overview — historical summary and validation state;
- Trades — actual native trade records/chart context;
- Robustness — selected native cross-check/retest/optimization methods rendered dynamically;
- Configuration — exact native configuration that executed.

Retester and Optimizer operate downstream on existing strategies. TraderCockpit invokes/reads them; it does not reproduce their algorithms.

## 7.7 Proof

Proof binds the complete chain: approved idea/source, native configuration bytes, SQX build/job, historical data/settings, native `.sqx`, native results/trades, validation plan/outcomes, and current product status.

Generated, tested, passed, promoted, exported, and deployed remain distinct states.

## 7.8 Automation

Custom Projects automate native workflows. TraderCockpit preserves their task/databank topology and invokes/observes native execution. Unknown native tasks remain opaque until evidenced.

---

# 8. Home/live-current lifecycle

Home evolves independently from the historical SQX Foundation Vertical.

For every Home zone:

1. identify the actual current/live producer;
2. expose one canonical backend read model;
3. include timestamp/scope where meaningful;
4. distinguish current, stale, pending, and unavailable state;
5. label any historical SQX summary explicitly;
6. never fabricate a value to make the dashboard look complete.

Examples:

- live market data comes from the selected market-data authority, not Builder data;
- live signals require current market + strategy/deployment context, not historical entry markers;
- current risk requires account/execution/exposure state, not historical drawdown;
- current performance must state whether it is account, deployed-strategy, or historical research performance.

---

# 9. One application/runtime/custody family

The product uses one canonical application server, one state-root/custody family, one native-SQX gateway family, and one desktop UI.

The development desktop is a thin native window around that same canonical server and `web/` surface. It does not own product logic or create a second backend.

Desktop HTTP exposure is loopback-only and browser-local mutation requests are protected against cross-origin/rebinding access. Explicit network-facing development hosting is a separate deliberate app-server mode.

All consequential native operations pass through backend authorization/control boundaries; the browser never invokes SQX processes directly.

---

# 10. Identity, custody, and evidence rules

- Do not mint candidate identity before a real native candidate exists.
- Do not mint run/job identity before a real operation exists.
- Preserve exact native configuration bytes and source/build identity.
- Preserve exact `.sqx` archive identity and provenance.
- Treat current-head/lifecycle pointers as mutable references to immutable events/objects rather than rewriting historical evidence.
- Fail closed on missing/mismatched runtime, launcher, configuration, project path, archive, or custody state.
- Resolved SQX project/config paths must remain inside the verified runtime; symlink/junction escape is forbidden.
- Trusted native launcher identity must be verified before execution.

---

# 11. Capability/add-on model

Capabilities register through one backend authority and typed presentation descriptors. Add-ons may contribute only to stable extension slots.

They may not:

- inject arbitrary frontend script/HTML;
- maintain a competing capability catalog;
- rewrite top-level desktop navigation;
- rewrite the internal SQX core research stage set;
- claim producer truth they do not own.

Unknown descriptor versions or unsupported capabilities fail closed.

---

# 12. Repository and cross-repository boundary

Production code must not import recovered/reference/Futures trees as runtime dependencies.

Forbidden production leakage includes:

- `javin23863/futures` quantitative architecture;
- Phase01 intake architecture;
- persistent Apollo product spine;
- copied personal/customer credentials or machine-specific state;
- TraderCockpit-owned replacements for SQX Builder/GA/backtest/robustness/optimizer/Custom Project execution.

Authorized lineage includes retained SQX evidence and the earlier consumer Google/OpenRouter concept only where the current architecture explicitly adopts it.

`tools/check_production_boundary.py` mechanically enforces the major prohibited paths/imports/markers and complements manual source review.

---

# 13. Release gates

## 13.1 Repository/desktop consolidation

Before feature expansion resumes:

- canonical production tree free of prohibited leakage;
- Home and dedicated StrategyQuant X screen accepted in Chromium;
- one runnable desktop host around the canonical server/UI;
- exact-head Product Runtime Acceptance green;
- intentionally small PR queue;
- GitHub default branch changed to `main`.

## 13.2 Native SQX Foundation Vertical

```text
StrategyQuant X
-> Idea/source
-> native authoring/configuration
-> approved exact Builder configuration
-> native Builder
-> real .sqx survivor
-> Candidate Lab
-> downstream native validation/retest
-> Backtest
-> Proof
-> restart/reopen same identities
```

Mocks, fixtures, external-LLM-only artifacts, or TraderCockpit substitute quantitative engines do not satisfy this gate.

## 13.3 Consumer account/LLM

```text
Google sign-in
-> stable internal subject
-> configured allowance
-> provider-bounded OpenRouter spend
-> backend-configured workhorse
-> usage attribution/readback
-> clean limit refusal
-> no spend after lapse/revocation
```

## 13.4 Home/live capability

Each Home zone becomes active only when its current/live producer is integrated through a canonical read model and unavailable/stale/current semantics are tested.

---

# 14. Delivery rule

A user-facing feature is not product-complete when only unit tests pass.

Required path:

`reviewed exact head -> Product Runtime Acceptance -> canonical main -> development desktop -> feature visible/inspectable in its correct product surface`.

Historical research features land in the StrategyQuant X screen. Live/current features land in Home or Operate as appropriate. Account/provider configuration lands in Settings. The product grows through this one desktop rather than through disconnected backend fragments.
