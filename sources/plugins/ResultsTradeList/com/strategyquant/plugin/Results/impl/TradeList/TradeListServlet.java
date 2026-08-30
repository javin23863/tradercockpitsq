package com.strategyquant.plugin.Results.impl.TradeList;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.stockchart.StockLoader;
import com.strategyquant.tradinglib.stockchart.StockLoaderForStockPicking;
import com.strategyquant.tradinglib.table.TradelistOrdersComparator;
import com.strategyquant.tradinglib.tradelist.TradelistExport;
import com.strategyquant.tradinglib.tradelist.TradelistTableColumnEntry;
import com.strategyquant.tradinglib.tradelist.TradelistTableView;
import com.strategyquant.tradinglib.tradelist.TradelistViewsManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.jar.JarFile;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeListServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(TradeListServlet.class);
   private static final String LOCK_TRADELISTSERVLET = "TradeListServlet";
   private static final int MAX_CACHED_RESULTS = 50;
   private HashMap<String, ArrayList<Object[]>> dataCache = new HashMap<>();
   private HashMap<String, Long> lastUsed = new HashMap<>();
   private StampedLock lock = new StampedLock();
   private TradelistOrdersComparator comparator = new TradelistOrdersComparator();
   private static IResultsGroupProvider rgProvider;
   private DateTimeFormatter formaterFullDateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
   private boolean exportRunning = false;

   public TradeListServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      String[] var4 = var1.split("/");
      if (var4 != null && var4.length > 0) {
         switch (var4[var4.length - 1]) {
            case "getTrades":
               return this.onGetTrades(var4, var2);
            case "export":
               return this.onExport(var4, var2);
            case "exportCloud":
               return this.onExportCloud(var4, var2);
            default:
               return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
         }
      } else {
         return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String onGetTrades(String[] var1, Map<String, String[]> var2) throws Exception {
      this.checkParamExists(var2, new String[]{"resultKey", "direction", "sampleType"});
      JsonCreator var3 = new JsonCreator();
      Map var4 = this.getParameters(var1, var2);
      Map var5 = this.getRgParams(var4);
      boolean var6 = false;

      try {
         var6 = ((String[])var2.get("expired"))[0].equals("true");
      } catch (Exception var15) {
      }

      var4.put("expired", var6);
      ResultsGroup var7 = rgProvider.get(var5, "TradeListServlet");

      try {
         if (var7.orders() == null) {
            var3.put("total_count", 0, true);
            var3.beginObject();
            var3.put("rows");
            var3.beginArray();
            var3.endArray(true);
            var3.endObject(true);
            var3.put("pos", 0, false);
            return var3.toString();
         }

         TradelistTableView var8 = null;
         if (var4.containsKey("viewXML")) {
            var8 = new TradelistTableView();
            var8.setFromXML(XMLUtil.stringToElement((String)var4.get("viewXML")));
         } else {
            var8 = TradelistViewsManager.getInstance().getSelectedTradelistView();
         }

         String var9 = this.getDataIdentificator(var4, var8, var7);
         boolean var10 = this.chartDataExists(var7, (String)var4.get("resultKey"));
         this.loadTrades(var4, var7, var8, var9, var10, var3);
      } catch (Exception var16) {
         Log.error("Cannot load trades", var16);
         return null;
      } finally {
         if (var7 != null) {
            var7.releaseLock("TradeListServlet");
         }
      }

      if (Log.isDebugEnabled()) {
         Log.debug(var3.toString());
      }

      return var3.toString();
   }

   private String onExport(String[] var1, Map<String, String[]> var2) throws Exception {
      this.checkParamExists(var2, new String[]{"resultKey", "direction", "sampleType", "path", "useComma"});
      Map var3 = this.getParameters(var1, var2);
      if (this.exportRunning) {
         return apiErrorJSON(L.t("Export is already running. Please wait until it finishes", new Object[0]), null);
      }

      this.exportRunning = true;
      boolean var4 = false;

      try {
         var4 = ((String[])var2.get("expired"))[0].equals("true");
      } catch (Exception var27) {
      }

      var3.put("expired", var4);

      try {
         String var5 = ((String[])var2.get("path"))[0];
         boolean var6 = Boolean.parseBoolean(((String[])var2.get("useComma"))[0]);
         Map var7 = this.getRgParams(var3);
         ResultsGroup var8 = rgProvider.get(var7, "TradeListServlet");

         try {
            TradelistTableView var9 = null;
            if (var3.containsKey("viewXML")) {
               var9 = new TradelistTableView();
               var9.setFromXML(XMLUtil.stringToElement((String)var3.get("viewXML")));
            } else {
               var9 = TradelistViewsManager.getInstance().getSelectedTradelistView();
            }

            String var10 = this.getDataIdentificator(var3, var9, var8);
            String var11 = var10.substring(0, var10.lastIndexOf("#"));
            ArrayList var12 = this.generateNewData(var8, var9, var3, true);
            TradelistExport var13 = new TradelistExport();
            if (var5.endsWith("xlsx")) {
               var13.toXlsx(var12, var5, var6);
            } else {
               var13.toCsv(var12, var5, var6);
            }
         } catch (Exception var26) {
            Log.error("Cannot export trades", var26);
            throw var26;
         } finally {
            if (var8 != null) {
               var8.releaseLock("TradeListServlet");
            }
         }
      } catch (Exception var29) {
         return apiErrorJSON(L.t("Tradelist export failed", new Object[0]), var29);
      } finally {
         this.exportRunning = false;
      }

      JSONObject var31 = new JSONObject();
      var31.put("success", "Tradelist exported.");
      return var31.toString();
   }

   private String onExportCloud(String[] var1, Map<String, String[]> var2) throws Exception {
      JSONObject var3 = new JSONObject();
      this.checkParamExists(var2, new String[]{"resultKey", "direction", "sampleType", "path", "useComma"});
      Map var4 = this.getParameters(var1, var2);
      if (this.exportRunning) {
         return apiErrorJSON(L.t("Export is already running. Please wait until it finishes", new Object[0]), null);
      }

      this.exportRunning = true;
      boolean var5 = false;

      try {
         var5 = ((String[])var2.get("expired"))[0].equals("true");
      } catch (Exception var27) {
      }

      var4.put("expired", var5);

      try {
         boolean var6 = Boolean.parseBoolean(((String[])var2.get("useComma"))[0]);
         String var7 = ((String[])var2.get("extension"))[0];
         Map var8 = this.getRgParams(var4);
         ResultsGroup var9 = rgProvider.get(var8, "TradeListServlet");

         try {
            TradelistTableView var10 = null;
            if (var4.containsKey("viewXML")) {
               var10 = new TradelistTableView();
               var10.setFromXML(XMLUtil.stringToElement((String)var4.get("viewXML")));
            } else {
               var10 = TradelistViewsManager.getInstance().getSelectedTradelistView();
            }

            ArrayList var11 = this.generateNewData(var9, var10, var4, true);
            TradelistExport var12 = new TradelistExport();
            if (var7.equals("xlsx")) {
               byte[] var13 = var12.toXlsxCloud(var11);
               var3.put("data", Base64.getEncoder().encodeToString(var13));
            } else {
               String var32 = var12.toCsvCloud(var11, var6);
               var3.put("data", Base64.getEncoder().encodeToString(var32.getBytes()));
            }
         } catch (Exception var26) {
            Log.error("Cannot export trades", var26);
            throw var26;
         } finally {
            if (var9 != null) {
               var9.releaseLock("TradeListServlet");
            }
         }
      } catch (Exception var29) {
         return apiErrorJSON(L.t("Tradelist export failed", new Object[0]), var29);
      } finally {
         this.exportRunning = false;
      }

      var3.put("success", "Tradelist exported.");
      return var3.toString();
   }

   private void loadTrades(Map<String, Object> var1, ResultsGroup var2, TradelistTableView var3, String var4, boolean var5, JsonCreator var6) {
      var6.beginObject();
      var6.put("rows");
      var6.beginArray();

      try {
         if (var3 == null) {
            Log.error("Cannot load the list of trades. No tradelist view is set.");
         }

         ArrayList var7 = null;
         if (this.dataCache.containsKey(var4)) {
            var7 = this.dataCache.get(var4);
         } else {
            var7 = this.generateNewData(var2, var3, var1, false);
            this.dataCache.put(var4, var7);
         }

         this.lastUsed.put(var4, System.currentTimeMillis());
         int var8 = (Integer)var1.get("posStart");
         int var9 = (Integer)var1.get("count");

         for (int var10 = var8; var10 < var7.size() && var10 < var8 + var9; var10++) {
            var6.beginObject();
            var6.put("data");
            var6.beginArray();

            for (int var11 = 0; var11 < ((Object[])var7.get(var10)).length - 5; var11++) {
               Object var12 = ((Object[])var7.get(var10))[var11];
               var6.setValue(var12, true);
            }

            Byte var16 = (Byte)((Object[])var7.get(var10))[((Object[])var7.get(var10)).length - 1];
            boolean var17 = var16 == 1 || var16 == 2;
            Byte var13 = (Byte)((Object[])var7.get(var10))[((Object[])var7.get(var10)).length - 2];
            if (var13 >= 20 && var13 <= 30) {
               var6.setValue(2, true);
            } else if (var13 >= 40 && var13 <= 50) {
               var6.setValue(1, true);
            } else {
               var6.setValue(0, true);
            }

            if (var17) {
               var6.setValue(var5 ? 1 : 2, true);
            } else {
               var6.setValue(0, true);
            }

            var6.setValue(((Object[])var7.get(var10))[((Object[])var7.get(var10)).length - 5], true);
            var6.setValue(SQTime.toString((Long)((Object[])var7.get(var10))[((Object[])var7.get(var10)).length - 4], this.formaterFullDateTime), true);
            var6.setValue(SQTime.toString((Long)((Object[])var7.get(var10))[((Object[])var7.get(var10)).length - 3], this.formaterFullDateTime), false);
            var6.endArray(true);
            var6.put("id", "r" + (var10 + 1), false);
            var6.endObject(var10 < var7.size() - 1 && var10 < var8 + var9 - 1);
         }

         var6.endArray(true);
         var6.put("total_count", var7.size(), true);
         var6.put("pos", var8, false);
         var6.endObject(false);
      } catch (Exception var14) {
         Log.error("Getting trades failed", var14);
      }
   }

   private ArrayList<Object[]> generateNewData(ResultsGroup var1, TradelistTableView var2, Map<String, Object> var3, boolean var4) throws Exception {
      if (this.dataCache.size() > 50) {
         this.deleteOldestData();
      }

      int var5 = (Integer)var3.get("sortColumn");
      boolean var6 = (Boolean)var3.get("asc");
      String var7 = (String)var3.get("resultKey");
      OrdersList var8 = null;
      if (this.shouldShowControlOrders()) {
         var8 = var1.orders().filter(var7, (Byte)var3.get("direction"), (Byte)var3.get("sampleType"), !(Boolean)var3.get("expired"));
      } else {
         var8 = var1.orders().filterExcludingControlOrders(var7, (Byte)var3.get("direction"), (Byte)var3.get("sampleType"), !(Boolean)var3.get("expired"));
      }

      if (var1.isPortfolio()) {
         if (!var7.equals("Portfolio")) {
            this.computeAccountBalance(var8);
         } else {
            this.resetAccountBalance(var8);
         }
      }

      if (var5 >= 0) {
         try {
            TradelistTableColumnEntry var9 = (TradelistTableColumnEntry)var2.columns.get(var5);
            this.comparator.setColumn(var9.tableColumn);
            this.comparator.setAsc(var6);
            var8.sort(this.comparator);
         } catch (Exception var10) {
            Log.error("Tradelist sorting failed", var10);
         }
      }

      return var2.getData(var8, 0, var8.size(), var4);
   }

   private void computeAccountBalance(OrdersList var1) {
      float var2 = 0.0F;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Order var4 = var1.get(var3);
         var2 += var4.PL;
         var4.AccountBalanceTemp = var2;
      }
   }

   private void resetAccountBalance(OrdersList var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Order var3 = var1.get(var2);
         var3.AccountBalanceTemp = -1.0E8F;
      }
   }

   private void deleteOldestData() {
      String var1 = null;
      long var2 = Long.MAX_VALUE;

      for (String var5 : this.lastUsed.keySet()) {
         long var6 = this.lastUsed.get(var5);
         if (var6 < var2) {
            var2 = var6;
            var1 = var5;
         }
      }

      this.dataCache.remove(var1);
      this.lastUsed.remove(var1);
   }

   private String getDataIdentificator(Map<String, Object> var1, TradelistTableView var2, ResultsGroup var3) {
      StringBuilder var4 = new StringBuilder();
      var4.append(var1.get("projectName"));
      var4.append(var1.get("databankName"));
      var4.append(var1.containsKey("backtestID") ? var1.get("backtestID") : var1.get("strategyName"));
      var4.append(var2.name);
      var4.append(var2.lastChanged);
      var4.append(var1.get("resultKey"));
      var4.append(var1.get("direction"));
      var4.append(var1.get("sampleType"));
      var4.append(var1.get("sortColumn"));
      var4.append(var1.get("asc"));
      var4.append(var1.get("expired"));
      var4.append(this.shouldShowControlOrders());
      var4.append("#");
      var4.append(var3.specialValues().getLong(SpecialValues.LastModified));
      return var4.toString();
   }

   private boolean chartDataExists(ResultsGroup var1, String var2) {
      JarFile var3 = null;

      try {
         var2 = var2.replace("amp", "&");
         Result var4 = var1.subResult(var2);
         var3 = new JarFile(var4.getStockChartPath());
         if (var1.isStockpickerStrategy()) {
            StockLoaderForStockPicking var5 = new StockLoaderForStockPicking();
            var5.load(var3, null, null, null, false, false, var4.getResultKey(), null, null);
         } else {
            StockLoader var18 = new StockLoader();
            var18.load(var3, null, null, null, true, false, var4.getResultKey(), null);
         }

         return true;
      } catch (Exception var15) {
         return false;
      } finally {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Exception var14) {
            }
         }
      }
   }

   private boolean shouldShowControlOrders() {
      boolean var1 = false;

      try {
         var1 = Boolean.parseBoolean(MainApp.settings().get("showControlOrders", "false"));
      } catch (Exception var3) {
      }

      return var1;
   }
}
