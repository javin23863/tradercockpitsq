# Consumer OpenRouter Account Authority v1

## Status

This document is a binding companion to the native-SQX product spine while PR #35 is being finalized.

It defines only the **consumer account, external-LLM transport, model-routing, and credit-allocation boundary**. It does not change StrategyQuant X's authority over strategy construction, Builder, backtesting, robustness, optimization, or native strategy intelligence.

## Product decision

TraderCockpit will reuse the proven **OpenRouter consumer-workhorse concept** from the earlier Futures / TraderCockpit application design rather than requiring consumers to configure a personal model-provider account.

The product flow is:

```text
consumer
  -> Google sign-in to TraderCockpit
  -> verified TraderCockpit account identity
  -> configured starter / plan credit allowance
  -> per-consumer OpenRouter credential or equivalent server-enforced spend boundary
  -> centrally selected efficient default model
  -> external-LLM assistance where the product actually needs it
```

The consumer does **not** sign into the operator's personal setup and does not receive the operator's master OpenRouter credential.

## Proven design lineage

The earlier TraderCockpit consumer application already established the relevant concepts:

1. A provider-neutral OpenAI-compatible workhorse transport whose backend is selected by one configuration row, with OpenRouter as the public/default provider lane.
2. An operator-held OpenRouter provisioning credential that can create a **per-customer key** with provider-enforced `limit`, `limit_reset`, and `expires_at` controls.
3. Customer model credentials stored in OS credential custody rather than exposed in browser code.
4. Google sign-in / membership verification as the consumer identity gate, composed with the OpenRouter credential lane.
5. Spend enforcement at the provider boundary so a broken local UI or client counter cannot silently create unlimited model spend.

TraderCockpitSQ should reuse that architecture concept. Do not copy personal credentials, customer records, or machine-specific setup from the earlier application.

## Google account boundary

Google OAuth authenticates the **consumer to TraderCockpit**. It is not an OpenRouter login and it does not authorize access to the consumer's Google data beyond the minimum identity scopes required by the product.

The intended account lifecycle is:

```text
first verified Google sign-in
  -> create / resolve TraderCockpit consumer account
  -> assign configured product entitlement
  -> grant configured initial model-credit allowance
  -> provision or associate the consumer's bounded OpenRouter spend authority
```

The exact starter-credit amount, renewal cadence, paid-plan allowance, and grace policy are product configuration. Engineering must not invent those commercial values in source code.

A consumer's Google identity must map to one stable internal account subject. Email text is useful for login/support presentation but should not become an unversioned substitute for the internal subject identity.

## OpenRouter credential and credit boundary

The operator/application owns the OpenRouter provisioning authority. Consumers must never receive that management credential.

Preferred enforcement is the proven per-consumer-key pattern:

- one bounded OpenRouter key/sub-key per active consumer or membership period;
- explicit spend limit;
- explicit reset behavior when the product plan uses recurring credits;
- explicit expiry when the entitlement period ends;
- revocation/disable path when membership ends or abuse requires cutoff;
- plaintext model credential shown only at creation to trusted backend provisioning code and then placed in secure credential custody;
- no OpenRouter secret in browser code, logs, source, fixtures, or public configuration.

TraderCockpit may also maintain an internal credit/read-model ledger for UX, receipts, support, and plan accounting, but that ledger is not the sole hard spending control. The provider-enforced OpenRouter limit remains the external money ceiling.

OpenRouter's returned usage/cost data should be recorded against the internal consumer subject so the UI can show remaining allowance and the backend can reconcile product credits with provider spend.

## Model-routing policy

The consumer application should use the **most cost-efficient model that satisfies the required capability and quality bar**.

At this checkpoint, the default OpenRouter workhorse is:

- **Z.ai GLM 5.3 Flash**

This is a routing policy, not a permanent code constant. The exact OpenRouter model identifier, provider preference, fallback list, and limits belong in backend configuration/capability policy so the product can change the default without a frontend rewrite when pricing or model quality changes.

Rules:

- ordinary consumer LLM work starts on the configured efficient default;
- do not silently route routine work to an expensive flagship model;
- escalation to another model requires a capability/quality reason defined by backend policy;
- model selection is never controlled by browser-supplied arbitrary provider credentials;
- every routed request remains attributable to the consumer account and its allowance.

## Relationship to native SQX intelligence

OpenRouter is the **external-LLM transport and billing fabric**, not a replacement trading/research engine.

The authority hierarchy remains:

```text
native SQX engine / AlgoWizard / Builder / validation
    = strategy and quantitative producer authority

OpenRouter workhorse
    = consumer-facing external LLM transport where assistance is needed

sqx-lab or other approved SQX extensions
    = optional native-artifact tooling that may use the external LLM lane

TraderCockpit
    = account, orchestration, custody, approval, routing, control, readback and UI
```

Therefore an OpenRouter model may help interpret an idea, assist an approved extension, summarize native results, or operate through exposed tools, but it must not become a second TraderCockpit-owned Builder, backtester, robustness engine, optimizer, or source of fabricated SQX results.

## Browser/backend boundary

The browser owns:

- Google sign-in initiation/state presentation;
- account/plan/remaining-credit presentation;
- user prompt/input;
- streamed response presentation;
- approval UI for any consequential native SQX action.

The trusted backend owns:

- Google token verification and internal-subject mapping;
- account entitlement state;
- OpenRouter provisioning/credential custody;
- credit/spend policy;
- model selection and fallback policy;
- usage accounting;
- tool authorization;
- SQX/native capability invocation.

The browser must never hold the OpenRouter provisioning credential or decide its own spending ceiling.

## Acceptance

This consumer LLM/account lane is not complete until a clean consumer flow proves:

```text
Google sign-in
  -> stable TraderCockpit account
  -> configured starter/plan allowance
  -> bounded OpenRouter spend authority
  -> request routed through configured GLM 5.3 Flash default
  -> usage/cost attributed to that consumer
  -> remaining allowance updated
  -> spend refuses cleanly at the configured limit
  -> sign-out / lapse / revocation cannot continue spending
```

A developer's personal OpenRouter key, a shared uncapped key, a browser-stored secret, a mock credit counter with no external spend ceiling, or a hard-coded permanent model choice does not satisfy this gate.
