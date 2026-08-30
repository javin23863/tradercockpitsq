package com.strategyquant.tradinglib.historyData;

import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EodHistoryDataCache {
   public static final Logger Log = LoggerFactory.getLogger(EodHistoryDataCache.class);
   private static final int BUFFER_SIZE = 2048;
   private static EodHistoryDataCache instance = new EodHistoryDataCache();
   private Map<String, byte[]> data = new HashMap<>();
   private Thread downloadThread;
   private volatile boolean stopped;
   private Map<String, ReentrantLock> locks = new HashMap<>();

   private EodHistoryDataCache() {
   }

   public void stop() {
      this.stopped = true;
      if (this.downloadThread != null) {
         this.downloadThread.interrupt();
      }
   }

   public byte[] getDataFile(String var1, int var2) throws IOException {
      return this.data.remove(HistoryDataManager.get().normalizeSymbol(var1) + var2);
   }

   public boolean exists(String var1, int var2) {
      return this.data.containsKey(HistoryDataManager.get().normalizeSymbol(var1) + var2);
   }

   public DownloadResultDto performLoad(String var1, int var2, int var3, DownloadMessageHandler var4) throws Exception {
      if (this.exists(var1, var2)) {
         byte[] var12 = this.getDataFile(var1, var2);
         Log.debug("Adjusted data: {} already fetched to {}", var1, var2);
         return var12 == null ? DownloadResultDto.failed(-1) : DownloadResultDto.success(var12, 0L);
      }

      String var5 = HistoryDataManager.get().getAdjustedIdent(var1);
      ReentrantLock var6 = this.getLock(var5);

      try {
         var6.lock();
         if (this.exists(var1, var2)) {
            byte[] var14 = this.getDataFile(var1, var2);
            Log.debug("Adjusted data: {} already fetched for {}", var1, var2);
            return var14 == null ? DownloadResultDto.failed(-1) : DownloadResultDto.success(var14, 0L);
         }

         this.downloadThread = Thread.currentThread();
         Log.debug("Downloading adjusted data: {} - {}", var1, var5);
         byte[] var7 = HistoryDataManager.get().downloadAdjustedHistoryData(var5, var4);
         if (var7 != null) {
            this.unzip(var7);
         }

         Log.debug("Downloaded adjusted data: {} - {}", var1, var5);
      } finally {
         var6.unlock();
      }

      byte[] var13 = this.getDataFile(var1, var2);
      return var13 == null ? DownloadResultDto.failed(-1, 0L, 0L) : DownloadResultDto.success(var13, 0L);
   }

   private synchronized ReentrantLock getLock(String var1) {
      ReentrantLock var2 = this.locks.get(var1);
      if (var2 == null) {
         var2 = new ReentrantLock();
         this.locks.put(var1, var2);
      }

      return var2;
   }

   private void unzip(byte[] var1) throws IOException {
      byte[] var2 = new byte[2048];
      ZipInputStream var3 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         for (ZipEntry var4 = var3.getNextEntry(); var4 != null; var4 = var3.getNextEntry()) {
            if (this.stopped) {
               return;
            }

            ByteArrayOutputStream var5 = new ByteArrayOutputStream((int)var4.getSize());

            int var6;
            while ((var6 = var3.read(var2)) > 0) {
               var5.write(var2, 0, var6);
            }

            var5.close();
            byte[] var7 = var5.toByteArray();
            if (var7.length > 0) {
               String[] var8 = var4.getName().split("/");
               if (var8.length == 2) {
                  this.storeContent(var8[0], var8[1], var7);
               }
            }
         }
      } finally {
         var3.closeEntry();
         var3.close();
      }
   }

   public void reset() {
      this.data.clear();
      this.stopped = false;
   }

   private void storeContent(String var1, String var2, byte[] var3) throws IOException {
      String var4 = var2.substring(0, 4);
      this.data.put(var1 + ".D" + var4, var3);
   }

   public static EodHistoryDataCache getInstance() {
      return instance;
   }
}
