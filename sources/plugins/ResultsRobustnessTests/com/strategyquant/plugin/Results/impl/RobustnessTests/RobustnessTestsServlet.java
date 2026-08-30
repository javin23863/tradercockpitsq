package com.strategyquant.plugin.Results.impl.RobustnessTests;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.plugin.Results.impl.RobustnessTests.views.RTViews;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.MonteCarloCrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobustnessTestsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(RobustnessTestsServlet.class);
   private static final String LOCK_ROBTESTSERVLET = "RobustnessTestsServlet";
   private RTChart rtChart = new RTChart();
   private ConfidenceLevels confidenceLevels = new ConfidenceLevels();
   private RiskOfRuins riskOfRuins = new RiskOfRuins();
   private RTViews rtViews = new RTViews("rtView", MainApp.getDataPath() + "/user/settings/views/robustnessTests");
   private RTViews rtRRViews = new RTViews("rtRRView", MainApp.getDataPath() + "/user/settings/views/robustnessTestsRR");
   private static IResultsGroupProvider rgProvider;

   public RobustnessTestsServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      if (var1.startsWith("views")) {
         String var4 = ((String[])var2.get("type"))[0].toString();
         if (var4.equals("rt")) {
            return this.rtViews.execute(var1, var2, var3);
         }

         if (var4.equals("rtRR")) {
            return this.rtRRViews.execute(var1, var2, var3);
         }
      }

      switch (var1) {
         case "printChart":
            return this.onPrintChart(var2);
         case "printConfLevels":
            return this.onPrintConfLevels(var2);
         case "printRiskOfRuinStats":
            return this.onPrintRiskOfRuinStats(var2);
         case "getMethods":
            return this.onGetMethods(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetMethods(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      boolean var3 = false;

      try {
         var3 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var12) {
      }

      JSONArray var4 = new JSONArray();
      ResultsGroup var5 = rgProvider.get(var1, "RobustnessTestsServlet");

      try {
         Result var6 = var5.mainResult();

         for (ICrossCheck var8 : SQPluginManager.getPlugins(ICrossCheck.class)) {
            if (MonteCarloCrossCheckMethod.class.isAssignableFrom(var8.getClass()) && var6.getInt(var8.getSettingName() + "_NumberOfSimulations") > 0) {
               var4.put(new JSONObject().put("value", var8.getSettingName()).put("name", var8.getName()));
            }
         }
      } finally {
         var5.releaseLock("RobustnessTestsServlet");
      }

      var2.put("methods", var4);
      var2.put("success", L.t("Methods listed.", new Object[0]));
      return var2.toString();
   }

   private String onPrintChart(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"width", "height"});
      boolean var2 = false;

      try {
         var2 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var16) {
      }

      ResultsGroup var3 = null;
      String var4 = ((String[])var1.get("methodName"))[0].toString();
      int var5 = Integer.parseInt(((String[])var1.get("width"))[0]);
      int var6 = Integer.parseInt(((String[])var1.get("height"))[0]);
      if (var5 >= 1 && var6 >= 1) {
         boolean var7 = false;
         if (var2) {
            var3 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{SQPaths.algoWizardStrategyPath});
         } else {
            this.checkParamExists(var1, new String[]{"project", "databank", "strategy"});
            String var8 = ((String[])var1.get("project"))[0].toString();
            String var9 = ((String[])var1.get("databank"))[0].toString();
            String var10 = ((String[])var1.get("strategy"))[0].toString();
            Databank var11 = (Databank)ProjectEngine.get(var8).getDatabanks().get(var9);
            var3 = var11.getLocked(var10, "RobustnessTestsServlet");
            var7 = true;
         }

         try {
            String var19 = var3.getMainResultKey();
            Result var20 = var3.subResult(var19);
            JSONObject var21 = new JSONObject();
            JSONObject var22 = this.rtChart.print(var20, var4, var5, var6);
            var21.put("chart", var22);
            var21.put("success", "ok");
            return var21.toString();
         } finally {
            if (var7 && var3 != null) {
               var3.releaseLock("RobustnessTestsServlet");
            }
         }
      } else {
         throw new Exception("Chart size is too small.");
      }
   }

   private String onPrintConfLevels(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"width", "height", "view"});
      boolean var2 = false;

      try {
         var2 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var23) {
      }

      ResultsGroup var3 = null;
      boolean var4 = false;
      if (var2) {
         var3 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{SQPaths.algoWizardStrategyPath});
      } else {
         this.checkParamExists(var1, new String[]{"project", "databank", "strategy"});
         String var5 = ((String[])var1.get("project"))[0].toString();
         String var6 = ((String[])var1.get("databank"))[0].toString();
         String var7 = ((String[])var1.get("strategy"))[0].toString();
         Databank var8 = (Databank)ProjectEngine.get(var5).getDatabanks().get(var6);
         var3 = var8.getLocked(var7, "RobustnessTestsServlet");
         var4 = true;
      }

      try {
         String var26 = ((String[])var1.get("methodName"))[0].toString();
         String var27 = ((String[])var1.get("view"))[0].toString();
         String var28 = var3.getMainResultKey();
         Result var29 = var3.subResult(var28);
         JSONObject var9 = new JSONObject();
         DatabankTableView var10 = this.rtViews.getViewByName(var27);
         MainApp.settings().set("rtView", var27);
         int var11 = var29.getInt(var26 + "_NumberOfSimulations");
         String var12 = var29.getString(var26 + "_Symbol");
         String var13 = var29.getString(var26 + "_TimeFrame");
         String var14 = var29.getString(var26 + "_DateRange");
         JSONObject var15 = new JSONObject();
         if (var12 != null) {
            var15.put("data", var12 + " / " + var13 + ", " + var14);
         }

         var15.put("simulations", var11);
         JSONArray var16 = new JSONArray();
         var15.put("methods", var16);
         String[] var17 = (String[])var29.get(var26 + "_Methods");
         if (var17 != null) {
            for (int var18 = 0; var18 < var17.length; var18++) {
               var16.put(var17[var18]);
            }
         }

         JSONArray var30 = this.confidenceLevels.print(var3, var29, var26, var10);
         var9.put("settings", var15);
         var9.put("rows", var30);
         var9.put("success", "ok");
         return var9.toString();
      } finally {
         if (var4 && var3 != null) {
            var3.releaseLock("RobustnessTestsServlet");
         }
      }
   }

   private String onPrintRiskOfRuinStats(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"level", "view"});
      boolean var2 = false;

      try {
         var2 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var16) {
      }

      ResultsGroup var3 = null;
      boolean var4 = false;
      if (var2) {
         var3 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{SQPaths.algoWizardStrategyPath});
      } else {
         this.checkParamExists(var1, new String[]{"project", "databank", "strategy"});
         String var5 = ((String[])var1.get("project"))[0].toString();
         String var6 = ((String[])var1.get("databank"))[0].toString();
         String var7 = ((String[])var1.get("strategy"))[0].toString();
         Databank var8 = (Databank)ProjectEngine.get(var5).getDatabanks().get(var6);
         var3 = var8.getLocked(var7, "RobustnessTestsServlet");
         var4 = true;
      }

      String var19 = ((String[])var1.get("methodName"))[0].toString();
      String var20 = ((String[])var1.get("view"))[0].toString();

      try {
         int var21 = Integer.parseInt(((String[])var1.get("level"))[0]);
         String var22 = var3.getMainResultKey();
         Result var9 = var3.subResult(var22);
         JSONObject var10 = new JSONObject();
         JSONArray var11 = this.riskOfRuins.print(var9, var19, var21, this.rtRRViews.getViewByName(var20));
         var10.put("rows", var11);
         var10.put("success", "ok");
         return var10.toString();
      } finally {
         if (var4 && var3 != null) {
            var3.releaseLock("RobustnessTestsServlet");
         }
      }
   }
}
