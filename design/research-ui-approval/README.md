# TraderCockpit Research UI approval prototype

Status: **interactive design candidate — not production UI**

This standalone prototype exists so the Research interaction model can be reviewed before backend/frontend implementation is committed.

It intentionally:

- does not call TraderCockpit APIs;
- does not mutate SQX;
- uses deterministic representative values;
- demonstrates the fixed Research hierarchy and the proposed Specification workspace;
- demonstrates Simple / Detailed / Native disclosure;
- distinguishes strategy architecture, rule grammar, Random vs Genetic search, Selection sequencing, and Validation families;
- includes a dedicated Monte Carlo laboratory;
- keeps Builder/Custom Project ownership separate.

Run locally from this directory with any static server, for example:

```bash
python -m http.server 8765
```

Then open `http://127.0.0.1:8765/`.

Approval of this prototype means approval of the interaction structure and visual direction only. It does not establish native SQX setting semantics that have not yet been observed from the installed producer/read model.
