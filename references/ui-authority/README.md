# TraderCockpit Prototype UI Authority

This directory holds the accepted frontend prototype: five screens of the neon TraderCockpit
desktop (near-black canvas, violet primary, colour-coded numbered cards, dense data tables, ring
gauges, market ticker, bottom status bar). It is the definitive structure for `web/`; the docs,
`web/model.mjs`, and the acceptance tests follow these pictures.

## Authority ruling

The five screens under [`screenshots/`](screenshots/) are the product UI authority. They supersede
both the earlier dark-blue `Chart / Backtest / Proof` shell and the earlier "ESQ" multicolor
mockups. Do not condense their tabs, invent a new visual direction, or re-introduce a sparse
placeholder shell.

| Screen | Product surface | Structure the product implements |
| --- | --- | --- |
| `cockpit-home.png` | Home | Neon chrome (rail, ticker, status bar) and dense Home board. Product Home implements the eight live/current zones in `manifest.json` `consumer_surface`: Market Overview · System Status · Alpha Stack · Pipeline Overview · Signals · Risk · Performance · Quick Actions, plus persistent Apollo. Card titles/values in the PNG are illustrative framing, not runtime truth and not the Home zone contract. |
| `order-flow-signals-models.png` | Research → Signals & Models | Tabs `Overview · Signals & Models · Order Flow · Footprint · Volume Profile · Liquidity Map · Replays · Alerts · Reports`; chart with toolbar/tools; Strategy Panel (Signals / Models / Rules), Signal Pulse, Active Models; Confluence · Market State · Session Context · Risk Overlay · Assistant |
| `evolutionary_search_trading_dashboard.png` | Research → Evolutionary Search | Strip `State · Objective Set · Optimization · Search Mode · Deterministic Seed · Budget · Time Elapsed · Pause/Stop`; Search Configuration, Population (islands), Generations, Pareto Frontier, Variation Operators, Fitness Evolution, Islands Overview, Archive & Objectives, Top Candidates, Deterministic Seed |
| `test-validate-dashboard.png` | Research → Test & Validate | KPI strip (Total Runs · Pass Rate · Avg. Sharpe · Out-of-Sample Sharpe · Max Drawdown · Expectancy · Profit Factor); Validation Funnel; Performance Overview; Return Distribution; seven stage cards `Initial Test · Fast Validation · Golden Validation · Scenario Tests · Stress Tests · Out-of-Sample · Evidence`; Run & Evidence Table; Validation Conclusions; Next Actions |
| `indicators-models-catalog.png` | Research → Indicators & Models | Pills `All Components · Indicators · Models · Strategies · Utilities · My Components`; search + Category/Market Fit/Timeframe/Data Type/Status filters; Categories/Market fit/Timeframe rail; component table; component detail panel with attributes, market fit, timeframe, dependencies, tags, performance, actions |

Global chrome on every screen: left rail (brand, `Home · Research · Explore · Automation · Operate ·
Settings`, workspace card, progress card, plan card, version line), top bar (workspace chip, `Data
Feeds · Broker · Compute · Automation` chips, search, notifications), market ticker (one cell per
watchlist symbol plus market-state cell), and the bottom status bar (`Live Runs · Positions · Daily
P&L · Buying Power · Drawdown · Last Run · View`).

The values shown inside the screens are illustrative product framing only — no number, symbol,
price, score, grade, or status in these images is a runtime source of truth. The product renders
real values only where a backend read model exists and explicit "not connected / no data yet"
states everywhere else.

## Integrity

The full-resolution PNGs are committed byte-for-byte under [`screenshots/`](screenshots/); their
exact bytes, dimensions, and SHA-256 digests are pinned in [`manifest.json`](manifest.json). The
[`previews/`](previews/) WebP files are 720-px derivatives of those PNGs for quick inline reference
and are not byte-level authority. Verify integrity at any time:

```sh
python3 - <<'PY'
import hashlib, json, pathlib
root = pathlib.Path("references/ui-authority")
manifest = json.loads((root / "manifest.json").read_text())
for asset in manifest["canonical_assets"]:
    data = (root.parent.parent / asset["repository_path"]).read_bytes()
    ok = len(data) == asset["bytes"] and hashlib.sha256(data).hexdigest() == asset["sha256"]
    print(("ok  " if ok else "FAIL"), asset["name"])
PY
```

## Consumer chain

The screens establish this product flow for the native StrategyQuant X research producer:

```text
idea / strategy specification (Signals & Models)
        ↓
native Builder search — Random Discovery or Genetic Evolution (Evolutionary Search)
        ↓
candidate import from exact native Results archives (Top Candidates / Candidate Review)
        ↓
Initial Test — native Retester (Test & Validate)
        ↓
Fast / Golden / scenario / stress / out-of-sample — native cross-checks as they connect
        ↓
Evidence — immutable Research Proof
        ↓
prop simulation / delivery (Operate)
```

This is a consumer contract, not a claim that every stage has a producer today. Search/evolution
scores are discovery signals, not validation results; TraderCockpit never computes a pass/fail
verdict.

## SQX backend depth → surface/tab mapping

| Authority screen | Surface / tab | Backend depth surfaced today | Producer | Read model(s) |
| --- | --- | --- | --- | --- |
| `cockpit-home` | Home | Custody counts and latest records (ideas, configurations, native jobs, candidates, historical results, proofs), runtime/system health, watchlist quotes, market state | platform orchestration + SQX runtime readiness | `/api/status`, `/api/market/quotes`, `/api/research/*` |
| `order-flow-signals-models` | Research → Signals & Models | Idea custody (Overview); exact native Builder specification — strategy shape, market identity, data setup, blocks, rankings, cross-checks, money management, native search mode (Signals & Models); enabled native signal blocks (Strategy Panel); live analytics frames await a market-data provider | SQX native Builder task (read-only) | `/api/research/ideas`, `/api/sqx-builder-config` |
| `evolutionary_search_trading_dashboard` | Research → Evolutionary Search | Native `BuildMode` GA parameters (population, generations, islands, migration, crossover/mutation, fresh blood, restart), native `Rankings` fitness/acceptance conditions/stop condition, exact configuration compile → approve → launch, native job custody, Candidate import | SQX Builder GA / search | `/api/sqx-builder-config`, `/api/research/configurations`, `/api/research/native-jobs`, `/api/sqx-outputs`, `/api/research/candidates` |
| `test-validate-dashboard` | Research → Test & Validate | Native Retester runs (Initial Test), native trade rows (Trades), Higher Precision robustness (Fast Validation), native `CrossChecks` enable flags for the other stages, executed configuration chain, Research Proof (Evidence) | SQX Retester / cross-checks | `/api/research/historical-results`, `/api/research/proofs`, `/api/sqx-builder-config` |
| `indicators-models-catalog` | Research → Indicators & Models | 536 native building blocks with category/enabled/weight/parameter attributes, native templates, imported native strategies, ideas; Models = platform-owned ML modality (not connected) | SQX native blocks + platform-owned ML/Models | `/api/sqx-builder-config`, `/api/sqx-presets`, `/api/research/*` |

Live/current values (market quotes, account/broker, execution, P&L) stay explicitly scoped from
historical research and remain `unavailable` until their producers are connected. The live-market
provider seam is `tradercockpit.market_data.MarketDataProvider.fetch_quotes` behind
`/api/market/quotes`; the watchlist is operator configuration (`TRADERCOCKPIT_WATCHLIST`) and no
symbols, prices, or timestamps are hard-coded.

Do not substitute screenshots from other lineages, regenerate lookalikes, or silently edit these
baseline files. Improvements belong in a new, explicitly versioned prototype lineage.
