# Backtest discipline grounding

Cites Quant-Guild lectures 77, 93, 97.

## Claims the product must honor

1. **Three pitfalls (lecture 97) that ruin a strategy:**
   - Survivorship bias — testing only names that still exist.
   - Look-ahead / leakage — using information not available at `t`.
   - Overfitting / multiple testing — searching many configs and
     reporting the winner's in-sample score as if it were a single test.

2. **Non-stationarity (lecture 93) means a backtest window is one
   regime-path, not the future.** Wave Intelligence and Edge Decay exist
   to *describe* that path. They do not license fabricating a live
   regime or a live decay number.

3. **Profitable vs tradable (lecture 97 + 77):** costs, fills, and
   concurrency (`Allow Duplicate Trades`) change the result. Edge Decay
   Analyzer's concurrency warning is required: if the edge depends on
   overlapping trades the native settings allowed, say so.

## Product rules (non-negotiable)

- Every ML / zoo / regime / decay record declares `scope`:
  `historical_research` | `historical_explanatory` | `live_current` |
  `simulated`.
- Purged / embargoed CV (López de Prado) is required for supervised fit
  scoring. Embargo at least one average trade duration on each side of
  the test fold.
- Multiple-testing correction (deflated Sharpe or Bonferroni on the
  family of fits) is required when more than one family is fit on the
  same Historical Result.
- Missing IS/OOS, missing trades, or missing native configuration →
  fail closed. Never interpolate a decay score.
