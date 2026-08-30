package com.strategyquant.plugin.Results.impl.PortfolioComposerChart;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioComposerChartServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioComposerChartServlet.class);
   private static final String LOCK_KEY = "PortfolioComposerChart";
   private static IResultsGroupProvider rgProvider;
   public ResultsGroup rg = null;

   public PortfolioComposerChartServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[0]);
      String var2 = "PortfolioComposer chart not stored.";
      JSONObject var3 = new JSONObject();
      ResultsGroup var4 = null;

      try {
         if (this.rg == null) {
            var4 = rgProvider.get(var1, "PortfolioComposerChart");
         } else {
            var4 = this.rg;
         }

         String var5 = (String)var4.specialValues().get(SpecialValues.PortfolioComposerChart);
         if (var5 != null) {
            var2 = var5;
         }
      } catch (RecordNotFoundException var11) {
         Log.info("onPrint: " + var11.getMessage());
         return apiErrorJSONNoLog(null, var11);
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Getting PortfolioComposer chart failed", new Object[0]), var12);
      } finally {
         if (var4 != null && this.rg == null) {
            var4.releaseLock("PortfolioComposerChart");
            Object var14 = null;
            this.rg = null;
         }
      }

      var3.put("chart", var2);
      var3.put("success", "ok");
      return var3.toString();
   }
}
