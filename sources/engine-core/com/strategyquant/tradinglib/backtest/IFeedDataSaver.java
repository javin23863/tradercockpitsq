package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;

public interface IFeedDataSaver {
   void beforeSave(HashPair[] var1);

   void setParams(boolean var1, int var2);

   void saveTick(VersatileData var1) throws Exception;

   void afterSave();
}
