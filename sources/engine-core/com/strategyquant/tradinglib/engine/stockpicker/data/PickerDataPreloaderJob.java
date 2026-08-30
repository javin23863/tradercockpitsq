package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.SymbolsDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataPreloaderJob extends GridJob<String> {
   public static final Logger Log = LoggerFactory.getLogger(PickerDataPreloaderJob.class);
   private String symbol;
   private BasketDto gi;
   private PickerDataTimeline timeline;
   private ConcurrentHashMap<String, String> symbolsMissing;

   public PickerDataPreloaderJob(String var1, String var2, BasketDto var3, PickerDataTimeline var4, ConcurrentHashMap<String, String> var5) {
      super(var2, 1, new HashMap());
      this.symbol = var1;
      this.gi = var3;
      this.timeline = var4;
      this.symbolsMissing = var5;
   }

   public String call() throws Exception {
      try {
         DataInfo var1 = DataManager.getDataInfo("History", this.symbol, true);
         if (var1 == null) {
            throw new Exception(L.t("Data for symbol '%s' cannot be found!", new Object[]{this.symbol}));
         }

         Log.debug(
            String.format(
               "Data found %s/%s, from %s to %s.", this.symbol, this.gi.getName(), SQTime.toDateString(var1.dateFrom), SQTime.toDateString(var1.dateTo)
            )
         );
         if (var1.rows < 1) {
            throw new Exception(L.t("No data available for symbol '%s'", new Object[]{this.symbol}));
         }

         SymbolsDataCache var2 = SymbolsDataCache.getInstance();
         var2.loadData(var1, this.timeline, new StopPauseEngine());
         return this.symbol;
      } catch (Exception var3) {
         this.symbolsMissing.put(this.symbol, this.gi.getName() + "," + var3.getMessage());
         Log.info(String.format("Couldn't load symbol data %s/%s. Reason: %s", this.symbol, this.gi.getName(), var3.getMessage()));
         return null;
      }
   }
}
