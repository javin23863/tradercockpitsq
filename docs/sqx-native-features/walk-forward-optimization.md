# Walk-Forward Optimization

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/walk-forward-optimization/  
**SQX tab:** Optimizer / Cross checks  
**Kind:** `native-run`

Walk-Forward Optimization periodically reoptimizes strategy parameters with out-of-sample segments.

## From official docs

- Optimization finds parameter values (periods, constants) that performed best historically.
- Walk-Forward tests strategy results with periodic reoptimization (e.g. every 300 days).

## Integration

- **Typical artifact:** WFO result columns; WFO equity samples
- **Widget:** Walk-Forward Optimization
