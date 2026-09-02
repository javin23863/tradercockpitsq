# Supervised ML grounding

Cites Quant-Guild lectures 17 (PCA), 44 (time series), 63 (neural nets),
plus the evaluation lectures in `backtest-discipline.md`.

## Claims the product must honor

1. **Train / validate / test are distinct.** Lecture 63 shows the
   train/val/test split as a first-class diagram. A model that only reports
   in-sample `estimator.score(X_train, y_train)` is not a completed research
   model. The product must report an out-of-sample score (or purged/embargoed
   CV) and must fail closed when the split cannot be formed.

2. **Features must be contemporaneous.** Lecture 44 (time-series analysis)
   requires that a feature at time `t` is constructed only from information
   available at `t`. MAE/MFE/Duration of a *completed* trade are
   post-outcome descriptors. They may be used to *explain* a finished
   Historical Result; they must not be sold as a live predictive feature
   without an explicit `scope: historical_explanatory` flag.

3. **Neural nets are optional and must not be default.** Lecture 63 treats
   neural nets as a capacity-heavy method that needs more data and a
   validation split. Retail default families are linear/tree ensembles
   (logistic regression, decision tree, random forest, gradient boosting).
   A neural-net family is allowed only when sklearn is present, a validation
   split exists, and `reason_code` is not `ml_data_insufficient`.

4. **PCA is a structure tool, not a signal.** Lecture 17 uses PCA on return
   series to find common factors. Indicator Zoo uses the same idea on a
   per-strategy indicator matrix: report explained-variance ratios and
   loadings. Do not treat a principal component as a tradable indicator
   unless the operator explicitly promotes it through custody.

## Retail-runnable method

- Library: scikit-learn (`sklearn>=1.3,<2`), CPU only.
- Allowlisted estimators: `LogisticRegression`, `DecisionTreeClassifier`,
  `RandomForestClassifier`, `GradientBoostingClassifier`. Optional later:
  `MLPClassifier` with a small hidden layer, only when trade count ≥ 200.
- No TensorFlow / PyTorch in the first implementation.

## Canonical papers (not Quant-Guild, but required)

- López de Prado, *Advances in Financial Machine Learning*, Ch. 7 (purged
  k-fold) and Ch. 14 (deflated Sharpe / multiple testing).
- Bailey, Borwein, López de Prado, Zhu — Deflated Sharpe Ratio.
