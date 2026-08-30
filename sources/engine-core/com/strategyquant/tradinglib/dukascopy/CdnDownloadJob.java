package com.strategyquant.tradinglib.dukascopy;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.tradinglib.historyData.DownloadMessageHandler;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CdnDownloadJob extends GridJob<DownloadResultDto> {
   private static final long serialVersionUID = 1L;
   private CdnCache cache;
   private int year;
   private int month;
   private int day;
   private DownloadMessageHandler failedHandler;
   private Future<DownloadResultDto> downloadFuture;
   private final ExecutorService executor = Executors.newSingleThreadExecutor();

   public CdnDownloadJob(String var1, CdnCache var2, int var3, int var4, int var5, int var6, DownloadMessageHandler var7) {
      super(var1, var6, null);
      this.cache = var2;
      this.year = var3;
      this.month = var4;
      this.day = var5;
      this.failedHandler = var7;
   }

   public DownloadResultDto call() throws Exception {
      try {
         this.downloadFuture = this.executor.submit(() -> this.cache.performLoad(this.year, this.month, this.day, this.failedHandler));
         return this.downloadFuture.get();
      } catch (CancellationException var2) {
         return null;
      }
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      if (var1.getMessageID() == 4) {
         this.cache.stop();
         this.downloadFuture.cancel(true);
      }
   }
}
