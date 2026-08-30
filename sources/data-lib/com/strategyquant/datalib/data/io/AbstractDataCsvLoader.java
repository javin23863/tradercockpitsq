package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataCsvLoader {
   public static final Logger Log = LoggerFactory.getLogger(AbstractDataCsvLoader.class);
   public static final String InvalidTimeConsecution = "Invalid time consecution. Each row must have same or latter time than the previous one";
   protected ImportDataInfo importInfo;
   protected JProgressBar progressBar;
   protected DataInfo dataInfo;
   protected String fileName;
   protected SimpleDateFormat df = null;
   protected int timeColumn = 0;
   protected boolean isTimeCol;
   protected volatile boolean cancel = false;
   protected volatile boolean paused = false;
   protected IProgressListener listener;
   protected BufferedReader reader = null;
   protected int rows;
   public int loadedRows;
   protected TimeframeRecognizer tfRecognizer = new TimeframeRecognizer();
   protected long previousTime;
   protected double progressPercent = 0.0;

   public abstract boolean readData() throws Exception;

   public Object[] parseLine(String[] var1, ImportDataInfo var2) throws Exception {
      Object[] var3 = new Object[var1.length];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = null;
      }

      if (var1.length != var2.columnTypes.size()) {
         throw new Exception(
            "The number of columns in data file doesn't match!(CSV columns: " + var1.length + ", ImportDataInfo columns: " + var2.columnTypes.size()
         );
      }

      for (int var6 = 0; var6 < var2.columnTypes.size(); var6++) {
         DefaultCol var5 = var2.columnTypes.get(var6);
         switch (var5.getType()) {
            case 1:
               var3[var6] = Integer.parseInt(var1[var6]);
               break;
            case 2:
               var3[var6] = Long.parseLong(var1[var6]);
            case 3:
            case 7:
            case 8:
            default:
               break;
            case 4:
               var3[var6] = var1[var6];
               break;
            case 5:
               var3[var6] = parseDoubleSpecial(var1[var6]);
               break;
            case 6:
               var3[var6] = this.parseTime(var1[var6], var1, var2);
               break;
            case 9:
               var3[var6] = parseDoubleSpecial(var1[var6]);
               break;
            case 10:
               var3[var6] = this.parseTime(var1[var6], var1, var2);
         }
      }

      return var3;
   }

   public static int intValueOf(String var0) {
      int var1 = 0;
      int var2 = 0;
      boolean var4 = false;
      int var3;
      if (var0 != null && (var3 = var0.length()) != 0) {
         char var5;
         if ((var5 = var0.charAt(0)) < '0' || var5 > '9') {
            if (!(var4 = var5 == '-')) {
               throw new NumberFormatException(var0);
            }

            var2++;
            if (var2 == var3 || (var5 = var0.charAt(var2)) < '0' || var5 > '9') {
               throw new NumberFormatException(var0);
            }
         }

         while (true) {
            var1 += '0' - var5;
            if (++var2 == var3) {
               return var4 ? var1 : -var1;
            }

            if ((var5 = var0.charAt(var2)) < '0' || var5 > '9') {
               throw new NumberFormatException(var0);
            }

            var1 *= 10;
         }
      } else {
         throw new NumberFormatException(var0);
      }
   }

   private static double parseDoubleSpecial(String var0) throws Exception {
      Object var1 = null;

      try {
         return Double.parseDouble(var0);
      } catch (Exception var4) {
         try {
            return Double.parseDouble(var0.replace(",", "."));
         } catch (Exception var3) {
            throw var3;
         }
      }
   }

   private long parseTime(String var1, String[] var2, ImportDataInfo var3) throws Exception {
      if (!var3.dateFormat.equals("Long (millis from epoch)") && !var3.dateFormat.equals("Long (seconds from epoch)")) {
         try {
            if (this.df == null) {
               this.initializeDateFormat(var3);
            }

            if (this.isTimeCol) {
               var1 = var1 + " " + var2[this.timeColumn];
            }

            Date var4 = this.df.parse(var1);
            return var4.getTime();
         } catch (Exception var5) {
            throw new Exception(
               "Parsing DateTime failed! Date: " + var1 + ", Expected format: " + this.df.toPattern() + ", ImportDataInfo format: " + var3.dateFormat, var5
            );
         }
      } else {
         return this.parseLongTime(var1, var3.dateFormat);
      }
   }

   private long parseLongTime(String var1, String var2) throws Exception {
      try {
         long var3 = Long.parseLong(var1);
         return var2.equals("Long (seconds from epoch)") ? var3 * 1000L : var3;
      } catch (Exception var5) {
         throw new Exception("Parsing DateTime failed! Date: " + var1 + " - Incorrect value for Long format", var5);
      }
   }

   private void initializeDateFormat(ImportDataInfo var1) {
      this.df = new SimpleDateFormat();
      int var2 = 0;
      this.isTimeCol = false;

      for (var2 = 0; var2 < var1.columnTypes.size(); var2++) {
         DefaultCol var3 = var1.columnTypes.get(var2);
         if (var3.getType() == 7) {
            this.isTimeCol = true;
            break;
         }
      }

      this.timeColumn = var2;
      if (this.isTimeCol) {
         String var6 = var1.dateFormat;
         if (!var6.contains("HH")) {
            String var4 = var1.timeFormat;
            if (var4 == null) {
               var6 = var6 + " HH:mm";
            } else {
               var6 = var6 + " " + var4;
            }
         }

         this.df.applyPattern(var6);
      } else {
         var1.dateFormat = var1.dateFormat.replaceAll("hh", "HH");
         this.df.applyPattern(var1.dateFormat);
      }
   }

   public int countRows(String var1) {
      try {
         BufferedReader var2 = new BufferedReader(new FileReader(new File(var1)));
         String var3 = null;
         int var4 = 0;

         while (true) {
            try {
               var3 = var2.readLine();
            } catch (IOException var6) {
               Log.error("Exception", var6);
            }

            if (var3 == null) {
               var2.close();
               return var4;
            }

            var4++;
         }
      } catch (IOException var7) {
         Log.error("Exception", var7);
         return -1;
      }
   }

   public void openFile() throws Exception {
      try {
         this.reader = new BufferedReader(new FileReader(new File(this.importInfo.filePath)));
         this.rows = 0;
         this.tfRecognizer.reset();
      } catch (FileNotFoundException var2) {
         throw new Exception("File not found: " + this.importInfo.filePath);
      }
   }

   public void close() throws Exception {
      if (this.reader != null) {
         try {
            this.reader.close();
            this.reader = null;
         } catch (IOException var2) {
            throw new Exception("Cannot close file");
         }
      }
   }

   protected String readLine() throws Exception {
      try {
         this.rows++;
         String var1 = this.reader.readLine();
         if (var1 != null && var1.equalsIgnoreCase("")) {
            var1 = this.reader.readLine();
         }

         if (var1 != null && var1.equalsIgnoreCase("")) {
            var1 = this.reader.readLine();
         }

         return var1;
      } catch (IOException var2) {
         throw new Exception(String.format("Cannot read lines at line: %d", this.rows));
      }
   }

   public void cancel() {
      this.cancel = true;
   }

   public String recognizeTimeframe() {
      return this.tfRecognizer.getTimeframe();
   }

   public void pause() {
      this.paused = true;
   }

   public void restart() {
      this.paused = false;
   }

   protected void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.cancel) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }
}
