package SQ.Blocks.Indicators.BollingerBands;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(BBWR) BB Width Ratio", display = "BB WR(@Chart@#Period#, #Deviation#)[#Shift#]", returnType = 7)
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
public class BBWidthRatio extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "2", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Output(name = "BBWidthRatio", color = "#008000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Input.getSeries(this.ComputedFrom).get(0), this.getCurrentBar());
      int var1 = this.CurrentBar > this.Period - 1 ? this.Period - 1 : this.CurrentBar;
      double var2 = 0.0;

      for (int var4 = var1; var4 >= 0; var4--) {
         double var5 = this.Input.getSeries(this.ComputedFrom).get(var4) - this.averageCalculator.getValue();
         var2 += var5 * var5;
      }

      double var7 = Math.sqrt(var2 / this.Period);
      if (this.averageCalculator.getValue() > 0.0) {
         this.Value.set(0, 2.0 * this.Deviation * var7 / this.averageCalculator.getValue());
      } else {
         this.Value.set(0, 0.0);
      }
   }
}
