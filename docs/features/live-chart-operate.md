# Live chart and proven-strategy signals (Operate)

Subordinate implementation guide. Canonical: backbone §2 Market
Overview / Signals, architecture live-market provider seam.
Grounding: `references/quant-guild/excerpts/backtest-discipline.md`
(do not mix historical and live).

Authority screen: `order-flow-signals-models` (the chart + signal
history + market state). Home Signals zone consumes the same live
read model; it does **not** get its own chart.

## 1. Purpose

When a strategy has made it through Proof and has a Promotion →
Export → Deployment identity, the operator needs a **live chart**
of the attached market plus the live signals of *that* deployed
model — the thing the prototype screen shows and the current
desktop does not.

Until a live-market provider **and** a live signal/execution
producer exist, the surface is a truthful placeholder, not a
replay of the backtest.

## 2. Preconditions (all required)

A live chart row is `status: current` only when **all** of:

1. `GET /api/market/quotes` (or a future bars endpoint) has
   `status: current` for the attached symbol;
2. a Deployment custody record exists for the selected identity
   (`operate_deployments` — on later branches; on `main` this
   authority is not implemented → fail closed);
3. a live signal producer is bound (the architecture's Signals
   zone producer). Historical entries/exits are not this producer.

If any precondition fails, render the authority chart *frame* with
an explicit overlay:

```
Live chart unavailable · {reason_code}
Historical trades remain in Research / Test & Validate.
```

Never draw synthetic candles, never replay `orders.bin` as live
bars, never attach Wave Intelligence's historical state_path as a
live overlay.

## 3. Read models

### 3.1 Bars (new; same seam family as quotes)

Schema: `tc.market-bars.v1`
Path: `GET /api/market/bars`
Query: `symbol` (must be on the operator watchlist),
`timeframe` ∈ registered `{M1,M5,M15,H1,H4,D1}`.

```json
{
  "schema": "tc.market-bars.v1",
  "scope": "live_current",
  "historical_fallback": false,
  "status": "unavailable",
  "reason_code": "provider_not_configured",
  "provider": null,
  "symbol": "ES",
  "timeframe": "M5",
  "bars": [],
  "provider_hookup": {
    "interface": "tradercockpit.market_data.MarketDataProvider.fetch_bars",
    "watchlist_env": "TRADERCOCKPIT_WATCHLIST"
  }
}
```

Extend `MarketDataProvider` with `fetch_bars(symbol, timeframe, limit)`
that returns `(open, high, low, close, volume, observed_at)` or
raises. No provider → empty `bars`, never mocked.

### 3.2 Live signals for a deployment

Schema: `tc.operate-live-signals.v1`
Path: `GET /api/operate/signals`
Query: `deploymentEntityId` (registered identity only).

Until a producer exists:

```json
{
  "schema": "tc.operate-live-signals.v1",
  "scope": "live_current",
  "status": "unavailable",
  "reason_code": "signal_producer_not_connected",
  "deployment_entity_id": "...",
  "signals": [],
  "detail": "Live signals require a connected deployment producer. Historical entries are in Research."
}
```

When connected, each signal: `id`, `observed_at`, `side`,
`state`, `producer`. No fabricated confluence score.

### 3.3 Attachment

The chart is attached to the **deployment's** instrument (from
export/deployment custody), not to a UI-typed symbol. If the
deployment has no instrument, `reason_code: deployment_instrument_missing`.

## 4. UI

Operate surface + the Signals & Models workspace chart pane.

- Canvas / SVG chart reads **only** `bars[]` from `tc.market-bars.v1`.
- Overlay markers read **only** `signals[]` from
  `tc.operate-live-signals.v1`.
- Indicator Zoo / Wave Intelligence overlays, if shown, carry a
  visible `historical` chip and use a separate series keyed from
  `/api/research/*`. They must not be blended into `bars[]`.
- Home Signals zone: latest live signal row or the unavailable
  reason — no chart.
- If a HUD shows the attached strategy's research stats, **Expected
  value** and **Sharpe** are mandatory and must come from
  `trade_metrics` / edge-decay — never from fabricated live P&L.

Authority grammar: dark cockpit chart from
`order-flow-signals-models`. Empty overlay is part of the design.

## 5. Tests

- No provider → `bars: []`, `provider_not_configured`.
- Symbol not on watchlist → 400.
- Signals GET without deployment producer →
  `signal_producer_not_connected`, empty list.
- Browser test: `/operate` and Signals workspace contain
  "not connected" / reason code, and
  `doesNotMatch(/\$\s?\d/)` still holds.
- A fixture provider that returns two bars must render exactly
  those two — no interpolated third bar.

## 6. Coding instructions

1. Add `fetch_bars` to the `MarketDataProvider` protocol in
   `product/tradercockpit/market_data.py` (or a sibling
   `market_bars.py` if you need to keep quotes file small).
2. Wire `/api/market/bars` next to `/api/market/quotes`.
3. Add `operate_live_signals.py` that returns the unavailable
   record until a producer protocol is bound (mirror quotes).
4. Frontend: `web/live-chart.mjs` used by Operate and the
   Signals workspace. Pass `deploymentEntityId` from route
   state only when it is a registered identity.
5. Do not implement a platform backtester to "simulate live".
6. Promotion/export/deployment APIs may still be unavailable on
   `main`; the chart must tolerate that with
   `deployment_authority_not_connected`.
