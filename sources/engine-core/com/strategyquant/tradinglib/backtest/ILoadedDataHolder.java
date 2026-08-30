package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.tradinglib.strategy.MarketData;

public interface ILoadedDataHolder {
   VersatileData getNextTick(VersatileData var1) throws Exception;

   long size();

   ILoadedDataHolder cloneHolder() throws Exception;

   void beforeSave(HashPair[] var1) throws Exception;

   void afterSave() throws Exception;

   void saveTick(VersatileData var1) throws Exception;

   void afterProduce();

   void setParams(boolean var1, int var2);

   HashPair[] getHashPairs();

   void destroy();

   void setMarketData(MarketData[] var1);

   MarketData[] getMarketData();

   long freeIfInMemory();
}
