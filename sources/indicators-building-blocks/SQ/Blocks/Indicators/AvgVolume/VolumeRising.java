package SQ.Blocks.Indicators.AvgVolume;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(VOL) Volume is rising", display = "Volume(@Chart@)[#Shift#] is rising", returnType = 3)
@Help("Is triggered if Volume is rising")
@OppositeBlock("VolumeRising")
@SortOrder(100)
@CategoryOrder(1200)
public class VolumeRising extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Chart.Volume.getRounded(this.Shift + 1);
      double var3 = this.Chart.Volume.getRounded(this.Shift);
      return var1 < var3;
   }
}
