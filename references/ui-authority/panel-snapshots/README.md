# Panel Snapshot Reference Intake

This folder is the repository reference location for **panel-level and state-level TraderCockpit snapshots** supplied by the operator. These captures complement the five canonical full-screen UI baselines; they do not replace them.

## Upload timing

Panel snapshots can be uploaded **now**. Keep the original image files intact. Do not resize, recolor, crop away state context, or regenerate them before intake unless the source itself is already a crop of a specific panel.

## Planned organization

Create subfolders only when a real snapshot exists for that surface:

```text
panel-snapshots/
  home/
  strategy/
  research/
  candidates/
  evolutionary-search/
  validate/
  prop-simulation/
  evidence/
  monitor/
  performance/
  execution-risk/
  governance/
```

Additional subfolders are allowed when the UI authority contains a distinct real workspace not covered above. Do not invent screens just to fill the directory tree.

## File naming

Preferred format:

```text
<surface>--<panel-or-state>--<state>--vNN.<ext>
```

Examples:

```text
validate--fast-pipeline--completed--v01.png
evolutionary-search--pareto-frontier--running--v01.png
strategy--signals-models--confluence--v01.png
home--pipeline-overview--mixed-state--v01.png
```

If the supplied source filename already carries meaningful identity, preserve it and record the normalized role in the manifest instead of renaming destructively.

## Manifest record required per accepted snapshot

When snapshots are added, create/update `panel-snapshots/manifest.json` with at least:

- repository path
- original filename
- surface/workspace
- panel/state represented
- source dimensions
- byte count
- SHA-256
- whether the capture is full screen, panel crop, modal, menu or state variant
- relationship to one of the five canonical baselines, when applicable
- notes about prototype-only values or intentionally illustrative data

## Authority rule

The five canonical full-screen images pinned by `../manifest.json` remain the historical product UI authority. Panel snapshots refine interaction/state understanding. If a panel snapshot appears to contradict the canonical lineage, preserve both and flag the conflict for operator review; do not silently choose one.

## Implementation use

Panel snapshots are reference evidence for connecting the UI to real backend capabilities. They must not be treated as proof that a producer or operation already exists. Before implementing a visible control from a snapshot, locate the backend producer/capability and apply the global adversarial gate in `/IMPLEMENTATION_CHECKLIST.md`.
