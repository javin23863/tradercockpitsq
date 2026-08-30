package com.strategyquant.plugin.Results.impl.Overview;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.OverviewTemplate;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.overview.OverviewTemplates;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverviewServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(OverviewServlet.class);
   private static final String LOCK_OVERVIEWSERVLET = "OverviewServlet";
   private static IResultsGroupProvider rgProvider;

   public OverviewServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getOverviewContent":
            return this.onGetOverviewContent(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command", new Object[0]) + " '" + var1 + "'.", null);
      }
   }

   private String onGetOverviewContent(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      Object var3 = null;
      ResultsGroup var4 = null;
      String var5 = "Portfolio";
      byte var6 = 0;
      byte var7 = 127;
      String var8 = "SQDefaultWithPortfolio";

      try {
         var5 = this.tryGetParam(var1, "resultKey")[0];
         var6 = Byte.parseByte(this.tryGetParam(var1, "direction")[0]);
         var7 = Byte.parseByte(this.tryGetParam(var1, "sampleType")[0]);
         var8 = this.tryGetParam(var1, "template")[0];
         var4 = rgProvider.get(var1, "OverviewServlet");
         OverviewTemplate var9 = (OverviewTemplate)OverviewTemplates.getInstance().findClassByName(var8);
         var3 = var9.drawValues(var4, var5, new StatsTypeCombination(var6, (byte)10, var7));
      } catch (Exception var14) {
         var2.put("error", "Cannot get overview content - " + var14.getMessage());
         return var2.toString();
      } finally {
         if (var4 != null) {
            var4.releaseLock("OverviewServlet");
         }
      }

      if (var3 != null) {
         var2.put("overviewHtml", var3);
         var2.put("success", L.t("Html code generated.", new Object[0]));
         return var2.toString();
      } else {
         return apiErrorJSON(L.t("Cannot get overview content.", new Object[0]), null);
      }
   }

   public JSONArray getTemplates() throws Exception {
      JSONArray var1 = new JSONArray();

      for (OverviewTemplate var3 : OverviewTemplates.getInstance().getAvailableClasses()) {
         if (!var3.getClass().getSimpleName().equals("AWSharedDefault")) {
            var1.put(new JSONObject().put("value", var3.getClass().getSimpleName()).put("name", var3.getName()));
         }
      }

      return var1;
   }
}
