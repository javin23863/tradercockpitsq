import { escapeHtml as esc } from "./ui.mjs";
import { workflowHref } from "./automation-settings-controls.mjs";

const fmt = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });
export const number = value => typeof value === "number" && Number.isFinite(value) ? fmt.format(value) : "—";
export const date = value => value && Number.isFinite(Number(value)) ? new Date(Number(value)).toISOString().slice(0, 10) : "Time unavailable";
const tone = value => value < 0 ? "loss" : "gain";
const link = (strategy, view, resultView) => workflowHref({ project: strategy.project, tab: "results", ...view, resultView });
const empty = text => `<div class="results-chart-empty">${esc(text)}</div>`;

export function metricCards(a) {
  const fields = [["Net profit", "NetProfit", "money"], ["Profit factor", "ProfitFactor"], ["Trades", "NumberOfTrades"], ["Win rate", "WinningPct", "percent"], ["Closed-trade drawdown", "Drawdown", "money"], ["Return / drawdown", "ReturnDDRatio"]];
  return `<div class="results-metrics">${fields.map(([label, key, unit]) => `<article><span>${label}</span><strong class="${key === "NetProfit" ? tone(a.metrics[key]) : key === "Drawdown" ? "loss" : ""}">${number(a.metrics[key])}${unit === "percent" ? "%" : ""}</strong><small>${unit === "money" ? "Account currency" : key === "NumberOfTrades" ? "Recorded filled trades" : key === "WinningPct" ? "Native column formula" : "Selected sample"}</small></article>`).join("")}</div>`;
}

function card(title, body, href = "", className = "") {
  return `<section class="results-card ${className}"><header><h3>${esc(title)}</h3>${href ? `<a href="${esc(href)}">Explore ↗</a>` : ""}</header>${body}</section>`;
}

function bars(rows, valueKey, labelFor = row => row.period, compact = false) {
  if (!rows.length) return empty("No recorded trades for this selection.");
  const width = 350, height = compact ? 42 : 150, bottom = compact ? 62 : 177;
  const values = rows.map(row => row[valueKey]);
  const low = Math.min(0, ...values), high = Math.max(0, ...values), span = high - low || 1;
  const y = value => 12 + (high - value) / span * (height - 22), zero = y(0), step = width / rows.length;
  return `<svg class="results-bars" viewBox="0 0 400 ${compact ? 68 : 198}" role="img" aria-label="${esc(valueKey)} by category"><line x1="34" x2="384" y1="${zero}" y2="${zero}" class="chart-grid"/>${rows.map((row, i) => {
    const value = row[valueKey], label = labelFor(row), x = 34 + i * step + step * .15;
    return `<rect class="${tone(value)}" x="${x}" y="${Math.min(zero, y(value))}" width="${Math.max(1, step * .7)}" height="${Math.max(1, Math.abs(y(value) - zero))}" rx="2"><title>${esc(label)}: ${number(value)}</title></rect>${i % Math.ceil(rows.length / 7) === 0 ? `<text x="${x + step * .35}" y="${bottom}" text-anchor="middle">${esc(label)}</text>` : ""}`;
  }).join("")}<text x="30" y="14" text-anchor="end">${number(high)}</text><text x="30" y="${height}" text-anchor="end">${number(low)}</text></svg>`;
}

