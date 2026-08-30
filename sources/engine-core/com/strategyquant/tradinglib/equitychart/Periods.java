package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SampleTypes;
import java.util.ArrayList;
import java.util.List;

public class Periods {
   public List<TimePeriod> getAvailablePeriods(OrdersList var1, String var2) throws Exception {
      ArrayList var3 = new ArrayList();
      TimePeriod var4 = null;

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var5 = var1.get(var6);
         if (var4 != null) {
            if (var4.name == SampleTypes.typeToString(var5.SampleType)) {
               var4.to = this.getX(var5.CloseTime, var6, var2);
            } else {
               var4.to = this.getX(var5.OpenTime, var6, var2);
               var3.add(var4);
               var4 = null;
            }
         }

         if (var4 == null) {
            var4 = new TimePeriod();
            var4.from = this.getX(var5.OpenTime, var6, var2);
            var4.to = this.getX(var5.CloseTime, var6, var2);
            var4.name = SampleTypes.typeToString(var5.SampleType);
            var4.type = var5.SampleType;
            var4.note = var5.Ticket + "";
         }
      }

      if (var4 != null) {
         var3.add(var4);
      }

      return var3;
   }

   private long getX(long var1, int var3, String var4) {
      return var4.equals("time") ? SQTime.getDateInMs(var1) : var3;
   }

   public List<TimePeriod> filter(List<TimePeriod> var1, byte var2) {
      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         TimePeriod var5 = (TimePeriod)var1.get(var4);
         if (var2 == 40 && SampleTypes.isISV(var5.type)) {
            var3.add(var5);
         } else if (var2 == 20 && SampleTypes.isOOS(var5.type)) {
            var3.add(var5);
         }
      }

      return var3;
   }
}
