"""Stripe $150/month membership — Checkout Session create/retrieve, secret-free read model."""

from __future__ import annotations

from datetime import datetime, timezone
import json
import os
from pathlib import Path
from typing import Callable, Mapping
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen

from tradercockpit.consumer_account import signed_in_account_id, signed_in_email


STRIPE_SECRET_KEY_ENV = "STRIPE_SECRET_KEY"
STRIPE_PRICE_ID_ENV = "STRIPE_PRICE_ID"
STRIPE_PRICE_AMOUNT_ENV = "STRIPE_PRICE_AMOUNT"
STRIPE_SUCCESS_URL_ENV = "STRIPE_SUCCESS_URL"
STRIPE_CANCEL_URL_ENV = "STRIPE_CANCEL_URL"

STRIPE_API_BASE = "https://api.stripe.com/v1"
STRIPE_MEMBERSHIP_NAME = "stripe-membership.json"
STRIPE_MEMBERSHIP_SCHEMA = "tc.stripe-membership.v1"
BILLING_API_PATH = "/api/account/billing"
BILLING_CHECKOUT_PATH = "/api/account/billing/checkout"
BILLING_SUCCESS_PATH = "/api/account/billing/success"
BILLING_CANCEL_PATH = "/api/account/billing/cancel"

DEFAULT_PRICE_AMOUNT_CENTS = 15000
DEFAULT_PRICE_CURRENCY = "usd"
DEFAULT_PRICE_INTERVAL = "month"

StripeTransport = Callable[[str, str, dict[str, str], bytes | None], tuple[int, bytes]]


def _is_loopback_host(host: str) -> bool:
    if host in {"localhost", "127.0.0.1", "::1"}:
        return True
    try:
        from ipaddress import ip_address

        return ip_address(host.split("%", 1)[0]).is_loopback
    except ValueError:
        return False


def _loopback_url(raw: str, label: str) -> str:
    parsed = urlparse(raw)
    if parsed.scheme != "http" or not parsed.hostname or not _is_loopback_host(parsed.hostname):
        raise ValueError(f"{label} must be a loopback http URL")
    if not parsed.path or parsed.path == "/":
        raise ValueError(f"{label} must include a path")
    return raw


def stripe_membership_path(data_root: Path | str) -> Path:
    return Path(data_root) / STRIPE_MEMBERSHIP_NAME


