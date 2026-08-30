package com.strategyquant.tradinglib.equitychart;

import java.util.ArrayList;

public class Points {
   public String SHOW_TRADE_PEAKS = "grow";
   public String SHOW_ALL_POINTS = "all";

   public void print(EquityChart var1, String var2, String var3) {
      ArrayList var8 = var1.getMainChartDatasets();
      if (!var8.isEmpty()) {
         MainChartDataset var9 = (MainChartDataset)var8.get(0);
         int var10 = this.getSize(var9, var2, var3);
         MainChartDataset var11 = new MainChartDataset("Points", var10);
         var11.name = "Trade points";
         var11.type = "point";
         var11.color = var2.equals(this.SHOW_ALL_POINTS) ? "#FF0000" : "#00ff00";

         for (int var12 = 0; var12 < var9.length; var12++) {
            long var4 = var9.xVals[var12];
            double var6 = var9.yVals[var12];
            if (var2.equals(this.SHOW_TRADE_PEAKS)) {
               if (var11.index < 0 || var11.yVals[var11.index] < var6) {
                  var11.addValue(var4, var6);
               }
            } else if (var3.equals("time")) {
               if (var12 == 0 || var9.yVals[var12 - 1] != var6) {
                  var11.addValue(var4, var6);
               }
            } else {
               var11.addValue(var4, var6);
            }
         }

         var1.addMainChartDataset(var11);
      }
   }

   private int getSize(MainChartDataset var1, String var2, String var3) {
      int var4 = 0;
      double var5 = 0.0;
      if (var2.equals(this.SHOW_ALL_POINTS)) {
         if (var3.equals("time")) {
            for (int var7 = 0; var7 < var1.length; var7++) {
               if (var7 == 0 || var1.yVals[var7 - 1] != var1.yVals[var7]) {
                  var4++;
               }
            }
         } else {
            var4 = var1.length;
         }
      } else {
         for (int var8 = 0; var8 < var1.length; var8++) {
            if (var8 == 0 || var5 < var1.yVals[var8]) {
               var5 = var1.yVals[var8];
               var4++;
            }
         }
      }

      return var4;
   }
}
