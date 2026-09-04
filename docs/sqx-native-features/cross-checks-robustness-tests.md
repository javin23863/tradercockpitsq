# Cross checks settings

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/cross-checks-robustness-tests/  
**SQX tab:** Builder › Full settings › Cross checks  
**Kind:** `write`

Cross check tests are additional tests applied to a strategy to test robustness.

## From official docs

- Robustness testing verifies behavior under small changes in inputs, history data, or other strategy components.

## Integration

- **Typical artifact:** Builder project XML (CrossChecks / AcceptanceSettings)
- **Widget:** Cross-check enable/config sliders and filters; Settings / Filters gears render each method's saved `Settings` and `AcceptanceSettings` children (including BASIC-tier WhatIf / MonteCarloManipulation / MonteCarlo / RetestWithHigherPrecision when present)
- **Screenshot:** [screenshots/cross_checks.png](screenshots/cross_checks.png)
