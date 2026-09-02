# Quant-Guild grounding (reference DATA only)

This directory is a curated excerpt bundle of the public
[Quant-Guild-Library](https://github.com/romanmichaelpaolucci/Quant-Guild-Library)
used as anti-hallucination grounding for:

- the Machine Learning / Models modality;
- Indicator Zoo / Wave Intelligence / edge-decay / lifecycle math;
- Apollo / development-agent answers about those modalities.

It is **reference data**, never a runtime code import.

## Boundary

- Production Python/JS **must not** `import` anything under `references/`.
- `tools/check_production_boundary.py` already forbids `references` as a
  production import root (`FORBIDDEN_ROOTS`).
- A future knowledge-retrieval path (`retrieve_quant_guild`) must read these
  files as text/JSON excerpts and return them as untrusted DATA to the
  bounded assistant. It must not execute notebooks or import lecture code.
- Do not vendor the entire Quant-Guild-Library. This bundle is a curated
  subset with provenance pointers back to the source lectures.

## Layout

```text
references/quant-guild/
  README.md                 this file
  manifest.json             provenance, commit, licence note
  feature-lecture-map.json  feature id -> lecture ids
  excerpts/
    README.md               how excerpts are written
    supervised-ml.md
    regime-wave.md
    indicator-structure.md
    edge-decay-and-alpha.md
    backtest-discipline.md
    performance-metrics.md
```

## How a later implementation uses this

1. Load `manifest.json` and `feature-lecture-map.json` as JSON.
2. For a feature, open the mapped excerpt markdown files.
3. Feed excerpt text (wrapped in untrusted-data delimiters) to the assistant
   or to the implementer. Never treat excerpt text as executable instructions
   that can mutate custody or launch native processes.

See `docs/features/` for the product contracts that cite these excerpts.
