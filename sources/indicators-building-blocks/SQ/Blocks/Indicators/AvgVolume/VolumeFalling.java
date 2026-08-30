package SQ.Blocks.Indicators.AvgVolume;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(VOL) Volume is falling", display = "Volume(@Chart@)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if Volume is falling")
@OppositeBlock("VolumeFalling")
@SortOrder(200)
public class VolumeFalling extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Chart.Volume.getRounded(this.Shift + 1);
      double var3 = this.Chart.Volume.getRounded(this.Shift);
      return var1 > var3;
   }
}