def _stripe_secret(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    return (source.get(STRIPE_SECRET_KEY_ENV) or "").strip()


def _stripe_price_id(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    return (source.get(STRIPE_PRICE_ID_ENV) or "").strip()


def stripe_checkout_configured(environ: Mapping[str, str] | None = None) -> bool:
    if not _stripe_secret(environ):
        return False
    if not _stripe_price_id(environ):
        return False
    try:
        stripe_success_url(environ)
        stripe_cancel_url(environ)
    except ValueError:
        return False
    return True


def stripe_success_url(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    raw = (source.get(STRIPE_SUCCESS_URL_ENV) or "").strip()
    if not raw:
        raise ValueError(
            f"Set {STRIPE_SUCCESS_URL_ENV} to a loopback http URL "
            f"(for example http://127.0.0.1:4173{BILLING_SUCCESS_PATH}?session_id={{CHECKOUT_SESSION_ID}})."
        )
    return _loopback_url(raw, STRIPE_SUCCESS_URL_ENV)


def stripe_cancel_url(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    raw = (source.get(STRIPE_CANCEL_URL_ENV) or "").strip()
    if not raw:
        raise ValueError(
            f"Set {STRIPE_CANCEL_URL_ENV} to a loopback http URL "
            f"(for example http://127.0.0.1:4173{BILLING_CANCEL_PATH})."
        )
    return _loopback_url(raw, STRIPE_CANCEL_URL_ENV)


def configured_price_amount_cents(environ: Mapping[str, str] | None = None) -> int:
    """Documented membership amount (15000 cents = $150/month USD). Checkout uses STRIPE_PRICE_ID."""

    source = environ if environ is not None else os.environ
    raw = (source.get(STRIPE_PRICE_AMOUNT_ENV) or "").strip()
    if not raw:
        return DEFAULT_PRICE_AMOUNT_CENTS
    try:
        amount = int(raw)
    except ValueError as exc:
        raise ValueError(f"{STRIPE_PRICE_AMOUNT_ENV} must be an integer number of cents") from exc
    if amount <= 0:
        raise ValueError(f"{STRIPE_PRICE_AMOUNT_ENV} must be positive")
    return amount


def _load_membership(data_root: Path | str | None) -> dict[str, object]:
    if data_root is None:
        return {}
    path = stripe_membership_path(data_root)
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(payload, dict) or payload.get("schema") != STRIPE_MEMBERSHIP_SCHEMA:
        return {}
    account_id = payload.get("account_id")
    if not isinstance(account_id, str) or not account_id.strip():
        return {}
    return payload


def _write_membership(data_root: Path | str, payload: dict[str, object]) -> None:
    path = stripe_membership_path(data_root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _urllib_stripe_request(
    method: str,
    path: str,
    secret_key: str,
    headers: dict[str, str],
    data: bytes | None,
) -> tuple[int, bytes]:
    request = Request(
        f"{STRIPE_API_BASE}{path}",
        data=data,
        headers=headers,
        method=method,
    )
    try:
        with urlopen(request, timeout=15) as response:  # noqa: S310 - fixed Stripe API host
            return int(response.status), response.read()
    except HTTPError as extra:
        return int(extra.code), extra.read()
    except URLError as extra:
        raise RuntimeError(f"Stripe API unreachable: {extra.reason}") from extra
    except TimeoutError as extra:
        raise RuntimeError("Stripe API timed out") from extra


def _stripe_request(
    method: str,
    path: str,
    secret_key: str,
    *,
    form: dict[str, str] | None = None,
    transport: StripeTransport | None = None,
) -> dict[str, object]:
    headers = {"Authorization": f"Bearer {secret_key}", "Accept": "application/json"}
    body: bytes | None = None
    if form is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        body = urlencode(form).encode("utf-8")
    if transport is None:
        status, raw = _urllib_stripe_request(method, path, secret_key, headers, body)
    else:
        status, raw = transport(method, path, headers, body)
    if status >= 400:
        raise RuntimeError(f"Stripe API request failed ({status})")
    try:
        payload = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as extra:
        raise RuntimeError("Stripe API returned non-JSON") from extra
    if not isinstance(payload, dict):
        raise RuntimeError("Stripe API returned a malformed body")
    return payload


def _subscription_active(status: object) -> bool:
    return isinstance(status, str) and status in {"active", "trialing"}


def _period_end_iso(payload: dict[str, object]) -> str | None:
    end = payload.get("current_period_end")
    if isinstance(end, int) and end > 0:
        return datetime.fromtimestamp(end, tz=timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    return None


def _membership_record_from_subscription(
    account_id: str,
    customer_id: str,
    subscription: dict[str, object],
) -> dict[str, object]:
    status = subscription.get("status")
    membership_status = "active" if _subscription_active(status) else "inactive"
    record: dict[str, object] = {
        "schema": STRIPE_MEMBERSHIP_SCHEMA,
        "account_id": account_id,
        "stripe_customer_id": customer_id,
        "stripe_subscription_id": subscription.get("id"),
        "status": membership_status,
        "updated_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
    }
    period_end = _period_end_iso(subscription)
    if period_end:
        record["current_period_end"] = period_end
    return record


def refresh_membership_from_stripe(
    data_root: Path | str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> dict[str, object] | None:
    secret = _stripe_secret(environ)
    if not secret:
        return None
    stored = _load_membership(data_root)
    subscription_id = stored.get("stripe_subscription_id")
    account_id = stored.get("account_id")
    customer_id = stored.get("stripe_customer_id")
    if not isinstance(subscription_id, str) or not subscription_id.strip():
        return stored or None
    if not isinstance(account_id, str) or not isinstance(customer_id, str):
        return stored or None
    subscription = _stripe_request("GET", f"/subscriptions/{subscription_id.strip()}", secret, transport=transport)
    record = _membership_record_from_subscription(account_id, customer_id, subscription)
    _write_membership(data_root, record)
    return record


def _persist_checkout_session(
    data_root: Path | str,
    account_id: str,
    session: dict[str, object],
) -> dict[str, object]:
    customer_id = session.get("customer")
    subscription_raw = session.get("subscription")
    subscription_id: str | None = None
    subscription_obj: dict[str, object] | None = None
    if isinstance(subscription_raw, dict):
        subscription_obj = subscription_raw
        sub_id = subscription_raw.get("id")
        subscription_id = sub_id if isinstance(sub_id, str) else None
    elif isinstance(subscription_raw, str):
        subscription_id = subscription_raw.strip() or None
    if not isinstance(customer_id, str) or not customer_id.strip():
        raise RuntimeError("Stripe checkout session returned no customer id")
    if not subscription_id:
        raise RuntimeError("Stripe checkout session returned no subscription id")
    if subscription_obj is not None:
        record = _membership_record_from_subscription(account_id, customer_id.strip(), subscription_obj)
    else:
        payment_status = session.get("payment_status")
        checkout_status = session.get("status")
        active = payment_status == "paid" and checkout_status == "complete"
        record = {
            "schema": STRIPE_MEMBERSHIP_SCHEMA,
            "account_id": account_id,
            "stripe_customer_id": customer_id.strip(),
            "stripe_subscription_id": subscription_id,
            "status": "active" if active else "inactive",
            "updated_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        }
    _write_membership(data_root, record)
    return record


def complete_stripe_checkout(
    data_root: Path | str,
    session_id: str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> dict[str, object]:
    secret = _stripe_secret(environ)
    if not secret:
        raise ValueError("Stripe checkout requires STRIPE_SECRET_KEY")
    account_id = signed_in_account_id(data_root)
    if not account_id:
        raise ValueError("Stripe checkout completion requires a verified Google session")
    clean_session_id = session_id.strip()
    if not clean_session_id:
        raise ValueError("Stripe checkout completion requires session_id")
    session = _stripe_request(
        "GET",
        f"/checkout/sessions/{clean_session_id}?expand[]=subscription",
        secret,
        transport=transport,
    )
    metadata = session.get("metadata")
    referenced = metadata.get("account_id") if isinstance(metadata, dict) else None
    client_reference = session.get("client_reference_id")
    bound = referenced if isinstance(referenced, str) and referenced.strip() else client_reference
    if not isinstance(bound, str) or bound.strip() != account_id:
        raise ValueError("Stripe checkout session account_id mismatch")
    return _persist_checkout_session(data_root, account_id, session)


def begin_stripe_checkout(
    data_root: Path | str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> str:
    secret = _stripe_secret(environ)
    price_id = _stripe_price_id(environ)
    if not secret or not price_id:
        raise ValueError("Stripe checkout requires STRIPE_SECRET_KEY and STRIPE_PRICE_ID")
    account_id = signed_in_account_id(data_root)
    if not account_id:
        raise ValueError("Stripe checkout requires a verified Google session")
    email = signed_in_email(data_root)
    success_url = stripe_success_url(environ)
    cancel_url = stripe_cancel_url(environ)
    fields = {
        "mode": "subscription",
        "success_url": success_url,
        "cancel_url": cancel_url,
        "client_reference_id": account_id,
        "metadata[account_id]": account_id,
        "line_items[0][price]": price_id,
        "line_items[0][quantity]": "1",
    }
    if email:
        fields["customer_email"] = email
    session = _stripe_request("POST", "/checkout/sessions", secret, form=fields, transport=transport)
    checkout_url = session.get("url")
    if not isinstance(checkout_url, str) or not checkout_url.strip():
        raise RuntimeError("Stripe checkout session returned no url")
    return checkout_url.strip()


def membership_status_record(
    data_root: Path | str | None = None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    """Secret-free membership readiness for /api/status and Settings."""

    amount = configured_price_amount_cents(environ)
    # Pricing is presented only on the Stripe-hosted checkout page. Read-model
    # detail strings never render a price into the product UI.
    if not stripe_checkout_configured(environ):
        detail = (
            f"Set {STRIPE_SECRET_KEY_ENV}, {STRIPE_PRICE_ID_ENV}, "
            f"{STRIPE_SUCCESS_URL_ENV}, and {STRIPE_CANCEL_URL_ENV} "
            f"to enable membership checkout."
        )
        return {
            "status": "unavailable",
            "reason_code": "checkout_not_configured",
            "detail": detail,
            "price_amount_cents": amount,
            "price_currency": DEFAULT_PRICE_CURRENCY,
            "price_interval": DEFAULT_PRICE_INTERVAL,
        }

    account_id = signed_in_account_id(data_root)
    if not account_id:
        return {
            "status": "unavailable",
            "reason_code": "not_signed_in",
            "detail": "Sign in with Google before subscribing.",
            "checkout_path": BILLING_CHECKOUT_PATH,
            "price_amount_cents": amount,
            "price_currency": DEFAULT_PRICE_CURRENCY,
            "price_interval": DEFAULT_PRICE_INTERVAL,
        }

    stored = _load_membership(data_root)
    if stored and stored.get("account_id") == account_id and stored.get("status") == "active":
        record: dict[str, object] = {
            "status": "ready",
            "reason_code": None,
            "detail": "Membership active.",
            "membership_status": "active",
            "price_amount_cents": amount,
            "price_currency": DEFAULT_PRICE_CURRENCY,
            "price_interval": DEFAULT_PRICE_INTERVAL,
        }
        period_end = stored.get("current_period_end")
        if isinstance(period_end, str) and period_end.strip():
            record["current_period_end"] = period_end
        return record

    return {
        "status": "unavailable",
        "reason_code": "inactive",
        "detail": "No active membership for this account.",
        "membership_status": "inactive",
        "checkout_path": BILLING_CHECKOUT_PATH,
        "price_amount_cents": amount,
        "price_currency": DEFAULT_PRICE_CURRENCY,
        "price_interval": DEFAULT_PRICE_INTERVAL,
    }


def billing_read_response(
    data_root: Path | str | None,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> tuple[int, dict[str, object]]:
    if data_root is not None and _load_membership(data_root).get("stripe_subscription_id"):
        try:
            refresh_membership_from_stripe(data_root, environ, transport=transport)
        except RuntimeError:
            pass
    record = membership_status_record(data_root, environ)
    return 200, {"schema": "tc.account-billing.v1", **record}


def billing_checkout_response(
    data_root: Path | str | None,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> tuple[int, str | dict[str, object]]:
    if data_root is None:
        return 503, {
            "error": "unavailable",
            "reason_code": "session_store_unbound",
            "detail": "Stripe checkout requires the application data root.",
        }
    if not signed_in_account_id(data_root):
        return 401, {
            "error": "account_not_signed_in",
            "reason_code": "account_not_signed_in",
            "detail": "Sign in with Google before starting checkout.",
        }
    try:
        return 302, begin_stripe_checkout(data_root, environ, transport=transport)
    except ValueError as exc:
        message = str(exc)
        if "Google session" in message:
            return 401, {
                "error": "account_not_signed_in",
                "reason_code": "account_not_signed_in",
                "detail": message,
            }
        return 503, {
            "error": "checkout_not_configured",
            "reason_code": "checkout_not_configured",
            "detail": message,
        }
    except RuntimeError as exc:
        return 503, {
            "error": "checkout_failed",
            "reason_code": "checkout_failed",
            "detail": str(exc),
        }


def billing_checkout_post_response(
    data_root: Path | str | None,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> tuple[int, dict[str, object] | str]:
    status, payload = billing_checkout_response(data_root, environ, transport=transport)
    if status == 302 and isinstance(payload, str):
        return 200, {"schema": "tc.account-billing-checkout.v1", "checkout_url": payload}
    if isinstance(payload, dict):
        return status, payload
    return status, {"error": "invalid_state", "detail": "Unexpected checkout response"}


def billing_success_response(
    data_root: Path | str | None,
    session_id: str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: StripeTransport | None = None,
) -> tuple[int, str | dict[str, object]]:
    if data_root is None:
        return 503, {
            "error": "unavailable",
            "reason_code": "session_store_unbound",
            "detail": "Stripe checkout completion requires the application data root.",
        }
    if not session_id.strip():
        return 400, {
            "error": "invalid_request",
            "reason_code": "checkout_session_missing",
            "detail": "Billing success requires session_id.",
        }
    try:
        complete_stripe_checkout(data_root, session_id, environ, transport=transport)
    except ValueError as exc:
        message = str(exc)
        if "Google session" in message or "account_id mismatch" in message:
            return 409, {
                "error": "account_not_signed_in",
                "reason_code": "account_not_signed_in",
                "detail": message,
            }
        return 400, {
            "error": "invalid_request",
            "reason_code": "checkout_session_invalid",
            "detail": message,
        }
    except RuntimeError as exc:
        return 503, {
            "error": "checkout_failed",
            "reason_code": "checkout_failed",
            "detail": str(exc),
        }
    return 302, "/settings?membership=active"


def billing_cancel_response() -> tuple[int, str]:
    return 302, "/settings?membership=canceled"
