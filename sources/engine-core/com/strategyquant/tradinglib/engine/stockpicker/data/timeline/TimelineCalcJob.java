package com.strategyquant.tradinglib.engine.stockpicker.data.timeline;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.PortfolioBacktestJob;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.HashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimelineCalcJob extends GridJob<String> {
   public static final Logger Log = LoggerFactory.getLogger(PortfolioBacktestJob.class);
   private String symbol;
   private Set<Long> timeline;
   private StopPauseEngine stopPauseEngine;

   public TimelineCalcJob(String var1, String var2, Set<Long> var3, StopPauseEngine var4) {
      super(var2, 1, new HashMap());
      this.symbol = var1;
      this.timeline = var3;
      this.stopPauseEngine = var4;
   }

   public String call() throws Exception {
      try {
         DataInfo var1 = DataManager.getDataInfo("History", this.symbol, true);
         if (var1 == null || var1.rows < 1) {
            return null;
         }

         this.loadSymbolData(var1);
      } catch (Exception var2) {
         Log.info(String.format("Couldn't load %s data. Reason: %s", this.symbol, var2.getMessage()), var2);
      }

      return null;
   }

   private void loadSymbolData(DataInfo var1) throws Exception {
      IDataLoader var2 = null;

      try {
         var2 = DataManager.getDataLoader(
            new ChartDef("History", var1.symbol, "D1", var1.dateFrom, var1.dateTo, var1.symbolInfo.defaultSpread, "No Session"), 1
         );
         var2.open();
         VersatileData var3 = new VersatileData();

         while (var2.hasNextTick()) {
            if (this.stopPauseEngine.isStopped()) {
               throw new TaskStoppedException();
            }

            var2.getNextTick(var3);
            long var4 = SQTime.getDateInMs(var3.time);
            this.timeline.add(var4);
         }

         var2.close();
      } catch (Exception var9) {
         throw new Exception(L.t("Cannot load data for symbol %s - %s", new Object[]{var1.symbol, var9.getMessage()}), var9);
      } finally {
         if (var2 != null) {
            var2.close();
         }
      }
   }
}
