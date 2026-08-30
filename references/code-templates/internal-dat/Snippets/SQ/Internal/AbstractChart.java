/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.SettingsKeys;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.results.SpecialValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Abstract base class for profile chart indicators (Volume Profile, TPO, etc.).
 * Contains shared SVG drawing helpers, utility methods, session tracking
 * fields,
 * and export infrastructure common to all profile chart types.
 *
 * Inheritance hierarchy:
 * IndicatorBlock -> AbstractChart -> VolumeProfileIndicatorChart /
 * TPOProfileIndicatorChart ->
 * concrete indicators
 */
public abstract class AbstractChart extends IndicatorBlock {

    private static final Logger Log = LoggerFactory.getLogger(AbstractChart.class);

    // Max sessions kept in history for SVG export. 0 = unlimited (grow as needed).
    protected static final int DEFAULT_MAX_SESSIONS_FOR_SVG = 20;

    // ---- Common chart parameter (subclasses declare their own @Parameter for
    // this) ----
    // Note: Chart, ProfileRows, BinSizeMode, StoreChartData, ShowCandlesticks, etc.
    // are
    // declared in subclasses with @Parameter annotations since annotations are not
    // inherited.

    // ---- Shared fields ----

    // Session tracking
    protected long currentSessionStart = 0;
    protected long currentSessionEnd = 0;
    protected long prevSessionStart = 0;
    protected long prevSessionEnd = 0;

    // Initial Balance
    protected double prevIBH = 0;
    protected double prevIBL = 0;

    // Total volume results
    protected double prevTotalVolume = 0;
    protected double prevTotalBullVolume = 0;
    protected double prevTotalBearVolume = 0;

    // Set to true to enable debug console logging for this indicator instance only.
    protected boolean debugEnabled = false;

    // Bin configuration (set by subclass)
    protected int lastNumBins = 0;
    protected static final int MAX_BINS = 2000;

    // Cluster bins (Gaussian-enhanced)
    protected double[] clusterBins;

    // Random suffix for this indicator instance (generated once in OnInit)
    protected String fileRandomSuffix;

    // Global StoreChartData trading option value — lazily read on first use
    private boolean storeChartData = false;
    private boolean storeChartDataLoaded = false;


    // Session history (shared arrays)
    protected int historyCount = 0;
    protected long[] histSessionStart;
    protected long[] histSessionEnd;
    protected double[] histIBH, histIBL;
    protected double[] histSessionHigh, histSessionLow;
    protected int[] histNumBins;
    protected double[] histTotalVolume;
    protected double[] histBullVolume;
    protected double[] histBearVolume;

    // M1 chart number for sub-timeframe access
    protected int M1chartNumber = -1;

    // ------------------------------------------------------------------------
    // Abstract methods — implemented by VolumeProfileIndicatorChart /
    // TPOProfileIndicatorChart
    // ------------------------------------------------------------------------

    /** Allocate/resize indicator-specific arrays (volumeBins, tpoBins, etc.) */
    protected abstract void ensureArrays();

    /** Export multi-session SVG chart. */
    protected abstract void exportMultiSessionSVG();

    /**
     * Return the ChartData for bar access. Subclass returns its @Parameter Chart
     * field.
     */
    protected abstract ChartData cfgChart();

    /** Return ProfileRows parameter value. */
    protected abstract int cfgProfileRows();

    /** Return BinSizeMode parameter value. */
    protected abstract int cfgBinSizeMode();

    // Returns StoreChartData from strategy settings. Read once on first call
    // (strategy is not yet available in OnInit, so lazy init is required).
    protected boolean cfgStoreChartData() {
        if (!storeChartDataLoaded) {
            try {
                StrategyBase strategy = getStrategy();
                if (strategy != null) {
                    SettingsMap settings = strategy.getSettings();
                    if (settings != null) {
                        storeChartData = settings.getBoolean(SettingsKeys.StoreChartData, false);
                    }
                }
            } catch (Exception e) {
                Log.error("Failed to read StoreChartData from strategy settings", e);
            }
            storeChartDataLoaded = true;
        }
        return storeChartData;
    }

