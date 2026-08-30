package com.strategyquant.plugin.CodeEditor.impl.IndicatorTester;

import com.strategyquant.indicatorTester.CalibrationInfo;
import com.strategyquant.indicatorTester.CalibrationRangesInfo;
import com.strategyquant.indicatorTester.IndicatorCalibrator;
import com.strategyquant.indicatorTester.IndicatorInfo;
import com.strategyquant.indicatorTester.IndicatorTesterConfig;
import com.strategyquant.indicatorTester.IndicatorsLoader;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorTesterServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(IndicatorTesterServlet.class);
   private static IndicatorTesterServlet instance;

   public IndicatorTesterServlet() {
      instance = this;
   }

   public static IndicatorTesterServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "listIndicators":
            return this.onListIndicators();
         case "createNewConfig":
            return this.onCreateNewConfig(var2);
         case "addTests":
            return this.onAddTests(var2);
         case "loadConfig":
            return this.onLoadConfig(var2);
         case "saveConfig":
            return this.onSaveConfig(var2);
         case "startTesting":
            return this.onStartTesting(var2);
         case "stopTesting":
            return this.onStopTesting();
         case "downloadTests":
            return this.onDownloadTests();
         case "calibrate":
            return this.onCalibrate(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onListIndicators() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (IndicatorInfo var4 : IndicatorsLoader.getIndicators()) {
         for (JSONObject var6 : var4.getJSONList()) {
            var2.put(var6);
         }
      }

      var1.put("indicators", var2);
      var1.put("success", L.t("Indicators listed.", new Object[0]));
      return var1.toString();
   }

   private String onCreateNewConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "engine")[0];
         String var4 = this.tryGetParam(var1, "barsToReserve")[0];
         IndicatorTesterConfig.resetConfigPath();
         var2.put("xmlConfig", XMLUtil.xmlToStringRaw(IndicatorTesterConfig.createDefaultConfig(var3, var4)));
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Adding tests failed.", new Object[0]), var5);
      }

      var2.put("success", L.t("Tests added.", new Object[0]));
      return var2.toString();
   }

   private String onAddTests(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "indicators")[0].split(",");
         String var4 = this.tryGetParam(var1, "xmlConfig")[0];
         IndicatorTesterConfig.updateConfig(XMLUtil.stringToElement(var4));
         var2.put("xmlConfig", XMLUtil.xmlToStringRaw(IndicatorTesterConfig.createNewConfig(var3)));
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Adding tests failed.", new Object[0]), var5);
      }

      var2.put("success", L.t("Tests added.", new Object[0]));
      return var2.toString();
   }

   private String onLoadConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         var3 = this.tryGetParam(var1, "filePath")[0];
      } catch (Exception var5) {
      }

      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(IndicatorTesterConfig.load(var3)));
      var2.put("success", L.t("Config loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSaveConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "xmlConfig")[0];
         String var4 = null;

         try {
            var4 = this.tryGetParam(var1, "filePath")[0];
         } catch (Exception var6) {
         }

         IndicatorTesterConfig.save(XMLUtil.stringToElement(var3), var4);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Adding tests failed.", new Object[0]), var7);
      }

      var2.put("success", L.t("Config saved.", new Object[0]));
      return var2.toString();
   }

   private String onStartTesting(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "xmlConfig")[0];
         IndicatorTesterConfig.updateConfig(XMLUtil.stringToElement(var3));
         IndicatorTestExecutor.get().start();
      } catch (Exception var4) {
         return apiErrorJSON(var4.getMessage(), null);
      }

      var2.put("success", L.t("Config loaded.", new Object[0]));
      return var2.toString();
   }

   private String onStopTesting() {
      JSONObject var1 = new JSONObject();
      IndicatorTestExecutor.get().stop();
      var1.put("success", L.t("Config loaded.", new Object[0]));
      return var1.toString();
   }

   private String onDownloadTests() {
      JSONObject var1 = new JSONObject();

      try {
         if (IndicatorTestExecutor.get().getRunningStatus() == 1) {
            throw new Exception("Indicator tests are running.<br>Please wait until the tests finish or stop the execution and try again.");
         }

         IndicatorTestsDownloader.downloadTests(DownloadProgressPublisher.get());
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot start indicator tests download.", new Object[0]), var3);
      }

      var1.put("success", L.t("Download started.", new Object[0]));
      return var1.toString();
   }

   private String onCalibrate(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "projectName");
         String var4 = null;

         try {
            var4 = this.tryGetParamValue(var1, "taskName");
         } catch (Exception var18) {
         }

         String var5 = this.tryGetParamValue(var1, "symbols");
         String var6 = this.tryGetParamValue(var1, "timeframes");
         String[] var7 = var5.split(",");
         String[] var8 = var6.split(",");
         int var9 = Engines.getEngine(this.tryGetParamValue(var1, "engine"));
         if (var9 == 1316847364 || var9 == -1816889229) {
            throw new Exception(L.t("Indicator calibration is not supported for AlgoCloud Stockpicker / Single-asset engine.", new Object[0]));
         }

         try {
            CalibrationRangesInfo.maxSteps = Integer.parseInt(this.tryGetParamValue(var1, "maxSteps"));
         } catch (Exception var17) {
            CalibrationRangesInfo.maxSteps = -1;
         }

         String var10 = var4 != null
            ? L.t("Starting indicators calibration for task %s...", new Object[]{var4})
            : L.t("Starting indicators calibration...", new Object[0]);
         ProgressEngine var11 = ProjectEngine.get(var3).progressEngine;
         var11.printToLog(var10);
         IndicatorCalibrator var12 = new IndicatorCalibrator();
         ArrayList var13 = var12.calibrate(var7, var8, var9);
         JSONArray var14 = new JSONArray();

         for (int var15 = 0; var15 < var13.size(); var15++) {
            CalibrationInfo var16 = (CalibrationInfo)var13.get(var15);
            var14.put(var16.toJSON());
         }

         var11.printToLog(L.t("Indicators calibration done", new Object[0]));
         var2.put("calibrationResults", var14);
      } catch (Exception var19) {
         return apiErrorJSON(L.t("Cannot start calibration", new Object[0]), var19);
      }

      var2.put("success", L.t("Calibration finished.", new Object[0]));
      return var2.toString();
   }
}
