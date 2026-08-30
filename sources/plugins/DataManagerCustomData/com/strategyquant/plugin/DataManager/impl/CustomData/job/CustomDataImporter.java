package com.strategyquant.plugin.DataManager.impl.CustomData.job;

import com.strategyquant.datalib.customData.CustomDataBinWriter;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataImporter {
   public static final Logger Log = LoggerFactory.getLogger(CustomDataImporter.class);
   private volatile boolean canceled;
   private volatile boolean paused;
   CustomDataCsvLoader csvLoader = null;
   CustomDataBinWriter writer = null;
   private IProgressListener listener;

   public void performImport(IProgressListener var1, CustomDataInfo var2, ImportDataInfo var3) throws Exception {
      this.listener = var1;
      this.canceled = false;
      this.paused = false;

      try {
         var1.onStart();
         var1.setStep(1);
         var1.setMessage(L.t("Checking file...", new Object[0]));
         double var4 = this.checkDataFile(var1, var2, var3);
         var1.setStep(2);
         var1.setMessage(L.t("Importing file...", new Object[0]));
         this.csvLoader = new CustomDataCsvLoader(var1, var2, var3);
         this.csvLoader.openFile();
         this.writer = new CustomDataBinWriter();
         String var6 = CustomDataManager.getDataFileName(var2.name);
         String var7 = getTempFileName(var6);
         this.writer.setFileName(var7);
         this.writer.open();

         while (this.csvLoader.readData() && !this.canceled) {
            this.writer.writeData(this.csvLoader.loadedData);
            var1.onProgress(this.csvLoader.loadedRows / var4 * 100.0);
            var1.setMessage(L.t("Imported custom data %s", new Object[]{SQTime.toDateMinuteString(this.csvLoader.loadedData.time)}));
            this.checkPaused();
         }

         this.csvLoader.close();
         this.writer.close();
         String var8 = this.csvLoader.recognizeTimeframe();
         CustomDataManager.updateData(var2.name, this.csvLoader.fromDate, this.csvLoader.toDate, this.csvLoader.loadedRows, var8);
         CustomDataManager.removeDataFile(var2.name);
         new File(var7).renameTo(new File(var6));
      } catch (Exception var10) {
         try {
            this.csvLoader.close();
            if (this.writer != null) {
               this.writer.close();
            }
         } catch (Exception var9) {
            Log.error("Error while closing file loader/writer.", var9);
         }

         Log.error("Import error.", var10);
         throw new Exception("Import error." + var10.getMessage(), var10);
      }
   }

   public static String getTempFileName(String var0) {
      int var1 = var0.indexOf(".");
      String var2 = var0.substring(0, var0.indexOf("."));
      String var3 = var0.substring(var1);
      return var2 + "_temp" + var3;
   }

   private int checkDataFile(IProgressListener var1, CustomDataInfo var2, ImportDataInfo var3) throws Exception {
      int var4 = this.countRows(var3.filePath);
      int var5 = 0;

      for (int var6 = 0; var6 < var3.columnTypes.size(); var6++) {
         if (((DefaultCol)var3.columnTypes.get(var6)).getDataType() == 9999) {
            var5++;
         }
      }

      if (var2.values != var5) {
         throw new Exception(L.t("Wrong number of values(columns) %d <> %d.", new Object[]{var2.values, var5}));
      } else {
         return var4;
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
