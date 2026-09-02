# Performance-metric grounding

Cites Quant-Guild lectures 34 (edge), 101 (Sharpe), 125 (CAGR),
129 (higher Sharpe).

## Mandatory trader-facing metrics

Every surface that presents a strategy or fitted-model result with
native trades **must** expose both of the following as first-class
fields the trader can read and replicate by hand. They are not
optional decorations and they are not hidden inside a composite
grade.

### Expected value (mandatory)

Per-trade expected value from the producer P&L sample:

```
p_win     = n_win / n
avg_win   = mean(PL | PL > 0)
avg_loss  = mean(PL | PL ≤ 0)          # ≤ 0 so scratches count as losses
EV        = p_win * avg_win + (1 - p_win) * avg_loss
          = mean(PL)                     # identity; publish both so the
                                         # trader can replicate the arithmetic
```

Publish `n`, `n_win`, `p_win`, `avg_win`, `avg_loss`, `expected_value`,
and the identity check `|EV - mean(PL)| ≤ ε`. Scope the window
(`is` / `oos` / `full`). If `n < 1`, the field remains present with
`status: unavailable`, `reason_code: trades_missing` — never omit
the key and never invent a value.

### Sharpe ratio (mandatory)

Per-trade Sharpe on the same P&L sample (excess over 0 unless a
risk-free series is actually connected):

```
r_i = PL_i / scale     # scale = 1 when PL is already a return;
                       # otherwise document the scale (e.g. account)
SR  = mean(r) / s      # s = sample standard deviation, ddof=1
```

Publish `sharpe`, `n`, `mean_return`, `stdev_return`, `ddof`,
`risk_free` (null if unused), and window scope. If `n < 2` or
`s = 0`, keep the field with `status: unavailable` and
`reason_code: sharpe_undefined` — never hide Sharpe and never
substitute 0.

Lecture 101: Sharpe assumes iid returns and is inflated by
selection. Always pair the raw SR with `n` and window. A deflated
Sharpe (Bailey et al.) is *additional* when the strategy was
selected from a search; it does not replace the raw SR the trader
must see.

```
DSR = (SR - SR_0) / sqrt(Var(SR))
```

If trial count `N` is unknown: `deflated_sharpe.status =
selection_count_unknown`. Do not derive a letter grade from DSR
in that case. Still show raw Sharpe.

## Other claims the product must honor

1. **CAGR hides path.** Lecture 125: CAGR = `(V_T / V_0)^{1/Y} - 1`.
   Pair it with max drawdown, EV, and Sharpe. Never show CAGR alone
   as "the" performance of a Candidate.

2. **You do not "get a higher Sharpe" by choosing a prettier window.**
   Lecture 129 is about process (costs, sizing, non-stationarity), not
   about picking the IS window that maximizes SR. The product must use
   the IS/OOS split recorded on the Historical Result, not a UI-chosen
   window.

## Retail-runnable method

Compute EV / Sharpe / CAGR / PF / DD only from native trade P&L
already in custody (`read_historical_trades`). Do not reconstruct
equity from assumed bar data. If MAE/MFE are missing, entry-quality
(XS) is `unavailable`, not zero. EV and Sharpe stay mandatory even
when XS is unavailable.
