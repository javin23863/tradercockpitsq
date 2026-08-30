package com.strategyquant.tradinglib.backtestrunner;

public interface IBacktestProgressListener {
   void setProgress(int var1);

   void increaseProgressStep();
}