    /** Return ShowCandlesticks parameter value. */
    protected abstract boolean cfgShowCandlesticks();

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    @Override
    protected void OnInit() throws TradingException {
        ensureArrays();
        fileRandomSuffix = UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    protected void OnDeinit() throws TradingException {
        if (cfgStoreChartData() && historyCount > 0) {
            exportMultiSessionSVG();
        }
    }

    // ------------------------------------------------------------------------
    // Export helpers
    // ------------------------------------------------------------------------

    protected String resolveExportFolder() {
    	String path = MainApp.getDataPath() + "internal/tmp/profilechart";
    	
        String p = Paths.get(path).normalize().toString();
        p = p.replace("\\", File.separator).replace("/", File.separator);
        try {
            File dir = new File(p);
            if (!dir.exists())
                dir.mkdirs();
        } catch (Exception e) {
            p = ".";
        }
        return p;
    }

    /**
     * Triggers SVG export on demand (called by signal blocks when a signal fires).
     */
    public void triggerExport() {
        if (historyCount > 0) {
            exportMultiSessionSVG();
        }
    }

    protected static String joinPath(String folder, String filename) {
        if (folder.endsWith(File.separator))
            return folder + filename;
        return folder + File.separator + filename;
    }

    /**
     * Saves the chart file path to strategy specialValues. Transferred to ResultsGroup after backtest.
     */
    protected void saveChartPath(String path) {
        StrategyBase strategy = getStrategy();
        if (strategy == null) return;
        
        SettingsMap sv = strategy.getSpecialValues();
        if (sv == null) return;
        
        String current = sv.getString(SpecialValues.ProfileChartPaths, "");
        String newValue = current.isEmpty() ? path : current + "," + path;
        sv.setString(SpecialValues.ProfileChartPaths, newValue);
    }

    // ------------------------------------------------------------------------
    // SVG drawing helpers
    // ------------------------------------------------------------------------

    protected void drawSvgLevel(BufferedWriter w, int px, int pw, int mt, int ph,
            double gLow, double gRange, double price, String color, String label) throws IOException {
        double y = mt + ph - (price - gLow) / gRange * ph;
        if (y < mt || y > mt + ph)
            return;
        w.write("<line x1=\"" + px + "\" y1=\"" + y + "\" x2=\"" + (px + pw) + "\" y2=\"" + y
                + "\" stroke=\"" + color
                + "\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
        w.write("<text x=\"" + (px + pw - 3) + "\" y=\"" + (y - 3)
                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"" + color
                + "\" text-anchor=\"end\" font-weight=\"700\">" + label + "</text>\n");
    }

    protected void drawSvgCandlesUnified(BufferedWriter w, int mx, int pw, int mt, int ph,
            double gLow, double gRange, long tStart, long tEnd, long tDur, double alpha) throws IOException {
        ChartData Chart = cfgChart();
        if (Chart == null || tDur <= 0)
            return;

        // Count bars across entire time range
        int barCount = 0;
        int i = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart)
                    break;
                if (t >= tStart && t < tEnd)
                    barCount++;
                i++;
            }
        } catch (Exception e) {
        }
        if (barCount == 0)
            return;

        double candleW = Math.max(1.0, (double) pw / barCount * 0.7);
        double candleStep = (double) pw / barCount;

