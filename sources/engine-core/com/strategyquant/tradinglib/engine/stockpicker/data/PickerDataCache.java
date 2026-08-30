package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataCache {
   public static final Logger Log = LoggerFactory.getLogger(PickerDataCache.class);
   private static PickerDataCache instance;
   private ConcurrentHashMap<Integer, LoadedPickerData> mapCache = new ConcurrentHashMap<>();
   private IBacktestProgressListener backtestProgressListener;

   public static synchronized PickerDataCache getInstance() {
      if (instance == null) {
         instance = new PickerDataCache();
      }

      return instance;
   }

   public LoadedPickerData loadData(ChartSetup var1, BasketDto var2, StopPauseEngine var3, ILastEventListener var4) throws Exception {
      int var5 = this.computeHash(var2, var1);
      Object var6 = null;

      try {
         return this._loadData(var5, var1, var2, var3, var4);
      } catch (TaskStoppedException var8) {
         Log.info("Loading data stopped");
         this.mapCache.remove(var5);
         throw var8;
      } catch (Exception var9) {
         throw new Exception(L.t("Exception when loading picker data - %s", new Object[]{var9.getMessage()}), var9);
      }
   }

   private LoadedPickerData _loadData(int var1, ChartSetup var2, BasketDto var3, StopPauseEngine var4, ILastEventListener var5) throws Exception {
      LoadedPickerData var6 = this.mapCache.computeIfAbsent(var1, var6x -> this.createLoadedData(var1, var2, var3, var4, var5));
      if (var6 == null) {
         this.mapCache.remove(var1);
         throw new Exception(L.t("Loaded picker data is null", new Object[0]));
      } else if (var6.getException() != null) {
         this.mapCache.remove(var1);
         throw var6.getException();
      } else {
         return var6;
      }
   }

   private LoadedPickerData createLoadedData(int var1, ChartSetup var2, BasketDto var3, StopPauseEngine var4, ILastEventListener var5) {
      LoadedPickerData var6 = null;

      try {
         Log.info(String.format("Loading %s data...", var3.getName()));
         if (var5 != null) {
            var5.setLastEvent(String.format(L.t("Loading %s data...", new Object[]{var3.getName()})));
         }

         PickerDataLoader var7 = new PickerDataLoader(var2, var3, var5, this.backtestProgressListener, var4);
         var6 = var7.load();
      } catch (Exception var8) {
         var6 = new LoadedPickerData();
         var6.setException(var8);
      }

      return var6;
   }

   private int computeHash(BasketDto var1, ChartSetup var2) {
      Log.debug(String.format("Calculating hash"));
      long var3 = System.currentTimeMillis();
      StringBuilder var5 = new StringBuilder();
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

      for (int var10 = 1; var10 < var2.getCharts().size(); var10++) {
         ChartDef var12 = var2.getCharts().get(var10);
         var5.append(var12.getSymbol());
         var5.append(var12.getTimeframe());
      }

      int var11 = var5.toString().hashCode();
      long var13 = System.currentTimeMillis() - var3;
      Log.debug(String.format("Calculating hash of stock group data took " + var13 + "ms"));
      return var11;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }
}
