package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(VP23) Value Shift Up (Bull Regime)", display = "ValueShiftUp(@Chart@#LookbackBars#,#MinimumGap#)", returnType = 3)
@Help(
   "Strong bullish regime: current session's VAL sits entirely above the prior session's VAH. The entire value area has shifted higher, indicating aggressive acceptance of higher prices. Prefer long setups—buy pullbacks into the new value area."
)
@OppositeBlock("VPValueShiftDown")
@ForEngine("MT4,MT5,TS,MC")
public class VPValueShiftUp extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "Current", showIfDefault = false)
   @Editor(type = 60)
   public String Symbol;
   @Parameter(name = "Lookback Bars", defaultValue = "48", minValue = 1.0, maxValue = 500.0, step = 1.0)
   @Help("Number of bars to look back for the prior session VP levels. Set to approximately one session worth of bars (e.g., 48 for M30 daily).")
   public int LookbackBars;
   @Parameter(name = "Minimum Gap", defaultValue = "0", minValue = 0.0, maxValue = 1000.0, step = 1.0)
   @Help("Minimum gap (in pips) between current VAL and prior VAH to trigger the signal. Set to 0 for any overlap-free shift.")
   public int MinimumGap;
   @Parameter(defaultValue = "1", name = "SessionType")
   @Help("Session selection: Previous/Actual Day/Week/Month/Year for profile calculation")
   @Editor(type = 40, values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8")
   public int SessionType;
   @Parameter(defaultValue = "50", name = "ProfileRows", minValue = 10.0, maxValue = 500.0, step = 10.0)
   @Help("Number of price levels (bins) in the profile")
   public int ProfileRows;
   @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30.0, maxValue = 95.0, step = 5.0)
   @Help("Percentage of total volume that defines the Value Area")
   public double ValueAreaPct;
   @Parameter
   public int Shift;

   @Override
   protected void OnInit() throws BlockDefinitionException {
   }

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return this._evaluateSignal();
   }

   private boolean _evaluateSignal() throws TradingException {
      double var1 = this.Strategy.convertPipsToRealPrice(this.Symbol, this.MinimumGap);
      VolumeProfile var3 = this.Strategy
         .Indicators
         .VolumeProfile(
            this.Chart,
            this.SessionType,
            this.ProfileRows,
            2,
            3,
            this.ValueAreaPct,
            0,
            5,
            20,
            40,
            false,
            false,
            6.0,
            2,
            1,
            2.0,
            50,
            1.5,
            14,
            true,
            false,
            20,
            true,
            true,
            true,
            true,
            false,
            false
         );
      double var4 = var3.VAL.getRounded(this.Shift);
      double var6 = var3.VAH.getRounded(this.Shift + this.LookbackBars);
      return var4 > var6 + var1;
   }
}
