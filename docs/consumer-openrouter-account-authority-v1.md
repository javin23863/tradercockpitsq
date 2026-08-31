# Consumer OpenRouter Account Authority v1

## Status

This document is the binding consumer-account/external-LLM companion to the native-SQX product spine in PR #35.

It defines only the **consumer account, external-LLM transport, model-routing, credit-allocation, and first implementation seam**. It does not change StrategyQuant X's authority over strategy construction, Builder, backtesting, robustness, optimization, or native strategy intelligence.

Read it with `AGENTS.md`, `IMPLEMENTATION_CHECKLIST.md`, `docs/product-architecture-v1.md`, `docs/product-backbone-spec-v1.md`, and `docs/sqx-authoring-authority-v1.md`.

## Product decision

TraderCockpit reuses the proven **OpenRouter consumer-workhorse concept** from earlier Futures/TraderCockpit application work rather than requiring consumers to configure personal model-provider accounts.

```text
consumer
  -> Google sign-in to TraderCockpit
  -> verified stable TraderCockpit account subject
  -> configured starter / plan credit allowance
  -> per-consumer OpenRouter credential or equivalent provider-enforced spend boundary
  -> centrally selected efficient default model
  -> external-LLM assistance where the product needs it
```

The consumer does not sign into the operator's personal setup and never receives the operator's OpenRouter provisioning credential.

## Proven design lineage

Earlier TraderCockpit consumer application work established these reusable concepts:

1. one provider-neutral OpenAI-compatible workhorse transport selected by backend configuration;
2. OpenRouter as the consumer/public provider lane;
3. an operator-held OpenRouter provisioning credential that creates per-customer credentials with provider-enforced `limit`, `limit_reset`, and `expires_at` controls;
4. customer model credentials kept out of browser code and placed in trusted credential custody;
5. Google sign-in/membership verification as the consumer identity gate;
6. provider-side spend enforcement so a broken local UI/counter cannot silently create unlimited inference spend.

TraderCockpitSQ reuses that architecture concept. Do not copy personal credentials, customer records, or machine-specific state from the earlier application.

## Google account boundary

Google OAuth authenticates the **consumer to TraderCockpit**. It is not an OpenRouter login and does not authorize unrelated Google-data access.

```text
first verified Google sign-in
  -> create or resolve stable internal TraderCockpit subject
  -> assign configured product entitlement
  -> grant configured initial model-credit allowance exactly once for that entitlement rule
  -> provision or associate bounded OpenRouter spend authority
```

Rules:

- use only the identity scopes required by the product;
- the internal subject, not mutable email text, is the durable account identity;
- repeated sign-in resolves the same subject;
- repeated sign-in cannot duplicate a one-time starter grant;
- grant idempotency is keyed by stable internal subject plus a configured allowance/grant-policy identity, not by email or login attempt;
- email/profile data is presentation/support metadata only;
- starter amount, renewal cadence, paid-plan allowance and grace rules are product configuration, not implementation guesses.

## Durable account/state pattern

The current TraderCockpit runtime already separates immutable execution facts from mutable current-state pointers. The consumer-account lane follows the same discipline.

Preferred local custody pattern:

- immutable typed account/entitlement/credit events;
- an atomic current-state/head pointer keyed by stable internal account subject;
- exact event ordering/version checks;
- no inference of entitlement or remaining allowance from missing files;
- no mutation of immutable research/content-addressed strategy objects to represent account balance;
- no second application server or separate account daemon merely for this lane.

The account store lives under the canonical TraderCockpit state authority, but remains logically separate from immutable research objects and run lifecycle state. Local state holds product/account/read-model facts and non-secret provider identifiers/hashes only; provider-side spend enforcement remains external.

## OpenRouter credential and credit boundary

The operator/application owns OpenRouter provisioning authority. Consumers never receive that management credential.

Preferred enforcement is the proven per-consumer credential pattern:

- bounded OpenRouter key/sub-key per active consumer or entitlement period;
- explicit provider-side spend limit;
- explicit reset behavior when the product plan uses recurring credits;
- explicit expiry when entitlement is finite;
- disable/revoke path for lapse/account closure/abuse handling;
- plaintext model credential visible only to trusted provisioning/custody code when necessary;
- no OpenRouter secret in browser code, logs, source, fixtures, or public configuration.

TraderCockpit may maintain an internal usage/credit ledger for UX, receipts, support and reconciliation. That ledger is not the sole hard spending control; the provider-enforced OpenRouter limit remains the external money ceiling.

OpenRouter usage/cost is attributed to the internal account subject so remaining allowance and provider spend can be reconciled.

## Model-routing policy

Use the **most cost-efficient model satisfying the required capability/quality bar**.