function scatter(points, xKey, yKey, xLabel) {
  if (!points.length) return empty("No valid duration records for this selection.");
  const xs = points.map(p => p[xKey]), ys = points.map(p => p[yKey]);
  const maxX = Math.max(...xs) || 1, low = Math.min(0, ...ys), high = Math.max(0, ...ys), span = high - low || 1;
  return `<svg class="results-scatter" viewBox="0 0 760 210" role="img" aria-label="Profit versus ${esc(xLabel)}"><line class="chart-grid" x1="48" x2="745" y1="${20 + high / span * 145}" y2="${20 + high / span * 145}"/>${points.map(p => `<circle class="${tone(p[yKey])}" cx="${48 + p[xKey] / maxX * 687}" cy="${20 + (high - p[yKey]) / span * 145}" r="3" opacity=".7"><title>${esc(xLabel)}: ${number(p[xKey])}; P/L: ${number(p[yKey])}</title></circle>`).join("")}<text x="48" y="190">0</text><text x="735" y="190" text-anchor="end">${number(maxX)} ${esc(xLabel)}</text><text x="40" y="22" text-anchor="end">${number(high)}</text><text x="40" y="168" text-anchor="end">${number(low)}</text></svg>`;
}

function curveSvg(points, { axis = "trade", compact = false, hasCapital = true } = {}) {
  if (!points.length) return empty("No closed trades in this selection.");
  const left = 75, right = 985, top = 22, floor = compact ? 175 : 275, ddTop = floor + 32, ddFloor = ddTop + 65;
  const balances = points.map(p => p.balance), low = Math.min(...balances), high = Math.max(...balances), span = high - low || 1;
  const times = points.map(p => axis === "time" ? p.time : p.trade);
  const xLow = Math.min(...times), xHigh = Math.max(...times), xSpan = xHigh - xLow || 1;
  const x = (p) => left + ((axis === "time" ? p.time : p.trade) - xLow) / xSpan * (right - left);
  const y = p => top + (high - p.balance) / span * (floor - top);
  const dd = Math.max(1, ...points.map(p => -p.drawdown));
  const path = points.map((p,i) => `${i ? "L" : "M"}${x(p)},${y(p)}`).join(" ");
  const ddPath = points.map(p => `L${x(p)},${ddTop - p.drawdown / dd * 65}`).join(" ");
  return `<svg class="results-equity-svg" viewBox="0 0 1000 ${ddFloor + 35}" role="img" aria-label="Closed-trade ${hasCapital ? "balance" : "cumulative P/L"} with aligned drawdown"><defs><linearGradient id="balance-fill" x1="0" x2="0" y1="0" y2="1"><stop stop-color="#9567fb" stop-opacity=".28"/><stop offset="1" stop-color="#9567fb" stop-opacity=".01"/></linearGradient></defs>${[0,.5,1].map(t => `<line x1="${left}" x2="${right}" y1="${top + t*(floor-top)}" y2="${top + t*(floor-top)}" class="chart-grid"/><text x="65" y="${top + t*(floor-top)+4}" text-anchor="end">${number(high-t*span)}</text>`).join("")}<path d="${path} L${x(points.at(-1))},${floor} L${x(points[0])},${floor} Z" fill="url(#balance-fill)"/><path d="${path}" fill="none" stroke="#a77dff" stroke-width="2.5" vector-effect="non-scaling-stroke"/><path d="M${x(points[0])},${ddTop} ${ddPath} L${x(points.at(-1))},${ddTop} Z" fill="#f47788" fill-opacity=".45"/><line x1="${left}" x2="${right}" y1="${ddTop}" y2="${ddTop}" class="chart-grid"/><text x="65" y="${ddTop+4}" text-anchor="end">0</text><text x="65" y="${ddFloor}" text-anchor="end">−${number(dd)}</text>${[0,.5,1].map(t=>{const i=Math.round(t*(points.length-1)),p=points[i];return `<text x="${x(p)}" y="${ddFloor+23}" text-anchor="${t===0?'start':t===1?'end':'middle'}">${axis==='time'?date(p.time):`Trade ${p.trade}`}</text>`;}).join("")}<line data-equity-crosshair x1="0" x2="0" y1="${top}" y2="${ddFloor}" stroke="#61dcef" stroke-dasharray="3 4" hidden/>${points.length===1?`<circle cx="${x(points[0])}" cy="${y(points[0])}" r="4" fill="#a77dff"/>`:""}</svg>`;
}

