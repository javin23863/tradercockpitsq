package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PMComputationListener;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PortfolioChecker;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PortfolioCombination;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PortfolioMasterComputer;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.conditions.DismissStruct;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectStatusListener;
import com.strategyquant.tradinglib.project.SQProject;
import java.math.BigInteger;
import java.util.ArrayList;

public class GPortfolioMasterComputer extends PortfolioMasterComputer implements PMComputationListener, ProjectStatusListener {
   private int minStrategiesInPortfolio;
   private int maxStrategiesInPortfolio;
   private BigInteger numberOfPossiblePortfolios;
   private long startTime;
   private long genStartTime;
   private int period;
   private CorrelationType type;
   private double maxCorrelation;
   private boolean allowNegativeCorrelation;
   private boolean addEmptyPeriods;
   private PortfolioMasterFitness fitnessFunction;
   private int maxFromSector;
   private PortfolioChecker portfolioChecker;

   public GPortfolioMasterComputer(ArrayList<ResultsGroup> var1, SQProject var2, ProgressEngine var3, PortfolioMasterSettings var4) throws Exception {
      this.originalResults = var1;
      this.minStrategiesInPortfolio = var4.minStrategiesInPortfolio;
      this.maxStrategiesInPortfolio = var4.maxStrategiesInPortfolio;
      if (this.maxStrategiesInPortfolio >= var1.size()) {
         this.maxStrategiesInPortfolio = var1.size() - 1;
      }

      this.maxFromSector = var4.maxStrategiesSector;
      this.numberOfPossiblePortfolios = var4.numberOfPossiblePortfolios;
      this.fitnessFunction = var4.fitnessFunction;
      this.period = var4.correlationPeriod;
      this.type = var4.correlationType;
      this.maxCorrelation = var4.maxCorrelation;
      this.allowNegativeCorrelation = var4.allowNegativeCorrelation;
      this.addEmptyPeriods = var4.addEmptyPeriods;
      this.project = var2;
      this.settings = var4;
      this.progressEngine = var3;
      var3.setProjectStatusListener(this);
   }

   @Override
   public void execute() throws Exception {
      this.startTime = System.currentTimeMillis();

      try {
         this.genStartTime = System.currentTimeMillis();
         this.beforeStart();
         this.portfolioChecker = new PortfolioChecker(
            this.period, this.type, this.maxCorrelation, this.allowNegativeCorrelation, this.addEmptyPeriods, this.maxFromSector, this.settings
         );
         this.portfolioChecker.setResults(this.originalResults);
         this.printLog("Genetic Portfolio Master");
         GPMasterComputer.run(
            this, this.fitnessFunction, this.originalResults.size(), this.minStrategiesInPortfolio, this.maxStrategiesInPortfolio, this.settings
         );
      } catch (Exception var2) {
         Log.error("GPortfolioMasterComputer error -  " + var2.getMessage(), var2);
         throw new Exception("GPortfolioMasterComputer error -  " + var2.getMessage(), var2);
      }
   }

   @Override
   public synchronized PortfolioCombination checkPortfolio(int[] var1) throws Exception {
      this.portfolioIndex++;
      PortfolioCombination var2 = new PortfolioCombination();
      var2.portfolioName = this.getPortfolioName(this.portfolioIndex);
      var2.portfolioIndexes = this.getPortfolioIndexes(var1);
      var2.dismiss = this.portfolioChecker.checkCorrectness(var1, var2.portfolioName);
      return var2;
   }

   @Override
   public synchronized void portfolioComputed(BacktestResult var1) throws Exception {
      try {
         if (var1.isDismissed()) {
            this.project.trackJob(null, var1.getDismissalReason(), null, 0, 0L, 0L);
            this.printLog(var1.getDismissalMessage());
         } else {
            ResultsGroup var2 = var1.getResult();
            DismissStruct var3 = this.checkCustomConditions(var2);
            if (var3 != null) {
               this.project.trackJob(null, var3.dismissReason, null, 0, 0L, 0L);
               this.printLog(L.t("%s - dismissed, reason: %s", new Object[]{var2.getName(), var3.dismissMessage}));
            } else {
               String var4 = this.fitnessFunction.getName()
                  + " "
                  + SampleTypes.typeToFullString(this.settings.fitnessSampleType)
                  + " "
                  + var2.specialValues().getString(PortfolioMasterSettings.PMFitnessValue);
               this.printLog(L.t("%s added. %s", new Object[]{var2.getName(), var4}));
               this.project.trackJob(null, 0, null, 0, 0L, 0L);
               this.settings.databankTarget.add(var2, true);
               this.settings.databankTarget.updateBestResults();
            }
         }
      } catch (Exception var5) {
         Log.error("Exc.", var5);
         throw var5;
      }
   }

   protected void done() {
      long var1 = System.currentTimeMillis();
      float var3 = (float)(var1 - this.startTime) / 1000.0F;
      this.printLog(String.format(L.t("Done in %s", new Object[0]), SQTime.formatDateTime(Float.valueOf(var3).intValue())));
      System.gc();
   }

   @Override
   public boolean isStopped() {
      return this.progressEngine.isStopped();
   }

   @Override
   public synchronized void printLog(String var1) {
      this.progressEngine.printToLogDebug(var1);
   }

   @Override
   public void resetStats() {
   }

   @Override
   public void computePortfolio(int[] var1) {
   }

   public void projectStatusChanged(int var1) {
      if (var1 == 4) {
         GPMasterComputer.userAbort.abort();
      }
   }
}
