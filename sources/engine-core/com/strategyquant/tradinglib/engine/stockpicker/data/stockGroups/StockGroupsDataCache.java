package com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockGroupsDataCache {
   public static final Logger Log = LoggerFactory.getLogger(StockGroupsDataCache.class);
   private ILastEventListener lastEventListener;
   private static StockGroupsDataCache instance;
   private ConcurrentHashMap<Integer, LoadedStockGroupData> mapCache = new ConcurrentHashMap<>();
   private StockGroupDataFile stockGroupFile = new StockGroupDataFile();
   private IBacktestProgressListener backtestProgressListener;

   public static synchronized StockGroupsDataCache getInstance() {
      if (instance == null) {
         instance = new StockGroupsDataCache();
      }

      return instance;
   }

   public LoadedStockGroupData loadData(ChartSetup var1, BasketDto var2, PickerDataTimeline var3, StopPauseEngine var4) throws Exception {
      if (this.lastEventListener != null) {
         this.lastEventListener.setLastEvent(L.t("Loading stock group data...", new Object[0]));
      }

      int var5 = this.computeHash(var2, var3);
      Object var6 = null;

      try {
         return this._loadData(var5, var1, var2, var3, var4);
      } catch (TaskStoppedException var8) {
         Log.info("Loading data stopped");
         this.mapCache.remove(var5);
         throw var8;
      } catch (Exception var9) {
         throw new Exception(L.t("Exception when loading stock group data - %s", new Object[]{var9.getMessage()}), var9);
      }
   }

   private LoadedStockGroupData _loadData(int var1, ChartSetup var2, BasketDto var3, PickerDataTimeline var4, StopPauseEngine var5) throws Exception {
      LoadedStockGroupData var6 = this.mapCache.computeIfAbsent(var1, var6x -> this.createLoadedData(var1, var2, var3, var4, var5));
      if (var6 == null) {
         this.mapCache.remove(var1);
         throw new Exception(L.t("Loaded stock group data is null", new Object[0]));
      } else if (var6.getException() != null) {
         this.mapCache.remove(var1);
         throw var6.getException();
      } else {
         return var6;
      }
   }

   private LoadedStockGroupData createLoadedData(int var1, ChartSetup var2, BasketDto var3, PickerDataTimeline var4, StopPauseEngine var5) {
      LoadedStockGroupData var6 = null;

      try {
         Log.info(String.format("Loading %s data from db...", var3.getName()));
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(String.format(L.t("Loading %s data from db...", new Object[0]), var3.getName()));
         }

         var6 = this.createDataFile(var1, var2, var3, var4, var5);
      } catch (Exception var8) {
         var6 = new LoadedStockGroupData();
         var6.setException(var8);
      }

      return var6;
   }

   private LoadedStockGroupData createDataFile(int var1, ChartSetup var2, BasketDto var3, PickerDataTimeline var4, StopPauseEngine var5) throws Exception {
      long var6 = System.currentTimeMillis();
      StockGroupsDataLoader var8 = new StockGroupsDataLoader(var2, var3, var4, this.lastEventListener, this.backtestProgressListener, var5);
      return var8.load();
   }

   private int computeHash(BasketDto var1, PickerDataTimeline var2) {
      long var3 = System.currentTimeMillis();
      StringBuilder var5 = new StringBuilder();
      var5.append(var2.hashCode());
      var5.append(var1.getId());
      List var6 = BasketOfStocksManager.getInstance().getStocks(var1.getId());

      for (int var7 = 0; var7 < var6.size(); var7++) {
         StockDto var8 = (StockDto)var6.get(var7);
         var5.append(var8.getTicker());
         DataInfo var9 = DataManager.getDataInfo("History", var8.getTicker(), true);
         if (var9 != null && var9.rows >= 1) {
            var5.append(var9.dateFrom);
            var5.append(var9.dateTo);
         }
      }

      int var10 = var5.toString().hashCode();
      long var11 = System.currentTimeMillis() - var3;
      Log.debug(String.format("Calculating hash of stock group data took " + var11 + "ms"));
      return var10;
   }

   private String getFileNameByHash(int var1) {
      return MainApp.getDataPath() + "internal/testfiles/picker/" + var1;
   }

   public void setLastEventListener(ILastEventListener var1) {
      this.lastEventListener = var1;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }
}
