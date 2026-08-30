package com.strategyquant.plugin.DataSource.impl.Files.job;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;

public class DataImporterJob extends GridJob<Void> {
   private DataImporter dataImporter = new DataImporter();
   private IProgressListener listener;
   private DataInfo dataInfo;
   private ImportDataInfo importInfo;
   private String symbol;

   public DataImporterJob(String var1, String var2, IProgressListener var3, DataInfo var4, ImportDataInfo var5) {
      super(var2, 0, null);
      this.listener = var3;
      this.dataInfo = var4;
      this.importInfo = var5;
      this.symbol = var1;
   }

   public Void call() throws Exception {
      try {
         this.listener.setMessage(L.t("Starting file import...", new Object[0]));
         this.listener.onProgress(1.0);
         this.dataImporter.performImport(this.listener, this.dataInfo, this.importInfo);
         this.listener.onProgress(100.0);
         this.listener.setMessage(this.dataImporter.isCanceled() ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      } catch (Exception var5) {
         this.listener.onError(var5.getMessage());
      } finally {
         this.sendRefreshMessage();
         DataManagerDataProgress.get().removeListener(this.symbol);
      }

      return null;
   }

   private void sendRefreshMessage() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.dataInfo.symbol, "import"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var2) {
      }
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.dataImporter.pause();
            break;
         case 3:
            this.dataImporter.restart();
            break;
         case 4:
            this.dataImporter.cancel();
      }
   }
}
