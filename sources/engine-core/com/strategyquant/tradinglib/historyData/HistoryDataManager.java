package com.strategyquant.tradinglib.historyData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.historyData.AbstractHistoryDataDao;
import com.strategyquant.datalib.historyData.FuturesHistoryDataDao;
import com.strategyquant.datalib.historyData.StockHistoryDataDao;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.TickerRenameInfo;
import com.strategyquant.datalib.historyData.dto.CommodityDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.exception.SQException;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.lib.hw.HardwareInfo;
import com.strategyquant.model.IdName;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLProtocolException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom2.JDOMException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryDataManager {
   public static final Long BMF_ID = 100000L;
   public static final String CDN_USER = "cdnuser";
   public static final String CDN_PASS = "QmwwrOrdxKK52pPUmjkf";
   public static final Logger Log = LoggerFactory.getLogger(HistoryDataManager.class);
   private static final String CDN_BASE_URL = "https://cdn.strategyquantcdn.com/";
   private static final String HK_BASE_CDN_URL = "https://cdn005.strategyquantcdn.com/";
   private static HistoryDataManager instance = null;
   private static final int TIMEOUT = 40;
   private static final int RETRY_COUNT = 5;
   private static final int RETRY_DELAY = 30;
   private String macAddr;
   private String diskSn;
   private boolean devel = false;
   private String customCdnBase;
   private String dataFolder = SQPaths.dataDirPath;
   private FuturesHistoryDataDao futuresDao;
   private StockHistoryDataDao stockDao;
   private long stockDaoBorn;
   private long futureDaoBorn;
   private boolean stockWasChanged;

   private HistoryDataManager() {
      File var1 = new File(MainApp.getDataPath() + File.separator + "user", "cdn.txt");
      if (var1.exists()) {
         try {
            List var2 = Files.readAllLines(var1.toPath());
            if (var2.size() > 2) {
               this.customCdnBase = ((String)var2.get(2)).trim();
               if (!this.customCdnBase.endsWith("/")) {
                  this.customCdnBase = this.customCdnBase + "/";
               }
            }
         } catch (Exception var3) {
            Log.error("Error while loading cdn.txt content", var3);
         }
      }

      this.macAddr = HardwareInfo.getMacAddress();
      this.diskSn = HardwareInfo.getDiskSN();
      this.checkDevelMode();
   }

   private String getBaseUrl() {
      if (this.customCdnBase != null) {
         return this.customCdnBase;
      } else {
         return this.useHkServer() ? "https://cdn005.strategyquantcdn.com/" : "https://cdn.strategyquantcdn.com/";
      }
   }

   private String getVersionUrl(boolean var1) {
      return this.getBaseUrl() + "data/database/" + (var1 ? "data_stock.version" : "data_futures.version");
   }

   private String getH2Url(boolean var1) {
      return this.getBaseUrl() + "data/database/" + (var1 ? "data_stock.zip" : "data_futures.zip");
   }

   private String getDataUrl() {
      return this.getBaseUrl() + "sqdata.php";
   }

   private void checkDevelMode() {
      File var1 = new File(MainApp.getDataPath() + "/usenewdata.txt");
      if (var1.exists()) {
         Log.info("Using development mode for downloading data. So it means that test data will be downloaded instead of real one!!!!!!!!!!");
         this.devel = true;
      }
   }

   public static HistoryDataManager get() {
      if (instance == null) {
         instance = new HistoryDataManager();
      }

      return instance;
   }

   public synchronized boolean shouldInvalidateFutureDatas(String var1, Date var2) {
      this.ensureFuturesDao();
      Date var3 = this.futuresDao.getChangeDate(var1);
      return this.shouldInvalidateDatas(var3, var2);
   }

   private boolean shouldInvalidateDatas(Date var1, Date var2) {
      return var1 != null && var2 != null ? var1.after(var2) || var1.equals(var2) : false;
   }

   public boolean shouldSetFixedTimeForEod(TickerDto var1, int var2) {
      return var2 == 2 && var1.isEod() && (var1.getMarketName().equals("CME") || var1.getMarketName().equals("CBOT"));
   }

   public boolean shouldShiftHoursForM1(TickerDto var1) {
      return !var1.isEod() && (var1.getMarketName().equals("NYMEX") || var1.getMarketName().equals("ICEUS") || var1.getMarketName().equals("COMEX"));
   }

   public int getEodShiftDate(TickerDto var1) {
      if (var1.getMarketName().equals("CME")) {
         return 54900000;
      } else {
         return var1.getMarketName().equals("CBOT") ? 50400000 : 0;
      }
   }

   public int getM1ShiftDate(TickerDto var1) {
      return !var1.getMarketName().equals("NYMEX") && !var1.getMarketName().equals("ICEUS") && !var1.getMarketName().equals("COMEX") ? 0 : 3600000;
   }

   public synchronized boolean shouldInvalidateStockDatas(String var1, Date var2) throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException, SQLException {
      this.ensureStockDao();
      Date var3 = this.stockDao.getChangeDate(var1);
      return this.shouldInvalidateDatas(var3, var2);
   }

   private boolean isDaoOld(long var1) {
      long var3 = System.currentTimeMillis() - var1;
      return var3 > 43200000L;
   }

   private synchronized void ensureFuturesDao() {
      if (this.futuresDao == null || this.isDaoOld(this.futureDaoBorn)) {
         this.disposeDao(this.futuresDao);
         boolean var1 = false;

         try {
            var1 = this.ensureDataActual(false);
         } catch (Exception var4) {
            Log.error("Error while downloading new version", var4);
         }

         this.futuresDao = new FuturesHistoryDataDao(this.dataFolder);
         if (var1) {
            try {
               Log.debug("Creating indexes for futures");
               this.futuresDao.createIndexes();
               Log.debug("Indexes created");
            } catch (Exception var3) {
               Log.error("Error while generating indexes", var3);
            }
         }

         this.futureDaoBorn = System.currentTimeMillis();
      }
   }

   private synchronized void ensureStockDao() {
      if (this.stockDao == null || this.isDaoOld(this.stockDaoBorn)) {
         this.disposeDao(this.stockDao);
         boolean var1 = false;

         try {
            var1 = this.ensureDataActual(true);
         } catch (Exception var4) {
            Log.error("Error while downloading new version", var4);
         }

         this.stockDao = new StockHistoryDataDao(this.dataFolder);
         if (var1) {
            this.stockWasChanged = true;

            try {
               Log.debug("Creating indexes for equity");
               this.stockDao.createIndexes();
               Log.debug("Indexes created");
            } catch (Exception var3) {
               Log.error("Error while generating indexes", var3);
            }
         }

         this.stockDaoBorn = System.currentTimeMillis();
      }
   }

   private int checkEvents() throws Exception {
      Map var1 = this.stockDao.getTickerRenamed();
      DataManager.get();
      ArrayList var2 = DataManager.listSafe();
      int var3 = 0;

      for (DataInfo var5 : var2) {
         String var6 = var5.uSymbol;
         if (var5.source == 5 && var6 != null && var5.dateTo != 0L) {
            if (var5.symbol != null && var5.symbol.endsWith("_limited.D")) {
               Log.debug("Skipping symbol {}", var5.symbol);
            } else {
               boolean var7 = "D1".equals(var5.timeframe);
               String var8 = var7 && var6.endsWith(".D") ? var6.substring(0, var6.length() - 2) : var6;
               String var9 = this.getActualName(var8, var5.dateTo, var1, 10);
               String var10 = var7 ? var9 + ".D" : var9;
               if (!var6.equalsIgnoreCase(var10)) {
                  String var11 = var5.symbol;
                  String var12 = DataManager.renameUnderlayingData("History", var5.symbol, var10);
                  Log.info("Renaming symbol {} -> {}", var11, var12);
                  var3++;
               }
            }
         }
      }

      return var3;
   }

   private String getActualName(String var1, long var2, Map<String, List<TickerRenameInfo>> var4, int var5) {
      if (var5 == 0) {
         Log.info("Cycle in renaming found!");
         return var1;
      }

      List var6 = (List)var4.get(var1);
      if (var6 != null) {
         TickerRenameInfo var7 = null;

         for (TickerRenameInfo var9 : var6) {
            if (var9.getDate() > var2) {
               var7 = var9;
               break;
            }
         }

         if (var7 != null) {
            return this.getActualName(var7.getTickerTo(), var7.getDate(), var4, var5 - 1);
         }
      }

      return var1;
   }

   public synchronized List<IdName> getStockMarkets() throws ClientProtocolException, IllegalStateException, IOException, JDOMException, SQLException, NoSuchAlgorithmException {
      this.ensureStockDao();
      return this.stockDao.getAllMarkets();
   }

   public synchronized List<TickerDto> getStockTickers(TickerFilterDto var1) throws ClientProtocolException, IllegalStateException, IOException, JDOMException, SQLException, NoSuchAlgorithmException {
      this.ensureStockDao();
      return this.stockDao.getTickers(var1);
   }

   public synchronized List<IdName> getFuturesMarkets() throws ClientProtocolException, IllegalStateException, IOException, JDOMException, SQLException, NoSuchAlgorithmException {
      this.ensureFuturesDao();
      return this.futuresDao.getAllMarkets();
   }

   public synchronized List<TickerDto> getFuturesTickers(TickerFilterDto var1) {
      this.ensureFuturesDao();

      try {
         return this.futuresDao.getTickers(var1);
      } catch (SQLException var3) {
         throw new RuntimeException(L.t("Error while getting futures tickers", new Object[0]), var3);
      }
   }

   public synchronized TickerDto getFutureTicker(String var1) throws SQLException, ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      this.ensureFuturesDao();
      return this.futuresDao.getTicker(var1);
   }

   public synchronized TickerDto getFutureTicker(Long var1) throws SQLException, ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      this.ensureFuturesDao();
      return this.futuresDao.getTicker(var1);
   }

   public synchronized TickerDto getStockTicker(String var1) throws SQLException, ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      this.ensureStockDao();
      return this.stockDao.getTicker(var1);
   }

   public synchronized TickerDto getStockTicker(Long var1) throws SQLException, ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      this.ensureStockDao();
      return this.stockDao.getTicker(var1);
   }

   private boolean ensureDataActual(boolean var1) throws ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      Log.debug("Checking if history data database is actual");
      String var2 = this.downloadAvailableVersion(var1);
      String var3 = this.getCurrentDownloadedVersion(var1);
      if (var3 == null || !this.existsH2(var1)) {
         Log.debug("Missing version file or database - downloading entire database from server");
         this.downloadDatabase(var1);
         this.saveLatestVersion(var1, var2);
         return true;
      } else if (var3.compareTo(var2) < 0) {
         Log.debug("Database is old. Downloaded version:" + var3 + ", available version: " + var2);
         this.downloadDatabase(var1);
         this.saveLatestVersion(var1, var2);
         return true;
      } else {
         return false;
      }
   }

   private void saveLatestVersion(boolean var1, String var2) throws IOException {
      Log.debug("Updating data version file");
      File var3 = null;
      if (this.devel) {
         var3 = new File(this.dataFolder, var1 ? "data_stock_dev.version" : "data_futures_dev.version");
      } else {
         var3 = new File(this.dataFolder, var1 ? "data_stock.version" : "data_futures.version");
      }

      Files.write(var3.toPath(), var2.getBytes(), StandardOpenOption.CREATE);
   }

   private String getCurrentDownloadedVersion(boolean var1) throws IOException {
      File var2 = null;
      if (this.devel) {
         var2 = new File(this.dataFolder, var1 ? "data_stock_dev.version" : "data_futures_dev.version");
      } else {
         var2 = new File(this.dataFolder, var1 ? "data_stock.version" : "data_futures.version");
      }

      return !var2.exists() ? null : new String(Files.readAllBytes(var2.toPath()));
   }

   private boolean existsH2(boolean var1) {
      File var2 = new File(this.dataFolder, var1 ? "data_stock.h2.db" : "data_futures.h2.db");
      return var2.exists();
   }

   private void downloadDatabase(boolean var1) throws ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      DownloadMessageHandler var2 = new DownloadMessageHandler() {
         @Override
         public void onDownloadMessage(String var1, String var2x) {
            HistoryDataManager.Log.error("Timeout while downloading database.");
         }
      };
      byte[] var3 = this.doDownload(this.getH2Url(var1), 0, var2);
      if (var3 == null) {
         Log.error("Database download failed.");
         throw new RuntimeException(L.t("Database download failed. No content.", new Object[0]));
      }

      this.unzipH2(var3);
      Log.debug("Database downloaded and unziped");
   }

   private String downloadAvailableVersion(boolean var1) throws ClientProtocolException, IllegalStateException, IOException, JDOMException, NoSuchAlgorithmException {
      DownloadMessageHandler var3 = new DownloadMessageHandler() {
         @Override
         public void onDownloadMessage(String var1, String var2) {
            HistoryDataManager.Log.error("Timeout while downloading version file.");
         }
      };
      String var4 = this.getVersionUrl(var1);
      byte[] var2 = this.doDownload(var4, 0, var3);
      if (var2 == null) {
         Log.error("Download database descriptions failed from server: " + var4);
         throw new RuntimeException(L.t("Database descriptions download failed. No content.", new Object[0]));
      } else {
         return new String(var2);
      }
   }

   private boolean useHkServer() {
      String var1 = MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN);
      return DataSources.DOWNLOAD_TYPE_CDN_CN.equals(var1);
   }

   public String normalizeSymbol(String var1) {
      if (var1.equalsIgnoreCase("CON")
         || var1.startsWith("CON.")
         || var1.equalsIgnoreCase("PRN")
         || var1.startsWith("PRN.")
         || var1.equalsIgnoreCase("AUX")
         || var1.startsWith("AUX.")) {
         var1 = "_" + var1;
      }

      return var1;
   }

   public byte[] downloadHistoryData(TickerDto var1, TickerKind var2, int var3, int var4, String var5, String var6, DownloadMessageHandler var7) throws Exception {
      boolean var8 = BMF_ID.equals(var1.getMarketId());
      if (var6.equals("M1")) {
         var6 = "minutes";
         if (var8) {
            var5 = var5.substring(0, var5.length() - 3);
         }
      } else {
         var6 = "eod";
         if (var8) {
            var5 = var5.substring(0, var5.length() - 3);
         } else {
            var5 = var5.substring(0, var5.length() - 2);
         }
      }

      String var9 = getZipDataFilename(var3);
      var5 = this.normalizeSymbol(var5);
      String var10 = String.format("%s/%s/%s.zip", var6, var5, var9);
      String var11 = this.getDataUrl();
      int var12 = var11.indexOf("/", 10);
      Log.info("Downloading {} from server: {}", var5, var11.substring(0, var12));
      return this.doPostDownload(var11, var10, var2.forDownload(), 30, var7);
   }

   public String getAdjustedIdent(String var1) {
      if (var1.length() <= 3) {
         return String.valueOf(var1.charAt(0));
      }

      String var2 = var1.substring(0, 2);
      if (var2.endsWith(".")) {
         var2 = String.valueOf(var1.charAt(0));
      }

      return var2;
   }

   public byte[] downloadAdjustedHistoryData(String var1, DownloadMessageHandler var2) throws Exception {
      String var3 = String.format("eod/adjusted_%s/data.zip", var1);
      String var4 = this.getDataUrl();
      return this.doPostDownload(var4, var3, TickerKind.STOCK.forDownload(), 30, var2);
   }

   private byte[] doDownload(String var1, int var2, DownloadMessageHandler var3) throws ClientProtocolException, IOException {
      CloseableHttpClient var4 = (CloseableHttpClient)HttpApacheManager.getInstance().getClient(40);

      B var10;
      label91: {
         Object var18;
         label92: {
            label93: {
               Object var16;
               try {
                  for (int var5 = 1; var5 <= 5; var5++) {
                     try {
                        CloseableHttpResponse var6 = var4.execute(new HttpGet(var1));
                        int var17 = var6.getStatusLine().getStatusCode();
                        if (var17 != 200) {
                           Log.error("Http result: " + var17 + " for url: " + var1);
                           this.notifyError(var3, "Error while downloding file. Http code: " + var17);
                           var18 = null;
                           break label92;
                        }

                        InputStream var8 = var6.getEntity().getContent();
                        byte[] var9 = IOUtils.toByteArray(var8);
                        var10 = var9;
                        break label93;
                     } catch (SocketTimeoutException | ConnectTimeoutException | SSLProtocolException var13) {
                        Log.error("Download timeout for url: " + var1, var13);
                        long var7 = 40000L;
                        Log.debug("Used timeout timeout: " + var7 + "ms ");
                        if (var13 instanceof ConnectTimeoutException) {
                           Log.debug("Host:" + ((ConnectTimeoutException)var13).getHost());
                        }

                        this.notifyError(var3, "Download timeout. Waiting for: 30sec");

                        try {
                           if (var2 != 0) {
                              Thread.sleep(var2 * 1000);
                           }
                        } catch (InterruptedException var12) {
                           var10 = null;
                           break label91;
                        }
                     } catch (Exception var14) {
                        Log.error("Error while downloading url: " + var1, var14);
                     }
                  }

                  var16 = null;
               } catch (Throwable var15) {
                  if (var4 != null) {
                     try {
                        var4.close();
                     } catch (Throwable var11) {
                        var15.addSuppressed(var11);
                     }
                  }

                  throw var15;
               }

               if (var4 != null) {
                  var4.close();
               }

               return (byte[])var16;
            }

            if (var4 != null) {
               var4.close();
            }

            return (byte[])var10;
         }

         if (var4 != null) {
            var4.close();
         }

         return (byte[])var18;
      }

      if (var4 != null) {
         var4.close();
      }

      return (byte[])var10;
   }

   private void notifyError(DownloadMessageHandler var1, String var2) {
      if (var1 != null) {
         var1.onDownloadMessage(var2, null);
      }
   }

   private byte[] doPostDownload(String var1, String var2, String var3, int var4, DownloadMessageHandler var5) throws Exception {
      if (this.devel && var2.startsWith("eod")) {
         return this.doPostDownloadDevel(var2, var5);
      }

      CloseableHttpClient var6 = (CloseableHttpClient)HttpApacheManager.getInstance().getClient(40);

      Object var10;
      label95: {
         byte[] var27;
         label96: {
            Object var24;
            try {
               int var7 = 1;

               while (var7 <= 5) {
                  try {
                     HttpPost var8 = new HttpPost(var1);
                     ArrayList var9 = new ArrayList();
                     V571hfnsHw var25 = MainApp.v571hfnsHw();
                     var9.add(new BasicNameValuePair("l", var25.eHvc2JguAd()));
                     var9.add(new BasicNameValuePair("hwid", var25.nmFllxIfvN()));
                     var9.add(new BasicNameValuePair("dsn", this.diskSn));
                     var9.add(new BasicNameValuePair("mac", this.macAddr));
                     var9.add(new BasicNameValuePair("path", var2));
                     var9.add(new BasicNameValuePair("type", var3));
                     var8.setEntity(new UrlEncodedFormEntity(var9));
                     CloseableHttpResponse var11 = var6.execute(var8);
                     int var12 = var11.getStatusLine().getStatusCode();
                     InputStream var13 = var11.getEntity().getContent();
                     if (var12 >= 200 && var12 < 300) {
                        byte[] var26 = IOUtils.toByteArray(var13);
                        var27 = var26;
                        break label96;
                     }

                     byte[] var14 = IOUtils.toByteArray(var13);
                     String var15 = new String(var14);
                     Object var16 = null;
                     Log.debug("Download request for h:{} failed", var25.nmFllxIfvN());
                     Log.error("Download failed with code: " + var12 + ". Response content: " + var15);

                     try {
                        JSONObject var17 = new JSONObject(var15);
                        var16 = var17.getString("error");
                     } catch (Exception var19) {
                        Log.error("Error while parsing history data response", var19);
                        throw new RuntimeException(L.t("Error while parsing history data response.", new Object[0]), var19);
                     }

                     throw new SQException("Error while downloading history data: " + var16);
                  } catch (SocketTimeoutException | ConnectTimeoutException | SSLProtocolException var21) {
                     Log.error("Download timeout for " + var2, var21);
                     this.notifyError(var5, "Download timeout. Waiting for: 30sec");

                     try {
                        if (var4 != 0) {
                           Thread.sleep(var4 * 1000);
                        }
                     } catch (InterruptedException var20) {
                        var10 = null;
                        break label95;
                     }

                     var7++;
                  } catch (Exception var22) {
                     Log.error("Error while downloading " + var2, var22);
                     throw var22;
                  }
               }

               var24 = null;
            } catch (Throwable var23) {
               if (var6 != null) {
                  try {
                     var6.close();
                  } catch (Throwable var18) {
                     var23.addSuppressed(var18);
                  }
               }

               throw var23;
            }

            if (var6 != null) {
               var6.close();
            }

            return (byte[])var24;
         }

         if (var6 != null) {
            var6.close();
         }

         return var27;
      }

      if (var6 != null) {
         var6.close();
      }

      return (byte[])var10;
   }

   private byte[] doPostDownloadDevel(String var1, DownloadMessageHandler var2) throws ClientProtocolException, IOException {
      String var3 = "https://cdn.strategyquantcdn.com/data/barchart_test/" + var1;
      return this.doDownload(var3, 0, var2);
   }

   public static String getZipDataFilename(long var0) {
      return var0 < 2019L ? "data00" : "data01";
   }

   private void unzipH2(byte[] var1) throws IOException {
      byte[] var2 = new byte[1024];
      ZipInputStream var3 = new ZipInputStream(new ByteArrayInputStream(var1));

      try {
         ZipEntry var4 = var3.getNextEntry();
         if (var4 != null) {
            File var5 = new File(this.dataFolder, var4.getName());
            BufferedOutputStream var6 = new BufferedOutputStream(new FileOutputStream(var5));

            int var7;
            try {
               while ((var7 = var3.read(var2)) > 0) {
                  var6.write(var2, 0, var7);
               }
            } catch (Throwable var11) {
               try {
                  var6.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            var6.close();
         }
      } catch (Throwable var12) {
         try {
            var3.close();
         } catch (Throwable var9) {
            var12.addSuppressed(var9);
         }

         throw var12;
      }

      var3.close();
   }

   public synchronized CommodityDto getCommodity(Long var1) throws SQLException, ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException {
      this.ensureFuturesDao();
      return this.futuresDao.getCommodity(var1);
   }

   public synchronized CommodityDto getCommodityForFuture(String var1) throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException, SQLException {
      this.ensureFuturesDao();
      return this.futuresDao.getCommodityForFuture(var1);
   }

   public synchronized List<CommodityDto> getCommodities(String[] var1) throws SQLException {
      this.ensureFuturesDao();
      return this.futuresDao.getCommoditiesForFutures(var1);
   }

   private String getHtml(List<TickerDto> var1) {
      HashMap var2 = new HashMap();

      for (TickerDto var4 : var1) {
         List var5 = (List)var2.get(var4.getMarketName());
         if (var5 == null) {
            var5 = new LinkedList();
            var2.put(var4.getMarketName(), var5);
         }

         var5.add(var4);
      }

      DateTimeFormatter var12 = DateTimeFormat.forPattern("dd.MM.YYYY");
      StringBuilder var13 = new StringBuilder();
      var13.append("<table>");

      for (String var6 : var2.keySet()) {
         var13.append("<tr><td class=\"dataType\" colspan=\"6\">" + this.safePrint(var6) + "</td></tr>\n");
         var13.append(
            String.format(
               "<tr class=\"dataHeader\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n", "Ticker", "Name", "Type", "Exchange", "Data From"
            )
         );

         for (TickerDto var9 : (List)var2.get(var6)) {
            String var10 = var9.getDateFrom() == null ? "" : var12.print(var9.getDateFrom().getTime());
            String var11 = var9.getName();
            if (var9.isEod()) {
               var11 = var11 + " - Daily";
            }

            var13.append(
               String.format(
                  "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                  var9.getTicker(),
                  this.safePrint(var11),
                  this.safePrint(var9.getType()),
                  this.safePrint(var9.getMarketName()),
                  var10
               )
            );
         }
      }

      var13.append("</table>");
      return var13.toString();
   }

   public synchronized String generateStockHtml() throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException, SQLException {
      this.ensureStockDao();
      List var1 = this.stockDao.getTickers(new TickerFilterDto());
      return this.getHtml(var1);
   }

   public synchronized String generateFuturesHtml() throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException, SQLException {
      this.ensureFuturesDao();
      List var1 = this.futuresDao.getTickers(new TickerFilterDto());
      return this.getHtml(var1);
   }

   private Object safePrint(String var1) {
      return var1 != null ? var1 : "-";
   }

   public synchronized void init() {
      try {
         this.ensureFuturesDao();
         this.ensureStockDao();
      } catch (Throwable var2) {
         Log.error("Error while history data init", var2);
      }
   }

   public synchronized void initAsync() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            HistoryDataManager.get().init();
         }
      }).start();
   }

   private void disposeDao(AbstractHistoryDataDao var1) {
      if (var1 != null) {
         var1.dispose();
      }
   }

   public synchronized void dispose() {
      this.disposeDao(this.futuresDao);
      this.disposeDao(this.stockDao);
   }

   public String getStockExchange(String var1) throws SQLException {
      this.ensureStockDao();
      return this.stockDao.getExchangeForTicker(var1);
   }

   public String getFuturesExchange(String var1) throws SQLException {
      this.ensureFuturesDao();
      return this.futuresDao.getExchangeForTicker(var1);
   }

   public void checkRenamedHistoryTickers() throws Exception {
      this.ensureStockDao();
      Log.debug("Checking renamed of stocks starting...");
      long var1 = System.currentTimeMillis();
      if (this.stockWasChanged) {
         int var3 = this.checkEvents();
         Log.info("Total renamed tickers {}", var3);
      }

      Log.debug("Checking renamed of stocks is over in {} ms", System.currentTimeMillis() - var1);
   }
}
