package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxNewHighDuration {
   public static final Logger Log = LoggerFactory.getLogger("MaxNewHighDuration");

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, byte var5, byte var6, byte var7, String var8) {
      try {
         Result var9 = var2.subResult(var3);
         int var10 = var9.stats(var5, var6, var7).getInt("MaxNewHighDuration");
         long var11 = var9.stats(var5, var6, var7).getLong("MaxNewHighDurationFrom");
         long var13 = var9.stats(var5, var6, var7).getLong("MaxNewHighDurationTo");
         if (var10 == 0) {
            return;
         }

         long var15 = -1L;
         long var17 = -1L;
         if (var8.equals("time")) {
            var15 = var11;
            var17 = var13;
         } else {
            for (int var19 = 0; var19 < var4.size(); var19++) {
               if (var4.get(var19).CloseTime >= var11 && var15 == -1L) {
                  var15 = var19;
               }

               if (var4.get(var19).CloseTime <= var13) {
                  var17 = var19;
               }
            }
         }

         this.printMarker(var1, var15, var17, var10);
      } catch (Exception var20) {
         Log.error("Exc.", var20);
      }
   }

   private void printMarker(EquityChart var1, long var2, long var4, int var6) {
      AreaFill var7 = new AreaFill();
      var7.text = String.format("Stagnation V2: %d days", var6);
      var7.verticalAlign = "bottom";
      var7.color = "#FFC0C0";
      var7.textColor = "#FF0000";
      var7.xFrom = var2;
      var7.xTo = var4;
      var1.addArea(var7);
   }
}
