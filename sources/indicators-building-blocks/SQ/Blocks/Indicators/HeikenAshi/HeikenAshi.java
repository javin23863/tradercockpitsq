package SQ.Blocks.Indicators.HeikenAshi;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@IgnoreInBuilder
@BuildingBlock(name = "(HA) Heiken Ashi", returnType = 2, display = "Heiken Ashi(@Chart@)[#Shift#]")
public class HeikenAshi extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Output(name = "HAOpen", color = "#008000")
   public DataSeries HAOpen;
   @Output(name = "HAClose", color = "#008000")
   public DataSeries HAClose;

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.HAOpen.set(0, this.Chart.Open(0));
         this.HAClose.set(0, this.Chart.Close(0));
      } else {
         this.HAClose.set(0, (this.Chart.Open(0) + this.Chart.High(0) + this.Chart.Low(0) + this.Chart.Close(0)) / 4.0);
         this.HAOpen.set(0, (this.HAOpen.get(1) + this.HAClose.get(1)) / 2.0);
      }
   }
}
