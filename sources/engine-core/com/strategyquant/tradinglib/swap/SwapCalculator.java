package com.strategyquant.tradinglib.swap;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import java.util.ArrayList;
import java.util.HashMap;

public class SwapCalculator {
   private static final HashMap<String, InstrumentInfo> instrumentsMap = new HashMap<>();
   private static final long DayMillis = 86400000L;

   public static double calculate(LiveOrderObj var0, SwapMethod var1) throws Exception {
      if (var1 != null && var1.isUsed()) {
         double var2 = var0.getOpenPrice();
         return calculateSwap(var0.getSymbol(), var2, var0.getOpenTime(), var0.getCloseTime(), var0.getSize(), var1, var0.isLong());
      } else {
         return 0.0;
      }
   }

   public static double calculate(Order var0, SwapMethod var1) throws Exception {
      if (var0.isBalanceOrder() || !var0.isRealOrder()) {
         return 0.0;
      } else if (var1 != null && var1.isUsed()) {
         double var2 = var0.OpenPrice;
         return calculateSwap(var0.Symbol, var2, var0.OpenTime, var0.CloseTime, var0.Size, var1, var0.isLong());
      } else {
         return 0.0;
      }
   }

   private static double calculateSwap(String var0, double var1, long var3, long var5, double var7, SwapMethod var9, boolean var10) throws Exception {
      InstrumentInfo var11 = getInstrumentInfo(var0);
      double var12 = calculateSwapCost(var11.tickStep, var1, var7, var11.pointValue, var9.getLongSwap(), var9.getType());
      double var14 = calculateSwapCost(var11.tickStep, var1, var7, var11.pointValue, var9.getShortSwap(), var9.getType());
      int var16 = getSwapCount(var9.getRolloutHour(), var9.getRolloutMinute(), var3, var5, var9.getTripleSwapOn());
      double var17 = var12 * var16;
      double var19 = var14 * var16;
      return var10 ? var17 : var19;
   }

   private static double calculateSwapCost(double var0, double var2, double var4, double var6, double var8, String var10) throws Exception {
      switch (var10) {
         case "points":
            return var4 * var6 * var0 * var8;
         case "percent":
            return var4 * var6 * var2 * (var8 / 100.0 / 360.0);
         case "money":
            return var4 * var8;
         default:
            throw new Exception(String.format("Swap type not recognized: %s", var10));
      }
   }

   private static int getSwapCount(int var0, int var1, long var2, long var4, String var6) throws Exception {
      long var7 = SQTime.setTime(var2, var0, var1, 0, 0);
      long var9 = SQTime.setTime(var4, var0, var1, 0, 0);
      int var11 = getTripleDayIndex(var6);
      ArrayList var12 = getTradeDaysOfWeek(var2, var4);
      int var13 = 0;

      for (int var14 = 0; var14 < var12.size(); var14++) {
         int var15 = (Integer)var12.get(var14);
         if ((var14 != 0 || var2 <= var7) && (var14 != var12.size() - 1 || var4 > var9) && var15 != 6 && var15 != 7) {
            if (var15 == var11) {
               var13 += 3;
            } else {
               var13++;
            }
         }
      }

      return var13;
   }

   private static ArrayList<Integer> getTradeDaysOfWeek(long var0, long var2) {
      ArrayList var4 = new ArrayList();
      int var5 = 1;
      long var6 = var0 - var0 % 86400000L;

      while (var6 <= var2) {
         int var8 = SQTime.getDayOfWeek(var6);
         var4.add(var8);
         var6 += 86400000L;
         if (var5++ > 10000) {
            break;
         }
      }

      return var4;
   }

   private static synchronized InstrumentInfo getInstrumentInfo(String var0) throws Exception {
      if (!instrumentsMap.containsKey(var0)) {
         DataInfo var1 = DataManager.getDataInfo("History", var0);
         if (var1 != null) {
            InstrumentInfo var2 = InstrumentManager.getInstrumentInfo(var1.instrument);
            if (var2 != null) {
               instrumentsMap.put(var0, var2);
               return var2;
            }
         }
      }

      return instrumentsMap.get(var0);
   }

   private static int getTripleDayIndex(String var0) {
      if (var0 == null) {
         return -1;
      }

      switch (var0) {
         case "MONDAY":
            return 1;
         case "TUESDAY":
            return 2;
         case "WEDNESDAY":
            return 3;
         case "THURSDAY":
            return 4;
         case "FRIDAY":
            return 5;
         default:
            return -1;
      }
   }
}
