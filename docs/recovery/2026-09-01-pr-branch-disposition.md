# PR and Branch Disposition (2026-09-01)

Historical recovery evidence. Irreversible actions (merge/close/delete/protection) are owner
actions; this records the recommended disposition and exact commands. Recorded SHAs and merge
status are from `origin` at audit time.

## Open pull requests

| PR | Branch | Base | Disposition | Reason |
|---|---|---|---|---|
| #72 Research end-to-end vertical | `research/workflow-correction-integrity-audit` | `main` | KEEP backend; do NOT adopt its UI as authority | Substantial, valuable custody/native execution/readback work. Its UI is still the dark-blue shell. Land the backend; the accepted UI is restored by #76. Let its exact-head acceptance (#805) finish for evidence; merge only after review, then reconnect its read models to the accepted surfaces (M1). |
| #74 Home capability cockpit | `ui/capability-cockpit-home` | `research/…` (stacked) | CLOSE; mine mechanics only | Wrong level of correction; it demotes the Home zones the pinned authority actually keeps. Not the visual authority. Reuse any useful capability-coverage wiring, then close. |
| #73 Cloud Agent environment | `cursor/setup-dev-environment-5d85` | `codex/sqx-engine-extract` (stale) | CLOSE; recreated on `main` | Based on a stale non-`main` base. The same `.cursor/environment.json` is recreated on the `main`-based recovery branch #76, so an old extraction branch never becomes setup authority. |
| #75 Recovery UI-authority evidence | `docs/fable-visual-recovery-handoff` | `main` | KEEP as evidence; folded into #76 | Its `references/ui-authority/**` is restored into #76. Keep #75 as the evidence trail or close once #76 lands. |
| #76 Recovery (this work) | `cursor/recovery-ui-authority-5d85` | `main` | ACTIVE | The canonical recovery: docs reconciliation, UI-authority restoration, shell/Home rebuild. |

## Branches

### Merged into `main` — safe to delete (history preserved on `main`)

All `checkpoint/*`, `archive/*`, `docs/*`, `research/capability-coverage-inventory`,
`research/vertical-completion`, `noop-should-not-create`, `tmp-same-tree-trigger-unused`, and the
large set of merged `codex/*` lanes (application/runtime, consumer-account, desktop-lifecycle,
native-runtime-trust, product-*, repo-consolidation, research-* that are merged, sqx-* that are
merged, ui-* including `codex/ui-prototype-authority`, `codex/ui-reference-acceptance`,
`codex/ui-signals-models-authority`, `codex/home-market-overview-authority`,
`codex/windows-desktop-packaging`) are ancestors of `main`.

Note: `codex/ui-prototype-authority@53645ac` and the other retained UI-authority branches are
merged; their product authority is now preserved in-tree at `references/ui-authority/` (restored
by #76), so the branches themselves are no longer needed as authorities.

Owner cleanup (delete every remote branch already merged into `main`, excluding protected/active
branches):

```bash
git fetch --prune origin
for b in $(git branch -r --merged origin/main \
  | sed 's# *origin/##' \
  | grep -vE '^(main|HEAD|cursor/recovery-ui-authority-5d85)$'); do
  git push origin --delete "$b"
done
```

### Unmerged — review before deleting

| Branch | Last commit | Disposition | Reason |
|---|---|---|---|
| `research/workflow-correction-integrity-audit` | 2026-09-02 | KEEP (PR #72) | Active canonical candidate. |
| `ui/capability-cockpit-home` | 2026-09-02 | CLOSE (PR #74) | Superseded by #76 authority. |
| `docs/fable-visual-recovery-handoff` | 2026-09-02 | KEEP/CLOSE (PR #75) | Evidence; folded into #76. |
| `cursor/recovery-ui-authority-5d85` | active | KEEP (PR #76) | This recovery. |
| `cursor/setup-dev-environment-5d85` | 2026-09-01 | CLOSE (PR #73) | Stale base; recreated on `main` in #76. |
| `cursor/setup-dev-environment-c0fc` | 2026-09-01 | CLOSE/DELETE | Duplicate environment-only branch; superseded by the `main`-based env config in #76. |
| `codex/sqx-engine-extract` | 2026-09-01 | SUPERSEDE/DELETE | The stale extraction base flagged in the handoff; must not be a product base. |
| `codex/research-ui-impeccable-contract` | 2026-09-01 | EVIDENCE then delete | Contains the runnable Research approval prototype `503ac75:design/research-ui-approval/`; used as the M1 interaction reference, then delete. |
| `codex/research-specification-requirements` | 2026-08-31 | SUPERSEDED/DELETE | Superseded by merged Research work. |
| `codex/research-integrity-correction` | 2026-09-01 | SUPERSEDED/DELETE | Superseded by PR #72's integrity audit. |
| `codex/delivery-integrity-guardrails` | 2026-09-01 | REVIEW | Possible unmerged guardrail work; confirm nothing valuable before delete. |
| `codex/installed-sqx-research-acceptance-runner` | 2026-09-01 | REVIEW | Possible unmerged acceptance-runner work. |
| `codex/merge-native-sqx-robustness-higher-precision` | 2026-09-01 | SUPERSEDED/DELETE | Higher-Precision robustness is merged on `main`. |
| `codex/native-sqx-robustness-higher-precision` | 2026-09-01 | SUPERSEDED/DELETE | Same. |
| `codex/native-sqx-robustness-monte-carlo-trade-manipulation` | 2026-09-01 | REVIEW/SALVAGE | Additional native robustness methods (future M-robustness); salvage the native seam or delete. |
| `codex/native-sqx-robustness-system-parameter-permutation` | 2026-09-01 | REVIEW/SALVAGE | Same. |
| `origin` (stray ref named `origin`) | 2026-09-01 | DELETE | Accidental branch literally named `origin`. |

Delete a specific unmerged branch after confirming it holds nothing worth keeping:

```bash
git push origin --delete <branch-name>
```

## Repository hygiene follow-ups (owner)

- Enable `main` branch protection with required checks (`product-runtime`, `windows-desktop-package`); the agent token cannot set this (403).
- After #76 lands, delete the merged branch set above and close #73/#74 (and #75 if folded).
- Keep exactly one canonical authority set (README, AGENTS, `docs/product-*`, `LIVING_IMPLEMENTATION_PLAN.md`, `references/ui-authority/`); `docs/recovery/` is evidence only.
