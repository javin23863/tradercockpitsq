package com.strategyquant.plugin.Results.impl.Explore;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.explore.Explore;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExploreServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ExploreServlet.class);
   private static final String LOCK_EXPLORESERVLET = "ExploreServlet";
   private static IResultsGroupProvider rgProvider;
   private static final int MAX_CACHED_RESULTS = 15;
   private HashMap<String, ArrayList<Object[]>> dataCache = new HashMap<>();
   private HashMap<String, Long> lastUsed = new HashMap<>();

   public ExploreServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "data":
            return this.onData(var2);
         case "info":
            return this.onInfo(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onData(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "databankName", "strategyName", "symbol", "dateFrom", "dateTo"});
      JsonCreator var2 = new JsonCreator();
      Explore var3 = null;
      ResultsGroup var4 = null;
      String var5 = null;

      try {
         var4 = rgProvider.get(var1, "ExploreServlet");
         var3 = (Explore)var4.specialValues().get(SpecialValues.Explore);
         var5 = this.getDataIdentificator(var1, var4);
      } catch (RecordNotFoundException var11) {
         Log.error("onData: " + var11.getMessage());
         return apiErrorJSONNoLog(null, var11);
      } finally {
         if (var4 != null) {
            var4.releaseLock("ExploreServlet");
         }
      }

      if (var3 == null) {
         var2.put("total_count", 0, true);
         var2.beginObject();
         var2.put("rows");
         var2.beginArray();
         var2.endArray(true);
         var2.endObject(true);
         var2.put("pos", 0, false);
         return var2.toString();
      } else {
         this.loadData(var1, var3, var5, var2);
         return var2.toString();
      }
   }

   private String onInfo(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[0]);
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      JSONArray var4 = new JSONArray();
      Explore var5 = null;
      ResultsGroup var6 = null;

      try {
         var6 = rgProvider.get(var1, "ExploreServlet");
         var5 = (Explore)var6.specialValues().get(SpecialValues.Explore);
      } catch (RecordNotFoundException var13) {
         Log.error("onData: " + var13.getMessage());
         return apiErrorJSONNoLog(null, var13);
      } finally {
         if (var6 != null) {
            var6.releaseLock("ExploreServlet");
         }
      }

      if (var5 != null) {
         ArrayList var7 = new ArrayList();
         ObjectIterator var8 = var5.getData().keySet().iterator();

         while (var8.hasNext()) {
            var7.add((String)var8.next());
         }

         Collections.sort(var7);

         for (int var9 = 0; var9 < var7.size(); var9++) {
            var3.put(var7.get(var9));
         }

         ObjectArrayList var18 = var5.getColumns();

         for (int var10 = 0; var10 < var18.size(); var10++) {
            var4.put(var18.get(var10));
         }
      }

      SettingsMap var16 = var6.specialValues();
      String var17 = SQTime.toString(var16.getLong(SpecialValues.HistoryFrom), "yyyy.MM.dd");
      String var19 = SQTime.toString(var16.getLong(SpecialValues.HistoryTo), "yyyy.MM.dd");
      var2.put("symbols", var3);
      var2.put("dateFrom", var17);
      var2.put("dateTo", var19);
      var2.put("columns", var4);
      var2.put("success", "ok");
      return var2.toString();
   }

   private void loadData(Map<String, String[]> var1, Explore var2, String var3, JsonCreator var4) {
      int var5 = 0;
      int var6 = 100;

      try {
         var5 = Integer.parseInt(((String[])var1.get("posStart"))[0]);
      } catch (Exception var12) {
      }

      try {
         var6 = Integer.parseInt(((String[])var1.get("count"))[0]);
      } catch (Exception var11) {
      }

      var4.beginObject();
      var4.put("rows");
      var4.beginArray();

      try {
         ArrayList var7 = null;
         if (this.dataCache.containsKey(var3)) {
            var7 = this.dataCache.get(var3);
         } else {
            var7 = this.generateNewData(var2, var1);
            this.dataCache.put(var3, var7);
         }

         for (int var8 = var5; var8 < var7.size() && var8 < var5 + var6; var8++) {
            var4.beginObject();
            var4.put("data");
            var4.beginArray();

            for (int var9 = 0; var9 < ((Object[])var7.get(var8)).length; var9++) {
               Object var10 = ((Object[])var7.get(var8))[var9];
               if (var9 < ((Object[])var7.get(var8)).length - 1) {
                  var4.setValue(var10, true);
               } else {
                  var4.setValue(var10, false);
               }
            }

            var4.endArray(true);
            var4.put("id", "r" + (var8 + 1), false);
            var4.endObject(var8 < var7.size() - 1 && var8 < var5 + var6 - 1);
         }

         var4.endArray(true);
         var4.put("total_count", var7.size(), true);
         var4.put("pos", var5, false);
         var4.endObject(false);
      } catch (Exception var13) {
         Log.error("Getting trades failed", var13);
      }
   }

   private ArrayList<Object[]> generateNewData(Explore var1, Map<String, String[]> var2) throws Exception {
      String var3 = this.tryGetParamValue(var2, "symbol");
      long var4 = SQTime.parseToMilis(this.tryGetParamValue(var2, "dateFrom"), "yyyy.MM.dd");
      long var6 = SQTime.parseToMilis(this.tryGetParamValue(var2, "dateTo"), "yyyy.MM.dd");
      ArrayList var8 = new ArrayList();
      int var9 = var1.getColumns().size();
      Long2ObjectAVLTreeMap var10 = var1.getSymbolData(var3);
      if (var10 == null) {
         throw new Exception(L.t("No data found for symbol %s", new Object[]{var3}));
      }

      LongBidirectionalIterator var11 = var10.keySet().iterator();

      while (var11.hasNext()) {
         long var12 = (Long)var11.next();
         if (var12 >= var4) {
            if (var12 > var6) {
               break;
            }

            Object[] var14 = new Object[var9 + 2];
            var14[0] = var3;
            var14[1] = SQTime.toFullDateMinuteString(var12);
            double[] var15 = (double[])var10.get(var12);

            for (int var16 = 0; var16 < var9; var16++) {
               var14[var16 + 2] = var15[var16];
            }

            var8.add(var14);
         }
      }

      return var8;
   }

   private String getDataIdentificator(Map<String, String[]> var1, ResultsGroup var2) {
      StringBuilder var3 = new StringBuilder();
      var3.append(this.getParam(var1, "projectName"));
      var3.append(this.getParam(var1, "databankName"));
      var3.append(var1.containsKey("backtestID") ? this.getParam(var1, "backtestID") : this.getParam(var1, "strategyName"));
      var3.append(this.getParam(var1, "symbol"));
      var3.append(this.getParam(var1, "dateFrom"));
      var3.append(this.getParam(var1, "dateTo"));
      var3.append(this.getParam(var1, "sortColumn"));
      var3.append(this.getParam(var1, "asc"));
      var3.append("#");
      var3.append(var2.specialValues().getLong(SpecialValues.LastModified));
      return var3.toString();
   }
}
