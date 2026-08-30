package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.conditions.DismissStruct;
import com.strategyquant.tradinglib.databank.IProgressListener;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PortfolioMasterComputer {
   public static final Logger Log = LoggerFactory.getLogger("PortfolioMasterComputer");
   protected ArrayList<ResultsGroup> originalResults;
   protected ConditionsChecker conditionsChecker = new ConditionsChecker();
   protected PortfolioMasterSettings settings;
   protected ProgressEngine progressEngine;
   protected SQProject project;
   protected int portfolioIndex = 0;

   public abstract void execute() throws Exception;

   private String indexesToString(int[] var1) {
      String var2 = "[";

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2 = var2 + var1[var3] + ",";
      }

      var2 = var2.substring(0, var2.length() - 1);
      return var2 + "]";
   }

   public String getPortfolioIndexes(int[] var1) {
      return "Portfolio " + this.indexesToString(var1);
   }

   public String getPortfolioName(int var1) {
      return "Portfolio " + var1;
   }

   public abstract void computePortfolio(int[] var1) throws Exception;

   public synchronized ArrayList<ResultsGroup> getResults(int[] var1) throws Exception {
      ArrayList var2 = new ArrayList();

      for (int var6 : var1) {
         var2.add(this.originalResults.get(var6));
      }

      return var2;
   }

   public void beforeStart() throws Exception {
      this.limitMaxStrategiesInDatabank();
      boolean var1 = this.settings.limitDateRange;
      if (var1) {
         long var2 = this.settings.dateRangeFrom;
         long var4 = this.settings.dateRangeTo;
         this.limitDataRange(this.originalResults, var2, var4);
      }

      this.setSampleParts(this.originalResults);

      for (int var6 = 0; var6 < this.originalResults.size(); var6++) {
         ResultsGroup var3 = this.originalResults.get(var6);
         var3.specialValues().set("result_index", var6);
      }

      for (ResultsGroup var8 : this.originalResults) {
         String var9 = var8.getNote();
         if (var9 != null) {
            var9 = var9.trim();
            var9 = var9.toLowerCase();
         }

         var8.specialValues().setString(SpecialValues.Sector, var9);
      }
   }

   private void limitMaxStrategiesInDatabank() throws Exception {
      try {
         PMDatabankFilter var1 = new PMDatabankFilter(this.settings.maxStrategiesInDatabank, false, this.settings.fitnessSampleType);
         this.settings.databankTarget.applyFilter(var1, new IProgressListener() {
            public void onProgress(double var1) {
               PortfolioMasterComputer.this.project.getProgress().update("max-strategies-change", var1, null);
            }

            public void onError(double var1, String var3) {
               PortfolioMasterComputer.this.project.getProgress().update("max-strategies-change", var1, var3);
            }

            public void onDone() {
               PortfolioMasterComputer.this.settings.databankTarget.removeFilter();
            }
         });
      } catch (Exception var2) {
         throw new Exception("Error while setting maximum portfolios in databank - " + var2.getMessage(), var2);
      }
   }

   protected synchronized DismissStruct checkCustomConditions(ResultsGroup var1) {
      int var2 = 200000;
      ArrayList var3 = this.settings.conditions;
      if (var3 != null && var3.size() > 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            try {
               Condition var5 = (Condition)var3.get(var4);
               if (var5.isUsed()) {
                  boolean var6 = this.conditionsChecker.check(var1, var5);
                  if (!var6) {
                     return new DismissStruct(var2 + var4, this.conditionsChecker.dismissalMessage);
                  }
               }
            } catch (Exception var7) {
               Log.error("Error while checking condition", var7);
               return new DismissStruct(10003, L.t("Error while evaluating conditions: %s", new Object[]{var7.getMessage()}));
            }
         }
      }

      return null;
   }

   public void limitDataRange(ArrayList<ResultsGroup> var1, long var2, long var4) throws Exception {
      try {
         for (int var6 = 0; var6 < var1.size(); var6++) {
            ResultsGroup var7 = (ResultsGroup)var1.get(var6);
            OrdersList var8 = var7.orders();
            ObjectListIterator var9 = var8.listIterator();

            while (var9.hasNext()) {
               Order var10 = (Order)var9.next();
               if (var10.OpenTime <= var2 || var10.CloseTime >= var4) {
                  var9.remove();
               }
            }

            long var14 = var8.isEmpty() ? 0L : var8.get(0).OpenTime;
            long var11 = var8.isEmpty() ? 0L : var8.get(var8.size() - 1).CloseTime;
            var7.specialValues().set(SpecialValues.HistoryFrom, var14);
            var7.specialValues().set(SpecialValues.HistoryTo, var11);
         }
      } catch (Exception var13) {
         throw new Exception("Error while limiting Data range - " + var13.getMessage(), var13);
      }
   }

   public void setSampleParts(ArrayList<ResultsGroup> var1) throws Exception {
      try {
         long var2 = 0L;
         long var4 = 0L;

         for (int var6 = 0; var6 < var1.size(); var6++) {
            ResultsGroup var7 = (ResultsGroup)var1.get(var6);
            long var8 = var7.specialValues().getLong(SpecialValues.HistoryFrom, Long.MAX_VALUE);
            long var10 = var7.specialValues().getLong(SpecialValues.HistoryTo, Long.MIN_VALUE);
            if (var2 == 0L || var8 < var2) {
               var2 = var8;
            }

            if (var10 > var4) {
               var4 = var10;
            }
         }

         Log.info(String.format("Data range %s - %s", SQTime.toDateString(var2), SQTime.toDateString(var4)));
         double var14 = SQTime.getDaysBetween(var2, var4);
         Double var15 = var14 / 100.0 * this.settings.IsPct;
         long var9 = SQTime.addDays(var2, var15.intValue());

         for (int var11 = 0; var11 < var1.size(); var11++) {
            ResultsGroup var12 = (ResultsGroup)var1.get(var11);
            this.setSampleParts(var12, var9);
         }
      } catch (Exception var13) {
         throw new Exception("Error while setting Sample parts - " + var13.getMessage(), var13);
      }
   }

   private void setSampleParts(ResultsGroup var1, long var2) throws Exception {
      OrdersList var4 = var1.orders();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Order var6 = var4.get(var5);
         if (this.settings.reverseSample) {
            if (var6.OpenTime >= var2) {
               var6.SampleType = 11;
            } else {
               var6.SampleType = 21;
            }
         } else if (var6.OpenTime >= var2) {
            var6.SampleType = 21;
         } else {
            var6.SampleType = 11;
         }
      }
   }

   protected abstract boolean isStopped();
}
