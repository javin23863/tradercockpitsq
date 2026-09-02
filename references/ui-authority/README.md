# TraderCockpit Prototype UI Authority

This directory records the canonical frontend prototype lineage that the SQX engine extraction must serve.

## Authority ruling

The multicolor ESQ TraderCockpit/Cursor-era prototype is the frontend product authority. It supersedes the earlier dark-blue `Chart / Backtest / Proof` shell as a product baseline.

The authority set is exactly these five recovered screens:

1. `cockpit-home.png` — Cockpit Home: market overview, engine/system status, Alpha Stack, pipeline overview, signals, risk, performance, quick actions, and Apollo.
2. `order-flow-signals-models.png` — strategy workspace: chart, Signals & Models, confluence, signal history, market state, and persistent Apollo composer.
3. `test-validate-dashboard.png` — validation workspace: Initial Test, Fast Pipeline, Golden Pipeline, scenario tests, OOS, stability/stress tests, costs, and validation funnel.
4. `evolutionary_search_trading_dashboard.png` — evolutionary search: population, generations, mutation, Pareto selection, deterministic seed, search budget, fitness evolution, Pareto frontier, MAP-Elites archive, islands, objectives, and candidate table.
5. `indicators-models-catalog.png` — Research / Indicators & Models catalog with capability/data requirements and strategy integration.

The full-resolution PNGs are committed byte-for-byte under [`screenshots/`](screenshots/); their exact bytes, dimensions, and SHA-256 digests are pinned in [`manifest.json`](manifest.json). Lightweight [`previews/`](previews/) copies exist for quick inline reference. The values shown inside the screens are illustrative product framing only — no number, symbol, price, score, or status in these images is a runtime source of truth. All live UI state comes from backend read models.

## Engine-facing consumer chain

These screens establish concrete product consumers for the backend flow:

```text
strategy construction / signals
        ↓
candidate generation
        ↓
evolutionary search
        ↓
initial backtest
        ↓
Fast robustness
        ↓
Golden robustness
        ↓
scenario / stress / OOS
        ↓
prop simulation
        ↓
evidence / monitoring
```

This is a consumer contract, not a claim that every backend stage is already complete. Engine work should recover and connect existing implementation first, then add only missing seams required by these consumers. Search/evolution scores are discovery signals, not validation results; validation and evidence remain distinct governed stages.

## SQX backend depth → product surface/tab mapping

Each authority screen is a presentation of native SQX (or platform-owned) capability. This table
maps the backend depth to the surface/tab that must expose it, so the depth is discoverable in the
product rather than buried behind routes. Values shown in the screens are illustrative; all live
state comes from the read models named here.

| Authority screen | Product surface / tab | Backend depth surfaced | Producer | Read model(s) |
| --- | --- | --- | --- | --- |
| `cockpit-home` | Home | Watchlist quotes, engine/system readiness, alpha stack, pipeline, signals, risk, performance, quick actions, Apollo | platform (orchestration) + SQX (research readiness) | `/api/status` (`tc.runtime-status.v1`), `/api/market/quotes` (`tc.market-quotes.v1`) |
| `order-flow-signals-models` | Research → Construct (Signals & Models) | Strategy panel (signals/models/rules), signal pulse/confluence, market/session state, risk overlay | SQX native authoring / blocks | research idea/configuration + native inspection read models |
| `evolutionary_search_trading_dashboard` | Research → Construct (Build, Evolutionary modality) | Population/islands, generations, variation operators, fitness evolution, Pareto frontier, deterministic seed/budget, top candidates | SQX Builder GA / search | native job + Builder config + candidate read models |
| `test-validate-dashboard` | Research → Backtest (Overview/Trades/Robustness/Configuration) + Proof | Validation funnel, Initial/Fast/Golden/scenario/stress/OOS stages, performance overview, return distribution, run & evidence table | SQX backtesting / robustness / Retester | historical result, trades, robustness, proof read models |
| `indicators-models-catalog` | Explore (Indicators & Models catalog) | Indicator families, model families (ML/Models modality), market-fit/timeframe/data requirements, per-component dependency/performance | SQX native blocks + platform-owned ML/Models | capability/catalog read models |

Live/current values (market quotes, account/broker, execution) stay explicitly scoped from
historical research and remain `unavailable` until their producers are connected. The live-market
provider seam is `tradercockpit.market_data.MarketDataProvider.fetch_quotes`, wired to
`/api/market/quotes`; the watchlist is operator configuration (`TRADERCOCKPIT_WATCHLIST`) and no
symbols, prices, or timestamps are hard-coded.

## Custody and verification

The five accepted screens were delivered by the owner on 2026-09-02 and committed directly to `screenshots/`. Verify integrity at any time by comparing the committed bytes against `manifest.json`:

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

Do not substitute screenshots from the earlier blue shell, regenerate lookalikes, or silently edit these baseline files. Improvements belong in a new, explicitly versioned prototype lineage while preserving this set as the historical product authority that informs the product framing.
