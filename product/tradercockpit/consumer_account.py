"""Google consumer account identity — loopback OAuth and secret-free session read model."""

from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
from ipaddress import ip_address
from pathlib import Path
import json
import os
import secrets
from typing import Callable, Mapping
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


GOOGLE_CLIENT_ID_ENV = "GOOGLE_CLIENT_ID"
GOOGLE_CLIENT_SECRET_ENV = "GOOGLE_CLIENT_SECRET"
GOOGLE_REDIRECT_URI_ENV = "GOOGLE_REDIRECT_URI"

GOOGLE_AUTHORIZE_PATH = "/api/account/google/authorize"
GOOGLE_CALLBACK_PATH_DEFAULT = "/api/account/google/callback"
GOOGLE_SIGN_OUT_PATH = "/api/account/sign-out"
GOOGLE_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo"
TRUSTED_GOOGLE_ISSUER = "https://accounts.google.com"

GOOGLE_OAUTH_NAME = "google-oauth.json"
GOOGLE_OAUTH_SCHEMA = "tc.google-oauth.v1"
GOOGLE_SESSION_NAME = "google-session.json"
GOOGLE_SESSION_SCHEMA = "tc.google-session.v1"

TokenTransport = Callable[[str, dict[str, str], bytes | None], tuple[int, bytes]]
UserinfoTransport = Callable[[str, dict[str, str]], tuple[int, bytes]]


def _is_loopback_host(host: str) -> bool:
    if host in {"localhost", "127.0.0.1", "::1"}:
        return True
    try:
        return ip_address(host.split("%", 1)[0]).is_loopback
    except ValueError:
        return False


def _google_credentials(environ: Mapping[str, str] | None = None) -> tuple[str, str]:
    source = environ if environ is not None else os.environ
    client_id = (source.get(GOOGLE_CLIENT_ID_ENV) or "").strip()
    client_secret = (source.get(GOOGLE_CLIENT_SECRET_ENV) or "").strip()
    return client_id, client_secret


def google_oauth_configured(environ: Mapping[str, str] | None = None) -> bool:
    client_id, client_secret = _google_credentials(environ)
    return bool(client_id and client_secret)


def google_authorize_path(environ: Mapping[str, str] | None = None) -> str | None:
    if not google_oauth_configured(environ):
        return None
    try:
        google_callback_uri(environ)
    except ValueError:
        return None
    return GOOGLE_AUTHORIZE_PATH


def google_oauth_path(data_root: Path | str) -> Path:
    return Path(data_root) / GOOGLE_OAUTH_NAME


def google_session_path(data_root: Path | str) -> Path:
    return Path(data_root) / GOOGLE_SESSION_NAME


def _load_google_oauth(data_root: Path | str) -> dict[str, object]:
    path = google_oauth_path(data_root)
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(payload, dict) or payload.get("schema") != GOOGLE_OAUTH_SCHEMA:
        return {}
    return payload


