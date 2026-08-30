package com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.SymbolsDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockGroupDataLoaderJob extends GridJob<String> {
   public static final Logger Log = LoggerFactory.getLogger(StockGroupDataLoaderJob.class);
   private String symbol;
   private StockDto stockInfo;
   private ChartDef chartDef;
   private LoadedStockGroupData stockGroupData;
   private PickerDataTimeline timeline;
   private StopPauseEngine stopPauseEngine;
   private ILastEventListener lastEventListener;

   public StockGroupDataLoaderJob(String var1, String var2, LoadedStockGroupData var3, PickerDataTimeline var4, StopPauseEngine var5, ILastEventListener var6) {
      super(var2, 1, new HashMap());
      this.symbol = var1;
      this.stockGroupData = var3;
      this.timeline = var4;
      this.stopPauseEngine = var5;
      this.lastEventListener = var6;
   }

   public String call() throws Exception {
      try {
         if (Stockpicker.isDebugModeEnabled()) {
            Log.debug(String.format("Loading data %s...", this.symbol));
         }

         if (this.lastEventListener != null) {
            this.lastEventListener.setLastEvent(L.t("Loading data %s...", new Object[]{this.symbol}));
         }

         DataInfo var1 = DataManager.getDataInfo("History", this.symbol, true);
         if (var1 == null) {
            Log.debug("Data for symbol '" + this.symbol + "' cannot be found!");
            return null;
         }

         this.symbol = var1.symbol;
         if (var1.rows < 1) {
            Log.debug("No data available for symbol '" + this.symbol + "'");
            return null;
         }

         SymbolsDataCache var2 = SymbolsDataCache.getInstance();
         var2.setLastEventListener(this.lastEventListener);
         LoadedSymbolData var3 = var2.loadData(var1, this.timeline, this.stopPauseEngine);
         this.stockGroupData.putSymbolData(this.symbol, var3);
      } catch (Exception var4) {
         Log.info(String.format("Couldn't load %s data. Reason: %s", this.symbol, var4.getMessage()), var4);
      }

      return null;
   }
}
