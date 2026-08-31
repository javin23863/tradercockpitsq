# Agent Execution Policy

This repository has one product line and one implementation plan.

## Read before editing

1. `docs/product-architecture-v1.md`
2. `docs/product-backbone-spec-v1.md`
3. `LIVING_IMPLEMENTATION_PLAN.md`

Do not create a competing roadmap, checklist, recovery plan, donor plan, or architecture override.

## Product identity

This is a new desktop trading platform.

Top-level surfaces are:

`Home | Research | Explore | Automation | Operate | Settings`

Home is the live/current cockpit and preserves exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

Research contains:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

StrategyQuant X / SQX is a native historical-research backend producer identity where technical provenance, runtime, or configuration requires it. It is not the platform name and not a user-facing workspace label.

## Producer ownership

Native SQX owns the quantitative behavior proven to belong to it, including native strategy authoring/AlgoWizard, Builder generation/search and GA behavior, historical backtesting, ranking/filter calculations, robustness/cross-checks, Retester, optimization/Walk-Forward, Custom Project execution, and native strategy/result artifacts.

The platform owns application mechanics: desktop lifecycle, Home/live presentation, accounts/auth, bounded model access, exact native configuration custody/approval, runtime verification/control/readback, product identities, Candidate Lab, Backtest/Proof presentation, Automation presentation, and durable evidence.

If native behavior is not wired, inspect the real native evidence or expose the capability as unavailable. Do not create a substitute quantitative engine.

Home likewise must not fabricate live market, signal, risk, account, execution, or performance state.

## Repository boundary

Production code must not import reference/source/Futures repositories as runtime dependencies.

Forbidden production architecture includes:

- copied Futures quantitative architecture;
- Phase01 intake architecture;
- persistent Apollo product spine;
- a platform-owned Builder/GA/backtester/robustness/optimizer/Custom Project executor;
- copied personal/customer credentials or machine-specific state;
- a second application server, account authority, result authority, or UI product spine.

`tools/check_production_boundary.py` enforces the major path/import/marker rules and complements manual review.

## Native runtime security

Before native execution:

- verify the expected native build/runtime identity;
- verify the executable launcher identity with a trusted digest where required;
- resolve native project/configuration paths physically and keep them inside the authorized runtime;
- reject symlink/junction/path escape;
- fail closed on missing or mismatched runtime/configuration/artifacts;
- browser code never chooses arbitrary executable paths or invokes native processes directly;
- any long-lived native process owned by the desktop must register its lifecycle handle with the desktop worker supervisor before control returns; detached or unregistered long-lived workers are prohibited.

## Consumer account/model boundary

Google authenticates the consumer to the platform; it is not OpenRouter login.

The operator/application keeps provider provisioning credentials. Consumer spend must be bounded by provider-enforced authority, with internal readback/accounting for product state. A local credit counter is not the sole monetary ceiling.

The current default workhorse is `z-ai/glm-5.3-flash`, but model/provider/fallback policy is backend-configurable.

## Implementation discipline

- Start every implementation branch from current `main`.
- Select the first incomplete applicable item in `LIVING_IMPLEMENTATION_PLAN.md`.
- Confirm no active branch owns the same product slice/files.
- Keep one branch limited to one coherent slice.
- Do not switch/reset/clean another active lane's checkout.
- Treat unknown local changes as protected concurrent work.
- Update the living plan only when real status or sequencing changes.
- Merge only after exact-head tests, applicable Product Runtime Acceptance, browser/desktop acceptance, and substantive review are clean.
- Delete the implementation branch after merge. Closed or historical branches are not future architecture authorities.

## UI/data rules

- UI state comes from backend read models.
- Do not hard-code producer truth, prices, signals, risk, balances, candidate IDs, validation outcomes, or model pricing.
- Historical research and live/current state remain explicitly scoped.
- Add-ons use typed registered extension slots and cannot inject arbitrary script/HTML or rewrite top-level navigation.

## Definition of complete

A slice is complete only when the intended user path works through the one development desktop, the canonical application/read-model/native-producer boundaries are preserved, durable truthful state returns to the correct surface, and exact-head acceptance/review is clean.
