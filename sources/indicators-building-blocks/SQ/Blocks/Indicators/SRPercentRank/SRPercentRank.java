package SQ.Blocks.Indicators.SRPercentRank;

import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(SRPC) SRPercRank", display = "SRPercRank(@Chart@#Mode#,#Length#,#ATRPeriod#)[#Shift#]", returnType = 1)
@Help(
   "SRPercRank helps you to identify Support/Ressistance Levels. Indicator si also able to help you with breakout areas identifications. Check blog post: adress"
)
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 0.1)
@ParameterSets(
   {
         @ParameterSet(set = "Mode=1,Length=12,ATRPeriod=12"),
         @ParameterSet(set = "Mode=1,Length=24,ATRPeriod=12"),
         @ParameterSet(set = "Mode=1,Length=48,ATRPeriod=12"),
         @ParameterSet(set = "Mode=1,Length=120,ATRPeriod=12"),
         @ParameterSet(set = "Mode=1,Length=240,ATRPeriod=12"),
         @ParameterSet(set = "Mode=1,Length=480,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=12,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=24,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=48,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=120,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=240,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=480,ATRPeriod=12")
   }
)
public class SRPercentRank extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "2")
   @Editor(type = 40, values = "Basic Mode=1,ATR Mode=2")
   public int Mode;
   @Parameter(defaultValue = "120", isPeriod = true, builderMinValue = 2.0, builderMaxValue = 500.0, minValue = 2.0, maxValue = 500.0, step = 1.0)
   public int Length;
   @Parameter(defaultValue = "12", isPeriod = true, minValue = 5.0, maxValue = 120.0, step = 1.0)
   public int ATRPeriod;
   @Output
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar >= this.Length + 1) {
         int var1 = 0;
         double var2 = 0.0;
         MTATR var4 = this.Indicators.MTATR(this.Chart, this.ATRPeriod);
         double var5 = var4.Value.getRounded(this.Shift);

         for (int var7 = 1; var7 <= this.Length; var7++) {
            if (this.Mode == 1) {
               if (this.Chart.Close.get(0) > this.Chart.Low.get(var7) && this.Chart.Close.get(0) < this.Chart.High.get(var7)) {
                  var1++;
               }
            } else if (this.Mode == 2
               && this.Chart.Close.get(0) > this.Chart.Low.get(var7) - var5
               && this.Chart.Close.get(0) < this.Chart.High.get(var7) + var5) {
               var1++;
            }
         }

         var2 = (double)var1 / this.Length * 100.0;
         this.Value.set(0, var2);
      }
   }
}
