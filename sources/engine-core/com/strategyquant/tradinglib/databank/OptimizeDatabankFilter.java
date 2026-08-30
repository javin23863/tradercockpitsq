package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeDatabankFilter extends AbstractDatabankFilter {
   public static final Logger Log = LoggerFactory.getLogger(OptimizeDatabankFilter.class);
   private static final String LOCK_OPTIMDATABANKFILTER = "OptimizeDatabankFilter";
   private IntOpenHashSet storedStrategies = new IntOpenHashSet();

   public OptimizeDatabankFilter(int var1) {
      super(var1);
   }

   @Override
   public void init(Databank var1, IProgressListener var2) throws Exception {
      this.databank = var1;
      this.calculateRankValues();
   }

   @Override
   public boolean shouldAddStrategy(ResultsGroup var1, double var2) {
      String var4 = this.getBaseName(var1.getName());
      int var5 = this.computeStrategyParamsHash(var4, var1);
      if (this.storedStrategies.contains(var5)) {
         return false;
      }

      ArrayList var6 = this.getStrategies(var4);
      return var6.size() < this.maxRecords ? true : this.hasWorseRankValue(var6, var2);
   }

   @Override
   public boolean addStrategy(ResultsGroup var1, double var2) throws Exception {
      String var4 = this.getBaseName(var1.getName());
      int var5 = this.computeStrategyParamsHash(var4, var1);
      if (this.storedStrategies.contains(var5)) {
         return false;
      } else {
         ArrayList var6 = this.getStrategiesNoLock(var4);
         if (var6.size() < this.maxRecords) {
            this.databank.refreshGrid();
            return true;
         } else {
            int var7 = var6.size() - this.maxRecords + 1;
            if (var7 > 1) {
               boolean var10 = true;
               throw new Exception(L.t("This shouldn't happen! Count: %d", new Object[]{var7}));
            } else {
               String var8 = this.getStrategyWithWorseRank(var6, var2);
               if (var8 != null) {
                  this.databank.removeNoLock(var8, true, false, true, "OptimizeDatabankFilter");
                  this.removeWorseStrategy(var8);
                  this.databank.refreshGrid();
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   @Override
   protected void actionOnInsert(ResultsGroup var1) {
      if (var1 != null) {
         String var2 = this.getBaseName(var1.getName());
         int var3 = this.computeStrategyParamsHash(var2, var1);
         this.storedStrategies.add(var3);
      }
   }

   private String getBaseName(String var1) {
      return var1.split("- Optimiz")[0];
   }

   protected ArrayList<String> getStrategiesNoLock(String var1) {
      return this.databank.getRecordKeysNoLock(var1);
   }

   protected ArrayList<String> getStrategies(String var1) {
      return this.databank.getRecordKeys(var1);
   }

   private int computeStrategyParamsHash(String var1, ResultsGroup var2) {
      StringBuffer var3 = new StringBuffer(var1);
      var3.append(var2.getParams());
      return var3.toString().hashCode();
   }
}
