# Desktop Agent: TraderCockpit UI + Implementation Authority

Start here before changing the engine or frontend framing.

The product UI authority is the recovered multicolor ESQ TraderCockpit prototype. It supersedes the earlier dark-blue `Chart / Backtest / Proof` shell.

The active cross-repository execution checklist is:

- [`/IMPLEMENTATION_CHECKLIST.md`](../../IMPLEMENTATION_CHECKLIST.md)

Read that checklist before implementation. It preserves the reviewed Phase-1 proof, the capability-graph product rules, the adversarial acceptance gate for every feature, and the implementation backlog from strategy construction through evidence/monitoring. It is an execution index, not permission to build duplicate subsystems.

## Repository-native visual previews

Open the five files in `references/ui-authority/previews/`:

1. `cockpit-home.webp` — preview of canonical `cockpit-home.png`
2. `order-flow-signals-models.webp` — preview of canonical `order-flow-signals-models.png`
3. `test-validate-dashboard.webp` — preview of canonical `test-validate-dashboard.png`
4. `evolutionary_search_trading_dashboard.webp` — preview of canonical `evolutionary_search_trading_dashboard.png`
5. `indicators-models-catalog.webp` — preview of canonical `indicators-models-catalog.png`

These WebP files are deliberately small repository-visible previews derived from the recovered canonical PNGs. They are not replacements for the source assets and must not be used as byte-level authority.

The exact canonical PNG identities, dimensions, byte counts, and SHA-256 digests are pinned in `references/ui-authority/manifest.json`. If a full-resolution PNG is later copied into `references/ui-authority/screenshots/`, verify it against that manifest; fail closed on any mismatch.

Panel/state reference captures supplied by the operator belong under:

- `references/ui-authority/panel-snapshots/`

Follow that folder's README and manifest rules. Preserve original bytes and report conflicts rather than silently normalizing them.

## Engine consumer contract

The prototype exposes these product consumers:

```text
strategy construction / signals
        ↓
candidate generation
        ↓
evolutionary search
        ↓
initial backtest
        ↓
Fast robustness
        ↓
Golden robustness
        ↓
scenario / stress / OOS
        ↓
prop simulation
        ↓
evidence / monitoring
```

This diagram identifies consumers; it is **not a mandatory runtime sequence**. Initial Test is optional, Fast and Golden are independent peer lanes, scenario/stress/OOS composition comes from backend plan authority, and Prop Simulation is optional. See `IMPLEMENTATION_CHECKLIST.md` for the binding rules.

Do not infer that every stage is already implemented. Recover and connect existing engine paths first, then address concrete missing execution seams. Evolution/search scores are discovery outputs, not validation evidence.

## Current blocking implementation milestone

Finish the genuine stored-strategy Phase-01 positive-control proof first:

stored strategy → canonical stored hypothesis → existing compiler → exact `CandidateSpec` → existing `phase01_intake` → `surviving_real == [spec_id]` with no drop → same proof through packaged worker boundary.

Do not move the TraderCockpit worker lock until the accepted replacement artifact exists, its digest is verified, and the packaged worker passes the proof.
