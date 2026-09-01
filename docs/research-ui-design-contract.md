# Research UI Design Contract

Status: **DRAFT — user approval required before production implementation**

This document is a visual and interaction contract for TraderCockpit Research. It is not a roadmap and does not change producer ownership or implementation order. Repository architecture remains authoritative.

The purpose of this contract is to prevent frontend and backend agents from coding blindly against different mental models.

## 1. Product role

Research is the historical strategy-research workspace inside TraderCockpit.

The user launches and operates TraderCockpit. StrategyQuant X 144.2953 is the native historical-research producer behind explicit integration boundaries. The Research UI reorganizes SQX capability around understandable research decisions without copying the SQX window hierarchy and without replacing SQX quantitative behavior.

Design mode: **Operate**.

Primary qualities:

- fast to scan;
- easy for a novice to start safely;
- complete enough for an expert to reach every supported native option;
- explicit about cause, execution stage, dependency, and consequence;
- dense where density helps work;
- restrained visually;
- no decorative complexity that competes with research state.

## 2. Fixed Research navigation

The architecture-fixed hierarchy remains:

```text
Research
├─ Construct
│  ├─ Idea
│  ├─ Specification
│  ├─ Build
│  └─ Candidates
├─ Backtest
│  ├─ Overview
│  ├─ Trades
│  ├─ Robustness
│  └─ Configuration
└─ Proof
```

The deeper SQX capability model lives inside these owning surfaces. Do not add Builder, Retester, Monte Carlo, Genetic, Custom Projects, or SQX as new top-level TraderCockpit workspaces.

## 3. Research shell

Desktop target: a three-zone work surface.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ TraderCockpit / Research / Construct / Specification          SQX Ready   │
├──────────────┬───────────────────────────────────────┬─────────────────────┤
│ Research nav │ Main work area                        │ Experiment summary  │
│              │                                       │                     │
│ Construct    │ Current section content               │ Effective choices   │
│  Idea        │                                       │ Dependencies        │
│  Specification                                      │ Cost / warnings     │
│  Build       │                                       │ Native provenance   │
│  Candidates  │                                       │                     │
│              │                                       │                     │
│ Backtest     │                                       │                     │
│ Proof        │                                       │                     │
└──────────────┴───────────────────────────────────────┴─────────────────────┘
```

The right summary rail is persistent on wide desktop layouts and collapses into an inspectable summary drawer/panel at narrow widths. It is not a second configuration editor.

The summary rail answers:

- what experiment is currently defined;
- what has changed since the last saved revision;
- whether the current configuration is structurally valid;
- which native capability owns the choice;
- which important dependent settings are active;
- likely execution cost class where known;
- whether a setting is unavailable/unproven;
- exact native identity/provenance in Native view.

## 4. One configuration, three levels of disclosure

Specification may expose three views of the same effective configuration:

```text
[ Simple ] [ Detailed ] [ Native ]
```

### Simple

Research intent and the highest-impact decisions. Intended for fast configuration and reusable presets/recipes.

### Detailed

All supported user-editable fields, dependencies, ranges, filters, and validation settings grouped by research meaning.

### Native

Exact effective SQX-facing configuration identity, current native values, source project/configuration provenance, unsupported fields, and read/write status.

Changing view does not fork or duplicate configuration state.

## 5. Construct / Idea

Idea remains intentionally light. It captures why the research exists, not the complete SQX configuration.

Primary content:

- strategy hypothesis / research question;
- market or domain intent;
- broad approach;
- constraints / exclusions;
- source/provenance;
- immutable revision identity;
- relationship to prior Idea revision.

Do not place the full Builder control surface here.

## 6. Construct / Specification

Specification is the main research-design workspace.

Its internal navigation is:

```text
Overview
Strategy
Data
Rule Space
Trading & Risk
Search
Selection
Validation
Advanced
```

This navigation is internal to Specification, not additional Construct tabs.

### 6.1 Overview

Overview is a compact map of the whole experiment.

Representative layout:

```text
SPECIFICATION / OVERVIEW

Strategy
New strategy · Long + Short · Symmetric entries
Search: Genetic Evolution

