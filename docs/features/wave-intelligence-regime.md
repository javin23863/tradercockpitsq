# Wave Intelligence (regime detection)

Subordinate implementation guide. Canonical: architecture §3 (platform
analytics over historical evidence), backbone §2 Signals / §3 Research.
Grounding: `references/quant-guild/excerpts/regime-wave.md`.

Authority screen: `order-flow-signals-models` (market-state / confluence
region). Lifecycle chips also appear on `cockpit-home` via the
lifecycle guide.

## 1. Purpose

Label the **historical backtest window of one strategy** with a small
set of regimes so the operator can see *when* that system was working.
This is "Wave Intelligence": regime detection for the current system
that ran in the historical backtest — not a live market-timing product.

Default methods (retail, CPU):

1. Gaussian HMM (2 or 3 states) — primary.
2. Kalman local-level trend/residual — companion feature.
3. Change-point on return variance — companion.

## 2. Ownership and scope

- Input series: native trade P&L and trade close times from
  `read_historical_trades`. Optional later: native equity curve if a
  producer record exists. **Do not** invent OHLC bars.
- Output scope: `historical_research`. Never copy the Viterbi path
  into Home Signals as live state.
- SQX still owns the backtest. The platform owns the regime labels
  as analytics-over-evidence.

## 3. Math (implement exactly)

### 3.1 Observation series

Build daily (or per-trade if fewer than 40 days) log-returns of
cumulative trade P&L:

```
V_0 = 0
V_t = V_{t-1} + PL_t
r_t = log(1 + PL_t / max(|V_{t-1}|, ε))   if a level exists
    = PL_t / scale                        otherwise
```

Require `n ≥ 60` observations. Else
`reason_code: regime_series_too_short`.

### 3.2 Gaussian HMM

States `k ∈ {0,..,K-1}`, `K ∈ {2,3}` (operator default 2).

```
S_t ~ Categorical(π),  S_t | S_{t-1} ~ A
r_t | S_t = k  ~  Normal(μ_k, σ_k²)
```

Fit EM (Baum–Welch). Decode `argmax P(S_{1:T} | r, θ)` (Viterbi).
Store `means`, `covars`, `transmat`, `state_path` (array of k),
`state_occupancy` (fraction of time in each k), `log_likelihood`.

Library: optional extra `hmmlearn`. If missing, a numpy EM of ≤150
lines is acceptable; if neither works,
`reason_code: regime_backend_not_installed`.

State names are **not** invented narratives. Expose `state_0`,
`state_1` plus `μ`, `σ`. The UI may map "higher μ" → "higher-drift"
as a derived label, never "bull market".

### 3.3 Kalman local-level

```
x_t = x_{t-1} + w_t,   w ~ N(0, q)
z_t = x_t + v_t,       v ~ N(0, r)
```

Initialize `x_0 = r_0`, `P_0 = 1`. `q`/`r` via simple EM or
fixed `q=1e-4`, `r=var(r_t)`. Report filtered `x_t` and residual
`z_t - x_t`. This is a feature, not a second regime taxonomy.

### 3.4 Change-points

CUSUM on `r_t²` (variance). Report at most 5 indices where the
statistic exceeds a documented threshold. If none,
`change_points: []` with `status: current` (empty is valid).

## 4. Read model and HTTP

Schema: `tc.research-wave-intelligence.v1`
Path: `GET /api/research/wave-intelligence`
Query: `historicalResultEntityId`,
`expectedHistoricalResultRevision`, optional `states=2|3`.

```json
{
  "schema": "tc.research-wave-intelligence.v1",
  "scope": "historical_research",
  "status": "current",
  "reason_code": null,
  "historical_result_entity_id": "...",
  "historical_result_revision": "...",
  "method": "gaussian_hmm",
  "n_observations": 120,
  "n_states": 2,
  "states": [
    {"id": "state_0", "mean": 0.001, "variance": 0.0004, "occupancy": 0.62}
  ],
  "state_path": [{"t": "2024-01-03", "state": "state_0"}],
  "kalman": {"status": "current", "q": 1e-4, "r": 0.01},
  "change_points": [],
  "detail": "Regime labels describe this Historical Result only."
}
```

Reason codes: `historical_result_missing`, `regime_series_too_short`,
`regime_backend_not_installed`, `regime_fit_failed`,
`states_invalid`.

POST is not required; derive-on-read. Optional later: persist a
custody record keyed by result revision so Home lifecycle can read
it without refitting.

## 5. UI

- Research Signals & Models / market-state panel
  (`order-flow-signals-models`): occupancy chips + a small state-path
  strip. Title: "Wave Intelligence (historical window)".
- Never draw a live candle chart from this record.
- Home lifecycle may show a one-line "Dominant regime: state_k (xx%)"
  with scope chip `historical`.

Empty: "Wave Intelligence needs a completed Historical Result with
at least 60 observations."

## 6. Tests

- Short series → `regime_series_too_short`, no fabricated path.
- Synthetic two-mean series (seeded) → two distinct `mean`s,
  occupancy sums to 1 ± 1e-6.
- `states=4` → 400 `states_invalid`.
- Missing result → 409/404 per existing identity mapping.
- UI: copy includes "historical" and not "live regime".

## 7. Coding instructions

1. New `product/tradercockpit/wave_intelligence.py`.
2. Build `r_t` only from `read_historical_trades`.
3. Prefer `hmmlearn`; fall back to numpy EM or fail closed.
4. Kalman and CUSUM in the same module (no new dependencies).
5. Wire GET in `app_server.py` (loopback).
6. `web/wave-intelligence.mjs` mounts on the Signals workspace
   market-state card and exposes `[data-wave-intelligence]`.
7. Do not add a top-level nav item.
8. Boundary check must stay green — no `eval`, no pickle of HMM.
   Persist JSON labels only if you persist at all.
