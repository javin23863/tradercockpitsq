---
name: tradercockpit-research-ui
description: Use for planning, designing, critiquing, prototyping, or implementing TraderCockpit Research UI. Applies the pinned Impeccable design methodology under TraderCockpit's existing product architecture and SQX producer-authority rules. Do not use for backend-only work.
---

# TraderCockpit Research UI skill

This skill adapts the pinned `vendor/impeccable` design skill to TraderCockpit without creating a competing product authority.

## Mandatory authority order

Before any Research UI work, read:

1. `AGENTS.md`
2. `docs/product-architecture-v1.md`
3. `docs/product-backbone-spec-v1.md`
4. `LIVING_IMPLEMENTATION_PLAN.md`
5. `docs/research-ui-design-contract.md`
6. `vendor/impeccable/.agents/skills/impeccable/SKILL.md`
7. The one Impeccable reference that owns the task.

TraderCockpit architecture and runtime truth always win over generic design guidance. Impeccable is a design methodology, not a second roadmap or product specification.

Do not create or overwrite `PRODUCT.md`, `DESIGN.md`, or another roadmap merely because upstream Impeccable normally uses those files. Existing TraderCockpit architecture documents remain product truth unless the repository policy is explicitly changed.

## Mode

Research is an Impeccable **Operate** surface. Load:

- `vendor/impeccable/.agents/skills/impeccable/reference/operate.md`

Use task completion, scanability, clear state, and earned familiarity as the visual priority. Do not turn Research into a marketing surface.

## Workflow

### 1. Shape before code

For a new Research surface, substantial restructuring, or a new interaction model, load:

- `vendor/impeccable/.agents/skills/impeccable/reference/shape.md`
- `vendor/impeccable/.agents/skills/impeccable/reference/new-work.md`

Resolve information architecture, user decisions, states, conditional behavior, and approval target before editing production UI.

### 2. Preserve the fixed product hierarchy

Top-level Research navigation remains:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

Depth belongs inside these owning surfaces. Do not create a competing top-level workspace to mirror SQX modules.

### 3. Model SQX depth truthfully

SQX remains native producer authority for Builder, random generation, genetic evolution, building blocks, ranking/filtering, cross-checks, Retester, robustness methods, optimization/Walk-Forward, Custom Projects, and native artifacts.

The UI may reorganize native capability around user decisions, but must not invent producer semantics, generic substitutes, unsupported fields, or fake outcomes.

Every native-facing control needs a truthful source/read-model path before production wiring. Unsupported or unproven capability is shown as unavailable rather than fabricated.

### 4. Use progressive disclosure, not capability deletion

Design three views of one effective configuration where appropriate:

- `Simple`: research intent and high-impact choices.
- `Detailed`: all supported configuration fields and dependencies.
- `Native`: exact SQX identity/effective values/provenance.

These are views of one configuration, never three independent models.

### 5. Make sequencing visible

Where SQX behavior is stage-dependent, show the stage and consequence. Examples:

- initial-population filtering vs normal Ranking filters;
- build-time cross-checks vs downstream Retester validation;
- multi-chart strategy inputs vs additional-market robustness tests;
- random generation vs genetic evolution;
- Builder configuration vs Custom Project automation.

Use funnels, pipelines, dependency callouts, and inline consequences rather than explanatory modals.

### 6. Prefer inline and structural UI

Follow Impeccable Operate guidance:

- restrained color; accents indicate action/state, not decoration;
- dense information is allowed when it improves work;
- standard navigation and form affordances are preferred;
- every control has default/hover/focus/active/disabled/loading/error where applicable;
- skeletons for loading;
- empty states teach the next action;
- motion only communicates state;
- avoid nested-card sprawl and modal-first interactions;
- do not hide critical causal relationships inside tooltips alone.

### 7. Approval gate before implementation

For major Research redesign work, do not ask the user to approve screenshots alone.

Produce an isolated, runnable frontend approval surface using the real TraderCockpit frontend stack and component vocabulary, with deterministic fixture/read-model data and no native mutation. It must demonstrate:

- navigation and hierarchy;
- conditional settings;
- representative dense states;
- errors/unavailable states;
- Simple/Detailed/Native transitions where used;
- the right-side effective experiment summary;
- at least one random-generation path;
- at least one genetic-evolution path;
- validation sequencing;
- Builder vs Custom Project distinction.

The user approves the interaction contract and runnable flow before backend wiring or production replacement begins.

### 8. Critique and audit before approval

Before presenting an approval candidate, load and apply:

- `vendor/impeccable/.agents/skills/impeccable/reference/critique.md`
- `vendor/impeccable/.agents/skills/impeccable/reference/audit.md`

After implementation, use `polish.md` and `harden.md` as bounded final passes. Do not enter open-ended visual polishing loops.

## Non-negotiable Research design contract

The current draft is `docs/research-ui-design-contract.md`. Treat sections marked `APPROVED` as binding and sections marked `DRAFT` as proposals requiring user approval.

Do not turn a draft visual or interaction decision into production code merely because it appears in the repository.
