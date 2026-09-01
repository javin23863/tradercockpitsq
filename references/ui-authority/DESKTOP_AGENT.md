# Desktop Agent: TraderCockpit UI Authority

Start here before changing the engine or frontend framing.

The product UI authority is the recovered multicolor ESQ TraderCockpit prototype. It supersedes the earlier dark-blue `Chart / Backtest / Proof` shell.

## Repository-native visual previews

Open the five files in `references/ui-authority/previews/`:

1. `cockpit-home.webp` — preview of canonical `cockpit-home.png`
2. `order-flow-signals-models.webp` — preview of canonical `order-flow-signals-models.png`
3. `test-validate-dashboard.webp` — preview of canonical `test-validate-dashboard.png`
4. `evolutionary_search_trading_dashboard.webp` — preview of canonical `evolutionary_search_trading_dashboard.png`
5. `indicators-models-catalog.webp` — preview of canonical `indicators-models-catalog.png`

These WebP files are deliberately small repository-visible previews derived from the recovered canonical PNGs. They are not replacements for the source assets and must not be used as byte-level authority.

The exact canonical PNG identities, dimensions, byte counts, and SHA-256 digests are pinned in `references/ui-authority/manifest.json`. If a full-resolution PNG is later copied into `references/ui-authority/screenshots/`, verify it against that manifest; fail closed on any mismatch.

## Engine consumer contract

The prototype establishes this concrete product flow:

```text
strategy construction / signals
        ↓
candidate generation
        ↓
evolutionary search
        ↓
initial backtest
        ↓
Fast robustness
        ↓
Golden robustness
        ↓
scenario / stress / OOS
        ↓
prop simulation
        ↓
evidence / monitoring
```

Do not infer that every stage is already implemented. Recover and connect existing engine paths first, then address concrete missing execution seams. Evolution/search scores are discovery outputs, not validation evidence.
