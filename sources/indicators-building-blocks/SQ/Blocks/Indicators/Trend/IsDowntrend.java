package SQ.Blocks.Indicators.Trend;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "IsDowntrend(@Chart@#Method#)", returnType = 3)
@Help("Is true when there is an downtrend identified by the given method.")
@SortOrder(100)
@OppositeBlock("IsUptrend")
public class IsDowntrend extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(name = "Method", defaultValue = "0")
   @Editor(type = 40, values = "Price below SMA200=0")
   public int Method;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Strategy.Indicators.SMA(this.Chart.getSeries(0), 200).Value.getRounded(1);
      double var3 = this.Chart.Close(1);
      return var3 < var1;
   }
}
