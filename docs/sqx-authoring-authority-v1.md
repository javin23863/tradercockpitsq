# SQX Authoring Authority v1

## Status

This document is the binding strategy-authoring companion to the consolidated native-SQX product spine in PR #35.

Read it with `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, `docs/product-backbone-spec-v1.md`, and `docs/consumer-openrouter-account-authority-v1.md`.

## Product decision

**StrategyQuant X is the strategy-intelligence and authoring authority. Apollo is deferred and must not be imported into the repaired product spine.**

TraderCockpit will not build, import, or merge a separate persistent strategy assistant, strategy language, or strategy-generation engine to reproduce capabilities owned by SQX.

The native product hierarchy is:

```text
user idea / strategy request
    -> native SQX AI Wizard / AI Assistant and AlgoWizard when authoring from an idea
    -> native SQX Builder when searching/generating candidates
    -> native SQX backtest / validation / robustness / optimization
    -> real SQX strategies/results
    -> TraderCockpit custody / Candidate Lab / Backtest / Proof / presentation
```

TraderCockpit remains the desktop/application, configuration/custody, approval, launch/control, readback and presentation layer. A backend bridge exists only to invoke or observe native SQX capabilities; it is not a second strategy-generation authority.

## Proven native AI authority

Current StrategyQuant product behavior/documentation for the retained 144 generation establishes an integrated SQ AI / AI Wizard capability that accepts natural-language strategy ideas, creates or improves strategies, and hands them into AlgoWizard for backtesting/further editing. That is the primary AI-assisted strategy-authoring capability for the repaired product.

The existence of native SQX AI-assisted authoring is **PROVEN**. What remains **OPEN EVIDENCE** is the exact supported programmable seam, if any, through which TraderCockpit can invoke that native AI capability without automating or reimplementing it.

Do not confuse those questions. Lack of a proven programmatic endpoint does not make `sqx-lab`, Apollo, OpenRouter, or a TraderCockpit LLM the core strategy-authoring engine.

## MCP boundary

The retained SQX 144.2953 source contains the first-party `ServletMCP` plugin. It is an MCP server for AI tool integration and registers SQX's MCP endpoint, but retained `MCPTools.createSpecs()` is narrow.

At this build it exposes:

- `list_projects`
- `list_databanks`
- `list_strategies`
- `get_strategy_stats`
- `run_project`
- `stop_project`

Those tools delegate to SQX's native `Project` program API. They provide native inspection/control, but do **not** expose AlgoWizard strategy construction, Builder configuration authoring, block/group/template authoring, candidate-generation primitives, or a general LLM endpoint.

Therefore:

- use MCP where its proven tools fit;
- do not treat MCP as the strategy-authoring engine;
- do not invent authoring methods absent from retained source;
- re-audit MCP when a newer SQX build adds tools.

## `sqx-lab` boundary

The repository contains the `sqx-lab` authoring plugin on:

- branch: `codex/sqx-lab-plugin`
- reviewed source head: `dad4b0bae2b7556ec15870dca96df057e1e91b84`
- plugin root: `plugins/sqx-lab/`

`sqx-lab` is an **optional SQX authoring extension for external-LLM/custom-artifact workflows**, not the foundation intelligence layer.

It is install-derived and authors native StrategyQuant X / AlgoWizard artifacts through:

```text
custom block -> random group -> strategy template -> build project
```

Useful capabilities include:

- SQX custom condition/price-level blocks;
- random groups sampled by Builder;
- validated `.sqx` strategy templates;
- cloning a real installed SQX project and wiring templates into native `project.cfx` Build tasks;
- deriving catalogs from the actual SQX installation rather than inventing identities;
- validating generated artifacts before import and leaving final acceptance to the real SQX installation.

This is materially different from PR #25's TraderCockpit-owned `tradercockpit.builder-strategy.v1` producer because `sqx-lab` authors **native SQX artifacts** and leaves Builder/backtest/GA semantics with SQX.

However, `sqx-lab` must not be promoted into the product's core LLM, universal authoring gateway, or replacement for native SQ AI / AlgoWizard. Use it only where its additional native-artifact capability is actually needed.

## Consumer OpenRouter relationship

OpenRouter is the bounded consumer external-LLM transport/billing fabric defined in `docs/consumer-openrouter-account-authority-v1.md`.

It may support:

- intent interpretation around the native workflow;
- approved `sqx-lab`/extension calls;
- explanation/summarization;
- allowed tool use.

It does not become the SQX strategy producer. The current workhorse policy (`z-ai/glm-5.3-flash`) is a consumer routing choice, not a strategy-authority decision.

## UI decision

There is **no persistent Apollo dock** in the repaired foundation product.

Construct / Idea may expose native SQX idea authoring through the smallest supported bridge executable evidence permits. The UI must distinguish native SQX AI/AlgoWizard authoring from optional `sqx-lab` custom-artifact operations and from general bounded language assistance.

The surface may:

- accept a plain-language request for native SQX AI/AlgoWizard authoring when a supported invocation seam is proven;
- expose native MCP project/strategy inspection and run/stop controls where useful;
- invoke `sqx-lab` for explicit custom block/group/template/project authoring needs;
- show proposed native artifacts/configuration and human-readable diffs;
- require explicit approval before consequential native Builder/validation execution.

It may not:

- route every idea through `sqx-lab` merely because an external LLM can call it;
- treat MCP as an authoring API when the retained registry does not provide authoring tools;
- fabricate an SQX block/template/project not valid for the target install;
- convert a request into a competing TraderCockpit executable strategy language;
- substitute TraderCockpit-generated candidates/results when SQX authoring/execution fails.

## Foundation vertical

The authoring proof is a native-SQX path:

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

The first implementation slice proves the **smallest supported native invocation path** available in the installed SQX runtime. MCP may supply project/strategy control/readback. `sqx-lab` participates only if the selected acceptance case specifically requires custom-artifact capability.

Successful `sqx-lab` generation is not a prerequisite for ordinary native SQX AI/AlgoWizard/Builder operation.

## PR disposition

- **PR #33 (Apollo):** superseded/deferred; do not merge/import its persistent assistant implementation.
- **PR #25:** reject its TraderCockpit-owned Builder/search producer as authoring engine.
- **Retained `ServletMCP`:** first-party native integration material for proven inspection/control tools.
- **`codex/sqx-lab-plugin`:** optional custom native-artifact material behind a narrow backend boundary.

## Integration boundary

Browser code never executes plugin scripts or SQX tooling directly.

Prefer direct native SQX capabilities. Backend boundaries remain transport-thin/capability-specific:

- native SQX AI/AlgoWizard bridge when a supported executable seam is proven;
- native SQX MCP client for exact published tools;
- `sqx-lab` invocation only for explicit custom-artifact authoring;
- native SQX Builder/Retester/project control through the consolidated gateway.

The backend owns local SQX install discovery/verification, exact configuration/artifact custody, structured failures, approval state, and native job identity/readback. The frontend owns user input, review, approval, progress/readback and navigation.

## Acceptance

Strategy authoring is complete only when TraderCockpit preserves user intent through a **real native SQX authoring/execution path** and can reopen the same native identities/results after restart.

A conversational demo, generated JSON, mock strategy, synthetic candidate, TraderCockpit-only strategy schema, or successful external-LLM artifact generation without native SQX acceptance does not satisfy this gate.

`sqx-lab` has its own narrower acceptance criterion: any artifact it authors must be install-derived, validated and accepted by the target SQX installation. That does not elevate the plugin above native SQX authoring authority.
