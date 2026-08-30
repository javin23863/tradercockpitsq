package com.strategyquant.plugin.Results.impl.Chart;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.stockchart.StockData;
import com.strategyquant.tradinglib.stockchart.StockLoader;
import com.strategyquant.tradinglib.stockchart.StockLoaderForStockPicking;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Map;
import java.util.jar.JarFile;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartServlet extends HttpJSONServlet {
   private static final DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
   private static final Logger Log = LoggerFactory.getLogger(ChartServlet.class);
   private static final String LOCK_CHARTSERVLET = "ChartServlet";
   private static IResultsGroupProvider rgProvider;

   public ChartServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "loadChartData":
            return this.onLoadChartData(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private Long tryToParseTime(String var1) {
      try {
         return formaterDate.parseMillis(var1);
      } catch (Exception var3) {
         return null;
      }
   }

   private String onLoadChartData(Map<String, String[]> var1) {
      JarFile var2 = null;
      Object var3 = null;

      try {
         Long var4 = null;
         boolean var5 = false;

         try {
            String var6 = this.tryGetParam(var1, "indexFrom")[0];
            var4 = this.tryToParseTime(var6);
            if (var4 == null) {
               var4 = (long)Double.parseDouble(var6);
            } else {
               var5 = true;
            }
         } catch (Exception var52) {
         }

         Long var58 = null;

         try {
            String var7 = this.tryGetParam(var1, "indexTo")[0];
            var58 = this.tryToParseTime(var7);
            if (var58 == null) {
               var58 = (long)Double.parseDouble(var7);
            } else {
               var5 = true;
            }
         } catch (Exception var51) {
         }

         boolean var59 = true;

         try {
            var59 = !this.tryGetParam(var1, "preview")[0].equalsIgnoreCase("false");
         } catch (Exception var50) {
         }

         Long var8 = null;

         try {
            String var9 = this.tryGetParam(var1, "trade")[0];
            var8 = Long.parseLong(var9);
         } catch (Exception var49) {
         }

         Integer var60 = null;

         try {
            String var10 = this.tryGetParam(var1, "zoomWidth")[0];
            var60 = Integer.parseInt(var10);
         } catch (Exception var48) {
         }

         ResultsGroup var61 = rgProvider.get(var1, "ChartServlet");
         String var11 = var61.getMainResultKey();
         Double var12 = null;
         ArrayList var13 = var61.symbols().list();
         if (var13 != null && !var13.isEmpty()) {
            String var14 = (String)var13.get(0);
            DataInfo var15 = DataManager.getDataInfo("History", var14);
            if (var15 != null) {
               InstrumentInfo var16 = InstrumentManager.getInstrumentInfo(var15.instrument);
               var12 = var16.tickSize;
            }
         }

         boolean var62 = false;

         try {
            Result var64 = var61.subResult(var11);
            Databank var67 = var61.getDatabank();
            var62 = var61.isStockpickerStrategy();
            String var17 = var67 != null && var67.isFileBased() && var61.storesChartData() ? var61.getFilePath() : var64.getStockChartPath();
            if (var17 == null) {
               return apiErrorJSON(L.t("Cannot load charts.", new Object[0]), null);
            }

            Log.info("Using chart path {}", var17);
            var2 = new JarFile(var17);
         } finally {
            var61.releaseLock("ChartServlet");
         }

         StockData var65 = null;
         if (var62) {
            StockLoaderForStockPicking var68 = new StockLoaderForStockPicking();
            String[] var70 = (String[])var1.get("stock");
            String var18 = var70 == null ? null : var70[0];
            var65 = var68.load(var2, var4, var58, var8, var59, var5, var11, var60, var18);
         } else {
            StockLoader var69 = new StockLoader();
            var65 = var69.load(var2, var4, var58, var8, var59, var5, var11, var60);
         }

         var65.setPipValue(var12);
         var3 = var65.toJSON();
      } catch (Exception var54) {
         Log.error("Error while loading chart", var54);
         return apiErrorJSON(L.t("Cannot load charts.", new Object[0]), var54);
      } finally {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Exception var46) {
            }
         }
      }

      JsonCreator var57 = new JsonCreator();
      var57.beginObject();
      var57.put("success", "Charts data loaded", true);
      var57.putRaw("chart", var3, false);

      try {
         var57.separator();
         var57.put("requestId", this.tryGetParam(var1, "requestId")[0], false);
      } catch (Exception var47) {
      }

      var57.endObject(false);
      return var57.toJson();
   }
}
