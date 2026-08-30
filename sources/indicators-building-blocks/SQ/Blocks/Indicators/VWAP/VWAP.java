package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(
   name = "(VWAP) Volume Weighted Average Price - VWAP",
   display = "Volume Weighted Average Price - VWAP(@Chart@#VWAPPeriod#)[#Shift#]",
   returnType = 2
)
@Help("VWAP help text")
public class VWAP extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Output
   public DataSeries Value;
   @Parameter(defaultValue = "10", minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int VWAPPeriod;

   protected void OnBarUpdate() throws TradingException {
      double var1 = 0.0;
      double var3 = 0.0;
      if (this.CurrentBar < this.VWAPPeriod) {
         this.Value.set(0, 0.0);
      } else {
         for (int var5 = 0; var5 < this.VWAPPeriod; var5++) {
            double var6 = (this.Input.Open.get(var5) + this.Input.High.get(var5) + this.Input.Low.get(var5) + this.Input.Close.get(var5)) / 4.0;
            double var8 = this.Input.Volume.get(var5);
            var1 += var6 * var8;
            var3 += var8;
         }

         double var10 = 0.0;
         if (var3 != 0.0) {
            var10 = var1 / var3;
         }

         this.Value.set(0, var10);
      }
   }
}