export function equityPanel(a, compact = false) {
  return `<div class="results-equity" data-equity-dashboard><div class="results-chart-legend"><span>● Closed-trade ${a.capital == null ? "cumulative P/L" : "balance"}</span><span class="loss">● Closed-trade drawdown</span><small>${a.capital == null ? "Initial capital unavailable" : `Initial capital ${number(a.capital)}`} · Account currency</small></div>${compact ? "" : `<div class="results-chart-controls"><label>X axis <select data-equity-axis><option value="trade">Trade</option><option value="time" ${a.time_axis_available ? "" : "disabled"}>Time</option></select></label><label>Zoom <input aria-label="Chart zoom" type="range" data-equity-zoom min="1" max="10" value="1" step="1"></label><label>Position <input aria-label="Chart position" type="range" data-equity-position min="0" max="100" value="100"></label><button data-equity-reset>Reset view</button></div>`}<div data-equity-plot>${curveSvg(a.equity, {compact, hasCapital:a.capital != null})}</div><output data-equity-tooltip aria-live="polite">${compact ? "Recorded trade sequence" : "Hover over the chart or use ← / → to inspect trades"}</output>${!a.time_axis_available?'<p class="field-help">Some close times are unavailable; use the trade axis.</p>':""}</div>`;
}

export function overviewDashboard(strategy, view) {
  const a = strategy.analytics;
  return `<div class="results-overview-grid">${card(a.capital == null ? "Cumulative P/L & drawdown" : "Balance & drawdown", equityPanel(a, true), link(strategy, view, "equity"), "results-hero-chart")}${card("Trade P/L distribution", bars(a.distribution, "count", r => number((r.from+r.to)/2), true), link(strategy, view, "trade-analysis"))}${card("Performance by year", bars(a.periods.year, "NetProfit", r => r.period, true), link(strategy, view, "trade-analysis"))}${card("Long / short contribution", bars(a.sides, "NetProfit", r => r.period, true), link(strategy, view, "trade-analysis"))}</div>`;
}

function periodTable(rows) {
  return `<div class="results-table-scroll"><table><thead><tr><th>Year</th><th>Net profit</th><th>Profit factor</th><th>Trades</th><th>Win rate</th></tr></thead><tbody>${rows.map(r=>`<tr><td>${esc(r.period)}</td><td class="${tone(r.NetProfit)}">${number(r.NetProfit)}</td><td>${number(r.ProfitFactor)}</td><td>${number(r.NumberOfTrades)}</td><td>${number(r.WinningPct)}%</td></tr>`).join("")}</tbody></table></div>`;
}

