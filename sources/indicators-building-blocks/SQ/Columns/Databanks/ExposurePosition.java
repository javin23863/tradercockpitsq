package SQ.Columns.Databanks;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExposurePosition extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("Exposure Position (Stockpicker engine)");
   static final int MILLIS_IN_DAY = 86400000;

   public ExposurePosition() {
      super(L.tsq("Exposure Position"), "Decimal2Pct", (byte)2, 0.0, 0.0, 100.0);
      this.setTooltip(
         L.tsq(
            "Exposure Position = # bars in all positions / total # bars in the sample.\nSpecial version for Stockpicker engine considering also # of open positions form max."
         )
      );
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDependencies(new String[]{"Exposure"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (!this.isStockpickerEngine(var4)) {
         return var1.getDouble("Exposure");
      }

      int var7 = this.getMaxOpenPositions(var2, var4);

      try {
         ChartSetup var8 = (ChartSetup)var4.get("BacktestChart");
         if (var8 == null) {
            return 0.0;
         }

         ChartDef var9 = var8.getMainChart();
         if (var9 == null) {
            return 0.0;
         }

         long var10 = var9.getHistoryFrom();
         long var12 = var9.getHistoryTo();
         int var14 = (int)((var12 - var10) / 86400000L);
         if (var14 == 0) {
            return 0.0;
         }

         if (var14 < 0) {
            return 0.0;
         }

         int[] var15 = new int[var14];

         for (int var16 = 0; var16 < var15.length; var16++) {
            var15[var16] = 0;
         }

         for (int var25 = 0; var25 < var3.size(); var25++) {
            Order var17 = var3.get(var25);
            if (var17.isFilledOrder()) {
               int var18 = (int)((var17.CloseTime - var17.OpenTime) / 86400000L);
               int var19 = (int)((var17.OpenTime - var10) / 86400000L);
               if (var19 >= 0 && var18 >= 0 && var19 + var18 < var15.length) {
                  for (int var20 = var19; var20 < var19 + var18; var20++) {
                     var15[var20]++;
                  }
               }
            }
         }

         double var26 = var15.length * var7;
         double var27 = 0.0;

         for (int var23 : var15) {
            var27 += var23;
         }

         return this.round2(var27 / var26 * 100.0);
      } catch (Exception var24) {
         Log.error("Exception ", var24);
         return 0.0;
      }
   }

   private boolean isStockpickerEngine(SettingsMap var1) {
      ChartSetup var2 = (ChartSetup)var1.get("BacktestChart");
      return var2 != null && var2.getBacktestEngine() == 1316847364;
   }

   private int getMaxOpenPositions(StatsTypeCombination var1, SettingsMap var2) {
      int var3 = 0;
      TradingOptions var4 = (TradingOptions)var2.get("TradingOptions");
      if (var4 != null) {
         for (TradingOption var6 : var4) {
            if (var6 instanceof StockpickerOptions) {
               if (var1.getDirection() == 0 || var1.getDirection() == 1) {
                  var3 += ((StockpickerOptions)var6).PickerMaxOpenPositionsLong;
               }

               if (var1.getDirection() == 0 || var1.getDirection() == -1) {
                  var3 += ((StockpickerOptions)var6).PickerMaxOpenPositionsShort;
               }
            }
         }
      }

      if (var3 == 0) {
         var3 = 1;
      }

      return var3;
   }
}
