# Agent Execution Policy

This repository has one product line and one implementation plan.

## Read before editing

1. `references/ui-authority/` — the five accepted prototype screens (open `screenshots/*.png` or `previews/*.webp` before any UI-impacting change).
2. `docs/product-architecture-v1.md`
3. `docs/product-backbone-spec-v1.md`
4. `LIVING_IMPLEMENTATION_PLAN.md`

Do not create a competing roadmap, checklist, recovery plan, donor plan, or architecture override. Historical recovery evidence under `docs/recovery/` is not a second authority.

The pictures win: the five screens are the definitive structure of `web/`. UI-impacting work must
match their layout and tab rows; do not condense tabs, reintroduce a sparse placeholder shell, or
invent a new visual direction without an explicit product-authority change.

## Product identity

This is a new desktop trading platform.

Top-level surfaces are:

`Home | Research | Explore | Automation | Operate | Settings`

Global chrome on every surface: left rail (brand, six-surface navigation, workspace / research
progress / account cards), top bar (workspace chip, `Data Feeds | Broker | Compute | Automation`
readiness chips, search, notifications), market ticker (one cell per watchlist symbol plus a
market-state cell), and the bottom status bar (`Live Runs | Positions | Daily P&L | Buying Power |
Drawdown | Last Run`).

Home is the Cockpit Home board from the `cockpit-home` screen: hero ("Turn Research into
Decisions that Compound." with the `Research → Build → Validate → Simulate → Deploy` workflow and
New Research / Build Strategy actions), Recent Activity, and exactly these eight numbered cards:

`Research | Build & Backtest | Prop Firm Simulation | Proof & Evidence | Active Builds | Candidate Review | System Health | Assistant`

Research is the historical strategy-research surface composed of four workspaces, one per
prototype screen, each with its exact tab row:

- `Signals & Models` — `Overview | Signals & Models | Order Flow | Footprint | Volume Profile | Liquidity Map | Replays | Alerts | Reports`
- `Evolutionary Search` — single dashboard (state strip, configuration, population/islands, generations, Pareto frontier, variation operators, fitness evolution, islands overview, archive & objectives, top candidates, deterministic seed/budget)
- `Test & Validate` — `Overview | Initial Test | Trades | Robustness | Configuration | Evidence`, with the seven-stage funnel `Initial Test | Fast Validation | Golden Validation | Scenario Tests | Stress Tests | Out-of-Sample | Evidence`
- `Indicators & Models` — `All Components | Indicators | Models | Strategies | Utilities | My Components`

The custody chain `Idea → Specification → Build → Candidates → Backtest → Robustness → Proof →
Delivery / Simulation` is folded into those workspaces (Idea = Signals & Models / Overview;
Specification = Signals & Models / Signals & Models; Build + Candidates = Evolutionary Search;
Backtest/Robustness/Configuration/Proof = Test & Validate tabs). Routes are
`/research?workspace=<id>&tab=<id>`; pre-prototype `stage`/`tab` links canonicalise to them.

Construct modalities stay distinct: Random Discovery and Genetic/Evolutionary search (both native
SQX, shown from the exact native `BuildMode`), and Machine Learning / Models (platform-owned, see
Producer ownership; shown as not connected until its backend exists).

StrategyQuant X / SQX is a native historical-research backend producer identity where technical provenance, runtime, or configuration requires it. It is not the platform name and not a user-facing workspace label.

## Producer ownership

Native SQX owns the quantitative behavior proven to belong to it, including native strategy authoring/AlgoWizard, Builder generation/search and GA behavior, historical backtesting, ranking/filter calculations, robustness/cross-checks, Retester, optimization/Walk-Forward, Custom Project execution, and native strategy/result artifacts.

The platform owns application mechanics: desktop lifecycle, Home/live presentation, accounts/auth, bounded model access, exact native configuration custody/approval, runtime verification/control/readback, product identities, Candidate Lab, Backtest/Proof presentation, Automation presentation, and durable evidence.

A missing integration seam does not transfer quantitative authority to TraderCockpit. Connect to native SQX or report the capability unavailable; do not create a substitute quantitative engine.

### Machine Learning / Models modality (platform-owned)

