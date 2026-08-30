package com.strategyquant.tradinglib.historyData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.ExchangeTimezone;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.exception.SQException;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadEodStockHistorySymbolJob extends GridJob<Void> {
   public static final String DOWNLOAD_GROUP = "HistoryDownloadPerformerGroup";
   public static final String DATE_TIME_FORMAT = "yyyy.MM.dd";
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(DownloadEodStockHistorySymbolJob.class);
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private DataManagerProgressListener progressListener;
   private volatile boolean stopped;
   private volatile boolean paused;
   private ExecuteOptions executeOptions;
   private String timeframe;
   private String symbol;
   private TickerKind kind;
   private TickerDto ticker;
   private long downloadFromDate;
   private long fromDate;
   private long toDate;
   private long lastSavedDate;
   private int totalRecords;
   private EodHistoryDataCache dataCache;
   private int totalDays;
   private int performedDays;
   private List<VersatileData> datas = new LinkedList<>();
   private boolean minutesShift;
   private int bartimeType;
   private DataInfo dataInfo;
   private int shiftHours = 0;
   private DateTimeZone sourceTz;
   private DateTimeZone targetTz;
   private boolean freeSymbol;
   private long timeShift;
   private long lastestDateFromFile;
   private String tickerForDownload;
   private TickerDto aliasTicker;
   private boolean forceDownload;

   public DownloadEodStockHistorySymbolJob(String var1, DataInfo var2, TickerKind var3, TickerDto var4, boolean var5) throws Exception {
      super(var1, 0, null);
      this.forceDownload = var5;
      this.executeOptions = new ExecuteOptions();
      this.executeOptions.setMaxRunningCount(6);
      this.dataInfo = var2;
      this.kind = var3;
      this.symbol = var2.symbol;
      this.timeframe = var2.timeframe;
      this.bartimeType = var2.barTimeType;
      this.minutesShift = var2.barTimeType == 2 && this.timeframe.equals("M1");
      this.ticker = var4;
      this.tickerForDownload = var4.getTicker();
      Long var6 = var4.getAliasTickerId();
      if (var6 != null) {
         this.aliasTicker = var3 == TickerKind.STOCK ? HistoryDataManager.get().getStockTicker(var6) : HistoryDataManager.get().getFutureTicker(var6);
         if (this.aliasTicker != null) {
            this.tickerForDownload = this.aliasTicker.getTicker();
         }
      }

      this.freeSymbol = HistoryDataSubscription.getInstance().isFreeSymbol(var2.uSymbol, var3 == TickerKind.FUTURES, this.timeframe.equals("D1"));

      String var7;
      try {
         var7 = ExchangeTimezone.get().getTimezone(var4.getMarketName());
      } catch (NullPointerException var11) {
         var7 = ExchangeTimezone.get().getTimezone(var4.getMarketName());
      }

      String var8 = var2.timezone;
      String var9 = null;
      if (var8 != null && !var8.startsWith("Exchange")) {
         var9 = var2.timezone;
      } else {
         var9 = var7;
         if (var8 != null && var8.length() > 8) {
            String var10 = var8.substring(8).trim();
            this.shiftHours = Integer.valueOf(var10);
         }
      }

      if (var9.equals("EETUS")) {
         var9 = "America/New_York";
         this.shiftHours = 7;
      }

      this.sourceTz = DateTimeZone.forID(Timezone.parseId(var7));
      this.targetTz = DateTimeZone.forID(Timezone.parseId(var9));
      if (HistoryDataManager.get().shouldSetFixedTimeForEod(var4, var2.barTimeType)) {
         this.timeShift = HistoryDataManager.get().getEodShiftDate(var4);
      } else if (HistoryDataManager.get().shouldShiftHoursForM1(var4)) {
         this.timeShift = HistoryDataManager.get().getM1ShiftDate(var4);
      }
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.progressListener.onPause();

         while (this.paused && !this.stopped) {
            Thread.sleep(500L);
         }

         this.progressListener.onContinue();
      }
   }

   private void prepareFiles() throws Exception {
      Log.debug("Preparing data files");
      String var1 = DataManager.getDataFileName("History", this.symbol, this.timeframe, "No Session");
      String var2 = var1 + ".copy";
      File var3 = new File(var1);
      File var4 = new File(var2);
      if (var3.exists()) {
         if (var3.length() == 0L) {
            var3.delete();
         } else {
            try {
               Files.copy(var3.toPath(), var4.toPath(), StandardCopyOption.REPLACE_EXISTING);
               this.reader = DataBinReaderNew.getInstance(1, this.dataInfo.symbolInfo);
               this.reader.setFileName(var2);
               this.reader.openFile();
            } catch (Exception var7) {
               Log.error("Cannot make a copy of existing symbol data file. ", var7);
               this.reader = null;
            }
         }
      }

      try {
         this.writer = this.freeSymbol
            ? DataBinWriterNew.getInstance(1, MainApp.getDataPath(), this.dataInfo.symbolInfo)
            : DataBinWriterNew.getCryptedInstance(1, MainApp.getDataPath(), this.dataInfo.symbolInfo);
         this.writer.setFileName(var1);
         this.writer.open();
      } catch (Exception var6) {
         Log.error("Cannot open symbol data file for writing. ", var6);
         throw new Exception(L.t("Error - Cannot open symbol data file for writing.", new Object[0]));
      }
   }

   public void setProgressListener(DataManagerProgressListener var1) {
      this.progressListener = var1;
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
            this.dataCache.stop();
            this.progressListener.setMessage(L.t("Stopping download - please wait", new Object[0]));
      }
   }

   private void correctTickData(VersatileData var1) {
      if (var1.open == 0.0) {
         var1.open = this.getNonZero(var1);
      }

      if (var1.high == 0.0) {
         var1.high = this.getNonZero(var1);
      }

      if (var1.low == 0.0) {
         var1.low = this.getNonZero(var1);
      }

      if (var1.close == 0.0) {
         var1.close = this.getNonZero(var1);
      }
   }

   private double getNonZero(VersatileData var1) {
      if (var1.open != 0.0) {
         return var1.open;
      } else if (var1.high != 0.0) {
         return var1.high;
      } else {
         return var1.low != 0.0 ? var1.low : var1.close;
      }
   }

   public Void call() throws Exception {
      String var1 = MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN);
      Log.debug("Downloading symbol: {}, from: {}", this.dataInfo.symbol, var1);
      long var2 = this.dataInfo.dateTo;
      long var4 = this.aliasTicker != null ? this.aliasTicker.getDateTo().getTime() : this.ticker.getDateTo().getTime();
      if (var4 <= var2) {
         this.updateProgress(L.t("Completed", new Object[0]), true, true);
         return null;
      }

      try {
         this.progressListener.onProgress(0.2);
         this.progressListener.setMessage(L.t("Preparing download", new Object[0]));
         Log.debug("Starting: {}", Thread.currentThread().getName());
         if (this.stopped) {
            this.setFinishProgress(L.t("Stopped", new Object[0]));
            return null;
         }

         this.prepareFiles();
         this.evalDates();
         String var6 = this.ticker.getName() != null ? this.ticker.getName() : this.ticker.getTicker();
         boolean var7 = !var6.equals(this.dataInfo.uSymbolName);
         if (this.fromDate == -1L) {
            throw new RuntimeException(L.t("Data for this timeframe are not available", new Object[0]));
         }

         this.dataCache = EodHistoryDataCache.getInstance();
         Log.debug("Downloading data from: {}, to: {}", SQTime.toDateString(this.downloadFromDate), SQTime.toDateString(this.toDate));
         long var8 = this.downloadFromDate;
         this.lastSavedDate = -1L;
         var8 = SQTime.setDayOfMonth(var8, 1);
         long var10 = this.getDownloadLimit(this.toDate);

         while (!this.stopped && var8 <= var10) {
            DownloadResultDto var12 = this.getForDay(var8);
            if (var12.getData() != null) {
               this.write(var12.getData());
            }

            this.checkPaused();
            long var13 = var8;
            if (this.timeframe.equals("M1")) {
               var8 = SQTime.addMonths(var8, 1);
            } else {
               var8 = SQTime.addYears(var8, 1);
            }

            int var15 = SQTime.getDaysBetween(var13, var8);
            this.performedDays += var15;
         }

         if (var7) {
            DataManager.updateUnderlyingSymbolName(this.dataInfo.connection, this.dataInfo.symbol, var6);
         }

         if (this.lastSavedDate == -1L && this.lastestDateFromFile > 0L) {
            this.lastSavedDate = this.lastestDateFromFile;
         }

         this.finish(this.lastSavedDate);
         this.closeFiles(true);
         this.recomputeClonedData();
         String var24 = this.stopped ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]);
         this.setFinishProgress(var24);
      } catch (SQException var20) {
         Log.error("Error while performing history data import", var20);
         this.onError(var20.getMessage());
      } catch (Throwable var21) {
         Log.error("Error while performing history data import", var21);
         this.onError(L.t("Error while performing history data import", new Object[0]));
      } finally {
         DataManagerDataProgress.get().removeListener(this.symbol);
         Log.debug("Download job for symbol: {} finished", this.symbol);
      }

      return null;
   }

   private void recomputeClonedData() {
      try {
         DataCloner var1 = new DataCloner();
         DataInfo var2 = DataManager.getDataInfo("History", this.dataInfo.symbol);
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

            var1.cloneToTimezone(this.dataInfo.symbol, var5.symbol, var7, var8, var5.removeWeekends, this.ticker, this.progressListener);
         }
      } catch (Exception var11) {
         Log.error("Error while recomputing cloned data.", var11);
         this.onError(L.t("Error while recomputing cloned data.", new Object[0]));
      }
   }

   private long getDownloadLimit(long var1) {
      if (this.timeframe.equals("M1")) {
         var1 = SQTime.setDayOfMonth(var1, 1);
         var1 = SQTime.addMonths(var1, 1);
         return SQTime.addDays(var1, -1);
      } else {
         var1 = SQTime.addYears(var1, 1);
         var1 = SQTime.setMonthOfYear(var1, 1);
         var1 = SQTime.setDayOfMonth(var1, 1);
         return SQTime.addDays(var1, -1);
      }
   }

   public void setFinishProgress(String var1) {
      this.performedDays = this.totalDays;
      this.updateProgress(var1, true, true);
   }

   private void updateProgress(String var1, boolean var2, boolean var3) {
      if (this.progressListener != null) {
         double var4 = SQUtils.round(this.performedDays * 100.0 / this.totalDays, 1);
         if (var2) {
            var4 = 100.0;
         }

         if (var4 < 100.0 || var2) {
            this.progressListener.onProgress(var4);
         }

         if (this.stopped) {
            var1 = L.t("Stopping", new Object[0]) + " - " + var1;
         }

         this.progressListener.setMessage(var1);
      }
   }

   private List<VersatileData> getVersatileDataForStock(byte[] var1) throws Exception {
      DataBinReaderNew var2 = DataBinReaderNew.getInstance(1, this.dataInfo.symbolInfo);
      var2.setData(var1);
      long var3 = -1L;
      LinkedList var5 = new LinkedList();

      try {
         while (var2.readData()) {
            if (var2.tickData.time >= this.downloadFromDate && var3 < var2.tickData.time) {
               VersatileData var6 = this.createData(var2.tickData);
               this.correctTickData(var6);
               var5.add(var6);
               this.storeStockVersatileDataToQueue(var6);
               var3 = var6.time;
            }
         }

         return var5;
      } finally {
         var2.closeFile();
      }
   }

   private VersatileData createData(VersatileData var1) {
      VersatileData var2 = new VersatileData();
      var2.time = var1.time;
      var2.close = var1.close;
      var2.open = var1.open;
      var2.low = var1.low;
      var2.high = var1.high;
      var2.volume = var1.volume;
      return var2;
   }

   private void storeStockVersatileDataToQueue(VersatileData var1) {
      if (var1.open == -1.0 && var1.high == -1.0) {
      }

      this.datas.add(0, var1);
   }

   private void write(byte[] var1) throws Exception {
      this.writeStocks(var1);
   }

   private void writeStocks(byte[] var1) throws Exception {
      List var2 = this.getVersatileDataForStock(var1);
      this.shiftTimeIfRequired(var2);

      for (VersatileData var4 : var2) {
         this.writer.writeData(var4);
         this.lastSavedDate = var4.time;
         this.totalRecords++;
      }
   }

   private void shiftTimeIfRequired(List<VersatileData> var1) {
      for (VersatileData var3 : var1) {
         var3.time = var3.time + this.timeShift;
         DateTime var4 = new DateTime(var3.time).withZoneRetainFields(this.sourceTz);
         DateTime var5 = var4.withZone(this.targetTz).toLocalDateTime().toDateTime();
         if (this.shiftHours != 0) {
            var5 = var5.plusHours(this.shiftHours);
         }

         if (this.minutesShift) {
            var5 = var5.plusMinutes(1);
         }

         var3.time = var5.getMillis();
      }
   }

   private DownloadResultDto getForDay(long var1) throws Exception {
      int var3 = SQTime.getFullYear(var1);
      int var4 = SQTime.getMonthOriginal(var1);
      return this.dataCache.performLoad(this.tickerForDownload, var3, var4, new DownloadMessageHandler() {
         @Override
         public void onDownloadMessage(String var1, String var2) {
            DownloadEodStockHistorySymbolJob.this.progressListener.onError(var1);
         }
      });
   }

   private boolean evalDates() throws Exception {
      boolean var1 = false;
      this.fromDate = this.aliasTicker != null ? this.aliasTicker.getDateFrom().getTime() : this.ticker.getDateFrom().getTime();
      this.toDate = this.aliasTicker != null ? this.aliasTicker.getDateTo().getTime() : this.ticker.getDateTo().getTime();
      this.lastestDateFromFile = this.reader == null ? -1L : this.readOriginalFile();
      if (this.lastestDateFromFile > this.fromDate) {
         this.downloadFromDate = SQTime.addDays(this.lastestDateFromFile, 1);
         this.downloadFromDate = SQTime.correctDayStart(this.downloadFromDate);
      } else {
         this.downloadFromDate = this.fromDate;
      }

      if (this.lastestDateFromFile != -1L) {
         Date var2 = new Date(this.lastestDateFromFile);
         if (this.kind == TickerKind.FUTURES && (this.forceDownload || HistoryDataManager.get().shouldInvalidateFutureDatas(this.ticker.getTicker(), var2))) {
            this.clearData();
            this.downloadFromDate = this.fromDate;
            var1 = true;
         }

         if (this.kind == TickerKind.STOCK && (this.forceDownload || HistoryDataManager.get().shouldInvalidateStockDatas(this.ticker.getTicker(), var2))) {
            this.clearData();
            this.downloadFromDate = this.fromDate;
            Log.debug("Performing full import of {}", this.ticker.getTicker());
            var1 = true;
         }
      }

      this.totalDays = SQTime.getDaysBetween(this.downloadFromDate, this.toDate) + 1;
      return var1;
   }

   private long readOriginalFile() throws Exception {
      long var1 = -1L;

      while (this.reader.readData()) {
         var1 = this.reader.tickData.time;
         VersatileData var3 = this.createData(this.reader.tickData);
         this.writer.writeData(var3);
         this.storeStockVersatileDataToQueue(var3);
         this.totalRecords++;
      }

      return var1;
   }

   private void clearData() throws Exception {
      this.totalRecords = 0;
      this.datas.clear();
      if (this.writer != null) {
         this.writer.close();
         String var1 = DataManager.getDataFileName("History", this.symbol, this.timeframe, "No Session");
         this.writer = this.freeSymbol
            ? DataBinWriterNew.getInstance(1, MainApp.getDataPath(), this.dataInfo.symbolInfo)
            : DataBinWriterNew.getCryptedInstance(1, MainApp.getDataPath(), this.dataInfo.symbolInfo);
         this.writer.setFileName(var1);
         this.writer.open();
      }
   }

   private void onError(String var1) {
      this.progressListener.onError(var1);
      this.closeFiles(false);
   }

   private void finish(long var1) {
      try {
         if (this.downloadFromDate != -1L && var1 != -1L) {
            long var3;
            if (this.timeframe.equals("D1")) {
               var3 = this.totalRecords * 60 * 60 * 24;
            } else {
               var3 = this.totalRecords * 60;
            }

            DataManager.updateDataInBatch(
               "History", this.symbol, this.fromDate, var1, this.totalRecords, var3, this.bartimeType, this.timeframe, this.dataInfo.timezone
            );
         }
      } catch (Exception var5) {
         Log.error("Error while saving results", var5);
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
         this.deleteCopyFile();
      } else {
         this.revertCopyFile();
      }
   }

   public boolean deleteCopyFile() {
      String var1 = DataManager.getDataFileName("History", this.symbol, this.timeframe, "No Session");
      String var2 = var1 + ".copy";
      return new File(var2).delete();
   }

   public void revertCopyFile() {
      String var1 = DataManager.getDataFileName("History", this.symbol, this.timeframe, "No Session");
      String var2 = var1 + ".copy";
      File var3 = new File(var2);
      if (var3.exists()) {
         File var4 = new File(var1);
         var4.delete();
         var3.renameTo(var4);
      }
   }
}
