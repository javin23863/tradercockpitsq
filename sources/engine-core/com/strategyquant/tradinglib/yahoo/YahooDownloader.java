package com.strategyquant.tradinglib.yahoo;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.Pair;
import com.strategyquant.tradinglib.dukascopy.DukascopyBinaryMerger;
import com.strategyquant.tradinglib.dukascopy.DukascopyUtils;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahooDownloader {
   public static final Logger Log = LoggerFactory.getLogger(YahooDownloader.class);
   private static final int HOUR_CONSTANT = 3600;
   protected boolean canceled = false;
   protected boolean stopping = false;
   protected boolean paused = false;
   protected DukascopyBinaryMerger dataMerger;
   private DataBinReaderNew reader;
   private DataBinWriterNew writer;
   private volatile int avgDownloadSpeedCounter = 0;
   private volatile double avgDownloadSpeed = 0.0;
   private long lastPerformed = 0L;
   protected String jobId;
   protected DataManagerProgressListener listener;
   protected DataInfo dataInfo;
   protected ImportInfo importInfo;
   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
   private YahooHelper yahooHelper;

   public void performDownload(String var1, DataManagerProgressListener var2, DataInfo var3, ImportInfo var4) throws Exception {
      this.jobId = var1;
      this.listener = var2;
      this.dataInfo = var3;
      this.importInfo = var4;
      this.canceled = false;
      this.stopping = false;
      this.yahooHelper = new YahooHelper();
      Log.debug("Starting download job for symbol:" + var4.symbol);

      try {
         this.prepareFiles();
         this.dataMerger = new DukascopyBinaryMerger("Downloaded", var1, 1, var4);
         this.dataMerger.setProgressListener(var2);
         this.dataMerger.setWriter(this.writer);
         this.dataMerger.setReader(this.reader);
         var2.setMessage(L.t("Starting download...", new Object[0]));
         Log.debug("Starting download");
         this.download();
         this.finish();
         this.closeFiles(true);
         this.recomputeClonedData();
      } catch (Exception var9) {
         Log.error("Error while performing Yahoo data download", var9);
         this.onError("Error while performing Yahoo data download. Detail: " + var9.getMessage(), var4);
         throw new Exception(L.t("Error while performing %s data download.\n%s", new Object[]{var3.symbol, var9.getMessage()}));
      } finally {
         DataManagerDataProgress.get().removeListener(var4.symbol);
         Log.debug("Download job for symbol:" + var4.symbol + " finished");
      }
   }

   private void download() throws Exception {
      long var1 = this.importInfo.dateFrom;
      long var3 = this.importInfo.dateTo;
      if (var1 == 0L) {
         var1 = -9999999999999L;
      }

      var1 = this.dataMerger.checkDate(var1);
      if (var1 <= var3) {
         long var5 = this.dataMerger.getPerformedDays();
         long var7 = System.currentTimeMillis();
         this.dataMerger.register(0, var1 / 1000L);
         this.dataMerger.reset(1);
         String var9 = null;
         String var10 = null;

         for (int var11 = 0; var11 < 3; var11++) {
            try {
               var10 = "https://query1.finance.yahoo.com/v8/finance/chart/"
                  + URLEncoder.encode(this.dataInfo.uSymbol, "UTF-8")
                  + "?period1="
                  + var1 / 1000L
                  + "&period2="
                  + var3 / 1000L
                  + "&interval=1d&events=history";
               var9 = this.getUrlContent(var10, true, null);
               if (var9.contains("Invalid cookie")) {
                  Log.error("Cannot download data from yahoo. Invalid cookie. " + var10);
                  throw new Exception(L.t("Cannot download data from Yahoo.", new Object[0]));
               }
               break;
            } catch (Exception var25) {
               Log.error("Failed to download data. Trying again ...", var25);
               Thread.sleep(1000L);
            }
         }

         if (var9 == null) {
            throw new Exception(L.t("Cannot download data from Yahoo.", new Object[0]));
         }

         JSONObject var28 = new JSONObject(var9);
         JSONObject var12 = var28.getJSONObject("chart");
         if (!var12.isNull("error")) {
            Object var29 = var12.get("error");
            throw new Exception(L.t("Error while downloading data from Yahoo. " + var29.toString(), new Object[0]));
         }

         JSONArray var13 = var12.getJSONArray("result");
         JSONObject var14 = var13.getJSONObject(0);
         JSONArray var15 = var14.getJSONArray("timestamp");
         JSONObject var16 = var14.getJSONObject("indicators");
         JSONObject var17 = var16.getJSONArray("quote").getJSONObject(0);
         JSONArray var18 = var17.getJSONArray("open");
         JSONArray var19 = var17.getJSONArray("high");
         JSONArray var20 = var17.getJSONArray("low");
         JSONArray var21 = var17.getJSONArray("close");
         JSONArray var22 = var17.getJSONArray("volume");
         VersatileData var23 = null;

         for (int var24 = 0; var24 < var15.length(); var24++) {
            var23 = this.parseData(var15, var18, var19, var20, var21, var22, var24);
            if (var23 != null) {
               this.dataMerger.updateSaveProgress(var23.time);
               this.dataMerger.storeToQueue(0, var23);
            }
         }

         this.dataMerger.tryToSaveQueue(var1);
         this.evaluateSpeed(var7, var5);
         if (this.dataMerger.getReader() != null) {
            this.dataMerger.readTill(SQTime.addDays(var23.time, 1), false);
         }
      }
   }

   private VersatileData parseData(JSONArray var1, JSONArray var2, JSONArray var3, JSONArray var4, JSONArray var5, JSONArray var6, int var7) {
      try {
         VersatileData var8 = new VersatileData();
         var8.time = var1.getLong(var7) * 1000L;
         var8.open = var2.getDouble(var7);
         var8.high = var3.getDouble(var7);
         var8.low = var4.getDouble(var7);
         var8.close = var5.getDouble(var7);
         var8.volume = var2.getInt(var7);
         return var8;
      } catch (Exception var9) {
         Log.warn("Skipping yahoo record, due invalid content");
         return null;
      }
   }

   private VersatileData parseData(String[] var1) throws Exception {
      VersatileData var2 = new VersatileData();
      var2.time = this.sdf.parse(var1[0]).getTime();
      var2.open = Double.parseDouble(var1[1]);
      var2.high = Double.parseDouble(var1[2]);
      var2.low = Double.parseDouble(var1[3]);
      var2.close = Double.parseDouble(var1[4]);
      var2.volume = Double.parseDouble(var1[6]);
      return var2;
   }

   private String[] getCookieCrumb() throws Exception {
      String var1 = null;
      String var2 = null;

      try {
         CloseableHttpClient var3 = HttpClientBuilder.create().build();
         HttpGet var4 = new HttpGet("https://finance.yahoo.com/quote/SPY");
         HttpResponse var5 = var3.execute(var4);
         if (var5.getStatusLine().getStatusCode() != 200) {
            throw new Exception(L.t("HTML status code - %d", new Object[]{var5.getStatusLine().getStatusCode()}));
         }

         for (Header var9 : var5.getAllHeaders()) {
            if (var9.getName().equalsIgnoreCase("Set-Cookie")) {
               var2 = var9.getValue();
            }
         }

         InputStream var14 = var5.getEntity().getContent();
         InputStreamReader var15 = new InputStreamReader(var14, StandardCharsets.UTF_8);
         BufferedReader var16 = new BufferedReader(var15);
         String var17 = "CrumbStore\":{\"crumb\":\"";
         String var10 = null;

         while ((var10 = var16.readLine()) != null) {
            if (var10.contains(var17)) {
               int var11 = var10.indexOf("CrumbStore\":{\"crumb\":\"") + var17.length();
               var1 = var10.substring(var11, var10.indexOf("\"", var11));
               break;
            }
         }

         var16.close();
      } catch (Error var12) {
         throw new Exception(L.t("Cannot get Yahoo cookie crumb.", new Object[0]), var12);
      } catch (Exception var13) {
         throw new Exception(L.t("Cannot get Yahoo cookie crumb.", new Object[0]), var13);
      }

      if (var1 != null && var2 != null) {
         return new String[]{var1, var2};
      } else {
         throw new Exception(L.t("Cannot get Yahoo cookies / crumb.", new Object[0]));
      }
   }

   private String getUrlContent(String var1, boolean var2, String var3) throws Exception {
      CloseableHttpClient var4 = HttpClientBuilder.create().build();
      HttpGet var5 = new HttpGet(var1);
      HttpResponse var6 = null;

      try {
         var6 = var4.execute(var5);
      } catch (Exception var9) {
         throw new Exception(L.t("Unable to get data from the URL '%s'", new Object[]{var1}), var9);
      }

      String var7 = SQUtils.inputStreamToString(var6.getEntity().getContent());
      int var8 = var6.getStatusLine().getStatusCode();
      if (var2 && var8 >= 400) {
         throw new Exception(L.t("HTML status code - %d. Reason: %s", new Object[]{var6.getStatusLine().getStatusCode(), var7}));
      } else {
         return var7;
      }
   }

   protected void prepareFiles() throws Exception {
      Log.debug("Preparing data files");
      this.listener.setMessage(L.t("Preparing data files", new Object[0]));
      Pair var1 = DukascopyUtils.prepareFiles(this.importInfo, this.dataInfo.timeframe);
      this.reader = (DataBinReaderNew)var1.getA();
      this.writer = (DataBinWriterNew)var1.getB();
   }

   protected void closeFiles(boolean var1) {
      try {
         this.writer.close();
      } catch (Exception var4) {
      }

      try {
         if (this.reader != null) {
            this.reader.closeFile();
         }
      } catch (Exception var3) {
      }

      if (var1) {
         DukascopyUtils.deleteCopyFile(this.importInfo.symbol, "D1");
      } else {
         DukascopyUtils.revertCopyFile(this.importInfo.symbol, "D1");
      }
   }

   protected void finish() {
      try {
         if (this.canceled) {
            this.dataMerger.setStopped(this.canceled);
         }

         this.dataMerger.finish();
         long var1 = this.dataMerger.getFromDate();
         long var3 = this.dataMerger.getToDate();
         if (var1 != -1L && var3 != -1L) {
            DataManager.updateData(
               this.dataInfo.connection,
               this.dataInfo.symbol,
               var1,
               var3,
               this.dataMerger.getSavedRows(),
               0L,
               this.dataInfo.barTimeType,
               this.dataInfo.timeframe,
               "Etc/UCT"
            );
         }
      } catch (Exception var5) {
         Log.error("Error while saving results", var5);
         throw new RuntimeException(L.t("Error while saving results", new Object[0]));
      }
   }

   protected void recomputeClonedData() {
      try {
         DataCloner var1 = new DataCloner();
         DataInfo var2 = DataManager.getDataInfo("History", this.importInfo.symbol);
         ArrayList var3 = DataManager.listCloned(var2.id);

         for (int var4 = 0; var4 < var3.size(); var4++) {
            DataInfo var5 = (DataInfo)var3.get(var4);
            DataManager.clearData(var5.connection, var5.symbol);
            this.listener.setMessage(L.t("Recomputing cloned data", new Object[0]));
            String[] var6 = var5.timezone.split("\\|");
            String var7 = var6[0];
            int var8 = 0;

            try {
               var8 = Integer.parseInt(var6[1]);
            } catch (Exception var10) {
            }

            var1.cloneToTimezone(this.importInfo.symbol, var5.symbol, var7, var8, var5.removeWeekends, null, this.listener);
         }
      } catch (Exception var11) {
         Log.error("Error while recomputing cloned data.", var11);
         throw new RuntimeException(L.t("Error while recomputing cloned data.", new Object[0]));
      }
   }

   protected void onError(String var1, ImportInfo var2) {
      if (this.dataMerger != null) {
         this.dataMerger.skipAllProgress();
      }

      this.listener.onError(var1);
      this.closeFiles(false);
   }

   protected void evaluateSpeed(long var1, long var3) {
      long var5 = this.dataMerger.getPerformedDays();
      long var7 = System.currentTimeMillis();
      if (var5 != var3) {
         this.lastPerformed = var5 - var3;
      }

      double var9 = (var7 - var1) / 1000.0;
      double var11 = this.lastPerformed / var9;
      double var13 = Math.round(var11 * 10.0) / 10.0;
      double var15 = this.avgDownloadSpeed / this.avgDownloadSpeedCounter;
      String var17 = "Kbps";
      if (var15 > 1024.0) {
         var17 = "Mbps";
         var15 /= 1024.0;
      }

      double var18 = Math.round(var15 * 10.0) / 10.0;
      String var20 = " (" + var13 + " days/sec) - " + var18 + var17;
      this.listener.setSpeed(var20);
      this.avgDownloadSpeedCounter = 0;
      this.avgDownloadSpeed = 0.0;
      long var21 = this.dataMerger.getRestDays();
      if (var21 > 0L) {
         long var23 = (long)(var21 * var9);
         this.listener.setEstimation(this.evaluateTime(var23));
      }
   }

   private String evaluateTime(long var1) {
      long var3 = var1 / 60L;
      long var5 = var1 / 3600L;
      StringBuilder var7 = new StringBuilder();
      if (var5 > 0L) {
         var7.append(var5 + "hours ");
         long var8 = var3 - var5 * 60L;
         var7.append(var8 + "mins");
      } else if (var3 > 0L) {
         var7.append(var3 + "mins ");
         long var10 = var1 - var3 * 60L;
         var7.append(var10 + "secs");
      } else {
         var7.append(var1 + "secs");
      }

      return var7.toString();
   }

   protected void checkPaused() throws InterruptedException {
      if (this.paused) {
         this.listener.onPause();

         while (this.paused && !this.canceled) {
            Thread.sleep(100L);
         }

         this.listener.onContinue();
      }
   }

   public void cancel() {
      this.canceled = true;
   }

   public void pause() {
      this.paused = true;
   }

   public void restart() {
      this.paused = false;
   }

   public boolean isCanceled() {
      return this.canceled;
   }
}
