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

@BuildingBlock(name = "(VP22) Falling POC (Short Regime)", display = "FallingPOC(@Chart@#LookbackBars#,#MinimumShift#)", returnType = 3)
@Help(
   "POC migration downward: current session POC is lower than the POC from LookbackBars ago by at least MinimumShift pips. Falling fair value across sessions suggests a bearish regime—prefer short setups on rallies to prior POC/value levels."
)
@OppositeBlock("VPRisingPOC")
@ForEngine("MT4,MT5,TS,MC")
public class VPFallingPOC extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "Current", showIfDefault = false)
   @Editor(type = 60)
   public String Symbol;
   @Parameter(name = "Lookback Bars", defaultValue = "48", minValue = 1.0, maxValue = 500.0, step = 1.0)
   @Help("Number of bars to look back for the prior POC value. Set to approximately one session worth of bars (e.g., 48 for M30 daily, 96 for M15 daily).")
   public int LookbackBars;
   @Parameter(name = "Minimum Shift", defaultValue = "5", minValue = 1.0, maxValue = 1000.0, step = 1.0)
   @Help("Minimum POC decrease (in pips) to qualify as a falling POC")
   public int MinimumShift;
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
      double var1 = this.Strategy.convertPipsToRealPrice(this.Symbol, this.MinimumShift);
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
      double var4 = var3.POC.getRounded(this.Shift);
      double var6 = var3.POC.getRounded(this.Shift + this.LookbackBars);
      return var4 < var6 - var1;
   }
}
