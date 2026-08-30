package com.strategyquant.webguilib.servlet;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.lib.L;
import com.strategyquant.lib.ProcessPriorityAdjuster;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.LauncherConfig;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.customization.Customization;
import com.strategyquant.lib.hw.HardwareInfo;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.snippets.SnippetsCompiler;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.utils.EmailSender;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.PrecachedRequest;
import com.strategyquant.tradinglib.TradelistColumn;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.benchmark.BenchmarkEngine;
import com.strategyquant.tradinglib.benchmark.BenchmarkProgressEngine;
import com.strategyquant.tradinglib.benchmark.BenchmarkResult;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.IProgressListener;
import com.strategyquant.tradinglib.databank.RecentColumns;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.gui.InitAfterGUILoad;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.performance.Performance;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategiesSaver;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.ProjectFeeds;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.results.IResultPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.tradelist.TradelistColumnsList;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.Electron;
import com.strategyquant.webguilib.WebAppManager;
import com.strategyquant.webguilib.WebServer;
import com.strategyquant.webguilib.config.RemoteAccessConfig;
import com.strategyquant.webguilib.server.AbstractUIWebServer;
import com.strategyquant.webguilib.server.JettyServer;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.Desktop.Action;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(MainServlet.class);
   private WebServer webServer;
   private XMLOutputter xmlOutputter = new XMLOutputter(Format.getRawFormat());
   private boolean loadNotified = false;
   private static MainServlet instance = null;
   public static String getConfigsCache = null;
   private static final String VERSIONS_XML_BASE_URL = "https://setup.strategyquant.com/";

   public MainServlet(WebServer var1) {
      this.webServer = var1;
      instance = this;
      this.preloadRequests();
   }

   public static MainServlet getInstance() {
      return instance;
   }

   @Override
   public String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "alive":
            return this.onAlive();
         case "exit":
            return this.onExit();
         case "minimize":
            return this.onMinimize();
         case "maximize":
            return this.onMaximize();
         case "toggleFullscreen":
            return this.onToggleFullscreen();
         case "regenerate":
            return this.onRegenerate();
         case "startapp":
            return this.onStartApp(var2);
         case "getappcode":
            return this.onGetAppCode();
         case "getapps":
            return this.onGetApps();
         case "tofront":
            return this.onToFront();
         case "getremoteaccess":
            return this.onGetRemoteAccess();
         case "setremoteaccess":
            return this.onSetRemoteAccess(var2);
         case "login":
            return this.onLogin(var2);
         case "checkaccess":
            return this.onCheckAccess();
         case "openFolder":
            return this.onOpenFolder(var2);
         case "openbrowser":
            return this.onOpenBrowser(var2);
         case "openhelp":
            return this.onOpenHelp(var2);
         case "opendocument":
            return this.onOpenDocument(var2);
         case "getSettings":
            return this.onGetSettings();
         case "saveSetting":
            return this.onSaveSetting(var2);
         case "createFile":
            return this.onCreateFile(var2);
         case "loadFile":
            return this.onLoadFile(var2);
         case "checkFileExists":
            return this.onCheckFileExists(var2);
         case "getCommon":
            return this.onCommon(var2);
         case "chooseFile":
            return this.onChooseFileJFC(var2);
         case "saveFile":
            return this.onSaveFile(var2);
         case "getWebSocketPort":
            return this.onGetWebSocketPort();
         case "saveSMTPSettings":
            return this.onSaveSMTPSettings(var2);
         case "getSMTPSettings":
            return this.onGetSMTPSettings(var2);
         case "testSMTP":
            return this.onTestSMTP(var2);
         case "setPerformanceSettings":
            return this.onSetPerformanceSettings(var2);
         case "getPerformanceSettings":
            return this.onGetPerformanceSettings(var2);
         case "setAutoSyncToAllDatabanks":
            return this.onSetAutoSyncToAllDatabanks(var2);
         case "getData":
            return this.onGetData();
         case "getConfigs":
            return this.onGetConfigs();
         case "getColumns":
            return this.onGetColumns();
         case "openlink":
            return this.onOpenlink(var2);
         case "exitapp":
            return this.onExitapp();
         case "appLoaded":
            return this.onAppLoaded();
         case "appSwitched":
            return this.onAppSwitched(var2);
         case "copyToClipboard":
            return this.onCopyToClipboard(var2);
         case "startBenchmark":
            return this.onStartBenchmark();
         case "saveConditions":
            return this.onSaveConditions(var2);
         case "loadConditions":
            return this.onLoadConditions(var2);
         case "stopStrategiesSaving":
            return this.onStopStrategiesSaving();
         case "loadInitializationData":
            return this.onLoadInitializationData();
         case "openCodeEditor":
            return this.onOpenCodeEditor();
         case "openHelpDialog":
            return this.onOpenHelpDialog();
         case "openDebugConsole":
            return this.onOpenDebugConsole();
         case "openPaymentDialog":
            return this.onOpenPaymentDialog(var2);
         case "loadLicenceInfo":
            return this.onLoadLicenceInfo();
         case "customizations":
            return this.onCustomizations(var2);
         case "versions":
            return this.onVersions(var2);
         case "install":
            return this.onInstall(var2);
         case "news":
            return this.onNews();
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command", new Object[0]) + " '" + var1 + "'.", null);
      }
   }

   private String onGetData() {
      JSONObject var1 = new JSONObject();

      try {
         JSONArray var2 = WSDataObjects.getSessions().getDataArray();
         var1.put("sessions", var2);
      } catch (Exception var13) {
         Log.error("Cannot get list of sessions", var13);
      }

      try {
         JSONArray var14 = WSDataObjects.getInstruments().getDataArray();
         var1.put("instruments", var14);
      } catch (Exception var12) {
         Log.error("Cannot get list of instruments", var12);
      }

      if (!MainApp.checkProduct("QDM")) {
         try {
            JSONArray var15 = WSDataObjects.getBaskets().getDataArray();
            var1.put("baskets", var15);
         } catch (Exception var11) {
            Log.error("Cannot get list of groups", var11);
         }
      }

      try {
         JSONArray var16 = WSDataObjects.getBrokers().getDataArray();
         var1.put("brokers", var16);
         JSONObject var3 = new JSONObject();
         var3.put("id", -1);
         var3.put("name", "No filter");
         var3.put("count", 0);
         var3.put("desc", "");
         var3.put("stocks", "");
         var1.put("BrokerNoFilter", var3);
      } catch (Exception var10) {
         Log.error("Cannot get list of brokers", var10);
      }

      JSONArray var17 = WSDataObjects.getExchanges().getDataArray();
      var1.put("exchanges", var17);
      JSONArray var18 = WSDataObjects.getCountries().getDataArray();
      var1.put("countries", var18);
      JSONArray var4 = WSDataObjects.getSectors().getDataArray();
      var1.put("sectors", var4);
      long var5 = System.currentTimeMillis();

      try {
         JSONObject var7 = WSDataObjects.getData("", "list").getDataObject();
         var1.put("data", var7);
      } catch (Exception var9) {
         Log.error("Cannot get list of data", var9);
      }

      Log.info("loadingTime " + (System.currentTimeMillis() - var5));

      try {
         JSONObject var19 = WSDataObjects.getCustomData("", "list").getDataObject();
         var1.put("customdata", var19.getJSONArray("data"));
      } catch (Exception var8) {
         Log.error("Cannot get list of data", var8);
      }

      if (!MainApp.checkProduct("QDM")) {
         var1.put("mmMethods", WSDataObjects.getMMMethods().getDataArray());
         var1.put("rmMethods", WSDataObjects.getRMMethods().getDataArray());
         var1.put("tradingOptions", TradingOptions.list());
      }

      var1.put("success", "Data loaded.");
      return var1.toString();
   }

   private String onOpenHelpDialog() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("url", String.format("%d/%s", AbstractUIWebServer.port, "HELP"));
      var1.put("success", "ok");
      return var1.toString();
   }

   private String onOpenDebugConsole() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("url", String.format("%d/%s", AbstractUIWebServer.port, "DEBUGCONSOLE"));
      var1.put("success", "ok");
      return var1.toString();
   }

   private String onOpenPaymentDialog(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Log.error("ARGS:");
      String var3 = "";

      try {
         var3 = ((String[])var1.get("productString"))[0].toString();
      } catch (Exception var5) {
      }

      Log.error("Product string '" + var3 + "'");
      var2.put("url", "https://strategyquant.com/sqxfiles/iframe.html?product=" + var3);
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onLoadLicenceInfo() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();

      try {
         V571hfnsHw var3 = MainApp.v571hfnsHw();
         var2.put("validUntilSec", var3.aIpB57erT2());
         var2.put("dataTrialExpiresSoon", var3.dataTrialExpiresSoon());
         var2.put("isTrial", var3.yIifbIrWD3());
         var2.put("isSpecialTrial", var3.isSpecialTrial());
         var2.put("type", var3.ljYePnIhcR());
         var2.put("futlab", var3.a1wUchdumV());
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot load licence info.", new Object[0]), var4);
      }

      var1.put("licence", var2);
      var1.put("success", L.t("Licence info loaded.", new Object[0]));
      return var1.toString();
   }

   private String onCustomizations(Map<String, String[]> var1) {
      return Customization.load().toString();
   }

   private static String getVersionsXmlUrl() {
      String var0 = MainApp.getProduct();
      String var1;
      switch (var0) {
         case "SQUANT":
            var1 = "builds_sq";
            break;
         case "SQBR":
            var1 = "builds_sqbr";
            break;
         case "QDM":
            var1 = "builds_qdm";
            break;
         case "EAANALYZER":
            var1 = "builds_eaa";
            break;
         default:
            return null;
      }

      return "https://setup.strategyquant.com/" + var1 + ".xml";
   }

   private String onVersions(Map<String, String[]> var1) {
      JSONArray var2 = new JSONArray();

      try {
         String var28 = MainApp.getProduct();
         String var4 = getVersionsXmlUrl();
         if (var4 == null) {
            throw new Exception("Unrecognized product: " + var28);
         }

         String var5 = MainApp.getAppVersion();
         long[] var6 = parseVersion(var5);
         boolean var7 = new File(MainApp.getDataPath(), "test_installer.txt").exists();
         Log.info("Loading all versions, current: {}, url: {}, testMode: {}", new Object[]{var5, var4, var7});
         String var8 = doHttpGet(var4);
         Element var9 = XMLUtil.stringToElement(var8);
         List var10 = var9.getChildren("build");
         int var11 = -1;
         int var12 = -1;
         long var13 = -1L;

         for (Element var16 : var10) {
            String var17 = var16.getAttributeValue("number");
            if (var17 != null && !"false".equals(var16.getAttributeValue("live"))) {
               long[] var18 = parseVersion(var17);
               int var19 = var2.length();
               JSONObject var20 = new JSONObject();
               Element var21 = var16.getChild("langs");
               if (var21 != null) {
                  for (Element var23 : var21.getChildren("lang")) {
                     String var24 = var23.getAttributeValue("code");
                     if (var24 != null) {
                        Element var25 = var23.getChild("description");
                        JSONObject var26 = new JSONObject();
                        var26.put("versionName", var23.getAttributeValue("version_name", ""));
                        var26.put("versionText", var23.getAttributeValue("version_text", ""));
                        var26.put("description", var25 != null ? var25.getText().trim() : "");
                        var20.put(var24, var26);
                     }
                  }
               }

               JSONObject var35 = new JSONObject();
               var35.put("version", var17);
               var35.put("published", var16.getAttributeValue("published", ""));
               var35.put("dev", "true".equals(var16.getAttributeValue("dev")));
               var35.put("langs", var20);
               var35.put("current", false);
               var2.put(var35);
               if (var18[0] == var6[0] && var18[1] == var6[1]) {
                  var11 = var19;
               } else if (var11 == -1 && var18[0] == var6[0] && var18[1] > var13) {
                  var13 = var18[1];
                  var12 = var19;
               }
            }
         }

         byte var29 = (byte)(var11 != -1 ? var11 : var12);
         if (var29 != -1) {
            var2.getJSONObject(var29).put("current", true);
         }

         boolean var30 = false;
         String var31 = "";

         for (int var32 = 0; var32 < var2.length(); var32++) {
            long[] var34 = parseVersion(var2.getJSONObject(var32).getString("version"));
            if (var34[0] > var6[0] || var34[0] == var6[0] && var34[1] > var6[1]) {
               var30 = true;
               var31 = var2.getJSONObject(var32).getString("version");
               break;
            }
         }

         JSONObject var33 = new JSONObject();
         var33.put("versions", var2);
         var33.put("isUpdateAvailable", var30);
         var33.put("newerVersion", var31);
         var33.put("currentVersion", var5);
         var33.put("success", "Versions loaded");
         Log.info(
            "Versions loaded: {} build(s) found, current: {}, update available: {}{}",
            new Object[]{var2.length(), var5, var30, var30 ? ", newer: " + var31 : ""}
         );
         return var33.toString();
      } catch (Exception var27) {
         Log.error("Failed to load versions", var27);
         Log.info("Versions loaded: {} build(s) found", var2.length());
         JSONObject var3 = new JSONObject();
         var3.put("versions", var2);
         var3.put("isUpdateAvailable", false);
         var3.put("success", "Versions loaded");
         return var3.toString();
      }
   }

   private String onInstall(Map<String, String[]> var1) {
      final String var2 = this.getParam(var1, "version")[0];
      Log.info("Starting installer for version: {}", var2);
      JSONObject var3 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            try {
               String var1x = MainApp.getDataPath() + "SQ_Installer.exe";
               if (MainApp.isBrazilianEdition()) {
                  var1x = MainApp.getDataPath() + "SQBR_Installer.exe";
               } else if (MainApp.checkProduct("QDM")) {
                  var1x = MainApp.getDataPath() + "QDM_Installer.exe";
               }

               String var2x = "build=" + var2;
               MainServlet.Log.info("Launching: {} {}", var1x, var2x);
               new ProcessBuilder(var1x, var2x).directory(new File(MainApp.getDataPath())).start();
            } catch (Exception var3x) {
               MainServlet.Log.error("Failed to launch installer", var3x);
            }

            MainApp.exitApp();
         }
      }).start();
      var3.put("success", L.t("Done", new Object[0]));
      return var3.toString();
   }

   private static long[] parseVersion(String var0) {
      String[] var1 = var0.split("\\.", 2);

      try {
         long var2 = Long.parseLong(var1[0]);
         long var4 = var1.length > 1 ? Long.parseLong(var1[1]) : 0L;
         return new long[]{var2, var4};
      } catch (NumberFormatException var6) {
         return new long[]{0L, 0L};
      }
   }

   private String onNews() {
      JSONArray var1 = new JSONArray();

      try {
         String var2 = doHttpGet("https://strategyquant.com/blog/rss");
         Element var3 = XMLUtil.stringToElement(var2);
         List var4 = var3.getChild("channel").getChildren("item");
         int var5 = Math.min(var4.size(), 1);

         for (int var6 = 0; var6 < var5; var6++) {
            Element var7 = (Element)var4.get(var6);
            JSONObject var8 = new JSONObject();
            var8.put("title", var7.getChildText("title"));
            var8.put("link", var7.getChildText("link"));
            var8.put("pubDate", var7.getChildText("pubDate"));
            var1.put(var8);
         }
      } catch (Exception var9) {
         Log.error("Failed to load news", var9);
      }

      JSONObject var10 = new JSONObject();
      var10.put("items", var1);
      var10.put("success", "News loaded");
      return var10.toString();
   }

   private static String doHttpGet(String var0) throws IOException {
      URL var1 = new URL(var0);
      HttpURLConnection var2 = (HttpURLConnection)var1.openConnection();
      var2.setRequestMethod("GET");
      BufferedReader var3 = new BufferedReader(new InputStreamReader(var2.getInputStream(), StandardCharsets.UTF_8));

      String var6;
      try {
         StringBuilder var4 = new StringBuilder();

         String var5;
         while ((var5 = var3.readLine()) != null) {
            var4.append(var5);
         }

         var6 = var4.toString();
      } catch (Throwable var8) {
         try {
            var3.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      var3.close();
      return var6;
   }

   private String onOpenCodeEditor() throws Exception {
      JSONObject var1 = new JSONObject();
      if (this.isChromiumBrowser()) {
         Program.get("CodeEditor").call("open", null);
      } else {
         var1.put("url", String.format("%d/%s", AbstractUIWebServer.port, "SQEDITOR"));
      }

      var1.put("success", "ok");
      return var1.toString();
   }

   private String onLoadInitializationData() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      ArrayList var3 = SQPluginManager.getPlugins(IResultPlugin.class);

      for (int var4 = 0; var4 < var3.size(); var4++) {
         IResultPlugin var5 = (IResultPlugin)var3.get(var4);

         try {
            JSONObject var6 = var5.getInitializationData();
            if (var6 != null) {
               var2.put(var5.getKey(), var6);
            }
         } catch (Exception var10) {
            Log.error("Cannot get initialization data of Result plugin '" + var5.getKey() + "'", var10);
         }
      }

      var1.put("results", var2);
      JSONObject var11 = new JSONObject();
      ArrayList var12 = SQPluginManager.getPlugins(ISettingTabPlugin.class);

      for (int var13 = 0; var13 < var12.size(); var13++) {
         ISettingTabPlugin var7 = (ISettingTabPlugin)var12.get(var13);

         try {
            JSONObject var8 = var7.getInitializationData();
            if (var8 != null) {
               var11.put(var7.getSettingName(), var8);
            }
         } catch (Exception var9) {
            Log.error("Cannot get initialization data of Setting plugin '" + var7.getSettingName() + "'", var9);
         }
      }

      var1.put("settings", var11);
      var1.put("success", "ok");
      return var1.toString();
   }

   private String onAlive() {
      return "ok";
   }

   private String onChooseFileJFC(Map<String, String[]> var1) throws Exception {
      throw new Exception("Not implemented for Electron.");
   }

   private String onSaveFile(Map<String, String[]> var1) throws Exception {
      throw new Exception("Not implemented for Electron.");
   }

   public static File saveFileUsingJFC(String var0, String var1, String[] var2, String[] var3, String var4, String var5) throws Exception {
      throw new Exception("Not implemented for Electron.");
   }

   private static String correctFileExtension(String var0, String[] var1) {
      String var2 = SQUtils.getExtension(var0).toLowerCase();
      if (var1.length == 0) {
         return var0;
      }

      boolean var3 = false;

      for (String var7 : var1) {
         if (var2.equals(var7)) {
            var3 = true;
            break;
         }
      }

      return var3 ? var0 : var0 + "." + var1[0];
   }

   private String onCommon(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      var2.put("product", MainApp.getProduct());
      var2.put("chromiumBrowser", this.isChromiumBrowser());
      var2.put("appVersion", MainApp.getAppVersion());
      var2.put("success", L.t("Application started.", new Object[0]));
      return var2.toString();
   }

   private String onRegenerate() {
      JSONObject var1 = new JSONObject();

      try {
         this.webServer.regenerateFiles();
         var1.put("success", L.t("GUI regenerated.", new Object[0]));
         return var1.toString();
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot regenerate GUI.", new Object[0]), var3);
      }
   }

   private String onStartApp(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("app"))[0].toString();
         this.webServer.getWebAppManager().startApp(var3);
         var2.put("success", L.t("Application started.", new Object[0]));
         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Error while starting app.", new Object[0]), var4);
      }
   }

   private String onGetAppCode() {
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("App code obtained", new Object[0]));
      var1.put("appCode", MainApp.getProduct());
      return var1.toString();
   }

   private String onGetApps() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      Map var3 = this.webServer.getWebAppManager().getApps();
      if (var3 != null) {
         for (String var5 : var3.keySet()) {
            JSONObject var6 = new JSONObject();
            var6.put("code", var5);
            var6.put("name", var3.get(var5));
            var2.put(var6);
         }
      }

      var1.put("success", L.t("Apps listed", new Object[0]));
      var1.put("apps", var2);
      return var1.toString();
   }

   private String onExit() {
      JSONObject var1 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            try {
               sleep(1000L);
            } catch (InterruptedException var2) {
            }

            MainApp.exitApp();
         }
      }).start();
      var1.put("success", L.t("Exiting app...", new Object[0]));
      return var1.toString();
   }

   private String onToFront() {
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("Window brought to front", new Object[0]));
      return var1.toString();
   }

   private String onMinimize() {
      JSONObject var1 = new JSONObject();
      if (this.isChromiumBrowser()) {
         Electron.getInstance().minimize();
         var1.put("success", L.t("Window minimized", new Object[0]));
      } else {
         var1.put("warning", L.t("Window not minimized. Request has not come from the Chromium app.", new Object[0]));
      }

      return var1.toString();
   }

   private String onMaximize() {
      JSONObject var1 = new JSONObject();
      if (this.isChromiumBrowser()) {
         Electron.getInstance().maximize();
         var1.put("success", L.t("Window maximized", new Object[0]));
      } else {
         var1.put("warning", L.t("Window not maximized. Request has not come from the Chromium app.", new Object[0]));
      }

      return var1.toString();
   }

   private String onToggleFullscreen() {
      JSONObject var1 = new JSONObject();
      if (this.isChromiumBrowser()) {
         if (BrowserGUI.getInstance().isFullscreen()) {
            var1.put("success", L.t("Window set to normal size", new Object[0]));
         } else {
            var1.put("success", L.t("Window set to fullscreen", new Object[0]));
         }
      } else {
         var1.put("warning", L.t("Window not set to fullscreen. Request has not come from the Chromium app.", new Object[0]));
      }

      return var1.toString();
   }

   private String onSetRemoteAccess(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("allow"))[0].toString();
         boolean var4 = Boolean.parseBoolean(((String[])var1.get("requirePassword"))[0].toString());
         String var5 = ((String[])var1.get("secureConnection"))[0].toString();
         RemoteAccessConfig.getInstance().setRemoteAccess(Boolean.parseBoolean(var3));
         RemoteAccessConfig.getInstance().setRequirePassword(var4);
         if (this.isChromiumBrowser()) {
            String var6 = ((String[])var1.get("password"))[0].toString();
            if (var4 && var6.trim().isEmpty()) {
               throw new Exception(L.t("No Password set.", new Object[0]));
            }

            RemoteAccessConfig.getInstance().setPassword(var4 ? var6 : null);
         }

         RemoteAccessConfig.getInstance().setSecureConnection(Boolean.parseBoolean(var5));
         RemoteAccessConfig.getInstance().saveSettings();
         var2.put("success", L.t("Settings saved.", new Object[0]));
         return var2.toString();
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Error while saving remote access settings", new Object[0]), var7);
      }
   }

   private String onGetRemoteAccess() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      var2.put("allow", RemoteAccessConfig.getInstance().isRemoteAccess());
      var2.put("requirePassword", RemoteAccessConfig.getInstance().isPasswordRequired());
      var2.put("passwordLength", RemoteAccessConfig.getInstance().getPasswordLength());
      var2.put("secureConnection", RemoteAccessConfig.getInstance().isSecureConnection());
      var1.put("settings", var2);
      var1.put("success", L.t("Settings loaded.", new Object[0]));
      return var1.toString();
   }

   private String onLogin(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         if (RemoteAccessConfig.getInstance().isPasswordRequired()) {
            String var3 = ((String[])var1.get("password"))[0].toString();
            if (!RemoteAccessConfig.getInstance().passwordValid(var3)) {
               Log.info("Remote access login was unsuccessful. Incorrect password");
               return apiErrorJSON(L.t("Remote access login failed - Incorrect password", new Object[0]), null);
            }

            var2.put("token", RemoteAccessConfig.getInstance().getToken());
         }

         Log.info("Remote access login was successful");
         var2.put("success", L.t("Login was successful.", new Object[0]));
         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Remote access login failed", new Object[0]), var4);
      }
   }

   private String onCheckAccess() {
      JSONObject var1 = new JSONObject();
      var1.put("success", L.t("Access granted.", new Object[0]));
      return var1.toString();
   }

   private String onOpenFolder(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("path"))[0].toString();
         File var4 = new File(var3);
         Desktop.getDesktop().open(var4.isDirectory() ? var4 : var4.getParentFile());
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Opening folder failed", new Object[0]), var5);
      }

      var2.put("success", L.t("Folder opened.", new Object[0]));
      return var2.toString();
   }

   private String onOpenBrowser(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("url"))[0].toString();
         this.openBrowser(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Opening system browser failed", new Object[0]), var4);
      }

      var2.put("success", L.t("URL loaded in default browser.", new Object[0]));
      return var2.toString();
   }

   private void openBrowser(String var1) throws Exception {
      try {
         if (var1 == null) {
            throw new Exception("URL is null.");
         }

         Log.info("Opening url '" + var1 + "' in system browser...");
         OperatingSystem var2 = new OperatingSystem();
         if (var2.isUnix()) {
            Runtime var3 = Runtime.getRuntime();
            String[] var4 = new String[]{"xdg-open", var1};
            var3.exec(var4);
         } else if (Desktop.isDesktopSupported()) {
            Desktop var6 = Desktop.getDesktop();
            if (var6 != null && var6.isSupported(Action.BROWSE)) {
               var6.browse(new URI(var1));
            }
         }
      } catch (Exception var5) {
         Log.error("Cannot open browser - " + var5.getMessage(), var5);
      }
   }

   private String onOpenHelp(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("topic"))[0].toString();
         this.openBrowser("https://strategyquant.com/doc/strategyquant/" + var3);
      } catch (Exception var4) {
         Log.error("Error while opening Help. Exc.", var4);
      }

      var2.put("success", L.t("Help opened.", new Object[0]));
      return var2.toString();
   }

   private String onOpenDocument(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("fileName"))[0].toString();
         String var4 = ((String[])var1.get("extension"))[0].toString();
         if (Desktop.isDesktopSupported()) {
            try {
               String var5 = L.getCurLangShortCut();
               String var6 = String.format("%s.%s", var3, var4);
               String var7 = String.format("%s%s.%s", var3, var5 != null && !var5.isEmpty() && !var5.equals("en") ? "_" + var5 : "", var4);
               if (!new File(MainApp.getDataPath() + var7).exists()) {
                  var7 = var6;
               }

               File var8 = new File(MainApp.getDataPath() + var7);
               if (!var8.exists()) {
                  throw new Exception(L.t("Document '%s' not found.", new Object[]{var3}));
               }

               Desktop.getDesktop().open(var8);
            } catch (IOException var9) {
               throw new Exception(L.t("Found no application associated with extension '%s'.", new Object[]{SQUtils.getExtension(var3)}));
            }
         }
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Error while opening document. ", new Object[0]), var10);
      }

      var2.put("success", L.t("Document opened.", new Object[0]));
      return var2.toString();
   }

   private String fixHelpTopic(String var1) {
      return var1.contains("#") ? var1.replace("#", ".htm#") : var1 + ".htm";
   }

   private String onGetSettings() {
      JSONObject var1 = new JSONObject();

      try {
         JSONObject var2 = MainApp.settings().toJSON();
         var2.put("langDebugMode", L.debug);
         var2.put("developmentMode", !MainApp.isRelease());
         var2.put("pickerDebug", Stockpicker.isDebugModeEnabled());
         var2.put("results2", new File(MainApp.getDataPath() + "/results2.txt").exists());
         var1.put("settings", var2);
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot resolve app settings", new Object[0]), var3);
      }

      var1.put("success", L.t("Settings returned.", new Object[0]));
      return var1.toString();
   }

   private String onSaveSetting(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("key"))[0].toString();
         String var4 = ((String[])var1.get("value"))[0].toString();
         MainApp.settings().set(var3, var4);
         MainApp.settings().save();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot save setting", new Object[0]), var5);
      }

      var2.put("success", L.t("Setting saved.", new Object[0]));
      return var2.toString();
   }

   private String onSaveSMTPSettings(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         MainApp.settings().set("SMTPServer", ((String[])var1.get("server"))[0].toString());
         MainApp.settings().set("SMTPPort", ((String[])var1.get("port"))[0].toString());
         MainApp.settings().set("SMTPUsername", ((String[])var1.get("username"))[0].toString());
         MainApp.settings().set("SMTPPassword", ((String[])var1.get("password"))[0].toString());
         MainApp.settings().set("SMTPEmailFrom", ((String[])var1.get("emailFrom"))[0].toString());
         MainApp.settings().set("SMTPUseSSL", ((String[])var1.get("ssl"))[0].toString());
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot save SMTP settings", new Object[0]), var4);
      }

      var2.put("success", L.t("SMTP settings saved.", new Object[0]));
      return var2.toString();
   }

   private String onGetSMTPSettings(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      var2.put("server", MainApp.settings().get("SMTPServer", ""));
      var2.put("port", Integer.parseInt(MainApp.settings().get("SMTPPort", "25")));
      var2.put("username", MainApp.settings().get("SMTPUsername", ""));
      var2.put("password", MainApp.settings().get("SMTPPassword", ""));
      var2.put("emailFrom", MainApp.settings().get("SMTPEmailFrom", ""));
      var2.put("ssl", Boolean.parseBoolean(MainApp.settings().get("SMTPUseSSL", "false")));
      var2.put("success", L.t("SMTP settings listed.", new Object[0]));
      return var2.toString();
   }

   private String onTestSMTP(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("server"))[0].toString();
      int var4 = Integer.parseInt(((String[])var1.get("port"))[0].toString());
      String var5 = ((String[])var1.get("username"))[0].toString();
      String var6 = ((String[])var1.get("password"))[0].toString();
      String var7 = ((String[])var1.get("emailFrom"))[0].toString();
      boolean var8 = Boolean.parseBoolean(((String[])var1.get("ssl"))[0].toString());
      String var9 = ((String[])var1.get("recipient"))[0].toString();

      try {
         EmailSender.sendTo(
            var3, var4, var5, var6, L.t("StrategyQuant Test Email", new Object[0]), L.t("This is a test email.", new Object[0]), var7, var9, var8
         );
         var2.put("result", L.t("Test Email was sent to your email address.", new Object[0]));
      } catch (Exception var11) {
         Log.error("Test Email could not be sent. Please check your SMTP settings.", var11);
         var2.put("result", L.t("Test Email could not be sent. Please check your SMTP settings.", new Object[0]));
      }

      var2.put("success", L.t("Tested.", new Object[0]));
      return var2.toString();
   }

   private String onCreateFile(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         if (MainApp.getProduct().equals("BACKTESTNODE")) {
            throw new Exception(L.t("Trying to create files on the remote server - access forbidden. Please contact our support.", new Object[0]));
         }

         String var3 = ((String[])var1.get("filePath"))[0].toString();
         String var4 = ((String[])var1.get("fileContent"))[0].toString();
         File var5 = SQFileManager.saveFile(var3, var4);

         try {
            File var6 = new File(SQPaths.configsDirPath);
            if (var6.getAbsolutePath().equals(var5.getParentFile().getAbsolutePath())) {
               try {
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getTaskTemplates(), new String[]{"SQUANT"});
               } catch (Exception var8) {
                  Log.error("Cannot send websocket task templates update. ", var8);
               }
            }
         } catch (Exception var9) {
            Log.error("Refreshing config templates failed", var9);
         }
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot save file", new Object[0]), var10);
      }

      var2.put("success", L.t("File saved.", new Object[0]));
      return var2.toString();
   }

   private String onLoadFile(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         if (MainApp.getProduct().equals("BACKTESTNODE")) {
            throw new Exception(L.t("Trying to load files from the remote server - access forbidden. Please contact our support.", new Object[0]));
         }

         String var3 = this.tryGetParam(var1, "filePath")[0];
         File var4 = new File(var3);
         if (!var4.exists()) {
            return apiErrorJSON(L.t("File '%s' doesn't exist.", new Object[]{var3}), null);
         }

         var2.put("fileContent", SQFileManager.loadFile(var3));
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot load file", new Object[0]), var5);
      }

      var2.put("success", L.t("File loaded.", new Object[0]));
      return var2.toString();
   }

   private String onCheckFileExists(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      String var3;
      try {
         var3 = this.tryGetParam(var1, "filePath")[0];
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot check file - filePath not specified", new Object[0]), null);
      }

      File var4 = new File(var3);
      var2.put("exists", var4.exists());
      var2.put("success", L.t("File checked.", new Object[0]));
      return var2.toString();
   }

   @PrecachedRequest(relativeURL = "/main/getWebSocketPort")
   private String onGetWebSocketPort() {
      JSONObject var1 = new JSONObject();
      String var2 = MainApp.getProduct();
      int var3 = JettyServer.port;
      switch (var2) {
         case "SQMANAGER":
            boolean var6 = this.webServer.getWebAppManager().isAppRunning("SQTRADER");
            if (var6) {
               var3 = WebAppManager.getAppPorts().get("SQTRADER");
            }
         default:
            var1.put("port", var3);
            var1.put("success", L.t("Port returned.", new Object[0]));
            return var1.toString();
      }
   }

   private String onSetPerformanceSettings(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      var2.put("success", L.t("Configuration options saved.", new Object[0]));

      try {
         String var3 = ((String[])var1.get("coreUsage"))[0].toString();
         if (var3.equals("X")) {
            var3 = var3 + ((String[])var1.get("customCores"))[0];
         }

         MainApp.settings().set("highPriority", ((String[])var1.get("highPriority"))[0]);
         MainApp.settings().set("coreUsage", var3);
         MainApp.settings().set("threadAffinity", ((String[])var1.get("threadAffinity"))[0]);
         MainApp.settings().set("gpuAccelerated", ((String[])var1.get("gpuAccelerated"))[0]);
         MainApp.settings().set("dontStorePendingOrders", ((String[])var1.get("dontStorePendingOrders"))[0]);
         MainApp.settings().set("memoryProtection", ((String[])var1.get("memoryProtection"))[0]);
         MainApp.settings().set("memoryCleanup", ((String[])var1.get("memoryCleanup"))[0]);
         MainApp.settings().set("memoryCleanupInterval", ((String[])var1.get("cleanupInterval"))[0]);
         MainApp.settings().set("databankSyncType", ((String[])var1.get("databankSyncInterval"))[0]);
         MainApp.settings().set("syncDatabanksAfterTaskDone", ((String[])var1.get("syncDatabanksAfterTaskDone"))[0]);
         MainApp.settings().set("storeChartData", ((String[])var1.get("storeChartData"))[0]);
         MainApp.settings().set("globalSoundsOff", ((String[])var1.get("soundsoff"))[0].toString());
         MainApp.settings().set("useAdvancedFileChooser", ((String[])var1.get("advancedFileChooser"))[0].toString());
         MainApp.settings().set("showControlOrders", ((String[])var1.get("showControlOrders"))[0].toString());
         MainApp.settings().set("headerCustomText", ((String[])var1.get("headerCustomText"))[0].toString());
         MainApp.settings().set("footerCustomText", ((String[])var1.get("footerCustomText"))[0].toString());
         MainApp.settings().set("autoUpdate", ((String[])var1.get("autoUpdate"))[0].toString());
         MainApp.settings().set("defaultResultToDisplay", ((String[])var1.get("defaultResultToDisplay"))[0].toString());
         MainApp.settings().set("ComputePipsMetrics", ((String[])var1.get("computePipsMetrics"))[0].toString());
         MainApp.settings().set("ComputePctsMetrics", ((String[])var1.get("computePctsMetrics"))[0].toString());
         MainApp.settings().set("ComputeSeparateMetrics", ((String[])var1.get("computeSeparateMetrics"))[0].toString());
         MainApp.settings().set("debugLevelActive", ((String[])var1.get("debugLevelActive"))[0].toString());
         MainApp.settings().set("dontStoreOP3DChartsData", ((String[])var1.get("dontStoreOP3DChartsData"))[0].toString());
         String var4 = ((String[])var1.get("gc"))[0].toString();
         LauncherConfig var5 = LauncherConfig.getInstance();
         var5.memoryUsage = Integer.parseInt(((String[])var1.get("memoryUsage"))[0]);
         var5.memoryInGB = Integer.parseInt(((String[])var1.get("memory"))[0]);
         var5.gcType = var4;
         var5.save();
         long var6 = HardwareInfo.getTotalMemoryInGb();
         if (var5.memoryInGB > var6 && var6 > 8L) {
            var2.put(
               "success",
               L.t(
                  "Configuration options saved. But you've assigned more memory than your system has available. This can cause startup failures. Try reducing the memory setting.",
                  new Object[0]
               )
            );
         }

         MainApp.settings().set("GCType", var4);
      } catch (Exception var8) {
         throw new Exception(L.t("Error while saving performance options.", new Object[0]), var8);
      }

      return var2.toString();
   }

   private String onGetPerformanceSettings(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = MainApp.settings().get("coreUsage", "-1");
      var2.put("cores", Performance.totalCores);
      var2.put("coreUsage", var3.startsWith("X") ? "X" : var3);
      var2.put("threadAffinity", Boolean.parseBoolean(MainApp.settings().get("threadAffinity", "false")));
      var2.put("highPriority", Boolean.parseBoolean(MainApp.settings().get("highPriority", "false")));
      LauncherConfig var4 = LauncherConfig.getInstance();
      var2.put("memory", var4.memoryInGB);
      var2.put("memoryUsage", var4.memoryUsage);
      var2.put("gpuAccelerated", Boolean.parseBoolean(MainApp.settings().get("gpuAccelerated", "true")));
      var2.put("dontStorePendingOrders", Boolean.parseBoolean(MainApp.settings().get("dontStorePendingOrders", "true")));
      var2.put("memoryProtection", Boolean.parseBoolean(MainApp.settings().get("memoryProtection", "false")));
      var2.put("memoryCleanup", Boolean.parseBoolean(MainApp.settings().get("memoryCleanup", "false")));
      var2.put("cleanupInterval", Integer.parseInt(MainApp.settings().get("memoryCleanupInterval", "0")));
      var2.put("databankSyncInterval", MainApp.settings().get("databankSyncType", "Auto-sync every 1 hour"));
      var2.put("storeChartData", Boolean.parseBoolean(MainApp.settings().get("storeChartData", "false")));
      var2.put("syncDatabanksAfterTaskDone", Boolean.parseBoolean(MainApp.settings().get("syncDatabanksAfterTaskDone", "false")));
      var2.put("dontStoreOP3DChartsData", Boolean.parseBoolean(MainApp.settings().get("dontStoreOP3DChartsData", "true")));
      String var5 = var4.gcType;
      if (var5 != null && !var5.equals("G1GC") && !var5.equals("ParallelGC")) {
         var5 = MainApp.settings().get("GCType", "ParallelGC");
      }

      var2.put("gc", var5 != null ? var5 : "auto");
      JSONArray var6 = new JSONArray();

      for (int var7 = 0; var7 < Performance.totalCores; var7++) {
         var6.put(var7 + 1);
      }

      var2.put("coreOptions", var6);
      var2.put("customCores", var3.startsWith("X") ? var3.replaceAll("\\D+", "") : Performance.totalCores);
      var2.put("debugLevelActive", Boolean.parseBoolean(MainApp.settings().get("debugLevelActive", "false")));
      var2.put("advancedFileChooser", Boolean.parseBoolean(MainApp.settings().get("useAdvancedFileChooser", "false")));
      var2.put("success", L.t("Performance settings listed.", new Object[0]));
      return var2.toString();
   }

   private String onSetAutoSyncToAllDatabanks(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "syncType")[0];
         List var4 = ProjectEngine.list();

         for (int var5 = 0; var5 < var4.size(); var5++) {
            SQProject var6 = (SQProject)var4.get(var5);
            ArrayList var7 = var6.getDatabanks().list();

            for (int var8 = 0; var8 < var7.size(); var8++) {
               ((Databank)var7.get(var8)).setSyncType(var3);
            }

            Element var13 = var6.getConfig();
            Element var9 = var13.getChild("Databanks");
            if (var9 != null) {
               List var10 = var9.getChildren("Databank");

               for (int var11 = 0; var11 < var10.size(); var11++) {
                  ((Element)var10.get(var11)).setAttribute("syncType", var3);
               }
            }

            var6.saveConfig();
         }
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot apply auto sync option", new Object[0]), var12);
      }

      var2.put("success", L.t("Auto sync option applied to all databanks.", new Object[0]));
      return var2.toString();
   }

   private String onGetColumns() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();

      try {
         JSONArray var3 = new JSONArray();

         for (DatabankColumn var5 : DatabankColumns.get().getAvailableClasses()) {
            JSONObject var6 = new JSONObject();
            var6.put("name", var5.getName());
            var6.put("class", var5.getClass().getSimpleName());
            var6.put("type", var5.getType());
            var6.put("tooltip", var5.getTooltip());
            var3.put(var6);
         }

         var2.put("databankColumns", var3);
         JSONArray var10 = new JSONArray();

         for (WalkForwardColumn var13 : WalkForwardColumns.get().getAvailableClasses()) {
            JSONObject var7 = new JSONObject();
            var7.put("name", var13.getName());
            var7.put("class", var13.getClass().getSimpleName());
            var7.put("type", var13.getType());
            var7.put("tooltip", var13.getTooltip());
            var10.put(var7);
         }

         var2.put("wfColumns", var10);
         JSONArray var12 = new JSONArray();

         for (TradelistColumn var16 : TradelistColumnsList.get().getAvailableClasses()) {
            JSONObject var8 = new JSONObject();
            var8.put("name", var16.getName());
            var8.put("class", var16.getClass().getSimpleName());
            var8.put("type", var16.getType());
            var8.put("tooltip", var16.getTooltip());
            var8.put("fontBold", var16.isFontBold());
            var12.put(var8);
         }

         var2.put("tradelistColumns", var12);
         JSONArray var15 = new JSONArray();

         for (String var18 : RecentColumns.getRecentCols()) {
            var15.put(var18);
         }

         var2.put("recentColumns", var15);
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Error while loading common data", new Object[0]), var9);
      }

      var1.put("commonData", var2);
      var1.put("success", L.t("Common data returned.", new Object[0]));
      return var1.toString();
   }

   private String onGetConfigs() {
      if (getConfigsCache != null) {
         return getConfigsCache;
      }

      JsonCreator var1 = new JsonCreator();
      var1.beginObject();
      if (!MainApp.checkProduct("QDM")) {
         try {
            var1.putBeginObject("configs");
            List var2 = ProjectEngine.list();
            boolean var3 = true;

            for (int var4 = 0; var4 < var2.size(); var4++) {
               SQProject var5 = (SQProject)var2.get(var4);
               String var6 = var5.getName();
               if (ProjectEngine.isDefaulProject(var6)) {
                  if (!var3) {
                     var1.separator();
                  }

                  var1.putRaw(var6, var5.toJSON(), false);
                  var3 = false;
               }
            }

            var1.endObject(true);
         } catch (Exception var7) {
            return apiErrorJSON(L.t("Error while loading project configs", new Object[0]), var7);
         }
      }

      var1.put("success", "Project configs loaded.", false);
      var1.endObject(false);
      getConfigsCache = var1.toJson();
      return getConfigsCache;
   }

   private String onOpenlink(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("url"))[0].toString();
      SQUtils.openUrlInDefaultWebBrowser(var3);
      var2.put("success", L.t("Done", new Object[0]));
      return var2.toString();
   }

   private String onExitapp() {
      JSONObject var1 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            MainApp.exitApp();
         }
      }).start();
      var1.put("success", L.t("Done", new Object[0]));
      return var1.toString();
   }

   private String onAppLoaded() {
      JSONObject var1 = new JSONObject();
      if (!this.loadNotified) {
         MainApp.AppLoaded = true;
         InitAfterGUILoad.notifyAllListeners();
         if (!MainApp.runInConsole()) {
            Log.info(MainApp.getProductName() + " loaded in " + SQTime.formatDateTime((System.currentTimeMillis() - MainApp.TimeAppStarted) / 1000L));
            BrowserGUI.getInstance().onAppLoaded();
         }

         if (MainApp.checkProduct("SQUANT")) {
            ProjectEngine.loadStrategies();
            this.notifyUIIfSnippetsCompilationFailed();
            this.notifyUIAboutImportantUpdates();
            BasketOfStocksManager.getInstance().synhronizeAsync(new Runnable() {
               @Override
               public void run() {
                  try {
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getData("", "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  } catch (Exception var2) {
                     MainServlet.Log.error("Error while sending WS messages about stockgroup synchronization.", var2);
                  }
               }
            });
            BrokerManager.getInstance().synhronizeAsync(new Runnable() {
               @Override
               public void run() {
                  try {
                     SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  } catch (Exception var2) {
                     MainServlet.Log.error("Error while sending WS messages about stockgroup synchronization.", var2);
                  }
               }
            });
            if (MainApp.isFirstRun() && MainApp.isBrazilianEdition()) {
               try {
                  this.onFirstRunBrazilianEdition();
               } catch (Exception var4) {
                  Log.error("Error while intiializing brazilian data", var4);
               }
            }

            boolean var2 = MainApp.settings().getBoolean("highPriority", false);
            if (var2) {
               new ProcessPriorityAdjuster().adjustPriorityToHigh();
            }
         }

         this.loadNotified = true;
      }

      try {
         BrowserGUI.notifyUILicenseChanged(true);
      } catch (Exception var3) {
      }

      var1.put("success", L.t("App notified", new Object[0]));
      return var1.toString();
   }

   private void onFirstRunBrazilianEdition() throws Exception {
      ArrayList var1 = DataManager.listSafe();
      LinkedList var2 = new LinkedList();

      for (DataInfo var4 : var1) {
         if (var4.source == 6 && var4.uSymbol != null) {
            var2.add(var4.uSymbol);
         }
      }

      TickerFilterDto var9 = new TickerFilterDto();
      var9.setSearchInTicker(true);
      var9.setExactMatch(true);
      var9.setNames(var2.toArray(new String[0]));
      List var10 = var2.isEmpty() ? new LinkedList() : HistoryDataManager.get().getFuturesTickers(var9);
      HashMap var5 = new HashMap();

      for (TickerDto var7 : var10) {
         var5.put(var7.getTicker(), var7);
      }

      for (DataInfo var12 : var1) {
         if (var12.source == 6 && var12.uSymbol != null) {
            TickerDto var8 = (TickerDto)var5.get(var12.uSymbol);
            if (var8 != null && "BMF".equals(var8.getMarketName())) {
               DownloadDispatcher.get().updateHistoryData(var12, var8);
            }
         }
      }
   }

   private void notifyUIIfSnippetsCompilationFailed() {
      try {
         CompilationResult var1 = SnippetsCompiler.getInstance().getLastCompilationResult();
         if (var1 == null || var1.success) {
            return;
         }

         DataToSend var2 = new DataToSend("SnippetsCompilationFailed", new JSONObject().put("errors", var1.getAsSimpleString()));
         SQWebSocketManager.addToDataQueue(var2, new String[]{"SQUANT"});
      } catch (Exception var3) {
         Log.error("Error while checking snippets. Exc.", var3);
      }
   }

   private void notifyUIAboutImportantUpdates() {
      try {
         File var1 = new File(SQStructure.INTERNAL_DIR_PATH + "UPDATE.dat");
         if (var1.exists()) {
            String var2 = SQUtils.fileToString(var1);
            DataToSend var3 = new DataToSend("ImportantUpdateChanges", new JSONObject().put("info", var2));
            SQWebSocketManager.addToDataQueue(var3, new String[]{"HOME"});
            var1.delete();
         }
      } catch (Exception var4) {
         Log.error("Error while checking snippets. Exc.", var4);
      }
   }

   private String onAppSwitched(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("productCode"))[0].toString();
      ProjectFeeds.setActiveProduct(var3);
      Log.debug("App switched to {}", var3);

      try {
         if (var3.equals("RETESTER")) {
            SQWebSocketManager.getInstance().sendProjectDataImmediately(ProjectEngine.get("Retester"));
         } else if (var3.equals("BUILDER")) {
            SQWebSocketManager.getInstance().sendProjectDataImmediately(ProjectEngine.get("Builder"));
         } else if (var3.equals("OPTIMIZER")) {
            SQWebSocketManager.getInstance().sendProjectDataImmediately(ProjectEngine.get("Optimizer"));
         } else if (var3.equals("PORTFOLIOMASTER")) {
            SQWebSocketManager.getInstance().sendProjectDataImmediately(ProjectEngine.get("PortfolioMaster"));
         } else if (var3.equals("PORTFOLIOCOMPOSER")) {
            SQWebSocketManager.getInstance().sendProjectDataImmediately(ProjectEngine.get("PortfolioComposer"));
         }
      } catch (Exception var5) {
         Log.error("Sending project updates failed", var5);
      }

      var2.put("success", L.t("App switched to %s.", new Object[]{var3}));
      return var2.toString();
   }

   private String onCopyToClipboard(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "data")[0];
         StringSelection var4 = new StringSelection(var3);
         Clipboard var5 = Toolkit.getDefaultToolkit().getSystemClipboard();
         var5.setContents(var4, null);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot load file", new Object[0]), var6);
      }

      var2.put("success", L.t("Data copied", new Object[0]));
      return var2.toString();
   }

   private String onStartBenchmark() {
      JSONObject var1 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            try {
               BenchmarkProgressEngine var1x = new BenchmarkProgressEngine();
               var1x.registerProgressListener(new IProgressListener() {
                  public void onProgress(double var1) {
                     SQWebSocketManager.addToDataQueue(new DataToSend("benchmark", new JSONObject().put("progress", var1)), new String[]{"SQUANT"});
                  }

                  public void onError(double var1, String var3) {
                  }

                  public void onDone() {
                  }
               });
               BenchmarkEngine var2 = new BenchmarkEngine(var1x);
               var2.initData();
               var2.startTest(false, 0, 60);
               ArrayList var3 = var2.getBenchmarkResults();
               BenchmarkResult var4 = (BenchmarkResult)var3.get(0);
               JSONObject var5 = new JSONObject();
               var5.put("dataLoadTime", SQUtils.formatDuration(var4.dataLoadTime));
               var5.put("testTime", SQUtils.formatDuration(var4.testTime));
               var5.put("avgStrategiesPerHour", (long)var4.avgStrategiesPerHour);
               var5.put("timePerTick", SQUtils.d2String(var4.timePerTick, 10));
               var5.put("strategyAverageTime", SQUtils.d2(var4.strategyAverageTime / 1000.0));
               var5.put("totalTrades", var4.totalTrades);
               var5.put("totalTicks", BenchmarkResult.TOTAL_TICKS);
               Performance.benchmarkTimePerTick = var4.timePerTick;
               MainApp.settings().set("BenchmarkTimePerTick", var4.timePerTick + "");
               SQWebSocketManager.addToDataQueue(new DataToSend("benchmark", new JSONObject().put("results", var5)), new String[]{"SQUANT"});
               var2.deleteData();
            } catch (Exception var6) {
               MainServlet.Log.error("Error while running Benchmark... Exc.", var6);
            }
         }
      }).start();
      var1.put("success", L.t("Benchmark started", new Object[0]));
      return var1.toString();
   }

   private String onSaveConditions(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "file")[0];
         String var4 = this.tryGetParam(var1, "xml")[0];
         if (!var3.toLowerCase().endsWith("xml")) {
            var3 = var3 + ".xml";
         }

         SQUtils.stringToFile(var3, var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot save filters.", new Object[0]), var5);
      }

      var2.put("success", L.t("Filters saved.", new Object[0]));
      return var2.toString();
   }

   private String onLoadConditions(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "file")[0];
         var2.put("xml", SQUtils.fileToString(var3));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot load filters.", new Object[0]), var4);
      }

      var2.put("success", L.t("Filters loaded.", new Object[0]));
      return var2.toString();
   }

   private String onStopStrategiesSaving() {
      JSONObject var1 = new JSONObject();
      StrategiesSaver.stop();
      var1.put("success", L.t("Saving stopped.", new Object[0]));
      return var1.toString();
   }

   private void preloadRequests() {
      if (!MainApp.runInConsole() && !MainApp.checkProduct("SQEDITOR")) {
         (new Thread() {
            @Override
            public void run() {
               try {
                  MainServlet.this.onGetConfigs();
               } catch (Exception var2) {
                  MainServlet.Log.error("Cannot load tutorials from web.", var2);
               }
            }
         }).start();
      }
   }
}
