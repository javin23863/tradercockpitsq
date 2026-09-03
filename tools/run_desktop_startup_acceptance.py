#!/usr/bin/env python3
"""Real-machine packaged desktop integration acceptance.

Proves TraderCockpit is the visible product on ordinary startup, that installed
SQX discovery does not launch SQX, and that an explicit trusted native action is
what crosses into sqcli.exe. Does not submit Builder ``start`` or Robustness.
"""

from __future__ import annotations

import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import subprocess
import sys
import time
from urllib.request import urlopen

from tradercockpit.native_runtime_config import write_native_runtime_config
from tradercockpit.sqx_gateway import SqxNativeControlGateway
from tradercockpit.sqx_presets import verified_sqx_home
from tradercockpit.sqx_runtime import SQX_LAUNCHER_RELATIVE_PATH


_SQX_PROCESS_NAMES = frozenset(
    {
        "strategyquantx.exe",
        "strategyquantx_ui.exe",
        "sqcli.exe",
    }
)


def _process_names() -> set[str]:
    completed = subprocess.run(
        [
            "powershell",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            "Get-CimInstance Win32_Process | Select-Object -ExpandProperty Name",
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    return {line.strip().casefold() for line in completed.stdout.splitlines() if line.strip()}


def _sqx_running() -> set[str]:
    return {name for name in _process_names() if name in _SQX_PROCESS_NAMES}


def _process_tree(pid: int) -> list[dict[str, str]]:
    script = (
        f"$root = {pid}; "
        "$rows = @(); "
        "Get-CimInstance Win32_Process | ForEach-Object { "
        "  if ($_.ProcessId -eq $root -or $_.ParentProcessId -eq $root) { "
        "    $rows += [pscustomobject]@{ pid = $_.ProcessId; ppid = $_.ParentProcessId; name = $_.Name; command = $_.CommandLine } "
        "  } "
        "}; "
        "$rows | ConvertTo-Json -Compress"
    )
    completed = subprocess.run(
        ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
        check=True,
        capture_output=True,
        text=True,
    )
    raw = completed.stdout.strip()
    if not raw:
        return []
    payload = json.loads(raw)
    if isinstance(payload, dict):
        return [payload]
    return list(payload)


def _wait_http(url: str, *, timeout_seconds: float, needle: str) -> str:
    deadline = time.monotonic() + timeout_seconds
    last: Exception | None = None
    while time.monotonic() < deadline:
        try:
            with urlopen(url, timeout=2) as response:
                body = response.read().decode("utf-8", errors="replace")
                if needle in body:
                    return body
                last = RuntimeError("response did not contain expected TraderCockpit marker")
        except Exception as exc:  # bounded poll of a starting desktop
            last = exc
        time.sleep(0.25)
    raise RuntimeError(f"TraderCockpit loopback was not ready at {url}") from last


def _discover_sqx_home(explicit: Path | None) -> Path:
    candidates: list[Path] = []
    if explicit is not None:
        candidates.append(explicit)
    env = os.environ.get("SQX_HOME")
    if env:
        candidates.append(Path(env))
    candidates.extend(Path.home().glob("Downloads/SQX_*/"))
    candidates.append(Path("C:/StrategyQuantX144"))
    seen: set[Path] = set()
    errors: list[str] = []
    for candidate in candidates:
        resolved = candidate.expanduser()
        if resolved in seen:
            continue
        seen.add(resolved)
        try:
            return verified_sqx_home(resolved)
        except Exception as exc:
            errors.append(f"{resolved}: {exc}")
    raise RuntimeError("could not verify an installed SQX 144.2953 home: " + "; ".join(errors))


def _json_get(url: str) -> dict[str, object]:
    with urlopen(url, timeout=5) as response:
        return json.loads(response.read().decode("utf-8"))


def _launch_desktop(
    exe: Path,
    *,
    port: int,
    data_root: Path,
    sqx_home: Path | None = None,
    launcher_sha256: str | None = None,
) -> subprocess.Popen[bytes]:
    command = [
        str(exe),
        "--port",
        str(port),
        "--start-path",
        "/home",
        "--data-root",
        str(data_root),
        "--title",
        "TraderCockpit",
    ]
    if sqx_home is not None:
        command.extend(["--sqx-home", str(sqx_home)])
    if launcher_sha256:
        command.extend(["--sqx-launcher-sha256", launcher_sha256])
    return subprocess.Popen(command, cwd=str(exe.parent))


def _stop_desktop(process: subprocess.Popen[bytes]) -> None:
    pid = process.pid
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
    subprocess.run(
        ["taskkill", "/F", "/PID", str(pid), "/T"],
        check=False,
        capture_output=True,
        text=True,
    )


def _stop_sqx_if_started(started: bool) -> None:
    if not started:
        return
    for image in ("StrategyQuantX.exe", "StrategyQuantX_ui.exe"):
        subprocess.run(
            ["taskkill", "/F", "/IM", image],
            check=False,
            capture_output=True,
            text=True,
        )


def _assert_tradercockpit_home(origin: str) -> str:
    return _wait_http(
        f"{origin}/home",
        timeout_seconds=45,
        needle="<title>TraderCockpit</title>",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Packaged TraderCockpit desktop integration acceptance")
    parser.add_argument("--exe", type=Path, required=True)
    parser.add_argument("--data-root", type=Path, required=True)
    parser.add_argument("--port", type=int, default=4174)
    parser.add_argument("--sqx-home", type=Path, default=None)
    parser.add_argument("--skip-explicit-native", action="store_true")
    parser.add_argument("--skip-sqx-already-running", action="store_true")
    args = parser.parse_args(argv)

    exe = args.exe.expanduser().resolve()
    if exe.name != "TraderCockpit.exe":
        raise SystemExit(f"acceptance requires TraderCockpit.exe, got {exe.name}")
    data_root = args.data_root.expanduser().resolve()
    data_root.mkdir(parents=True, exist_ok=True)
    origin = f"http://127.0.0.1:{args.port}"
    sqx_home = _discover_sqx_home(args.sqx_home)
    launcher = (sqx_home / SQX_LAUNCHER_RELATIVE_PATH).resolve()
    launcher_sha256 = sha256(launcher.read_bytes()).hexdigest()
    write_native_runtime_config(data_root, sqx_home=sqx_home, launcher_sha256=launcher_sha256)

    report: dict[str, object] = {
        "schema": "tc.desktop-startup-acceptance.v1",
        "exe": str(exe),
        "data_root": str(data_root),
        "sqx_home": str(sqx_home),
        "tests": {},
    }

    sqx_before = _sqx_running()
    desktop = _launch_desktop(
        exe,
        port=args.port,
        data_root=data_root,
    )
    try:
        body = _assert_tradercockpit_home(origin)
        if "<title>TraderCockpit</title>" not in body:
            raise SystemExit("Launching TraderCockpit did not serve the TraderCockpit frontend")
        sqx_after_start = _sqx_running()
        spawned = sqx_after_start - sqx_before
        if spawned:
            raise SystemExit(
                "SQX launched during ordinary TraderCockpit startup: "
                + ", ".join(sorted(spawned))
            )
        status = _json_get(f"{origin}/api/status")
        research = status["research_backend"]
        if research.get("verified") is not True:
            raise SystemExit(f"installed SQX was not recognized: {research}")
        if _sqx_running() - sqx_before:
            raise SystemExit("SQX launcher started during discovery/status")
        report["tests"]["test1_startup"] = {
            "result": "Launching TraderCockpit displayed TraderCockpit.",
            "sqx_launched_during_ordinary_startup": False,
            "sqx_already_running": bool(sqx_before),
            "url": f"{origin}/home",
            "pid": desktop.pid,
            "process_tree": _process_tree(desktop.pid),
        }
        report["tests"]["test2_discovery"] = {
            "result": "status recognized installed SQX without launching it",
            "producer": research.get("producer"),
            "build": research.get("build"),
        }
        if sqx_before:
            report["tests"]["test4_sqx_already_running"] = {
                "result": "TraderCockpit opened its own UI while SQX was already running",
                "sqx_processes": sorted(sqx_after_start),
            }

        if not args.skip_explicit_native:
            gateway = SqxNativeControlGateway(sqx_home, launcher_sha256, timeout_seconds=30)
            staged = sqx_home / "user" / "tmp" / "tc-desktop-explicit-loadconfig.cfx"
            staged.parent.mkdir(parents=True, exist_ok=True)
            config = b"<Task type=\"Build\" name=\"Build\" taskXMLFile=\"Build-Task1.xml\"/>\n"
            staged.write_bytes(config)
            before = _sqx_running()
            context = gateway._preflight(staged, sha256(config).hexdigest())
            command = gateway._builder_command(context, "loadconfig")
            completed = gateway.runner(
                list(command),
                cwd=str(context.home),
                stdin=subprocess.DEVNULL,
                capture_output=True,
                text=True,
                timeout=float(gateway.timeout_seconds),
                check=False,
                shell=False,
            )
            after = _sqx_running()
            still_home = _assert_tradercockpit_home(origin)
            if "<title>TraderCockpit</title>" not in still_home:
                raise SystemExit("TraderCockpit UI was replaced after explicit native action")
            report["tests"]["test3_explicit_native"] = {
                "result": "explicit trusted loadconfig invoked the installed SQX launcher",
                "command0": command[0],
                "exit_code": completed.returncode,
                "sqx_processes_before": sorted(before),
                "sqx_processes_after": sorted(after),
                "launcher_sha256": launcher_sha256,
            }
            if Path(command[0]).name.casefold() != "sqcli.exe":
                raise SystemExit("explicit native action did not use sqcli.exe")
    finally:
        _stop_desktop(desktop)

    started_sqx_for_test4 = False
    if not args.skip_sqx_already_running and "test4_sqx_already_running" not in report["tests"]:
        if not _sqx_running():
            sqx_exe = sqx_home / "StrategyQuantX.exe"
            if not sqx_exe.is_file():
                raise SystemExit(f"TEST 4 needs {sqx_exe}")
            subprocess.Popen([str(sqx_exe)], cwd=str(sqx_home))
            started_sqx_for_test4 = True
            deadline = time.monotonic() + 45
            while time.monotonic() < deadline and not _sqx_running():
                time.sleep(0.5)
            if not _sqx_running():
                _stop_sqx_if_started(True)
                raise SystemExit("TEST 4 could not start installed StrategyQuant X")
        overlapping = _launch_desktop(
            exe,
            port=args.port,
            data_root=data_root,
        )
        try:
            _assert_tradercockpit_home(origin)
            if not _sqx_running():
                raise SystemExit("TEST 4 expected installed SQX to remain running")
            report["tests"]["test4_sqx_already_running"] = {
                "result": "TraderCockpit opened its own UI while SQX was already running",
                "sqx_processes": sorted(_sqx_running()),
            }
        finally:
            _stop_desktop(overlapping)
            _stop_sqx_if_started(started_sqx_for_test4)

    restarted = _launch_desktop(
        exe,
        port=args.port,
        data_root=data_root,
    )
    try:
        _assert_tradercockpit_home(origin)
        report["tests"]["test5_restart"] = {
            "result": "TraderCockpit UI returned and reused the canonical data root",
            "data_root": str(data_root),
        }
    finally:
        _stop_desktop(restarted)

    receipt = data_root / "desktop-startup-acceptance.json"
    receipt.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, sort_keys=True))
    print("Launching TraderCockpit displayed TraderCockpit.")
    print("SQX did not launch during ordinary startup.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
