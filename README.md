# TraderCockpit

TraderCockpit is a desktop trading platform with one canonical application/runtime and one
development desktop. It makes StrategyQuant X (SQX) native historical-research capability
usable through one coherent, information-dense cockpit, and it is built to become a product
the owner can use daily and later license and sell.

## Product surfaces

`Home | Research | Explore | Automation | Operate | Settings`

### Canonical UI authority

The visual and product authority is the five-screen neon TraderCockpit prototype pinned in
[`references/ui-authority/`](references/ui-authority/) (`screenshots/*.png` byte-for-byte plus
`manifest.json`, `README.md`, and `DESKTOP_AGENT.md`; `previews/*.webp` are 720-px derivatives).
The pictures are the definitive structure of the one `web/` tree. Any UI-impacting change must
match their layout and tab rows; do not condense tabs, reintroduce a sparse placeholder shell, or
invent a new design.

| Screen | What the product implements |
| --- | --- |
| `cockpit-home` | Home chrome + density from the neon prototype. Product Home is the eight live/current zones `Market Overview · System Status · Alpha Stack · Pipeline Overview · Signals · Risk · Performance · Quick Actions` plus persistent Apollo. PNG card titles are illustrative framing, not the Home zone contract. |
| `order-flow-signals-models` | Research → Signals & Models: `Overview · Signals & Models · Order Flow · Footprint · Volume Profile · Liquidity Map · Replays · Alerts · Reports`; chart, Strategy Panel, Signal Pulse, Active Models, Confluence / Market State / Session Context / Risk Overlay / Assistant |
| `evolutionary_search_trading_dashboard` | Research → Evolutionary Search: state strip, Search Configuration, Population (islands), Generations, Pareto Frontier, Variation Operators, Fitness Evolution, Islands Overview, Archive & Objectives, Top Candidates, Deterministic Seed, exact configuration custody |
| `test-validate-dashboard` | Research → Test & Validate: `Overview · Initial Test · Trades · Robustness · Configuration · Evidence`; KPI strip, seven-stage Validation Funnel, Performance Overview, Return Distribution, stage cards, Run & Evidence Table, Validation Conclusions, Next Actions |
| `indicators-models-catalog` | Research → Indicators & Models: `All Components · Indicators · Models · Strategies · Utilities · My Components`; search/filters, categories rail, component table, detail panel |

Every surface shares the prototype chrome: rail (six surfaces, workspace / research-progress /
account cards), top bar (`Data Feeds · Broker · Compute · Automation` chips), market ticker, and
bottom status bar (`Live Runs · Positions · Daily P&L · Buying Power · Drawdown · Last Run`).

Truthful data rule: the full prototype layout is built, but real values render only where a read
model exists; everything else is a clearly styled "not connected / no data yet" state. No number,
symbol, price, score or grade is fabricated.

### Research workflow

The custody workflow

