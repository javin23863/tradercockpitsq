/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
 * TPO Profile Chart - intermediate helper class for TPO indicators.
 */
package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Arrays;


/**
 * Abstract base class for TPO Profile chart indicators.
 * Contains TPO calculation logic, SVG export, and TPO-specific fields.
 * Concrete indicators (TPOProfile) extend this.
 */
public abstract class TPOProfileIndicatorChart extends AbstractChart {

    // Max price bins per session in SVG. If a session has more raw bins, adjacent ones
    // are merged so the profile stays readable and the file size stays manageable.
    // e.g. 1000 raw bins (100.00–110.00 in 0.01 steps) → merged into 100 visual rows,
    // each covering a 0.10 range (100.00–100.10, 100.10–100.20, ..., 109.90–110.00).
    private static final int MAX_SVG_BINS_PER_SESSION = 100;

    // SVG export colors
    protected static final String DEFAULT_DARK_BG_COLOR = "#1a1a2e";
    protected static final String DEFAULT_DARK_PLOT_COLOR = "#16213e";
    protected static final String DEFAULT_LIGHT_BG_COLOR = "#e0e0e0";
    protected static final String DEFAULT_LIGHT_PLOT_COLOR = "#faf5eb";

    // SVG/chart display
    protected static final int DEFAULT_DARK_CANDLE_OPACITY = 100;
    protected static final int DEFAULT_LIGHT_CANDLE_OPACITY = 60;
    protected static final int DEFAULT_DARK_PROFILE_OPACITY = 50;
    protected static final int DEFAULT_LIGHT_PROFILE_OPACITY = 80;
    protected static final int DEFAULT_TPO_FONT_SIZE = 12;
    protected static final int DEFAULT_DELTA_FONT_SIZE = 11;
    protected static final int DEFAULT_CHART_HEIGHT = 900;
    protected static final int DEFAULT_SESSION_WIDTH = 1200;

    // Letter-to-color palette for TPO brackets
    protected static final String[] TPO_LETTER_PALETTE = {
            "#e6194b", "#3cb44b", "#ffe119", "#4363d8", "#f58231",
            "#911eb4", "#42d4f4", "#f032e6", "#bfef45", "#fabed4",
            "#469990", "#dcbeff", "#9A6324", "#fffac8", "#800000",
            "#aaffc3", "#808000", "#ffd8b1", "#000075", "#a9a9a9",
            "#e6beff", "#1abc9c", "#2ecc71", "#3498db", "#9b59b6",
            "#34495e", "#f39c12", "#d35400", "#c0392b", "#7f8c8d",
            "#27ae60", "#2980b9", "#8e44ad", "#2c3e50", "#e74c3c",
            "#90b9e0", "#58d68d", "#f5b041", "#af7ac5", "#5dade2",
            "#48c9b0", "#eb984e", "#f1948a", "#85929e", "#73c6b6",
            "#82e0aa", "#f7dc6f", "#bb8fce", "#aed6f1", "#f0b27a",
            "#d7bde2", "#a3e4d7", "#fad7a0", "#d5f5e3", "#fadbd8",
            "#d6eaf8", "#e8daef", "#fdebd0", "#d4efdf", "#f2d7d5",
            "#d0ece7", "#f9e79f"
    };

    // Previous session profile results (TPO-specific)
    protected double prevTPO_POC = 0;
    protected double prevTPO_VAH = 0;
    protected double prevTPO_VAL = 0;

    // VCP results (TPO)
    protected double prevTPO_VPOC = 0;
    protected double prevTPO_VVAH = 0;
    protected double prevTPO_VVAL = 0;

    // Bull/Bear POC results (TPO)
    protected double prevTPO_BullPOC = 0;
    protected double prevTPO_BearPOC = 0;
    protected int[] bullTpoBins;
    protected int[] bearTpoBins;

    // TPO profile arrays
    protected int[] tpoBins;
    protected long[] tpoMask;
    protected long[] bracketFirstTime = new long[62];
    protected long[] bracketLastTime = new long[62];
    protected double cumVolForBrackets = 0.0;
    protected int maxUsedBracket = -1;
    protected int tpoBracketCount = 0;
    protected int lastTpoPocIndex = -1;

    // TPO analysis fields
    protected int lastSinglePrintCount = 0;
    protected int lastTpoHighBin = -1;
    protected int lastTpoLowBin = -1;
    protected boolean lastPoorHigh = false;
    protected boolean lastPoorLow = false;
    protected boolean lastExcessHigh = false;
    protected boolean lastExcessLow = false;
    protected double lastEffectiveBracketVolume = 0;
    protected long lastEffectiveBracketMillis = 0;
    protected int lastBuyingTailLen = 0;
    protected int lastSellingTailLen = 0;
    protected int lastProfileShape = 5;
    protected double lastLedgeUpPrice = 0;
    protected double lastLedgeDownPrice = 0;

    // TPO history arrays
    protected double[] histTPO_POC, histTPO_VAH, histTPO_VAL;
    protected double[] histBinSize;
    protected int[][] histTpoBins;
    protected long[][] histTpoMask;
    protected int[] histTpoBracketCount;
    protected int[] histProfileShape;

    // ------------------------------------------------------------------------
    // TPO-specific methods
    // ------------------------------------------------------------------------

    // Allocates working arrays for TPO bins and masks. Fixed Tick mode uses MAX_BINS (worst case),
    // Range-Based mode uses exactly ProfileRows bins.
    protected void ensureArrays() {
        int needed = (cfgBinSizeMode() == 2) ? MAX_BINS : cfgProfileRows();
        if (tpoBins == null || tpoBins.length < needed) {
            tpoBins = new int[needed];
        }
        if (tpoMask == null || tpoMask.length < needed) {
            tpoMask = new long[needed];
        }
        if (bracketFirstTime == null || bracketFirstTime.length != 62)
            bracketFirstTime = new long[62];
        if (bracketLastTime == null || bracketLastTime.length != 62)
            bracketLastTime = new long[62];
        if (clusterBins == null || clusterBins.length < needed) {
            clusterBins = new double[needed];
        }
        if (bullTpoBins == null || bullTpoBins.length < needed) {
            bullTpoBins = new int[needed];
        }
        if (bearTpoBins == null || bearTpoBins.length < needed) {
            bearTpoBins = new int[needed];
        }
    }

    // Lazily allocates session history arrays (used only for SVG chart export).
    // Capacity = DEFAULT_MAX_SESSIONS_FOR_SVG when > 0 (fixed size, oldest sessions dropped);
    // otherwise starts at 32 and grows automatically via growHistory().
    protected void ensureHistory() {
        int cap = (DEFAULT_MAX_SESSIONS_FOR_SVG > 0) ? DEFAULT_MAX_SESSIONS_FOR_SVG : 32;
        int binCap = (cfgBinSizeMode() == 2) ? MAX_BINS : cfgProfileRows();
        if (histSessionStart == null || histSessionStart.length < cap) {
            histSessionStart = new long[cap];
            histSessionEnd = new long[cap];
            histTPO_POC = new double[cap];
            histBinSize = new double[cap];
            histTPO_VAH = new double[cap];
            histTPO_VAL = new double[cap];
            histIBH = new double[cap];
            histIBL = new double[cap];
            histSessionHigh = new double[cap];
            histSessionLow = new double[cap];
            histTpoBins = new int[cap][binCap];
            histTpoMask = new long[cap][binCap];
            histTpoBracketCount = new int[cap];
            histProfileShape = new int[cap];
            histNumBins = new int[cap];
            histTotalVolume = new double[cap];
            histBullVolume = new double[cap];
            histBearVolume = new double[cap];

            historyCount = 0;
        }
    }

    // Doubles the capacity of all session history arrays when they are full.
    // New rows in 2D bin arrays must be explicitly initialised (Arrays.copyOf leaves them null).
    // In capped mode (DEFAULT_MAX_SESSIONS_FOR_SVG > 0) this is never called.
    protected void growHistory() {
        if (DEFAULT_MAX_SESSIONS_FOR_SVG > 0) return; // capped mode — fixed size, never grow
        int oldCap = histSessionStart.length;
        int newCap = oldCap * 2;
        int binCap = (cfgBinSizeMode() == 2) ? MAX_BINS : cfgProfileRows();
        histSessionStart = Arrays.copyOf(histSessionStart, newCap);
        histSessionEnd = Arrays.copyOf(histSessionEnd, newCap);
        histTPO_POC = Arrays.copyOf(histTPO_POC, newCap);
        histBinSize = Arrays.copyOf(histBinSize, newCap);
        histTPO_VAH = Arrays.copyOf(histTPO_VAH, newCap);
        histTPO_VAL = Arrays.copyOf(histTPO_VAL, newCap);
        histIBH = Arrays.copyOf(histIBH, newCap);
        histIBL = Arrays.copyOf(histIBL, newCap);
        histSessionHigh = Arrays.copyOf(histSessionHigh, newCap);
        histSessionLow = Arrays.copyOf(histSessionLow, newCap);
        histNumBins = Arrays.copyOf(histNumBins, newCap);
        histTotalVolume = Arrays.copyOf(histTotalVolume, newCap);
        histBullVolume = Arrays.copyOf(histBullVolume, newCap);
        histBearVolume = Arrays.copyOf(histBearVolume, newCap);
        histTpoBracketCount = Arrays.copyOf(histTpoBracketCount, newCap);
        histProfileShape = Arrays.copyOf(histProfileShape, newCap);
        histTpoBins = Arrays.copyOf(histTpoBins, newCap);
        histTpoMask = Arrays.copyOf(histTpoMask, newCap);
        for (int i = oldCap; i < newCap; i++) {
            histTpoBins[i] = new int[binCap];
            histTpoMask[i] = new long[binCap];
        }
    }

    // Saves the finished session TPO profile into history arrays for SVG export.
    // If the same session start already exists, the existing entry is updated in-place
    // instead of appending a duplicate.
    protected void pushSessionHistory(double sessionHigh, double sessionLow, double binSize) {
        ensureHistory();
        int nb = lastNumBins;

        // If the latest entry is the same session, update in-place (don't push
        // duplicate)
        if (historyCount > 0 && histSessionStart[historyCount - 1] == prevSessionStart) {
            int idx = historyCount - 1;
            histSessionEnd[idx] = prevSessionEnd;
            histTPO_POC[idx] = prevTPO_POC;
            histTPO_VAH[idx] = prevTPO_VAH;
            histTPO_VAL[idx] = prevTPO_VAL;
            histIBH[idx] = prevIBH;
            histIBL[idx] = prevIBL;
            histSessionHigh[idx] = sessionHigh;
            histSessionLow[idx] = sessionLow;
            histNumBins[idx] = nb;
            histBinSize[idx] = binSize;
            System.arraycopy(tpoBins, 0, histTpoBins[idx], 0, nb);
            System.arraycopy(tpoMask, 0, histTpoMask[idx], 0, nb);
            histTpoBracketCount[idx] = tpoBracketCount;
            histProfileShape[idx] = lastProfileShape;
            histTotalVolume[idx] = prevTotalVolume;
            histBullVolume[idx] = prevTotalBullVolume;
            histBearVolume[idx] = prevTotalBearVolume;
            return;
        }

        if (historyCount >= histSessionStart.length) {
            if (DEFAULT_MAX_SESSIONS_FOR_SVG > 0) {
                // Capped mode: drop oldest session by shifting everything left by 1
                int cap = histSessionStart.length;
                System.arraycopy(histSessionStart,    1, histSessionStart,    0, cap - 1);
                System.arraycopy(histSessionEnd,      1, histSessionEnd,      0, cap - 1);
                System.arraycopy(histTPO_POC,         1, histTPO_POC,         0, cap - 1);
                System.arraycopy(histBinSize,         1, histBinSize,         0, cap - 1);
                System.arraycopy(histTPO_VAH,         1, histTPO_VAH,         0, cap - 1);
                System.arraycopy(histTPO_VAL,         1, histTPO_VAL,         0, cap - 1);
                System.arraycopy(histIBH,             1, histIBH,             0, cap - 1);
                System.arraycopy(histIBL,             1, histIBL,             0, cap - 1);
                System.arraycopy(histSessionHigh,     1, histSessionHigh,     0, cap - 1);
                System.arraycopy(histSessionLow,      1, histSessionLow,      0, cap - 1);
                System.arraycopy(histNumBins,         1, histNumBins,         0, cap - 1);
                System.arraycopy(histTotalVolume,     1, histTotalVolume,     0, cap - 1);
                System.arraycopy(histBullVolume,      1, histBullVolume,      0, cap - 1);
                System.arraycopy(histBearVolume,      1, histBearVolume,      0, cap - 1);
                System.arraycopy(histTpoBracketCount, 1, histTpoBracketCount, 0, cap - 1);
                System.arraycopy(histProfileShape,    1, histProfileShape,    0, cap - 1);
                System.arraycopy(histTpoBins,         1, histTpoBins,         0, cap - 1);
                System.arraycopy(histTpoMask,         1, histTpoMask,         0, cap - 1);
                historyCount = cap - 1;
            } else {
                growHistory();
            }
        }
        int idx = historyCount;
        histSessionStart[idx] = prevSessionStart;
        histSessionEnd[idx] = prevSessionEnd;
        histTPO_POC[idx] = prevTPO_POC;
        histTPO_VAH[idx] = prevTPO_VAH;
        histTPO_VAL[idx] = prevTPO_VAL;
        histIBH[idx] = prevIBH;
        histIBL[idx] = prevIBL;
        histSessionHigh[idx] = sessionHigh;
        histSessionLow[idx] = sessionLow;
        histNumBins[idx] = nb;
        histBinSize[idx] = binSize;
        System.arraycopy(tpoBins, 0, histTpoBins[idx], 0, nb);
        System.arraycopy(tpoMask, 0, histTpoMask[idx], 0, nb);
        histTpoBracketCount[idx] = tpoBracketCount;
        histProfileShape[idx] = lastProfileShape;
        histTotalVolume[idx] = prevTotalVolume;
        histBullVolume[idx] = prevTotalBullVolume;
        histBearVolume[idx] = prevTotalBearVolume;
        historyCount++;
    }

    // ------------------------------------------------------------------------

    // Releases all large arrays after the backtest is finished so GC can reclaim memory immediately.
    // Called after SVG export (super.OnDeinit) so the export still has access to the data.
    @Override
    protected void OnDeinit() throws TradingException {
        super.OnDeinit(); // SVG export if StoreChartData = true

        // Working arrays
        tpoBins     = null;
        tpoMask     = null;
        bullTpoBins = null;
        bearTpoBins = null;
        clusterBins = null;

        // Session history arrays (large — sessions × bins)
        histTpoBins         = null;
        histTpoMask         = null;
        histSessionStart    = null;
        histSessionEnd      = null;
        histTPO_POC         = null;
        histTPO_VAH         = null;
        histTPO_VAL         = null;
        histBinSize         = null;
        histIBH             = null;
        histIBL             = null;
        histSessionHigh     = null;
        histSessionLow      = null;
        histNumBins         = null;
        histTotalVolume     = null;
        histBullVolume      = null;
        histBearVolume      = null;
        histTpoBracketCount = null;
        histProfileShape    = null;
    }

