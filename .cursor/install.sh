#!/usr/bin/env bash
# Idempotent repository bootstrap for the TraderCockpit Cloud Agent environment.
# Safe to re-run: installs the canonical package and browser-acceptance tooling
# against the checked-out source without rewriting lockfiles or product state.
set -euo pipefail

cd "$(dirname "$0")/.."

# The canonical dev/CI commands (package.json "dev", README, Product Runtime
# Acceptance) invoke `python`. The base image ships Python 3.12 as `python3`,
# so provide the `python` alias when it is missing.
if ! command -v python >/dev/null 2>&1; then
  sudo apt-get update
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends python-is-python3
fi

# Install the one canonical application package. It has no runtime dependencies;
# --no-deps matches Product Runtime Acceptance exactly.
python -m pip install --no-deps -e .

# Browser-acceptance tooling used by tests/run-browser-regression.mjs. Pinned to
# the version Product Runtime Acceptance uses so local runs match CI.
npm install --no-save playwright@1.62.1
npx playwright install --with-deps chromium
