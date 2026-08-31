# TraderCockpit Product Backbone Specification v1

## Status and authority

This is the binding detailed implementation specification beneath `docs/product-architecture-v1.md`.

It is read together with:

- `docs/home-strategyquant-surface-authority-v1.md`;
- `docs/sqx-authoring-authority-v1.md`;
- `docs/consumer-openrouter-account-authority-v1.md`;
- `docs/repository-consolidation-v1.md`;
- `IMPLEMENTATION_CHECKLIST.md`;
- `AGENTS.md`.

The product hierarchy is now direct and non-contradictory:

```text
TraderCockpit desktop
  Home                -> live/current orientation
  StrategyQuant X     -> historical strategy research
  Explore             -> capability discovery
  Automation          -> native workflow/Custom Project surface
  Operate             -> live/deployed execution/performance/risk
  Settings            -> account/runtime/provider configuration
```

Inside **StrategyQuant X** only:

```text
Construct -> Backtest -> Proof
```

No older route name, prototype branch, or isolated test may restore the superseded five-workspace/Apollo shell or promote the SQX research stages into global application navigation.

---

# 1. Evidence vocabulary

## `PROVEN`

Directly supported by accepted product authority, retained/executable SQX evidence, or verified product code/runtime behavior.

## `SPECIFIED`

A binding TraderCockpit product/application decision selected to present proven capabilities coherently. It is not a claim about hidden SQX internals.

## `OPEN EVIDENCE`

A native mapping, result field, command seam, live producer, progress state, or capability detail still requiring evidence. It stays unavailable/constrained until proved; it is never replaced with invented producer behavior.

---

# 2. One product, multiple producer domains

TraderCockpit is the application authority. It does not imply that one backend owns every product surface.

## Historical strategy research

StrategyQuant X 144.2953 is the producer authority for native strategy authoring/generation, historical backtesting, cross-checks/robustness, Retester, optimization/Walk-Forward, and Custom Project execution.

## Live/current product state

Home and Operate consume the appropriate current/live producers for market data, signals, account/execution state, risk, and performance. SQX historical output may be linked or summarized where useful but is not silently promoted into live truth.

## External language assistance

OpenRouter is the bounded external-LLM transport/billing fabric. It does not own account truth, native SQX strategy truth, candidate/result/proof truth, or live market/execution truth.

---

# 3. Global desktop frame

## 3.1 Top-level navigation

The top-level desktop navigation is exactly:

```text
Home | StrategyQuant X | Explore | Automation | Operate | Settings
```

Rules:

- `/home` is the default route;
- `/strategyquant` is one dedicated historical-research screen;
- no top-level `Construct`, `Backtest`, or `Proof` navigation item;
- no persistent Apollo dock;
- no frontend-owned provider/model/capability master list;
- no fabricated runtime, market, account, candidate, run, or result identity in global chrome.

## 3.2 Top bar

The top bar may show:

- current product surface;
- active context identity when one exists;
- canonical application/runtime status;
- native SQX runtime state when relevant;
- account/allowance status when authenticated;
- alerts/settings controls when backed by real state.

Historical research context and live/current context remain explicitly scoped.

## 3.3 Context rail

A context rail may show backend-provided recent work, ideas, strategies, candidates, runs, proofs, automations, deployments, or watch items. It is not a second navigation architecture and does not manufacture counts/categories.

---

# 4. Cockpit Home

Home answers: **What matters now, and where should I go next?**

The accepted Home contains exactly eight semantic zones.

## 4.1 Market Overview

Purpose: current market orientation.

Backend read model should expose only fields owned by the selected market-data authority, for example:

- instrument/symbol identity;
- timeframe/context;
- source/provider;
- session/market state;
- producer timestamp/freshness;
- optional typed market-condition descriptors when actually produced.

Requirements:

- stale state is labeled stale;
- missing feed is unavailable, not replaced with historical SQX bars;
- frontend never creates prices or market regime labels.

## 4.2 System Status

Purpose: current operational readiness and attention.

May include:

- TraderCockpit application server health;
- desktop/runtime state;
- native SQX runtime/worker readiness;
- data/provider/add-on health;
- actionable alerts/errors.

A healthy desktop server does not imply SQX, broker, market feed, or external provider readiness.

## 4.3 Alpha Stack

Purpose: current strategy/candidate/deployment context.

May show only canonical identities and state from custody/lifecycle/execution read models. Distinguish:

- historical candidate;
- validated/promoted research strategy;
- exported strategy;
- deployed/live strategy.

