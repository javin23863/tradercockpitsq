# Model / strategy lifecycle and edge decay

Subordinate implementation guide. Canonical: backbone §2 Home (Alpha
Stack, Pipeline Overview, Performance). Grounding:
`references/quant-guild/excerpts/edge-decay-and-alpha.md`,
`backtest-discipline.md`, `performance-metrics.md`.

Authority screen: `cockpit-home`. Robustness numbers also render on
`test-validate-dashboard` (see `sqx-addons.md` — same math, one
implementation).

## 1. Purpose

Track **how the selected strategy and its Indicator Zoo members
behave through time** using the evidence we already have because the
strategy went through Construct → Backtest → Robustness → Proof:

- IS vs OOS decay (Edge Decay Analyzer four-pillar score + grade);
- optional forward/live residual only when a live producer exists
  (otherwise `unavailable`);
- a lifecycle strip on Home that does **not** invent an 8th-zone
  replacement.

This is the Home "model lifecycle" / "alpha decay" idea. It is
analytics over custody, not a new producer.

## 2. Reconcile with the eight Home zones

Do **not** add a ninth `data-home-zone`. Bind into existing zones:

| Zone | What this guide adds |
|------|----------------------|
| `alpha-stack` / candidate-review | Per-selected identity: Edge grade + IS→OOS PF decay for the strategy; Indicator Zoo top-importance members with the same decay when computable |
| `pipeline-overview` | Existing custody counts stay; add no numeric grades here |
| `performance` | May show the same record with `scope: historical_research` explicitly. Never merge with live P&L |
| `signals` | Must **not** show historical regime or decay as live signals |

If no Candidate / Proof / Historical Result is selected, the strip
is the truthful empty state already used by Alpha Stack.

## 3. Math (one implementation, two placements)

Implement once in `product/tradercockpit/edge_decay.py`. Consume from
Home and from Robustness (`sqx-addons.md`).

### 3.1 Inputs from native trades

Split trades by the Historical Result's recorded IS/OOS boundary
(configuration custody). If the split is missing:
`reason_code: is_oos_split_missing` and **no score**.

Compute, per side (IS, OOS, Full). **Expected value and Sharpe are
mandatory** — they are always present on the record (value or
explicit unavailable). The trader must be able to replicate both
from the published components (see
`references/quant-guild/excerpts/performance-metrics.md`).

- Expected value: `EV = p_win * avg_win + (1 - p_win) * avg_loss`
  which must equal `mean(PL)` within ε. Publish `n`, `n_win`,
  `p_win`, `avg_win`, `avg_loss`, `expected_value`.
- Sharpe: `mean(r) / sample_std(r)` (`ddof=1`) on per-trade P&L
  (risk-free = 0 unless a series is connected). Publish `sharpe`,
  `n`, `mean_return`, `stdev_return`. If `n < 2` or `stdev = 0`:
  field stays, `reason_code: sharpe_undefined`.
- Profit Factor, Net Profit, Avg Trade, Win Rate;
- Max DD % and Return/DD from the cumulative P&L path of *those*
  trades (not invented bars);
- Median XS = `median(MFE / max(MAE, ε))`.

### 3.2 Pillar scores

Linear interpolate each metric between documented anchors
(0 / 50 / 100). Default Strategy-mode anchors (document in code
constants; do not hide them):

| Metric | 0 | 50 | 100 |
|--------|---|----|-----|
| PF | 1.0 | 1.3 | 2.0 |
| Win rate | 0.40 | 0.50 | 0.60 |
| Return/DD | 0.5 | 1.5 | 3.0 |
| Max DD % (lower is better) | 40 | 20 | 8 |
| Sharpe | 0.0 | 0.8 | 1.6 |
| Median XS | 0.8 | 1.2 | 2.0 |
| PF decay IS→OOS (lower is better) | 0.50 | 0.20 | 0.05 |

Weights (Strategy mode): Profitability 0.25, Consistency 0.30,
Risk 0.25, Entry 0.20. Edge mode: Consistency 0.35, Entry 0.30,
Profitability 0.20, Risk 0.15.

```
score = 100 * Σ w_pillar * mean(normalized metrics in pillar)
grade = A if score≥85 else B if ≥70 else C if ≥55 else D if ≥40 else F
```

