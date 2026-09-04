# Builder module

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/builder/  
**SQX tab:** Builder  
**Kind:** `native-run`

Builder is the core of the program, where new trading strategies are built.

## From official docs

- Before starting build or retest, data and settings should be configured; results are stored in the databank.
- Every module (Builder, Retester, Optimizer, Custom projects) switches between **Progress**, **Full settings**, and **Results**.
- Progress: Start/Pause/Stop, logs, performance and memory charts, settings overview.
- Full settings: all module settings. Results: metrics for the databank-selected strategy.

## Integration

- **Typical artifact:** Builder project XML; databank strategies
- **Widget:** Progress / Full settings / Results tabs; Start/Pause/Stop
