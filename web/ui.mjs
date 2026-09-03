// Shared presentation primitives for the prototype grammar (cards, chips, KPIs, tables,
// rings, chart frames). Pure functions returning HTML strings; no data fetching here.

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

export function readable(code, fallback = "Unavailable") {
  if (!code) return fallback;
  return String(code).replaceAll("_", " ").replaceAll("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function shortId(value, length = 12) {
  const text = String(value ?? "");
  return text.length > length ? `…${text.slice(-length)}` : text;
}

export function formatInt(value) {
  return Number.isFinite(value) ? Number(value).toLocaleString("en-US") : "—";
}

const ICONS = Object.freeze({
  home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/>',
  research: '<path d="M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 0-3 3z"/><path d="M8 4v16"/><path d="M11 8h5M11 12h5"/>',
  explore: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>',
  automation: '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1"/>',
  operate: '<path d="M12 2 21 7v10l-9 5-9-5V7z"/><path d="M12 12 21 7M12 12v10M12 12 3 7"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
  flask: '<path d="M9 3h6M10 3v6L4.5 19a1 1 0 0 0 .9 1.5h13.2a1 1 0 0 0 .9-1.5L14 9V3"/>',
  code: '<path d="m8 8-4 4 4 4M16 8l4 4-4 4M14 5l-4 14"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  bell: '<path d="M6 8a6 6 0 0 1 12 0v5l2 3H4l2-3z"/><path d="M10 19a2 2 0 0 0 4 0"/>',
  chevron: '<path d="m9 6 6 6-6 6"/>',
  down: '<path d="m6 9 6 6 6-6"/>',
  check: '<circle cx="12" cy="12" r="9"/><path d="m8.5 12 2.5 2.5 4.5-5"/>',
  warn: '<path d="M12 3 2 21h20z"/><path d="M12 10v5M12 18h.01"/>',
  play: '<path d="M7 5v14l12-7z"/>',
  pause: '<path d="M8 5v14M16 5v14"/>',
  stop: '<rect x="6" y="6" width="12" height="12" rx="1"/>',
  star: '<path d="m12 3 2.8 5.8 6.2.9-4.5 4.4 1.1 6.3L12 17.5 6.4 20.4l1.1-6.3L3 9.7l6.2-.9z"/>',
  spark: '<path d="M4 17 9 10l4 4 7-9"/>',
  layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 13 9 5 9-5"/>',
  shield: '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/>',
  chart: '<path d="M4 20V4M4 20h16"/><path d="M8 16v-5M12 16V8M16 16v-3"/>',
  table: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 10h18M9 4v16"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  bot: '<rect x="4" y="7" width="16" height="12" rx="3"/><path d="M12 3v4M9 13h.01M15 13h.01"/>',
  mic: '<path d="M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z"/><path d="M19 11a7 7 0 0 1-14 0M12 18v3"/>',
  copy: '<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V5a1 1 0 0 1 1-1h10"/>',
  dots: '<circle cx="5" cy="12" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="19" cy="12" r="1.5"/>',
  external: '<path d="M14 4h6v6M20 4l-9 9"/><path d="M19 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h5"/>',
  compare: '<path d="M8 3v18M16 3v18M3 8h5M16 8h5M3 16h5M16 16h5"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  list: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
  workspace: '<rect x="3" y="3" width="18" height="18" rx="4"/><path d="M12 8v8M8 12h8"/>',
  crown: '<path d="m3 8 4 4 5-7 5 7 4-4-2 11H5z"/>',
  bookmark: '<path d="M6 3h12v18l-6-4-6 4z"/>',
  activity: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
  target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
});

export function icon(name, { size = 16, className = "" } = {}) {
  const body = ICONS[name] || ICONS.dots;
  return `<svg class="icon ${escapeHtml(className)}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${body}</svg>`;
}

// Tone vocabulary: ready | current | pending | unavailable | error | info | purple | blue | green | orange | red
export function dot(tone = "unavailable") {
  return `<span class="dot tone-${escapeHtml(tone)}" aria-hidden="true"></span>`;
}

export function chip(label, tone = "unavailable", { className = "", attrs = "" } = {}) {
  return `<span class="chip tone-${escapeHtml(tone)} ${escapeHtml(className)}" ${attrs}>${dot(tone)}<span>${escapeHtml(label)}</span></span>`;
}

// Kept for the existing read-model binders which emit `status-badge status-<tone>`.
export function statusBadge(label, tone = "unavailable", extra = "") {
  return `<span class="status-badge status-${escapeHtml(tone)}" ${extra}><span class="status-dot"></span>${escapeHtml(label)}</span>`;
}

export function tag(label, tone = "neutral") {
  return `<span class="tag tone-${escapeHtml(tone)}">${escapeHtml(label)}</span>`;
}

export function linkButton(path, label, { primary = false, iconName = "", disabled = false, className = "", title = "" } = {}) {
  const classes = `button ${primary ? "button-primary" : "button-secondary"} ${disabled ? "button-disabled" : ""} ${className}`.trim();
  const inner = `${iconName ? icon(iconName, { size: 14 }) : ""}<span>${escapeHtml(label)}</span>`;
  if (disabled) {
    return `<span class="${classes}" aria-disabled="true" ${title ? `title="${escapeHtml(title)}"` : ""}>${inner}</span>`;
  }
  return `<a class="${classes}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${inner}</a>`;
}

export function actionButton(label, { primary = false, iconName = "", disabled = false, attrs = "", className = "", title = "" } = {}) {
  return `<button type="button" class="button ${primary ? "button-primary" : "button-secondary"} ${className}" ${disabled ? "disabled" : ""} ${attrs} ${title ? `title="${escapeHtml(title)}"` : ""}>${iconName ? icon(iconName, { size: 14 }) : ""}<span>${escapeHtml(label)}</span></button>`;
}

export function card({
  title,
  sub = "",
  number = null,
  accent = "neutral",
  actions = "",
  body = "",
  footer = "",
  className = "",
  attrs = "",
  headIcon = "",
}) {
  const badge = number === null
    ? (headIcon ? `<span class="card-icon tone-${escapeHtml(accent)}">${icon(headIcon, { size: 15 })}</span>` : "")
    : `<span class="card-number tone-${escapeHtml(accent)}">${escapeHtml(number)}</span>`;
  return `<article class="card accent-${escapeHtml(accent)} ${escapeHtml(className)}" ${attrs}>
    <header class="card-head">${badge}<div class="card-titles"><h2>${escapeHtml(title)}</h2>${sub ? `<p>${escapeHtml(sub)}</p>` : ""}</div>${actions ? `<div class="card-actions">${actions}</div>` : ""}</header>
    <div class="card-body">${body}</div>
    ${footer ? `<footer class="card-foot">${footer}</footer>` : ""}
  </article>`;
}

export function footLink(path, label, { iconName = "chevron", tone = "purple", disabled = false, note = "" } = {}) {
  const inner = `<span>${escapeHtml(label)}</span>${note ? `<small>${escapeHtml(note)}</small>` : ""}${icon(iconName, { size: 14 })}`;
  if (disabled) return `<span class="foot-link tone-${escapeHtml(tone)} is-disabled" aria-disabled="true">${inner}</span>`;
  return `<a class="foot-link tone-${escapeHtml(tone)}" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${inner}</a>`;
}

export function viewAll(path, label = "View all", disabled = false) {
  if (disabled) return `<span class="view-all is-disabled" aria-disabled="true">${escapeHtml(label)}</span>`;
  return `<a class="view-all" href="${escapeHtml(path)}" data-route="${escapeHtml(path)}">${escapeHtml(label)}</a>`;
}

export function kpi({ label, value, delta = "", tone = "neutral", note = "", attrs = "" }) {
  return `<div class="kpi tone-${escapeHtml(tone)}" ${attrs}><span class="kpi-label">${escapeHtml(label)}</span><span class="kpi-value">${escapeHtml(value)}</span>${delta ? `<span class="kpi-delta">${escapeHtml(delta)}</span>` : ""}${note ? `<span class="kpi-note">${escapeHtml(note)}</span>` : ""}</div>`;
}

export function statList(rows) {
  return `<div class="stat-list">${rows.map(([label, value, tone = ""]) => `<div class="stat-row"><span>${escapeHtml(label)}</span><strong class="${tone ? `tone-text-${escapeHtml(tone)}` : ""}">${escapeHtml(value)}</strong></div>`).join("")}</div>`;
}

export function table({ columns, rows, empty = "No rows.", className = "", attrs = "" }) {
  const head = columns.map((column) => `<th class="${column.align === "right" ? "is-right" : ""}">${escapeHtml(column.label)}</th>`).join("");
  const body = rows.length
    ? rows.map((row) => `<tr ${row.attrs || ""}>${row.cells.map((cell, index) => `<td class="${columns[index]?.align === "right" ? "is-right" : ""}">${cell}</td>`).join("")}</tr>`).join("")
    : `<tr class="table-empty"><td colspan="${columns.length}">${escapeHtml(empty)}</td></tr>`;
  return `<div class="table-wrap ${escapeHtml(className)}" ${attrs}><table class="data-table"><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
}

export function ring({ value, label = "", tone = "purple", size = 96, caption = "" }) {
  const pct = Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
  const text = Number.isFinite(value) ? String(Math.round(value)) : "—";
  return `<div class="ring tone-${escapeHtml(tone)} ${Number.isFinite(value) ? "" : "is-empty"}" style="--pct:${pct};--size:${size}px"><div class="ring-track"><div class="ring-inner"><strong>${escapeHtml(text)}</strong>${label ? `<span>${escapeHtml(label)}</span>` : ""}</div></div>${caption ? `<p class="ring-caption">${escapeHtml(caption)}</p>` : ""}</div>`;
}

export function bar(pct, tone = "purple", { label = "", value = "" } = {}) {
  const width = Number.isFinite(pct) ? Math.max(0, Math.min(100, pct)) : 0;
  return `<div class="bar-row"><span class="bar-label">${escapeHtml(label)}</span><div class="bar tone-${escapeHtml(tone)}" role="img" aria-label="${escapeHtml(`${label} ${value}`)}"><i style="width:${width}%"></i></div><span class="bar-value">${escapeHtml(value)}</span></div>`;
}

// Chart frame: axes + grid drawn structurally; series are drawn only when a read model
// provides them (none do yet), otherwise the frame carries an explicit unavailable state.
// Scale numeric series into one SVG path over a fixed viewBox. Shared by the chart frame
// and the compact sparkline so real read-model series draw the same way everywhere.
export function seriesPath(values, { width = 100, height = 100, min = null, max = null } = {}) {
  const numbers = (values || []).map(Number).filter((value) => Number.isFinite(value));
  if (numbers.length < 2) return "";
  const low = min ?? Math.min(...numbers);
  const high = max ?? Math.max(...numbers);
  const span = high - low || 1;
  return numbers.map((value, index) => {
    const x = (index / (numbers.length - 1)) * width;
    const y = height - ((value - low) / span) * height;
    return `${index ? "L" : "M"}${x.toFixed(2)} ${y.toFixed(2)}`;
  }).join(" ");
}

export function candleMarks(bars, { width = 100, height = 100 } = {}) {
  const rows = (bars || []).filter((bar) => (
    bar
    && Number.isFinite(Number(bar.open))
    && Number.isFinite(Number(bar.high))
    && Number.isFinite(Number(bar.low))
    && Number.isFinite(Number(bar.close))
  ));
  if (!rows.length) return "";
  const highs = rows.map((bar) => Number(bar.high));
  const lows = rows.map((bar) => Number(bar.low));
  const min = Math.min(...lows);
  const max = Math.max(...highs);
  const span = max - min || 1;
  const slot = width / rows.length;
  const bodyWidth = Math.max(slot * 0.55, 0.4);
  return rows.map((bar, index) => {
    const open = Number(bar.open);
    const close = Number(bar.close);
    const high = Number(bar.high);
    const low = Number(bar.low);
    const x = (index + 0.5) * slot;
    const yHigh = height - ((high - min) / span) * height;
    const yLow = height - ((low - min) / span) * height;
    const yOpen = height - ((open - min) / span) * height;
    const yClose = height - ((close - min) / span) * height;
    const top = Math.min(yOpen, yClose);
    const bodyHeight = Math.max(Math.abs(yClose - yOpen), 0.4);
    const tone = close >= open ? "up" : "down";
    return `<g class="candle tone-${tone}" data-candle-index="${index}">`
      + `<line class="candle-wick" x1="${x.toFixed(2)}" x2="${x.toFixed(2)}" y1="${yHigh.toFixed(2)}" y2="${yLow.toFixed(2)}" vector-effect="non-scaling-stroke"/>`
      + `<rect class="candle-body" x="${(x - bodyWidth / 2).toFixed(2)}" y="${top.toFixed(2)}" width="${bodyWidth.toFixed(2)}" height="${bodyHeight.toFixed(2)}"/>`
      + `</g>`;
  }).join("");
}

export function chartFrame({ height = 180, title = "", state = "unavailable", detail = "No producer connected.", legend = [], yLabels = ["", "", ""], xLabels = [] , className = "", series = [], candles = [] }) {
  const legendHtml = legend.length ? `<div class="chart-legend">${legend.map(([name, tone]) => `<span>${dot(tone)}${escapeHtml(name)}</span>`).join("")}</div>` : "";
  const y = yLabels.map((label) => `<span>${escapeHtml(label)}</span>`).join("");
  const x = xLabels.map((label) => `<span>${escapeHtml(label)}</span>`).join("");
  const drawn = series.filter((line) => Array.isArray(line.values) && line.values.length > 1);
  const all = drawn.flatMap((line) => line.values.map(Number)).filter(Number.isFinite);
  const bounds = all.length ? { min: Math.min(...all), max: Math.max(...all) } : {};
  const candleSvg = candleMarks(candles);
  const seriesSvg = drawn.length
    ? drawn.map((line) => `<path class="tone-${escapeHtml(line.tone || "purple")}" d="${seriesPath(line.values, bounds)}" vector-effect="non-scaling-stroke"/>`).join("")
    : "";
  const svg = (candleSvg || seriesSvg)
    ? `<svg class="chart-series" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">${candleSvg}${seriesSvg}</svg>`
    : "";
  return `<div class="chart-frame ${escapeHtml(className)}" data-chart-state="${escapeHtml(state)}" style="--h:${height}px">
    ${title || legendHtml ? `<div class="chart-top">${title ? `<strong>${escapeHtml(title)}</strong>` : ""}${legendHtml}</div>` : ""}
    <div class="chart-plot"><div class="chart-y">${y}</div><div class="chart-grid"><span></span><span></span><span></span><span></span>${svg}</div>
      <div class="chart-overlay"><span class="chart-overlay-title">${escapeHtml(state === "unavailable" ? "No data yet" : readable(state))}</span><span class="chart-overlay-detail">${escapeHtml(detail)}</span></div>
    </div>
    ${x ? `<div class="chart-x">${x}</div>` : ""}
  </div>`;
}

export function sparkline(state = "unavailable", values = null) {
  const path = Array.isArray(values) ? seriesPath(values, { width: 60, height: 16 }) : "";
  if (path) {
    return `<span class="sparkline is-${escapeHtml(state)}" aria-label="${escapeHtml(state)}"><svg viewBox="-1 -1 62 18" preserveAspectRatio="none"><path d="${path}"/></svg></span>`;
  }
  return `<span class="sparkline is-${escapeHtml(state)}" aria-label="${escapeHtml(state === "unavailable" ? "no data" : state)}"><svg viewBox="0 0 60 18" preserveAspectRatio="none"><path d="M0 9 H60" stroke-dasharray="2 3"/></svg></span>`;
}

// Compact truthful placeholder. Keeps the `.empty-state` contract that the read-model
// binders replace when their data arrives.
export function unavailable(title, detail, { tone = "unavailable", attrs = "", compact = false } = {}) {
  return `<div class="empty-state tone-${escapeHtml(tone)} ${compact ? "is-compact" : ""}" ${attrs}><div class="empty-icon">${icon(tone === "error" ? "warn" : "activity", { size: 14 })}</div><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div></div>`;
}

export function note(text, tone = "neutral") {
  return `<p class="note tone-${escapeHtml(tone)}">${escapeHtml(text)}</p>`;
}

export function banner(text, { tone = "purple", iconName = "activity" } = {}) {
  return `<div class="banner tone-${escapeHtml(tone)}">${icon(iconName, { size: 14 })}<span>${escapeHtml(text)}</span></div>`;
}

export function tabRow(items, activeId, hrefFor, { className = "", ariaLabel = "Tabs" } = {}) {
  return `<nav class="tab-row ${escapeHtml(className)}" aria-label="${escapeHtml(ariaLabel)}">${items.map((item) => {
    const active = item.id === activeId;
    const href = hrefFor(item);
    return `<a class="tab ${active ? "is-active" : ""}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" ${active ? 'aria-current="page"' : ""}>${escapeHtml(item.label)}${item.count !== undefined ? `<span class="tab-count">${escapeHtml(item.count)}</span>` : ""}</a>`;
  }).join("")}</nav>`;
}

export function pillRow(items, activeId, hrefFor, { className = "" } = {}) {
  return `<nav class="pill-row ${escapeHtml(className)}">${items.map((item) => {
    const active = item.id === activeId;
    const href = hrefFor(item);
    return `<a class="pill ${active ? "is-active" : ""}" href="${escapeHtml(href)}" data-route="${escapeHtml(href)}" ${active ? 'aria-current="page"' : ""}>${escapeHtml(item.label)}${item.count !== undefined ? `<span class="pill-count">${escapeHtml(item.count)}</span>` : ""}</a>`;
  }).join("")}</nav>`;
}

export function pageTitle(title, { subtitle = "", actions = "", star = true } = {}) {
  return `<div class="page-title"><div class="page-title-text"><h1>${escapeHtml(title)}${star ? `<span class="page-star" aria-hidden="true">${icon("star", { size: 15 })}</span>` : ""}</h1>${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ""}</div>${actions ? `<div class="page-actions">${actions}</div>` : ""}</div>`;
}

export function identityRows(rows) {
  return `<div class="identity-list">${rows.map(([label, value]) => `<div class="stat-row"><span>${escapeHtml(label)}</span><code>${escapeHtml(value)}</code></div>`).join("")}</div>`;
}

export function toneForStatus(status) {
  if (status === "ready" || status === "current" || status === "completed" || status === "submitted" || status === "approved") return "ready";
  if (status === "stale" || status === "pending" || status === "prepared" || status === "compiled") return "pending";
  if (status === "error" || status === "failed" || status === "invalid" || status === "interrupted") return "error";
  return "unavailable";
}
