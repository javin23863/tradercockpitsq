package SQ.Blocks.Indicators.Reflex;

import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(RFX) Reflex", display = "Reflex(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("Reflex help text")
@ParameterSets(
   {
         @ParameterSet(set = "Period=6"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=36"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=240"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=200")
   }
)
public class Reflex extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "24", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;
   @Buffer
   public DataSeries filt;
   @Buffer
   public DataSeries MS;
   private SumCalculator sumCalculator;

   protected void OnBarUpdate() throws TradingException {
      if (Engines.isTradestationEngine(this.Indicators.Engine)) {
         this.onBarUpdateTS();
      } else {
         this.onBarUpdateMT();
      }
   }

   protected void onBarUpdateMT() throws TradingException {
      if (this.CurrentBar < this.Period) {
         this.Value.set(0, 0.0);
         this.MS.set(0, 0.0);
         this.filt.set(0, 0.0);
      } else {
         double var3 = Math.exp(-4.442212012175967 / (this.Period * 0.5));
         double var5 = 2.0 * var3 * this.cosine(Math.cos(254.51999999999998 / (0.5 * this.Period)));
         double var7 = var5;
         double var9 = -var3 * var3;
         double var11 = 1.0 - var7 - var9;
         double var13 = var11 * (this.Input.Close.get(0) + this.Input.Close.get(1)) / 2.0 + var7 * this.filt.get(1) + var9 * this.filt.get(2);
         this.filt.set(0, var13);
         double var15 = (this.filt.get(this.Period) - var13) / this.Period;
         double var17 = 0.0;

         for (int var19 = 1; var19 <= this.Period; var19++) {
            var17 += var13 + var19 * var15 - this.filt.get(var19);
         }

         var17 /= this.Period;
         this.MS.set(0, 0.04 * var17 * var17 + 0.96 * this.MS.get(1));
         double var22 = var17 / Math.sqrt(this.MS.get(0));
         this.Value.set(0, var22);
      }
   }

   private double cosine(double var1) {
      return Math.cos(var1 * Math.PI / 180.0);
   }

   protected void onBarUpdateTS() throws TradingException {
      if (this.CurrentBar < this.Period) {
         this.Value.set(0, 0.0);
         this.MS.set(0, 0.0);
         this.filt.set(0, 0.0);
      } else {
         double var1 = Math.exp(-4.442212012175967 / (0.5 * this.Period));
         double var3 = 2.0 * var1 * Math.cos(Math.toRadians(254.51999999999998 / (0.5 * this.Period)));
         double var5 = var3;
         double var7 = -var1 * var1;
         double var9 = 1.0 - var5 - var7;
         double var11 = var9 * (this.Input.Close.get(0) + this.Input.Close.get(1)) / 2.0 + var5 * this.filt.get(1) + var7 * this.filt.get(2);
         this.filt.set(0, var11);
         double var13 = (this.filt.get(this.Period) - var11) / this.Period;
         double var15 = 0.0;

         for (int var17 = 1; var17 <= this.Period; var17++) {
            var15 += var11 + var17 * var13 - this.filt.get(var17);
         }

         var15 /= this.Period;
         this.MS.set(0, 0.04 * var15 * var15 + 0.96 * this.MS.get(1));
         double var20 = var15 / Math.sqrt(this.MS.get(0));
         this.Value.set(0, var20);
      }
   }
}
