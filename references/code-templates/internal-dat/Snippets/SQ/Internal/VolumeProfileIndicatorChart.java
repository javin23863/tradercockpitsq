/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
 * Volume Profile Chart - intermediate helper class for VP indicators.
 */
package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

/**
 * Abstract base class for Volume Profile chart indicators.
 * Contains VP calculation logic, SVG export, and VP-specific fields.
 * Concrete indicators (VolumeProfile, VolumeProfileCustomHours, etc.) extend
 * this.
 */
public abstract class VolumeProfileIndicatorChart extends AbstractChart {

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
    protected static final int DEFAULT_DELTA_FONT_SIZE = 8;
    protected static final int DEFAULT_CHART_HEIGHT = 800;
    protected static final int DEFAULT_SESSION_WIDTH = 400;

    // Previous session profile results (VP-specific)
    protected double prevPOC = 0;
    protected double prevVAH = 0;
    protected double prevVAL = 0;
    protected double[] prevHVN = new double[5];
    protected double[] prevLVN = new double[5];

    // VCP results
    protected double prevVPOC = 0;
    protected double prevVVAH = 0;
    protected double prevVVAL = 0;

    // Bull/Bear POC results
    protected double prevBullPOC = 0;
    protected double prevBearPOC = 0;
    protected double[] bullVolumeBins;
    protected double[] bearVolumeBins;

    // Derived data series results
    protected double prevDelta = 0;
    protected double prevDeltaPOC = 0;
    protected double prevDeltaVA = 0;
    protected double prevRange = 0;
    protected double prevPOCPctUp = 0;
    protected double prevPOCPctDown = 0;
    // Track previous session values for delta calculations
    protected double prevPrevPOC = 0;
    protected double prevPrevVAMid = 0;

    // ZigZag state
    protected int zzDirection = 0;
    protected double zzPivotHigh = 0;
    protected double zzPivotLow = Double.MAX_VALUE;
    protected long zzPivotHighTime = 0;
    protected long zzPivotLowTime = 0;
    protected long zzSessionStart = 0;
    protected long zzLastPivotTime = 0;
    protected boolean zzPivotConfirmed = false;
    protected double zzLastPivotPrice = 0;
    protected int zzLastPivotDir = 0;

    // Arrays for profile calculation (reused)
    protected double[] volumeBins;

    // VP history arrays
    protected double[] histPOC, histVAH, histVAL;
    protected double[][] histVolumeBins;
    protected double[][] histBullBins;
    protected double[][] histBearBins;
    protected double[][] histHVN;
    protected double[][] histLVN;
    protected double[] histPivotPrice;
    protected int[] histPivotDir;
    protected String[] histSessionLabel;

    // ------------------------------------------------------------------------
    // VP-specific methods
    // ------------------------------------------------------------------------

