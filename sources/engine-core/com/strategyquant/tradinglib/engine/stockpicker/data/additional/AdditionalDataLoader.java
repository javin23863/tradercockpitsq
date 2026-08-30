package com.strategyquant.tradinglib.engine.stockpicker.data.additional;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.SymbolsDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionalDataLoader {
   public static final Logger Log = LoggerFactory.getLogger(AdditionalDataLoader.class);
   private ChartSetup chartSetup;
   private ILastEventListener lastEventListener;
   private IBacktestProgressListener backtestProgressListener;
   private StopPauseEngine stopPauseEngine;
   private PickerDataTimeline timeline;
   private LoadedAdditionalData additionalData = new LoadedAdditionalData();

   public AdditionalDataLoader(ChartSetup var1, PickerDataTimeline var2, ILastEventListener var3, IBacktestProgressListener var4, StopPauseEngine var5) {
      this.chartSetup = var1;
      this.timeline = var2;
      this.lastEventListener = var3;
      this.backtestProgressListener = var4;
      this.stopPauseEngine = var5;
   }

   public LoadedAdditionalData load() throws Exception {
      long var1 = System.currentTimeMillis();
      String var3 = this.chartSetup.getMainChart().getSymbol();

      for (int var4 = 1; var4 < this.chartSetup.getCharts().size(); var4++) {
         String var5 = "NA";

         try {
            ChartDef var6 = this.chartSetup.getCharts().get(var4);
            var5 = var6.getSymbol();
            if (var3.equals(var5)) {
               this.additionalData.add(null);
            } else {
               if (Stockpicker.isDebugModeEnabled()) {
                  Log.debug(String.format("Loading additional data %d/%s...", var4, var5));
               }

               if (this.lastEventListener != null) {
                  this.lastEventListener.setLastEvent(L.t("Loading additional data %s...", new Object[]{var5}));
               }

               DataInfo var7 = DataManager.getDataInfo("History", var5, true);
               if (var7 == null) {
                  throw new Exception(L.t("Data for symbol '%s' cannot be found!", new Object[]{var5}));
               }

               var5 = var7.symbol;
               if (var7.rows < 1) {
                  throw new Exception(L.t("No data available for symbol '%s'", new Object[]{var5}));
               }

               SymbolsDataCache var8 = SymbolsDataCache.getInstance();
               var8.setLastEventListener(this.lastEventListener);
               LoadedSymbolData var9 = var8.loadData(var7, this.timeline, this.stopPauseEngine);
               this.additionalData.add(var9);
            }
         } catch (Exception var10) {
            throw new Exception(L.t("Error while loading additional data %d/%s - %s", new Object[]{var4, var5, var10.getMessage()}), var10);
         }
      }

      long var11 = System.currentTimeMillis() - var1;
      String var12 = SQTimeOld.formatDateTime((int)(var11 / 1000L));
      Log.info("Loading additional data done in {}", var12);
      return this.additionalData;
   }
}
