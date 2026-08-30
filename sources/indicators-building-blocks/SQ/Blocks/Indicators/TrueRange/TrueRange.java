package SQ.Blocks.Indicators.TrueRange;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(TR) TrueRange", returnType = 7, display = "TrueRange[@Chart@#Shift#]")
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
public class TrueRange extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Output(name = "TrueRange", color = "#FF0000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      double var1 = this.Chart.Close(1);
      double var3 = this.Chart.High(0);
      double var5 = this.Chart.Low(0);
      double var7;
      if (var1 > var3) {
         var7 = var1;
      } else {
         var7 = var3;
      }

      double var9;
      if (var1 < var5) {
         var9 = var1;
      } else {
         var9 = var5;
      }

      double var11 = var7 - var9;
      this.Value.set(0, var11);
   }
}
