# Operator MCP: MetaTrader 5 and TradingView

Personal MCP stays on this machine. The commercial desktop uses the same
producers through backend read models — it does not import those GitHub repos
and it does not place trades from the browser.

## Capability split (no overlap)

| Job | Producer | Where |
|---|---|---|
| Login, ticks, bars, account snapshot | MetaTrader 5 terminal | Product `metatrader.py` + [Qoyyuum MCP](https://github.com/Qoyyuum/mcp-metatrader5-server) for operator chat |
| Live scanner quotes | TradingView public scanner | Product `tradingview.py` (skill from [atilaahmettaner/tradingview-mcp](https://github.com/atilaahmettaner/tradingview-mcp)) |
| Drive your TradingView Desktop (Pine, replay, drawings) | TradingView Desktop + CDP | Operator MCP only: [tradesdontlie/tradingview-mcp](https://github.com/tradesdontlie/tradingview-mcp) |
| Duplicate MT5 MCP | — | Do **not** also install [ariadng/metatrader-mcp-server](https://github.com/ariadng/metatrader-mcp-server); same terminal API |

Order send / `place_trade` stays on the personal MT5 MCP. The product never exposes it.

SQX Dukascopy history stays the Research historical producer. MT5/TradingView bars are consumer/operator live context only.

## Secrets (names only)

Put these in the operator secrets file (`TRADERCOCKPIT_SECRETS_PATH` or Desktop `keys.env`):

```
MT5_LOGIN=
MT5_PASSWORD=
MT5_SERVER=
MT5_TERMINAL_PATH=C:\Program Files\MetaTrader 5\terminal64.exe
MT5_WATCHLIST=EURUSD
TRADINGVIEW_MARKET_DATA=1
TRADINGVIEW_WATCHLIST=NASDAQ:AAPL
```

`MT5_TERMINAL_PATH` is optional when the default install exists. The browser never chooses it.

## Personal Cursor MCP

`tools/write_operator_mcp_config.py` writes `%USERPROFILE%\.cursor\mcp.json` with a wrapper that loads the secrets file, then starts `uvx --from mcp-metatrader5-server mt5mcp`. Credentials stay out of the JSON.

TradingView Desktop MCP is added only if you clone tradesdontlie and set `TRADINGVIEW_MCP_ROOT` to that clone. Launch TradingView with `--remote-debugging-port=9222` first.

## Consumer desktop

The same env names are the consumer hookup. Each consumer uses their own MT5 login/server and optional TradingView scanner flag. `/api/market/quotes` and `/api/market/bars` render the ticker and Research chart series. Live signals stay fail-closed.
