# Edge, alpha, and decay grounding

Cites Quant-Guild lectures 34 (edge), 46 (luck vs skill), 77 (profitable
vs tradable), 96 (finding alpha), 125 (CAGR).

## Claims the product must honor

1. **Edge is measured against a real baseline, not 50%.** Lecture 34:
   random timing already has a win rate set by drift, exits, and costs.
   A signal has edge only if its win rate (or expectancy) beats that
   *measured* random baseline. This is the WinRateEdge method.

2. **Alpha is not a backtest profit.** Lecture 96: a positive historical
   P&L is not alpha. Alpha requires a stated benchmark, a stated window,
   and a residual that survives costs. The product may compute a
   research-scoped residual against a declared benchmark series; it must
   label it `scope: historical_research` and must not show it as live
   alpha.

3. **Profitable ≠ tradable.** Lecture 77: a strategy can be profitable
   on paper and untradable live (capacity, costs, regime break,
   psychological streaks). Edge Decay Analyzer's four pillars
   (profitability, consistency, risk, entry quality) plus IS→OOS decay
   exist to surface that gap. They are a filter, not a live-profit
   promise.

4. **Luck vs skill needs a distribution.** Lecture 46: a single Sharpe or
   win rate is not skill. Report a Z-score against the random (or
   resampled) baseline, and a multiple-testing-aware deflated Sharpe
   when many candidates were searched.

5. **CAGR / Sharpe are not oracles.** Lectures 125 and 101: CAGR hides
   path; Sharpe assumes iid returns and is inflated by selection. Always
   pair a headline metric with sample size, IS/OOS split, and a decay
   or deflated figure.

## Edge Decay Analyzer math (platform analytics over SQX evidence)

Score ∈ [0, 100] = weighted sum of four pillars. Default *Strategy* weights:

| Pillar          | Weight | Inputs (OOS unless noted)                          |
|-----------------|--------|----------------------------------------------------|
| Profitability   | 0.25   | Profit Factor, Net Profit, Avg Trade               |
| Consistency     | 0.30   | Stability, PF decay IS→OOS, Win Rate               |
| Risk            | 0.25   | Return/DD, Max DD %, Sharpe                        |
| Entry quality   | 0.20   | Median XS = MFE / max(MAE, ε)                      |

Each metric uses linear interpolation between documented 0 / 50 / 100
anchors. Grade: A ≥ 85, B ≥ 70, C ≥ 55, D ≥ 40, else F.

*Edge* mode (raw signal, no SL/TP) reweights toward Consistency and
Entry Quality and relaxes drawdown anchors.

IS→OOS decay for a metric `m`:

```
decay_m = (m_IS - m_OOS) / max(|m_IS|, ε)
```

Positive decay means the metric got worse OOS. The consistency pillar
must include `decay_PF`. If the Historical Result has no IS/OOS split,
the record is `status: unavailable`, `reason_code: is_oos_split_missing`.

## WinRateEdge math

Let `p_s` be the signal win rate, `p_r` the pooled random-entry win
rate, `n` the signal trade count.

```
z = (p_s - p_r) / sqrt(p_r (1 - p_r) / n)
```

Report `z`, a one-sided p-value, and the conventional thresholds
1.645 (95%) and 3.09 (99.9%). The random baseline **must** be measured
from producer-backed random-entry trades (or an explicit
`reason_code: random_baseline_missing`). Do not assume `p_r = 0.5`.