    // Allocates working arrays for volume bins. Fixed Tick mode uses MAX_BINS (worst case),
    // Range-Based mode uses exactly ProfileRows bins.
    protected void ensureArrays() {
        int needed = (cfgBinSizeMode() == 2) ? MAX_BINS : cfgProfileRows();
        if (volumeBins == null || volumeBins.length < needed) {
            volumeBins = new double[needed];
        }
        if (clusterBins == null || clusterBins.length < needed) {
            clusterBins = new double[needed];
        }
        if (bullVolumeBins == null || bullVolumeBins.length < needed) {
            bullVolumeBins = new double[needed];
        }
        if (bearVolumeBins == null || bearVolumeBins.length < needed) {
            bearVolumeBins = new double[needed];
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
            histSessionLabel = new String[cap];
            histSessionEnd = new long[cap];
            histPOC = new double[cap];
            histVAH = new double[cap];
            histVAL = new double[cap];
            histIBH = new double[cap];
            histIBL = new double[cap];
            histSessionHigh = new double[cap];
            histSessionLow = new double[cap];
            histVolumeBins = new double[cap][binCap]; // full bin snapshots — needed for SVG histogram bars
            histBullBins = new double[cap][binCap];
            histBearBins = new double[cap][binCap];
            histHVN = new double[cap][5];
            histLVN = new double[cap][5];
            histNumBins = new int[cap];
            histTotalVolume = new double[cap];
            histBullVolume = new double[cap];
            histBearVolume = new double[cap];
            histPivotPrice = new double[cap];
            histPivotDir = new int[cap];
            historyCount = 0;
        }
        // Ensure bull/bear bin arrays exist (may be missing from older allocation)
        if (histBullBins == null || histBullBins.length < cap) {
            histBullBins = new double[cap][binCap];
        }
        if (histBearBins == null || histBearBins.length < cap) {
            histBearBins = new double[cap][binCap];
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
        histSessionLabel = Arrays.copyOf(histSessionLabel, newCap);
        histSessionEnd = Arrays.copyOf(histSessionEnd, newCap);
        histPOC = Arrays.copyOf(histPOC, newCap);
        histVAH = Arrays.copyOf(histVAH, newCap);
        histVAL = Arrays.copyOf(histVAL, newCap);
        histIBH = Arrays.copyOf(histIBH, newCap);
        histIBL = Arrays.copyOf(histIBL, newCap);
        histSessionHigh = Arrays.copyOf(histSessionHigh, newCap);
        histSessionLow = Arrays.copyOf(histSessionLow, newCap);
        histNumBins = Arrays.copyOf(histNumBins, newCap);
        histTotalVolume = Arrays.copyOf(histTotalVolume, newCap);
        histBullVolume = Arrays.copyOf(histBullVolume, newCap);
        histBearVolume = Arrays.copyOf(histBearVolume, newCap);
        histPivotPrice = Arrays.copyOf(histPivotPrice, newCap);
        histPivotDir = Arrays.copyOf(histPivotDir, newCap);
        histVolumeBins = Arrays.copyOf(histVolumeBins, newCap);
        histBullBins = Arrays.copyOf(histBullBins, newCap);
        histBearBins = Arrays.copyOf(histBearBins, newCap);
        histHVN = Arrays.copyOf(histHVN, newCap);
        histLVN = Arrays.copyOf(histLVN, newCap);
        for (int i = oldCap; i < newCap; i++) {
            histVolumeBins[i] = new double[binCap];
            histBullBins[i] = new double[binCap];
            histBearBins[i] = new double[binCap];
            histHVN[i] = new double[5];
            histLVN[i] = new double[5];
        }
    }

    // Saves the finished session profile into history arrays for SVG export.
    // If the same session start already exists (e.g. Actual mode recalculates every bar),
    // the existing entry is updated in-place instead of appending a duplicate.
    protected void pushSessionHistory(double sessionHigh, double sessionLow) {
        ensureHistory();
        int nb = lastNumBins;

        // Search last 8 entries — multi-session indicators (London/NY/Tokyo) can interleave
        int updateIdx = -1;
        for (int i = historyCount - 1; i >= Math.max(0, historyCount - 8); i--) {
            if (histSessionStart[i] == prevSessionStart) {
                updateIdx = i;
                break;
            }
        }
        if (updateIdx >= 0) {
            int idx = updateIdx;
            histSessionEnd[idx] = prevSessionEnd;
            if (cfgCurrentSessionLabel() != null)
                histSessionLabel[idx] = cfgCurrentSessionLabel();
            histPOC[idx] = prevPOC;
            histVAH[idx] = prevVAH;
            histVAL[idx] = prevVAL;
            histIBH[idx] = prevIBH;
            histIBL[idx] = prevIBL;
            histSessionHigh[idx] = sessionHigh;
            histSessionLow[idx] = sessionLow;
            histNumBins[idx] = nb;
            double[] srcBins = (cfgEnableVCP() && clusterBins != null && clusterBins.length >= nb) ? clusterBins
                    : volumeBins;
            System.arraycopy(srcBins, 0, histVolumeBins[idx], 0, nb);
            System.arraycopy(bullVolumeBins, 0, histBullBins[idx], 0, nb);
            System.arraycopy(bearVolumeBins, 0, histBearBins[idx], 0, nb);
            System.arraycopy(prevHVN, 0, histHVN[idx], 0, 5);
            System.arraycopy(prevLVN, 0, histLVN[idx], 0, 5);
            histTotalVolume[idx] = prevTotalVolume;
            histBullVolume[idx] = prevTotalBullVolume;
            histBearVolume[idx] = prevTotalBearVolume;
            histPivotPrice[idx] = zzLastPivotPrice;
            histPivotDir[idx] = zzLastPivotDir;
            return;
        }

        if (historyCount >= histSessionStart.length) {
            if (DEFAULT_MAX_SESSIONS_FOR_SVG > 0) {
                // Capped mode: drop oldest session by shifting everything left by 1
                int cap = histSessionStart.length;
                System.arraycopy(histSessionStart,  1, histSessionStart,  0, cap - 1);
                System.arraycopy(histSessionEnd,    1, histSessionEnd,    0, cap - 1);
                System.arraycopy(histSessionLabel,  1, histSessionLabel,  0, cap - 1);
                System.arraycopy(histPOC,           1, histPOC,           0, cap - 1);
                System.arraycopy(histVAH,           1, histVAH,           0, cap - 1);
                System.arraycopy(histVAL,           1, histVAL,           0, cap - 1);
                System.arraycopy(histIBH,           1, histIBH,           0, cap - 1);
                System.arraycopy(histIBL,           1, histIBL,           0, cap - 1);
                System.arraycopy(histSessionHigh,   1, histSessionHigh,   0, cap - 1);
                System.arraycopy(histSessionLow,    1, histSessionLow,    0, cap - 1);
                System.arraycopy(histNumBins,       1, histNumBins,       0, cap - 1);
                System.arraycopy(histTotalVolume,   1, histTotalVolume,   0, cap - 1);
                System.arraycopy(histBullVolume,    1, histBullVolume,    0, cap - 1);
                System.arraycopy(histBearVolume,    1, histBearVolume,    0, cap - 1);
                System.arraycopy(histPivotPrice,    1, histPivotPrice,    0, cap - 1);
                System.arraycopy(histPivotDir,      1, histPivotDir,      0, cap - 1);
                System.arraycopy(histVolumeBins,    1, histVolumeBins,    0, cap - 1);
                System.arraycopy(histBullBins,      1, histBullBins,      0, cap - 1);
                System.arraycopy(histBearBins,      1, histBearBins,      0, cap - 1);
                System.arraycopy(histHVN,           1, histHVN,           0, cap - 1);
                System.arraycopy(histLVN,           1, histLVN,           0, cap - 1);
                historyCount = cap - 1; // new entry will be written at last slot
            } else {
                growHistory();
            }
        }
        int idx = historyCount;
        histSessionStart[idx] = prevSessionStart;
        histSessionEnd[idx] = prevSessionEnd;
        histSessionLabel[idx] = cfgCurrentSessionLabel();
        histPOC[idx] = prevPOC;
        histVAH[idx] = prevVAH;
        histVAL[idx] = prevVAL;
        histIBH[idx] = prevIBH;
        histIBL[idx] = prevIBL;
        histSessionHigh[idx] = sessionHigh;
        histSessionLow[idx] = sessionLow;
        histNumBins[idx] = nb;
        double[] srcBins2 = (cfgEnableVCP() && clusterBins != null && clusterBins.length >= nb) ? clusterBins
                : volumeBins;
        System.arraycopy(srcBins2, 0, histVolumeBins[idx], 0, nb);
        System.arraycopy(bullVolumeBins, 0, histBullBins[idx], 0, nb);
        System.arraycopy(bearVolumeBins, 0, histBearBins[idx], 0, nb);
        System.arraycopy(prevHVN, 0, histHVN[idx], 0, 5);
        System.arraycopy(prevLVN, 0, histLVN[idx], 0, 5);
        histTotalVolume[idx] = prevTotalVolume;
        histBullVolume[idx] = prevTotalBullVolume;
        histBearVolume[idx] = prevTotalBearVolume;
        histPivotPrice[idx] = zzLastPivotPrice;
        histPivotDir[idx] = zzLastPivotDir;
        historyCount++;
    }

    // Releases all large arrays after the backtest is finished so GC can reclaim memory immediately.
    // Called after SVG export (super.OnDeinit) so the export still has access to the data.
    @Override
    protected void OnDeinit() throws TradingException {
        super.OnDeinit(); // SVG export if StoreChartData = true

        // Working arrays
        volumeBins     = null;
        clusterBins    = null;
        bullVolumeBins = null;
        bearVolumeBins = null;

        // Session history arrays (large — sessions × bins)
        histVolumeBins   = null;
        histBullBins     = null;
        histBearBins     = null;
        histSessionStart = null;
        histSessionEnd   = null;
        histSessionLabel = null;
        histPOC          = null;
        histVAH          = null;
        histVAL          = null;
        histIBH          = null;
        histIBL          = null;
        histSessionHigh  = null;
        histSessionLow   = null;
        histHVN          = null;
        histLVN          = null;
        histNumBins      = null;
        histTotalVolume  = null;
        histBullVolume   = null;
        histBearVolume   = null;
        histPivotPrice   = null;
        histPivotDir     = null;

        // Small result arrays
        prevHVN = null;
        prevLVN = null;
    }

    // Calculates the Volume Profile for [prevSessionStart, prevSessionEnd).
    // Pass 1: find session high/low + Initial Balance.
    // Pass 2: distribute each bar's volume evenly across the bins it touches.
    // Then finds POC, Value Area, HVN/LVN, VCP, Bull/Bear POC and derived series.
    protected void calculateVolumeProfile() throws TradingException {
        if (cfgChart() == null) {
            return;
        }

        // Pass 1: find session high/low and Initial Balance high/low
        double sessionHigh = Double.MIN_VALUE;
        double sessionLow = Double.MAX_VALUE;
        int barsInSession = 0;
        double sessionTotalVolume = 0;

        // Initial Balance: high/low of the first N minutes of the session
        long ibEndTime = prevSessionStart + getIBPeriodMillis();
        double ibHigh = Double.MIN_VALUE;
        double ibLow = Double.MAX_VALUE;

        int i = 0;
        try {
            while (true) {
                long barTime = cfgChart().Time(i);

                if (barTime < prevSessionStart) {
                    break; // bars are in reverse-chronological order
                }

                if (barTime >= prevSessionStart && barTime < prevSessionEnd) {
                    if (isSunday(barTime)) { // skip Forex weekend gap bars
                        i++;
                        continue;
                    }
                    double hi = cfgChart().High(i);
                    double lo = cfgChart().Low(i);
                    sessionHigh = Math.max(sessionHigh, hi);
                    sessionLow = Math.min(sessionLow, lo);
                    barsInSession++;
                    sessionTotalVolume += cfgChart().Volume(i);

                    // Track IB high/low for bars within IB period
                    if (barTime < ibEndTime) {
                        ibHigh = Math.max(ibHigh, hi);
                        ibLow = Math.min(ibLow, lo);
                    }
                }
                i++;
            }
        } catch (Exception e) {
        }

        if (barsInSession == 0 || sessionHigh <= sessionLow) {
            return;
        }

        // Store Initial Balance values
        if (ibHigh > ibLow) {
            prevIBH = ibHigh;
            prevIBL = ibLow;
        }

        double range = sessionHigh - sessionLow;

        // Bin size: Fixed Tick = TicksPerBin * tickSize; Range-Based = range / ProfileRows
        double tickSize = cfgChart().getInstrumentInfo().tickStep;
        int numBins;
        double binSize;

        if (cfgBinSizeMode() == 2) {
            binSize = cfgTicksPerBin() * tickSize;
            numBins = (int) Math.ceil(range / binSize);
            numBins = Math.max(1, Math.min(numBins, MAX_BINS));
        } else {
            numBins = cfgProfileRows();
            binSize = range / numBins;
        }
        lastNumBins = numBins;

        if (volumeBins == null || volumeBins.length < numBins) {
            volumeBins = new double[numBins];
        }

        // Clear bins
        for (int j = 0; j < numBins; j++) {
            volumeBins[j] = 0;
        }
        if (bullVolumeBins == null || bullVolumeBins.length < numBins) {
            bullVolumeBins = new double[numBins];
        }
        if (bearVolumeBins == null || bearVolumeBins.length < numBins) {
            bearVolumeBins = new double[numBins];
        }
        for (int j = 0; j < numBins; j++) {
            bullVolumeBins[j] = 0;
            bearVolumeBins[j] = 0;
        }

        // Pass 2: distribute each bar's volume across the bins it touches
        double totalVolume = 0;
        double totalBullVolume = 0;
        double totalBearVolume = 0;

        i = 0;
        try {
            while (true) {
                long barTime = cfgChart().Time(i);

                if (barTime < prevSessionStart) {
                    break;
                }

                if (barTime >= prevSessionStart && barTime < prevSessionEnd) {
                    if (isSunday(barTime)) {
                        i++;
                        continue;
                    }
                    double close = cfgChart().Close(i);
                    double open = cfgChart().Open(i);
                    double high = cfgChart().High(i);
                    double low = cfgChart().Low(i);
                    double barVol = cfgChart().Volume(i);

                    // Map bar high/low to bin indices (*1000 to reduce floating-point error)
                    int vBinHigh = (int) (((high - sessionLow) * 1000) / (binSize * 1000));
                    int vBinLow  = (int) (((low  - sessionLow) * 1000) / (binSize * 1000));

                    vBinHigh = Math.max(0, Math.min(numBins - 1, vBinHigh));
                    vBinLow  = Math.max(0, Math.min(numBins - 1, vBinLow));

                    // Volume is split evenly across all bins the bar touches
                    double perBin = barVol / (vBinHigh - vBinLow + 1);
                    boolean isBull = (close >= open);
                    for (int i1 = vBinLow; i1 <= vBinHigh; i1++) {
                        volumeBins[i1] += perBin;
                        if (isBull) {
                            bullVolumeBins[i1] += perBin;
                        } else {
                            bearVolumeBins[i1] += perBin;
                        }
                    }

                    totalVolume += barVol;
                    if (isBull) {
                        totalBullVolume += barVol;
                    } else {
                        totalBearVolume += barVol;
                    }

                }
                i++;
            }
        } catch (Exception e) {
        }

        // POC = bin with the highest volume; price = centre of that bin
        int pocIndex = 0;
        double maxVolume = volumeBins[0];
        for (int j = 1; j < numBins; j++) {
            if (volumeBins[j] > maxVolume) {
                maxVolume = volumeBins[j];
                pocIndex = j;
            }
        }
        prevPOC = sessionLow + (pocIndex + 0.5) * binSize;

        calculateValueArea(pocIndex, totalVolume, binSize, sessionLow, numBins);

        findHVNs(pocIndex, binSize, sessionLow, numBins);
        if (cfgEnableLVN())
            findLVNs(pocIndex, binSize, sessionLow, numBins);

        // VCP: Gaussian-enhanced profile; if disabled, VCP outputs equal standard values
        if (cfgEnableVCP()) {
            applyClusterEnhancement(numBins, binSize, sessionLow, totalVolume);
        } else {
            prevVPOC = prevPOC;
            prevVVAH = prevVAH;
            prevVVAL = prevVAL;
        }

        // Bull/Bear POC: POC calculated separately for bull and bear candles
        int bullPocIdx = 0;
        double bullMax = bullVolumeBins[0];
        int bearPocIdx = 0;
        double bearMax = bearVolumeBins[0];
        for (int j = 1; j < numBins; j++) {
            if (bullVolumeBins[j] > bullMax) {
                bullMax = bullVolumeBins[j];
                bullPocIdx = j;
            }
            if (bearVolumeBins[j] > bearMax) {
                bearMax = bearVolumeBins[j];
                bearPocIdx = j;
            }
        }
        prevBullPOC = (bullMax > 0) ? sessionLow + (bullPocIdx + 0.5) * binSize : 0;
        prevBearPOC = (bearMax > 0) ? sessionLow + (bearPocIdx + 0.5) * binSize : 0;

        prevTotalVolume = totalVolume;
        prevTotalBullVolume = totalBullVolume;
        prevTotalBearVolume = totalBearVolume;

        // Derived series
        prevDelta = totalBullVolume - totalBearVolume; // net buying pressure
        double newVAMid = (prevVAH + prevVAL) / 2.0;
        prevDeltaPOC = (prevPrevPOC != 0) ? prevPOC - prevPrevPOC : 0;   // POC shift vs previous session
        prevDeltaVA  = (prevPrevVAMid != 0) ? newVAMid - prevPrevVAMid : 0; // VA midpoint shift
        prevRange = sessionHigh - sessionLow;
        if (prevRange > 0) {
            prevPOCPctUp   = (sessionHigh - prevPOC) / prevRange * 100.0; // POC distance from top (%)
            prevPOCPctDown = (prevPOC - sessionLow)  / prevRange * 100.0; // POC distance from bottom (%)
        } else {
            prevPOCPctUp = 0;
            prevPOCPctDown = 0;
        }
        prevPrevPOC = prevPOC;
        prevPrevVAMid = newVAMid;

        // Save to history only when chart export is enabled — history arrays are large
        if (cfgStoreChartData()) {
            pushSessionHistory(sessionHigh, sessionLow);
        }
    }

    // Calculates VAH and VAL: expands outward from POC, always absorbing the higher-volume
    // neighbouring bin first, until ValueAreaPct% of total volume is captured.
    protected void calculateValueArea(int pocIndex, double totalVolume, double binSize, double sessionLow,
            int numBins) {
        double targetVolume = totalVolume * (cfgValueAreaPct() / 100.0);
        double accumulatedVolume = volumeBins[pocIndex];

        int upperIndex = pocIndex;
        int lowerIndex = pocIndex;

        while (accumulatedVolume < targetVolume) {
            boolean canGoUp   = (upperIndex + 1) < numBins;
            boolean canGoDown = (lowerIndex - 1) >= 0;

            if (!canGoUp && !canGoDown) {
                break;
            }

            double volumeAbove = canGoUp   ? volumeBins[upperIndex + 1] : -1;
            double volumeBelow = canGoDown ? volumeBins[lowerIndex - 1] : -1;

            if (volumeAbove >= volumeBelow) {
                upperIndex++;
                accumulatedVolume += volumeBins[upperIndex];
            } else {
                lowerIndex--;
                accumulatedVolume += volumeBins[lowerIndex];
            }
        }

        prevVAL = sessionLow + lowerIndex * binSize;         // bottom edge of lowest included bin
        prevVAH = sessionLow + (upperIndex + 1) * binSize;  // top edge of highest included bin
    }

    // Finds HVNs: local volume peaks above HvnThresholdPct%, excluding POC.
    // Candidates are sorted by volume and accepted greedily — only if a real valley
    // (min volume < 50% of the smaller peak) separates them from already-accepted nodes.
    protected void findHVNs(int pocIndex, double binSize, double sessionLow, int numBins) {
        for (int n = 0; n < 5; n++)
            prevHVN[n] = 0;

        double maxVol = 0;
        for (int j = 0; j < numBins; j++) {
            if (volumeBins[j] > maxVol)
                maxVol = volumeBins[j];
        }
        if (maxVol <= 0)
            return;

        double threshold = maxVol * cfgHvnThresholdPct() / 100.0;
        int maxNodes = Math.min(cfgHvnCount(), 5);

        // Collect local maxima above threshold (excluding POC)
        int[] candidateIdx = new int[numBins];
        double[] candidateVol = new double[numBins];
        int candidateCount = 0;

        for (int j = 0; j < numBins; j++) {
            if (j == pocIndex)
                continue;
            if (volumeBins[j] < threshold)
                continue;

            double left  = (j > 0)           ? volumeBins[j - 1] : -1;
            double right = (j < numBins - 1) ? volumeBins[j + 1] : -1;
            if (volumeBins[j] > left && volumeBins[j] > right) {
                candidateIdx[candidateCount] = j;
                candidateVol[candidateCount] = volumeBins[j];
                candidateCount++;
            }
        }

        // Sort by volume descending
        for (int a = 0; a < candidateCount - 1; a++) {
            int best = a;
            for (int b = a + 1; b < candidateCount; b++) {
                if (candidateVol[b] > candidateVol[best])
                    best = b;
            }
            if (best != a) {
                double tmpV = candidateVol[a];
                candidateVol[a] = candidateVol[best];
                candidateVol[best] = tmpV;
                int tmpI = candidateIdx[a];
                candidateIdx[a] = candidateIdx[best];
                candidateIdx[best] = tmpI;
            }
        }

        // Greedy selection: accept only if a real valley separates the candidate from each already-accepted HVN
        int[] accepted = new int[maxNodes];
        int acceptedCount = 0;

        for (int c = 0; c < candidateCount && acceptedCount < maxNodes; c++) {
            int idx = candidateIdx[c];
            double vol = candidateVol[c];
            boolean ok = true;

            for (int k = 0; k < acceptedCount; k++) {
                int prevIdx = accepted[k];
                int lo = Math.min(idx, prevIdx) + 1;
                int hi = Math.max(idx, prevIdx);
                double minBetween = Double.MAX_VALUE;
                for (int m = lo; m < hi; m++) {
                    if (volumeBins[m] < minBetween)
                        minBetween = volumeBins[m];
                }
                // Valley must dip below 50% of the smaller peak to count as a real separation
                double smallerPeak = Math.min(vol, volumeBins[prevIdx]);
                if (minBetween >= smallerPeak * 0.5) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                accepted[acceptedCount] = idx;
                prevHVN[acceptedCount] = sessionLow + (idx + 0.5) * binSize;
                acceptedCount++;
            }
        }
    }

    // Finds LVNs: local volume minima below LvnThresholdPct%. Edge bins are excluded.
    // Results sorted by volume ascending (emptiest first), stored in prevLVN[0..4].
    protected void findLVNs(int pocIndex, double binSize, double sessionLow, int numBins) {
        for (int n = 0; n < 5; n++)
            prevLVN[n] = 0;

        double maxVol = 0;
        for (int j = 0; j < numBins; j++) {
            if (volumeBins[j] > maxVol)
                maxVol = volumeBins[j];
        }
        if (maxVol <= 0)
            return;

        double threshold = maxVol * cfgLvnThresholdPct() / 100.0;
        int maxNodes = Math.min(cfgHvnCount(), 5); // same count limit as HVNs

        // Collect local minima below threshold (skip first and last bin)
        int[] candidateIdx = new int[numBins];
        double[] candidateVol = new double[numBins];
        int candidateCount = 0;

        for (int j = 1; j < numBins - 1; j++) {
            if (volumeBins[j] > threshold)
                continue;

            // Local minimum: lower than both neighbours
            if (volumeBins[j] < volumeBins[j - 1] && volumeBins[j] < volumeBins[j + 1]) {
                candidateIdx[candidateCount] = j;
                candidateVol[candidateCount] = volumeBins[j];
                candidateCount++;
            }
        }

        // Sort by volume ascending so the emptiest bins come first
        for (int a = 0; a < candidateCount - 1; a++) {
            int best = a;
            for (int b = a + 1; b < candidateCount; b++) {
                if (candidateVol[b] < candidateVol[best])
                    best = b;
            }
            if (best != a) {
                double tmpV = candidateVol[a];
                candidateVol[a] = candidateVol[best];
                candidateVol[best] = tmpV;
                int tmpI = candidateIdx[a];
                candidateIdx[a] = candidateIdx[best];
                candidateIdx[best] = tmpI;
            }
        }

        int count = Math.min(maxNodes, candidateCount);
        for (int n = 0; n < count; n++) {
            prevLVN[n] = sessionLow + (candidateIdx[n] + 0.5) * binSize;
        }
    }

    // Condensed export methods (simplified for length, logic preserved)
    protected void exportVolumeProfilePNG_Dense(
            long prevStart,
            long prevEnd,
            double sessionLow,
            double binSize,
            double totalVolume,
            int pocIndex) {
        String folder = resolveExportFolder();
        new File(folder).mkdirs();

        String t1 = formatForFilename(prevStart);
        String t2 = formatForFilename(prevEnd);

        String symbolName = "";
        try {
            symbolName = cfgChart().Symbol;
        } catch (Exception e) {
        }
        String[] SESSION_NAMES = { "", "Previous Day", "Previous Week", "Previous Month", "Previous Year",
                "Actual Day", "Actual Week", "Actual Month", "Actual Year", "Previous Swing", "Actual Swing" };
        String sessionLabel = (cfgSessionLabel() != null) ? cfgSessionLabel()
                : (cfgSessionType() >= 1 && cfgSessionType() < SESSION_NAMES.length)
                        ? SESSION_NAMES[cfgSessionType()]
                        : "Session";
        String tfName = "";
        try {
            tfName = cfgChart().Timeframe;
        } catch (Exception e) {
        }
        String binLabel = (cfgBinSizeMode() == 2) ? "Fixed Tick" : "Range";
        String titleExtra = (tfName.isEmpty() ? "" : " | TF:" + tfName)
                + " | Bins:" + binLabel
                + (cfgEnableVCP() ? " | VolumeCluster" : "");
        String fileSymbol = symbolName.isEmpty() ? "" : symbolName.replaceAll("[^A-Za-z0-9_-]", "") + "_";
        String fileSession = sessionLabel.replaceAll("\\s+", "");
        String filePath = folder + File.separator
                + "VolumeProfile_" + fileSymbol + fileSession + "_" + t1 + "_" + t2 + ".png";

        final int width = 1600;
        final int height = 1000;
        final int marginLeft = 220;
        final int marginRight = 40;
        final int marginTop = 70;
        final int marginBottom = 90;
        final int plotW = width - marginLeft - marginRight;
        final int plotH = height - marginTop - marginBottom;

        double maxVol = 0.0;
        for (int i = 0; i < lastNumBins; i++) {
            double v = (cfgEnableVCP() && clusterBins != null && clusterBins.length >= lastNumBins) ? clusterBins[i]
                    : volumeBins[i];
            if (v > maxVol)
                maxVol = v;
        }
        if (maxVol <= 0)
            return;

        int valIndex = (binSize > 0) ? (int) Math.floor((prevVAL - sessionLow) / binSize) : 0;
        int vahIndex = (binSize > 0) ? (int) Math.floor((prevVAH - sessionLow) / binSize) : (lastNumBins - 1);
        valIndex = clampInt(valIndex, 0, lastNumBins - 1);
        vahIndex = clampInt(vahIndex, 0, lastNumBins - 1);
        int vaLow = Math.min(valIndex, vahIndex);
        int vaHigh = Math.max(valIndex, vahIndex);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            SimpleDateFormat dtFmt = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US);
            String titleText = "StrategyQuantX - Volume Profile"
                    + (symbolName.isEmpty() ? "" : " - " + symbolName)
                    + " | " + sessionLabel + titleExtra + "  "
                    + dtFmt.format(new Date(prevStart)) + " -> " + dtFmt.format(new Date(prevEnd));
            g.drawString(titleText, marginLeft, 30);

            // StrategyQuantX branding (top-right): logo + web address
            int logoSize = 30;
            int logoX = width - marginRight - logoSize;
            int logoY = 6;
            g.setColor(new Color(0x41, 0x81, 0xED));
            g.fillRoundRect(logoX, logoY, logoSize, logoSize, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            FontMetrics fmQ = g.getFontMetrics();
            int qW = fmQ.stringWidth("Q");
            g.drawString("Q", logoX + (logoSize - qW) / 2, logoY + logoSize - 8);
            g.setColor(new Color(0x41, 0x81, 0xED));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fmUrl = g.getFontMetrics();
            String urlText = "strategyquant.com";
            int urlW = fmUrl.stringWidth(urlText);
            g.drawString(urlText, logoX - 8 - urlW, logoY + logoSize / 2 + 5);

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString(String.format(Locale.US,
                    "VOL: POC=%.6f  VAH=%.6f  VAL=%.6f  TotalVol=%.0f", prevPOC,
                    prevVAH, prevVAL, totalVolume), marginLeft, 52);

            g.setColor(new Color(220, 220, 220));
            g.drawRect(marginLeft, marginTop, plotW, plotH);

            double rowH = (double) plotH / lastNumBins;
            int barH = Math.max(1, (int) Math.floor(rowH) - 1);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            FontMetrics fmPrice = g.getFontMetrics();

            int yLabelStep = yLabelStepForDenseAxis();

            for (int i = 0; i < lastNumBins; i++) {
                double vol = (cfgEnableVCP() && clusterBins != null && clusterBins.length >= lastNumBins)
                        ? clusterBins[i]
                        : volumeBins[i];
                int barW = (int) Math.round((vol / maxVol) * plotW);
                int y = (int) Math.round(marginTop + (lastNumBins - 1 - i) * rowH);

                if (i >= vaLow && i <= vaHigh) {
                    g.setColor(new Color(245, 250, 245));
                    g.fillRect(marginLeft, y, plotW, barH);
                }

                g.setColor(i == pocIndex ? new Color(255, 215, 0) : new Color(120, 120, 120));
                g.fillRect(marginLeft, y, barW, barH);

                if (i % yLabelStep == 0) {
                    String priceTxt = String.format(Locale.US, "%.6f", sessionLow + (i + 0.5) * binSize);
                    g.setColor(Color.BLACK);
                    g.drawString(priceTxt, marginLeft - 10 - fmPrice.stringWidth(priceTxt),
                            y + (barH / 2) + (fmPrice.getAscent() / 2) - 2);
                    g.setColor(new Color(180, 180, 180));
                    g.drawLine(marginLeft - 4, y + barH / 2, marginLeft, y + barH / 2);
                }
            }

            // Draw VAL/VAH/POC Lines
            drawLevelLine(g, marginTop, plotW, marginLeft, rowH, vaLow, prevVAL, new Color(220, 20, 60), "VAL");
            drawLevelLine(g, marginTop, plotW, marginLeft, rowH, vaHigh, prevVAH, new Color(34, 139, 34), "VAH");
            drawLevelLine(g, marginTop, plotW, marginLeft, rowH, pocIndex, prevPOC, new Color(255, 165, 0), "POC");

            // Draw HVN/LVN Lines
            for (int n = 0; n < 5; n++) {
                if (prevHVN[n] > 0 && binSize > 0) {
                    int hvnIdx = clampInt((int) ((prevHVN[n] - sessionLow) / binSize), 0, lastNumBins - 1);
                    drawLevelLine(g, marginTop, plotW, marginLeft, rowH, hvnIdx, prevHVN[n], new Color(128, 0, 128),
                            "HVN" + (n + 1));
                }
                if (prevLVN[n] > 0 && binSize > 0) {
                    int lvnIdx = clampInt((int) ((prevLVN[n] - sessionLow) / binSize), 0, lastNumBins - 1);
                    drawLevelLine(g, marginTop, plotW, marginLeft, rowH, lvnIdx, prevLVN[n], new Color(0, 150, 136),
                            "LVN" + (n + 1));
                }
            }

            // Bottom axis
            int axisY = marginTop + plotH + 30;
            g.setColor(Color.BLACK);
            g.drawLine(marginLeft, axisY, marginLeft + plotW, axisY);
            for (int p : new int[] { 0, 25, 50, 75, 100 }) {
                int x = marginLeft + (int) (p / 100.0 * plotW);
                g.drawLine(x, axisY, x, axisY + 6);
                g.drawString(p + "%", x - 10, axisY + 22);
            }

            ImageIO.write(img, "png", new File(filePath));
        } catch (Exception e) {
        } finally {
            g.dispose();
        }
    }