export function analysisDashboard(a) {
  const labels = {weekday: r => ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"][Number(r.period)],month: r=>["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"][Number(r.period)-1],hour:r=>r.period+":00",year:r=>r.period};
  const outcomes = [{period:"Wins",count:a.metrics.NumberOfProfits},{period:"Losses",count:a.metrics.NumberOfLosses},{period:"Breakeven",count:a.breakeven}];
  return `<p class="results-analysis-note">Grouped by ${a.period_by === "open_time" ? "open" : "close"} time · Native timestamp clock, without browser timezone conversion${a.missing_time?` · ${a.missing_time} trades excluded from time groups: timestamp unavailable`:""}</p><div class="results-analysis-grid">${card("Yearly results",periodTable([{period:"All",...a.metrics},...a.periods.year]))}${["year","hour","weekday","month"].map(key=>card(`Net profit by ${key}`,bars(a.periods[key],"NetProfit",labels[key]))+card(`Trades by ${key}`,bars(a.periods[key],"NumberOfTrades",labels[key]))).join("")}${card("Long / short P/L",bars(a.sides,"NetProfit"))}${card("Long / short trades",bars(a.sides,"NumberOfTrades"))}${card("Trade outcomes",bars(outcomes,"count"))}${card("Winning P/L by month",bars(a.periods.month,"GrossProfit",labels.month))}${card("Losing P/L by month",bars(a.periods.month.map(r=>({...r,GrossLoss:-r.GrossLoss})),"GrossLoss",labels.month))}${card("Trades by duration",bars(a.durations,"count"))}${card("P/L versus duration",scatter(a.duration_points,"seconds","pl","seconds"))}${card("Trade P/L distribution",bars(a.distribution,"count",r=>number((r.from+r.to)/2)))}${["weekday","month"].map(key=>card(`Winning trades by ${key}`,bars(a.periods[key],"NumberOfProfits",labels[key]))+card(`Losing trades by ${key}`,bars(a.periods[key],"NumberOfLosses",labels[key]))).join("")}</div>${a.missing_duration?`<p>${a.missing_duration} trades have unavailable or invalid duration.</p>`:""}`;
}

export function bindEquityDashboard(root, a) {
  const host = root.querySelector("[data-equity-dashboard]");
  if (!host || !a) return;
  const plot = host.querySelector("[data-equity-plot]"), axis = host.querySelector("[data-equity-axis]");
  const zoom = host.querySelector("[data-equity-zoom]"), position = host.querySelector("[data-equity-position]"), output = host.querySelector("output");
  let points = a.equity, cursor = 0;
  const inspect = index => {
    if (!points.length) return;
    cursor = Math.max(0,Math.min(points.length-1,index));
    const p = points[cursor];
    output.textContent = `Trade ${p.trade} · ${date(p.time)} · ${a.capital == null ? "Cumulative P/L" : "Balance"} ${number(p.balance)} · Drawdown ${number(p.drawdown)}`;
    const cross = plot.querySelector("[data-equity-crosshair]");
    const xs = points.map(p=>axis?.value==='time'?p.time:p.trade), lo=Math.min(...xs), hi=Math.max(...xs);
    const x=75+(xs[cursor]-lo)/(hi-lo||1)*910;
    cross.hidden=false; cross.removeAttribute("hidden"); cross.setAttribute("x1",x); cross.setAttribute("x2",x);
  };
  const draw = () => {
    const count=Math.max(1,Math.ceil(a.equity.length/Number(zoom?.value||1)));
    const start=Math.round((a.equity.length-count)*Number(position?.value||0)/100);
    points=a.equity.slice(start,start+count);
    plot.innerHTML=curveSvg(points,{axis:axis?.value||"trade",compact:!axis,hasCapital:a.capital != null});
    output.textContent=`Showing ${points.length} of ${a.equity.length} recorded trades`;
  };
  host.addEventListener("input",event=>{if(event.target.matches("[data-equity-zoom],[data-equity-position]")) draw();});
  axis?.addEventListener("change",draw);
  host.querySelector("[data-equity-reset]")?.addEventListener("click",()=>{zoom.value="1";position.value="100";axis.value="trade";draw();});
  plot.tabIndex=0; plot.setAttribute("aria-label",`${a.capital == null ? "Cumulative P/L" : "Balance"} chart. Use left and right arrow keys to inspect trades.`);
  plot.addEventListener("keydown",event=>{if(["ArrowLeft","ArrowRight"].includes(event.key)){event.preventDefault();inspect(cursor+(event.key==='ArrowRight'?1:-1));}});
  plot.addEventListener("pointermove",event=>{
    const svg=plot.querySelector("svg"); if(!svg||!points.length)return;
    const box=svg.getBoundingClientRect(), fraction=Math.max(0,Math.min(1,((event.clientX-box.left)/box.width*1000-75)/910));
    const xs=points.map(p=>axis?.value==='time'?p.time:p.trade), target=Math.min(...xs)+fraction*(Math.max(...xs)-Math.min(...xs));
    let best=0;for(let i=1;i<xs.length;i++)if(Math.abs(xs[i]-target)<Math.abs(xs[best]-target))best=i;
    inspect(best);
  });
}
