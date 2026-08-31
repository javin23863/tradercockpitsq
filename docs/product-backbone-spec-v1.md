# TraderCockpit Product Backbone Specification v1

## Status and authority

Binding detailed specification beneath `docs/product-architecture-v1.md`.

Read with:

- `docs/home-research-surface-authority-v1.md`;
- `docs/sqx-authoring-authority-v1.md`;
- `docs/consumer-openrouter-account-authority-v1.md`;
- `docs/repository-consolidation-v1.md`;
- `IMPLEMENTATION_CHECKLIST.md`;
- `AGENTS.md`.

The product hierarchy is:

```text
TraderCockpit desktop
  Home        -> live/current orientation
  Research    -> historical strategy research
  Explore     -> capability discovery
  Automation  -> registered workflow/project control
  Operate     -> live/deployed execution/performance/risk
  Settings    -> account/runtime/provider configuration
```

Inside **Research** only:

```text
Construct -> Backtest -> Proof
```

StrategyQuant X / SQX is a current backend producer identity. It is not the platform name and not a top-level workspace label.

---

# 1. Evidence vocabulary

## PROVEN

Directly supported by accepted product authority, retained/executable producer evidence, or verified product/runtime behavior.

## SPECIFIED

A binding platform decision selected to present proven capabilities coherently. It is not a claim about hidden backend internals.

## OPEN EVIDENCE

A producer mapping, field, command seam, live producer, progress state, or capability detail still requiring evidence. It remains unavailable/constrained until proven.

---

# 2. One product, multiple producer domains

The platform is application authority. One backend does not own every product surface.

## Historical strategy research

The current proven native SQX backend owns the historical quantitative operations already established for it: native authoring/generation, historical backtesting, robustness/cross-checks, Retester, optimization/Walk-Forward, Custom Project execution, and native artifacts.

## Live/current product state

Home and Operate consume appropriate current/live producers for market data, signals, account/execution state, risk, and performance. Historical research output may be linked/summarized only with explicit scope.

## External language assistance

OpenRouter is bounded external LLM transport/billing. It does not own account truth, producer truth, candidate/result/proof truth, or live market/execution truth.

---

# 3. Global desktop frame

Top-level navigation is exactly:

```text
Home | Research | Explore | Automation | Operate | Settings
```

Rules:

- `/home` is default;
- `/research` is the historical-research workspace;
- `/strategyquant` is compatibility-only and redirects to `/research`;
- no top-level Construct/Backtest/Proof items;
- no persistent Apollo dock;
- no frontend-owned provider/model/capability master list;
- no fabricated runtime, market, account, candidate, run, or result identity.

The top bar may show current surface/context, canonical runtime status, producer runtime state when relevant, account/allowance state, and backed alerts/settings.

Historical and live/current context remain explicitly scoped.

---

# 4. Cockpit Home

Home answers: **What matters now, and where should I go next?**

Exactly eight semantic zones:

1. Market Overview — current market orientation from selected market-data authority.
2. System Status — application/runtime/worker/provider health and alerts.
3. Alpha Stack — current strategy/candidate/champion/deployed context from canonical state.
4. Pipeline Overview — current research/validation/deployment attention state.
5. Signals — current signal/confluence only when live strategy + market producers exist.
6. Risk — current portfolio/account/deployment risk from actual execution/risk authority.
7. Performance — explicitly scoped current/live or historical summary.
8. Quick Actions — navigation to owning surfaces only.

Minimum zone states: `current`, `stale` where relevant, `pending`, `unavailable`, `error`.

No frontend fallback may turn unavailable into synthetic truth.

---

# 5. Research workspace

Research answers: **What historical strategy are we constructing/testing, what did the native producer execute, and what evidence supports it?**

Canonical route: `/research`.

Internal navigation:

```text
Construct | Backtest | Proof
```

Invalid stage/tab values resolve only to registered states; query text cannot create product states.

## 5.1 Construct / Idea

Capture idea/source/provenance, open existing native strategy/template when appropriate, preserve revision identity, identify unresolved native requirements, and use bounded language assistance only as assistance to explicit product state.

Entering text does not create candidate/run identity.

## 5.2 Construct / Specification

Resolve the smallest complete set of native requirements for one exact executable plan. Each field/group has explicit state such as `proven_default`, `user_selected`, `unresolved`, `unsupported`, or `not_applicable`. Missing required meaning locks Build.

## 5.3 Construct / Build

Compile/review/approve/launch one exact native configuration.

Required custody:

1. source native template/project/task identity;
2. verified producer build/runtime identity;
3. source bytes/hash;
4. typed approved changes;
5. exact executable bytes/hash;
6. human-readable diff;
7. approval bound to exact revision;
8. native job/control identity after launch.

The platform preserves untouched native fields and only mutates registered typed fields.

## 5.4 Construct / Candidates

Candidate Lab consumes real native survivors.

```text
Idea/source revision
-> construct plan
-> exact native configuration
-> native producer job
-> exact native strategy artifact
-> platform candidate identity
```

No local candidate may be fabricated from UI text.

## 5.5 Backtest / Overview

Historical producer-backed summary and validation lifecycle only.

## 5.6 Backtest / Trades

Actual native historical trade/order records and chart context only.

## 5.7 Backtest / Robustness

Selected native cross-check/retest/optimization methods rendered dynamically from exact plans. Method names are capabilities, not permanent tabs.

## 5.8 Backtest / Configuration

Immutable configuration actually executed: source identity, executed bytes/hash, approved diff, producer build/job identity, historical data/trading settings.

## 5.9 Proof

