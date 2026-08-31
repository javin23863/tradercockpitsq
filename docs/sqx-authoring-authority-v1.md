# SQX Authoring Authority v1

## Status

This document is a binding amendment to the native-SQX product spine while PR #35 is being finalized.

For strategy authoring and assistant behavior, this document **supersedes every Apollo-specific requirement** currently present in `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, and `docs/product-backbone-spec-v1.md` until those documents are consolidated.

## Product decision

**StrategyQuant X is the strategy-intelligence and authoring authority. Apollo is deferred and must not be imported into the repaired product spine.**

TraderCockpit will not build, import, or merge a separate persistent assistant, strategy language, or strategy-generation engine to reproduce capabilities owned by SQX.

The native product hierarchy is:

```text
user idea / strategy request
    -> native SQX AI Wizard / AI Assistant and AlgoWizard when authoring from an idea
    -> native SQX Builder when searching/generating candidates
    -> native SQX backtest / validation / robustness / optimization
    -> real SQX strategies/results
    -> TraderCockpit custody / Candidate Lab / Backtest / Proof / presentation
```

TraderCockpit remains the desktop/application, configuration/custody, approval, launch/control, readback and presentation layer. Any backend bridge exists only to invoke or observe native SQX capabilities; it is not a second reasoning or strategy-generation layer.

## Proven native AI authority

StrategyQuant's current product documentation for the retained 144 generation establishes an integrated SQ AI / AI Wizard capability that accepts natural-language strategy ideas, creates or improves strategies, and hands them into AlgoWizard for backtesting and further editing. That is the primary AI-assisted authoring capability for the repaired product.

This changes the earlier evidence wording: the existence of native SQX AI-assisted authoring is **PROVEN**. What remains **OPEN EVIDENCE** is the exact supported programmable seam, if any, through which TraderCockpit can invoke that native AI capability without automating or reimplementing it.

Do not confuse those two questions. Lack of a proven programmatic endpoint does not make `sqx-lab`, Apollo, or a TraderCockpit LLM the core authoring engine.

## MCP boundary

The retained SQX 144.2953 source contains the first-party `ServletMCP` plugin. It is an MCP server for AI tool integration and registers SQX's MCP endpoint, but the retained `MCPTools.createSpecs()` implementation is narrow.

At this retained build it exposes these six tools:

- `list_projects`
- `list_databanks`
- `list_strategies`
- `get_strategy_stats`
- `run_project`
- `stop_project`

Those tools delegate to SQX's native `Project` program API. They provide useful native inspection/control, but they do **not** expose AlgoWizard strategy construction, Builder configuration authoring, block/group/template authoring, candidate generation primitives, or a general LLM endpoint.

Therefore:

- MCP is a first-party SQX integration surface and should be used where its proven tools fit.
- MCP is **not** currently the strategy-authoring engine.
- Do not invent MCP authoring methods that the retained source does not expose.
- Re-audit MCP when a newer SQX build adds tools; capability discovery should widen from evidence rather than assumptions.

## `sqx-lab` boundary

The repository also contains the `sqx-lab` authoring plugin on branch:

- branch: `codex/sqx-lab-plugin`
- reviewed source head at this decision: `dad4b0bae2b7556ec15870dca96df057e1e91b84`
- plugin root: `plugins/sqx-lab/`

`sqx-lab` is an **optional SQX authoring extension for the contemporary external-LLM/custom-idea workflow**, not the foundation intelligence layer.

It is install-derived and authors native StrategyQuant X / AlgoWizard artifacts through the chain:

```text
custom block -> random group -> strategy template -> build project
```

Its useful capabilities include:

- authoring SQX custom condition/price-level blocks;
- authoring random groups sampled by Builder;
- emitting validated `.sqx` strategy templates;
- cloning a real installed SQX project and wiring templates into native `project.cfx` Build tasks;
- deriving catalogs from the actual SQX installation rather than inventing project/template identities;
- validating generated artifacts before import and leaving final acceptance to the real SQX installation.

This is useful when a user wants to introduce custom building blocks, groups, templates or project structures, especially through current external LLM tooling. It is materially different from PR #25's TraderCockpit-owned `tradercockpit.builder-strategy.v1` producer because it authors **native SQX artifacts** and leaves Builder/backtest/GA semantics with SQX.

However, `sqx-lab` must not be promoted into the product's core LLM, universal authoring gateway, or replacement for native SQ AI / AlgoWizard. Use it only where its additional native-artifact authoring capability is actually needed.

## UI decision

There is **no persistent Apollo dock** in the repaired foundation product.

Construct / Idea may expose the native SQX idea-authoring capability through the smallest supported bridge that executable evidence permits. The UI must make clear when the action is native SQX AI/AlgoWizard authoring versus an optional `sqx-lab` custom-artifact operation.

The surface may:

- accept a plain-language strategy request for native SQX AI/AlgoWizard authoring when a supported invocation seam is proven;
- expose native MCP project/strategy inspection and run/stop controls where useful;
- invoke `sqx-lab` for explicit custom block/group/template/project authoring needs;
- show proposed native artifacts/configuration and a human-readable diff where artifacts are generated;
- require explicit approval before destructive or expensive native Builder/validation execution.

It may not:

- route every idea through `sqx-lab` merely because it is callable from an external LLM;
- treat MCP as an authoring API when the retained MCP tool registry does not provide authoring tools;
- fabricate an SQX block/template/project not present in or valid for the target install;
- convert a request into `tradercockpit.builder-strategy.v1` or another competing executable strategy language;
- substitute TraderCockpit-generated candidates/results when SQX authoring or execution fails.

## Foundation vertical correction

The foundation proof is a native-SQX path, not an Apollo or `sqx-lab` proof:

```text
idea / bounded strategy request
  -> native SQX authoring capability
  -> native AlgoWizard / Builder configuration and approval
  -> native Builder when candidate search is required
  -> real .sqx survivor
  -> Candidate Lab
  -> native validation/retest
  -> Backtest
  -> Proof
  -> restart/reopen same identities