Presence in a native SQX databank does not imply deployment.

## 4.4 Pipeline Overview

Purpose: current work/attention across research, validation, promotion, deployment, and operation.

Rules:

- derive state from canonical lifecycle/read models;
- do not invent generic Phase 0/1 or fixed phase counts;
- pending/not-run/failed/pass remain distinct;
- historical SQX workflow state and live deployment state may be connected but not conflated.

## 4.5 Signals

Purpose: current signal/confluence state.

Activation requires a live/current market context plus a strategy/deployment signal producer. Historical backtest entries/exits are not live signals.

## 4.6 Risk

Purpose: current portfolio/account/deployment risk.

Activation requires the actual account/execution/risk producer. Historical drawdown or Monte Carlo output may provide research evidence elsewhere but is not current account exposure.

## 4.7 Performance

Purpose: current performance with explicit scope.

Every displayed metric must identify whether it is:

- live account;
- deployed strategy;
- paper/simulated operation;
- historical research summary.

Scopes are never silently merged.

## 4.8 Quick Actions

Purpose: navigation to owning product surfaces.

Examples may include opening StrategyQuant X, Explore, Automation, Operate, or Settings. A Quick Action does not create a parallel workflow or bypass authorization.

## 4.9 Home unavailable-state contract

Every zone supports at minimum:

- `current`;
- `stale` where freshness matters;
- `pending` where work exists but is unresolved;
- `unavailable` where no producer/capability is configured;
- `error` for explicit producer failure.

No frontend fallback may turn unavailable into synthetic current data.

---

# 5. StrategyQuant X screen

StrategyQuant X answers: **What historical strategy are we constructing/testing, what did native SQX execute, and what evidence supports the result?**

Canonical top-level path: `/strategyquant`.

Internal research navigation:

```text
Construct | Backtest | Proof
```

Research stage/tab values must resolve only to registered states. Invalid values collapse/refuse deterministically; arbitrary query text does not create new product states.

## 5.1 Construct / Idea

Purpose:

- capture idea/source/provenance;
- open an existing native strategy/template when appropriate;
- preserve revision identity;
- identify unresolved native requirements;
- use bounded language assistance only as an assistant to explicit product state.

Rules:

- entering text does not create candidate or run identity;
- language assistance does not invent trading meaning;
- native authoring follows the hierarchy in `docs/sqx-authoring-authority-v1.md`.

## 5.2 Construct / Specification

Purpose: resolve the smallest complete set of native SQX requirements for one exact executable plan.

Native requirement families may include:

- strategy shape / What to build;
- Parts to improve;
- entries/conditions/periods;
- exits/stops/targets;
- historical data/symbol/timeframe/date ranges/IS-OOS/precision;
- trading/session/cost assumptions;
- Building blocks and parameter ranges;
- ATM;
- money management/sizing;
- search/build mode;
- Genetic options when native genetic Builder mode is selected;
- Ranking/basic filters;
- selected Cross checks/filters;
- Notes/provenance where relevant.

Each field/group has explicit state such as `proven_default`, `user_selected`, `unresolved`, `unsupported`, or `not_applicable`. Missing required native meaning locks Build.

## 5.3 Construct / Build

Purpose: compile/review/approve/launch one exact native SQX configuration.

Required custody:

1. source native template/project/task identity;
2. verified SQX build/runtime identity;
3. source bytes/hash;
4. typed approved changes;
5. exact executable bytes/hash;
6. human-readable source-to-executed diff;
7. approval bound to that exact revision;
8. native job/control identity after launch.

TraderCockpit must preserve untouched native fields and may mutate only registered typed fields.

Native Builder owns strategy generation, GA behavior, initial historical testing, fitness/ranking/filtering, and databank output.

Build remains locked when:

- runtime/build identity is unverified;
- required config path escapes the verified runtime;
- expected source hash mismatches;
- required native field is unresolved;
- approval belongs to a different config revision.

## 5.4 Construct / Candidates

Candidate Lab consumes real native Builder survivors.

Candidate custody chain:

```text
Idea/source revision
-> Construct plan
-> exact native configuration
-> native SQX Builder job
-> exact .sqx artifact
-> TraderCockpit candidate identity
```

Requirements:

- immutable archive/content identity;
- idempotent import;
- no local candidate fabricated from UI text;
- no cross-job/config substitution;
- producer-backed fields plus TraderCockpit provenance only.

## 5.5 Backtest / Overview

