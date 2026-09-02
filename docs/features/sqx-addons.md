# SQX plugins as typed platform add-ons

Subordinate implementation guide. Canonical: architecture §10,
backbone §10. Grounding: `references/quant-guild/excerpts/` as cited
per add-on. These eight StrategyQuant *Results / authoring* plugins
are incorporated as **typed extension slots**, not as a second UI
spine and not as copied Vue plugin folders.

Authority screens:

- `test-validate-dashboard` — Edge Decay, WinRateEdge, RunCompare
- `cockpit-home` — Prop Firm Simulation (Delivery / Simulation)
- `evolutionary_search_trading_dashboard` + Construct — sqx-lab
- Delivery / Operate export — Source Code Translator

## 1. Consolidation (no redundancy)

| Plugin (source) | Product placement | Shared implementation |
|-----------------|-------------------|------------------------|
| Edge Decay Analyzer | Research Backtest **Robustness** + Home lifecycle | `edge_decay.py` — one math, two views ([lifecycle guide](model-strategy-lifecycle-edge-decay.md)) |
| WinRateEdge (Strategy vs Random) | Research Backtest **Robustness** | `winrate_edge.py` next to edge decay |
| LucidFlex Prop Evaluator | Delivery **Prop Firm Simulation** | `prop_simulation.py` rule-set `lucidflex` |
| 2-Step Challenge Analyzer (FTMO) | Delivery **Prop Firm Simulation** | same module, rule-set `ftmo_2step` |
| RunCompare | Research Backtest **Robustness** / optimization history | `run_compare.py` |
| Source Code Translator | Delivery / Export | `source_translate.py` (bounded LLM) |
| Strategy Templates Skills | Construct authoring assistant | `sqx_lab_authoring.py` (layer: template) |
| SQX Authoring Toolkit (sqx-lab) | Construct authoring assistant | same module (layers: block, group, template, project) |

Do **not** ship eight parallel Results tabs. Do **not** vendor the
upstream `index.html` / Vue plugin files into `web/`.

## 2. Typed slot contract

Schema: `tc.addon-descriptor.v1`
Registry: `GET /api/extensions` → `tc.addon-catalog.v1`

```json
{
  "schema": "tc.addon-catalog.v1",
  "status": "current",
  "addons": [
    {
      "id": "edge-decay",
      "version": "1",
      "owning_producer": "platform_analytics",
      "evidence_producer": "sqx_native_trades",
      "placement": ["research.backtest.robustness", "home.alpha-stack"],
      "read_schema": "tc.research-edge-decay.v1",
      "action_schema": null,
      "availability": { "status": "ready" }
    }
  ]
}
```

Rules (architecture §10):

- no arbitrary JS/HTML injection;
- no competing frontend catalog;
- no new top-level nav;
- no rewrite of Research core stages;
- unknown `version` / unknown `placement` → fail closed
  (`reason_code: addon_descriptor_unknown`);
- `owning_producer` is `platform_analytics` or `platform_authoring`
  or `platform_export`. Native SQX remains `evidence_producer`.

Implementation: `product/tradercockpit/extensions.py` (missing on
`main`). Built-in descriptors are code constants; later third-party
slots load from `{data_root}/addons/*.json` with the same schema
and a deny-list for `script` / `html` presentation fields.

Frontend: one slot renderer per `placement` string. The renderer
asks the backend for the descriptor + the read model. It never
`eval`s descriptor text.

## 3. Mandatory trader metrics (every analytics add-on)

Wherever an add-on presents a strategy or run that has native
trades, **Expected value** and **Sharpe ratio** are mandatory
visible fields (value or explicit unavailable). Formulas:
`references/quant-guild/excerpts/performance-metrics.md`.

The trader must be able to replicate EV from `p_win`, `avg_win`,
`avg_loss` and Sharpe from `mean_return`, `stdev_return`, `n`.
Do not hide either inside a composite score.

## 4. Edge Decay Analyzer → Robustness + Home

See [model-strategy-lifecycle-edge-decay.md](model-strategy-lifecycle-edge-decay.md).
Improvements over the community plugin:

- fail closed without IS/OOS instead of inventing a score;
- EV and Sharpe always published, not only used inside the Risk
  pillar;
