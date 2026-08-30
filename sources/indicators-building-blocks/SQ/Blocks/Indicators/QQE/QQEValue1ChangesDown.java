package SQ.Blocks.Indicators.QQE;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(
   name = "QQE Value1 line changes direction downwards",
   display = "QQE(@Chart@#RSIPeriod#).Value1[#Shift#] changes direction downwards",
   returnType = 3
)
@Help("Is triggered if QQE Value1 changes direction downwards")
@SortOrder(100)
@OppositeBlock("QQEValue1ChangesUp")
@ParameterSet(set = "RSIPeriod=14,sF=5,wF=4.236")
public class QQEValue1ChangesDown extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "RSIPeriod", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0, isPeriod = true)
   public int RSIPeriod;
   @Parameter(
      category = "Default",
      name = "sF",
      defaultValue = "5",
      minValue = 2.0,
      maxValue = 650.0,
      step = 1.0,
      builderMinValue = 1.0,
      builderMaxValue = 650.0,
      builderStep = 1.0,
      isPeriod = true
   )
   public int sF;
   @Parameter(
      category = "Default",
      name = "wF",
      defaultValue = "4.236",
      minValue = 0.1,
      maxValue = 100.0,
      builderMinValue = 0.01,
      builderMaxValue = 100.0,
      builderStep = 0.025
   )
   public double wF;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      QQE var1 = this.Strategy.Indicators.QQE(this.Chart, this.RSIPeriod, this.sF, this.wF);
      double var2 = var1.Value1.getRounded(this.Shift + 2);
      double var4 = var1.Value1.getRounded(this.Shift + 1);
      double var6 = var1.Value1.getRounded(this.Shift);
      return var2 < var4 && var4 > var6;
   }
}
