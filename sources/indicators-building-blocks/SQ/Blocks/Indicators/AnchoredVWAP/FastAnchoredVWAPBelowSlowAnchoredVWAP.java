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

@BuildingBlock(
   name = "(DVWAPD) Faster Anchored VWAP is below Slower Anchored VWAP",
   display = "Faster AnchoredVWAP(@Chart@#FastSessionType#,#FastStdDevMult#)[#Shift#] is below Slower AnchoredVWAP(@Chart@#SlowSessionType#,#SlowStdDevMult#)[#Shift#]",
   returnType = 3
)
@Help("Is triggered if Faster Anchored VWAP is below Slower Anchored VWAP")
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("FastAnchoredVWAPAboveSlowAnchoredVWAP")
@ParameterSets(
   {
         @ParameterSet(set = "FastPeriod=10,SlowPeriod=20"),
         @ParameterSet(set = "FastPeriod=20,SlowPeriod=40"),
         @ParameterSet(set = "FastPeriod=40,SlowPeriod=80"),
         @ParameterSet(set = "FastPeriod=100,SlowPeriod=200"),
         @ParameterSet(set = "FastPeriod=12,SlowPeriod=24"),
         @ParameterSet(set = "FastPeriod=24,SlowPeriod=48"),
         @ParameterSet(set = "FastPeriod=48,SlowPeriod=96"),
         @ParameterSet(set = "FastPeriod=120,SlowPeriod=240")
   }
)
public class FastAnchoredVWAPBelowSlowAnchoredVWAP extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1", name = "SessionType")
   @Help("Anchor point for VWAP calculation")
   @Editor(type = 40, values = "Daily=1,Weekly=2,Monthly=3")
   public int FastSessionType;
   @Parameter(defaultValue = "1.0", name = "StdDevMult", minValue = 0.5, maxValue = 4.0, step = 0.1)
   @Help("Standard deviation multiplier for bands")
   public double FastStdDevMult;
   @Parameter(defaultValue = "1", name = "SessionType")
   @Help("Anchor point for VWAP calculation")
   @Editor(type = 40, values = "Daily=1,Weekly=2,Monthly=3")
   public int SlowSessionType;
   @Parameter(defaultValue = "1.0", name = "StdDevMult", minValue = 0.5, maxValue = 4.0, step = 0.1)
   @Help("Standard deviation multiplier for bands")
   public double SlowStdDevMult;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AnchoredVWAP var1 = this.Strategy.Indicators.AnchoredVWAP(this.Chart, this.FastSessionType, this.FastStdDevMult);
      AnchoredVWAP var2 = this.Strategy.Indicators.AnchoredVWAP(this.Chart, this.SlowSessionType, this.SlowStdDevMult);
      double var3 = var1.VWAP.getRounded(this.Shift);
      double var5 = var2.VWAP.getRounded(this.Shift);
      return var3 < var5;
   }
}
