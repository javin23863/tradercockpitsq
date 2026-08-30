package com.strategyquant.datalib.data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DataExporter {
   private static final DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyy_MM_dd");
   private long fromDate;
   private long toDate;
   private DataBinReaderNew reader;
   private String folder;
   private String symbol;
   private int dateType;
   private DataInfo info;
   private String tmpFolder;

   public void exportTick(String var1, long var2, long var4, String var6) throws Exception {
      DataInfo var7 = DataManager.getDataInfo("History", var1);
      this.folder = var6 + "//" + var7.instrument;
      this.symbol = var1;
      File var8 = new File(this.folder);
      var8.mkdirs();
      this.fromDate = var2 > 0L ? SQTime.correctDayStart(var2) : 0L;
      this.toDate = var4 > 0L ? SQTime.correctDayEnd(var4) : 0L;
      this.prepareReader(var1, "TICK");
      this.performExport();
   }

   public void exportM1(String var1, long var2, long var4, String var6) throws Exception {
      DataInfo var7 = DataManager.getDataInfo("History", var1);
      this.folder = var6 + "//" + var7.instrument;
      this.symbol = var1;
      File var8 = new File(this.folder);
      var8.mkdirs();
      this.fromDate = var2 > 0L ? SQTime.correctDayStart(var2) : 0L;
      this.toDate = var4 > 0L ? SQTime.correctDayEnd(var4) : 0L;
      this.prepareReader(var1, "M1");
      this.performExport();
   }

   private void performExport() throws Exception {
      Path var1 = Files.createTempDirectory("sq4_export");
      this.tmpFolder = var1.toString();
      DataBinWriterNew var2 = null;
      int var3 = -1;

      while (this.reader.readData()) {
         VersatileData var4 = this.reader.tickData;
         long var5 = var4.time;
         if (this.toDate > 0L && var5 > this.toDate) {
            break;
         }

         if (this.fromDate == 0L || var5 >= this.fromDate) {
            int var7 = SQTime.getDayOfYear(var5);
            if (var7 != var3) {
               if (var2 != null && var2 != null) {
                  this.close(var2);
               }

               var2 = this.prepareWriter(formaterDate.print(var5) + ".dat", this.dateType);
               var3 = var7;
            }

            var2.writeData(var4);
         }
      }

      if (var2 != null) {
         this.close(var2);
      }

      this.reader.closeFile();
      this.deleteFolder(var1.toFile());
   }

   private void deleteFolder(File var1) {
      String[] var2 = var1.list();

      for (String var6 : var2) {
         File var7 = new File(var1.getPath(), var6);
         var7.delete();
      }

      var1.delete();
   }

   private void close(DataBinWriterNew var1) throws Exception {
      var1.close();
      String var2 = var1.getFileName();
      String var3 = new File(var2).getName();
      int var4 = var3.length();
      var3 = var3.substring(0, var4 - 4);
      FileOutputStream var5 = new FileOutputStream(this.folder + "//" + var3 + ".zip");
      ZipOutputStream var6 = new ZipOutputStream(var5);
      ZipEntry var7 = new ZipEntry(var3 + ".dat");
      var6.putNextEntry(var7);
      FileInputStream var8 = new FileInputStream(var2);
      byte[] var10 = new byte[2048];

      int var9;
      while ((var9 = var8.read(var10)) > 0) {
         var6.write(var10, 0, var9);
      }

      var8.close();
      var6.closeEntry();
      var6.close();
   }

   private DataBinWriterNew prepareWriter(String var1, int var2) throws Exception {
      DataBinWriterNew var3 = DataBinWriterNew.getInstance(var2, MainApp.getDataPath(), this.info.symbolInfo);
      var3.setFileName(this.tmpFolder + "/" + var1);
      var3.open();
      return var3;
   }

   private void prepareReader(String var1, String var2) throws Exception {
      String var3 = DataManager.getDataFileName("History", var1, var2, "No Session");
      this.dateType = var2.equals("TICK") ? 2 : 1;
      this.info = DataManager.getDataInfo("History", var1);
      this.reader = DataBinReaderNew.getInstance(this.dateType, this.info.symbolInfo);
      this.reader.setFileName(var3);
      this.reader.openFile();
      DataManager.checkDataExport(this.reader, this.info.source);
   }
}
