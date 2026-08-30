package com.strategyquant.datalib.data;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.File;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCloner {
   private static final int REFRESH_RATE = 100;
   public static final Logger Log = LoggerFactory.getLogger(DataCloner.class);
   private volatile boolean canceled = false;
   private boolean cloneRunning = false;
   private volatile boolean paused;
   private IProgressListener listener;
   private TickerDto ticker;
   private DataInfo sourceSymbolInfo;
   private DataInfo targetSymbolInfo;
   private int shiftHours;
   private boolean removeWeekends;
   private long dateFrom;
   private long dateTo;
   private int rows;
   private long secondsRecords;
   private DateTimeZone sourceTz;
   private DateTimeZone targetTz;
   private IDataLoader loader;
   private DataBinWriterNew writer;

   public void cancel() {
      this.canceled = true;
   }

   public void cloneToTimezone(String var1, String var2, String var3, int var4, boolean var5, TickerDto var6, IProgressListener var7) {
      try {
         this.ticker = var6;
         this.canceled = false;
         this.cloneRunning = true;
         this.listener = var7;
         this.shiftHours = var4;
         this.removeWeekends = var5;
         this.sourceSymbolInfo = DataManager.getDataInfo("History", var1);
         this.targetSymbolInfo = DataManager.getDataInfo("History", var2);
         this.targetSymbolInfo.timezone = Timezone.shiftHours(var3, var4);
         var7.setMessage(
            L.t("Cloning '%s' data to timezone '%s'", new Object[]{var1, Timezone.print(this.targetSymbolInfo.timezone, this.targetSymbolInfo.source, false)})
         );
         this.cloneData();
      } catch (Exception var12) {
         Log.error("Error while cloning data. Exc.", var12);
         var7.onError(var12.getMessage());
      } finally {
         this.cloneRunning = false;
      }
   }

   private void cloneData() throws Exception {
      this.secondsRecords = 0L;
      this.dateFrom = 0L;
      this.dateTo = 0L;
      this.rows = 0;
      if (this.sourceSymbolInfo.rows > 0) {
         String var1 = DataManager.getDataFileName(
            this.sourceSymbolInfo.connection, this.targetSymbolInfo.symbol, this.sourceSymbolInfo.timeframe, "No Session"
         );
         String var2 = DataManager.getTempFileName(var1);
         this.prepareClone(var2);
         this.performCloneFile();
         this.loader.close();
         this.writer.close();
         DataManager.removeDataFiles(this.targetSymbolInfo.connection, this.targetSymbolInfo.symbol);
         new File(var2).renameTo(new File(var1));
      }

      DataManager.updateData(
         this.targetSymbolInfo.connection,
         this.targetSymbolInfo.symbol,
         this.dateFrom,
         this.dateTo,
         this.rows,
         this.secondsRecords,
         this.sourceSymbolInfo.barTimeType,
         this.sourceSymbolInfo.timeframe,
         this.targetSymbolInfo.timezone,
         this.sourceSymbolInfo.id,
         this.removeWeekends
      );
      this.listener.onProgress(100.0);
      this.listener.onFinish();
   }

   private void prepareClone(String var1) throws DataException, Exception {
      ChartDef var2 = new ChartDef(
         this.sourceSymbolInfo.connection,
         this.sourceSymbolInfo.symbol,
         this.sourceSymbolInfo.timeframe,
         this.sourceSymbolInfo.dateFrom,
         this.sourceSymbolInfo.dateTo,
         2.5,
         "No Session"
      );
      this.loader = DataManager.getDataLoader(var2, 1);
      this.loader.open();
      boolean var3 = this.loader.isCrypted();
      this.writer = null;
      if (this.sourceSymbolInfo.timeframe.equals("TICK")) {
         this.writer = var3
            ? DataBinWriterNew.getCryptedInstance(2, MainApp.getDataPath(), this.sourceSymbolInfo.symbolInfo)
            : DataBinWriterNew.getInstance(2, MainApp.getDataPath(), this.sourceSymbolInfo.symbolInfo);
      } else {
         this.writer = var3
            ? DataBinWriterNew.getCryptedInstance(1, MainApp.getDataPath(), this.sourceSymbolInfo.symbolInfo)
            : DataBinWriterNew.getInstance(1, MainApp.getDataPath(), this.sourceSymbolInfo.symbolInfo);
      }

      this.writer.setFileName(var1);
      this.writer.open();
      String var4 = this.sourceSymbolInfo.timezone != null ? this.sourceSymbolInfo.timezone : "Etc/UCT";
      String var5 = this.targetSymbolInfo.timezone;
      this.sourceTz = null;
      this.targetTz = null;
      if ((this.sourceSymbolInfo.source == 6 || this.sourceSymbolInfo.source == 5) && (var4 == null || var4.startsWith("Exchange"))) {
         String var6 = ExchangeTimezone.get().getTimezone(this.ticker.getMarketName());
         if (var4 != null && var4.length() > 8) {
            String var7 = var4.substring(8).trim();
            this.shiftHours = this.shiftHours + Integer.valueOf(var7);
         }

         this.sourceTz = DateTimeZone.forID(var6);
         if (var5.startsWith("Exchange")) {
            this.targetTz = DateTimeZone.forID(var6);
         }
      }

      if (this.sourceTz == null) {
         String var8 = Timezone.parseId(var4);
         if (var8.equals("EETUS")) {
            var8 = "America/New_York";
            this.shiftHours -= 7;
         }

         this.sourceTz = DateTimeZone.forID(var8);
      }

      if (this.targetTz == null) {
         String var9 = Timezone.parseId(var5);
         if (var9.equals("EETUS")) {
            var9 = "America/New_York";
            this.shiftHours += 7;
         }

         this.targetTz = DateTimeZone.forID(var9);
      }
   }

   private void performCloneFile() throws Exception, InterruptedException {
      VersatileData var1 = new VersatileData();
      long var3 = -1L;

      while (this.loader.hasNextTick()) {
         this.loader.getNextTick(var1);
         DateTime var5 = new DateTime(var1.time).withZoneRetainFields(this.sourceTz);
         DateTime var6 = var5.withZone(this.targetTz).toLocalDateTime().toDateTime();
         if (this.shiftHours != 0) {
            var6 = var6.plusHours(this.shiftHours);
         }

         var1.time = var6.getMillis();
         if (var1.time >= var3) {
            var3 = var1.time;
            if (this.removeWeekends) {
               int var2 = var6.getDayOfWeek();
               if (var2 == 6 || var2 == 7) {
                  continue;
               }
            }

            if (this.dateFrom == 0L) {
               this.dateFrom = var1.time;
            }

            this.dateTo = var1.time;
            this.writer.writeData(var1);
            this.rows++;
            if (this.rows % 100 == 0) {
               this.listener.onProgress((double)this.rows / this.sourceSymbolInfo.rows * 100.0);
               this.listener.setMessage(L.t("Cloned %s", new Object[]{SQTime.toDateMinuteString(this.dateTo)}));
            }

            if (this.canceled) {
               break;
            }

            this.checkPaused();
         }
      }
   }

   public void createSymbol(String var1, DataInfo var2) throws Exception {
      DataManager.addData(var2.connection, var1, var2.instrument, var2.barTimeType, var2.source, var2.uSymbol, var2.uSymbolName, -1, var2.brokerId);
   }

   public boolean isRunning() {
      return this.cloneRunning;
   }

   public void pause() {
      this.paused = true;
   }

   public void restart() {
      this.paused = false;
   }

   private void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.canceled) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }
}
