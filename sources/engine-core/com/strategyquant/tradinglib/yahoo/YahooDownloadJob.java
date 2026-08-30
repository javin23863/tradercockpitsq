package com.strategyquant.tradinglib.yahoo;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;

public class YahooDownloadJob extends GridJob<Void> {
   private YahooDownloader downloader = new YahooDownloader();
   private String jobId;
   private DataManagerProgressListener listener;
   private DataInfo dataInfo;
   private ImportInfo importInfo;

   public YahooDownloadJob(String var1, DataManagerProgressListener var2, DataInfo var3, ImportInfo var4) {
      super(var1, 0, null);
      this.jobId = var1;
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
   }

   public Void call() throws Exception {
      try {
         this.listener.setMessage(L.t("Starting Yahoo download...", new Object[]{true}));
         this.listener.onProgress(1.0);
         this.downloader.performDownload(this.jobId, this.listener, this.dataInfo, this.importInfo);
         this.listener.onProgress(100.0);
         this.listener.setMessage(this.downloader.isCanceled() ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      } catch (Exception var2) {
         this.listener.onError(var2.getMessage());
      }

      SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.dataInfo.symbol, "download"), "SQUANT", "QDM", "AlgoWizard");
      return null;
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.downloader.pause();
            break;
         case 3:
            this.downloader.restart();
            break;
         case 4:
            this.downloader.cancel();
      }
   }
}