Durable chain connecting idea/source revision, approved construct revision, exact native configuration, producer build/runtime/launcher, native project/job, historical data/settings, native strategy artifact, native result/trade artifacts, validation plan/outcomes, and current product status.

Unexecuted methods never imply pass. Historical result never implies promotion/deployment.

---

# 6. Current native SQX authoring/control boundary

Current hierarchy:

1. native SQX AI Wizard / AI Assistant + AlgoWizard / Builder;
2. retained SQX MCP published inspection/control tools;
3. optional `sqx-lab` custom-native-artifact tooling;
4. platform orchestration/custody/UI.

Retained MCP tools in 144.2953 are exactly:

- `list_projects`;
- `list_databanks`;
- `list_strategies`;
- `get_strategy_stats`;
- `run_project`;
- `stop_project`.

Do not infer MCP authoring capability absent evidence.

---

# 7. Canonical server and desktop

One application server authority, one product API family, one state-root/custody family, one `web/` UI.

Desktop requirements:

- starts canonical handler/server;
- private control server binds literal loopback only;
- exact loopback Host required to prevent DNS rebinding;
- browser-originated mutations must be same-origin;
- opens same canonical web UI;
- shuts server down with window;
- contains no producer/account business logic of its own.

Explicit network-facing development hosting is a separate deliberate server mode.

---

# 8. API/read-model blueprint

Current/required families:

## Application/runtime

- application/system status;
- native producer runtime descriptor/readiness;
- installed capabilities/add-ons.

## Home/live

- market overview;
- Alpha Stack;
- pipeline/attention;
- signals;
- risk;
- scoped performance.

Remain unavailable until real producers exist.

## Research

- native preset/configuration discovery;
- exact config/approval custody;
- native job control/readback;
- native output discovery/import;
- candidate reads;
- exact historical run/result reads;
- validation/retest/optimization plans/results;
- proof/evidence reads;
- native project topology/control/readback.

## Account/model

- active account;
- allowance/usage;
- model policy;
- authenticated session state;
- provider spend-authority metadata without management secrets.

Browser values never choose arbitrary account subjects, management credentials, runtime roots, executable paths, or unrestricted model routes.

---

# 9. Storage and identity

- immutable evidence is not rewritten as current state;
- mutable heads are atomic and scoped;
- exact native bytes/hashes remain custody inputs;
- account and research identity namespaces are unambiguous;
- model-policy changes do not rewrite durable account history;
- live read models carry producer/time/scope;
- cross-process correctness is required for money/entitlement grants.

---

# 10. Native runtime trust/filesystem boundary

Before native execution:

- verify expected build/version markers;
- verify trusted launcher identity/hash;
- physically resolve project/config paths;
- refuse symlink/junction/path escape outside authorized runtime;
- verify expected preset/config/artifact hashes where required;
- fail closed on missing/mismatched runtime;
- never accept browser-selected executable/runtime paths.

Native errors/refusals are structured and visible.

---

# 11. Consumer account/OpenRouter backbone

```text
Google sign-in
-> stable platform subject
-> configured starter/plan entitlement
-> bounded provider spend authority
-> backend-selected model policy
-> account-attributed usage/readback
```

Invariants:

- issuer/subject normalization cannot split one user;
- starter grant idempotent and safe across multiple writers;
- durable grant-policy identity;
- provider hard limit cannot exceed authorized allowance;
- management credential never reaches browser/customer;
- usage state/model policy are separate authorities;
- default workhorse `z-ai/glm-5.3-flash`, backend-configurable;
- exhausted/revoked/lapsed refuses before further spend.

Commercial values remain configuration.

---

# 12. Capability/add-on descriptors

One backend registry is authoritative. Descriptors identify capability ID/version, owning producer, availability, product placement/slot, schema versions, and optional typed presentation descriptors.

No arbitrary script/HTML injection, competing catalog, top-level navigation rewrite, Research core-stage rewrite, or unsupported truth claim.

Unknown descriptor versions fail closed.

---

# 13. UI truthfulness/security

- Escape untrusted text before HTML composition.
- URL/query state selects registered product/research states only.
- Browser route values do not establish durable identity.
- Browser never receives provider management secrets or arbitrary filesystem authority.
- Native controls require canonical backend routes.
- Unavailable/stale/current scope is explicit.

---

# 14. Release acceptance

## Consolidation

- Home + Research navigation exact;
- `/strategyquant` compatibility redirect works;
- all eight Home zones present;
- Research internal stages/tabs exact;
- no Apollo product spine;
- desktop loopback/security tests green;
- full Product Runtime Acceptance green on exact head.

## Research Foundation Vertical

```text
Research
-> Idea/source
-> native authoring/configuration
-> approved exact Builder configuration
-> native Builder
-> real native survivor
-> Candidate Lab
-> downstream native validation/retest
-> Backtest
-> Proof
-> restart/reopen same identities
```

Mocks/fixtures/substitute quantitative engines do not satisfy it.

## Consumer account/LLM

```text
Google sign-in
-> stable subject
-> configured allowance
-> provider-bounded spend
-> configured workhorse
-> usage attribution
-> clean limit refusal
-> no spend after lapse/revocation
```

## Home/live

Each zone activates only when its actual producer is integrated through a canonical read model with unavailable/stale/current semantics.

---

# 15. Completion rule

A user-facing feature must be visible/inspectable in the one development desktop through the canonical application/read-model/producer boundary. Backend-only infrastructure may land only as a prerequisite for a defined product path and must be observable through appropriate status/read models.

No feature is product-complete merely because an isolated unit suite or backend PR is green.
