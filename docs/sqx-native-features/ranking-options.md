# Ranking options

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/ranking-options/  
**SQX tab:** Builder › Full settings › Ranking  
**Kind:** `write`

Define Fitness (rank) computation and databank retention rules.

## From official docs

- Fitness (0–1) reflects strategy quality from backtest results.
- Configure Strategy Selection criteria, top-N databank capacity, and Custom conditions for acceptance/rejection.

## Integration

- **Typical artifact:** Builder project XML (Rankings / AcceptanceSettings)
- **Widget:** Fitness criteria; databank capacity; custom conditions table (`use`, `column`, `sampleType`, `comparator`, `threshold` from saved XML); Automatic filters gear opens `AutomaticDismissal` / `Problem` nodes
- **Screenshot:** [screenshots/ranking.png](screenshots/ranking.png)
