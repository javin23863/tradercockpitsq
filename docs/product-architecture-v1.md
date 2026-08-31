# TraderCockpit Product Architecture v1

## 1. Product decision

`main` is the canonical product line.

TraderCockpit is a **new desktop trading platform** with multiple product surfaces. The platform owns its own product identity, navigation, account model, application UX, custody, control/readback, presentation, and live-product surfaces.

**Research** is the platform's historical strategy-research workspace.

**StrategyQuant X 144.2953 is one current native historical-research producer/backend authority. It is not the platform name and it is not a user-facing workspace name.** The vendor/backend identity appears only where technical provenance, exact native configuration, runtime diagnostics, or integration contracts require it.

Production boundary:

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
  +--> Research
         |
         | exact native configuration/control/readback
         v
       native historical-research producer(s)
         |
         | current proven backend: StrategyQuant X 144.2953
         | AI/AlgoWizard · Builder · Backtest · Cross checks
         | Retester · Optimizer/Walk-Forward · Custom Projects
         v
       platform custody · Candidate Lab · Backtest · Proof
```

The platform must not implement a second quantitative engine where the current native producer owns the operation.

Recovered SQX material is producer evidence. Older Futures quantitative architecture remains quarantined. Earlier Google/OpenRouter work may be used only as authorized consumer-account design lineage.

Apollo is deferred and is not a persistent product spine. There is no generic Phase 0 / Phase 1 intake architecture.

## 2. Binding companion documents

- `docs/product-backbone-spec-v1.md` — detailed application/UI/API/custody contract;
- `docs/home-research-surface-authority-v1.md` — focused Home/Research placement and naming;
- `docs/sqx-authoring-authority-v1.md` — current native SQX AI/MCP/optional `sqx-lab` backend hierarchy;
- `docs/consumer-openrouter-account-authority-v1.md` — Google account/OpenRouter authority;
- `docs/repository-consolidation-v1.md` — cleanup and desktop delivery rules;
- `IMPLEMENTATION_CHECKLIST.md` — implementation/release gates;
- `AGENTS.md` — repository implementation policy.

No older vendor-named route or prototype branch overrides the Research naming authority.

---

# 3. Top-level desktop product surfaces

Exactly:

```text
Home | Research | Explore | Automation | Operate | Settings
```

## 3.1 Home — current/live cockpit

Home is the default surface and preserves:

1. Market Overview
2. System Status
3. Alpha Stack
4. Pipeline Overview
5. Signals
6. Risk
7. Performance
8. Quick Actions

Home never fabricates live truth. Historical research does not become live prices, signals, account risk, execution state, or current performance merely because a research backend produced it.

## 3.2 Research — historical strategy research

Research is one dedicated top-level workspace at `/research`.

Inside it:

```text
Construct -> Backtest -> Proof
```

Construct:

```text
Idea | Specification | Build | Candidates
```

Backtest:

```text
Overview | Trades | Robustness | Configuration
```

Proof is the exact evidence/provenance chain.

`/strategyquant` is compatibility-only and redirects to `/research`. Older `/construct/*`, `/backtest/*`, and `/proof` routes may also redirect.

Do not add Optimizer, Monte Carlo, Walk-Forward, MCP, LLM, or add-ons as permanent top-level Research stages. Native methods appear contextually from backend capability/configuration descriptors.

## 3.3 Explore

Explore is capability/catalog discovery. Frontend code does not own a competing master catalog.

## 3.4 Automation

Automation presents registered native workflows/projects. The platform may inspect/configure/control/read them but must not recreate a producer-owned task engine.

## 3.5 Operate

Operate owns live/deployed runs, execution, risk, and current performance when real producers exist.

## 3.6 Settings

Settings owns account/allowance, model policy, native runtime, provider, add-on, and application configuration.

---

# 4. Producer ownership

## 4.1 Current native SQX backend owns proven historical quantitative computation

StrategyQuant X 144.2953 currently owns, where proven:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy generation/search and GA mechanics;
- native strategy/block semantics;
- historical backtest engine behavior;
- fitness/ranking/filter calculations;
- robustness/cross-check algorithms;
- Retester;
- Optimizer / Walk-Forward;
- Custom Project task/databank execution;
- native `.sqx` strategy/result artifacts.

A missing programmable seam means inspect/wire more native evidence or expose the capability as unavailable. It does not authorize a platform substitute producer.

## 4.2 Platform owns application mechanics

TraderCockpit owns:

- desktop lifecycle/navigation;
- live/current Home presentation from correct producers;
- Google consumer identity/stable account subject;
- entitlement/allowance/read models;
- OpenRouter provisioning custody/model-routing policy;
- idea/source intake and revisioning;
- native-requirement gap detection;
- exact configuration editing/review/approval;
- exact native configuration custody;
- native runtime verification/job control/readback;
- immutable product identities around native artifacts;
- Candidate Lab presentation;
- historical Backtest/Proof presentation inside Research;
- capability discovery/add-on registration;
- structured refusal when required producers are unavailable.

Producer-neutral lifecycle/custody envelopes must not become hidden quantitative engines.

---

# 5. Current native authoring hierarchy

1. Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder — current native authoring/generation authority.
2. Native SQX MCP (`ServletMCP`) — first-party inspection/control. Retained build 144.2953 publishes exactly `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, and `stop_project`.
3. `sqx-lab` — optional external-LLM/custom-artifact extension.
4. TraderCockpit — orchestration, custody, approval, control/readback, and presentation.

Do not invent MCP authoring methods. Do not route every idea through `sqx-lab`.

---

# 6. Consumer account/OpenRouter architecture

```text
verified Google sign-in
  -> stable platform subject
  -> configured starter/plan entitlement
  -> provider-bounded per-consumer OpenRouter spend authority
  -> backend-selected model policy
  -> account-attributed usage/readback
```

Rules:

- operator/application keeps provisioning/management credentials;
- browser/consumer never receives them;
- provider-enforced limit/reset/expiry is the hard money boundary;
- starter amount/renewal/paid-plan values are configuration;
- current workhorse policy is `z-ai/glm-5.3-flash`, backend-replaceable;
- OpenRouter may support bounded assistance/tools but never replaces quantitative producer authority.

---

# 7. Research lifecycle

## 7.1 Idea/source

User-facing idea/source record and provenance belong to the platform.

## 7.2 Native authoring when needed

Use the smallest proven native authoring capability. Missing native transport is not permission for a platform-only strategy representation.

## 7.3 Specification

Resolve only requirements needed to create one inspectable native configuration. Unresolved required fields lock compute.

## 7.4 Build

Compile an approved exact native configuration snapshot and launch the actual producer only after runtime/configuration verification.

## 7.5 Candidates

Candidate Lab consumes real native survivors and preserves:

`idea/source -> exact configuration -> native job -> native artifact -> product candidate identity`.

Candidate Lab is not a generator.

## 7.6 Backtest

- Overview — historical producer-backed summary/validation state;
- Trades — actual native trade records;
- Robustness — selected native methods dynamically;
- Configuration — exact executed native configuration.

## 7.7 Proof

Proof binds idea/source, exact config bytes, producer build/job, historical data/settings, native artifact, native results/trades, validation identities/outcomes, and current product status.

Generated, tested, passed, promoted, exported, and deployed remain distinct.

## 7.8 Automation

Native workflows remain native. Unknown tasks remain opaque until evidenced.

---

# 8. Home/live-current lifecycle

For every Home zone:

1. identify actual live/current producer;
2. expose one canonical backend read model;
3. include timestamp/scope where meaningful;
4. distinguish current, stale, pending, unavailable;
5. label historical research explicitly if summarized;
6. never fabricate values.

---

# 9. One application/runtime/custody family

Use one canonical application server, one state-root/custody family, one native-research gateway family, and one desktop UI.

The development desktop is a thin loopback-only native window around the same canonical server and `web/` surface. It does not own product logic or create a second backend.

Consequential native operations pass through backend control boundaries; the browser never launches producer processes directly.

---

# 10. Identity/custody rules

- No candidate identity before a real native candidate exists.
- No run/job identity before a real operation exists.
- Preserve exact native config bytes/source/build identity.
- Preserve exact native artifact identity/provenance.
- Mutable heads point to immutable events/objects.
- Fail closed on missing/mismatched runtime, launcher, config, project path, archive, or custody state.
- Resolved project/config paths must remain inside verified runtime.
- Trusted launcher identity must be verified before execution.

---

# 11. Capability/add-on model

Capabilities register through one backend authority and typed presentation descriptors. Add-ons may not inject arbitrary frontend code, maintain competing catalogs, rewrite top-level navigation, rewrite Research core stages, or claim producer truth they do not own.

Unknown descriptor versions fail closed.

---

# 12. Repository boundary

Production must not import recovered/reference/Futures trees as runtime dependencies.

Forbidden leakage includes Futures quantitative architecture, Phase01 intake, persistent Apollo, copied personal/customer credentials, and platform-owned replacements for native quantitative engines.

`tools/check_production_boundary.py` mechanically enforces major prohibited paths/imports/markers and complements manual review.

---

# 13. Release gates

## 13.1 Repository/desktop consolidation

- production tree free of prohibited leakage;
- Home and Research accepted in Chromium;
- one runnable desktop host;
- exact-head Product Runtime Acceptance green;
- intentionally small PR queue;
- GitHub default branch changed to `main`.

## 13.2 Research Foundation Vertical

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

Mocks, fixtures, external-LLM-only artifacts, or substitute quantitative engines do not satisfy this gate.

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

Each Home zone activates only when its real producer is integrated through a canonical read model with unavailable/stale/current semantics.

---

# 14. Delivery rule

A feature is not complete because unit tests pass.

Required path:

`reviewed exact head -> Product Runtime Acceptance -> canonical main -> development desktop -> feature visible/inspectable in its correct product surface`.

Historical research features land in **Research**. Live/current features land in Home or Operate. Account/provider configuration lands in Settings. The platform grows through one desktop, not disconnected backend fragments.