    protected void drawLevelLine(Graphics2D g, int marginTop, int plotW, int marginLeft, double rowH, int index,
            double price, Color c, String label) {
        int y = (int) Math.round(marginTop + (lastNumBins - 1 - index) * rowH);
        g.setColor(c);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(marginLeft, y, marginLeft + plotW, y);
        // label logic simplified
        g.drawString(label + " " + String.format(Locale.US, "%.6f", price), marginLeft + plotW - 150, y - 5);
    }

    // Exports the last DEFAULT_MAX_SESSIONS_FOR_SVG (50) sessions as an SVG chart file.
    // Called from OnDeinit() only when StoreChartData = true.
    // Has no effect on trading signals or backtest results — purely visual export.
    protected void exportMultiSessionSVG() {
        if (historyCount == 0)
            return;
        String folder = resolveExportFolder();
        new File(folder).mkdirs();

        // Instrument + session info (used in filename and title)
        String symbolName = "";
        try {
            symbolName = cfgChart().Symbol;
        } catch (Exception e) {
        }
        String[] SESSION_NAMES = { "", "Previous Day", "Previous Week", "Previous Month", "Previous Year",
                "Actual Day", "Actual Week", "Actual Month", "Actual Year", "Previous Swing", "Actual Swing" };
        String sessionLabel = (cfgSessionLabel() != null) ? cfgSessionLabel()
                : (cfgSessionType() >= 1 && cfgSessionType() < SESSION_NAMES.length)
                        ? SESSION_NAMES[cfgSessionType()]
                        : "Session";
        String tfName2 = "";
        try {
            tfName2 = cfgChart().Timeframe;
        } catch (Exception e) {
        }
        String binLabel2 = (cfgBinSizeMode() == 2) ? "Fixed Tick(" + cfgTicksPerBin() + ")"
                : "Range(" + cfgProfileRows() + ")";
        String titleExtra = (tfName2.isEmpty() ? "" : " | TF:" + tfName2)
                + " | Bins:" + binLabel2
                + (cfgEnableVCP() ? " | VolumeCluster" : "");
        String fileSymbol = symbolName.isEmpty() ? "" : symbolName.replaceAll("[^A-Za-z0-9_-]", "") + "_";
        String fileSession = sessionLabel.replaceAll("\\s+", "");
        String strategyNameForFile = (getStrategy() != null ? getStrategy().getStrategyName() : "");
        strategyNameForFile = strategyNameForFile == null ? "" : strategyNameForFile.replaceAll("[^A-Za-z0-9_\\-]", "_");
        String filePath = folder + File.separator
                + "VP_" + (strategyNameForFile.isEmpty() ? "" : strategyNameForFile + "_") + fileSymbol + fileSession + "_" + fileRandomSuffix + ".svg";
        saveChartPath(filePath);

        int N = historyCount;
        int startIdx = 0;
        int maxSess = DEFAULT_MAX_SESSIONS_FOR_SVG;
        if (N > maxSess) {
            startIdx = N - maxSess;
            N = maxSess;
        }
        int latest = startIdx + N - 1;
        String t1 = formatForFilename(histSessionStart[latest]);

        // Unified price scale across exported session window
        double globalHigh = Double.MIN_VALUE;
        double globalLow = Double.MAX_VALUE;
        for (int k = 0; k < N; k++) {
            globalHigh = Math.max(globalHigh, histSessionHigh[startIdx + k]);
            globalLow = Math.min(globalLow, histSessionLow[startIdx + k]);
        }
        if (globalHigh <= globalLow)
            return;
        double globalRange = globalHigh - globalLow;
        // Add 2% padding
        globalLow -= globalRange * 0.02;
        globalHigh += globalRange * 0.02;
        globalRange = globalHigh - globalLow;

        int marginLeft = 80;
        int marginRight = 70;
        int marginTop = 60;
        int marginBottom = 95;
        int volPanelH = cfgShowVolumeSubchart() ? 120 : 0; // height of volume bar subplot
        int volPanelGap = cfgShowVolumeSubchart() ? 10 : 0; // gap between price chart and volume panel
        int plotH = DEFAULT_CHART_HEIGHT - volPanelGap - volPanelH; // shrink main chart to fit volume panel
        int plotW = Math.max(800, N * DEFAULT_SESSION_WIDTH);
        int totalW = marginLeft + plotW + marginRight;
        int totalH = marginTop + plotH + volPanelGap + volPanelH + marginBottom;

        // Full time range from first to last exported session
        long tFirst = histSessionStart[startIdx];
        long tLast = histSessionEnd[startIdx + N - 1];
        long fullRange = tLast - tFirst;
        if (fullRange <= 0)
            return;

        // Pre-compute pixel start/end for each session based on bar positions
        // (bars are drawn evenly across plotW, so session boundaries align
        // with actual bar positions, not time-proportional positions)
        double[] sesStartPx = new double[N];
        double[] sesEndPx = new double[N];
        {
            // Collect all bar times in display order (oldest-first = left-to-right)
            java.util.List<long[]> allBars = new java.util.ArrayList<>();
            int bi = 0;
            try {
                while (true) {
                    long t = cfgChart().Time(bi);
                    if (t < tFirst)
                        break;
                    if (t >= tFirst && t < tLast)
                        allBars.add(new long[] { t });
                    bi++;
                }
            } catch (Exception e) {
            }
            java.util.Collections.reverse(allBars); // now oldest-first
            int totalBars = allBars.size();
            if (totalBars > 0) {
                double barStep = (double) plotW / totalBars;
                for (int k = 0; k < N; k++) {
                    int firstIdx = -1, lastIdx = -1;
                    for (int j = 0; j < totalBars; j++) {
                        long bt = allBars.get(j)[0];
                        if (bt >= histSessionStart[startIdx + k] && bt < histSessionEnd[startIdx + k]) {
                            if (firstIdx < 0)
                                firstIdx = j;
                            lastIdx = j;
                        }
                    }
                    if (firstIdx >= 0) {
                        sesStartPx[k] = marginLeft + firstIdx * barStep;
                        sesEndPx[k] = marginLeft + (lastIdx + 1) * barStep;
                    } else {
                        // fallback to time-proportional if no bars found
                        sesStartPx[k] = marginLeft + (double) (histSessionStart[startIdx + k] - tFirst) / fullRange * plotW;
                        sesEndPx[k] = marginLeft + (double) (histSessionEnd[startIdx + k] - tFirst) / fullRange * plotW;
                    }
                }
            } else {
                for (int k = 0; k < N; k++) {
                    sesStartPx[k] = marginLeft + (double) (histSessionStart[startIdx + k] - tFirst) / fullRange * plotW;
                    sesEndPx[k] = marginLeft + (double) (histSessionEnd[startIdx + k] - tFirst) / fullRange * plotW;
                }
            }
        }
        // segStartPx alias for downstream (same as sesStartPx in bar-based layout)
        double[] segStartPx = sesStartPx;

        double candleAlpha = Math.max(0.1, Math.min(1.0, DEFAULT_DARK_CANDLE_OPACITY / 100.0));
        double profileAlpha = Math.max(0.1, Math.min(1.0, DEFAULT_DARK_PROFILE_OPACITY / 100.0));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            w.write("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\""
                    + totalW
                    + "\" height=\"" + totalH + "\" viewBox=\"0 0 " + totalW + " " + totalH + "\">");
            w.write("<rect id=\"bgRect\" width=\"" + totalW + "\" height=\"" + totalH
                    + "\" fill=\"" + DEFAULT_DARK_BG_COLOR + "\"/>\n");

            // Title
            String strategyName = (getStrategy() != null ? getStrategy().getStrategyName() : "");
            String sessionsSuffix = " (last " + N + " sessions)";
            String title = "Volume Profile" + (symbolName.isEmpty() ? "" : " - " + symbolName)
                    + " | " + sessionLabel + titleExtra + sessionsSuffix;
            String titleWithStrategy = (strategyName.isEmpty() ? "" : svgEsc(strategyName) + " - ") + svgEsc(title);
            w.write("<text id=\"chartTitle\" class=\"thTitle\" x=\"" + marginLeft
                    + "\" y=\"35\" font-family=\"Sans-Serif\" font-size=\"16\" font-weight=\"700\" fill=\"#ffffff\">"
                    + "StrategyQuantX - " + titleWithStrategy + "</text>\n");

            // Plot background
            w.write("<rect id=\"plotRect\" x=\"" + marginLeft + "\" y=\"" + marginTop
                    + "\" width=\"" + plotW + "\" height=\"" + plotH
                    + "\" fill=\"" + DEFAULT_DARK_PLOT_COLOR + "\" rx=\"4\"/>\n");

            // Price axis labels + grid
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

            // Session boundary lines + date labels + volume labels (always visible)
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);

                // Vertical dashed session boundary
                w.write("<line class=\"thSesDiv\" x1=\"" + sesX + "\" y1=\"" + marginTop
                        + "\" x2=\"" + sesX + "\" y2=\"" + (marginTop + plotH)
                        + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n");
                // Date label at bottom
                String dateLabel = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US)
                        .format(new Date(histSessionStart[startIdx + k]));
                double labelX = (sesX + sesXEnd) / 2;
                w.write("<text class=\"thDate\" x=\"" + labelX + "\" y=\""
                        + (marginTop + plotH + volPanelGap + volPanelH + 45)
                        + "\" font-family=\"Sans-Serif\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">"
                        + svgEsc(dateLabel) + "</text>\n");

                // Volume labels

                double sesHighY = marginTop + plotH - (histSessionHigh[startIdx + k] - globalLow) / globalRange * plotH;
                double sesLowY = marginTop + plotH - (histSessionLow[startIdx + k] - globalLow) / globalRange * plotH;

                // Count how many label lines we need
                int lineCount = 4; // Vol, Bull, Bear, Delta always shown
                if (cfgShowPOCDelta())
                    lineCount++;
                if (cfgShowVADelta())
                    lineCount++;
                if (cfgShowProfileRange())
                    lineCount++;
                if (cfgShowPOCPosition())
                    lineCount += 2; // top% and bottom%

