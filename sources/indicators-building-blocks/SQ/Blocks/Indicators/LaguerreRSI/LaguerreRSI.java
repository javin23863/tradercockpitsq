package SQ.Blocks.Indicators.LaguerreRSI;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(LRSI) Laguerre RSI", display = "Laguerre RSI(@Chart@#Gamma#)[#Shift#]", returnType = 1)
@Help("LaguerreRSI help text")
@Indicator(oscillator = true, middleValue = 0.5, min = 0.0, max = 1.0, step = 0.01)
@ParameterSets(
   {
         @ParameterSet(set = "Gamma=0.1"),
         @ParameterSet(set = "Gamma=0.2"),
         @ParameterSet(set = "Gamma=0.3"),
         @ParameterSet(set = "Gamma=0.4"),
         @ParameterSet(set = "Gamma=0.5"),
         @ParameterSet(set = "Gamma=0.6"),
         @ParameterSet(set = "Gamma=0.7"),
         @ParameterSet(set = "Gamma=0.8"),
         @ParameterSet(set = "Gamma=0.9")
   }
)
public class LaguerreRSI extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "0.5", isPeriod = true, minValue = 0.0, maxValue = 1.0, step = 0.01)
   public double Gamma;
   @Output
   public DataSeries LRSI;
   @Buffer
   public DataSeries L0;
   @Buffer
   public DataSeries L1;
   @Buffer
   public DataSeries L2;
   @Buffer
   public DataSeries L3;

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar < 20) {
         this.LRSI.set(0, 0.0);
         this.L0.set(0, 0.0);
         this.L1.set(0, 0.0);
         this.L2.set(0, 0.0);
         this.L3.set(0, 0.0);
      } else {
         this.L0.set(0, (1.0 - this.Gamma) * this.Chart.Close.get(0) + this.Gamma * this.L0.get(1));
         this.L1.set(0, -this.Gamma * this.L0.get(0) + this.L0.get(1) + this.Gamma * this.L1.get(1));
         this.L2.set(0, -this.Gamma * this.L1.get(0) + this.L1.get(1) + this.Gamma * this.L2.get(1));
         this.L3.set(0, -this.Gamma * this.L2.get(0) + this.L2.get(1) + this.Gamma * this.L3.get(1));
         double var1 = 0.0;
         double var3 = 0.0;
         if (this.L0.get(0) >= this.L1.get(0)) {
            var1 = this.L0.get(0) - this.L1.get(0);
         } else {
            var3 = this.L1.get(0) - this.L0.get(0);
         }

         if (this.L1.get(0) >= this.L2.get(0)) {
            var1 = var1 + this.L1.get(0) - this.L2.get(0);
         } else {
            var3 = var3 + this.L2.get(0) - this.L1.get(0);
         }

         if (this.L2.get(0) >= this.L3.get(0)) {
            var1 = var1 + this.L2.get(0) - this.L3.get(0);
         } else {
            var3 = var3 + this.L3.get(0) - this.L2.get(0);
         }

         if (var1 + var3 != 0.0) {
            this.LRSI.set(0, var1 / (var1 + var3));
         } else {
            this.LRSI.set(0, 0.0);
         }
      }
   }
}
