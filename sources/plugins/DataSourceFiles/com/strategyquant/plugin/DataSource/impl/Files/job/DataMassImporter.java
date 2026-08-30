package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.MassImportDataInfo;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.MassImportDataInfo.OverwriteStrategy;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMassImporter {
   public static final Logger Log = LoggerFactory.getLogger(DataMassImporter.class);
   private static final String ACTION_LINK = "dataSourceDukascopy/importDataAction";
   private volatile boolean canceled;
   private MassImportDataInfo importInfo;
   private File file;
   private Set<String> existingSymbols;
   private MultiProgressListener progressListener;

   public DataMassImporter(MassImportDataInfo var1, File var2, Set<String> var3) {
      this.importInfo = var1;
      this.file = var2;
      this.existingSymbols = var3;
   }

   public void cancel() {
      this.canceled = true;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public String performImport() throws Exception {
      if (this.canceled) {
         return null;
      }

      String var1 = FilenameUtils.removeExtension(this.file.getName());
      var1 = var1 + this.importInfo.getPostfix();
      this.progressListener = DataManagerDataProgress.get().createListener(var1);
      this.progressListener.setMessage(L.t("Importing...", new Object[0]));
      this.progressListener.onProgress(0.2);
      DataInfo var2 = DataManager.getDataInfo(this.importInfo.getConnection(), var1);
      boolean var3 = var2 != null;
      if (var3 && this.importInfo.getOverwriteStrategy() == OverwriteStrategy.skip) {
         this.progressListener.setMessage(L.t("Skipped", new Object[0]));
         this.progressListener.onProgress(100.0);
         return var1 + " skipped";
      }

      boolean var4 = this.importInfo.getOverwriteStrategy() == OverwriteStrategy.overwrite;
      String var5 = var2 != null && !var4 ? this.generateTickerName(this.file) : var1;
      this.doImportFile(this.file, var5, this.importInfo);
      this.progressListener.setMessage(L.t("Completed", new Object[0]));
      this.progressListener.onProgress(100.0);

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var1, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var7) {
      }

      return var5;
   }

   private void doImportFile(File var1, String var2, MassImportDataInfo var3) throws Exception {
      DataManager.addData(var3.getConnection(), var2, var3.getInstrument(), var3.getBarType(), 1);
      List var4 = this.loadData(var1, var3);
      this.writeData(var2, var4);
   }

   private void writeData(String var1, List<VersatileData> var2) throws Exception {
      String var3 = this.importInfo.getConnection();
      int var4 = var2.size();
      if (var4 != 0) {
         if (this.canceled) {
            return;
         }

         DataInfo var5 = DataManager.getDataInfo(var3, var1);
         DataBinWriterNew var6 = null;
         if (this.importInfo.getTimeframe().equals("TICK")) {
            var6 = DataBinWriterNew.getInstance(2, MainApp.getDataPath(), var5.symbolInfo);
         } else {
            var6 = DataBinWriterNew.getInstance(1, MainApp.getDataPath(), var5.symbolInfo);
         }

         String var7 = DataManager.getDataFileName(var3, var1, this.importInfo.getTimeframe(), "No Session");
         String var8 = DataManager.getTempFileName(var7);
         var6.setFileName(var8);
         var6.open();

         for (VersatileData var10 : var2) {
            if (this.canceled) {
               break;
            }

            var6.writeData(var10);
         }

         long var17 = ((VersatileData)var2.get(0)).time;
         long var11 = ((VersatileData)var2.get(var4 - 1)).time;
         int var13 = SQTime.getDaysBetween(var17, var11) + 1;
         long var14 = var13 * 86400;
         DataManager.updateData(var3, var1, var17, var11, var4, var14, var5.barTimeType, this.importInfo.getTimeframe(), this.importInfo.getTimezone());
         var6.close();
         DataManager.removeDataFiles(var3, var1);
         new File(var8).renameTo(new File(var7));
      }
   }

   private String generateTickerName(File var1) throws Exception {
      String var2 = FilenameUtils.removeExtension(var1.getName());

      while (this.existingSymbols.contains(var2)) {
         var2 = DataManager.generateName(var2);
      }

      return var2;
   }

   private List<VersatileData> loadData(File var1, MassImportDataInfo var2) throws IOException {
      LinkedList var3 = new LinkedList();

      for (String var6 : Files.readAllLines(var1.toPath())) {
         if (this.canceled) {
            break;
         }

         var6 = var6.trim();
         if (!var6.isEmpty()) {
            String[] var7 = var6.split(",");
            if (!"Date".equals(var7[0])) {
               VersatileData var8 = new VersatileData();
               var8.time = SQTime.parseToMilis(var7[0], var2.getDateFormat());
               if (var2.getTimeframe().equals("TICK")) {
                  var8.ask = Double.valueOf(var7[1]);
                  var8.bid = Double.valueOf(var7[2]);
                  var8.volume = Double.valueOf(var7[3]);
               } else {
                  var8.open = Double.valueOf(var7[1]);
                  var8.high = Double.valueOf(var7[2]);
                  var8.low = Double.valueOf(var7[3]);
                  var8.close = Double.valueOf(var7[4]);
                  var8.volume = Double.valueOf(var7[5]);
               }

               var3.add(var8);
            }
         }
      }

      return var3;
   }
}
