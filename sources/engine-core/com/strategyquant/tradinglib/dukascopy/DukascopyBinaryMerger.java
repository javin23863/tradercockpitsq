package com.strategyquant.tradinglib.dukascopy;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DukascopyBinaryMerger {
   public static final String DATE_TIME_FORMAT = "yyyy.MM.dd";
   public static final long VOLUME_CONST = 1000000L;
   private static final Logger Log = LoggerFactory.getLogger(DukascopyBinaryMerger.class);
   private Map<Integer, Long> dates = new HashMap<>();
   private LinkedList<VersatileData>[] queue;
   private String jobId;
   private int lastSavedHourIndex = -1;
   private ImportInfo importInfo;
   private int currentBatchSize;
   private boolean ignoreWeekend = false;
   private long realSavedPerBatch = 0L;
   private VersatileData origNotSavedTickData = new VersatileData();
   private DataBinWriterNew writer;
   private DataBinReaderNew reader;
   private DataManagerProgressListener progressListener;
   private int savedRows = 0;
   protected long requestFromDate;
   protected long fromDate = -1L;
   private long toDate = -1L;
   private int totalDays;
   protected volatile int performedDays = 0;
   private int lastEvaluatedDay = -1;
   private boolean stopped;
   private boolean skipProgress;
   private int lastSaved = -1;
   private long savedSecs = 0L;
   private String messagePrefix;
   private boolean saveError;
   private DateTimeZone sourceTz;
   private DateTimeZone targetTz;
   private int shiftHours = 0;

   public DukascopyBinaryMerger(String var1, String var2, int var3, ImportInfo var4) {
      this.jobId = var2;
      this.messagePrefix = var1;
      this.importInfo = var4;
      this.totalDays = this.getTotalDays();
      this.requestFromDate = var4.oldDateFrom != 0L && var4.oldDateFrom < var4.dateFrom ? var4.oldDateFrom : var4.dateFrom;
      this.queue = new LinkedList[var3];
      this.origNotSavedTickData.time = -1L;
   }

   public void setTargetTimezone(String var1) {
      this.sourceTz = DateTimeZone.UTC;
      if (var1.equals("EETUS")) {
         var1 = "America/New_York";
         this.shiftHours = 7;
      }

      this.targetTz = DateTimeZone.forID(Timezone.parseId(var1));
   }

   public boolean isSaveError() {
      return this.saveError;
   }

   private int getTotalDays() {
      long var1 = this.importInfo.dateFrom;
      long var3 = this.importInfo.dateTo;
      if (this.importInfo.oldDateFrom != 0L && this.importInfo.oldDateFrom < var1) {
         var1 = this.importInfo.oldDateFrom;
      }

      if (this.importInfo.oldDateTo != 0L && this.importInfo.oldDateTo > var3) {
         var3 = this.importInfo.oldDateTo;
      }

      return SQTime.getDaysBetween(var1, var3) + 1;
   }

   public void setIgnoreWeekend(boolean var1) {
      this.ignoreWeekend = var1;
   }

   public void resetWithBatchAndStart(int var1, int var2) {
      this.lastSavedHourIndex = var1;
      this.realSavedPerBatch = 0L;
      this.currentBatchSize = var2;

      for (int var3 = 0; var3 < this.queue.length; var3++) {
         this.queue[var3] = null;
      }
   }

   public void reset(int var1) {
      this.resetWithBatchAndStart(-1, var1);
   }

   public int getJobsIndex(String var1) {
      String var2 = var1.substring(this.jobId.length() + 1);
      Integer var3 = Integer.parseInt(var2);
      return var3;
   }

   private long getTime(int var1, long var2) {
      return var2 + var1;
   }

   private double getPrice(int var1) {
      return this.importInfo.priceConstant * var1;
   }

   public void showError(long var1, String var3) {
      String var4 = String.format(this.messagePrefix + " %s", SQTime.toString(var1, "yyyy.MM.dd")) + " - " + L.t("ERROR: %s", new Object[]{var3});
      this.evalPerformedDays(var1);
      this.updateProgress(var4, false);
   }

   public synchronized void handleBi5Data(DownloadResultDto var1, String var2, boolean var3, boolean var4) throws Exception {
      int var5 = this.getJobsIndex(var2);
      long var6 = this.dates.get(var5);
      if (var4 && var1.getData() != null) {
         Log.debug("Decoding data");
         ByteBuffer var8 = DukascopyUtils.decodeBi5Data(new ByteArrayInputStream(var1.getData()));
         if (var3) {
            this.handleHourData(var8, var5, var6);
         } else {
            this.handleOhlcData(var8, var5, var6);
         }

         Log.debug("Decoding data finished");
         this.tryToSaveQueue(var6);
      } else {
         if (var1.getErrorMessage() != null) {
            this.showError(var6, "download failed: " + var1.getErrorMessage());
         } else {
            this.showError(var6, "download failed. Error code: " + var1.getHttpCode());
         }
      }
   }

   private void handleHourData(ByteBuffer var1, int var2, Long var3) {
      boolean var4 = false;
      if (var1 != null) {
         while (var1.hasRemaining()) {
            VersatileData var5 = new VersatileData();
            var5.time = this.getTime(var1.getInt(), var3);
            var5.ask = this.getPrice(var1.getInt());
            var5.bid = this.getPrice(var1.getInt());
            var1.getFloat();
            var5.volume = var1.getFloat() * 1000000.0F;
            this.storeToQueue(var2, var5);
            var4 = true;
         }
      }

      if (!var4) {
         Log.debug("Data downloaded - but don't contains any tick data");
         this.storeToQueue(var2, null);
      }
   }

   private void handleOhlcData(ByteBuffer var1, int var2, Long var3) {
      boolean var4 = false;
      if (var1 != null) {
         while (var1.hasRemaining()) {
            VersatileData var5 = new VersatileData();
            int var6 = var1.getInt();
            var5.time = this.getTime(var6 * 1000, var3);
            var5.open = this.getPrice(var1.getInt());
            var5.close = this.getPrice(var1.getInt());
            var5.low = this.getPrice(var1.getInt());
            var5.high = this.getPrice(var1.getInt());
            var5.volume = var1.getFloat() * 1000000.0F;
            this.storeToQueue(var2, var5);
            var4 = true;
         }
      }

      if (!var4) {
         Log.debug("Data downloaded - but doesn't contain any tick data");
         this.storeToQueue(var2, null);
      }
   }

   public Long getDateForJob(String var1) {
      int var2 = this.getJobsIndex(var1);
      return this.dates.get(var2);
   }

   public void tryToSaveQueue(long var1) throws Exception {
      Log.debug("Trying to save datas in queue");
      int var3 = this.lastSavedHourIndex;

      while (true) {
         if (++var3 == this.currentBatchSize) {
            Log.debug("Everything saved");
            if (this.realSavedPerBatch == 0L) {
               this.performedDays = SQTime.getDaysBetween(this.requestFromDate, var1);
               String var11 = String.format(this.messagePrefix + " %s", SQTime.toString(var1, "yyyy.MM.dd"));
               this.updateProgress(var11, false);
            }
            break;
         }

         LinkedList var4 = this.queue[var3];
         if (var4 == null) {
            Log.debug("Skipping saving - found missing datas - waiting for them");
            break;
         }

         long var5 = this.dates.get(var3);
         Log.debug("Saving data for:" + this.getDateDesc(var5));

         for (VersatileData var8 : var4) {
            if (this.importInfo.downloadM1Data && var8.volume > 0.0) {
               long var9 = Math.round(var8.volume * 100000.0);
               if (var9 == 0L) {
                  var8.volume = 1.0;
               }
            }

            if (!this.importInfo.downloadM1Data || var8.volume > 0.0) {
               this.performSave(var8, true);
               this.realSavedPerBatch++;
            }
         }

         this.lastSavedHourIndex = var3;
         this.queue[var3] = null;
      }
   }

   public void storeToQueue(int var1, VersatileData var2) {
      LinkedList var3 = this.queue[var1];
      if (var3 == null) {
         var3 = new LinkedList();
         this.queue[var1] = var3;
      }

      if (var2 != null) {
         var3.add(var2);
      }

      if (this.importInfo.dateFrom == 0L) {
         this.importInfo.dateFrom = var2.time;
         this.totalDays = this.getTotalDays();
      }
   }

   public void register(int var1, long var2) {
      this.dates.put(var1, var2);
   }

   public long checkDate(long var1) throws Exception {
      if (this.getReader() == null) {
         return this.ignoreWeekend && SQTime.getDayOfWeek(var1) == 6 ? SQTime.addDays(var1, 1) : var1;
      }

      this.readTill(var1, true);
      if (this.hasOrigNotSavedData()) {
         return this.ignoreWeekend && SQTime.getDayOfWeek(var1) == 6 ? SQTime.addDays(var1, 1) : var1;
      }

      boolean var3 = this.hasDaySomeRecords(var1, !this.importInfo.overwrite);
      if (!this.importInfo.overwrite && var3) {
         long var4 = SQTime.addDays(var1, 1);

         while (true) {
            var3 = this.hasDaySomeRecords(var4, true);
            if (!var3 && (!this.ignoreWeekend || SQTime.getDayOfWeek(var4) != 6)) {
               return var4;
            }

            var4 = SQTime.addDays(var4, 1);
         }
      } else {
         return var1;
      }
   }

   public void clearQueue(int var1) {
      this.queue[var1] = new LinkedList<>();
   }

   public void resetQueue(int var1) {
      this.queue[var1] = null;
   }

   public void setWriter(DataBinWriterNew var1) {
      this.writer = var1;
   }

   public void setReader(DataBinReaderNew var1) {
      this.reader = var1;
   }

   public void skipAllProgress() {
      this.skipProgress = true;
   }

   public void setStopped(boolean var1) {
      this.stopped = var1;
   }

   public long getFromDate() {
      return this.fromDate;
   }

   public long getToDate() {
      return this.toDate;
   }

   public int getSavedRows() {
      return this.savedRows;
   }

   public DataBinReaderNew getReader() {
      return this.reader;
   }

   protected boolean hasOrigNotSavedData() {
      return this.origNotSavedTickData.time == -1L;
   }

   public String getDateDesc(long var1) {
      return SQTime.toString(var1, "yyyy.MM.dd");
   }

   public void setProgressListener(DataManagerProgressListener var1) {
      this.progressListener = var1;
   }

   public boolean readTill(long var1, boolean var3) throws Exception {
      boolean var4 = false;
      var1 = this.getRecountedTime(var1);
      if (this.origNotSavedTickData.time != -1L) {
         if (this.origNotSavedTickData.time >= var1) {
            return false;
         }

         var4 = true;
         if (var3) {
            this.performSave(this.origNotSavedTickData, false);
            this.setOrigNotSavedTickData(null);
         }
      }

      if (this.reader != null) {
         for (; this.reader.readData(); var4 = true) {
            this.setOrigNotSavedTickData(this.reader.tickData);
            if (this.origNotSavedTickData.time >= var1) {
               break;
            }

            if (var3) {
               this.performSave(this.origNotSavedTickData, false);
               this.setOrigNotSavedTickData(null);
            }
         }

         if (this.origNotSavedTickData.time < var1 && !var3) {
            this.setOrigNotSavedTickData(null);
         }
      }

      return var4;
   }

   protected boolean hasDaySomeRecords(long var1, boolean var3) throws Exception {
      long var4 = SQTime.addDays(var1, 1);
      return this.readTill(var4, var3);
   }

   public long getSavedSecs() {
      return this.savedSecs;
   }

   public void performSave(VersatileData var1, boolean var2) throws Exception {
      try {
         this.saveError = false;
         this.recountTimeToTargetTz(var1, var2);
         this.writer.writeData(var1);
      } catch (Throwable var6) {
         this.saveError = true;
      }

      this.savedRows++;
      int var3 = SQTime.getDateTime(var1.time);
      if (this.lastSaved != var3) {
         this.savedSecs++;
      }

      this.lastSaved = var3;
      long var4 = var1.time;
      if (var4 > this.toDate) {
         this.toDate = var4;
      }

      if (var4 < this.fromDate || this.fromDate == -1L) {
         this.fromDate = var4;
      }

      if (this.evalPerformedDays(var1.time)) {
         this.updateSaveProgress(var1.time);
      }
   }

   private void recountTimeToTargetTz(VersatileData var1, boolean var2) {
      if (var2 && this.targetTz != null) {
         DateTime var3 = new DateTime(var1.time).withZoneRetainFields(this.sourceTz);
         DateTime var4 = var3.withZone(this.targetTz).toLocalDateTime().toDateTime();
         if (this.shiftHours != 0) {
            var4 = var4.plusHours(this.shiftHours);
         }

         var1.time = var4.getMillis();
      }
   }

   private long getRecountedTime(long var1) {
      if (this.targetTz == null) {
         return var1;
      }

      DateTime var3 = new DateTime(var1).withZoneRetainFields(this.sourceTz);
      DateTime var4 = var3.withZone(this.targetTz).toLocalDateTime().toDateTime();
      if (this.shiftHours != 0) {
         var4 = var4.plusHours(this.shiftHours);
      }

      return var4.getMillis();
   }

   private boolean evalPerformedDays(long var1) {
      int var3 = SQTime.getDay(var1);
      boolean var4 = var3 != this.lastEvaluatedDay;
      if (var4) {
         if (this.requestFromDate == 0L) {
            this.requestFromDate = var1;
         }

         this.performedDays = SQTime.getDaysBetween(this.requestFromDate, var1);
         if (this.performedDays >= this.totalDays) {
            this.performedDays = this.totalDays - 1;
         }
      }

      this.lastEvaluatedDay = var3;
      return var4;
   }

   public void updateProgress(String var1, boolean var2) {
      if (this.progressListener != null && !this.skipProgress) {
         double var3 = SQUtils.round(this.performedDays * 100.0 / this.totalDays, 1);
         if (var3 < 100.0 || var2) {
            this.progressListener.onProgress(var3);
         }

         if (this.stopped) {
            var1 = "Stopping - " + var1;
         }

         this.progressListener.setMessage(var1);
      }
   }

   public void updateSaveProgress(long var1) {
      String var3 = String.format(this.messagePrefix + " %s", SQTime.toString(var1, "yyyy.MM.dd"));
      this.updateProgress(var3, false);
   }

   public void setFinishProgress(String var1) {
      this.performedDays = this.totalDays;
      this.updateProgress(var1, true);
   }

   public void setOrigNotSavedTickData(VersatileData var1) {
      if (var1 == null) {
         this.origNotSavedTickData.time = -1L;
      } else {
         this.origNotSavedTickData.time = var1.time;
         this.origNotSavedTickData.volume = var1.volume;
         this.origNotSavedTickData.bid = var1.bid;
         this.origNotSavedTickData.ask = var1.ask;
         this.origNotSavedTickData.open = var1.open;
         this.origNotSavedTickData.close = var1.close;
         this.origNotSavedTickData.low = var1.low;
         this.origNotSavedTickData.high = var1.high;
      }
   }

   public void finish() throws Exception {
      if (this.origNotSavedTickData.time != -1L) {
         this.performSave(this.origNotSavedTickData, false);
      }

      if (this.reader != null) {
         while (this.reader.readData()) {
            this.setOrigNotSavedTickData(this.reader.tickData);
            this.performSave(this.origNotSavedTickData, false);
         }

         this.setOrigNotSavedTickData(null);
      }
   }

   public long getRestDays() {
      return this.totalDays - this.performedDays;
   }

   public long getPerformedDays() {
      return this.performedDays;
   }
}
