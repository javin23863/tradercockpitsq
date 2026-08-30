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
package SQ.Blocks.Indicators.TPOProfile;

import SQ.Internal.TPOProfileIndicatorChart;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


/**
 * Volume Profile indicator using main chart data.
 * 
 * SETUP:
 * - Main chart can be any timeframe (M1, M30, H1, etc.)
 * - Profile is calculated from main chart bars
 * 
 * Algorithm:
 * - On session change, scans chart backwards to collect previous session
 * data
 * - Uses histogram binning with fixed number of bins (ProfileRows)
 * - O(n) scan where n = bars in session
 */
@BuildingBlock(name = "(TPO) Market Profile", display = "TPO_Letters(#SessionType#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@Help("TPO (Market Profile). Outputs POC/VAH/VAL based on Time Price Opportunities.")
@ParameterSet(set = "SessionType=1,ProfileRows=50,ValueAreaPct=70")
@ParameterSet(set = "SessionType=2,ProfileRows=100,ValueAreaPct=70")
public class TPOProfile extends TPOProfileIndicatorChart {

    @Parameter(defaultChartIndex = 0, category = "General")
    public ChartData Chart;

    // public ChartData M1Chart;

    @Parameter(defaultValue = "1", name = "SessionType", category = "General")
    @Help("Session selection: Previous/Actual Day/Week/Month/Year for profile calculation")
    @Editor(type = Editors.Selection, values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8")
    public int SessionType;

    @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10, maxValue = 500, step = 10, category = "Profile Calculation")
    @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
    public int ProfileRows;

    @Parameter(defaultValue = "2", name = "BinSizeMode", category = "Profile Calculation")
    @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
    @Editor(type = Editors.Selection, values = "Range-Based=1,Fixed Tick Size=2")
    public int BinSizeMode;

    @Parameter(defaultValue = "1", name = "TicksPerBin", minValue = 1, maxValue = 1000, step = 1, category = "Profile Calculation")
    @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
    public int TicksPerBin;

    @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30, maxValue = 95, step = 5, category = "Profile Calculation")
    @Help("Percentage of total volume that defines the Value Area")
    public double ValueAreaPct;

    @Parameter(defaultValue = "false", name = "UseCustomHours", category = "Session / Time")
    @Help("If true (Daily session only), session is StartHour:StartMinute -> EndHour:EndMinute (can cross midnight). Useful for RTH-only profiles.")
    public boolean UseCustomHours;

    @Parameter(defaultValue = "0", name = "StartHour", minValue = 0, maxValue = 23, step = 1, category = "Session / Time")
    public int StartHour;

    @Parameter(defaultValue = "0", name = "StartMinute", minValue = 0, maxValue = 59, step = 1, category = "Session / Time")
    public int StartMinute;

    @Parameter(defaultValue = "23", name = "EndHour", minValue = 0, maxValue = 23, step = 1, category = "Session / Time")
    public int EndHour;

    @Parameter(defaultValue = "59", name = "EndMinute", minValue = 0, maxValue = 59, step = 1, category = "Session / Time")
    public int EndMinute;

    @Parameter(defaultValue = "30", name = "BracketMinDaily", minValue = 1, maxValue = 120, step = 1, category = "Time Structure")
    @Help("TPO bracket size in minutes for Daily sessions")
    public int BracketMinDaily;

    @Parameter(defaultValue = "240", name = "BracketMinWeekly", minValue = 1, maxValue = 10000, step = 1, category = "Time Structure")
    @Help("TPO bracket size in minutes for Weekly sessions")
    public int BracketMinWeekly;

    @Parameter(defaultValue = "720", name = "BracketMinMonthly", minValue = 1, maxValue = 10000, step = 1, category = "Time Structure")
    @Help("TPO bracket size in minutes for Monthly sessions")
    public int BracketMinMonthly;

    @Parameter(defaultValue = "720", name = "BracketMinYearly", minValue = 1, maxValue = 10000, step = 1, category = "Time Structure")
    @Help("TPO bracket size in minutes for Yearly sessions")
    public int BracketMinYearly;

    // ---- SVG/PNG Export settings (visual only — no effect on TPO_POC/VAH/VAL outputs) ----
    // Note: chart export is enabled via global Trading Options > "Store Chart Data"
    @Parameter(defaultValue = "true", name = "ShowCandlesticks", category = "Chart Display", showIfDefault = false)
    @Help("Show candlestick price chart alongside each session profile in SVG export")
    public boolean ShowCandlesticks;

    @Parameter(defaultValue = "true", name = "ShowShapeLabel", category = "Chart Display", showIfDefault = false)
    @Help("Annotate detected profile shape (P/b/D/DD) on each session panel in SVG export")
    public boolean ShowShapeLabel;

    @Parameter(defaultValue = "false", name = "UseBlockMode", category = "Chart Display", showIfDefault = false)
    @Help("Draw TPO profile as colored blocks instead of letters in SVG export")
    public boolean UseBlockMode;

    @Output(name = "TPO_POC", color = Colors.Orange)
    public DataSeries TPO_POC;

    @Output(name = "TPO_VAH", color = Colors.LightGreen)
    public DataSeries TPO_VAH;

    @Output(name = "TPO_VAL", color = Colors.Pink)
    public DataSeries TPO_VAL;

    @Output(name = "TPO_PoorHigh", color = Colors.Red, show = false)
    public DataSeries TPO_PoorHigh;

    @Output(name = "TPO_PoorLow", color = Colors.Red, show = false)
    public DataSeries TPO_PoorLow;

    @Output(name = "TPO_ExcessHigh", color = Colors.Green, show = false)
    public DataSeries TPO_ExcessHigh;

    @Output(name = "TPO_ExcessLow", color = Colors.Green, show = false)
    public DataSeries TPO_ExcessLow;

    @Output(name = "TPO_SinglePrints", color = Colors.Gray, show = false)
    public DataSeries TPO_SinglePrints;

    @Output(name = "TPO_BuyingTailLen", color = Colors.LightGreen, show = false)
    public DataSeries TPO_BuyingTailLen;

    @Output(name = "TPO_SellingTailLen", color = Colors.Pink, show = false)
    public DataSeries TPO_SellingTailLen;

    @Output(name = "TPO_ProfileShape", color = Colors.Yellow, show = false)
    public DataSeries TPO_ProfileShape;

    @Output(name = "TPO_LedgeUp", color = Colors.LightGreen, show = false)
    public DataSeries TPO_LedgeUp;

    @Output(name = "TPO_LedgeDown", color = Colors.Pink, show = false)
    public DataSeries TPO_LedgeDown;

    @Output(name = "TPO_IBH", color = Colors.Azure, show = false)
    public DataSeries TPO_IBH;

    @Output(name = "TPO_IBL", color = Colors.Magenta, show = false)
    public DataSeries TPO_IBL;

    @Output(name = "TPO_VPOC", color = Colors.Azure, show = false)
    public DataSeries TPO_VPOC;

    @Output(name = "TPO_VVAH", color = Colors.LightGreen, show = false)
    public DataSeries TPO_VVAH;

    @Output(name = "TPO_VVAL", color = Colors.Beige, show = false)
    public DataSeries TPO_VVAL;

    @Output(name = "TPO_BullPOC", color = Colors.Green, show = false)
    public DataSeries TPO_BullPOC;

    @Output(name = "TPO_BearPOC", color = Colors.Red, show = false)
    public DataSeries TPO_BearPOC;

    @Output(name = "TPO_TotalVolume", color = Colors.Gray, show = false)
    public DataSeries TPO_TotalVolume;

    @Output(name = "TPO_TotalBullVolume", color = Colors.Green, show = false)
    public DataSeries TPO_TotalBullVolume;

    @Output(name = "TPO_TotalBearVolume", color = Colors.Red, show = false)
    public DataSeries TPO_TotalBearVolume;

    // Session tracking
    // ------------------------------------------------------------------------
    // Getter implementations for TPOProfileIndicatorChart abstract methods
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
    protected int cfgIBMinutes() {
        return 0;
    } // TPO uses bracket-based IB

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
        return false;
    }

    @Override
    protected int cfgVolumeMALength() {
        return 20;
    }

    @Override
    protected boolean cfgShowPOCDelta() {
        return false;
    }

    @Override
    protected boolean cfgShowVADelta() {
        return false;
    }

    @Override
    protected boolean cfgShowProfileRange() {
        return false;
    }

    @Override
    protected boolean cfgShowPOCPosition() {
        return false;
    }

    @Override
    protected int cfgBracketMinDaily() {
        return BracketMinDaily;
    }

    @Override
    protected int cfgBracketMinWeekly() {
        return BracketMinWeekly;
    }

    @Override
    protected int cfgBracketMinMonthly() {
        return BracketMinMonthly;
    }

    @Override
    protected int cfgBracketMinYearly() {
        return BracketMinYearly;
    }

    @Override
    protected boolean cfgShowShapeLabel() {
        return ShowShapeLabel;
    }

    @Override
    protected boolean cfgUseBlockMode() {
        return UseBlockMode;
    }
    // ------------------------------------------------------------------------

    @Override
    protected void OnBarUpdate() throws TradingException {
        SessionType = SQUtils.fixAllowedRange(SessionType, 1, 8, 1);
        ensureArrays();

        long curTime = Chart.Time(0);

        //debug("VP", "=== OnBarUpdate === CurrentBar: " + CurrentBar + ", Time: " + SQTime.toFullDateTimeString(curTime));
        //debug("VP", "currentSessionEnd: " + SQTime.toFullDateTimeString(currentSessionEnd) + ", curTime >= currentSessionEnd: " + (curTime >= currentSessionEnd));

        boolean actualMode = (SessionType >= 5 && SessionType <= 8);

        if (!actualMode) {
            if (currentSessionEnd == 0 || curTime >= currentSessionEnd) {
                debug("VP", "*** NEW SESSION DETECTED ***");
                prevSessionStart = currentSessionStart;
                prevSessionEnd = currentSessionEnd;

                debug("VP", "Saved prev session: Start=" + SQTime.toFullDateTimeString(prevSessionStart) + ", End="
                        + SQTime.toFullDateTimeString(prevSessionEnd));

                calculateSessionBoundaries(curTime);

                if (prevSessionStart > 0 && isSunday(prevSessionStart)) {
                    prevSessionStart = SQTime.addDays(SQTime.setTime(prevSessionStart, 0, 0, 0, 0), -2);
                    prevSessionEnd = SQTime.addDays(prevSessionStart, 1);
                }
                if (prevSessionStart > 0 && isSunday(prevSessionStart)) {
                    prevSessionStart = SQTime.addDays(SQTime.setTime(prevSessionStart, 0, 0, 0, 0), -2);
                    prevSessionEnd = SQTime.addDays(prevSessionStart, 1);
                }

                debug("VP", "New session boundaries: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                        + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));

                if (prevSessionStart > 0 && prevSessionEnd > 0) {
                    debug("VP", "Calling calculateTPOProfile()...");
                    calculateTPOProfile();
                    debug("VP",
                            "Profile calculated: POC=" + prevTPO_POC + ", VAH=" + prevTPO_VAH + ", VAL=" + prevTPO_VAL);
                } else {
                    debug("VP", "Skipping profile calc - no valid prev session boundaries");
                }
            }
        } else {
            if (currentSessionEnd == 0 || curTime >= currentSessionEnd) {
                calculateSessionBoundaries(curTime);
            }
            prevSessionStart = currentSessionStart;
            prevSessionEnd = Math.min(currentSessionEnd, curTime);

            if (prevSessionStart > 0 && prevSessionEnd > prevSessionStart) {
                calculateTPOProfile();
            }
        }

        TPO_POC.set(0, prevTPO_POC);
        TPO_VAH.set(0, prevTPO_VAH);
        TPO_VAL.set(0, prevTPO_VAL);
        TPO_PoorHigh.set(0, lastPoorHigh ? 1.0 : 0.0);
        TPO_PoorLow.set(0, lastPoorLow ? 1.0 : 0.0);
        TPO_ExcessHigh.set(0, lastExcessHigh ? 1.0 : 0.0);
        TPO_ExcessLow.set(0, lastExcessLow ? 1.0 : 0.0);
        TPO_SinglePrints.set(0, lastSinglePrintCount);
        TPO_BuyingTailLen.set(0, lastBuyingTailLen);
        TPO_SellingTailLen.set(0, lastSellingTailLen);
        TPO_ProfileShape.set(0, lastProfileShape);
        TPO_LedgeUp.set(0, lastLedgeUpPrice);
        TPO_LedgeDown.set(0, lastLedgeDownPrice);
        TPO_IBH.set(0, prevIBH);
        TPO_IBL.set(0, prevIBL);
        TPO_VPOC.set(0, prevTPO_VPOC);
        TPO_VVAH.set(0, prevTPO_VVAH);
        TPO_VVAL.set(0, prevTPO_VVAL);
        TPO_BullPOC.set(0, prevTPO_BullPOC);
        TPO_BearPOC.set(0, prevTPO_BearPOC);
        TPO_TotalVolume.set(0, prevTotalVolume);
        TPO_TotalBullVolume.set(0, prevTotalBullVolume);
        TPO_TotalBearVolume.set(0, prevTotalBearVolume);

        //debug("VP", "Output set: TPO_POC=" + prevTPO_POC + ", TPO_VAH=" + prevTPO_VAH + ", TPO_VAL=" + prevTPO_VAL);
    }

    // ------------------------------------------------------------------------

    /**
     * Calculates session start and end times based on SessionType.
     */
    private void calculateSessionBoundaries(long curTime) throws TradingException {
        long dayStart = SQTime.setTime(curTime, 0, 0, 0, 0);

        // Map SessionType to a base session kind (D/W/M/Y), regardless of
        // Previous/Actual mode.
        // 1=PrevDay,2=PrevWeek,3=PrevMonth,4=PrevYear,5=ActDay,6=ActWeek,7=ActMonth,8=ActYear
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

        debug("VP", "calculateSessionBoundaries: curTime=" + SQTime.toFullDateTimeString(curTime)
                + ", dayStart=" + SQTime.toFullDateTimeString(dayStart)
                + ", SessionType=" + SessionType + ", baseType=" + baseType);

        switch (baseType) {
            case 1: // Daily
                if (UseCustomHours) {
                    // Daily custom session hours (RTH/ETH style). Can cross midnight.
                    long start = SQTime.setTime(dayStart, StartHour, StartMinute, 0, 0);
                    long end = SQTime.setTime(dayStart, EndHour, EndMinute, 0, 0);

                    if (end <= start) {
                        // crosses midnight
                        end = SQTime.addDays(end, 1);
                    }

                    // If we're before today's start, we are still in yesterday's session.
                    if (curTime < start) {
                        start = SQTime.addDays(start, -1);
                        end = SQTime.addDays(end, -1);
                    }

                    currentSessionStart = start;
                    currentSessionEnd = end;

                    debug("VP", "Daily CUSTOM session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                            + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
                } else {
                    currentSessionStart = dayStart;
                    currentSessionEnd = SQTime.addDays(dayStart, 1);
                    // Skip Sunday: if current day is Sunday, shift session to Friday
                    if (isSunday(currentSessionStart)) {
                        currentSessionStart = SQTime.addDays(currentSessionStart, -2); // Friday
                        currentSessionEnd = SQTime.addDays(currentSessionStart, 1); // Saturday 00:00
                    }
                    debug("VP", "Daily session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                            + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
                }
                break;

            case 2: // Weekly (Mon..Mon)
                currentSessionStart = SQTime.setDayOfWeek(dayStart, SQTime.MONDAY);
                if (currentSessionStart > dayStart) {
                    currentSessionStart = SQTime.addDays(currentSessionStart, -7);
                }
                currentSessionEnd = SQTime.addDays(currentSessionStart, 7);
                debug("VP", "Weekly session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                        + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
                break;

            case 3: // Monthly
                currentSessionStart = SQTime.setDayOfMonth(dayStart, 1);
                currentSessionEnd = SQTime.addMonths(currentSessionStart, 1);
                debug("VP", "Monthly session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                        + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
                break;

            case 4: // Yearly
                currentSessionStart = SQTime.setTime(
                        SQTime.setDayOfMonth(SQTime.setMonthOfYear(curTime, 1), 1), 0, 0, 0, 0);
                currentSessionEnd = SQTime.addYears(currentSessionStart, 1);

                debug("VP", "Yearly session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                        + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
                break;

            default:
                currentSessionStart = dayStart;
                currentSessionEnd = SQTime.addDays(dayStart, 1);
                debug("VP", "Default (daily) session: Start=" + SQTime.toFullDateTimeString(currentSessionStart)
                        + ", End=" + SQTime.toFullDateTimeString(currentSessionEnd));
        }
    }
}
