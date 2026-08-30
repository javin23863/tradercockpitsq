package com.strategyquant.plugin.Results.impl.StrategyConfig;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtest.AutoRetestStrategyConfigModifier;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.strategyConfig.StrategyConfig;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Arrays;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyConfigServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(StrategyConfigServlet.class);
   private static final String LOCK_STRCONFIGSERVLET = "StrategyConfigServlet";
   private static IResultsGroupProvider rgProvider;
   private StrategyConfig strategyConfig = new StrategyConfig();

   public StrategyConfigServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "loadStrategySettings":
            return this.onLoadStrategySettings(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onLoadStrategySettings(Map<String, String[]> var1) throws Exception {
      boolean var2 = false;

      try {
         var2 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var13) {
      }

      ResultsGroup var3 = rgProvider.get(var1, "StrategyConfigServlet");
      JSONObject var4 = new JSONObject();
      String var5 = null;

      try {
         var5 = var3.getLastSettings();
      } finally {
         var3.releaseLock("StrategyConfigServlet");
      }

      if (var5 == null) {
         throw new Exception("Strategy doesn't contain any settings.");
      }

      Element var6 = XMLUtil.stringToElement(var5);
      Element var7 = new Element("Task").addContent(var6.clone());
      String var8 = XMLUtil.elementToString(var7);
      Element var9 = ProjectResources.checkTaskResources(var6);
      if (var9 != null) {
         var4.put("resourcesXML", XMLUtil.elementToString(var9));
      }

      var4.put("xml", var8);
      var4.put("success", "ok");
      return var4.toString();
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "databankName", "strategyName", "taskType", "taskName"});
      String var2 = ((String[])var1.get("projectName"))[0].toString();
      String var3 = ((String[])var1.get("taskType"))[0].toString();
      String var4 = ((String[])var1.get("taskName"))[0].toString();
      JSONObject var5 = new JSONObject();
      JSONArray var6 = new JSONArray();
      Element var7 = null;
      String var8 = null;
      ResultsGroup var9 = null;

      try {
         var9 = rgProvider.get(var1, "StrategyConfigServlet");
         var8 = var9.getLastSettings();
      } catch (RecordNotFoundException var18) {
         Log.info("onPrint: " + var18.getMessage());
         return apiErrorJSONNoLog(null, var18);
      } catch (Exception var19) {
         return apiErrorJSON(L.t("Getting strategy config failed", new Object[0]), var19);
      } finally {
         if (var9 != null) {
            var9.releaseLock("StrategyConfigServlet");
         }
      }

      if (!var2.equals("AlgoWizard")) {
         SQProject var10 = ProjectEngine.get(var2);
         var7 = var2.equals("AlgoWizard") ? null : var10.getTaskSettingsByName(var4);
         if (var3.equals("AutomaticRetest")) {
            Element var11 = XMLUtil.stringToElement(var8);
            AutoRetestStrategyConfigModifier.modifySettings(var11, var7);
            var7 = var11;
            var3 = "Retest";
         }
      } else {
         var3 = "Retest";

         try {
            var7 = XMLUtil.stringToElement(((String[])var1.get("settingsXML"))[0].toString());
         } catch (Exception var17) {
            var7 = null;
         }
      }

      if (var8 != null) {
         ISQTask var23 = ProjectConfigHelper.createTask(var3, var4, var2);
         Element var24 = XMLUtil.stringToElement(var8);
         String[] var12 = var23.getSettings();
         var6 = this.strategyConfig.print(Arrays.asList(var12), var24, var7);
      }

      var5.put("settings", var6);
      var5.put("success", "ok");
      return var5.toString();
   }
}
