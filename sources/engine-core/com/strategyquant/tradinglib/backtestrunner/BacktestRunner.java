package com.strategyquant.tradinglib.backtestrunner;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.correlation.FitPortfolio;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.exception.StrategyStoppedException;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestRunner {
   public static final Logger Log = LoggerFactory.getLogger("BacktestRunner");
   private BacktestSettings backtestSettings;
   private StopPauseEngine stopPauseEngine;
   private GridJob parentGridJob = null;
   private String strategyName = "UnknownStrategy";
   private double totalProgressSteps;
   private double currentProgressStep = 0.0;
   private double progressStepWeight = 0.0;
   private int dismissalReason;
   private String dismissalMessage;
   private int dismissBadStrategies;
   private boolean fitnessComputed = false;
   private double globalATR;
   private DurationStats durationStats;
   private boolean printPartialProgress = false;
   private ILastEventListener lastEventListener;
   private boolean isEvolution = false;
   private ConditionsChecker conditionsChecker = new ConditionsChecker();
   private CorrelationComputer correlationComputer = new CorrelationComputer();
   private final String lastSettingsXml;

   public BacktestRunner(BacktestSettings var1, StopPauseEngine var2, GridJob var3, boolean var4, ILastEventListener var5, String var6) throws Exception {
      this.backtestSettings = var1;
      if (this.backtestSettings.isRetester) {
         this.dismissBadStrategies = this.backtestSettings.dismissBadStrategies;
         this.backtestSettings.dismissBadStrategies = 0;
      }

      this.stopPauseEngine = var2;
      this.parentGridJob = var3;
      this.strategyName = var1.strategyName;
      this.printPartialProgress = var4;
      this.lastEventListener = var5;
      this.lastSettingsXml = var6;
      this.durationStats = new DurationStats();
      this.computeTotalProgressSteps();
   }

   public BacktestResult execute() throws Exception {
      return this.execute(3);
   }

   public BacktestResult execute(int var1) throws Exception {
      long var2 = System.currentTimeMillis();
      if (Log.isDebugEnabled()) {
         Log.debug(String.format("Backtest runner %s started", this.strategyName));
      }

      BacktestResult var4 = this.runBacktestOnMainSetup(var1);
      ResultsGroup var5 = var4.getResult();
      if (var5 != null) {
         var5.specialValues().set(SpecialValues.DateGenerated, this.backtestSettings.dateGenerated);
         var5.specialValues().set(SpecialValues.DateLastModified, SQTime.getLocalCurrentTimeInMs());
      }

      if (var5 != null && BadStrategyException.getReasonAsString(10017) == var5.specialValues().getString(SpecialValues.FiltersResultFailedReason)) {
         return var4.dismiss("dismissed, strategy indicators require X reserved bars", 10017);
      }

      if (!this.backtestSettings.isRetester && var4.isDismissed() && this.fitnessComputed) {
         return var4;
      }

      if (!this.backtestSettings.forceRunCrossChecks && this.backtestSettings.isRetester && var4.isDismissed()) {
         return var4;
      }

      if (var1 == 3 && var4.getResult() != null) {
         var4 = this.runAndSaveCrossChecks(var4, var1);
         if (!this.fitnessComputed) {
            var5 = var4.getResult();
            if (var5 != null) {
               this.computeFitnessOnMainData(var5, var1, true);
            }
         }

         if (var4.isDismissed()) {
            return var4;
         }
      }

      if (this.stopPauseEngine.isStopped()) {
         if (Log.isDebugEnabled()) {
            Log.debug("Clearing results for {}", this.strategyName);
         }

         var5 = var4.getResult();
         if (var5 != null) {
            var5.clear();
         }

         return var4.dismiss("Task stopped - Strategy test not finished", 10002);
      } else {
         double var6 = (System.currentTimeMillis() - var2) / 1000.0;
         if (Log.isDebugEnabled()) {
            Log.debug(String.format("Backtest runner %s finished in %.4f s.", this.strategyName, var6));
         }

         return var4;
      }
   }

   private BacktestResult runBacktestOnMainSetup(int var1) throws Exception {
      ResultsGroup var2 = null;
      long var3 = System.currentTimeMillis();
      if (this.strategyName != null) {
         this.printEvent(L.t("Running backtest for %s...", new Object[]{this.strategyName}));
      }

      ChartSetup var5 = this.backtestSettings.chartSetups.get(0);

      try {
         var2 = this.backtestStrategy(var5);
      } catch (BadStrategyException var18) {
         BacktestResult var7 = new BacktestResult(null, this.durationStats);
         var7.dismiss(
            String.format(DurationStats.MainTest + " %s, - " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, var18.getReasonAsString()),
            var18.getReason()
         );
         return var7;
      }

      this.durationStats.addDuration(DurationStats.MainTest, System.currentTimeMillis() - var3);
      this.currentProgressStep++;
      if (this.stopPauseEngine.isStopped()) {
         var2.clear();
         return new BacktestResult(L.t("Task stopped - Strategy test not finished", new Object[0]), 10002, this.durationStats);
      }

      this.computeFitnessOnMainData(var2, var1, false);
      BacktestResult var6 = new BacktestResult(var2, this.durationStats);
      if (this.backtestSettings.isRetester) {
         var2.specialValues().set(SpecialValues.FiltersResultFailedReason, SpecialValues.FiltersResultPassed);
      }

      var2.setDatabank(null);
      var2.setName(this.strategyName);
      if (this.backtestSettings.mergedStrategies != null) {
         var2.setMergedResults(this.backtestSettings.mergedStrategies);
      }

      if (!this.globalConditionsPassed(var2, var1)) {
         if (this.backtestSettings.isRetester) {
            var2.specialValues().set(SpecialValues.FiltersResultFailedReason, this.dismissalMessage);
         }

         var6.dismiss(
            String.format(DurationStats.MainTest + " %s, - " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, this.dismissalMessage),
            this.dismissalReason
         );
      }

      if (this.backtestSettings.isRetester) {
         this.checkAutomaticDismissalRules(var6);
      }

      if (var1 == 3 && !this.fitToExistingPortfolio(var2)) {
         var6.dismiss(L.t("Fit to existing portfolio - dismissed: %s, %s", new Object[]{this.strategyName, this.dismissalMessage}), this.dismissalReason);
      }

      int var20 = this.backtestSettings.chartSetups.getMainSetup().getBacktestEngine();
      if (this.lastSettingsXml != null && (var20 == 56756755 || var20 == 938213070)) {
         Element var8 = XMLUtil.stringToElement(this.lastSettingsXml);
         Element var9 = var8.getChild("Options").getChild("BuildTradingOptions").getChild("Params");
         List var10 = var9.getChildren();
         Optional var11 = var10.stream()
            .filter(
               var0 -> "Param".equals(var0.getName())
                  && "ReservedBars".equals(var0.getAttributeValue("key"))
                  && "ReservedBars".equals(var0.getAttributeValue("className"))
            )
            .findFirst();
         if (var11.isPresent()) {
            Element var12 = (Element)var11.get();
            int var13 = Integer.valueOf(var12.getText());
            Integer var14 = 0;
            if (var2 != null) {
               String var15 = var2.getName();
               Element var16 = var2.getStrategyXml();
               var14 = this.findParamElementsWithPeriod(var16, 0, 0, var15);
               if (var14 >= var13) {
                  String var17 = BadStrategyException.getReasonAsString(10017);
                  var2.specialValues().set(SpecialValues.FiltersResultFailedReason, var17);
               }
            }
         }
      }

      return var6;
   }

   private Integer findParamElementsWithPeriod(Element var1, int var2, int var3, String var4) {
      Integer var5 = 0;
      List var6 = var1.getChildren();

      try {
         if (var6 != null) {
            int var7 = -1;
            boolean var8 = false;

            for (int var9 = 0; var9 < var6.size(); var9++) {
               Element var10 = (Element)var6.get(var9);
               if (!var10.getName().equals("CustomIndicators") && var10.getName().equals("Param")) {
                  String var11 = var10.getAttributeValue("key");
                  if (var11.equals("#Shift#")) {
                     var2 += Integer.parseInt(var10.getText().trim());
                     break;
                  }
               }
            }

            for (int var17 = 0; var17 < var6.size(); var17++) {
               Element var18 = (Element)var6.get(var17);
               if (!var18.getName().equals("CustomIndicators")) {
                  if (var18.getName().equals("Param")) {
                     var8 = false;
                     String var19 = var18.getAttributeValue("key");
                     String var12 = "";
                     if (var19.equals("#Method#")) {
                        var12 = var18.getAttributeValue("values");
                        if (var12 == null) {
                           var12 = "";
                        }
                     }

                     if (var19.equals("#Chart#")) {
                        var7 = Integer.parseInt(var18.getText().trim());
                     } else if (var19.equals("#Bars#")) {
                        var3 += Integer.parseInt(var18.getText().trim());
                     } else if (var19.toLowerCase().contains("period")) {
                        var8 = true;
                     } else if (var19.equals("#SlowEMA#")) {
                        var8 = true;
                     } else if (var19.equals("#FastEMA#")) {
                        var8 = true;
                     } else if (var19.equals("#SignalPeriod#")) {
                        var8 = true;
                     } else if (var19.equals("#Fast#")) {
                        var8 = true;
                     } else if (var19.equals("#Slow#")) {
                        var8 = true;
                     } else if (var19.equals("#Smooth#")) {
                        var8 = true;
                     } else if (var19.equals("#Length#")) {
                        var8 = true;
                     } else if (var19.equals("#Slowing#")) {
                        var8 = true;
                     } else if (var19.equals("#Gamma#")) {
                        var8 = true;
                     } else if (var19.equals("#Method#") && (var12.equals("Price below SMA200=0") || var12.equals("Price above SMA200=0"))) {
                        var8 = true;
                     }

                     if (var5 == null || var2 + var3 > var5) {
                        var5 = var2 + var3;
                     }

                     if (var8 && var7 == 0) {
                        try {
                           if (var19.equals("#Gamma#")) {
                              if (var5 == null || 20 + var2 + var3 > var5) {
                                 var5 = 20 + var2 + var3;
                                 Log.debug(String.format("Rg Name  %s  Key value: %s", var4, var5.toString()));
                              }
                           } else if (var19.equals("#Method#") && (var12.equals("Price below SMA200=0") || var12.equals("Price above SMA200=0"))) {
                              if (var5 == null || 200 + var2 + var3 > var5) {
                                 var5 = 200 + var2 + var3;
                                 Log.debug(String.format("Rg Name  %s  Key value: %s", var4, var5.toString()));
                              }
                           } else if (!var18.getText().isBlank() && !var18.getText().isEmpty()) {
                              int var13 = Integer.parseInt(var18.getText().trim());
                              if (var5 == null || var13 + var2 + var3 > var5) {
                                 var5 = var13 + var2 + var3;
                                 Log.debug(String.format("Rg Name  %s  Key value: %s", var4, var5.toString()));
                              }
                           }
                        } catch (NumberFormatException var14) {
                           Log.debug("Invalid period value: " + var18.getText());
                        }
                     }
                  } else if (var18.getName().equals("Item")) {
                     String var20 = var18.getAttributeValue("key");
                     if (var20.toLowerCase().contains("awo") && (var5 == null || 34 + var2 + var3 > var5)) {
                        var5 = 36 + var2 + var3;
                        Log.debug(String.format("Rg Name  %s  Key value: %s", var4, var5.toString()));
                     }
                  } else {
                     var7 = -1;
                  }

                  List var21 = var18.getChildren();
                  if (var21 != null && var21.size() >= 1) {
                     Integer var22 = this.findParamElementsWithPeriod(var18, var2, var3, var4);
                     if (var22 != null && (var5 == null || var22 > var5)) {
                        var5 = var22;
                     }
                  }
               }
            }
         }
      } catch (NumberFormatException var15) {
         Log.debug("Error findParamElementsWithPeriod : " + var15);
      }

      return var5;
   }

   private boolean fitToExistingPortfolio(ResultsGroup var1) {
      try {
         FitPortfolio var2 = this.backtestSettings.fitPortfolio;
         if (var2 != null && var2.active) {
            Databank var3 = var2.databank;

            for (int var4 = 0; var4 < var3.getRecords().size(); var4++) {
               ResultsGroup var5 = var3.getRecords().get(var4);
               double var6 = SQUtils.round2(
                  this.correlationComputer.computeCorrelation(var5, var1, (byte)127, var2.corrPeriod, var2.corrType, var2.corrAddEmptyPeriods)
               );
               if (var6 != -99.0 && (var6 > var2.corrMax || !var2.corrAllowNegative && var6 < 0.0)) {
                  this.dismissalReason = 10014;
                  if (var6 < 0.0) {
                     this.dismissalMessage = L.t("%s correlation %.2f < 0, negative correlation not allowed", new Object[]{var5.getName(), var6});
                  } else {
                     this.dismissalMessage = L.t("%s correlation %.2f > %.2f", new Object[]{var5.getName(), var6, var2.corrMax});
                  }

                  return false;
               }
            }
         }
      } catch (Exception var8) {
         Log.error("Error", var8);
      }

      return true;
   }

   private void checkAutomaticDismissalRules(BacktestResult var1) {
      try {
         ResultsGroup var2 = var1.getResult();
         int var3 = var2.mainResult().getStrategyProblems();
         int var4 = BadStrategyException.getFirstReason(var3, this.dismissBadStrategies);
         if (var4 != 0) {
            this.dismissalReason = var4;
            this.dismissalMessage = BadStrategyException.getReasonAsString(this.dismissalReason);
            var2.specialValues().set(SpecialValues.FiltersResultFailedReason, this.dismissalMessage);
            var1.dismiss(
               String.format(DurationStats.MainTest + " %s, - " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, this.dismissalMessage),
               this.dismissalReason
            );
         }
      } catch (Exception var5) {
         Log.error("Error", var5);
      }
   }

   private void printEvent(String var1) {
      if (this.stopPauseEngine != null) {
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(var1);
         }

         if (this.printPartialProgress) {
            this.stopPauseEngine.printToLogDebug(var1);
         }
      }
   }

   private void computeTotalProgressSteps() throws Exception {
      this.currentProgressStep = 0.0;
      int var1 = this.backtestSettings.chartSetups.size();
      if (var1 > 1) {
         var1++;
      }

      if (this.backtestSettings.useCrossChecks) {
         for (int var2 = 0; var2 < this.backtestSettings.crossChecks.size(); var2++) {
            ICrossCheck var3 = this.backtestSettings.crossChecks.get(var2);
            if (var3.doesRetest()) {
               if (var3.doesForEverySetup()) {
                  var1 += var3.getNumberOfSimulations() * this.backtestSettings.chartSetups.size();
               } else {
                  var1 += var3.getNumberOfSimulations();
               }
            } else {
               var1++;
            }
         }
      }

      this.totalProgressSteps = var1;
      this.progressStepWeight = 1.0 / this.totalProgressSteps;
   }

   private void computeFitnessOnMainData(ResultsGroup var1, int var2, boolean var3) throws Exception {
      IFitnessFunction var4 = this.backtestSettings.fitnessFunction;
      if (var4 == null) {
         String var5 = "<Rankings>\r\n  <FitnessCriteria method=\"ComputeFromStrategyResult\">\r\n    <Settings>\r\n      <Ranking type=\"ReturnDDRatio\" />\r\n    </Settings>\r\n  </FitnessCriteria>\r\n</Rankings>";
         Element var6 = XMLUtil.stringToElement(var5);
         var4 = ProjectConfigHelper.getFitnessFunction(var6);
      }

      if (var3
         || var2 != 3
         || var4.getClass().getSimpleName().equals("FitnessMethodStrategyResult")
         || var4.getClass().getSimpleName().equals("FitnessExistingPortfolio")) {
         ResultsGroup var11 = null;
         if (var4.getClass().getSimpleName().equals("FitnessExistingPortfolio")) {
            FitPortfolio var12 = this.backtestSettings.fitPortfolio;
            if (var12 != null) {
               Databank var7 = var12.databank;
               if (var7.getRecords().size() > 0) {
                  ArrayList var8 = new ArrayList();
                  var8.add(var1);

                  for (int var9 = 0; var9 < var7.getRecords().size(); var9++) {
                     ResultsGroup var10 = var7.getRecords().get(var9);
                     var8.add(var10);
                  }

                  var11 = ResultsGroup.merge(var8, new String[]{"AdditionalMarket", "CrossCheck_"}, null);
                  var11.createPortfolioResult("sum", null);
               }
            }
         }

         this.computeFitness(var4, var1, var11, (byte)0, (byte)127);
         this.computeFitness(var4, var1, var11, (byte)0, (byte)10);
         this.computeFitness(var4, var1, var11, (byte)0, (byte)20);
         OutOfSample var13 = this.backtestSettings.outOfSample;
         if (var13 != null) {
            this.computeFitness(var4, var1, var11, (byte)0, (byte)11);
            this.computeFitness(var4, var1, var11, (byte)0, (byte)40);
            int var14 = var13.getPartsCount((byte)40);

            for (int var16 = 0; var16 < var14; var16++) {
               this.computeFitness(var4, var1, var11, (byte)0, OutOfSample.getISVPart(var16 + 1));
            }

            var14 = var13.getPartsCount((byte)20);

            for (int var17 = 0; var17 < var14; var17++) {
               this.computeFitness(var4, var1, var11, (byte)0, OutOfSample.getOOSPart(var17 + 1));
            }
         } else {
            var1.portfolio().setFitness((byte)11, var1.portfolio().getFitness((byte)10));
            var1.portfolio().setFitness((byte)40, 0.0);
         }

         this.fitnessComputed = true;
      }
   }

   private boolean computeAndFilterFitnessOnCrossCheck(ResultsGroup var1, ICrossCheck var2) throws Exception {
      if (!this.backtestSettings.fitnessFunction.getClass().getSimpleName().equals(var2.getClass().getSimpleName())) {
         return true;
      }

      this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, (byte)127);
      this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, (byte)10);
      this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, (byte)20);
      OutOfSample var3 = this.backtestSettings.outOfSample;
      if (var3 != null) {
         this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, (byte)11);
         this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, (byte)40);
         int var4 = var3.getPartsCount((byte)40);

         for (int var5 = 0; var5 < var4; var5++) {
            this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, OutOfSample.getISVPart(var5 + 1));
         }

         var4 = var3.getPartsCount((byte)20);

         for (int var7 = 0; var7 < var4; var7++) {
            this.computeFitness(this.backtestSettings.fitnessFunction, var1, (byte)0, OutOfSample.getOOSPart(var7 + 1));
         }
      } else {
         var1.portfolio().setFitness((byte)11, var1.portfolio().getFitness((byte)10));
         var1.portfolio().setFitness((byte)40, 0.0);
      }

      this.fitnessComputed = true;
      return this.globalConditionsPassed(var1, 3);
   }

   private void computeFitness(IFitnessFunction var1, ResultsGroup var2, byte var3, byte var4) throws Exception {
      double var5 = var1.computeFitness(var2, var3, var4);
      var2.portfolio().setFitness(var4, var5);
      if (Log.isDebugEnabled() && var3 == 0 && var4 == 10) {
         Log.debug("Fitness (IS) computed to: " + var5);
      }
   }

   private void computeFitness(IFitnessFunction var1, ResultsGroup var2, ResultsGroup var3, byte var4, byte var5) throws Exception {
      double var6 = var1.computeFitness(var3 == null ? var2 : var3, var4, var5);
      var2.portfolio().setFitness(var5, var6);
      if (Log.isDebugEnabled() && var4 == 0 && var5 == 10) {
         Log.debug("Fitness (IS) computed to: " + var6);
      }
   }

   private BacktestResult runAndSaveCrossChecks(BacktestResult var1, int var2) {
      if (!this.backtestSettings.useCrossChecks) {
         return var1;
      }

      ResultsGroup var3 = var1.getResult();

      for (int var4 = 0; var4 < this.backtestSettings.crossChecks.size() && !this.stopPauseEngine.isStopped(); var4++) {
         ICrossCheck var5 = this.backtestSettings.crossChecks.get(var4);
         var5.setStopPauseEngine(this.stopPauseEngine);
         var5.registerProgressListener(new IBacktestProgressListener() {
            @Override
            public void setProgress(int var1) {
               BacktestRunner.this.setJobProgress(var1);
            }

            @Override
            public void increaseProgressStep() {
               BacktestRunner.this.currentProgressStep++;
            }
         });
         long var6 = System.currentTimeMillis();

         try {
            this.printEvent(L.t("Running cross check %s for %s...", new Object[]{var5.getName(), this.strategyName}));
            boolean var8 = var5.runTest(
               var3, var4, this.globalATR, this.parentGridJob, this.backtestSettings.singleThreadedOptimizations, this.lastEventListener, this.strategyName
            );
            this.durationStats.addDuration(var5.getName(), System.currentTimeMillis() - var6);
            if (!var8) {
               if (var3.specialValues().containsKey(SpecialValues.FiltersResultFailedReason)
                  && var3.specialValues().getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
                  var3.specialValues().set(SpecialValues.FiltersResultFailedReason, var5.getDismissalMessage());
               }

               var1.dismiss(
                  String.format("%s, " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, var5.getDismissalMessage()), var5.getDismissalReason()
               );
               if (!this.backtestSettings.evaluateAllCrossChecks && this.fitnessComputed) {
                  return var1;
               }
            }

            if (!this.checkGlobalCrossCheckConditions(var5, var1.getResult(), var2)) {
               if (var3.specialValues().containsKey(SpecialValues.FiltersResultFailedReason)
                  && var3.specialValues().getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
                  var3.specialValues().set(SpecialValues.FiltersResultFailedReason, this.dismissalMessage);
               }

               var1.dismiss(String.format("%s, " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, this.dismissalMessage), this.dismissalReason);
               if (!this.backtestSettings.evaluateAllCrossChecks && this.fitnessComputed) {
                  return var1;
               }
            }

            if (!this.computeAndFilterFitnessOnCrossCheck(var3, var5)) {
               if (var3.specialValues().containsKey(SpecialValues.FiltersResultFailedReason)
                  && var3.specialValues().getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
                  var3.specialValues().set(SpecialValues.FiltersResultFailedReason, this.dismissalMessage);
               }

               var1.dismiss(String.format("%s, " + L.t("dismissed", new Object[0]) + ": %s", this.strategyName, this.dismissalMessage), this.dismissalReason);
               if (!this.backtestSettings.evaluateAllCrossChecks && this.fitnessComputed) {
                  return var1;
               }
            }
         } catch (StrategyStoppedException var9) {
            this.durationStats.addDuration(var5.getName(), System.currentTimeMillis() - var6);
            var1.dismiss(
               String.format("%s, " + L.t("exception in cross check", new Object[0]) + " %s - %s", this.strategyName, var5.getName(), var9.getMessage()), 10004
            );
         } catch (BadStrategyException var10) {
            if (var10.getReason() != 1 && var10.getReason() != 512) {
               Log.error("Cross check bad strategy exception: " + var10.getMessage(), var10);
            }

            this.durationStats.addDuration(var5.getName(), System.currentTimeMillis() - var6);
            var1.dismiss(
               String.format("%s, " + L.t("exception in cross check", new Object[0]) + " %s - %s", this.strategyName, var5.getName(), var10.getMessage()),
               var5.getBadStrategyReason()
            );
         } catch (Exception var11) {
            Log.error("Cross check exception: " + var11.getMessage(), var11);
            this.durationStats.addDuration(var5.getName(), System.currentTimeMillis() - var6);
            var1.dismiss(
               String.format("%s, " + L.t("exception in cross check", new Object[0]) + " %s - %s", this.strategyName, var5.getName(), var11.getMessage()),
               10004
            );
         }
      }

      return var1;
   }

   private boolean checkGlobalCrossCheckConditions(ICrossCheck var1, ResultsGroup var2, int var3) {
      if (var3 == 2) {
         return true;
      }

      ArrayList var4 = null;
      int var5;
      if (var3 == 1) {
         var4 = this.backtestSettings.initialGenerationConditions;
         var5 = 100000;
      } else {
         var4 = this.backtestSettings.rankingConditions;
         var5 = 200000;
      }

      if (var4 != null && var4.size() > 0) {
         for (int var6 = 0; var6 < var4.size(); var6++) {
            Condition var7 = (Condition)var4.get(var6);
            if (var7.isUsed()
               && (
                  var7.isCrossCheckCondition(var1.getSettingName()) || var7.isPortfolioCondition() && var1.getSettingName().equals("RetestOnAdditionalMarkets")
               )
               && (this.fitnessComputed || !var7.isFitnessCondition())) {
               try {
                  boolean var8 = this.conditionsChecker.check(var2, var7);
                  if (!var8) {
                     this.dismissalReason = var5 + var6;
                     this.dismissalMessage = L.t(
                        "%s filter: %s",
                        new Object[]{
                           var3 == 1 ? L.t("Initial population", new Object[0]) : L.t("Global", new Object[0]), this.conditionsChecker.dismissalMessage
                        }
                     );
                     return false;
                  }
               } catch (CrossCheckDataNotExistException var9) {
               } catch (Exception var10) {
                  Log.error("Error while checking condition", var10);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = L.t("Error while evaluating conditions: %s", new Object[]{var10.getMessage()});
                  return false;
               }
            }
         }
      }

      return true;
   }

   private ResultsGroup backtestStrategy(ChartSetup var1) throws Exception {
      ResultsGroup var2;
      if (var1.getBacktestEngine() != 1316847364 && var1.getBacktestEngine() != -1816889229) {
         BacktestEngine var6 = new BacktestEngine(TraderSimulators.getSimulator(var1));
         var6.setSingleThreaded(true);
         var6.setStopPauseEngine(this.stopPauseEngine);
         var6.setLastSettings(this.lastSettingsXml);
         var6.registerProgressListener(new IBacktestProgressListener() {
            @Override
            public void setProgress(int var1) {
               BacktestRunner.this.setJobProgress(var1);
            }

            @Override
            public void increaseProgressStep() {
            }
         });
         var6.addSetup(this.backtestSettings.getSettingsMap(var1));
         var2 = var6.runBacktest(this.backtestSettings.strategyName, this.backtestSettings.mainResultKey, false, this.backtestSettings.removeEndOfTestOrders)
            .getResults();
         this.globalATR = var6.getGlobalATR();
      } else {
         StockpickerBacktestEngine var3 = new StockpickerBacktestEngine();
         var3.setStopPauseEngine(this.stopPauseEngine);
         var3.setLastEventListener(this.lastEventListener);
         if (var1.getBacktestEngine() == -1816889229) {
            String var4 = var1.getMainChart().getSymbol();
            String var5 = StockpickerBacktestEngine.createBasketName(var4);
            var1 = var1.getClone(var5);
         }

         var3.registerProgressListener(new IBacktestProgressListener() {
            @Override
            public void setProgress(int var1) {
               BacktestRunner.this.setJobProgress(var1);
            }

            @Override
            public void increaseProgressStep() {
            }
         });
         var3.addSetup(this.backtestSettings.getSettingsMap(var1));
         var3.addStrategyXml(this.backtestSettings.elStrategy);
         var3.setLastSettings(this.lastSettingsXml);
         var2 = var3.runBacktest(this.backtestSettings.strategyName);
         this.globalATR = var3.getGlobalATR();
      }

      return var2;
   }

   protected void setJobProgress(double var1) {
      if (this.parentGridJob != null) {
         double var3 = this.currentProgressStep / this.totalProgressSteps + this.progressStepWeight * var1 / 100.0;
         this.parentGridJob.sendProgress((int)(var3 * 100.0));
      }
   }

   private boolean globalConditionsPassed(ResultsGroup var1, int var2) {
      if (var2 == 2) {
         return true;
      }

      ArrayList var3 = null;
      int var4;
      if (var2 == 1) {
         var3 = this.backtestSettings.initialGenerationConditions;
         var4 = 100000;
      } else {
         var3 = this.backtestSettings.rankingConditions;
         var4 = 200000;
      }

      if (var3 != null && var3.size() > 0) {
         for (int var5 = 0; var5 < var3.size(); var5++) {
            Condition var6 = (Condition)var3.get(var5);
            if (var6.isUsed()
               && !var6.isCrossCheckCondition()
               && (!var6.isPortfolioCondition() || !this.isCrossCheckRetestOnAdditionalMarketsUsed())
               && (this.fitnessComputed || !var6.isFitnessCondition())) {
               try {
                  boolean var7 = this.conditionsChecker.check(var1, var6);
                  if (!var7) {
                     this.dismissalReason = var4 + var5;
                     if (var2 == 1) {
                        this.dismissalMessage = L.t("Initial population filter", new Object[0]) + ": " + this.conditionsChecker.dismissalMessage;
                     } else {
                        this.dismissalMessage = L.t("Global filter", new Object[0]) + ":" + this.conditionsChecker.dismissalMessage;
                     }

                     return false;
                  }
               } catch (Exception var8) {
                  Log.error("Error while checking condition", var8);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = L.t("Error while evaluating conditions: %s", new Object[]{var8.getMessage()});
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean isCrossCheckRetestOnAdditionalMarketsUsed() {
      if (!this.backtestSettings.useCrossChecks) {
         return false;
      }

      for (int var1 = 0; var1 < this.backtestSettings.crossChecks.size(); var1++) {
         ICrossCheck var2 = this.backtestSettings.crossChecks.get(var1);
         if (var2.getSettingName().equals("RetestOnAdditionalMarkets")) {
            return true;
         }
      }

      return false;
   }

   public void stop() {
      this.stopPauseEngine.stop();
   }

   public void setStrategy(Element var1) {
      this.backtestSettings.elStrategy = var1;
   }

   public void setStrategyName(String var1) {
      this.strategyName = var1;
   }

   public void setEvolution(boolean var1) {
      this.isEvolution = var1;
   }

   public BacktestSettings getBacktestSettings() {
      return this.backtestSettings;
   }
}
