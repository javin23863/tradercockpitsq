package com.strategyquant.plugin.DataSource.impl.Dukascopy;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.SymbolData;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DukasDataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.qdm.QDM;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.dukascopy.CdnInfo;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DukasServlet extends HttpJSONServlet {
   DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyyMMdd");
   private static final Logger Log = LoggerFactory.getLogger(DukasServlet.class);
   private boolean canceled = false;
   private String availableDataResponse = null;

   public DukasServlet() {
      this.preloadAvailableDataResponse();
   }

   private void preloadAvailableDataResponse() {
      if (!MainApp.runInConsole()) {
         (new Thread() {
            @Override
            public void run() {
               try {
                  DukasServlet.this.availableDataResponse = DukasServlet.this.onGetDataList();
               } catch (Exception var2) {
                  DukasServlet.Log.error("Cannot load Dukas data.", var2);
               }
            }
         }).start();
      }
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getDataList":
            return this.onGetDataList();
         case "addData":
            return this.onAddData(var2);
         case "addCancel":
            return this.onAddCancel();
         case "importData":
            return this.onImportData(var2);
         case "importDataAction":
            return this.onImportDataAction(var2);
         case "setParallelDownload":
            return this.onSetParallelDownload(var2);
         case "getParallelDownload":
            return this.onGetParallelDownload();
         case "updateAll":
            return this.onUpdateAll();
         case "updateSelected":
            return this.onUpdateSelected(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onGetParallelDownload() {
      JSONObject var1 = new JSONObject();
      var1.put("success", "ok");
      var1.put("parallelDownload", MainApp.settings().get("parallelDownload", "3"));
      var1.put("cdnParallelDownload", MainApp.settings().get("cdnParallelDownload", "3"));
      var1.put("cdnPreferred", MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN));
      return var1.toString();
   }

   private String onSetParallelDownload(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "parallelDownload")[0];
      String var3 = this.tryGetParam(var1, "cdnParallelDownload")[0];
      String var4 = this.tryGetParam(var1, "cdnPreferred")[0];
      DownloadDispatcher.get().setQueueSizes(Integer.valueOf(var2), Integer.valueOf(var3));
      MainApp.settings().set("parallelDownload", var2);
      MainApp.settings().set("cdnParallelDownload", var3);
      MainApp.settings().set("cdnPreferred", var4);
      MainApp.settings().save();
      JSONObject var5 = new JSONObject();
      var5.put("success", "ok");
      return var5.toString();
   }

   private String onGetDataList() {
      if (this.availableDataResponse != null) {
         return this.availableDataResponse;
      }

      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      ArrayList var3 = new ArrayList();
      JSONArray var4 = new JSONArray();
      var4.put(new JSONObject().put("name", "All").put("value", ""));

      for (SymbolData var6 : DukasDataManager.get().getAvailableDataList()) {
         JSONObject var7 = new JSONObject();
         var7.put("symbol", var6.symbol);
         var7.put("name", var6.name);
         String var8 = var6.category + " - " + var6.subcategory;
         var7.put("category", var6.category);
         var7.put("subCategory", var6.subcategory);
         var7.put("fullCategory", var8);
         var7.put("dateFrom", var6.dateFrom);
         var7.put("dateFromM1", var6.dateFromM1);
         var7.put("decimals", var6.decimals);
         var7.put("tickValue", var6.tickValue);
         var7.put("defaultSpread", var6.defaultSpread);
         var7.put("tickSize", var6.tickSize);
         var7.put("tickStep", var6.tickStep);
         var7.put("instrumentType", var6.instrumentType);
         var2.put(var7);
         if (!var3.contains(var6.category)) {
            var3.add(var6.category);
            var4.put(new JSONObject().put("name", var6.category + " - All").put("value", var6.category));
         }

         if (!var3.contains(var8)) {
            var3.add(var8);
            var4.put(new JSONObject().put("name", var8).put("value", var8));
         }
      }

      var1.put("categories", var4);
      var1.put("data", var2);
      var1.put("success", L.t("Data listed", new Object[0]));
      return var1.toString();
   }

   private String onAddData(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols[]"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DukasServlet.this._onAdd(var1);
            }
         }).start();
      }

      var2.put("success", L.t("Add symbols.", new Object[0]));
      return var2.toString();
   }

   private void _onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         String[] var4 = this.tryGetParam(var1, "symbols[]");
         String[] var5 = var1.containsKey("instruments[]") ? this.tryGetParam(var1, "instruments[]") : null;
         String var6 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         String var7 = this.tryGetParam(var1, "dataType")[0];
         int var8 = Integer.valueOf(this.tryGetParamValue(var1, "broker"));
         if (!var7.equals("TICK") && !var7.equals("M1")) {
            throw new Exception(L.t("Invalid data type. Must be %s or %s", new Object[]{"TICK", "M1"}));
         }

         int var10 = 0;

         for (int var11 = 0; var11 < var4.length; var11++) {
            String var9 = var4[var11];
            String var12 = var5 == null ? null : var5[var11];
            if (var12 != null && var12.equals("-1")) {
               var12 = null;
            }

            String var13 = DukasDataManager.get().addData(var9, var6, var8, var12);
            DataManager.updateTimeframe("History", var13, var7);
            Log.info(String.format("Symbol '%s' added.", var13));
            var10 = SQUtils.round(100.0 * var11 / var4.length);
            var2.put("percent", var10);
            var2.put("info", L.t("Added symbol '%s'", new Object[]{var13}));
            DataManagerAddProgressSender.getInstance().sendData(var2);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(String.join(",", var4), "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            if (this.canceled) {
               break;
            }
         }
      } catch (Exception var17) {
         Log.error("Cannot add symbols. Exc.", var17);
         var3 = var17.getMessage();
      } finally {
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         if (var3 != null) {
            var2.put("error", var3);
         }

         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String onAddCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private String onUpdateAll() {
      try {
         ArrayList var1 = DataManager.list();
         ArrayList var2 = new ArrayList();

         for (int var3 = 0; var3 < var1.size(); var3++) {
            DataInfo var4 = (DataInfo)var1.get(var3);
            if (var4.source == 2 && var4.sourceDataId == 0) {
               var2.add((DataInfo)var1.get(var3));
            }
         }

         if (var2.isEmpty()) {
            return "No symbols to update";
         }

         HashMap var9 = new HashMap();
         String[] var10 = new String[var2.size()];
         String[] var5 = new String[var2.size()];

         for (int var6 = 0; var6 < var2.size(); var6++) {
            DataInfo var7 = (DataInfo)var2.get(var6);
            var10[var6] = var7.symbol;
            var5[var6] = var7.uSymbol;
         }

         var9.put("symbols", new String[]{String.join(",", var10)});
         var9.put("uSymbols", new String[]{String.join(",", var5)});
         var9.put("dateFrom", new String[]{"2013.01.01"});
         var9.put("dateTo", new String[]{SQTime.toUIDateString(System.currentTimeMillis())});
         var9.put("dateType", new String[]{"sinceLast"});
         var9.put("overwrite", new String[]{"true"});
         var9.put("dataType", new String[]{"M1"});
         String var11 = MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN);
         var9.put("downloadType", new String[]{var11});
         return this.onImportData(var9);
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var8);
      }
   }

   private String onUpdateSelected(Map<String, String[]> var1) {
      try {
         String[] var2 = this.tryGetParam(var1, "symbols")[0].split(",");
         String[] var3 = null;

         try {
            var3 = this.tryGetParam(var1, "datesFrom")[0].split(",");
         } catch (Exception var13) {
         }

         String[] var4 = null;

         try {
            var4 = this.tryGetParam(var1, "datesTo")[0].split(",");
         } catch (Exception var12) {
         }

         ArrayList var6 = new ArrayList();

         for (int var7 = 0; var7 < var2.length; var7++) {
            DataInfo var5 = DataManager.getDataInfo("History", var2[var7]);
            if (var5.source == 2 && var5.sourceDataId == 0) {
               var5.dateFromStr = var3 == null ? null : var3[var7];
               var5.dateToStr = var4 == null ? null : var4[var7];
               var6.add(var5);
            }
         }

         if (var6.isEmpty()) {
            return "No symbols to update";
         }

         HashMap var17 = new HashMap();
         var2 = new String[var6.size()];
         String[] var8 = new String[var6.size()];
         String[] var9 = new String[var6.size()];
         String[] var10 = new String[var6.size()];

         for (int var11 = 0; var11 < var6.size(); var11++) {
            DataInfo var16 = (DataInfo)var6.get(var11);
            var2[var11] = var16.symbol;
            var8[var11] = var16.uSymbol;
            var9[var11] = var16.dateFromStr;
            var10[var11] = var16.dateToStr;
         }

         var17.put("symbols", new String[]{String.join(",", var2)});
         var17.put("uSymbols", new String[]{String.join(",", var8)});
         var17.put("dateFrom", new String[]{"2013.01.01"});
         var17.put("dateTo", new String[]{SQTime.toUIDateString(System.currentTimeMillis())});
         var17.put("dateType", new String[]{"sinceLast"});
         var17.put("overwrite", new String[]{"true"});
         var17.put("dataType", new String[]{"M1"});
         String var18 = MainApp.settings().get("cdnPreferred", DataSources.DOWNLOAD_TYPE_CDN);
         var17.put("downloadType", new String[]{var18});
         if (var3 != null && var4 != null) {
            var17.put("datesFrom", new String[]{String.join(",", var9)});
            var17.put("datesTo", new String[]{String.join(",", var10)});
         }

         return this.onImportData(var17);
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var14);
      }
   }

   private String onImportData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "symbols")[0].split(",");
         String[] var4 = this.tryGetParam(var1, "uSymbols")[0].split(",");
         long var5 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
         long var7 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
         String var9 = this.tryGetParam(var1, "dateType")[0];
         boolean var10 = this.tryGetParam(var1, "overwrite")[0].equals("true");
         String[] var11 = null;

         try {
            var11 = this.tryGetParam(var1, "datesFrom")[0].split(",");
         } catch (Exception var24) {
         }

         String[] var12 = null;

         try {
            var12 = this.tryGetParam(var1, "datesTo")[0].split(",");
         } catch (Exception var23) {
         }

         String var13 = this.getParam(var1, "downloadType", DataSources.DOWNLOAD_TYPE_STANDARD);
         int var14 = Integer.parseInt(MainApp.settings().get("parallelDownload", "3"));
         int var15 = Integer.parseInt(MainApp.settings().get("cdnParallelDownload", "3"));
         DownloadDispatcher.get().setQueueSizes(var14, var15);

         for (int var16 = 0; var16 < var3.length; var16++) {
            String var17 = var3[var16];
            DataInfo var18 = DataManager.getDataInfo("History", var17);
            if (var18.source == 2) {
               if (var11 != null && var12 != null) {
                  var5 = SQTime.parseToMilis(var11[var16], "yyyy.MM.dd");
                  var7 = SQTime.parseToMilis(var12[var16], "yyyy.MM.dd");
               }

               ImportInfo var19 = new ImportInfo();
               var19.origSymbol = var4[var16];
               var19.symbol = var17;
               var19.overwrite = var10;
               var19.downloadType = var13;
               String var20 = var13;
               if (MainApp.v571hfnsHw().a1wUchdumV() && !MainApp.v571hfnsHw().xpoHYYsX() && !DukasDataManager.get().canFreeDownloadFromCdn(var18)) {
                  var20 = DataSources.DOWNLOAD_TYPE_STANDARD;
               }

               var19.useCDN = DataSources.isCDN(var20);
               var19.instrumentInfo = var18.symbolInfo;
               var19.rows = var18.rows;
               var19.oldDateFrom = var18.dateFrom;
               var19.oldDateTo = var18.dateTo;
               var19.ignoreWeekend = var18.symbolInfo.dataType == 3;
               boolean var21 = var18.timeframe != null && var18.timeframe.equals("M1");
               SymbolData var22 = DukasDataManager.get().getAvailableDataInfo(var19.origSymbol);
               if (var22 == null) {
                  throw new Exception(L.t("No info found for symbol '%s'.", new Object[]{var19.origSymbol}));
               }

               var19.priceConstant = Math.pow(10.0, -var22.decimals);
               var19.downloadM1Data = var21;
               var19.dateFrom = var5;
               var19.dateTo = var7;
               if (var9.equals("allTime")) {
                  var19.dateFrom = var21 ? var22.dateFromM1 : var22.dateFrom;
               } else if (var9.equals("sinceLast")) {
                  var19.dateFrom = var18.dateTo > 0L ? var18.dateTo : (var21 ? var22.dateFromM1 : var22.dateFrom);
               }

               if (var19.dateFrom > var19.dateTo) {
                  return apiErrorJSON(L.t("Date from must be before Date to.", new Object[0]), null);
               }

               this.sanitizeDates(var19, var21, var22);
               if (var21) {
                  QDM.getInstance().activity.increase(var19.useCDN ? 4 : 2);
               } else {
                  QDM.getInstance().activity.increase(var19.useCDN ? 3 : 1);
               }

               DownloadDispatcher.get().update(var18, var19);
            }
         }
      } catch (Exception var25) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var25);
      }

      var2.put("success", L.t("Data import started", new Object[0]));
      return var2.toString();
   }

   private void sanitizeDates(ImportInfo var1, boolean var2, SymbolData var3) {
      if (var2) {
         if (var1.dateFrom < var3.dateFromM1) {
            var1.dateFrom = var3.dateFromM1;
         }
      } else if (var1.dateFrom < var3.dateFrom) {
         var1.dateFrom = var3.dateFrom;
      }
   }

   private void fillCdnInfos(CdnInfo var1, String var2) throws ClientProtocolException, IOException, IllegalStateException, JDOMException {
      CloseableHttpResponse var3 = HttpClientBuilder.create().build().execute(new HttpGet(var2));
      var3.getEntity().getContent();
      SAXBuilder var4 = new SAXBuilder();
      Document var5 = var4.build(var3.getEntity().getContent());
      Element var6 = var5.getRootElement();

      for (Element var9 : var6.getChildren("data")) {
         String var10 = var9.getAttributeValue("symbol");
         String var11 = var9.getAttributeValue("url");
         String var12 = var9.getAttributeValue("format");
         String var13 = var9.getAttributeValue("tf");
         var1.addSymbolInfo(var10, var13, var11, var12);
         Log.debug("CDN metadata added:" + var10 + ", timeframe:" + var13 + ", url:" + var11);
      }
   }

   private String onImportDataAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      MultiProgressListener var5 = DataManagerDataProgress.get().createListener(var2);
      switch (var3) {
         case "stop":
            DownloadDispatcher.get().stop(var2);
            break;
         case "pause":
            var5.onPause();
            DownloadDispatcher.get().pause(var2);
            break;
         case "continue":
            var5.onContinue();
            DownloadDispatcher.get().resume(var2);
      }

      var4.put("success", "ok");
      return var4.toString();
   }
}
