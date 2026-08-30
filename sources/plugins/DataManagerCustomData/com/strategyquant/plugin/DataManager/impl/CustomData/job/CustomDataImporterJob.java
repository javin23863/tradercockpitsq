package com.strategyquant.plugin.DataManager.impl.CustomData.job;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.data.io.ImportDataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;

public class CustomDataImporterJob extends GridJob<Void> {
   private CustomDataImporter dataImporter = new CustomDataImporter();
   private IProgressListener listener;
   private CustomDataInfo dataInfo;
   private ImportDataInfo importInfo;

   public CustomDataImporterJob(String var1, IProgressListener var2, CustomDataInfo var3, ImportDataInfo var4) {
      super(var1, 0, null);
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
   }

   public Void call() throws Exception {
      try {
         this.listener.setMessage(L.t("Starting custom data import...", new Object[0]));
         this.listener.onProgress(1.0);
         this.dataImporter.performImport(this.listener, this.dataInfo, this.importInfo);
         this.listener.onProgress(100.0);
         this.listener.setMessage(this.dataImporter.isCanceled() ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      } catch (Exception var2) {
         this.listener.onError(var2.getMessage());
      }

      this.sendRefreshMessage();
      return null;
   }

   private void sendRefreshMessage() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getCustomData(this.dataInfo.name, "import"), new String[]{"SQUANT", "QDM"});
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
