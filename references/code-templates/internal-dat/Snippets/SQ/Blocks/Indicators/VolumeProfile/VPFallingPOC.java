package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name = "(VP22) Falling POC (Short Regime)", display = "FallingPOC(@Chart@#LookbackBars#,#MinimumShift#)", returnType = ReturnTypes.Boolean)
@Help("POC migration downward: current session POC is lower than the POC from LookbackBars ago by at least MinimumShift pips. Falling fair value across sessions suggests a bearish regime—prefer short setups on rallies to prior POC/value levels.")
@OppositeBlock("VPRisingPOC")
@ForEngine("MT4,MT5,TS,MC")
public class VPFallingPOC extends ConditionBlock {

    @Parameter
    public ChartData Chart;

    @Parameter(defaultValue = "Current", showIfDefault = false)
    @Editor(type = Editors.SelectionSymbols)
    public String Symbol;

    @Parameter(name = "Lookback Bars", defaultValue = "48", minValue = 1, maxValue = 500, step = 1)
    @Help("Number of bars to look back for the prior POC value. Set to approximately one session worth of bars (e.g., 48 for M30 daily, 96 for M15 daily).")
    public int LookbackBars;

    @Parameter(name = "Minimum Shift", defaultValue = "5", minValue = 1, maxValue = 1000, step = 1)
    @Help("Minimum POC decrease (in pips) to qualify as a falling POC")
    public int MinimumShift;

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
        double minShift = Strategy.convertPipsToRealPrice(Symbol, MinimumShift);

        VolumeProfile indicator = Strategy.Indicators.VolumeProfile(Chart, SessionType, ProfileRows, 2, 3, ValueAreaPct,
                0, 5, 20, 40, false, false, 6.0, 2, 1, 2.0, 50, 1.5, 14, true, false, 20, true, true, true, true, false, false);

        double currentPOC = indicator.POC.getRounded(Shift);
        double priorPOC = indicator.POC.getRounded(Shift + LookbackBars);

        // POC has fallen by at least MinimumShift
        return currentPOC < priorPOC - minShift;
    }

}
