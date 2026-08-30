package com.strategyquant.plugin.CrossCheck.impl.MonteCarloRetest;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.PrecachedRequest;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.montecarlo.retest.MonteCarloRetestList;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonteCarloRetestServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(MonteCarloRetestServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList();
         case "fitnessList":
            return this.onFitnessList();
         case "fitnessGetConfidenceLevels":
            return this.onFitnessGetConfidenceLevels();
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   @PrecachedRequest(relativeURL = "/monteCarloRetest/list")
   private String onList() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      List var3 = MonteCarloRetestList.get().getAvailableClasses();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         MonteCarloRetest var5 = (MonteCarloRetest)var3.get(var4);
         JSONObject var6 = new JSONObject();
         JSONArray var7 = new JSONArray();
         var7.put(false);
         var7.put(var5.printFormatedName());
         JSONObject var8 = new JSONObject();
         var8.put("display", var5.getFormatedName());
         var8.put("config", XMLUtil.elementToString(var5.getXML()));
         var6.put("userdata", var8);
         var6.put("id", var5.getClass().getSimpleName());
         var6.put("data", var7);
         var2.put(var6);
      }

      var1.put("rows", var2);
      var1.put("success", L.t("RT methods listed.", new Object[0]));
      return var1.toString();
   }

   @PrecachedRequest(relativeURL = "/monteCarloRetest/fitnessGetConfidenceLevels")
   private String onFitnessGetConfidenceLevels() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (int var6 : CrossCheckMethod.ConfidenceLevels) {
         var2.put(var6);
      }

      var1.put("levels", var2);
      var1.put("success", L.t("Levels listed.", new Object[0]));
      return var1.toString();
   }

   @PrecachedRequest(relativeURL = "/monteCarloRetest/fitnessList")
   private String onFitnessList() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      var2.put(new JSONObject().put("name", "Net Profit (Return)").put("key", "NetProfit"));
      var2.put(new JSONObject().put("name", "Return / Drawdown ratio").put("key", "ReturnDDRatio"));
      var2.put(new JSONObject().put("name", "Drawdown").put("key", "Drawdown"));
      var2.put(new JSONObject().put("name", "R Expectancy").put("key", "RExpectancy"));
      var2.put(new JSONObject().put("name", "Annual % Return").put("key", "AnnualPctReturn"));
      JSONArray var3 = new JSONArray();
      List var4 = DatabankColumns.get().getAvailableClasses();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         DatabankColumn var6 = (DatabankColumn)var4.get(var5);
         JSONObject var7 = new JSONObject();
         var7.put("name", var6.getName());
         var7.put("key", var6.getClass().getSimpleName());
         var3.put(var7);
      }

      var2.put(new JSONObject().put("name", "Other").put("key", "Other").put("options", var3));
      var1.put("types", var2);
      var1.put("success", L.t("Listed.", new Object[0]));
      return var1.toString();
   }
}
