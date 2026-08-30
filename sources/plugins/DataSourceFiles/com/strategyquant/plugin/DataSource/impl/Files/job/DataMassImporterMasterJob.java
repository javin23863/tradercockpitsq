package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.MassImportDataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.gridlib.compute.common.ExecuteOptions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMassImporterMasterJob extends GridJob<Void> {
   public static final Logger Log = LoggerFactory.getLogger(DataMassImporterMasterJob.class);
   private static final int MAX_IMPORT_COUNT_CONCURRENT = 50;
   private static final int BATCH_PROGRESS = 10;
   private MassImportDataInfo importInfo;
   private AtomicInteger finished = new AtomicInteger(0);
   private JSONObject progressJson = new JSONObject();
   private volatile boolean error;
   private int totalFiles;
   private boolean canceled;
   public static final String IMPORT_GROUP = "DataMassImportJobChildrenGroup";
   private static final String CHILD_JOB_PREFIX = "DataMassImportChildJob_";
   private List<String> symbols = new LinkedList<>();
   private File[] filesForImport;

   public DataMassImporterMasterJob(String var1, MassImportDataInfo var2) {
      super(var1, 0, null);
      this.importInfo = var2;
      this.filesForImport = this.readFiles(var2.getPath());
   }

   private synchronized void importFinished(String var1) {
      this.symbols.add(var1);
      if (this.finished.incrementAndGet() % 10 == 0) {
         this.updateProgress(L.t("Symbol %s imported", new Object[]{var1}));
      }
   }

   public boolean hasFilesToImport() {
      return this.filesForImport != null && this.filesForImport.length > 0;
   }

   public void updateProgress(String var1) {
      Integer var2 = SQUtils.round(100.0 * this.finished.doubleValue() / this.totalFiles);
      this.progressJson.put("percent", var2);
      if (var1 != null) {
         this.progressJson.put("info", var1);
      }

      DataManagerAddProgressSender.getInstance().sendData(this.progressJson);
   }

   private File[] readFiles(String var1) {
      File var2 = new File(var1);
      return !var2.exists() ? null : var2.listFiles(new FileFilter() {
         @Override
         public boolean accept(File var1) {
            return var1.isFile() && var1.getName().endsWith(".csv");
         }
      });
   }

   public Void call() throws Exception {
      Set var1 = DataManager.list().stream().map(var0 -> var0.symbol).collect(Collectors.toSet());
      this.canceled = false;
      this.registerListener();

      try {
         this.error = false;
         File[] var2 = this.readFiles(this.importInfo.getPath());
         if (!this.hasFilesToImport()) {
            return null;
         }

         this.totalFiles = var2.length;
         LinkedList var3 = new LinkedList();
         int var4 = 0;

         for (File var8 : var2) {
            var3.add(new DataMassImporterChildrenJob("DataMassImportChildJob_" + var8.getName(), var8, this.importInfo, var1));
            var4++;
         }

         ExecuteOptions var12 = new ExecuteOptions();
         var12.setMaxRunningCount(50);
         SQGrid.getGridClient().executeOnGrid("DataMassImportJobChildrenGroup", var3, var12);

         while (this.finished.get() < this.totalFiles && !this.canceled) {
         }

         String var13 = null;
         if (this.error) {
            this.progressJson.put("error", L.t("Some error occured during import. Check the log please.", new Object[0]));
         } else if (this.importInfo.isCreateStockGroup()) {
            var13 = this.createStockGroup();
         }

         BasketOfStocksManager.getInstance().updateGroupsOfSymbols();
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         this.progressJson.put("percent", 100.0);
         String var14 = var13 == null
            ? L.t("Symbols imported: %s", new Object[]{var4})
            : L.t("Symbols imported: %s and created stock group: %s", new Object[]{var4, var13});
         this.progressJson.put("message", var14);
         DataManagerAddProgressSender.getInstance().sendData(this.progressJson);
      } finally {
         this.unregisterListener();
      }

      return null;
   }

   private String createStockGroup() throws Exception {
      BasketDto var1 = new BasketDto();
      var1.setCount(this.symbols.size());
      var1.setTotal(this.symbols.size());
      var1.setName(this.getGroupName());
      BasketOfStocksManager.getInstance().saveGroup(var1);
      LinkedList var2 = new LinkedList();

      for (String var4 : this.symbols) {
         StockDto var5 = new StockDto();
         var5.setTicker(var4);
         var5.setDateFrom(-1L);
         var5.setDateTo(-1L);
         var2.add(var5);
      }

      BasketOfStocksManager.getInstance().saveStocks(var1.getId(), var2);
      SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      MultiProgressListener var6 = DataManagerDataProgress.get().createListener(var1.getName());
      var6.setMessage(L.t("Stockgroup created.", new Object[0]));
      var6.onProgress(100.0);
      DataManagerDataProgress.get().removeListener(var1.getName());
      return var1.getName();
   }

   private String getGroupName() {
      String var1 = new File(this.importInfo.getPath()).getName();
      String var2 = "[" + var1 + "]";
      int var3 = 0;

      while (BasketOfStocksManager.getInstance().getBasket(var2) != null) {
         var2 = "[" + var1 + ++var3 + "]";
      }

      return var2;
   }

   private void unregisterListener() {
      SQGrid.getGridClient().removeMessageListener("DataMassImportJobChildrenGroup");
   }

   private void registerListener() {
      SQGrid.getGridClient().registerMessageListener("DataMassImportJobChildrenGroup", new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            if (!DataMassImporterMasterJob.this.canceled) {
               if (var1.getMessageID() == 1) {
                  String var2 = var1.getJobDetails().getJobID();
                  String var3 = var2.substring("DataMassImportChildJob_".length());
                  String var4 = var3.substring(0, var3.length() - 4);
                  MultiProgressListener var5 = DataManagerDataProgress.get().createListener(var4);
                  if (!var1.getJobDetails().isSuccess()) {
                     DataMassImporterMasterJob.this.error = true;
                     var5.setMessage(L.t("Error during import file: %s, error: %s", new Object[]{var3, var1.getJobDetails().getException()}));
                  } else {
                     var5.setMessage(L.t("File imported", new Object[0]));
                  }

                  DataMassImporterMasterJob.this.importFinished((String)var1.getData());
                  var5.onProgress(100.0);
               }
            }
         }
      });
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 4:
            this.canceled = true;
            SQGrid.getGridClient().stop("DataMassImportJobChildrenGroup");
      }
   }
}
