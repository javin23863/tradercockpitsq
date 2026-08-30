package com.strategyquant.tradinglib.simulator;

import com.strategyquant.lib.L;
import org.json.JSONArray;
import org.json.JSONObject;

public class Engines {
   private String[] langs = new String[]{
      L.tsq("MetaTrader4"), L.tsq("MetaTrader5 (netted)"), L.tsq("MetaTrader5 (hedged)"), L.tsq("Tradestation"), L.tsq("MultiCharts"), L.tsq("JForex")
   };
   public static final String MetaTrader4Name = "MetaTrader4";
   public static final int MetaTrader4 = -659455871;
   public static final String MetaTrader5NettedName = "MetaTrader5 (netted)";
   public static final int MetaTrader5Netted = 1441180233;
   public static final String MetaTrader5HedgedName = "MetaTrader5 (hedged)";
   public static final int MetaTrader5Hedged = 395961824;
   public static final String TradestationName = "Tradestation";
   public static final int Tradestation = 56756755;
   public static final String MultiChartsName = "MultiCharts";
   public static final int MultiCharts = 938213070;
   public static final String JForexName = "JForex";
   public static final int JForex = 1949519549;
   public static final String StockpickerName = "Stockpicker";
   public static final int Stockpicker = 1316847364;
   public static final String StockpickerSingleAssetName = "Single-asset cloud strategy";
   public static final int StockpickerSingleAsset = -1816889229;

   public static JSONArray toJSON() {
      JSONArray var0 = new JSONArray()
         .put("MetaTrader4")
         .put("MetaTrader5 (netted)")
         .put("MetaTrader5 (hedged)")
         .put("Tradestation")
         .put("MultiCharts")
         .put("JForex");
      var0.put("Stockpicker");
      var0.put("Single-asset cloud strategy");
      return var0;
   }

   public static JSONObject toJSONKeys() {
      JSONObject var0 = new JSONObject();
      var0.put("Stockpicker", "SP");
      var0.put("Single-asset cloud strategy", "SA");
      var0.put("MetaTrader4", "MT4");
      var0.put("MetaTrader5 (netted)", "MT5");
      var0.put("MetaTrader5 (hedged)", "MT5");
      var0.put("Tradestation", "TS");
      var0.put("MultiCharts", "MC");
      var0.put("JForex", "JF");
      return var0;
   }

   public static String getEngineKey(String var0) {
      if (var0 == null) {
         return null;
      }

      switch (var0) {
         case "MetaTrader4":
            return "MT4";
         case "MetaTrader5 (hedged)":
            return "MT5";
         case "MetaTrader5 (netted)":
            return "MT5";
         case "Tradestation":
            return "TS";
         case "MultiCharts":
            return "MC";
         case "JForex":
            return "JF";
         case "Stockpicker":
            return "SP";
         case "Single-asset cloud strategy":
            return "SA";
         default:
            return null;
      }
   }

   public static int getEngine(String var0) {
      if (var0 == null) {
         return -1;
      }

      switch (var0) {
         case "MetaTrader4":
            return -659455871;
         case "MetaTrader5 (hedged)":
            return 395961824;
         case "MetaTrader5 (netted)":
            return 1441180233;
         case "Tradestation":
            return 56756755;
         case "MultiCharts":
            return 938213070;
         case "JForex":
            return 1949519549;
         case "Stockpicker":
            return 1316847364;
         case "Single-asset cloud strategy":
            return -1816889229;
         default:
            return -1;
      }
   }

   public static String getEngineName(int var0) {
      switch (var0) {
         case -1816889229:
            return "Single-asset cloud strategy";
         case -659455871:
            return "MetaTrader4";
         case 56756755:
            return "Tradestation";
         case 395961824:
            return "MetaTrader5 (hedged)";
         case 938213070:
            return "MultiCharts";
         case 1316847364:
            return "Stockpicker";
         case 1441180233:
            return "MetaTrader5 (netted)";
         case 1949519549:
            return "JForex";
         default:
            return null;
      }
   }

   public static JSONArray list() {
      JSONArray var0 = new JSONArray();
      var0.put(new JSONObject().put("name", "MetaTrader4").put("value", "MetaTrader4"));
      var0.put(new JSONObject().put("name", "MetaTrader5 (netted)").put("value", "MetaTrader5 (netted)"));
      var0.put(new JSONObject().put("name", "MetaTrader5 (hedged)").put("value", "MetaTrader5 (hedged)"));
      var0.put(new JSONObject().put("name", "Tradestation").put("value", "Tradestation"));
      var0.put(new JSONObject().put("name", "MultiCharts").put("value", "MultiCharts"));
      var0.put(new JSONObject().put("name", "JForex").put("value", "JForex"));
      var0.put(new JSONObject().put("name", L.tsq("AlgoCloud Stockpicker")).put("value", "Stockpicker"));
      var0.put(new JSONObject().put("name", L.tsq("AlgoCloud Single-asset")).put("value", "Single-asset cloud strategy"));
      return var0;
   }

   public static boolean isTradestationEngine(int var0) {
      return var0 == 56756755 || var0 == 938213070;
   }

   public static boolean isAWCloudEngine(int var0) {
      return var0 == 1316847364 || var0 == -1816889229;
   }

   public static boolean isStandardEngine(int var0) {
      return !isAWCloudEngine(var0);
   }
}
