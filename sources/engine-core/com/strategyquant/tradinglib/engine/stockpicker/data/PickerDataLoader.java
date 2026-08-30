package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.data.additional.AdditionalDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.StockGroupsDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.StockGroupsDataLoader;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.DefaultTimeline;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataLoader {
   public static final Logger Log = LoggerFactory.getLogger(StockGroupsDataLoader.class);
   private ChartSetup chartSetup;
   private BasketDto dto;
   private ILastEventListener lastEventListener;
   private IBacktestProgressListener backtestProgressListener;
   private StopPauseEngine stopPauseEngine;

   public PickerDataLoader(ChartSetup var1, BasketDto var2, ILastEventListener var3, IBacktestProgressListener var4, StopPauseEngine var5) {
      this.chartSetup = var1;
      this.dto = var2;
      this.lastEventListener = var3;
      this.backtestProgressListener = var4;
      this.stopPauseEngine = var5;
   }

   public LoadedPickerData load() throws Exception {
      long var1 = System.currentTimeMillis();
      LoadedPickerData var3 = new LoadedPickerData();
      PickerDataTimeline var4;
      if (this.useDefaultTimeline(this.dto)) {
         Log.info(String.format("Default timeline will be used while loading %s data...", this.dto.getName()));
         var4 = DefaultTimeline.getInstance();
      } else {
         var4 = new PickerDataTimeline();
         var4.init(this.dto, this.stopPauseEngine);
      }

      var3.timeline = var4;
      StockGroupsDataCache var5 = StockGroupsDataCache.getInstance();
      var5.setLastEventListener(this.lastEventListener);
      var5.registerProgressListener(this.backtestProgressListener);
      var3.stockGroupData = var5.loadData(this.chartSetup, this.dto, var4, this.stopPauseEngine);
      AdditionalDataCache var6 = AdditionalDataCache.getInstance();
      var6.setLastEventListener(this.lastEventListener);
      var6.registerProgressListener(this.backtestProgressListener);
      var3.additionalData = var6.loadData(this.chartSetup, var4, this.stopPauseEngine);
      long var7 = System.currentTimeMillis() - var1;
      String var9 = SQTimeOld.formatDateTime((int)(var7 / 1000L));
      Log.info("Loading {} data done in {}", this.dto.getName(), var9);
      return var3;
   }

   private boolean useDefaultTimeline(BasketDto var1) {
      return MainApp.getProduct() == "BACKTESTNODE" && var1.getName().startsWith("[[") && DefaultTimeline.getInstance().generated;
   }
}
