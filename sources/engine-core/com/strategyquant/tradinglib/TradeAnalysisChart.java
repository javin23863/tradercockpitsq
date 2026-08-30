package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.debug.Debugger;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TradeAnalysisChart extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger(TradeAnalysisChart.class);
   public String name = "?";

   @Override
   public String toString() {
      return this.name;
   }

   public abstract AbstractChart draw(OrdersList var1, byte var2, byte var3);

   protected String monthToString(int var1) {
      switch (var1) {
         case 0:
            return "Jan";
         case 1:
            return "Feb";
         case 2:
            return "Mar";
         case 3:
            return "Apr";
         case 4:
            return "May";
         case 5:
            return "Jun";
         case 6:
            return "Jul";
         case 7:
            return "Aug";
         case 8:
            return "Sep";
         case 9:
            return "Oct";
         case 10:
            return "Nov";
         case 11:
            return "Dec";
         default:
            return "?";
      }
   }

   protected String dayToString(int var1) {
      switch (var1) {
         case 0:
            return "Sun";
         case 1:
            return "Mon";
         case 2:
            return "Tue";
         case 3:
            return "Wed";
         case 4:
            return "Thu";
         case 5:
            return "Fri";
         case 6:
            return "Sat";
         default:
            return "?";
      }
   }

   protected ArrayList<TimeDuration> calculateTimeDurationScale(OrdersList var1) {
      ArrayList var2 = new ArrayList();
      var2.clear();
      int var3 = this.getMaxTimeInTrade(var1);
      if (var3 < 3600) {
         var2.add(new TimeDurationMin(1));
         var2.add(new TimeDurationMin(2));
         var2.add(new TimeDurationMin(3));
         var2.add(new TimeDurationMin(4));
         var2.add(new TimeDurationMin(6));
         var2.add(new TimeDurationMin(8));
         var2.add(new TimeDurationMin(10));
         var2.add(new TimeDurationMin(15));
         var2.add(new TimeDurationMin(20));
         var2.add(new TimeDurationMin(30));
         var2.add(new TimeDurationMin(40));
         var2.add(new TimeDurationMin(50));
         var2.add(new TimeDurationMin(55));
         var2.add(new TimeDurationHour(1));
      } else if (var3 < 86400) {
         var2.add(new TimeDurationMin(5));
         var2.add(new TimeDurationMin(10));
         var2.add(new TimeDurationMin(15));
         var2.add(new TimeDurationMin(30));
         var2.add(new TimeDurationHour(1));
         var2.add(new TimeDurationHour(2));
         var2.add(new TimeDurationHour(4));
         var2.add(new TimeDurationHour(8));
         var2.add(new TimeDurationHour(10));
         var2.add(new TimeDurationHour(12));
         var2.add(new TimeDurationHour(16));
         var2.add(new TimeDurationHour(18));
         var2.add(new TimeDurationHour(20));
         var2.add(new TimeDurationDay(1));
      } else if (var3 < 691200) {
         var2.add(new TimeDurationMin(5));
         var2.add(new TimeDurationMin(10));
         var2.add(new TimeDurationMin(15));
         var2.add(new TimeDurationMin(30));
         var2.add(new TimeDurationHour(1));
         var2.add(new TimeDurationHour(2));
         var2.add(new TimeDurationHour(4));
         var2.add(new TimeDurationHour(8));
         var2.add(new TimeDurationHour(16));
         var2.add(new TimeDurationHour(18));
         var2.add(new TimeDurationHour(22));
         var2.add(new TimeDurationDay(1));
         var2.add(new TimeDurationDay(4));
         var2.add(new TimeDurationDay(8));
      } else {
         var2.add(new TimeDurationMin(5));
         var2.add(new TimeDurationMin(10));
         var2.add(new TimeDurationMin(15));
         var2.add(new TimeDurationMin(30));
         var2.add(new TimeDurationHour(1));
         var2.add(new TimeDurationHour(4));
         var2.add(new TimeDurationHour(8));
         var2.add(new TimeDurationHour(16));
         var2.add(new TimeDurationDay(1));
         var2.add(new TimeDurationDay(4));
         var2.add(new TimeDurationDay(8));
         var2.add(new TimeDurationDay(16));
         var2.add(new TimeDurationDay(30));
         var2.add(new TimeDurationOther());
      }

      return var2;
   }

   private int getMaxTimeInTrade(OrdersList var1) {
      int var2 = -1;
      int var3 = -1;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var5 = var1.get(var4);
         var3 = var5.Duration;
         if (var3 > var2) {
            var2 = var3;
         }
      }

      return var2;
   }
}