- concurrency warning if native settings allowed duplicate trades
  (plugin idea, keep);
- no Vue/localStorage; settings are backend config
  (`mode=strategy|edge`, asset preset as documented constants).

## 5. WinRateEdge → Robustness

Schema: `tc.research-winrate-edge.v1`
Path: `GET /api/research/winrate-edge`
Query: `historicalResultEntityId`,
`expectedHistoricalResultRevision`.

```
z = (p_s - p_r) / sqrt(p_r (1 - p_r) / n_s)
```

`p_s` from the selected result's trades. `p_r` from a measured
random-entry baseline bound to the same symbol/date/exit rules
(native RAND block / Retester portfolio). If that baseline is
absent: `reason_code: random_baseline_missing` — **do not** assume
`p_r = 0.5`.

Also publish EV and Sharpe for the signal sample (mandatory).

UI: Robustness method card "Entry vs random" with Z, p-value,
thresholds 1.645 / 3.09, and the missing-baseline state.

Tests: missing baseline fails closed; seeded binomial fixture
reproduces `z` within ε; extra query keys 400.

## 6. Prop Firm Simulation (LucidFlex + FTMO)

One module, two **rule-set documents** — not two UIs.

Schema: `tc.delivery-prop-simulation.v1`
Path: `GET/POST /api/delivery/prop-simulation`

POST body (exact keys):

```json
{
  "action": "evaluate",
  "historical_result_entity_id": "...",
  "expected_historical_result_revision": "...",
  "rule_set": "lucidflex" ,
  "account_type": "evaluation",
  "account_size": 50000,
  "size_multiplier": 1.0
}
```

`rule_set` ∈ `{lucidflex, ftmo_2step}`. Constants live at the top
of `prop_simulation.py` and must match the published firm rules
cited in the plugin pages (update only with a documented revision).

Shared engine:

1. Group native trades into US-Eastern trading days
   (DST-aware; `zoneinfo.ZoneInfo("America/New_York")`).
2. Walk end-of-day balance; estimate worst-case **intraday** equity
   as `close - MAE` per trade (plugin method). A green day can
   still breach.
3. Apply the rule-set's profit target, daily loss, max loss /
   trailing MLL, consistency, min days.
4. Verdict: `pass` | `almost` | `fail` with the exact rule and
   date. `almost` is LucidFlex-evaluation consistency only.
5. Optional Monte Carlo (start-day windows or day-shuffle that
   preserves losing-streak lengths as documented per rule-set).
   Require ≥ 40 trading days; else
   `reason_code: prop_mc_history_too_short` and skip MC without
   inventing a pass probability.
6. Always publish EV and Sharpe of the sized trade stream.

LucidFlex-specific: trailing MLL locks at start + $100; funded
mode drops target/consistency; size multiplier scales P&L and
contracts. FTMO-specific: Phase 1 +10% / Phase 2 +5%, daily −5%,
overall −10%, min 4 days; static vs trailing floor.

Improvements over the plugins: one engine; MAE-intraday is
explicitly an estimate (`intraday_estimator: "mae"`); fail closed
when MAE is missing (`reason_code: mae_missing`, do not pretend
EOD-only is "real intraday").

UI: Delivery / Simulation on Home (`prop-simulation` card) and
Test & Validate funnel. No second "FTMO" top-level item.

## 7. RunCompare

Schema: `tc.research-run-compare.v1`
Path: `GET /api/research/run-compare?strategyName=...`
POST `action=record` after a completed Historical Result
(idempotent on `historical_result_revision`).

Each row: result identity, timestamp, EV, Sharpe (mandatory),
PF, CAGR, Max DD, Win Rate, MAR if computable, operator `note`
(plain text, escaped), composite score
`mean(rank-normalized {MAR, Sharpe, CAGR, -MaxDD, WinRate, PF})`.

Organize by strategy name from custody, not from UI typing.
Persist with `atomic_write_json`.

Improvement: composite score weights are backend constants, not
localStorage; no fabricated prior runs.

## 8. Source Code Translator (Delivery / Export)

Schema: `tc.delivery-source-translate.v1`
Path: `POST /api/delivery/translate`