Market & Data
EURUSD · H1 · 2005–2026
IST / ISV / OOS configured

Rule Space
37 signals · 14 indicators · 6 operators
Market + Stop entries · 4 exit families

Selection
6 acceptance filters
Fitness: Return/DD composite
Databank: best 500

Validation
Higher precision → Additional markets → Monte Carlo

[Review changed settings]                         [Save revision]
```

Each summary row opens its owning section. Avoid dashboard-card nesting; use compact grouped rows, dividers, and status labels.

### 6.2 Strategy

The first decision is the native strategy source/build mode.

Representative choices:

```text
What are you creating?

● Generate a new strategy
○ Generate from a native template
○ Improve an existing strategy

Strategy architecture
○ Simple / primary chart
○ Multi-timeframe / multi-chart
○ Multi-symbol
○ Other installed native architecture, when discovered

Directions
☑ Long
☑ Short

Long / short relationship
Entries   [Symmetric | Independent]
Exits     [Symmetric | Independent]

Complexity
Entry conditions       [native range]
Exit conditions        [native range]
Lookback / shift       [native range]
```

Conditional behavior matters.

If `Improve existing` is selected, show the native parts-to-improve model instead of irrelevant new-generation settings:

```text
Parts to improve
Long entry         [native action]
Short entry        [native action]
Order type         [native action]
Long exit          [native action]
Short exit         [native action]
ATM / exits        [when supported]
```

Do not show fields that the selected native mode cannot use.

### 6.3 Data

Data separates strategy input architecture from validation markets.

Primary chart:

```text
Primary market
Symbol      EURUSD
Timeframe   H1
Engine      MetaTrader 5 / installed native value
```

Strategy context charts:

```text
Additional charts used by the strategy itself
+ EURUSD H4
+ GBPUSD H1
+ Add chart
```

This must not be labeled as additional-market robustness.

Historical range uses a visual timeline where supported:

```text
2005 ───────────────────────────────────────── 2026
|──────── IST ────────|── ISV ──|──── OOS ────|
```

Supported regions may include IST, ISV, OOS, and No Trade according to the actual installed configuration semantics.

Backtest assumptions are grouped below:

- precision;
- spread;
- slippage;
- commission;
- swaps;
- session;
- minimum distance / execution constraints;
- reserved warm-up bars;
- other installed native data settings.

Do not hard-code ranges or enums that have not been discovered from installed SQX or an authoritative current native configuration seam.

### 6.4 Rule Space

Rule Space represents the native strategy grammar, not a flat list of indicators.

Top-level taxonomy:

```text
Entry language
├─ Prebuilt signals
├─ Raw indicators
├─ Operators / comparisons
├─ Price / candle values
├─ Time conditions
├─ Custom blocks
└─ Other installed native block families

Pending-entry expressions
├─ Stop-price blocks
└─ Limit-price blocks

Order actions
├─ Market
├─ Stop
├─ Limit
├─ Enter / Reverse where native-supported
└─ Other installed native order actions

Exit language
├─ Stop Loss
├─ Profit Target
├─ Time / bars
├─ Trailing / dynamic exits
├─ Custom exits
└─ Other installed native exits
```

The default presentation is a searchable taxonomy with counts, enabled state, and dependencies.

Representative block inspector:

```text
RSI
Role                   Raw indicator
Enabled                Yes
Selection weight       1.0

Period
● Random range         5 — 100
○ Fixed
○ Selected values
○ Parameter sets

Shift                  0 — 5
Chart binding          Primary / selected chart
Comparison domain      Native / calibrated

Dependencies
Requires comparison/operator blocks
```

Parameter Values and Parameter Sets must be visually distinguished. Weighted parameter sets must show their probability effect rather than appearing as a second unrelated list.

The UI should detect obvious grammar incompatibilities from backend capability/read-model truth when possible, for example enabling raw indicator values while no compatible comparison/operator blocks are available.

### 6.5 Trading & Risk

Trading behavior is distinct from rule generation.

Representative groups:

```text
Session & timing
Trading window            08:00 — 17:00
Close at end of window    On
Exit end of day           Off
Exit end of week          Off

Activity
Maximum trades/day        5

