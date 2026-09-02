# Windows desktop acceptance runbook

Hand-off instructions for the Windows desktop agent: pull the branch, run the desktop against
the authorized installed StrategyQuant X 144.2953 runtime, exercise the real user path, verify
that the cockpit drives the real native producer (not an imitation of it), and bring back an
assessment. This is an acceptance procedure for the executable-native authority rules in
`AGENTS.md`; it is not a roadmap and does not change `LIVING_IMPLEMENTATION_PLAN.md`.

## 0. Prerequisites on the Windows machine

- Windows 10/11, Python 3.12 on `PATH`, Git, Microsoft Edge WebView2 Runtime.
- StrategyQuant X **build 144.2953** installed, licensed, and launched at least once as the
  logged-in user (so `user\projects\Builder` and `user\projects\Retester` exist).
- The SQX install root (`%SQX_HOME%`, e.g. `C:\StrategyQuantX`) contains: `sqcli.exe`,
  `internal\web\SQUANT\build.dat`, `internal\SQUANT.dat`, `internal\libs\SQTradingLib.jar`,
  `user\projects\Builder\project.cfx`, `user\projects\Retester\project.cfx`.
- Historical data for the Builder task's configured symbol/timeframe is loaded in SQX Data
  Manager. If it is not, native runs must fail for that real reason, and that failure is part of
  what we want to observe.
- An OpenRouter API key for the Assistant.

## 1. Pull and install

```powershell
git clone https://github.com/javin23863/tradercockpitsq.git
cd tradercockpitsq
git fetch origin cursor/recovery-ui-authority-5d85
git checkout cursor/recovery-ui-authority-5d85
git log -1 --oneline
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[desktop]"
```

Sanity without SQX:

```powershell
python tools/check_production_boundary.py
python -m unittest discover -s tests/product
```

Both must pass. Record the head commit and the test count.

## 2. Environment (one PowerShell session)

```powershell
$env:SQX_HOME = "C:\StrategyQuantX"
$env:SQX_LAUNCHER_SHA256 = (Get-FileHash "$env:SQX_HOME\sqcli.exe" -Algorithm SHA256).Hash.ToLower()
$env:OPENROUTER_API_KEY = "<key>"
$env:TRADERCOCKPIT_WATCHLIST = "ES,NQ"
$env:TRADERCOCKPIT_DATA_ROOT = "C:\tc-acceptance-data"
```

`TRADERCOCKPIT_WATCHLIST` is optional; the ticker stays "provider not configured" because no
live feed exists yet. Use a fresh `TRADERCOCKPIT_DATA_ROOT` so custody starts empty. Record
`$env:SQX_LAUNCHER_SHA256` and the contents of `internal\web\SQUANT\build.dat` (expect `2953`).

## 3. Launch

Real desktop shell:

```powershell
tradercockpit-desktop
```

Server mode for API inspection alongside the desktop (same code, same `web/`):

```powershell
python -m tradercockpit.app_server --host 127.0.0.1 --port 4173 --sqx-home $env:SQX_HOME --data-root $env:TRADERCOCKPIT_DATA_ROOT
```

Then open `http://127.0.0.1:4173/` in Edge. The `curl` commands below assume port 4173; the
desktop shell uses a random loopback port printed as `TraderCockpit listening on …`.

Screenshot the first screen. It must be the prototype Cockpit Home (rail, hero, eight numbered
cards), not a placeholder shell.

## 4. Runtime trust checks before any native click

```powershell
curl http://127.0.0.1:4173/api/status | python -m json.tool
```

Report exactly:

- `research_backend.status` = `ready`, `research_backend.verified` = `true`,
  `research_backend.build` = `144.2953`.
- `research_backend.execution.launcher_verified` = `true`, `gateway_available` = `true`,
  `available` = `true`.
- `research_custody.status` = `ready`.
- `assistant.status` and `provider.status` = `ready`; `model.default_model` = `z-ai/glm-5.3-flash`.
- `market_data.status` = `unavailable` (expected; no live provider yet).