### 3.3 Indicator-level decay

For each Indicator Zoo member with `used_by_strategy: true`, the
first slice reports **the parent strategy's decay** and the member's
permutation importance — it does **not** invent a per-indicator P&L
unless a producer field isolates it. UI copy:
"Importance on this result · strategy edge grade is X".

Later slice (only if a native per-block contribution exists): compute
a leave-one-indicator residual. Until then, fail closed —
`reason_code: indicator_attribution_unavailable` — rather than
faking a decay line per RSI/MACD.

### 3.4 Forward / live decay

Only when Operate has a deployment + live trades producer. Until
then the `forward` block is:

```
{ "status": "unavailable", "reason_code": "live_execution_producer_missing" }
```

Do not fill it from the backtest tail.

## 4. Read model and HTTP

Schema: `tc.research-edge-decay.v1`
Path: `GET /api/research/edge-decay`
Query: `historicalResultEntityId`,
`expectedHistoricalResultRevision`, optional `mode=strategy|edge`.

```json
{
  "schema": "tc.research-edge-decay.v1",
  "scope": "historical_research",
  "status": "current",
  "reason_code": null,
  "mode": "strategy",
  "score": 71.4,
  "grade": "B",
  "pillars": {
    "profitability": 68.0,
    "consistency": 74.0,
    "risk": 70.0,
    "entry_quality": 73.0
  },
  "metrics": {
    "oos": {
      "n": 40,
      "n_win": 22,
      "p_win": 0.55,
      "avg_win": 120.0,
      "avg_loss": -80.0,
      "expected_value": 30.0,
      "sharpe": 0.9,
      "mean_return": 30.0,
      "stdev_return": 33.33,
      "profit_factor": 1.41
    },
    "is": {
      "n": 80,
      "expected_value": 36.0,
      "sharpe": 1.2,
      "profit_factor": 1.62
    },
    "decay": { "profit_factor": 0.13, "expected_value": 0.17, "sharpe": 0.25 }
  },
  "forward": {
    "status": "unavailable",
    "reason_code": "live_execution_producer_missing"
  },
  "detail": "Filter only; not a live-profit promise."
}
```

Home may embed a *pointer* to this record inside the Alpha Stack
payload later; first slice is the dedicated GET plus a Home binder
that fetches it when a current Historical Result / Proof identity
exists.

## 5. UI

Home `candidate-review` / Alpha Stack: after the existing candidate
rows, render `[data-edge-decay]` with **Expected value** and
**Sharpe** as always-visible cells (value or "unavailable ·
reason"), then grade chip, four pillar bars, PF/EV/Sharpe decay,
and the forward unavailable line. Copy must include "historical"
and must not show `$` fabricated P&L (use producer numbers only
when present). The trader must see the EV components (`p_win`,
`avg_win`, `avg_loss`) so they can replicate the arithmetic.

Robustness tab: same component, same API (no second math).

## 6. Tests

- Missing IS/OOS → `is_oos_split_missing`, no `score`; EV and Sharpe
  keys still present (unavailable or computable on Full).
- Seeded trades with known PF / EV / Sharpe → each within a
  documented epsilon of a hand-computed fixture; `|EV - mean(PL)|`
  ≤ ε.
- `n < 2` → `sharpe` field present with `sharpe_undefined`.
- Forward block always unavailable on a fresh data root.
- Home binder does not write live-looking numbers from this record
  into `[data-home-zone="signals"]`.
- No dollar literals in the decay panel unless they come from the
  record (and browser-regression money guard still passes).

## 7. Coding instructions

1. Implement `edge_decay_record(...)` in
   `product/tradercockpit/edge_decay.py` with the tables in §3.2 as
   module-level constants.
2. Reuse `read_historical_trades`. Split by configuration IS/OOS
   dates; if dates are absent, fail closed.
3. Wire GET. Loopback-only.
4. `web/edge-decay.mjs` used by Home Alpha Stack binder **and**
   Robustness. One renderer.
5. Do not persist a score that cannot be rebuilt from the same
   trades + constants (derive-on-read).
6. Document constants in the module docstring so another agent can
   retune them without hunting.
