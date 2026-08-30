package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.OhlcDataReader;
import com.strategyquant.datalib.data.io.newDataFormat.OhlcDataWriter;
import com.strategyquant.datalib.data.io.newDataFormat.RandomAccessReaderFile;
import com.strategyquant.datalib.data.io.newDataFormat.TickDataReader;
import com.strategyquant.datalib.data.io.newDataFormat.TickDataWriter;
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

public class LoadedDataHolderFileBDF implements ILoadedDataHolder, IFeedDataSaver {
   public static final Logger Log = LoggerFactory.getLogger("LoadedDataHolderFile");
   private static final int NUM_RECORDS = 200;
   private int index = 0;
   private TickDataWriter[] tickWriters = new TickDataWriter[10];
   private OhlcDataWriter[] ohlcWriters = new OhlcDataWriter[10];
   private TickDataReader[] tickReaders = new TickDataReader[10];
   private OhlcDataReader[] ohlcReaders = new OhlcDataReader[10];
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
   private MarketData[] marketDatas = null;

   public LoadedDataHolderFileBDF(int var1) throws Exception {
      this.hash = var1;
      this.fileName = TestfilesUtils.getTestdataFile(var1);
      File var2 = new File(this.fileName);
      if (var2.exists()) {
         if (var2.length() < 100L) {
            var2.delete();
         } else {
            this.openRar();
         }
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
         Log.error("Exception", var4);
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
         Log.error("Exception ", var2);
         var2.printStackTrace();
      }
   }

   @Override
   public ILoadedDataHolder cloneHolder() throws Exception {
      LoadedDataHolderFileBDF var1 = new LoadedDataHolderFileBDF(this.hash);
      var1.fileName = this.fileName;
      var1.index = 0;
      var1.lastSessionStartTime = this.lastSessionStartTime;
      var1.hashPairs = this.hashPairs;
      var1.marketDatas = this.marketDatas;
      return var1;
   }

   @Override
   public VersatileData getNextTick(VersatileData var1) throws Exception {
      if (!this.rar.dataRemaining()) {
         return null;
      }

      byte var2 = this.rar.readByte();
      int var3 = var2 >> 6;
      int var4 = var2 & 15;
      var1.type = (var2 & 48) >> 4;
      byte var5 = this.rar.readByte();
      var1.connectionHash = this.hashPairs[var5].connectionHash;
      var1.symbolHash = this.hashPairs[var5].symbolHash;
      if (var3 == 0) {
         this.lastSessionStartTime = this.rar.readLong();
         var1.sessionStartTime = this.lastSessionStartTime;
      } else {
         var1.sessionStartTime = this.lastSessionStartTime;
      }

      int var6 = var1.connectionHash + var1.symbolHash;
      this.lastDecimals = var4;
      this.lastOHLC = var1.type == 2;
      if (var1.type == 1) {
         this.getTickReader(var6, var4).readData(this.rar, var1);
      } else {
         this.getOhlcReader(var6, var4).readData(this.rar, var1);
      }

      this.index++;
      return var1;
   }

   @Override
   public long size() {
      return 0L;
   }

   @Override
   public void saveTick(VersatileData var1) throws Exception {
      byte var2 = (byte)this.decimals;
      var2 = (byte)(var2 | 16 * var1.type);
      boolean var3 = this.lastSessionStartTime == var1.sessionStartTime;
      var2 = (byte)(var2 | 64 * (var3 ? 1 : 0));
      this.byteBuffer.put(var2);
      this.byteBuffer.put(this.getHashPairIndex(var1.connectionHash, var1.symbolHash));
      if (!var3) {
         this.lastSessionStartTime = var1.sessionStartTime;
         this.byteBuffer.putLong(var1.sessionStartTime);
      }

      if (this.isOhlc) {
         this.getOhlcWriter(this.decimals).putData(this.byteBuffer, var1);
      } else {
         this.getTickWriter(this.decimals).putData(this.byteBuffer, var1);
      }

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
            this.rar = null;
            this.inChannel.close();
         } catch (IOException var3) {
            Log.error("Error closing Inchannel + ReadFile", var3);
         }
      }

      if (this.rar != null) {
         try {
            this.rar.closeFile();
         } catch (IOException var2) {
            Log.error("Error closing ReadFile", var2);
         }
      }
   }

   @Override
   public void setParams(boolean var1, int var2) {
      this.isOhlc = var1;
      this.decimals = var2;
   }

   private TickDataWriter getTickWriter(int var1) {
      if (var1 > 9) {
         throw new IllegalArgumentException("Decimals cannot be bigger than 9");
      }

      if (this.tickWriters[var1] == null) {
         this.tickWriters[var1] = new TickDataWriter(true);
      }

      return this.tickWriters[var1];
   }

   private OhlcDataWriter getOhlcWriter(int var1) {
      if (var1 > 9) {
         throw new IllegalArgumentException("Decimals cannot be bigger than 9");
      }

      if (this.ohlcWriters[var1] == null) {
         this.ohlcWriters[var1] = new OhlcDataWriter(true);
      }

      return this.ohlcWriters[var1];
   }

   private TickDataReader getTickReader(int var1, int var2) {
      if (var2 > 9) {
         throw new IllegalArgumentException("Decimals cannot be bigger than 9");
      }

      if (this.tickReaders[var2] == null) {
         this.tickReaders[var2] = new TickDataReader(true);
      }

      return this.tickReaders[var2];
   }

   private OhlcDataReader getOhlcReader(int var1, int var2) {
      if (var2 > 9) {
         throw new IllegalArgumentException("Decimals cannot be bigger than 9");
      }

      if (this.ohlcReaders[var2] == null) {
         this.ohlcReaders[var2] = new OhlcDataReader(true);
      }

      return this.ohlcReaders[var2];
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
      this.marketDatas = var1;
   }

   @Override
   public MarketData[] getMarketData() {
      return this.marketDatas;
   }

   @Override
   public long freeIfInMemory() {
      return 0L;
   }

   public void close() throws IOException {
      if (this.rar != null) {
         this.rar.closeFile();
      }

      if (this.randAccessFile != null) {
         this.randAccessFile.close();
      }
   }
}
