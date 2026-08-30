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
package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.VolumeProfileIndicatorChart;

import SQ.Blocks.Indicators.MTATR.MTATR;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


/**
 * Volume Profile indicator.
 */
@BuildingBlock(name = "(VP) Volume Profile", display = "VolumeProfile(#SessionType#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@Help("Volume Profile. Outputs POC / VAH / VAL. Use H1 Timeframe for monthly session, M30 for weekly session")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set = "SessionType=1,ProfileRows=100,ValueAreaPct=70")
@ParameterSet(set = "SessionType=1,ProfileRows=50,ValueAreaPct=70")
@ParameterSet(set = "SessionType=2,ProfileRows=100,ValueAreaPct=70")
@ParameterSet(set = "SessionType=3,ProfileRows=100,ValueAreaPct=70")

public class VolumeProfile extends VolumeProfileIndicatorChart {

    @Parameter(defaultChartIndex = 0, category = "General")
    public ChartData Chart;

    @Parameter(defaultValue = "2", name = "SessionType", category = "General")
    @Help("Session selection: Previous/Actual Day/Week/Month/Year/Swing for profile calculation")
    @Editor(type = Editors.Selection, values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8,Previous Swing=9,Actual Swing=10")
    public int SessionType;

    @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10, maxValue = 500, step = 10, category = "Profile Calculation")
    @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
    public int ProfileRows;

    @Parameter(defaultValue = "2", name = "BinSizeMode", category = "Profile Calculation")
    @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
    @Editor(type = Editors.Selection, values = "Range-Based=1,Fixed Tick Size=2")
    public int BinSizeMode;

    @Parameter(defaultValue = "3", name = "TicksPerBin", minValue = 1, maxValue = 1000, step = 1, category = "Profile Calculation")
    @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
    public int TicksPerBin;

    @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30, maxValue = 95, step = 5, category = "Profile Calculation")
    @Help("Percentage of total volume that defines the Value Area")
    public double ValueAreaPct;

    @Parameter(defaultValue = "0", name = "IBMinutes", minValue = 0, maxValue = 14400, step = 1, category = "Session / Time")
    @Help("Initial Balance period override in minutes. 0 = use session-type default (60min daily, 12h weekly, etc). Values > 0 override the default.")
    public int IBMinutes;

    @Parameter(defaultValue = "5", name = "HvnCount", minValue = 1, maxValue = 10, step = 1, category = "Volume Structure")
    @Help("Number of High Volume Nodes (local peaks) to detect in the profile")
    public int HvnCount;

    @Parameter(defaultValue = "20", name = "HvnThresholdPct", minValue = 10, maxValue = 90, step = 5, category = "Volume Structure")
    @Help("Minimum volume as % of max bin volume for a local peak to qualify as HVN")
    public int HvnThresholdPct;

    @Parameter(defaultValue = "40", name = "LvnThresholdPct", minValue = 10, maxValue = 90, step = 5, category = "Volume Structure")
    @Help("Maximum volume as % of max bin volume for a local valley to qualify as LVN")
    public int LvnThresholdPct;

    @Parameter(defaultValue = "false", name = "EnableLVN", category = "Volume Structure")
    @Help("If true, detect Low Volume Nodes (LVN1-LVN5). If false, LVN outputs remain 0.")
    public boolean EnableLVN;

    @Parameter(defaultValue = "false", name = "EnableVolumeCluster", category = "Advanced Features")
    @Help("Enable Volume Cluster Profile: Gaussian-enhanced clustering that smooths the profile around dominant volume peaks")
    public boolean EnableVCP;

    @Parameter(defaultValue = "6.0", name = "VolumeClusterSpread", minValue = 0.5, maxValue = 20.0, step = 0.5, category = "Advanced Features")
    @Help("Gaussian sigma controlling how wide each cluster center's influence reaches (higher = smoother/wider)")
    public double ClusterSpread;

    @Parameter(defaultValue = "2", name = "MaxVolumeClusterCenters", minValue = 1, maxValue = 10, step = 1, category = "Advanced Features")
    @Help("Maximum number of cluster peaks used in Gaussian enhancement")
    public int MaxClusterCenters;

    @Parameter(defaultValue = "1", name = "PivotMethod", category = "Advanced Features")
    @Help("ZigZag reversal method: 1=Percentage of price, 2=Fixed Ticks, 3=ATR Multiple")
    @Editor(type = Editors.Selection, values = "Percentage=1,Fixed Ticks=2,ATR Multiple=3")
    public int PivotMethod;

    @Parameter(defaultValue = "2.0", name = "PivotPct", minValue = 0.01, maxValue = 10.0, step = 0.1, category = "Advanced Features")
    @Help("Reversal threshold as percentage of price (e.g. 2.0 = 2%). Used when PivotMethod=Percentage")
    public double PivotPct;

    @Parameter(defaultValue = "50", name = "PivotTicks", minValue = 1, maxValue = 10000, step = 1, category = "Advanced Features")
    @Help("Reversal threshold in ticks (e.g. 50 = 50 ticks). Used when PivotMethod=Fixed Ticks")
    public int PivotTicks;

    @Parameter(defaultValue = "1.5", name = "PivotATRMultiple", minValue = 0.1, maxValue = 10.0, step = 0.1, category = "Advanced Features")
    @Help("Reversal threshold as multiple of ATR (e.g. 1.5 = 1.5x ATR). Used when PivotMethod=ATR Multiple")
    public double PivotATRMultiple;

    @Parameter(defaultValue = "14", name = "PivotATRPeriod", minValue = 2, maxValue = 200, step = 1, category = "Advanced Features")
    @Help("ATR period in bars for computing average true range. Used when PivotMethod=ATR Multiple")
    public int PivotATRPeriod;

    // ---- SVG/PNG Export settings (visual only — no effect on POC/VAH/VAL outputs) ----
    // Note: chart export is enabled via global Trading Options > "Store Chart Data"
    @Parameter(defaultValue = "true", name = "ShowCandlesticks", category = "Chart Display", showIfDefault = false)
    @Help("Show candlestick price chart alongside each session profile in SVG export")
    public boolean ShowCandlesticks;

    @Parameter(defaultValue = "false", name = "ShowVolumeSubchart", category = "Chart Display", showIfDefault = false)
    @Help("Show a volume bar subplot below the main chart in SVG export")
    public boolean ShowVolumeSubchart;

    @Parameter(defaultValue = "20", name = "VolumeMALength", minValue = 2, maxValue = 200, step = 1, category = "Chart Display", showIfDefault = false)
    @Help("Period for the moving average line overlaid on the volume subchart in SVG export")
    public int VolumeMALength;

    @Parameter(defaultValue = "true", name = "ShowPOCDelta", category = "Chart Display", showIfDefault = false)
    @Help("Show POC price change vs previous session in SVG export")
    public boolean ShowPOCDelta;

    @Parameter(defaultValue = "true", name = "ShowVADelta", category = "Chart Display", showIfDefault = false)
    @Help("Show Value Area midpoint change vs previous session in SVG export")
    public boolean ShowVADelta;

    @Parameter(defaultValue = "true", name = "ShowProfileRange", category = "Chart Display", showIfDefault = false)
    @Help("Show session high-low range in SVG export")
    public boolean ShowProfileRange;

    @Parameter(defaultValue = "true", name = "ShowPOCPosition", category = "Chart Display", showIfDefault = false)
    @Help("Show POC position as pct from top and bottom of profile range in SVG export")
    public boolean ShowPOCPosition;

    @Parameter(defaultValue = "false", name = "ShowDeltaPerLevel", category = "Chart Display", showIfDefault = false)
    @Help("Show per-level delta (bull minus bear) labels on each bin in SVG export")
    public boolean ShowDeltaPerLevel;

    @Parameter(defaultValue = "false", name = "ShowZigZagLine", category = "Chart Display", showIfDefault = false)
    @Help("Show the ZigZag overlay line in SVG export (only applies to Swing session types)")
    public boolean ShowZigZagLine;

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
        return SessionType;
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
    protected boolean cfgShowZigZagLine() {
        return ShowZigZagLine;
    }

    @Override
    protected int cfgPivotMethod() {
        return PivotMethod;
    }

    @Override
    protected double cfgPivotPct() {
        return PivotPct;
    }

    @Override
    protected int cfgPivotTicks() {
        return PivotTicks;
    }

    @Override
    protected double cfgPivotATRMultiple() {
        return PivotATRMultiple;
    }

    @Override
    protected int cfgPivotATRPeriod() {
        return PivotATRPeriod;
    }
    // ------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        SessionType = SQUtils.fixAllowedRange(SessionType, 1, 10, 1);
        ensureArrays();

        long curTime = Chart.Time(0);

        boolean isZigZag = (SessionType == 9 || SessionType == 10);
        boolean actualMode = (SessionType >= 5);

        if (isZigZag) {
            // ZigZag session mode
            boolean actualZZ = (SessionType == 10);
            boolean pivotFound = detectZigZagPivot();

            if (!actualZZ) {
                // Previous ZigZag: only recalculate when a new pivot is confirmed
                if (pivotFound && zzSessionStart > 0 && zzLastPivotTime > zzSessionStart) {
                    prevSessionStart = zzSessionStart;
                    prevSessionEnd = zzLastPivotTime;
                    calculateVolumeProfile();
                    zzSessionStart = zzLastPivotTime;
                }
            } else {
                // Actual ZigZag: recalculate every bar from session start to now
                if (pivotFound && zzSessionStart > 0) {
                    // Finalize previous segment first
                    prevSessionStart = zzSessionStart;
                    prevSessionEnd = zzLastPivotTime;
                    calculateVolumeProfile();
                    zzSessionStart = zzLastPivotTime;
                }
                if (zzSessionStart > 0 && curTime > zzSessionStart) {
                    prevSessionStart = zzSessionStart;
                    prevSessionEnd = curTime;
                    calculateVolumeProfile();
                }
            }
        } else if (!actualMode) {
            // PREVIOUS session mode: calculate profile only when a session completes.
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
            // ACTUAL session mode
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
     * Calculates session start and end times based on SessionType.
     * Standard 24h sessions for Daily, etc.
     */
    private void calculateSessionBoundaries(long curTime) throws TradingException {
        long dayStart = SQTime.setTime(curTime, 0, 0, 0, 0);
        // Map SessionType to a base session kind
        int baseType;
        if (SessionType == 1 || SessionType == 5)
            baseType = 1; // Daily
        else if (SessionType == 2 || SessionType == 6)
            baseType = 2; // Weekly
        else if (SessionType == 3 || SessionType == 7)
            baseType = 3; // Monthly
        else if (SessionType == 4 || SessionType == 8)
            baseType = 4; // Yearly
        else
            baseType = 1;

        switch (baseType) {
            case 0: // Hourly
                currentSessionStart = SQTime.setTime(curTime, SQTime.getHour(curTime), 0, 0, 0);
                currentSessionEnd = SQTime.addHours(currentSessionStart, 1);
                break;

            case 1: // Daily - Standard full day
                currentSessionStart = dayStart;
                currentSessionEnd = SQTime.addDays(dayStart, 1);
                // Skip Sunday: if current day is Sunday, shift session to Friday
                if (isSunday(currentSessionStart)) {
                    currentSessionStart = SQTime.addDays(currentSessionStart, -2); // Friday
                    currentSessionEnd = SQTime.addDays(currentSessionStart, 1); // Saturday 00:00
                }
                break;
            case 2: // Weekly (Mon..Mon)
                currentSessionStart = SQTime.setDayOfWeek(dayStart, SQTime.MONDAY);
                if (currentSessionStart > dayStart) {
                    currentSessionStart = SQTime.addDays(currentSessionStart, -7);
                }
                currentSessionEnd = SQTime.addDays(currentSessionStart, 7);
                // Log.info("currentSessionStart : {}, currentSessionEnd: {}",
                // SQTime.toDateMinuteString(currentSessionStart),
                // SQTime.toDateMinuteString(currentSessionEnd));
                break;
            case 3: // Monthly
                currentSessionStart = SQTime.setDayOfMonth(dayStart, 1);
                currentSessionEnd = SQTime.addMonths(currentSessionStart, 1);
                break;

            case 4: // Yearly
                currentSessionStart = SQTime.setTime(
                        SQTime.setDayOfMonth(SQTime.setMonthOfYear(curTime, 1), 1), 0, 0, 0, 0);
                currentSessionEnd = SQTime.addYears(currentSessionStart, 1);
                break;

            default:
                currentSessionStart = dayStart;
                currentSessionEnd = SQTime.addDays(dayStart, 1);
        }

    }
}