This is **export assistance**, not a native SQX generator
replacement. Input is the exact promoted/exported strategy
pseudo-code or approved XML already in custody — never a browser
filesystem path.

Rules:

- provider key stays server-side (existing account/model boundary);
- hardcoded provider host; size-capped body;
- output is escaped text; UI is a `<pre>` / copy button;
- no screenshot-upload loop in v1 (the community plugin's
  drag-and-drop error-fix is a later extra and must treat images
  as untrusted);
- target language ∈ a registered enum (the 15 platforms listed
  on the plugin page);
- spend uses the membership-funded OpenRouter allowance;
- fail closed when the assistant transport is
  `provider_not_configured`.

Do not send native archive bytes that are not already in the
export custody record.

## 9. sqx-lab authoring assistant (Construct)

The four skills (custom block → random group → strategy template →
build project) plus the standalone Strategy Templates skill become
**one** Construct assistant flow:

```
plain-English thesis
  → design spec (filter + trigger + falsifiable why)
  → operator approve
  → emit native artifact bytes
  → operator import / Build in SQX (the oracle)
```

Platform never executes Builder/GA. It may:

- read the install-derived catalog of blocks/groups the way
  existing preset/block inspection already does;
- emit XML / `.sqx` / `.cfx` **only** from proven skeletons
  already in the authorized runtime (transplant holes; do not
  hand-write SQX XML from scratch);
- store the artifact as Idea/source or configuration *draft*
  custody pending review/approval.

`/sqx-setup` and `/sqx-doctor` semantics become Settings / native
runtime health — they must not become a second desktop.

If the install catalog is missing: `reason_code:
native_catalog_unavailable`. Never invent a block the install
does not have.

LLM assistance is the bounded Apollo/account path. Treat any
generated XML as untrusted until native import/Build accepts it.

## 10. Tests (registry + each add-on)

- Catalog lists only registered ids; unknown placement ignored.
- Descriptor with `presentation.html` is rejected by the loader.
- Edge decay / WinRateEdge / prop / run-compare each assert
  `expected_value` and `sharpe` keys exist.
- Prop: missing MAE → `mae_missing`, no PASS.
- WinRateEdge: no `p_r = 0.5` default.
- Translator: extra path field → 400; no key in the read model.
- Authoring: emitted artifact is stored as draft; Build is not
  invoked by the HTTP handler.

## 11. Coding instructions

1. Add `product/tradercockpit/extensions.py` with
   `addon_catalog_record()` and the built-in descriptor tuple.
2. Implement `edge_decay.py` first (lifecycle guide). Robustness
   and Home both call it.
3. Add `winrate_edge.py`, `prop_simulation.py`, `run_compare.py`,
   `source_translate.py`, `sqx_lab_authoring.py` as separate
   modules — one placement each, shared EV/Sharpe helper
   `product/tradercockpit/trade_metrics.py`
   (`expected_value_record`, `sharpe_record`).
4. Wire routes in `app_server.py`. Loopback-only mutations.
5. Frontend slot mounts: Robustness card list, Home
   prop-simulation / alpha-stack, Delivery export panel,
   Construct assistant panel. No new top-level nav.
6. Do not copy plugin zips into the repo. Cite the URLs in this
   file as provenance only.
7. Boundary: no `eval`, no `pickle`, no `shell=True`, no
   `references` import.

## 12. Provenance URLs (not runtime dependencies)

- https://strategyquant.com/codebase/edge-decay-analyzer/
- https://strategyquant.com/codebase/strategy-vs-random-edge-testing-winrateedge-results-panel/
- https://strategyquant.com/codebase/lucidflex-futures-evaluator/
- https://strategyquant.com/codebase/the-2-step-challenge-analyzer-for-ftmo-know-if-your-strategy-will-pass-before-you-pay/
- https://strategyquant.com/codebase/how-runcompare-simplifies-optimization/
- https://strategyquant.com/codebase/source-code-translator/
- https://strategyquant.com/codebase/strategy-templates-skills/
- https://strategyquant.com/codebase/sqx-authoring-toolkit-sqx-lab-four-claude-code-skills-that-author-your-blocks-groups-templates-and-projects/