        i = 0;
        int barIdx = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart)
                    break;
                if (t >= tStart && t < tEnd) {
                    double o = Chart.Open(i), h = Chart.High(i), l = Chart.Low(i), c = Chart.Close(i);
                    double x = mx + (barCount - 1 - barIdx) * candleStep;
                    double yH = mt + ph - (h - gLow) / gRange * ph;
                    double yL = mt + ph - (l - gLow) / gRange * ph;
                    double yO = mt + ph - (o - gLow) / gRange * ph;
                    double yC = mt + ph - (c - gLow) / gRange * ph;
                    boolean bull = c >= o;
                    String clr = bull ? "#26a69a" : "#ef5350";
                    String alphaStr = String.format(Locale.US, "%.2f", alpha);
                    double wickX = x + candleW / 2;
                    w.write("<line x1=\"" + wickX + "\" y1=\"" + yH + "\" x2=\"" + wickX + "\" y2=\"" + yL
                            + "\" stroke=\"" + clr + "\" stroke-width=\"0.8\" stroke-opacity=\"" + alphaStr + "\"/>\n");
                    double bodyTop = Math.min(yO, yC);
                    double bodyH = Math.max(0.5, Math.abs(yC - yO));
                    w.write("<rect x=\"" + x + "\" y=\"" + bodyTop + "\" width=\"" + candleW
                            + "\" height=\"" + bodyH + "\" fill=\"" + clr
                            + "\" fill-opacity=\"" + alphaStr + "\"/>\n");
                    barIdx++;
                }
                i++;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Like drawSvgCandlesUnified but gives each candle a unique id for replay
     * control.
     * Each candle gets id="rc_0", "rc_1", etc. (chronological order, oldest first).
     */
    protected void drawSvgCandlesWithIds(BufferedWriter w, int mx, int pw, int mt, int ph,
            double gLow, double gRange, long tStart, long tEnd, long tDur, double alpha) throws IOException {
        ChartData Chart = cfgChart();
        if (Chart == null || tDur <= 0)
            return;

        int barCount = 0;
        int i = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart)
                    break;
                if (t >= tStart && t < tEnd)
                    barCount++;
                i++;
            }
        } catch (Exception e) {
        }
        if (barCount == 0)
            return;

        double candleW = Math.max(1.0, (double) pw / barCount * 0.7);
        double candleStep = (double) pw / barCount;

        i = 0;
        int barIdx = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart)
                    break;
                if (t >= tStart && t < tEnd) {
                    double o = Chart.Open(i), h = Chart.High(i), l = Chart.Low(i), c = Chart.Close(i);
                    double x = mx + (barCount - 1 - barIdx) * candleStep;
                    double yH = mt + ph - (h - gLow) / gRange * ph;
                    double yL = mt + ph - (l - gLow) / gRange * ph;
                    double yO = mt + ph - (o - gLow) / gRange * ph;
                    double yC = mt + ph - (c - gLow) / gRange * ph;
                    boolean bull = c >= o;
                    String clr = bull ? "#26a69a" : "#ef5350";
                    String alphaStr = String.format(Locale.US, "%.2f", alpha);
                    double wickX = x + candleW / 2;
                    // chronological index: oldest bar is 0
                    int chronoIdx = barCount - 1 - barIdx;
                    w.write("<g id=\"rc_" + chronoIdx + "\">");
                    w.write("<line x1=\"" + wickX + "\" y1=\"" + yH + "\" x2=\"" + wickX + "\" y2=\"" + yL
                            + "\" stroke=\"" + clr + "\" stroke-width=\"0.8\" stroke-opacity=\"" + alphaStr + "\"/>");
                    double bodyTop = Math.min(yO, yC);
                    double bodyH = Math.max(0.5, Math.abs(yC - yO));
                    w.write("<rect x=\"" + x + "\" y=\"" + bodyTop + "\" width=\"" + candleW
                            + "\" height=\"" + bodyH + "\" fill=\"" + clr
                            + "\" fill-opacity=\"" + alphaStr + "\"/>");
                    w.write("</g>\n");
                    barIdx++;
                }
                i++;
            }
        } catch (Exception e) {
        }
    }

    protected void drawSvgTimeAxis(BufferedWriter w, int mx, int pw, int mt, int ph,
            long tStart, long tEnd, String timeFmt) throws IOException {
        ChartData Chart = cfgChart();
        if (Chart == null)
            return;

        // Pass 1: count bars in range
        int n = 0;
        int i = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart) break;
                if (t >= tStart && t < tEnd) n++;
                i++;
            }
        } catch (Exception e) {
        }
        if (n == 0) return;

        // Pass 2: fill timestamps into a plain long[] — avoids long[1] wrapper per bar
        long[] barTimes = new long[n];
        i = 0;
        int bi = 0;
        try {
            while (true) {
                long t = Chart.Time(i);
                if (t < tStart) break;
                if (t >= tStart && t < tEnd) barTimes[bi++] = t;
                i++;
            }
        } catch (Exception e) {
        }

        double stepPx = (double) pw / n;
        int labelEvery = Math.max(1, (int) Math.ceil(n * 14.0 / pw));
        SimpleDateFormat tf = new SimpleDateFormat(timeFmt, Locale.US);
        for (int b = 0; b < n; b += labelEvery) {
            long bt = barTimes[n - 1 - b];
            double x = mx + b * stepPx + stepPx / 2;
            String xs = String.format(Locale.US, "%.1f", x);
            w.write("<line x1=\"" + xs + "\" y1=\"" + (mt + ph)
                    + "\" x2=\"" + xs + "\" y2=\"" + (mt + ph + 4)
                    + "\" stroke=\"#555\" stroke-width=\"0.5\"/>\n");
            int ty = mt + ph + 6;
            w.write("<text x=\"" + xs + "\" y=\"" + ty
                    + "\" transform=\"rotate(-45," + xs + "," + ty + ")\""
                    + " font-family=\"Monospace\" font-size=\"9\" fill=\"#888\" text-anchor=\"end\">"
                    + tf.format(new Date(bt)) + "</text>\n");
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    protected static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    protected int yLabelStepForDenseAxis() {
        int nb = (lastNumBins > 0) ? lastNumBins : cfgProfileRows();
        if (nb <= 150)
            return 1;
        return (int) Math.ceil(nb / 150.0);
    }

    protected String formatForFilename(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return sdf.format(new Date(time));
    }

    protected static String svgEsc(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
                "&apos;");
    }

    /**
     * Returns true if the given timestamp falls on a Sunday.
     * Used to skip Forex weekend gap bars from profile calculations.
     */
    protected boolean isSunday(long time) {
        return SQTime.getDayOfWeek(time) == SQTime.SUNDAY;
    }

    @Override
    public void debug(String category, String message) {
        if (debugEnabled) {
            super.debug(category, message);
        }
    }
}
