package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForwardStability {
   private static final Logger Log = LoggerFactory.getLogger(WalkForwardStability.class);

   public static synchronized void compute(DatabankColumn var0, SQStats var1, WalkForwardResult var2) {
      double var3 = 0.0;

      try {
         double var5 = 0.0;
         double var7 = 0.0;
         double var9 = 0.0;
         double var11 = 0.0;
         ArrayList var13 = var2.wfPeriods;

         for (int var14 = 0; var14 < var13.size() - 1; var14++) {
            WalkForwardPeriod var15 = (WalkForwardPeriod)var13.get(var14);
            var5 += var15.optimizationStatData == null ? 0.0 : var0.getNumericValue(var15.optimizationStatData);
            var7 += var15.runStatData == null ? 0.0 : var0.getNumericValue(var15.runStatData);
            var9 += SQTime.getDaysBetween(var15.optimizeFrom, var15.optimizeTo);
            var11 += SQTime.getDaysBetween(var15.runFrom, var15.runTo);
         }

         double var16;
         double var21;
         if (var0.isDependentOnTradingPeriod()) {
            var21 = SQUtils.safeDivide(var5, var9);
            var16 = SQUtils.safeDivide(var7, var11);
         } else {
            var21 = var5;
            var16 = var7;
         }

         double var18 = SQUtils.safeDivide(var16, var21) * 100.0;
         var3 = SQUtils.round(var18, 2);
      } catch (Exception var20) {
         Log.error("Error while computing WalkForward Stability. Exc.", var20);
      }

      var1.set(var0.getStatsValueName(), var3);
   }
}
