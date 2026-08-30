package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.RandomAccessReaderFile;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedDataHolderFilePlain implements ILoadedDataHolder, IFeedDataSaver {
   public static final Logger Log = LoggerFactory.getLogger("LoadedDataHolderFile");
   private static final int NUM_RECORDS = 200;
   private int index = 0;
   final ByteBuffer byteBuffer = ByteBuffer.allocate(11400);
   private RandomAccessFile randAccessFile = null;
   private RandomAccessReaderFile rar;
   private FileChannel inChannel;
   private boolean isOhlc;
   private int decimals;
   private String fileName;
   private int hash;
   private long lastSessionStartTime = -1L;
   private HashPair[] hashPairs;
   private boolean lastOHLC;
   private int lastDecimals;
   private boolean loaded = false;

   public LoadedDataHolderFilePlain(int var1) throws Exception {
      this.hash = var1;
      this.fileName = TestfilesUtils.getTestdataFile(var1);
      File var2 = new File(this.fileName);
      if (var2.exists()) {
         this.openRar();
      }
   }

   private void openRar() throws Exception {
      this.rar = new RandomAccessReaderFile(this.fileName);
      this.rar.openFile();
      this.lastSessionStartTime = -1L;
      this.loadHeader();
   }

   private void loadHeader() throws Exception {
      if (!this.rar.dataRemaining()) {
         throw new Exception(L.t("No data remaining in loading header!", new Object[0]));
      }

      byte var1 = this.rar.readByte();
      this.hashPairs = new HashPair[var1];

      for (int var2 = 0; var2 < var1; var2++) {
         HashPair var3 = new HashPair();
         var3.connectionHash = this.rar.readInt();
         var3.symbolHash = this.rar.readInt();
         this.hashPairs[var2] = var3;
      }

      int var4 = this.rar.readInt();
      if (var4 != 123456789) {
         throw new Exception(L.t("Incorrect file header!", new Object[0]));
      }

      this.loaded = true;
   }

   @Override
   public void beforeSave(HashPair[] var1) {
      File var2 = new File(this.fileName);

      try {
         this.randAccessFile = new RandomAccessFile(var2, "rw");
         this.lastSessionStartTime = -1L;
         this.hashPairs = var1;
         this.saveHeader();
      } catch (FileNotFoundException var4) {
         var4.printStackTrace();
      }
   }

   private void saveHeader() {
      this.byteBuffer.put((byte)this.hashPairs.length);

      for (HashPair var4 : this.hashPairs) {
         this.byteBuffer.putInt(var4.connectionHash);
         this.byteBuffer.putInt(var4.symbolHash);
      }

      this.byteBuffer.putInt(123456789);
   }

   @Override
   public void afterSave() {
      try {
         if (this.byteBuffer.position() > 0) {
            this.byteBuffer.flip();
            this.randAccessFile.getChannel().write(this.byteBuffer);
            this.byteBuffer.clear();
         }

         this.randAccessFile.close();
         this.openRar();
         this.loaded = true;
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   @Override
   public ILoadedDataHolder cloneHolder() throws Exception {
      LoadedDataHolderFilePlain var1 = new LoadedDataHolderFilePlain(this.hash);
      var1.fileName = this.fileName;
      var1.index = 0;
      var1.lastSessionStartTime = this.lastSessionStartTime;
      var1.hashPairs = this.hashPairs;
      return var1;
   }

   @Override
   public VersatileData getNextTick(VersatileData var1) throws Exception {
      if (!this.rar.dataRemaining()) {
         return null;
      }

      var1.type = this.rar.readInt();
      byte var2 = this.rar.readByte();
      var1.connectionHash = this.hashPairs[var2].connectionHash;
      var1.symbolHash = this.hashPairs[var2].symbolHash;
      var1.sessionStartTime = this.rar.readLong();
      var1.time = this.rar.readLong();
      var1.ask = this.rar.readDouble();
      var1.bid = this.rar.readDouble();
      var1.volume = this.rar.readDouble();
      this.index++;
      return var1;
   }

   @Override
   public long size() {
      return 0L;
   }

   @Override
   public void saveTick(VersatileData var1) throws Exception {
      this.byteBuffer.putInt(var1.type);
      this.byteBuffer.put(this.getHashPairIndex(var1.connectionHash, var1.symbolHash));
      this.byteBuffer.putLong(var1.sessionStartTime);
      this.byteBuffer.putLong(var1.time);
      this.byteBuffer.putDouble(var1.ask);
      this.byteBuffer.putDouble(var1.bid);
      this.byteBuffer.putDouble(var1.volume);
      if (this.byteBuffer.remaining() < 100) {
         this.byteBuffer.flip();
         this.randAccessFile.getChannel().write(this.byteBuffer);
         this.byteBuffer.clear();
      }
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
      if (this.inChannel != null) {
         try {
            this.rar.closeFile();
            this.inChannel.close();
         } catch (IOException var2) {
            var2.printStackTrace();
         }
      }
   }

   @Override
   public void setParams(boolean var1, int var2) {
      this.isOhlc = var1;
      this.decimals = var2;
   }

   @Override
   public HashPair[] getHashPairs() {
      return this.hashPairs;
   }

   public boolean isOHLC() {
      return this.lastOHLC;
   }

   public int getDecimals() {
      return this.lastDecimals;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   @Override
   public void destroy() {
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
