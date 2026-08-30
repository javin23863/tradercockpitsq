package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import it.unimi.dsi.fastutil.longs.Long2IntAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;

public class YearMarkers {
   public static final Color COLOR_LIGHT_GRAY = new Color(208, 208, 208);
   private Calendar cal = Calendar.getInstance();

   public void print(EquityChart var1, OrdersList var2, String var3) {
      ArrayList var4 = this.getAvailablePeriods(var2, var3);
      if (!var4.isEmpty()) {
         for (TimePeriod var6 : var4) {
            AreaFill var7 = new AreaFill();
            var7.text = var6.name;
            var7.borderColor = "#D0D0D0";
            var7.textColor = "#D0D0D0";
            var7.borderColorDark = "#545454";
            var7.textColorDark = "#aaa";
            var7.borderOnly = true;
            var7.xFrom = var6.from;
            var7.xTo = var6.to;
            var7.verticalAlign = "bottom";
            var1.addArea(var7);
         }
      }
   }

   private ArrayList<TimePeriod> getAvailablePeriods(OrdersList var1, String var2) {
      ArrayList var3 = new ArrayList();
      int var4 = -1;
      int var5 = -1;
      if (var2.equals("time")) {
         Long2IntAVLTreeMap var9 = new Long2IntAVLTreeMap();

         for (int var10 = 0; var10 < var1.size(); var10++) {
            Order var8 = var1.get(var10);
            long var6 = SQTime.getDateInMs(var8.CloseTime);
            var4 = SQTime.getFullYear(var8.CloseTime);
            var9.put(var6, var4);
         }

         long var23 = 0L;

         for (ObjectBidirectionalIterator var12 = var9.long2IntEntrySet().iterator(); var12.hasNext(); var23++) {
            Entry var13 = (Entry)var12.next();
            long var18 = var13.getLongKey();
            var4 = var13.getIntValue();
            if (var3.isEmpty()) {
               TimePeriod var14 = new TimePeriod();
               var14.name = var4 + "";
               var14.from = var2.equals("time") ? this.getYearInMs(var18) : var23;
               var14.to = 0L;
               var3.add(var14);
            } else if (var5 != var4) {
               TimePeriod var27 = (TimePeriod)var3.get(var3.size() - 1);
               var27.to = var2.equals("time") ? this.getYearInMs(var18) : var23;
               var27 = new TimePeriod();
               var27.name = var4 + "";
               var27.from = var2.equals("time") ? this.getYearInMs(var18) : var23;
               var27.to = 0L;
               var3.add(var27);
            }

            var5 = var4;
         }
      } else {
         for (int var21 = 0; var21 < var1.size(); var21++) {
            Order var20 = var1.get(var21);
            long var19 = SQTime.getDateInMs(var20.CloseTime);
            var4 = SQTime.getFullYear(var20.CloseTime);
            if (var3.isEmpty()) {
               TimePeriod var24 = new TimePeriod();
               var24.name = var4 + "";
               var24.from = var21;
               var24.to = 0L;
               var3.add(var24);
            } else if (var5 != var4) {
               TimePeriod var25 = (TimePeriod)var3.get(var3.size() - 1);
               var25.to = var21;
               var25 = new TimePeriod();
               var25.name = var4 + "";
               var25.from = var21;
               var25.to = 0L;
               var3.add(var25);
            }

            var5 = var4;
         }
      }

      if (!var3.isEmpty() && ((TimePeriod)var3.get(var3.size() - 1)).to == 0L) {
         if (var2.equals("time")) {
            long[] var22 = EquityChartUtils.dateRange(var1);
            ((TimePeriod)var3.get(var3.size() - 1)).to = var22[1];
         } else {
            ((TimePeriod)var3.get(var3.size() - 1)).to = var1.size() - 1;
         }
      }

      return var3;
   }

   private long getYearInMs(long var1) {
      int var3 = SQTime.getFullYear(var1);
      this.cal.set(var3, 0, 1, 0, 0, 0);
      return this.cal.getTimeInMillis();
   }
}