                int lineH = 11; // pixels between lines
                int labelTotalH = lineCount * lineH;
                double[] volYs = new double[lineCount];
                if (sesHighY - marginTop > labelTotalH + 5) {
                    for (int ln = 0; ln < lineCount; ln++)
                        volYs[ln] = sesHighY - (labelTotalH - ln * lineH) + 2;
                } else {
                    for (int ln = 0; ln < lineCount; ln++)
                        volYs[ln] = sesLowY + 14 + ln * lineH;
                }

                int li = 0;
                w.write("<g class=\"sStats\" data-shy=\"" + String.format(Locale.US, "%.2f", sesHighY) + "\">\n");
                if (histSessionLabel != null && histSessionLabel[startIdx + k] != null) {
                    w.write("<text class=\"thSesLbl\" x=\"" + labelX + "\" y=\"" + (volYs[0] - lineH)
                            + "\" font-family=\"Sans-Serif\" font-size=\"13\" font-weight=\"800\" fill=\"#ffcc00\" text-anchor=\"middle\">"
                            + svgEsc(histSessionLabel[startIdx + k]) + "</text>\n");
                }
                // line
                // index
                String volTotal = String.format(Locale.US, "%.0f", histTotalVolume[startIdx + k]);
                String volBull = String.format(Locale.US, "%.0f", histBullVolume[startIdx + k]);
                String volBear = String.format(Locale.US, "%.0f", histBearVolume[startIdx + k]);
                double totalDelta = histBullVolume[startIdx + k] - histBearVolume[startIdx + k];
                String volDelta = String.format(Locale.US, "%+.0f", totalDelta);
                String deltaColor = (totalDelta >= 0) ? "#00bcd4" : "#f44336";
                w.write("<text class=\"thVol\" id=\"rpVol_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Vol: "
                        + volTotal + "</text>\n");
                w.write("<text id=\"rpBull_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#4caf50\" text-anchor=\"middle\">Bull: "
                        + volBull + "</text>\n");
                w.write("<text id=\"rpBear_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#f44336\" text-anchor=\"middle\">Bear: "
                        + volBear + "</text>\n");
                w.write("<text id=\"rpDelta_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                        + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"" + deltaColor
                        + "\" text-anchor=\"middle\">Delta: "
                        + volDelta + "</text>\n");

                // POC delta vs previous session
                if (cfgShowPOCDelta()) {
                    String pocDeltaStr;
                    String pocDeltaClr;
                    if (k > 0) {
                        double pd = histPOC[startIdx + k] - histPOC[startIdx + k - 1];
                        pocDeltaStr = String.format(Locale.US, "%+.5f", pd);
                        pocDeltaClr = (pd >= 0) ? "#00bcd4" : "#f44336";
                    } else {
                        pocDeltaStr = "N/A";
                        pocDeltaClr = "#888";
                    }
                    w.write("<text id=\"rpDPOC_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                            + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"" + pocDeltaClr
                            + "\" text-anchor=\"middle\">\u0394POC: " + pocDeltaStr + "</text>\n");
                }

                // VA midpoint delta vs previous session
                if (cfgShowVADelta()) {
                    String vaDeltaStr;
                    String vaDeltaClr;
                    if (k > 0) {
                        double vaAvg = (histVAH[startIdx + k] + histVAL[startIdx + k]) / 2.0;
                        double vaAvgPrev = (histVAH[startIdx + k - 1] + histVAL[startIdx + k - 1]) / 2.0;
                        double vd = vaAvg - vaAvgPrev;
                        vaDeltaStr = String.format(Locale.US, "%+.5f", vd);
                        vaDeltaClr = (vd >= 0) ? "#00bcd4" : "#f44336";
                    } else {
                        vaDeltaStr = "N/A";
                        vaDeltaClr = "#888";
                    }
                    w.write("<text id=\"rpDVA_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                            + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"" + vaDeltaClr
                            + "\" text-anchor=\"middle\">\u0394VA: " + vaDeltaStr + "</text>\n");
                }

                // Profile range
                if (cfgShowProfileRange()) {
                    double range = histSessionHigh[startIdx + k] - histSessionLow[startIdx + k];
                    String rangeStr = String.format(Locale.US, "%.5f", range);
                    w.write("<text class=\"thStat\" id=\"rpRange_" + k + "\" x=\"" + labelX + "\" y=\"" + volYs[li++]
                            + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Range: "
                            + rangeStr + "</text>\n");
                }

                // POC position from top and bottom
                if (cfgShowPOCPosition()) {
                    double range = histSessionHigh[startIdx + k] - histSessionLow[startIdx + k];
                    if (range > 0) {
                        double pctTop = (histSessionHigh[startIdx + k] - histPOC[startIdx + k]) / range * 100.0;
                        double pctBot = (histPOC[startIdx + k] - histSessionLow[startIdx + k]) / range * 100.0;
                        w.write("<text class=\"thStat\" id=\"rpPOCUp_" + k + "\" x=\"" + labelX + "\" y=\""
                                + volYs[li++]
                                + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#aaa\" text-anchor=\"middle\">POC\u2191: "
                                + String.format(Locale.US, "%.1f%%", pctTop) + "</text>\n");
                        w.write("<text class=\"thStat\" id=\"rpPOCDn_" + k + "\" x=\"" + labelX + "\" y=\""
                                + volYs[li++]
                                + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#aaa\" text-anchor=\"middle\">POC\u2193: "
                                + String.format(Locale.US, "%.1f%%", pctBot) + "</text>\n");
                    } else {
                        w.write("<text class=\"thStat\" id=\"rpPOCUp_" + k + "\" x=\"" + labelX + "\" y=\""
                                + volYs[li++]
                                + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#888\" text-anchor=\"middle\">POC\u2191: N/A</text>\n");
                        w.write("<text class=\"thStat\" id=\"rpPOCDn_" + k + "\" x=\"" + labelX + "\" y=\""
                                + volYs[li++]
                                + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#888\" text-anchor=\"middle\">POC\u2193: N/A</text>\n");
                    }
                }
                w.write("</g>\n"); // close sStats
            }

            // Replay mode: actual sessions get per-bar replay controls
            boolean isReplayMode = (cfgSessionType() >= 5 && cfgSessionType() != 9);

            // --- Auto-scale wrapper: all price-dependent SVG goes inside chartG ---
            w.write("<defs><clipPath id=\"plotClip\"><rect x=\"" + marginLeft + "\" y=\"" + marginTop + "\" width=\""
                    + plotW + "\" height=\"" + plotH + "\"/></clipPath></defs>\n");
            w.write("<g id=\"chartG\" clip-path=\"url(#plotClip)\">\n");

            // Candles group - always drawn unified for the full tFirst-tLast range
            w.write("<g id=\"candlesG\" opacity=\"" + String.format(Locale.US, "%.2f", candleAlpha) + "\">\n");
            if (cfgShowCandlesticks()) {
                drawSvgCandlesUnified(w, marginLeft, plotW, marginTop, plotH,
                        globalLow, globalRange, tFirst, tLast, fullRange, 1.0);
            }
            w.write("</g>\n");

            // Profile bars group (opacity-controlled for theme switching)
            w.write("<g id=\"profileG\" opacity=\"" + String.format(Locale.US, "%.2f", profileAlpha) + "\">\n");
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);

                // Wrap session in replay group for JS manipulation
                if (isReplayMode) {
                    w.write("<g id=\"rpProfile_" + k + "\">\n");
                }

                int sesNB = histNumBins[startIdx + k];
                double sesRange = histSessionHigh[startIdx + k] - histSessionLow[startIdx + k];
                double sesBinSize = (sesNB > 0 && sesRange > 0) ? sesRange / sesNB : 1;
                double[] sesBins = histVolumeBins[startIdx + k];

                double maxVol = 0;
                int pocIdx = 0;
                for (int j = 0; j < sesNB; j++) {
                    if (sesBins[j] > maxVol) {
                        maxVol = sesBins[j];
                        pocIdx = j;
                    }
                }
                if (maxVol <= 0) {
                    if (isReplayMode)
                        w.write("</g>\n");
                    continue;
                }

