package com.strategyquant.tradinglib.exchange;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.Pair;
import com.strategyquant.tradinglib.dukascopy.DukascopyBinaryMerger;
import com.strategyquant.tradinglib.dukascopy.DukascopyUtils;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.historyData.HttpApacheManager;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Exchange implements IExchange {
   private static final int STATUS_CODE_TOO_MANY_REQUESTS = 429;
   private static final int RETRY_COUNT = 5;
   public static final Logger Log = LoggerFactory.getLogger(Exchange.class);
   private static final int HOUR_CONSTANT = 3600;
   protected static final int DOWNLOAD_TIMEOUT = 120;
   protected volatile boolean canceled = false;
   protected volatile boolean stopping = false;
   protected volatile boolean paused = false;
   protected DukascopyBinaryMerger dataMerger;
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private volatile int avgDownloadSpeedCounter = 0;
   private volatile double avgDownloadSpeed = 0.0;
   private long lastPerformed = 0L;
   protected String jobId;
   protected DataManagerProgressListener listener;
   protected DataInfo dataInfo;
   protected ImportInfo importInfo;

   public String getProduct() {
      return "SQUANTQDM";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   @Override
   public void performDownload(String var1, DataManagerProgressListener var2, DataInfo var3, ImportInfo var4) throws Exception {
      this.jobId = var1;
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
      this.canceled = false;
      this.stopping = false;
      Log.debug("Starting download job for symbol:" + var4.symbol);

      try {
         this.prepareFiles();
         this.dataMerger = new DukascopyBinaryMerger(L.t("Downloaded", new Object[0]), var1, 1, var4);
         this.dataMerger.setProgressListener(var2);
         this.dataMerger.setWriter(this.writer);
         this.dataMerger.setReader(this.reader);
         var2.setMessage(L.t("Downloading...", new Object[0]));
         Log.debug("Starting download");
         Exception var5 = null;

         try {
            this.download();
         } catch (Exception var11) {
            var5 = var11;
         }

         this.finish();
         this.closeFiles(true);
         this.recomputeClonedData();
         if (var5 != null) {
            throw var5;
         }
      } catch (Exception var12) {
         Log.error(String.format("Error while performing %s data download", this.getName()), var12);
         this.onError(L.t("Error while performing %s data download.\n%s", new Object[]{this.getName(), var12.getMessage()}), var4);
         throw new Exception(L.t("Error while performing %s data download.\n%s", new Object[]{this.getName(), var12.getMessage()}));
      } finally {
         DataManagerDataProgress.get().removeListener(var4.symbol);
         Log.debug("Download job for symbol:" + var4.symbol + " finished");
      }
   }

   protected String doDownload(String var1) {
      int var2 = 0;

      while (var2 < 5) {
         try {
            byte[] var3 = HttpApacheManager.getInstance().getData(var1, 120);
            return new String(var3, StandardCharsets.UTF_8);
         } catch (HttpApacheManager.HttpException var5) {
            String var4 = new String(var5.getResultBytes(), StandardCharsets.UTF_8);
            Log.info("Detected error during download. Code:{}, Content:{}.", var5.getCode(), var4);
            if (var2 != 4) {
               this.addWaitingCycleAfterError(var5, var2 + 1);
            }

            var2++;
         } catch (Exception var6) {
            Log.error("Error while downloading url: " + var1, var6);
            throw new RuntimeException(L.t("Error while download data", new Object[0]), var6);
         }
      }

      throw new RuntimeException(L.t("Error while download data", new Object[0]));
   }

   private void addWaitingCycleAfterError(HttpApacheManager.HttpException var1, int var2) {
      int var3 = 5000;
      if (var1.getCode() == 429) {
         var3 = 120000 * var2;
         Log.info("Detected too many requests. Waiting for {}ms and trying again", var3);
      } else {
         Log.info("Detected http error. Waiting for {}ms and trying again", var3);
      }

      try {
         Thread.sleep(var3);
      } catch (InterruptedException var5) {
         Log.error("", var5);
      }
   }

   public abstract void download() throws Exception;

   protected void prepareFiles() throws Exception {
      Log.debug("Preparing data files");
      this.listener.setMessage(L.t("Preparing data files", new Object[0]));
      Pair var1 = DukascopyUtils.prepareFiles(this.importInfo, this.dataInfo.timeframe);
      this.reader = (DataBinReaderNew)var1.getA();
      this.writer = (DataBinWriterNew)var1.getB();
   }

   protected void closeFiles(boolean var1) {
      try {
         this.writer.close();
      } catch (Exception var4) {
      }

      try {
         if (this.reader != null) {
            this.reader.closeFile();
         }
      } catch (Exception var3) {
      }

      String var2 = this.dataInfo.timeframe;
      if (var1) {
         DukascopyUtils.deleteCopyFile(this.importInfo.symbol, var2);
      } else {
         DukascopyUtils.revertCopyFile(this.importInfo.symbol, var2);
      }
   }

   protected void finish() {
      try {
         if (this.canceled) {
            this.dataMerger.setStopped(this.canceled);
         }

         this.dataMerger.finish();
         long var1 = this.dataMerger.getFromDate();
         long var3 = this.dataMerger.getToDate();
         if (var1 != -1L && var3 != -1L) {
            DataManager.updateData(
               this.dataInfo.connection,
               this.dataInfo.symbol,
               var1,
               var3,
               this.dataMerger.getSavedRows(),
               0L,
               this.dataInfo.barTimeType,
               this.dataInfo.timeframe,
               "Etc/UCT"
            );
         }
      } catch (Exception var5) {
         Log.error("Error while saving results", var5);
         throw new RuntimeException(L.t("Error while saving results", new Object[0]));
      }
   }

   protected void recomputeClonedData() {
      try {
         DataCloner var1 = new DataCloner();
         DataInfo var2 = DataManager.getDataInfo("History", this.importInfo.symbol);
         ArrayList var3 = DataManager.listCloned(var2.id);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            DataInfo var5 = (DataInfo)var3.get(var4);
            DataManager.clearData(var5.connection, var5.symbol);
            this.listener.setMessage(L.t("Recomputing cloned data", new Object[0]));
            String[] var6 = var5.timezone.split("\\|");
            String var7 = var6[0];
            int var8 = 0;

            try {
               var8 = Integer.parseInt(var6[1]);
            } catch (Exception var10) {
            }

            var1.cloneToTimezone(this.importInfo.symbol, var5.symbol, var7, var8, var5.removeWeekends, null, this.listener);
         }
      } catch (Exception var11) {
         Log.error("Error while recomputing cloned data.", var11);
         throw new RuntimeException(L.t("Error while recomputing cloned data.", new Object[0]));
      }
   }

   protected void onError(String var1, ImportInfo var2) {
      if (this.dataMerger != null) {
         this.dataMerger.skipAllProgress();
      }

      this.listener.onError(var1);
      this.closeFiles(false);
   }

   protected void evaluateSpeed(long var1, long var3, long var5, long var7) {
      if (var5 > 0L && var7 > 0L) {
         double var9 = var5 * 8.0 / 1024.0 / (var7 / 1000.0);
         this.avgDownloadSpeed += var9;
         this.avgDownloadSpeedCounter++;
      }

      long var29 = this.dataMerger.getPerformedDays();
      long var11 = System.currentTimeMillis();
      if (var29 != var3) {
         this.lastPerformed = var29 - var3;
      }

      double var13 = (var11 - var1) / 1000.0;
      double var15 = this.lastPerformed / var13;
      double var17 = Math.round(var15 * 10.0) / 10.0;
      double var19 = this.avgDownloadSpeed / this.avgDownloadSpeedCounter;
      String var21 = "Kbps";
      if (var19 > 1024.0) {
         var21 = "Mbps";
         var19 /= 1024.0;
      }

      double var22 = Math.round(var19 * 10.0) / 10.0;
      String var24 = " (" + var17 + " days/sec) - " + var22 + var21;
      this.listener.setSpeed(var24);
      this.avgDownloadSpeedCounter = 0;
      this.avgDownloadSpeed = 0.0;
      long var25 = this.dataMerger.getRestDays();
      if (var25 > 0L) {
         long var27 = (long)(var25 * var13);
         this.listener.setEstimation(this.evaluateTime(var27));
      }
   }

   private String evaluateTime(long var1) {
      long var3 = var1 / 60L;
      long var5 = var1 / 3600L;
      StringBuilder var7 = new StringBuilder();
      if (var5 > 0L) {
         var7.append(var5 + L.t("hours", new Object[0]) + " ");
         long var8 = var3 - var5 * 60L;
         var7.append(var8 + L.t("mins", new Object[0]));
      } else if (var3 > 0L) {
         var7.append(var3 + L.t("mins", new Object[0]) + " ");
         long var10 = var1 - var3 * 60L;
         var7.append(var10 + L.t("secs", new Object[0]));
      } else {
         var7.append(var1 + L.t("secs", new Object[0]));
      }

      return var7.toString();
   }

   protected void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.canceled) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }

   @Override
   public void cancel() {
      this.canceled = true;
   }

   @Override
   public void pause() {
      this.paused = true;
   }

   @Override
   public void restart() {
      this.paused = false;
   }

   @Override
   public boolean isCanceled() {
      return this.canceled;
   }

   @Override
   public abstract IExchange clone();
}
