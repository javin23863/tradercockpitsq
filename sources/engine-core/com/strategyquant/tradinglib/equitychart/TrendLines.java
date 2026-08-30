package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import java.util.ArrayList;
import java.util.List;

public class TrendLines {
   public void print(EquityChart var1, List<TimePeriod> var2, String var3) {
      if (var2 != null && !var2.isEmpty()) {
         int var8 = 0;
         boolean var10 = true;
         MainChartDataset var11 = new MainChartDataset("TrendLines", var2.size() + 1);
         var11.color = "#FF0000";
         ArrayList var12 = var1.getMainChartDatasets();
         if (!var12.isEmpty()) {
            MainChartDataset var13 = (MainChartDataset)var12.get(0);

            for (int var14 = 0; var14 < var13.length; var14++) {
               long var4 = var13.xVals[var14];
               double var6 = var13.yVals[var14];
               TimePeriod var9 = (TimePeriod)var2.get(var8);
               if (var3.equals("time")) {
                  var4 = SQTime.getDateInMs(var4);
               }

               if (var10) {
                  var11.addValue(var4, var6);
                  var10 = false;
               } else if (var4 == var9.to || var4 > var9.to) {
                  var11.addValue(var4, var6);
                  if (++var8 == var2.size()) {
                     break;
                  }
               }
            }

            if (var11.getSize() > 1 && var11.xVals[var11.getSize() - 1] == 0L && var11.yVals[var11.getSize() - 1] == 0.0) {
               var11.xVals[var11.getSize() - 1] = var11.xVals[var11.getSize() - 2];
               var11.yVals[var11.getSize() - 1] = var11.yVals[var11.getSize() - 2];
            }

            var1.addMainChartDataset(var11);
         }
      }
   }
}