Protective architecture
Stop Loss                 Required / Optional / Disabled
Profit Target             Allowed / Required / Disabled
Native min/max ranges     ...

Backtest behavior
Realistic gap handling    On
Store chart data          On
```

Money management and ATM are separate subgroups when supported by the installed runtime. Do not collapse them into one generic `Risk` slider.

### 6.6 Search

Search starts with a clear native method choice:

```text
How should SQX explore the rule space?

[ Random Discovery ]        [ Genetic Evolution ]
Independent sampling        Population-based evolution
```

#### Random Discovery

When Random is selected, do not show genetic population/island/crossover controls.

The flow preview is:

```text
Rule Space → Generate → Backtest → Selection → Build-time Validation → Databank
```

Expose only native Random-generation settings actually supported by the installed Builder configuration.

#### Genetic Evolution

Selecting Genetic reveals grouped controls with explanatory sequencing:

```text
Population
Population size                 [native]
Maximum generations             [native]

Evolution operators
Crossover probability           [native]
Mutation probability            [native]

Population topology
Islands                         [native]
Migration interval              [native]
Migration amount/rate           [native]

Starting population
● Generate new
○ Use initial-population databank

Initial-population gate
Decimation                      [native]
Initial filters                 [native]

Diversity
Duplicate handling              [native]
Fresh blood                     [native]
Weakest replacement             [native]
Cadence                         [native]

Restart / stagnation
Restart policy                  [native]
Stagnation behavior             [native]
Final generation handling       [native]
```

The interface must make it obvious that initial-population filtering is not the same as normal Ranking filtering.

### 6.7 Selection

Selection is shown as an execution funnel, not an undifferentiated filter form.

Representative visualization:

```text
Generated
   ↓
Initial-population gate       genetic-only
   ↓
Backtested
   ↓
Automatic dismissal
   ↓
Acceptance filters
   ↓
Build-time validation
   ↓
Fitness / ranking
   ↓
Databank
```

The exact native order must come from the installed producer/read-model authority. Do not hard-code this diagram if runtime evidence proves a different order for the current build.

Ranking editor should expose native-supported concepts such as:

- ranking/fitness source;
- predefined fitness metric;
- composite weighted fitness where supported;
- databank capacity;
- replacement/stop behavior;
- automatic dismissal;
- custom filters;
- IS / OOS / robustness / portfolio scopes where native-supported.

Filter rows use a dense table/editor:

```text
Scope     Metric             Operator     Value
IS        Trades             >=           100
IS        Profit Factor      >=           1.25
OOS       Profit Factor      >=           1.10
...       ...                ...          ...
```

Each filter can expose when it executes and what population/result set it affects.

### 6.8 Validation

Validation is a staged plan, not one on/off switch.

The UI distinguishes at minimum:

- OOS / unseen-time validation;
- higher-precision retest;
- additional-market/timeframe robustness;
- Monte Carlo trade manipulation;
- Monte Carlo full-retest methods;
- Walk-Forward;
- Walk-Forward Matrix;
- optimization/parameter-landscape methods;
- other installed SQX cross-check families.

The default representation is progressive cost and confidence:

```text
Discovery                                  Confirmation
────────────────────────────────────────────────────────
OOS                         → Higher precision
Basic acceptance            → Additional markets
                            → Monte Carlo
                            → Walk-Forward
                            → Parameter landscape
```

Each stage shows:

- research question being tested;
- whether it runs during Builder, Retester, or another native task;
- approximate cost class where known;
- filters/acceptance criteria applied to its result;
- dependencies;
- current enabled state.

#### Higher precision

This means retesting the same strategy with finer native backtest precision. It is not a second timeframe input to the strategy.

#### Additional markets/timeframes

This means testing an already-created strategy against other markets/timeframes for generalization. It is not the same as multi-chart strategy architecture.

#### Monte Carlo

Monte Carlo opens a dedicated nested work surface.

Trade-manipulation family may include installed methods such as:

- randomized trade order;
- skipped/missed trades;
- block/randomization methods supported by the current build.

Full-retest family may include installed methods such as:

- parameter jitter/randomization;
- historical-data/price perturbation;
- randomized starting bar;
- spread/slippage/execution degradation;
- other current SQX methods.

The UI must show whether a method operates on an existing trade sequence or requires complete retests.

Representative Monte Carlo editor:

```text
Monte Carlo / Model sensitivity

