package com.strategyquant.plugin.CrossCheck.impl.MonteCarloManipulation;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.PrecachedRequest;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.montecarlo.manipulation.MonteCarloManipulationList;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonteCarloManipulationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(MonteCarloManipulationServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList(var2);
         case "fitnessList":
            return this.onFitnessList();
         case "fitnessGetConfidenceLevels":
            return this.onFitnessGetConfidenceLevels();
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onList(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      List var4 = MonteCarloManipulationList.get().getAvailableClasses();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         MonteCarloManipulation var6 = (MonteCarloManipulation)var4.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();
         var8.put(false);
         var8.put(var6.printFormatedName());
         JSONObject var9 = new JSONObject();
         var9.put("display", var6.getFormatedName());
         var9.put("config", XMLUtil.elementToString(var6.getXML()));
         var7.put("userdata", var9);
         var7.put("id", var6.getClass().getSimpleName());
         var7.put("data", var8);
         var3.put(var7);
      }

      var2.put("rows", var3);
      var2.put("success", L.t("Methods listed.", new Object[0]));
      return var2.toString();
   }

   @PrecachedRequest(relativeURL = "/monteCarloManipulation/fitnessGetConfidenceLevels")
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

   @PrecachedRequest(relativeURL = "/monteCarloManipulation/fitnessList")
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
