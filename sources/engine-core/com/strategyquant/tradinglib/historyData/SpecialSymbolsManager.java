package com.strategyquant.tradinglib.historyData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecialSymbolsManager {
   public static final Logger Log = LoggerFactory.getLogger(SpecialSymbolsManager.class);
   private static final String DEFAULT_CONNECTION = "History";
   private static final String SPY_SYMBOL = "SPY_benchmark.D";

   public static void updateAll() throws Exception {
      try {
         boolean var0 = MainApp.runInConsole() && !MainApp.CLIWebServeActive;
         if (var0) {
            return;
         }

         DataInfo var1 = DataManager.getDataInfo("History", "SPY_benchmark.D");
         if (var1 == null) {
            String var2 = "SPY_benchmark";
            if (!InstrumentManager.checkInstrumentExists(var2, true)) {
               InstrumentManager.addInstrument(
                  var2, "History data instrument", 1.0, 0.01, 0.01, 0.0, 0.0, "<Method type=\"None\" use=\"true\"><Params/></Method>", (byte)5
               );
            }

            LinkedList var3 = new LinkedList();
            var1 = new DataInfo();
            var1.timeframe = "D1";
            var1.barTimeType = 1;
            var1.timezone = "Exchange";
            var1.connection = "History";
            var1.symbol = "SPY_benchmark.D";
            var1.source = 5;
            var1.instrument = var2;
            var1.uSymbol = "SPY_benchmark.D";
            var1.uSymbolName = "SPY_benchmark.D";
            var1.symbolInfo = InstrumentManager.getInstrumentInfo(var2);
            var3.add(var1);
            DataManager.addDataInBatch("History", var3, null);
         }

         TickerDto var6 = HistoryDataManager.get().getStockTicker("SPY.D");
         var6.setTicker("SPY_benchmark.D");
         String var7 = "internal_download_SPY_benchmark.D";
         DownloadHistorySymbolJob var4 = new DownloadHistorySymbolJob(var7, var1, TickerKind.STOCK, var6);
         var4.setForceFlush(true);
         var4.setProgressListener((DataManagerProgressListener)DataManagerDataProgress.get().createListener(var1.symbol, "sqEquityData/updateDataAction"));
         SQGrid.getGridClient().executeOnGrid(var7, var4);
      } catch (Exception var5) {
         Log.error("Error while initialization of SPY_benchmark.D", var5);
      }
   }

   public static boolean isSpecial(String var0) {
      return "SPY_benchmark.D".equals(var0);
   }

   public static boolean isSpecial(DataInfo var0) {
      return isSpecial(var0.symbol);
   }
}
