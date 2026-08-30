package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import SQ.Utils.TimeDiffUtil;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(WC) Weekly Close", returnType = 2, display = "CloseW[@Chart@#Shift#]")
@SortOrder(800)
public class CloseW extends ValueBlock {
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
            ? this.Strategy.Stockpicker.data.OpenW(this.chartIndex, var1 + this.Shift)
            : this.Strategy.Stockpicker.data.CloseW(this.chartIndex, var1 + this.Shift);
      }

      if (!Engines.isTradestationEngine(this.Strategy.getEngine())) {
         return this.Chart.CloseW(this.Shift + var1);
      }

      int var2 = TimeDiffUtil.getDiffInWeeks(this.Chart, var1);
      if (this.Shift == 0 && var2 == 0 && var1 != 0) {
         int var3 = var1;
         if (var3 >= 0 && var3 < this.Chart.Close.size()) {
            if (this.barsShiftedSeries.size() < this.Chart.Close.size() - this.reservedBars) {
               for (int var4 = 0; var4 < this.Chart.Close.size() - this.reservedBars - this.barsShiftedSeries.size(); var4++) {
                  this.barsShiftedSeries.add(0.0);
               }
            }

            if (this.barsShiftedSeries.get(var1) == 0.0) {
               double var6 = this.Chart.Close.get(var3);
               this.barsShiftedSeries.set(var1, var6);
               return var6;
            } else {
               return this.barsShiftedSeries.get(var1);
            }
         } else {
            return 0.0;
         }
      } else {
         return this.Chart.CloseW(this.Shift + var2);
      }
   }
}
