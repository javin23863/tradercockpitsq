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
 * Volume Profile indicator with Custom Hours session definition.
 * Instead of fixed Daily/Weekly/Monthly/Yearly sessions, the user
 * defines a session by specifying a start and end hour:minute.
 * The profile is computed for the custom time window.
 */
@BuildingBlock(name = "(VP) Volume Profile Custom Hours", display = "VolumeProfileCH(#SessionStartHours#:#SessionStartMinutes#-#SessionEndHours#:#SessionEndMinutes#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@Help("Volume Profile with Custom Hours. Define your own session window by setting start/end hours and minutes. Outputs POC / VAH / VAL for the custom session period.")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set = "SessionStartHours=8,SessionStartMinutes=0,SessionEndHours=16,SessionEndMinutes=0,ProfileRows=100,ValueAreaPct=70")
@ParameterSet(set = "SessionStartHours=9,SessionStartMinutes=30,SessionEndHours=16,SessionEndMinutes=0,ProfileRows=50,ValueAreaPct=70")
@ParameterSet(set = "SessionStartHours=0,SessionStartMinutes=0,SessionEndHours=8,SessionEndMinutes=0,ProfileRows=100,ValueAreaPct=70")
public class VolumeProfileCustomHours extends VolumeProfileIndicatorChart {

    @Parameter(defaultChartIndex = 0)
    public ChartData Chart;

    @Parameter(defaultValue = "8", name = "SessionStartHours", minValue = 0, maxValue = 23, step = 1)
    @Help("Session start hour (0-23)")
    public int SessionStartHours;

    @Parameter(defaultValue = "0", name = "SessionStartMinutes", minValue = 0, maxValue = 59, step = 1)
    @Help("Session start minutes (0-59)")
    public int SessionStartMinutes;

    @Parameter(defaultValue = "16", name = "SessionEndHours", minValue = 0, maxValue = 23, step = 1)
    @Help("Session end hour (0-23)")
    public int SessionEndHours;

    @Parameter(defaultValue = "0", name = "SessionEndMinutes", minValue = 0, maxValue = 59, step = 1)
    @Help("Session end minutes (0-59)")
    public int SessionEndMinutes;

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

    // ------------------------------------------------------------------------
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
    // ------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        ensureArrays();

        long curTime = Chart.Time(0);

        boolean actualMode = (SessionMode == 2);

        if (!actualMode) {
            if (currentSessionEnd == 0 || curTime >= currentSessionEnd) {
                prevSessionStart = currentSessionStart;
                prevSessionEnd = currentSessionEnd;
                calculateSessionBoundaries(curTime);

                if (prevSessionStart > 0 && isSunday(prevSessionStart)) {
                    prevSessionStart = SQTime.addDays(SQTime.setTime(prevSessionStart, 0, 0, 0, 0), -2);
                    prevSessionEnd = SQTime.addDays(prevSessionStart, 1);
                }
                if (prevSessionStart > 0 && isSunday(prevSessionStart)) {
                    prevSessionStart = SQTime.addDays(SQTime.setTime(prevSessionStart, 0, 0, 0, 0), -2);
                    prevSessionEnd = SQTime.addDays(prevSessionStart, 1);
                }

                if (prevSessionStart > 0 && prevSessionEnd > 0) {
                    calculateVolumeProfile();
                }
            }
        } else {
            if (currentSessionEnd == 0 || curTime >= currentSessionEnd) {
                calculateSessionBoundaries(curTime);
            }
            prevSessionStart = currentSessionStart;
            prevSessionEnd = Math.min(currentSessionEnd, curTime);

            if (prevSessionStart > 0 && prevSessionEnd > prevSessionStart) {
                calculateVolumeProfile();
            }
        }

        // Output session's profile values
        POC.set(0, prevPOC);
        VAH.set(0, prevVAH);
        VAL.set(0, prevVAL);
        IBH.set(0, prevIBH);
        IBL.set(0, prevIBL);
        HVN1.set(0, prevHVN[0]);
        HVN2.set(0, prevHVN[1]);
        HVN3.set(0, prevHVN[2]);
        HVN4.set(0, prevHVN[3]);
        HVN5.set(0, prevHVN[4]);
        LVN1.set(0, prevLVN[0]);
        LVN2.set(0, prevLVN[1]);
        LVN3.set(0, prevLVN[2]);
        LVN4.set(0, prevLVN[3]);
        LVN5.set(0, prevLVN[4]);
        VPOC.set(0, prevVPOC);
        VVAH.set(0, prevVVAH);
        VVAL.set(0, prevVVAL);
        BullPOC.set(0, prevBullPOC);
        BearPOC.set(0, prevBearPOC);
        TotalVolume.set(0, prevTotalVolume);
        TotalBullVolume.set(0, prevTotalBullVolume);
        TotalBearVolume.set(0, prevTotalBearVolume);
        Delta.set(0, prevDelta);
        DeltaPOC.set(0, prevDeltaPOC);
        DeltaVA.set(0, prevDeltaVA);
        ProfileRange.set(0, prevRange);
        POCPctUp.set(0, prevPOCPctUp);
        POCPctDown.set(0, prevPOCPctDown);
    }

    // ------------------------------------------------------------------------

    /**
     * Calculates session start and end times based on custom hours.
     * Session is defined by SessionStartHours:SessionStartMinutes to
     * SessionEndHours:SessionEndMinutes within a given day.
     * If end time <= start time, the session wraps to the next day
     * (overnight session).
     */
    private void calculateSessionBoundaries(long curTime) throws TradingException {
        long dayStart = SQTime.setTime(curTime, 0, 0, 0, 0);

        // Session start time on this day
        long sessionStartToday = SQTime.setTime(dayStart,
                SessionStartHours, SessionStartMinutes, 0, 0);

        // Session end time
        long sessionEndToday;
        int startMins = SessionStartHours * 60 + SessionStartMinutes;
        int endMins = SessionEndHours * 60 + SessionEndMinutes;

        if (endMins > startMins) {
            // Same-day session: e.g., 08:00 - 16:00
            sessionEndToday = SQTime.setTime(dayStart,
                    SessionEndHours, SessionEndMinutes, 0, 0);
        } else {
            // Overnight session: e.g., 18:00 - 06:00 (wraps to next day)
            sessionEndToday = SQTime.setTime(SQTime.addDays(dayStart, 1),
                    SessionEndHours, SessionEndMinutes, 0, 0);
        }

        // If curTime is before today's session start, use yesterday's session
        if (curTime < sessionStartToday) {
            sessionStartToday = SQTime.addDays(sessionStartToday, -1);
            sessionEndToday = SQTime.addDays(sessionEndToday, -1);
        }

        // If curTime is past this session's end, advance to next day's session
        // (prevents recalculating the same profile on every bar in the gap)
        if (curTime >= sessionEndToday) {
            sessionStartToday = SQTime.addDays(sessionStartToday, 1);
            sessionEndToday = SQTime.addDays(sessionEndToday, 1);
        }

        currentSessionStart = sessionStartToday;
        currentSessionEnd = sessionEndToday;

        // Skip Sunday: if current session falls on Sunday, shift to Friday
        if (isSunday(currentSessionStart)) {
            currentSessionStart = SQTime.addDays(currentSessionStart, -2);
            currentSessionEnd = SQTime.addDays(currentSessionEnd, -2);
        }
    }

    private String sessionLabel() {
        return String.format("%02d:%02d-%02d:%02d",
                SessionStartHours, SessionStartMinutes,
                SessionEndHours, SessionEndMinutes);
    }
}
