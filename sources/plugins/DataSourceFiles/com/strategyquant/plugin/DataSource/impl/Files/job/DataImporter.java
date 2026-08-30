package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.DataCsvLoader;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataImporter {
   public static final Logger Log = LoggerFactory.getLogger(DataImporter.class);
   private DataCsvLoader csvLoader = new DataCsvLoader();
   private volatile boolean canceled;
   private volatile boolean paused;
   private IProgressListener listener;
   private boolean skipInvalidRows;

   public void performImport(IProgressListener var1, DataInfo var2, ImportDataInfo var3) throws Exception {
      this.listener = var1;
      this.canceled = false;
      DataBinWriterNew var4 = null;
      long var5 = 0L;
      int var7 = 0;
      this.skipInvalidRows = true;
      File var8 = new File(MainApp.getDataPath() + "/importnocheck.txt");
      if (var8.exists()) {
         this.skipInvalidRows = false;
      }

      Log.info(this.skipInvalidRows ? "Invalid csv rows will be ignored" : "Invalid csv rows will be proccessed");

      try {
         var1.onStart();
         this.checkDataFile(var1, var2, var3);
         var1.setStep(2);
         var1.setMessage(L.t("Importing file...", new Object[0]));
         if (var3.timeframe.equals("TICK")) {
            var4 = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), var2.symbolInfo);
         } else {
            var4 = DataBinWriterNew.getInstance(1, MainApp.getDataPath(), var2.symbolInfo);
         }

         String var9 = DataManager.getDataFileName(var2.connection, var2.symbol, var3.timeframe, "No Session");
         String var10 = DataManager.getTempFileName(var9);
         var4.setFileName(var10);
         var4.open();
         long var11 = var2.dateFrom;
         long var13 = var2.dateTo;
         long var15 = var3.beginTimeNewFile;
         long var17 = var3.endTimeNewFile;
         long var19 = TimeframeManager.getMillis(var3.timeframe);
         var19 = var19 == 0L ? 1000L : var19;
         long var21 = var11 < var15 && var11 != 0L ? var11 : var15;
         long var23 = var17;
         if (var17 > 0L && var13 > var17) {
            var23 = var13;
         } else if (var17 < 0L && var13 < var17) {
            var23 = var13;
         }

         int var25 = (int)((var23 - var21) / var19);
         int var26 = 0;
         long var27 = 0L;
         int var29 = -1;
         int var30 = -1;
         VersatileData var31 = new VersatileData();
         if (var11 < var15 && var11 != 0L) {
            ChartDef var32 = new ChartDef(var2.connection, var2.symbol, var2.timeframe, var11, var15, 2.5, "No Session");
            IDataLoader var33 = DataManager.getDataLoader(var32, 1);
            var33.open();

            while (var33.hasNextTick() && !this.canceled) {
               var33.getNextTick(var31);
               if (var31.time >= var15) {
                  break;
               }

               var4.writeData(var31);
               var30 = SQTime.getDateTime(var31.time);
               if (var30 != var29) {
                  var27++;
               }

               var29 = var30;
               this.setProgress(++var26, var25);
               var1.setMessage(L.t("Copying data %s", new Object[]{SQTime.toDateMinuteString(var31.time)}));
               this.checkPaused();
            }

            var33.close();
         }

         this.csvLoader = new DataCsvLoader();
         DataCsvLoader.reset();
         this.csvLoader.setParams(var3, var2, null);
         this.csvLoader.openFile();
         var3.resetLastAskBid();

         while (this.csvLoader.readData() && !this.canceled) {
            if ((!this.csvLoader.isDataCorrect() || this.csvLoader.tickData.getCheckSum() == 0) && this.skipInvalidRows) {
               Log.warn(
                  "Skipping row #{} due incorrect content: '{}'. Average price:{}",
                  new Object[]{this.csvLoader.loadedRows + var3.skipRows, this.csvLoader.getCurrentLine(), this.csvLoader.getAveragePriceValue()}
               );
            } else {
               this.convertToOriginalTimezone(this.csvLoader.tickData);
               if (this.csvLoader.tickData.time <= var5 && (this.csvLoader.tickData.time >= 0L || this.csvLoader.tickData.time >= var5)) {
                  if (this.csvLoader.tickData.time == var5) {
                     var7++;
                  } else if (var3.errorHandling != 1) {
                     System.out.println("HERE (2)");
                     throw new Exception("This shouldn't happen - time on line is smaller than previous record!");
                  }
               } else {
                  var7 = 0;
               }

               var5 = this.csvLoader.tickData.time;
               if (var7 > 0) {
                  this.csvLoader.tickData.time += var7;
               }

               var4.writeData(this.csvLoader.tickData);
               var30 = SQTime.getDateTime(this.csvLoader.tickData.time);
               if (var30 != var29) {
                  var27++;
               }

               var29 = var30;
               this.setProgress(++var26, var25);
               var1.setMessage(L.t("Imported file data %s", new Object[]{SQTime.toDateMinuteString(var5)}));
               this.checkPaused();
            }
         }

         this.csvLoader.close();
         DataCsvLoader.reset();
         if (var13 > var17 && var13 != 0L) {
            ChartDef var41 = new ChartDef(var2.connection, var2.symbol, var2.timeframe, var17, var13, 2.5, "No Session");
            IDataLoader var42 = DataManager.getDataLoader(var41, 1);
            var42.open();

            while (var42.hasNextTick() && !this.canceled) {
               var42.getNextTick(var31);
               var4.writeData(var31);
               var30 = SQTime.getDateTime(var31.time);
               if (var30 != var29) {
                  var27++;
               }

               var29 = var30;
               this.setProgress(++var26, var25);
               var1.setMessage(L.t("Imported file data %s", new Object[]{SQTime.toDateMinuteString(var31.time)}));
               this.checkPaused();
            }

            var42.close();
         }

         if (!var3.timeframe.equals("TICK")) {
            var27 = var26 * 60;
         }

         DataManager.updateData(var2.connection, var2.symbol, var21, var23, var26, var27, var2.barTimeType, var3.timeframe, var3.timezone);

         try {
            TimeframeManager.addTimeframe(var3.timeframe);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getTimeframes(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         } catch (Exception var35) {
         }

         var4.close();
         DataManager.removeDataFiles(var2.connection, var2.symbol);
         new File(var10).renameTo(new File(var9));
         this.recomputeClonedData(var2);
         var1.onProgress(100.0);
         var1.onFinish();
      } catch (Exception var36) {
         try {
            this.csvLoader.close();
            if (var4 != null) {
               var4.close();
            }
         } catch (Exception var34) {
            Log.error("Error while closing file loader/writer.", var34);
         }

         Log.error("Import error.", var36);
         throw new Exception("Import error." + var36.getMessage(), var36);
      }
   }

   public void setProgress(int var1, int var2) {
      double var3 = (double)var1 / var2 * 100.0;
      if (var3 > 99.0) {
         var3 = 99.0;
      }

      this.listener.onProgress(var3);
   }

   private void recomputeClonedData(DataInfo var1) throws Exception {
      try {
         DataCloner var2 = new DataCloner();
         DataInfo var3 = DataManager.getDataInfo("History", var1.symbol);
         ArrayList var4 = DataManager.listCloned(var3.id);

         for (int var5 = 0; var5 < var4.size(); var5++) {
            DataInfo var6 = (DataInfo)var4.get(var5);
            DataManager.clearData(var6.connection, var6.symbol);
            this.listener.setMessage(L.t("Recomputing cloned data", new Object[0]));
            String[] var7 = var6.timezone.split("\\|");
            String var8 = var7[0];
            int var9 = 0;

            try {
               var9 = Integer.parseInt(var7[1]);
            } catch (Exception var11) {
            }

            var2.cloneToTimezone(var1.symbol, var6.symbol, var8, var9, var6.removeWeekends, null, this.listener);
         }
      } catch (Exception var12) {
         Log.error("Error while recomputing cloned data.", var12);
         throw new Exception("Error while recomputing cloned data.", var12);
      }
   }

   private void convertToOriginalTimezone(VersatileData var1) {
   }

   private void checkDataFile(IProgressListener var1, DataInfo var2, ImportDataInfo var3) throws Exception {
      this.csvLoader = new DataCsvLoader();
      DataCsvLoader.reset();
      this.csvLoader.setParams(var3, var2, null);
      var1.setStep(1);
      var1.setMessage(L.t("Checking file for import...", new Object[0]));
      var3 = this.csvLoader.checkFile(var1);
      this.checkGaps(var1, var2, var3);
      this.checkTimeframe(var1, var2, var3);
   }

   protected void checkGaps(IProgressListener var1, DataInfo var2, ImportDataInfo var3) throws Exception {
      if (var2.dateFrom != 0L && var2.dateTo != 0L) {
         if (this.csvLoader.beginTimeNewFile != -1L && this.csvLoader.endTimeNewFile != -1L) {
            long var4 = 0L;
            long var6 = 0L;
            Log.debug("Db data date range: From " + new Date(var2.dateFrom) + " to " + new Date(var2.dateTo));
            Log.debug("Imported file date range: From " + new Date(this.csvLoader.beginTimeNewFile) + " to " + new Date(this.csvLoader.endTimeNewFile));
            if (this.csvLoader.endTimeNewFile < var2.dateFrom) {
               var4 = this.csvLoader.endTimeNewFile / 86400000L;
               var6 = var2.dateFrom > 0L ? var2.dateFrom / 86400000L : var4;
            } else if (this.csvLoader.beginTimeNewFile > var2.dateTo) {
               var4 = this.csvLoader.beginTimeNewFile / 86400000L;
               var6 = var2.dateTo > 0L ? var2.dateTo / 86400000L : var4;
            }

            if (var4 != 0L && Math.abs(var6 - var4) > 5L) {
               int var8 = (int)Math.abs(var6 - var4);
               var1.onConfirm(
                  "The imported data will create a gap of "
                     + var8
                     + " days if added to the current history data for this symbol. Do you really want to proceed with the import?"
               );
            }

            var3.beginTimeNewFile = this.csvLoader.beginTimeNewFile;
            var3.endTimeNewFile = this.csvLoader.endTimeNewFile;
         } else {
            throw new Exception("Exception during import: Cannot load first and last date, bad file format?");
         }
      }
   }

   private void checkTimeframe(IProgressListener var1, DataInfo var2, ImportDataInfo var3) throws Exception {
      String var4 = var3.timeframe;
      if (var4 == null) {
         var4 = this.importFormatContainsBid(var3) ? "TICK" : this.csvLoader.recognizeTimeframe();
      }

      if (var4 == null) {
         throw new Exception("Import file has unknown or unsupported timeframe!");
      }

      if (var2.timeframe != null && !var2.timeframe.equals(var4)) {
         throw new Exception(
            "Import file has different timeframe than the symbol where you want to import it!<br>Import file timeframe was recognized to "
               + var4
               + ", your symbol "
               + var2.symbol
               + " has its base data in timeframe "
               + var2.timeframe
               + " (loaded in some previous import).<br><br>It is not possible to import "
               + var4
               + " data to existing "
               + var2.timeframe
               + " data. Please delete the existing data from this symbol or import it under a different symbol name."
         );
      }

      var3.timeframe = var4;
   }

   private boolean importFormatContainsBid(ImportDataInfo var1) {
      for (int var2 = 0; var2 < var1.columnTypes.size(); var2++) {
         DefaultCol var3 = (DefaultCol)var1.columnTypes.get(var2);
         if (var3.getName().equals("Bid") || var3.getName().equals("Ask")) {
            return true;
         }
      }

      return false;
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

   public void cancel() {
      if (this.csvLoader != null) {
         this.csvLoader.cancel();
      }

      this.canceled = true;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public void pause() {
      this.paused = true;
      if (this.csvLoader != null) {
         this.csvLoader.pause();
      }
   }

   public void restart() {
      this.paused = false;
      if (this.csvLoader != null) {
         this.csvLoader.restart();
      }
   }
}
