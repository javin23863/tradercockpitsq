package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Equity {
   public static final Logger Log = LoggerFactory.getLogger(Equity.class);
   long msFrom;
   long msTo;
   int daysCount;

   public void print(EquityChart var1, OrdersList var2, ResultsGroup var3, String var4, String var5, byte var6, byte var7, String var8) {
      if (var6 == 0 && var7 == 127) {
         if (var8.equals("daily")) {
            this.printDailyEquity(var1, var2, var3, var4, var5, var6, var7);
         } else if (var8.equals("maemfe")) {
            this.printMaemfe(var1, var2, var3, var4, var5, var6, var7);
         }
      }
   }

   public void printMaemfe(EquityChart var1, OrdersList var2, ResultsGroup var3, String var4, String var5, byte var6, byte var7) {
      try {
         MainChartDataset var8 = null;
         MainChartDataset var9 = null;
         if (!this.containsMaemfe(var2)) {
            return;
         }

         if (var4.equals("time")) {
            this.parseDateRange(var1);
            var8 = new MainChartDataset("MAE", this.daysCount);
            var9 = new MainChartDataset("MFE", this.daysCount);
            this.maemfeByTime(var8, var9, var2);
         } else {
            var8 = new MainChartDataset("MAE", var2.size());
            var9 = new MainChartDataset("MFE", var2.size());
            this.maemfeByTrades(var8, var9, var2);
         }

         var8.title = "MAE";
         var8.color = "red";
         var1.addMainChartDataset(var8);
         var9.title = "MFE";
         var9.color = "green";
         var1.addMainChartDataset(var9);
      } catch (Exception var10) {
         Log.error("Error while rendering Daily Equity curve. Exc.", var10);
      }
   }

   private boolean containsMaemfe(OrdersList var1) {
      for (int var3 = 0; var3 < var1.size(); var3++) {
         Order var2 = var1.get(var3);
         if (var2.MAE != 0.0F || var2.MFE != 0.0F) {
            return true;
         }
      }

      return false;
   }

   private void maemfeByTrades(MainChartDataset var1, MainChartDataset var2, OrdersList var3) {
      double var4 = 0.0;

      for (int var7 = 0; var7 < var3.size(); var7++) {
         Order var6 = var3.get(var7);
         var1.addValue(var7, var4 + var6.MAE * -1.0F);
         var2.addValue(var7, var4 + var6.MFE);
         var4 += var6.PL;
      }
   }

   private void maemfeByTime(MainChartDataset var1, MainChartDataset var2, OrdersList var3) {
      Long2DoubleOpenHashMap var4 = new Long2DoubleOpenHashMap();
      Long2DoubleOpenHashMap var5 = new Long2DoubleOpenHashMap();
      Long2DoubleOpenHashMap var6 = new Long2DoubleOpenHashMap();
      double var7 = 0.0;

      for (int var12 = 0; var12 < var3.size(); var12++) {
         Order var9 = var3.get(var12);
         var7 += var9.PL;
         long var10 = SQTime.getDateInMs(var9.CloseTime);
         if (!var5.containsKey(var10)) {
            var5.put(var10, 0.0);
            var6.put(var10, 0.0);
         }

         var4.put(var10, var7);
         var5.put(var10, var5.get(var10) + var9.MAE * -1.0F);
         var6.put(var10, var6.get(var10) + var9.MFE);
      }

      long var14 = SQTime.getDateInMs(this.msFrom);
      double var15 = 0.0;

      do {
         if (var4.containsKey(var14)) {
            var1.addValue(var14, var15 + var5.get(var14));
            var2.addValue(var14, var15 + var6.get(var14));
            var15 = var4.get(var14);
         } else if (var1.index < 0) {
            var1.addValue(var14, 0.0);
            var2.addValue(var14, 0.0);
         } else {
            var1.addValue(var14, var1.yVals[var1.index]);
            var2.addValue(var14, var2.yVals[var2.index]);
         }

         var14 = SQTime.addDays(var14, 1);
      } while (var14 <= this.msTo);
   }

   public void printDailyEquity(EquityChart var1, OrdersList var2, ResultsGroup var3, String var4, String var5, byte var6, byte var7) {
      try {
         Result var8 = var3.subResult(var5);
         Long2FloatRBTreeMap var9 = var8.getWorstDailyEquity();
         if (var9 == null || var9.isEmpty() || var9.size() < 5) {
            return;
         }

         MainChartDataset var10 = null;
         if (var4.equals("time")) {
            if (var3.isStockpickerStrategy()) {
               var10 = new MainChartDataset("DailyEquity", var9.size());
               this.dailyEquityByTimeAll(var10, var9);
            } else {
               this.parseDateRange(var1);
               var10 = new MainChartDataset("DailyEquity", this.daysCount);
               this.dailyEquityByTime(var10, var9);
            }
         } else {
            var10 = new MainChartDataset("DailyEquity", var2.size());
            this.dailyEquityByTrades(var10, var9, var2);
         }

         var10.title = "Daily Equity";
         var10.color = "red";
         var1.addMainChartDataset(var10);
      } catch (Exception var11) {
         Log.error("Error while rendering Daily Equity curve. Exc.", var11);
      }
   }

   private void dailyEquityByTimeAll(MainChartDataset var1, Long2FloatRBTreeMap var2) {
      ObjectBidirectionalIterator var3 = var2.long2FloatEntrySet().iterator();

      while (var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         long var5 = var4.getLongKey();
         float var7 = var4.getFloatValue();
         var1.addValue(var5, var7);
      }
   }

   private void dailyEquityByTrades(MainChartDataset var1, Long2FloatRBTreeMap var2, OrdersList var3) {
      double var6 = Float.MAX_VALUE;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var8 = var3.get(var9);
         double var4 = this.getWorstEquity(var8.OpenTime, var8.CloseTime, var2);
         if (var4 == Float.MAX_VALUE) {
            var4 = var6;
         } else {
            var6 = var4;
         }

         var1.addValue(var9, var4);
      }
   }

   private double getWorstEquity(long var1, long var3, Long2FloatRBTreeMap var5) {
      double var6 = 0.0;
      long var8 = SQTime.getDateInMs(var1);
      boolean var10 = true;

      do {
         if (var5.containsKey(var8) && (var10 || var5.get(var8) < var6)) {
            var6 = var5.get(var8);
            var10 = false;
         }

         var8 = SQTime.addDays(var8, 1);
      } while (var8 <= var3);

      return var6;
   }

   private void dailyEquityByTime(MainChartDataset var1, Long2FloatRBTreeMap var2) throws Exception {
      if (var1.length != 0) {
         long var3 = SQTime.getDateInMs(this.msFrom);
         double var5 = Float.MAX_VALUE;

         do {
            if (var2.containsKey(var3)) {
               double var7 = var2.get(var3);
               if (var7 == Float.MAX_VALUE) {
                  var7 = var5;
               } else {
                  var5 = var7;
               }

               if (var7 == Float.MAX_VALUE) {
                  var7 = 0.0;
               }

               var1.addValue(var3, var7);
            } else if (var1.index < 0) {
               var1.addValue(var3, 0.0);
            } else {
               var1.addValue(var3, var1.yVals[var1.index]);
            }

            var3 = SQTime.addDays(var3, 1);
         } while (var3 <= this.msTo);
      }
   }

   private void parseDateRange(EquityChart var1) throws Exception {
      ArrayList var2 = var1.getMainChartDatasets();
      if (var2.isEmpty()) {
         throw new Exception(L.t("Cannot get start-end time for computing daily equity.", new Object[0]));
      }

      MainChartDataset var3 = (MainChartDataset)var2.get(0);
      if (var3.length == 0) {
         this.msFrom = -1L;
         this.msTo = -1L;
         this.daysCount = 0;
      } else {
         this.msFrom = var3.xVals[0];
         this.msTo = var3.xVals[var3.xVals.length - 1];
         this.daysCount = EquityChartUtils.getDaysBetween(this.msFrom, this.msTo);
      }
   }
}
