# Source inventory

The extraction contains 2,714 Java source files across the following
responsibility roots:

| Root | Files | Primary responsibility |
| --- | ---: | --- |
| `sources/engine-core` | 844 | backtesting, simulators, GP/evolution, optimization, WFO, robustness, results, orders, risk, data model |
| `sources/platform-runtime` | 223 | embedded shared runtime |
| `sources/launcher-app` | 7 | embedded launcher/application bootstrap |
| `sources/indicators-building-blocks` | 929 | indicators, conditions, blocks, exits, SL/PT, trading options, Monte Carlo, stats |
| `sources/data-lib` | 178 | data services and models |
| `sources/grid-lib` | 57 | grid/distributed execution |
| `sources/jobs-lib` | 8 | job contracts |
| `sources/plugin-api` | 17 | plugin contracts |
| `sources/web-gui-lib` | 25 | web GUI support |
| `sources/wizard-business` | 22 | wizard/business support |
| `sources/plugins` | 404 | 176 application, task, settings, cross-check, result, data, and service plugin roots |

The reference layer contains 9,747 files. Its largest groups are code
templates, extracted readable `internal.dat` entries, plugin assets, snippet
extensions, custom-indicator text/configuration, workflow exports, and readable
JAR resources. See [`extraction-report.md`](extraction-report.md) for the
provenance and exclusions.