Negative test: set `$env:SQX_LAUNCHER_SHA256` to 64 zeros, restart, and confirm
`execution.launcher_verified` = `false` and every native Launch/Run control in the UI is disabled
with a `trusted_launcher_*` reason. Restore the real hash and restart.

## 5. Click path (the real user flow)

Work top to bottom and screenshot each numbered step.

### 5.1 Home

Top chips show `Compute: Ready · StrategyQuant X 144.2953`. Card 7 System Health shows the
research backend ready and native execution available. Card 8 Assistant greets
"Good day, Trader." with `z-ai/glm-5.3-flash via openrouter`. Ask it:
"Is the native SQX runtime configured and what is in custody?" Expect a grounded answer
(runtime ready, empty custody). Report the reply verbatim.

### 5.2 Research → Signals & Models → Overview

Create an Idea (title, draft, source), save, revise once. Two revisions appear; the rail
"Research progress" becomes 1/6.

### 5.3 Signals & Models → Signals & Models tab

The Native Strategy Specification must show the real installed Builder task: symbol, timeframe,
date range, test precision, engine, the 536 blocks with the actual enabled selection, money
management, GA settings, ranking conditions, cross-check flags. Open SQX → Builder project →
Settings and compare five values side by side: symbol, dateFrom/dateTo, population size,
generations, one ranking condition. Any difference is a defect; report both values.

### 5.4 Research → Evolutionary Search

The strip and cards mirror the same native `BuildMode` / `Rankings` values SQX shows.

Build custody card: **Compile** → expect `exact_native_builder_task_snapshot` and a source
project SHA-256 equal to
`Get-FileHash "$env:SQX_HOME\user\projects\Builder\project.cfx" -Algorithm SHA256`.
**Approve** → a new immutable configuration revision. **Launch Builder** → the first real native
mutation.

Verify during and after Launch:

- The gateway runs exactly `sqcli.exe -project action=loadconfig name=Builder file=<approved xml>`
  followed by `sqcli.exe -project action=start name=Builder`. Confirm in Task Manager that
  `sqcli.exe` / `java` spawned; confirm the desktop console logs the receipts; confirm
  `/api/research/native-jobs` shows one job with both receipts `completed`.
- Open SQX → Builder → Results databank: strategies must be appearing (the Builder is really
  generating). Let it produce at least one result, then stop it in SQX or let the native stop
  condition end it.
- Report time to first strategy, number of strategies, and any SQX error dialog.

### 5.5 Candidates (Evolutionary Search → Top Candidates / Candidate import)

