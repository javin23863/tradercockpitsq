package com.strategyquant.tradinglib.results;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerprintsMap {
   public static final Logger Log = LoggerFactory.getLogger("FingerprintsMap");
   private ObjectArrayList<StrategyFingerprint> fingerprints = new ObjectArrayList();
   private IntOpenHashSet strategyNames = new IntOpenHashSet();
   private IntOpenHashSet exactFingerprints = new IntOpenHashSet();

   public boolean checkContainsExact(StrategyFingerprint var1) {
      if (this.exactFingerprints.contains(var1.exactFingerprint)) {
         for (int var2 = 0; var2 < this.fingerprints.size(); var2++) {
            StrategyFingerprint var3 = (StrategyFingerprint)this.fingerprints.get(var2);
            if (var1.exactFingerprint == var3.exactFingerprint) {
            }
         }
      }

      return this.exactFingerprints.contains(var1.exactFingerprint);
   }

   public void add(String var1, double var2, StrategyFingerprint var4) {
      if (var4 != null) {
         this.strategyNames.add(var1.hashCode());
         this.exactFingerprints.add(var4.exactFingerprint);
         var4.recordName = var1;
         var4.fitness = var2;
         this.fingerprints.add(var4);
      }
   }

   public void remove(String var1) {
      if (this.strategyNames.contains(var1.hashCode())) {
         StrategyFingerprint var2 = this.findFingerprint(var1);
         if (var2 != null) {
            this.exactFingerprints.remove(var2.exactFingerprint);
            this.fingerprints.remove(var2);
         }
      }
   }

   private StrategyFingerprint findFingerprint(String var1) {
      for (int var2 = 0; var2 < this.fingerprints.size(); var2++) {
         StrategyFingerprint var3 = (StrategyFingerprint)this.fingerprints.get(var2);
         if (var3.recordName.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public StrategyFingerprint findSimilarFingerprint(StrategyFingerprint var1) {
      double var2 = var1.trades * 0.05;
      int var4 = (int)var2;
      if (var4 == 0) {
         var4 = 1;
      }

      int var5 = var1.trades - var4;
      int var6 = var1.trades + var4;
      double var7 = var1.profit - var1.profit * 0.05;
      double var9 = var1.profit + var1.profit * 0.05;
      if (var7 > var9) {
         double var11 = var7;
         var7 = var9;
         var9 = var11;
      }

      double var18 = var1.drawdown - var1.drawdown * 0.05;
      double var13 = var1.drawdown + var1.drawdown * 0.05;
      if (var18 > var13) {
         double var15 = var18;
         var18 = var13;
         var13 = var15;
      }

      int var19 = var1.tradesHash;

      for (int var16 = 0; var16 < this.fingerprints.size(); var16++) {
         StrategyFingerprint var17 = (StrategyFingerprint)this.fingerprints.get(var16);
         if (var17.trades >= var5
            && var17.trades <= var6
            && !(var17.profit < var7)
            && !(var17.profit > var9)
            && !(var17.drawdown < var18)
            && !(var17.drawdown > var13)
            && (var19 == -1 || var17.tradesHash == -1 || var19 == var17.tradesHash)
            && var17.fitness < var1.fitness) {
            return var17;
         }
      }

      return null;
   }
}
