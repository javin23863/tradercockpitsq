package com.strategyquant.tradinglib.engine.stockpicker.data.symbols;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.lib.L;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymbolsDataCache {
   public static final Logger Log = LoggerFactory.getLogger(SymbolsDataCache.class);
   private ILastEventListener lastEventListener;
   private static SymbolsDataCache instance;
   private ConcurrentHashMap<Integer, LoadedSymbolData> mapCache = new ConcurrentHashMap<>();
   private IBacktestProgressListener backtestProgressListener;

   public static synchronized SymbolsDataCache getInstance() {
      if (instance == null) {
         instance = new SymbolsDataCache();
      }

      return instance;
   }

   public LoadedSymbolData loadData(DataInfo var1, PickerDataTimeline var2, StopPauseEngine var3) throws Exception {
      if (this.lastEventListener != null) {
         this.lastEventListener.setLastEvent(L.t("Loading symbol %s data ...", new Object[]{var1.symbol}));
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
         throw new Exception(L.t("Exception when loading symbol data - %s", new Object[]{var8.getMessage()}), var8);
      }
   }

   private LoadedSymbolData _loadData(int var1, DataInfo var2, PickerDataTimeline var3, StopPauseEngine var4) throws Exception {
      LoadedSymbolData var5 = this.mapCache.computeIfAbsent(var1, var5x -> this.createLoadedData(var1, var2, var3, var4));
      if (var5 == null) {
         this.mapCache.remove(var1);
         throw new Exception(L.t("Loaded picker data is null", new Object[0]));
      } else if (var5.getException() != null) {
         this.mapCache.remove(var1);
         throw var5.getException();
      } else {
         return var5;
      }
   }

   private LoadedSymbolData createLoadedData(int var1, DataInfo var2, PickerDataTimeline var3, StopPauseEngine var4) {
      LoadedSymbolData var5 = null;

      try {
         Log.debug(String.format("Loading symbol %s data from file...", var2.symbol));
         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(L.t("Loading symbol %s data from file...", new Object[]{var2.symbol}));
         }

         SymbolDataLoader var6 = new SymbolDataLoader();
         var5 = var6.load(var2, var3, var4);
      } catch (Exception var7) {
         var5 = new LoadedSymbolData();
         var5.setException(var7);
      }

      return var5;
   }

   private int computeHash(DataInfo var1, PickerDataTimeline var2) {
      StringBuilder var3 = new StringBuilder();
      var3.append(var2.hashCode());
      var3.append(var1.symbol);
      var3.append(var1.dateFrom);
      var3.append(var1.dateTo);
      return var3.toString().hashCode();
   }

   public void setLastEventListener(ILastEventListener var1) {
      this.lastEventListener = var1;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }
}
