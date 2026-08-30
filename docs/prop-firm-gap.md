# Prop-firm simulation finding

The requested archive was searched across engine classes, plugin roots,
building blocks, configuration, and readable resources. It contains no
standalone plugin or package named `PropFirm`, `Funding`, `Challenge`, or
`TrailingDrawdown`.

That does not mean the risk mechanics are absent. The source tree includes the
primitive surfaces needed to build a prop-firm policy layer:

- account balance/equity and trade-result calculations in the backtest,
  result, and simulator packages
- drawdown, loss, daily-limit, and account-stat columns under
  `sources/indicators-building-blocks/SQ/Columns/`
- time/session, max-trades-per-day, SL/PT, and related controls under
  `sources/indicators-building-blocks/SQ/TradingOptions/`
- risk and money-management blocks under `SQ/RiskManagement` and
  `SQ/MoneyManagement`
- What-If and robustness manipulation/cross-check plugins under
  `sources/plugins/CrossCheckWhatIf/`, `CrossCheckMonteCarloManipulation/`,
  and `CrossCheckMonteCarloRetest/`
- simulator and backtest result paths under
  `sources/engine-core/com/strategyquant/tradinglib/`

The correct next implementation for the new backend is a clearly named
prop-firm policy adapter that composes these primitives and adds explicit
rules for daily loss, total drawdown, trailing drawdown, profit targets,
consistency, trading-day requirements, and breach state. That adapter is not
present in the old package and therefore is not falsely represented here as
SQX-derived parity code.
