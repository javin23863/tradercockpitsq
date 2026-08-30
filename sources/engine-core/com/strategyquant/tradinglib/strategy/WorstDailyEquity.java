package com.strategyquant.tradinglib.strategy;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public class WorstDailyEquity {
   public void compute(ResultsGroup var1) throws Exception {
      Result var2 = var1.portfolio();
      Long2FloatRBTreeMap var3 = var2.getWorstDailyEquity();
      if (var3 != null && !var3.isEmpty()) {
         OrdersList var4 = var1.orders();

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Order var6 = var4.get(var5);
            var6.worstDailyEquity = this.getWorstEquity(var6.OpenTime, var6.CloseTime, var3);
         }
      }
   }

   private float getWorstEquity(long var1, long var3, Long2FloatRBTreeMap var5) {
      float var8 = 0.0F;
      long var9 = var1;
      boolean var11 = true;

      while (true) {
         long var6 = var9;
         if (var5.containsKey(var6) && (var11 || var5.get(var6) < var8)) {
            var8 = var5.get(var6);
            var11 = false;
         }

         if (var9 > var3) {
            return var8;
         }

         var9 = SQTime.addDays(var9, 1);
      }
   }
}