    // Calculates the TPO Profile for [prevSessionStart, prevSessionEnd).
    // Pass 1: find session high/low + Initial Balance.
    // Pass 2: accumulate TPO letter counts per bin using bracket bitmasks, then find POC, Value Area, profile shape.
    protected void calculateTPOProfile() throws TradingException {
        debug("VP", "--- calculateTPOProfile START ---");
        debug("VP", "Looking for bars between:");
        debug("VP", "  prevSessionStart = " + SQTime.toFullDateTimeString(prevSessionStart));
        debug("VP", "  prevSessionEnd   = " + SQTime.toFullDateTimeString(prevSessionEnd));

        if (cfgChart() == null) {
            debug("VP", "ERROR: cfgChart() is NULL!");
            return;
        }

        // First pass: find session high/low
        double sessionHigh = Double.MIN_VALUE;
        double sessionLow = Double.MAX_VALUE;
        int barsInSession = 0;
        double sessionTotalVolume = 0;
        int totalBarsScanned = 0;
        long firstSessionBarTime = 0;
        long lastSessionBarTime = 0;

        // Initial Balance tracking
        long ibEndTime = prevSessionStart + getIBPeriodMillis();
        double ibHigh = Double.MIN_VALUE;
        double ibLow = Double.MAX_VALUE;

        int i = 0;
        try {
            while (true) {
                long barTime = cfgChart().Time(i);
                totalBarsScanned++;

                // Debug first few bars to verify M1 chart is working
                /*
                 * if (i < 3) {
                 * debug("VP", "Pass1 M1Bar[" + i + "]: time=" +
                 * SQTime.toFullDateTimeString(barTime));
                 * }
                 */

                // Stop if we've gone past the previous session
                if (barTime < prevSessionStart) {
                    debug("VP", "Pass1: Stopped at index " + i + ", barTime=" + SQTime.toFullDateTimeString(barTime)
                            + " < prevSessionStart");
                    break;
                }

                // Check if bar is within previous session
                if (barTime >= prevSessionStart && barTime < prevSessionEnd) {
                    // Skip Sunday bars
                    if (isSunday(barTime)) {
                        i++;
                        continue;
                    }
                    double hi = cfgChart().High(i);
                    double lo = cfgChart().Low(i);
                    sessionHigh = Math.max(sessionHigh, hi);
                    sessionLow = Math.min(sessionLow, lo);

                    if (barsInSession == 0) {
                        firstSessionBarTime = barTime;
                    }
                    lastSessionBarTime = barTime;
                    barsInSession++;

                    sessionTotalVolume += cfgChart().Volume(i);

                    // Track IB high/low for bars within IB period
                    if (barTime < ibEndTime) {
                        ibHigh = Math.max(ibHigh, hi);
                        ibLow = Math.min(ibLow, lo);
                    }
                    /*
                     * if (barsInSession <= 3 || barsInSession % 200 == 0) {
                     * debug("VP", "Pass1 SessionBar #" + barsInSession + " [idx=" + i + "]: time="
                     * + SQTime.toFullDateTimeString(barTime) + ", close=" + close);
                     * }
                     */
                }

                i++;
            }
        } catch (Exception e) {
            debug("VP", "Pass1: Exception at index " + i + " (end of data): " + e.getMessage());
        }

        debug("VP", "=== PASS 1 SUMMARY ===");
        debug("VP", "  Total M1 bars scanned: " + totalBarsScanned);
        debug("VP", "  Bars in session: " + barsInSession);
        debug("VP", "  First session bar: " + SQTime.toFullDateTimeString(firstSessionBarTime));
        debug("VP", "  Last session bar: " + SQTime.toFullDateTimeString(lastSessionBarTime));
        debug("VP", "  Session HIGH (from highs): " + sessionHigh);
        debug("VP", "  Session LOW (from lows): " + sessionLow);

        if (barsInSession == 0) {
            debug("VP", "ERROR: No bars found in session! Check session boundaries and M1 data availability");
            return;
        }

        if (sessionHigh <= sessionLow) {
            debug("VP", "ERROR: Invalid range - sessionHigh(" + sessionHigh + ") <= sessionLow(" + sessionLow + ")");
            return;
        }

        // Store Initial Balance values
        if (ibHigh > ibLow) {
            prevIBH = ibHigh;
            prevIBL = ibLow;
        }


        double range = sessionHigh - sessionLow;

        // Compute bin size and number of bins based on mode
        double tickSize = cfgChart().getInstrumentInfo().tickStep;
        int numBins;
        double binSize;

        if (cfgBinSizeMode() == 2) {
            // Fixed tick size mode: bin size = cfgTicksPerBin() * tickSize (always
            // constant)
            binSize = cfgTicksPerBin() * tickSize;
            numBins = (int) Math.ceil(range / binSize);
            numBins = Math.max(1, Math.min(numBins, MAX_BINS));
        } else {
            // Range-based mode (default): bin size = range / cfgProfileRows()
            numBins = cfgProfileRows();
            binSize = range / numBins;
        }
        lastNumBins = numBins;



        // Ensure arrays are large enough
        if (tpoBins == null || tpoBins.length < numBins) {
            tpoBins = new int[numBins];
        }

        debug("VP", "=== BIN CALCULATION ===");
        debug("VP", "  Range = " + range);
        debug("VP", "  numBins = " + numBins);
        debug("VP", "  binSize = " + binSize);
        debug("VP", "  Bin 0 covers: " + sessionLow + " to " + (sessionLow + binSize));
        debug("VP", "  Bin " + (numBins - 1) + " covers: " + (sessionLow + (numBins - 1) * binSize) + " to "
                + sessionHigh);

        // Clear bins
        for (int j = 0; j < numBins; j++) {
            tpoBins[j] = 0;
        }
        // Clear bull/bear TPO bins
        if (bullTpoBins == null || bullTpoBins.length < numBins) {
            bullTpoBins = new int[numBins];
        }
        if (bearTpoBins == null || bearTpoBins.length < numBins) {
            bearTpoBins = new int[numBins];
        }
        for (int j = 0; j < numBins; j++) {
            bullTpoBins[j] = 0;
            bearTpoBins[j] = 0;
        }

        // Second pass: accumulate volume into bins + classic TPO letter masks (one TPO
        // per bracket per price level)
        double totalVolume = 0;
        double totalBullVolume = 0;
        double totalBearVolume = 0;
        long bracketMillis = getBracketMillis();
        int bracketCount;

        // Compute bracket count for the session (cap to 62 to keep single-character
        // letters A-Z a-z 0-9)
        long sessionDur = Math.max(0L, prevSessionEnd - prevSessionStart);
        bracketCount = (int) Math.ceil(sessionDur / (double) bracketMillis);
        bracketCount = clampInt(bracketCount, 1, 62);
        lastEffectiveBracketVolume = 0.0;
        this.lastEffectiveBracketMillis = bracketMillis;
        this.tpoBracketCount = bracketCount;

        // Allocate / clear TPO mask (one long per bin; bit k = bracket k has at least
        // one touch at this price level)
        if (tpoMask == null || tpoMask.length < numBins) {
            tpoMask = new long[numBins];
        }
        for (int j = 0; j < numBins; j++) {
            tpoMask[j] = 0L;
        }

        // Reset volume-based bracket state
        cumVolForBrackets = 0.0;
        maxUsedBracket = -1;
        for (int k = 0; k < 62; k++) {
            bracketFirstTime[k] = 0L;
            bracketLastTime[k] = 0L;
        }

        i = 0;
        int volumeAssignments = 0;
        int tpoTouches = 0;

        // Track next bar's time to compute each bar's end time (bars iterate
        // newest-first)
        long nextBarTime = prevSessionEnd;

        try {
            while (true) {
                long barTime = cfgChart().Time(i);

                if (barTime < prevSessionStart) {
                    break;
                }

                if (barTime >= prevSessionStart && barTime < prevSessionEnd) {

                    // Skip Sunday bars
                    if (isSunday(barTime)) {
                        i++;
                        continue;
                    }

                    // ----- VOLUME tracking -----
                    double barVol = cfgChart().Volume(i);

                    totalVolume += barVol;
                    boolean isBullVol = (cfgChart().Close(i) >= cfgChart().Open(i));
                    if (isBullVol) {
                        totalBullVolume += barVol;
                    } else {
                        totalBearVolume += barVol;
                    }

                    // ----- CLASSIC TPO LETTERS (bracket-based, Low..High range) -----
                    // Compute which bracket(s) this bar spans.
                    // barTime = bar open, barEndTime = next bar's open (or session end)
                    long barEndTime = Math.min(nextBarTime, prevSessionEnd);

                    int startBracket, endBracket;
                    // Time-based: spread letters across all brackets the bar covers
                    startBracket = (int) ((barTime - prevSessionStart) / bracketMillis);
                    endBracket = (int) ((Math.max(barEndTime - 1, barTime) - prevSessionStart) / bracketMillis);
                    startBracket = clampInt(startBracket, 0, 61);
                    endBracket = clampInt(endBracket, 0, 61);



                    // Build combined bitmask for all brackets this bar covers
                    long combinedBits = 0L;
                    for (int bk = startBracket; bk <= endBracket; bk++) {
                        combinedBits |= (1L << bk);
                        if (bracketFirstTime[bk] == 0L)
                            bracketFirstTime[bk] = barTime;
                        bracketLastTime[bk] = barTime;
                        if (bk > maxUsedBracket)
                            maxUsedBracket = bk;
                    }

                    // Use startBracket as representative for IB breakout tracking
                    int bracketIndex = startBracket;

                    double lo = cfgChart().Low(i);
                    double hi = cfgChart().High(i);
                    double cl = cfgChart().Close(i);
                    double op = cfgChart().Open(i);
                    boolean isBullBar = (cl >= op);

                    int loBin = (int) ((lo - sessionLow) / binSize);
                    int hiBin = (int) ((hi - sessionLow) / binSize);

                    loBin = Math.max(0, Math.min(numBins - 1, loBin));
                    hiBin = Math.max(0, Math.min(numBins - 1, hiBin));

                    if (hiBin < loBin) {
                        int tmp = hiBin;
                        hiBin = loBin;
                        loBin = tmp;
                    }



                    for (int b = loBin; b <= hiBin; b++) {
                        long before = tpoMask[b];
                        long after = before | combinedBits;
                        tpoTouches += Long.bitCount(after) - Long.bitCount(before);
                        tpoMask[b] = after;
                        // Track per-direction TPO counts (simple: each touched bin +1)
                        if (isBullBar) {
                            bullTpoBins[b]++;
                        } else {
                            bearTpoBins[b]++;
                        }
                    }

                    // Debug first few and periodic
                    if (volumeAssignments <= 5 || volumeAssignments % 500 == 0) {
                        debug("VP", "Pass2 #" + volumeAssignments + ": time=" + SQTime.toFullDateTimeString(barTime) +
                                ", vol=" + barVol +
                                ", brackets=" + startBracket + "-" + endBracket + "/" + bracketCount);
                    }
                    volumeAssignments++;

                }

                nextBarTime = barTime;
                i++;
            }
        } catch (Exception e) {
            debug("VP", "Pass2: Exception at index " + i + " (end of data)");
        }



        // Convert masks -> TPO counts, total TPO, and single prints
        long totalTPO = 0;
        int singlePrintCount = 0;
        int tpoHighBin = -1;
        int tpoLowBin = -1;

        for (int b = 0; b < numBins; b++) {
            int cnt = Long.bitCount(tpoMask[b]);
            tpoBins[b] = cnt;
            totalTPO += cnt;

            if (cnt > 0) {
                if (tpoLowBin == -1)
                    tpoLowBin = b;
                tpoHighBin = b;
                if (cnt == 1)
                    singlePrintCount++;
            }
        }

        this.lastSinglePrintCount = singlePrintCount;
        this.lastTpoHighBin = tpoHighBin;
        this.lastTpoLowBin = tpoLowBin;

        // Poor / Excess high-low (classic heuristic):
        // - Excess high/low: extreme bin has exactly 1 TPO
        // - Poor high/low: extreme bin has 2+ TPO (no excess)
        this.lastExcessHigh = (tpoHighBin >= 0 && tpoBins[tpoHighBin] == 1);
        this.lastExcessLow = (tpoLowBin >= 0 && tpoBins[tpoLowBin] == 1);
        this.lastPoorHigh = (tpoHighBin >= 0 && tpoBins[tpoHighBin] >= 2);
        this.lastPoorLow = (tpoLowBin >= 0 && tpoBins[tpoLowBin] >= 2);

        // --- Buying Tail: count single-print bins from bottom up ---
        int buyTail = 0;
        if (tpoLowBin >= 0) {
            for (int bt = tpoLowBin; bt <= tpoHighBin; bt++) {
                if (tpoBins[bt] == 1)
                    buyTail++;
                else
                    break;
            }
        }
        this.lastBuyingTailLen = buyTail;

        // --- Selling Tail: count single-print bins from top down ---
        int sellTail = 0;
        if (tpoHighBin >= 0) {
            for (int st = tpoHighBin; st >= tpoLowBin; st--) {
                if (tpoBins[st] == 1)
                    sellTail++;
                else
                    break;
            }
        }
        this.lastSellingTailLen = sellTail;

        // --- Profile Shape Detection ---
        this.lastProfileShape = detectProfileShape(tpoLowBin, tpoHighBin);

        // --- Ledge Detection ---
        // A ledge: 3+ consecutive bins ending at the exact same TPO count,
        // forming a flat vertical edge. Upward ledge = edge on the right/high side,
        // Downward ledge = edge on the right/low side.
        this.lastLedgeUpPrice = 0;
        this.lastLedgeDownPrice = 0;
        if (tpoLowBin >= 0 && tpoHighBin > tpoLowBin) {
            int bestLedgeUpLen = 0;
            int bestLedgeUpBin = -1;
            int bestLedgeDownLen = 0;
            int bestLedgeDownBin = -1;
            int runStart = tpoLowBin;
            for (int lb = tpoLowBin + 1; lb <= tpoHighBin; lb++) {
                if (tpoBins[lb] == tpoBins[runStart] && tpoBins[lb] > 0) {
                    int runLen = lb - runStart + 1;
                    // Upper ledge: consecutive bins with same count in upper half
                    int midBin = tpoLowBin + (tpoHighBin - tpoLowBin) / 2;
                    if (lb >= midBin && runLen > bestLedgeUpLen && runLen >= 3) {
                        bestLedgeUpLen = runLen;
                        bestLedgeUpBin = lb; // top of the run
                    }
                    // Lower ledge: consecutive bins with same count in lower half
                    if (runStart <= midBin && runLen > bestLedgeDownLen && runLen >= 3) {
                        bestLedgeDownLen = runLen;
                        bestLedgeDownBin = runStart; // bottom of the run
                    }
                } else {
                    runStart = lb;
                }
            }
            if (bestLedgeUpBin >= 0) {
                this.lastLedgeUpPrice = sessionLow + (bestLedgeUpBin + 0.5) * binSize;
            }
            if (bestLedgeDownBin >= 0) {
                this.lastLedgeDownPrice = sessionLow + (bestLedgeDownBin + 0.5) * binSize;
            }
        }

        debug("VP", "Pass2 complete: volumeAssignments=" + volumeAssignments + ", totalVolume=" + totalVolume
                + ", bracketCount=" + bracketCount + ", tpoTouches(unique)=" + tpoTouches
                + ", totalTPO=" + totalTPO + ", singlePrintBins=" + singlePrintCount
                + ", hiBin=" + tpoHighBin + ", loBin=" + tpoLowBin);

        // Calculate TPO POC + Value Area (range-based TPO histogram)
        calculateTPOLevels(totalTPO, binSize, sessionLow, numBins);

        // VCP disabled for TPO – use standard levels
        prevTPO_VPOC = prevTPO_POC;
        prevTPO_VVAH = prevTPO_VAH;
        prevTPO_VVAL = prevTPO_VAL;

        // Bull/Bear POC from TPO bins
        int bullPocIdx = 0;
        int bullMax = bullTpoBins[0];
        int bearPocIdx = 0;
        int bearMax = bearTpoBins[0];
        for (int j = 1; j < numBins; j++) {
            if (bullTpoBins[j] > bullMax) {
                bullMax = bullTpoBins[j];
                bullPocIdx = j;
            }
            if (bearTpoBins[j] > bearMax) {
                bearMax = bearTpoBins[j];
                bearPocIdx = j;
            }
        }
        prevTPO_BullPOC = (bullMax > 0) ? sessionLow + (bullPocIdx + 0.5) * binSize : 0;
        prevTPO_BearPOC = (bearMax > 0) ? sessionLow + (bearPocIdx + 0.5) * binSize : 0;

        // Store total volume figures
        prevTotalVolume = totalVolume;
        prevTotalBullVolume = totalBullVolume;
        prevTotalBearVolume = totalBearVolume;

        debug("VP", "--- calculateTPOProfile END ---");
        debug("VP", "FINAL RESULTS: TPO_POC=" + prevTPO_POC + ", TPO_VAH=" + prevTPO_VAH + ", TPO_VAL=" + prevTPO_VAL);
        // Save to history only when chart export is enabled — history arrays are large
        if (cfgStoreChartData()) {
            pushSessionHistory(sessionHigh, sessionLow, binSize);
        }

        // SVG export deferred to OnDeinit()

    }

    // Finds POC (bin with most TPO touches) and Value Area (VAH/VAL) from the TPO histogram.
    protected void calculateTPOLevels(long totalTPO, double binSize, double sessionLow, int numBins) {
        debug("VP", "=== TPO CALCULATION ===");
        debug("VP", "  Total TPO: " + totalTPO);

        if (totalTPO <= 0) {
            prevTPO_POC = 0;
            prevTPO_VAH = 0;
            prevTPO_VAL = 0;
            debug("VP", "  TPO skipped: totalTPO <= 0");
            return;
        }

        int tpoPocIndex = findMaxIndexInt(tpoBins, numBins);
        this.lastTpoPocIndex = tpoPocIndex;
        prevTPO_POC = sessionLow + (tpoPocIndex + 0.5) * binSize;

        double[] va = calculateValueAreaInt(tpoPocIndex, totalTPO, binSize, sessionLow, numBins);
        prevTPO_VAL = va[0];
        prevTPO_VAH = va[1];



        debug("VP", "  TPO POC index: " + tpoPocIndex + " price=" + prevTPO_POC);
        debug("VP", "  TPO VAH=" + prevTPO_VAH + ", TPO VAL=" + prevTPO_VAL);
    }

    // Returns the index of the maximum value in arr[0..len-1].
    protected int findMaxIndexInt(int[] arr, int len) {
        int idx = 0;
        int max = arr[0];
        for (int i = 1; i < len; i++) {
            if (arr[i] > max) {
                max = arr[i];
                idx = i;
            }
        }
        return idx;
    }

    // Value Area expansion for integer TPO histograms.
    // Expands outward from POC, always absorbing the higher-TPO side first,
    // until ValueAreaPct% of total TPO is captured. Returns {VAL, VAH}.
    protected double[] calculateValueAreaInt(int pocIndex, long total, double binSize, double sessionLow, int numBins) {
        double target = total * (cfgValueAreaPct() / 100.0);
        long accum = tpoBins[pocIndex];

        int upper = pocIndex;
        int lower = pocIndex;

        int iterations = 0;
        while (accum < target) {
            boolean canUp = (upper + 1) < numBins;
            boolean canDn = (lower - 1) >= 0;
            if (!canUp && !canDn)
                break;

            int above = canUp ? tpoBins[upper + 1] : -1;
            int below = canDn ? tpoBins[lower - 1] : -1;

            iterations++;

            if (above >= below) {
                upper++;
                accum += tpoBins[upper];
            } else {
                lower--;
                accum += tpoBins[lower];
            }
        }

        double val = sessionLow + lower * binSize;
        double vah = sessionLow + (upper + 1) * binSize;
        return new double[] { val, vah };
    }

