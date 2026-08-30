package com.strategyquant.tradinglib.dukascopy.job;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DukasDataManager;
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
import com.strategyquant.lib.app.MainApp;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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

public class DownloadDukascopySymbolJob extends GridJob<Void> {
   private static final int START_HOUR_SUNDAY_FOREX = 19;
   private static final int MAX_M1_DOWNLOAD_PARALLEL = 3;
   private static final int HOUR_CONSTANT = 3600;
   private static final int TIMEOUT = 100;
   public static final String DOWNLOAD_GROUP = "DukascopyDownloadPerformerGroup";
   private static final int HOURS = 24;
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(DownloadDukascopySymbolJob.class);
   private ImportInfo importInfo;
   private DukascopyBinaryMerger dukascopyMerger;
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private List<String> errors = new LinkedList<>();
   private long currentDateTime = -1L;
   private DataManagerProgressListener progressListener;
   private volatile boolean stopped;
   private volatile boolean cdnError;
   private long endDateTime;
   private volatile boolean paused;
   private volatile AtomicInteger downloadCount = new AtomicInteger();
   private DownloadRateCorrector downloadCorrector = new DownloadRateCorrector();
   private SpeedEvaluator speedEvaluator = new SpeedEvaluator();
   private CdnCache cdnCache;
   private ExecuteOptions executeOptions;
   private volatile long downloadStarted = -1L;
   private LinkedList<GridJob<?>> currentDownloadJobs;
   private DownloadMessageHandler failedHandler;
   private String targetTimezone = "Etc/UCT";
   private DataInfo dataInfo;
   private Future<?> preloadFuture;
   private String baseCdnUrl;

   public DownloadDukascopySymbolJob(String var1, String var2, ImportInfo var3) throws Exception {
      super(var2, 0, null);
      this.baseCdnUrl = var1;
      this.importInfo = var3;
      this.executeOptions = new ExecuteOptions();
      this.executeOptions.setMaxRunningCount(6);
      this.currentDateTime = SQTime.correctDayStart(var3.dateFrom);
      this.endDateTime = SQTime.correctDayStart(var3.dateTo);
      long var4 = SQTime.correctDayStart(System.currentTimeMillis());
      if (this.endDateTime >= var4) {
         this.endDateTime = SQTime.addDays(this.endDateTime, -1);
      }

      this.failedHandler = new DownloadMessageHandler() {
         @Override
         public void onDownloadMessage(String var1, String var2x) {
            DownloadDukascopySymbolJob.this.progressListener.setMessage(var1);
            if (var2x != null) {
               DownloadDukascopySymbolJob.this.progressListener.setSpeed(var2x);
            }
         }
      };
      this.dataInfo = DataManager.getDataInfo("History", var3.symbol);
      if (MainApp.v571hfnsHw().a1wUchdumV() && !MainApp.v571hfnsHw().xpoHYYsX() && !DukasDataManager.get().canFreeDownloadFromCdn(this.dataInfo)) {
         var3.useCDN = false;
      }
   }

   private boolean loadCheckSumsForCDN() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
      if (!this.importInfo.useCDN) {
         return false;
      }

      this.progressListener.setMessage(L.t("Loading symbol's checksums", new Object[0]));
      int var1 = this.baseCdnUrl.indexOf("/", 10);
      Log.info("Downloading {} from server: {}", this.importInfo.symbol, this.baseCdnUrl.substring(0, var1));
      String var2 = this.baseCdnUrl + "/" + this.importInfo.origSymbol + "/metadata.dat";
      HttpClient var3 = HttpApacheManager.getInstance().getClient(100);
      HttpGet var4 = new HttpGet(var2);
      HashSet var5 = new HashSet();
      Object var6 = null;

      try {
         HttpResponse var7 = var3.execute(var4);
         int var8 = var7.getStatusLine().getStatusCode();
         if (var8 < 200 || var8 >= 300) {
            Log.error("Error while loading CDN database from url: {}. Got error code: {}", var2, var8);
            return false;
         }

         var7.getEntity().getContent();
         HttpEntity var9 = var7.getEntity();
         InputStream var10 = var9.getContent();
         byte[] var11 = IOUtils.toByteArray(var10);
         String var12 = new String(var11, "UTF-8");
         String[] var13 = var12.split(";");
         if (var13 != null) {
            for (String var17 : var13) {
               var17 = var17.trim();
               if (!var17.isEmpty()) {
                  var5.add(var17);
               }
            }
         }
      } catch (Exception var27) {
         Log.error("Error while loading CDN database from url:" + var2, var27);
         return false;
      } finally {
         var4.releaseConnection();
         if (var6 != null) {
            try {
               var6.close();
            } catch (IOException var26) {
            }
         }
      }

