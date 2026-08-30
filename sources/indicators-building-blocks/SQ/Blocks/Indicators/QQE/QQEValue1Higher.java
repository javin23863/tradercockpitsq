package SQ.Blocks.Indicators.QQE;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "QQE Value1 line is higher than Level", display = "QQE(@Chart@#RSIPeriod#).Value1[#Shift#] > #Level#", returnType = 3)
@SortOrder(300)
@OppositeBlock(value = "QQEValue1Lower", oscillator = true, middleValue = 50.0, field = "Level")
@ParameterSet(set = "RSIPeriod=14,sF=5,wF=4.236")
public class QQEValue1Higher extends ConditionBlock {
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
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 100.0, step = 5.0, builderMinValue = 0.0, builderMaxValue = 100.0, builderStep = 0.25)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      QQE var1 = this.Strategy.Indicators.QQE(this.Chart, this.RSIPeriod, this.sF, this.wF);
      double var2 = var1.Value1.getRounded(this.Shift);
      return var2 > this.Level;
   }
}
