package com.strategyquant.tradinglib.data.download;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataFolderSweeper;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.StockGroupUpdateErrorManager;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.lib.tempfiles.TempFilesManager;
import com.strategyquant.tradinglib.crypto.CryptoDownloadJob;
import com.strategyquant.tradinglib.darwinex.DarwinexImportManager;
import com.strategyquant.tradinglib.dukascopy.CdnInfo;
import com.strategyquant.tradinglib.dukascopy.DukasImportManager;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.historyData.DownloadEodStockHistorySymbolJob;
import com.strategyquant.tradinglib.historyData.DownloadHistorySymbolJob;
import com.strategyquant.tradinglib.historyData.EodHistoryDataCache;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.mt5api.Mt5ApiDownloaderJob;
import com.strategyquant.tradinglib.mt5api.Mt5ApiImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.yahoo.YahooDownloadJob;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadDispatcher implements IDownloadDispatcher {
   private static final int BATCH_UPDATE = 10;
   private static final long MIN_SYNC_UI_DELTA = 1500L;
   private long lastSync;
   private List<JSONObject> symbolsForUpdate = new LinkedList<>();
   private IGridMessageListener gridFinishListener = new IGridMessageListener() {
      public void messageReceived(GridMessage var1) {
         if (var1.getMessageID() == 1) {
            String var2 = var1.getJobDetails().getJobID();
            String var3 = var2.substring("downloadjob_".length());
            DownloadDispatcher.this.downloadFinished(var3);
            DownloadDispatcher.this.jobsFinished++;
            DownloadDispatcher.this.updateTotal();
            DownloadDispatcher.this.tryToRunDownload();
            DownloadDispatcher.this.updateUI(var3);
            if (!DownloadDispatcher.this.commonQueue.isBusy() && !DownloadDispatcher.this.cdnQueue.isBusy() && !DownloadDispatcher.this.skipReset) {
               DownloadDispatcher.this.jobsFinished = 0;
               DownloadDispatcher.this.jobsToBeHandled = 0;
            }

            if (!DownloadDispatcher.this.commonQueue.isBusy() && !DownloadDispatcher.this.cdnQueue.isBusy()) {
               DownloadDispatcher.this.finishBatch();
            }
         }

         for (int var4 = 0; var4 < DownloadDispatcher.this.extraListeners.size(); var4++) {
            DownloadDispatcher.this.extraListeners.get(var4).messageReceived(var1);
         }
      }
   };
   public static final String NEW_CDN_USER = "cdnuser";
   public static final String NEW_CDN_PASS = "QmwwrOrdxKK52pPUmjkf";
   private static final String DARWINEX_CDN_BASE_URL = "https://cdn.strategyquantcdn.com/data/darwinex";
   private static final String DARWINEX_HK_BASE_CDN_URL = "https://cdn005.strategyquantcdn.com/data/darwinex";
   private static final String DUKASCOPY_CDN_BASE_URL = "https://cdn.strategyquantcdn.com/data/dukascopy";
   private static final String DUKASCOPY_HK_BASE_CDN_URL = "https://cdn005.strategyquantcdn.com/data/dukascopy";
   public static final String SYMBOL_TOTAL = "TotalProgressSymbol";
   public static final String JOB_PREFIX = "downloadjob_";
   private static final Logger Log = LoggerFactory.getLogger(DownloadDispatcher.class);
   private static final int TIMEOUT = 10;
   private static DownloadDispatcher instance = new DownloadDispatcher();
   private Queue commonQueue = new Queue(this);
   private Queue cdnQueue = new Queue(this);
   private Set<String> cryptoExchangeDownload = new HashSet<>();
   private AtomicBoolean mt5apiDownload = new AtomicBoolean(false);
   private ReentrantLock lock = new ReentrantLock(true);
   private ArrayList<IGridMessageListener> extraListeners = new ArrayList<>();
   private MultiProgressListener totalListener;
   private volatile int jobsFinished = 0;
   private volatile int jobsToBeHandled = 0;
   private volatile boolean skipReset = false;
   private boolean newCdn = false;
   private String customDukascopyBaseCdnUrl;
   private String customDarwinexBaseCdnUrl;

   private void updateUI(String var1) {
      try {
         if (!MainApp.runInConsole()) {
            boolean var2 = !this.commonQueue.isBusy() && !this.cdnQueue.isBusy();
            synchronized (this.symbolsForUpdate) {
               this.symbolsForUpdate.add(this.getSymbolInfo(var1));
               long var4 = System.currentTimeMillis() - this.lastSync;
               if (var2 || this.symbolsForUpdate.size() % 10 == 0 && var4 > 1500L) {
                  this.sendUpdateUI();
               }
            }
         }
      } catch (Exception var8) {
         Log.error("Error while updating UI", var8);
      }
   }

   private JSONObject getSymbolInfo(String var1) throws DataException {
      DataInfo var2 = DataManager.getDataInfo("History", var1);
      JSONObject var3 = new JSONObject();
      var3.put("symbol", var2.symbol);
      var3.put("barType", var2.barTimeType);
      var3.put("dateFrom", var2.dateFrom);
      var3.put("dateTo", var2.dateTo);
      var3.put("rows", var2.rows);
      var3.put("totalDays", SQTime.getDaysBetween(var2.dateFrom, var2.dateTo));
      var3.put("connection", var2.connection);
      var3.put("filename", var2.filename);
      var3.put("instrument", var2.instrument);
      var3.put("timeframe", var2.timeframe);
      var3.put("timezone", Timezone.print(var2.timezone, var2.source, false));
      var3.put("timezoneShort", Timezone.print(var2.timezone, var2.source, true));
      var3.put("source", var2.source);
      var3.put("secondsRecords", var2.secondsRecords);
      InstrumentInfo var4 = InstrumentManager.getInstrumentInfo(var2.instrument);
      var3.put("pointValue", var4.pointValue);
      var3.put("tickSize", var4.tickSize);
      var3.put("tickStep", var4.tickStep);
      var3.put("dataType", var4.dataType);
      var3.put("spread", var4.defaultSpread);
      var3.put("slippage", var4.defaultSlippage);
      var3.put("commissions", var4.commissions);
      var3.put("swap", var4.swap);
      var3.put("uSymbol", var2.uSymbol);
      var3.put("uSymbolName", var2.uSymbolName);
      var3.put("id", var2.id);
      var3.put("sourceDataId", var2.sourceDataId);
      var3.put("show", var2.show);
      var3.put("broker", var4.broker);
      var3.put("brokerName", BrokerManager.getInstance().getBrokerName(var4.broker));
      return var3;
   }

   public boolean isEmpty() {
      return !this.cdnQueue.isBusy() && !this.commonQueue.isBusy();
   }

   private void sendUpdateUI() {
      JSONObject var1 = new JSONObject();
      var1.put("action", "download_single");
      synchronized (this.symbolsForUpdate) {
         var1.put("data", new JSONArray(this.symbolsForUpdate.toArray()));
         this.symbolsForUpdate.clear();
         this.lastSync = System.currentTimeMillis();
      }

      SQWebSocketManager.addToDataQueue(new DataToSend("data", var1), "SQUANT", "QDM", "AlgoWizard");
   }

   private void finishBatch() {
      DataManager.flushUpdatedData();
      if (BasketOfStocksManager.getInstance() != null) {
         BasketOfStocksManager.getInstance().updateGroupsOfSymbols();
         SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), "SQUANT", "QDM", "AlgoWizard");
         this.sendStockGroupErrorsToUI();
      }

      try {
         if (!MainApp.runInConsole()) {
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "download"), "SQUANT", "QDM", "AlgoWizard");
         }
      } catch (Exception var2) {
         Log.error("Error while updating UI", var2);
      }
   }

   private void sendStockGroupErrorsToUI() {
      String var1 = this.getDownloadGroupSummaryError();
      if (var1 != null) {
         this.totalListener.setMessage(var1);
      }
   }

   private String getDownloadGroupSummaryError() {
      Map var1 = StockGroupUpdateErrorManager.getInstance().finished();
      if (var1 == null) {
         return null;
      }

      Set var2 = var1.keySet();
      HashMap var3 = new HashMap();

      for (String var5 : var2) {
         Set var6 = BasketOfStocksManager.getInstance().getGroupsForStock(var5);
         if (var6 != null) {
            for (Integer var8 : var6) {
               List var9 = (List)var3.get(var8);
               if (var9 == null) {
                  var9 = new LinkedList();
                  var3.put(var8, var9);
               }

               var9.add((String)var1.get(var5));
            }
         }
      }

      if (var3.isEmpty()) {
         return null;
      }

      StringBuilder var11 = new StringBuilder();

      for (Integer var14 : var3.keySet()) {
         BasketDto var15 = BasketOfStocksManager.getInstance().getBasket(var14);
         List var16 = (List)var3.get(var14);
         var11.append("Updated group " + var15.getName() + ", " + var16.size() + " error" + (var16.size() > 1 ? "s" : "") + System.lineSeparator());

         for (int var17 = 0; var17 < var16.size(); var17++) {
            String var10 = (String)var16.get(var17);
            var11.append(var10);
            var11.append(System.lineSeparator());
         }
      }

      String var13 = var11.toString();
      return var13.trim().replace(System.lineSeparator(), "<br>");
   }

   public DownloadDispatcher() {
      this.totalListener = DataManagerDataProgress.get().createListener("TotalProgressSymbol", "");
      File var1 = new File(MainApp.getDataPath() + File.separator + "user", "cdn.txt");
      if (var1.exists()) {
         try {
            List var2 = Files.readAllLines(var1.toPath());
            this.customDukascopyBaseCdnUrl = ((String)var2.get(0)).trim();
            this.customDarwinexBaseCdnUrl = ((String)var2.get(1)).trim();
         } catch (Exception var3) {
            Log.error("Error while loading cdn.txt content", var3);
         }
      }
   }

   private boolean useHkServer() {
      String var1 = MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN);
      return DataSources.DOWNLOAD_TYPE_CDN_CN.equals(var1);
   }

   private String getDarwinexBaseUrl() {
      if (this.customDarwinexBaseCdnUrl != null) {
         return this.customDarwinexBaseCdnUrl;
      } else {
         return this.useHkServer() ? "https://cdn005.strategyquantcdn.com/data/darwinex" : "https://cdn.strategyquantcdn.com/data/darwinex";
      }
   }

   private String getDukascopyBaseUrl(ImportInfo var1) {
      if (!var1.useCDN) {
         return null;
      } else {
         String var2 = var1.downloadM1Data ? "m1" : "tick";
         if (this.customDukascopyBaseCdnUrl != null) {
            return this.customDukascopyBaseCdnUrl + "/" + var2;
         } else {
            return DataSources.DOWNLOAD_TYPE_CDN_CN.equals(var1.downloadType)
               ? "https://cdn005.strategyquantcdn.com/data/dukascopy/" + var2
               : "https://cdn.strategyquantcdn.com/data/dukascopy/" + var2;
         }
      }
   }

   private void updateTotal() {
      try {
         Log.debug("Total updating");
         this.lock.lock();
         double var1 = 0.0;
         if (this.jobsToBeHandled != 0) {
            var1 = (double)this.jobsFinished / this.jobsToBeHandled;
         } else if (this.jobsFinished > 0) {
            var1 = 1.0;
         }

         this.totalListener.setMessage(this.jobsFinished + " / " + this.jobsToBeHandled);
         this.totalListener.onProgress(var1 * 100.0);
      } finally {
         this.lock.unlock();
      }

      Log.debug("Total updated");
   }

   private void tryToRunDownload() {
      this.commonQueue.tryToRunDownload();
      this.cdnQueue.tryToRunDownload();
   }

   @Override
   public boolean isMt5DownloadExecuted() {
      return this.mt5apiDownload.get();
   }

   private CdnInfo readDukascopyCdnInfos(String var1) throws ClientProtocolException, IOException, IllegalStateException, JDOMException {
      CdnInfo var2 = new CdnInfo();
      RequestConfig var3 = RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build();
      CloseableHttpResponse var4 = null;

      try {
         var4 = HttpClientBuilder.create().setDefaultRequestConfig(var3).build().execute(new HttpGet(var1));
      } catch (UnknownHostException var16) {
         Log.error("Error while downloade descriptor. Unknown host: " + var1, var16);
         throw new RuntimeException("The server address could not be resolved. This may be due to a DNS or network issue.");
      }

      var4.getEntity().getContent();
      SAXBuilder var5 = new SAXBuilder();
      Document var6 = null;

      try {
         var6 = var5.build(var4.getEntity().getContent());
      } catch (Exception var15) {
         Log.error("Error while parse CDN descriptor: " + var1, var15);
         throw new RuntimeException("Error while downloading and parsing descriptor XML.");
      }

      Element var7 = var6.getRootElement();

      for (Element var10 : var7.getChildren("data")) {
         String var11 = var10.getAttributeValue("symbol");
         String var12 = var10.getAttributeValue("url");
         String var13 = var10.getAttributeValue("format");
         String var14 = var10.getAttributeValue("tf");
         var2.addSymbolInfo(var11, var14, var12, var13);
         Log.debug("CDN metadata added: {}, timeframe:{}, url:{}", new Object[]{var11, var14, var12});
      }

      return var2;
   }

   public static DownloadDispatcher get() {
      return instance;
   }

   public void updateHistoryData(List<DataInfo> var1, Map<String, TickerDto> var2) throws Exception {
      try {
         this.skipReset = true;
         this.lock.lock();
         boolean var3 = false;

         for (DataInfo var5 : var1) {
            TickerDto var6 = (TickerDto)var2.get(var5.uSymbol);
            this.testSubscriptions(var5, var6);
            if (var6 != null && var5.sourceDataId == 0) {
               boolean var7 = this.registerUpdate(var5, var6, null, true);
               var3 = var7 || var3;
            }
         }

         if (!var3 && !this.commonQueue.isBusy() && !this.cdnQueue.isBusy()) {
            this.sendStockGroupErrorsToUI();
         }
      } finally {
         this.skipReset = false;
         this.lock.unlock();
      }

      this.cdnQueue.tryToRunDownload();
   }

   private void testSubscriptions(DataInfo var1, TickerDto var2) {
      String var3 = var1.timeframe;
      boolean var4 = !var3.equals("M1");
      if (var1.source == 5 && !HistoryDataSubscription.getInstance().isAllowedEquity(var1.uSymbol, var4)) {
         throw new RuntimeException(L.t("You don't have subscriptions for requested data.", new Object[0]));
      }

      if (var1.source == 6 && !HistoryDataSubscription.getInstance().isAllowedFuture(var1.uSymbol, var2.getMarketName(), var4)) {
         throw new RuntimeException(L.t("You don't have subscriptions for requested data.", new Object[0]));
      }
   }

   public void updateHistoryData(DataInfo var1, TickerDto var2) throws Exception {
      try {
         this.lock.lock();
         if (var2 == null) {
            Log.info("Can't download {}. Ticker definition was not found. Ticker was probably removed from central database.", var1.symbol);
         }

         if (var2 != null && var1.sourceDataId == 0) {
            this.testSubscriptions(var1, var2);
            this.registerUpdate(var1, var2, null, true);
         }
      } finally {
         this.lock.unlock();
      }

      this.cdnQueue.tryToRunDownload();
   }

   private boolean registerUpdate(DataInfo var1, TickerDto var2, ImportInfo var3, boolean var4) throws Exception {
      if ((!var4 || !this.cdnQueue.exists(var1.symbol)) && (var4 || !this.commonQueue.exists(var1.symbol))) {
         TickerDto var5 = var2;
         if (var2 != null && var2.getAliasTickerId() != null) {
            boolean var6 = var1.source == 5;
            TickerDto var7 = var6
               ? HistoryDataManager.get().getStockTicker(var2.getAliasTickerId())
               : HistoryDataManager.get().getFutureTicker(var2.getAliasTickerId());
            if (var7 != null) {
               var5 = var7;
            }
         }

         if (var5 != null && var5.getDateTo().getTime() <= var1.dateTo) {
            this.jobsToBeHandled++;
            this.jobsFinished++;
            this.updateTotal();
            return false;
         }

         DownloadDispatcher.Details var8 = new DownloadDispatcher.Details();
         var8.ticker = var2;
         var8.importInfo = var3;
         var8.symbol = var1.symbol;
         var8.dataInfo = var1;
         var8.listener = createListener(var1);
         var8.listener.onProgress(0.1);
         var8.listener.setMessage(L.t("Waiting in queue", new Object[0]));
         if (var4) {
            this.cdnQueue.register(var1.symbol, var8);
         } else {
            this.commonQueue.register(var1.symbol, var8);
         }

         Log.debug("Symbol: {} registered for download.", var1.symbol);
         this.jobsToBeHandled++;
         this.updateTotal();
         return true;
      } else {
         Log.debug("Symbol is already in queue or running: {}", var1.symbol);
         return false;
      }
   }

   public void update(DataInfo var1, ImportInfo var2) throws Exception {
      boolean var3 = false;

      try {
         this.lock.lock();
         var3 = this.registerUpdate(var1, null, var2, var2.useCDN);
      } finally {
         this.lock.unlock();
      }

      if (var3) {
         this.tryToRunDownload();
      }
   }

   public static DataManagerProgressListener createListener(DataInfo var0) throws Exception {
      String var1 = null;
      switch (var0.source) {
         case 2:
            var1 = "dataSourceDukascopy/importDataAction";
            break;
         case 3:
            var1 = "dataSourceYahoo/importDataAction";
            break;
         case 4:
            var1 = "darwinex/downloadDataAction";
            break;
         case 5:
            var1 = "sqEquityData/updateDataAction";
            break;
         case 6:
            var1 = "sqFuturesData/updateDataAction";
            break;
         case 7:
            var1 = "dataSourceCrypto/importDataAction";
            break;
         case 8:
            var1 = "dataSourceMt5Api/downloadDataAction";
            break;
         default:
            throw new NotDownloadableDataException("Not supported datasource: " + var0.source);
      }

      return (DataManagerProgressListener)DataManagerDataProgress.get().createListener(var0.symbol, var1);
   }

   public void setQueueSizes(int var1, int var2) {
      this.lock.lock();
      boolean var3 = this.commonQueue.setQueueSize(var1);
      boolean var4 = this.cdnQueue.setQueueSize(var2);
      this.lock.unlock();
      if (var4) {
         this.cdnQueue.tryToRunDownload();
      }

      if (var3) {
         this.commonQueue.tryToRunDownload();
      }
   }

   public int getQueueSize() {
      return this.commonQueue.getQueueSize();
   }

   @Override
   public boolean isCryptoExchangeDownload(String var1) {
      return this.cryptoExchangeDownload.contains(var1);
   }

   @Override
   public void extecuteDownloadJob(DownloadDispatcher.Details var1) throws Exception {
      int var2 = var1.dataInfo.source;

      try {
         if (var2 == 5 || var2 == 6) {
            this.executeHistoryDownload(var1);
         } else if (var2 == 2) {
            this.executeDukascopyDownload(var1);
         } else if (var2 == 4) {
            this.executeDarwinexDownload(var1);
         } else if (var2 == 3) {
            this.executeYahooDownload(var1);
         } else if (var2 == 7) {
            this.executeCryptoDownload(var1);
         } else if (var2 == 8) {
            this.executeMt5Download(var1);
         }

         DataFolderSweeper.get().setSkip(true);
         TempFilesManager.get().setAutoDeleteEnabled(false);
         Log.debug("Executed download for: {}", var1.symbol);
      } catch (Exception var4) {
         this.jobsFinished++;
         throw var4;
      }
   }

   private void executeMt5Download(DownloadDispatcher.Details var1) throws Exception {
      String var2 = getJobIdent(var1.dataInfo.symbol);
      Mt5ApiDownloaderJob var3 = new Mt5ApiDownloaderJob(var2, var1.listener, var1.dataInfo, (Mt5ApiImportInfo)var1.importInfo);
      this.registerListener(var2);
      this.mt5apiDownload.set(true);
      SQGrid.getGridClient().executeOnGrid(var2, var3);
   }

   private void executeCryptoDownload(DownloadDispatcher.Details var1) throws Exception {
      String var2 = getJobIdent(var1.dataInfo.symbol);
      CryptoDownloadJob var3 = new CryptoDownloadJob(var2, var1.listener, var1.dataInfo, var1.importInfo);
      this.cryptoExchangeDownload.add(var1.dataInfo.uSymbolName);
      this.registerListener(var2);
      SQGrid.getGridClient().executeOnGrid(var2, var3);
   }

   private void executeYahooDownload(DownloadDispatcher.Details var1) throws Exception {
      String var2 = getJobIdent(var1.dataInfo.symbol);
      YahooDownloadJob var3 = new YahooDownloadJob(var2, var1.listener, var1.dataInfo, var1.importInfo);
      this.registerListener(var2);
      SQGrid.getGridClient().executeOnGrid(var2, var3);
   }

   private void executeDukascopyDownload(DownloadDispatcher.Details var1) throws Exception {
      String var2 = getJobIdent(var1.dataInfo.symbol);
      String var3 = this.getDukascopyBaseUrl(var1.importInfo);
      GridJob var4 = DukasImportManager.get().importData(var2, var3, var1.importInfo, var1.listener, this.gridFinishListener);
      SQGrid.getGridClient().executeOnGrid(var2, var4);
   }

   private void registerListener(String var1) {
      GridClient var2 = SQGrid.getGridClient();
      if (var2.isRegisteredMessageListener(var1)) {
         var2.removeMessageListener(var1);
      }

      var2.registerMessageListener(var1, this.gridFinishListener);
   }

   private void executeDarwinexDownload(DownloadDispatcher.Details var1) throws Exception {
      String var2 = getJobIdent(var1.dataInfo.symbol);
      String var3 = this.getDarwinexBaseUrl();
      GridJob var4 = DarwinexImportManager.get().importData(var2, var3, var1.importInfo, var1.listener, this.gridFinishListener);
      SQGrid.getGridClient().executeOnGrid(var2, var4);
   }

   private void executeHistoryDownload(DownloadDispatcher.Details var1) throws Exception {
      DataInfo var2 = var1.dataInfo;
      String var3 = getJobIdent(var2.symbol);
      TickerFilterDto var4 = new TickerFilterDto();
      var4.setSearchInTicker(true);
      var4.setExactMatch(true);
      var4.setNames(new String[]{var2.uSymbol});
      boolean var5 = var2.source == 5;
      if (var5 && var1.ticker.isEod()) {
         DownloadEodStockHistorySymbolJob var7 = new DownloadEodStockHistorySymbolJob(
            var3, var2, var5 ? TickerKind.STOCK : TickerKind.FUTURES, var1.ticker, false
         );
         var7.setProgressListener(var1.listener);
         this.registerListener(var3);
         SQGrid.getGridClient().executeOnGrid(var3, var7);
      } else {
         DownloadHistorySymbolJob var6 = new DownloadHistorySymbolJob(var3, var2, var5 ? TickerKind.STOCK : TickerKind.FUTURES, var1.ticker);
         var6.setProgressListener(var1.listener);
         this.registerListener(var3);
         SQGrid.getGridClient().executeOnGrid(var3, var6);
      }
   }

   private void downloadFinished(String var1) {
      int var2;
      int var3;
      try {
         this.lock.lock();
         if (!this.cdnQueue.downloadJobFinished(var1)) {
            this.commonQueue.downloadJobFinished(var1);
         }

         DataInfo var4 = DataManager.getDataInfo("History", var1);
         if (var4.source == 7) {
            this.cryptoExchangeDownload.remove(var4.uSymbolName);
         } else if (var4.source == 8) {
            this.mt5apiDownload.set(false);
         }

         var2 = this.cdnQueue.getRunningJobsCount() + this.commonQueue.getRunningJobsCount();
         var3 = this.cdnQueue.getWaitingJobsCount() + this.commonQueue.getWaitingJobsCount();
         if (var2 == 0 && var3 == 0) {
            TempFilesManager.get().setAutoDeleteEnabled(true);
            EodHistoryDataCache.getInstance().reset();
         }
      } finally {
         this.lock.unlock();
      }

      Log.info("Finished: {}, already executed: {}, in queue: {}", new Object[]{var1, var2, var3});
      SQGrid.getGridClient().removeMessageListener(getJobIdent(var1));
   }

   private String getGridGroup(String var1) {
      return getJobIdent(var1);
   }

   @Override
   public void performPause(String var1, DownloadDispatcher.Details var2) {
      if (var2 != null) {
         var2.paused = true;
         var2.listener.onPause();
      } else {
         SQGrid.getGridClient().pause(this.getGridGroup(var1));
         DataManagerDataProgress.get().createListener(var1).onPause();
      }

      Log.debug("Paused symbol: {}", var1);
   }

   @Override
   public void performResume(String var1, DownloadDispatcher.Details var2) {
      if (var2 != null) {
         var2.paused = false;
         var2.listener.onContinue();
      } else {
         SQGrid.getGridClient().restart(this.getGridGroup(var1));
         DataManagerDataProgress.get().createListener(var1).onContinue();
      }

      Log.debug("Resumed symbol: {}", var1);
   }

   @Override
   public void performStop(String var1, DownloadDispatcher.Details var2) {
      if (var2 != null) {
         var2.listener.onProgress(100.0);
         var2.listener.setMessage(L.t("Stopped", new Object[0]));
         this.jobsFinished++;
      } else {
         SQGrid.getGridClient().stop(this.getGridGroup(var1));
      }

      Log.debug("Stopped symbol: {}", var1);
   }

   public static String getJobIdent(String var0) {
      return "downloadjob_" + var0;
   }

   public void stopAll() {
      try {
         this.lock.lock();
         this.commonQueue.stopAll();
         this.cdnQueue.stopAll();
         this.jobsFinished = this.jobsToBeHandled;
      } finally {
         this.lock.unlock();
      }

      DataManager.flushUpdatedData();
      this.updateTotal();
   }

   public void stop(String var1) {
      try {
         this.lock.lock();
         DownloadDispatcher.Details var2 = this.removeFromDownloadQueue(var1);
         this.performStop(var1, var2);
      } finally {
         this.lock.unlock();
      }

      this.tryToRunDownload();
      this.updateTotal();
   }

   private DownloadDispatcher.Details removeFromDownloadQueue(String var1) {
      DownloadDispatcher.Details var2 = this.cdnQueue.removeFromDownloadQueue(var1);
      return var2 != null ? var2 : this.commonQueue.removeFromDownloadQueue(var1);
   }

   private DownloadDispatcher.Details getFromDownloadQueue(String var1) {
      DownloadDispatcher.Details var2 = this.cdnQueue.getFromDownloadQueue(var1);
      return var2 != null ? var2 : this.commonQueue.getFromDownloadQueue(var1);
   }

   public void pauseAll() {
      try {
         this.lock.lock();
         this.commonQueue.pauseAll();
         this.cdnQueue.pauseAll();
      } finally {
         this.lock.unlock();
         Log.debug("Paused all");
      }
   }

   public void pause(String var1) {
      try {
         this.lock.lock();
         DownloadDispatcher.Details var2 = this.getFromDownloadQueue(var1);
         this.performPause(var1, var2);
      } finally {
         this.lock.unlock();
      }
   }

   public void resumeAll() {
      try {
         this.lock.lock();
         this.commonQueue.resumeAll();
         this.cdnQueue.resumeAll();
      } finally {
         this.lock.unlock();
         Log.debug("Resumed all");
      }

      this.tryToRunDownload();
   }

   public void resume(String var1) {
      try {
         this.lock.lock();
         DownloadDispatcher.Details var2 = this.getFromDownloadQueue(var1);
         this.performResume(var1, var2);
      } finally {
         this.lock.unlock();
      }
   }

   public void addExtraListener(IGridMessageListener var1) {
      this.extraListeners.add(var1);
   }

   public void removeExtraListener(IGridMessageListener var1) {
      this.extraListeners.remove(var1);
   }

   static class Details {
      public String symbol;
      public DataInfo dataInfo;
      public DataManagerProgressListener listener;
      public boolean paused = false;
      public TickerDto ticker;
      public ImportInfo importInfo;
   }
}
