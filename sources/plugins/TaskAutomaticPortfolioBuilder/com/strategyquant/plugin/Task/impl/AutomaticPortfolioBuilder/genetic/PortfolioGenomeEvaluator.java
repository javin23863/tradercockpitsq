package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PMComputationListener;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PMPortfolioComputer;
import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PortfolioCombination;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

public class PortfolioGenomeEvaluator implements FitnessEvaluator<PortfolioGenome> {
   public static final Logger Log = LoggerFactory.getLogger("PortfolioGenomeEvaluator");
   private GenomeSettings settings;
   private PMComputationListener listener = null;
   private PMPortfolioComputer portfolioComputer = new PMPortfolioComputer();
   private static PortfolioFitnessCache fitnessCache = new PortfolioFitnessCache();
   private PortfolioMasterSettings pmSettings;

   public PortfolioGenomeEvaluator(GenomeSettings var1, PMComputationListener var2, PortfolioMasterSettings var3) {
      this.settings = var1;
      this.listener = var2;
      this.pmSettings = var3;
      fitnessCache.reset();
   }

   public double getFitness(PortfolioGenome var1, List<? extends PortfolioGenome> var2) {
      try {
         int var3 = var1.getHash();
         double var4 = fitnessCache.getFitness(var3);
         if (var4 >= 0.0) {
            return var4;
         } else {
            int[] var6 = var1.getIndexes();
            PortfolioCombination var7 = this.listener.checkPortfolio(var6);
            if (var7.dismiss != null) {
               fitnessCache.setFitness(var3, 0.0);
               this.listener.portfolioComputed(new BacktestResult(var7.dismiss));
               return 0.0;
            } else {
               ResultsGroup var8 = this.portfolioComputer.createPortfolio(this.listener.getResults(var6), var7.portfolioName, this.pmSettings);
               PortfolioMasterFitness var9 = this.pmSettings.fitnessFunction;
               double var10 = var9.compute(var8, this.pmSettings.fitnessSampleType);
               var8.specialValues().set(PortfolioMasterSettings.PMFitnessValue, var9.print(var10));
               var8.portfolio().setFitness((byte)10, var9.transformToFitnessRange(var9.compute(var8, (byte)10)));
               var8.portfolio().setFitness((byte)20, var9.transformToFitnessRange(var9.compute(var8, (byte)20)));
               var8.portfolio().setFitness((byte)127, var9.transformToFitnessRange(var9.compute(var8, (byte)127)));
               var8.setNote(var7.portfolioIndexes);
               var4 = var8.getFitness();
               fitnessCache.setFitness(var3, var4);
               this.listener.portfolioComputed(new BacktestResult(var8));
               return var4;
            }
         }
      } catch (Exception var12) {
         Log.error("Exc.", var12);
         return 0.0;
      }
   }

   public boolean isNatural() {
      return this.settings.fitnessMaximize;
   }
}
