package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StabilitySQ3 extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("StabilitySQ3");

   public StabilitySQ3() {
      super(L.tsq("Stability SQ3"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("Stability - how straight and steep is the equity curve (SQ3 version)"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() == 0) {
         return 0.0;
      }

      double var7 = 0.0;
      double var9 = 0.0;
      int var11 = var3.size();
      double var12 = 0.0;
      double var14 = -1.0E7;
      double var16 = 0.0;
      int var18 = 6;
      if (var18 > var11 / 2) {
         var18 = var11 / 2;
      }

      for (int var19 = 0; var19 < var11; var19++) {
         Order var20 = var3.get(var19);
         var12 += this.getPLByStatsType(var20, var2);
         if (var12 > var14) {
            var14 = var12;
         }

         if (var19 > var3.size() - 1 - var18) {
            var16 += var12;
         }
      }

      if (var14 == 0.0) {
         var7 = 10000.0;
      } else {
         var7 = var14;
      }

      var9 = var16 / var18;
      if (!(var9 <= 0.0) && var7 <= 0.0) {
      }

      double var56 = var9 / var11;
      double var21 = var9 * 0.1F;
      double var23 = 10000.0 / (var3.size() * 10.0);
      double var25 = Math.atan(var23);
      if (var25 > Math.PI / 2) {
         var25 = Math.PI / 2;
      }

      double var27 = 0.0;
      double var29 = 0.0;
      double var31 = 0.0;
      double var33 = 0.0;
      double var35 = 0.0;
      double var37 = 0.0;
      double var39 = 0.0;
      double var41 = 0.0;
      double var43 = 0.0;
      double var45 = 0.0;
      var12 = 0.0;

      for (int var47 = 0; var47 < var3.size(); var47++) {
         Order var48 = var3.get(var47);
         var12 += this.getPLByStatsType(var48, var2);
         double var49 = var47 * var23;
         var27 += var12 * var49;
         var29 += var12;
         var31 += var12 * var12;
         var33 += var49;
         var35 += var49 * var49;
         var37++;
         double var51 = var47 * var56;
         var45 += Math.abs(var12 - var51) / var21;
      }

      if (var37 > 0.0) {
         var39 = var27 - var29 * var33 / var37;
         var41 = var31 - var29 * var29 / var37;
         var43 = var35 - var33 * var33 / var37;
      } else {
         var39 = 0.0;
         var41 = 0.0;
         var43 = 0.0;
      }

      if (var41 != 0.0 && var43 != 0.0 && var37 != 0.0) {
         double var61 = var39 / Math.sqrt(var41 * var43);
         double var62 = Math.abs(var45 / var37);
         var62 = 5.0 / (5.0 + var62);
         var62 = (var61 + 2.0 * var62) / 3.0;
         if (var62 <= 0.0 || var62 >= 1.0) {
            var62 = 0.01;
         }

         if (var37 > 0.0 && var37 < 100.0) {
            double var65 = (1.0 - 100.0 / (var37 + 100.0)) * 2.0;
            var62 *= var65;
         }

         return this.round2(var62);
      } else {
         return 0.0;
      }
   }
}
