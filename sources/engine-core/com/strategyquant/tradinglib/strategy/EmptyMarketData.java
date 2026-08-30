package com.strategyquant.tradinglib.strategy;

import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.engine.TradingSetup;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyMarketData extends MarketData {
   public static final Logger Log = LoggerFactory.getLogger("EmptyMarketData");
   private ArrayList<ChartData> chartData;
   private TradingSetup tradingSetup;
   private ChartSetup chartSetup;
   private int mainChartBars = 0;
   private ChartData emptyChartData = new ChartData();

   @Override
   public ChartData Chart(int var1) {
      return this.emptyChartData;
   }
}
