package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.WalkForwardResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForwardScore {
   private static final Logger Log = LoggerFactory.getLogger(WalkForwardScore.class);

   public static synchronized void compute(DatabankColumn var0, ResultsGroup var1, SQStats var2, WalkForwardResult var3) {
      double var4 = 0.0;

      try {
         double var6 = 0.0;
         double var8 = 0.0;
         SQStats var10 = var1.portfolio().stats((byte)0, (byte)10, (byte)127);
         var6 = var0.getNumericValue(var10);
         var8 = var0.getNumericValue(var3.stats);
         var4 = SQUtils.round(var8 / (var6 / 100.0), 2);
      } catch (Exception var11) {
         Log.error("Error while computing WalkForwardScore. Exc.", var11);
      }

      var2.set(var0.getStatsValueName(), var4);
   }
}