    // Classifies the TPO profile shape based on POC position and bin distribution.
    // Returns: 1=P-shape, 2=b-shape, 3=D-shape, 4=Double Distribution, 5=Other
    protected int detectProfileShape(int lowBin, int highBin) {
        if (lowBin < 0 || highBin < 0 || highBin <= lowBin)
            return 5;

        int activeBins = highBin - lowBin + 1;
        if (activeBins < 3)
            return 5;

        // Find POC position relative to the active range
        int pocIdx = lastTpoPocIndex;
        double pocRelPos = (double) (pocIdx - lowBin) / (double) (activeBins - 1); // 0.0=bottom, 1.0=top

        // --- Check Double Distribution first (two peaks separated by single-print
        // valley) ---
        // Scan for a valley (consecutive bins with count <= 1) separating two clusters
        int valleyStart = -1, valleyEnd = -1;
        boolean inValley = false;
        int thirdLow = lowBin + activeBins / 4;
        int thirdHigh = highBin - activeBins / 4;
        for (int b = thirdLow; b <= thirdHigh; b++) {
            if (tpoBins[b] <= 1) {
                if (!inValley) {
                    valleyStart = b;
                    inValley = true;
                }
                valleyEnd = b;
            } else {
                if (inValley && (valleyEnd - valleyStart + 1) >= 2) {
                    // Check if there are substantial TPOs both above and below the valley
                    int belowSum = 0, aboveSum = 0;
                    for (int bb = lowBin; bb < valleyStart; bb++)
                        belowSum += tpoBins[bb];
                    for (int ab = valleyEnd + 1; ab <= highBin; ab++)
                        aboveSum += tpoBins[ab];
                    if (belowSum >= 3 && aboveSum >= 3) {
                        return 4; // Double Distribution
                    }
                }
                inValley = false;
            }
        }
        // Final valley check at loop end
        if (inValley && (valleyEnd - valleyStart + 1) >= 2) {
            int belowSum = 0, aboveSum = 0;
            for (int bb = lowBin; bb < valleyStart; bb++)
                belowSum += tpoBins[bb];
            for (int ab = valleyEnd + 1; ab <= highBin; ab++)
                aboveSum += tpoBins[ab];
            if (belowSum >= 3 && aboveSum >= 3) {
                return 4; // Double Distribution
            }
        }

        // --- P-shape: POC in top third, thin bottom ---
        if (pocRelPos > 0.66) {
            // Check that the bottom third is thin
            int thirdSize = activeBins / 3;
            double bottomAvg = 0;
            for (int b = lowBin; b < lowBin + thirdSize; b++)
                bottomAvg += tpoBins[b];
            bottomAvg /= Math.max(1, thirdSize);
            double topAvg = 0;
            for (int b = highBin - thirdSize + 1; b <= highBin; b++)
                topAvg += tpoBins[b];
            topAvg /= Math.max(1, thirdSize);
            if (topAvg > bottomAvg * 1.5)
                return 1; // P-shape
        }

        // --- b-shape: POC in bottom third, thin top ---
        if (pocRelPos < 0.33) {
            int thirdSize = activeBins / 3;
            double topAvg = 0;
            for (int b = highBin - thirdSize + 1; b <= highBin; b++)
                topAvg += tpoBins[b];
            topAvg /= Math.max(1, thirdSize);
            double bottomAvg = 0;
            for (int b = lowBin; b < lowBin + thirdSize; b++)
                bottomAvg += tpoBins[b];
            bottomAvg /= Math.max(1, thirdSize);
            if (bottomAvg > topAvg * 1.5)
                return 2; // b-shape
        }

        // --- D-shape: POC near center, relatively symmetric ---
        if (pocRelPos >= 0.33 && pocRelPos <= 0.66) {
            // Check symmetry: compare top-half TPO sum vs bottom-half TPO sum
            int mid = lowBin + activeBins / 2;
            int bottomSum = 0, topSum = 0;
            for (int b = lowBin; b < mid; b++)
                bottomSum += tpoBins[b];
            for (int b = mid; b <= highBin; b++)
                topSum += tpoBins[b];
            double ratio = (double) Math.min(bottomSum, topSum) / Math.max(1, Math.max(bottomSum, topSum));
            if (ratio > 0.6)
                return 3; // D-shape (balanced/symmetric)
        }

        return 5; // Other
    }

    // Exports multi-session SVG chart: TPO profile bars, candlesticks, POC/VAH/VAL levels, shape labels.
    // Called from OnDeinit() when StoreChartData = true.
    protected void exportMultiSessionSVG() {
        if (historyCount == 0)
            return;
        String folder = resolveExportFolder();
        new File(folder).mkdirs();

        int latest = historyCount - 1;
        String t1 = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new java.util.Date(histSessionStart[latest]));

        // Instrument + session info (used in filename and title)
        String symbolName = "";
        try {
            symbolName = cfgChart().Symbol;
        } catch (Exception e) {
        }
        String[] SESSION_NAMES = { "", "Previous Day", "Previous Week", "Previous Month",
                "Previous Year", "Actual Day", "Actual Week", "Actual Month", "Actual Year" };
        String sessionLabel = (cfgSessionType() >= 1 && cfgSessionType() < SESSION_NAMES.length)
                ? SESSION_NAMES[cfgSessionType()]
                : "Session";
        String fileSymbol = symbolName.isEmpty() ? "" : symbolName.replaceAll("[^A-Za-z0-9_-]", "") + "_";
        String fileSession = sessionLabel.replaceAll("\\s+", "");
        String tfName = "";
        try {
            tfName = cfgChart().Timeframe;
        } catch (Exception e) {
        }
        String binLabel = (cfgBinSizeMode() == 2) ? "Fixed Tick" : "Range";
        // For fixed tick mode, compute the constant bin size for SVG rendering
        double fixedTickBinSize = 0;
        if (cfgBinSizeMode() == 2) {
            double tickSize = cfgChart().getInstrumentInfo().tickStep;
            fixedTickBinSize = cfgTicksPerBin() * tickSize;
        }
        String titleExtra = (tfName.isEmpty() ? "" : " | TF:" + tfName)
                + " | Bins:" + binLabel;
        String strategyNameForFile = (getStrategy() != null ? getStrategy().getStrategyName() : "");
        strategyNameForFile = strategyNameForFile == null ? "" : strategyNameForFile.replaceAll("[^A-Za-z0-9_\\-]", "_");
        String filePath = folder + File.separator
                + "TPO_" + (strategyNameForFile.isEmpty() ? "" : strategyNameForFile + "_") + fileSymbol + fileSession + "_" + fileRandomSuffix + ".svg";
        saveChartPath(filePath);

        int N = historyCount;

        double globalHigh = Double.MIN_VALUE;
        double globalLow = Double.MAX_VALUE;
        for (int k = 0; k < N; k++) {
            globalHigh = Math.max(globalHigh, histSessionHigh[k]);
            globalLow = Math.min(globalLow, histSessionLow[k]);
        }
        if (globalHigh <= globalLow)
            return;
        double globalRange = globalHigh - globalLow;
        globalLow -= globalRange * 0.02;
        globalHigh += globalRange * 0.02;
        globalRange = globalHigh - globalLow;
        int marginLeft = 80;
        int marginRight = 70;
        int marginTop = 60;
        int marginBottom = 95;
        int plotH = 800;
        int plotW = Math.max(800, N * 300);
        // Enable replay mode for "Actual" session types (5=Day, 6=Week,
        // 7=Month, 8=Year)
        boolean isReplayMode = (cfgSessionType() >= 5);
        int totalW = marginLeft + plotW + marginRight;
        int totalH = marginTop + plotH + marginBottom;

        // Cumulative session durations (skip gaps between sessions such as weekends)
        long[] sesDur = new long[N];
        long totalDur = 0;
        for (int k = 0; k < N; k++) {
            sesDur[k] = histSessionEnd[k] - histSessionStart[k];
            if (sesDur[k] <= 0)
                sesDur[k] = 1;
            totalDur += sesDur[k];
        }
        if (totalDur <= 0)
            return;

        // Pre-compute pixel start/end for each session
        double[] sesStartPx = new double[N];
        double[] sesEndPx = new double[N];
        long cumDur = 0;
        for (int k = 0; k < N; k++) {
            sesStartPx[k] = marginLeft + (double) cumDur / totalDur * plotW;
            cumDur += sesDur[k];
            sesEndPx[k] = marginLeft + (double) cumDur / totalDur * plotW;
        }

        double candleAlpha = Math.max(0.1, Math.min(1.0, DEFAULT_DARK_CANDLE_OPACITY / 100.0));
        double profileAlpha = Math.max(0.1, Math.min(1.0, DEFAULT_DARK_PROFILE_OPACITY / 100.0));

        String[] SHAPE_NAMES = { "", "P-shape", "b-shape", "D-shape", "Double Dist.", "Other" };

        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            w.write("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\""
                    + totalW
                    + "\" height=\"" + totalH + "\" viewBox=\"0 0 " + totalW + " " + totalH + "\">");
            w.write("<rect id=\"bgRect\" width=\"" + totalW + "\" height=\"" + totalH
                    + "\" fill=\"" + DEFAULT_DARK_BG_COLOR + "\"/>\n");

            // Title
            String title = "TPO Profile" + (symbolName.isEmpty() ? "" : " - " + symbolName)
                    + " | " + sessionLabel + titleExtra + " (" + N + " sessions)";
            w.write("<text id=\"chartTitle\" class=\"thTitle\" x=\"" + marginLeft
                    + "\" y=\"35\" font-family=\"Sans-Serif\" font-size=\"16\" font-weight=\"700\" fill=\"#ffffff\">"
                    + "StrategyQuantX - " + svgEsc(title) + "</text>\n");

            // Plot background
            w.write("<rect id=\"plotRect\" x=\"" + marginLeft + "\" y=\"" + marginTop
                    + "\" width=\"" + plotW + "\" height=\"" + plotH
                    + "\" fill=\"" + DEFAULT_DARK_PLOT_COLOR + "\" rx=\"4\"/>\n");

            // Price axis
            int priceSteps = 20;
            for (int s = 0; s <= priceSteps; s++) {
                double price = globalLow + globalRange * s / priceSteps;
                double y = marginTop + plotH - (price - globalLow) / globalRange * plotH;
                w.write("<text class=\"thPrice\" x=\"" + (marginLeft + plotW + 8) + "\" y=\"" + (y + 4)
                        + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#888\" text-anchor=\"start\">"
                        + String.format(Locale.US, "%.5f", price) + "</text>\n");
                w.write("<line class=\"thGrid\" x1=\"" + marginLeft + "\" y1=\"" + y
                        + "\" x2=\"" + (totalW - marginRight) + "\" y2=\"" + y
                        + "\" stroke=\"#2a2a4a\" stroke-width=\"0.5\"/>\n");
            }

