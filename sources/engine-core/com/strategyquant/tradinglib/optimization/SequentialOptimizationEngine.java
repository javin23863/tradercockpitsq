package com.strategyquant.tradinglib.optimization;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.robustnesstests.FitnessData;
import com.strategyquant.tradinglib.robustnesstests.SequentialOptimizationResults;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequentialOptimizationEngine {
   private static final Logger Log = LoggerFactory.getLogger("SequentialOptimizationEngine");
   private final String lastSettingsXml;
   private SequentialOptimizationSettings settings;
   private StopPauseEngine stopPauseEngine;
   private String dismissalMessage = null;
   private ILastEventListener lastEventListener;
   private String jobGroupID;
   private INewResultListener newResultListener = null;
   private IEngineLogListener engineLogListener = null;
   private IStepsListener stepsListener = null;
   private StrategyBase strategy = null;
   private String strategyName;
   private SettingsMap backtestSettings;
   private String lastSettingsXML;
   private ArrayList<Variable> variablesToOptimize;
   private ArrayList<Parameter> optimizationParams;
   private HashMap<Double, ResultsGroup> lastResults;
   private ArrayList<String> parameterNames;
   private ArrayList<Double> parameterValues;
   private int totalCount = 0;
   private int step = 0;

   public SequentialOptimizationEngine(SequentialOptimizationSettings var1, StopPauseEngine var2, ILastEventListener var3, String var4) {
      this.settings = var1;
      this.stopPauseEngine = var2;
      this.lastEventListener = var3;
      this.lastResults = new HashMap<>();
      this.parameterNames = new ArrayList<>();
      this.parameterValues = new ArrayList<>();
      this.variablesToOptimize = new ArrayList<>();
      this.optimizationParams = new ArrayList<>();
      this.lastSettingsXml = var4;
   }

   public void initialize(String var1, Element var2, SettingsMap var3) throws Exception {
      this.strategyName = var1;
      this.backtestSettings = var3;
      this.strategy = StrategyBase.createXmlStrategy(var2);
      this.strategy.transformToVariables(this.settings.symmetry, this.settings.paramTypes);
      Variables var4 = this.strategy.variables();
      var4.sortByName();
      this.variablesToOptimize.clear();
      this.optimizationParams.clear();
      if (this.settings.manualParams != null) {
         for (int var5 = 0; var5 < this.settings.manualParams.size(); var5++) {
            Parameter var6 = this.settings.manualParams.get(var5);
            if (var6.use && this.shouldOptimizeParameter(var6.getType(), var6.getName())) {
               Variable var7 = var4.get(var6.getName());
               if (var7 == null) {
                  Log.error("Variable with name " + var6.getName() + " not found. This should not happen!");
               } else {
                  this.variablesToOptimize.add(var7);
                  this.optimizationParams.add(var6);
                  this.parameterNames.add(var7.getName());
                  this.parameterValues.add(var6.original);
               }
            }
         }
      } else {
         for (int var9 = 0; var9 < var4.size(); var9++) {
            Variable var11 = var4.get(var9);
            if (this.shouldOptimizeParameter(var11.getInternalType(), var11.getName())) {
               this.optimizationParams.add(null);
               this.variablesToOptimize.add(var11);
               this.parameterNames.add(var11.getName());
               this.parameterValues.add(var11.getValueAsDouble());
            }
         }
      }

      try {
         Element var10 = (Element)var3.get("StrategyLastSettings");
         this.lastSettingsXML = var10 != null ? XMLUtil.elementToString(var10) : null;
      } catch (Exception var8) {
         Log.error("Cannot read last settings", var8);
      }
   }

   public boolean optimize(boolean var1, boolean var2, GridJob var3, ISequentialOptimizationCompleteListener var4) throws Exception {
      if (this.variablesToOptimize.size() == 0) {
         this.dismissalMessage = L.t("No parameters to optimize", new Object[0]);
         var4.onFinish(false, this.strategy, null, null);
         return false;
      }

      this.totalCount = 0;
      int var5 = 0;
      this.step = 0;
      SequentialOptimizationResults var6 = new SequentialOptimizationResults(this.settings.paramTypes, this.settings.symmetry, false);
      if (var1) {
         String var7 = null;

         try {
            var7 = (String)this.backtestSettings.get("StrategyNote");
         } catch (Exception var15) {
         }

         BacktestRunner var8 = new BacktestRunner(
            new BacktestSettings(this.backtestSettings), this.stopPauseEngine, null, false, this.lastEventListener, this.lastSettingsXml
         );
         BacktestResult var9 = var8.execute();
         if (var9 != null) {
            ResultsGroup var10 = var9.getResult();
            if (var10 != null) {
               var10.specialValues().set(SpecialValues.Note, var7);
               var10.specialValues().setString("OptimizationParameters", this.getOptimizationParameters(-1, 0.0));
               var10.setLastSettings(this.lastSettingsXML);
               if (this.newResultListener != null) {
                  var10.setName(this.strategyName);
                  this.newResultListener.newResult(var10);
               }
            }
         }
      }

      ResultsGroup var16 = null;
      StrategyBase var18 = this.strategy.clone();

      for (int var19 = 0; var19 < this.variablesToOptimize.size(); var19++) {
         Variable var20 = this.variablesToOptimize.get(var19);
         FitnessData var11 = this.optimizeParameter(this.strategyName, var18, var20, this.optimizationParams.get(var19), this.backtestSettings, var3, var2);
         double var12 = var11.analyze(this.settings.stableAreaCount, this.settings.stabilityRangePct);
         var6.add(var11);
         if (var12 != -Double.MAX_VALUE) {
            var20.setValue(var12);
            this.parameterValues.set(this.totalCount, var12);
            var16 = this.lastResults.get(var12);
            Variables var14 = var18.variables();
            var18.variables().setValue(var20.getName(), var12);
         } else {
            var5++;
         }

         this.totalCount++;
         this.lastResults.clear();
      }

      if (var5 <= (int)(0.01 * this.totalCount * (100 - this.settings.pctToPass))) {
         if (this.settings.applyToStrategy) {
            this.strategy.transformToNumbers();
            var6.setApplyToStrategy(true);
         }

         var6.setPassed(true);
         if (var16 != null) {
            var16 = var16.clone();
            var16.specialValues().setString(SpecialValues.FiltersResultFailedReason, SpecialValues.FiltersResultPassed);
            var4.onFinish(true, this.strategy, var6, var16);
         }

         return true;
      } else {
         var6.setPassed(false);
         this.dismissalMessage = L.t("Too many unstable parameters - %d of %d (treshold %d%%)", new Object[]{var5, this.totalCount, this.settings.pctToPass});
         var4.onFinish(false, this.strategy, var6, null);
         return false;
      }
   }

   private boolean shouldOptimizeParameter(byte var1, String var2) {
      return (var1 == 1 || var1 == 4) && !var2.startsWith("Magic");
   }

   private FitnessData optimizeParameter(String var1, StrategyBase var2, Variable var3, Parameter var4, SettingsMap var5, GridJob var6, boolean var7) throws Exception {
      this.stopPauseEngine.checkPaused();
      this.stopPauseEngine.printToLog(L.t("Sequential optimization: %s - Optimizing parameter %s...", new Object[]{var1, var3.getName()}));
      ArrayList var8 = this.createJobs(var1, var2, var3, var4, var5, var6);
      final FitnessData var9 = new FitnessData(var3, var8.size());
      if (var7) {
         for (int var10 = 0; var10 < var8.size(); var10++) {
            SequentialOptimizationJob var11 = (SequentialOptimizationJob)var8.get(var10);
            this.stopPauseEngine.checkPaused();
            if (this.stopPauseEngine.isStopped()) {
               return var9;
            }

            JobDetails var12 = new JobDetails(var11.getJobId());
            BacktestResult var13 = null;

            try {
               long var14 = System.currentTimeMillis();
               var13 = var11.call();
               long var16 = System.currentTimeMillis() - var14;
               var12.setDuration(var16);
               this.processResult(var13, var11.getJobId(), var9);
            } catch (TradingException var22) {
               Log.error("Trading exception during optimization", var22);
            } catch (Throwable var23) {
               var12.setException(SQUtils.getStackTrace(var23));
               Log.error("Error while running sequential optimization job #{}", var10, var23);
            } finally {
               if (var11 != null) {
                  var11.destroy();
               }
            }
         }
      } else {
         this.jobGroupID = UUID.randomUUID().toString();
         final GridClient var25 = SQGrid.getGridClient();
         var25.executeOnGrid(this.jobGroupID, var8);
         var25.registerMessageListener(this.jobGroupID, new IGridMessageListener() {
            public void messageReceived(GridMessage var1) {
               try {
                  if (SQProject.isMemoryProtectionUsed()) {
                     MemoryUsageChecker.checkAvailableMemory();
                  }

                  if (var1.getMessageID() == 1) {
                     BacktestResult var2x = (BacktestResult)var1.getData();
                     SequentialOptimizationEngine.this.processResult(var2x, var1.getJobDetails().getJobID(), var9);
                     String var3x = var1.getJobDetails().getException();
                     if (var3x != null && !var3x.isEmpty()) {
                        SequentialOptimizationEngine.Log.error("Job exception: " + var3x);
                     }
                  }
               } catch (OutOfMemoryError var4x) {
                  var25.stop(SequentialOptimizationEngine.this.jobGroupID);
               }
            }
         });
         var25.waitForFinish(this, null, null, this.jobGroupID);
      }

      return var9;
   }

   private void processResult(BacktestResult var1, String var2, FitnessData var3) {
      ResultsGroup var4 = var1.getResult();

      try {
         String[] var5 = this.getJobIDData(var2);
         int var6 = Integer.parseInt(var5[0]);
         double var7 = Double.parseDouble(var5[1]);
         if (var4 != null) {
            try {
               var3.set(var6, var7, var4.getFitness());
               var4.setLastSettings(this.lastSettingsXML);
               if (this.newResultListener != null) {
                  var4.setName(this.strategyName + " - Opt. " + this.totalCount + "." + var6);
                  var4.specialValues().setString("OptimizationParameters", this.getOptimizationParameters(this.totalCount, var7));
                  this.newResultListener.newResult(var4);
               }

               if (this.stepsListener != null) {
                  this.stepsListener.step(++this.step);
               }

               this.lastResults.put(var7, var4);
            } catch (Exception var10) {
               Log.error("Sequential Optimization error", var10);
            }
         } else {
            Log.debug("Sequential Optimization backtest result is null. DismissalMessage: " + var1.getDismissalMessage());
            var3.set(var6, var7, 0.0);
         }
      } catch (Exception var11) {
         Log.error("Unable to get job details from job id", var11);
      }
   }

   private ArrayList<SequentialOptimizationJob> createJobs(String var1, StrategyBase var2, Variable var3, Parameter var4, SettingsMap var5, GridJob var6) throws Exception {
      ArrayList var7 = new ArrayList();

      try {
         double[] var8 = this.getCombinations(var3, var4, var3.getInternalType() == 1);

         for (int var9 = 0; var9 < var8.length; var9++) {
            double var10 = var8[var9];
            Map var12 = this.updateBacktestSettings(var5, var1, var2, var3, var10);
            String var13 = this.getJobID(var1, var3.getName(), var9, var10);
            SequentialOptimizationJob var14 = new SequentialOptimizationJob(
               var13, var12, this.stopPauseEngine, this.lastEventListener, var1, var3.getName(), var10, this.lastSettingsXml
            );
            if (var6 != null) {
               var14.setPriority(var6.getPriority() + 1);
            }

            var7.add(var14);
         }

         return var7;
      } catch (Exception var15) {
         this.stopPauseEngine.printToLog(var1 + ": " + L.t("Sequential Optimization job preparation error", new Object[0]) + " - " + var15.getMessage());
         Log.error("Error while preparing Sequential Optimization jobs for strategy" + var1, var15);
         throw var15;
      }
   }

   private String getJobID(String var1, String var2, int var3, double var4) {
      return var1 + "_SequentialOpt." + var2 + "_" + var3 + "/" + var4;
   }

   private String[] getJobIDData(String var1) throws Exception {
      try {
         int var2 = var1.lastIndexOf("_");
         String var3 = var1.substring(var2 + 1);
         return var3.split("/");
      } catch (Exception var4) {
         throw new Exception("Unable to parse data from jobID", var4);
      }
   }

   private String getOptimizationParameters(int var1, double var2) {
      String var4 = "";

      for (int var5 = 0; var5 < this.parameterNames.size(); var5++) {
         var4 = var4 + "," + this.parameterNames.get(var5) + "=" + (var5 == var1 ? var2 : this.parameterValues.get(var5));
      }

      return var4.substring(1);
   }

   private double[] getCombinations(Variable var1, Parameter var2, boolean var3) {
      double var4 = var1.getValueAsDouble();
      double var6 = var1.getParamType() != null && "ParamTypePeriod".equals(var1.getParamType()) ? 2.0 : 0.0;
      boolean var8 = var2 != null;
      double var9 = var8 ? var2.start : (1.0 - 0.01 * this.settings.distributionDown) * var4;
      var9 = var3 ? SQUtils.round(var9).intValue() : var9;
      double var11 = var8 ? var2.stop : (1.0 + 0.01 * this.settings.distributionUp) * var4;
      var11 = var3 ? SQUtils.round(var11).intValue() : var11;
      double var13 = var8 ? var2.step : (1.0 * var11 - var9) / (this.settings.steps - 1);
      var13 = var3 ? Math.max(1.0, var13) : var13;
      int var15 = this.getStepDecimals(var13);
      if (var13 == 0.0) {
         var13 = 1.0;
      }

      ArrayList var16 = new ArrayList();
      if (var8) {
         this.settings.steps = 1000000;
      }

      for (int var17 = 0; var17 < this.settings.steps; var17++) {
         double var18 = var9 + var13 * var17;
         var18 = Math.max(var3 ? (int)var18 : SQUtils.round(var18, var15), var6);
         if (!var16.contains(var18)) {
            var16.add(var18);
         }

         if (var18 >= var11) {
            break;
         }
      }

      if (!var8 && var16.size() < this.settings.steps) {
         boolean var23 = var9 <= var6;

         while (var16.size() < this.settings.steps) {
            if (!var23) {
               double var26 = Math.max(var9 - var13, var6);
               var26 = var3 ? (int)var26 : SQUtils.round(var26, var15);
               if (!var16.contains(var26)) {
                  var16.add(0, var26);
               }

               var9 = var26;
               var23 = var26 <= var6;
            }

            double var28 = var11 + var13;
            var28 = var3 ? (int)var28 : SQUtils.round(var28, var15);
            var11 = var28;
            if (!var16.contains(var28)) {
               var16.add(var28);
            }
         }
      }

      double[] var24 = new double[var16.size()];

      for (int var30 = 0; var30 < var16.size(); var30++) {
         var24[var30] = (Double)var16.get(var30);
      }

      return var24;
   }

   protected Map<String, Serializable> updateBacktestSettings(SettingsMap var1, String var2, StrategyBase var3, Variable var4, double var5) throws Exception {
      StrategyBase var7 = var3.clone();
      this.copyModifiedVariables(var3, var7);
      Variables var8 = var7.variables();
      var8.get(var4.getName()).setValue(var4.getInternalType() == 1 ? (int)var5 : var5);
      var7.transformToNumbers();
      var1.set("StrategyObject", var7);
      var1.set("StrategyXml", var7.getStrategyXml());
      HashMap var9 = new HashMap();
      String[] var10 = var1.getAllKeys();

      for (int var11 = 0; var11 < var10.length; var11++) {
         if (var10[var11].equals("TradingOptions")) {
            TradingOptions var12 = new TradingOptions();
            var12.addAll(0, (ArrayList)var1.get(var10[var11]));
            var9.put("TradingOptions", var12.getClone());
         } else {
            var9.put(var10[var11], (Serializable)var1.get(var10[var11]));
         }
      }

      return var9;
   }

   private void copyModifiedVariables(StrategyBase var1, StrategyBase var2) {
      Variables var3 = var1.variables();
      Variables var4 = var2.variables();

      for (Variable var6 : var3) {
         Variable var7 = var4.get(var6.getName());
         if (var7 != null && (var6.getInternalType() == 1 || var6.getInternalType() == 4)) {
            double var8 = var6.getValueAsDouble();
            var7.setValue(var6.getInternalType() == 1 ? (int)var8 : var8);
         }
      }
   }

   public String getDismissalMessage() {
      return this.dismissalMessage;
   }

   public void setNewResultListener(INewResultListener var1) {
      this.newResultListener = var1;
   }

   public void setLogListener(IEngineLogListener var1) {
      this.engineLogListener = var1;
   }

   public void setStepsListener(IStepsListener var1) {
      this.stepsListener = var1;
   }

   public int getTotalStepsCount() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.variablesToOptimize.size(); var2++) {
         Variable var3 = this.variablesToOptimize.get(var2);
         Parameter var4 = this.optimizationParams.get(var2);
         double[] var5 = this.getCombinations(var3, var4, var3.getInternalType() == 1);
         var1 += var5.length;
      }

      return var1;
   }

   public int getStepDecimals(double var1) {
      if (var1 == 0.0) {
         return 0;
      }

      var1 = Math.abs(var1);

      int var3;
      for (var3 = 0; var1 < 1.0; var3++) {
         var1 *= 10.0;
      }

      return var3;
   }
}