      this.cdnCache = new CdnCache(this.getJobId(), this.baseCdnUrl, this.importInfo.origSymbol, var5, this.currentDateTime);
      return !var5.isEmpty();
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
      String var1 = this.importInfo.downloadM1Data ? "M1" : "TICK";
      Pair var2 = DukascopyUtils.prepareFiles(this.importInfo, var1);
      this.reader = (DataBinReaderNew)var2.getA();
      this.writer = (DataBinWriterNew)var2.getB();
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

      if (this.importInfo.useCDN) {
         int var9 = 0;

         for (int var11 = this.importInfo.downloadM1Data ? 3 : 1; this.currentDateTime <= this.endDateTime && var9 < var11; var9++) {
            String var13 = this.getJobId() + "_" + var9 + "_cdn";
            int var14 = SQTime.getFullYear(this.currentDateTime);
            int var6 = SQTime.getMonth(this.currentDateTime) + 1;
            int var7 = SQTime.getDay(this.currentDateTime);
            var1.add(new CdnDownloadJob(var13, this.cdnCache, var14, var6, var7, 0, this.failedHandler));
            this.dukascopyMerger.register(var9, this.currentDateTime);
            this.currentDateTime = SQTime.addDays(this.currentDateTime, 1);
         }

         this.dukascopyMerger.reset(var9);
         return var1;
      } else if (this.importInfo.downloadM1Data) {
         int var8;
         for (var8 = 0; this.currentDateTime <= this.endDateTime && var8 < 3; var8++) {
            String var10 = DukascopyUtils.generateM1DukascopyUrl(this.currentDateTime, this.importInfo.origSymbol);
            String var12 = this.getJobId() + "_" + var8;
            var1.add(this.createDukascopyDownloadJob(var10, var12));
            this.dukascopyMerger.register(var8, this.currentDateTime);
            this.currentDateTime = SQTime.addDays(this.currentDateTime, 1);
         }

         this.dukascopyMerger.reset(var8);
         return var1;
      } else {
         byte var2 = 0;
         if (this.importInfo.ignoreWeekend && SQTime.getDayOfWeek(this.currentDateTime) == 7) {
            var2 = 19;
            this.currentDateTime = SQTime.setHour(this.currentDateTime, var2);
         }

         for (int var3 = var2; var3 < 24; var3++) {
            String var4 = DukascopyUtils.generateTickDukascopyUrl(this.currentDateTime, this.importInfo.origSymbol);
            String var5 = this.getJobId() + "_" + var3;
            this.dukascopyMerger.register(var3, this.currentDateTime);
            var1.add(this.createDukascopyDownloadJob(var4, var5));
            this.currentDateTime = SQTime.addHours(this.currentDateTime, 1);
         }

         this.dukascopyMerger.resetWithBatchAndStart(var2 - 1, 24);
         return var1;
      }
   }

   private GridJob<DownloadResultDto> createDukascopyDownloadJob(String var1, String var2) {
      HashMap var3 = new HashMap();
      var3.put("url", var1);
      return new DownloadWithFlexibileSpeedJob(var2, 0, var3, this.downloadCorrector);
   }

   private List<GridJob<DownloadResultDto>> generateDownloadJobs(List<String> var1) {
      LinkedList var2 = new LinkedList();

      for (String var4 : var1) {
         long var5 = this.dukascopyMerger.getDateForJob(var4);
         String var7 = this.importInfo.downloadM1Data
            ? DukascopyUtils.generateM1DukascopyUrl(var5, this.importInfo.origSymbol)
            : DukascopyUtils.generateTickDukascopyUrl(var5, this.importInfo.origSymbol);
         var2.add(this.createDukascopyDownloadJob(var7, var4));
      }

      return var2;
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
            this.downloadCorrector.stop();
            if (this.cdnCache != null) {
               this.cdnCache.stop();
            }

            if (this.preloadFuture != null) {
               this.preloadFuture.cancel(true);
            }

            synchronized (this) {
               if (this.currentDownloadJobs != null) {
                  for (GridJob var4 : this.currentDownloadJobs) {
                     var4.messageReceived(var1);
                  }
               }
            }
      }
   }

   public Void call() throws Exception {
      if (this.currentDateTime >= this.endDateTime) {
         this.progressListener.onProgress(100.0);
         this.progressListener.setMessage(L.t("Completed", new Object[0]));
         return null;
      }

      try {
         this.progressListener.onProgress(0.1);
         this.progressListener.setMessage(L.t("Waiting in queue", new Object[0]));
         Log.debug("Starting download job for symbol: {}", this.importInfo.symbol);
         int var1 = this.importInfo.downloadM1Data ? 3 : 24;
         this.dukascopyMerger = new DukascopyBinaryMerger("Writing ", this.getJobId(), var1, this.importInfo);
         this.dukascopyMerger.setProgressListener(this.progressListener);
         this.dukascopyMerger.setIgnoreWeekend(this.importInfo.ignoreWeekend);
         this.handleBroker();
         if (this.stopped) {
            this.dukascopyMerger.setFinishProgress(L.t("Stopped", new Object[0]));
            this.sendRefreshMessage();
            return null;
         }

         boolean var2 = this.loadCheckSumsForCDN();
         if (!var2) {
            this.importInfo.useCDN = false;
         }

         this.progressListener.setDownloadType(var2 ? L.t("Fast download", new Object[0]) : L.t("Normal download", new Object[0]));
         this.progressListener.setDataType(this.importInfo.downloadM1Data ? L.t("M1 data", new Object[0]) : L.t("Tick data", new Object[0]));
         this.prepareFiles();
         this.dukascopyMerger.setWriter(this.writer);
         this.dukascopyMerger.setReader(this.reader);
         this.progressListener.setMessage(L.t("Downloading...", new Object[0]));
         Log.debug("Starting download");
         if (this.cdnCache != null) {
            ExecutorService var3 = Executors.newSingleThreadExecutor();

            try {
               this.preloadFuture = var3.submit(() -> this.cdnCache.preload(this.currentDateTime, this.endDateTime, this.failedHandler));
               this.preloadFuture.get();
            } catch (CancellationException var16) {
            }

            this.preloadFuture = null;
         }

         this.downloadStarted = System.currentTimeMillis();

         while (!this.stopped && !this.cdnError) {
            long var19 = this.dukascopyMerger.getPerformedDays();
            long var5 = System.currentTimeMillis();
            List var7 = this.generateDownloadJobs();
            Log.debug("Generating downloads jobs: {}", var7.size());
            if (var7.isEmpty()) {
               break;
            }

            if (this.downloadStarted == -1L) {
               this.downloadStarted = System.currentTimeMillis();
            }

            synchronized (this) {
               this.currentDownloadJobs = new LinkedList<>(var7);
            }

            this.executeJobsAndWait(var7);
            if (this.stopped) {
               break;
            }

            Log.debug("Download finished. Errors: {}", this.errors.size());
            this.downloadMissing();
            this.checkPaused();
            this.errors.clear();
            this.evaluateSpeed(var5, var19);
            if (this.importInfo.downloadM1Data && this.reader != null) {
               this.dukascopyMerger.readTill(this.currentDateTime, false);
            }

            if (this.dukascopyMerger.isSaveError()) {
               this.onError(L.t("Error while saving data", new Object[0]));
               break;
            }
         }

         this.finish();
         this.closeFiles(true);
         this.recomputeClonedData();
         if (this.cdnError) {
            this.onError(L.t("Download problem - check your internet connection", new Object[0]));
         } else {
            long var20 = (System.currentTimeMillis() - this.downloadStarted) / 1000L;
            String var21 = this.stopped ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]);
            this.dukascopyMerger.setFinishProgress(var21 + " in " + this.evaluateTime(var20));
         }

         this.sendRefreshMessage();
      } catch (Exception var17) {
         Log.error("Error while performing dukascopy import", var17);
         this.onError(L.t("Error while performing dukascopy import", new Object[0]));
      } finally {
         if (this.cdnCache != null) {
            this.cdnCache.clean();
         }

         DataManagerDataProgress.get().removeListener(this.importInfo.symbol);
         Log.debug("Download job for symbol: {} finished", this.importInfo.symbol);
      }

      return null;
   }

   private void handleBroker() {
      if (this.dataInfo.brokerId != -1) {
         BrokerDto var1 = BrokerManager.getInstance().getBroker(this.dataInfo.brokerId);
         String var2 = var1.getMtTimezone();
         this.targetTimezone = var2;
         this.dukascopyMerger.setTargetTimezone(this.targetTimezone);
      }
   }

   private void evaluateSpeed(long var1, long var3) {
      long var5 = this.dukascopyMerger.getPerformedDays();
      long var7 = System.currentTimeMillis();
      double var9 = (var7 - this.downloadStarted) / 1000.0;
      double var11 = var5 / var9;
      double var13 = Math.round(var11 * 10.0) / 10.0;
      String var15 = " (" + var13 + " " + L.t("days/sec", new Object[0]) + ") - " + this.speedEvaluator.getSpeed();
      this.progressListener.setSpeed(var15);
      this.speedEvaluator.reset();
      long var16 = this.dukascopyMerger.getRestDays();
      if (var16 > 0L) {
         double var18 = var9 / var5;
         long var20 = (long)(var16 * var18);
         this.progressListener.setEstimation(this.evaluateTime(var20));
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
         this.onError(L.t("Error while recomputing cloned data.", new Object[0]));
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
         if (this.stopped) {
            return;
         }

         boolean var5 = var3.endsWith("_cdn");
         if (var5) {
            boolean var6 = var4 != null && var4.isChecksumFailed();
            if (!var6) {
               this.handleCdnRecords(var3, var4, var2);
            } else {
               this.cdnError = var6;
            }
         } else {
            this.handleBi5Data(var4, var3, var2);
         }
      } finally {
         this.downloadCount.decrementAndGet();
      }
   }

   private void handleBi5Data(DownloadResultDto var1, String var2, boolean var3) {
      try {
         if (!var3) {
            String var4 = this.dukascopyMerger.getDateDesc(this.dukascopyMerger.getDateForJob(var2));
            Log.debug("Parsing for date:{} failed", var4);
         }

         this.dukascopyMerger.handleBi5Data(var1, var2, !this.importInfo.downloadM1Data, var3);
      } catch (Exception var5) {
         Log.error("Error while handling data", var5);
         var3 = false;
      }
   }

   private boolean performCdnData(String var1, int var2, long var3, String var5) throws Exception {
      DataBinReaderNew var6 = DataBinReaderNew.getInstance(this.importInfo.downloadM1Data ? 1 : 2, null);
      var6.setFileName(var5);

      try {
         var6.openFile();

         boolean var7;
         for (var7 = false; var6.readData(); var7 = true) {
            VersatileData var8 = new VersatileData();
            var8.time = var6.tickData.time;
            var8.volume = var6.tickData.volume * 1000000.0;
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
         this.dukascopyMerger.resetQueue(var2);
         return false;
      } finally {
         if (var5 != null) {
            var6.closeFile();
         }
      }
   }

   private void handleCdnRecords(String var1, DownloadResultDto var2, boolean var3) {
      String var4 = var1.substring(0, var1.length() - 4);
      Integer var5 = this.dukascopyMerger.getJobsIndex(var4);
      long var6 = this.dukascopyMerger.getDateForJob(var4);

      try {
         if (var3) {
            var3 = this.performCdnData(var1, var5, var6, var2.getFilename());
         } else {
            var3 = false;
            this.dukascopyMerger.showError(var6, "Download failed. Error code: " + var2.getHttpCode());
         }
      } catch (Exception var10) {
         Log.error("Error while handling response", var10);
         var3 = false;
      }

      if (!this.dukascopyMerger.isSaveError()) {
         if (!var3) {
            if (this.importInfo.downloadM1Data) {
               this.errors.add(this.getJobId() + "_" + var5);
               this.dukascopyMerger.register(var5, var6);
            } else {
               byte var8 = 0;
               if (this.importInfo.ignoreWeekend && SQTime.getDayOfWeek(var6) == 7) {
                  var8 = 19;
               }

               for (int var9 = var8; var9 < 24; var9++) {
                  this.errors.add(this.getJobId() + "_" + var9);
                  this.dukascopyMerger.register(var9, SQTime.addHours(var6, var9));
               }

               this.dukascopyMerger.resetWithBatchAndStart(var8 - 1, 24);
            }
         }
      }
   }

   private void executeJobsAndWait(List<GridJob<DownloadResultDto>> var1) throws Exception {
      if (!var1.isEmpty()) {
         GridClient var2 = SQGrid.getGridClient();
         this.downloadCount.set(var1.size());
         var2.executeOnGrid("DukascopyDownloadPerformerGroup", var1, this.executeOptions);
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

   private void downloadMissing() throws Exception {
      if (!this.errors.isEmpty()) {
         List var1 = this.generateDownloadJobs(this.errors);
         this.errors.clear();
         this.executeJobsAndWait(var1);
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
            String var5 = this.importInfo.downloadM1Data ? "M1" : "TICK";
            long var6;
            if (var5.equals("TICK")) {
               var6 = this.dukascopyMerger.getSavedSecs();
            } else {
               var6 = this.dukascopyMerger.getSavedRows() * 60;
            }

            if (this.importInfo.rows == 0) {
               int var10003 = this.importInfo.downloadM1Data ? 1 : 1;
               QDM.getInstance().symbols.add(this.importInfo.origSymbol, 1, var10003, this.importInfo.useCDN, SQTime.getYearsBetween(var1, var3));
            }

            DataManager.updateData("History", this.importInfo.symbol, var1, var3, this.dukascopyMerger.getSavedRows(), var6, 1, var5, this.targetTimezone);
         }
      } catch (Exception var8) {
         Log.error("Error while saving results", var8);
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

      String var2 = this.importInfo.downloadM1Data ? "M1" : "TICK";
      if (var1) {
         DukascopyUtils.deleteCopyFile(this.importInfo.symbol, var2);
      } else {
         DukascopyUtils.revertCopyFile(this.importInfo.symbol, var2);
      }
   }
}