`/api/sqx-outputs` lists the `.sqx` files in `user\projects\Builder\databanks\Results\` with
sizes and SHA-256 equal to `Get-FileHash` on disk. Import one as a Candidate: the Candidate
archive SHA equals the file's SHA. Tamper test: copy a `.sqx`, append a byte with `Add-Content`,
try to import the tampered copy → it must be rejected.

### 5.6 Research → Test & Validate → Initial Test

Run the native Retester on the imported Candidate. Verify:

- a new isolated project `user\projects\TraderCockpit-Retester-<32 hex>\project.cfx` appears
  (not the shared `Retester` project);
- its `Retest-Task1.xml` is the installed Retester task with only the candidate bound;
- SQX shows the project if you open it;
- a result archive lands in `user\projects\TraderCockpit-Retester-…\databanks\Results\`;
- the Historical Result `result_archive_sha256` equals `Get-FileHash` of that file.

### 5.7 Test & Validate → Trades

The trade table equals the Trades list SQX shows for the same strategy (open the result in SQX →
Trades). Compare trade count, first/last open time, first trade P/L, one drawdown value. Note the
SampleType values you see (10 in-sample, 20 out-of-sample, 127 all).

### 5.8 Test & Validate → Overview (cockpit verdict)

After the Retester completes: the KPI strip populates (Profit Factor, Ret/DD, Max Drawdown,
Expectancy), the equity curve draws, the Run & Evidence row shows statistics and a verdict chip,
funnel stage 1 shows pass/fail with check dots.

Compare the statistics to SQX's databank columns for the same strategy: Net profit, Profit
factor, Drawdown, Ret/DD ratio, # of trades, Winning %. They should match to two decimals.
`Avg. Trades Per Month` may differ (cockpit uses the traded span, SQX uses the data range);
report both numbers. Report each stage's state (`Pass | Fail | Incomplete | Not run`) and hover
the check dots to read which native condition passed or failed.

### 5.9 Test & Validate → Robustness

Run **Higher Precision**. Verify a second archive in the isolated project results;
`/api/research/historical-results` (`list-robustness`) shows the run with precision and an engine
SHA equal to `Get-FileHash internal\libs\SQTradingLib.jar`. Back on Overview, stages 2 (Fast
Validation) and 3 (Golden Validation) now carry verdicts.

### 5.10 Test & Validate → Evidence

Create the Proof (Idea + Historical Result + validation). Bookmark the `proofEntity` URL, close
the desktop completely, relaunch, paste the URL: the same Proof renders with identical revision
hashes.

### 5.11 Explore / Automation / Operate / Settings

Truthful states: Explore shows `Native research producer: Ready 144.2953` and
`Models & assistant: Ready`, data feeds and extensions not configured; Operate and Automation
show not-connected states with no numbers.

## 6. Backend fidelity checks

Run these while and after 5.4–5.9 and keep the outputs:

```powershell
curl http://127.0.0.1:4173/api/sqx-builder-config | python -m json.tool > builder-config.json
curl http://127.0.0.1:4173/api/sqx-outputs | python -m json.tool > outputs.json
curl http://127.0.0.1:4173/api/research/native-jobs | python -m json.tool > jobs.json
curl http://127.0.0.1:4173/api/research/candidates | python -m json.tool > candidates.json
curl http://127.0.0.1:4173/api/research/historical-results | python -m json.tool > results.json
curl "http://127.0.0.1:4173/api/research/historical-results?entityId=<entity from results.json>" | python -m json.tool > result-detail.json
```

What must hold:

- Every SHA-256 the cockpit reports (source project, executable XML, candidate archive, result
  archive, engine jar, launcher) equals `Get-FileHash` of the corresponding file on disk at that
  moment. List each pair.
- `result-detail.json` → `trades_readback.payload.trades` count equals the SQX Trades list count;
  `cockpit_verdict.payload.statistics.full` matches the SQX databank columns as in 5.8;
  `cockpit_verdict.payload.native_conditions.state` = `available` and the listed conditions equal
  the Rankings / Higher Precision acceptance conditions visible in SQX Builder settings.
- No cockpit file was written inside `%SQX_HOME%` except
  `user\projects\TraderCockpit-Retester-*` (`Get-ChildItem "$env:SQX_HOME\user\projects"`).
- Kill test: while the Builder is running (5.4), close the desktop. Report whether `sqcli.exe` /
  java kept running or terminated, and what `/api/research/native-jobs` says after relaunch
  (interrupted vs completed).
- Failure test: point the Builder task at a symbol with no data in SQX, approve, launch. Report the
  exact reason code the cockpit shows versus what SQX logs in `user\log`. The cockpit must fail
  closed with the producer's reason and never show a fabricated result.

## 7. What to bring back

For each step: screenshot, pass/fail, and the exact discrepancy. Plus:

- the six JSON files from section 6, the `.sqx` result archive(s) produced, and SQX's own exported
  report for the same strategy (Results → right-click → Export);
- `%TRADERCOCKPIT_DATA_ROOT%` zipped (custody records; no secrets are stored there);
- the desktop console output and `%SQX_HOME%\user\log\*` from the session;
- timings: Builder launch → first strategy; Retester start → result archive; Higher Precision
  start → archive;
- anything that felt wrong in the UI flow (where you had to guess what to click).

The assessment feeds the next refinement pass: statistics/verdict mismatches against SQX's own
columns, gateway behaviour around the real process lifecycle, and surfaces where the flow was
unclear.
