package com.strategyquant.plugin.Settings.impl.WhatToBuild;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhatToBuildServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(WhatToBuildServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "listFiles":
            return this.onListFiles();
         case "getTemplateConfig":
            return this.onGetTemplateConfig(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onListFiles() throws Exception {
      JSONObject var1 = new JSONObject();

      try {
         var1.put("strategyFiles", this.loadFilesFromFolder(SQPaths.strategiesDirPath));
         var1.put("strategyTemplateFiles", this.loadFilesFromFolder(SQPaths.strategyTemplatesDirPath));
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot list config files.", new Object[0]), var3);
      }

      var1.put("success", L.t("Config files listed.", new Object[0]));
      return var1.toString();
   }

   private JSONArray loadFilesFromFolder(String var1) {
      JSONArray var2 = new JSONArray();
      File var3 = new File(var1);
      if (var3.exists()) {
         for (File var7 : var3.listFiles()) {
            var2.put(var7.getName());
         }
      } else {
         var3.mkdirs();
      }

      return var2;
   }

   private String onGetTemplateConfig(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      String var4 = this.tryGetParam(var1, "fileName")[0];
      var4 = var4.replaceAll(Pattern.quote("\\"), "/");
      var4 = var4.contains("/") ? var4 : SQPaths.strategyTemplatesDirPath + "/" + var4;
      File var5 = new File(var4);
      if (!var5.exists()) {
         return apiErrorJSON(L.t("Selected template file doesn't exist", new Object[0]), null);
      }

      boolean var6 = false;

      try {
         var6 = Boolean.parseBoolean(this.tryGetParam(var1, "reload")[0]);
      } catch (Exception var17) {
      }

      try {
         Element var7 = null;
         if (SQUtils.getExtension(var4).toLowerCase().equals("sqw")) {
            var7 = XMLUtil.fileToXmlElement(var5);
            if (var7 == null) {
               throw new Exception(L.t("Portfolio settings not found in the template file.", new Object[0]));
            }
         } else {
            ResultsGroup var8 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{var4});
            var7 = var8.getStrategyXml();
            var2.put("lastSettings", var8.getLastSettings());
         }

         Element var22 = XMLUtil.getChildElem(var7, "Strategy");
         Element var9 = XMLUtil.getChildElem(var22, "Datas");
         List var10 = var9.getChildren("data");

         for (int var11 = 0; var11 < var10.size(); var11++) {
            Element var12 = (Element)var10.get(var11);
            JSONObject var13 = new JSONObject();
            var13.put("name", XMLUtil.getNodeValue(var12, "chart"));
            var13.put("symbol", XMLUtil.getNodeValue(var12, "symbol"));
            String var14 = XMLUtil.getNodeValue(var12, "timeFrame");

            try {
               TimeframeManager.getMillis(var14);
            } catch (Exception var16) {
               var14 = TimeframeManager.translateSQ3TFToString(Integer.parseInt(var14));
            }

            var13.put("timeframe", var14);
            var3.put(var13);
         }
      } catch (Exception var18) {
         return apiErrorJSON(L.t("Cannot load template chart data settings.", new Object[0]), var18);
      }

      if (var6) {
         var2.put("strategyTemplateFiles", this.loadFilesFromFolder(SQPaths.strategyTemplatesDirPath));
      }

      var2.put("chartSettings", var3);
      var2.put("success", L.t("Template chart data returned.", new Object[0]));
      return var2.toString();
   }
}
