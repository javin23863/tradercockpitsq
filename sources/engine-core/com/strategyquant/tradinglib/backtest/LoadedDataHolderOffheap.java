package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.MemoryInfo;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.memory.OffHeapMemory;
import com.strategyquant.lib.offheap.IOffHeapCallback;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.File;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedDataHolderOffheap implements ILoadedDataHolder {
   public static final Logger Log = LoggerFactory.getLogger("LoadedDataHolderOffheap");
   protected static final int MEGABYTE = 1048576;
   private long address = -2L;
   private int hash;
   private long lastSessionStartTime = -1L;
   private boolean lastOHLC;
   private HashPair[] hashPairs;
   private String fileName;
   private int records = 0;
   private long recordsSize = 0L;
   private long allocatedSize = 0L;
   private long readRecordSize = 0L;
   private boolean isOhlc;
   private MarketData[] marketDatas = null;
   private long newAllocatedSize;
   private IOffHeapCallback offheapCallback;

   public LoadedDataHolderOffheap(int var1, LoadedDataHolderFileBDF var2, DataLoadStats var3, LoadDataProgressEngine var4) throws Exception {
      this.hash = var1;
      this.hashPairs = var2.getHashPairs();
      this.offheapCallback = new IOffHeapCallback() {
         public void freeOffHeapMemory(boolean var1) {
            throw new IllegalArgumentException("Not yet implemented!");
         }

         public String getIdentification() {
            return "LoadedDataHolderOffheap";
         }
      };
      this.fileName = TestfilesUtils.getTestdataFile(var1);
      File var5 = new File(this.fileName);
      if (!var5.exists()) {
         throw new Exception(L.t("File %s does not exist!", new Object[]{this.fileName}));
      }

      try {
         this.loadFileToOffheapMemory(var2, var3, var5.length(), var4);
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
      Log.info("Deallocating LoadedDataHolderOffheap, stack: {}", SQUtils.getStackTrace());
      OffHeapMemory.deallocate(this.offheapCallback, this.address);
   }

   public LoadedDataHolderOffheap(int var1, long var2) {
      this.hash = var1;
   }

   private void loadFileToOffheapMemory(LoadedDataHolderFileBDF var1, DataLoadStats var2, long var3, LoadDataProgressEngine var5) throws Exception {
      long var6 = System.currentTimeMillis();
      long var8 = var3 / 10L;
      long var10 = var8 * 30L;
      this.address = OffHeapMemory.allocate(this.offheapCallback, var10);
      this.allocatedSize = var10;
      VersatileData var12 = new VersatileData();
      this.records = 0;
      long var13 = 0L;

      while (true) {
         var12 = var1.getNextTick(var12);
         if (var12 == null) {
            var2.loadingTime = System.currentTimeMillis() - var6;
            Log.debug(
               "Data loaded from file to offheap memory in {} ms., records: {}, recordsSize: {}, allocatedSize: {}",
               new Object[]{var2.loadingTime, this.records, this.recordsSize, this.allocatedSize}
            );
            if (Log.isDebugEnabled()) {
               Log.debug("Latest time: {}", SQTime.toDateMinuteString(var13));
            }

            if (var5 != null) {
               double var15 = var2.loadingTime / 1000.0;
               var5.printToLog(L.t("Loaded to memory in %.4f s., memory: %s", new Object[]{var15, SQUtils.formatBytesToHumanFormat(this.allocatedSize)}));
            }

            return;
         }

         this.setParams(var12.type == 2, 0);
         this._saveTick(var12);
         if (var12.time > var13) {
            var13 = var12.time;
         }
      }
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
   public ILoadedDataHolder cloneHolder() throws Exception {
      LoadedDataHolderOffheap var1 = new LoadedDataHolderOffheap(this.hash, this.address);
      this.checkOffHeap();
      var1.address = this.address;
      var1.lastSessionStartTime = this.lastSessionStartTime;
      var1.hashPairs = this.hashPairs;
      var1.address = this.address;
      var1.records = this.records;
      var1.recordsSize = this.recordsSize;
      var1.allocatedSize = this.allocatedSize;
      var1.readRecordSize = 0L;
      var1.marketDatas = this.marketDatas;
      var1.offheapCallback = this.offheapCallback;
      return var1;
   }

   @Override
   public VersatileData getNextTick(VersatileData var1) throws Exception {
      if (this.readRecordSize >= this.recordsSize) {
         return null;
      }

      byte var2 = this.getByte();
      int var3 = var2 >> 6;
      var1.type = (var2 & 48) >> 4;
      byte var4 = this.getByte();
      var1.connectionHash = this.hashPairs[var4].connectionHash;
      var1.symbolHash = this.hashPairs[var4].symbolHash;
      if (var3 == 0) {
         this.lastSessionStartTime = this.getLong();
         var1.sessionStartTime = this.lastSessionStartTime;
      } else {
         var1.sessionStartTime = this.lastSessionStartTime;
      }

      this.lastOHLC = var1.type == 2;
      if (var1.type == 1) {
         this.readTickData(var1);
      } else {
         this.readOHLCData(var1);
      }

      this.records++;
      return var1;
   }

   private void readTickData(VersatileData var1) {
      var1.time = this.getLong();
      var1.bid = this.getDouble();
      var1.ask = this.getDouble();
      var1.volume = this.getDouble();
   }

   private void readOHLCData(VersatileData var1) {
      var1.time = this.getLong();
      var1.open = this.getDouble();
      var1.high = this.getDouble();
      var1.low = this.getDouble();
      var1.close = this.getDouble();
      var1.volume = this.getDouble();
   }

   @Override
   public long size() {
      return this.recordsSize;
   }

   @Override
   public void saveTick(VersatileData var1) throws Exception {
      throw new Exception("Not implemented here!");
   }

   private void _saveTick(VersatileData var1) throws Exception {
      byte var2 = 0;
      var2 = (byte)(var2 | 16 * var1.type);
      boolean var3 = this.lastSessionStartTime == var1.sessionStartTime;
      var2 = (byte)(var2 | 64 * (var3 ? 1 : 0));
      this.putByte(var2);
      this.putByte(this.getHashPairIndex(var1.connectionHash, var1.symbolHash));
      if (!var3) {
         this.lastSessionStartTime = var1.sessionStartTime;
         this.putLong(var1.sessionStartTime);
      }

      if (this.isOhlc) {
         this.saveOHLCData(var1);
      } else {
         this.saveTickData(var1);
      }

      this.records++;
   }

   private void saveTickData(VersatileData var1) {
      this.putLong(var1.time);
      this.putDouble(var1.bid);
      this.putDouble(var1.ask);
      this.putDouble(var1.volume);
   }

   private void saveOHLCData(VersatileData var1) {
      this.putLong(var1.time);
      this.putDouble(var1.open);
      this.putDouble(var1.high);
      this.putDouble(var1.low);
      this.putDouble(var1.close);
      this.putDouble(var1.volume);
   }

   private void putByte(byte var1) {
      this.checkAllocatedSize();
      OffHeapMemory.putByte(this.address + this.recordsSize, var1);
      this.recordsSize++;
   }

   private void putLong(long var1) {
      this.checkAllocatedSize();
      OffHeapMemory.putLong(this.address + this.recordsSize, var1);
      this.recordsSize += 8L;
   }

   private void putDouble(double var1) {
      this.checkAllocatedSize();
      OffHeapMemory.putDouble(this.address + this.recordsSize, var1);
      this.recordsSize += 8L;
   }

   private void checkAllocatedSize() {
      if (this.recordsSize > this.allocatedSize - 100L) {
         this.newAllocatedSize = (long)(this.allocatedSize * 1.1);
         Log.debug("LoadedDataOfheap - Reallocating from {} to {}", this.allocatedSize, this.newAllocatedSize);
         this.address = OffHeapMemory.reallocate(this.offheapCallback, this.address, this.newAllocatedSize);
         this.allocatedSize = this.newAllocatedSize;
      }
   }

   private byte getByte() {
      byte var1 = OffHeapMemory.getByte(this.address + this.readRecordSize);
      this.readRecordSize++;
      return var1;
   }

   private long getLong() {
      long var1 = OffHeapMemory.getLong(this.address + this.readRecordSize);
      this.readRecordSize += 8L;
      return var1;
   }

   private double getDouble() {
      double var1 = OffHeapMemory.getDouble(this.address + this.readRecordSize);
      this.readRecordSize += 8L;
      return var1;
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
      Log.info("Destroying LoadedDataHolderOffheap, stack: {}", SQUtils.getStackTrace());
      OffHeapMemory.deallocate(this.offheapCallback, this.address);
      this.address = -1L;
   }

   public void checkOffHeap() {
      if (this.address < 0L) {
         OffHeapMemory.throwMemoryException(this.address);
      }
   }

   @Override
   public void setMarketData(MarketData[] var1) {
      this.marketDatas = var1;
   }

   @Override
   public MarketData[] getMarketData() {
      return this.marketDatas;
   }

   public static boolean isEnoughMemory(int var0, MemoryMXBean var1) {
      String var2 = TestfilesUtils.getTestdataFile(var0, false);
      File var3 = new File(var2);
      if (!var3.exists()) {
         return true;
      }

      long var4 = var3.length() / 10L;
      long var6 = var4 * 30L / 1048576L;
      MemoryUsage var8 = var1.getHeapMemoryUsage();
      long var9 = var8.getMax() / 1048576L;
      MemoryInfo var11 = new MemoryInfo();
      OffHeapMemory.getInfo(var11);
      long var12 = var11.allocatedMemory / 1048576L;
      long var14 = var8.getCommitted() / 1048576L;
      long var16 = var9 - (var12 + var14);
      return !(var6 > var16 * 0.7);
   }

   @Override
   public long freeIfInMemory() {
      Log.info("freeIfInMemory LoadedDataHolderOffheap, stack: {}", SQUtils.getStackTrace());
      OffHeapMemory.deallocate(this.offheapCallback, this.address);
      this.address = -1L;
      return this.allocatedSize;
   }
}