                // Draw histogram bars (stacked bull/bear)
                for (int j = 0; j < sesNB; j++) {
                    if (sesBins[j] <= 0)
                        continue;
                    double binLowPrice = histSessionLow[startIdx + k] + j * sesBinSize;
                    double binHighPrice = binLowPrice + sesBinSize;
                    double y1 = marginTop + plotH - (binHighPrice - globalLow) / globalRange * plotH;
                    double y2 = marginTop + plotH - (binLowPrice - globalLow) / globalRange * plotH;
                    double barH = Math.max(0.5, y2 - y1 - 0.3);
                    double barW = (sesBins[j] / maxVol) * sesPixW * 0.8;

                    double bullVol = histBullBins[k][j];
                    double bearVol = histBearBins[k][j];
                    double totalBin = bullVol + bearVol;
                    if (totalBin <= 0)
                        totalBin = sesBins[j];

                    double bullW = barW * (bullVol / totalBin);
                    double bearW = barW - bullW;

                    double binMid = binLowPrice + sesBinSize / 2;
                    boolean isPOC = (j == pocIdx);
                    boolean isVA = (binMid >= histVAL[startIdx + k] && binMid <= histVAH[startIdx + k]);

                    // Bull portion (green tones)
                    if (bullW > 0) {
                        String bullFill;
                        if (isPOC)
                            bullFill = "rgba(255,215,0,0.85)";
                        else if (isVA)
                            bullFill = "rgba(76,175,80,0.85)";
                        else
                            bullFill = "rgba(76,175,80,0.55)";
                        w.write("<rect x=\"" + sesX + "\" y=\"" + y1
                                + "\" width=\"" + bullW + "\" height=\"" + barH
                                + "\" fill=\"" + bullFill + "\"/>\n");
                    }
                    // Bear portion (red tones)
                    if (bearW > 0) {
                        String bearFill;
                        if (isPOC)
                            bearFill = "rgba(255,215,0,0.85)";
                        else if (isVA)
                            bearFill = "rgba(239,83,80,0.85)";
                        else
                            bearFill = "rgba(239,83,80,0.55)";
                        w.write("<rect x=\"" + (sesX + bullW) + "\" y=\"" + y1
                                + "\" width=\"" + bearW + "\" height=\"" + barH
                                + "\" fill=\"" + bearFill + "\"/>");
                    }

                    // Per-level delta label (left side of profile bar)
                    if (barH >= 2) {
                        double binDelta = bullVol - bearVol;
                        String dTxt = String.format(Locale.US, "%+.0f", binDelta);
                        String dClr = (binDelta >= 0) ? "#4caf50" : "#f44336";
                        double dY = y1 + barH / 2 + 3;
                        w.write("<text x=\"" + (sesX - 3) + "\" y=\"" + dY
                                + "\" font-family=\"Monospace\" font-size=\"" + DEFAULT_DELTA_FONT_SIZE
                                + "\" font-weight=\"700\" fill=\"" + dClr
                                + "\" text-anchor=\"end\">" + dTxt + "</text>\n");
                    }
                }
                // Close replay group for this session
                if (isReplayMode) {
                    w.write("</g>\n");
                }
            }
            w.write("</g>\n");

            // Level lines (always visible, not opacity-controlled)
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);
                int sesLevelW = (int) sesPixW;
                drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                        histPOC[startIdx + k], "#ffa500", "POC");
                drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                        histVAH[startIdx + k], "#4caf50", "VAH");
                drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                        histVAL[startIdx + k], "#f44336", "VAL");
                // IB range box
                if (histIBH[startIdx + k] > 0 && histIBL[startIdx + k] > 0) {
                    double ibhY = marginTop + plotH - (histIBH[startIdx + k] - globalLow) / globalRange * plotH;
                    double iblY = marginTop + plotH - (histIBL[startIdx + k] - globalLow) / globalRange * plotH;
                    double ibTop = Math.min(ibhY, iblY);
                    double ibHeight = Math.abs(iblY - ibhY);
                    w.write("<rect x=\"" + (int) sesX + "\" y=\"" + ibTop
                            + "\" width=\"" + sesLevelW + "\" height=\"" + ibHeight
                            + "\" fill=\"#ff9800\" fill-opacity=\"0.13\" stroke=\"none\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                if (histIBH[startIdx + k] > 0)
                    drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                            histIBH[startIdx + k], "#00bcd4", "IBH");
                if (histIBL[startIdx + k] > 0)
                    drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                            histIBL[startIdx + k], "#e040fb", "IBL");
                // HVN/LVN level lines
                for (int n = 0; n < 5; n++) {
                    if (histHVN[startIdx + k][n] > 0)
                        drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                                histHVN[startIdx + k][n], "#9c27b0", "HVN" + (n + 1));
                    if (histLVN[startIdx + k][n] > 0)
                        drawSvgLevel(w, (int) sesX, sesLevelW, marginTop, plotH, globalLow, globalRange,
                                histLVN[startIdx + k][n], "#009688", "LVN" + (n + 1));
                }
            }

            // Zigzag overlay line (only for ZigZag sessions)
            boolean isZigZag = (cfgSessionType() == 9 || cfgSessionType() == 10);
            if (isZigZag && cfgShowZigZagLine() && N > 1) {
                w.write("<g id=\"zigzagG\" opacity=\"0.85\">\n");
                // Debug: output pivot values as SVG comment
                StringBuilder dbg = new StringBuilder("<!-- ZZ pivots: ");
                for (int k = 0; k < N; k++) {
                    dbg.append(String.format(Locale.US, "[%d]=%.5f/%d ", k, histPivotPrice[startIdx + k], histPivotDir[startIdx + k]));
                }
                dbg.append(" -->\n");
                w.write(dbg.toString());
                StringBuilder polyPoints = new StringBuilder();
                for (int k = 0; k < N; k++) {
                    double pivotP = histPivotPrice[startIdx + k];
                    // Fallback: use session high/low if pivot not recorded
                    if (pivotP <= 0) {
                        pivotP = (k % 2 == 0) ? histSessionHigh[startIdx + k] : histSessionLow[startIdx + k];
                    }
                    if (pivotP <= 0)
                        continue;
                    double px = sesEndPx[k];
                    double py = marginTop + plotH - (pivotP - globalLow) / globalRange * plotH;
                    if (polyPoints.length() > 0)
                        polyPoints.append(" ");
                    polyPoints.append(String.format(Locale.US, "%.1f,%.1f", px, py));
                    // (pivot markers removed – circles distort into ovals under chartG zoom
                    // scaling)
                }
                if (polyPoints.length() > 0) {
                    w.write("<polyline points=\"" + polyPoints.toString()
                            + "\" fill=\"none\" stroke=\"#00e5ff\" stroke-width=\"1\" stroke-linejoin=\"round\" vector-effect=\"non-scaling-stroke\"/>\n");
                }
                w.write("</g>\n");
            }

            // Close auto-scale wrapper
            w.write("</g>\n"); // close chartG

            // === Per-level delta labels (outside chartG to avoid zoom stretching) ===
            if (cfgShowDeltaPerLevel()) {
                w.write("<g id=\"deltaG\" clip-path=\"url(#plotClip)\">\n");
                for (int k = 0; k < N; k++) {
                    double sesX = sesStartPx[k];
                    double sesXEnd = sesEndPx[k];
                    int sesNB = histNumBins[startIdx + k];
                    double sesRange = histSessionHigh[startIdx + k] - histSessionLow[startIdx + k];
                    double sesBinSize = (sesNB > 0 && sesRange > 0) ? sesRange / sesNB : 1;
                    double[] sesBins = histVolumeBins[startIdx + k];
                    double maxVol = 0;
                    for (int j = 0; j < sesNB; j++) {
                        if (sesBins[j] > maxVol)
                            maxVol = sesBins[j];
                    }
                    if (maxVol <= 0)
                        continue;
                    w.write("<g class=\"dSes\" data-x1=\"" + String.format(Locale.US, "%.1f", sesX)
                            + "\" data-x2=\"" + String.format(Locale.US, "%.1f", sesXEnd) + "\">\n");
                    for (int j = 0; j < sesNB; j++) {
                        if (sesBins[j] <= 0)
                            continue;
                        double bullVol = histBullBins[k][j];
                        double bearVol = histBearBins[k][j];
                        double binDelta = bullVol - bearVol;
                        double binMidPrice = histSessionLow[startIdx + k] + j * sesBinSize + sesBinSize / 2;
                        // Compute bar height using session's own range (not globalRange)
                        // so the filter works correctly when autoscale zooms to this session
                        double barH = (sesRange > 0) ? plotH * sesBinSize / sesRange : 0;
                        if (barH < 2)
                            continue;
                        String dTxt = String.format(Locale.US, "%+.0f", binDelta);
                        String dClr = (binDelta >= 0) ? "#4caf50" : "#f44336";
                        double dY = marginTop + plotH - (binMidPrice - globalLow) / globalRange * plotH;
                        w.write("<text x=\"" + (sesX - 3) + "\" y=\"" + dY
                                + "\" data-price=\"" + String.format(Locale.US, "%.10f", binMidPrice)
                                + "\" font-family=\"Monospace\" font-size=\"" + DEFAULT_DELTA_FONT_SIZE
                                + "\" font-weight=\"700\" fill=\"" + dClr
                                + "\" text-anchor=\"end\">" + dTxt + "</text>\n");
                    }
                    w.write("</g>\n");
                }
                w.write("</g>\n");
            }

            // Y-axis group (updated by autoscale JS)
            w.write("<g id=\"yAxisBg\"><rect x=\"0\" y=\"0\" width=\"" + marginLeft + "\" height=\"" + totalH
                    + "\" fill=\"" + DEFAULT_DARK_BG_COLOR + "\"/></g>\n");
            w.write("<g id=\"yAxisG\"></g>\n");

            // === Volume bar subplot (below price chart) ===
            if (cfgShowVolumeSubchart()) {
                int volTop = marginTop + plotH + volPanelGap;
                // Volume panel background
                w.write("<rect id=\"volSubBg\" x=\"" + marginLeft + "\" y=\"" + volTop
                        + "\" width=\"" + plotW + "\" height=\"" + volPanelH
                        + "\" fill=\"" + DEFAULT_DARK_PLOT_COLOR + "\" rx=\"4\" opacity=\"0.6\"/>");
                // Separator line
                w.write("<line class=\"thVolSep\" x1=\"" + marginLeft + "\" y1=\"" + volTop
                        + "\" x2=\"" + (marginLeft + plotW) + "\" y2=\"" + volTop
                        + "\" stroke=\"#555\" stroke-width=\"0.8\"/>");
                // "Volume" label
                w.write("<text class=\"thVolLbl\" x=\"" + (marginLeft + 5) + "\" y=\"" + (volTop + 12)
                        + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#888\" font-weight=\"700\">Volume</text>\n");

                // Draw volume bars across full time range (continuous)
                {
                    int bCount = 0;
                    double maxBarVol = 0;
                    java.util.List<Double> volList = new java.util.ArrayList<>();
                    java.util.List<Boolean> bullList = new java.util.ArrayList<>();
                    int bi = 0;
                    try {
                        while (true) {
                            long t = cfgChart().Time(bi);
                            if (t < tFirst)
                                break;
                            if (t >= tFirst && t < tLast) {
                                double v = cfgChart().Volume(bi);
                                volList.add(v);
                                bullList.add(cfgChart().Close(bi) >= cfgChart().Open(bi));
                                bCount++;
                                if (v > maxBarVol)
                                    maxBarVol = v;
                            }
                            bi++;
                        }
                    } catch (Exception e2) {
                    }

                    if (bCount > 0 && maxBarVol > 0) {
                        double vStep = (double) plotW / bCount;
                        double vBarW = Math.max(1.0, vStep * 0.7);
                        // Linear scaling for true proportional bar heights
                        int usableH = volPanelH - 16;

                        // Draw volume bars
                        for (int b = 0; b < bCount; b++) {
                            double vol = volList.get(b);
                            boolean bull = bullList.get(b);
                            double vBarH = (vol / maxBarVol) * usableH;
                            double vx = marginLeft + (bCount - 1 - b) * vStep;
                            double vy = volTop + volPanelH - vBarH;
                            String vClr = bull ? "#26a69a" : "#ef5350";
                            w.write("<rect x=\"" + String.format(Locale.US, "%.1f", vx)
                                    + "\" y=\"" + String.format(Locale.US, "%.1f", vy)
                                    + "\" width=\"" + String.format(Locale.US, "%.1f", vBarW)
                                    + "\" height=\"" + String.format(Locale.US, "%.1f", Math.max(0.5, vBarH))
                                    + "\" fill=\"" + vClr + "\" opacity=\"0.7\"/>");
                        }

                        // Moving average SMA overlay
                        int maPeriod = Math.min(cfgVolumeMALength(), bCount);
                        if (maPeriod >= 2) {
                            StringBuilder maPath = new StringBuilder();
                            boolean first = true;
                            for (int b = 0; b < bCount; b++) {
                                int start = Math.max(0, b - maPeriod / 2);
                                int end = Math.min(bCount, start + maPeriod);
                                start = Math.max(0, end - maPeriod);
                                double sum = 0;
                                for (int m = start; m < end; m++)
                                    sum += volList.get(m);
                                double maVal = sum / (end - start);
                                double maH = (maVal / maxBarVol) * usableH;
                                double mx2 = marginLeft + (bCount - 1 - b) * vStep + vBarW / 2;
                                double my = volTop + volPanelH - maH;
                                if (first) {
                                    maPath.append(String.format(Locale.US, "M%.1f,%.1f", mx2, my));
                                    first = false;
                                } else {
                                    maPath.append(String.format(Locale.US, " L%.1f,%.1f", mx2, my));
                                }
                            }
                            w.write("<path d=\"" + maPath.toString()
                                    + "\" fill=\"none\" stroke=\"#ffab40\" stroke-width=\"1.2\" opacity=\"0.9\"/>\n");
                        }
                    }

                    // Session boundary lines in volume panel
                    for (int k = 0; k < N; k++) {
                        double volSesBoundX = sesStartPx[k];
                        w.write("<line class=\"thVolSep\" x1=\"" + String.format(Locale.US, "%.1f", volSesBoundX)
                                + "\" y1=\"" + volTop
                                + "\" x2=\"" + String.format(Locale.US, "%.1f", volSesBoundX) + "\" y2=\""
                                + (volTop + volPanelH)
                                + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n");

                    }
                }
            } // end cfgShowVolumeSubchart()

            // Time axis: bar times across full range
            boolean isMultiDay = (cfgSessionType() == 2 || cfgSessionType() == 3 || cfgSessionType() == 4
                    || cfgSessionType() == 6 || cfgSessionType() == 7 || cfgSessionType() == 8
                    || cfgSessionType() == 9 || cfgSessionType() == 10);
            String timeFmt = isMultiDay ? "dd.MM" : "HH:mm";
            drawSvgTimeAxis(w, marginLeft, plotW,
                    marginTop, plotH + volPanelGap + volPanelH, tFirst, tLast, timeFmt);

            // Collect bar data for crosshair tooltip
            StringBuilder barDataJs = new StringBuilder();
            barDataJs.append("var barData=[");
            boolean firstBar = true;
            for (int k = 0; k < N; k++) {
                double sesX = sesStartPx[k];
                double sesXEnd = sesEndPx[k];
                double sesPixW = Math.max(10, sesXEnd - sesX);
                // Count bars in this session
                int bCount = 0;
                int bi = 0;
                try {
                    while (true) {
                        long t = cfgChart().Time(bi);
                        if (t < histSessionStart[startIdx + k])
                            break;
                        if (t >= histSessionStart[startIdx + k] && t < histSessionEnd[startIdx + k])
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
                        if (t < histSessionStart[startIdx + k])
                            break;
                        if (t >= histSessionStart[startIdx + k] && t < histSessionEnd[startIdx + k]) {
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
                replayJs.append("var rpIsCluster=" + (cfgEnableVCP() ? "true" : "false")
                        + ",rpClusterSigma=" + String.format(Locale.US, "%.2f", cfgClusterSpread())
                        + ",rpMaxCenters=" + cfgMaxClusterCenters() + ";\n");
                replayJs.append("var rpShowDelta=" + (cfgShowDeltaPerLevel() ? "true" : "false")
                        + ",rpDeltaFs=" + DEFAULT_DELTA_FONT_SIZE + ";\n");
                replayJs.append("var rpSessions=[");

                for (int rk = 0; rk < N; rk++) {
                    double rSesX = sesStartPx[rk];
                    double rSesXEnd = sesEndPx[rk];
                    double rSesPixW = Math.max(10, rSesXEnd - rSesX);
                    int rNB = histNumBins[startIdx + rk];
                    double rSesLow = histSessionLow[startIdx + rk];
                    double rSesRange = histSessionHigh[startIdx + rk] - rSesLow;
                    double rBinSize = (rNB > 0 && rSesRange > 0) ? rSesRange / rNB : 1;

                    // Count bars in this session
                    int rBarCount = 0;
                    int ri = 0;
                    try {
                        while (true) {
                            long t = cfgChart().Time(ri);
                            if (t < histSessionStart[startIdx + rk])
                                break;
                            if (t >= histSessionStart[startIdx + rk] && t < histSessionEnd[startIdx + rk])
                                rBarCount++;
                            ri++;
                        }
                    } catch (Exception e2) {
                    }

                    // Collect per-bar bin contributions (oldest first)
                    double[][] barBins = new double[rBarCount][rNB];
                    boolean[] barBull = new boolean[rBarCount];
                    ri = 0;
                    int rbi = 0;
                    try {
                        while (true) {
                            long t = cfgChart().Time(ri);
                            if (t < histSessionStart[startIdx + rk])
                                break;
                            if (t >= histSessionStart[startIdx + rk] && t < histSessionEnd[startIdx + rk]) {
                                int chronoIdx = rBarCount - 1 - rbi;
                                double o2 = cfgChart().Open(ri), h2 = cfgChart().High(ri), l2 = cfgChart().Low(ri),
                                        c2 = cfgChart().Close(ri);
                                double v2 = cfgChart().Volume(ri);
                                barBull[chronoIdx] = (c2 >= o2);
                                int bHi = Math.max(0, Math.min(rNB - 1, (int) ((h2 - rSesLow) / rBinSize)));
                                int bLo = Math.max(0, Math.min(rNB - 1, (int) ((l2 - rSesLow) / rBinSize)));
                                double perBin = v2 / Math.max(1, bHi - bLo + 1);
                                for (int b = bLo; b <= bHi; b++) {
                                    barBins[chronoIdx][b] = perBin;
                                }
                                rbi++;
                            }
                            ri++;
                        }
                    } catch (Exception e2) {
                    }

                    // Emit session object
                    if (rk > 0)
                        replayJs.append(",");
                    replayJs.append("{nb:" + rNB
                            + ",low:" + String.format(Locale.US, "%.10f", rSesLow)
                            + ",bs:" + String.format(Locale.US, "%.10f", rBinSize)
                            + ",x:" + String.format(Locale.US, "%.1f", rSesX)
                            + ",pw:" + String.format(Locale.US, "%.1f", rSesPixW)
                            + ",n:" + rBarCount
                            + ",bars:[");
                    for (int b = 0; b < rBarCount; b++) {
                        if (b > 0)
                            replayJs.append(",");
                        replayJs.append("{b:" + (barBull[b] ? "1" : "0") + ",d:{");
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

                // Replay toolbar SVG elements - inline with drawing toolbar
                int drawBtnW = 26, drawGap = 3, drawTotalBtns = 15;
                int drawTbW = drawTotalBtns * (drawBtnW + drawGap) + drawGap;
                int annoX2 = marginLeft + drawTbW + 8;
                int tbY = marginTop + plotH + volPanelGap + volPanelH + 60;
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
                        + "\" rx=\"6\" fill=\"rgba(20,20,35,0.85)\" stroke=\"#444\" stroke-width=\"0.5\"/>\n");

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
                    w.write("<rect id=\"" + btnIds[b] + "\" x=\"" + bx + "\" y=\"" + tbY
                            + "\" width=\"" + btnW + "\" height=\"" + btnH + "\" " + btnStyle + ">"
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
                w.write("<line x1=\"" + slX + "\" y1=\"" + slY + "\" x2=\"" + (slX + sliderW) + "\" y2=\"" + slY
                        + "\" stroke=\"#555\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n");
                w.write("<line id=\"rpSliderFill\" x1=\"" + slX + "\" y1=\"" + slY + "\" x2=\"" + (slX + sliderW)
                        + "\" y2=\"" + slY + "\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n");
                w.write("<circle id=\"rpSliderKnob\" cx=\"" + (slX + sliderW) + "\" cy=\"" + slY
                        + "\" r=\"6\" fill=\"#4fc3f7\" stroke=\"#fff\" stroke-width=\"1\" cursor=\"pointer\"/>\n");
                w.write("<rect id=\"rpSliderHit\" x=\"" + (slX - 5) + "\" y=\"" + (slY - 10) + "\" width=\""
                        + (sliderW + 10) + "\" height=\"20\" fill=\"transparent\" cursor=\"pointer\"/>\n");

                // Counter text
                int ctX = slX + sliderW + 15;
                w.write("<text id=\"rpCounter\" x=\"" + ctX + "\" y=\"" + (tbY + btnH / 2) + "\" "
                        + "font-family=\"Monospace\" font-size=\"11\" fill=\"#aaa\" dominant-baseline=\"central\">"
                        + "0 / 0</text>\n");

                // Session label
                int sesLblX = ctX + 80;
                w.write("<text id=\"rpSesLabel\" x=\"" + sesLblX + "\" y=\"" + (tbY + btnH / 2) + "\" "
                        + "font-family=\"Monospace\" font-size=\"11\" fill=\"#7ab5ff\" dominant-baseline=\"central\">"
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

                // Session highlight rectangle (initially hidden)
                w.write("<rect id=\"rpSesHilight\" x=\"0\" y=\"" + marginTop + "\" width=\"100\" height=\"" + plotH
                        + "\" fill=\"none\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-dasharray=\"6,3\" opacity=\"0.6\" style=\"display:none\"/>\n");

                // Store toolbar geometry for JS
                barDataJs.append("var rpSlX=" + slX + ",rpSlW=" + sliderW + ",rpTbX=" + tbX + ";\n");

                // Emit per-session X start and end positions for JS
                barDataJs.append("var rpSesXArr=[");
                for (int rk = 0; rk < N; rk++) {
                    if (rk > 0)
                        barDataJs.append(",");
                    barDataJs.append(String.format(Locale.US, "%.1f", sesStartPx[rk]));
                }
                barDataJs.append("];\n");
                barDataJs.append("var rpSesXEnd=[");
                for (int rk = 0; rk < N; rk++) {
                    if (rk > 0)
                        barDataJs.append(",");
                    barDataJs.append(String.format(Locale.US, "%.1f", sesEndPx[rk]));
                }
                barDataJs.append("];\n");

                // Emit original KPI values per session for restore
                barDataJs.append("var rpOrigKpi=[");
                for (int rk = 0; rk < N; rk++) {
                    if (rk > 0)
                        barDataJs.append(",");
                    barDataJs.append("{v:" + String.format(Locale.US, "%.0f", histTotalVolume[startIdx + rk])
                            + ",b:" + String.format(Locale.US, "%.0f", histBullVolume[startIdx + rk])
                            + ",r:" + String.format(Locale.US, "%.0f", histBearVolume[startIdx + rk])
                            + ",d:" + String.format(Locale.US, "%+.0f", histBullVolume[startIdx + rk] - histBearVolume[startIdx + rk])
                            + ",poc:" + String.format(Locale.US, "%.6f", histPOC[startIdx + rk])
                            + ",vah:" + String.format(Locale.US, "%.6f", histVAH[startIdx + rk])
                            + ",val:" + String.format(Locale.US, "%.6f", histVAL[startIdx + rk])
                            + ",sh:" + String.format(Locale.US, "%.6f", histSessionHigh[startIdx + rk])
                            + ",sl:" + String.format(Locale.US, "%.6f", histSessionLow[startIdx + rk])
                            + "}");
                }
                barDataJs.append("];\n");
            }

            // Crosshair SVG elements (initially hidden)
            w.write("<g id=\"crosshairG\" style=\"display:none;pointer-events:none\">\n");
            w.write("<line id=\"chV\" x1=\"0\" y1=\"" + marginTop + "\" x2=\"0\" y2=\"" + (marginTop + plotH)
                    + "\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n");
            w.write("<line id=\"chH\" x1=\"" + marginLeft + "\" y1=\"0\" x2=\"" + (marginLeft + plotW)
                    + "\" y2=\"0\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n");
            // Price label on right axis
            w.write("<g id=\"chPriceG\">");
            w.write("<rect id=\"chPriceBg\" x=\"" + (marginLeft + plotW + 2)
                    + "\" y=\"0\" width=\"60\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>");
            w.write("<text id=\"chPriceTxt\" x=\"" + (marginLeft + plotW + 32)
                    + "\" y=\"12\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>");
            w.write("</g>\n");
            // Time label below axis
            w.write("<g id=\"chTimeG\">");
            w.write("<rect id=\"chTimeBg\" x=\"0\" y=\"" + (marginTop + plotH + 2)
                    + "\" width=\"70\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>");
            w.write("<text id=\"chTimeTxt\" x=\"0\" y=\"" + (marginTop + plotH + 14)
                    + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>");
            w.write("</g>\n");
            // OHLC tooltip box
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
                    volPanelGap + volPanelH, marginBottom);

            // --- Emit autoscale JavaScript ---
            w.write("<script type=\"text/javascript\">\n");
            w.write("//<![CDATA[\n");
            // Session data for autoscale
            StringBuilder sesJs = new StringBuilder("var _sesData=[");
            for (int k = 0; k < N; k++) {
                if (k > 0)
                    sesJs.append(",");
                sesJs.append(String.format(Locale.US, "{x1:%.1f,x2:%.1f,h:%.6f,l:%.6f}",
                        sesStartPx[k], sesEndPx[k], histSessionHigh[startIdx + k], histSessionLow[startIdx + k]));
            }
            sesJs.append("];");
            w.write(sesJs.toString() + "\n");
            w.write("var _mt=" + marginTop + ",_ph=" + plotH + ",_gL=" +
                    String.format(Locale.US, "%.8f", globalLow) + ",_gR=" +
                    String.format(Locale.US, "%.8f", globalRange) + ",_mL=" + marginLeft + ";\n");
            // Y-axis color globals (updated by theme toggle)
            w.write("var _yFill='#ccc',_yStroke='#888',_yBg='" + DEFAULT_DARK_BG_COLOR + "';\n");
            // Autoscale function
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
            w.write("  var invSy=1/sy;\n");
            w.write("  var txts=(cg?cg.getElementsByTagName('text'):[]);\n");
            w.write("  for(var i=0;i<txts.length;i++){\n");
            w.write("    var t=txts[i];\n");
            w.write("    if(!t.dataset.oy)t.dataset.oy=t.getAttribute('y');\n");
            w.write("    var oy=parseFloat(t.dataset.oy)||0;\n");
            w.write("    var d=(oy+3)*(1-invSy);\n");
            w.write("    t.setAttribute('transform','translate(0,'+d+') scale(1,'+invSy+')');\n");
            w.write("  }\n");

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
            // Delta labels repositioning (outside chartG, no stretching)
            w.write("  var dg=document.getElementById('deltaG');\n");
            w.write("  if(dg){\n");
            w.write("    var dGroups=dg.getElementsByClassName('dSes');\n");
            w.write("    for(var di=0;di<dGroups.length;di++){\n");
            w.write("      var dses=dGroups[di];\n");
            w.write("      var dx1=parseFloat(dses.dataset.x1),dx2=parseFloat(dses.dataset.x2);\n");
            w.write("      if(dx2>=vL&&dx1<=vR){\n");
            w.write("        dses.style.display='';\n");
            w.write("        var dtxts=dses.getElementsByTagName('text');\n");
            w.write("        for(var dj=0;dj<dtxts.length;dj++){\n");
            w.write("          var dt=dtxts[dj];\n");
            w.write("          var dp=parseFloat(dt.dataset.price);\n");
            w.write("          var ny=_mt+_ph-(dp-visL)/(visH-visL)*_ph;\n");
            w.write("          dt.setAttribute('y',ny);\n");
            w.write("        }\n");
            w.write("      }else{\n");
            w.write("        dses.style.display='none';\n");
            w.write("      }\n");
            w.write("    }\n");
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
            // Toolbar
            w.write("  var tb=document.getElementById('toolbar');\n");
            w.write("  if(tb)tb.setAttribute('transform','translate('+sx+',0)');\n");
            // Replay toolbar
            w.write("  var rp=document.getElementById('replayToolbar');\n");
            w.write("  if(rp)rp.setAttribute('transform','translate('+sx+',0)');\n");
            // Y-axis labels
            w.write("  var ya=document.getElementById('yAxisG');\n");
            w.write("  if(ya)ya.setAttribute('transform','translate('+sx+',0)');\n");
            // Y-axis background (for readability over chart content)
            w.write("  var yb=document.getElementById('yAxisBg');\n");
            w.write("  if(yb)yb.setAttribute('transform','translate('+sx+',0)');\n");
            // Chart title
            w.write("  var tt=document.getElementById('chartTitle');\n");
            w.write("  if(tt)tt.setAttribute('transform','translate('+sx+',0)');\n");
            // Annotations checkbox area
            w.write("  var ac=document.getElementById('annoGroup');\n");
            w.write("  if(ac)ac.setAttribute('transform','translate('+sx+',0)');\n");
            // Crosshair tooltip
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
            double gLow, double gRange, String barDataJs, int volExtra, int marginBottom) throws IOException {
        // Horizontal toolbar at the bottom of the chart
        int btnW = 26, btnH = 26, gap = 3;
        int tbY = mt + ph + volExtra + 60;
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
        // Print button is included at the end of the toolbar row
        int totalBtns = ids.length + 1; // +1 for print button
        int tbW = totalBtns * (btnW + gap) + gap;
        int tbX = mx; // align with chart left edge

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
        // Print button at end of toolbar row
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

        // Annotations checkbox — right of toolbar
        int annoX = tbX + tbW + 8;
        w.write("<g id=\"annoGroup\" style=\"cursor:pointer\">\n");
        w.write("<foreignObject x=\"" + annoX + "\" y=\"" + (tbY + 2) + "\" width=\"120\" height=\"22\">\n");
        w.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
        w.write("<label style=\"display:flex;align-items:center;gap:3px;font-family:Sans-Serif;font-size:10px;color:#ccc;cursor:pointer;background:rgba(30,30,50,0.85);padding:2px 5px;border-radius:4px;border:0.5px solid #555\">\n");
        w.write("<input id=\"annoCheck\" type=\"checkbox\" checked=\"checked\" style=\"margin:0;cursor:pointer\"/>\n");
        w.write("<span id=\"annoLabel\">Annotations</span></label>\n");
        w.write("</body></foreignObject>\n");
        w.write("</g>\n");

        // Color palette (hidden by default) — positioned above the color button
        String[] colors = { "#ffcc00", "#00e5ff", "#ff5252", "#69f0ae", "#ff80ab", "#b388ff", "#ffffff", "#ffa726" };
        int swatchSize = 18;
        int palX = tbX + gap + 6 * (btnW + gap); // aligned with color button
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
        w.write("  var sl=document.querySelectorAll('.thStat');for(var i=0;i<sl.length;i++){if(!sl[i].dataset.dk)sl[i].dataset.dk=sl[i].getAttribute('fill');sl[i].setAttribute('fill',isDark?sl[i].dataset.dk:'#000');}\n");
        w.write("  var br=document.querySelectorAll('.thBrand');for(var i=0;i<br.length;i++)br[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
        w.write("  var sesLbl=document.querySelectorAll('.thSesLbl');for(var i=0;i<sesLbl.length;i++)sesLbl[i].setAttribute('fill',isDark?'#ffcc00':'#000');\n");
        w.write("  document.getElementById('tbBg').setAttribute('fill',isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)');\n");
        w.write("  document.getElementById('tbBg').setAttribute('stroke',isDark?'#555':'#aaa');\n");
        w.write("  var btns=document.querySelectorAll('.tbBtn');for(var i=0;i<btns.length;i++){btns[i].setAttribute('fill',btnFill());btns[i].setAttribute('stroke',btnStroke());}\n");
        w.write("  var bl=document.querySelectorAll('.tbLbl');for(var i=0;i<bl.length;i++)bl[i].setAttribute('fill',isDark?'#ccc':'#333');\n");
        w.write("  var al=document.getElementById('annoLabel');if(al){al.style.color=isDark?'#ccc':'#333';al.parentElement.style.background=isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)';al.parentElement.style.borderColor=isDark?'#555':'#aaa';}\n");
        w.write("  document.getElementById('themeIcon').textContent=isDark?'\\u2600':'\\u263E';\n");
        w.write("  var cg=document.getElementById('candlesG');if(cg)cg.setAttribute('opacity',isDark?dCandAlpha:lCandAlpha);\n");
        w.write("  var pg=document.getElementById('profileG');if(pg)pg.setAttribute('opacity',isDark?dProfAlpha:lProfAlpha);\n");
        w.write("  _yFill=isDark?'#ccc':'#333';_yStroke=isDark?'#888':'#bbb';_yBg=isDark?dBg:lBg;\n");
        w.write("  var yb=document.getElementById('yAxisBg');if(yb){var yr=yb.getElementsByTagName('rect');if(yr.length)yr[0].setAttribute('fill',_yBg);}\n");
        // Volume subchart theme updates
        w.write("  var vsb=document.getElementById('volSubBg');if(vsb)vsb.setAttribute('fill',isDark?dPl:lPl);\n");
        w.write("  var vsp=document.querySelectorAll('.thVolSep');for(var i=0;i<vsp.length;i++)vsp[i].setAttribute('stroke',isDark?'#555':'#bbb');\n");
        w.write("  var vlb=document.querySelectorAll('.thVolLbl');for(var i=0;i<vlb.length;i++)vlb[i].setAttribute('fill',isDark?'#888':'#555');\n");
        w.write("  _autoScale();\n");
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
        // Toggle crosshair on/off
        w.write("document.getElementById('g_btnCross').addEventListener('click',function(e){\n");
        w.write("  e.stopPropagation();crosshairOn=!crosshairOn;\n");
        w.write("  var btn=document.getElementById('btnCross');\n");
        w.write("  btn.setAttribute('fill',crosshairOn?'#4181ed':btnFill());\n");
        w.write("  btn.setAttribute('stroke',crosshairOn?'#7ab5ff':btnStroke());\n");
        w.write("  if(!crosshairOn)chG.style.display='none';\n");
        w.write("});\n");
        // Find nearest bar by x position
        w.write("function nearestBar(px){\n");
        w.write("  if(!barData||barData.length===0)return null;\n");
        w.write("  var best=null,bestD=1e9;\n");
        w.write("  for(var i=0;i<barData.length;i++){var d=Math.abs(barData[i].x-px);if(d<bestD){bestD=d;best=barData[i];}}\n");
        w.write("  return best;}\n");
        // Format timestamp to date+time
        w.write("function fmtDT(ms){var d=new Date(ms);\n");
        w.write("  var dd=d.getUTCFullYear()+'-'+('0'+(d.getUTCMonth()+1)).slice(-2)+'-'+('0'+d.getUTCDate()).slice(-2);\n");
        w.write("  var tt=('0'+d.getUTCHours()).slice(-2)+':'+('0'+d.getUTCMinutes()).slice(-2);\n");
        w.write("  return{date:dd,time:tt};}\n");
        // Crosshair mousemove handler (appended to existing)
        w.write("svg.addEventListener('mousemove',function(e){\n");
        w.write("  if(!crosshairOn)return;\n");
        w.write("  var p=svgPt(e);\n");
        w.write("  if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH){chG.style.display='none';return;}\n");
        w.write("  chG.style.display='';\n");
        w.write("  chV.setAttribute('x1',p.x);chV.setAttribute('x2',p.x);\n");
        w.write("  chH.setAttribute('y1',p.y);chH.setAttribute('y2',p.y);\n");
        // Price label
        w.write("  var price=GL+GR*(1-(p.y-MT)/PH);\n");
        w.write("  chPriceTxt.textContent=fmtP(price);\n");
        w.write("  chPriceBg.setAttribute('y',p.y-8);chPriceTxt.setAttribute('y',p.y+3);\n");
        // Time label
        w.write("  chTimeBg.setAttribute('x',p.x-35);chTimeTxt.setAttribute('x',p.x);\n");
        // Find nearest bar for OHLC
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
        // Position tooltip near cursor but keep in bounds
        w.write("    var tx=p.x+15,ty=p.y-50;\n");
        w.write("    if(tx+170>MX+PW)tx=p.x-180;if(ty<MT)ty=MT+5;\n");
        w.write("    chTooltipBg.setAttribute('x',tx);chTooltipBg.setAttribute('y',ty);\n");
        w.write("    chTD.setAttribute('x',tx+8);chTO.setAttribute('x',tx+8);\n");
        w.write("    chTH.setAttribute('x',tx+8);chTL.setAttribute('x',tx+8);\n");
        w.write("    chTC.setAttribute('x',tx+8);chTV.setAttribute('x',tx+8);\n");
        w.write("    chTD.setAttribute('y',ty+14);\n");
        w.write("  }\n");
        w.write("});\n");
        // Hide crosshair when mouse leaves plot
        w.write("svg.addEventListener('mouseleave',function(){if(crosshairOn)chG.style.display='none';});\n");

        // ==================== Multi-Session Replay Engine JavaScript
        // ====================
        w.write("// Bar Replay Engine (multi-session)\n");
        w.write("if(typeof rpSessions!=='undefined' && rpSessions.length>0){\n");
        w.write("var rpStep=rpSessions[rpCurSes].n,rpPlaying=false,rpTimer=null;\n");
        w.write("var rpKnob=document.getElementById('rpSliderKnob');\n");
        w.write("var rpFill=document.getElementById('rpSliderFill');\n");
        w.write("var rpCtr=document.getElementById('rpCounter');\n");
        w.write("var rpSesLbl=document.getElementById('rpSesLabel');\n");
        w.write("var rpPauseIco=document.getElementById('rpPauseIco');\n");
        w.write("var rpTbG=document.getElementById('replayToolbar');\n");
        w.write("var rpHilight=document.getElementById('rpSesHilight');\n");
        w.write("var rpCurPocI=0,rpCurVaLo=0,rpCurVaHi=0;\n");

        // rpShowAll: make all sessions fully visible (candles + profile)
        w.write("function rpShowAll(){\n");
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var cg=document.getElementById('rpCandles_'+s);\n");
        w.write("    if(cg){var cs=cg.querySelectorAll('[id^=rc_]');cs.forEach(function(c){c.style.display='';});}\n");
        w.write("    var pg=document.getElementById('rpProfile_'+s);\n");
        w.write("    if(pg)pg.style.display='';\n");
        w.write("  }\n");
        w.write("}\n");

        // rpRender: rebuild active session's profile; hide future sessions
        w.write("function rpRender(){\n");
        w.write("  var ses=rpSessions[rpCurSes];\n");
        w.write("  var nb=ses.nb,bars=ses.bars,total=ses.n;\n");

        // Show/hide candles: past sessions=show, active=step-based, future=hide
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var cg=document.getElementById('rpCandles_'+s);\n");
        w.write("    if(!cg)continue;\n");
        w.write("    if(s>rpCurSes){cg.style.display='none';continue;}\n");
        w.write("    cg.style.display='';\n");
        w.write("    var cs=cg.querySelectorAll('[id^=rc_]');\n");
        w.write("    if(s===rpCurSes){\n");
        w.write("      cs.forEach(function(c){var ci=parseInt(c.id.split('_')[1]);c.style.display=(ci<rpStep)?'':'none';});\n");
        w.write("    } else {\n");
        w.write("      cs.forEach(function(c){c.style.display='';});\n");
        w.write("    }\n");
        w.write("  }\n");

        // Profile: past=show, active=rebuild, future=hide
        w.write("  for(var s=0;s<rpN;s++){\n");
        w.write("    var pg=document.getElementById('rpProfile_'+s);\n");
        w.write("    if(!pg)continue;\n");
        w.write("    if(s>rpCurSes){pg.style.display='none';continue;}\n");
        w.write("    if(s!==rpCurSes){pg.style.display='';continue;}\n");

        // Clear and rebuild active session profile
        w.write("    pg.style.display='';\n");
        w.write("    while(pg.firstChild)pg.removeChild(pg.firstChild);\n");
        w.write("    var bins=new Array(nb),bull=new Array(nb),bear=new Array(nb);\n");
        w.write("    for(var j=0;j<nb;j++){bins[j]=0;bull[j]=0;bear[j]=0;}\n");
        w.write("    for(var i=0;i<rpStep;i++){\n");
        w.write("      var bar=bars[i];\n");
        w.write("      for(var k in bar.d){\n");
        w.write("        var idx=parseInt(k),vol=bar.d[k];\n");
        w.write("        bins[idx]+=vol;if(bar.b)bull[idx]+=vol;else bear[idx]+=vol;\n");
        w.write("      }\n");
        w.write("    }\n");
        // Apply Gaussian cluster enhancement if enabled
        w.write("    var db=bins;\n");
        w.write("    if(rpIsCluster){\n");
        w.write("      var avg=0;for(var j=0;j<nb;j++)avg+=bins[j];avg/=Math.max(1,nb);\n");
        w.write("      var pks=[],pvs=[];\n");
        w.write("      for(var j=0;j<nb;j++){\n");
        w.write("        var lOk=(j===0||bins[j]>=bins[j-1]),rOk=(j===nb-1||bins[j]>=bins[j+1]);\n");
        w.write("        if(lOk&&rOk&&bins[j]>avg){pks.push(j);pvs.push(bins[j]);}\n");
        w.write("      }\n");
        w.write("      var mc=Math.min(rpMaxCenters,pks.length);\n");
        w.write("      for(var a=0;a<mc;a++){var bi2=a;for(var b2=a+1;b2<pks.length;b2++){if(pvs[b2]>pvs[bi2])bi2=b2;}if(bi2!==a){var ti=pks[a];pks[a]=pks[bi2];pks[bi2]=ti;var tv2=pvs[a];pvs[a]=pvs[bi2];pvs[bi2]=tv2;}}\n");
        w.write("      if(mc>0){\n");
        w.write("        db=new Array(nb);for(var j=0;j<nb;j++)db[j]=0;\n");
        w.write("        for(var c=0;c<mc;c++){var ctr=pks[c],cv=pvs[c];\n");
        w.write("          for(var j=0;j<nb;j++){var d2=(j-ctr)/rpClusterSigma;db[j]+=cv*Math.exp(-0.5*d2*d2);}\n");
        w.write("        }\n");
        w.write("      }\n");
        w.write("    }\n");
        // Find POC and VA from draw bins
        w.write("    var maxV=0;rpCurPocI=0;\n");
        w.write("    for(var j=0;j<nb;j++){if(db[j]>maxV){maxV=db[j];rpCurPocI=j;}}\n");
        w.write("    if(maxV<=0){rpUpdateUI();return;}\n");
        w.write("    var totalV=0;for(var j=0;j<nb;j++)totalV+=db[j];\n");
        w.write("    var vaTarget=totalV*0.7,vaAcc=db[rpCurPocI];rpCurVaLo=rpCurPocI;rpCurVaHi=rpCurPocI;\n");
        w.write("    while(vaAcc<vaTarget&&(rpCurVaLo>0||rpCurVaHi<nb-1)){\n");
        w.write("      var up=(rpCurVaHi<nb-1)?db[rpCurVaHi+1]:-1,dn=(rpCurVaLo>0)?db[rpCurVaLo-1]:-1;\n");
        w.write("      if(up>=dn&&up>=0){rpCurVaHi++;vaAcc+=db[rpCurVaHi];}else if(dn>=0){rpCurVaLo--;vaAcc+=db[rpCurVaLo];}else break;\n");
        w.write("    }\n");
        // Draw profile bars using draw bins (db) for shape, raw bins for bull/bear
        // split
        w.write("    for(var j=0;j<nb;j++){\n");
        w.write("      if(db[j]<=0)continue;\n");
        w.write("      var bLow=ses.low+j*ses.bs,bHi=bLow+ses.bs;\n");
        w.write("      var y1=rpMT+rpPH-(bHi-rpGLow)/rpGRange*rpPH;\n");
        w.write("      var y2=rpMT+rpPH-(bLow-rpGLow)/rpGRange*rpPH;\n");
        w.write("      var bH=Math.max(0.5,y2-y1-0.3);\n");
        w.write("      var bW=(db[j]/maxV)*ses.pw*0.8;\n");
        w.write("      var bv=bull[j],sv=bear[j],tot=bv+sv;\n");
        w.write("      if(tot<=0)tot=1;\n");
        w.write("      var bullW=bW*(bv/tot),bearW=bW-bullW;\n");
        w.write("      var isPOC=(j===rpCurPocI),isVA=(j>=rpCurVaLo&&j<=rpCurVaHi);\n");
        w.write("      if(bullW>0){\n");
        w.write("        var f=isPOC?'rgba(255,215,0,0.85)':isVA?'rgba(76,175,80,0.85)':'rgba(76,175,80,0.55)';\n");
        w.write("        var r=document.createElementNS(ns,'rect');\n");
        w.write("        r.setAttribute('x',ses.x);r.setAttribute('y',y1);\n");
        w.write("        r.setAttribute('width',bullW);r.setAttribute('height',bH);\n");
        w.write("        r.setAttribute('fill',f);pg.appendChild(r);\n");
        w.write("      }\n");
        w.write("      if(bearW>0){\n");
        w.write("        var f2=isPOC?'rgba(255,215,0,0.85)':isVA?'rgba(239,83,80,0.85)':'rgba(239,83,80,0.55)';\n");
        w.write("        var r2=document.createElementNS(ns,'rect');\n");
        w.write("        r2.setAttribute('x',ses.x+bullW);r2.setAttribute('y',y1);\n");
        w.write("        r2.setAttribute('width',bearW);r2.setAttribute('height',bH);\n");
        w.write("        r2.setAttribute('fill',f2);pg.appendChild(r2);\n");
        w.write("      }\n");
        // Per-level delta labels in replay
        w.write("      if(rpShowDelta&&bH>=2){\n");
        w.write("        var dv=bv-sv;\n");
        w.write("        var dtxt=(dv>=0?'+':'')+Math.round(dv);\n");
        w.write("        var dclr=dv>=0?'#4caf50':'#f44336';\n");
        w.write("        var dt=document.createElementNS(ns,'text');\n");
        w.write("        dt.setAttribute('x',ses.x-3);dt.setAttribute('y',y1+bH/2+3);\n");
        w.write("        dt.setAttribute('font-family','Monospace');dt.setAttribute('font-size',rpDeltaFs);\n");
        w.write("        dt.setAttribute('font-weight','700');dt.setAttribute('fill',dclr);\n");
        w.write("        dt.setAttribute('text-anchor','end');\n");
        w.write("        dt.textContent=dtxt;pg.appendChild(dt);\n");
        w.write("      }\n");
        w.write("    }\n");
        w.write("  }\n");
        w.write("  rpUpdateUI();\n");
        w.write("}\n");

        // rpUpdateUI: update slider, counter, session label, highlight, KPIs
        w.write("function rpUpdateUI(){\n");
        w.write("  var ses=rpSessions[rpCurSes],total=ses.n;\n");
        w.write("  var pct=total>0?rpStep/total:1;\n");
        w.write("  rpKnob.setAttribute('cx',rpSlX+pct*rpSlW);\n");
        w.write("  rpFill.setAttribute('x2',rpSlX+pct*rpSlW);\n");
        w.write("  rpCtr.textContent=rpStep+' / '+total;\n");
        w.write("  rpSesLbl.textContent='Session '+(rpCurSes+1)+'/'+rpN;\n");
        w.write("  var rpSI=document.getElementById('rpSesInput');if(rpSI)rpSI.value=(rpCurSes+1);\n");
        // Session highlight
        w.write("  rpHilight.setAttribute('x',rpSesXArr[rpCurSes]);\n");
        w.write("  rpHilight.setAttribute('width',rpSesXEnd[rpCurSes]-rpSesXArr[rpCurSes]);\n");
        w.write("  rpHilight.style.display='';\n");
        // Update KPIs for active session
        w.write("  rpUpdateKpi();\n");
        w.write("}\n");

        // rpUpdateKpi: update ALL stats for all sessions
        w.write("function rpUpdateKpi(){try{\n");
        // Restore past sessions to original KPIs, zero future sessions
        w.write("  for(var q=0;q<rpN;q++){\n");
        w.write("    var ve2=document.getElementById('rpVol_'+q);\n");
        w.write("    if(!ve2)continue;\n");
        w.write("    var ok=rpOrigKpi[q];\n");
        w.write("    if(q>rpCurSes){\n");
        w.write("      ve2.textContent='Vol: 0';\n");
        w.write("      document.getElementById('rpBull_'+q).textContent='Bull: 0';\n");
        w.write("      document.getElementById('rpBear_'+q).textContent='Bear: 0';\n");
        w.write("      var dEl0=document.getElementById('rpDelta_'+q);\n");
        w.write("      dEl0.textContent='Delta: 0';dEl0.setAttribute('fill','#888');\n");
        w.write("      var dpEl0=document.getElementById('rpDPOC_'+q);if(dpEl0){dpEl0.textContent='\\u0394POC: ';dpEl0.setAttribute('fill','#888');}\n");
        w.write("      var dvEl0=document.getElementById('rpDVA_'+q);if(dvEl0){dvEl0.textContent='\\u0394VA: ';dvEl0.setAttribute('fill','#888');}\n");
        w.write("      var rgEl0=document.getElementById('rpRange_'+q);if(rgEl0)rgEl0.textContent='Range: ';\n");
        w.write("      var puEl0=document.getElementById('rpPOCUp_'+q);if(puEl0)puEl0.textContent='POC\\u2191: ';\n");
        w.write("      var pdEl0=document.getElementById('rpPOCDn_'+q);if(pdEl0)pdEl0.textContent='POC\\u2193: ';\n");
        w.write("    } else if(q!==rpCurSes||rpStep>=rpSessions[q].n){\n");
        w.write("      ve2.textContent='Vol: '+ok.v;\n");
        w.write("      document.getElementById('rpBull_'+q).textContent='Bull: '+ok.b;\n");
        w.write("      document.getElementById('rpBear_'+q).textContent='Bear: '+ok.r;\n");
        w.write("      var dEl=document.getElementById('rpDelta_'+q);\n");
        w.write("      dEl.textContent='Delta: '+(ok.d>=0?'+':'')+ok.d;\n");
        w.write("      dEl.setAttribute('fill',ok.d>=0?'#00bcd4':'#f44336');\n");
        // Restore derived stats
        w.write("      var dpEl=document.getElementById('rpDPOC_'+q);\n");
        w.write("      if(dpEl){var pd=q>0?(ok.poc-rpOrigKpi[q-1].poc):0;dpEl.textContent='\\u0394POC: '+(q>0?(pd>=0?'+':'')+pd.toFixed(5):'N/A');dpEl.setAttribute('fill',q>0?(pd>=0?'#00bcd4':'#f44336'):'#888');}\n");
        w.write("      var dvEl=document.getElementById('rpDVA_'+q);\n");
        w.write("      if(dvEl){var vm=(ok.vah+ok.val)/2;var vp=q>0?((rpOrigKpi[q-1].vah+rpOrigKpi[q-1].val)/2):vm;var vd2=vm-vp;dvEl.textContent='\\u0394VA: '+(q>0?(vd2>=0?'+':'')+vd2.toFixed(5):'N/A');dvEl.setAttribute('fill',q>0?(vd2>=0?'#00bcd4':'#f44336'):'#888');}\n");
        w.write("      var rgEl=document.getElementById('rpRange_'+q);\n");
        w.write("      if(rgEl)rgEl.textContent='Range: '+(ok.sh-ok.sl).toFixed(5);\n");
        w.write("      var puEl=document.getElementById('rpPOCUp_'+q),pdEl=document.getElementById('rpPOCDn_'+q);\n");
        w.write("      if(puEl){var rng=ok.sh-ok.sl;if(rng>0){puEl.textContent='POC\\u2191: '+((ok.sh-ok.poc)/rng*100).toFixed(1)+'%';pdEl.textContent='POC\\u2193: '+((ok.poc-ok.sl)/rng*100).toFixed(1)+'%';}}\n");
        w.write("    }\n");
        w.write("  }\n");
        // Update active session if mid-replay
        w.write("  var s=rpCurSes,ses=rpSessions[s],ok2=rpOrigKpi[s];\n");
        w.write("  if(rpStep<ses.n){\n");
        w.write("    var tv=0,bv=0,rv=0;\n");
        w.write("    for(var i=0;i<rpStep;i++){\n");
        w.write("      var bar=ses.bars[i];\n");
        w.write("      for(var k in bar.d){var vol=bar.d[k];tv+=vol;if(bar.b)bv+=vol;else rv+=vol;}\n");
        w.write("    }\n");
        w.write("    var dv=bv-rv;\n");
        w.write("    document.getElementById('rpVol_'+s).textContent='Vol: '+Math.round(tv);\n");
        w.write("    document.getElementById('rpBull_'+s).textContent='Bull: '+Math.round(bv);\n");
        w.write("    document.getElementById('rpBear_'+s).textContent='Bear: '+Math.round(rv);\n");
        w.write("    var dEl2=document.getElementById('rpDelta_'+s);\n");
        w.write("    dEl2.textContent='Delta: '+(dv>=0?'+':'')+Math.round(dv);\n");
        w.write("    dEl2.setAttribute('fill',dv>=0?'#00bcd4':'#f44336');\n");
        // Compute derived stats from replay bins
        w.write("    var curPoc=ses.low+rpCurPocI*ses.bs+ses.bs/2;\n");
        w.write("    var curVaL=ses.low+rpCurVaLo*ses.bs;\n");
        w.write("    var curVaH=ses.low+(rpCurVaHi+1)*ses.bs;\n");
        w.write("    var rng2=ok2.sh-ok2.sl;\n");
        // ΔPOC
        w.write("    var dpEl2=document.getElementById('rpDPOC_'+s);\n");
        w.write("    if(dpEl2){var pd2=s>0?(curPoc-rpOrigKpi[s-1].poc):0;dpEl2.textContent='\\u0394POC: '+(s>0?(pd2>=0?'+':'')+pd2.toFixed(5):'N/A');dpEl2.setAttribute('fill',s>0?(pd2>=0?'#00bcd4':'#f44336'):'#888');}\n");
        // ΔVA
        w.write("    var dvEl2=document.getElementById('rpDVA_'+s);\n");
        w.write("    if(dvEl2){var vm2=(curVaH+curVaL)/2;var vp2=s>0?((rpOrigKpi[s-1].vah+rpOrigKpi[s-1].val)/2):vm2;var vd3=vm2-vp2;dvEl2.textContent='\\u0394VA: '+(s>0?(vd3>=0?'+':'')+vd3.toFixed(5):'N/A');dvEl2.setAttribute('fill',s>0?(vd3>=0?'#00bcd4':'#f44336'):'#888');}\n");
        // Range (fixed)
        w.write("    var rgEl2=document.getElementById('rpRange_'+s);\n");
        w.write("    if(rgEl2)rgEl2.textContent='Range: '+rng2.toFixed(5);\n");
        // POC position
        w.write("    var puEl2=document.getElementById('rpPOCUp_'+s),pdEl3=document.getElementById('rpPOCDn_'+s);\n");
        w.write("    if(puEl2&&rng2>0){puEl2.textContent='POC\\u2191: '+((ok2.sh-curPoc)/rng2*100).toFixed(1)+'%';pdEl3.textContent='POC\\u2193: '+((curPoc-ok2.sl)/rng2*100).toFixed(1)+'%';}\n");
        w.write("  }\n");
        w.write("}catch(e){}}\n");

        // rpSetStep: change bar step within current session
        w.write("function rpSetStep(s){var t=rpSessions[rpCurSes].n;rpStep=Math.max(0,Math.min(t,s));rpRender();}\n");

        // rpSwitchSession: switch to a different session (relative)
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

        // rpTogglePlay: fix — always reset to 0 of current session, then auto-continue
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

        // Initialize: show all sessions fully
        w.write("rpUpdateUI();\n");
        w.write("}\n");
        w.write("hilite();})();\n");
        w.write("]]></script>\n");
    }

    /**
     * Returns the Initial Balance period in milliseconds.
     * If cfgIBMinutes() > 0, uses that override. Otherwise uses session-type
     * defaults:
     * Daily=60min, Weekly=12h, Monthly=24h, Yearly=6days, ZigZag=60min.
     */
    protected long getIBPeriodMillis() {
        if (cfgIBMinutes() > 0) {
            return (long) cfgIBMinutes() * 60L * 1000L;
        }

        int baseType;
        if (cfgSessionType() == 1 || cfgSessionType() == 5)
            baseType = 1; // Daily
        else if (cfgSessionType() == 2 || cfgSessionType() == 6)
            baseType = 2; // Weekly
        else if (cfgSessionType() == 3 || cfgSessionType() == 7)
            baseType = 3; // Monthly
        else if (cfgSessionType() == 4 || cfgSessionType() == 8)
            baseType = 4; // Yearly
        else if (cfgSessionType() == 9 || cfgSessionType() == 10)
            baseType = 5; // ZigZag
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
            case 5:
                return 60L * 60L * 1000L; // 60 minutes (ZigZag default)
            default:
                return 60L * 60L * 1000L;
        }
    }

    /**
     * Computes the reversal threshold based on cfgPivotMethod().
     */
    protected double computePivotThreshold() throws TradingException {
        double tickSize = cfgChart().getInstrumentInfo().tickStep;
        switch (cfgPivotMethod()) {
            case 2: // Fixed Ticks
                return cfgPivotTicks() * tickSize;
            case 3: // ATR Multiple
                return computeATR(cfgPivotATRPeriod()) * cfgPivotATRMultiple();
            default: // 1 = Percentage
                double refPrice = (zzDirection == 1) ? zzPivotHigh : zzPivotLow;
                if (refPrice <= 0)
                    refPrice = cfgChart().Close(0);
                return refPrice * (cfgPivotPct() / 100.0);
        }
    }

    /**
     * Computes Average True Range inline (SMA of True Range).
     */
    protected double computeATR(int period) throws TradingException {
        int available = Math.min(period, CurrentBar);
        if (available <= 0)
            return cfgChart().High(0) - cfgChart().Low(0);
        double sum = 0;
        for (int i = 0; i < available; i++) {
            double hi = cfgChart().High(i);
            double lo = cfgChart().Low(i);
            double pc = (i + 1 <= CurrentBar) ? cfgChart().Close(i + 1) : hi;
            double tr = Math.max(hi - lo, Math.max(Math.abs(hi - pc), Math.abs(lo - pc)));
            sum += tr;
        }
        return sum / available;
    }

    /**
     * Detects ZigZag pivots based on threshold reversal.
     * Returns true if a new pivot was confirmed on this bar.
     */
    // Detects ZigZag swing pivots. Tracks the running high (when trending up) or running low
    // (when trending down). A pivot is confirmed when price reverses by at least the threshold
    // (% / ticks / ATR). Returns true on the bar a new pivot is confirmed.
    protected boolean detectZigZagPivot() throws TradingException {
        long curTime = cfgChart().Time(0);
        double high = cfgChart().High(0);
        double low = cfgChart().Low(0);

        // First bar: initialise
        if (zzDirection == 0) {
            zzPivotHigh = high;
            zzPivotLow = low;
            zzPivotHighTime = curTime;
            zzPivotLowTime = curTime;
            zzDirection = 1; // assume uptrend to begin
            zzSessionStart = curTime;
            return false;
        }

        boolean pivotFound = false;
        double threshold = computePivotThreshold();

        if (zzDirection == 1) {
            if (high > zzPivotHigh) {
                zzPivotHigh = high;
                zzPivotHighTime = curTime;
            }
            // Swing high confirmed when price reverses down by at least threshold
            if ((zzPivotHigh - low) >= threshold) {
                zzLastPivotPrice = zzPivotHigh;
                zzLastPivotDir   = 1;  // swing high
                zzLastPivotTime  = zzPivotHighTime;
                zzDirection      = -1;
                zzPivotLow       = low;
                zzPivotLowTime   = curTime;
                pivotFound = true;
            }
        } else {
            if (low < zzPivotLow) {
                zzPivotLow = low;
                zzPivotLowTime = curTime;
            }
            // Swing low confirmed when price reverses up by at least threshold
            if ((high - zzPivotLow) >= threshold) {
                zzLastPivotPrice = zzPivotLow;
                zzLastPivotDir   = -1; // swing low
                zzLastPivotTime  = zzPivotLowTime;
                zzDirection      = 1;
                zzPivotHigh      = high;
                zzPivotHighTime  = curTime;
                pivotFound = true;
            }
        }

        return pivotFound;
    }

    // Volume Cluster Profile (Gaussian Enhancement)
    // ------------------------------------------------------------------------

    // Volume Cluster Profile (VCP): spreads each dominant peak's volume across neighbours
    // using a Gaussian kernel (weight = exp(-0.5*(dist/sigma)^2)). The smoothed profile
    // is used to calculate VPOC, VVAH, VVAL instead of the raw bin values.
    protected void applyClusterEnhancement(int numBins, double binSize, double sessionLow, double totalVolume) {
        // Find local peaks above the per-bin average
        double avg = totalVolume / Math.max(1, numBins);
        int[] peakIndices = new int[numBins];
        double[] peakValues = new double[numBins];
        int peakCount = 0;

        for (int j = 0; j < numBins; j++) {
            boolean leftOk  = (j == 0)           || (volumeBins[j] >= volumeBins[j - 1]);
            boolean rightOk = (j == numBins - 1) || (volumeBins[j] >= volumeBins[j + 1]);
            if (leftOk && rightOk && volumeBins[j] > avg) {
                peakIndices[peakCount] = j;
                peakValues[peakCount]  = volumeBins[j];
                peakCount++;
            }
        }

        // Keep only the strongest N peaks
        int maxCenters = Math.min(cfgMaxClusterCenters(), peakCount);
        if (maxCenters == 0) {
            // No peaks above average — fall back to the standard (non-VCP) values
            prevVPOC = prevPOC;
            prevVVAH = prevVAH;
            prevVVAL = prevVAL;
            return;
        }

        for (int a = 0; a < maxCenters; a++) {
            int bestIdx = a;
            for (int b = a + 1; b < peakCount; b++) {
                if (peakValues[b] > peakValues[bestIdx]) {
                    bestIdx = b;
                }
            }
            if (bestIdx != a) {
                int tmpI = peakIndices[a];
                peakIndices[a] = peakIndices[bestIdx];
                peakIndices[bestIdx] = tmpI;
                double tmpV = peakValues[a];
                peakValues[a] = peakValues[bestIdx];
                peakValues[bestIdx] = tmpV;
            }
        }

        // Build Gaussian-enhanced profile: each peak radiates volume to surrounding bins
        double sigma = cfgClusterSpread();
        if (clusterBins == null || clusterBins.length < numBins) {
            clusterBins = new double[numBins];
        }
        for (int j = 0; j < numBins; j++) {
            clusterBins[j] = 0;
        }

        for (int c = 0; c < maxCenters; c++) {
            int center     = peakIndices[c];
            double centerVol = peakValues[c];
            for (int j = 0; j < numBins; j++) {
                double dist   = (j - center) / sigma;
                double weight = Math.exp(-0.5 * dist * dist);
                clusterBins[j] += centerVol * weight;
            }
        }

        // VPOC = bin with the highest smoothed volume
        int vpocIdx = 0;
        double vpocMax = clusterBins[0];
        for (int j = 1; j < numBins; j++) {
            if (clusterBins[j] > vpocMax) {
                vpocMax = clusterBins[j];
                vpocIdx = j;
            }
        }
        prevVPOC = sessionLow + (vpocIdx + 0.5) * binSize;

        // Value Area on the smoothed profile → VVAH / VVAL
        double clusterTotal = 0;
        for (int j = 0; j < numBins; j++) {
            clusterTotal += clusterBins[j];
        }
        double targetVolume = clusterTotal * (cfgValueAreaPct() / 100.0);
        double accumulated = clusterBins[vpocIdx];
        int lo = vpocIdx;
        int hi = vpocIdx;

        while (accumulated < targetVolume && (lo > 0 || hi < numBins - 1)) {
            double expandLo = (lo > 0) ? clusterBins[lo - 1] : 0;
            double expandHi = (hi < numBins - 1) ? clusterBins[hi + 1] : 0;
            if (expandLo >= expandHi && lo > 0) {
                lo--;
                accumulated += clusterBins[lo];
            } else if (hi < numBins - 1) {
                hi++;
                accumulated += clusterBins[hi];
            } else {
                lo--;
                accumulated += clusterBins[lo];
            }
        }

        prevVVAL = sessionLow + lo * binSize;
        prevVVAH = sessionLow + (hi + 1) * binSize;
    }

    // ------------------------------------------------------------------------
    // Abstract getters for @Parameter fields (declared in concrete indicators)
    // ------------------------------------------------------------------------

    protected abstract com.strategyquant.tradinglib.ChartData cfgChart();

    protected abstract int cfgBinSizeMode();

    protected abstract int cfgProfileRows();

    protected abstract int cfgTicksPerBin();

    protected abstract double cfgValueAreaPct();

    protected abstract int cfgHvnCount();

    protected abstract int cfgHvnThresholdPct();

    protected abstract int cfgLvnThresholdPct();

    protected abstract boolean cfgEnableLVN();

    protected abstract boolean cfgEnableVCP();

    protected abstract double cfgClusterSpread();

    protected abstract int cfgMaxClusterCenters();

    protected abstract int cfgIBMinutes();

    protected abstract int cfgSessionType();

    /**
     * Override to customize session label in SVG title. Returns null to use
     * default.
     */
    protected String cfgSessionLabel() {
        return null;
    }

    /**
     * Override to provide a label for the current session being calculated
     * (e.g., London, NY).
     */
    protected String cfgCurrentSessionLabel() {
        return null;
    }

    protected abstract boolean cfgShowCandlesticks();

    protected abstract boolean cfgShowVolumeSubchart();

    protected abstract int cfgVolumeMALength();

    protected abstract boolean cfgShowPOCDelta();

    protected abstract boolean cfgShowVADelta();

    protected abstract boolean cfgShowProfileRange();

    protected abstract boolean cfgShowPOCPosition();

    protected abstract boolean cfgShowDeltaPerLevel();

    /** Override to enable zigzag overlay line in SVG export. Default: false. */
    protected boolean cfgShowZigZagLine() {
        return false;
    }

    protected abstract int cfgPivotMethod();

    protected abstract double cfgPivotPct();

    protected abstract int cfgPivotTicks();

    protected abstract double cfgPivotATRMultiple();

    protected abstract int cfgPivotATRPeriod();

}
