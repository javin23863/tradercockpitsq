# TraderCockpit Agent Execution Policy

This file is repository-level policy for every LLM or implementation agent working in `javin23863/tradercockpitsq`.

## 1. Non-negotiable product direction

- `main` is the canonical TraderCockpit production line.
- **StrategyQuant X 144.2953 is the strategy-research producer/backend authority. TraderCockpit is the desktop/application/UI/configuration/custody layer around that backend.**
- Do not implement a second Builder, genetic algorithm, strategy-tree language, backtester, robustness engine, optimizer, or Custom Project executor in TraderCockpit when SQX owns that operation.
- The currently proven producer boundary is the verified SQX 144.2953 runtime controlled through native configuration/projects/databanks/`sqcli.exe`. Production adapters may evolve, but producer ownership does not move into ad-hoc TraderCockpit algorithms.
- Recovered/source/reference trees are evidence and build-time research material. Production code must not import those trees as loose runtime dependencies. This does not make the actual SQX worker “reference only.”
- `javin23863/futures` remains quarantined unless the user explicitly reverses that decision.
- Do not invent Phase 0 / Phase 1 / `phase01_intake` product stages.
- The accepted TraderCockpit prototype defines presentation. Do not clone SQX’s dense interface.

`docs/product-architecture-v1.md` is the controlling architecture. `IMPLEMENTATION_CHECKLIST.md` is the controlling execution order. **`docs/product-backbone-spec-v1.md` is the binding detailed UI/application/API/add-on contract and must be read before implementation of any product surface or backend seam.** **For strategy authoring/assistant behavior, `docs/sqx-authoring-authority-v1.md` is the binding amendment and supersedes older Apollo-specific requirements until the architecture documents are consolidated.**

The backbone is intentionally stable: core research stages are exactly `Construct | Backtest | Proof`; Construct core tabs are `Idea | Specification | Build | Candidates`; Backtest core tabs are exactly `Overview | Trades | Robustness | Configuration`. Dynamic capabilities and add-ons populate registered extension slots rather than rewriting this core navigation.

### Authoring authority amendment

Apollo is deferred. Do not import or merge a persistent Apollo assistant into the repaired native-SQX spine.

The current authoring authority is the SQX-oriented authoring adapter defined in `docs/sqx-authoring-authority-v1.md`. Its first repository candidate is the existing `sqx-lab` toolchain on `codex/sqx-lab-plugin`, which authors validated native SQX/AlgoWizard blocks, groups, `.sqx` templates and `project.cfx` projects against the actual SQX installation.

Do not claim that SQX exposes a directly embeddable first-party LLM API until executable evidence proves that exact seam. The product contract is the authoring adapter; the current implementation candidate is `sqx-lab`, and a future proven native SQX AI interface may replace it behind the same boundary.

## 2. Mandatory context before planning or editing

Before changing an SQX-backed capability:

1. inspect the relevant original SQX screenshots, not only prose summaries;
2. inspect matching `.cfx`, task XML, preset/configuration, output archives, or runtime evidence;
3. inspect the accepted TraderCockpit prototype mapping;
4. read the matching section of `docs/product-backbone-spec-v1.md`;
5. for authoring/assistant work, also read `docs/sqx-authoring-authority-v1.md`;
6. identify which native SQX module owns the operation;
7. identify exactly what TraderCockpit must configure/control/read/persist/present;
8. only then edit implementation files.

The observed Builder configuration surfaces are:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`

These define the native construction/search configuration. Genetic evolution is one Builder build mode. Retester operates on existing strategies. Custom Projects automate ordered native tasks/databanks.

## 3. Producer boundary — do not reconstruct a replacement engine

Previous repository policy required deterministic TraderCockpit-owned reconstruction when SQX internals were incomplete. **That policy is superseded.**

When native producer behavior is not yet wired:

- inspect more native configuration/source/runtime evidence;
- expose the native field/capability as unresolved or unavailable if necessary;
- extend the adapter to the actual SQX producer;
- do not fill the gap by creating a new TraderCockpit algorithm and calling it SQX-backed.

TraderCockpit may implement application mechanics that SQX does not own: API routing, desktop supervision, intent/configuration records, exact configuration snapshots, process control, content-addressed custody, lifecycle state, read models, UI state, provenance and proof.

TraderCockpit must not manufacture strategy generation, backtest results, native fitness, robustness outcomes, optimization outcomes, or Custom Project task semantics.

## 4. Required user lifecycle

The product lifecycle is:

```text
Ideas / sources
  → SQX-oriented authoring when needed
  → Construct native SQX configuration
  → native Builder generation + initial evaluation
  → Candidate Lab
  → native Backtest / cross-check / Retester / Optimizer funnel
  → Proof
