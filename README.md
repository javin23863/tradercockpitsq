# TraderCockpit

TraderCockpit is a desktop trading platform with one canonical application/runtime and one
development desktop. It makes StrategyQuant X (SQX) native historical-research capability
usable through one coherent, information-dense cockpit, and it is built to become a product
the owner can use daily and later license and sell.

## Product surfaces

`Home | Research | Explore | Automation | Operate | Settings`

### Canonical UI authority

The visual and product authority is the recovered multicolor "ESQ TraderCockpit" prototype,
pinned in [`references/ui-authority/`](references/ui-authority/) (five accepted screens plus
`manifest.json`, `README.md`, and `DESKTOP_AGENT.md`). It supersedes the earlier dark-blue
`Chart / Backtest / Proof` shell. Any UI-impacting change MUST first inspect
`references/ui-authority/previews/*.webp` and match that visual grammar. Do not invent a new
design or reintroduce the dark-blue shell.

The five accepted screens are:

1. `cockpit-home` — Cockpit Home: Market Overview, Engine & System Status, System Alerts, Resource Usage, Alpha Stack, Pipeline Overview, Signal Feed, Risk Overview, Performance Overview, Quick Actions, and the persistent Apollo assistant.
2. `order-flow-signals-models` — Strategy workspace: chart, Signals & Models, confluence, signal history, market state, and the Apollo composer.
3. `evolutionary_search_trading_dashboard` — Evolutionary Search: population, generations, mutation, Pareto front, deterministic seed/budget, MAP-Elites archive, islands, objectives, and candidate table.
4. `test-validate-dashboard` — Test & Validate: Initial Test, Fast/Golden pipelines, scenario/OOS/stability/stress, costs, and the validation funnel.
5. `indicators-models-catalog` — Research: Indicators & Models catalog (technical indicators and Machine Learning / model families) with capability/data requirements and strategy integration.

### Research workflow

Research is the historical strategy-research workspace. Its accepted workflow is:

`Idea → Construct → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

Construct supports distinct problem-solving modalities:

- Random Discovery (native SQX Builder search);
- Genetic / Evolutionary search (native SQX GA);
- Machine Learning / Models (platform-owned modality — decision trees, forests, gradient boosting, neural nets, and other standard-library models applied across indicators/strategies/assets, producing signals/features/models that feed the same Candidates → Backtest → Robustness → Proof custody).

StrategyQuant X / SQX is a native backend producer identity where technical
provenance/runtime/configuration requires it. It is not the platform name and not a workspace
label.

### Apollo assistant and knowledge library

Apollo is the persistent in-product assistant shown across the accepted screens. It is a
bounded LLM surface governed by the consumer account/model boundary (default workhorse
`z-ai/glm-5.3-flash`, backend-configurable). It is grounded against a curated quant knowledge
library ([Quant-Guild-Library](https://github.com/romanmichaelpaolucci/Quant-Guild-Library))
used as anti-hallucination reference data for both Apollo answers and development agents.
Apollo is an assistant surface only — it is never a product/result authority or a quantitative
engine. (This is distinct from, and must not become, the forbidden legacy "Apollo product
spine".)

## Canonical repository authority

Read in this order:

1. [`references/ui-authority/`](references/ui-authority/) — accepted visual/product authority.
2. [`docs/product-architecture-v1.md`](docs/product-architecture-v1.md) — product ownership and producer boundaries.
3. [`docs/product-backbone-spec-v1.md`](docs/product-backbone-spec-v1.md) — detailed UI/API/custody/security contract.
4. [`LIVING_IMPLEMENTATION_PLAN.md`](LIVING_IMPLEMENTATION_PLAN.md) — the single milestone roadmap and current status.
5. [`AGENTS.md`](AGENTS.md) — implementation and review discipline.

There are no competing planning documents. Historical recovery evidence lives under
`docs/recovery/` and is not a second authority.

## Current backend state

The application server exposes a working research custody chain plus native SQX inspection.
Read models on `main` include:

- runtime/system status (`/api/status`);
- immutable Idea/source custody (`/api/research/ideas`);
- exact native configuration custody (`/api/research/configurations`);
- native Builder job custody/readback (`/api/research/native-jobs`);
- Candidate custody bound to exact native output (`/api/research/candidates`);
- native Retester historical results (`/api/research/historical-results`);
- Proof/evidence (`/api/research/proofs`);
- native SQX preset/builder-config/output/project-topology inspection (`/api/sqx-*`).

Native mutation runs only through the bounded trusted SQX gateway with fresh
runtime/launcher/configuration verification before every process. Live market/signal/risk/
performance producers, the Machine Learning modality, and the Apollo LLM gateway are not yet
connected and render explicit unavailable states rather than fabricated values.

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