Shows historical producer-backed summary and validation lifecycle for a selected candidate/run. It may include compatible comparisons only when all compared metrics/results have explicit scope and provenance.

## 5.6 Backtest / Trades

Shows actual native historical trade/order records and chart context. No synthetic trades are generated to fill the view.

## 5.7 Backtest / Robustness

Shows selected native cross-check/retest/optimization methods dynamically from an exact native-backed plan.

Examples may include evidenced What If, Monte Carlo trade manipulation, higher precision, additional markets, Monte Carlo retest, Sequential Optimization, parameter permutation/optimization profiles, Walk-Forward Optimization, or Walk-Forward Matrix.

These names are capabilities/methods, not permanent tabs. Exact method/order/settings/filter semantics come from native evidence/configuration.

## 5.8 Backtest / Configuration

Shows the immutable configuration that actually executed, including source identity, executed bytes/hash, approved diff, SQX build/job identity, and historical data/trading settings.

## 5.9 Proof

Proof is the durable chain connecting:

- idea/source revision;
- approved construct revision;
- exact native configuration bytes;
- SQX build/runtime/launcher identity;
- native project/task/job identity;
- historical market/data/settings;
- native `.sqx` survivor;
- native result/trade artifacts;
- validation plan/method/outcomes;
- current product status.

Proof never infers a pass from an unexecuted method or promotion from a historical result.

---

# 6. Native SQX authoring and control

Hierarchy:

1. native SQX AI Wizard / AI Assistant + AlgoWizard / Builder;
2. retained SQX MCP published inspection/control tools;
3. optional `sqx-lab` custom native-artifact tooling;
4. TraderCockpit orchestration/custody/UI.

Retained MCP tool set in 144.2953 is exactly:

- `list_projects`;
- `list_databanks`;
- `list_strategies`;
- `get_strategy_stats`;
- `run_project`;
- `stop_project`.

MCP is not treated as an authoring endpoint unless later native evidence proves such a capability.

---

# 7. Canonical application server and desktop

There is one TraderCockpit application server authority (`ThreadingHTTPServer` in the current implementation), one product API family, one state-root/custody family, and one `web/` UI.

The development desktop:

- starts that canonical handler/server;
- binds its private control server to literal loopback only;
- rejects Host values that do not match its exact loopback host/port to prevent DNS rebinding;
- rejects browser-originated mutations that are not same-origin;
- opens the same canonical web UI in one native window;
- shuts its server down when the window exits;
- contains no product/SQX/account business logic of its own.

The explicit `tradercockpit.app_server` development entrypoint may be network-configurable for deliberate development use. That does not make the desktop wrapper network-facing by default.

---

# 8. Core API/read-model blueprint

Exact endpoint names may evolve only through reviewed contract changes. Current/required families include:

## Application/runtime

- application/system status;
- native SQX runtime descriptor/readiness;
- installed capabilities/add-ons.

## Home/live

- market overview read model;
- Alpha Stack read model;
- pipeline/attention read model;
- signal read model;
- risk read model;
- scoped performance read model.

These remain unavailable until their real producers are integrated.

## SQX research

- native preset/configuration discovery;
- exact native config/approval custody;
- native job control/readback;
- native Builder output discovery/import;
- candidate reads;
- exact historical run/result reads;
- native validation/retest/optimization plan/results;
- proof/evidence reads;
- Custom Project topology/control/readback.

## Account/model

- active account read;
- allowance/usage read;
- model-policy read;
- authenticated sign-in/session state;
- provider spend-authority metadata without provider management secrets.

Browser query/body values never choose arbitrary account subjects, provider management credentials, native runtime roots, executable paths, or unrestricted model routes.

---

# 9. Storage and identity

The existing content-addressed/immutable-event pattern remains the product default where it fits.

Rules:

- immutable evidence is never rewritten to represent current state;
- mutable current-state pointers are atomic and explicitly scoped;
- exact native bytes/hashes remain part of custody;
- account and research state do not share ambiguous identity namespaces;
- model-policy changes do not rewrite durable account identity/history;
- live read models carry producer/time/scope rather than masquerading as immutable historical artifacts.

Cross-process correctness is required for account grants or other money/entitlement state; process-local locking alone is insufficient.

---

# 10. Native runtime trust and filesystem boundary

Before native execution:

