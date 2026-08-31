# Product Architecture v1

This document is the stable architecture authority for the platform.

## 1. Product identity

TraderCockpit is one desktop trading platform with these top-level surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

The platform owns its product identity and user experience.

StrategyQuant X / SQX 144.2953 is a native historical-research backend producer where currently proven. It is not the platform name and not a user-facing workspace label.

## 2. Home and Research are separate domains

### Home

Home is the live/current cockpit and preserves exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

Each zone reads from the producer that actually owns the current/live state. Historical research results may be summarized only with explicit scope; they never become live prices, signals, account risk, execution state, or current performance by implication.

Unavailable live producers render unavailable/stale/pending/error state rather than fabricated values.

### Research

Research is the historical strategy-research workspace.

Inside Research:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

Canonical route: `/research`.

These are internal Research states, not top-level workspaces.

## 3. Producer ownership

### Native historical-research producer

Where proven by retained runtime/configuration/source evidence, native SQX owns:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy generation/search;
- GA/evolutionary mechanics;
- native strategy/block semantics;
- historical backtesting;
- native fitness/ranking/filter calculations;
- robustness/cross-check methods;
- Retester;
- Optimizer and Walk-Forward execution;
- Custom Project execution/task semantics;
- native `.sqx` strategy/result artifacts.

A missing integration seam does not transfer this authority into platform-owned substitute algorithms. The product must inspect more native evidence, integrate the actual producer, or expose the capability as unavailable.

### Platform application authority

The platform owns:

- desktop lifecycle and navigation;
- Home/live-current presentation from correct producers;
- consumer identity/account state;
- bounded external model access and policy;
- idea/source revisioning and provenance;
- exact native configuration mapping, review, approval, and custody;
- native runtime verification/control/readback;
- product identities around jobs/candidates/results/proofs;
- Candidate Lab presentation;
- Backtest and Proof presentation;
- Automation presentation/control boundaries;
- capability/add-on registration;
- structured refusal when a producer is unavailable.

Producer-neutral lifecycle/custody envelopes are allowed. They must not become hidden alternative quantitative engines.

## 4. Native authoring/control hierarchy

Use the smallest proven native capability:

1. native SQX AI Wizard / AI Assistant + AlgoWizard / Builder for native authoring/generation;
2. retained native MCP for its published inspection/control tools only;
3. optional `sqx-lab` custom native-artifact tooling only when explicitly needed;
4. platform orchestration/custody/presentation around those producer capabilities.

The retained MCP tool set in 144.2953 is limited to:

`list_projects | list_databanks | list_strategies | get_strategy_stats | run_project | stop_project`

Do not invent additional MCP authoring methods.

## 5. Consumer account and model access

Google authenticates the consumer to the platform; it is not an OpenRouter login.

Required architecture:

`verified Google identity -> stable platform account -> configured allowance -> provider-bounded spend authority -> backend-selected model policy -> account-attributed usage/readback`

Rules:

- provider provisioning/management credentials remain server-side;
- provider-enforced per-consumer limit/reset/expiry/revocation is the monetary boundary;
- a local credit counter is not the sole hard spend limit;
- starter/plan amounts and renewal rules are configuration, not source-code guesses;
- account history and model policy are separate authorities;
- current default workhorse is `z-ai/glm-5.3-flash`, replaceable through backend configuration;
- exhausted/revoked/lapsed state refuses before further spend;
- account grant admission requiring monetary authority must be correct across multiple writer processes.

External LLM transport may assist with intent, summaries, approved tools, and extensions. It does not own quantitative producer truth.

## 6. One application/runtime family

The product has:

- one canonical Python application server;
- one `web/` UI;
- one desktop host around that same server/UI;
- one state/custody family;
- one native-research gateway/runtime-verification family;
- one product identity chain for idea/configuration/job/candidate/result/proof.

Do not create a second server, account authority, result authority, strategy engine, or UI product spine to avoid integration conflicts.

The desktop private server is loopback-only, validates its exact Host, and rejects cross-origin browser mutations. Browser code never invokes native processes directly.

## 7. Native runtime trust

Before native execution:

- verify expected build/runtime identity;
- verify the executable launcher identity using a trusted digest where required;
- verify relevant native engine artifacts separately where required;
- resolve project/configuration paths physically and keep them inside the authorized runtime;
- reject symlink/junction/path escape;
- preserve exact configuration/archive bytes and hashes;
- fail closed on missing/mismatched runtime, launcher, configuration, project, or artifact state.

## 8. Identity, custody, and proof

- Text entry alone does not create candidate or run identity.
- A candidate identity is bound to a real producer artifact.
- Exact native configuration bytes and producer build identity are durable custody.
- Native archive/result identity is preserved by content/provenance.
- Mutable current pointers reference immutable events/objects rather than rewriting history.
- Generated, tested, passed, promoted, exported, and deployed remain distinct states.
- Proof binds idea/source, approved configuration, producer/runtime/job, data/settings, native artifact, result/trades, validation outcomes, and current product status.

## 9. Automation

Automation may inspect/configure/control/read registered native workflows. Native Custom Project task execution remains native.

Read-only topology custody may expose task order, native task kind, selected proven fields, databank references, and exact project archive identity. Unknown native task semantics remain opaque.

The platform must not create a replacement task-loop engine.

## 10. Capability/add-on model

One backend capability authority supplies typed descriptors used by UI and language/tool surfaces.

Add-ons may contribute only through registered typed extension slots. They may not:

- inject arbitrary frontend JavaScript/HTML;
- maintain a competing capability catalog;
- rewrite top-level product navigation;
- rewrite Research core stages;
- claim producer truth they do not own.

Unknown descriptor versions fail closed.

## 11. Repository boundary

Production code must not import recovered/source/reference/Futures repositories as runtime dependencies.

Forbidden production architecture includes:

- copied Futures quantitative architecture;
- Phase01 intake architecture;
- persistent Apollo product spine;
- platform-owned replacements for native Builder/GA/backtest/robustness/optimizer/Custom Project execution;
- copied personal/customer credentials or machine-specific state.

`tools/check_production_boundary.py` enforces the major prohibited path/import/marker rules and complements manual review.

## 12. Delivery model

Every user-facing feature is delivered through the same development desktop.

Required development path:

`current main -> one bounded implementation branch -> exact-head acceptance/review -> merge -> delete branch -> feature visible/inspectable in desktop`

Implementation order and current status live only in `LIVING_IMPLEMENTATION_PLAN.md`.
