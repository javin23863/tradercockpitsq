package com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockGroupsDataLoader {
   public static final Logger Log = LoggerFactory.getLogger(StockGroupsDataLoader.class);
   private ChartSetup chartSetup;
   private BasketDto dto;
   private ILastEventListener lastEventListener;
   private IBacktestProgressListener backtestProgressListener;
   private StopPauseEngine stopPauseEngine;
   private double total;
   private double processed = 0.0;
   private int progress = 1;
   private int lastProgress = 0;
   private int lastIndex;
   private PickerDataTimeline timeline;

   public StockGroupsDataLoader(
      ChartSetup var1, BasketDto var2, PickerDataTimeline var3, ILastEventListener var4, IBacktestProgressListener var5, StopPauseEngine var6
   ) {
      this.chartSetup = var1;
      this.dto = var2;
      this.timeline = var3;
      this.lastEventListener = var4;
      this.backtestProgressListener = var5;
      this.stopPauseEngine = var6;
   }

   public LoadedStockGroupData load() throws Exception {
      long var1 = System.currentTimeMillis();
      LoadedStockGroupData var3 = new LoadedStockGroupData(this.dto);
      this.runOnGrid(var3);
      long var4 = System.currentTimeMillis() - var1;
      String var6 = SQTimeOld.formatDateTime((int)(var4 / 1000L));
      Log.info("Loading {} data done in {}", this.dto.getName(), var6);
      return var3;
   }

   private void runOnGrid(final LoadedStockGroupData var1) throws Exception {
      this.lastIndex = 0;
      this.processed = 0.0;
      final List var2 = BasketOfStocksManager.getInstance().getStocks(this.dto.getId());
      this.total = var2.size();
      SimpleGridEngine var3 = new SimpleGridEngine<StockGroupDataLoaderJob, String>(this.stopPauseEngine, null, false) {
         @Override
         protected ArrayList<StockGroupDataLoaderJob> createJobsBatch(int var1x, GridClient var2x) throws Exception {
            return StockGroupsDataLoader.this.createBatch(var1x, var1, var2);
         }

         protected void processResult(String var1x, JobDetails var2x) throws Exception {
            StockGroupsDataLoader.this.processed++;
            StockGroupsDataLoader.this.progress = (int)(StockGroupsDataLoader.this.processed / StockGroupsDataLoader.this.total / 4.0 * 100.0);
            Log.debug("Loaded symbols {}/{}", StockGroupsDataLoader.this.processed, StockGroupsDataLoader.this.total);
            if (StockGroupsDataLoader.this.backtestProgressListener != null
               && StockGroupsDataLoader.this.progress != StockGroupsDataLoader.this.lastProgress
               && StockGroupsDataLoader.this.progress % 5.0 == 0.0) {
               StockGroupsDataLoader.this.backtestProgressListener.setProgress(StockGroupsDataLoader.this.progress);
               StockGroupsDataLoader.this.lastProgress = StockGroupsDataLoader.this.progress;
            }
         }

         @Override
         protected void onError(String var1x, Exception var2x) {
            Log.error("Stock groups loader Error {}, Exception ", var1x, var2x);
         }
      };
      var3.start();
      var3.unregisterListeners();
   }

   protected ArrayList<StockGroupDataLoaderJob> createBatch(int var1, LoadedStockGroupData var2, List<StockDto> var3) throws Exception {
      ArrayList var4 = new ArrayList();
      int var5 = 0;

      for (int var6 = this.lastIndex; var6 < this.lastIndex + var1 && !(var6 >= this.total); var6++) {
         if (this.stopPauseEngine.isStopped()) {
            throw new TaskStoppedException();
         }

         StockDto var7 = (StockDto)var3.get(var6);
         StockGroupDataLoaderJob var8 = new StockGroupDataLoaderJob(
            var7.getTicker(), var7.getTicker() + var6, var2, this.timeline, this.stopPauseEngine, this.lastEventListener
         );
         var4.add(var8);
         var5++;
      }

      this.lastIndex += var5;
      return var4.size() == 0 ? null : var4;
   }
}
