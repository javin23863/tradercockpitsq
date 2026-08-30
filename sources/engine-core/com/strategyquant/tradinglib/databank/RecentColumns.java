package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecentColumns {
   private static final Logger Log = LoggerFactory.getLogger(RecentColumns.class);
   private static final String filePath = SQPaths.settingsDirPath + "/RecentRankingCols.txt";
   private static int MAX_COLUMNS = 8;
   private static RecentColumns instance;
   private ArrayList<String> recentCols = new ArrayList<>();

   private RecentColumns() {
      this.recentCols.clear();
      Log.debug("Loading recently used ranking columns...");

      try {
         File var1 = new File(filePath);
         if (var1.exists()) {
            String var2 = SQUtils.fileToString(var1);

            for (String var6 : var2.split("\n")) {
               var6 = var6.trim();
               if (!var6.isEmpty()) {
                  this.recentCols.add(var6);
               }
            }
         }
      } catch (Exception var7) {
         Log.warn("File '" + filePath + "' cannot be loaded.");
      }

      instance = this;
   }

   private static RecentColumns get() {
      if (instance == null) {
         new RecentColumns();
      }

      return instance;
   }

   private void save() {
      PrintWriter var1 = null;

      try {
         var1 = new PrintWriter(SQUtils.createFile(filePath));

         for (String var3 : this.recentCols) {
            if (var3 != null && !var3.isEmpty()) {
               var1.println(var3);
            }
         }

         var1.close();
      } catch (Exception var4) {
         Log.error("Cannot save recent ranking columns. ", var4);
         if (var1 != null) {
            var1.close();
         }
      }
   }

   private void _updateRecentCols(String[] var1) {
      for (String var5 : var1) {
         var5 = var5.trim();
         int var6 = this.recentCols.indexOf(var5);
         if (var6 >= 0) {
            this.recentCols.remove(var6);
         }

         this.recentCols.add(0, var5);
      }

      if (this.recentCols.size() > MAX_COLUMNS) {
         this.recentCols.subList(MAX_COLUMNS, this.recentCols.size()).clear();
      }

      this.save();
   }

   public static List<String> getRecentCols() {
      List var0 = Arrays.asList("NetProfit", "NumberOfTrades", "Drawdown", "DrawdownPct", "ReturnDDRatio", "ProfitFactor", "AvgBarsInTrade", "WinningPct");
      ArrayList var1 = new ArrayList();

      for (String var3 : get().recentCols) {
         if (checkColumn(var3)) {
            var1.add(var3);
         }
      }

      for (String var5 : var0) {
         if (checkColumn(var5)) {
            var1.add(var5);
         }
      }

      return var1.stream().distinct().collect(Collectors.toList());
   }

   private static boolean checkColumn(String var0) {
      try {
         DatabankColumns.get().findClassByName(var0);
      } catch (Exception var4) {
         try {
            WalkForwardColumns.get().findClassByName(var0);
         } catch (Exception var3) {
            return false;
         }
      }

      return true;
   }

   public static void updateRecentCols(String[] var0) {
      get()._updateRecentCols(var0);
   }
}
