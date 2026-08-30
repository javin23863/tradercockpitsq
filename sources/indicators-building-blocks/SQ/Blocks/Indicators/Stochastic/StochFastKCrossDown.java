package SQ.Blocks.Indicators.Stochastic;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "Stochastic.Fast %K crosses below Level",
   display = "Stoch(@Chart@#KPeriod#, #DPeriod#, #Slowing#).Fast %K[#Shift#] crosses below #Level#",
   returnType = 3
)
@SortOrder(600)
@OppositeBlock(value = "StochFastKCrossUp", oscillator = true, middleValue = 50.0, field = "Level")
@ParameterSets(
   {
         @ParameterSet(set = "KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0"),
         @ParameterSet(set = "KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0"),
         @ParameterSet(set = "KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0"),
         @ParameterSet(set = "KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0,Level=20"),
         @ParameterSet(set = "KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0,Level=20"),
         @ParameterSet(set = "KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0,Level=20"),
         @ParameterSet(set = "KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0,Level=80"),
         @ParameterSet(set = "KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0,Level=80"),
         @ParameterSet(set = "KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0,Level=80")
   }
)
public class StochFastKCrossDown extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(name = "%K Period", defaultValue = "9", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int KPeriod;
   @Parameter(name = "%D Period", defaultValue = "3", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int DPeriod;
   @Parameter(defaultValue = "3", minValue = 2.0, isPeriod = true, maxValue = 10000.0, step = 1.0)
   public int Slowing;
   @Parameter(name = "MA Method", defaultValue = "0")
   @Editor(type = 40, values = "Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
   public int MAMethod;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Low/High=0,Close/Close=1")
   public int PriceField;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 100.0, step = 0.5)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Stochastic var1 = this.Strategy.Indicators.Stochastic(this.Input, this.KPeriod, this.DPeriod, this.Slowing, this.MAMethod, this.PriceField);
      double var2 = var1.FastK.getRounded(this.Shift + 1);
      double var4 = var1.FastK.getRounded(this.Shift);
      return var2 > this.Level && var4 < this.Level;
   }
}
