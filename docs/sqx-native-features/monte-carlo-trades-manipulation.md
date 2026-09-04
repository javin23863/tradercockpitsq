# Monte Carlo trades manipulation

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/monte-carlo-trades-manipulation/  
**SQX tab:** Builder › Cross checks › MC trades  
**Kind:** `native-run`

Monte Carlo simulations that manipulate existing trades (shuffle, miss some, etc.).

## From official docs

- Fast: works on trades from the main backtest without rerunning it.
- Verifies dependence of equity on trade order and impact of missing trades.

## Integration

- **Typical artifact:** Cross-check config; MC sample columns from existing trades
- **Widget:** MC trades manipulation cross check
