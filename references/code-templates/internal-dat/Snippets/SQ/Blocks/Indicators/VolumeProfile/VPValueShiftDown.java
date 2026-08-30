package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name = "(VP24) Value Shift Down (Bear Regime)", display = "ValueShiftDown(@Chart@#LookbackBars#,#MinimumGap#)", returnType = ReturnTypes.Boolean)
@Help("Strong bearish regime: current session's VAH sits entirely below the prior session's VAL. The entire value area has shifted lower, indicating aggressive acceptance of lower prices. Prefer short setups—sell rallies into the new value area.")
@OppositeBlock("VPValueShiftUp")
@ForEngine("MT4,MT5,TS,MC")
public class VPValueShiftDown extends ConditionBlock {

    @Parameter
    public ChartData Chart;

    @Parameter(defaultValue = "Current", showIfDefault = false)
    @Editor(type = Editors.SelectionSymbols)
    public String Symbol;

    @Parameter(name = "Lookback Bars", defaultValue = "48", minValue = 1, maxValue = 500, step = 1)
    @Help("Number of bars to look back for the prior session VP levels. Set to approximately one session worth of bars (e.g., 48 for M30 daily).")
    public int LookbackBars;

    @Parameter(name = "Minimum Gap", defaultValue = "0", minValue = 0, maxValue = 1000, step = 1)
    @Help("Minimum gap (in pips) between prior VAL and current VAH to trigger the signal. Set to 0 for any overlap-free shift.")
    public int MinimumGap;

    // *****************************************************************************************************************************************************

    @Parameter(defaultValue = "1", name = "SessionType")
    @Help("Session selection: Previous/Actual Day/Week/Month/Year for profile calculation")
    @Editor(type = Editors.Selection, values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8")
    public int SessionType;

    @Parameter(defaultValue = "50", name = "ProfileRows", minValue = 10, maxValue = 500, step = 10)
    @Help("Number of price levels (bins) in the profile")
    public int ProfileRows;

    @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30, maxValue = 95, step = 5)
    @Help("Percentage of total volume that defines the Value Area")
    public double ValueAreaPct;


    // *****************************************************************************************************************************************************

    @Parameter
    public int Shift;

    // ------------------------------------------------------------------------

    @Override
    protected void OnInit() throws BlockDefinitionException {
    }

    @Override
    public boolean OnBlockEvaluate() throws TradingException {
        return _evaluateSignal();
    }

    private boolean _evaluateSignal() throws TradingException {
        double minGap = Strategy.convertPipsToRealPrice(Symbol, MinimumGap);

        VolumeProfile indicator = Strategy.Indicators.VolumeProfile(Chart, SessionType, ProfileRows, 2, 3, ValueAreaPct,
                0, 5, 20, 40, false, false, 6.0, 2, 1, 2.0, 50, 1.5, 14, true, false, 20, true, true, true, true, false, false);

        // Current session values (most recent computed session)
        double currentVAH = indicator.VAH.getRounded(Shift);
        // Prior session values (look back far enough to be in the previous session)
        double priorVAL = indicator.VAL.getRounded(Shift + LookbackBars);

        // Current VAH sits below prior VAL (value shifted entirely lower)
        return currentVAH < priorVAL - minGap;
    }

}
