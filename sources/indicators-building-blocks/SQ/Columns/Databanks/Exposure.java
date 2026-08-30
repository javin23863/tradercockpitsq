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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exposure extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("Exposure");
   static final int MILLIS_IN_DAY = 86400000;

   public Exposure() {
      super(L.tsq("Exposure"), "Decimal2Pct", (byte)2, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Exposure = # bars in all positions / total # bars in the sample"));
      this.setPLTypeRestrictions(new byte[]{10});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      try {
         long var7 = Long.MAX_VALUE;
         long var9 = Long.MIN_VALUE;
         ChartSetup var11 = (ChartSetup)var4.get("BacktestChart");
         if (var11 != null) {
            ChartDef var12 = var11.getMainChart();
            if (var12 != null) {
               var7 = var12.getHistoryFrom();
               var9 = var12.getHistoryTo();
            }
         } else {
            var7 = var4.getLong("PortfolioDataStart", Long.MAX_VALUE);
            var9 = var4.getLong("PortfolioDataEnd", Long.MIN_VALUE);
         }

         if (var7 != Long.MAX_VALUE && var9 != Long.MIN_VALUE) {
            int var21 = (int)((var9 - var7) / 86400000L);
            if (var21 == 0) {
               return 0.0;
            }

            if (var21 < 0) {
               return 0.0;
            }

            boolean[] var13 = new boolean[var21];

            for (int var14 = 0; var14 < var3.size(); var14++) {
               Order var15 = var3.get(var14);
               if (var15.isFilledOrder()) {
                  int var16 = (int)((var15.CloseTime - var15.OpenTime) / 86400000L);
                  int var17 = (int)((var15.OpenTime - var7) / 86400000L);
                  if (var17 >= 0 && var16 >= 0 && var17 + var16 < var13.length) {
                     for (int var18 = var17; var18 < var17 + var16; var18++) {
                        var13[var18] = true;
                     }
                  }
               }
            }

            double var22 = 0.0;

            for (boolean var19 : var13) {
               var22 += var19 ? 1.0 : 0.0;
            }

            return this.round2(var22 / var13.length * 100.0);
         } else {
            return 0.0;
         }
      } catch (Exception var20) {
         Log.error("Exception ", var20);
         return 0.0;
      }
   }
}
