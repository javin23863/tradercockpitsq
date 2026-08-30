package com.strategyquant.plugin.Results.impl.SPOverview;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.strategyConfig.StrategyConfig;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPOverviewServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SPOverviewServlet.class);
   private static final String LOCK_SPOVERVIEWSERVLET = "SPOverviewServlet";
   private static IResultsGroupProvider rgProvider;
   private StrategyConfig strategyConfig = new StrategyConfig();

   public SPOverviewServlet(IResultsGroupProvider var1) {
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
      JSONObject var2 = new JSONObject();
      boolean var3 = Boolean.valueOf(this.tryGetParam(var1, "showAll")[0]);
      OrdersList var4 = null;
      ResultsGroup var5 = null;

      try {
         var5 = rgProvider.get(var1, "SPOverviewServlet");
         var4 = var5.orders();
      } catch (RecordNotFoundException var34) {
         Log.info("onPrint: " + var34.getMessage());
         return apiErrorJSONNoLog(null, var34);
      } finally {
         if (var5 != null) {
            var5.releaseLock("SPOverviewServlet");
         }
      }

      Object2ObjectOpenHashMap var6 = new Object2ObjectOpenHashMap();
      double var9 = 0.0;
      double var11 = 0.0;
      double var13 = 0.0;

      for (int var15 = 0; var15 < var4.size(); var15++) {
         Order var16 = var4.get(var15);
         if (var16.isFilledOrder() && var16.isRealOrder()) {
            String var7 = var16.Symbol;
            if (!var6.containsKey(var7)) {
               var6.put(var7, new SPOverviewServlet.SPStock(var7));
            }

            SPOverviewServlet.SPStock var8 = (SPOverviewServlet.SPStock)var6.get(var7);
            var8.pl = var8.pl + var16.PL;
            var8.volume = var8.volume + var16.Size;
            var8.trades++;
            if (var16.PL > 0.0F) {
               var9 += var16.PL;
            } else {
               var11 += var16.PL;
            }

            var13 += var16.Size;
         }
      }

      JSONObject var40 = new JSONObject();
      JSONArray var41 = new JSONArray();
      var40.put("data", var41);
      JSONObject var17 = new JSONObject();
      JSONArray var18 = new JSONArray();
      var17.put("rows", var18);
      ArrayList var19 = new ArrayList(var6.keySet());
      Collections.sort(var19);
      double var20 = 0.0;
      double var22 = 0.0;
      double var24 = 0.0;
      double var26 = 0.0;
      ArrayList var28 = new ArrayList();

      for (int var29 = 0; var29 < var19.size(); var29++) {
         String var37 = (String)var19.get(var29);
         SPOverviewServlet.SPStock var38 = (SPOverviewServlet.SPStock)var6.get(var37);
         var28.add(var38);
      }

      var28.sort(new SPOverviewServlet.StockComparatorByVolume());

      for (int var44 = 0; var44 < var28.size(); var44++) {
         SPOverviewServlet.SPStock var39 = (SPOverviewServlet.SPStock)var28.get(var44);
         var18.put(
            new JSONObject()
               .put("id", "r" + var44)
               .put("data", new JSONArray().put(var44 + 1).put(var39.symbol).put(SQUtils.round2(var39.pl)).put(var39.volume).put(var39.trades))
         );
         var20 = var39.volume / var13 * 100.0;
         if (var39.pl < 0.0) {
            var20 *= -1.0;
            var22 = var39.pl / var11 * 100.0 * -1.0;
            if (var39.pl < var24) {
               var24 = var39.pl;
            }
         } else {
            var22 = var39.pl / var9 * 100.0;
            if (var39.pl > var26) {
               var26 = var39.pl;
            }
         }

         var41.put(new JSONObject().put("x", var39.symbol).put("y", SQUtils.round2(var20)).put("v", SQUtils.round2(var22)).put("pl", SQUtils.round2(var39.pl)));
         if (!var3 && var18.length() == 100) {
            break;
         }
      }

      var40.put("min", SQUtils.round2(var24));
      var40.put("max", SQUtils.round2(var26));
      JSONArray var45 = new JSONArray();
      var28.sort(new SPOverviewServlet.StockComparatorByPL());
      this.copyData(var28, var45, var9, false, true);
      JSONArray var30 = new JSONArray();
      Collections.reverse(var28);
      this.copyData(var28, var30, var11, false, false);
      JSONArray var31 = new JSONArray();
      var28.sort(new SPOverviewServlet.StockComparatorByVolume());
      this.copyData(var28, var31, var13, true, false);
      var2.put("stats", this.getStats(var5));
      var2.put("treeMap", var40);
      var2.put("top10Profit", var45);
      var2.put("top10Loss", var30);
      var2.put("top10Volume", var31);
      var2.put("listAll", var17);
      var2.put("success", "ok");
      return var2.toString();
   }

   private JSONObject getStats(ResultsGroup var1) throws Exception {
      JSONObject var2 = new JSONObject();
      SQStats var3 = var1.portfolio().stats((byte)0, (byte)10, (byte)127);
      var2.put("NetProfit", SQUtils.d2(var3.getDouble("NetProfit")));
      var2.put("AvgProfitPerYear", SQUtils.d2(var3.getDouble("AvgProfitPerYear")));
      var2.put("CAGR", SQUtils.d2(var3.getDouble("CAGR")));
      var2.put("NumberOfTrades", var3.getInt("NumberOfTrades"));
      var2.put("SharpeRatio", var3.getInt("SharpeRatio"));
      var2.put("ProfitFactor", SQUtils.d2(var3.getDouble("ProfitFactor")));
      var2.put("ReturnDDRatio", SQUtils.d2(var3.getDouble("ReturnDDRatio")));
      var2.put("AvgTrade", SQUtils.d2(var3.getDouble("AvgTrade")));
      var2.put("Drawdown", SQUtils.d2(var3.getDouble("Drawdown")));
      var2.put("DrawdownPct", SQUtils.d2(var3.getDouble("DrawdownPct")));
      var2.put("Exposure", SQUtils.d2(var3.getDouble("Exposure")));
      var2.put("AnnualPctReturnDDRatio", SQUtils.d2(var3.getDouble("AnnualPctReturnDDRatio")));
      var2.put("AvgProfitPerMonth", SQUtils.d2(var3.getDouble("AvgProfitPerMonth")));
      return var2;
   }

   private void copyData(List<SPOverviewServlet.SPStock> var1, JSONArray var2, double var3, boolean var5, boolean var6) {
      for (int var10 = 0; var10 < Math.min(10, var1.size()); var10++) {
         SPOverviewServlet.SPStock var7 = (SPOverviewServlet.SPStock)var1.get(var10);
         double var8;
         if (var5) {
            var8 = var7.volume / var3 * 100.0;
         } else {
            if (var6 && var7.pl <= 0.0 || !var6 && var7.pl >= 0.0) {
               var2.put(new JSONObject().put("x", "").put("y", 0));
               continue;
            }

            var8 = var7.pl / var3 * 100.0;
         }

         var2.put(new JSONObject().put("x", var7.symbol).put("y", SQUtils.round2(var8)));
      }
   }

   private class SPStock {
      public String symbol;
      public double pl;
      public double volume;
      public long trades;

      public SPStock(String nullx) {
         this.symbol = nullx;
         this.pl = 0.0;
         this.volume = 0.0;
         this.trades = 0L;
      }
   }

   public class StockComparatorByPL implements Comparator<SPOverviewServlet.SPStock> {
      public int compare(SPOverviewServlet.SPStock var1, SPOverviewServlet.SPStock var2) {
         if (var1.pl > var2.pl) {
            return -1;
         } else {
            return var1.pl < var2.pl ? 1 : 0;
         }
      }
   }

   public class StockComparatorByVolume implements Comparator<SPOverviewServlet.SPStock> {
      public int compare(SPOverviewServlet.SPStock var1, SPOverviewServlet.SPStock var2) {
         if (var1.volume > var2.volume) {
            return -1;
         } else {
            return var1.volume < var2.volume ? 1 : 0;
         }
      }
   }
}
