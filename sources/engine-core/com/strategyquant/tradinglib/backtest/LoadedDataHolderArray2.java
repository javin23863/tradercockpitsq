package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedDataHolderArray2 implements ILoadedDataHolder {
   public static final Logger Log = LoggerFactory.getLogger("LoadedDataHolderArray");
   private VersatileData[] dataArray = null;
   private int hash;
   private long lastSessionStartTime = -1L;
   private HashPair[] hashPairs;
   private String fileName;
   private int totalRecords = 0;
   private long allocatedSize = 0L;
   private int readRecord = 0;
   private boolean isOhlc;
   private MarketData[] marketDatas = null;
   private long newAllocatedSize;

   public LoadedDataHolderArray2(int var1, LoadedDataHolderFileBDF var2, DataLoadStats var3, LoadDataProgressEngine var4) throws Exception {
      this.hash = var1;
      this.hashPairs = var2.getHashPairs();
      this.fileName = TestfilesUtils.getTestdataFile(var1, true);
      File var5 = new File(this.fileName);
      if (!var5.exists()) {
         throw new Exception(L.t("File %s doesnot exist!", new Object[]{this.fileName}));
      }

      try {
         this.loadFileToArray(var2, var3, var5.length(), var4);
      } catch (OutOfMemoryError var7) {
         Log.info("Cannot allocate enough memory for offheap data! Already alocated: {}, new size that failed: {}", this.allocatedSize, this.newAllocatedSize);
         if (var4 != null) {
            var4.printToLog(L.t("Cannot load data into memory, keeping it in file", new Object[0]));
         }

         this.freeAlocatedMemory();
         throw var7;
      }
   }

   private void freeAlocatedMemory() {
      this.dataArray = null;
   }

   public LoadedDataHolderArray2(int var1, VersatileData[] var2) {
      this.hash = var1;
   }

   private void loadFileToArray(LoadedDataHolderFileBDF var1, DataLoadStats var2, long var3, LoadDataProgressEngine var5) throws Exception {
      long var6 = System.currentTimeMillis();
      long var8 = var3 / 10L;
      this.dataArray = new VersatileData[(int)var8];
      VersatileData var10 = new VersatileData();
      this.totalRecords = 0;
      long var11 = 0L;

      while (true) {
         var10 = var1.getNextTick(var10);
         if (var10 == null) {
            var2.loadingTime = System.currentTimeMillis() - var6;
            Log.info("Data loaded from file to array in {} ms., records: {}", var2.loadingTime, this.totalRecords);
            Log.info("Latest time: {}", SQTime.toDateMinuteString(var11));
            if (Log.isDebugEnabled()) {
               Log.debug("Data loaded from file to array in {} ms., records: {}", var2.loadingTime, this.totalRecords);
               Log.debug("Latest time: {}", SQTime.toDateMinuteString(var11));
            }

            if (var5 != null) {
               double var13 = var2.loadingTime / 1000.0;
               var5.printToLog(L.t("Loaded to memory in %.4f s., memory: %s", new Object[]{var13, SQUtils.formatBytesToHumanFormat(this.allocatedSize)}));
            }

            return;
         }

         this.setParams(var10.type == 2, 0);
         this._saveTick(var10);
         if (var10.time > var11) {
            var11 = var10.time;
         }
      }
   }

   @Override
   public ILoadedDataHolder cloneHolder() throws Exception {
      LoadedDataHolderArray2 var1 = new LoadedDataHolderArray2(this.hash, this.dataArray);
      var1.dataArray = this.dataArray;
      var1.lastSessionStartTime = this.lastSessionStartTime;
      var1.hashPairs = this.hashPairs;
      var1.totalRecords = this.totalRecords;
      var1.allocatedSize = this.allocatedSize;
      var1.readRecord = 0;
      var1.marketDatas = this.marketDatas;
      return var1;
   }

   @Override
   public VersatileData getNextTick(VersatileData var1) throws Exception {
      if (this.readRecord >= this.totalRecords) {
         return null;
      }

      this.readRecord++;
      return this.dataArray[this.readRecord - 1];
   }

   @Override
   public long size() {
      return this.totalRecords;
   }

   @Override
   public void saveTick(VersatileData var1) throws Exception {
      throw new Exception(L.t("Not implemented here!", new Object[0]));
   }

   public void _saveTick(VersatileData var1) throws Exception {
      VersatileData var2 = new VersatileData();
      var2.copyFrom(var1);
      if (this.totalRecords >= this.dataArray.length) {
         throw new Exception(L.t("Records bigger than allocated array: %d >= %d ", new Object[]{this.totalRecords, this.dataArray.length}));
      }

      this.dataArray[this.totalRecords] = var2;
      this.totalRecords++;
   }

   @Override
   public void afterProduce() {
   }

   @Override
   public void setParams(boolean var1, int var2) {
      this.isOhlc = var1;
   }

   @Override
   public HashPair[] getHashPairs() {
      return this.hashPairs;
   }

   @Override
   public void destroy() {
      this.dataArray = null;
   }

   @Override
   public void setMarketData(MarketData[] var1) {
      this.marketDatas = var1;
   }

   @Override
   public MarketData[] getMarketData() {
      return this.marketDatas;
   }

   @Override
   public void beforeSave(HashPair[] var1) throws Exception {
      throw new Exception(L.t("Not implemented here!", new Object[0]));
   }

   @Override
   public void afterSave() throws Exception {
      throw new Exception(L.t("Not implemented here!", new Object[0]));
   }

   @Override
   public long freeIfInMemory() {
      return 0L;
   }
}
