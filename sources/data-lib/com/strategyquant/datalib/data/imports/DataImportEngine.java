package com.strategyquant.datalib.data.imports;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataImportEngine {
   public static final Logger Log = LoggerFactory.getLogger(DataImportEngine.class);
   private ImportDataInfo importInfo;
   private DataInfo dataInfo;
   private DateTimeZone timeZone;
   private DataCsvLoader csvLoader;
   private static DataImportEngine instance;
   private boolean confirmed = false;
   private boolean canceled = false;
   private volatile boolean importRunning = false;
   private int rows = 0;
   private Thread importThread;
   private IProgressListener listener;

   public static void importData(String var0, String var1, String var2, ImportDataInfo var3, IProgressListener var4) throws Exception {
      if (instance == null) {
         instance = new DataImportEngine();
      }

      if (instance.importRunning) {
         throw new Exception("Import is already running");
      }

      instance.listener = var4;
      instance.importInfo = var3;
      instance.dataInfo = DataManager.getDataInfo(var0, var1);
      instance.timeZone = DateTimeZone.forID(var2);
      instance.confirmed = false;
      instance.canceled = false;
      instance.rows = 0;
      if (instance.dataInfo.rows == 0) {
         instance.dataInfo.timezone = var2;
      }

      instance.importDataFile();
   }

   public static void continueImport() {
      instance.confirmed = true;
   }

   public static void cancelImport() {
      instance.csvLoader.cancel();
      instance.canceled = true;
   }

   private void importDataFile() throws Exception {
      this.importThread = new Thread() {
         @Override
         public void run() {
            DataImportEngine.this.importRunning = true;
            DataBinWriterNew var1 = null;
            long var2 = 0L;
            int var4 = 0;

            try {
               DataImportEngine.this.listener.onStart();
               DataImportEngine.instance.checkDataFile();
               DataImportEngine.this.listener.setStep(2);
               DataImportEngine.this.listener.setMessage(L.t("Importing data...", new Object[0]));
               if (DataImportEngine.this.importInfo.timeframe.equals("TICK")) {
                  var1 = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), DataImportEngine.this.dataInfo.symbolInfo);
               } else {
                  var1 = DataBinWriterNew.getInstance(1, MainApp.getDataPath(), DataImportEngine.this.dataInfo.symbolInfo);
               }

               String var5 = DataManager.getDataFileName(
                  DataImportEngine.this.dataInfo.connection, DataImportEngine.this.dataInfo.symbol, DataImportEngine.this.importInfo.timeframe, "No Session"
               );
               String var6 = DataManager.getTempFileName(var5);
               var1.setFileName(var6);
               var1.open();
               long var7 = DataImportEngine.this.dataInfo.dateFrom;
               long var9 = DataImportEngine.this.dataInfo.dateTo;
               long var11 = DataImportEngine.this.importInfo.beginTimeNewFile;
               long var13 = DataImportEngine.this.importInfo.endTimeNewFile;
               long var15 = TimeframeManager.getMillis(DataImportEngine.this.importInfo.timeframe);
               var15 = var15 == 0L ? 1000L : var15;
               long var17 = var7 < var11 && var7 != 0L ? var7 : var11;
               long var19 = var9 > var13 ? var9 : var13;
               int var21 = (int)((var19 - var17) / var15);
               int var22 = 0;
               long var23 = 0L;
               int var25 = -1;
               int var26 = -1;
               VersatileData var27 = new VersatileData();
               if (var7 < var11 && var7 != 0L) {
                  ChartDef var28 = new ChartDef(
                     DataImportEngine.this.dataInfo.connection,
                     DataImportEngine.this.dataInfo.symbol,
                     DataImportEngine.this.dataInfo.timeframe,
                     var7,
                     var11,
                     2.5,
                     "No Session"
                  );
                  IDataLoader var29 = DataManager.getDataLoader(var28, 1);
                  var29.open();

                  while (var29.hasNextTick()) {
                     var29.getNextTick(var27);
                     var1.writeData(var27);
                     var26 = SQTime.getDateTime(var27.time);
                     if (var26 != var25) {
                        var23++;
                     }

                     var25 = var26;
                     var22++;
                     DataImportEngine.this.listener.onProgress((double)var22 / var21 * 100.0);
                     if (DataImportEngine.this.canceled) {
                        var29.close();
                        DataImportEngine.this.cancel(var1, var5);
                     }
                  }

                  var29.close();
               }

               DataImportEngine.this.csvLoader = new DataCsvLoader();
               DataCsvLoader.reset();
               DataImportEngine.this.csvLoader.setParams(DataImportEngine.this.importInfo, DataImportEngine.this.dataInfo, null);
               DataImportEngine.this.csvLoader.openFile();
               DataImportEngine.this.importInfo.resetLastAskBid();

               while (DataImportEngine.this.csvLoader.readData()) {
                  if (DataImportEngine.this.csvLoader.isDataCorrect()) {
                     DataImportEngine.this.convertToOriginalTimezone(DataImportEngine.this.csvLoader.tickData);
                     if (DataImportEngine.this.csvLoader.tickData.time > var2) {
                        var4 = 0;
                     } else {
                        if (DataImportEngine.this.csvLoader.tickData.time != var2) {
                           throw new Exception("This shouldn't happen - time on line is smaller than previous record!");
                        }

                        var4++;
                     }

                     var2 = DataImportEngine.this.csvLoader.tickData.time;
                     if (var4 > 0) {
                        DataImportEngine.this.csvLoader.tickData.time += var4;
                     }

                     var1.writeData(DataImportEngine.this.csvLoader.tickData);
                     DataImportEngine.this.rows++;
                     var22++;
                     DataImportEngine.this.listener.onProgress((double)var22 / var21 * 100.0);
                     if (DataImportEngine.this.canceled) {
                        DataImportEngine.this.csvLoader.close();
                        DataImportEngine.this.cancel(var1, var5);
                     }
                  }
               }

               DataImportEngine.this.csvLoader.close();
               DataCsvLoader.reset();
               if (var9 > var13) {
                  ChartDef var34 = new ChartDef(
                     DataImportEngine.this.dataInfo.connection,
                     DataImportEngine.this.dataInfo.symbol,
                     DataImportEngine.this.dataInfo.timeframe,
                     var13,
                     var9,
                     2.5,
                     "No Session"
                  );
                  IDataLoader var35 = DataManager.getDataLoader(var34, 1);
                  var35.open();

                  while (var35.hasNextTick()) {
                     var35.getNextTick(var27);
                     var1.writeData(var27);
                     var22++;
                     DataImportEngine.this.listener.onProgress((double)var22 / var21 * 100.0);
                     if (DataImportEngine.this.canceled) {
                        var35.close();
                        DataImportEngine.this.cancel(var1, var5);
                     }
                  }

                  var35.close();
               }

               DataManager.updateData(
                  DataImportEngine.this.dataInfo.connection,
                  DataImportEngine.this.dataInfo.symbol,
                  var17,
                  var19,
                  var22,
                  var23,
                  DataImportEngine.this.dataInfo.barTimeType,
                  DataImportEngine.this.importInfo.timeframe,
                  DataImportEngine.this.dataInfo.timezone
               );
               var1.close();
               DataManager.removeDataFiles(DataImportEngine.this.dataInfo.connection, DataImportEngine.this.dataInfo.symbol);
               new File(var6).renameTo(new File(var5));
               DataImportEngine.this.importRunning = false;
               DataImportEngine.this.listener.onProgress(100.0);
               DataImportEngine.this.listener.onFinish();
            } catch (Exception var31) {
               DataImportEngine.this.importRunning = false;

               try {
                  DataImportEngine.this.csvLoader.close();
                  if (var1 != null) {
                     var1.close();
                  }
               } catch (Exception var30) {
                  DataImportEngine.Log.error("Error while closing file loader/writer.", var30);
               }

               DataImportEngine.Log.error("Import error.", var31);
               DataImportEngine.this.listener.onError(var31.getMessage());
            }
         }
      };
      this.importThread.start();
   }

   private void cancel(DataBinWriterNew var1, String var2) throws Exception {
      var1.close();
      new File(var2).delete();
      new File(DataManager.getTempFileName(var2)).delete();
      throw new Exception("Import canceled");
   }

   private void checkDataFile() throws Exception {
      this.csvLoader = new DataCsvLoader();
      DataCsvLoader.reset();
      this.csvLoader.setParams(this.importInfo, this.dataInfo, null);
      this.listener.setStep(1);
      this.listener.setMessage(L.t("Checking file...", new Object[0]));
      this.importInfo = this.csvLoader.checkFile(this.listener);
      this.checkGaps();
      this.checkTimeframe();
   }

   protected void checkGaps() throws Exception {
      if (this.dataInfo.dateFrom != 0L && this.dataInfo.dateTo != 0L) {
         if (this.csvLoader.beginTimeNewFile != -1L && this.csvLoader.endTimeNewFile != -1L) {
            long var1 = 0L;
            long var3 = 0L;
            Log.debug("Db data date range: From " + new Date(this.dataInfo.dateFrom) + " to " + new Date(this.dataInfo.dateTo));
            Log.debug("Imported file date range: From " + new Date(this.csvLoader.beginTimeNewFile) + " to " + new Date(this.csvLoader.endTimeNewFile));
            if (this.csvLoader.endTimeNewFile < this.dataInfo.dateFrom) {
               var1 = this.csvLoader.endTimeNewFile / 86400000L;
               var3 = this.dataInfo.dateFrom > 0L ? this.dataInfo.dateFrom / 86400000L : var1;
            } else if (this.csvLoader.beginTimeNewFile > this.dataInfo.dateTo) {
               var1 = this.csvLoader.beginTimeNewFile / 86400000L;
               var3 = this.dataInfo.dateTo > 0L ? this.dataInfo.dateTo / 86400000L : var1;
            }

            if (var1 != 0L && Math.abs(var3 - var1) > 5L) {
               int var5 = (int)Math.abs(var3 - var1);
               this.listener
                  .onConfirm(
                     L.t(
                        "The imported data will create a gap of %d days if added to the current history data for this symbol. Do you really want to proceed with the import?",
                        new Object[]{var5}
                     )
                  );

               while (!this.confirmed && !this.canceled) {
                  Thread.sleep(50L);
               }

               if (this.canceled) {
                  throw new Exception("Import canceled");
               }
            }

            this.importInfo.beginTimeNewFile = this.csvLoader.beginTimeNewFile;
            this.importInfo.endTimeNewFile = this.csvLoader.endTimeNewFile;
         } else {
            throw new Exception(L.t("Exception during import: Cannot load first and last date, bad file format?", new Object[0]));
         }
      }
   }

   private void checkTimeframe() throws Exception {
      String var1 = this.importInfo.timeframe;
      if (var1 == null) {
         var1 = this.importFormatContainsBid() ? "TICK" : this.csvLoader.recognizeTimeframe();
      }

      if (var1 == null) {
         throw new Exception("Import file has unknown or unsupported timeframe!");
      }

      if (this.dataInfo.timeframe != null && !this.dataInfo.timeframe.equals(var1)) {
         throw new Exception(
            "Import file has different timeframe than the symbol where you want to import it!<br>Import file timeframe was recognized to "
               + var1
               + ", your symbol "
               + this.dataInfo.symbol
               + " has its base data in timeframe "
               + this.dataInfo.timeframe
               + " (loaded in some previous import).<br><br>It is not possible to import "
               + var1
               + " data to existing "
               + this.dataInfo.timeframe
               + " data. Please delete the existing data from this symbol or import it under a different symbol name."
         );
      }

      this.importInfo.timeframe = var1;
   }

   private boolean importFormatContainsBid() {
      for (int var1 = 0; var1 < this.importInfo.columnTypes.size(); var1++) {
         DefaultCol var2 = this.importInfo.columnTypes.get(var1);
         if (var2.getName().equals("Bid") || var2.getName().equals("Ask")) {
            return true;
         }
      }

      return false;
   }

   public static CustomDataFormat getFileFormat(String var0, ArrayList<CustomDataFormat> var1, CustomDataFormat var2) throws Exception {
      BufferedReader var3 = null;
      String var4 = null;
      String[] var5 = new String[25];
      Object var6 = null;
      Object var7 = null;
      String[] var8 = null;
      String[] var9 = null;
      int var10 = 0;

      try {
         File var11 = new File(var0);

         for (var3 = new BufferedReader(new FileReader(var11)); (var4 = var3.readLine()) != null && var10 != 25; var10++) {
            var4 = var4.trim().replaceAll(" +", " ");
            var5[var10] = var4;
         }

         ArrayList var12 = new ArrayList(var1);
         var12.add(0, var2);
         Collections.sort(var12, new Comparator<CustomDataFormat>() {
            public int compare(CustomDataFormat var1, CustomDataFormat var2x) {
               return var1.getOrder() - var2x.getOrder();
            }
         });

         for (CustomDataFormat var14 : var12) {
            try {
               var8 = new String[10];

               for (int var15 = 0; var15 < 10; var15++) {
                  var8[var15] = var5[var15 + var14.getSkipRows()];
               }

               var6 = var8[0].split(var14.getSeparator());
               if (var14.getName().equals("MetaTrader5 Tick Data")) {
                  var9 = new String[]{(String)((Object[])var6)[0], (String)((Object[])var6)[1], (String)((Object[])var6)[2], null};
                  if (((Object[])var6).length >= 4 && !((Object[])var6)[3].isBlank()) {
                     var9[3] = (String)((Object[])var6)[3];
                  } else {
                     var9[3] = "0";
                  }
               } else {
                  var9 = new String[((Object[])var6).length - var14.getSkipColumns()];

                  for (int var26 = var14.getSkipColumns(); var26 < ((Object[])var6).length; var26++) {
                     var9[var26 - var14.getSkipColumns()] = (String)((Object[])var6)[var26];
                  }
               }

               String var27 = CsvFileReader.findSeparator(var8, 0);
               if (!var14.getSeparator().equals(var27)) {
                  throw new Exception(String.format("The format uses different value separator '%s' as recognized '%s'.", var14.getSeparator(), var27));
               }

               ImportDataInfo var16 = new ImportDataInfo();
               var16.separator = var14.getSeparator();
               var16.skipRows = var14.getSkipRows();
               var16.skipCols = var14.getSkipColumns();
               var16.dateFormat = var14.getDateFormat();
               var16.timeFormat = var14.getTimeFormat();
               var16.columnTypes = new ArrayList<>(var14.getColumns().values());
               DataCsvLoader var17 = new DataCsvLoader();
               var17.parseLine(var9, var16);
               Log.info("Import history data - recognized file format '" + var14.getName() + "'.");
               return var14;
            } catch (Exception var18) {
               Log.debug("Import history data - cannot apply file format '" + var14.getName() + "'. Reason: " + var18.getMessage(), var18);
            }
         }

         var3.close();
         throw new Exception("Cannot apply any predefined file format.");
      } catch (Exception var19) {
         Log.error("Import history data. " + var19.getMessage(), var19);
         throw new Exception("Import history data. " + var19.getMessage(), var19);
      }
   }

   public static void waitUntilFinished() throws InterruptedException {
      instance.importThread.join();
   }

   public static int getRowsCount() {
      return instance.rows;
   }

   private void convertToOriginalTimezone(VersatileData var1) {
   }
}
