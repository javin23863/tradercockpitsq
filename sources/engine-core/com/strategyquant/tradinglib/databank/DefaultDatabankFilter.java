package com.strategyquant.tradinglib.databank;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.FingerprintsMap;
import com.strategyquant.tradinglib.results.StrategyFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDatabankFilter extends AbstractDatabankFilter {
   public static final Logger Log = LoggerFactory.getLogger(DefaultDatabankFilter.class);
   private static final String LOCK_DEFAULTDATABANKFILTER = "DefaultDatabankFilter";
   private boolean dismissTooSimilarStrategies;
   private FingerprintsMap fingerprintsMap = new FingerprintsMap();

   public DefaultDatabankFilter(int var1, boolean var2) {
      super(var1);
      this.dismissTooSimilarStrategies = var2;
   }

   private boolean filterOutUsingFingerprint(ResultsGroup var1, StrategyFingerprint var2) {
      var2.fitness = var1.getFitness();
      if (this.fingerprintsMap.checkContainsExact(var2)) {
         return true;
      }

      StrategyFingerprint var3 = this.fingerprintsMap.findSimilarFingerprint(var2);
      if (var3 != null) {
         try {
            if (this.databank.contains(var3.recordName)) {
               this.databank.removeNoLockNoException(var3.recordName, true, false, true, "DefaultDatabankFilter");
               var1.mainResult().set("ReplacedWorseStrategy", 10007);
            }

            this.fingerprintsMap.remove(var3.recordName);
         } catch (Exception var5) {
            Log.error("Cannot remove similar, but worse record", var5);
         }

         this.removeWorseStrategy(var3.recordName);
         this.databank.refreshGrid();
      }

      return false;
   }

   private boolean checkFingerprint(ResultsGroup var1, StrategyFingerprint var2) {
      var2.fitness = var1.getFitness();
      return this.fingerprintsMap.checkContainsExact(var2);
   }

   public boolean getDismissTooSimilarStrategies() {
      return this.dismissTooSimilarStrategies;
   }

   protected void addFingerprint(ResultsGroup var1) {
      if (this.dismissTooSimilarStrategies) {
         if (var1 != null) {
            this.fingerprintsMap.add(var1.getName(), var1.getFitness(), var1.getFingerprint());
         }
      }
   }

   protected void removeFingerprint(String var1) {
      if (this.dismissTooSimilarStrategies) {
         this.fingerprintsMap.remove(var1);
      }
   }

   @Override
   public void onRemove(String var1) {
      super.onRemove(var1);
      if (this.dismissTooSimilarStrategies) {
         this.fingerprintsMap.remove(var1);
      }
   }

   @Override
   public boolean shouldAddStrategy(ResultsGroup var1, double var2) {
      if (this.dismissTooSimilarStrategies) {
         StrategyFingerprint var4 = var1.getFingerprint();
         if (var4 != null && this.checkFingerprint(var1, var4)) {
            return false;
         }
      }

      if (this.databank.getRecordsSizeNoLock() >= this.maxRecords && this.maxRecords > 0) {
         double var6 = this.getWorstRankValue();
         return var2 > var6;
      } else {
         return true;
      }
   }

   @Override
   protected boolean addStrategy(ResultsGroup var1, double var2) throws Exception {
      if (this.dismissTooSimilarStrategies) {
         StrategyFingerprint var4 = var1.getFingerprint();
         if (var4 != null && this.filterOutUsingFingerprint(var1, var4)) {
            return false;
         }
      }

      while (this.databank.getRecordsSizeNoLock() >= this.maxRecords && this.maxRecords > 0) {
         String var7 = this.getWorstStrategy();
         double var5 = this.getWorstRankValue();
         if (!(var2 > var5)) {
            Log.debug("Strategy{} not added, its fitness was lower than worst fitness in databank", var1.getName());
            this.databank.refreshGrid();
            return false;
         }

         this.databank.removeNoLockNoException(var7, true, false, true, "DefaultDatabankFilter");
         this.removeWorseStrategy(var7);
         var1.mainResult().set("ReplacedWorseStrategy", 10007);
         Log.debug("Strategy {} replaced by better (1)", var1.getName());
      }

      if (this.dismissTooSimilarStrategies) {
         this.fingerprintsMap.add(var1.getName(), var1.getFitness(), var1.getFingerprint());
      }

      this.databank.refreshGrid();
      return true;
   }
}
