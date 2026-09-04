# Progress logs and charts

**Ingested:** 2026-09-04  
**Source:** https://strategyquant.com/doc/strategyquant/project-logs-performance-stats-and-charts/  
**SQX tab:** Builder › Progress  
**Kind:** `read-model`

When a project is running, the Progress screen shows performance characteristics and logs.

## From official docs

- Some charts are configurable; you choose what to display.

## Integration

- **Typical artifact:** Runtime logs; `engine` / `engine-channel` counts; `engineCharts` Chart.js or grid/rows items; `engine/getTypes` / `engine/saveSelection`
- **Widget:** Progress screen charts (two official slots with type pickers; stats/logs/charts patch live every 2s; Custom projects list rows patch running/percent from the catalog GET every 2s; Fitness Evolution stays a native popup)
- **Screenshot:** [screenshots/progress_charts.png](screenshots/progress_charts.png)