Methods
☑ Parameter jitter
   Probability changed      [native]
   Maximum change           [native]

☑ Randomized start

☑ Execution degradation
   Native parameters        [...]

Simulations                 [native]
Confidence / percentile     [native]
Acceptance rule             [native filter]

Estimated work              200 complete retests
```

Never invent a method because it sounds statistically useful. Only installed/native-supported methods appear as enabled controls.

### 6.9 Advanced

Advanced contains supported native settings that do not belong cleanly in the decision-oriented groups above, plus inspection of capability/read-model state.

It is not a dumping ground for settings the design did not understand. Every field still has an owning meaning, dependency, and provenance.

## 7. Quick configuration without losing depth

TraderCockpit should support a low-friction start without reducing SQX to generic knobs.

Inside Specification, Simple view may offer reusable starting configurations such as:

```text
Start from

[ New Random Discovery ]
[ New Genetic Discovery ]
[ Improve Existing Strategy ]
[ Saved Research Configuration ]
[ Native Template / Project where supported ]
```

These are bindings to real supported native configurations/templates or clearly labeled TraderCockpit configuration revisions. They are not hidden substitute algorithms.

Selecting a starting configuration immediately populates the same Detailed/Native model and shows exactly what was applied.

## 8. Builder configuration vs Custom Projects

This distinction must be explicit.

### Builder configuration

Defines one native Builder strategy-generation/search operation.

### Native Custom Project

Defines a native multi-task research workflow that can sequence Builder, Retester, filters, loops, databanks, correlation tasks, and other installed project tasks.

TraderCockpit must not flatten these into the same object.

Research may show a one-click entry to a reusable native Custom Project, but ownership is Automation / native project execution. A deep link can say:

```text
Automated research recipe
Build → Higher Precision → Additional Markets → Monte Carlo → Final Databank

[Open in Automation]    [Run approved project]
```

The exact task graph comes from the native Custom Project/read-model seam. TraderCockpit does not implement its own task-loop engine.

## 9. Construct / Build

Build becomes execution-oriented after Specification carries the configuration depth.

Before start:

```text
BUILD

Specification
EURUSD H1 · Genetic Evolution · 4 islands · 37 signals
[Review specification]

Native configuration
Compiled            Ready
Reopened            Verified
Approval            Approved
Producer            SQX 144.2953
[Inspect effective native configuration]

