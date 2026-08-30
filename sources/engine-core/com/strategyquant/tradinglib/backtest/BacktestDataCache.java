package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.ticksimulator.SpreadsMap;
import com.strategyquant.lib.L;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.engine.BacktestTradingSetup;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestDataCache {
   public static final Logger Log = LoggerFactory.getLogger("BacktestDataCache");
   private static BacktestDataCache instance;
   private long cacheRecordsSize = 1000000L;
   private ConcurrentHashMap<Integer, LoadedData> mapCache = new ConcurrentHashMap<>();
   private Int2ObjectOpenHashMap<StartDataInfo> startDataInfoCache = new Int2ObjectOpenHashMap();
   private int dataType = 0;
   private LoadDataProgressEngine loadDataProgressEngine;
   private StampedLock cacheLock = new StampedLock();

   public static synchronized BacktestDataCache getInstance() {
      if (instance == null) {
         instance = new BacktestDataCache();
      }

      return instance;
   }

   public LoadedData loadData(BacktestTradingSetup[] var1, ArrayList<ChartDef> var2, SpreadsMap var3, int var4, ITradingSimulator var5) throws Exception {
      int var6 = this.computeHash(var2, var4, var1);
      LoadedData var7 = null;
      Object var8 = null;
      LoadDataProgressEngine var9 = this.loadDataProgressEngine;
      if (var9 != null) {
         var9.printToLog(L.t("Loading backtest data for %s - %s", new Object[]{var9.getTestName(), var9.getSymbolTF()}));
      }

      try {
         return this._loadData(var6, var1, var2, var3, var4, var5);
      } catch (TaskStoppedException var15) {
         Log.info("Loading data stopped");
         this.mapCache.remove(var6);
         throw var15;
      } catch (Exception var16) {
         Log.error("Exception when loading data", var16);
         String var10 = TestfilesUtils.getTestdataFile(var6);
         File var11 = new File(var10);
         if (var11.exists()) {
            if (!var11.delete()) {
               throw new Exception(L.t("Cannot delete corrupted data file '%s'", new Object[]{var10}), var16);
            }

            this.mapCache.remove(var6);
            Log.info("Deleted corrupted data file {}, regenerating it again", var10);
         }

         try {
            var7 = this._loadData(var6, var1, var2, var3, var4, var5);
         } catch (TaskStoppedException var13) {
            Log.info("Loading data stopped");
            throw var13;
         } catch (Exception var14) {
            Log.error("Exception when loading data", var14);
         }

         return var7;
      }
   }

   private LoadedData _loadData(int var1, BacktestTradingSetup[] var2, ArrayList<ChartDef> var3, SpreadsMap var4, int var5, ITradingSimulator var6) throws Exception {
      LoadedData var7 = this.mapCache
         .computeIfAbsent(var1, var7x -> this.createLoadedData(var1, this.dataType, var2, var3, var4, var5, var6, this.loadDataProgressEngine));
      if (var7 == null) {
         this.mapCache.remove(var1);
         throw new Exception(L.t("Loaded data is null", new Object[0]));
      } else if (var7.getException() != null) {
         this.mapCache.remove(var1);
         throw var7.getException();
      } else if (!var7.isJustCreated()) {
         LoadedData var8 = new LoadedData(var7, this.loadDataProgressEngine);
         var8.setLoadStats(var7.getLoadStats());
         var7.resetLoadStats();
         return var8;
      } else {
         var7.setNotJustCreated();
         return var7;
      }
   }

   private LoadedData createLoadedData(
      int var1, int var2, BacktestTradingSetup[] var3, ArrayList<ChartDef> var4, SpreadsMap var5, int var6, ITradingSimulator var7, LoadDataProgressEngine var8
   ) {
      LoadedData var9 = new LoadedData(var1, var2);
      var9.trackUsed();

      try {
         var9.load(var3, var4, var5, var6, var7, var8);
      } catch (Exception var11) {
      }

      return var9;
   }

   private int computeHash(ArrayList<ChartDef> var1, int var2, BacktestTradingSetup[] var3) {
      StringBuilder var4 = new StringBuilder();
      var4.append(var2);

      for (ChartDef var6 : var1) {
         var4.append(var6.getChartsHash());
      }

      for (BacktestTradingSetup var8 : var3) {
         var4.append(var8.getChartsHash());
      }

      return var4.toString().hashCode();
   }

   public synchronized void getRealStartDataInfo(ChartDef var1, int var2, ITradingSimulator var3, StartDataInfo var4) throws Exception {
      int var6 = var1.getChartsHash().hashCode() + var3.getClass().getName().hashCode();
      StartDataInfo var5;
      if (!this.startDataInfoCache.containsKey(var6)) {
         var5 = new StartDataInfo();
         this.getRealDataStartTime(var1, var2, var3, var5);
         this.startDataInfoCache.put(var6, var5);
      } else {
         var5 = (StartDataInfo)this.startDataInfoCache.get(var6);
      }

      var4.copyFrom(var5);
   }

   private void getRealDataStartTime(ChartDef var1, int var2, ITradingSimulator var3, StartDataInfo var4) throws Exception {
      ReservedBarsType var5 = var3.getReservedBarsType();
      if (var5.type == 2) {
         var4.realDataStartTime = var1.getHistoryFrom();
         var4.reservedBars = Math.max(var5.minimumReservedBars, var2);
      }

      BacktestRealDataStartTimeComputer var6 = new BacktestRealDataStartTimeComputer(var2, var1, var3);
      var6.computeRealDataStartTime(var4, this.loadDataProgressEngine);
      var6.destroy();
   }

   public void setDataType(int var1) {
      this.dataType = var1;
   }

   public void setLoadDataProgressEngine(LoadDataProgressEngine var1) {
      this.loadDataProgressEngine = var1;
   }

   public synchronized long freeCache() {
      return 0L;
   }

   public void deleteTempTestfile(String var1) {
      TestfilesUtils.deleteTempTestfile(var1);
   }
}
