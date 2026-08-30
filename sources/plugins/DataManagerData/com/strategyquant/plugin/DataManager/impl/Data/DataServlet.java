package com.strategyquant.plugin.DataManager.impl.Data;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.consts.DataTypes;
import com.strategyquant.datalib.data.BatchProgressController;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinReaderNew;
import com.strategyquant.datalib.data.io.newDataFormat.DataBinWriterNew;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.metatrader4.MT4Utils;
import com.strategyquant.datalib.metatrader4.Mt4Properties;
import com.strategyquant.datalib.metatrader4.Mt4SymbolProperties;
import com.strategyquant.datalib.metatrader4.MT4Utils.MetaTraderLocation;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.datalib.timezone.Timezones;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.CsvExportJob;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.format.Format;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.format.Formats;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.Items;
import com.strategyquant.plugin.DataManager.impl.Data.job.CloneToTimezoneJob;
import com.strategyquant.plugin.DataManager.impl.Data.job.MT4ExportJob;
import com.strategyquant.plugin.DataManager.impl.Data.job.MT5ExportJob;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.historyData.SpecialSymbolsManager;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerConfirm;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerSaveProgreessSender;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.quality.QualityChecker;
import com.strategyquant.tradinglib.stockchart.PreviewCaption;
import com.strategyquant.tradinglib.stockchart.StockChartData;
import com.strategyquant.tradinglib.stockchart.StockData;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.http.util.LangUtils;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   private static final Logger Log = LoggerFactory.getLogger(DataServlet.class);
   private static final String MT4_EXPORT_JOB_ID = "MT4_Export_data_job";
   private static final String MT5_EXPORT = "MT5_Export";
   private static final int BATCH_LIMIT = 5;
   private volatile boolean canceled = false;

   private void checkLimitedSymbol(String var1) {
      if (DataManager.isLimited(var1)) {
         throw new IllegalArgumentException("Operation can't be done on limited symbols");
      }
   }

   private void checkLimitedSymbols(String[] var1) {
      for (String var5 : var1) {
         this.checkLimitedSymbol(var5);
      }
   }

   private int getLimitedCount(String[] var1) {
      int var2 = 0;

      for (String var6 : var1) {
         if (DataManager.isLimited(var6)) {
            var2++;
         }
      }

      return var2;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      this.canceled = false;
      switch (var1) {
         case "listData":
            return this.onListData(var2);
         case "editData":
            return this.onEditData(var2);
         case "cancelDataOperation":
            return this.onCancelDataOperation(var2);
         case "clearData":
            return this.onClearData(var2);
         case "removeData":
            return this.onRemoveData(var2);
         case "showData":
            return this.onShowData(var2);
         case "saveDataChanges":
            return this.onSaveDataChanges(var2);
         case "getSymbolData":
            return this.onGetSymbolData(var2);
         case "addTimeframe":
            return this.onAddTimeframe(var2);
         case "reviewData":
            return this.onReviewData(var2);
         case "getIndexForDate":
            return this.onGetIndexForDate(var2);
         case "reviewChart":
            return this.onReviewChart(var2);
         case "cloneToTimezone":
            return this.onCloneToTimezone(var2);
         case "cloneToTimezoneAction":
            return this.onCloneToTimezoneAction(var2);
         case "exportTick":
            return this.onExport(true, var2);
         case "exportM1":
            return this.onExport(false, var2);
         case "exportCDN":
            return this.onExportCDN();
         case "exportToCsv":
            return this.onExportToCsv(var2);
         case "exportToCsvAction":
            return this.onExportToCsvAction(var2);
         case "exportToCsvLoadSettings":
            return this.onExportToCsvLoadSettings();
         case "exportToCsvSaveFileFormat":
            return this.onExportToCsvSaveFileFormat(var2);
         case "exportToCsvSaveAsFileFormat":
            return this.onExportToCsvSaveAsFileFormat(var2);
         case "exportToCsvDeleteFileFormat":
            return this.onExportToCsvDeleteFileFormat(var2);
         case "exportToMT4":
            return this.onExportToMT4(var2);
         case "exportToMT4Action":
            return this.onExportToMT4Action(var2);
         case "exportToMT4GetDataFolder":
            return this.onExportToMT4GetDataFolder(var2);
         case "exportToMT4GetServerNames":
            return this.onExportToMT4GetServerNames(var2);
         case "exportToMT4LoadProperties":
            return this.onExportToMT4LoadProperties(var2);
         case "exportToMT5":
            return this.onExportToMT5(var2);
         case "exportToMT5Action":
            return this.onExportToMT5Action(var2);
         case "checkQualitySummary":
            return this.onCheckQualitySummary(var2);
         case "checkQualityDetails":
            return this.onCheckQualityDetails(var2);
         case "updateAll":
            return this.onUpdateAll();
         case "updateSelected":
            return this.onUpdateSelected(var2);
         case "stopAll":
            return this.onStopAll();
         case "pauseAll":
            return this.onPauseAll();
         case "resumeAll":
            return this.onResumeAll();
         case "listTimezones":
            return this.onListTimezones(var2);
         case "load":
            return this.onLoad(var2);
         case "save":
            return this.onSave(var2);
         case "confirm":
            return this.onConfirm(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onCancelDataOperation(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      this.canceled = true;
      var2.put("success", L.t("Operation canceled.", new Object[0]));
      return var2.toString();
   }

   private String onResumeAll() {
      DownloadDispatcher.get().resumeAll();
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("Resuming download...", new Object[0]));
      return var1.toString();
   }

   private String onPauseAll() {
      DownloadDispatcher.get().pauseAll();
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("Pausing download...", new Object[0]));
      return var1.toString();
   }

   private String onStopAll() {
      DownloadDispatcher.get().stopAll();
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("Stopping download...", new Object[0]));
      return var1.toString();
   }

   private String onListTimezones(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      CLILogger.log("Timezone,Timezone name");

      for (Timezone var4 : Timezones.getTimezones()) {
         CLILogger.log(var4.printShortName() + "," + var4.getId());
      }

      var2.put("success", L.t("Timezones listed.", new Object[0]));
      return var2.toString();
   }

   private String onExportToCsv(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "symbols")[0].split(",");
         this.checkLimitedSymbols(var3);
         String var4 = this.tryGetParam(var1, "timeframe")[0];
         String var5 = this.tryGetParam(var1, "directory")[0];
         String var6 = this.tryGetParam(var1, "filePrefix")[0];
         String var7 = this.tryGetParam(var1, "session")[0];
         String var8 = this.tryGetParam(var1, "fileFormat")[0];
         String var10 = this.getParam(var1, "targetTimezone", "");
         if ("".equals(var10)) {
            var10 = null;
         }

         Format var9;
         if (var8.equals("Custom")) {
            String var11 = this.tryGetParam(var1, "header")[0];
            String var12 = this.tryGetParam(var1, "format")[0];
            boolean var13 = Boolean.parseBoolean(((String[])var1.get("includeHeader"))[0]);
            var9 = Formats.parseCustomFormat(var8, var11, var12, var13);
         } else {
            var9 = Formats.get().findByName(var8);
            if (var9 == null) {
               throw new Exception(L.t("Cannot find format with name '%s'", new Object[]{var8}));
            }
         }

         long var26 = Long.MIN_VALUE;
         long var27 = Long.MIN_VALUE;
         String var15 = this.getParam(var1, "dateType", "custom");
         if (!var15.equals("allTime")) {
            try {
               if (var1.containsKey("dateFrom")) {
                  var26 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
               }
            } catch (Exception var24) {
               if (!MainApp.runInConsoleWithoutActiveWebserver()) {
                  throw new Exception(L.t("Cannot parse date from - " + var24.getMessage(), new Object[0]));
               }

               CLILogger.log(L.t("Cannot parse date from - " + var24.getMessage(), new Object[0]));
            }

            try {
               if (var1.containsKey("dateTo")) {
                  var27 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
               }
            } catch (Exception var23) {
               if (!MainApp.runInConsoleWithoutActiveWebserver()) {
                  throw new Exception(L.t("Cannot parse date to - " + var23.getMessage(), new Object[0]));
               }

               CLILogger.log(L.t("Cannot parse date to - " + var23.getMessage(), new Object[0]));
            }
         }

         for (int var16 = 0; var16 < var3.length; var16++) {
            String var17 = var3[var16];
            DataInfo var18 = DataManager.getDataInfo("History", var17);
            if (var18 == null) {
               throw new IllegalArgumentException("Symbol " + var17 + " not found.");
            }

            if (var18.rows != 0) {
               if (var26 == Long.MIN_VALUE || var15.equals("allTime")) {
                  var26 = var18.dateFrom;
               }

               if (var27 == Long.MIN_VALUE || var15.equals("allTime")) {
                  var27 = var18.dateTo;
               }

               MultiProgressListener var19 = DataManagerDataProgress.get().createListener(var17, "data/exportToCsvAction");
               String var20 = String.format("CsvExport_%s", var17);
               String var21 = String.format("%s/%s%s-%s-%s.csv", var5, var6, var17, var4, var7);
               CsvExportJob var22 = new CsvExportJob(var20, var19, var18.connection, var17, var4, var7, var21, var26, var27, var9, var10);
               SQGrid.getGridClient().executeOnGrid(var20, var22);
            }
         }
      } catch (Exception var25) {
         return apiErrorJSON(L.t("Cannot export data.", new Object[0]), var25);
      }

      var2.put("success", L.t("Exporting data.", new Object[0]));
      return var2.toString();
   }

   private String onExportToCsvAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      String var5 = "CsvExport_" + var2;
      switch (var3) {
         case "stop":
            SQGrid.getGridClient().stop(var5);
            break;
         case "pause":
            SQGrid.getGridClient().pause(var5);
            break;
         case "continue":
            SQGrid.getGridClient().restart(var5);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onExportToCsvLoadSettings() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("items", Items.get().toJson());
      var1.put("formats", Formats.get().toJson());
      var1.put("success", L.t("Settings loaded.", new Object[0]));
      return var1.toString();
   }

   private String onExportToCsvSaveFileFormat(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "fileFormat")[0];
      String var4 = this.tryGetParam(var1, "header")[0];
      String var5 = this.tryGetParam(var1, "format")[0];
      boolean var6 = Boolean.parseBoolean(((String[])var1.get("includeHeader"))[0]);

      try {
         Formats.get().updateFormat(var3, var4, var5, var6);
         var2.put("formats", Formats.get().toJson());
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot update file format.", new Object[0]), var8);
      }

      var2.put("success", L.t("Format saved.", new Object[0]));
      return var2.toString();
   }

   private String onExportToCsvSaveAsFileFormat(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "newFormatName")[0];
      String var4 = this.tryGetParam(var1, "header")[0];
      String var5 = this.tryGetParam(var1, "format")[0];
      boolean var6 = Boolean.parseBoolean(((String[])var1.get("includeHeader"))[0]);

      try {
         Format var7 = Formats.get().findByName(var3);
         if (var7 != null) {
            throw new Exception(L.t("Format with name '%s' already exists. Please choose another name.", new Object[]{var3}));
         }

         Formats.get().addFormat(var3, var4, var5, var6);
         var2.put("formats", Formats.get().toJson());
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot save file format.", new Object[0]), var8);
      }

      var2.put("success", L.t("Format saved.", new Object[0]));
      return var2.toString();
   }

   private String onExportToCsvDeleteFileFormat(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "fileFormat")[0];

      try {
         Formats.get().deleteFormat(var3);
         var2.put("formats", Formats.get().toJson());
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot delete file format.", new Object[0]), var5);
      }

      var2.put("success", L.t("Format deleted.", new Object[0]));
      return var2.toString();
   }

   private String onCheckQualityDetails(Map<String, String[]> var1) throws NumberFormatException, Exception {
      String var2 = ((String[])var1.get("symbol"))[0].toString();
      int var3 = Integer.parseInt(this.tryGetParam(var1, "totalErrors")[0]);
      String var4 = ((String[])var1.get("timeframe"))[0].toString();
      String var5 = ((String[])var1.get("session"))[0].toString();

      int var6;
      try {
         var6 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var12) {
         var6 = 0;
      }

      int var7;
      try {
         var7 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var11) {
         var7 = 20;
      }

      QualityChecker var8 = new QualityChecker();
      String var9 = var8.getDetails(var4, var2, var5, var6, var6 + var7);
      JsonCreator var10 = new JsonCreator();
      var10.beginObject();
      var10.putRaw("rows", var9, true);
      var10.put("pos", var6, true);
      var10.put("total_count", var3, true);
      var10.put("success", "ok", false);
      var10.endObject(false);
      return var10.toJson();
   }

   private String onCheckQualitySummary(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("symbol"))[0].toString();
      String var3 = ((String[])var1.get("timeframe"))[0].toString();
      String var4 = ((String[])var1.get("session"))[0].toString();
      QualityChecker var5 = new QualityChecker();
      String var6 = var5.getSummary(var3, var2, var4);
      JsonCreator var7 = new JsonCreator();
      var7.beginObject();
      var7.putRaw("problems", var6, true);
      var7.put("success", "ok", false);
      var7.endObject(false);
      return var7.toJson();
   }

   private String onExportToMT4GetDataFolder(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("appPath"))[0].toString();
      JSONObject var3 = new JSONObject();
      MetaTraderLocation var4 = MT4Utils.getDataFolder(var2);
      if (var4 == null) {
         File var5 = MT4Utils.getDataFolderFile();
         var3.put("error", L.t("Cannot find MT4 data folder in default location '%s'.", new Object[]{var5.getAbsolutePath()}));
      } else {
         var3.put("dataPath", var4.getDataFolder());
         var3.put("serverNames", new JSONArray(var4.getServerNames()));
      }

      var3.put("success", "ok");
      return var3.toString();
   }

   private String onExportToMT4GetServerNames(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("dataFolderPath"))[0].toString();
      JSONObject var3 = new JSONObject();

      try {
         var3.put("serverNames", new JSONArray(MT4Utils.getServerNames(var2)));
      } catch (Exception var5) {
         var3.put("error", L.t("Data folder doesn't contain MT4 history files.", new Object[0]));
      }

      var3.put("success", "ok");
      return var3.toString();
   }

   private String onExportToMT4LoadProperties(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("filePath"))[0].toString();
      JSONObject var3 = new JSONObject();
      Mt4Properties var4;
      if (var2.equals("default")) {
         var4 = new Mt4Properties();

         try {
            String var5 = MainApp.settings().get("mt4ExportAppPath", "");
            var3.put("appPath", var5);
            String var6 = MainApp.settings().get("mt4ExportServerName", "");
            if (!var5.isEmpty()) {
               MetaTraderLocation var7 = MT4Utils.getDataFolder(var5);
               var3.put("dataPath", var7.getDataFolder());
               var3.put("serverNames", new JSONArray(var7.getServerNames()));
               if (var6.equals("") && var7.getServerNames().length > 0) {
                  var6 = var7.getServerNames()[0];
               }
            }

            var3.put("serverName", var6);
         } catch (Exception var8) {
            Log.error("ExportToMT4 - Error while loading last settings.", var8);
         }
      } else {
         var4 = new Mt4Properties(var2);
      }

      var3.put("symbols", var4.toJSON());
      var3.put("success", "ok");
      return var3.toString();
   }

   private String onExportToMT4(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"sqSymbol", "mt4Symbol", "serverName", "appPath", "dataPath", "properties"});
      String var2 = ((String[])var1.get("sqSymbol"))[0].toString();
      String var3 = ((String[])var1.get("mt4Symbol"))[0].toString();
      String var4 = ((String[])var1.get("encoding"))[0].toString();
      String var5 = ((String[])var1.get("serverName"))[0].toString();
      String var6 = ((String[])var1.get("appPath"))[0].toString();
      String var7 = ((String[])var1.get("dataPath"))[0].toString();
      String var8 = ((String[])var1.get("properties"))[0].toString();
      String var9 = ((String[])var1.get("timeframe"))[0].toString();
      String var10 = ((String[])var1.get("exportMode"))[0].toString();
      String var11 = this.getParam(var1, "targetTimezone", "");
      if ("".equals(var11)) {
         var11 = null;
      }

      if (var8.equals("default")) {
         Mt4Properties var12 = new Mt4Properties();
         var8 = var12.findBySymbol(var3);
      }

      MainApp.settings().set("mt4ExportAppPath", var6);
      MainApp.settings().set("mt4ExportServerName", var5);
      long var27 = Long.MIN_VALUE;
      long var14 = Long.MIN_VALUE;

      try {
         if (var1.containsKey("dateFrom")) {
            var27 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
         }
      } catch (Exception var26) {
         if (!MainApp.runInConsoleWithoutActiveWebserver()) {
            throw new Exception(L.t("Cannot parse date from - " + var26.getMessage(), new Object[0]));
         }

         CLILogger.log(L.t("Cannot parse date from - " + var26.getMessage(), new Object[0]));
      }

      try {
         if (var1.containsKey("dateTo")) {
            var14 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
         }
      } catch (Exception var25) {
         if (!MainApp.runInConsoleWithoutActiveWebserver()) {
            throw new Exception(L.t("Cannot parse date to - " + var25.getMessage(), new Object[0]));
         }

         CLILogger.log(L.t("Cannot parse date to - " + var25.getMessage(), new Object[0]));
      }

      MultiProgressListener var16 = DataManagerDataProgress.get().createListener(var2, "data/exportToMT4Action");
      Mt4SymbolProperties var17 = new Mt4SymbolProperties();

      try {
         var17.parse(var8);
      } catch (Exception var24) {
         Log.error("Invalid MT4 data specification file. Exc.", var24);
         throw new Exception(L.t("Invalid MT4 data specification file.", new Object[0]));
      }

      DataInfo var18 = DataManager.getDataInfo("History", var2);
      if (var18 == null) {
         throw new Exception(L.t("Symbol %s doesn't exist.", new Object[]{var2}));
      }

      if (var27 == Long.MIN_VALUE) {
         var27 = var18.dateFrom;
      }

      if (var14 == Long.MIN_VALUE) {
         var14 = var18.dateTo;
      }

      String var19 = "MT4_Export_data_job_" + var2;
      MT4ExportJob var20 = new MT4ExportJob(var19, var2, var3, var4, var27, var14, var7, var5, var17, var9, var10, var11, var16);
      ArrayList var21 = new ArrayList(1);
      var21.add(var20);
      GridClient var22 = SQGrid.getGridClient();
      var22.executeOnGrid(var19, var21);
      JSONObject var23 = new JSONObject();
      var23.put("success", "ok");
      return var23.toString();
   }

   private String onExportToMT4Action(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      GridClient var5 = SQGrid.getGridClient();
      String var6 = "MT4_Export_data_job_" + var2;
      switch (var3) {
         case "stop":
            var5.stop(var6);
            break;
         case "pause":
            var5.pause(var6);
            break;
         case "continue":
            var5.restart(var6);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onExportToMT5(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "symbol")[0];
         String var4 = this.getParam(var1, "connection", "History");
         String var5 = this.tryGetParam(var1, "timeframe")[0];
         String var6 = this.tryGetParam(var1, "spread")[0];
         double var7 = Double.parseDouble(this.tryGetParam(var1, "spreadValue")[0]);
         double var9 = Double.parseDouble(this.tryGetParam(var1, "spreadPoints")[0]);
         String var11 = this.tryGetParam(var1, "directory")[0];
         String var12 = this.tryGetParam(var1, "fileName")[0];
         String var13 = this.getParam(var1, "targetTimezone", "");
         if ("".equals(var13)) {
            var13 = null;
         }

         String var14 = var11 + "/" + var12 + ".csv";
         long var15 = Long.MIN_VALUE;
         long var17 = Long.MIN_VALUE;

         try {
            if (var1.containsKey("dateFrom")) {
               var15 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
            }
         } catch (Exception var23) {
            if (!MainApp.runInConsoleWithoutActiveWebserver()) {
               throw new Exception(L.t("Cannot parse date from - " + var23.getMessage(), new Object[0]));
            }

            CLILogger.log(L.t("Cannot parse date from - " + var23.getMessage(), new Object[0]));
         }

         try {
            if (var1.containsKey("dateTo")) {
               var17 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
            }
         } catch (Exception var24) {
            if (!MainApp.runInConsoleWithoutActiveWebserver()) {
               throw new Exception(L.t("Cannot parse date to - " + var24.getMessage(), new Object[0]));
            }

            CLILogger.log(L.t("Cannot parse date to - " + var24.getMessage(), new Object[0]));
         }

         DataInfo var19 = DataManager.getDataInfo("History", var3);
         if (var19 == null) {
            throw new Exception(L.t("Symbol %s doesn't exist.", new Object[]{var3}));
         }

         if (var19.rows == 0) {
            throw new Exception(L.t("Symbol %s doesn't contain any data.", new Object[]{var3}));
         }

         if (var15 == Long.MIN_VALUE) {
            var15 = var19.dateFrom;
         }

         if (var17 == Long.MIN_VALUE) {
            var17 = var19.dateTo;
         }

         MultiProgressListener var20 = DataManagerDataProgress.get().createListener(var3, "data/exportToMT5Action");
         String var21 = "MT5_Export_" + var3;
         MT5ExportJob var22 = new MT5ExportJob(var21, var20, var4, var3, var5, var6, var9, var7, var14, var15, var17, var13);
         SQGrid.getGridClient().executeOnGrid(var21, var22);
      } catch (Exception var25) {
         return apiErrorJSON(L.t("Cannot export data.", new Object[0]), var25);
      }

      var2.put("success", L.t("Exporting data.", new Object[0]));
      return var2.toString();
   }

   private String onExportToMT5Action(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      String var5 = "MT5_Export_" + var2;
      switch (var3) {
         case "stop":
            SQGrid.getGridClient().stop(var5);
            break;
         case "pause":
            SQGrid.getGridClient().pause(var5);
            break;
         case "continue":
            SQGrid.getGridClient().restart(var5);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onListData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         DataToSend var3 = WSDataObjects.getData("", "list");
         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            PrintWriter var4 = null;

            try {
               var4 = new PrintWriter(this.tryGetParam(var1, "csv")[0]);
            } catch (Exception var16) {
               if (var1.containsKey("csv")) {
                  Log.error("Cannot open file. Reason: ", var16);
               }
            }

            String var5 = "Symbol,Instrument,Timeframe,Timezone,Date from,Date to,Total days,Total records,Source,Data type";
            if (var4 == null) {
               CLILogger.log(var5);
            } else {
               var4.println(var5);
            }

            JSONArray var6 = var3.getDataObject().getJSONArray("data");

            for (int var7 = 0; var7 < var6.length(); var7++) {
               JSONObject var8 = var6.getJSONObject(var7);
               String var9 = "";

               try {
                  if (var8.getLong("dateFrom") > 0L) {
                     var9 = SQTime.toUIDateString(Long.valueOf(var8.getLong("dateFrom")));
                  }
               } catch (Exception var15) {
               }

               String var10 = "";

               try {
                  if (var8.getLong("dateTo") > 0L) {
                     var10 = SQTime.toUIDateString(Long.valueOf(var8.getLong("dateTo")));
                  }
               } catch (Exception var14) {
               }

               String var11 = DataSources.toString(var8.getInt("source"));
               String var12 = DataTypes.toString(var8.getInt("dataType"));
               String var13 = var8.getString("symbol")
                  + ","
                  + var8.getString("instrument")
                  + ","
                  + (var8.has("timeframe") ? var8.getString("timeframe") : "")
                  + ","
                  + (var8.has("timezone") ? "\"" + var8.getString("timezone") + "\"" : "")
                  + ","
                  + var9
                  + ","
                  + var10
                  + ","
                  + var8.getLong("totalDays")
                  + ","
                  + var8.getLong("rows")
                  + ","
                  + var11
                  + ","
                  + var12;
               if (var4 == null) {
                  CLILogger.log(var13);
               } else {
                  var4.println(var13);
               }
            }

            if (var4 != null) {
               var4.flush();
               var4.close();
            }
         } else {
            SQWebSocketManager.addToDataQueue(var3, new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }
      } catch (Exception var17) {
         return apiErrorJSON(L.t("Cannot get list of data.", new Object[0]), var17);
      }

      var2.put("success", L.t("Data listed.", new Object[0]));
      return var2.toString();
   }

   private String onEditData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "connection")[0];
         String var4 = this.tryGetParam(var1, "symbol")[0];
         String var5 = var1.containsKey("swap") ? this.tryGetParam(var1, "swap")[0] : null;
         this.checkLimitedSymbol(var4);
         String var6 = this.tryGetParam(var1, "symbolNew")[0];
         DataInfo var7 = DataManager.getDataInfo("History", var4);
         if (var7 == null) {
            throw new Exception("No data info found for symbol '" + var4 + "'.");
         }

         String var8 = var7.instrument;
         int var9 = var7.barTimeType;
         int var10 = var7.brokerId;

         try {
            var8 = this.tryGetParam(var1, "instrument")[0];
            InstrumentInfo var11 = InstrumentManager.getInstrumentInfo(var8);
            var10 = var11.broker;
         } catch (Exception var16) {
         }

         try {
            var9 = Integer.parseInt(this.tryGetParam(var1, "barType")[0]);
         } catch (Exception var15) {
         }

         if (var7.brokerId != var10 && var7.rows != 0) {
            BrokerDto var19 = BrokerManager.getInstance().getBroker(var7.brokerId);
            BrokerDto var12 = BrokerManager.getInstance().getBroker(var10);
            boolean var13 = var19.getMtTimezone() != null || var12.getMtTimezone() != null;
            if (var13 && !LangUtils.equals(var19.getMtTimezone(), var12.getMtTimezone())) {
               return apiErrorJSONNoLog(
                  L.t(
                     "Cannot update the instrument because the broker of this instrument uses a different timezone. To avoid data corruption, please clear this symbols data first and then try again",
                     new Object[0]
                  ),
                  null
               );
            }
         }

         DataManager.updateData(var3, var4, var7.dateFrom, var7.dateTo, var7.rows, var7.secondsRecords, var9, var7.timeframe, null);
         if (!var8.equals(var7.instrument)) {
            DataManager.updateInstrument(var3, var4, var8);
         }

         if (var7.brokerId != var10) {
            DataManager.updateBroker(var3, var4, var10);
         }

         if (var5 != null && !var5.equals(var7.symbolInfo.swap)) {
            DataManager.updateInstrumentSwap(var3, var4, var8, var5);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }

         if (SpecialSymbolsManager.isSpecial(var4)) {
            throw new Exception(L.t("Modification of this symbol is not allowed.", new Object[0]));
         }

         if (!var4.equals(var6)) {
            if (SpecialSymbolsManager.isSpecial(var6)) {
               throw new Exception(L.t("This symbol name '%s' couldn't be used.", new Object[]{var6}));
            }

            boolean var20 = var7.timeframe != null && var7.timeframe.equals("D1");
            boolean var21 = var7.source == 5 || var7.source == 6;
            if (var21 && var20 && !var6.endsWith(".D")) {
               throw new Exception(
                  L.t("Cannot to rename symbol '%s' to '%s'. Reason: Daily futures and equities must end to .D postfix", new Object[]{var4, var6})
               );
            }

            var7 = DataManager.getDataInfo("History", var6);
            if (var7 != null) {
               throw new Exception("Symbol with name '" + var4 + "' already exists.");
            }

            try {
               DataManager.renameData(var3, var4, var6);
            } catch (Exception var14) {
               Log.error(String.format("Cannot rename symbol '%s' to '%s'. Reason:", var4, var6), var14);
               throw new Exception(L.t("Cannot to rename symbol '%s' to '%s'. Reason: %s", new Object[]{var4, var6, var14.getMessage()}));
            }

            var4 = var4 + "##" + var6;
         }

         this.sendDataUpdate(var4, "rename");
      } catch (Exception var17) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var17);
      }

      var2.put("success", L.t("Data updated.", new Object[0]));
      return var2.toString();
   }

   private String onClearData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      boolean var5 = false;

      final String var3;
      final boolean var4;
      try {
         final String[] var6 = this.tryGetParam(var1, "connections")[0].split(",");
         final String var7 = this.tryGetParam(var1, "symbols")[0];
         final String[] var8 = var7.split(",");
         int var9 = this.getLimitedCount(var8);
         if (var9 == 0) {
            var3 = "Data cleared";
            var4 = true;
         } else if (var9 == var8.length) {
            var3 = "Data was not cleared - limited symbols can't be cleared";
            var4 = false;
         } else {
            var3 = "Data cleared, but not all. Limited data can't be cleared";
            var4 = true;
         }

         if (var8.length >= 5) {
            (new Thread() {
               @Override
               public void run() {
                  JSONObject var1x = new JSONObject();

                  for (int var2x = 0; var2x < var6.length; var2x++) {
                     DataManager.clearDataInBatch(var6[var2x], var8[var2x]);
                     if (var2x % 5 == 0) {
                        Integer var3x = SQUtils.round(100.0 * var2x / var6.length);
                        var1x.put("percent", var3x);
                        var1x.put("info", L.t("Cleared symbol '%s'", new Object[]{var8[var2x]}));
                        DataManagerAddProgressSender.getInstance().sendData(var1x);
                     }

                     if (DataServlet.this.canceled) {
                        break;
                     }
                  }

                  DataManager.flushUpdatedData();
                  var1x.put("percent", 100.0);
                  var1x.put("info", L.t("Cleared symbols", new Object[0]));
                  if (var4) {
                     var1x.put("message", L.t(var3, new Object[0]));
                  } else {
                     var1x.put("error", L.t(var3, new Object[0]));
                  }

                  DataManagerAddProgressSender.getInstance().sendData(var1x);

                  try {
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var7, "update"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  } catch (Exception var4x) {
                     DataServlet.Log.error("Error while sending message to ui", var4x);
                  }

                  DataServlet.this.updateStockGroups(var8);
               }
            }).start();
         } else {
            for (int var10 = 0; var10 < var6.length; var10++) {
               DataManager.clearDataInBatch(var6[var10], var8[var10]);
            }

            DataManager.flushUpdatedData();
            this.updateStockGroups(var8);
            this.sendDataUpdate(this.tryGetParam(var1, "symbols")[0], "clear");
         }
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot clear data.", new Object[0]), var11);
      }

      if (var5) {
         var2.put("success", L.t("Clearing data started. Please wait...", new Object[0]));
      } else if (var4) {
         var2.put("success", var3);
      } else {
         var2.put("error", var3);
      }

      return var2.toString();
   }

   private void updateStockGroups(String[] var1) {
      if (BasketOfStocksManager.getInstance() != null) {
         new Thread(new Runnable() {
            @Override
            public void run() {
               Set var1x = BasketOfStocksManager.getInstance().updateGroupsOfSymbols();
               if (!var1x.isEmpty()) {
                  try {
                     String var2 = String.join(",", var1x);
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var2, "update"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  } catch (Exception var3) {
                     DataServlet.Log.error("", var3);
                  }
               }
            }
         }).start();
      }
   }

   private String onRemoveData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      boolean var5 = false;

      try {
         final String[] var6 = this.tryGetParam(var1, "connections")[0].split(",");
         final String var7 = this.tryGetParam(var1, "symbols")[0];
         final String[] var8 = var7.split(",");
         final JSONObject var9 = new JSONObject();
         if (var8.length >= 5) {
            var5 = true;
            new Thread(new Runnable() {
               @Override
               public void run() {
                  try {
                     DataManager.deleteDataInBatch(var6, var8, new BatchProgressController() {
                        public void updateProgress(int var1, int var2x, String var3) throws Exception {
                           if (var1 % 5 == 0) {
                              Integer var4 = SQUtils.round(100.0 * var1 / var2x);
                              var9.put("percent", var4);
                              var9.put("info", L.t("Removed symbol '%s'", new Object[]{var3}));
                              DataManagerAddProgressSender.getInstance().sendData(var9);
                           }
                        }

                        public boolean isCancel() {
                           return DataServlet.this.canceled;
                        }

                        public void finished() {
                           DataServlet.this.sendDataUpdate(var7, "remove");
                           DataServlet.this.updateStockGroups(var8);
                           var9.put("percent", 100.0);
                           var9.put("info", L.t("Removed symbols", new Object[0]));
                           var9.put("message", L.t("Data removed", new Object[0]));
                           DataManagerAddProgressSender.getInstance().sendData(var9);
                        }
                     });
                  } catch (ClassNotFoundException var2x) {
                     DataServlet.Log.error("Error while removing data", var2x);
                  }
               }
            }).start();
         } else {
            DataManager.deleteDataInBatch(var6, var8, null);
            this.updateStockGroups(var8);
            this.sendDataUpdate(this.tryGetParam(var1, "symbols")[0], "remove");
         }
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot remove data.", new Object[0]), var10);
      }

      if (var5) {
         var2.put("success", L.t("Removing data started. Please wait...", new Object[0]));
      } else {
         var2.put("success", L.t("Data removed", new Object[0]));
      }

      return var2.toString();
   }

   private String onShowData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "connection")[0];
         String var4 = this.tryGetParam(var1, "symbol")[0];
         boolean var5 = Boolean.valueOf(this.tryGetParam(var1, "show")[0]);
         DataManager.showData(var3, var4, var5);
         this.sendDataUpdate(var4, "show");
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot change data.", new Object[0]), var6);
      }

      var2.put("success", L.t("Data changed.", new Object[0]));
      return var2.toString();
   }

   private String onGetSymbolData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();

      try {
         String var4 = this.tryGetParam(var1, "symbol")[0];
         String var5 = this.tryGetParam(var1, "dateFrom")[0];
         String var6 = this.tryGetParam(var1, "dateTo")[0];
         String var7 = this.tryGetParam(var1, "session")[0];
         double var8 = 2.5;
         if (SessionManager.getSession(var7) == null) {
            Log.info("Session '{}' doesn't exist, using NoSession.", var7);
            var7 = "No Session";
         }

         if (!var4.equals("not set")) {
            long var10 = 0L;
            long var12 = 0L;
            if (!var5.equals("not set")) {
               try {
                  var10 = SQTime.parseToMilis(var5, "yyyy.MM.dd");
               } catch (Exception var26) {
               }
            }

            if (!var6.equals("not set")) {
               try {
                  var12 = SQTime.parseToMilis(var6, "yyyy.MM.dd");
               } catch (Exception var25) {
               }
            }

            ChartDef var14 = new ChartDef("History", var4, "D1", var10, var12, var8, var7);
            IDataLoader var15 = DataManager.getDataLoader(var14, 1, null);
            var15.open();
            double var16 = -1.0;
            double var18 = 0.0;
            int var20 = 0;

            while (var15.hasNextTick()) {
               JSONArray var21 = new JSONArray();
               VersatileData var22 = new VersatileData();
               var15.getNextTick(var22);
               var21.put(SQTime.toUIDateString(var22.time));
               var21.put(var22.close);
               double var23 = var22.high - var22.low;
               if (var16 < 0.0) {
                  var16 = var23;
                  var18 = var22.close;
               } else {
                  var23 = Math.max(Math.abs(var22.low - var18), Math.max(var23, Math.abs(var22.high - var18)));
                  var16 = ((Math.min(var20 + 1, 14) - 1) * var16 + var23) / Math.min(var20 + 1, 14);
                  var18 = var22.close;
               }

               var20++;
               var21.put(var16);
               var3.put(var21);
            }

            var15.close();
         }

         var2.put("data", var3);
      } catch (Exception var27) {
         return apiErrorJSON(L.t("Cannot get symbol data.", new Object[0]), var27);
      }

      var2.put("success", L.t("Symbol data loaded.", new Object[0]));
      return var2.toString();
   }

   private String onAddTimeframe(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "timeframe")[0];
         TimeframeManager.addTimeframe(var3);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getTimeframes(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot add timeframe.", new Object[0]), var4);
      }

      var2.put("success", L.t("Timeframe added.", new Object[0]));
      return var2.toString();
   }

   private String onSaveDataChanges(Map<String, String[]> var1) {
      try {
         final String var2 = ((String[])var1.get("symbol"))[0].toString();
         final String var3 = ((String[])var1.get("timeframe"))[0].toString();
         final String var4 = ((String[])var1.get("session"))[0].toString();
         String[] var5 = this.getParam(var1, "deleted[]");
         final HashMap var6 = new HashMap();
         final boolean var7 = var3.equals("TICK");
         String var8 = var7 ? "yyyy.MM.dd HH:mm:ss.SSS" : "yyyy.MM.dd HH:mm:ss";
         List var9 = var1.keySet().stream().filter(var0 -> var0.startsWith("changed[")).collect(Collectors.toList());

         for (String var11 : var9) {
            String[] var12 = this.getParam(var1, var11);
            var6.put(SQTime.parseToMilis(var12[0], var8), var12);
         }

         final Set var15 = var5 != null ? Arrays.stream(var5).map(var1x -> SQTime.parseToMilis(var1x, var8)).collect(Collectors.toSet()) : new HashSet();
         if (var15.isEmpty() && var9.isEmpty()) {
            return apiErrorJSON(L.t("No changes was performed.", new Object[0]), null);
         }

         new Thread(
               new Runnable() {
                  @Override
                  public void run() {
                     try {
                        DataInfo var1x = DataManager.getDataInfo("History", var2);
                        int var2x = var3.equals("TICK") ? 2 : 1;
                        DataBinReaderNew var3x = DataBinReaderNew.getInstance(var2x, var1x.symbolInfo);
                        String var4x = DataManager.getDataFileName("History", var2, var3, var4);
                        File var5x = new File(var4x);
                        String var6x = var5x.getAbsolutePath();
                        var3x.setFileName(var6x);
                        var3x.openFile();
                        boolean var7x = var3x.isCrypted();
                        DataBinWriterNew var8x = var7x
                           ? DataBinWriterNew.getCryptedInstance(var2x, MainApp.getDataPath(), var1x.symbolInfo)
                           : DataBinWriterNew.getInstance(var2x, MainApp.getDataPath(), var1x.symbolInfo);
                        String var9x = var5x.getAbsoluteFile() + ".temp";
                        var8x.setFileName(var9x);
                        var8x.open();
                        new VersatileData();
                        long var11 = -1L;
                        long var13 = -1L;
                        long var15x = 0L;
                        int var17 = 0;
                        JSONObject var18 = new JSONObject();

                        while (var3x.dataRemaining()) {
                           var3x.readData();
                           VersatileData var10 = var3x.tickData;
                           long var19 = var10.time;
                           if (var17 % 200 == 0) {
                              Integer var21 = SQUtils.round(100.0 * var17 / var1x.rows);
                              var18.put("percent", var21);
                              var18.put("info", L.t("Saving data %s", new Object[]{SQTime.toString(var19)}));
                              DataManagerSaveProgreessSender.getInstance().sendData(var18);
                           }

                           var17++;
                           if (!var15.contains(var19)) {
                              String[] var23 = (String[])var6.get(var19);
                              if (var23 != null) {
                                 if (var7) {
                                    var10.ask = Double.valueOf(var23[1]);
                                    var10.bid = Double.valueOf(var23[2]);
                                    var10.volume = Double.valueOf(var23[3]);
                                 } else {
                                    var10.open = Double.valueOf(var23[1]);
                                    var10.high = Double.valueOf(var23[2]);
                                    var10.low = Double.valueOf(var23[3]);
                                    var10.close = Double.valueOf(var23[4]);
                                    var10.volume = Double.valueOf(var23[5]);
                                 }
                              }

                              var8x.writeData(var10);
                              if (var11 == -1L) {
                                 var11 = var19;
                              }

                              var13 = var19;
                              var15x++;
                           }
                        }

                        var3x.closeFile();
                        var8x.close();
                        var5x.delete();
                        new File(var9x).renameTo(var5x);
                        DataManager.updateData("History", var1x.symbol, var11, var13, (int)var15x, var1x.secondsRecords, 1, var1x.timeframe, "Etc/UCT");
                        var18.put("percent", 100.0);
                        var18.put("info", L.t("Data saved", new Object[0]));
                        DataManagerSaveProgreessSender.getInstance().sendData(var18);
                     } catch (Exception var22) {
                        DataServlet.Log.error("Error while updating. ", var22);
                     }
                  }
               }
            )
            .start();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot load chart.", new Object[0]), var13);
      }

      JSONObject var14 = new JSONObject();
      var14.put("success", L.t("Data saving.", new Object[0]));
      return var14.toString();
   }

   private String onGetIndexForDate(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("symbol"))[0].toString();
      String var3 = ((String[])var1.get("timeframe"))[0].toString();
      String var4 = ((String[])var1.get("session"))[0].toString();
      long var5 = SQTime.parseDateToMilis(((String[])var1.get("date"))[0].toString());
      DataInfo var7 = DataManager.getDataInfo("History", var2);
      DataBinReaderNew var8 = this.getDataReader(var2, var3, var4);
      int var9 = this.seekReader(var8, var7, var5);
      var8.seek(var9);
      new VersatileData();

      while (var8.dataRemaining()) {
         var8.readData();
         VersatileData var10 = var8.tickData;
         long var11 = var10.time;
         if (var11 >= var5) {
            break;
         }

         var9++;
      }

      var8.closeFile();
      JSONObject var13 = new JSONObject();
      var13.put("success", "Index listed.");
      var13.put("index", var9);
      return var13.toString();
   }

   private int seekReader(DataBinReaderNew var1, DataInfo var2, long var3) throws Exception {
      long var5 = var1.getTotalRecords();
      if (var5 == 0L) {
         var5 = var2.rows;
      }

      long var7 = var2.dateTo - var2.dateFrom;
      double var9 = var7 / var5;
      long var11 = var3 - var2.dateFrom;
      int var13 = (int)(var11 / var9);

      int var14;
      for (var14 = 0; var14 < 5; var14++) {
         var1.seek(var13);
         var1.readData();
         long var15 = var1.tickData.time;
         if (var15 < var3) {
            break;
         }

         var13 -= var13 / 10;
      }

      return var14 != 5 && var13 >= 0 ? var13 : 0;
   }

   private DataBinReaderNew getDataReader(String var1, String var2, String var3) throws Exception {
      DataInfo var4 = DataManager.getDataInfo("History", var1);
      boolean var5 = var2.equals("TICK");
      DataBinReaderNew var6 = null;
      if (var5) {
         var6 = DataBinReaderNew.getInstance(2, var4.symbolInfo);
      } else {
         var6 = DataBinReaderNew.getInstance(1, var4.symbolInfo);
      }

      String var7 = DataManager.getDataFileName("History", var1, var2, var3);
      File var8 = new File(var7);
      var6.setFileName(var8.getAbsolutePath());
      var6.openFile();
      return var6;
   }

   private String onReviewData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      int var3;
      try {
         var3 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var16) {
         var3 = 0;
      }

      int var4;
      try {
         var4 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var15) {
         var4 = 100;
      }

      String var5 = ((String[])var1.get("symbol"))[0].toString();
      String var6 = ((String[])var1.get("timeframe"))[0].toString();
      String var7 = ((String[])var1.get("session"))[0].toString();
      DataInfo var9 = DataManager.getDataInfo("History", var5);
      if (var9 == null) {
         throw new Exception("No data info found for symbol '" + var5 + "'.");
      }

      boolean var10 = var6.equals("TICK");
      long var11 = 0L;
      JSONArray var8;
      if (var10) {
         var8 = this.listTickData(var5, var6, var7, var3, var4, var9);
         var11 = var9.rows;
      } else {
         ChartDef var13 = new ChartDef("History", var5, var6, 0L, SQTime.toLong(2100, 1, 1), 2.5, var7);
         IDataLoader var14 = DataManager.getDataLoader(var13, 1, null);
         var14.open();
         var11 = var14.getTotalRecords();
         var8 = this.listOHLCData(var3, var4, var14, var9);
      }

      var2.put("total_count", var11);
      var2.put("rows", var8);
      var2.put("pos", var3);
      var2.put("success", L.t("Data listed.", new Object[0]));
      return var2.toString();
   }

   private String onExport(boolean var1, Map<String, String[]> var2) throws Exception {
      String var3 = ((String[])var2.get("symbol"))[0].toString();
      String var4 = ((String[])var2.get("path"))[0].toString();
      this.checkLimitedSymbol(var3);
      long var5 = 0L;
      long var7 = 0L;

      try {
         var5 = SQTime.parseToMilis(this.tryGetParam(var2, "dateFrom")[0], "yyyy.MM.dd");
      } catch (Exception var11) {
      }

      try {
         var7 = SQTime.parseToMilis(this.tryGetParam(var2, "dateTo")[0], "yyyy.MM.dd");
      } catch (Exception var10) {
      }

      DataInfo var9 = DataManager.getDataInfo("History", var3);
      if (var9 == null) {
         throw new Exception(L.t("Symbol %s doesn't exist.", new Object[]{var3}));
      }

      if (var1) {
         DataManager.exportTick(var3, var5, var7, var4);
      } else {
         DataManager.exportM1(var3, var5, var7, var4);
      }

      return "{}";
   }

   private String onExportCDN() throws Exception {
      Thread var1 = new Thread() {
         @Override
         public void run() {
            String[] var1x = new String[]{"AUDUSD", "EURUSD", "GBPUSD", "NZDUSD", "USDCAD", "USDCHF", "USDJPY", "XAGUSD", "XAUUSD"};

            try {
               for (int var2 = 0; var2 < var1x.length; var2++) {
                  DataManager.exportTick(var1x[var2], 0L, 0L, "C:/SQ4-cdndata/data/tick");
               }

               for (int var4 = 0; var4 < var1x.length; var4++) {
                  DataManager.exportM1(var1x[var4] + "_M1", 0L, 0L, "C:/SQ4-cdndata/data/m1");
               }
            } catch (Exception var3) {
               DataServlet.Log.error("Error while export CDN data. Exc.", var3);
            }
         }
      };
      var1.start();
      return "{}";
   }

   private String onReviewChart(Map<String, String[]> var1) throws Exception {
      JsonCreator var2 = new JsonCreator();
      var2.beginObject();
      long var3 = 0L;
      long var5 = 0L;

      try {
         var3 = Long.parseLong(this.tryGetParam(var1, "indexFrom")[0]);
      } catch (Exception var21) {
      }

      try {
         var5 = Long.parseLong(this.tryGetParam(var1, "indexTo")[0]);
      } catch (Exception var20) {
      }

      boolean var7 = true;

      try {
         var7 = Boolean.parseBoolean(this.tryGetParam(var1, "loadPreview")[0]);
      } catch (Exception var19) {
      }

      String var8 = ((String[])var1.get("symbol"))[0].toString();
      String var9 = ((String[])var1.get("timeframe"))[0].toString();
      String var10 = ((String[])var1.get("session"))[0].toString();
      DataInfo var11 = DataManager.getDataInfo("History", var8);
      if (var11 == null) {
         throw new Exception("No data info found for symbol '" + var8 + "'.");
      }

      ChartDef var12 = new ChartDef("History", var8, var9, 0L, SQTime.toLong(2100, 1, 1), 2.5, var10);
      IDataLoader var13 = DataManager.getDataLoader(var12, 1, null);
      var13.open();
      long var14 = var13.getTotalRecords();
      if (var3 < 0L) {
         var3 = 0L;
      }

      if (var5 < 0L) {
         var5 = var3 + 100L;
      }

      if (var5 >= var14) {
         var5 = var14 - 1L;
      }

      if (var3 >= var5) {
         var3 = var5 - 100L;
         if (var3 < 0L) {
            var3 = 0L;
         }
      }

      try {
         StockData var16 = new StockData();
         var16.setCount(var14);
         if (var7) {
            this.fillPreview(var16, var14, var13);
         }

         var16.setAreaFrom(var3);
         var16.setAreaTo(var5);
         var13.seek((int)var3);
         this.fillStockData(var16, var13, var11);
         var16.getChartData()[0].setTitle(var8 + "/" + var9);
         var2.putRaw("chart", var16.toJSON(), false);

         try {
            var2.separator();
            var2.put("requestId", this.tryGetParam(var1, "requestId")[0], false);
         } catch (Exception var18) {
         }
      } catch (Exception var22) {
         return apiErrorJSON(L.t("Cannot load chart.", new Object[0]), var22);
      }

      var13.close();
      var2.put("success", "Chart data loaded", false);
      var2.endObject(false);
      return var2.toJson();
   }

   private String onCloneToTimezone(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String[] var3 = this.tryGetParam(var1, "symbols")[0].split(",");
      String var4 = ((String[])var1.get("postfix"))[0].toString();
      int var5 = Integer.parseInt(this.tryGetParam(var1, "cloneTimezoneType")[0]);
      this.checkLimitedSymbols(var3);
      boolean var6 = Boolean.parseBoolean(this.tryGetParam(var1, "removeWeekends")[0]);
      int var7 = 0;

      for (int var9 = 0; var9 < var3.length; var9++) {
         String var10 = var3[var9];
         DataInfo var11 = DataManager.getDataInfo("History", var10);
         String var8;
         if (var5 == 1) {
            var7 = 0;
            var8 = ((String[])var1.get("cloneTimezone"))[0].toString();
         } else {
            var7 = Integer.parseInt(this.tryGetParam(var1, "cloneTimezoneShift")[0]);
            var8 = var11.timezone;
            if (var8 == null) {
               var8 = "Etc/UCT";
            }
         }

         if (var11.sourceDataId != 0) {
            if (MainApp.runInConsoleWithoutActiveWebserver()) {
               CLILogger.log(L.t("Symbol '%s' - cloned data cannot be cloned again.", new Object[]{var10}));
            }
         } else if (var11.rows != 0 || var11.source != 1) {
            String var12 = this.addPostfixToSymbolName(var10, var4, var8, var7, var11);

            while (DataManager.checkDataExists("History", var12)) {
               var12 = DataManager.generateName(var12);
            }

            if (var9 == 0) {
               MainApp.settings().set("CloneTimezone", var7 == 0 ? var8 : var7 + "");
               MainApp.settings().set("CloneRemoveWeekends", var6 + "");
            }

            MultiProgressListener var13 = DataManagerDataProgress.get().createListener(var10, "data/cloneToTimezoneAction");
            TickerDto var14 = null;
            if (var11.source == 6) {
               var14 = HistoryDataManager.get().getFutureTicker(var11.uSymbol);
            } else if (var11.source == 5) {
               var14 = HistoryDataManager.get().getStockTicker(var11.uSymbol);
            }

            CloneToTimezoneJob var15 = new CloneToTimezoneJob("CloneToTimezoneJob_" + var10, var10, var12, var8, var7, var6, var14, var13);
            SQGrid.getGridClient().executeOnGrid("CloneToTimezoneJob_" + var10, var15);
         } else if (MainApp.runInConsoleWithoutActiveWebserver()) {
            CLILogger.log(L.t("Symbol '%s' has no data to clone (unknown timezone).", new Object[]{var10}));
         }
      }

      var2.put("success", L.t("Data cloned.", new Object[0]));
      return var2.toString();
   }

   private String addPostfixToSymbolName(String var1, String var2, String var3, int var4, DataInfo var5) {
      String var6 = var1 + var2;
      var6 = var6.replaceAll("\\{timeframe\\}", var5.timeframe);
      String var12;
      if (var4 == 0) {
         var12 = Timezone.print(var3, var5.source, true);
         var12 = var12.replace(",", "").replaceAll("\\s+", "_");
      } else if (var4 < 0) {
         var12 = var4 + "";
      } else {
         var12 = "+" + var4;
      }

      var6 = var6.replaceAll("\\{cloneTime\\}", var12);
      var6 = var6.replaceAll("-", "Minus");
      var6 = var6.replaceAll("\\+", "Plus");
      return var6.replace("&", "");
   }

   private String onCloneToTimezoneAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      String var5 = "CloneToTimezoneJob_" + var2;
      switch (var3) {
         case "stop":
            SQGrid.getGridClient().stop(var5);
            break;
         case "pause":
            SQGrid.getGridClient().pause(var5);
            break;
         case "continue":
            SQGrid.getGridClient().restart(var5);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private void fillPreview(StockData var1, long var2, IDataLoader var4) throws Exception {
      short var5 = 1000;
      int var6;
      double var7;
      if (var2 < var5) {
         var6 = (int)var2;
         var7 = 1.0;
      } else {
         var7 = (double)var2 / var5;
         var6 = var5;
      }

      LinkedList var9 = new LinkedList();
      float[] var10 = new float[var6];
      double var11 = 0.0;
      int var13 = 0;
      Integer var14 = null;
      VersatileData var15 = new VersatileData();

      while (var11 < var2 && var13 < var6) {
         var4.seek((int)var11);
         var4.hasNextTick();
         var4.getNextTick(var15);
         var10[var13] = (float)(Math.round(var15.close * 10000.0) / 10000.0);
         int var16 = SQTime.getFullYear(var15.time);
         if (var14 == null || var16 != var14) {
            PreviewCaption var17 = new PreviewCaption(var13, String.valueOf(var16));
            var9.add(var17);
            var14 = var16;
         }

         var11 += var7;
         var13++;
      }

      var1.setPreviewYVals(var10);
      var1.setPreviewCaptions(var9.toArray(new PreviewCaption[0]));
   }

   private void fillStockData(StockData var1, IDataLoader var2, DataInfo var3) throws Exception {
      long var4 = var1.getAreaTo() - var1.getAreaFrom();
      int var6 = 0;
      StockChartData var7 = new StockChartData();
      var7.setPrecision(var3.symbolInfo.decimals);
      var1.setChartData(new StockChartData[1]);
      var1.getChartData()[0] = var7;
      VersatileData var8 = new VersatileData();
      var7.setClose(new LinkedList());
      var7.setLow(new LinkedList());
      var7.setHigh(new LinkedList());
      var7.setOpen(new LinkedList());
      LinkedList var9 = new LinkedList();

      while (var2.hasNextTick()) {
         var2.getNextTick(var8);
         var9.add(var8.time);
         var7.getClose().add(var8.close);
         var7.getOpen().add(var8.open);
         var7.getHigh().add(var8.high);
         var7.getLow().add(var8.low);
         if (var6++ == var4) {
            break;
         }
      }

      long[] var10 = new long[var9.size()];
      int var11 = 0;

      for (Long var13 : var9) {
         var10[var11++] = var13;
      }

      var7.setxVals(var10);
   }

   private JSONArray listTickData(String var1, String var2, String var3, int var4, int var5, DataInfo var6) throws Exception {
      JSONArray var7 = new JSONArray();
      DataInfo var8 = DataManager.getDataInfo("History", var1);
      DataBinReaderNew var9 = DataBinReaderNew.getInstance(2, var8.symbolInfo);
      String var10 = DataManager.getDataFileName("History", var1, var2, var3);
      File var11 = new File(var10);
      var9.setFileName(var11.getAbsolutePath());
      var9.openFile();
      var9.seek(var4);
      long var12 = 0L;
      new VersatileData();

      while (var9.dataRemaining() && var12 < var5) {
         var9.readData();
         VersatileData var14 = var9.tickData;
         JSONArray var15 = new JSONArray();
         var15.put(SQTime.toFullDateTimeString(var14.time));
         var15.put(SQUtils.d2String(var14.ask, var6.symbolInfo.decimals));
         var15.put(SQUtils.d2String(var14.bid, var6.symbolInfo.decimals));
         var15.put(var14.volume > 1.0 ? SQUtils.round(var14.volume) : 1);
         JSONObject var16 = new JSONObject();
         var16.put("id", "r" + (var4 + var12));
         var16.put("data", var15);
         var7.put(var16);
         var12++;
      }

      var9.closeFile();
      return var7;
   }

   private JSONArray listOHLCData(int var1, int var2, IDataLoader var3, DataInfo var4) throws Exception {
      JSONArray var5 = new JSONArray();
      var3.seek(var1);

      for (long var6 = 0L; var3.hasNextTick() && var6 < var2; var6++) {
         VersatileData var8 = new VersatileData();
         var3.getNextTick(var8);
         JSONArray var9 = new JSONArray();
         var9.put(SQTime.toFullDateMinuteString(var8.time));
         var9.put(SQUtils.d2String(var8.open, var4.symbolInfo.decimals));
         var9.put(SQUtils.d2String(var8.high, var4.symbolInfo.decimals));
         var9.put(SQUtils.d2String(var8.low, var4.symbolInfo.decimals));
         var9.put(SQUtils.d2String(var8.close, var4.symbolInfo.decimals));
         var9.put(var8.volume > 1.0 ? SQUtils.roundLong(var8.volume) : 1L);
         JSONObject var10 = new JSONObject();
         var10.put("id", "r" + (var1 + var6));
         var10.put("data", var9);
         var5.put(var10);
      }

      var3.close();
      return var5;
   }

   private void sendDataUpdate(String var1, String var2) {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var1, var2), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var4) {
         Log.error("Cannot send websocket data update. ", var4);
      }
   }

   private TickerFilterDto getFilterForHistoryTickers(List<String> var1) {
      TickerFilterDto var2 = new TickerFilterDto();
      var2.setSearchInTicker(true);
      var2.setExactMatch(true);
      var2.setNames(var1.toArray(new String[0]));
      return var2;
   }

   private String onUpdateAll() {
      try {
         JSONObject var1 = new JSONObject();
         ArrayList var2 = DataManager.listSafe();
         var2 = new ArrayList<>(var2.stream().filter(var0 -> var0.source != 8).toList());
         Collections.shuffle(var2);
         this.performUpdate(var2);
         var1.put("success", L.t("Updates started.", new Object[0]));
         return var1.toString();
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var3);
      }
   }

   private void performUpdate(List<DataInfo> var1) throws Exception {
      LinkedList var2 = new LinkedList();
      LinkedList var3 = new LinkedList();

      for (DataInfo var5 : var1) {
         if (var5.source == 5 && var5.uSymbol != null) {
            var3.add(var5.uSymbol);
         }

         if (var5.source == 6 && var5.uSymbol != null) {
            var2.add(var5.uSymbol);
         }
      }

      List var18 = var2.isEmpty() ? new LinkedList() : HistoryDataManager.get().getFuturesTickers(this.getFilterForHistoryTickers(var2));
      List var19 = var3.isEmpty() ? new LinkedList() : HistoryDataManager.get().getStockTickers(this.getFilterForHistoryTickers(var3));
      Map var6 = this.toMap(var18);
      Map var7 = this.toMap(var19);
      HashMap var8 = new HashMap();
      String[] var9 = new String[1];
      var8.put("symbols", var9);

      for (DataInfo var11 : var1) {
         if (!DataManager.isLimited(var11.symbol)) {
            var9[0] = var11.symbol;
            if (var11.source == 5) {
               DownloadDispatcher.get().updateHistoryData(var11, (TickerDto)var7.get(var11.uSymbol));
            } else if (var11.source == 6) {
               DownloadDispatcher.get().updateHistoryData(var11, (TickerDto)var6.get(var11.uSymbol));
            } else if (var11.source == 2) {
               try {
                  Program.get("DataSourceDukascopy").call("updateSelected", new Object[]{var8});
               } catch (Exception var17) {
               }
            } else if (var11.source == 7) {
               try {
                  Program.get("DataSourceCrypto").call("updateSelected", new Object[]{var8});
               } catch (Exception var16) {
               }
            } else if (var11.source == 4) {
               try {
                  Program.get("DataSourceDarwinex").call("updateSelected", new Object[]{var8});
               } catch (Exception var15) {
               }
            } else if (var11.source == 3) {
               try {
                  Program.get("DataSourceYahoo").call("updateSelected", new Object[]{var8});
               } catch (Exception var14) {
               }
            } else if (var11.source == 8) {
               try {
                  Program.get("DataSourceMt5Api").call("updateSelected", new Object[]{var8});
               } catch (Exception var13) {
               }
            }
         }
      }
   }

   private Map<String, TickerDto> toMap(List<TickerDto> var1) {
      HashMap var2 = new HashMap();

      for (TickerDto var4 : var1) {
         var2.put(var4.getTicker(), var4);
      }

      return var2;
   }

   private String onUpdateSelected(Map<String, String[]> var1) {
      try {
         JSONObject var2 = new JSONObject();
         String[] var3 = this.tryGetParamValue(var1, "symbols").split(",");
         LinkedList var4 = new LinkedList();

         for (String var8 : var3) {
            DataInfo var9 = DataManager.getDataInfo("History", var8);
            if (var9 == null) {
               Log.error(String.format("Symbol with name '%s' doesn't exist.", var8));
            } else {
               var4.add(var9);
            }
         }

         this.performUpdate(var4);
         var2.put("success", L.t("Updates started.", new Object[0]));
         return var2.toString();
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var10);
      }
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         ArrayList var5 = new ArrayList();
         Element var6 = XMLUtil.fileToXmlElement(var3);
         if (!var6.getName().equals("Data")) {
            throw new Exception("No Data found to import.");
         }

         ArrayList var8 = new ArrayList();
         ArrayList var9 = new ArrayList();

         for (Element var11 : var6.getChildren("DataInfo")) {
            DataInfo var12 = new DataInfo();
            var12.setFromXML(var11);
            if (var12.sourceDataId > 0) {
               var9.add(var12);
            } else {
               var8.add(var12);
            }
         }

         var8.addAll(var9);
         HashMap var21 = new HashMap();

         for (Element var24 : var6.getChildren("Broker")) {
            BrokerDto var13 = ProjectResources.loadBrokerElement(var24);
            Integer var14 = var13.getId();
            BrokerDto var15 = BrokerManager.getInstance().getBroker(var13.getName());
            if (var15 != null) {
               var21.put(var14, var15.getId());
            } else {
               var13.setId(null);
               BrokerManager.getInstance().saveBroker(var13);
               var21.put(var14, var13.getId());
            }
         }

         boolean var23 = false;

         for (DataInfo var26 : var8) {
            int var7 = -1;
            DataInfo var27 = DataManager.getDataInfo("History", var26.symbol);
            if (var27 == null) {
               Integer var29 = (Integer)var21.get(var26.symbolInfo.broker);
               var26.symbolInfo.broker = var29 == null ? -1 : var29;
               ProjectResources.checkInstrumentExists(var26.symbolInfo);
               Integer var31 = (Integer)var21.get(var26.brokerId);
               DataManager.addData(
                  "History", var26.symbol, var26.instrument, var26.barTimeType, var26.source, var26.uSymbol, var26.uSymbolName, -1, var31 == null ? -1 : var31
               );
               var5.add(var26.symbol);
               if (var26.sourceDataId > 0) {
                  for (DataInfo var33 : var8) {
                     if (var33.id == var26.sourceDataId) {
                        DataInfo var19 = DataManager.getDataInfo("History", var33.symbol);
                        if (var19 != null) {
                           var7 = var19.id;
                           break;
                        }
                     }
                  }
               }

               DataManager.updateData("History", var26.symbol, 0L, 0L, 0, 0L, var26.barTimeType, var26.timeframe, var26.timezone, var7, var26.removeWeekends);
            } else {
               String var28 = "";
               if (!var23) {
                  DataManagerConfirm.get()
                     .awaitConfirmation(
                        L.t("Overwrite confirm", new Object[0]),
                        L.t("Symbol '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var27.symbol})
                     );
                  var28 = DataManagerConfirm.get().getConfirmedAction();
               }

               if (!var28.equals("skip")) {
                  if (var28.equals("cancel")) {
                     break;
                  }

                  if (var28.equals("overwrite") || var28.equals("overwrite_all") || var23) {
                     if (var28.equals("overwrite_all")) {
                        var23 = true;
                     }

                     DataManager.clearData("History", var26.symbol);
                     if (var26.sourceDataId > 0) {
                        for (DataInfo var17 : var8) {
                           if (var17.id == var26.sourceDataId) {
                              DataInfo var18 = DataManager.getDataInfo("History", var17.symbol);
                              if (var18 != null) {
                                 var7 = var18.id;
                                 break;
                              }
                           }
                        }
                     }

                     DataManager.updateData(
                        "History", var26.symbol, 0L, 0L, 0, 0L, var26.barTimeType, var26.timeframe, var26.timezone, var7, var26.removeWeekends
                     );
                     Integer var30 = (Integer)var21.get(var26.brokerId);
                     DataManager.updateBroker("History", var26.symbol, var30 == null ? -1 : var30);
                     var5.add(var26.symbol);
                  }
               }
            }
         }

         this.sendDataUpdate(String.join(",", var5), "add");
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var4.put("success", L.t("Data (%d) loaded.", new Object[]{var5.size()}));
         return var4.toString();
      } catch (Exception var20) {
         return apiErrorJSON(L.t("Cannot load data.", new Object[0]), var20);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("Data");
         String[] var6 = this.tryGetParamValue(var1, "symbols").split(",");
         TreeSet var7 = new TreeSet();

         for (String var11 : var6) {
            DataInfo var12 = DataManager.getDataInfo("History", var11);
            if (var12 == null) {
               Log.error(String.format("Symbol with name '%s' doesn't exist.", var11));
            } else {
               var5.addContent(var12.getXML());
               var7.add(var12.brokerId);
               var7.add(var12.symbolInfo.broker);
            }
         }

         for (Integer var15 : var7) {
            if (var15 != -1) {
               BrokerDto var16 = BrokerManager.getInstance().getBroker(var15);
               var5.addContent(ProjectResources.createBrokerElement(var16));
            }
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Data saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot save data.", new Object[0]), var13);
      }
   }

   private String onConfirm(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("action"))[0];
         DataManagerConfirm.get().setConfirmedAction(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Confirmation failed.", new Object[0]), var4);
      }

      var2.put("success", L.t("Action confirmed.", new Object[0]));
      return var2.toString();
   }
}