Execution
[Start Build]
```

While running, the surface should expose only producer/read-model truth actually available, for example:

```text
Generation          17 / 50
Population          100
Generated           18,421
Backtested          ...
Accepted            73
Databank            73 / 500
```

Where native rejection/dismissal reason counts are available, show a live waterfall/table so the user can diagnose an over-constrained experiment. Do not fabricate diagnostics from frontend guesses.

Build is not the place to duplicate every Specification control. A compact `Change specification` action returns to the owning setting.

## 10. Construct / Candidates

Candidate rows/cards preserve exact producer and Specification provenance.

Every Candidate must make it possible to navigate back to the immutable Specification/configuration revision that produced it.

Candidate presentation may include:

- native archive identity;
- originating Builder/native job;
- current promotion/state;
- key result metrics from authoritative result custody;
- validation status with explicit scope;
- links to Backtest Overview, Trades, Robustness, Configuration.

Do not mix live/current performance into historical Candidate metrics.

## 11. Backtest

### Overview

Summary of one Historical Result with explicit data scope, producer/run provenance, headline statistics, and validation summaries.

### Trades

Dense inspectable trade table/readback from the authoritative native seam. No fabricated trade rows.

### Robustness

Results of actual native robustness/cross-check methods. Configuration of a new validation plan belongs primarily in Specification; results and drill-down belong here.

### Configuration

Exact immutable configuration and producer provenance used for the selected result. This is the audit/recovery surface that answers, “what exact experiment produced this strategy/result?”

## 12. Interaction rules

- Prefer inline expansion and split-pane inspectors over modals.
- Use searchable pickers for large native catalogs.
- Keep labels stable across Simple/Detailed/Native.
- Show conditional sections only when relevant, but preserve a visible explanation of why a section is unavailable.
- Changes update the summary rail immediately as local draft state; canonical save/compile/approval remains backend-owned.
- Dirty state is explicit.
- Saved revisions are immutable.
- Do not silently reset dependent settings. When a parent choice invalidates child settings, show what will change before applying destructive normalization.
- Do not hide critical dependency information exclusively in hover tooltips.
- Keyboard focus, disabled state, loading state, error state, and selected state are mandatory for interactive components.

## 13. Visual language

The exact visual world remains subject to user approval, but the Operate constraints are fixed for the approval candidate:

- dark desktop product surface may preserve the current TraderCockpit direction unless a redesign candidate clearly improves it;
- restrained neutral layers;
- one primary accent for actions/current selection;
- semantic colors for success/warning/error/info;
- no gradient-heavy AI/SaaS styling;
- no card-inside-card wall;
- no oversized decorative headings;
- familiar inputs, tables, tabs, split panes, searchable selectors, and disclosure controls;
- dense data is acceptable;
- motion 150–250 ms and only for state change/feedback;
- no choreographed page-load animation.

## 14. Backend/read-model contract expected by the UI

The UI should ultimately consume a capability-oriented read model rather than hard-coded frontend knowledge alone.

Conceptual capability descriptor fields may include:

```text
key
native identity/path
family
label
description
type
current/effective value
allowed values/range/units
required/optional
visible-when dependency
read capability
write capability
producer build/version
source/provenance
availability/refusal reason
```

This schema is conceptual until the backend slice proves the actual native seam. Do not implement an invented generic schema merely because it appears here.

The important contract is that the frontend must be able to ask what the installed/authorized SQX configuration supports and render unsupported capability truthfully.

## 15. Approval artifact — not screenshots alone

Before replacing production Research UI, build an isolated runnable approval surface in the existing TraderCockpit frontend stack.

It must use deterministic fixture/read-model data and perform no native mutations.

The approval surface must demonstrate these scenarios:

1. Simple Random Discovery configuration.
2. Detailed Genetic Evolution configuration with islands, migration, initial-population controls, fresh blood, restart/stagnation.
3. Rule Space with signals, raw indicators, operators, parameter ranges, and parameter sets.
4. Multi-chart strategy context vs separate additional-market robustness.
5. Selection funnel showing initial-population gate, Ranking filters, build-time validation, and databank.
6. Monte Carlo trade-manipulation vs full-retest configuration.
7. Higher-precision validation.
8. Saved/reusable configuration applying into the same Detailed model.
9. Native Custom Project shown as automation workflow rather than Builder settings.
10. Invalid/unavailable native capability state.
11. Dirty/unsaved revision state.
12. Build-ready summary and transition into execution.

The approval candidate must be navigable and interactive enough to evaluate actual flow, density, disclosure, terminology, keyboard/focus behavior, and conditional states. Static image comps may inform direction but are not sufficient approval evidence.

## 16. Impeccable review gate

Before user presentation of the approval candidate:

- run an Impeccable-style critique for hierarchy, clarity, cognitive load, and task flow;
- run an accessibility/responsive audit;
- remove generic AI-UI tells such as unnecessary nested cards and decorative gradients;
- perform one bounded correction pass;
- present the runnable candidate and the unresolved design choices.

Do not continue polishing indefinitely.

## 17. Decisions still requiring user approval

The following remain DRAFT until the user approves them through the runnable candidate:

- exact visual treatment and density;
- whether the Specification internal navigator is a left subrail or compact horizontal/vertical hybrid;
- the final names `Simple`, `Detailed`, and `Native`;
- right-summary-rail width and collapse behavior;
- exact quick-start presentation;
- how much native terminology is shown by default versus progressively disclosed;
- final representation of large Rule Space catalogs;
- final validation-pipeline visualization;
- final Custom Project deep-link/Automation handoff treatment.

No frontend or backend agent should treat these draft decisions as settled merely because they are written here.
