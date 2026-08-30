package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import SQ.Utils.TimeDiffUtil;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(MH) Monthly High", returnType = 2, display = "HighM[@Chart@#Shift#]")
@SortOrder(600)
@OppositeBlock("LowM")
public class HighM extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   protected void OnInit() {
      this.barsShiftedSeries = new DataSeries();
      if (this.isTradestationEngine()) {
         SettingsMap var1 = this.Strategy.getSettings();
         if (var1.containsKey("ReservedBars")) {
            this.reservedBars = var1.getInt("ReservedBars", 0);
         }
      }
   }

   @Override
   protected void OnDeinit() {
      if (this.barsShiftedSeries != null) {
         this.barsShiftedSeries.destroy();
      }
   }

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.strategyTriggeredAt() == 5 && var1 + this.Shift == 0
            ? this.Strategy.Stockpicker.data.OpenM(this.chartIndex, var1 + this.Shift)
            : this.Strategy.Stockpicker.data.HighM(this.chartIndex, var1 + this.Shift);
      }

      if (!Engines.isTradestationEngine(this.Strategy.getEngine())) {
         return this.Chart.HighM(this.Shift + var1);
      }

      int var2 = TimeDiffUtil.getDiffInMonths(this.Chart, var1);
      if (this.Shift == 0 && var2 == 0 && var1 != 0) {
         int var3 = var1;
         if (var3 >= 0 && var3 < this.Chart.High.size()) {
            if (this.barsShiftedSeries.size() < this.Chart.High.size() - this.reservedBars) {
               for (int var4 = 0; var4 < this.Chart.High.size() - this.reservedBars - this.barsShiftedSeries.size(); var4++) {
                  this.barsShiftedSeries.add(0.0);
               }
            }

            if (this.barsShiftedSeries.get(var1) == 0.0) {
               int var7 = this.Chart.High.size() - 1;
               double var5 = this.calculateHighDUpToBar(var3, var7);
               this.barsShiftedSeries.set(var1, var5);
               return var5;
            } else {
               return this.barsShiftedSeries.get(var1);
            }
         } else {
            return 0.0;
         }
      } else {
         return this.Chart.HighM(this.Shift + var2);
      }
   }

   private double calculateHighDUpToBar(int var1, int var2) throws TradingException {
      double var3 = Double.MIN_VALUE;

      for (int var5 = var1; var5 <= var2 && SQTime.isSameMonth(this.Chart.Time.get(var5), this.Chart.Time.get(var1)); var5++) {
         var3 = Math.max(var3, this.Chart.High.get(var5));
      }

      return var3;
   }
}
