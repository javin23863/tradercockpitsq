package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.DismissStruct;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioChecker {
   public static final Logger Log = LoggerFactory.getLogger("PortfolioChecker");
   public static final String RESULT_OK = "OK";
   public static final String RESULT_INDEX = "result_index";
   private int correlationPeriod;
   private CorrelationType correlationType;
   private double maxCorrelation;
   private boolean allowNegativeCorrelation;
   private boolean addEmptyPeriods;
   private int maxFromSector;
   private PortfolioMasterSettings settings;
   private CorrelationComputer correlationComputer = new CorrelationComputer();
   private HashMap<String, Double> correlationsMap = new HashMap<>();
   private List<ResultsGroup> results = null;

   public PortfolioChecker(int var1, CorrelationType var2, double var3, boolean var5, boolean var6, int var7, PortfolioMasterSettings var8) {
      this.correlationPeriod = var1;
      this.correlationType = var2;
      this.maxCorrelation = var3;
      this.allowNegativeCorrelation = var5;
      this.addEmptyPeriods = var6;
      this.maxFromSector = var7;
      this.settings = var8;
   }

   public synchronized void setResults(List<ResultsGroup> var1) {
      this.results = var1;
   }

   public synchronized DismissStruct checkCorrectness(int[] var1, String var2) {
      ArrayList var3 = new ArrayList();

      for (int var7 : var1) {
         var3.add(this.results.get(var7));
      }

      if (!this.checkMaxFromSector(var3)) {
         return new DismissStruct(
            10016, L.t("%s - dismissed, reason: %s", new Object[]{var2, L.t("More strategies from the same sector than allowed.", new Object[0])})
         );
      }

      String var8 = this.checkCorrelation(var3);
      return var8 != null ? new DismissStruct(10015, L.t("%s - dismissed, reason: %s", new Object[]{var2, var8})) : null;
   }

   private synchronized boolean checkMaxFromSector(List<ResultsGroup> var1) {
      if (this.maxFromSector == 0) {
         return true;
      }

      HashMap var2 = new HashMap();

      for (ResultsGroup var4 : var1) {
         String var5 = var4.specialValues().getString(SpecialValues.Sector);
         if (var5 != null) {
            if (var2.containsKey(var5)) {
               int var6 = (Integer)var2.get(var5) + 1;
               if (var6 > this.maxFromSector) {
                  return false;
               }

               var2.put(var5, var6);
            } else {
               var2.put(var5, 1);
            }
         }
      }

      return true;
   }

   private synchronized String checkCorrelation(List<ResultsGroup> var1) {
      if (this.maxCorrelation <= 0.0) {
         return null;
      }

      for (ResultsGroup var6 : var1) {
         for (ResultsGroup var8 : var1) {
            if (!var6.equals(var8)) {
               double var2 = this.computeCorrelation(var6, var8);
               if (var2 != -99.0) {
                  String var4 = this.printCombination(var6, var8);
                  if (var2 > this.maxCorrelation || !this.allowNegativeCorrelation && var2 < 0.0) {
                     return var2 < 0.0
                        ? L.t("%s correlation %.2f < 0, negative correlation not allowed", new Object[]{var4, var2})
                        : L.t("%s correlation %.2f > %.2f", new Object[]{var4, var2, this.maxCorrelation});
                  }
               }
            }
         }
      }

      return null;
   }

   private synchronized double computeCorrelation(ResultsGroup var1, ResultsGroup var2) {
      try {
         String var5 = this.key(var1, var2);
         double var3;
         if (this.correlationsMap.containsKey(var5)) {
            var3 = this.correlationsMap.get(var5);
         } else {
            var3 = this.correlationComputer
               .computeCorrelation(var1, var2, this.settings.correlationSampleType, this.correlationPeriod, this.correlationType, this.addEmptyPeriods);
            this.correlationsMap.put(var5, var3);
         }

         return var3;
      } catch (Exception var6) {
         Log.error("Failed to calculate correlation - " + var6.getMessage(), var6);
         return -99.0;
      }
   }

   private synchronized String key(ResultsGroup var1, ResultsGroup var2) {
      int var3 = var1.specialValues().getInt("result_index");
      int var4 = var2.specialValues().getInt("result_index");
      return var3 > var4 ? var3 + "," + var4 : var4 + "," + var3;
   }

   private synchronized String printCombination(ResultsGroup var1, ResultsGroup var2) {
      return var1.getName() + " - " + var2.getName();
   }
}
