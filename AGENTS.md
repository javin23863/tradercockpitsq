# TraderCockpit execution rules

These rules apply to work on this product branch.

- The primary assistant owns planning, architecture, review, repository inspection, code correction, and acceptance.
- Do every task possible in the current environment before delegating anything to a desktop or other agent.
- Delegate only a narrowly defined residual operation that genuinely requires unavailable local files, OS-specific software, GUI access, hardware, credentials, or another inaccessible runtime.
- Do not send repository archaeology, GitHub inspection, defect identification, code correction, or planning to a sub-agent when those can be done here.
- Review the actual diff and executable evidence after any external work; do not accept status reports as proof.
- Keep this product line production-only. Do not merge `sources/**`, `references/**`, recovered vendor trees, or donor-repository material into it.
- Do not inspect or depend on another repository unless the user explicitly changes scope.
- Do not create replacement engines, fake evaluators, synthetic pass results, fabricated market data, or substitute identity objects.
- Unsupported or unconnected capability must fail closed and remain visibly unavailable rather than being simulated.
- Avoid speculative abstractions and duplicate systems. Prefer the smallest change that connects or corrects an existing implementation.
- Do not create documentation-only commits unless the user explicitly asks for documentation work.
- One phase at a time. Do not expand scope to solve adjacent historical or validation problems unless they are required for the current acceptance target.
