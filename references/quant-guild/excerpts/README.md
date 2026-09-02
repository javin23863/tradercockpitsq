# Excerpt writing rules

Each excerpt file is a short grounding note. It states:

- the lecture ids it cites (see `../feature-lecture-map.json`);
- the mathematical claim that later product code must honor;
- the retail-runnable method (CPU, sklearn / numpy / hmmlearn);
- what the product must **not** invent.

Excerpts are **not** copies of Quant-Guild notebooks. They are the implementer's
contract with that teaching library. If a later agent needs more detail, it
must fetch the lecture at the pinned commit rather than guessing.
