package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByCloseTime;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquityChartUtils {
   private static final Comparator<Order> comparatorByCloseTime = new OrderComparatorByCloseTime();
   private static final String[] colors = new String[]{
      "#3366CC",
      "#980048",
      "#ff9062",
      "#007a38",
      "#a89f00",
      "#64ff88",
      "#ffdd6f",
      "#4400ae",
      "#415fff",
      "#ff4969",
      "#ff30ba",
      "#63ffba",
      "#884ffe",
      "#6d0055",
      "#d8a200",
      "#eb5200",
      "#56ee38",
      "#fff872",
      "#df6697",
      "#0243e8",
      "#5a7008",
      "#00318b",
      "#d7ff5b",
      "#dd87d7",
      "#d800c4",
      "#b3b700",
      "#01ba6b",
      "#9120d6",
      "#899936",
      "#cf0040",
      "#cd9eff",
      "#b763ff",
      "#00e39c",
      "#e6ff86",
      "#4f0659",
      "#7e9e00",
      "#6ed000",
      "#01cfa3",
      "#ffa84d",
      "#c90023",
      "#6a2978",
      "#9ae691",
      "#868500",
      "#0032ab",
      "#398f00",
      "#dac05e",
      "#322fd5",
      "#971d00",
      "#ff615b",
      "#ffdd3d",
      "#4c0084",
      "#fb73ff",
      "#9c4d00",
      "#756dc6",
      "#980077",
      "#ff487b",
      "#aeffa4",
      "#005cd2",
      "#ff7b36",
      "#02c93f",
      "#92c000",
      "#b584ff"
   };
   private static final String[] colors_dark = new String[]{
      "#556EE6",
      "#980048",
      "#ff9062",
      "#007a38",
      "#a89f00",
      "#64ff88",
      "#ffdd6f",
      "#4400ae",
      "#415fff",
      "#ff4969",
      "#ff30ba",
      "#63ffba",
      "#884ffe",
      "#6d0055",
      "#d8a200",
      "#eb5200",
      "#56ee38",
      "#fff872",
      "#df6697",
      "#0243e8",
      "#5a7008",
      "#00318b",
      "#d7ff5b",
      "#dd87d7",
      "#d800c4",
      "#b3b700",
      "#01ba6b",
      "#9120d6",
      "#899936",
      "#cf0040",
      "#cd9eff",
      "#b763ff",
      "#00e39c",
      "#e6ff86",
      "#4f0659",
      "#7e9e00",
      "#6ed000",
      "#01cfa3",
      "#ffa84d",
      "#c90023",
      "#6a2978",
      "#9ae691",
      "#868500",
      "#0032ab",
      "#398f00",
      "#dac05e",
      "#322fd5",
      "#971d00",
      "#ff615b",
      "#ffdd3d",
      "#4c0084",
      "#fb73ff",
      "#9c4d00",
      "#756dc6",
      "#980077",
      "#ff487b",
      "#aeffa4",
      "#005cd2",
      "#ff7b36",
      "#02c93f",
      "#92c000",
      "#b584ff"
   };
   public static final Logger Log = LoggerFactory.getLogger(EquityChartUtils.class);

   public static MainChartDataset generateDataset(OrdersList var0, String var1) {
      if (var1.equals("time")) {
         long[] var2 = dateRange(var0);
         return generateDailyDataset(var0, var2[0], var2[1]);
      } else {
         return generateTradeDataset(var0);
      }
   }

   public static MainChartDataset generateTradeDataset(OrdersList var0) {
      MainChartDataset var1 = new MainChartDataset("main", var0.size());
      double var2 = 0.0;
      var0.sort(comparatorByCloseTime);

      for (int var4 = 0; var4 < var0.size(); var4++) {
         var2 += var0.get(var4).PL;
         var1.addValue(var4, var2);
      }

      return var1;
   }

   private static MainChartDataset generateDailyDataset(OrdersList var0, long var1, long var3) {
      MainChartDataset var5 = new MainChartDataset("main", var0.isEmpty() ? 0 : getDaysBetween(var1, var3));
      if (var0.isEmpty()) {
         return var5;
      }

      Long2DoubleOpenHashMap var8 = new Long2DoubleOpenHashMap();

      for (int var10 = 0; var10 < var0.size(); var10++) {
         Order var9 = var0.get(var10);
         long var6 = SQTime.getDateInMs(var9.CloseTime);
         if (var8.containsKey(var6)) {
            var8.put(var6, var8.get(var6) + var9.PL);
         } else {
            var8.put(var6, var9.PL);
         }
      }

      long var12 = SQTime.getDateInMs(var1);
      double var13 = 0.0;

      do {
         if (var8.containsKey(var12)) {
            var13 += var8.get(var12);
         }

         var5.addValue(var12, var13);
         var12 = SQTime.addDays(var12, 1);
      } while (var12 <= var3);

      return var5;
   }

   public static int getDaysBetween(long var0, long var2) {
      int var4 = 0;
      long var5 = SQTime.getDateInMs(var0);

      do {
         var4++;
         var5 = SQTime.addDays(var5, 1);
      } while (var5 <= var2);

      return var4;
   }

   public static List<MainChartDataset> generatePorfolioDatasets(ResultsGroup var0, byte var1, byte var2, String var3, String var4) throws Exception {
      ArrayList var5 = new ArrayList();
      OrdersList var6 = var0.orders().filter("Portfolio", var1, var2, true);
      if (var3 == null || var3.equals("Portfolio only")) {
         MainChartDataset var7 = generateDataset(var6, var4);
         var7.name = "Portfolio";
         var7.title = "Portfolio";
         var5.add(var7);
      }

      if (var3 == null || var3.equals("Portfolio parts only")) {
         String var13 = var0.getMainResultKey();
         var6 = var0.orders().filter(var13, var1, var2, true);
         MainChartDataset var8 = generateDataset(var6, var4);
         var8.name = var13;
         var8.title = var13;
         var5.add(var8);

         for (String var10 : var0.getResultKeys()) {
            if (!var10.equals("Portfolio") && !var10.equals(var13) && !var10.startsWith("WF:") && !var10.startsWith("CrossCheck_")) {
               var6 = var0.orders().filter(var10, var1, var2, true);
               var8 = generateDataset(var6, var4);
               var8.name = var10;
               var8.title = var10;
               var5.add(var8);
            }
         }
      }

      return var5;
   }

   public static String getColor(int var0, boolean var1) {
      if (var1) {
         return var0 < colors_dark.length ? colors_dark[var0] : randomColor(var0);
      } else {
         return var0 < colors.length ? colors[var0] : randomColor(var0);
      }
   }

   public static long[] dateRange(OrdersList var0) {
      return dateRange(var0, false);
   }

   public static long[] dateRange(OrdersList var0, boolean var1) {
      long[] var2 = new long[]{0L, 0L};
      if (var0.isEmpty()) {
         return var2;
      }

      long var3 = -1L;
      long var5 = -1L;

      for (int var8 = 0; var8 < var0.size(); var8++) {
         Order var7 = var0.get(var8);
         long var9 = var1 ? var7.OpenTime : var7.CloseTime;
         if (var3 == -1L || var3 > var9) {
            var3 = var9;
         }

         if (var5 < var7.CloseTime) {
            var5 = var7.CloseTime;
         }
      }

      var2[0] = var3;
      var2[1] = var5;
      return var2;
   }

   public static String randomColor(int var0) {
      Random var1 = new Random(var0);
      int var2 = var1.nextInt(16777216);
      return String.format("#%06x", var2);
   }
}
