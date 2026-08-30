package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "BearishEngulfing pattern", display = "BearishEngulfing pattern(@Chart@) before #Shift# bars", returnType = 3)
@Help("Is triggered when BearishEngulfing pattern is formed")
@OppositeBlock("BullishEngulfing")
@SortOrder(400)
public class BearishEngulfing extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1")
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Chart.Open(this.Shift);
      double var3 = this.Chart.Open(this.Shift + 1);
      double var5 = this.Chart.Close(this.Shift);
      double var7 = this.Chart.Close(this.Shift + 1);
      return var7 > var3 && var1 > var5 && var1 >= var7 && var3 >= var5 && var1 - var5 > var7 - var3;
   }
}
