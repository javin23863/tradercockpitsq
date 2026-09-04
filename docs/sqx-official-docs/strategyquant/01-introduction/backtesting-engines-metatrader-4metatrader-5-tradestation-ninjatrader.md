# Backtesting engines – MetaTrader 4,MetaTrader 5, Tradestation

- Source: <https://strategyquant.com/doc/strategyquant/backtesting-engines-metatrader-4metatrader-5-tradestation-%c2%b7-ninjatrader/>
- Section: StrategyQuant X › Introduction
- Fetched: 2026-09-04

---

StrategyQuant X can export trading strategies to multiple trading platforms:

- **MetaTrader 4 / 5** – forex, CFDs
- **Tradestation** – futures, stocks, forex
- **MultiCharts** – futures, stocks, forex
- **PseudoCode** – human readable and understandable description of strategy logic

There are some differences between these platforms in how each of them handles trades, opens and closes positions, manages the open trades, etc. which can cause strategy to have very different results in one engine compared to the other. **Always develop strategies in the engine you’ll be trading it later!**

StrategyQuant allows you to switch the **backtesting engine** so that internal testing engine knows how to trade in a way that matches the selected platform.

Each trading platform and backtesting engine also allows you to configure some settings that are specific for a given platform.
