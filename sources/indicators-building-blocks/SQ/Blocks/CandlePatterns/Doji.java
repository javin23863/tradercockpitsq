package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Doji pattern", display = "Doji pattern(@Chart@) before #Shift# bars", returnType = 3)
@Help("Is triggered when Doji pattern is formed")
@SortOrder(100)
@CategoryOrder(200)
public class Doji extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1")
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = SQUtils.round(Math.abs(this.Chart.Open(this.Shift) - this.Chart.Close(this.Shift)), this.Chart.getInstrumentInfo().decimals);
      double var3 = SQUtils.round(0.6 * this.Chart.getInstrumentInfo().tickSize, this.Chart.getInstrumentInfo().decimals);
      return var1 < var3;
   }
}
