# Regime / Wave Intelligence grounding

Cites Quant-Guild lectures 49 (Markov chains), 51 (HMM), 71 (Markov
property), 72/74 (regime-switching bot), 92 (Kalman), 93 (non-stationarity),
95 (Kalman mean-reversion).

## Claims the product must honor

1. **Markets are non-stationary.** Lecture 93: a single global mean/variance
   is not a valid assumption for a multi-year backtest. Wave Intelligence
   exists to label *regimes on the historical window of one strategy*, not
   to invent a live market-timing oracle.

2. **The Markov property is an assumption, not a fact.** Lecture 71: a
   process is Markov if `P(X_{t+1}|X_t,...,X_0) = P(X_{t+1}|X_t)`. HMM
   (lecture 51) relaxes this by introducing a hidden state `S_t` such that
   observations `X_t` are conditionally independent given `S_t`, and `S_t`
   itself is Markov.

3. **Gaussian HMM is the default retail regime model.** Lecture 51:
   observations ~ Normal(`μ_k`, `σ_k²`) in hidden state `k`. Fit with EM
   (Baum–Welch). Decode the most likely state path with Viterbi. Two or
   three states (risk-on / risk-off, or bull / bear / chop) is enough for
   a retail backtest window. Do not invent a 7-state "Wave Intelligence"
   taxonomy with no evidence.

4. **Kalman is a state estimator, not a classifier.** Lectures 92 and 95:
   the linear Gaussian filter

   ```
   x_t = F x_{t-1} + w_t,   w ~ N(0, Q)
   z_t = H x_t + v_t,       v ~ N(0, R)
   ```

   is used to extract a latent trend / mean-reversion level from a
   price or spread series. Wave Intelligence may expose the Kalman
   filtered level and residual as features of the *same historical
   window*; it does not replace HMM.

5. **A regime label is historical evidence, never a live signal** unless
   a live-market producer is connected and the same model is re-run on
   live bars. Mixing a backtest Viterbi path into Home Signals as if it
   were current is forbidden.

## Retail-runnable method

- HMM: `hmmlearn.hmm.GaussianHMM` (optional extra) or a small
  numpy EM implementation if hmmlearn is absent — then
  `reason_code: regime_backend_not_installed` is allowed.
- Kalman: a 20-line numpy filter (constant-velocity or local-level).
  No control-systems package required.
- Change-point: binary segmentation on returns variance (PELT-lite or
  a simple CUSUM). Fail closed if the series is shorter than 60 bars.

## Canonical papers

- Hamilton (1989), "A New Approach to the Economic Analysis of
  Nonstationary Time Series and the Business Cycle" — Markov switching.
- Rabiner (1989), "A tutorial on hidden Markov models".
- Kalman (1960), "A New Approach to Linear Filtering and Prediction
  Problems".
- Bai & Perron (1998/2003) — multiple structural breaks.
