# Windows desktop acceptance runbook

Hand-off instructions for the Windows desktop: pull current `main`, run the desktop
against the authorized installed StrategyQuant X 144.2953 runtime, exercise the real
user path, verify that the cockpit drives the real native producer (not an imitation of
it), and bring back an assessment. This is an acceptance procedure for the
executable-native authority rules in `AGENTS.md`. It is not a roadmap, not a second
product spine, and not permission to implement a substitute discovery engine or
live-broker stack.

Checkout `main`. Do not implement Settings path pickers, browser-chosen `sqx_home`, or
a second live-market producer. Report findings.

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
git checkout main
git pull origin main
git log -1 --oneline
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[desktop,ml]"
```

Record the head. It must be current `main`. Do not check out a parallel desktop
feature branch.

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

Screenshot the first screen. It must be the live/current Cockpit Home (rail, ticker, eight
zones: Market Overview, System Status, Alpha Stack, Pipeline Overview, Signals, Risk,
Performance, Quick Actions), not the Approval Board workflow cards and not a placeholder shell.

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
- `assistant.tools.approved` includes `retrieve_quant_guild` plus the product tools
  `navigate_surface`, `draft_idea_revision`, `propose_specification_fields`,
  `request_compile`, `request_launch`. `assistant.tools.native_mutation` = `false`.
- `assistant.voice.native_mutation` = `false`. Speak stays enabled; missing mic or STT
  is `unavailable` / `provider_not_configured`, not a second assistant.
- `assistant.spend_boundary.provider_enforced` = `false` (expected until consumer account authority).
- `assistant.knowledge.status` = `ready` with a non-zero `entry_count`.
- `market_data.status` = `unavailable` (expected; no live provider yet).

Negative test: set `$env:SQX_LAUNCHER_SHA256` to 64 zeros, restart, and confirm
`execution.launcher_verified` = `false` and every native Launch/Run control in the UI is disabled
with a `trusted_launcher_*` reason. Settings and Home System Status must show the
`sqx_launcher_hash_mismatch` recovery copy (`SQX_LAUNCHER_SHA256` / restore the authorized
launcher) and must not expose a filesystem path or a bind control. Unset `SQX_HOME` (or point it
at a missing directory), restart, and confirm `research_backend.reason_code` =
`runtime_not_configured` with recovery copy that names `SQX_HOME` / `--sqx-home` and says the
browser cannot choose the path. Restore the real hash and `SQX_HOME` and restart.

## 5. Click path (the real user flow)

Work top to bottom and screenshot each numbered step.

### 5.1 Home

Top chips show `Compute: Ready · StrategyQuant X 144.2953`. Card 7 System Health shows the
research backend ready and native execution available. Card 8 Assistant greets
"Good day, Trader." with `z-ai/glm-5.3-flash via openrouter`. The card lists
`Knowledge library: Quant-Guild · <n> references` and
`Approved tools: retrieve_quant_guild, navigate_surface, draft_idea_revision, propose_specification_fields, request_compile, request_launch · confirm mutations · backend only`. Ask it:
"Is the native SQX runtime configured and what is in custody?" Expect a grounded answer
(runtime ready, empty custody). Ask a Quant-Guild topic (Sharpe or walk-forward). The
reply may cite a catalog title/URL; it must not invent statistics or paste a lecture
transcript. Product-tool proposals (navigate / draft Idea / Specification / compile /
launch) must not write custody or invoke `sqcli` until Confirm. Report both replies verbatim.

### 5.2 Research → Signals & Models → Overview

Create an Idea (title, draft, source), save, revise once. Two revisions appear; the rail
"Research progress" becomes 1/6.

### 5.3 Signals & Models → Signals & Models tab

The Native Strategy Specification must show the real installed Builder task: symbol, timeframe,
date range, test precision, engine, the 536 blocks with the actual enabled selection, money
management, GA settings, ranking conditions, cross-check flags. Open SQX → Builder project →
Settings and compare five values side by side: symbol, dateFrom/dateTo, population size,
generations, one ranking condition. Any difference is a defect; report both values.

The price chart draws producer OHLC bars only. The Historical Result picker overlays native
Portfolio fills on those bars when a completed result is selected; with no selection the chart
stays idle and draws no markers. Do not expect invented fills, alias matching, or quotes used
as candles.

### 5.4 Research → Evolutionary Search

The strip and cards bind `BuildMode` / `Rankings` from the **approved** configuration
executable XML, not the live installed task. After Compile → Approve, Random Discovery vs
Genetic Evolution must match that approved XML. Genetic-only operators and islands stay
hidden in Random Discovery. With no approved configuration, Search Mode is Unavailable.

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
`Avg. Trades Per Month` uses the chart-history span from result `settings.xml` Setup
`dateFrom`/`dateTo` when exactly one dated Setup exists; otherwise it uses the traded span and
reports `months_basis`. Compare both the SQX databank value and the cockpit `months_basis`.
Report each stage's state (`Pass | Fail | Incomplete | Not run`) and hover the check dots to
read which native condition passed or failed.

### 5.9 Test & Validate → Robustness

The Robustness catalog is producer-backed from the native CrossChecks subtree. Only Higher
Precision is launchable. Additional markets / Monte Carlo / walk-forward / What-If / permutation
report profile presence and feed stages when a native result exists. `WF*` and confidence-level
Monte Carlo stay `unevaluated` until the result archive carries those producer-recorded columns;
then they evaluate from those values. The cockpit must not recompute them.

Run **Higher Precision**. Verify a second archive in the isolated project results;
`/api/research/historical-results` (`list-robustness`) shows the run with precision and an engine
SHA equal to `Get-FileHash internal\libs\SQTradingLib.jar`. Back on Overview, stages 2 (Fast
Validation) and 3 (Golden Validation) now carry verdicts.

### 5.10 Test & Validate → Evidence

Create the Proof (Idea + Historical Result + validation). Bookmark the `proofEntity` URL.
Hop Signals → Evolutionary Search → Test & Validate: `configuration` / `proofEntity` /
`validationRef` / `historicalResult` must survive Research chrome hops. Home Quick Actions must start without
leftover IDs. Then close the desktop completely, relaunch without `--start-path`: the last
registered path including those custody IDs must restore. `--start-path /home` must win over
the saved session. Paste the Proof URL: the same Proof renders with identical revision hashes.

### 5.11 Explore / Automation / Operate / Settings

Truthful states: Explore shows `Native research producer: Ready 144.2953` and
`Models & assistant: Ready`, data feeds and extensions not configured; Operate and Automation
show not-connected states with no numbers. Settings → Native research runtime is readback
only (expected/observed build, launcher trust, execution gate, fail-closed recovery copy).
There is no browser path picker. Binding remains process-side (`SQX_HOME` / `--sqx-home` /
data-root `native-runtime.json`). Do not add a discovery UI during this acceptance pass.

### 5.12 Research → Indicators & Models → Models

After a completed Historical Result exists (5.6): Fit one allowlisted sklearn family
(Logistic regression / Decision tree / Random forest / Gradient boosting) on that exact
result identity. GET `/api/research/models` must never require loading a pickle. Bind the
fitted catalog digest onto the imported Candidate. Candidates list shows
`Bound model sha256 …`. The Candidate archive SHA must stay equal to the `.sqx` on disk.
A POST with an extra `path` key must be refused. SQX still owns backtest and robustness.

## 6. Backend fidelity checks

Run these while and after 5.4–5.9 and keep the outputs:

```powershell
curl http://127.0.0.1:4173/api/sqx-builder-config | python -m json.tool > builder-config.json
curl http://127.0.0.1:4173/api/sqx-outputs | python -m json.tool > outputs.json
curl http://127.0.0.1:4173/api/research/native-jobs | python -m json.tool > jobs.json
curl http://127.0.0.1:4173/api/research/candidates | python -m json.tool > candidates.json
curl http://127.0.0.1:4173/api/research/historical-results | python -m json.tool > results.json
curl "http://127.0.0.1:4173/api/research/historical-results?entityId=<entity from results.json>" | python -m json.tool > result-detail.json
curl http://127.0.0.1:4173/api/research/models | python -m json.tool > models.json
curl http://127.0.0.1:4173/api/assistant | python -m json.tool > assistant.json
curl http://127.0.0.1:4173/api/desktop/session | python -m json.tool > session.json
```

What must hold:

- Every SHA-256 the cockpit reports (source project, executable XML, candidate archive, result
  archive, engine jar, launcher) equals `Get-FileHash` of the corresponding file on disk at that
  moment. List each pair.
- `result-detail.json` → `trades_readback.payload.trades` count equals the SQX Trades list count;
  `cockpit_verdict.payload.statistics.full` matches the SQX databank columns as in 5.8;
  `cockpit_verdict.payload.native_conditions.state` = `available` and the listed conditions equal
  the Rankings / Higher Precision acceptance conditions visible in SQX Builder settings.
- `assistant.json` → `tools.approved` includes retrieve plus the five product tools and
  `native_mutation` is false. Confirming a launch still requires an approved configuration
  and the trusted gateway. `models.json` catalog detail mentions bind onto an existing
  Candidate, not a pickle.
- `session.json` path is a registered surface; extra query keys and malformed custody IDs are
  refused if you POST them.
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

- the JSON files from section 6, the `.sqx` result archive(s) produced, and SQX's own exported
  report for the same strategy (Results → right-click → Export);
- `%TRADERCOCKPIT_DATA_ROOT%` zipped (custody records; no secrets are stored there);
- the desktop console output and `%SQX_HOME%\user\log\*` from the session;
- timings: Builder launch → first strategy; Retester start → result archive; Higher Precision
  start → archive;
- anything that felt wrong in the UI flow (where you had to guess what to click).

The assessment feeds the next refinement pass: statistics/verdict mismatches against SQX's own
columns, gateway behaviour around the real process lifecycle, and surfaces where the flow was
unclear.
