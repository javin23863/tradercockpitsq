package com.strategyquant.tradinglib.crypto;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.exchange.IExchange;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoDownloadJob extends GridJob<Void> {
   public static final Logger Log = LoggerFactory.getLogger(CryptoDownloadJob.class);
   private IExchange downloader;
   private String jobId;
   private DataManagerProgressListener listener;
   private DataInfo dataInfo;
   private ImportInfo importInfo;

   public CryptoDownloadJob(String var1, DataManagerProgressListener var2, DataInfo var3, ImportInfo var4) {
      super(var1, 0, null);
      this.jobId = var1;
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
   }

   public Void call() throws Exception {
      try {
         String var1 = this.dataInfo.uSymbolName;
         IExchange var2 = this.findExchangeByName(var1);
         if (var2 == null) {
            throw new Exception(L.t("Cannot find plugin for '%s'.", new Object[]{var1}));
         }

         this.listener.setMessage(L.t("Starting crypto download...", new Object[0]));
         this.listener.onProgress(1.0);
         this.downloader = var2.clone();
         this.downloader.performDownload(this.jobId, this.listener, this.dataInfo, this.importInfo);
         this.listener.onProgress(100.0);
         this.listener.setMessage(this.downloader.isCanceled() ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      } catch (Exception var6) {
         this.listener.onError(var6.getMessage());
      } finally {
         Log.debug("Download job for symbol:" + this.importInfo.symbol + " finished");
      }

      SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.dataInfo.symbol, "download"), "SQUANT", "QDM", "AlgoWizard");
      return null;
   }

   public void destroy() {
      super.destroy();
      this.listener.onProgress(100.0);
      this.listener.setMessage(L.t("Stopped", new Object[0]));
   }

   private IExchange findExchangeByName(String var1) {
      for (IExchange var3 : SQPluginManager.getPlugins(IExchange.class)) {
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

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
