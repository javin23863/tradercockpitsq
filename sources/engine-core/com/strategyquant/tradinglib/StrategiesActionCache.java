package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategiesActionCache {
   private static final Logger Log = LoggerFactory.getLogger(StrategiesActionCache.class);
   private static final String lockName = "StrategiesActionCache";
   private static ArrayList<ResultsGroup> strategies = new ArrayList<>();

   public static void cutFrom(Databank var0, String[] var1) throws Exception {
      boolean var2 = false;
      strategies.clear();

      for (int var3 = 0; var3 < var1.length; var3++) {
         String var4 = var1[var3];
         ResultsGroup var5 = null;

         try {
            var5 = var0.getLocked(var4, "StrategiesActionCache");
            if (var5.specialValues().containsKey(SpecialValues.IsMergedPortfolio) && var5.getMergedResults() == null) {
               var5.setMergedResults(StrategyMerger.getSplitResults(var5));
            }

            strategies.add(var5.clone());

            try {
               var0.removeNoLock(var4, true, false, false, "StrategiesActionCache");
            } catch (Exception var11) {
               Log.error("Cannot remove cut strategy '" + var4 + "'", var11);
            }
         } catch (Exception var12) {
            var2 = true;
            Log.error("Cannot cut strategy '" + var4 + "'", var12);
         } finally {
            if (var5 != null) {
               var5.releaseLock("StrategiesActionCache");
            }
         }
      }

      var0.notifyListeners("removed");
      var0.updateBestResults();
      var0.refreshGrid();
      if (var2) {
         throw new Exception("Some of the strategies were not copied. Check log file for further details");
      }
   }

   public static void copyFrom(Databank var0, String[] var1) throws Exception {
      boolean var2 = false;
      strategies.clear();

      for (int var3 = 0; var3 < var1.length; var3++) {
         String var4 = var1[var3];
         ResultsGroup var5 = null;

         try {
            var5 = var0.getLocked(var4, "StrategiesActionCache");
            if (var5.specialValues().containsKey(SpecialValues.IsMergedPortfolio) && var5.getMergedResults() == null) {
               var5.setMergedResults(StrategyMerger.getSplitResults(var5));
            }

            strategies.add(var5.clone());
         } catch (Exception var10) {
            var2 = true;
            Log.error("Cannot cut strategy '" + var4 + "'", var10);
         } finally {
            if (var5 != null) {
               var5.releaseLock("StrategiesActionCache");
            }
         }
      }

      if (var2) {
         throw new Exception("Some of the strategies were not copied. Check log file for further details");
      }
   }

   public static void pasteTo(Databank var0) throws Exception {
      if (strategies.size() == 0) {
         throw new Exception("Nothing to paste");
      }

      for (int var1 = 0; var1 < strategies.size(); var1++) {
         var0.add(strategies.get(var1).clone(), false);
      }

      var0.updateBestResults();
      var0.refreshGrid();
   }
}
