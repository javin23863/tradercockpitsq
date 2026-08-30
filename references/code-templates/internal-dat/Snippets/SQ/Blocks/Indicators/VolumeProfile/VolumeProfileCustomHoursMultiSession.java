/*
 * Copyright (c) 2017-2024, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * this code in your open source or commercial product, please contact us.
 *
 *  - StrategyQuant programs & samples are NOT a financial advice.
 *  - WE ARE NOT RESPONSIBLE for your trading decisions OR the use of our programs in trading.
 *
 * Contact: support(at)strategyquant.com
 *
 * This file is provided under Apache License 2.0 with no warranty.
 *
 */
package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.VolumeProfileIndicatorChart;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.ParameterSet;

/**
 * Volume Profile indicator with Multi-Session support.
 * Provides 4 independent configurable sessions (London, New York, Sydney,
 * Tokyo)
 * that can be individually enabled/disabled. Each session outputs its own
 * POC/VAH/VAL.
 * Sessions may overlap (e.g. London and New York).
 */
@BuildingBlock(name = "(VP) Volume Profile Custom Hours MultiSession", display = "VolumeProfileCHMulti(#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@Help("Volume Profile with 4 configurable sessions (London, New York, Sydney, Tokyo). Each session can be enabled/disabled independently and outputs its own POC/VAH/VAL.")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set = "EnableLondon=true,EnableNewYork=true,EnableSydney=false,EnableTokyo=true,ProfileRows=100,ValueAreaPct=70")
public class VolumeProfileCustomHoursMultiSession extends VolumeProfileIndicatorChart {

    @Parameter(defaultChartIndex = 0)
    public ChartData Chart;

    // --- London Session ---
    @Parameter(defaultValue = "true", name = "EnableLondon")
    @Help("Enable London session profile")
    public boolean EnableLondon;

    @Parameter(defaultValue = "7", name = "LondonStartHour", minValue = 0, maxValue = 23, step = 1)
    @Help("London session start hour (0-23)")
    public int LondonStartHour;

    @Parameter(defaultValue = "0", name = "LondonStartMin", minValue = 0, maxValue = 59, step = 1)
    @Help("London session start minutes (0-59)")
    public int LondonStartMin;

    @Parameter(defaultValue = "16", name = "LondonEndHour", minValue = 0, maxValue = 23, step = 1)
    @Help("London session end hour (0-23)")
    public int LondonEndHour;

    @Parameter(defaultValue = "0", name = "LondonEndMin", minValue = 0, maxValue = 59, step = 1)
    @Help("London session end minutes (0-59)")
    public int LondonEndMin;

    // --- New York Session ---
    @Parameter(defaultValue = "true", name = "EnableNewYork")
    @Help("Enable New York session profile")
    public boolean EnableNewYork;

    @Parameter(defaultValue = "13", name = "NewYorkStartHour", minValue = 0, maxValue = 23, step = 1)
    @Help("New York session start hour (0-23)")
    public int NewYorkStartHour;

    @Parameter(defaultValue = "0", name = "NewYorkStartMin", minValue = 0, maxValue = 59, step = 1)
    @Help("New York session start minutes (0-59)")
    public int NewYorkStartMin;

    @Parameter(defaultValue = "22", name = "NewYorkEndHour", minValue = 0, maxValue = 23, step = 1)
    @Help("New York session end hour (0-23)")
    public int NewYorkEndHour;

    @Parameter(defaultValue = "0", name = "NewYorkEndMin", minValue = 0, maxValue = 59, step = 1)
    @Help("New York session end minutes (0-59)")
    public int NewYorkEndMin;

    // --- Sydney Session ---
    @Parameter(defaultValue = "false", name = "EnableSydney")
    @Help("Enable Sydney session profile")
    public boolean EnableSydney;

    @Parameter(defaultValue = "21", name = "SydneyStartHour", minValue = 0, maxValue = 23, step = 1)
    @Help("Sydney session start hour (0-23)")
    public int SydneyStartHour;

    @Parameter(defaultValue = "0", name = "SydneyStartMin", minValue = 0, maxValue = 59, step = 1)
    @Help("Sydney session start minutes (0-59)")
    public int SydneyStartMin;

    @Parameter(defaultValue = "6", name = "SydneyEndHour", minValue = 0, maxValue = 23, step = 1)
    @Help("Sydney session end hour (0-23)")
    public int SydneyEndHour;

    @Parameter(defaultValue = "0", name = "SydneyEndMin", minValue = 0, maxValue = 59, step = 1)
    @Help("Sydney session end minutes (0-59)")
    public int SydneyEndMin;

    // --- Tokyo Session ---
    @Parameter(defaultValue = "true", name = "EnableTokyo")
    @Help("Enable Tokyo session profile")
    public boolean EnableTokyo;

    @Parameter(defaultValue = "0", name = "TokyoStartHour", minValue = 0, maxValue = 23, step = 1)
    @Help("Tokyo session start hour (0-23)")
    public int TokyoStartHour;

    @Parameter(defaultValue = "0", name = "TokyoStartMin", minValue = 0, maxValue = 59, step = 1)
    @Help("Tokyo session start minutes (0-59)")
    public int TokyoStartMin;

    @Parameter(defaultValue = "9", name = "TokyoEndHour", minValue = 0, maxValue = 23, step = 1)
    @Help("Tokyo session end hour (0-23)")
    public int TokyoEndHour;

    @Parameter(defaultValue = "0", name = "TokyoEndMin", minValue = 0, maxValue = 59, step = 1)
    @Help("Tokyo session end minutes (0-59)")
    public int TokyoEndMin;

    @Parameter(defaultValue = "1", name = "SessionMode")
    @Help("Previous = last completed session. Actual = current developing session.")
    @Editor(type = Editors.Selection, values = "Previous=1,Actual=2")
    public int SessionMode;

    @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10, maxValue = 500, step = 10)
    @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
    public int ProfileRows;

    @Parameter(defaultValue = "2", name = "BinSizeMode")
    @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
    @Editor(type = Editors.Selection, values = "Range-Based=1,Fixed Tick Size=2")
    public int BinSizeMode;

    @Parameter(defaultValue = "3", name = "TicksPerBin", minValue = 1, maxValue = 1000, step = 1)
    @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
    public int TicksPerBin;

    @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30, maxValue = 95, step = 5)
    @Help("Percentage of total volume that defines the Value Area")
    public double ValueAreaPct;

    @Parameter(defaultValue = "5", name = "HvnCount", minValue = 1, maxValue = 10, step = 1)
    @Help("Number of High Volume Nodes (local peaks) to detect in the profile")
    public int HvnCount;

    @Parameter(defaultValue = "20", name = "HvnThresholdPct", minValue = 10, maxValue = 90, step = 5)
    @Help("Minimum volume as % of max bin volume for a local peak to qualify as HVN")
    public int HvnThresholdPct;

    @Parameter(defaultValue = "40", name = "LvnThresholdPct", minValue = 10, maxValue = 90, step = 5)
    @Help("Maximum volume as % of max bin volume for a local valley to qualify as LVN")
    public int LvnThresholdPct;

    @Parameter(defaultValue = "false", name = "EnableLVN")
    @Help("If true, detect Low Volume Nodes (LVN1-LVN5). If false, LVN outputs remain 0.")
    public boolean EnableLVN;

    @Parameter(defaultValue = "false", name = "EnableVolumeCluster")
    @Help("Enable Volume Cluster Profile: Gaussian-enhanced clustering that smooths the profile around dominant volume peaks")
    public boolean EnableVCP;

    @Parameter(defaultValue = "6.0", name = "VolumeClusterSpread", minValue = 0.5, maxValue = 20.0, step = 0.5)
    @Help("Gaussian sigma controlling how wide each cluster center's influence reaches (higher = smoother/wider)")
    public double ClusterSpread;

    @Parameter(defaultValue = "2", name = "MaxVolumeClusterCenters", minValue = 1, maxValue = 10, step = 1)
    @Help("Maximum number of cluster peaks used in Gaussian enhancement")
    public int MaxClusterCenters;

    @Parameter(defaultValue = "0", name = "IBMinutes", minValue = 0, maxValue = 14400, step = 1)
    @Help("Initial Balance period override in minutes. 0 = use default (60min). Values > 0 override the default.")
    public int IBMinutes;

    // ---- SVG/PNG Export settings (visual only — no effect on POC/VAH/VAL outputs) ----
    // Note: chart export is enabled via global Trading Options > "Store Chart Data"
    @Parameter(defaultValue = "true", name = "ShowCandlesticks", showIfDefault = false)
    @Help("If true, show candlestick price chart alongside each session profile in SVG export")
    public boolean ShowCandlesticks;

    @Parameter(defaultValue = "false", name = "ShowVolumeSubchart", showIfDefault = false)
    @Help("If true, show a volume bar subplot below the main chart in SVG export")
    public boolean ShowVolumeSubchart;

    @Parameter(defaultValue = "20", name = "VolumeMALength", minValue = 2, maxValue = 200, step = 1, showIfDefault = false)
    @Help("Period for the moving average line overlaid on the volume subchart")
    public int VolumeMALength;

    @Parameter(defaultValue = "true", name = "ShowPOCDelta", showIfDefault = false)
    @Help("Show POC price change vs previous session in SVG export")
    public boolean ShowPOCDelta;

    @Parameter(defaultValue = "true", name = "ShowVADelta", showIfDefault = false)
    @Help("Show Value Area midpoint change vs previous session in SVG export")
    public boolean ShowVADelta;

    @Parameter(defaultValue = "true", name = "ShowProfileRange", showIfDefault = false)
    @Help("Show session high-low range in SVG export")
    public boolean ShowProfileRange;

    @Parameter(defaultValue = "true", name = "ShowPOCPosition", showIfDefault = false)
    @Help("Show POC position as pct from top and bottom of profile range in SVG export")
    public boolean ShowPOCPosition;

    @Parameter(defaultValue = "false", name = "ShowDeltaPerLevel", showIfDefault = false)
    @Help("If true, show per-level delta (bull minus bear) labels on each bin of the volume profile in SVG export")
    public boolean ShowDeltaPerLevel;

    // --- Standard outputs (first enabled session drives these) ---
    @Output(name = "POC", color = Colors.Yellow)
    public DataSeries POC;

    @Output(name = "VAH", color = Colors.Green)
    public DataSeries VAH;

    @Output(name = "VAL", color = Colors.Red)
    public DataSeries VAL;

    @Output(name = "IBH", color = Colors.Azure, show = false)
    public DataSeries IBH;

    @Output(name = "IBL", color = Colors.Magenta, show = false)
    public DataSeries IBL;

    @Output(name = "HVN1", color = Colors.Purple, show = false)
    public DataSeries HVN1;

    @Output(name = "HVN2", color = Colors.Purple, show = false)
    public DataSeries HVN2;

    @Output(name = "HVN3", color = Colors.Purple, show = false)
    public DataSeries HVN3;

    @Output(name = "HVN4", color = Colors.Purple, show = false)
    public DataSeries HVN4;

    @Output(name = "HVN5", color = Colors.Purple, show = false)
    public DataSeries HVN5;

    @Output(name = "LVN1", color = Colors.Orange, show = false)
    public DataSeries LVN1;

    @Output(name = "LVN2", color = Colors.Orange, show = false)
    public DataSeries LVN2;

    @Output(name = "LVN3", color = Colors.Orange, show = false)
    public DataSeries LVN3;

    @Output(name = "LVN4", color = Colors.Orange, show = false)
    public DataSeries LVN4;

    @Output(name = "LVN5", color = Colors.Orange, show = false)
    public DataSeries LVN5;

    @Output(name = "VPOC", color = Colors.Azure, show = false)
    public DataSeries VPOC;

    @Output(name = "VVAH", color = Colors.LightGreen, show = false)
    public DataSeries VVAH;

    @Output(name = "VVAL", color = Colors.Beige, show = false)
    public DataSeries VVAL;

    @Output(name = "BullPOC", color = Colors.Green, show = false)
    public DataSeries BullPOC;

    @Output(name = "BearPOC", color = Colors.Red, show = false)
    public DataSeries BearPOC;

    @Output(name = "TotalVolume", color = Colors.Gray, show = false)
    public DataSeries TotalVolume;

    @Output(name = "TotalBullVolume", color = Colors.Green, show = false)
    public DataSeries TotalBullVolume;

    @Output(name = "TotalBearVolume", color = Colors.Red, show = false)
    public DataSeries TotalBearVolume;

    @Output(name = "Delta", color = Colors.Azure, show = false)
    public DataSeries Delta;

    @Output(name = "DeltaPOC", color = Colors.Azure, show = false)
    public DataSeries DeltaPOC;

    @Output(name = "DeltaVA", color = Colors.Azure, show = false)
    public DataSeries DeltaVA;

    @Output(name = "ProfileRange", color = Colors.Gray, show = false)
    public DataSeries ProfileRange;

    @Output(name = "POCPctUp", color = Colors.Gray, show = false)
    public DataSeries POCPctUp;

    @Output(name = "POCPctDown", color = Colors.Gray, show = false)
    public DataSeries POCPctDown;

    // --- Per-session outputs ---
    @Output(name = "LondonPOC", color = Colors.Yellow, show = false)
    public DataSeries LondonPOC;

    @Output(name = "LondonVAH", color = Colors.Green, show = false)
    public DataSeries LondonVAH;

    @Output(name = "LondonVAL", color = Colors.Red, show = false)
    public DataSeries LondonVAL;

    @Output(name = "NewYorkPOC", color = Colors.Yellow, show = false)
    public DataSeries NewYorkPOC;

    @Output(name = "NewYorkVAH", color = Colors.Green, show = false)
    public DataSeries NewYorkVAH;

    @Output(name = "NewYorkVAL", color = Colors.Red, show = false)
    public DataSeries NewYorkVAL;

    @Output(name = "SydneyPOC", color = Colors.Yellow, show = false)
    public DataSeries SydneyPOC;

    @Output(name = "SydneyVAH", color = Colors.Green, show = false)
    public DataSeries SydneyVAH;

    @Output(name = "SydneyVAL", color = Colors.Red, show = false)
    public DataSeries SydneyVAL;

    @Output(name = "TokyoPOC", color = Colors.Yellow, show = false)
    public DataSeries TokyoPOC;

    @Output(name = "TokyoVAH", color = Colors.Green, show = false)
    public DataSeries TokyoVAH;

    @Output(name = "TokyoVAL", color = Colors.Red, show = false)
    public DataSeries TokyoVAL;

    // --- Multi-session constants ---
    private static final int NUM_SESSIONS = 4;
    private static final int S_LONDON = 0;
    private static final int S_NEWYORK = 1;
    private static final int S_SYDNEY = 2;
    private static final int S_TOKYO = 3;
    private static final String[] SESSION_NAMES = { "London", "NewYork", "Sydney", "Tokyo" };
    private static final String[] SESSION_COLORS = { "#2196f3", "#ff9800", "#4caf50", "#e040fb" };
    // Per-session tracking (arrays of NUM_SESSIONS)
    private long[] currentSessionStart = new long[NUM_SESSIONS];
    private long[] currentSessionEnd = new long[NUM_SESSIONS];
    private long[] prevSessionStart = new long[NUM_SESSIONS];
    private long[] prevSessionEnd = new long[NUM_SESSIONS];

    // Per-session profile results
    private double[] prevPOC = new double[NUM_SESSIONS];
    private double[] prevVAH = new double[NUM_SESSIONS];
    private double[] prevVAL = new double[NUM_SESSIONS];
    private double[] prevIBH = new double[NUM_SESSIONS];
    private double[] prevIBL = new double[NUM_SESSIONS];
    private double[][] prevHVN = new double[NUM_SESSIONS][5];
    private double[][] prevLVN = new double[NUM_SESSIONS][5];

    // Per-session VCP results
    private double[] prevVPOC = new double[NUM_SESSIONS];
    private double[] prevVVAH = new double[NUM_SESSIONS];
    private double[] prevVVAL = new double[NUM_SESSIONS];

    // Per-session Bull/Bear POC results
    private double[] prevBullPOC = new double[NUM_SESSIONS];
    private double[] prevBearPOC = new double[NUM_SESSIONS];

    // Per-session total volume results
    private double[] prevTotalVolume = new double[NUM_SESSIONS];
    private double[] prevTotalBullVolume = new double[NUM_SESSIONS];
    private double[] prevTotalBearVolume = new double[NUM_SESSIONS];

    // Per-session derived data series
    private double[] prevDelta = new double[NUM_SESSIONS];
    private double[] prevDeltaPOC = new double[NUM_SESSIONS];
    private double[] prevDeltaVA = new double[NUM_SESSIONS];
    private double[] prevRange = new double[NUM_SESSIONS];
    private double[] prevPOCPctUp = new double[NUM_SESSIONS];
    private double[] prevPOCPctDown = new double[NUM_SESSIONS];

    // Arrays for profile calculation (reused across sessions)
    private double[] volumeBins;
    private double[] bullVolumeBins;
    private double[] bearVolumeBins;
    private double[] clusterBins;

    // Tracking
    private int M1chartNumber = -1;
    private int lastNumBins = 0;

    // Session history for multi-session export
    private int historyCount = 0;
    private long[] histSessionStart;
    private long[] histSessionEnd;
    private double[] histPOC, histVAH, histVAL, histIBH, histIBL;
    private double[] histSessionHigh, histSessionLow;
    private double[][] histVolumeBins;
    private double[][] histBullBins;
    private double[][] histBearBins;
    private double[][] histHVN;
    private double[][] histLVN;
    private int[] histNumBins;
    private double[] histTotalVolume;
    private double[] histBullVolume;
    private double[] histBearVolume;

    // Active session index being processed (used by calculateVolumeProfile)
    private int activeSes = 0;

    /**
     * Copy array[s] values TO parent scalar fields before calling
     * calculateVolumeProfile().
     * The parent reads prevSessionStart/End as scalars.
     */
    private void copyToParentScalars(int s) {
        super.prevSessionStart = prevSessionStart[s];
        super.prevSessionEnd = prevSessionEnd[s];
        // Also restore session-specific VP results so that if
        // calculateVolumeProfile() returns early (0 bars), the parent
        // scalars still hold this session's values, preventing
        // cross-session contamination.
        super.prevPOC = prevPOC[s];
        super.prevVAH = prevVAH[s];
        super.prevVAL = prevVAL[s];
        super.prevIBH = prevIBH[s];
        super.prevIBL = prevIBL[s];
        for (int n = 0; n < 5; n++) {
            super.prevHVN[n] = prevHVN[s][n];
            super.prevLVN[n] = prevLVN[s][n];
        }
        super.prevVPOC = prevVPOC[s];
        super.prevVVAH = prevVVAH[s];
        super.prevVVAL = prevVVAL[s];
        super.prevBullPOC = prevBullPOC[s];
        super.prevBearPOC = prevBearPOC[s];
        super.prevTotalVolume = prevTotalVolume[s];
        super.prevTotalBullVolume = prevTotalBullVolume[s];
        super.prevTotalBearVolume = prevTotalBearVolume[s];
        super.prevDelta = prevDelta[s];
        super.prevDeltaPOC = prevDeltaPOC[s];
        super.prevDeltaVA = prevDeltaVA[s];
        super.prevRange = prevRange[s];
        super.prevPOCPctUp = prevPOCPctUp[s];
        super.prevPOCPctDown = prevPOCPctDown[s];
    }

    /**
     * Copy parent scalar result fields back INTO our arrays after
     * calculateVolumeProfile().
     */
    private void copyFromParentScalars(int s) {
        prevPOC[s] = super.prevPOC;
        prevVAH[s] = super.prevVAH;
        prevVAL[s] = super.prevVAL;
        prevIBH[s] = super.prevIBH;
        prevIBL[s] = super.prevIBL;
        for (int n = 0; n < 5; n++) {
            prevHVN[s][n] = super.prevHVN[n];
            prevLVN[s][n] = super.prevLVN[n];
        }
        prevVPOC[s] = super.prevVPOC;
        prevVVAH[s] = super.prevVVAH;
        prevVVAL[s] = super.prevVVAL;
        prevBullPOC[s] = super.prevBullPOC;
        prevBearPOC[s] = super.prevBearPOC;
        prevTotalVolume[s] = super.prevTotalVolume;
        prevTotalBullVolume[s] = super.prevTotalBullVolume;
        prevTotalBearVolume[s] = super.prevTotalBearVolume;
        prevDelta[s] = super.prevDelta;
        prevDeltaPOC[s] = super.prevDeltaPOC;
        prevDeltaVA[s] = super.prevDeltaVA;
        prevRange[s] = super.prevRange;
        prevPOCPctUp[s] = super.prevPOCPctUp;
        prevPOCPctDown[s] = super.prevPOCPctDown;
    }

    // Arrays for profile calculation (reused)

    // Fixed export folder relative to SQX install
    // Random suffix for this indicator instance (generated once in OnInit)

    // Session history for multi-session export
    private String[] histSessionType;
    // ------------------------------------------------------------------------
    // Getter implementations for VolumeProfileIndicatorChart abstract methods
    // ------------------------------------------------------------------------

    @Override
    protected com.strategyquant.tradinglib.ChartData cfgChart() {
        return Chart;
    }

    @Override
    protected int cfgBinSizeMode() {
        return BinSizeMode;
    }

    @Override
    protected int cfgProfileRows() {
        return ProfileRows;
    }

    @Override
    protected int cfgTicksPerBin() {
        return TicksPerBin;
    }

    @Override
    protected double cfgValueAreaPct() {
        return ValueAreaPct;
    }

    @Override
    protected int cfgHvnCount() {
        return HvnCount;
    }

    @Override
    protected int cfgHvnThresholdPct() {
        return HvnThresholdPct;
    }

    @Override
    protected int cfgLvnThresholdPct() {
        return LvnThresholdPct;
    }

    @Override
    protected boolean cfgEnableLVN() {
        return EnableLVN;
    }

    @Override
    protected boolean cfgEnableVCP() {
        return EnableVCP;
    }

    @Override
    protected double cfgClusterSpread() {
        return ClusterSpread;
    }

    @Override
    protected int cfgMaxClusterCenters() {
        return MaxClusterCenters;
    }

    @Override
    protected int cfgIBMinutes() {
        return IBMinutes;
    }

    @Override
    protected int cfgSessionType() {
        return (SessionMode == 2) ? 5 : 0;
    } // Return 5 (Actual Day) when in Actual mode for replay toolbar

    @Override
    protected String cfgSessionLabel() {
        return (SessionMode == 2) ? "Actual Session" : "Previous Session";
    }

    @Override
    protected String cfgCurrentSessionLabel() {
        if (activeSes >= 0 && activeSes < SESSION_NAMES.length) {
            return SESSION_NAMES[activeSes];
        }
        return null;
    }

    @Override
    protected boolean cfgShowCandlesticks() {
        return ShowCandlesticks;
    }

    @Override
    protected boolean cfgShowVolumeSubchart() {
        return ShowVolumeSubchart;
    }

    @Override
    protected int cfgVolumeMALength() {
        return VolumeMALength;
    }

    @Override
    protected boolean cfgShowPOCDelta() {
        return ShowPOCDelta;
    }

    @Override
    protected boolean cfgShowVADelta() {
        return ShowVADelta;
    }

    @Override
    protected boolean cfgShowProfileRange() {
        return ShowProfileRange;
    }

    @Override
    protected boolean cfgShowPOCPosition() {
        return ShowPOCPosition;
    }

    @Override
    protected boolean cfgShowDeltaPerLevel() {
        return ShowDeltaPerLevel;
    }

    @Override
    protected int cfgPivotMethod() {
        return 0;
    }

    @Override
    protected double cfgPivotPct() {
        return 0.5;
    }

    @Override
    protected int cfgPivotTicks() {
        return 50;
    }

    @Override
    protected double cfgPivotATRMultiple() {
        return 1.5;
    }

    @Override
    protected int cfgPivotATRPeriod() {
        return 14;
    }

    private boolean isSessionEnabled(int s) {
        switch (s) {
            case S_LONDON:
                return EnableLondon;
            case S_NEWYORK:
                return EnableNewYork;
            case S_SYDNEY:
                return EnableSydney;
            case S_TOKYO:
                return EnableTokyo;
            default:
                return false;
        }
    }

    /** Returns {startHour, startMin, endHour, endMin} for session index s. */
    private int[] getSessionTimes(int s) {
        switch (s) {
            case S_LONDON:
                return new int[] { LondonStartHour, LondonStartMin, LondonEndHour, LondonEndMin };
            case S_NEWYORK:
                return new int[] { NewYorkStartHour, NewYorkStartMin, NewYorkEndHour, NewYorkEndMin };
            case S_SYDNEY:
                return new int[] { SydneyStartHour, SydneyStartMin, SydneyEndHour, SydneyEndMin };
            case S_TOKYO:
                return new int[] { TokyoStartHour, TokyoStartMin, TokyoEndHour, TokyoEndMin };
            default:
                return new int[] { 0, 0, 0, 0 };
        }
    }

    // ------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        ensureArrays();

        long curTime = Chart.Time(0);
        boolean actualMode = (SessionMode == 2);

        // Process each enabled session independently
        for (int s = 0; s < NUM_SESSIONS; s++) {
            if (!isSessionEnabled(s))
                continue;
            activeSes = s;

            if (!actualMode) {
                if (currentSessionEnd[s] == 0 || curTime >= currentSessionEnd[s]) {
                    prevSessionStart[s] = currentSessionStart[s];
                    prevSessionEnd[s] = currentSessionEnd[s];
                    calculateSessionBoundaries(curTime, s);

                    if (prevSessionStart[s] > 0 && isSunday(prevSessionStart[s])) {
                        prevSessionStart[s] = SQTime.addDays(SQTime.setTime(prevSessionStart[s], 0, 0, 0, 0), -2);
                        prevSessionEnd[s] = SQTime.addDays(prevSessionStart[s], 1);
                    }

                    if (prevSessionStart[s] > 0 && prevSessionEnd[s] > 0) {
                        copyToParentScalars(s);
                        calculateVolumeProfile();
                        copyFromParentScalars(s);
                    }
                }
            } else {
                if (currentSessionEnd[s] == 0 || curTime >= currentSessionEnd[s]) {
                    calculateSessionBoundaries(curTime, s);
                }
                // Only recalculate if the bar is within this session's time window
                if (curTime >= currentSessionStart[s] && curTime < currentSessionEnd[s]) {
                    prevSessionStart[s] = currentSessionStart[s];
                    prevSessionEnd[s] = Math.min(currentSessionEnd[s], curTime);

                    if (prevSessionStart[s] > 0 && prevSessionEnd[s] > prevSessionStart[s]) {
                        copyToParentScalars(s);
                        calculateVolumeProfile();
                        copyFromParentScalars(s);
                    }
                }
            }
        }

        // Find first enabled session for standard outputs
        int first = -1;
        for (int s = 0; s < NUM_SESSIONS; s++) {
            if (isSessionEnabled(s)) {
                first = s;
                break;
            }
        }
        if (first >= 0) {
            POC.set(0, prevPOC[first]);
            VAH.set(0, prevVAH[first]);
            VAL.set(0, prevVAL[first]);
            IBH.set(0, prevIBH[first]);
            IBL.set(0, prevIBL[first]);
            HVN1.set(0, prevHVN[first][0]);
            HVN2.set(0, prevHVN[first][1]);
            HVN3.set(0, prevHVN[first][2]);
            HVN4.set(0, prevHVN[first][3]);
            HVN5.set(0, prevHVN[first][4]);
            LVN1.set(0, prevLVN[first][0]);
            LVN2.set(0, prevLVN[first][1]);
            LVN3.set(0, prevLVN[first][2]);
            LVN4.set(0, prevLVN[first][3]);
            LVN5.set(0, prevLVN[first][4]);
            VPOC.set(0, prevVPOC[first]);
            VVAH.set(0, prevVVAH[first]);
            VVAL.set(0, prevVVAL[first]);
            BullPOC.set(0, prevBullPOC[first]);
            BearPOC.set(0, prevBearPOC[first]);
            TotalVolume.set(0, prevTotalVolume[first]);
            TotalBullVolume.set(0, prevTotalBullVolume[first]);
            TotalBearVolume.set(0, prevTotalBearVolume[first]);
            Delta.set(0, prevDelta[first]);
            DeltaPOC.set(0, prevDeltaPOC[first]);
            DeltaVA.set(0, prevDeltaVA[first]);
            ProfileRange.set(0, prevRange[first]);
            POCPctUp.set(0, prevPOCPctUp[first]);
            POCPctDown.set(0, prevPOCPctDown[first]);
        }

        // Per-session outputs
        LondonPOC.set(0, prevPOC[S_LONDON]);
        LondonVAH.set(0, prevVAH[S_LONDON]);
        LondonVAL.set(0, prevVAL[S_LONDON]);
        NewYorkPOC.set(0, prevPOC[S_NEWYORK]);
        NewYorkVAH.set(0, prevVAH[S_NEWYORK]);
        NewYorkVAL.set(0, prevVAL[S_NEWYORK]);
        SydneyPOC.set(0, prevPOC[S_SYDNEY]);
        SydneyVAH.set(0, prevVAH[S_SYDNEY]);
        SydneyVAL.set(0, prevVAL[S_SYDNEY]);
        TokyoPOC.set(0, prevPOC[S_TOKYO]);
        TokyoVAH.set(0, prevVAH[S_TOKYO]);
        TokyoVAL.set(0, prevVAL[S_TOKYO]);
    }

    // ------------------------------------------------------------------------

    /**
     * Calculates session start and end times for the given session index.
     * Uses per-session configurable hours/minutes.
     */
    private void calculateSessionBoundaries(long curTime, int s) throws TradingException {
        int[] times = getSessionTimes(s);
        int sesStartH = times[0], sesStartM = times[1];
        int sesEndH = times[2], sesEndM = times[3];

        long dayStart = SQTime.setTime(curTime, 0, 0, 0, 0);

        long sessionStartToday = SQTime.setTime(dayStart, sesStartH, sesStartM, 0, 0);

        long sessionEndToday;
        int startMins = sesStartH * 60 + sesStartM;
        int endMins = sesEndH * 60 + sesEndM;

        if (endMins > startMins) {
            sessionEndToday = SQTime.setTime(dayStart, sesEndH, sesEndM, 0, 0);
        } else {
            sessionEndToday = SQTime.setTime(SQTime.addDays(dayStart, 1), sesEndH, sesEndM, 0, 0);
        }

        if (curTime < sessionStartToday) {
            sessionStartToday = SQTime.addDays(sessionStartToday, -1);
            sessionEndToday = SQTime.addDays(sessionEndToday, -1);
        }

        if (curTime >= sessionEndToday) {
            sessionStartToday = SQTime.addDays(sessionStartToday, 1);
            sessionEndToday = SQTime.addDays(sessionEndToday, 1);
        }

        currentSessionStart[s] = sessionStartToday;
        currentSessionEnd[s] = sessionEndToday;

        if (isSunday(currentSessionStart[s])) {
            currentSessionStart[s] = SQTime.addDays(currentSessionStart[s], -2);
            currentSessionEnd[s] = SQTime.addDays(currentSessionEnd[s], -2);
        }
    }

    private String sessionLabel() {
        int[] t = getSessionTimes(activeSes);
        String modeLabel = (SessionMode == 1) ? "Previous" : "Actual";
        return modeLabel + " " + SESSION_NAMES[activeSes] + " " + String.format("%02d:%02d-%02d:%02d",
                t[0], t[1], t[2], t[3]);
    }
}
