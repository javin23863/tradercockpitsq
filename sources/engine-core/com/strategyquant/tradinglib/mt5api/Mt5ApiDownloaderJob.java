package com.strategyquant.tradinglib.mt5api;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.python.PythonRunner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mt5ApiDownloaderJob extends GridJob<Void> {
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(Mt5ApiDownloaderJob.class);
   private DataManagerProgressListener listener;
   private DataInfo dataInfo;
   private Mt5ApiImportInfo importInfo;
   private long fromTime;
   private long toTime;
   private int rows;
   private volatile boolean paused;
   private volatile boolean stopped;

   public Mt5ApiDownloaderJob(String var1, DataManagerProgressListener var2, DataInfo var3, Mt5ApiImportInfo var4) {
      super(var1, 0, null);
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.stopped) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }

   private File downloadToFile() throws Exception {
      PythonRunner.ProgressCallbackAdapter var1 = new PythonRunner.ProgressCallbackAdapter() {
         @Override
         public void onMessage(String var1) {
            Mt5ApiDownloaderJob.Log.debug(var1);
            Mt5ApiDownloaderJob.this.listener.setMessage(var1);
         }

         @Override
         public void onError(String var1) {
            Mt5ApiDownloaderJob.Log.error("Error while fetching symbol list: " + var1);
            Mt5ApiDownloaderJob.this.listener.onError(var1);
         }

         @Override
         public void onProgress(double var1) {
            double var3 = var1 * 100.0;
            Mt5ApiDownloaderJob.this.listener.onProgress(var3 * 0.5);
         }

         @Override
         public void onResult(String var1) {
         }
      };
      Mt5ApiManager.Source var2 = this.importInfo.path == null ? Mt5ApiManager.Source.installed : Mt5ApiManager.Source.portable;
      return Mt5ApiManager.get().downloadData(var2, this.importInfo.path, this.dataInfo, this.importInfo.dateFrom, this.importInfo.dateTo, var1);
   }

   public Void call() throws Exception {
      long var1 = this.dataInfo.dateFrom;
      if (var1 > 0L) {
         this.listener.onError("MT5 Api ticker are not updatable");
         return null;
      }

      try {
         boolean var3 = this.dataInfo.timeframe.equals("TICK");
         File var4 = this.downloadToFile();
         this.copyData(var4, var3);

         try {
            Files.delete(var4.toPath());
         } catch (Exception var10) {
         }

         this.finish(var3);
      } catch (Exception var11) {
         Log.error("", var11);
         this.listener.onError("Error while downloading");
      } finally {
         this.listener.onProgress(100.0);
         this.listener.setMessage(L.t("Completed", new Object[0]));
         this.sendRefreshMessage();
      }

      return null;
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.paused = true;
            break;
         case 3:
            this.paused = false;
            break;
         case 4:
            this.stopped = true;
            this.listener.setMessage(L.t("Stopping download - please wait", new Object[0]));
            Mt5ApiManager.get().cancel();
      }
   }

   private void finish(boolean var1) {
      if (this.rows != 0) {
         long var2;
         if (var1) {
            var2 = this.rows;
         } else {
            var2 = this.rows * 60;
         }

         DataManager.updateData("History", this.importInfo.symbol, this.fromTime, this.toTime, this.rows, var2, 1, this.dataInfo.timeframe, "Exchange");
      }
   }

   private void copyData(File var1, boolean var2) throws Exception {
      if (!this.stopped) {
         int var3 = var2 ? 2 : 1;
         DataBinWriterNew var4 = DataBinWriterNew.getInstance(var3, MainApp.getDataPath(), this.importInfo.instrumentInfo);
         String var5 = DataManager.getDataFileName("History", this.importInfo.symbol, this.dataInfo.timeframe, "No Session");
         var4.setFileName(var5);
         var4.open();
         this.fromTime = -1L;
         this.toTime = -1L;
         this.rows = 0;
         long var6 = var1.length();
         long var8 = 0L;

         try {
            BufferedReader var10 = new BufferedReader(new FileReader(var1));

            String var11;
            try {
               while ((var11 = var10.readLine()) != null) {
                  var8 += var11.length() + 1;
                  if (this.stopped) {
                     break;
                  }

                  this.checkPaused();
                  var11 = var11.trim();
                  if (!var11.isEmpty() && !var11.contains("time")) {
                     try {
                        VersatileData var12 = var2 ? this.parseTickLine(var11) : this.parseOhlcLine(var11);
                        var4.writeData(var12);
                        this.rows++;
                        if (this.fromTime == -1L) {
                           this.fromTime = var12.time;
                        }

                        this.toTime = var12.time;
                        this.listener.setMessage("Writing " + SQTime.formatDate(var12.time));
                     } catch (Exception var29) {
                        Log.error("", var29);
                     } finally {
                        double var15 = (double)var8 / var6;
                        this.listener.onProgress(50.0 + var15 * 0.5);
                     }
                  }
               }
            } catch (Throwable var31) {
               try {
                  var10.close();
               } catch (Throwable var28) {
                  var31.addSuppressed(var28);
               }

               throw var31;
            }

            var10.close();
         } finally {
            var4.close();
         }
      }
   }

   private void sendRefreshMessage() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.importInfo.symbol, "download"), "SQUANT", "QDM", "AlgoWizard");
      } catch (Exception var2) {
         Log.error("Cannot send data Websocket message. ", var2);
      }
   }

   private VersatileData parseOhlcLine(String var1) {
      VersatileData var2 = new VersatileData();
      String[] var3 = var1.split(",");
      var2.time = SQTime.parseToMilis(var3[0], "yyyy-MM-dd HH:mm:ss");
      var2.open = Double.valueOf(var3[1]);
      var2.high = Double.valueOf(var3[2]);
      var2.low = Double.valueOf(var3[3]);
      var2.close = Double.valueOf(var3[4]);
      var2.volume = Double.valueOf(var3[5]);
      return var2;
   }

   private VersatileData parseTickLine(String var1) {
      VersatileData var2 = new VersatileData();
      String[] var3 = var1.split(",");
      long var4 = SQTime.parseToMilis(var3[0], "yyyy-MM-dd HH:mm:ss.SSS");
      var2.time = var4;
      var2.ask = Double.valueOf(var3[2]);
      var2.bid = Double.valueOf(var3[1]);
      var2.volume = Double.valueOf(var3[4]);
      return var2;
   }
}
