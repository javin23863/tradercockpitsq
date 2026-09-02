# Windows desktop packaging (M5)

TraderCockpit ships as a frozen `TraderCockpit.exe` PyInstaller host over the canonical
`web/` tree and `tradercockpit.desktop` backend. Packaging installs the **product desktop
identity** only — never StrategyQuant X (`StrategyQuantX.exe`, `sqcli.exe`) and never the
other `%LOCALAPPDATA%\TraderCockpit` product tree.

## Operator flow

1. **Build** the frozen desktop (Windows only):

   ```powershell
   python -m pip install -e ".[desktop,desktop-build]"
   python tools/build_windows_desktop.py
   ```

2. **Package** build + release manifest + zip installer bundle:

   ```powershell
   python tools/package_windows_desktop.py
   ```

   Outputs under `dist/windows/`:

   - `TraderCockpit.exe` — frozen desktop
   - `release/` — staged payload, `release-manifest.json`, `TraderCockpit-Setup.ps1`, zip
   - `TraderCockpit-<version>-win64.zip` — portable installer bundle
   - `TraderCockpit-<version>-Setup.exe` — when `makensis` is on PATH (optional)

3. **Install** (per-user, Start Menu shortcut `TraderCockpitSQ.lnk`):

   ```powershell
   python tools/install_windows_desktop.py --exe dist/windows/TraderCockpit.exe
   ```

   Or from the release bundle:

   ```powershell
   Expand-Archive dist/windows/TraderCockpit-0.1.0-win64.zip -DestinationPath $env:TEMP\tc-setup
   powershell -ExecutionPolicy Bypass -File $env:TEMP\tc-setup\TraderCockpit-Setup.ps1
   ```

   Default install root: `%LOCALAPPDATA%\Programs\TraderCockpitSQ`  
   Writes `install-manifest.json` (`tc.windows-install.v1`) with version and executable SHA-256.

4. **Sign** (operator Authenticode — fail closed when material is missing):

   ```powershell
   python tools/sign_windows_executable.py --exe dist/windows/TraderCockpit.exe
   ```

5. **Update** (trusted payload hash required):

   ```powershell
   python tools/update_windows_desktop.py `
     --payload dist/windows/TraderCockpit.exe `
     --expected-sha256 <sha256-from-release-manifest.json> `
     --version 0.1.0
   ```

   Backs up the previous executable under `.rollback/` and records rollback custody in
   `install-manifest.json`.

6. **Rollback**:

   ```powershell
   python tools/rollback_windows_desktop.py
   ```

## Signing environment (names only)

| Variable | Purpose |
| --- | --- |
| `TRADERCOCKPIT_SIGNING_CERT_PATH` | Operator `.pfx` / certificate file |
| `TRADERCOCKPIT_SIGNING_CERT_PASSWORD` | Certificate password |
| `TRADERCOCKPIT_SIGNTOOL_PATH` | Optional override for `signtool.exe` |
| `TRADERCOCKPIT_SIGNING_TIMESTAMP_URL` | Optional RFC 3161 timestamp server |

When signing material is absent, tools report `signing_not_configured` and do **not** claim a
production-signed artifact.

## Post-install configuration (unchanged from stacked account/billing)

Packaging does not embed secrets. Operators configure at runtime:

- `TRADERCOCKPIT_DATA_ROOT` (optional override)
- `SQX_HOME`, `SQX_LAUNCHER_SHA256` (native SQX boundary)
- Google consumer auth (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `TRADERCOCKPIT_SESSION_SECRET`)
- Stripe membership (`STRIPE_*`)
- OpenRouter assistant/credits (`OPENROUTER_API_KEY`, `OPENROUTER_MANAGEMENT_KEY`, optional `OPENROUTER_CREDIT_LIMIT_USD`)

## Not in this slice

- M6 Windows Idea→Proof acceptance on real installed SQX (`docs/windows-desktop-acceptance-runbook.md`)
- DualRuntimeProof greens, live broker, or a substitute SQX engine
- Auto-update CDN / phone-home updater
