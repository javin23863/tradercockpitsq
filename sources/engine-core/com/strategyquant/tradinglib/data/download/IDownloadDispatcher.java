package com.strategyquant.tradinglib.data.download;

public interface IDownloadDispatcher {
   void extecuteDownloadJob(DownloadDispatcher.Details var1) throws Exception;

   boolean isCryptoExchangeDownload(String var1);

   boolean isMt5DownloadExecuted();

   void performStop(String var1, DownloadDispatcher.Details var2);

   void performPause(String var1, DownloadDispatcher.Details var2);

   void performResume(String var1, DownloadDispatcher.Details var2);
}
