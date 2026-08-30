# TraderCockpit execution rules

These rules apply to work on the production and consolidation lines.

- The primary assistant owns planning, architecture, review, repository inspection, code correction, and acceptance.
- Do every task possible in the current environment before delegating anything to a desktop or other agent.
- Delegate only a narrowly defined residual operation that genuinely requires unavailable local files, OS-specific software, GUI access, hardware, credentials, or another inaccessible runtime.
- Do not send repository archaeology, GitHub inspection, defect identification, code correction, or planning to a sub-agent when those can be done here.
- Review the actual diff and executable evidence after any external work; do not accept status reports as proof.
- Keep the production line production-only. Do not merge `sources/**`, `references/**`, recovered vendor trees, donor-repository material, or runtime-experiment artifacts into production code.
- `javin23863/futures` is quarantined. Do not inspect, recover from, copy from, test against, depend on, or use it as an acceptance gate unless the user explicitly reverses this rule.
- SQX extraction/parity/runtime-smoke/plugin branches are reference or experimental lanes. Production may consume only deliberately reviewed behavior through TraderCockpit-owned contracts; branch names, recovered classes, and runtime experiments are not capability authority.
- `codex/repo-consolidation` is the current cleanup spine. Do not merge historical branches wholesale into it. Transplant only reviewed production changes that preserve the existing product kernel and accepted UI authority.
- Preserve accepted UI checkpoints as immutable evidence. Reconcile their reviewed composition into the cleanup spine without importing stale checklists or reference trees.
- Do not create replacement engines, fake evaluators, synthetic pass results, fabricated market data, or substitute identity objects.
- Unsupported or unconnected capability must fail closed and remain visibly unavailable rather than being simulated.
- Avoid speculative abstractions and duplicate systems. Prefer the smallest change that connects or corrects an existing implementation.
- When concurrent work exists, use an isolated branch/worktree. Never reset, clean, stash, overwrite, or switch away unknown local changes from another lane.
- Do not create documentation-only commits unless documentation is itself the requested cleanup/authority correction.
- One phase at a time. Do not expand scope to solve adjacent historical or validation problems unless they are required for the current acceptance target.