```

Custom Projects automate this lifecycle using native SQX tasks/databanks.

For plain-language strategy creation, use the SQX authoring adapter. The current candidate implementation is the repository's `sqx-lab` native-artifact toolchain. TraderCockpit may collect the user's request, present generated native artifacts/configuration for review, and require explicit approval; it must not create a second assistant-owned strategy language, silently launch compute, waive gates, promote candidates, or claim proof.

## 5. Foundation gate before feature expansion

Do not treat isolated feature PRs as progress ahead of the foundation vertical.

The first required product proof is:

`simple indicator idea → SQX authoring adapter/native artifact → approved native Builder configuration → native Builder run → native .sqx survivor → Candidate Lab → one downstream native validation/retest → Backtest result → Proof → restart/reopen same identities`

This must execute through the canonical TraderCockpit application and real SQX producer. A mock, fixture, Python replacement strategy, synthetic result, or TraderCockpit-only strategy language cannot satisfy the gate.

Until this proof is green, prioritize work that closes this exact path.

## 6. Disposition of duplicate producer work

- PR #23: native SQX candidate/Retester/custody pieces are candidate integration material.
- PR #2: verified native Builder control is candidate integration material.
- PR #25: do not merge its TraderCockpit-owned Builder/search producer. Salvage only valid UI/custody/application pieces.
- `product/tradercockpit/builder/evolution.py` on `main`: do not expand or treat as production producer authority; quarantine/remove from production wiring during the spine repair.
- PR #27: do not merge TraderCockpit-owned Monte Carlo/robustness producer algorithms where native SQX owns the cross-check.
- PR #28: do not merge a TraderCockpit-owned task/loop executor as a replacement for native Custom Projects.
- PR #33: Apollo is superseded/deferred; do not merge or import its persistent assistant implementation into the native spine.
- `codex/sqx-lab-plugin`: candidate SQX authoring integration material. Audit and expose it only through a narrow backend authoring adapter; browser code must not invoke plugin/SQX tooling directly.
- Algorithm-parity ingredient PRs are evidence/test donors, not production engine modules.
- Read-only UI/proof PRs may be reused only after verifying they consume real canonical backend truth.

## 7. Assistant-first execution

The primary assistant must do every operation available through connected repository/runtime tools before delegating.

This includes repository inspection, planning, code changes, commits, pushes, PR updates, test inspection and review. Delegate only a concrete operation that requires an unavailable local Windows/SQX/GUI/runtime environment.

A desktop agent is an implementation/runtime executor, not the product architect unless the user explicitly says otherwise.

## 8. Concurrent work isolation

- Each concurrent lane gets its own branch/worktree.
- Never switch/reset/clean another lane’s checkout.
- Treat unknown local changes as protected concurrent work.
- Re-check live PR heads before touching shared files.
- Acceptance runs on a clean checkout pinned to the exact tested commit.

## 9. Implementation behavior

- Prefer completing one end-to-end vertical over creating isolated capability fragments.
- Do not create a new architecture because a shared file is occupied; wait/rebase or use a clearly temporary adapter seam.
- Do not create a second server, store, candidate identity, run pipeline, or result authority to avoid an integration conflict.
- Preserve exact native configuration, archive and producer identities.
- Native producer errors must be structured and visible; never silently fall back to a substitute implementation.
- UI data comes from backend read models; do not hard-code producer state, result metrics, phase counts, candidate IDs or validation truth.
- The frontend and SQX authoring adapter must not maintain their own invented master lists of indicators, asset classes, robustness methods, add-ons, providers, delivery targets, or capabilities. Authoring catalogs must come from the actual SQX installation/toolchain; product capability lists come from the backend capability manifest/read models defined in `docs/product-backbone-spec-v1.md`.
- Fast/Golden or other product profiles must compile to inspectable native SQX-backed plans rather than frontend constants.
- Add-ons may contribute only through registered typed extension slots and compatible presentation primitives. They do not append arbitrary core-stage tabs or inject arbitrary frontend code from backend data.
- Documentation is not an implementation substitute. After an architecture decision is recorded, default to executable product work.

## 10. Review and correction ownership

After any implementation report:

1. inspect the actual diff/state;
2. compare it with the native SQX authority, `docs/product-backbone-spec-v1.md`, `docs/sqx-authoring-authority-v1.md` where applicable, and TraderCockpit UI authority;
3. fix concrete defects directly when possible;
4. run focused acceptance;
5. run full applicable product/browser/runtime acceptance on the exact head;
6. report product completion only when the real user path is executable.

Green unit tests do not establish product completion.

## 11. PR and Codex closure

For a PR that is intended to merge:

- record the exact current head;
- run required executable acceptance on that exact head;
- inspect substantive Codex findings for that exact head;
- correct valid findings and rerun acceptance;
- a corrective commit requires fresh review of the new head;
- do not describe mechanical workflow success as substantive review closure.

Codex quota/unavailability may defer review closure, but it must not cause new speculative feature branches. Continue only with non-overlapping executable work that advances the controlling product spine.

## 12. Binding acceptance question

For every claimed capability ask:

> Can a user perform the intended operation through the real TraderCockpit desktop/UI, through the canonical application adapter, through the actual SQX producer that owns the operation, and receive durable truthful results back in TraderCockpit?

For authoring, additionally ask:

> Did the user's request become a native SQX artifact/configuration that the real installed SQX system can load/build, rather than a TraderCockpit-only strategy representation?

If no, the capability is not product-complete.

Any older repository instruction requiring TraderCockpit-owned reproduction of SQX producer algorithms, a persistent Apollo assistant, or a different core research navigation model is superseded by this file, `docs/product-architecture-v1.md`, `docs/product-backbone-spec-v1.md`, and `docs/sqx-authoring-authority-v1.md`.