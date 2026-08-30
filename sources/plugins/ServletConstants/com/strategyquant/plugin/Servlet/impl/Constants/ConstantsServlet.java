package com.strategyquant.plugin.Servlet.impl.Constants;

import com.strategyquant.datalib.data.imports.Separators;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.datalib.timezone.Timezones;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.DataSources;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.DatabankSyncTypes;
import com.strategyquant.tradinglib.GCTypes;
import com.strategyquant.tradinglib.PortfolioInitialBalanceTypes;
import com.strategyquant.tradinglib.PrecachedRequest;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.SwapTypes;
import com.strategyquant.tradinglib.TripleSwapOptions;
import com.strategyquant.tradinglib.atm.ATMMoveSL2BETypes;
import com.strategyquant.tradinglib.atm.ATMTypes;
import com.strategyquant.tradinglib.atm.exits.ATMExitLevels;
import com.strategyquant.tradinglib.atm.sizes.ATMSizes;
import com.strategyquant.tradinglib.commissions.CommissionsMethodsList;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankColumnTypes;
import com.strategyquant.tradinglib.exception.StrategyProblem;
import com.strategyquant.tradinglib.optimization.OptimizationConst;
import com.strategyquant.tradinglib.performance.Performance;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.project.ProjectConditions;
import com.strategyquant.tradinglib.project.websocket.DataUpdateTypes;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.project.websocket.WebSocketConst;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.strategy.SimpleStrategyTypes;
import com.strategyquant.tradinglib.task.settings.buildmode.GenerationTypes;
import com.strategyquant.tradinglib.task.settings.buildmode.StopConditionTypes;
import com.strategyquant.tradinglib.task.settings.buildtype.ImproveTypes;
import com.strategyquant.tradinglib.task.settings.buildtype.StrategyArchitectures;
import com.strategyquant.tradinglib.task.settings.buildtype.StrategyDirections;
import com.strategyquant.tradinglib.task.settings.buildtype.StrategyTypes;
import com.strategyquant.tradinglib.task.settings.partstoimprove.ImproveActionTypes;
import com.strategyquant.tradinglib.tasks.HelpTexts;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ConstantsServlet.class);
   private static ConstantsServlet instance;
   private String getAllResponse = null;

   public ConstantsServlet() {
      instance = this;
   }

   public static ConstantsServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "getAll":
            return this.onGetAll();
         case "listCommissionMethods":
            return this.onListCommissionMethods();
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onGetAll() {
      JSONObject var1 = new JSONObject();
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONObject var4 = new JSONObject().put("name", "Selected timeframe only (fastest)").put("value", 1).put("minTF", "X");
      JSONObject var5 = new JSONObject().put("name", "1 minute data tick simulation (slow)").put("value", 2).put("minTF", "M1");
      JSONObject var6 = new JSONObject().put("name", "Real Tick - custom spread (slowest)").put("value", 3).put("minTF", "TICK");
      JSONObject var7 = new JSONObject().put("name", "Real Tick - real spread (slowest)").put("value", 4).put("minTF", "TICK");
      var3.put(var4).put(var5).put(var6).put(var7);
      var2.put("precisions", var3);
      JSONArray var8 = new JSONArray();
      var8.put(new JSONObject().put("name", "Stock").put("value", 1));
      var8.put(new JSONObject().put("name", "Futures").put("value", 2));
      var8.put(new JSONObject().put("name", "Forex").put("value", 3));
      var8.put(new JSONObject().put("name", "CFDs").put("value", 4));
      var8.put(new JSONObject().put("name", "ETF").put("value", 5));
      var8.put(new JSONObject().put("name", "Index").put("value", 6));
      var8.put(new JSONObject().put("name", "Crypto").put("value", 7));
      var8.put(new JSONObject().put("name", "Bond").put("value", 8));
      var2.put("dataTypes", var8);
      JSONObject var9 = new JSONObject();
      var9.put("file", 1);
      var9.put("dukascopy", 2);
      var9.put("darwinex", 4);
      var9.put("sqequity", 5);
      var9.put("sqfutures", 6);
      var9.put("crypto", 7);
      var9.put("yahoo", 3);
      var9.put("mt5api", 8);
      var2.put("dataSource", var9);
      JSONArray var10 = new JSONArray();
      var10.put(new JSONObject().put("name", DataSources.NAME_FILE).put("value", 1));
      var10.put(new JSONObject().put("name", DataSources.NAME_DUKASCOPY).put("value", 2));
      var10.put(new JSONObject().put("name", DataSources.NAME_Yahoo).put("value", 3));
      var10.put(new JSONObject().put("name", DataSources.NAME_DARWINEX).put("value", 4));
      var10.put(new JSONObject().put("name", DataSources.NAME_Cryptocurrency).put("value", 7));
      if (MainApp.checkProduct("SQUANT")) {
         var10.put(new JSONObject().put("name", DataSources.NAME_SQEquityData).put("value", 5));
         var10.put(new JSONObject().put("name", DataSources.NAME_SQFuturesData).put("value", 6));
      }

      var10.put(new JSONObject().put("name", DataSources.NAME_MT5_API).put("value", 8));
      var2.put("dataSources", var10);
      JSONObject var11 = new JSONObject()
         .put("sinceLastDate", "sinceLastDate")
         .put("lastYear", "lastYear")
         .put("last6Months", "last6Months")
         .put("allTime", "allTime");
      var2.put("dataRanges", var11);
      JSONObject var12 = new JSONObject();
      var12.put("startOfBar", 1);
      var12.put("endOfBar", 2);
      var2.put("barDataTypes", var12);
      JSONObject var13 = new JSONObject();
      var13.put("both", 0);
      var13.put("long", 1);
      var13.put("short", -1);
      var2.put("directions", var13);
      JSONObject var14 = new JSONObject();
      var14.put("full", 127);
      var14.put("in", 10);
      var14.put("ist", 11);
      var14.put("isv", 40);
      var14.put("isvEvery", 66);
      var14.put("isv1", 41);
      var14.put("isv2", 42);
      var14.put("isv3", 43);
      var14.put("isv4", 44);
      var14.put("isv5", 45);
      var14.put("isv6", 46);
      var14.put("isv7", 47);
      var14.put("isv8", 48);
      var14.put("isv9", 49);
      var14.put("isv10", 50);
      var14.put("out", 20);
      var14.put("oosEvery", 77);
      var14.put("oos1", 21);
      var14.put("oos2", 22);
      var14.put("oos3", 23);
      var14.put("oos4", 24);
      var14.put("oos5", 25);
      var14.put("oos6", 26);
      var14.put("oos7", 27);
      var14.put("oos8", 28);
      var14.put("oos9", 29);
      var14.put("oos10", 30);
      var14.put("notrade", 99);
      var2.put("sampleTypes", var14);
      JSONArray var15 = new JSONArray();
      var15.put(new JSONObject().put("value", 127).put("name", SampleTypes.typeToString((byte)127)));
      var15.put(new JSONObject().put("value", 10).put("name", SampleTypes.typeToString((byte)10)));
      var15.put(new JSONObject().put("value", 11).put("name", SampleTypes.typeToString((byte)11)));
      var15.put(new JSONObject().put("value", 40).put("name", SampleTypes.typeToString((byte)40)));
      var15.put(new JSONObject().put("value", 66).put("name", SampleTypes.typeToString((byte)66)));
      var15.put(new JSONObject().put("value", 41).put("name", SampleTypes.typeToString((byte)41)));
      var15.put(new JSONObject().put("value", 42).put("name", SampleTypes.typeToString((byte)42)));
      var15.put(new JSONObject().put("value", 43).put("name", SampleTypes.typeToString((byte)43)));
      var15.put(new JSONObject().put("value", 44).put("name", SampleTypes.typeToString((byte)44)));
      var15.put(new JSONObject().put("value", 45).put("name", SampleTypes.typeToString((byte)45)));
      var15.put(new JSONObject().put("value", 46).put("name", SampleTypes.typeToString((byte)46)));
      var15.put(new JSONObject().put("value", 47).put("name", SampleTypes.typeToString((byte)47)));
      var15.put(new JSONObject().put("value", 48).put("name", SampleTypes.typeToString((byte)48)));
      var15.put(new JSONObject().put("value", 49).put("name", SampleTypes.typeToString((byte)49)));
      var15.put(new JSONObject().put("value", 50).put("name", SampleTypes.typeToString((byte)50)));
      var15.put(new JSONObject().put("value", 20).put("name", SampleTypes.typeToString((byte)20)));
      var15.put(new JSONObject().put("value", 77).put("name", SampleTypes.typeToString((byte)77)));
      var15.put(new JSONObject().put("value", 21).put("name", SampleTypes.typeToString((byte)21)));
      var15.put(new JSONObject().put("value", 22).put("name", SampleTypes.typeToString((byte)22)));
      var15.put(new JSONObject().put("value", 23).put("name", SampleTypes.typeToString((byte)23)));
      var15.put(new JSONObject().put("value", 24).put("name", SampleTypes.typeToString((byte)24)));
      var15.put(new JSONObject().put("value", 25).put("name", SampleTypes.typeToString((byte)25)));
      var15.put(new JSONObject().put("value", 26).put("name", SampleTypes.typeToString((byte)26)));
      var15.put(new JSONObject().put("value", 27).put("name", SampleTypes.typeToString((byte)27)));
      var15.put(new JSONObject().put("value", 28).put("name", SampleTypes.typeToString((byte)28)));
      var15.put(new JSONObject().put("value", 29).put("name", SampleTypes.typeToString((byte)29)));
      var15.put(new JSONObject().put("value", 30).put("name", SampleTypes.typeToString((byte)30)));
      var2.put("sampleTypeList", var15);
      JSONObject var16 = new JSONObject();
      var16.put("money", 10);
      var16.put("percent", 20);
      var16.put("pips", 30);
      var16.put("ticks", 30);
      var16.put("openMoney", 40);
      var16.put("openPercent", 50);
      var2.put("plTypes", var16);
      JSONObject var17 = new JSONObject();
      var17.put("int", 1);
      var17.put("double", 2);
      var17.put("str", 30);
      var17.put("boolean", 3);
      var17.put("date", 20);
      var17.put("time", 10);
      var17.put("intList", 4);
      var17.put("strList", 5);
      var2.put("propGridTypes", var17);
      JSONObject var18 = new JSONObject();
      var18.put("main", "main");
      var18.put("portfolio", "portfolio");
      var2.put("resultTypes", var18);
      JSONArray var19 = new JSONArray();
      var19.put(new JSONObject().put("name", L.tsq("Main data")).put("value", "main"));
      var19.put(new JSONObject().put("name", L.tsq("Portfolio")).put("value", "portfolio"));

      for (ICrossCheck var21 : SQPluginManager.getPlugins(ICrossCheck.class)) {
         var19.put(
            new JSONObject()
               .put("name", var21.getName())
               .put("value", var21.getSettingName())
               .put("crosscheck", true)
               .put("title", var21.getColumnTitleTemplate())
               .put(
                  "walkforward",
                  WalkForwardCrossCheckMethod.class.isAssignableFrom(var21.getClass()) && !var21.getSettingName().equals("OptProfileSysParamPermutation")
               )
         );
      }

      var2.put("resultTypesList", var19);
      JSONArray var61 = new JSONArray();
      var61.put(new JSONObject().put("name", "WF").put("value", 30));
      var61.put(new JSONObject().put("name", "WF Stability of").put("value", 31));
      var61.put(new JSONObject().put("name", "WF Score of").put("value", 32));
      var61.put(new JSONObject().put("name", "WF Special - ").put("value", 33));
      var2.put("wfSubresultsList", var61);
      JSONObject var62 = new JSONObject();
      var62.put("beforeStart", 0);
      var62.put("finished", 3);
      var62.put("paused", 2);
      var62.put("pausing", 5);
      var62.put("running", 1);
      var62.put("stopped", 4);
      var62.put("stopping", 6);
      var62.put("error", 50);
      var62.put("loading", 100);
      var2.put("runningStatuses", var62);
      JSONArray var22 = new JSONArray();

      for (String var24 : HelpTexts.getAll().keySet()) {
         JSONObject var25 = new JSONObject();
         var25.put("tabName", var24);
         var25.put("helpText", HelpTexts.get(var24));
         var22.put(var25);
      }

      var2.put("helpTexts", var22);
      var2.put("dataUpdateTypes", new DataUpdateTypes().toJSON());
      JSONArray var63 = WSDataObjects.getTimeframes().getDataArray();
      var2.put("timeframes", var63);
      JSONObject var64 = new JSONObject();
      var64.put("Intraday", "Intraday");
      var64.put("TICK", "TICK");
      var64.put("M1", "M1");
      var64.put("M5", "M5");
      var64.put("M15", "M15");
      var64.put("M30", "M30");
      var64.put("H1", "H1");
      var64.put("H4", "H4");
      var64.put("D1", "D1");
      var64.put("Weekly", "Weekly");
      var64.put("Monthly", "Monthly");
      var2.put("timeframe", var64);
      JSONArray var65 = new JSONArray();
      var2.put("databankProperties", var65);
      JSONObject var26 = new JSONObject();
      var2.put("optimization", var26);
      JSONObject var27 = new JSONObject();
      var27.put("simple", 1);
      var27.put("sequential", 4);
      var27.put("walk_fo", 2);
      var27.put("walk_fm", 3);
      var26.put("optimizations", var27);
      JSONObject var28 = new JSONObject();
      var28.put("file", 1);
      var28.put("databank", 2);
      var26.put("sources", var28);
      JSONObject var29 = new JSONObject();
      var29.put("percent", 10);
      var29.put("days", 20);
      var29.put("bars", 30);
      var26.put("periodTypes", var29);
      JSONObject var30 = new JSONObject();
      var30.put("floating", 15);
      var30.put("fixed", 25);
      var26.put("optimizationTypes", var30);
      JSONObject var31 = new JSONObject();
      var31.put(OptimizationConst.wfTypeToString(0), 0);
      var31.put(OptimizationConst.wfTypeToString(1), 1);
      var31.put(OptimizationConst.wfTypeToString(2), 2);
      var26.put("wfTypes", var31);
      JSONObject var32 = new JSONObject();
      var32.put("WF_TYPE_SIMIS_SIMOOS", 0);
      var32.put("WF_TYPE_SIMIS_EXACTOOS", 1);
      var32.put("WF_TYPE_EXACTIS_EXACTOOS", 2);
      var26.put("wfType", var32);
      JSONObject var33 = new JSONObject();
      var33.put("storeAll", 5);
      var33.put("storeBest", 10);
      var26.put("simpleOptimizations", var33);
      var2.put("engines", Engines.toJSON());
      var2.put("engineTypes", Engines.list());
      var2.put("engineKeys", Engines.toJSONKeys());
      JSONObject var34 = new JSONObject();
      var34.put("results", "Results");
      var34.put("initialPopulation", "Initial population");
      var34.put("lastGeneration", "Last generation");
      var34.put("strategiesToImprove", "Strategies to improve");
      var34.put("strategiesToOptimize", "Strategies to optimize");
      var34.put("existingPortfolio", "Existing portfolio");
      var34.put("simpleStrategies", "Simple strategies");
      var2.put("gedatabanks", var34);
      var2.put("taskConfigs", WSDataObjects.getTaskTemplates().getDataArray());
      JSONObject var35 = new JSONObject();
      var35.put("options", Performance.getCoreUsageOptions());
      var35.put("cores", Performance.totalCores);
      String var36 = MainApp.settings().get("coreUsage", "-1");
      var35.put("coreUsage", var36.startsWith("X") ? "X" : var36);
      var35.put("customCores", var36.startsWith("X") ? var36.replaceAll("\\D+", "") : Performance.totalCores);
      var2.put("performance", var35);
      JSONObject var37 = new JSONObject();
      var37.put("soundsoff", Boolean.parseBoolean(MainApp.settings().get("globalSoundsOff", "true")));
      var37.put("showControlOrders", Boolean.parseBoolean(MainApp.settings().get("showControlOrders", "false")));
      var37.put("headerCustomText", MainApp.settings().get("headerCustomText", ""));
      var37.put("footerCustomText", MainApp.settings().get("footerCustomText", ""));
      var37.put("autoUpdate", Boolean.parseBoolean(MainApp.settings().get("autoUpdate", "true")));
      var37.put("computePipsMetrics", Boolean.parseBoolean(MainApp.settings().get("ComputePipsMetrics", "false")));
      var37.put("computePctsMetrics", Boolean.parseBoolean(MainApp.settings().get("ComputePctsMetrics", "false")));
      var37.put("computeSeparateMetrics", Boolean.parseBoolean(MainApp.settings().get("ComputeSeparateMetrics", "true")));
      var37.put("defaultResultToDisplay", MainApp.settings().get("defaultResultToDisplay", "portfolio"));
      var2.put("global", var37);
      var2.put("buildStrategyTypes", new StrategyTypes().toJSON());
      var2.put("buildImproveTypes", new ImproveTypes().toJSON());
      var2.put("buildStrategyDirections", new StrategyDirections().toJSON());
      var2.put("buildGenerationTypes", new GenerationTypes().toJSON());
      var2.put("buildStopConditionTypes", new StopConditionTypes().toJSON());
      var2.put("improveActionTypes", new ImproveActionTypes().toJSON());
      var2.put("strategyArchitectures", new StrategyArchitectures().toJSON());
      new DatabankColumnTypes();
      var2.put("databankColumnTypes", DatabankColumnTypes.toJSONObject());
      JSONObject var38 = new JSONObject();
      var38.put("Decimal2", "Decimal2");
      var38.put("Decimal2Pct", "Decimal2Pct");
      var38.put("Decimal2Pips", "Decimal2Pips");
      var38.put("Decimal2PL", "Decimal2PL");
      var38.put("Decimal4", "Decimal4");
      var38.put("Decimal5", "Decimal5");
      var38.put("Integer", "Integer");
      var38.put("Text", "Text");
      var2.put("columnValueTypes", var38);
      var2.put("WSConst", new WebSocketConst().toJSON());
      JSONObject var39 = new JSONObject();
      var39.put("progress", "progress-channel");
      var39.put("databanks", "databanks-channel");
      var39.put("engine", "engine-channel");
      var39.put("engineCharts", "engine-charts-channel");
      var39.put("log", "log-channel");
      var39.put("bestResults", "best-results-channel");
      var2.put("channels", var39);
      JSONArray var40 = new JSONArray();

      for (StrategyProblem var42 : BadStrategyException.listProblems()) {
         var40.put(new JSONObject().put("code", var42.code).put("shortInfo", var42.shortInfo).put("longInfo", var42.longInfo));
      }

      var2.put("strategyProblems", var40);
      var2.put("importFileSeparators", Separators.toJSON());
      var2.put("simpleStrategyTypes", new SimpleStrategyTypes().toJSON());
      JSONArray var66 = new JSONArray();

      try {
         for (Timezone var43 : Timezones.getTimezones()) {
            JSONObject var44 = new JSONObject();
            var44.put("name", var43.getName());
            var44.put("value", var43.getId());
            var66.put(var44);
         }
      } catch (Exception var60) {
         return apiErrorJSON(L.t("Cannot list timezones.", new Object[0]), var60);
      }

      var2.put("timezones", var66);
      JSONObject var68 = new JSONObject();
      var68.put("add", "add");
      var68.put("remove", "remove");
      var68.put("update", "update");
      var68.put("reset", "reset");
      var2.put("databankChange", var68);
      var2.put("benchmarkTimePerTick", Performance.benchmarkTimePerTick);
      JSONObject var69 = new JSONObject();
      var69.put("portfolioOnly", "Portfolio only");
      var69.put("portfolioPartsOnly", "Portfolio parts only");
      var2.put("equityChart", var69);
      var2.put("databankSyncTypes", new DatabankSyncTypes().toJSON());
      JSONArray var70 = new JSONArray();
      var70.put(new JSONObject().put("name", L.tsq("never")).put("value", 0));
      var70.put(new JSONObject().put("name", L.tsq("every 10 seconds")).put("value", 10000));
      var70.put(new JSONObject().put("name", L.tsq("every 20 seconds")).put("value", 20000));
      var70.put(new JSONObject().put("name", L.tsq("every 30 seconds")).put("value", 30000));
      var70.put(new JSONObject().put("name", L.tsq("every 45 seconds")).put("value", 40000));
      var70.put(new JSONObject().put("name", L.tsq("every minute")).put("value", 60000));
      var70.put(new JSONObject().put("name", L.tsq("every 2 minutes")).put("value", 120000));
      var70.put(new JSONObject().put("name", L.tsq("every 5 minutes")).put("value", 300000));
      var70.put(new JSONObject().put("name", L.tsq("every 10 minutes")).put("value", 600000));
      var70.put(new JSONObject().put("name", L.tsq("every 15 minutes")).put("value", 900000));
      var70.put(new JSONObject().put("name", L.tsq("every 30 minutes")).put("value", 1800000));
      var70.put(new JSONObject().put("name", L.tsq("every hour")).put("value", 3600000));
      var2.put("memoryCleanupIntervals", var70);
      JSONObject var45 = new JSONObject();
      var45.put("SQ", "SQUANT");
      var45.put("AW", "AlgoWizard");
      var45.put("QDM", "QDM");
      var2.put("products", var45);
      var2.put("gcTypes", new GCTypes().toJSON());
      AbstractBroker var46 = MainApp.v571hfnsHw().inygRmSMx7();
      if (var46 != null) {
         JSONArray var47 = new JSONArray();
         String[] var48 = var46.getSupportedPlatforms();
         if (var48 != null) {
            for (int var49 = 0; var49 < var48.length; var49++) {
               var47.put(var48[var49]);
            }
         }

         var2.put(
            "broker",
            new JSONObject()
               .put("name", var46.getName())
               .put("encryption", var46.usesEAEncryption())
               .put("platforms", var47)
               .put("bannerFileName", var46.getBannerFileName())
               .put("welcomeTitle", var46.getWelcomeTitle())
               .put("welcomeSubtitle", var46.getWelcomeSubtitle())
               .put("description", var46.getDescription())
               .put("firstTimeLabel", var46.getFirstTimeLabel())
               .put("startButtonTitle", var46.getStartButtonTitle())
               .put("startButtonSubtitle", var46.getStartButtonSubtitle())
         );
      }

      var2.put("projectConditionTypes", ProjectConditions.toJSON());
      JSONObject var71 = new JSONObject();
      var71.put("comparator", "comparator");
      var71.put("databank", "databank");
      var71.put("duration", "duration");
      var71.put("number", "number");
      var2.put("projectConditionFields", var71);
      JSONObject var72 = new JSONObject();
      var72.put("IndicatorValuePrice", 1);
      var72.put("IndicatorValueNumber", 2);
      var72.put("IndicatorValuePriceRange", 3);
      var72.put("SignalValueBoolean", 10);
      var72.put("SignalValueAction", 11);
      var2.put("customDataType", var72);
      JSONArray var73 = new JSONArray();
      var73.put(new JSONObject().put("name", "Indicator value - price").put("value", 1));
      var73.put(new JSONObject().put("name", "Indicator value - number").put("value", 2));
      var73.put(new JSONObject().put("name", "Indicator value - price range").put("value", 3));
      var73.put(new JSONObject().put("name", "Signal - 0 means false, anything else means true").put("value", 10));
      var2.put("customDataTypes", var73);
      var2.put("portfolioInitialBalanceTypes", new PortfolioInitialBalanceTypes().toJSON());
      JSONObject var50 = new JSONObject();
      var50.put("doesntExist", 0);
      var50.put("differs", 1);
      var50.put("notEnoughData", 2);
      var50.put("existsWithDifferentName", 3);
      var2.put("projectResourceStatuses", var50);
      JSONObject var51 = new JSONObject();
      var51.put("maximize", 1);
      var51.put("minimize", 2);
      var51.put("aproximate", 3);
      var2.put("valueTypes", var51);
      JSONObject var52 = new JSONObject();
      var52.put("loadProject", 0);
      var52.put("loadTaskConfig", 1);
      var52.put("applyStrategyConfig", 2);
      var52.put("loadTemplate", 3);
      var52.put("loadStrategyToOptimize", 4);
      var52.put("resolveProjectResources", 5);
      var2.put("projectResourceActionTypes", var52);
      JSONObject var53 = new JSONObject();
      var53.put("main", "Default - Main data");
      var53.put("portfolio", "Default - Portfolio");
      var2.put("defaultDatabankViews", var53);
      var2.put("atmTypes", new ATMTypes().toJSON());
      var2.put("atmPositionSizes", new ATMSizes().toJSON());
      var2.put("atmExitLevels", new ATMExitLevels().toJSON());
      var2.put("atmMoveSL2BETypes", new ATMMoveSL2BETypes().toJSON());
      JSONArray var54 = new JSONArray();

      for (CorrelationType var56 : CorrelationTypes.getInstance().getAvailableClasses()) {
         JSONObject var57 = new JSONObject();
         var57.put("name", var56.getName());
         var57.put("value", var56.getClass().getSimpleName());
         var54.put(var57);
      }

      var2.put("correlationTypes", var54);
      JSONArray var74 = new JSONArray();
      var74.put(new JSONObject().put("name", L.tsq("Hour")).put("value", 5));
      var74.put(new JSONObject().put("name", L.tsq("Day")).put("value", 10));
      var74.put(new JSONObject().put("name", L.tsq("Week")).put("value", 20));
      var74.put(new JSONObject().put("name", L.tsq("Month")).put("value", 30));
      var74.put(new JSONObject().put("name", L.tsq("Year")).put("value", 40));
      var2.put("correlationPeriods", var74);
      var2.put("swapTypes", new SwapTypes().toJSON());
      var2.put("tripleSwapOptions", new TripleSwapOptions().toJSON());
      var2.put("isBrazilianEdition", MainApp.isBrazilianEdition());
      var2.put("isProEdition", MainApp.v571hfnsHw().gDmtOfRJBr());
      var2.put("AWEditorVersion", MainApp.settings().get("AWEditorVersion", "2"));
      var2.put("commissionMethods", this.getCommissionMethodsJSON());
      var2.put("isFirstRun", MainApp.isFirstRun());
      var1.put("constants", var2);
      var1.put("success", L.t("Constants returned.", new Object[0]));
      JSONArray var75 = new JSONArray();
      var75.put(new JSONObject().put("name", L.tsq("SharpeRatio")).put("value", "SharpRatio"));
      var75.put(new JSONObject().put("name", L.tsq("ReturnDrawdownRatio")).put("value", "ReturnDrawdownRatio"));
      var75.put(new JSONObject().put("name", L.tsq("CAGRMaxDrawdownRatio")).put("value", "CAGRMaxDrawdownRatio"));
      var75.put(new JSONObject().put("name", L.tsq("CAGRMeanDrawdownRatio")).put("value", "CAGRMeanDrawdownRatio"));
      var75.put(new JSONObject().put("name", L.tsq("NetProfit")).put("value", "NetProfit"));
      var2.put("pcSelectionTypes", var75);
      var2.put("useHttps", Boolean.parseBoolean(MainApp.settings().get("SSLUse", "false")));
      JSONArray var76 = new JSONArray();

      for (IAppPlugin var59 : SQPluginManager.getPlugins(IAppPlugin.class)) {
         var76.put(
            new JSONObject().put("name", var59.getName()).put("appCode", var59.getAppCode()).put("disabledForSpecialTrial", var59.disabledForSpecialTrial())
         );
      }

      var2.put("appPlugins", var76);
      var2.put("appPath", MainApp.getDataPath());
      return var1.toString();
   }

   @PrecachedRequest(relativeURL = "/constants/listCommissionMethods")
   private String onListCommissionMethods() {
      JSONObject var1 = new JSONObject();
      Object var2 = null;
      if (!MainApp.checkProduct("QDM")) {
         try {
            var2 = this.getCommissionMethodsJSON();
         } catch (Exception var4) {
            return apiErrorJSON(L.t("Cannot get commission methods.", new Object[0]), var4);
         }
      } else {
         var2 = new JSONArray();
      }

      var1.put("methods", var2);
      var1.put("success", L.t("Commission methods listed.", new Object[0]));
      return var1.toString();
   }

   private JSONArray getCommissionMethodsJSON() {
      JSONArray var1 = new JSONArray();

      for (CommissionsMethod var3 : CommissionsMethodsList.get().getAvailableClasses()) {
         JSONObject var4 = new JSONObject();
         var4.put("name", var3.getName());
         var4.put("help", var3.getNote());
         var4.put("class", var3.getClass().getSimpleName());
         var4.put("config", XMLUtil.elementToString(var3.getXML()));
         var4.put("display", var3.getFormatedName());
         var1.put(var4);
      }

      return var1;
   }
}
