package SQ.Blocks.Indicators.BollingerBands;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(BB) Bar closes above Upper band", display = "Bar closes above BB(@Chart@#Period#, #Deviation#).Upper[#Shift#]", returnType = 3)
@OppositeBlock("BBBarClosesBelowDown")
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=10,Deviation=1.9,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,Deviation=2.1,ComputedFrom=0")
   }
)
public class BBBarClosesAboveUp extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "2", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Strategy.Indicators.BollingerBands(this.Chart.getSeries(this.ComputedFrom), this.Period, this.Deviation).Upper.getRounded(this.Shift);
      return this.Chart.Close(this.Shift) > var1;
   }
}
