package com.strategyquant.plugin.Results.impl.TradeAnalysis;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import java.text.DecimalFormat;
import org.json.JSONArray;

public class AnnualStatsComputer {
   public static final int YEAR_ALL = 0;
   public DecimalFormat twoDFormat = new DecimalFormat("0.00");

   public JSONArray compute(int var1, OrdersList var2, byte var3, byte var4) {
      JSONArray var5 = new JSONArray();
      int var6 = 0;
      double var7 = 0.0;
      double var9 = 0.0;
      double var11 = 0.0;
      double var13 = 0.0;
      int var15 = 0;
      int var16 = 0;
      double var17 = 0.0;
      double var19 = 0.0;

      for (int var23 = 0; var23 < var2.size(); var23++) {
         Order var24 = var2.get(var23);
         long var21 = var24.getTimeByPeriodType(var4);
         if (var1 == 0 || SQTime.getFullYear(var21) == var1) {
            var6++;
            var19 = var24.getPLByType(var3);
            var7 += var19;
            if (var19 > 0.0) {
               var15++;
               var9 += Math.abs(var19);
            } else if (var19 < 0.0) {
               var16++;
               var11 += Math.abs(var19);
            }
         }
      }

      if (var6 == 0) {
         var17 = 0.0;
      } else {
         var17 = SQUtils.safeDivide(var15, var15 + var16) * 100.0;
      }

      if (var6 == 0) {
         var13 = 0.0;
      }

      if (var9 == 0.0) {
         var13 = 0.0;
      }

      if (var11 == 0.0) {
         if (var6 > 10) {
            var13 = 5.0;
         } else {
            var13 = 0.0;
         }
      } else {
         var13 = var9 / var11;
      }

      var5.put(var1 == 0 ? "All" : var1);
      var5.put(PlTypes.printPL(var7, var3));
      var5.put(SQUtils.d2(var13));
      var5.put(var6);
      var5.put(PlTypes.printPL(var17, (byte)20));
      return var5;
   }
}
