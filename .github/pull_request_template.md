## Delivery authority

Living plan item: `replace-with-plan-item-id`

User path: describe the exact desktop path this slice makes work or advances

Native producer seam: `not-applicable` or name the installed SQX executable/project/task/artifact seam actually used

Native producer acceptance: `not-applicable` | `pending` | `required`

Review class: `intermediate` | `final-prototype`

## Scope

Describe the bounded change. Production implementation PRs target current `main` directly; stacked production PRs are not allowed.

## Truth boundary

State what the producer owns and what TraderCockpit owns. Do not claim native outcome truth that the producer seam does not expose.

## Acceptance

- [ ] Focused tests
- [ ] Product Runtime Acceptance on exact head
- [ ] Browser/desktop acceptance where applicable
- [ ] Installed SQX Acceptance on exact head where native behavior changes
- [ ] Substantive exact-head adversarial review
