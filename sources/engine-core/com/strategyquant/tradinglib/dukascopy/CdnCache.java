package com.strategyquant.tradinglib.dukascopy;

import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.tempfiles.TempFilesManager;
import com.strategyquant.tradinglib.data.download.SpeedEvaluator;
import com.strategyquant.tradinglib.historyData.DownloadMessageHandler;
import com.strategyquant.tradinglib.historyData.HttpApacheManager;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLProtocolException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdnCache {
   public static final Logger Log = LoggerFactory.getLogger(CdnCache.class);
   private static final int BUFFER_SIZE = 49152;
   private static final int TIMEOUT = 40;
   private static final int RETRY_COUNT = 5;
   private static final int RETRY_DELAY = 30;
   private Set<String> cachedFiles = new HashSet<>();
   private Set<String> downloadedUrls = new HashSet<>();
   private String dukasSymbol;
   private String cdnBaseUrl;
   private String tmpFolder;
   private String rootFolderPath;
   private long lastDurationNano;
   private long lastDownloadedBytes;
   private File rootFolder;
   private String fromDateStr;
   private boolean loadMonthPackage;
   private volatile boolean stopped = false;
   private Thread downloadThread;
   private SpeedEvaluator speedEvaluator = new SpeedEvaluator();

   public CdnCache(String var1, String var2, String var3, Set<String> var4, long var5) {
      this.cdnBaseUrl = var2;
      this.dukasSymbol = var3;
      this.tmpFolder = DataManager.fixFilename(var1 + "_" + System.currentTimeMillis());
      File var7 = new File(MainApp.getDataPath() + "/internal/tmp", this.tmpFolder);
      var7.mkdirs();
      this.rootFolderPath = var7.getAbsolutePath();
      this.rootFolder = new File(this.rootFolderPath);
      this.rootFolder.mkdirs();
      this.fromDateStr = SQTime.toString(var5, "yyyy_MM_dd") + ".dat";
      String var8 = SQTime.toString(var5, "yyyy_MM") + ".zip";
      this.loadMonthPackage = var4.contains(var8);
   }

   public String getDataFile(int var1, int var2, int var3) {
      String var4 = this.getFileName(var1, var2, var3);
      return !this.cachedFiles.contains(var4) ? null : this.rootFolderPath + File.separator + var4;
   }

   private String getFileName(int var1, int var2, int var3) {
      String var4 = String.format("%04d_%02d_%02d", var1, var2, var3);
      return var4 + ".dat";
   }

   private String getUrl(int var1, int var2) {
      return this.loadMonthPackage
         ? DukascopyUtils.generateCdnMonthUrl(this.cdnBaseUrl, var1, var2, this.dukasSymbol)
         : DukascopyUtils.generateCdnYearUrl(this.cdnBaseUrl, var1, this.dukasSymbol);
   }

   private void sendMessage(DownloadMessageHandler var1, String var2, String var3) {
      if (var1 != null) {
         var1.onDownloadMessage(var2, var3);
      }
   }

   public void preload(long var1, long var3, final DownloadMessageHandler var5) {
      ExecutorService var6 = Executors.newFixedThreadPool(3);
      HashSet var7 = new HashSet();

      try {
         long var8 = var1;
         Log.debug("Preloading data");

         for (; var8 <= var3 && !this.stopped; var8 = SQTime.addMonths(var8, 1)) {
            if (this.stopped) {
               return;
            }

            final int var10 = SQTime.getFullYear(var8);
            int var11 = SQTime.getMonth(var8) + 1;
            final String var12 = this.getUrl(var10, var11);
            synchronized (var7) {
               if (!var7.contains(var12)) {
                  var7.add(var12);
                  Log.debug("Registering downloading year: {}", var10);
                  var6.submit(new Runnable() {
                     @Override
                     public void run() {
                        if (!CdnCache.this.stopped) {
                           CdnCache.Log.debug("Downloading year: {}", var10);
                           CdnCache.this.sendMessage(var5, "Downloading year:" + var10, null);
                           SpeedEvaluator var1x = CdnCache.this.doDownload(var12, var5);
                           if (!CdnCache.this.stopped) {
                              CdnCache.this.sendMessage(var5, "Downloaded year:" + var10, var1x == null ? null : var1x.getSpeed());
                           }

                           CdnCache.Log.debug("Downloaded year: {}", var10);
                        }
                     }
                  });
               }
            }
         }
      } finally {
         try {
            Log.debug("Waiting for all to download");
            var6.shutdown();
            var6.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
         } catch (InterruptedException var22) {
         }

         var6.shutdownNow();
      }

      Log.debug("Preload finished");
   }

   private SpeedEvaluator doDownload(String var1, DownloadMessageHandler var2) {
      HttpClient var3 = this.getClient();
      SpeedEvaluator var4 = new SpeedEvaluator();
      int var5 = -1;

      for (int var6 = 1; var6 <= 5 && !this.stopped; var6++) {
         HttpGet var7 = new HttpGet(var1);
         Log.debug("Downloading data from url: {}", var1);

         try {
            long var8 = System.nanoTime();
            HttpResponse var10 = var3.execute(var7);
            this.downloadedUrls.add(var1);
            var5 = var10.getStatusLine().getStatusCode();
            if (var5 >= 200 && var5 < 300) {
               byte[] var11 = this.getResponseData(var10);
               synchronized (this) {
                  this.lastDownloadedBytes = var11.length;
                  this.lastDurationNano = System.nanoTime() - var8;

                  try {
                     this.unzip(var11);
                  } catch (Exception var16) {
                     Log.error("Error while unziping data for url: " + var1, var16);
                  }

                  var4.evalSpeed(DownloadResultDto.success(null, this.lastDownloadedBytes, this.lastDurationNano));
                  return var4;
               }
            }

            if (var5 == -1) {
               this.sendMessage(var2, "Error while downloding file. Unknown communication error. Please check your internet connection.", null);
            } else {
               this.sendMessage(var2, "Error while downloding file. Http code: " + var5, null);
            }

            Log.error("Http result: " + var5 + " for url: " + var1);
         } catch (SocketTimeoutException | SSLProtocolException var18) {
            Log.error("Download timeout", var18);
            long var9 = 40000L;
            Log.debug("Used timeout timeout: {}ms", var9);
            if (var18 instanceof ConnectTimeoutException) {
               Log.debug("Host: {}", ((ConnectTimeoutException)var18).getHost());
            }

            this.sendMessage(var2, "Download timeout. Waiting for: 30sec", null);
            if (this.stopped) {
               break;
            }

            try {
               this.downloadThread = Thread.currentThread();
               Thread.sleep(30000L);
            } catch (InterruptedException var15) {
               return null;
            }

            this.sendMessage(var2, "Trying download again.", null);
         } catch (Exception var19) {
            Log.error("Error while downloading url: " + var1, var19);
         }
      }

      return null;
   }

   private HttpClient getClient() {
      return HttpApacheManager.getInstance().getClient(40);
   }

   public synchronized DownloadResultDto performLoad(int var1, int var2, int var3, DownloadMessageHandler var4) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
      String var5 = this.getUrl(var1, var2);
      if (this.downloadedUrls.contains(var5)) {
         String var18 = this.getDataFile(var1, var2, var3);
         return var18 == null ? DownloadResultDto.failed(-1) : DownloadResultDto.success(var18, this.lastDownloadedBytes, this.lastDurationNano);
      }

      HttpClient var6 = this.getClient();
      int var7 = -1;

      for (int var8 = 1; var8 <= 5 && !this.stopped; var8++) {
         HttpGet var9 = new HttpGet(var5);

         try {
            long var10 = System.nanoTime();
            HttpResponse var12 = var6.execute(var9);
            this.downloadedUrls.add(var5);
            var7 = var12.getStatusLine().getStatusCode();
            if (var7 == 200) {
               byte[] var13 = this.getResponseData(var12);
               this.lastDownloadedBytes = var13.length;
               this.lastDurationNano = System.nanoTime() - var10;
               this.unzip(var13);
               String var14 = this.getDataFile(var1, var2, var3);
               return var14 == null
                  ? DownloadResultDto.failed(-1, this.lastDownloadedBytes, this.lastDurationNano)
                  : DownloadResultDto.success(var14, this.lastDownloadedBytes, this.lastDurationNano);
            }

            Log.error("Http result: " + var7 + " for url: " + var5);
            this.sendMessage(var4, "Error while downloding file. Http code: " + var7, null);
         } catch (SocketTimeoutException | SSLProtocolException var16) {
            Log.error("Download timeout", var16);
            long var11 = 40000L;
            Log.debug("Used timeout timeout: {}ms", var11);
            if (var16 instanceof ConnectTimeoutException) {
               Log.debug("Host: {}", ((ConnectTimeoutException)var16).getHost());
            }

            this.sendMessage(var4, "Download timeout. Waiting for: 30sec", null);

            try {
               this.downloadThread = Thread.currentThread();
               Thread.sleep(30000L);
            } catch (InterruptedException var15) {
               return DownloadResultDto.failed(var7);
            }

            this.sendMessage(var4, "Trying download again.", null);
         } catch (Exception var17) {
            Log.error("Error while downloading url: " + var5, var17);
         }
      }

      return DownloadResultDto.failed(var7);
   }

   private byte[] getResponseData(HttpResponse var1) throws IllegalStateException, IOException {
      HttpEntity var2 = var1.getEntity();
      InputStream var3 = var2.getContent();
      return IOUtils.toByteArray(var3);
   }

   public void clean() {
      TempFilesManager.get().cleanSubFolder(this.tmpFolder);
   }

   private void unzip(byte[] var1) throws IOException {
      byte[] var2 = new byte[49152];
      ZipInputStream var3 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         for (ZipEntry var4 = var3.getNextEntry(); var4 != null && !this.stopped; var4 = var3.getNextEntry()) {
            String var5 = var4.getName();
            int var6 = var5.compareTo(this.fromDateStr);
            if (var6 >= 0) {
               this.cachedFiles.add(var5);
               File var7 = new File(this.rootFolder, var5);
               BufferedOutputStream var8 = new BufferedOutputStream(new FileOutputStream(var7));

               int var9;
               try {
                  while ((var9 = var3.read(var2)) > 0) {
                     var8.write(var2, 0, var9);
                  }
               } catch (Throwable var16) {
                  try {
                     var8.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }

                  throw var16;
               }

               var8.close();
            }
         }
      } finally {
         var3.closeEntry();
         var3.close();
      }
   }

   public void stop() {
      this.stopped = true;
      if (this.downloadThread != null) {
         this.downloadThread.interrupt();
      }
   }
}
