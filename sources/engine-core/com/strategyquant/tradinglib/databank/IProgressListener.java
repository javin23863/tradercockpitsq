package com.strategyquant.tradinglib.databank;

public interface IProgressListener {
   void onProgress(double var1);

   void onDone();

   void onError(double var1, String var3);
}
