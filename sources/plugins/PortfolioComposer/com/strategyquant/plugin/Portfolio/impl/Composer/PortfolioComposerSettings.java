package com.strategyquant.plugin.Portfolio.impl.Composer;

import com.strategyquant.tradinglib.ResultsGroup;
import java.io.Serializable;
import java.util.ArrayList;

public class PortfolioComposerSettings implements Serializable {
   public PortfolioComposerSettings.PortfolioSelectionType PortfolioSelectionType;
   public boolean dateRangeLimited = false;
   public long startDay = 0L;
   public long endDay = 0L;
   public double initialCapital = 0.0;
   public int leverage = 1;
   public int decimals;
   public int maxSimulations;
   public double confidenceLevel;
   public double RiskFreeRate;
   public ArrayList<ResultsGroup> strategies;
   public ArrayList<Double> weights;

   public PortfolioComposerSettings() {
      this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.SharpeRatio;
      this.maxSimulations = 100;
      this.confidenceLevel = 0.95;
      this.RiskFreeRate = 0.7;
      this.strategies = new ArrayList<>();
      this.weights = new ArrayList<>();
   }

   public void setSelectionTypeByString(String var1) throws Exception {
      switch (var1) {
         case "SharpRatio":
            this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.SharpeRatio;
            break;
         case "ReturnDrawdownRatio":
            this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.ReturnDrawdownRatio;
            break;
         case "CAGRMaxDrawdownRatio":
            this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.CAGRMaxDrawdownRatio;
            break;
         case "CAGRMeanDrawdownRatio":
            this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.CAGRMeanDrawdownRatio;
            break;
         case "NetProfit":
            this.PortfolioSelectionType = PortfolioComposerSettings.PortfolioSelectionType.NetProfit;
            break;
         default:
            throw new Exception("Invalid PortfolioSelectionType: " + var1);
      }
   }

   public int getDecimalsByFitness() {
      switch (this.PortfolioSelectionType) {
         case SharpeRatio:
            return 3;
         case ReturnDrawdownRatio:
         default:
            return 0;
         case CAGRMaxDrawdownRatio:
         case CAGRMeanDrawdownRatio:
            return 2;
      }
   }

   public enum PortfolioSelectionType {
      SharpeRatio,
      ReturnDrawdownRatio,
      CAGRMaxDrawdownRatio,
      CAGRMeanDrawdownRatio,
      NetProfit;
   }
}
