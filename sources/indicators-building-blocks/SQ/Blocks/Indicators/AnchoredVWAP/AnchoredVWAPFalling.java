package SQ.Blocks.Indicators.AnchoredVWAP;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(AVWAPF) Anchored VWAP is falling", display = "AnchoredVWAP(@Chart@#SessionType#,#StdDevMult#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if Anchored VWAP is falling")
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("AnchoredVWAPRising")
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=80"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=96"),
         @ParameterSet(set = "Period=120")
   }
)
public class AnchoredVWAPFalling extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1", name = "SessionType")
   @Help("Anchor point for VWAP calculation")
   @Editor(type = 40, values = "Daily=1,Weekly=2,Monthly=3")
   public int SessionType;
   @Parameter(defaultValue = "1.0", name = "StdDevMult", minValue = 0.5, maxValue = 4.0, step = 0.1)
   @Help("Standard deviation multiplier for bands")
   public double StdDevMult;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AnchoredVWAP var1 = this.Strategy.Indicators.AnchoredVWAP(this.Chart, this.SessionType, this.StdDevMult);
      double var2 = var1.VWAP.getRounded(this.Shift);
      double var4 = var1.VWAP.getRounded(this.Shift + 1);
      return var2 < var4;
   }
}
