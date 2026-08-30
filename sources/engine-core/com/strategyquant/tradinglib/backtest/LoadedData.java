package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.datalib.ticksimulator.ITickSimulator;
import com.strategyquant.datalib.ticksimulator.SpreadsMap;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.engine.BacktestTradingSetup;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedData {
   public static final Logger Log = LoggerFactory.getLogger("LoadedData");
   private ILoadedDataHolder loadedDataHolder = null;
   private DataLoadStats dataLoadStats = new DataLoadStats();
   private int hash;
   private Exception loadException = null;
   private int usedCounter = 0;
   private long lastTimeUsed = 0L;
   private boolean justCreated;
   private int dataType;
   protected final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
   private OutOfSample outOfSampleSettings;

   public LoadedData(int var1, int var2) {
      this.hash = var1;
      this.dataType = var2;
      this.justCreated = true;
   }

   public LoadedData(LoadedData var1, LoadDataProgressEngine var2) throws Exception {
      this.hash = var1.hash;
      this.dataType = var1.dataType;
      if (var1.loadException != null) {
         throw var1.loadException;
      }

      if (var2 != null) {
         var2.printToLog(L.t("Data loaded from memory", new Object[0]));
         var2.onDataPreparationFinished(0.0, var1.size());
      }

      this.loadedDataHolder = var1.loadedDataHolder.cloneHolder();
      this.justCreated = false;
   }

   public long size() throws Exception {
      return this.loadedDataHolder.size();
   }

   public VersatileData getNextTick(VersatileData var1) throws Exception {
      return this.loadedDataHolder.getNextTick(var1);
   }

   public DataLoadStats getLoadStats() {
      return this.dataLoadStats;
   }

   public void setLoadStats(DataLoadStats var1) {
      this.dataLoadStats.ticks = var1.ticks;
      this.dataLoadStats.loadingTime = var1.loadingTime;
   }

   public void resetLoadStats() {
      this.dataLoadStats.ticks = -1;
      this.dataLoadStats.loadingTime = 0L;
   }

   public void load(BacktestTradingSetup[] var1, ArrayList<ChartDef> var2, SpreadsMap var3, int var4, ITradingSimulator var5, LoadDataProgressEngine var6) throws Exception {
      try {
         LoadedDataHolderFileBDF var7 = new LoadedDataHolderFileBDF(this.hash);
         if (!var7.isLoaded()) {
            FeedDataCreator var8 = new FeedDataCreator();

            try {
               var7.beforeSave(this.computeHashPairs(var2));
               var8.loadFeed(var2, var4, this.dataLoadStats, var7, var6);
               var7.afterSave();
            } catch (TaskStoppedException var14) {
               var7.close();
               String var10 = TestfilesUtils.getTestdataFile(this.hash);
               File var11 = new File(var10);
               if (var11.exists()) {
                  if (!var11.delete()) {
                     Log.info("Cannot delete unfinished data file {}", var10);
                  } else {
                     Log.info("Deleted unfinished data file {}, will be created again", var10);
                  }
               }

               throw var14;
            } catch (Exception var15) {
               var7.close();
               throw var15;
            }
         }

         ILoadedDataHolder var17;
         try {
            var17 = var7;
            if (LoadedDataHolderOffheap.isEnoughMemory(this.hash, this.memoryBean)) {
               var17 = new LoadedDataHolderOffheap(this.hash, var7, this.dataLoadStats, var6);
               var7.close();
            }
         } catch (Exception var12) {
            var7.close();
            throw var12;
         } catch (OutOfMemoryError var13) {
            Log.info("Switching back to file-based data holder");
            var17 = var7.cloneHolder();
         }

         if (var17 instanceof LoadedDataHolderOffheap && var17.size() <= 0L) {
            throw new Exception(L.t("Data are empty - there could be problem with data source or sessions.", new Object[0]));
         }

         MarketData[] var9 = this.prepareMarketData(var1, var3, var17, var5, var6);
         this.loadedDataHolder = var17.cloneHolder();
         this.loadedDataHolder.setMarketData(var9);
      } catch (Exception var16) {
         this.loadException = var16;
         throw var16;
      }
   }

   private void exportToFile(String var1, TimeDataSeries var2, DataSeries var3) throws TradingException {
      StringBuilder var4 = new StringBuilder();

      for (int var5 = var2.size() - 1; var5 >= 0; var5--) {
         var4.append(SQTime.toDateMinuteString(var2.get(var5)));
         var4.append(";");
         var4.append(var3.get(var5));
         var4.append("\n");
      }

      SQUtils.stringToFile(new File("d:/temp3/exp/" + var1 + ".txt"), var4.toString());
   }

   private MarketData[] prepareMarketData(
      BacktestTradingSetup[] var1, SpreadsMap var2, ILoadedDataHolder var3, ITradingSimulator var4, LoadDataProgressEngine var5
   ) throws Exception {
      long var6 = System.currentTimeMillis();
      MarketData[] var8 = new MarketData[var1.length];

      for (int var9 = 0; var9 < var1.length; var9++) {
         var8[var9] = new MarketData(null, var1[var9].getChartSetup().getClone(), var1[var9].getIndyStartingBar());
         var8[var9].setPerformanceParams(false, false, false, false);
      }

      ITickSimulator var23 = var4.getTickSimulator();
      var23.setSymbolsSpread(var2);
      VersatileData var10 = new VersatileData();
      TickEvent var11 = new TickEvent();
      BarUpdateInfo var12 = new BarUpdateInfo();
      String var13 = var1[0].getChartSetup().getMainChart().getSession();
      Session var14 = SessionManager.getSession(var13);
      if (var14 == null) {
         var14 = SessionManager.getSession("No Session");
      }

      var14.clearSessionTempData();
      SessionStatus var15 = new SessionStatus();

      try {
         while (true) {
            var10 = var3.getNextTick(var10);
            if (var10 == null) {
               long var25 = System.currentTimeMillis();
               if (var5 != null) {
                  double var26 = (var25 - var6) / 1000.0;
                  var5.printToLog(L.t("Precomputed MarketData in %.4f s.", new Object[]{var26}));
               }
               break;
            }

            if (var10.type == 1) {
               double var24 = var2.getSpread(var10.connectionHash + var10.symbolHash);
               var10.ask = var24 == -1.0 ? var10.ask : SQUtils.round7(var10.bid + var24);
               var11.set(var10.time, var10.connectionHash, var10.symbolHash, var10.bid, var10.ask, var10.volume, var10.sessionStartTime, false, false);
               var12.updatedChart = -1;
               var12.multipleChartsUpdated = false;
               var12.eventType = 1;

               for (int var18 = 0; var18 < var1.length; var18++) {
                  var8[var18].processTick(var11, var12, 3);
                  var8[var18].processTick(var11, var12, 2);
               }

               var10.reset();
            } else {
               var23.init(var10);
               boolean var16 = false;

               while (var23.getNextTick(var11)) {
                  if (!var16) {
                     var14.checkTimeIsInSession(var11.getTime(), var15, null);
                     var16 = true;
                  }

                  var11.setSessionStartTime(var15.sessionStartTime);
                  var11.setSessionEndTime(var15.sessionEndTime);
                  var12.updatedChart = -1;
                  var12.multipleChartsUpdated = false;
                  var12.eventType = 1;

                  for (int var17 = 0; var17 < var1.length; var17++) {
                     var8[var17].processTick(var11, var12, 3);
                     var8[var17].processTick(var11, var12, 2);
                  }

                  var10.reset();
               }
            }
         }
      } finally {
         var3.afterProduce();
      }

      return var8;
   }

   private HashPair[] computeHashPairs(ArrayList<ChartDef> var1) {
      ArrayList var2 = new ArrayList();

      for (ChartDef var4 : var1) {
         HashPair var5 = new HashPair(var4.getConnectionHash(), var4.getSymbolHash());
         boolean var6 = false;

         for (HashPair var8 : var2) {
            if (var8.symbolHash == var5.symbolHash && var8.connectionHash == var5.connectionHash) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            var2.add(var5);
         }
      }

      HashPair[] var9 = new HashPair[var2.size()];
      int var10 = 0;

      for (HashPair var12 : var2) {
         var9[var10++] = var12;
      }

      return var9;
   }

   public void destroy() {
      this.loadedDataHolder.afterProduce();
   }

   public void trackUsed() {
      this.usedCounter++;
      this.lastTimeUsed = System.currentTimeMillis();
   }

   public void ensureDataType(int var1) throws Exception {
      this.checkDataType(this.dataType, var1);
   }

   private void checkDataType(int var1, int var2) throws Exception {
      if (var1 != var2) {
         ;
      }
   }

   public MarketData[] getMarketData() {
      return this.loadedDataHolder.getMarketData();
   }

   public long freeIfInMemory() {
      return this.loadedDataHolder.freeIfInMemory();
   }

   public boolean isJustCreated() {
      return this.justCreated;
   }

   public void setNotJustCreated() {
      this.justCreated = false;
   }

   public Exception getException() {
      return this.loadException;
   }
}
