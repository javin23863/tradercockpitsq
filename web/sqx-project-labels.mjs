const PROJECT_DISPLAY_NAMES = Object.freeze({
  "DJ CFD - Dukascopy": "Indices Template",
  "EW FUTURES BREAKOUT H1 - Tradestation": "EW Futures Template H1 Breakout",
  "GBPJPY BREAKOUT H1 - Dukascopy": "Forex Template H1 Breakout",
  "GBPJPY BREAKOUT H4 - Dukascopy": "Forex Template H4 Breakout",
  "GBPUSD H1 - Dukascopy": "Forex Template H1",
  "GOLD BREAKOUT M30 - Dukascopy": "Gold Template M30 Breakout",
  "GOLD H1 CFD - Dukascopy": "Gold indices Template H1",
  "NQ BREAKOUT FUTURES  H1 - Tradestation": "NQ Futures Template H1 Breakout",
  "NQ CFD H1 - Dukascopy": "Indices Template Futures H1",
  "NQ CFD H1 D1 MULTI-TIMEFRAME  - Dukascopy": "Indices Futures H1 D1 Multi TimeFrame"
});

function projectDisplayName(name) {
  return PROJECT_DISPLAY_NAMES[name] || name;
}

function recordDisplayName(record, nativeName) {
  if (typeof record?.display_name === "string" && record.display_name) {
    return record.display_name;
  }
  return projectDisplayName(nativeName);
}

export { PROJECT_DISPLAY_NAMES, projectDisplayName, recordDisplayName };
