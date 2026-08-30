package SQ.Blocks.Indicators.UlcerIndex;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(UI) Ulcer Index", display = "Ulcer Index(@Chart@#Mode#,#Period#)[#Shift#]", returnType = 1)
@Help("Ulcer Index indicator")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=96"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=200")
   }
)
public class UlcerIndex extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(defaultValue = "1")
   @Editor(type = 40, values = "UP UI=1,Down UI=2")
   public int Mode;
   @Parameter(defaultValue = "24", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;
   @Buffer
   public DataSeries ddBuffer;
   private HighestCalculator highestCalculator;
   private LowestCalculator lowestCalculator;

   protected void OnInit() throws TradingException {
      this.highestCalculator = new HighestCalculator(this.Period);
      this.lowestCalculator = new LowestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar < this.Period + 1) {
         this.Value.set(0, 0.0);
         this.ddBuffer.set(0, 0.0);
      } else if (this.Mode == 1) {
         this.highestCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
         double var1 = this.highestCalculator.getHighestValue();
         double var3 = 100.0 * (this.Input.Close.get(0) - var1) / var1;
         this.ddBuffer.set(0, var3);
         double var5 = 0.0;

         for (int var7 = 0; var7 < this.Period; var7++) {
            var5 += Math.pow(this.ddBuffer.get(var7), 2.0);
         }

         double var12 = Math.sqrt(var5 / this.Period);
         this.Value.set(0, var12);
      } else if (this.Mode == 2) {
         this.lowestCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
         double var9 = this.lowestCalculator.getLowestValue();
         double var10 = 100.0 * (this.Input.Close.get(0) - var9) / var9;
         this.ddBuffer.set(0, var10);
         double var11 = 0.0;

         for (int var13 = 0; var13 < this.Period; var13++) {
            var11 += Math.pow(this.ddBuffer.get(var13), 2.0);
         }

         double var14 = Math.sqrt(var11 / this.Period);
         this.Value.set(0, var14);
      }
   }
}
