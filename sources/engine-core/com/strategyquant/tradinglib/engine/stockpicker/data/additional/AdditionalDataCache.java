package com.strategyquant.tradinglib.engine.stockpicker.data.additional;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionalDataCache {
   public static final Logger Log = LoggerFactory.getLogger(AdditionalDataCache.class);
   private ILastEventListener lastEventListener;
   private static AdditionalDataCache instance;
   private ConcurrentHashMap<Integer, LoadedAdditionalData> mapCache = new ConcurrentHashMap<>();
   private AdditionalDataFile addditionalDataFile = new AdditionalDataFile();
   private IBacktestProgressListener backtestProgressListener;

   public static synchronized AdditionalDataCache getInstance() {
      if (instance == null) {
         instance = new AdditionalDataCache();
      }

      return instance;
   }

   public LoadedAdditionalData loadData(ChartSetup var1, PickerDataTimeline var2, StopPauseEngine var3) throws Exception {
      if (this.lastEventListener != null) {
         this.lastEventListener.setLastEvent(L.t("Loading additional data...", new Object[0]));
      }

      int var4 = this.computeHash(var1, var2);
      Object var5 = null;

      try {
         return this._loadData(var4, var1, var2, var3);
      } catch (TaskStoppedException var7) {
         Log.info("Loading data stopped");
         this.mapCache.remove(var4);
         throw var7;
      } catch (Exception var8) {
         throw new Exception(L.t("Exception when loading additional data - %s", new Object[]{var8.getMessage()}), var8);
      }
   }

   private LoadedAdditionalData _loadData(int var1, ChartSetup var2, PickerDataTimeline var3, StopPauseEngine var4) throws Exception {
      LoadedAdditionalData var5 = this.mapCache.computeIfAbsent(var1, var5x -> this.createLoadedData(var1, var2, var3, var4));
      if (var5 == null) {
         this.mapCache.remove(var1);
         throw new Exception(L.t("Loaded additional data is null", new Object[0]));
      } else if (var5.getException() != null) {
         this.mapCache.remove(var1);
         throw var5.getException();
      } else {
         return var5;
      }
   }

   private LoadedAdditionalData createLoadedData(int var1, ChartSetup var2, PickerDataTimeline var3, StopPauseEngine var4) {
      LoadedAdditionalData var5 = null;

      try {
         Log.info("Loading additional data from file...");
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(L.t("Loading additional data from file...", new Object[0]));
         }

         var5 = this.createDataFile(var1, var2, var3, var4);
      } catch (Exception var7) {
         var5 = new LoadedAdditionalData();
         var5.setException(var7);
      }

      return var5;
   }

   private LoadedAdditionalData createDataFile(int var1, ChartSetup var2, PickerDataTimeline var3, StopPauseEngine var4) throws Exception {
      long var5 = System.currentTimeMillis();
      AdditionalDataLoader var7 = new AdditionalDataLoader(var2, var3, this.lastEventListener, this.backtestProgressListener, var4);
      return var7.load();
   }

   private int computeHash(ChartSetup var1, PickerDataTimeline var2) {
      long var3 = System.currentTimeMillis();
      StringBuilder var5 = new StringBuilder();
      var5.append(var2.hashCode());

      for (int var6 = 1; var6 < var1.getCharts().size(); var6++) {
         ChartDef var7 = var1.getCharts().get(var6);
         var5.append(var7.getSymbol());
         var5.append(var7.getTimeframe());
      }

      int var9 = var5.toString().hashCode();
      long var10 = System.currentTimeMillis() - var3;
      Log.debug(String.format("Calculating hash of addditional data took " + var10 + "ms"));
      return var9;
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
