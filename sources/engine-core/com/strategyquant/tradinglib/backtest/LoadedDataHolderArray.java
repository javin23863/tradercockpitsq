package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.strategy.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedDataHolderArray implements ILoadedDataHolder, IFeedDataSaver {
   public static final Logger Log = LoggerFactory.getLogger("LoadedDataHolderArray");
   private static int NUMBER_OF_ARRAYS = 2000;
   private static int MEMORY_INITIALIZATION_CHUNKS = 1000000;
   private BacktestDataFeedElement[][] data = null;
   private int arraysCount = 0;
   private int dataCount = 0;
   private HashPair[] hashPairs;
   private volatile boolean isLoaded = false;
   private int index = 0;
   private int hash;

   public LoadedDataHolderArray(int var1, ILoadedDataHolder var2, DataLoadStats var3, LoadDataProgressEngine var4) throws Exception {
      this.hash = var1;
      this.hashPairs = var2.getHashPairs();
      this.loadDataToArray(var2, var3);
   }

   public LoadedDataHolderArray(int var1) {
      this.hash = var1;
   }

   private void loadDataToArray(ILoadedDataHolder var1, DataLoadStats var2) throws Exception {
      long var3 = System.currentTimeMillis();
      this.beforeSave(this.hashPairs);
      VersatileData var5 = new VersatileData();

      while (true) {
         var5 = var1.getNextTick(var5);
         if (var5 == null) {
            var2.loadingTime = System.currentTimeMillis() - var3;
            Log.info("Data loaded ARRAY memory in " + var2.loadingTime + " ms.");
            return;
         }

         this.saveTick(var5);
      }
   }

   @Override
   public void beforeSave(HashPair[] var1) {
      this.isLoaded = false;
      this.data = new BacktestDataFeedElement[NUMBER_OF_ARRAYS][];
      this.data[this.arraysCount++] = new BacktestDataFeedElement[MEMORY_INITIALIZATION_CHUNKS];
      this.index = 0;
   }

   @Override
   public void afterSave() {
   }

   @Override
   public ILoadedDataHolder cloneHolder() {
      LoadedDataHolderArray var1 = new LoadedDataHolderArray(this.hash);
      var1.data = this.data;
      var1.arraysCount = this.arraysCount;
      var1.dataCount = this.dataCount;
      var1.isLoaded = this.isLoaded;
      var1.index = 0;
      var1.hashPairs = this.hashPairs;
      return var1;
   }

   @Override
   public void saveTick(VersatileData var1) throws Exception {
      if (var1.type == 1) {
         this.saveTickData(var1.time, var1.connectionHash, var1.symbolHash, var1.bid, var1.ask, var1.volume, var1.sessionStartTime);
      } else {
         this.saveMinuteData(var1.time, var1.connectionHash, var1.symbolHash, var1.open, var1.high, var1.low, var1.close, var1.volume, var1.sessionStartTime);
      }
   }

   private void saveTickData(long var1, int var3, int var4, double var5, double var7, double var9, long var11) throws Exception {
      int var13 = this.dataCount / MEMORY_INITIALIZATION_CHUNKS;
      int var14 = this.dataCount - var13 * MEMORY_INITIALIZATION_CHUNKS;
      if (this.arraysCount <= var13) {
         this.data[this.arraysCount++] = new BacktestDataFeedElement[MEMORY_INITIALIZATION_CHUNKS];
      }

      this.data[var13][var14] = new BacktestDataFeedElement();
      this.data[var13][var14].setTickData(var1, this.getHashPairIndex(var3, var4), var5, var7, var9, var11);
      this.dataCount++;
   }

   private void saveMinuteData(long var1, int var3, int var4, double var5, double var7, double var9, double var11, double var13, long var15) throws Exception {
      int var17 = this.dataCount / MEMORY_INITIALIZATION_CHUNKS;
      int var18 = this.dataCount - var17 * MEMORY_INITIALIZATION_CHUNKS;
      if (this.arraysCount <= var17) {
         this.data[this.arraysCount++] = new BacktestDataFeedElement[MEMORY_INITIALIZATION_CHUNKS];
      }

      this.data[var17][var18] = new BacktestDataFeedElement();
      this.data[var17][var18].setOhlcData(var1, this.getHashPairIndex(var3, var4), var5, var7, var9, var11, var13, var15);
      this.dataCount++;
   }

   private byte getHashPairIndex(int var1, int var2) throws Exception {
      for (byte var3 = 0; var3 < this.hashPairs.length; var3++) {
         if (this.hashPairs[var3].connectionHash == var1 && this.hashPairs[var3].symbolHash == var2) {
            return var3;
         }
      }

      throw new Exception(L.t("HashPair for %d / %d was not found!", new Object[]{var1, var2}));
   }

   @Override
   public VersatileData getNextTick(VersatileData var1) throws Exception {
      if (this.index >= this.dataCount) {
         return null;
      }

      int var2 = this.index / MEMORY_INITIALIZATION_CHUNKS;
      int var3 = this.index - var2 * MEMORY_INITIALIZATION_CHUNKS;
      BacktestDataFeedElement var4 = this.data[var2][var3];
      var1.time = var4.getTime();
      var1.connectionHash = this.hashPairs[var4.getHashPairIndex()].connectionHash;
      var1.symbolHash = this.hashPairs[var4.getHashPairIndex()].symbolHash;
      if (var4.getType() == 2) {
         var1.type = 2;
         var1.open = var4.getOpen();
         var1.high = var4.getHigh();
         var1.low = var4.getLow();
         var1.close = var4.getClose();
         var1.volume = var4.getVolume();
         var1.sessionStartTime = var4.getSessionStartTime();
      } else {
         var1.type = 1;
         var1.bid = var4.getBid();
         var1.ask = var4.getAsk();
         var1.volume = var4.getVolume();
         var1.sessionStartTime = var4.getSessionStartTime();
      }

      this.index++;
      return var1;
   }

   @Override
   public long size() {
      return this.dataCount;
   }

   @Override
   public void afterProduce() {
   }

   @Override
   public void setParams(boolean var1, int var2) {
   }

   @Override
   public HashPair[] getHashPairs() {
      return null;
   }

   @Override
   public void destroy() {
      this.data = null;
   }

   @Override
   public void setMarketData(MarketData[] var1) {
   }

   @Override
   public MarketData[] getMarketData() {
      return null;
   }

   @Override
   public long freeIfInMemory() {
      return 0L;
   }
}
