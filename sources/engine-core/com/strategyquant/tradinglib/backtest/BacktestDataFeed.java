package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.ticksimulator.ITickSimulator;
import com.strategyquant.datalib.ticksimulator.SpreadsMap;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.engine.BacktestTradingSetup;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.robustnesstests.DataModifierCallback;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.setup.Portfolio;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestDataFeed {
   public static final Logger Log = LoggerFactory.getLogger("BacktestDataFeed");
   private ConnectionManager connectionManager;
   volatile LoadedData loadedData = null;
   private BacktestTradingSetup[] tradingSetups;
   private ITradingSimulator defaultTradingSimulator;
   private StartDataInfo startDataInfo = new StartDataInfo();
   private int backtestPrecision;
   private ITradingSimulator[] tradingSimulators;
   private BacktestDataCache backtestDataCache = BacktestDataCache.getInstance();
   protected TickEvent tick = new TickEvent();
   protected long lastIndex;
   private DataModifierCallback dataModifierCallback = null;
   private StopPauseEngine stopPauseEngine;
   private SpreadsMap spreadsMap;
   private LoadDataProgressEngine loadDataProgressEngine;
   private int barEventType;
   private long totalTicks = 0L;
   private OutOfSample outOfSampleSettings;
   private boolean trading = true;

   public BacktestDataFeed(int var1, ConnectionManager var2, ITradingSimulator var3, ITradingSimulator[] var4) throws Exception {
      this.backtestPrecision = var1;
      this.connectionManager = var2;
      this.defaultTradingSimulator = var3;
      this.tradingSimulators = var4;
   }

   public void initialize(Portfolio var1, ITradingSimulator var2, OutOfSample var3) throws Exception {
      this.setPortfolio(var1);
      this.loadDataForFeed(var2, var3);
   }

   private void loadDataForFeed(ITradingSimulator var1, OutOfSample var2) throws Exception {
      this.loadedData = null;
      this.backtestDataCache.setLoadDataProgressEngine(this.loadDataProgressEngine);
      ArrayList var3 = this.getAllBacktestChartDefs();
      this.spreadsMap = new SpreadsMap(var3);
      this.loadedData = this.backtestDataCache.loadData(this.tradingSetups, var3, this.spreadsMap, this.backtestPrecision, var1);
      this.setOOS(var2);
      MarketData[] var4 = this.loadedData.getMarketData();
      if (var4 != null && var4.length == this.tradingSetups.length) {
         for (int var5 = 0; var5 < this.tradingSetups.length; var5++) {
            this.tradingSetups[var5].setLoadedMarketData(var4[var5]);
         }

         this.totalTicks = this.loadedData.size();
      } else {
         throw new Exception(L.t("Prepared MarketData array doesn't have as same size as TradingSetups!", new Object[0]));
      }
   }

   private void setOOS(OutOfSample var1) {
      this.outOfSampleSettings = var1;
   }

   public void start() throws Exception {
      long var1 = System.currentTimeMillis();
      VersatileData var3 = new VersatileData();
      TickEvent var4 = new TickEvent();
      ITickSimulator var5 = this.defaultTradingSimulator.getTickSimulator();
      var5.setSymbolsSpread(this.spreadsMap);
      this.barEventType = this.defaultTradingSimulator.getBarEventType();
      long var6 = 0L;
      this.trading = true;

      while (true) {
         var3 = this.loadedData.getNextTick(var3);
         if (var3 == null) {
            for (ITradingSimulator var18 : this.tradingSimulators) {
               var18.eventEndOfTest();
            }

            this.loadedData.destroy();
            long var16 = System.currentTimeMillis();
            if (Log.isDebugEnabled()) {
               Log.debug(String.format("SEND TOOK: %d ms, ticks: %d", var16 - var1, var6));
            }

            return;
         }

         if (this.stopPauseEngine != null && this.stopPauseEngine.isStopped()) {
            this.loadedData.destroy();
            long var14 = System.currentTimeMillis();
            if (Log.isDebugEnabled()) {
               Log.debug(String.format("BACKTEST DATA FEED STOPPED %d ms, ticks: %d", var14 - var1, this.loadedData.size()));
            }

            for (ITradingSimulator var13 : this.tradingSimulators) {
               var13.eventEndOfTest();
            }

            return;
         }

         if (var3.type == 1) {
            if (this.backtestPrecision != 4 && this.backtestPrecision != 5) {
               double var8 = this.spreadsMap.getSpread(var3.connectionHash + var3.symbolHash);
               var3.ask = var8 == -1.0 ? var3.ask : SQUtils.round7(var3.bid + var8);
            }

            var4.set(var3.time, var3.connectionHash, var3.symbolHash, var3.bid, var3.ask, var3.volume, var3.sessionStartTime, false, false);
            if (this.dataModifierCallback != null) {
               this.dataModifierCallback.modifyData(var4);
            }

            var6++;
            this.processEvent(var4, 0.0, 0.0);
         } else {
            if (this.dataModifierCallback != null) {
               this.dataModifierCallback.modifyOHLCData(var3);
            }

            var5.init(var3);

            while (var5.getNextTick(var4)) {
               if (this.dataModifierCallback != null) {
                  this.dataModifierCallback.modifyData(var4);
               }

               var6++;
               this.processEvent(var4, var3.high, var3.low);
            }
         }
      }
   }

   private void processEvent(TickEvent var1, double var2, double var4) throws Exception {
      if (this.outOfSampleSettings != null && this.outOfSampleSettings.hasNoTrade()) {
         if (this.trading) {
            if (this.outOfSampleSettings.isNoTrade(var1.getTime())) {
               this.trading = false;

               for (ITradingSimulator var9 : this.tradingSimulators) {
                  var9.eventNoTrade();
               }
            }
         } else if (!this.outOfSampleSettings.isNoTrade(var1.getTime())) {
            this.trading = true;
         }
      }

      if (this.barEventType != 3 && this.trading) {
         for (int var10 = 0; var10 < this.tradingSimulators.length; var10++) {
            this.tradingSimulators[var10].eventNewTick(var1, var2, var4);
         }
      }

      BarUpdateInfo var12 = null;

      for (int var13 = 0; var13 < this.tradingSetups.length; var13++) {
         BacktestTradingSetup var11 = this.tradingSetups[var13];
         var11.processEvent(var1, this.trading);
         if (var13 == 0) {
            var12 = var11.getBarUpdateInfo();
         }

         if (var11.getTradingException() != null) {
            throw var11.getTradingException();
         }
      }

      if (this.barEventType == 3) {
         if (var12.eventType == 5 && var12.mainChartEventType != -1) {
            var12.eventType = var12.mainChartEventType;
         }

         if (this.trading) {
            for (int var14 = 0; var14 < this.tradingSimulators.length; var14++) {
               this.tradingSimulators[var14].eventNewTick(var1, var2, var4, var12);
            }
         }
      }
   }

   public void setPortfolio(Portfolio var1) throws Exception {
      ArrayList var2 = var1.getTradingSetups();
      this.tradingSetups = new BacktestTradingSetup[var2.size()];
      int var3 = 0;

      for (TradingSetup var5 : var1.getTradingSetups()) {
         this.createConnectionsForSetup(var5.getChartSetup());
         var5.getChartSetup().runChecks(this.connectionManager);
         this.tradingSetups[var3++] = (BacktestTradingSetup)var5;
      }

      for (BacktestTradingSetup var7 : this.tradingSetups) {
         var7.setBarEventType(this.defaultTradingSimulator.getBarEventType());
      }
   }

   private void createConnectionsForSetup(ChartSetup var1) throws DataException {
      for (ChartDef var3 : var1.getCharts()) {
         String var4 = var3.getConnectionName();
         if (!this.connectionManager.hasConnection(var4)) {
            BacktestConnection var5 = new BacktestConnection(var4, this);
            this.connectionManager.addConnection(var4, var5);
         }
      }
   }

   public void setDataModifierCallback(DataModifierCallback var1) {
      this.dataModifierCallback = var1;
   }

   public void setStopPauseEngine(StopPauseEngine var1) {
      this.stopPauseEngine = var1;
   }

   public ArrayList<ChartDef> getAllBacktestChartDefs() throws Exception {
      ArrayList var1 = new ArrayList();

      for (BacktestTradingSetup var6 : this.tradingSetups) {
         ArrayList var2 = var6.getChartSetup().getCharts();

         for (int var7 = 0; var7 < var2.size(); var7++) {
            ChartDef var8 = (ChartDef)var2.get(var7);
            if (var7 == 0) {
               this.backtestDataCache.getRealStartDataInfo(var8, var6.getReservedBars(), this.defaultTradingSimulator, this.startDataInfo);
               var6.setReservedBars(this.startDataInfo.reservedBars);
            }

            var8.setLoadedHistoryFrom(this.startDataInfo.realDataStartTime);
            var1.add(new ChartDef(var8));
         }
      }

      return var1;
   }

   public void setLoadDataProgressEngine(LoadDataProgressEngine var1) {
      this.loadDataProgressEngine = var1;
   }

   public long getTotalTicks() {
      return this.totalTicks;
   }

   public MarketData getMarketData() {
      if (this.loadedData == null) {
         return null;
      }

      MarketData[] var1 = this.loadedData.getMarketData();
      return var1 != null && var1.length != 0 ? var1[0] : null;
   }
}
