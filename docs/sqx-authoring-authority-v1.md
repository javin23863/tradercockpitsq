# SQX Authoring Authority v1

## Status

This document is a binding amendment to the native-SQX product spine while PR #35 is being finalized.

For strategy authoring and assistant behavior, this document **supersedes every Apollo-specific requirement** currently present in `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, and `docs/product-backbone-spec-v1.md` until those documents are consolidated.

## Product decision

**Apollo is deferred and must not be imported into the repaired product spine.**

TraderCockpit will not build, import, or merge a separate persistent Apollo assistant merely to reproduce a strategy-authoring capability that already exists around the SQX backend.

The current authoring direction is:

```text
plain-language strategy request
    -> SQX-oriented AI authoring adapter
    -> exact native SQX artifacts/configuration
    -> explicit user review/approval
    -> native SQX Builder / AlgoWizard workflow
    -> real .sqx candidates/results
    -> TraderCockpit Candidate Lab / Backtest / Proof
```

TraderCockpit remains the application, custody, review, launch/control, readback and presentation layer. It does not become a second strategy-language or strategy-generation authority.

## Current repository authoring asset

The repository already contains the `sqx-lab` authoring plugin on branch:

- branch: `codex/sqx-lab-plugin`
- reviewed source head at this decision: `dad4b0bae2b7556ec15870dca96df057e1e91b84`
- plugin root: `plugins/sqx-lab/`

The plugin is install-derived and produces StrategyQuant X / AlgoWizard artifacts through the chain:

```text
custom block -> random group -> strategy template -> build project
```

Its documented capabilities include:

- authoring SQX custom condition/price-level blocks;
- authoring random groups sampled by Builder;
- emitting validated `.sqx` strategy templates;
- cloning a real installed SQX project and wiring templates into native `project.cfx` Build tasks;
- deriving catalogs from the actual SQX installation rather than inventing project/template identities;
- validating generated artifacts and using the real SQX install/build path for confirmation.

This is materially different from PR #25's TraderCockpit-owned `tradercockpit.builder-strategy.v1` producer. `sqx-lab` authors **native SQX artifacts** and leaves Builder/backtest/GA semantics with SQX.

## Evidence boundary

The repository proves the `sqx-lab` plugin/toolchain and its native-artifact workflow. It does **not yet prove that SQX 144.2953 itself exposes a directly embeddable first-party LLM API** that TraderCockpit can call.

Therefore implementation must not hard-code the claim "SQX has a native LLM endpoint" until executable evidence establishes that exact seam.

For now, the product abstraction is an `SQX authoring adapter`. Its first candidate implementation is the existing `sqx-lab` toolchain. If later evidence proves a direct native SQX AI/LLM interface, the adapter may be redirected to it without changing the TraderCockpit product contract.

## UI decision

There is **no persistent Apollo dock** in the repaired foundation product.

The Construct / Idea surface may provide a simple strategy-description input such as "Describe the strategy you want to build" only when it is connected to the SQX authoring adapter. That surface must not contain a second independent assistant runtime, autonomous agent, or hidden TraderCockpit strategy DSL.

The authoring flow may:

- accept a plain-language strategy request;
- invoke the SQX authoring adapter;
- return proposed native blocks/groups/templates/projects/configuration;
- identify unresolved choices required by the native artifact path;
- show the exact proposed native artifacts/configuration and a human-readable diff;
- require explicit approval before native Builder execution.

It may not:

- fabricate an SQX block/template/project not present in or valid for the target install;
- convert the request into `tradercockpit.builder-strategy.v1` or another competing executable strategy language;
- silently launch Builder or validation;
- substitute TraderCockpit-generated candidates/results when SQX authoring or execution fails.

## Foundation vertical correction

The foundation proof is amended to use the existing SQX-oriented authoring path rather than Apollo:

```text
plain-language/simple-indicator request
  -> SQX authoring adapter
  -> validated native block/group/template/project or exact approved native Builder configuration
  -> native Builder
  -> real .sqx survivor
  -> Candidate Lab
  -> native validation/retest
  -> Backtest
  -> Proof
  -> restart/reopen same identities
```

The first implementation slice must prove the smallest executable path through the real installed SQX system. It should reuse one known-good donor/project/template path from `sqx-lab` rather than attempting broad free-form strategy generation on day one.

## PR disposition

- **PR #33 (Apollo): superseded/deferred. Do not merge or import its frontend assistant implementation into the native spine.** Its autonomy-safety ideas may be retained as historical design notes only.
- **PR #25:** its TraderCockpit-owned Builder/search producer remains rejected for production. Do not use it as the authoring engine.
- **`codex/sqx-lab-plugin`:** candidate authoring integration material. Audit and integrate through a narrow backend adapter; do not expose plugin internals directly to browser code.

## Integration boundary

The browser must never execute plugin scripts or SQX tooling directly.

Use one backend authoring boundary with structured request/result contracts. The backend owns:

- local SQX install discovery/verification;
- controlled invocation of the authoring toolchain;
- exact input/output artifact custody;
- structured failures;
- explicit approval state;
- handoff to the existing native SQX Builder control path.

The frontend owns only user input, review, approval, progress/readback and navigation.

## Acceptance

The authoring capability is not complete until a user can enter one bounded strategy request in TraderCockpit and receive a **real native SQX artifact/configuration that SQX successfully loads/builds**, with the exact artifact/configuration retained and reopened after restart.

A conversational demo, generated JSON, mock strategy, synthetic candidate, or a TraderCockpit-only strategy schema does not satisfy this gate.