`Idea → Specification → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

is folded into the four Research workspaces: Idea and the exact native specification live in
Signals & Models; Build (compile → approve → launch the native Builder) and Candidates in
Evolutionary Search; Backtest, Trades, Robustness, Configuration and Proof are Test & Validate
tabs; the native block space, templates and imported strategies are the Indicators & Models
catalog. Routes are `/research?workspace=<id>&tab=<id>`; older `stage`/`tab` links canonicalise.

Construct modalities stay distinct:

- Random Discovery (native SQX Builder search);
- Genetic / Evolutionary search (native SQX GA — population, generations, islands, crossover/mutation, fresh blood and restart settings are read from the exact native `BuildMode`);
- Machine Learning / Models (platform-owned modality — decision trees, forests, gradient boosting, neural nets, and other standard-library models applied across indicators/strategies/assets, producing signals/features/models that feed the same Candidates → Backtest → Robustness → Proof custody; not connected yet).

StrategyQuant X / SQX is a native backend producer identity where technical
provenance/runtime/configuration requires it. It is not the platform name and not a workspace
label.

### Assistant (Apollo) and knowledge library

The Assistant card ("Your trading copilot", Apollo identity) appears on Home and in the Research
workspaces. It is a functional, bounded LLM surface governed by the consumer account/model
boundary: the backend `/api/assistant` transport calls OpenRouter with the operator credential
(`OPENROUTER_API_KEY`) and the backend model policy (default workhorse `z-ai/glm-5.3-flash`;
`TRADERCOCKPIT_ASSISTANT_MODEL`, `TRADERCOCKPIT_ASSISTANT_FALLBACK_MODELS`), grounded with a
secret-free read-model context and a curated Quant-Guild catalog
([Quant-Guild-Library](https://github.com/romanmichaelpaolucci/Quant-Guild-Library)) of
public lecture titles, source URLs, and platform-authored notes as anti-hallucination
reference data. Matching citations ride on `/api/assistant` replies. The widget is never disabled: readiness is reported truthfully
from `/api/status`, and an unconfigured provider answers with its exact `provider_not_configured`
state. It is an assistant surface only — never a product/result authority or a quantitative
engine, and distinct from the forbidden legacy "Apollo product spine".

### Cockpit validation verdict

StrategyQuant X produces the backtest and its exact native trade records; the cockpit computes
the verdict. Each completed Historical Result carries `cockpit_verdict`
(`tc.research-cockpit-verdict.v1`): SQX-formula statistics over the native trades, the exact
native Rankings / Higher Precision acceptance conditions for the first two funnel stages, the
documented cockpit policy (`TRADERCOCKPIT_VERDICT_POLICY` override) for Golden Validation,
Scenario Tests, Stress Tests (seeded trade-order/skip Monte Carlo) and Out-of-Sample, and Proof
custody for Evidence. Native columns the cockpit cannot recompute stay explicitly `unevaluated`.

## Canonical repository authority

Read in this order:

1. [`references/ui-authority/`](references/ui-authority/) — accepted visual/product authority.
2. [`docs/product-architecture-v1.md`](docs/product-architecture-v1.md) — product ownership and producer boundaries.
3. [`docs/product-backbone-spec-v1.md`](docs/product-backbone-spec-v1.md) — detailed UI/API/custody/security contract.
4. [`LIVING_IMPLEMENTATION_PLAN.md`](LIVING_IMPLEMENTATION_PLAN.md) — the single milestone roadmap and current status.
5. [`AGENTS.md`](AGENTS.md) — implementation and review discipline.

There are no competing planning documents. Historical recovery evidence lives under
`docs/recovery/` and is not a second authority. The Windows desktop agent's acceptance procedure
against the real installed StrategyQuant X runtime is
[`docs/windows-desktop-acceptance-runbook.md`](docs/windows-desktop-acceptance-runbook.md).

## Current backend state

The application server exposes a working research custody chain plus native SQX inspection.
Read models on `main` include:

- runtime/system status (`/api/status`);
- immutable Idea/source custody (`/api/research/ideas`);
- exact native configuration custody (`/api/research/configurations`);
- native Builder job custody/readback (`/api/research/native-jobs`);
- Candidate custody bound to exact native output (`/api/research/candidates`);
- native Retester historical results with native trades and the cockpit verdict (`/api/research/historical-results`);
- Proof/evidence (`/api/research/proofs`);
- native SQX preset/builder-config/output/project-topology inspection (`/api/sqx-*`);
- the bounded Assistant transport (`/api/assistant`, loopback only).

Native mutation runs only through the bounded trusted SQX gateway with fresh
runtime/launcher/configuration verification before every process. Live market/signal/risk/
performance producers and the Machine Learning modality are not yet connected and render
explicit unavailable states rather than fabricated values.

## Desktop

The desktop is a thin native window around the same canonical local server and `web/` UI. It
creates no second backend or second UI source tree.

Source/development launch:

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

The desktop private server is loopback-only, validates its exact Host, and rejects
cross-origin browser mutations.

### Windows packaged desktop

Windows uses pywebview with the `edgechromium` renderer, requiring Microsoft Edge WebView2
Runtime. Build from a Windows checkout with Python 3.12:

```powershell
python -m pip install -e ".[desktop,desktop-build]"
python tools/build_windows_desktop.py
```

Output: `dist/windows/TraderCockpit.exe`. Product Runtime Acceptance builds and launches this
frozen WebView2 desktop on `windows-latest` and publishes it as the `TraderCockpit-windows`
artifact. Real installed-SQX runtime verification happens on a Windows desktop where the
authorized SQX 144.2953 program is available.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

Product Runtime Acceptance additionally runs Chromium browser acceptance on Linux and
builds/launches the frozen WebView2 desktop on Windows.

## Development rule

Every implementation branch starts from current `main`, follows the current milestone in
`LIVING_IMPLEMENTATION_PLAN.md`, inspects `references/ui-authority/` before any UI change, and
is deleted after merge. A feature is complete only when its intended user path works in the
real desktop and visibly matches the accepted product authority — passing tests alone is not
completion.
