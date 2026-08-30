package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.swing.JProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCsvLoader extends AbstractDataCsvLoader {
   public static final Logger Log = LoggerFactory.getLogger(DataCsvLoader.class);
   public long beginTimeNewFile = -1L;
   public long endTimeNewFile = -1L;
   private double averagePriceValue = 0.0;
   public final VersatileData tickData = new VersatileData();
   private String currentLine;

   public DataCsvLoader() {
      this.df = null;
      this.timeColumn = 0;
      this.isTimeCol = false;
   }

   public void setParams(ImportDataInfo var1, DataInfo var2, JProgressBar var3) {
      this.importInfo = var1;
      this.progressBar = var3;
      this.dataInfo = var2;
   }

   public ImportDataInfo checkFile(IProgressListener var1) throws Exception {
      this.beginTimeNewFile = -1L;
      this.listener = var1;
      this.loadedRows = 0;
      long var2 = new File(this.importInfo.filePath).length();
      this.previousTime = 0L;
      this.progressPercent = 0.0;
      this.cancel = false;
      this.openFile();
      String var4 = null;
      String[] var5 = null;
      int var6 = this.importInfo.columnTypes.size();
      String[] var7 = new String[var6];
      this.tfRecognizer.reset();
      boolean var9 = true;
      boolean var10 = false;
      this.importInfo.resetLastAskBid();
      long var11 = 0L;

      while (!this.cancel) {
         this.checkPaused();
         if (this.rows == 0 && this.importInfo.skipRows > 0) {
            for (int var13 = 0; var13 < this.importInfo.skipRows; var13++) {
               String var14 = this.readLine();
               var11 += var14.length();
            }
         }

         var4 = this.readLine();
         if (var9) {
            var10 = var4.replaceAll("( +)", " ").trim().length() < var4.length();
            var9 = false;
         }

         if (var4 == null) {
            break;
         }

         var11 += var4.length();
         if (var1 != null) {
            this.progressPercent = (double)var11 / var2 * 100.0;
            var1.onProgress(this.progressPercent);
            var1.setMessage("Checking row #" + this.loadedRows);
         }

         if (var10) {
            var4 = var4.replaceAll("( +)", " ").trim();
         }

         try {
            var5 = var4.split(this.importInfo.separator);
            if (this.importInfo.isMT5TickImport()) {
               var5 = this.importInfo.correctMT5TickData(var5);
               if (var5 == null) {
                  continue;
               }
            }

            this.loadedRows++;
            if (this.importInfo.skipCols > 0) {
               if (var5.length - this.importInfo.skipCols <= 0) {
                  throw new Exception("There are less columns than specified in Skip Columns parameter!");
               }

               if (var5.length - this.importInfo.skipCols < var6) {
                  throw new Exception(
                     String.format("There are not enough data columns in the file! Found: %d, Required: %d", var5.length - this.importInfo.skipCols, var6)
                  );
               }

               var7 = Arrays.copyOfRange(var5, this.importInfo.skipCols, var5.length);
            } else {
               var7 = var5;
            }

            Object[] var8 = this.parseLine(var7, this.importInfo);
            this.parseTickData(var8);
            if (this.loadedRows < 100 && this.tickData.ask != 0.0 && this.tickData.bid != 0.0 && this.tickData.bid > this.tickData.ask) {
               throw new Exception("Bid cannot be bigger than Ask, please check your columns!");
            }

            if (this.tickData.time == Long.MIN_VALUE) {
               throw new Exception("Cannot recognize date and time!");
            }

            if (this.tickData.time > 0L && this.tickData.time < this.previousTime) {
               if (this.importInfo.errorHandling != 1) {
                  throw new Exception("Invalid time consecution. Each row must have same or latter time than the previous one");
               }

               Log.error(
                  String.format(
                     "Error parsing on row: %d, line contents: %s, message: %s",
                     this.rows,
                     var4,
                     "Invalid time consecution. Each row must have same or latter time than the previous one"
                  )
               );
            }

            if (this.loadedRows < 100000) {
               this.tfRecognizer.processTime(this.tickData.time);
            }

            this.previousTime = this.tickData.time;
            if (this.beginTimeNewFile == -1L) {
               this.beginTimeNewFile = this.tickData.time;
            }

            this.endTimeNewFile = this.tickData.time;
         } catch (Exception var15) {
            if (this.importInfo.errorHandling != 1) {
               throw new Exception(String.format("Error parsing on row: %d, line contents: %s, message: %s", this.loadedRows, var4, var15.getMessage()), var15);
            }

            Log.debug(String.format("[checking_data] Skipping row %d, line: %s, reason: %s", this.loadedRows, var4, var15.getMessage()));
         }
      }

      if (this.cancel && var1 != null) {
         throw new Exception("File check canceled");
      }

      this.close();
      if (this.beginTimeNewFile > this.endTimeNewFile) {
         long var19 = this.beginTimeNewFile;
         this.beginTimeNewFile = this.endTimeNewFile;
         this.endTimeNewFile = var19;
         this.reverseFile(this.importInfo);
      }

      this.importInfo.importFileRows = this.rows;
      this.importInfo.beginTimeNewFile = this.beginTimeNewFile;
      this.importInfo.endTimeNewFile = this.endTimeNewFile;
      Log.info("Imported " + this.loadedRows + " rows");
      return this.importInfo;
   }

   private ImportDataInfo reverseFile(ImportDataInfo var1) throws Exception {
      Log.info("File in reverse order, reversing to temporary file");
      String var2 = MainApp.getDataPath() + "/internal/tmp/" + UUID.randomUUID().toString() + ".txt";
      this.openFile();
      Object var3 = null;
      ArrayList var4 = new ArrayList();

      while (true) {
         if (this.rows == 0 && var1.skipRows > 0) {
            for (int var5 = 0; var5 < var1.skipRows; var5++) {
               this.readLine();
            }
         }

         var3 = this.readLine();
         if (this.progressBar != null) {
            this.progressBar.setValue(this.rows);
         }

         if (var3 == null) {
            this.close();
            Collections.reverse(var4);
            PrintWriter var9 = new PrintWriter(new BufferedWriter(new FileWriter(var2, StandardCharsets.UTF_8)));

            for (String var7 : var4) {
               var9.println(var7);
            }

            var9.close();
            var1.filePath = var2;
            var1.skipRows = 0;
            var1.reversedFile = true;
            return var1;
         }

         this.loadedRows++;
         var4.add(var3);
      }
   }

   public int getLoadedRows() {
      return this.loadedRows;
   }

   protected void parseTickData(Object[] var1) throws Exception {
      this.tickData.reset();

      for (int var2 = 0; var2 < this.importInfo.columnTypes.size(); var2++) {
         DefaultCol var3 = this.importInfo.columnTypes.get(var2);
         switch (var3.getDataType()) {
            case 1:
               this.tickData.volume = this.tickData.volume + ((Double)var1[var2]).intValue();
               break;
            case 2:
               this.tickData.ask = (Double)var1[var2];
               break;
            case 3:
               this.tickData.bid = (Double)var1[var2];
               break;
            case 4:
               this.tickData.open = (Double)var1[var2];
               break;
            case 5:
               this.tickData.high = (Double)var1[var2];
               break;
            case 6:
               this.tickData.low = (Double)var1[var2];
               break;
            case 7:
               this.tickData.close = (Double)var1[var2];
               break;
            case 8:
               throw new Exception("Cannot have indicator value in data file!");
            case 9:
               this.tickData.time = (Long)var1[var2];
         }
      }
   }

   @Override
   public boolean readData() throws Exception {
      if (this.rows == 0 && this.importInfo.skipRows > 0) {
         for (int var2 = 0; var2 < this.importInfo.skipRows; var2++) {
            this.readLine();
         }
      }

      String var1 = this.reader.readLine();
      this.currentLine = var1;
      if (var1 != null && !var1.equals("")) {
         this.loadedRows++;
         Object[] var8 = null;
         int var3 = this.importInfo.columnTypes.size();
         String[] var4 = new String[var3];

         try {
            var8 = var1.split(this.importInfo.separator);
            if (this.importInfo.skipCols > 0) {
               if (var8.length - this.importInfo.skipCols <= 0) {
                  throw new Exception("There are less columns than specified in Skip Columns parameter!");
               }

               if (var8.length - this.importInfo.skipCols < var3) {
                  throw new Exception(
                     String.format("There are not enough data columns in the file! Found: %d, Required: %d", var8.length - this.importInfo.skipCols, var3)
                  );
               }

               var4 = Arrays.copyOfRange(var8, this.importInfo.skipCols, var8.length);
            } else {
               var4 = var8;
            }

            if (this.importInfo.isMT5TickImport()) {
               var4 = this.importInfo.correctMT5TickData(var4);
               if (var4 == null) {
                  this.tickData.reset();
                  return true;
               }
            }

            Object[] var5 = this.parseLine(var4, this.importInfo);
            this.parseTickData(var5);
            if (this.isDataCorrect()) {
               this.computeAveragePriceValue();
            }

            if (this.tickData.volume == 0.0) {
               this.tickData.volume = 1.0;
            }
         } catch (Exception var7) {
            if (this.importInfo.errorHandling != 1) {
               throw new Exception(String.format("Error parsing on line: %d, message: %s", this.loadedRows, var7.getMessage()));
            }

            Log.info(String.format("[loading_data] Skipping row %d, line: %s, reason: %s", this.loadedRows, var1, var7.getMessage()));
            this.tickData.time = Long.MIN_VALUE;
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void openFile() throws Exception {
      super.openFile();
      this.averagePriceValue = 0.0;
      this.previousTime = 0L;
   }

   private void computeAveragePriceValue() {
      double var1 = 0.0;
      int var3 = 0;
      if (this.tickData.ask != 0.0) {
         var1 += this.tickData.ask;
         var3++;
      }

      if (this.tickData.bid != 0.0) {
         var1 += this.tickData.bid;
         var3++;
      }

      if (this.tickData.open != 0.0) {
         var1 += this.tickData.open;
         var3++;
      }

      if (this.tickData.high != 0.0) {
         var1 += this.tickData.high;
         var3++;
      }

      if (this.tickData.low != 0.0) {
         var1 += this.tickData.low;
         var3++;
      }

      if (this.tickData.close != 0.0) {
         var1 += this.tickData.close;
         var3++;
      }

      this.averagePriceValue = var1 / var3;
   }

   public boolean isDataCorrect() {
      if (this.tickData.time == Long.MIN_VALUE) {
         return false;
      }

      if (!(this.averagePriceValue <= 0.0)
         && !(this.tickData.open < 0.0)
         && !(this.tickData.high < 0.0)
         && !(this.tickData.low < 0.0)
         && !(this.tickData.close < 0.0)) {
         if (!(this.tickData.ask > 0.0) || !(this.tickData.ask < this.averagePriceValue / 50.0) && !(this.tickData.ask > 50.0 * this.averagePriceValue)) {
            if (!(this.tickData.bid > 0.0) || !(this.tickData.bid < this.averagePriceValue / 50.0) && !(this.tickData.bid > 50.0 * this.averagePriceValue)) {
               if (!(this.tickData.open > 0.0)
                  || !(this.tickData.open < this.averagePriceValue / 50.0) && !(this.tickData.open > 50.0 * this.averagePriceValue)) {
                  if (!(this.tickData.high > 0.0)
                     || !(this.tickData.high < this.averagePriceValue / 50.0) && !(this.tickData.high > 50.0 * this.averagePriceValue)) {
                     return !(this.tickData.low > 0.0)
                           || !(this.tickData.low < this.averagePriceValue / 50.0) && !(this.tickData.low > 50.0 * this.averagePriceValue)
                        ? !(this.tickData.close > 0.0)
                           || !(this.tickData.close < this.averagePriceValue / 50.0) && !(this.tickData.close > 50.0 * this.averagePriceValue)
                        : false;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   public int getIndicatorsCount() {
      return 0;
   }

   public static void reset() {
   }

   public String getCurrentLine() {
      return this.currentLine;
   }

   public double getAveragePriceValue() {
      return this.averagePriceValue;
   }
}
