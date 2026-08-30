package com.strategyquant.plugin.Servlet.impl.Strategy;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.engine.TradingEngine;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.TradingSetupStatus;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(StrategyServlet.class);
   private TradingEngine tradingEngine = TradingEngine.getLiveEngine();
   private static StrategyServlet instance;

   public StrategyServlet() {
      instance = this;
   }

   public static StrategyServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, JSONObject var2) {
      switch (var1) {
         case "add":
            return this.onAdd(var2);
         case "start":
            return this.onStart(var2);
         case "stop":
            return this.onStop(var2);
         case "remove":
            return this.onRemove(var2);
         case "list":
            return this.onList();
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onAdd(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         Object var3 = var1.get("ConnectionName");
         if (var3 != null) {
            String var4 = var3.toString();
            MyStrategy var5 = new MyStrategy();
            ChartSetup var6 = new ChartSetup(var4, "EURUSD", "H1", 1, 100L, "No Session");
            this.tradingEngine.addSetup(var6, null, var5, new String[]{var4});
            var2.put("success", L.t("Strategy added.", new Object[0]));
            return var2.toString();
         } else {
            return apiErrorJSON(L.t("Cannot add strategy. Connection name not specified.", new Object[0]), null);
         }
      } catch (Exception var7) {
         return apiErrorJSON(null, var7);
      }
   }

   public String onList() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (TradingSetup var4 : this.tradingEngine.getTradingSetups()) {
         JSONObject var5 = new JSONObject();
         var5.put("SetupID", var4.getSetupId());
         var5.put("StrategyName", var4.getStrategy().getStrategyName());
         var5.put("ConnectionName", ((ChartDef)var4.getChartSetup().getCharts().get(0)).getConnectionName());
         var5.put("Symbol", ((ChartDef)var4.getChartSetup().getCharts().get(0)).getSymbol());
         var5.put("Status", TradingSetupStatus.toString(var4.getStatus()));
         var2.put(var5);
      }

      var1.put("strategies", var2);
      var1.put("success", L.t("Found %d trading setups.", new Object[]{this.tradingEngine.getTradingSetups().size()}));
      return var1.toString();
   }

   private String onRemove(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         Object var3 = var1.get("SetupID");
         if (var3 != null) {
            int var4 = Integer.parseInt(String.valueOf(var3));
            this.tradingEngine.remove(this.tradingEngine.getSetupById(var4));
            var2.put("success", L.t("Trading setup with ID '%s' removed.", new Object[]{var4}));
            return var2.toString();
         } else {
            return apiErrorJSON(L.t("Cannot remove trading setup. Setup ID not specified.", new Object[0]), null);
         }
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot remove trading setup.", new Object[0]), var5);
      }
   }

   private String onStart(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         Object var3 = var1.get("SetupID");
         if (var3 != null) {
            int var4 = Integer.parseInt(String.valueOf(var3));
            if (this.tradingEngine.getSetupById(var4).getStatus() != 1) {
               this.tradingEngine.getSetupById(var4).start(false);
               var2.put("success", L.t("Trading setup with ID '%s' started.", new Object[]{var4}));
               return var2.toString();
            } else {
               return apiErrorJSON(L.t("Trading setup with ID '%s' is already running.", new Object[]{var4}), null);
            }
         } else {
            return apiErrorJSON(L.t("Cannot start trading setup. Setup ID not specified.", new Object[0]), null);
         }
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot start setup.", new Object[0]), var5);
      }
   }

   private String onStop(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         Object var3 = var1.get("SetupID");
         if (var3 != null) {
            int var4 = (Integer)var3;
            if (this.tradingEngine.getSetupById(var4).getStatus() == 1) {
               this.tradingEngine.getSetupById(var4).stop();
               var2.put("success", L.t("Trading setup with ID '%s' stopped.", new Object[]{var4}));
               return var2.toString();
            } else {
               return apiErrorJSON(L.t("Trading setup with ID '%s' is not running.", new Object[]{var4}), null);
            }
         } else {
            return apiErrorJSON(L.t("Cannot stop trading setup. Setup ID not specified.", new Object[0]), null);
         }
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot stop setup.", new Object[0]), var5);
      }
   }
}
