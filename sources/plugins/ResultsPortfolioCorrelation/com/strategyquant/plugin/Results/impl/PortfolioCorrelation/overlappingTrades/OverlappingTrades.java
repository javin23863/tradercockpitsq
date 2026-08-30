package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;

public class OverlappingTrades {
   public OverlappingResult compute(ResultsGroup var1, String var2, String var3, OrdersList var4, OrdersList var5) throws Exception {
      try {
         OverlappingResult var6 = new OverlappingResult(var2, var3);
         int var7 = 0;
         String var8 = "N/A";

         for (int var13 = 0; var13 < var4.size(); var13++) {
            Order var14 = var4.get(var13);
            if (var14.isRealOrder()) {
               for (int var15 = 0; var15 < var5.size(); var15++) {
                  Order var16 = var5.get(var15);
                  if (var16.isRealOrder() && var14.OpenTime < var16.CloseTime && var14.CloseTime > var16.OpenTime) {
                     long var9 = var14.OpenTime > var16.OpenTime ? var14.OpenTime : var16.OpenTime;
                     long var11 = var14.CloseTime < var16.CloseTime ? var14.CloseTime : var16.CloseTime;
                     var8 = SQTime.formatDateTime((int)((var11 - var9) / 1000L));
                     var6.data.add(new OverlappingTrade(var14.OpenTime, var14.CloseTime, var16.OpenTime, var16.CloseTime, var8));
                     var7++;
                  }
               }
            }
         }

         var6.count = var7;
         return var6;
      } finally {
         var4.clear();
         var5.clear();
      }
   }
}
