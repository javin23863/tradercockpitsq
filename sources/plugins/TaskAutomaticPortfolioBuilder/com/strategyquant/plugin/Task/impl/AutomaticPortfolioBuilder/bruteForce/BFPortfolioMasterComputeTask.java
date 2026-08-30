package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import java.util.concurrent.Callable;

public class BFPortfolioMasterComputeTask implements Callable<BacktestResult> {
   private PMComputationListener listener;
   private PortfolioMasterSettings settings;
   private int[] indexes;

   public BFPortfolioMasterComputeTask(int[] var1, PMComputationListener var2, PortfolioMasterSettings var3) {
      this.indexes = var1;
      this.listener = var2;
      this.settings = var3;
   }

   public BacktestResult call() throws Exception {
      PMPortfolioComputer var1 = new PMPortfolioComputer();
      PortfolioCombination var2 = this.listener.checkPortfolio(this.indexes);
      if (var2.dismiss != null) {
         return new BacktestResult(var2.dismiss);
      }

      ResultsGroup var3 = var1.createPortfolio(this.listener.getResults(this.indexes), var2.portfolioName, this.settings);
      PortfolioMasterFitness var4 = this.settings.fitnessFunction;
      double var5 = var4.compute(var3, this.settings.fitnessSampleType);
      var3.specialValues().set(PortfolioMasterSettings.PMFitnessValue, var4.print(var5));
      var3.portfolio().setFitness((byte)10, var4.transformToFitnessRange(var4.compute(var3, (byte)10)));
      var3.portfolio().setFitness((byte)20, var4.transformToFitnessRange(var4.compute(var3, (byte)20)));
      var3.portfolio().setFitness((byte)127, var4.transformToFitnessRange(var4.compute(var3, (byte)127)));
      var3.setNote(var2.portfolioIndexes);
      return new BacktestResult(var3);
   }
}
