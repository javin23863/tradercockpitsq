# Product Architecture v1

This document is the stable architecture authority for the platform.

## 1. Product identity

TraderCockpit is one desktop trading platform with these top-level surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

The platform owns its product identity and user experience.

The accepted visual/product authority is the multicolor "ESQ TraderCockpit" prototype pinned in
`references/ui-authority/` (five accepted screens + `manifest.json`). It supersedes the earlier
dark-blue `Chart / Backtest / Proof` shell. UI-impacting work must match that authority; it must
not reintroduce the dark-blue shell or invent a new direction without an explicit
product-authority change.

StrategyQuant X / SQX 144.2953 is a native historical-research backend producer where currently proven. It is not the platform name and not a user-facing workspace label.

## 2. Home and Research are separate domains

### Home

Home is the multicolor Cockpit Home defined by the `cockpit-home` authority screen. Its zones
are:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

elaborated as shown in the authority with Engine & System Status, System Alerts, Resource
Usage, and Signal Feed, plus the persistent Apollo assistant. Each zone reads from the producer
that actually owns the current/live state. Historical research results may be summarized only
with explicit scope; they never become live prices, signals, account risk, execution state, or
current performance by implication. Unavailable live producers render
unavailable/stale/pending/error state rather than fabricated values — and the accepted design
keeps those truthful states visually consistent rather than dominating the cockpit.

### Research

Research is the historical strategy-research workspace. Its accepted workflow is:

`Idea → Construct → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

Construct supports distinct problem-solving modalities, all feeding the same downstream custody:

- Random Discovery — native SQX Builder search;
- Genetic / Evolutionary search — native SQX GA (Evolutionary Search authority screen);
- Machine Learning / Models — platform-owned modality (see 3, Producer ownership).

Backtest surfaces cover `Overview | Trades | Robustness | Configuration`, presented in the
accepted Test & Validate, Evolutionary Search, and Indicators & Models grammar. Delivery /
Simulation covers prop-firm/paper simulation and hand-off after Proof.

Canonical route: `/research`. These are internal Research states, not top-level workspaces.

## 3. Producer ownership

### Native historical-research producer

Native SQX owns:

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

The authorized installed SQX 144.2953 program is the primary executable specification for how those capabilities are configured, persisted, launched, and read back whenever that runtime is available. Direct runtime observation, saved native artifacts/configurations, screenshots, and real bounded scenario runs establish integration behavior. Retained/decompiled source and archived references are supporting implementation aids, not prerequisite evidence gates when the running producer can answer the question directly.

A missing integration seam does not transfer this authority into platform-owned substitute algorithms. The product must inspect/run the actual producer, use source/reference material only where it clarifies non-observable details, or expose the capability as unavailable. It must not block an observable integration on a separate retained-evidence checkpoint.

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

### Machine Learning / Models modality (platform-owned)

The Machine Learning / Models modality is platform-owned and distinct from SQX's owned
semantics. It applies standard, well-known ML libraries (decision trees, forests, gradient
boosting, neural nets, classifiers) across indicators/strategies/assets to produce
signals/features/models. Its artifacts flow into the same Candidate → Backtest → Robustness →
Proof custody, where historical evaluation and robustness remain owned by native SQX wherever
SQX owns that behavior. This modality is NOT a substitute for SQX Builder/GA/backtest/robustness/
optimizer/Custom-Project execution (which remain forbidden to duplicate); it is a separate,
explicitly-scoped research capability. Model mathematics is grounded against the curated quant
knowledge library rather than invented, and the modality exposes truthful unavailable state
until its backend is connected.

### Apollo assistant and knowledge library (platform-owned)

Apollo is the persistent in-product assistant surface in the authority. It is a bounded LLM
surface under the consumer account/model boundary (section 5), grounded against the curated
Quant-Guild knowledge library
(`https://github.com/romanmichaelpaolucci/Quant-Guild-Library`) for anti-hallucination. The
knowledge library is reference data (ingested/retrieved), never a runtime code import
(section 11). Apollo assists with intent, explanation, summaries, and approved tools; it never
owns producer truth, never becomes a result/quantitative authority, and never mutates native
state directly. This bounded assistant is explicitly distinct from the forbidden legacy
"persistent Apollo product spine" (section 11).

## 4. Native authoring/control hierarchy

Use the smallest actual native capability that serves the user path:

1. native SQX AI Wizard / AI Assistant + AlgoWizard / Builder for native authoring/generation;
2. retained native MCP for its published inspection/control tools only;
3. optional `sqx-lab` custom native-artifact tooling only when explicitly needed;
4. platform orchestration/custody/presentation around those producer capabilities.

The retained MCP tool set in 144.2953 is limited to:

`list_projects | list_databanks | list_strategies | get_strategy_stats | run_project | stop_project`

Do not invent additional MCP authoring methods.

When exact native behavior is uncertain and the installed runtime is accessible, determine it by exercising the program before designing another platform abstraction. Source/decompiled inspection is secondary to that executable observation unless the required detail is not externally observable.

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

Runtime trust is a security/integrity boundary. It is not a requirement that a user’s current native project/configuration bytes equal one archived reference blob.

Three identities must remain separate:

- runtime trust authorizes the installed build and configured launcher to execute;
- artifact custody preserves the exact bytes and hashes used by each operation;
- producer validity is established by required native structure and the authorized producer's own load/execute/output behavior.

An installed engine-library digest may be captured as immutable execution provenance without becoming a compiled-in validity oracle. Pinning a library digest for authorization requires an explicit independent security policy and configuration authority; a hash recovered from retained evidence is not sufficient justification.

## 8. Identity, custody, and proof

- Text entry alone does not create candidate or run identity.
- A candidate identity is bound to a real producer artifact.
- Exact native configuration bytes and producer build identity are durable custody.
- Custody hashes identify what was used; they do not by themselves prove that only those bytes are producer-valid.
- Native archive/result identity is preserved by content/provenance.
- Mutable current pointers reference immutable events/objects rather than rewriting history.
- Generated, tested, passed, promoted, exported, and deployed remain distinct states.
- Proof binds idea/source, approved configuration, producer/runtime/job, data/settings, native artifact, result/trades, validation outcomes, and current product status.

## 9. Automation

Automation may inspect/configure/control/read registered native workflows. Native Custom Project task execution remains native.

Read-only topology custody may expose task order, native task kind, selected fields, databank references, and exact project archive identity. Unknown native task semantics should be resolved first from the running producer when observable; only genuinely non-observable details remain opaque pending source-level inspection.

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
- a persistent "Apollo product spine" as a second product/result/state authority (the bounded Apollo *assistant* surface in section 3 is a distinct, allowed UI/LLM surface and is not this);
- platform-owned replacements for native Builder/GA/backtest/robustness/optimizer/Custom Project execution (the platform-owned Machine Learning / Models modality in section 3 is a distinct, allowed capability and is not this);
- importing the Quant-Guild-Library (or any reference/source repository) as a runtime code dependency (it is reference/knowledge data only);
- copied personal/customer credentials or machine-specific state.

`tools/check_production_boundary.py` enforces the major prohibited path/import/marker rules and complements manual review.

## 12. Delivery model

Every user-facing feature is delivered through the same development desktop.

Required development path:

`current main -> one bounded implementation branch -> inspect/run actual native producer when relevant -> exact-head acceptance/review -> merge -> delete branch -> feature visible/inspectable in desktop`

Do not split “implementation” and “real installed-producer evidence” into separate completion tracks when the authorized runtime is available. The runtime exercise is part of implementing and accepting the feature.

Implementation order and current status live only in `LIVING_IMPLEMENTATION_PLAN.md`.