def _write_google_oauth(data_root: Path | str, payload: dict[str, object]) -> None:
    path = google_oauth_path(data_root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _load_google_session(data_root: Path | str | None) -> dict[str, object]:
    if data_root is None:
        return {}
    path = google_session_path(data_root)
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(payload, dict) or payload.get("schema") != GOOGLE_SESSION_SCHEMA:
        return {}
    issuer = payload.get("issuer")
    subject = payload.get("subject")
    account_id = payload.get("account_id")
    if not isinstance(issuer, str) or not isinstance(subject, str) or not isinstance(account_id, str):
        return {}
    if not issuer.strip() or not subject.strip() or not account_id.strip():
        return {}
    return payload


def _write_google_session(data_root: Path | str, payload: dict[str, object]) -> None:
    path = google_session_path(data_root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _account_id(issuer: str, subject: str) -> str:
    digest = sha256(f"{issuer}:{subject}".encode("utf-8")).hexdigest()
    return f"sha256:{digest}"


def google_callback_uri(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    raw = (source.get(GOOGLE_REDIRECT_URI_ENV) or "").strip()
    if not raw:
        raise ValueError("GOOGLE_REDIRECT_URI must be a loopback http URL registered with Google")
    parsed = urlparse(raw)
    if parsed.scheme != "http" or not parsed.hostname or not _is_loopback_host(parsed.hostname):
        raise ValueError("GOOGLE_REDIRECT_URI must be a loopback http URL")
    if parsed.path and parsed.path != "/":
        return raw
    raise ValueError("GOOGLE_REDIRECT_URI must include a callback path")


def google_callback_path(environ: Mapping[str, str] | None = None) -> str:
    try:
        return urlparse(google_callback_uri(environ)).path
    except ValueError:
        return GOOGLE_CALLBACK_PATH_DEFAULT


def _urllib_request(url: str, headers: dict[str, str], data: bytes | None) -> tuple[int, bytes]:
    request = Request(url, data=data, headers=headers, method="POST" if data is not None else "GET")
    try:
        with urlopen(request, timeout=10) as response:  # noqa: S310 - fixed Google endpoints
            return int(response.status), response.read()
    except HTTPError as extra:
        return int(extra.code), extra.read()
    except URLError as extra:
        raise RuntimeError(f"Google OAuth unreachable: {extra.reason}") from extra
    except TimeoutError as extra:
        raise RuntimeError("Google OAuth timed out") from extra


def _parse_json_object(body: bytes) -> dict[str, object]:
    try:
        payload = json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as extra:
        raise RuntimeError("Google OAuth returned non-JSON") from extra
    if not isinstance(payload, dict):
        raise RuntimeError("Google OAuth returned a malformed body")
    return payload


def _google_token_request(
    client_id: str,
    client_secret: str,
    fields: dict[str, str],
    *,
    transport: TokenTransport | None = None,
) -> dict[str, object]:
    send = transport or _urllib_request
    headers = {"Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json"}
    status, body = send(GOOGLE_TOKEN_URL, headers, urlencode(fields).encode("utf-8"))
    if status in {401, 403}:
        raise RuntimeError("Google OAuth rejected the credential")
    if status >= 400:
        raise RuntimeError(f"Google OAuth token exchange failed ({status})")
    payload = _parse_json_object(body)
    access = payload.get("access_token")
    if not isinstance(access, str) or not access.strip():
        raise RuntimeError("Google OAuth returned no access token")
    return payload


def _google_userinfo(access_token: str, *, transport: UserinfoTransport | None = None) -> dict[str, object]:
    send = transport or _urllib_request
    status, body = send(
        GOOGLE_USERINFO_URL,
        {"Authorization": f"Bearer {access_token.strip()}", "Accept": "application/json"},
    )
    if status >= 400:
        raise RuntimeError(f"Google userinfo failed ({status})")
    payload = _parse_json_object(body)
    sub = payload.get("sub")
    email = payload.get("email")
    if not isinstance(sub, str) or not sub.strip():
        raise RuntimeError("Google userinfo returned no subject")
    if not isinstance(email, str) or not email.strip():
        raise RuntimeError("Google userinfo returned no email")
    return payload


def begin_google_oauth(data_root: Path | str, environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    client_id, client_secret = _google_credentials(source)
    if not client_id or not client_secret:
        raise ValueError("Google OAuth requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET")
    redirect_uri = google_callback_uri(source)
    state = secrets.token_urlsafe(32)
    existing = _load_google_oauth(data_root)
    payload: dict[str, object] = {"schema": GOOGLE_OAUTH_SCHEMA, "oauth_state": state}
    refresh = existing.get("refresh_token")
    if isinstance(refresh, str) and refresh.strip():
        payload["refresh_token"] = refresh.strip()
    _write_google_oauth(data_root, payload)
    query = urlencode(
        {
            "client_id": client_id,
            "redirect_uri": redirect_uri,
            "response_type": "code",
            "scope": "openid email profile",
            "state": state,
            "access_type": "offline",
            "prompt": "consent",
        }
    )
    return f"{GOOGLE_AUTHORIZE_URL}?{query}"


def complete_google_oauth(
    data_root: Path | str,
    code: str,
    state: str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: TokenTransport | None = None,
    userinfo_transport: UserinfoTransport | None = None,
) -> None:
    source = environ if environ is not None else os.environ
    client_id, client_secret = _google_credentials(source)
    if not client_id or not client_secret:
        raise ValueError("Google OAuth requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET")
    expected = _load_google_oauth(data_root).get("oauth_state")
    if not isinstance(expected, str) or not expected or state != expected:
        raise ValueError("Google OAuth state mismatch")
    redirect_uri = google_callback_uri(source)
    token_payload = _google_token_request(
        client_id,
        client_secret,
        {"grant_type": "authorization_code", "code": code, "redirect_uri": redirect_uri, "client_id": client_id, "client_secret": client_secret},
        transport=transport,
    )
    access = token_payload.get("access_token")
    if not isinstance(access, str) or not access.strip():
        raise RuntimeError("Google OAuth returned no access token")
    profile = _google_userinfo(access, transport=userinfo_transport)
    subject = str(profile["sub"]).strip()
    email = str(profile["email"]).strip()
    verified_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    session = {
        "schema": GOOGLE_SESSION_SCHEMA,
        "issuer": TRUSTED_GOOGLE_ISSUER,
        "subject": subject,
        "email": email,
        "account_id": _account_id(TRUSTED_GOOGLE_ISSUER, subject),
        "verified_at": verified_at,
    }
    _write_google_session(data_root, session)
    oauth_record: dict[str, object] = {"schema": GOOGLE_OAUTH_SCHEMA}
    refresh = token_payload.get("refresh_token")
    if isinstance(refresh, str) and refresh.strip():
        oauth_record["refresh_token"] = refresh.strip()
    if access.strip():
        oauth_record["access_token"] = access.strip()
    _write_google_oauth(data_root, oauth_record)


def clear_google_session(data_root: Path | str) -> None:
    for path in (google_session_path(data_root), google_oauth_path(data_root)):
        if path.is_file():
            path.unlink()


def signed_in_account_id(data_root: Path | str | None) -> str | None:
    session = _load_google_session(data_root)
    account_id = session.get("account_id")
    if isinstance(account_id, str) and account_id.strip():
        return account_id.strip()
    return None


def signed_in_email(data_root: Path | str | None) -> str | None:
    session = _load_google_session(data_root)
    email = session.get("email")
    if isinstance(email, str) and email.strip():
        return email.strip()
    return None


def account_status_record(
    data_root: Path | str | None = None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    """Secret-free consumer account readiness for /api/status and Settings."""

    authorize_path = google_authorize_path(environ)
    if not google_oauth_configured(environ):
        record: dict[str, object] = {
            "status": "unavailable",
            "reason_code": "provider_not_configured",
            "detail": (
                "Set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, and GOOGLE_REDIRECT_URI "
                "for consumer Google sign-in."
            ),
        }
        if authorize_path:
            record["authorize_path"] = authorize_path
        return record

    try:
        google_callback_uri(environ)
    except ValueError as exc:
        return {
            "status": "unavailable",
            "reason_code": "provider_not_configured",
            "detail": str(exc),
        }

    session = _load_google_session(data_root)
    if session:
        email = session.get("email")
        detail = f"Signed in as {email}." if isinstance(email, str) and email.strip() else "Verified Google consumer session."
        return {
            "status": "ready",
            "reason_code": None,
            "detail": detail,
            "provider": "google",
            "email": email if isinstance(email, str) else None,
            "account_id": session.get("account_id"),
            "verified_at": session.get("verified_at"),
        }

    return {
        "status": "unavailable",
        "reason_code": "signed_out",
        "detail": "No verified Google session. Sign in from Settings.",
        "authorize_path": authorize_path,
    }