The Machine Learning / Models research modality is a platform-owned capability, distinct from
SQX's owned semantics. It uses standard, well-known ML libraries (e.g. scikit-learn, gradient
boosting, neural nets) to fit models across indicators/strategies/assets and produce
signals/features/models. It is NOT a reimplementation of SQX Builder/GA/backtester/robustness:
its outputs flow into the same Candidates → Backtest → Robustness → Proof custody, where
historical evaluation and robustness remain owned by the native SQX producer wherever SQX owns
that behavior. The production-boundary rule against a "substitute quantitative engine" refers
to duplicating SQX's Builder/GA/backtest/robustness/optimizer/Custom-Project execution — not to
this explicitly-scoped ML modality. Model math should be grounded against the curated quant
knowledge library rather than invented.

### Assistant (Apollo, bounded)

The Assistant card ("Your trading copilot", Apollo identity) appears on Home and in the Research
workspaces as the prototype shows. It is a bounded LLM surface under the consumer account/model
boundary (default `z-ai/glm-5.3-flash`, backend-configurable), grounded against the curated
Quant-Guild knowledge library for anti-hallucination. Until model access and the account
authority exist it renders truthful unavailable state with `Ask Assistant` disabled. The
assistant never owns producer truth, never becomes a result/quantitative authority, and never
mutates native state directly. It is explicitly distinct from the forbidden legacy "persistent
Apollo product spine" (a prohibited second product/result architecture); do not build that.

## Executable-native authority

When the authorized installed SQX runtime is available, **the running program is the primary executable specification for integration behavior**.

- Inspect the real SQX UI, saved projects/configurations, runtime files, process behavior, and result artifacts directly.
- Run bounded scenarios in the installed program to determine how a native capability is configured, persisted, launched, and read back.
- User-supplied screenshots and previously captured real-runtime scenarios are valid supporting observations and should be reused rather than ignored or re-proved from scratch.
- Retained/decompiled source, archived references, and static fixtures are secondary aids for details that are not directly observable. They may explain or harden an integration, but they are not a prerequisite gate when the installed runtime can answer the question directly.
- Do not create a separate “evidence gate”, “producer-evidence checkpoint”, or retained-byte-identity prerequisite before implementation when the required behavior can be observed by running SQX.
- Do not require a live installed project/configuration to equal one archived reference blob in order to be considered native-valid. Validate runtime/build identity, structure, custody, and the actual producer behavior instead.
- Runtime execution/observation belongs inside the implementation and acceptance loop for the slice. Do not implement against static evidence and postpone the real producer run as a separate future checkpoint when the runtime is available.
- Source inspection remains appropriate when exact non-observable serialization/API details are needed, but it must not replace an available direct producer test.

## Identity is not validity

Keep these three authorities separate in production code, read models, tests, and documentation:

- **Runtime trust** authorizes an SQX installation to execute through verified build/runtime markers and the configured trusted launcher boundary.
- **Artifact custody** records the exact configuration, project, engine, result, and launcher bytes/hashes used by an operation.
- **Producer validity** comes from required native structure plus acceptance/production by the authorized SQX runtime at the real native seam.

A digest may prove which bytes were inspected or executed. It must not make one archived Git blob the only valid mutable user configuration. A compiled-in artifact digest may authorize execution only when an independently documented security trust policy—not retained evidence alone—requires that exact artifact. Production-boundary tests must reject archived Builder identity predicates and compiled-in Retester engine allowlists in the Research execution path.

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
- For native integrations, inspect/run the installed producer as part of the slice whenever it is available; do not defer that producer exercise into a separate evidence checkpoint.
- Merge only after exact-head tests, applicable Product Runtime Acceptance, browser/desktop acceptance, and substantive review are clean.
- Delete the implementation branch after merge. Closed or historical branches are not future architecture authorities.

## UI/data rules

- UI state comes from backend read models.
- Build the full prototype layout, but render real values only where a read model exists; everywhere else render a clearly styled "not connected / no data yet" state. Charts render axes and frames; series appear only from a read model.
- Do not hard-code producer truth, prices, symbols, signals, risk, balances, scores, grades, candidate IDs, validation outcomes, or model pricing.
- Native SQX depth is shown from the exact native task with native tag names visible; presentation labels never assign quantitative semantics or verdicts.
- Historical research and live/current state remain explicitly scoped.
- One `web/` tree of vanilla ES modules; no framework or build system.
- Add-ons use typed registered extension slots and cannot inject arbitrary script/HTML or rewrite top-level navigation.

## Definition of complete

A slice is complete only when the intended user path works through the one development desktop, the canonical application/read-model/native-producer boundaries are preserved, durable truthful state returns to the correct surface, and exact-head acceptance/review is clean. For a native SQX slice, completion includes exercising the actual installed producer when that runtime is available; static retained evidence is not a substitute and is not a separate prerequisite gate.