            // Session boundary lines + date labels + shape labels + candles + profiles
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);

                // Vertical dashed session boundary
                w.write("<line class=\"thSesDiv\" x1=\"" + sesX + "\" y1=\"" + marginTop
                        + "\" x2=\"" + sesX + "\" y2=\"" + (marginTop + plotH)
                        + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n");
                // Date label at bottom
                String dateLabel = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US)
                        .format(new java.util.Date(histSessionStart[k]));
                double labelX = (sesX + sesXEnd) / 2;
                w.write("<text class=\"thDate\" x=\"" + labelX + "\" y=\""
                        + (marginTop + plotH + 55)
                        + "\" font-family=\"Sans-Serif\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">"
                        + svgEsc(dateLabel) + "</text>\n");

                // Volume labels – position relative to session profile (wrapped in sStats for
                // autoscale)
                double sesHighY = marginTop + plotH - (histSessionHigh[k] - globalLow) / globalRange * plotH;
                double sesLowY = marginTop + plotH - (histSessionLow[k] - globalLow) / globalRange * plotH;
                double volY1, volY2, volY3;
                if (sesHighY - marginTop > 40) {
                    // Enough room above profile
                    volY1 = sesHighY - 26;
                    volY2 = sesHighY - 15;
                    volY3 = sesHighY - 4;
                } else {
                    // Profile near top — place below session low
                    volY1 = sesLowY + 14;
                    volY2 = sesLowY + 25;
                    volY3 = sesLowY + 36;
                }
                String volTotal = String.format(Locale.US, "%.0f", histTotalVolume[k]);
                String volBull = String.format(Locale.US, "%.0f", histBullVolume[k]);
                String volBear = String.format(Locale.US, "%.0f", histBearVolume[k]);
                w.write("<g class=\"sStats\" data-shy=\"" + String.format(Locale.US, "%.2f", sesHighY) + "\">\n");
                w.write("<text class=\"thVol\" id=\"rpVol_" + k + "\" x=\"" + labelX + "\" y=\"" + volY1
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Vol: "
                        + volTotal + "</text>\n");
                w.write("<text id=\"rpBull_" + k + "\" x=\"" + labelX + "\" y=\"" + volY2
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#4caf50\" text-anchor=\"middle\">Bull: "
                        + volBull + "</text>\n");
                w.write("<text id=\"rpBear_" + k + "\" x=\"" + labelX + "\" y=\"" + volY3
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#f44336\" text-anchor=\"middle\">Bear: "
                        + volBear + "</text>\n");
                w.write("</g>\n"); // close sStats

                // Shape label above chart
                if (cfgShowShapeLabel() && histProfileShape[k] >= 1 && histProfileShape[k] <= 5) {
                    String shapeName = SHAPE_NAMES[histProfileShape[k]];
                    String shapeColor;
                    switch (histProfileShape[k]) {
                        case 1:
                            shapeColor = "#4fc3f7";
                            break;
                        case 2:
                            shapeColor = "#ff8a65";
                            break;
                        case 3:
                            shapeColor = "#81c784";
                            break;
                        case 4:
                            shapeColor = "#ce93d8";
                            break;
                        default:
                            shapeColor = "#bdbdbd";
                            break;
                    }
                    w.write("<text x=\"" + labelX + "\" y=\""
                            + (marginTop - 8)
                            + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\""
                            + shapeColor + "\" text-anchor=\"middle\">"
                            + shapeName + "</text>\n");
                }
            }

            // --- Auto-scale wrapper: only rects/lines go inside chartG (no text) ---
            w.write("<defs><clipPath id=\"plotClip\"><rect x=\"" + marginLeft + "\" y=\"" + marginTop + "\" width=\""
                    + plotW + "\" height=\"" + plotH + "\"/></clipPath></defs>\n");
            w.write("<g id=\"chartG\" clip-path=\"url(#plotClip)\">\n");

            // Candles group – drawn per-session to align with TPO letters
            w.write("<g id=\"candlesG\" opacity=\"" + String.format(Locale.US, "%.2f", candleAlpha) + "\">\n");
            if (cfgShowCandlesticks()) {
                for (int k = 0; k < N; k++) {
                    int sesPx = (int) sesStartPx[k];
                    int sesPw = (int) Math.max(10, sesEndPx[k] - sesStartPx[k]);
                    long sesDurK = histSessionEnd[k] - histSessionStart[k];
                    if (sesDurK <= 0)
                        sesDurK = 1;
                    drawSvgCandlesUnified(w, sesPx, sesPw, marginTop, plotH,
                            globalLow, globalRange, histSessionStart[k], histSessionEnd[k], sesDurK, 1.0);
                }
            }
            w.write("</g>\n");

            // TPO profile group – RECTS ONLY inside chartG (VA bg, blocks, IB boxes)
            w.write("<g id=\"profileG\" opacity=\"" + String.format(Locale.US, "%.2f", profileAlpha) + "\">\n");
            for (int k = 0; k < N; k++) {
                if (isReplayMode)
                    w.write("<g id=\"rpProfile_" + k + "\">\n");
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);

                // Draw Value Area background rect
                double vaLow = histTPO_VAL[k];
                double vaHigh = histTPO_VAH[k];
                if (vaHigh > vaLow) {
                    double vaY1 = marginTop + plotH - (vaHigh - globalLow) / globalRange * plotH;
                    double vaY2 = marginTop + plotH - (vaLow - globalLow) / globalRange * plotH;
                    w.write("<rect x=\"" + sesX + "\" y=\"" + vaY1
                            + "\" width=\"" + sesPixW + "\" height=\"" + Math.max(1, vaY2 - vaY1)
                            + "\" fill=\"rgba(70,130,180,0.30)\"/>\n");
                }

                // Block mode only: draw rects inside chartG (text mode letters go to tpoTextG
                // below)
                if (cfgUseBlockMode()) {
                    int bracketCount = histTpoBracketCount[k];
                    if (bracketCount <= 0)
                        bracketCount = 1;
                    int sesNB = histNumBins[k];
                    double sesRange = histSessionHigh[k] - histSessionLow[k];
                    double sesBinSize = (fixedTickBinSize > 0) ? fixedTickBinSize
                            : ((sesNB > 0 && sesRange > 0) ? sesRange / sesNB : 1);
                    // Merge fine-grained bins down to MAX_SVG_BINS_PER_SESSION visual rows
                    int mergeFactor = Math.max(1, (sesNB + MAX_SVG_BINS_PER_SESSION - 1) / MAX_SVG_BINS_PER_SESSION);
                    int svgBins = (sesNB + mergeFactor - 1) / mergeFactor;
                    int maxCols = 0;
                    for (int sj = 0; sj < svgBins; sj++) {
                        int binStart = sj * mergeFactor;
                        int binEnd = Math.min(binStart + mergeFactor, sesNB);
                        long mergedMask = 0L;
                        for (int bj = binStart; bj < binEnd; bj++)
                            mergedMask |= histTpoMask[k][bj];
                        int cnt = Long.bitCount(mergedMask);
                        if (cnt > maxCols)
                            maxCols = cnt;
                    }
                    if (maxCols <= 0)
                        maxCols = 1;
                    double letterW = Math.min(14.0, sesPixW / maxCols);
                    String[] BRACKET_COLORS = {
                            "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb", "#64b5f6",
                            "#4fc3f7", "#4dd0e1", "#4db6ac", "#81c784", "#aed581", "#dce775",
                            "#fff176", "#ffd54f", "#ffb74d", "#ff8a65"
                    };
                    for (int sj = 0; sj < svgBins; sj++) {
                        int binStart = sj * mergeFactor;
                        int binEnd = Math.min(binStart + mergeFactor, sesNB);
                        long mask = 0L;
                        for (int bj = binStart; bj < binEnd; bj++)
                            mask |= histTpoMask[k][bj];
                        if (mask == 0L)
                            continue;
                        double binLowPrice = histSessionLow[k] + binStart * sesBinSize;
                        double binHighPrice = histSessionLow[k] + binEnd * sesBinSize;
                        double y1 = marginTop + plotH - (binHighPrice - globalLow) / globalRange * plotH;
                        double y2 = marginTop + plotH - (binLowPrice - globalLow) / globalRange * plotH;
                        double rowH = Math.max(1, y2 - y1);
                        double blockW = Math.max(2, letterW * 0.85);
                        double blockH = Math.max(1, rowH * 0.9);
                        double blockY = y1 + (rowH - blockH) / 2;
                        int col = 0;
                        for (int b = 0; b < bracketCount && b < 62; b++) {
                            if ((mask & (1L << b)) != 0) {
                                String lclr = BRACKET_COLORS[b % BRACKET_COLORS.length];
                                double bx = sesX + col * letterW;
                                w.write("<rect class=\"tpoB\" data-bi=\"" + (b % BRACKET_COLORS.length) + "\" x=\"" + bx
                                        + "\" y=\"" + blockY
                                        + "\" width=\"" + String.format(Locale.US, "%.1f", blockW)
                                        + "\" height=\"" + String.format(Locale.US, "%.1f", blockH)
                                        + "\" fill=\"" + lclr + "\" rx=\"0.5\"/>");
                                col++;
                            }
                        }
                    }
                }

                if (isReplayMode)
                    w.write("</g>\n");
            }
            w.write("</g>\n");

            // Level LINES only inside chartG (text labels go to tpoTextG below)
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);
                int sesLevelW = (int) sesPixW;
                // POC line
                {
                    double y = marginTop + plotH - (histTPO_POC[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<line x1=\"" + (int) sesX + "\" y1=\"" + y + "\" x2=\"" + ((int) sesX + sesLevelW)
                                + "\" y2=\"" + y
                                + "\" stroke=\"#ffa500\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                // VAH line
                {
                    double y = marginTop + plotH - (histTPO_VAH[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<line x1=\"" + (int) sesX + "\" y1=\"" + y + "\" x2=\"" + ((int) sesX + sesLevelW)
                                + "\" y2=\"" + y
                                + "\" stroke=\"#4caf50\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                // VAL line
                {
                    double y = marginTop + plotH - (histTPO_VAL[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<line x1=\"" + (int) sesX + "\" y1=\"" + y + "\" x2=\"" + ((int) sesX + sesLevelW)
                                + "\" y2=\"" + y
                                + "\" stroke=\"#f44336\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                // IB box
                if (histIBH[k] > 0 && histIBL[k] > 0) {
                    double ibhY = marginTop + plotH - (histIBH[k] - globalLow) / globalRange * plotH;
                    double iblY = marginTop + plotH - (histIBL[k] - globalLow) / globalRange * plotH;
                    double ibTop = Math.min(ibhY, iblY);
                    double ibHeight = Math.abs(iblY - ibhY);
                    w.write("<rect x=\"" + (int) sesX + "\" y=\"" + ibTop
                            + "\" width=\"" + sesLevelW + "\" height=\"" + ibHeight
                            + "\" fill=\"#ff9800\" fill-opacity=\"0.13\" stroke=\"none\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                // IBH line
                if (histIBH[k] > 0) {
                    double y = marginTop + plotH - (histIBH[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<line x1=\"" + (int) sesX + "\" y1=\"" + y + "\" x2=\"" + ((int) sesX + sesLevelW)
                                + "\" y2=\"" + y
                                + "\" stroke=\"#00bcd4\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                // IBL line
                if (histIBL[k] > 0) {
                    double y = marginTop + plotH - (histIBL[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<line x1=\"" + (int) sesX + "\" y1=\"" + y + "\" x2=\"" + ((int) sesX + sesLevelW)
                                + "\" y2=\"" + y
                                + "\" stroke=\"#e040fb\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
            }

            // Close auto-scale wrapper (chartG) – NO text was written inside it
            w.write("</g>\n"); // close chartG

            // --- tpoTextG: ALL text elements rendered OUTSIDE chartG for sharp rendering
            // ---
            // These get repositioned by JS directly (y = sy*oy + ty), no scale transform
            w.write("<g id=\"tpoTextG\" clip-path=\"url(#plotClip)\">\n");

            // TPO letter text (text mode only)
            if (!cfgUseBlockMode()) {
                String[] BRACKET_COLORS_T = {
                        "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb", "#64b5f6",
                        "#4fc3f7", "#4dd0e1", "#4db6ac", "#81c784", "#aed581", "#dce775",
                        "#fff176", "#ffd54f", "#ffb74d", "#ff8a65"
                };
                for (int k = 0; k < N; k++) {
                    double sesX = sesStartPx[k];
                    double sesXEnd = sesEndPx[k];
                    double sesPixW = Math.max(10, sesXEnd - sesX);
                    int bracketCount = histTpoBracketCount[k];
                    if (bracketCount <= 0)
                        bracketCount = 1;
                    int sesNB = histNumBins[k];
                    double sesRange = histSessionHigh[k] - histSessionLow[k];
                    double sesBinSize = (fixedTickBinSize > 0) ? fixedTickBinSize
                            : ((sesNB > 0 && sesRange > 0) ? sesRange / sesNB : 1);
                    // Merge fine-grained bins down to MAX_SVG_BINS_PER_SESSION visual rows
                    int mergeFactor = Math.max(1, (sesNB + MAX_SVG_BINS_PER_SESSION - 1) / MAX_SVG_BINS_PER_SESSION);
                    int svgBins = (sesNB + mergeFactor - 1) / mergeFactor;
                    int maxCols = 0;
                    for (int sj = 0; sj < svgBins; sj++) {
                        int binStart = sj * mergeFactor;
                        int binEnd = Math.min(binStart + mergeFactor, sesNB);
                        long mergedMask = 0L;
                        for (int bj = binStart; bj < binEnd; bj++)
                            mergedMask |= histTpoMask[k][bj];
                        int cnt = Long.bitCount(mergedMask);
                        if (cnt > maxCols)
                            maxCols = cnt;
                    }
                    if (maxCols <= 0)
                        maxCols = 1;
                    double letterW = Math.min(14.0, sesPixW / maxCols);
                    for (int sj = 0; sj < svgBins; sj++) {
                        int binStart = sj * mergeFactor;
                        int binEnd = Math.min(binStart + mergeFactor, sesNB);
                        long mask = 0L;
                        for (int bj = binStart; bj < binEnd; bj++)
                            mask |= histTpoMask[k][bj];
                        if (mask == 0L)
                            continue;
                        double binLowPrice = histSessionLow[k] + binStart * sesBinSize;
                        double binHighPrice = histSessionLow[k] + binEnd * sesBinSize;
                        double y1 = marginTop + plotH - (binHighPrice - globalLow) / globalRange * plotH;
                        double y2 = marginTop + plotH - (binLowPrice - globalLow) / globalRange * plotH;
                        double rowH = Math.max(1, y2 - y1);
                        // Scale font to row height; clamp between 4px (min readable) and DEFAULT_TPO_FONT_SIZE
                        double fontSize = Math.min(DEFAULT_TPO_FONT_SIZE, Math.max(4.0, rowH * 0.85));
                        int col = 0;
                        for (int b = 0; b < bracketCount && b < 62; b++) {
                            if ((mask & (1L << b)) != 0) {
                                char ch = bracketChar(b);
                                String lclr = BRACKET_COLORS_T[b % BRACKET_COLORS_T.length];
                                double lx = sesX + col * letterW;
                                double fOff = fontSize * 0.85;
                                double ly = y1 + fOff;
                                w.write("<text class=\"tpoL\" data-bi=\"" + (b % BRACKET_COLORS_T.length) + "\" x=\""
                                        + lx + "\" y=\"" + ly
                                        + "\" data-by=\"" + String.format(Locale.US, "%.2f", y1)
                                        + "\" data-off=\"" + String.format(Locale.US, "%.2f", fOff)
                                        + "\" font-family=\"Monospace\" font-size=\"" + fontSize
                                        + "\" fill=\"" + lclr + "\">"
                                        + ch + "</text>\n");
                                col++;
                            }
                        }
                    }
                }
            }

            // Level TEXT labels (POC, VAH, VAL, IBH, IBL) – outside chartG for sharp
            // rendering
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                int sesLevelW = (int) Math.max(10, sesXEnd - sesX);
                // POC label
                {
                    double y = marginTop + plotH - (histTPO_POC[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<text x=\"" + ((int) sesX + sesLevelW - 3) + "\" y=\"" + (y - 3)
                                + "\" data-by=\"" + String.format(Locale.US, "%.2f", y)
                                + "\" data-off=\"-3"
                                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#ffa500\" text-anchor=\"end\" font-weight=\"700\">POC</text>\n");
                }
                // VAH label
                {
                    double y = marginTop + plotH - (histTPO_VAH[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<text x=\"" + ((int) sesX + sesLevelW - 3) + "\" y=\"" + (y - 3)
                                + "\" data-by=\"" + String.format(Locale.US, "%.2f", y)
                                + "\" data-off=\"-3"
                                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#4caf50\" text-anchor=\"end\" font-weight=\"700\">VAH</text>\n");
                }
                // VAL label
                {
                    double y = marginTop + plotH - (histTPO_VAL[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<text x=\"" + ((int) sesX + sesLevelW - 3) + "\" y=\"" + (y - 3)
                                + "\" data-by=\"" + String.format(Locale.US, "%.2f", y)
                                + "\" data-off=\"-3"
                                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#f44336\" text-anchor=\"end\" font-weight=\"700\">VAL</text>\n");
                }
                // IBH label
                if (histIBH[k] > 0) {
                    double y = marginTop + plotH - (histIBH[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<text x=\"" + ((int) sesX + sesLevelW - 3) + "\" y=\"" + (y - 3)
                                + "\" data-by=\"" + String.format(Locale.US, "%.2f", y)
                                + "\" data-off=\"-3"
                                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#00bcd4\" text-anchor=\"end\" font-weight=\"700\">IBH</text>\n");
                }
                // IBL label
                if (histIBL[k] > 0) {
                    double y = marginTop + plotH - (histIBL[k] - globalLow) / globalRange * plotH;
                    if (y >= marginTop && y <= marginTop + plotH)
                        w.write("<text x=\"" + ((int) sesX + sesLevelW - 3) + "\" y=\"" + (y - 3)
                                + "\" data-by=\"" + String.format(Locale.US, "%.2f", y)
                                + "\" data-off=\"-3"
                                + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#e040fb\" text-anchor=\"end\" font-weight=\"700\">IBL</text>\n");
                }
            }
            w.write("</g>\n"); // close tpoTextG

            // Y-axis group (rendered AFTER tpoTextG so it covers any letters extending into
            // Y-axis area)
            w.write("<g id=\"yAxisBg\"><rect x=\"0\" y=\"0\" width=\"" + marginLeft + "\" height=\"" + totalH
                    + "\" fill=\"" + DEFAULT_DARK_BG_COLOR + "\"/></g>\n");
            w.write("<g id=\"yAxisG\"></g>\n");

            // Time axis: bar times (HH:mm) below each session
            for (int k = 0; k < N; k++) {
                drawSvgTimeAxis(w, (int) sesStartPx[k],
                        (int) Math.max(10, sesEndPx[k] - sesStartPx[k]),
                        marginTop, plotH, histSessionStart[k], histSessionEnd[k], "HH:mm");
            }

            // Collect bar data for crosshair tooltip
            StringBuilder barDataJs = new StringBuilder();
            barDataJs.append("var barData=[");
            boolean firstBar = true;
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);
                int bCount = 0;
                int bi = 0;
                try {
                    while (true) {
                        long t = cfgChart().Time(bi);
                        if (t < histSessionStart[k])
                            break;
                        if (t >= histSessionStart[k] && t < histSessionEnd[k])
                            bCount++;
                        bi++;
                    }
                } catch (Exception e2) {
                }
                if (bCount == 0)
                    continue;
                double cStep = sesPixW / bCount;
                bi = 0;
                int bIdx = 0;
                try {
                    while (true) {
                        long t = cfgChart().Time(bi);
                        if (t < histSessionStart[k])
                            break;
                        if (t >= histSessionStart[k] && t < histSessionEnd[k]) {
                            double xPos = sesX + (bCount - 1 - bIdx) * cStep + cStep / 2;
                            double o = cfgChart().Open(bi), h = cfgChart().High(bi), l = cfgChart().Low(bi),
                                    c = cfgChart().Close(bi);
                            double v = cfgChart().Volume(bi);
                            if (!firstBar)
                                barDataJs.append(",");
                            barDataJs.append("{x:").append(String.format(Locale.US, "%.1f", xPos));
                            barDataJs.append(",t:").append(t);
                            barDataJs.append(",o:").append(String.format(Locale.US, "%.5f", o));
                            barDataJs.append(",h:").append(String.format(Locale.US, "%.5f", h));
                            barDataJs.append(",l:").append(String.format(Locale.US, "%.5f", l));
                            barDataJs.append(",c:").append(String.format(Locale.US, "%.5f", c));
                            barDataJs.append(",v:").append(String.format(Locale.US, "%.0f", v));
                            barDataJs.append("}");
                            firstBar = false;
                            bIdx++;
                        }
                        bi++;
                    }
                } catch (Exception e2) {
                }
            }
            barDataJs.append("];\n");

            // ==================== MULTI-SESSION BAR REPLAY DATA + TOOLBAR
            // ====================
            if (isReplayMode) {
                StringBuilder replayJs = new StringBuilder();
                replayJs.append("var rpMT=" + marginTop + ",rpPH=" + plotH
                        + ",rpGLow=" + String.format(Locale.US, "%.10f", globalLow)
                        + ",rpGRange=" + String.format(Locale.US, "%.10f", globalRange)
                        + ",rpN=" + N + ";\n");
                replayJs.append("var rpSessions=[\n");

                for (int rk = 0; rk < N; rk++) {
                    double rSesX = sesStartPx[rk];
                    double rSesXEnd = sesEndPx[rk];
                    double rSesPixW = Math.max(10, rSesXEnd - rSesX);
                    int rNB = histNumBins[rk];
                    double rSesLow = histSessionLow[rk];
                    double rSesRange = histSessionHigh[rk] - histSessionLow[rk];
                    double rBinSize = (fixedTickBinSize > 0) ? fixedTickBinSize
                            : ((rNB > 0 && rSesRange > 0) ? rSesRange / rNB : 1);

                    // Count bars in this session
                    int rBarCount = 0;
                    int ri = 0;
                    try {
                        while (true) {
                            long t = cfgChart().Time(ri);
                            if (t < histSessionStart[rk])
                                break;
                            if (t >= histSessionStart[rk] && t < histSessionEnd[rk])
                                rBarCount++;
                            ri++;
                        }
                    } catch (Exception e2) {
                    }

                    // Collect per-bar bin contributions (oldest first)
                    double[][] barBins = new double[rBarCount][rNB];
                    boolean[] barBull = new boolean[rBarCount];
                    int[] barBracket = new int[rBarCount]; // bracket index for each bar
                    int[][] barBracketBins = new int[rBarCount][]; // which bins each bar touches (for bitmask)
                    ri = 0;
                    int rbi = 0;
                    try {
                        while (true) {
                            long t = cfgChart().Time(ri);
                            if (t < histSessionStart[rk])
                                break;
                            if (t >= histSessionStart[rk] && t < histSessionEnd[rk]) {
                                int chronoIdx = rBarCount - 1 - rbi;
                                double o2 = cfgChart().Open(ri), h2 = cfgChart().High(ri), l2 = cfgChart().Low(ri),
                                        c2 = cfgChart().Close(ri);
                                double v2 = cfgChart().Volume(ri);
                                barBull[chronoIdx] = (c2 >= o2);
                                int bHi = Math.max(0, Math.min(rNB - 1, (int) ((h2 - rSesLow) / rBinSize)));
                                int bLo = Math.max(0, Math.min(rNB - 1, (int) ((l2 - rSesLow) / rBinSize)));
                                double perBin = v2 / Math.max(1, bHi - bLo + 1);
                                for (int b = bLo; b <= bHi; b++)
                                    barBins[chronoIdx][b] = perBin;
                                // Compute bracket index for this bar
                                long bracketMillis2 = lastEffectiveBracketMillis > 0 ? lastEffectiveBracketMillis
                                        : 3600000L;
                                int bkIdx = (int) ((t - histSessionStart[rk]) / bracketMillis2);
                                bkIdx = Math.max(0, Math.min(61, bkIdx));
                                barBracket[chronoIdx] = bkIdx;
                                // Store which bins this bar touches
                                barBracketBins[chronoIdx] = new int[bHi - bLo + 1];
                                for (int bb = 0; bb <= bHi - bLo; bb++)
                                    barBracketBins[chronoIdx][bb] = bLo + bb;
                                rbi++;
                            }
                            ri++;
                        }
                    } catch (Exception e2) {
                    }

                    // Compute TPO layout params for this session
                    int rBracketCount = histTpoBracketCount[rk];
                    if (rBracketCount <= 0)
                        rBracketCount = 1;
                    int rMaxCols = 0;
                    for (int sj = 0; sj < rNB; sj++) {
                        int cnt = Long.bitCount(histTpoMask[rk][sj]);
                        if (cnt > rMaxCols)
                            rMaxCols = cnt;
                    }
                    if (rMaxCols <= 0)
                        rMaxCols = 1;
                    double rLetterW = Math.min(14.0, rSesPixW / rMaxCols);

                    // Emit session object with TPO layout params
                    if (rk > 0)
                        replayJs.append(",");
                    replayJs.append("{nb:" + rNB
                            + ",low:" + String.format(Locale.US, "%.10f", rSesLow)
                            + ",bs:" + String.format(Locale.US, "%.10f", rBinSize)
                            + ",x:" + String.format(Locale.US, "%.1f", rSesX)
                            + ",pw:" + String.format(Locale.US, "%.1f", rSesPixW)
                            + ",bc:" + rBracketCount
                            + ",lw:" + String.format(Locale.US, "%.2f", rLetterW)
                            + ",n:" + rBarCount + ",bars:[");
                    for (int b = 0; b < rBarCount; b++) {
                        if (b > 0)
                            replayJs.append(",");
                        replayJs.append("{b:" + (barBull[b] ? "1" : "0")
                                + ",bk:" + barBracket[b] + ",d:{");
                        boolean first = true;
                        for (int j = 0; j < rNB; j++) {
                            if (barBins[b][j] > 0) {
                                if (!first)
                                    replayJs.append(",");
                                replayJs.append(j + ":" + String.format(Locale.US, "%.2f", barBins[b][j]));
                                first = false;
                            }
                        }
                        replayJs.append("}}");
                    }
                    replayJs.append("]}");
                }
                replayJs.append("];\n");
                replayJs.append("var rpCurSes=" + (N - 1) + ";\n");
                barDataJs.append(replayJs);

                // Emit session X positions
                StringBuilder rpXArr = new StringBuilder("var rpSesXArr=[");
                StringBuilder rpXEnd = new StringBuilder("var rpSesXEnd=[");
                for (int rk = 0; rk < N; rk++) {
                    if (rk > 0) {
                        rpXArr.append(",");
                        rpXEnd.append(",");
                    }
                    rpXArr.append(String.format(Locale.US, "%.1f", sesStartPx[rk]));
                    rpXEnd.append(String.format(Locale.US, "%.1f", sesEndPx[rk]));
                }
                rpXArr.append("];\n");
                rpXEnd.append("];\n");
                barDataJs.append(rpXArr).append(rpXEnd);

                // Emit original KPI values
                barDataJs.append("var rpOrigKpi=[");
                for (int rk = 0; rk < N; rk++) {
                    if (rk > 0)
                        barDataJs.append(",");
                    barDataJs.append("{v:" + String.format(Locale.US, "%.0f", histTotalVolume[rk])
                            + ",b:" + String.format(Locale.US, "%.0f", histBullVolume[rk])
                            + ",r:" + String.format(Locale.US, "%.0f", histBearVolume[rk])
                            + "}");
                }
                barDataJs.append("];\n");

                // Replay toolbar SVG elements - inline with drawing toolbar
                int drawBtnW = 26, drawGap = 3, drawTotalBtns = 15;
                int drawTbW = drawTotalBtns * (drawBtnW + drawGap) + drawGap;
                int annoX2 = marginLeft + drawTbW + 8;
                int tbY = marginTop + plotH + 60;
                int sliderW = 200;
                int btnW = 28, btnH = 22, gap = 4;
                int tbX = annoX2 + 120 + 50;
                int totalTbW = 9 * (btnW + gap) + sliderW + 320;
                String btnStyle = "rx=\"4\" fill=\"#2a2a3e\" stroke=\"#555\" stroke-width=\"0.8\" cursor=\"pointer\"";
                String txtStyle = "font-family=\"Sans-Serif\" font-size=\"12\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";
                String txtStyleSm = "font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";

                w.write("<!-- Replay Toolbar -->\n");
                w.write("<g id=\"replayToolbar\">\n");
                w.write("<rect id=\"rpTbBg\" x=\"" + (tbX - 5) + "\" y=\"" + (tbY - 3) + "\" width=\"" + totalTbW
                        + "\" height=\"" + (btnH + 6)
                        + "\" rx=\"6\" fill=\"rgba(20,20,40,0.85)\" stroke=\"#444\" stroke-width=\"0.8\"/>\n");

                // Buttons: |<< << |< < >/|| > >| >> >>|
                String[] btnIds = { "rpSesFirst", "rpSesPrev10", "rpFirst", "rpPrev", "rpPlay", "rpNext", "rpLast",
                        "rpSesNext10", "rpSesLast" };
                // Icons: distinct per function
                String[] btnLabels = { "\u23EE", "\u23EA", "\u25C0|", "\u25C0", "\u25B6", "\u25B6", "|\u25B6",
                        "\u23E9", "\u23ED" };
                String[] btnTips = { "First Session", "-10 Sessions", "Prev Session", "Step Back", "Play/Pause",
                        "Step Forward", "Next Session", "+10 Sessions", "Last Session" };
                // Smaller font for multi-char composite buttons
                boolean[] useSmallFont = { false, false, true, false, false, false, true, false, false };
                for (int b = 0; b < 9; b++) {
                    int bx = tbX + b * (btnW + gap);
                    w.write("<rect id=\"" + btnIds[b] + "\" x=\"" + bx + "\" y=\"" + tbY + "\" width=\"" + btnW
                            + "\" height=\"" + btnH + "\" " + btnStyle + ">"
                            + "<title>" + btnTips[b] + "</title></rect>\n");
                    w.write("<text x=\"" + (bx + btnW / 2) + "\" y=\"" + (tbY + btnH / 2) + "\" "
                            + (useSmallFont[b] ? txtStyleSm : txtStyle) + ">"
                            + btnLabels[b] + "</text>\n");
                }
                // Pause icon (hidden initially) — over rpPlay which is index 4
                w.write("<text id=\"rpPauseIco\" x=\"" + (tbX + 4 * (btnW + gap) + btnW / 2) + "\" y=\""
                        + (tbY + btnH / 2) + "\" " + txtStyle + " style=\"display:none\">\u23F8</text>\n");

                // Slider
                int slX = tbX + 9 * (btnW + gap) + 10;
                int slY = tbY + btnH / 2;
                barDataJs.append("var rpSlX=" + slX + ",rpSlW=" + sliderW + ";\n");
                w.write("<line x1=\"" + slX + "\" y1=\"" + slY + "\" x2=\"" + (slX + sliderW)
                        + "\" y2=\"" + slY + "\" stroke=\"#555\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n");
                w.write("<line id=\"rpSliderFill\" x1=\"" + slX + "\" y1=\"" + slY + "\" x2=\"" + (slX + sliderW)
                        + "\" y2=\"" + slY + "\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n");
                w.write("<circle id=\"rpSliderKnob\" cx=\"" + (slX + sliderW) + "\" cy=\"" + slY
                        + "\" r=\"6\" fill=\"#4fc3f7\" stroke=\"#222\" stroke-width=\"1\" cursor=\"pointer\"/>\n");
                w.write("<rect id=\"rpSliderHit\" x=\"" + slX + "\" y=\"" + (slY - 8) + "\" width=\"" + sliderW
                        + "\" height=\"16\" fill=\"transparent\" cursor=\"pointer\"/>\n");

                // Counter
                int ctX = slX + sliderW + 10;
                w.write("<text id=\"rpCounter\" x=\"" + ctX + "\" y=\"" + (tbY + btnH / 2)
                        + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#ccc\" dominant-baseline=\"central\">"
                        + "0 / 0</text>\n");

                // Session label
                int sesLblX = ctX + 75;
                w.write("<text id=\"rpSesLabel\" x=\"" + sesLblX + "\" y=\"" + (tbY + btnH / 2)
                        + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#7ab5ff\" dominant-baseline=\"central\">"
                        + "Session " + N + "/" + N + "</text>\n");

                // Session number textbox input
                int sesInputX = sesLblX + 110;
                w.write("<foreignObject x=\"" + sesInputX + "\" y=\"" + (tbY + 1) + "\" width=\"60\" height=\""
                        + (btnH - 2) + "\">\n");
                w.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
                w.write("<input id=\"rpSesInput\" type=\"text\" value=\"" + N + "\" "
                        + "style=\"width:50px;height:" + (btnH - 6) + "px;background:#1a1a2e;color:#7ab5ff;"
                        + "border:1px solid #555;border-radius:3px;font-family:Monospace;font-size:11px;"
                        + "text-align:center;outline:none;padding:1px 2px\""
                        + " title=\"Enter session number and press Enter\"/>\n");
                w.write("</body></foreignObject>\n");

                w.write("</g>\n");

                // Session highlight rect
                w.write("<rect id=\"rpSesHilight\" x=\"0\" y=\"" + marginTop + "\" width=\"0\" height=\"" + plotH
                        + "\" fill=\"rgba(255,255,100,0.06)\" pointer-events=\"none\" style=\"display:none\"/>\n");
            }

            // Crosshair SVG elements (initially hidden)
            w.write("<g id=\"crosshairG\" style=\"display:none;pointer-events:none\">\n");
            w.write("<line id=\"chV\" x1=\"0\" y1=\"" + marginTop + "\" x2=\"0\" y2=\"" + (marginTop + plotH)
                    + "\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n");
            w.write("<line id=\"chH\" x1=\"" + marginLeft + "\" y1=\"0\" x2=\"" + (marginLeft + plotW)
                    + "\" y2=\"0\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n");
            w.write("<g id=\"chPriceG\">");
            w.write("<rect id=\"chPriceBg\" x=\"" + (marginLeft + plotW + 2)
                    + "\" y=\"0\" width=\"60\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>");
            w.write("<text id=\"chPriceTxt\" x=\"" + (marginLeft + plotW + 32)
                    + "\" y=\"12\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>");
            w.write("</g>\n");
            w.write("<g id=\"chTimeG\">");
            w.write("<rect id=\"chTimeBg\" x=\"0\" y=\"" + (marginTop + plotH + 2)
                    + "\" width=\"70\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>");
            w.write("<text id=\"chTimeTxt\" x=\"0\" y=\"" + (marginTop + plotH + 14)
                    + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>");
            w.write("</g>\n");
            w.write("<g id=\"chTooltipG\">");
            w.write("<rect id=\"chTooltipBg\" x=\"0\" y=\"0\" width=\"165\" height=\"100\" rx=\"4\" fill=\"rgba(20,20,40,0.92)\" stroke=\"#555\" stroke-width=\"0.5\"/>");
            w.write("<text id=\"chTooltipTxt\" x=\"8\" y=\"14\" font-family=\"Monospace\" font-size=\"10\" fill=\"#ddd\">");
            w.write("<tspan id=\"chTD\" x=\"8\" dy=\"0\"></tspan>");
            w.write("<tspan id=\"chTO\" x=\"8\" dy=\"14\"></tspan>");
            w.write("<tspan id=\"chTH\" x=\"8\" dy=\"14\"></tspan>");
            w.write("<tspan id=\"chTL\" x=\"8\" dy=\"14\"></tspan>");
            w.write("<tspan id=\"chTC\" x=\"8\" dy=\"14\"></tspan>");
            w.write("<tspan id=\"chTV\" x=\"8\" dy=\"14\"></tspan>");
            w.write("</text></g>\n");
            w.write("</g>\n");

            // Interactive drawing toolbar (works in browsers)
            writeSvgDrawingTools(w, marginLeft, marginTop, plotW, plotH, globalLow, globalRange, barDataJs.toString(),
                    marginBottom);

            // --- Emit autoscale JavaScript ---
            w.write("<script type=\"text/javascript\">\n");
            w.write("//<![CDATA[\n");
            StringBuilder sesJs = new StringBuilder("var _sesData=[");
            for (int k = 0; k < N; k++) {
                if (k > 0)
                    sesJs.append(",");
                sesJs.append(String.format(Locale.US, "{x1:%.1f,x2:%.1f,h:%.6f,l:%.6f}",
                        sesStartPx[k], sesEndPx[k], histSessionHigh[k], histSessionLow[k]));
            }
            sesJs.append("];");
            w.write(sesJs.toString() + "\n");
            w.write("var _mt=" + marginTop + ",_ph=" + plotH + ",_gL=" +
                    String.format(Locale.US, "%.8f", globalLow) + ",_gR=" +
                    String.format(Locale.US, "%.8f", globalRange) + ",_mL=" + marginLeft + ";\n");
            w.write("var _yFill='#ccc',_yStroke='#888',_yBg='" + DEFAULT_DARK_BG_COLOR + "';\n");
            // Autoscale function (same as Volume Profile)
            w.write("function _autoScale(){\n");
            w.write("  var svg=document.querySelector('svg');\n");
            w.write("  if(!svg)return;\n");
            w.write("  var cr=svg.getBoundingClientRect();\n");
            w.write("  var vb=svg.viewBox.baseVal;\n");
            w.write("  var de=document.documentElement,bd=document.body;\n");
            w.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
            w.write("  var pixToSvg=(cr.width>0)?vb.width/cr.width:1;\n");
            w.write("  var sl=scrollPx*pixToSvg;\n");
            w.write("  var vw=(window.innerWidth||document.documentElement.clientWidth)*pixToSvg;\n");
            w.write("  var vL=sl,vR=sl+vw;\n");
            w.write("  var visH=-1e30,visL=1e30,hasVis=false;\n");
            w.write("  for(var i=0;i<_sesData.length;i++){\n");
            w.write("    var s=_sesData[i];\n");
            w.write("    if(s.x2>=vL && s.x1<=vR){hasVis=true;visH=Math.max(visH,s.h);visL=Math.min(visL,s.l);}\n");
            w.write("  }\n");
            w.write("  if(!hasVis){visH=_gL+_gR;visL=_gL;}\n");
            w.write("  if(visH<=visL){visH=_gL+_gR;visL=_gL;}\n");
            w.write("  var pad=(visH-visL)*0.05;visH+=pad;visL-=pad;\n");
            w.write("  var minSpan=_gR*0.40;\n");
            w.write("  if((visH-visL)<minSpan){\n");
            w.write("    var mid=(visH+visL)/2;\n");
            w.write("    visL=mid-minSpan/2;visH=mid+minSpan/2;\n");
            w.write("  }\n");
            w.write("  var gH=_gL+_gR;\n");
            w.write("  if(visH>gH)visH=gH;\n");
            w.write("  if(visL<_gL)visL=_gL;\n");
            w.write("  if(visH<=visL){visH=gH;visL=_gL;}\n");
            w.write("  var yH=_mt+_ph-(visH-_gL)/_gR*_ph;\n");
            w.write("  var yL=_mt+_ph-(visL-_gL)/_gR*_ph;\n");
            w.write("  if(yL<=yH){yH=_mt;yL=_mt+_ph;}\n");
            w.write("  var sy=_ph/(yL-yH);\n");
            w.write("  if(!isFinite(sy)||sy<=0)sy=1;\n");
            w.write("  sy=Math.max(1,Math.min(3,sy));\n");
            w.write("  var ty=_mt-yH*sy;\n");
            w.write("  if(!isFinite(ty))ty=0;\n");
            w.write("  var cg=document.getElementById('chartG');\n");
            w.write("  if(cg)cg.setAttribute('transform','matrix(1,0,0,'+sy+',0,'+ty+')');\n");
            // Reposition tpoTextG text directly (outside chartG, no scale transform =
            // sharp)
            w.write("  var tg=document.getElementById('tpoTextG');\n");
            w.write("  if(tg){var txts=tg.getElementsByTagName('text');\n");
            w.write("  for(var i=0;i<txts.length;i++){\n");
            w.write("    var t=txts[i];\n");
            w.write("    var by=parseFloat(t.dataset.by)||0;\n");
            w.write("    var off=parseFloat(t.dataset.off)||0;\n");
            w.write("    t.setAttribute('y',sy*by+ty+off);\n");
            w.write("  }}\n");
            // Update Y-axis labels
            w.write("  var ya=document.getElementById('yAxisG');\n");
            w.write("  if(ya){ya.innerHTML='';var nT=10;\n");
            w.write("    for(var t=0;t<=nT;t++){\n");
            w.write("      var p=visL+(visH-visL)*t/nT;\n");
            w.write("      var yy=_mt+_ph-(_ph*t/nT);\n");
            w.write("      var ln=document.createElementNS('http://www.w3.org/2000/svg','line');\n");
            w.write("      ln.setAttribute('x1',_mL);ln.setAttribute('x2',_mL+6);\n");
            w.write("      ln.setAttribute('y1',yy);ln.setAttribute('y2',yy);\n");
            w.write("      ln.setAttribute('stroke',_yStroke);ya.appendChild(ln);\n");
            w.write("      var tx=document.createElementNS('http://www.w3.org/2000/svg','text');\n");
            w.write("      tx.setAttribute('x',_mL-3);tx.setAttribute('y',yy+3);\n");
            w.write("      tx.setAttribute('font-family','Monospace');tx.setAttribute('font-size','9');\n");
            w.write("      tx.setAttribute('fill',_yFill);tx.setAttribute('text-anchor','end');\n");
            w.write("      tx.textContent=p.toFixed(5);ya.appendChild(tx);\n");
            w.write("    }\n");
            w.write("  }\n");
            w.write("  var sgs=document.querySelectorAll('.sStats');\n");
            w.write("  for(var i=0;i<sgs.length;i++){\n");
            w.write("    var sg=sgs[i];\n");
            w.write("    var origShy=parseFloat(sg.dataset.shy)||0;\n");
            w.write("    var newShy=sy*origShy+ty;\n");
            w.write("    sg.setAttribute('transform','translate(0,'+(newShy-origShy)+')');\n");
            w.write("  }\n");
            w.write("}\n");
            w.write("// embedded Results panel uses container-driven sizing; autoscale disabled\n");

            // --- Sticky scroll: keep toolbar, Y-axis, and title fixed on horizontal scroll
            // ---
            w.write("function _stickyScroll(){\n");
            w.write("  var svg=document.querySelector('svg');\n");
            w.write("  if(!svg)return;\n");
            w.write("  var vb=svg.viewBox.baseVal;\n");
            w.write("  var cr=svg.getBoundingClientRect();\n");
            w.write("  var de=document.documentElement,bd=document.body;\n");
            w.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
            w.write("  var sx=scrollPx*(vb.width/cr.width);\n");
            w.write("  var tb=document.getElementById('toolbar');\n");
            w.write("  if(tb)tb.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var rp=document.getElementById('replayToolbar');\n");
            w.write("  if(rp)rp.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var ya=document.getElementById('yAxisG');\n");
            w.write("  if(ya)ya.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var yb=document.getElementById('yAxisBg');\n");
            w.write("  if(yb)yb.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var tt=document.getElementById('chartTitle');\n");
            w.write("  if(tt)tt.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var ac=document.getElementById('annoGroup');\n");
            w.write("  if(ac)ac.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("  var ch=document.getElementById('crosshairInfo');\n");
            w.write("  if(ch)ch.setAttribute('transform','translate('+sx+',0)');\n");
            w.write("}\n");
            w.write("window.addEventListener('scroll',_stickyScroll);\n");
            w.write("window.addEventListener('resize',_stickyScroll);\n");
            w.write("setTimeout(_stickyScroll,120);\n");
            w.write("//]]>\n");
            w.write("</script>\n");

            w.write("</svg>\n");
        } catch (Exception e) {
        }
    }

    protected void writeSvgDrawingTools(BufferedWriter w, int mx, int mt, int pw, int ph,
            double gLow, double gRange, String barDataJs, int marginBottom) throws IOException {
        // Horizontal toolbar at the bottom of the chart
        int btnW = 26, btnH = 26, gap = 3;
        int tbY = mt + ph + 60;
        String[] ids = { "btnTrend", "btnHoriz", "btnRect", "btnText", "btnDraw", "btnRuler",
                "btnColor", "btnTheme", "btnUndo", "btnClear",
                "btnCross",
                "btnZoomIn", "btnZoomOut", "btnZoomRst" };
        String[] labels = { "\u2571", "\u2500", "\u25AD", "T", "\u270E", "\uD83D\uDCCF",
                "\u25CF", "\u2600", "\u21A9", "\uD83D\uDDD1",
                "\u253C",
                "+", "\u2212", "\u21BA" };
        String[] tips = { "Trend Line", "Horizontal Level", "Rectangle", "Text", "Freehand Draw", "Ruler",
                "Color", "Light/Dark Mode", "Undo", "Clear All",
                "Crosshair",
                "Zoom In", "Zoom Out", "Reset Zoom" };
        int totalBtns = ids.length + 1;
        int tbW = totalBtns * (btnW + gap) + gap;
        int tbX = mx;

        w.write("<g id=\"toolbar\" style=\"cursor:pointer\">\n");
        w.write("<rect id=\"tbBg\" x=\"" + (tbX - 2) + "\" y=\"" + (tbY - 2) + "\" width=\"" + (tbW + 4)
                + "\" height=\"" + (btnH + 4)
                + "\" rx=\"4\" fill=\"rgba(30,30,50,0.85)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n");
        for (int i = 0; i < ids.length; i++) {
            int bx = tbX + gap + i * (btnW + gap);
            w.write("<g id=\"g_" + ids[i] + "\">\n");
            w.write("<rect class=\"tbBtn\" id=\"" + ids[i] + "\" x=\"" + bx + "\" y=\"" + tbY + "\" width=\"" + btnW
                    + "\" height=\"" + btnH + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n");
            if (ids[i].equals("btnColor")) {
                w.write("<circle id=\"colorDot\" cx=\"" + (bx + btnW / 2) + "\" cy=\"" + (tbY + btnH / 2)
                        + "\" r=\"7\" fill=\"#ffcc00\" stroke=\"#888\" stroke-width=\"0.5\" pointer-events=\"none\"/>\n");
            } else if (ids[i].equals("btnTheme")) {
                w.write("<text class=\"tbLbl\" id=\"themeIcon\" x=\"" + (bx + btnW / 2) + "\" y=\""
                        + (tbY + btnH / 2 + 5)
                        + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">"
                        + "\u2600" + "</text>\n");
            } else {
                w.write("<text class=\"tbLbl\" x=\"" + (bx + btnW / 2) + "\" y=\"" + (tbY + btnH / 2 + 5)
                        + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">"
                        + labels[i] + "</text>\n");
            }
            w.write("<title>" + tips[i] + "</title>\n");
            w.write("</g>\n");
        }
        int printBx = tbX + gap + ids.length * (btnW + gap);
        w.write("<g id=\"g_btnPrint\">\n");
        w.write("<rect class=\"tbBtn\" id=\"btnPrint\" x=\"" + printBx + "\" y=\"" + tbY + "\" width=\"" + btnW
                + "\" height=\"" + btnH + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n");
        w.write("<text class=\"tbLbl\" x=\"" + (printBx + btnW / 2) + "\" y=\"" + (tbY + btnH / 2 + 5)
                + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">"
                + "\uD83D\uDDA8" + "</text>\n");
        w.write("<title>Print</title>\n");
        w.write("</g>\n");
        w.write("</g>\n");

        int annoX = tbX + tbW + 8;
        w.write("<g id=\"annoGroup\" style=\"cursor:pointer\">\n");
        w.write("<foreignObject x=\"" + annoX + "\" y=\"" + (tbY + 2) + "\" width=\"120\" height=\"22\">\n");
        w.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
        w.write("<label style=\"display:flex;align-items:center;gap:3px;font-family:Sans-Serif;font-size:10px;color:#ccc;cursor:pointer;background:rgba(30,30,50,0.85);padding:2px 5px;border-radius:4px;border:0.5px solid #555\">\n");
        w.write("<input id=\"annoCheck\" type=\"checkbox\" checked=\"checked\" style=\"margin:0;cursor:pointer\"/>\n");
        w.write("<span id=\"annoLabel\">Annotations</span></label>\n");
        w.write("</body></foreignObject>\n");
        w.write("</g>\n");

        String[] colors = { "#ffcc00", "#00e5ff", "#ff5252", "#69f0ae", "#ff80ab", "#b388ff", "#ffffff", "#ffa726" };
        int swatchSize = 18;
        int palX = tbX + gap + 6 * (btnW + gap);
        int palY = tbY - swatchSize - 8;
        w.write("<g id=\"colorPalette\" style=\"display:none;cursor:pointer\">\n");
        w.write("<rect x=\"" + (palX - 3) + "\" y=\"" + (palY - 3) + "\" width=\""
                + (colors.length * (swatchSize + 3) + 6) + "\" height=\"" + (swatchSize + 6)
                + "\" rx=\"4\" fill=\"rgba(30,30,50,0.9)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n");
        for (int i = 0; i < colors.length; i++) {
            int sx = palX + i * (swatchSize + 3);
            w.write("<rect class=\"swatch\" data-color=\"" + colors[i] + "\" x=\"" + sx + "\" y=\"" + palY
                    + "\" width=\"" + swatchSize + "\" height=\"" + swatchSize
                    + "\" rx=\"3\" fill=\"" + colors[i] + "\" stroke=\"#888\" stroke-width=\"0.5\"/>\n");
        }
        w.write("</g>\n");

        // JavaScript
        w.write("<script type=\"text/ecmascript\"><![CDATA[\n");
        w.write("(function(){\n");
        w.write("var svg=document.documentElement,ns='http://www.w3.org/2000/svg';\n");
        w.write("var MX=" + mx + ",MT=" + mt + ",PW=" + pw + ",PH=" + ph + ";\n");
        w.write("var GL=" + String.format(Locale.US, "%.10f", gLow) + ",GR=" + String.format(Locale.US, "%.10f", gRange)
                + ";\n");
        w.write("var mode='',pt1=null,drawn=[],preview=null;\n");
        w.write("var activeColor='#ffcc00';\n");
        w.write("var crosshairOn=false;\n");
        w.write(barDataJs);
        w.write("var drawModes={'btnTrend':'trend','btnHoriz':'horiz','btnRect':'rect','btnText':'text','btnDraw':'draw','btnRuler':'ruler'};\n");
        w.write("var toolBtnIds=['btnTrend','btnHoriz','btnRect','btnText','btnDraw','btnRuler'];\n");
        w.write("function svgPt(e){\n");
        w.write("  if(svg&&typeof svg.createSVGPoint==='function'){\n");
        w.write("    var p=svg.createSVGPoint();p.x=e.clientX;p.y=e.clientY;\n");
        w.write("    var m=svg.getScreenCTM();\n");
        w.write("    if(m&&typeof m.inverse==='function')return p.matrixTransform(m.inverse());\n");
        w.write("  }\n");
        w.write("  var r=svg&&svg.getBoundingClientRect?svg.getBoundingClientRect():{left:0,top:0,width:1,height:1};\n");
        w.write("  var vb=(svg&&svg.viewBox&&svg.viewBox.baseVal)?svg.viewBox.baseVal:{x:0,y:0,width:r.width||1,height:r.height||1};\n");
        w.write("  var sx=(r.width>0)?vb.width/r.width:1,sy=(r.height>0)?vb.height/r.height:1;\n");
        w.write("  return{x:vb.x+(e.clientX-r.left)*sx,y:vb.y+(e.clientY-r.top)*sy};}\n");
        w.write("function clamp(p){return{x:Math.max(MX,Math.min(MX+PW,p.x)),y:Math.max(MT,Math.min(MT+PH,p.y))};}\n");
        w.write("function yToPrice(y){return GL+GR*(1-(y-MT)/PH);}\n");
        w.write("function fmtP(p){return p.toFixed(5);}\n");
        w.write("var isDark=true;\n");
        w.write("var _tpoDk=['#e57373','#f06292','#ba68c8','#9575cd','#7986cb','#64b5f6','#4fc3f7','#4dd0e1','#4db6ac','#81c784','#aed581','#dce775','#fff176','#ffd54f','#ffb74d','#ff8a65'];\n");
        w.write("var _tpoLt=['#c62828','#ad1457','#6a1b9a','#4527a0','#283593','#1565c0','#0277bd','#00838f','#00695c','#2e7d32','#558b2f','#9e9d24','#f9a825','#ff8f00','#ef6c00','#d84315'];\n");
        w.write("function btnFill(){return isDark?'#2a2a4a':'#e8e8f0';}\n");
        w.write("function btnStroke(){return isDark?'#666':'#aaa';}\n");
        w.write("function hilite(){toolBtnIds.forEach(function(id){var b=document.getElementById(id);");
        w.write("b.setAttribute('fill',drawModes[id]===mode?'#4181ed':btnFill());");
        w.write("b.setAttribute('stroke',drawModes[id]===mode?'#7ab5ff':btnStroke());});}\n");
        w.write("function rmPreview(){if(preview){preview.parentNode.removeChild(preview);preview=null;}}\n");
        w.write("function addPriceLabel(g,x,y,price,clr){\n");
        w.write("var t=document.createElementNS(ns,'text');t.setAttribute('x',x+PW-3);t.setAttribute('y',y-3);\n");
        w.write("t.setAttribute('font-family','Monospace');t.setAttribute('font-size','9');\n");
        w.write("t.setAttribute('fill',clr);t.setAttribute('text-anchor','end');\n");
        w.write("t.textContent=fmtP(price);g.appendChild(t);}\n");

        // Color palette toggle
        w.write("var palette=document.getElementById('colorPalette');\n");
        w.write("var colorDot=document.getElementById('colorDot');\n");
        w.write("document.getElementById('g_btnColor').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();palette.style.display=palette.style.display==='none'?'block':'none';});\n");
        w.write("var swatches=document.querySelectorAll('.swatch');\n");
        w.write("for(var i=0;i<swatches.length;i++){(function(sw){\n");
        w.write("  sw.addEventListener('click',function(e){e.stopPropagation();\n");
        w.write("    activeColor=sw.getAttribute('data-color');colorDot.setAttribute('fill',activeColor);\n");
        w.write("    palette.style.display='none';});\n");
        w.write("})(swatches[i]);}\n");

        // Undo / Clear
        w.write("document.getElementById('g_btnUndo').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();if(drawn.length){var el=drawn.pop();el.parentNode.removeChild(el);}});\n");
        w.write("document.getElementById('g_btnClear').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();drawn.forEach(function(el){el.parentNode.removeChild(el);});drawn=[];});\n");

        // Theme toggle (Light/Dark)
        w.write("var dBg='" + DEFAULT_DARK_BG_COLOR + "',dPl='" + DEFAULT_DARK_PLOT_COLOR + "',lBg='" + DEFAULT_LIGHT_BG_COLOR
                + "',lPl='"
                + DEFAULT_LIGHT_PLOT_COLOR + "';\n");
        w.write("var dCandAlpha="
                + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, DEFAULT_DARK_CANDLE_OPACITY / 100.0)))
                + ",lCandAlpha="
                + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, DEFAULT_LIGHT_CANDLE_OPACITY / 100.0)))
                + ",dProfAlpha="
                + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, DEFAULT_DARK_PROFILE_OPACITY / 100.0)))
                + ",lProfAlpha="
                + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, DEFAULT_LIGHT_PROFILE_OPACITY / 100.0)))
                + ";\n");
        w.write("document.getElementById('g_btnTheme').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();isDark=!isDark;\n");
        w.write("  document.getElementById('bgRect').setAttribute('fill',isDark?dBg:lBg);\n");
        w.write("  document.getElementById('plotRect').setAttribute('fill',isDark?dPl:lPl);\n");
        w.write("  var tt=document.querySelectorAll('.thTitle');for(var i=0;i<tt.length;i++)tt[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
        w.write("  var gd=document.querySelectorAll('.thGrid');for(var i=0;i<gd.length;i++)gd[i].setAttribute('stroke',isDark?'#2a2a4a':'#ddd');\n");
        w.write("  var pl=document.querySelectorAll('.thPrice');for(var i=0;i<pl.length;i++)pl[i].setAttribute('fill',isDark?'#888':'#555');\n");
        w.write("  var dl=document.querySelectorAll('.thDate');for(var i=0;i<dl.length;i++)dl[i].setAttribute('fill',isDark?'#aaa':'#666');\n");
        w.write("  var sd=document.querySelectorAll('.thSesDiv');for(var i=0;i<sd.length;i++)sd[i].setAttribute('stroke',isDark?'#555':'#bbb');\n");
        w.write("  var vl=document.querySelectorAll('.thVol');for(var i=0;i<vl.length;i++)vl[i].setAttribute('fill',isDark?'#ccc':'#000');\n");
        w.write("  var br=document.querySelectorAll('.thBrand');for(var i=0;i<br.length;i++)br[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
        w.write("  document.getElementById('tbBg').setAttribute('fill',isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)');\n");
        w.write("  document.getElementById('tbBg').setAttribute('stroke',isDark?'#555':'#aaa');\n");
        w.write("  var btns=document.querySelectorAll('.tbBtn');for(var i=0;i<btns.length;i++){btns[i].setAttribute('fill',btnFill());btns[i].setAttribute('stroke',btnStroke());}\n");
        w.write("  var bl=document.querySelectorAll('.tbLbl');for(var i=0;i<bl.length;i++)bl[i].setAttribute('fill',isDark?'#ccc':'#333');\n");
        w.write("  var al=document.getElementById('annoLabel');if(al){al.style.color=isDark?'#ccc':'#333';al.parentElement.style.background=isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)';al.parentElement.style.borderColor=isDark?'#555':'#aaa';}\n");
        w.write("  document.getElementById('themeIcon').textContent=isDark?'\\u2600':'\\u263E';\n");
        w.write("  var cg=document.getElementById('candlesG');if(cg)cg.setAttribute('opacity',isDark?dCandAlpha:lCandAlpha);\n");
        w.write("  var pg=document.getElementById('profileG');if(pg)pg.setAttribute('opacity',isDark?dProfAlpha:lProfAlpha);\n");
        w.write("  _yFill=isDark?'#ccc':'#555';_yStroke=isDark?'#888':'#bbb';_yBg=isDark?dBg:lBg;\n");
        w.write("  var yb=document.getElementById('yAxisBg');if(yb)yb.querySelector('rect').setAttribute('fill',_yBg);\n");
        w.write("  _autoScale();\n");
        w.write("  var _tc=isDark?_tpoDk:_tpoLt;\n");
        w.write("  var tl=document.querySelectorAll('.tpoL');for(var i=0;i<tl.length;i++){var bi=parseInt(tl[i].dataset.bi)||0;tl[i].setAttribute('fill',_tc[bi%_tc.length]);}\n");
        w.write("  var tb=document.querySelectorAll('.tpoB');for(var i=0;i<tb.length;i++){var bi=parseInt(tb[i].dataset.bi)||0;tb[i].setAttribute('fill',_tc[bi%_tc.length]);}\n");
        w.write("  hilite();\n");
        w.write("});\n");

        // Print
        w.write("document.getElementById('g_btnPrint').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();\n");
        w.write("  var tb=document.getElementById('toolbar');\n");
        w.write("  var pp=document.getElementById('printPanel');\n");
        w.write("  var incAnno=document.getElementById('annoCheck')?document.getElementById('annoCheck').checked:true;\n");
        w.write("  tb.style.display='none';palette.style.display='none';pp.style.display='none';\n");
        w.write("  if(!incAnno){drawn.forEach(function(el){el.style.display='none';});}\n");
        w.write("  setTimeout(function(){window.print();setTimeout(function(){\n");
        w.write("    tb.style.display='';pp.style.display='';\n");
        w.write("    if(!incAnno){drawn.forEach(function(el){el.style.display='';});}\n");
        w.write("  },300);},100);\n");
        w.write("});\n");

        // Tool button click handlers — use the group wrapper for reliable clicks
        w.write("toolBtnIds.forEach(function(id){\n");
        w.write("document.getElementById('g_'+id).addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();mode=(mode===drawModes[id])?'':drawModes[id];pt1=null;rmPreview();hilite();\n");
        w.write("});});\n");

        // Freehand draw state
        w.write("var drawPts=[],drawEl=null,isDrawing=false;\n");

        // SVG click handler
        w.write("svg.addEventListener('click',function(e){\n");
        w.write("  if(!mode||mode==='draw')return;var raw=svgPt(e),p=clamp(raw);\n");
        w.write("  if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH)return;\n");
        w.write("  if(mode==='horiz'){var g=document.createElementNS(ns,'g');\n");
        w.write("    var ln=document.createElementNS(ns,'line');\n");
        w.write("    ln.setAttribute('x1',MX);ln.setAttribute('y1',p.y);ln.setAttribute('x2',MX+PW);ln.setAttribute('y2',p.y);\n");
        w.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');ln.setAttribute('stroke-dasharray','6,3');\n");
        w.write("    g.appendChild(ln);addPriceLabel(g,MX,p.y,yToPrice(p.y),activeColor);\n");
        w.write("    svg.appendChild(g);drawn.push(g);return;}\n");
        w.write("  if(mode==='trend'){if(!pt1){pt1=p;return;}rmPreview();\n");
        w.write("    var ln=document.createElementNS(ns,'line');\n");
        w.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
        w.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1.5');\n");
        w.write("    svg.appendChild(ln);drawn.push(ln);pt1=null;return;}\n");
        w.write("  if(mode==='rect'){if(!pt1){pt1=p;return;}rmPreview();\n");
        w.write("    var r=document.createElementNS(ns,'rect');\n");
        w.write("    r.setAttribute('x',Math.min(pt1.x,p.x));r.setAttribute('y',Math.min(pt1.y,p.y));\n");
        w.write("    r.setAttribute('width',Math.abs(p.x-pt1.x));r.setAttribute('height',Math.abs(p.y-pt1.y));\n");
        w.write("    r.setAttribute('fill',activeColor);r.setAttribute('fill-opacity','0.12');\n");
        w.write("    r.setAttribute('stroke',activeColor);r.setAttribute('stroke-width','1');r.setAttribute('stroke-dasharray','4,2');\n");
        w.write("    svg.appendChild(r);drawn.push(r);pt1=null;return;}\n");
        w.write("  if(mode==='text'){var txt=prompt('Enter text:');if(txt){\n");
        w.write("    var t=document.createElementNS(ns,'text');t.setAttribute('x',p.x);t.setAttribute('y',p.y);\n");
        w.write("    t.setAttribute('font-family','Sans-Serif');t.setAttribute('font-size','13');\n");
        w.write("    t.setAttribute('fill',activeColor);t.setAttribute('font-weight','600');t.textContent=txt;\n");
        w.write("    svg.appendChild(t);drawn.push(t);}return;}\n");
        // Ruler click handler
        w.write("  if(mode==='ruler'){if(!pt1){pt1=p;return;}rmPreview();\n");
        w.write("    var g=document.createElementNS(ns,'g');\n");
        w.write("    var ln=document.createElementNS(ns,'line');\n");
        w.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
        w.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');ln.setAttribute('stroke-dasharray','3,2');\n");
        w.write("    g.appendChild(ln);\n");
        w.write("    var dp=Math.abs(yToPrice(pt1.y)-yToPrice(p.y));\n");
        w.write("    var dx=Math.abs(p.x-pt1.x),dy=Math.abs(p.y-pt1.y),dist=Math.sqrt(dx*dx+dy*dy);\n");
        w.write("    var mx2=(pt1.x+p.x)/2,my2=(pt1.y+p.y)/2;\n");
        w.write("    var t1=document.createElementNS(ns,'text');\n");
        w.write("    t1.setAttribute('x',mx2);t1.setAttribute('y',my2-8);\n");
        w.write("    t1.setAttribute('font-family','Sans-Serif');t1.setAttribute('font-size','11');\n");
        w.write("    t1.setAttribute('fill',activeColor);t1.setAttribute('text-anchor','middle');t1.setAttribute('font-weight','700');\n");
        w.write("    t1.textContent='\\u0394Price: '+fmtP(dp);g.appendChild(t1);\n");
        w.write("    var t2=document.createElementNS(ns,'text');\n");
        w.write("    t2.setAttribute('x',mx2);t2.setAttribute('y',my2+6);\n");
        w.write("    t2.setAttribute('font-family','Sans-Serif');t2.setAttribute('font-size','10');\n");
        w.write("    t2.setAttribute('fill',activeColor);t2.setAttribute('text-anchor','middle');\n");
        w.write("    t2.textContent='\\u0394Px: '+Math.round(dist)+'px';g.appendChild(t2);\n");
        w.write("    svg.appendChild(g);drawn.push(g);pt1=null;return;}\n");
        w.write("});\n");

        // Zoom via viewBox manipulation
        w.write("var svg=(typeof svg!=='undefined'&&svg)?svg:document.querySelector('svg');\n");
        w.write("if(!svg)svg=document.documentElement.querySelector('svg');\n");
        w.write("var vbBase=(svg&&svg.viewBox&&svg.viewBox.baseVal)?svg.viewBox.baseVal:null;\n");
        w.write("var origVB={x:0,y:0,w:(vbBase&&vbBase.width?vbBase.width:(parseFloat(svg&&svg.getAttribute('width'))||1000)),h:(vbBase&&vbBase.height?vbBase.height:(parseFloat(svg&&svg.getAttribute('height'))||600))};\n");
        w.write("var vb={x:0,y:0,w:origVB.w,h:origVB.h};\n");
        w.write("function setVB(){svg.setAttribute('viewBox',vb.x+' '+vb.y+' '+vb.w+' '+vb.h);}\n");
        w.write("function zoomAt(cx,cy,factor){\n");
        w.write("  var nw=vb.w*factor,nh=vb.h*factor;\n");
        w.write("  if(nw<50||nh<50||nw>origVB.w*3||nh>origVB.h*3)return;\n");
        w.write("  vb.x=cx-(cx-vb.x)*factor;vb.y=cy-(cy-vb.y)*factor;\n");
        w.write("  vb.w=nw;vb.h=nh;setVB();}\n");
        // Mouse wheel zoom
        w.write("svg.addEventListener('wheel',function(e){\n");
        w.write("  e.preventDefault();var p=svgPt(e);\n");
        w.write("  zoomAt(p.x,p.y,e.deltaY>0?1.12:0.89);\n");
        w.write("},{passive:false});\n");
        // Zoom buttons
        w.write("document.getElementById('g_btnZoomIn').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();zoomAt(vb.x+vb.w/2,vb.y+vb.h/2,0.75);});\n");
        w.write("document.getElementById('g_btnZoomOut').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();zoomAt(vb.x+vb.w/2,vb.y+vb.h/2,1.33);});\n");
        w.write("document.getElementById('g_btnZoomRst').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();vb.x=0;vb.y=0;vb.w=origVB.w;vb.h=origVB.h;setVB();});\n");
        // Pan with mouse drag when no drawing tool is active
        w.write("var isPan=false,panPt=null;\n");
        w.write("svg.addEventListener('mousedown',function(e){\n");
        w.write("  if(mode==='draw'){var raw=svgPt(e),p=clamp(raw);\n");
        w.write("    if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH)return;\n");
        w.write("    isDrawing=true;drawPts=[p.x+','+p.y];\n");
        w.write("    drawEl=document.createElementNS(ns,'polyline');\n");
        w.write("    drawEl.setAttribute('fill','none');drawEl.setAttribute('stroke',activeColor);\n");
        w.write("    drawEl.setAttribute('stroke-width','2');drawEl.setAttribute('stroke-linecap','round');\n");
        w.write("    drawEl.setAttribute('stroke-linejoin','round');\n");
        w.write("    svg.appendChild(drawEl);return;}\n");
        w.write("  if(!mode){isPan=true;panPt=svgPt(e);svg.style.cursor='grabbing';e.preventDefault();}\n");
        w.write("});\n");
        w.write("svg.addEventListener('mousemove',function(e){\n");
        w.write("  if(isPan&&panPt){var p=svgPt(e);vb.x-=(p.x-panPt.x);vb.y-=(p.y-panPt.y);setVB();panPt=svgPt(e);return;}\n");
        w.write("  if(mode==='draw'&&isDrawing){var raw=svgPt(e),p=clamp(raw);\n");
        w.write("    drawPts.push(p.x+','+p.y);if(drawEl)drawEl.setAttribute('points',drawPts.join(' '));return;}\n");
        w.write("  if(!mode&&!isPan){svg.style.cursor='grab';}\n");
        w.write("  if(!mode||!pt1)return;var raw=svgPt(e),p=clamp(raw);rmPreview();\n");
        w.write("  if(mode==='trend'||mode==='ruler'){preview=document.createElementNS(ns,'g');\n");
        w.write("    var ln=document.createElementNS(ns,'line');\n");
        w.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);\n");
        w.write("    ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
        w.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');\n");
        w.write("    ln.setAttribute('stroke-dasharray','3,3');ln.setAttribute('opacity','0.6');\n");
        w.write("    preview.appendChild(ln);\n");
        w.write("    if(mode==='ruler'){\n");
        w.write("      var dp=Math.abs(yToPrice(pt1.y)-yToPrice(p.y));\n");
        w.write("      var dx=Math.abs(p.x-pt1.x),dy=Math.abs(p.y-pt1.y),dist=Math.sqrt(dx*dx+dy*dy);\n");
        w.write("      var mx2=(pt1.x+p.x)/2,my2=(pt1.y+p.y)/2;\n");
        w.write("      var t1=document.createElementNS(ns,'text');\n");
        w.write("      t1.setAttribute('x',mx2);t1.setAttribute('y',my2-8);\n");
        w.write("      t1.setAttribute('font-family','Sans-Serif');t1.setAttribute('font-size','11');\n");
        w.write("      t1.setAttribute('fill',activeColor);t1.setAttribute('text-anchor','middle');t1.setAttribute('font-weight','700');\n");
        w.write("      t1.textContent='\\u0394Price: '+fmtP(dp);preview.appendChild(t1);\n");
        w.write("      var t2=document.createElementNS(ns,'text');\n");
        w.write("      t2.setAttribute('x',mx2);t2.setAttribute('y',my2+6);\n");
        w.write("      t2.setAttribute('font-family','Sans-Serif');t2.setAttribute('font-size','10');\n");
        w.write("      t2.setAttribute('fill',activeColor);t2.setAttribute('text-anchor','middle');\n");
        w.write("      t2.textContent='\\u0394Px: '+Math.round(dist)+'px';preview.appendChild(t2);\n");
        w.write("    }\n");
        w.write("    svg.appendChild(preview);}\n");
        w.write("  if(mode==='rect'){preview=document.createElementNS(ns,'rect');\n");
        w.write("    preview.setAttribute('x',Math.min(pt1.x,p.x));preview.setAttribute('y',Math.min(pt1.y,p.y));\n");
        w.write("    preview.setAttribute('width',Math.abs(p.x-pt1.x));preview.setAttribute('height',Math.abs(p.y-pt1.y));\n");
        w.write("    preview.setAttribute('fill',activeColor);preview.setAttribute('fill-opacity','0.08');\n");
        w.write("    preview.setAttribute('stroke',activeColor);preview.setAttribute('stroke-width','0.8');\n");
        w.write("    preview.setAttribute('stroke-dasharray','3,3');preview.setAttribute('opacity','0.5');\n");
        w.write("    svg.appendChild(preview);}\n");
        w.write("});\n");
        w.write("svg.addEventListener('mouseup',function(e){\n");
        w.write("  if(isPan){isPan=false;svg.style.cursor='grab';}\n");
        w.write("  if(mode!=='draw'||!isDrawing)return;\n");
        w.write("  isDrawing=false;if(drawEl&&drawPts.length>1){drawn.push(drawEl);}else if(drawEl){drawEl.parentNode.removeChild(drawEl);}\n");
        w.write("  drawEl=null;drawPts=[];\n");
        w.write("});\n");

        // Crosshair toggle and logic
        w.write("var chG=document.getElementById('crosshairG');\n");
        w.write("var chV=document.getElementById('chV'),chH=document.getElementById('chH');\n");
        w.write("var chPriceBg=document.getElementById('chPriceBg'),chPriceTxt=document.getElementById('chPriceTxt');\n");
        w.write("var chTimeBg=document.getElementById('chTimeBg'),chTimeTxt=document.getElementById('chTimeTxt');\n");
        w.write("var chTooltipG=document.getElementById('chTooltipG'),chTooltipBg=document.getElementById('chTooltipBg');\n");
        w.write("var chTD=document.getElementById('chTD'),chTO=document.getElementById('chTO');\n");
        w.write("var chTH=document.getElementById('chTH'),chTL=document.getElementById('chTL');\n");
        w.write("var chTC=document.getElementById('chTC'),chTV=document.getElementById('chTV');\n");
        w.write("document.getElementById('g_btnCross').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();crosshairOn=!crosshairOn;\n");
        w.write("  var btn=document.getElementById('btnCross');\n");
        w.write("  btn.setAttribute('fill',crosshairOn?'#4181ed':btnFill());\n");
        w.write("  btn.setAttribute('stroke',crosshairOn?'#7ab5ff':btnStroke());\n");
        w.write("  if(!crosshairOn)chG.style.display='none';\n");
        w.write("});\n");
        w.write("function nearestBar(px){\n");
        w.write("  if(!barData||barData.length===0)return null;\n");
        w.write("  var best=null,bestD=1e9;\n");
        w.write("  for(var i=0;i<barData.length;i++){var d=Math.abs(barData[i].x-px);if(d<bestD){bestD=d;best=barData[i];}}\n");
        w.write("  return best;}\n");
        w.write("function fmtDT(ms){var d=new Date(ms);\n");
        w.write("  var dd=d.getUTCFullYear()+'-'+('0'+(d.getUTCMonth()+1)).slice(-2)+'-'+('0'+d.getUTCDate()).slice(-2);\n");
        w.write("  var tt=('0'+d.getUTCHours()).slice(-2)+':'+('0'+d.getUTCMinutes()).slice(-2);\n");
        w.write("  return{date:dd,time:tt};}\n");
        w.write("svg.addEventListener('mousemove',function(e){\n");
        w.write("  if(!crosshairOn)return;\n");
        w.write("  var p=svgPt(e);\n");
        w.write("  if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH){chG.style.display='none';return;}\n");
        w.write("  chG.style.display='';\n");
        w.write("  chV.setAttribute('x1',p.x);chV.setAttribute('x2',p.x);\n");
        w.write("  chH.setAttribute('y1',p.y);chH.setAttribute('y2',p.y);\n");
        w.write("  var price=GL+GR*(1-(p.y-MT)/PH);\n");
        w.write("  chPriceTxt.textContent=fmtP(price);\n");
        w.write("  chPriceBg.setAttribute('y',p.y-8);chPriceTxt.setAttribute('y',p.y+3);\n");
        w.write("  chTimeBg.setAttribute('x',p.x-35);chTimeTxt.setAttribute('x',p.x);\n");
        w.write("  var bar=nearestBar(p.x);\n");
        w.write("  if(bar){\n");
        w.write("    var dt=fmtDT(bar.t);\n");
        w.write("    chTimeTxt.textContent=dt.time;\n");
        w.write("    chTD.textContent=dt.date+' '+dt.time;\n");
        w.write("    chTO.textContent='O: '+bar.o;\n");
        w.write("    chTH.textContent='H: '+bar.h;\n");
        w.write("    chTL.textContent='L: '+bar.l;\n");
        w.write("    chTC.textContent='C: '+bar.c;\n");
        w.write("    chTV.textContent='V: '+bar.v;\n");
        w.write("    var tx=p.x+15,ty=p.y-50;\n");
        w.write("    if(tx+170>MX+PW)tx=p.x-180;if(ty<MT)ty=MT+5;\n");
        w.write("    chTooltipBg.setAttribute('x',tx);chTooltipBg.setAttribute('y',ty);\n");
        w.write("    chTD.setAttribute('x',tx+8);chTO.setAttribute('x',tx+8);\n");
        w.write("    chTH.setAttribute('x',tx+8);chTL.setAttribute('x',tx+8);\n");
        w.write("    chTC.setAttribute('x',tx+8);chTV.setAttribute('x',tx+8);\n");
        w.write("    chTD.setAttribute('y',ty+14);\n");
        w.write("  }\n");
        w.write("});\n");
        w.write("svg.addEventListener('mouseleave',function(){if(crosshairOn)chG.style.display='none';});\n");

        // ==================== Multi-Session Replay Engine JavaScript (TPO)
        // ====================
        w.write("// Bar Replay Engine (multi-session TPO)\n");
        w.write("if(typeof rpSessions!=='undefined' && rpSessions.length>0){\n");
        w.write("var rpStep=rpSessions[rpCurSes].n,rpPlaying=false,rpTimer=null;\n");
        w.write("var rpKnob=document.getElementById('rpSliderKnob');\n");
        w.write("var rpFill=document.getElementById('rpSliderFill');\n");
        w.write("var rpCtr=document.getElementById('rpCounter');\n");
        w.write("var rpSesLbl=document.getElementById('rpSesLabel');\n");
        w.write("var rpPauseIco=document.getElementById('rpPauseIco');\n");
        w.write("var rpTbG=document.getElementById('replayToolbar');\n");
        w.write("var rpHilight=document.getElementById('rpSesHilight');\n");

        // Save original TPO letter DOM for each session
        w.write("var rpOrigProf=[];\n");
        w.write("for(var s=0;s<rpN;s++){var pg=document.getElementById('rpProfile_'+s);rpOrigProf.push(pg?pg.innerHTML:'');}\n");

        // rpShowAll
        w.write("function rpShowAll(){\n");
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var cg=document.getElementById('rpCandles_'+s);\n");
        w.write("    if(cg){var cs=cg.querySelectorAll('[id^=rc_]');cs.forEach(function(c){c.style.display='';});}\n");
        w.write("    var pg=document.getElementById('rpProfile_'+s);\n");
        w.write("    if(pg){pg.style.display='';pg.innerHTML=rpOrigProf[s];}\n");
        w.write("  }\n");
        w.write("}\n");

        // rpRender
        w.write("function rpRender(){\n");
        w.write("  var ses=rpSessions[rpCurSes];\n");
        w.write("  var nb=ses.nb,bars=ses.bars,total=ses.n;\n");

        // Candle visibility
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var cg=document.getElementById('rpCandles_'+s);\n");
        w.write("    if(!cg)continue;\n");
        w.write("    if(s>rpCurSes){cg.style.display='none';continue;}\n");
        w.write("    if(s===rpCurSes){\n");
        w.write("      if(rpStep===0){cg.style.display='none';continue;}\n");
        w.write("      cg.style.display='';\n");
        w.write("      var cs=cg.querySelectorAll('[id^=rc_]');\n");
        w.write("      cs.forEach(function(c){var ci=parseInt(c.id.split('_')[1]);c.style.display=(ci<rpStep)?'':'none';});\n");
        w.write("    } else {\n");
        w.write("      cg.style.display='';\n");
        w.write("      var cs2=cg.querySelectorAll('[id^=rc_]');cs2.forEach(function(c){c.style.display='';});\n");
        w.write("    }\n");
        w.write("  }\n");

        // Profile rebuild - TPO letters
        w.write("  var bCols=['#e57373','#f06292','#ba68c8','#9575cd','#7986cb','#64b5f6',\n");
        w.write("    '#4fc3f7','#4dd0e1','#4db6ac','#81c784','#aed581','#dce775',\n");
        w.write("    '#fff176','#ffd54f','#ffb74d','#ff8a65'];\n");
        w.write("  var bChars='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';\n");
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var pg=document.getElementById('rpProfile_'+s);\n");
        w.write("    if(!pg)continue;\n");
        w.write("    if(s>rpCurSes){pg.style.display='none';continue;}\n");
        w.write("    if(s!==rpCurSes){pg.style.display='';pg.innerHTML=rpOrigProf[s];continue;}\n");
        w.write("    if(rpStep===0){pg.style.display='none';continue;}\n");
        w.write("    pg.style.display='';\n");
        w.write("    if(rpStep>=total){pg.innerHTML=rpOrigProf[s];continue;}\n");

        // Rebuild TPO letters from replay data
        w.write("    while(pg.firstChild)pg.removeChild(pg.firstChild);\n");
        // Build bracket set per bin: which brackets have been seen
        w.write("    var mask=new Array(nb);\n");
        w.write("    for(var j=0;j<nb;j++)mask[j]={};\n");
        w.write("    for(var i=0;i<rpStep;i++){\n");
        w.write("      var bar=bars[i],bk=bar.bk;\n");
        w.write("      for(var k in bar.d){mask[parseInt(k)][bk]=1;}\n");
        w.write("    }\n");
        // Draw TPO letters/blocks from accumulated mask
        w.write("    var lw=ses.lw||10;\n");
        w.write("    for(var j=0;j<nb;j++){\n");
        w.write("      var m=mask[j];if(Object.keys(m).length===0)continue;\n");
        w.write("      var bLow=ses.low+j*ses.bs,bHi=bLow+ses.bs;\n");
        w.write("      var y1=rpMT+rpPH-(bHi-rpGLow)/rpGRange*rpPH;\n");
        w.write("      var y2=rpMT+rpPH-(bLow-rpGLow)/rpGRange*rpPH;\n");
        w.write("      var rH=Math.max(1,y2-y1);\n");
        w.write("      var fs=Math.max(10,Math.min(12,rH*0.9));\n");
        w.write("      var col=0;\n");
        w.write("      for(var b=0;b<62;b++){if(m[b]){\n");
        w.write("        var ch=b<bChars.length?bChars[b]:'?';\n");
        w.write("        var lclr=bCols[b%bCols.length];\n");
        w.write("        var t=document.createElementNS(ns,'text');\n");
        w.write("        t.setAttribute('x',(ses.x+col*lw).toFixed(1));\n");
        w.write("        var ly=y1+fs*0.85;\n");
        w.write("        t.setAttribute('y',ly.toFixed(1));\n");
        w.write("        t.setAttribute('font-family','Monospace');\n");
        w.write("        t.setAttribute('font-size',fs.toFixed(1));\n");
        w.write("        t.setAttribute('fill',lclr);\n");
        w.write("        t.textContent=ch;\n");
        w.write("        pg.appendChild(t);col++;\n");
        w.write("      }}\n");
        w.write("    }\n");
        w.write("  }\n");
        w.write("  rpUpdateUI();\n");
        w.write("}\n");

        // rpUpdateUI
        w.write("function rpUpdateUI(){\n");
        w.write("  var ses=rpSessions[rpCurSes],total=ses.n;\n");
        w.write("  var pct=total>0?rpStep/total:1;\n");
        w.write("  rpKnob.setAttribute('cx',rpSlX+pct*rpSlW);\n");
        w.write("  rpFill.setAttribute('x2',rpSlX+pct*rpSlW);\n");
        w.write("  rpCtr.textContent=rpStep+' / '+total;\n");
        w.write("  rpSesLbl.textContent='Session '+(rpCurSes+1)+'/'+rpN;\n");
        w.write("  var rpSI=document.getElementById('rpSesInput');if(rpSI)rpSI.value=(rpCurSes+1);\n");
        w.write("  rpHilight.setAttribute('x',rpSesXArr[rpCurSes]);\n");
        w.write("  rpHilight.setAttribute('width',rpSesXEnd[rpCurSes]-rpSesXArr[rpCurSes]);\n");
        w.write("  rpHilight.style.display='';\n");
        w.write("  rpUpdateKpi();\n");
        w.write("}\n");

        // rpUpdateKpi — TPO only has Vol/Bull/Bear
        w.write("function rpUpdateKpi(){try{\n");
        w.write("  for(var q=0;q<rpN;q++){\n");
        w.write("    var ve=document.getElementById('rpVol_'+q);\n");
        w.write("    if(!ve)continue;\n");
        w.write("    var ok=rpOrigKpi[q];\n");
        w.write("    if(q>rpCurSes){\n");
        w.write("      ve.textContent='Vol: 0';\n");
        w.write("      document.getElementById('rpBull_'+q).textContent='Bull: 0';\n");
        w.write("      document.getElementById('rpBear_'+q).textContent='Bear: 0';\n");
        w.write("    } else if(q!==rpCurSes||rpStep>=rpSessions[q].n){\n");
        w.write("      ve.textContent='Vol: '+ok.v;\n");
        w.write("      document.getElementById('rpBull_'+q).textContent='Bull: '+ok.b;\n");
        w.write("      document.getElementById('rpBear_'+q).textContent='Bear: '+ok.r;\n");
        w.write("    }\n");
        w.write("  }\n");
        w.write("  var s=rpCurSes,ses=rpSessions[s];\n");
        w.write("  if(rpStep<ses.n){\n");
        w.write("    var tv=0,bv=0,rv=0;\n");
        w.write("    for(var i=0;i<rpStep;i++){\n");
        w.write("      var bar=ses.bars[i];\n");
        w.write("      for(var k in bar.d){var vol=bar.d[k];tv+=vol;if(bar.b)bv+=vol;else rv+=vol;}\n");
        w.write("    }\n");
        w.write("    document.getElementById('rpVol_'+s).textContent='Vol: '+Math.round(tv);\n");
        w.write("    document.getElementById('rpBull_'+s).textContent='Bull: '+Math.round(bv);\n");
        w.write("    document.getElementById('rpBear_'+s).textContent='Bear: '+Math.round(rv);\n");
        w.write("  }\n");
        w.write("}catch(e){}}\n");

        // rpSetStep
        w.write("function rpSetStep(s){var t=rpSessions[rpCurSes].n;rpStep=Math.max(0,Math.min(t,s));rpRender();}\n");

        // rpSwitchSession (relative)
        w.write("function rpSwitchSession(dir){\n");
        w.write("  rpGoToSession(rpCurSes+dir);\n");
        w.write("}\n");

        // rpGoToSession: navigate directly to session index n (0-based)
        w.write("function rpGoToSession(n){\n");
        w.write("  if(rpPlaying){rpPlaying=false;clearInterval(rpTimer);rpTimer=null;rpPauseIco.style.display='none';}\n");
        w.write("  var ns=Math.max(0,Math.min(rpN-1,n));\n");
        w.write("  if(ns===rpCurSes)return;\n");
        w.write("  rpStep=rpSessions[rpCurSes].n;rpRender();\n");
        w.write("  rpCurSes=ns;\n");
        w.write("  rpStep=rpSessions[rpCurSes].n;\n");
        w.write("  rpRender();\n");
        w.write("}\n");

        // rpJumpSession: jump by delta sessions (e.g. +10 or -10)
        w.write("function rpJumpSession(delta){\n");
        w.write("  rpGoToSession(rpCurSes+delta);\n");
        w.write("}\n");

        // rpTogglePlay
        w.write("function rpTogglePlay(){\n");
        w.write("  if(rpPlaying){rpPlaying=false;clearInterval(rpTimer);rpTimer=null;}\n");
        w.write("  else{if(rpStep>=rpSessions[rpCurSes].n)rpStep=0;rpPlaying=true;rpTimer=setInterval(function(){\n");
        w.write("    rpStep++;if(rpStep>=rpSessions[rpCurSes].n){\n");
        w.write("      if(rpCurSes<rpN-1){rpStep=rpSessions[rpCurSes].n;rpRender();rpCurSes++;rpStep=0;rpRender();}\n");
        w.write("      else{rpPlaying=false;clearInterval(rpTimer);rpTimer=null;rpPauseIco.style.display='none';rpRender();}\n");
        w.write("      return;}\n");
        w.write("    rpRender();\n");
        w.write("  },300);}\n");
        w.write("  rpPauseIco.style.display=rpPlaying?'':'none';\n");
        w.write("}\n");

        // Button handlers
        w.write("document.getElementById('rpSesFirst').addEventListener('click',function(){rpGoToSession(0);});\n");
        w.write("document.getElementById('rpSesPrev10').addEventListener('click',function(){rpJumpSession(-10);});\n");
        w.write("document.getElementById('rpFirst').addEventListener('click',function(){rpSwitchSession(-1);});\n");
        w.write("document.getElementById('rpPrev').addEventListener('click',function(){rpSetStep(rpStep-1);});\n");
        w.write("document.getElementById('rpPlay').addEventListener('click',function(){rpTogglePlay();});\n");
        w.write("document.getElementById('rpNext').addEventListener('click',function(){rpSetStep(rpStep+1);});\n");
        w.write("document.getElementById('rpLast').addEventListener('click',function(){rpSwitchSession(1);});\n");
        w.write("document.getElementById('rpSesNext10').addEventListener('click',function(){rpJumpSession(10);});\n");
        w.write("document.getElementById('rpSesLast').addEventListener('click',function(){rpGoToSession(rpN-1);});\n");

        // Session number textbox handler
        w.write("var rpSesInput=document.getElementById('rpSesInput');\n");
        w.write("if(rpSesInput){\n");
        w.write("  rpSesInput.addEventListener('mousedown',function(e){e.stopPropagation();});\n");
        w.write("  rpSesInput.addEventListener('click',function(e){e.stopPropagation();this.focus();});\n");
        w.write("  rpSesInput.addEventListener('keydown',function(e){\n");
        w.write("    if(e.key==='Enter'){e.preventDefault();var v=parseInt(this.value,10);\n");
        w.write("      if(!isNaN(v))rpGoToSession(v-1);}\n");
        w.write("    e.stopPropagation();\n");
        w.write("  });\n");
        w.write("  rpSesInput.addEventListener('keyup',function(e){e.stopPropagation();});\n");
        w.write("}\n");

        // Slider
        w.write("var rpDragging=false;\n");
        w.write("function rpSliderPos(e){\n");
        w.write("  var pt=svgPt(e);\n");
        w.write("  var pct=Math.max(0,Math.min(1,(pt.x-rpSlX)/rpSlW));\n");
        w.write("  rpSetStep(Math.round(pct*rpSessions[rpCurSes].n));\n");
        w.write("}\n");
        w.write("document.getElementById('rpSliderHit').addEventListener('mousedown',function(e){rpDragging=true;rpSliderPos(e);});\n");
        w.write("document.getElementById('rpSliderKnob').addEventListener('mousedown',function(e){rpDragging=true;});\n");
        w.write("svg.addEventListener('mousemove',function(e){if(rpDragging)rpSliderPos(e);});\n");
        w.write("svg.addEventListener('mouseup',function(){rpDragging=false;});\n");

        // Keyboard shortcuts
        w.write("document.addEventListener('keydown',function(e){\n");
        w.write("  if(e.target&&e.target.tagName==='INPUT')return;\n");
        w.write("  if(e.ctrlKey&&e.key==='Home'){e.preventDefault();rpGoToSession(0);return;}\n");
        w.write("  if(e.ctrlKey&&e.key==='End'){e.preventDefault();rpGoToSession(rpN-1);return;}\n");
        w.write("  if(e.key==='ArrowLeft')rpSetStep(rpStep-1);\n");
        w.write("  else if(e.key==='ArrowRight')rpSetStep(rpStep+1);\n");
        w.write("  else if(e.key===' '){e.preventDefault();rpTogglePlay();}\n");
        w.write("  else if(e.key==='Home'){e.preventDefault();rpSetStep(0);}\n");
        w.write("  else if(e.key==='End'){e.preventDefault();rpSetStep(rpSessions[rpCurSes].n);}\n");
        w.write("  else if(e.key==='PageUp'){e.preventDefault();rpSwitchSession(-1);}\n");
        w.write("  else if(e.key==='PageDown'){e.preventDefault();rpSwitchSession(1);}\n");
        w.write("});\n");

        // Initialize
        w.write("rpUpdateUI();\n");
        w.write("}\n");
        w.write("hilite();})();\n");
        w.write("]]></script>\n");
    }

    /**
     * Formats legend times:
     * - same-day sessions: HH:mm
     * - multi-day sessions: MM-dd HH:mm
     */
    protected static String formatLegendTime(long t, long sessionStart, long sessionEnd) {
        boolean sameDay = (SQTime.getFullYear(t) == SQTime.getFullYear(sessionStart))
                && (SQTime.getDayOfYear(t) == SQTime.getDayOfYear(sessionStart));

        int hh = SQTime.getHour(t);
        int mm = SQTime.getMinute(t);

        if (sameDay) {
            return String.format(Locale.US, "%02d:%02d", hh, mm);
        } else {
            int mo = SQTime.getMonthOriginal(t);
            int da = SQTime.getDay(t);
            return String.format(Locale.US, "%02d-%02d %02d:%02d", mo, da, hh, mm);
        }
    }

    protected long sumTPOCounts() {
        long sum = 0;
        if (tpoBins == null)
            return 0;
        for (int i = 0; i < tpoBins.length; i++)
            sum += tpoBins[i];
        return sum;
    }

    protected static String buildLettersString(long mask, int bracketCount) {
        if (bracketCount <= 0 || mask == 0L)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bracketCount; i++) {
            if (((mask >>> i) & 1L) != 0L) {
                sb.append(bracketChar(i));
            }
        }
        return sb.toString();
    }

    protected static char bracketChar(int idx) {
        if (idx < 26)
            return (char) ('A' + idx);
        if (idx < 52)
            return (char) ('a' + (idx - 26));
        if (idx < 62)
            return (char) ('0' + (idx - 52));
        return '?';
    }

    protected static String bracketColor(int bracketIndex) {
        if (bracketIndex < 0)
            return "#000000";
        return TPO_LETTER_PALETTE[bracketIndex % TPO_LETTER_PALETTE.length];
    }

    /**
     * Returns the TPO bracket size in milliseconds based on session type.
     * Uses configurable BracketMinXxx parameters per session type.
     */
    protected long getBracketMillis() {
        int bracketMinutes;
        if (cfgSessionType() == 1 || cfgSessionType() == 5)
            bracketMinutes = cfgBracketMinDaily();
        else if (cfgSessionType() == 2 || cfgSessionType() == 6)
            bracketMinutes = cfgBracketMinWeekly();
        else if (cfgSessionType() == 3 || cfgSessionType() == 7)
            bracketMinutes = cfgBracketMinMonthly();
        else if (cfgSessionType() == 4 || cfgSessionType() == 8)
            bracketMinutes = cfgBracketMinYearly();
        else
            bracketMinutes = cfgBracketMinDaily();

        return (long) bracketMinutes * 60L * 1000L;
    }

    /**
     * Returns the Initial Balance period in milliseconds based on session type.
     * Daily=60min, Weekly=12h, Monthly=24h, Yearly=6days.
     */
    protected long getIBPeriodMillis() {
        int baseType;
        if (cfgSessionType() == 1 || cfgSessionType() == 5)
            baseType = 1; // Daily
        else if (cfgSessionType() == 2 || cfgSessionType() == 6)
            baseType = 2; // Weekly
        else if (cfgSessionType() == 3 || cfgSessionType() == 7)
            baseType = 3; // Monthly
        else if (cfgSessionType() == 4 || cfgSessionType() == 8)
            baseType = 4; // Yearly
        else
            baseType = 1;

        switch (baseType) {
            case 1:
                return 60L * 60L * 1000L; // 60 minutes
            case 2:
                return 12L * 60L * 60L * 1000L; // 12 hours
            case 3:
                return 24L * 60L * 60L * 1000L; // 24 hours
            case 4:
                return 6L * 24L * 60L * 60L * 1000L; // 6 days
            default:
                return 60L * 60L * 1000L;
        }
    }

    // ------------------------------------------------------------------------
    // Abstract getters for @Parameter fields (declared in concrete indicators)
    // ------------------------------------------------------------------------

    protected abstract com.strategyquant.tradinglib.ChartData cfgChart();

    protected abstract int cfgBinSizeMode();

    protected abstract int cfgProfileRows();

    protected abstract int cfgTicksPerBin();

    protected abstract double cfgValueAreaPct();

    protected abstract int cfgIBMinutes();

    protected abstract int cfgSessionType();

    protected abstract boolean cfgShowCandlesticks();

    protected abstract boolean cfgShowVolumeSubchart();

    protected abstract int cfgVolumeMALength();

    protected abstract boolean cfgShowPOCDelta();

    protected abstract boolean cfgShowVADelta();

    protected abstract boolean cfgShowProfileRange();

    protected abstract boolean cfgShowPOCPosition();

    protected abstract int cfgBracketMinDaily();

    protected abstract int cfgBracketMinWeekly();

    protected abstract int cfgBracketMinMonthly();

    protected abstract int cfgBracketMinYearly();

    protected abstract boolean cfgShowShapeLabel();

    protected abstract boolean cfgUseBlockMode();
}
