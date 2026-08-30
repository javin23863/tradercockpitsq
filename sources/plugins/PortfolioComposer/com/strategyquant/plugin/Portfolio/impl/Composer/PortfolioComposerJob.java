package com.strategyquant.plugin.Portfolio.impl.Composer;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.tradinglib.ResultsGroup;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioComposerJob extends GridJob<ResultsGroup> {
   public static final Logger Log = LoggerFactory.getLogger(PortfolioComposerJob.class);
   private String portfolioName;
   private Map<String, Serializable> params;
   private PortfolioComposer composer = new PortfolioComposer();
   private PortfolioComposerSettings settings;
   private Double[] weights;

   public PortfolioComposerJob(String var1, Map<String, Serializable> var2) throws Exception {
      super(var1, 1, var2);
      this.portfolioName = (String)var2.get("StrategyName");
      this.settings = (PortfolioComposerSettings)var2.get("OptimizationSettings");
      this.settings = (PortfolioComposerSettings)var2.get("OptimizationSettings");
      this.weights = (Double[])var2.get("Weights");
   }

   public ResultsGroup call() throws Exception {
      try {
         return this.composer.recalculate(this.portfolioName, this.settings, this.weights);
      } catch (Exception var2) {
         throw var2;
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.composer.stop();
      }
   }
}
