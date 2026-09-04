# Indicators calibration

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/indicators-calibration/  
**SQX tab:** Builder › Building blocks  
**Kind:** `native-run`

Some indicator values depend on the market; calibration configures min, max, and step values for all indicators in one place.

## From official docs

- After calibration, min/max/step values reflect the useful range for the configured data settings.
- Calibration can be started with **Calibrate now** or automatically via **calibrate before start** in Building blocks.

## Integration

- **Typical artifact:** Project/builder XML; indicator min/max/step calibration cache
- **Widget:** Calibrate now; calibrate before start
- **Screenshot:** [screenshots/popup.png](screenshots/popup.png)
- **Native seam (144.2953):** Electron posts `indyTester/calibrate` with `projectName`, `taskName`, `symbols`, `timeframes`, `maxSteps`, `engine`. This desktop calls the same servlet (`POST /api/sqx-calibrate`) and applies returned min/max/step onto existing Block `indicatorMin`/`indicatorMax`/`indicatorStep` and existing `#Level#` Param ranges. It does not invent blocks, params, or ranges. Keep StrategyQuant X open.
