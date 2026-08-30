package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.FingerprintsMap;
import com.strategyquant.tradinglib.results.StrategyFingerprint;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImproveDatabankFilter extends AbstractDatabankFilter {
   public static final Logger Log = LoggerFactory.getLogger(ImproveDatabankFilter.class);
   private static final String LOCK_IMPROVEDATABANKFILTER = "ImproveDatabankFilter";
   private boolean dismissTooSimilarStrategies;
   private Int2ObjectOpenHashMap<FingerprintsMap> fingerprintsMaps = new Int2ObjectOpenHashMap();

   public ImproveDatabankFilter(int var1, boolean var2) {
      super(var1);
      this.dismissTooSimilarStrategies = var2;
   }

   private String getBaseName(String var1) {
      return var1.split("- Improv")[0];
   }

   private ArrayList<String> getStrategies(String var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = this.databank.getRecordKeys();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         String var5 = (String)var3.get(var4);
         if (var5.startsWith(var1) && var5.contains(" - Improv")) {
            var2.add(var5);
         }
      }

      return var2;
   }

   private ArrayList<String> getStrategiesNoLock(String var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = this.databank.getRecordKeysNoLock();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         String var5 = (String)var3.get(var4);
         if (var5.startsWith(var1) && var5.contains(" - Improv")) {
            var2.add(var5);
         }
      }

      return var2;
   }

   private boolean checkFingerprint(ResultsGroup var1, StrategyFingerprint var2, String var3) {
      var2.fitness = var1.getFitness();
      FingerprintsMap var4 = (FingerprintsMap)this.fingerprintsMaps.get(SQUtils.betterHashCode(var3));
      return var4 != null && var4.checkContainsExact(var2);
   }

   private boolean filterOutUsingFingerprint(ResultsGroup var1, StrategyFingerprint var2, String var3) {
      var2.fitness = var1.getFitness();
      FingerprintsMap var4 = (FingerprintsMap)this.fingerprintsMaps.get(SQUtils.betterHashCode(var3));
      if (var4 != null) {
         if (var4.checkContainsExact(var2)) {
            return true;
         }

         StrategyFingerprint var5 = var4.findSimilarFingerprint(var2);
         if (var5 != null) {
            try {
               if (this.databank.contains(var5.recordName)) {
                  this.databank.removeNoLockNoException(var5.recordName, true, false, true, "ImproveDatabankFilter");
                  var1.mainResult().set("ReplacedWorseStrategy", 10007);
               }

               var4.remove(var5.recordName);
            } catch (Exception var7) {
               Log.error("Cannot remove similar, but worse record", var7);
            }

            this.removeWorseStrategy(var5.recordName);
            this.databank.refreshGrid();
         }
      }

      return false;
   }

   public boolean getDismissTooSimilarStrategies() {
      return this.dismissTooSimilarStrategies;
   }

   protected void addFingerprint(ResultsGroup var1, String var2) {
      if (this.dismissTooSimilarStrategies) {
         if (var1 != null) {
            int var3 = SQUtils.betterHashCode(var2);
            FingerprintsMap var4 = (FingerprintsMap)this.fingerprintsMaps.get(var3);
            if (var4 == null) {
               var4 = new FingerprintsMap();
               this.fingerprintsMaps.put(var3, var4);
            }

            var4.add(var1.getName(), var1.getFitness(), var1.getFingerprint());
         }
      }
   }

   @Override
   public boolean shouldAddStrategy(ResultsGroup var1, double var2) {
      String var4 = this.getBaseName(var1.getName());
      if (this.dismissTooSimilarStrategies) {
         StrategyFingerprint var5 = var1.getFingerprint();
         if (var5 != null && this.checkFingerprint(var1, var5, var4)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected boolean addStrategy(ResultsGroup var1, double var2) throws Exception {
      String var4 = this.getBaseName(var1.getName());
      if (this.dismissTooSimilarStrategies) {
         StrategyFingerprint var5 = var1.getFingerprint();
         if (var5 != null && this.filterOutUsingFingerprint(var1, var5, var4)) {
            return false;
         }
      }

      ArrayList var8 = this.getStrategiesNoLock(var4);
      if (var8.size() < this.maxRecords) {
         this.databank.refreshGrid();
         if (this.dismissTooSimilarStrategies) {
            this.addFingerprint(var1, var4);
         }

         return true;
      } else {
         int var6 = var8.size() - this.maxRecords + 1;
         if (var6 > 1) {
            throw new Exception(L.t("This shouldn't happen! Count: %d", new Object[]{var6}));
         }

         String var7 = this.getStrategyWithWorseRank(var8, var2);
         if (var7 != null) {
            this.databank.removeNoLock(var7, true, false, true, "ImproveDatabankFilter");
            this.removeWorseStrategy(var7);
            if (this.dismissTooSimilarStrategies) {
               this.addFingerprint(var1, var4);
            }

            this.databank.refreshGrid();
            return true;
         } else {
            return false;
         }
      }
   }
}
