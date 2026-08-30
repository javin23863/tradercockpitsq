package com.strategyquant.tradinglib.darwinex;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.utils.Pair;
import com.strategyquant.qdm.QDM;
import com.strategyquant.tradinglib.data.download.SpeedEvaluator;
import com.strategyquant.tradinglib.dukascopy.CdnCache;
import com.strategyquant.tradinglib.dukascopy.CdnDownloadJob;
import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import com.strategyquant.tradinglib.dukascopy.DukascopyBinaryMerger;
import com.strategyquant.tradinglib.dukascopy.DukascopyUtils;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.historyData.DownloadMessageHandler;
import com.strategyquant.tradinglib.historyData.HttpApacheManager;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexDownloadJob extends GridJob<Void> {
   private static final int HOUR_CONSTANT = 3600;
   private static final int MAX_M1_DOWNLOAD_PARALLEL = 3;
   public static final String DOWNLOAD_GROUP = "DarwinexDownloadPerformerGroup";
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(DarwinexDownloadJob.class);
   private ImportInfo importInfo;
   private DukascopyBinaryMerger dukascopyMerger;
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private long currentDateTime = -1L;
   private DataManagerProgressListener progressListener;
   private volatile boolean stopped;
   private volatile boolean cdnError;
   private long endDateTime;
   private volatile boolean paused;
   private volatile AtomicInteger downloadCount = new AtomicInteger();
   private long lastPerformed = 0L;
   private CdnCache cdnCache;
   private ExecuteOptions executeOptions;
   private SpeedEvaluator speedEvaluator = new SpeedEvaluator();
   private DownloadMessageHandler downloadMessageHandler;
   private String targetTimezone = "Etc/UCT";
   private DataInfo dataInfo;
   private Future<?> preloadFuture;
   private String cdnUrl;

   public DarwinexDownloadJob(String var1, String var2, ImportInfo var3) throws Exception {
      super(var2, 0, null);
      this.cdnUrl = var1;
      this.importInfo = var3;
      this.executeOptions = new ExecuteOptions();
      this.executeOptions.setMaxRunningCount(6);
      this.currentDateTime = SQTime.correctDayStart(var3.dateFrom);
      this.endDateTime = SQTime.correctDayStart(var3.dateTo);
      long var4 = SQTime.correctDayStart(System.currentTimeMillis());
      if (this.endDateTime >= var4) {
         this.endDateTime = SQTime.addDays(this.endDateTime, -1);
      }

      this.downloadMessageHandler = new DownloadMessageHandler() {
         @Override
         public void onDownloadMessage(String var1, String var2x) {
            DarwinexDownloadJob.this.progressListener.setMessage(var1);
            if (var2x != null) {
               DarwinexDownloadJob.this.progressListener.setSpeed(var2x);
            }
         }
      };
      this.dataInfo = DataManager.getDataInfo("History", var3.symbol);
   }

   private void handleBroker() {
      if (this.dataInfo.brokerId != -1) {
         BrokerDto var1 = BrokerManager.getInstance().getBroker(this.dataInfo.brokerId);
         String var2 = var1.getMtTimezone();
         this.targetTimezone = var2;
         this.dukascopyMerger.setTargetTimezone(this.targetTimezone);
      }
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.progressListener.onPause();

         while (this.paused && !this.stopped) {
            Thread.sleep(100L);
         }

         this.progressListener.onContinue();
      }
   }

   private void prepareFiles() throws Exception {
      Log.debug("Preparing data files");
      this.progressListener.setMessage(L.t("Preparing data files", new Object[0]));
      Pair var1 = DukascopyUtils.prepareFiles(this.importInfo, "TICK");
      this.reader = (DataBinReaderNew)var1.getA();
      this.writer = (DataBinWriterNew)var1.getB();
   }

   public void setProgressListener(DataManagerProgressListener var1) {
      this.progressListener = var1;
   }

   private List<GridJob<DownloadResultDto>> generateDownloadJobs() throws Exception {
      LinkedList var1 = new LinkedList();
      if (this.currentDateTime > this.endDateTime) {
         return var1;
      }

      this.currentDateTime = this.dukascopyMerger.checkDate(this.currentDateTime);
      if (this.currentDateTime > this.endDateTime) {
         return var1;
      }

      int var2 = 0;

      for (int var3 = this.importInfo.downloadM1Data ? 3 : 1; this.currentDateTime <= this.endDateTime && var2 < var3; var2++) {
         String var4 = this.getJobId() + "_" + var2 + "_cdn";
         int var5 = SQTime.getFullYear(this.currentDateTime);
         int var6 = SQTime.getMonth(this.currentDateTime) + 1;
         int var7 = SQTime.getDay(this.currentDateTime);
         var1.add(new CdnDownloadJob(var4, this.cdnCache, var5, var6, var7, 0, this.downloadMessageHandler));
         this.dukascopyMerger.register(var2, this.currentDateTime);
         this.currentDateTime = SQTime.addDays(this.currentDateTime, 1);
      }

      this.dukascopyMerger.reset(var2);
      return var1;
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
            this.progressListener.setMessage(L.t("Stopping download - please wait", new Object[0]));
            this.cdnCache.stop();
            if (this.preloadFuture != null) {
               this.preloadFuture.cancel(true);
            }
      }
   }

   private void loadCheckSumsForCDN() throws Exception {
      this.progressListener.setMessage(L.t("Loading symbol's checksums", new Object[0]));
      String var1 = this.cdnUrl;
      int var2 = var1.indexOf("/", 10);
      Log.info("Downloading {} from server: {}", this.importInfo.origSymbol, var1.substring(0, var2));
      String var3 = var1 + "/" + this.importInfo.origSymbol + "/metadata.dat";
      HttpClient var4 = HttpApacheManager.getInstance().getClient();
      HttpGet var5 = new HttpGet(var3);
      int var6 = -1;
      int var7 = -1;
      HashSet var8 = new HashSet();
      Object var9 = null;

      try {
         HttpResponse var10 = var4.execute(var5);
         var10.getEntity().getContent();
         HttpEntity var11 = var10.getEntity();
         InputStream var12 = var11.getContent();
         byte[] var13 = IOUtils.toByteArray(var12);
         String var14 = new String(var13, "UTF-8");
         String[] var15 = var14.split(";");
         if (var15 != null) {
            for (String var19 : var15) {
               var19 = var19.trim();
               if (!var19.isEmpty()) {
                  var8.add(var19);

                  try {
                     String var20 = var19.substring(0, var19.length() - 4);
                     Integer var21 = Integer.valueOf(var20);
                     if (var6 == -1 || var21 < var6) {
                        var6 = var21;
                     }

                     if (var21 > var7) {
                        var7 = var21;
                     }
                  } catch (Exception var30) {
                  }
               }
            }
         }
      } catch (Exception var31) {
         Log.error("Error while loading CDN database from url:" + var3, var31);
         throw var31;
      } finally {
         var5.releaseConnection();
         if (var9 != null) {
            try {
               var9.close();
            } catch (IOException var29) {
            }
         }
      }

      int var33 = SQTime.getFullYear(this.currentDateTime);
      if (var33 < var6) {
         this.currentDateTime = SQTime.toLong(var6, 1, 1, 0, 0, 0);
      }

      int var34 = SQTime.getFullYear(this.endDateTime);
      if (var7 < var34) {
         this.endDateTime = SQTime.toLong(var7, 12, 31, 23, 59, 0);
      }

      this.cdnCache = new CdnCache(this.getJobId(), var1, this.importInfo.origSymbol, var8, this.currentDateTime);
   }

   public Void call() throws Exception {
      try {
         this.progressListener.onProgress(0.1);
         this.progressListener.setMessage(L.t("Waiting in queue", new Object[0]));
         Log.debug("Starting download job for symbol:" + this.importInfo.symbol);
         this.dukascopyMerger = new DukascopyBinaryMerger("Writing ", this.getJobId(), 1, this.importInfo);
         this.dukascopyMerger.setProgressListener(this.progressListener);
         this.dukascopyMerger.setIgnoreWeekend(this.importInfo.ignoreWeekend);
         this.handleBroker();
         if (this.stopped) {
            this.dukascopyMerger.setFinishProgress("Stopped");
            this.sendRefreshMessage();
            return null;
         }

         this.loadCheckSumsForCDN();
         this.progressListener.setDownloadType(L.t("Fast download", new Object[0]));
         this.progressListener.setDataType(L.t("Tick data", new Object[0]));
         this.prepareFiles();
         this.dukascopyMerger.setWriter(this.writer);
         this.dukascopyMerger.setReader(this.reader);
         this.progressListener.setMessage(L.t("Downloading...", new Object[0]));
         Log.debug("Starting download");
         if (this.cdnCache != null) {
            ExecutorService var1 = Executors.newSingleThreadExecutor();

            try {
               this.preloadFuture = var1.submit(() -> this.cdnCache.preload(this.currentDateTime, this.endDateTime, this.downloadMessageHandler));
               this.preloadFuture.get();
            } catch (CancellationException var12) {
            }

            this.preloadFuture = null;
         }

         long var15 = System.currentTimeMillis();

         while (!this.stopped && !this.cdnError) {
            long var3 = this.dukascopyMerger.getPerformedDays();
            long var5 = System.currentTimeMillis();
            List var7 = this.generateDownloadJobs();
            Log.debug("Generating downloads jobs:" + var7.size());
            if (var7.isEmpty()) {
               break;
            }

            this.executeJobsAndWait(var7);
            if (this.stopped) {
               break;
            }

            this.checkPaused();
            this.evaluateSpeed(var5, var3);
            if (this.importInfo.downloadM1Data && this.reader != null) {
               this.dukascopyMerger.readTill(this.currentDateTime, false);
            }
         }

         this.finish();
         this.closeFiles(true);
         this.recomputeClonedData();
         if (this.cdnError) {
            this.onError("Download problem - check your internet connection");
         } else {
            long var16 = (System.currentTimeMillis() - var15) / 1000L;
            String var17 = this.stopped ? "Stopped" : "Completed";
            this.dukascopyMerger.setFinishProgress(var17 + " in " + this.evaluateTime(var16));
         }

         this.sendRefreshMessage();
      } catch (Exception var13) {
         Log.error("Error while performing darwinex download", var13);
         this.onError("Error while performing darwinex download");
      } finally {
         if (this.cdnCache != null) {
            this.cdnCache.clean();
         }

         DataManagerDataProgress.get().removeListener(this.importInfo.symbol);
         Log.debug("Download job for symbol:" + this.importInfo.symbol + " finished");
      }

      return null;
   }

   private void evaluateSpeed(long var1, long var3) {
      long var5 = this.dukascopyMerger.getPerformedDays();
      long var7 = System.currentTimeMillis();
      if (var5 != var3) {
         this.lastPerformed = var5 - var3;
      }

      double var9 = (var7 - var1) / 1000.0;
      double var11 = this.lastPerformed / var9;
      double var13 = Math.round(var11 * 10.0) / 10.0;
      String var15 = " (" + var13 + " days/sec) - " + this.speedEvaluator.getSpeed();
      this.progressListener.setSpeed(var15);
      this.speedEvaluator.reset();
      long var16 = this.dukascopyMerger.getRestDays();
      if (var16 > 0L) {
         long var18 = (long)(var16 * var9);
         this.progressListener.setEstimation(this.evaluateTime(var18));
      }
   }

   private String evaluateTime(long var1) {
      long var3 = var1 / 60L;
      long var5 = var1 / 3600L;
      StringBuilder var7 = new StringBuilder();
      if (var5 > 0L) {
         var7.append(var5 + "hours ");
         long var8 = var3 - var5 * 60L;
         var7.append(var8 + "mins");
      } else if (var3 > 0L) {
         var7.append(var3 + "mins ");
         long var10 = var1 - var3 * 60L;
         var7.append(var10 + "secs");
      } else {
         var7.append(var1 + "secs");
      }

      return var7.toString();
   }

   private void recomputeClonedData() {
      try {
         DataCloner var1 = new DataCloner();
         DataInfo var2 = DataManager.getDataInfo("History", this.importInfo.symbol);
         ArrayList var3 = DataManager.listCloned(var2.id);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            DataInfo var5 = (DataInfo)var3.get(var4);
            DataManager.clearData(var5.connection, var5.symbol);
            this.progressListener.setMessage(L.t("Recomputing cloned data", new Object[0]));
            String[] var6 = var5.timezone.split("\\|");
            String var7 = var6[0];
            int var8 = 0;

            try {
               var8 = Integer.parseInt(var6[1]);
            } catch (Exception var10) {
            }

            var1.cloneToTimezone(this.importInfo.symbol, var5.symbol, var7, var8, var5.removeWeekends, null, this.progressListener);
         }
      } catch (Exception var11) {
         Log.error("Error while recomputing cloned data.", var11);
         this.onError("Error while recomputing cloned data.");
      }
   }

   private void onError(String var1) {
      if (this.dukascopyMerger != null) {
         this.dukascopyMerger.skipAllProgress();
      }

      this.progressListener.onError(var1);
      this.closeFiles(false);
   }

   private void sendRefreshMessage() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.importInfo.symbol, "download"), "SQUANT", "QDM", "AlgoWizard");
      } catch (Exception var2) {
         Log.error("Cannot send data Websocket message. ", var2);
      }
   }

   public synchronized void handleDownloadedMessage(GridMessage var1) {
      try {
         boolean var2 = var1.getJobDetails().isSuccess();
         String var3 = var1.getJobDetails().getJobID();
         DownloadResultDto var4 = (DownloadResultDto)var1.getData();
         this.speedEvaluator.evalSpeed(var4);
         if (!this.stopped) {
            this.handleCdnRecords(var3, var4, var2);
            return;
         }
      } finally {
         this.downloadCount.decrementAndGet();
      }
   }

   private boolean performCdnData(String var1, int var2, long var3, String var5) throws Exception {
      DataBinReaderNew var6 = DataBinReaderNew.getInstance(2, null);
      var6.setFileName(var5);
      var6.openFile();

      try {
         boolean var7;
         for (var7 = false; var6.readData(); var7 = true) {
            VersatileData var8 = new VersatileData();
            var8.time = var6.tickData.time;
            var8.volume = var6.tickData.volume;
            var8.ask = var6.tickData.ask;
            var8.bid = var6.tickData.bid;
            var8.open = var6.tickData.open;
            var8.low = var6.tickData.low;
            var8.high = var6.tickData.high;
            var8.close = var6.tickData.close;
            this.dukascopyMerger.storeToQueue(var2, var8);
         }

         if (!var7) {
            this.dukascopyMerger.clearQueue(var2);
         }

         this.dukascopyMerger.tryToSaveQueue(var3);
         return true;
      } catch (Exception var12) {
         this.dukascopyMerger.clearQueue(var2);
         return false;
      } finally {
         var6.closeFile();
      }
   }

   private void handleCdnRecords(String var1, DownloadResultDto var2, boolean var3) {
      String var4 = var1.substring(0, var1.length() - 4);
      Integer var5 = this.dukascopyMerger.getJobsIndex(var4);
      long var6 = this.dukascopyMerger.getDateForJob(var4);

      try {
         if (var3 && var2.getFilename() != null) {
            var3 = this.performCdnData(var1, var5, var6, var2.getFilename());
         } else {
            if (!var3) {
               this.dukascopyMerger.showError(var6, "Download failed. Error code: " + var2.getHttpCode());
            }

            this.dukascopyMerger.updateProgress("No data for date: " + SQTime.toString(var6, "yyyy.MM.dd"), false);
            var3 = false;
         }
      } catch (Exception var9) {
         Log.error("Error while handling response", var9);
         var3 = false;
      }
   }

   private void executeJobsAndWait(List<GridJob<DownloadResultDto>> var1) throws Exception {
      if (!var1.isEmpty()) {
         GridClient var2 = SQGrid.getGridClient();
         this.downloadCount.set(var1.size());
         var2.executeOnGrid("DarwinexDownloadPerformerGroup", var1, this.executeOptions);
         this.waitForFinish();
      }
   }

   private void waitForFinish() {
      while (this.downloadCount.get() != 0 && !this.stopped) {
         try {
            Thread.sleep(200L);
         } catch (InterruptedException var2) {
         }
      }
   }

   private void finish() {
      try {
         if (this.stopped) {
            this.dukascopyMerger.setStopped(this.stopped);
         }

         this.dukascopyMerger.finish();
         long var1 = this.dukascopyMerger.getFromDate();
         long var3 = this.dukascopyMerger.getToDate();
         if (var1 != -1L && var3 != -1L) {
            long var5 = this.dukascopyMerger.getSavedSecs();
            if (this.importInfo.rows == 0) {
               int var10003 = this.importInfo.downloadM1Data ? 1 : 1;
               QDM.getInstance().symbols.add(this.importInfo.origSymbol, 1, var10003, this.importInfo.useCDN, SQTime.getYearsBetween(var1, var3));
            }

            DataManager.updateData("History", this.importInfo.symbol, var1, var3, this.dukascopyMerger.getSavedRows(), var5, 1, "TICK", this.targetTimezone);
         }
      } catch (Exception var7) {
         Log.error("Error while saving results", var7);
         throw new RuntimeException(L.t("Error while saving results", new Object[0]));
      }
   }

   private void closeFiles(boolean var1) {
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

      if (var1) {
         DukascopyUtils.deleteCopyFile(this.importInfo.symbol, "TICK");
      } else {
         DukascopyUtils.revertCopyFile(this.importInfo.symbol, "TICK");
      }
   }
}