- verify expected SQX build/version markers;
- verify trusted launcher identity/hash;
- resolve project/config paths physically, not only lexically;
- refuse symlink/junction/path escape outside authorized SQX runtime;
- verify expected preset/config/artifact hashes where the contract requires them;
- fail closed on missing/mismatched runtime state;
- never accept browser-selected executable/runtime filesystem paths.

Native process errors and refusal reasons are structured and visible.

---

# 11. Consumer account/OpenRouter backbone

Approved path:

```text
Google sign-in
-> stable TraderCockpit subject
-> configured starter/plan entitlement
-> bounded provider spend authority
-> backend-selected OpenRouter model policy
-> account-attributed usage/readback
```

Required invariants:

- Google issuer/subject normalization cannot split one consumer into multiple accounts;
- starter grant is idempotent and safe across multiple writer processes;
- grant-policy identity is durable;
- provider hard limit cannot exceed authorized product allowance;
- provider management credential never reaches browser/customer;
- account usage state and model policy are separate authorities;
- current default workhorse is `z-ai/glm-5.3-flash`, configurable in backend;
- exhausted/revoked/lapsed state refuses before further spend.

Commercial values stay configuration.

---

# 12. Capability/add-on descriptors

One backend capability registry is authoritative.

A descriptor identifies at minimum:

- capability ID/version;
- owning producer;
- availability/state;
- supported product placement/extension slot;
- configuration/read/action schema versions;
- optional typed visualization/presentation descriptors.

Rules:

- no arbitrary script/HTML injection;
- no add-on-created top-level app navigation without an explicit product architecture change;
- no add-on-created replacement for internal SQX core stages;
- unknown descriptor versions fail closed;
- frontend and language tools consume the same capability authority rather than separate hard-coded lists.

---

# 13. UI truthfulness and security

- Escape all untrusted text before HTML composition.
- URL/query state selects only registered product/research states.
- Browser cannot provide durable identity simply by placing a value in the route.
- Browser never receives OpenRouter provisioning secrets or unrestricted native process control.
- Consequential actions pass through backend authorization and verified producer state.
- UI disabled/pending/unavailable state is not cosmetic; it must match backend capability/read state.
- No fixture/demo value is displayed as product truth outside explicit test/development fixtures.

---

# 14. Repository salvage and quarantine rules

## Vetted native donors

- PR #15: Custom Project topology/path custody; native execution stays SQX.
- PR #23: candidate/Retester/readback and trusted launcher verification; integrate only pieces that fit canonical shared contracts.

## Frozen/superseded material

- TraderCockpit-owned Builder/GA/evolution producer code;
- duplicate robustness/optimizer/workflow producers;
- persistent Apollo product spine;
- Phase01 architecture;
- old five-workspace/21-state navigation shell;
- stale account PR #36 as a merge candidate (use only donor ideas/tests where valid).

Historical Git branches remain evidence. They do not remain active product authority.

---

# 15. Acceptance matrix

## Repository consolidation

Must prove:

- no prohibited production imports/paths/markers;
- duplicate TraderCockpit Builder producer removed;
- `/home` default desktop route;
- all eight Home zones present;
- `/strategyquant` distinct from Home;
- exact internal Construct/Backtest/Proof and tab sets;
- browser back/forward and compatibility redirects;
- desktop starts/stops canonical server;
- desktop loopback/Host/origin protections;
- current browser tooling/Chromium acceptance;
- full Product Runtime Acceptance green on exact head.

## Native SQX Foundation Vertical

Must prove through the development desktop:

```text
StrategyQuant X
-> Idea/source
-> exact native specification/configuration
-> approval
-> native Builder
-> real .sqx candidate
-> downstream native validation/retest
-> Backtest
-> Proof
-> restart/reopen same identities
```

## Home/live activation

Each Home zone requires its actual producer/read model, freshness/scope semantics, and unavailable/stale/current tests before displaying real values.

## Consumer account/LLM

Must prove stable Google identity, cross-process-safe grant, provider hard spend ceiling, configured model policy, usage attribution, and refusal after exhaustion/revocation/lapse.

---

# 16. Delivery rule

Every merged user-facing feature must be visible or inspectable through the one development desktop immediately after landing.

A historical research feature belongs in the StrategyQuant X screen. A live/current feature belongs in Home/Operate. Account/runtime/provider configuration belongs in Settings. Capability discovery belongs in Explore. Native workflow automation belongs in Automation.

Backend-only infrastructure may land only when it is a prerequisite for a defined product path and is observable through an appropriate status/read model.

No feature is called product-complete merely because a unit suite or isolated backend PR is green.
