# Feature implementation guides (subordinate)

These files are **implementation instructions** for later coding slices.
They are not a second architecture, roadmap, or product authority.

Read first, in this order:

1. `docs/product-architecture-v1.md`
2. `docs/product-backbone-spec-v1.md`
3. `LIVING_IMPLEMENTATION_PLAN.md`
4. the guide for the slice you are implementing
5. `references/quant-guild/` for the math that guide cites

If a guide and a canonical document disagree, the canonical document
wins and the guide must be corrected.

| Guide | Surface | Living-plan section |
|-------|---------|---------------------|
| [ml-models.md](ml-models.md) | Research Construct / Models | Research vertical |
| [indicator-zoo.md](indicator-zoo.md) | Indicators & Models catalog | Research vertical |
| [wave-intelligence-regime.md](wave-intelligence-regime.md) | Signals/Order-Flow + lifecycle | Research vertical |
| [model-strategy-lifecycle-edge-decay.md](model-strategy-lifecycle-edge-decay.md) | Home Alpha Stack / lifecycle | Home live/current |
| [live-chart-operate.md](live-chart-operate.md) | Operate live chart + Signals | Home + Operate |
| [sqx-addons.md](sqx-addons.md) | typed add-on slots | Capability/add-on backbone |

Authority screens (`references/ui-authority/`):

- `indicators-models-catalog` — ML families, Indicator Zoo
- `order-flow-signals-models` — Wave Intelligence / regime + live chart
- `test-validate-dashboard` — Robustness analytics (edge decay, WinRateEdge, RunCompare)
- `cockpit-home` — model/strategy lifecycle + decay
- `evolutionary_search_trading_dashboard` — Construct authoring (sqx-lab)

Baseline code (do not rebuild): the sklearn ML catalog/fit path already
exists on branch `cursor/ml-models-e2e-5d85` (error handling on
`cursor/ml-models-fit-error-handling-5d85`). Rebase or cherry-pick that
work onto current `main` as the first ML implementation commit.

**Mandatory trader metrics:** any surface that shows a strategy or
fitted model with native trades must publish **Expected value** and
**Sharpe ratio** (value or explicit unavailable). The trader must be
able to replicate EV from `p_win`, `avg_win`, `avg_loss` and Sharpe
from `mean_return`, `stdev_return`, `n`. Shared helper specified as
`product/tradercockpit/trade_metrics.py` in `sqx-addons.md`.
Formulas: `references/quant-guild/excerpts/performance-metrics.md`.