```

The first implementation slice should prove the **smallest supported native invocation path** available in the installed SQX runtime. MCP may supply project/strategy control and readback on that path. `sqx-lab` may be used only if the selected acceptance case specifically requires its custom-artifact capability.

Do not make successful `sqx-lab` generation a prerequisite for proving ordinary native SQX AI/AlgoWizard/Builder operation.

## PR disposition

- **PR #33 (Apollo): superseded/deferred. Do not merge or import its frontend assistant implementation into the native spine.** Its autonomy-safety ideas may be retained as historical design notes only.
- **PR #25:** its TraderCockpit-owned Builder/search producer remains rejected for production. Do not use it as the authoring engine.
- **Retained `ServletMCP`:** first-party native integration material for its proven inspection/control tools; not an authoring engine at the retained build.
- **`codex/sqx-lab-plugin`:** optional custom native-artifact authoring material. Keep it behind a narrow backend boundary and invoke it only for the capabilities it actually adds.

## Integration boundary

The browser must never execute plugin scripts or SQX tooling directly.

Prefer direct native SQX capabilities over wrappers. Where TraderCockpit needs a backend boundary, keep it transport-thin and capability-specific:

- native SQX AI/AlgoWizard bridge when a supported executable seam is proven;
- native SQX MCP client for the exact published MCP tools;
- `sqx-lab` invocation only for explicit custom-artifact authoring;
- native SQX Builder/Retester/project control through the consolidated SQX gateway.

The backend owns local SQX install discovery/verification, exact configuration/artifact custody, structured failures, approval state, and native job identity/readback. The frontend owns user input, review, approval, progress/readback and navigation.

## Acceptance

The authoring capability is complete only when TraderCockpit can preserve a user's intent through a **real native SQX authoring/execution path** and reopen the same native identities/results after restart.

A conversational demo, generated JSON, mock strategy, synthetic candidate, TraderCockpit-only strategy schema, or successful external-LLM artifact generation without native SQX acceptance does not satisfy this gate.

`sqx-lab` has its own narrower acceptance criterion: any artifact it authors must be install-derived, validated, and accepted by the target SQX installation. That criterion does not elevate the plugin above native SQX authoring authority.
