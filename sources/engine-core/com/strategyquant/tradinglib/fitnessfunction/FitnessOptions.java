package com.strategyquant.tradinglib.fitnessfunction;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FitnessOptions {
   public static String list() {
      JSONObject var0 = new JSONObject();
      JSONArray var1 = new JSONArray();
      var1.put(new JSONObject().put("name", L.tsq("Net Profit (Return)")).put("key", "NetProfit"));
      var1.put(new JSONObject().put("name", L.tsq("Return / Drawdown ratio")).put("key", "ReturnDDRatio"));
      var1.put(new JSONObject().put("name", L.tsq("R Expectancy (Van Tharp)")).put("key", "RExpectancy"));
      var1.put(new JSONObject().put("name", L.tsq("Annual Return % / Max DD %")).put("key", "AnnualPctReturnDDRatio"));
      JSONArray var2 = new JSONArray();
      List var3 = DatabankColumns.get().getAvailableClasses();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         DatabankColumn var5 = (DatabankColumn)var3.get(var4);
         if (!var5.getType().equals("Text") && !var5.getClassName().equals("Fitness")) {
            JSONArray var6 = new JSONArray();
            var6.put(var5.getName());
            var6.put("{{selectWidget options='" + ValueTypes.getTypes().toString() + "' value='" + var5.getValueType() + "'}}");
            var6.put("{{spinnerWidget value='1' min='0'}}");
            var6.put("{{spinnerWidget value='" + var5.getTarget() + "' min='0'}}");
            JSONObject var7 = new JSONObject();
            var7.put("id", "r" + var4);
            var7.put("data", var6);
            var7.put("userdata", new JSONObject().put("key", var5.getClass().getSimpleName()).put("type", var5.getValueType()).put("target", var5.getTarget()));
            var2.put(var7);
         }
      }

      var1.put(new JSONObject().put("name", L.tsq("Weighted Fitness (multiple goals)")).put("key", "Weighted").put("rows", var2));
      var0.put("types", var1);
      var0.put("success", L.t("Listed.", new Object[0]));
      return var0.toString();
   }
}
