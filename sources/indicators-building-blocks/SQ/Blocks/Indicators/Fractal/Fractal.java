package SQ.Blocks.Indicators.Fractal;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "Fractal", display = "Fractal(@Chart@#Fractal#)[#Shift#]", returnType = 2)
public class Fractal extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(name = "Fractal", defaultValue = "3")
   @Editor(type = 40, values = "3-bar=3,5-bar=5")
   public int Fractal;
   @Output(name = "Up", color = "#008000", showZeroValues = false, chartType = "point")
   public DataSeries Up;
   @Output(name = "Down", color = "#FF0000", showZeroValues = false, chartType = "point")
   public DataSeries Down;
   double dCurrent;
   int FractalUsed;

   protected void OnInit() throws TradingException {
      if ((this.Fractal - 1) / 2 <= 0) {
         this.FractalUsed = 3;
      } else {
         this.FractalUsed = this.Fractal;
      }
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar < this.FractalUsed - 1) {
         this.Up.set(0, 0.0);
         this.Down.set(0, 0.0);
      } else {
         int var1 = (this.FractalUsed - 1) / 2;
         int var2 = var1 + 1;
         double var3 = this.Chart.High.get(var2);
         double var5 = this.Chart.Low.get(var2);
         boolean var7 = true;
         boolean var8 = true;

         for (int var9 = this.FractalUsed; var9 > 0; var9--) {
            if (var9 != var2) {
               if (this.Chart.High.get(var9) >= var3) {
                  var7 = false;
               }

               if (this.Chart.Low.get(var9) <= var5) {
                  var8 = false;
               }
            }
         }

         this.Up.set(0, var7 ? var3 : 0.0);
         this.Down.set(0, var8 ? var5 : 0.0);
      }
   }
}
