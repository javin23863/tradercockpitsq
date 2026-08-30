package com.strategyquant.tradinglib.historyData;

import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryDataCache {
   public static final Logger Log = LoggerFactory.getLogger(HistoryDataCache.class);
   private static final int BUFFER_SIZE = 2048;
   private Set<String> filesInCache = new HashSet<>();
   private Map<String, byte[]> data = new HashMap<>();
   private String symbolForDownload;
   private String timeframe;
   private TickerKind kind;
   private Thread downloadThread;
   private volatile boolean stopped;
   private TickerDto ticker;

   public HistoryDataCache(String var1, TickerDto var2, TickerKind var3, String var4) {
      this.symbolForDownload = var1;
      this.timeframe = var4;
      this.kind = var3;
      this.ticker = var2;
   }

   public void stop() {
      this.stopped = true;
      if (this.downloadThread != null) {
         this.downloadThread.interrupt();
      }
   }

   public byte[] getDataFile(int var1, int var2) {
      String var3 = this.timeframe.equals("M1") ? String.format("%04d%02d", var1, var2) : String.format("%04d", var1);
      return this.data.get(var3);
   }

   public synchronized DownloadResultDto performLoad(int var1, int var2, DownloadMessageHandler var3) throws Exception {
      String var4 = HistoryDataManager.getZipDataFilename(var1);
      if (this.filesInCache.contains(var4)) {
         byte[] var7 = this.getDataFile(var1, var2);
         return var7 == null ? DownloadResultDto.failed(-1) : DownloadResultDto.success(var7, 0L);
      }

      this.downloadThread = Thread.currentThread();
      byte[] var5 = HistoryDataManager.get().downloadHistoryData(this.ticker, this.kind, var1, var2, this.symbolForDownload, this.timeframe, var3);
      if (var5 != null) {
         this.unzip(var5, var1, var2);
      }

      this.filesInCache.add(var4);
      byte[] var6 = this.getDataFile(var1, var2);
      return var6 == null ? DownloadResultDto.failed(-1, 0L, 0L) : DownloadResultDto.success(var6, 0L);
   }

   private void unzip(byte[] var1, int var2, int var3) throws IOException {
      byte[] var4 = new byte[2048];
      ZipInputStream var5 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         for (ZipEntry var6 = var5.getNextEntry(); var6 != null; var6 = var5.getNextEntry()) {
            if (this.stopped) {
               return;
            }

            ByteArrayOutputStream var7 = new ByteArrayOutputStream((int)var6.getSize());

            int var8;
            while ((var8 = var5.read(var4)) > 0) {
               var7.write(var4, 0, var8);
            }

            var7.close();
            byte[] var9 = var7.toByteArray();
            this.storeContent(var6.getName(), var9);
         }
      } finally {
         var5.closeEntry();
         var5.close();
      }
   }

   private void storeContent(String var1, byte[] var2) {
      var1 = var1.split("\\.")[0];
      this.data.put(var1, var2);
   }
}