Current default workhorse policy:

- model slug: `z-ai/glm-5.3-flash`

The slug, provider preference, fallback/escalation list and request limits belong in backend policy/configuration so the product can change routing without a frontend rewrite.

Rules:

- routine consumer LLM work starts on the configured efficient default;
- do not silently route routine work to an expensive flagship model;
- escalation requires backend capability/quality policy;
- browser code cannot provide arbitrary provider credentials or unbounded model IDs;
- every request is attributable to account subject, allowance, selected model and usage/cost.

## Relationship to native SQX intelligence

```text
native SQX AI / AlgoWizard / Builder / validation
    = strategy and quantitative producer authority

OpenRouter workhorse
    = bounded consumer external-LLM transport

sqx-lab / approved SQX extensions
    = optional native-artifact tooling that may use the bounded external-LLM lane

TraderCockpit
    = account, orchestration, custody, approval, routing, control, readback and UI
```

An OpenRouter model may help interpret an idea, assist an approved extension, summarize native results, or operate allowed tools. It must not become a second TraderCockpit Builder, backtester, robustness engine, optimizer, or source of fabricated SQX results.

## Browser/backend boundary

Browser owns:

- Google sign-in initiation/state presentation;
- account/plan/remaining-credit presentation;
- user prompt/input;
- streamed response presentation;
- approval UI for consequential native SQX actions.

Trusted backend owns:

- Google token/code verification and external-identity → internal-subject mapping;
- account entitlement state;
- durable account/credit event custody;
- OpenRouter provisioning/credential custody;
- credit/spend policy;
- model selection/fallback policy;
- usage accounting;
- tool authorization;
- SQX/native capability invocation.

The browser never holds the OpenRouter provisioning credential or decides its own spending ceiling.

## Canonical application seam

The current product has one canonical stdlib application server in `product/tradercockpit/app_server.py`, with pure response helpers and injected collaborators behind one `/api/*` authority. The consumer account/LLM lane extends this server; it does not introduce a second server.

First implementation should add capability-specific collaborators such as:

- account/session service;
- Google identity adapter;
- account-event/current-head store;
- OpenRouter provisioning/spend adapter;
- model-policy/usage service.

HTTP route functions remain thin translations over those services, following the existing `(status, payload)` test pattern and loopback HTTP acceptance pattern.

## First implementation slice — contract/state foundation

The first code slice after PR #35 is accepted is deliberately **not** a full live OAuth/billing rollout. It establishes the invariant-bearing application core without requiring operator secrets.

Required scope:

1. typed stable internal account subject and external Google-subject binding contract;
2. immutable account/allowance event records plus atomic current-state head;
3. explicit grant-policy identity so first/starter grants are idempotent for `(account subject, grant policy)`;
4. configured model-policy record with default `z-ai/glm-5.3-flash`;
5. provider-spend-authority metadata contract containing provider credential identity/hash/limit/reset/expiry state but never plaintext management credentials;
6. account/session/allowance read model;
7. canonical server read endpoints `GET /api/account` and `GET /api/model-policy`; no write/login/provisioning endpoint in this first slice;
8. injected Google/OpenRouter interfaces that fail closed when not configured;
9. regression tests for duplicate starter-grant prevention, subject mismatch, corrupt/missing head state, exhausted/revoked state, model policy coming from backend config, secret absence from wire payloads, and unknown/mutating account API paths refusing rather than implicitly creating state.

`GET /api/account` returns only the current product account/read-model fields needed by the UI. It never returns provider plaintext credentials. `GET /api/model-policy` returns public routing-policy metadata (for example configured workhorse identity/capability status), never the OpenRouter provisioning credential or a consumer bearer key.

Out of scope for this first slice:

- choosing the commercial starter-credit amount;
- shipping a real operator OpenRouter provisioning credential;
- browser Google OAuth UI;
- live Google OAuth callback/exchange;
- live key provisioning;
- live model inference;
- account mutation over unauthenticated browser requests;
- native SQX strategy-authoring changes.

Those follow after the contract/state slice is independently green and reviewed.

## Acceptance

The complete consumer lane is not done until:

```text
Google sign-in
  -> stable TraderCockpit subject
  -> configured starter/plan allowance
  -> bounded OpenRouter spend authority
  -> request routed through configured GLM 5.3 Flash default
  -> usage/cost attributed to that consumer
  -> remaining allowance updated
  -> spend refuses at configured provider limit
  -> sign-out / lapse / revocation cannot continue spending
```

A developer personal OpenRouter key, a shared uncapped key, browser-stored secret, mock-only credit counter, hard-coded commercial allowance, or permanent frontend model choice does not satisfy this gate.
