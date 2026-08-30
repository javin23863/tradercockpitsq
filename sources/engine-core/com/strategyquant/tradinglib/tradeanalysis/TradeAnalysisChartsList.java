package com.strategyquant.tradinglib.tradeanalysis;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class TradeAnalysisChartsList extends CustomClasses<TradeAnalysisChart> {
   private static TradeAnalysisChartsList instance;

   public static TradeAnalysisChartsList getInstance() {
      if (instance == null) {
         instance = new TradeAnalysisChartsList();
      }

      return instance;
   }

   private TradeAnalysisChartsList() {
      this.setDirName("TradeAnalysis");
      this.setExpectedClassType(TradeAnalysisChart.class);
      this.loadAvailableClasses();
   }
}
