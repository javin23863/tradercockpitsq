package SQ.Blocks.Indicators.ROC;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(ROC) ROC", display = "ROC(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("Price rate of change")
public class ROC extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "5", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar < this.Period) {
         this.Value.set(0, 0.0);
      } else {
         double var1 = this.Input.Close(this.Period);
         if (var1 == 0.0) {
            this.Value.set(0, 0.0);
         } else {
            double var3 = (this.Input.Close(0) - var1) / var1 * 100.0;
            this.Value.set(0, var3);
         }
      }
   }
}
