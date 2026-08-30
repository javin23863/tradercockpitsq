package com.strategyquant.plugin.CryptoExchange.impl.Poloniex;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.exchange.Exchange;
import com.strategyquant.tradinglib.exchange.IExchange;
import com.strategyquant.tradinglib.exchange.SymbolInfo;
import com.strategyquant.tradinglib.historyData.HttpApacheManager;
import java.math.BigDecimal;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Takacs")
@Name(name = "Poloniex")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing Poloniex data")
@PluginImplementation
public class CryptoExchangePoloniexPlugin extends Exchange {
   private JSONArray availableSymbols = null;

   public CryptoExchangePoloniexPlugin() {
      (new Thread() {
         @Override
         public void run() {
            try {
               CryptoExchangePoloniexPlugin.this.getSymbols();
            } catch (Exception var2) {
            }
         }
      }).start();
   }

   public String getName() {
      return "Poloniex";
   }

   public JSONArray getSymbols() throws Exception {
      if (this.availableSymbols != null) {
         return this.availableSymbols;
      }

      String var1 = new String(HttpApacheManager.getInstance().getData("https://api.poloniex.com/markets"));
      JSONArray var3 = new JSONArray(var1);
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < var3.length(); var5++) {
         JSONObject var6 = var3.getJSONObject(var5);
         String var2 = var6.getString("symbol");
         var4.put(var2);
      }

      this.availableSymbols = var4;
      return this.availableSymbols;
   }

   public IExchange clone() {
      return new CryptoExchangePoloniexPlugin();
   }

   public void download() throws Exception {
      long var5 = TimeframeManager.getMillis(this.dataInfo.timeframe);
      String var7 = this.convertTimeframe(this.dataInfo.timeframe);
      long var8 = this.importInfo.dateFrom > 0L ? this.importInfo.dateFrom : 0L;
      if (var8 == 0L) {
         var8 = this.getMinimalDate(this.dataInfo.uSymbol);
      }

      var8 = this.dataMerger.checkDate(var8);
      short var12 = 500;

      while (var8 <= this.importInfo.dateTo) {
         long var13 = this.getEndOfTime(var8, var12 - 1, this.dataInfo.timeframe);
         String var1 = "https://api.poloniex.com/markets/"
            + this.dataInfo.uSymbol
            + "/candles?limit="
            + var12
            + "&interval="
            + var7
            + "&startTime="
            + var8
            + "&endTime="
            + var13;
         long var15 = this.dataMerger.getPerformedDays();
         long var17 = System.currentTimeMillis();
         String var3 = this.doDownload(var1);
         long var10 = System.currentTimeMillis() - var17;
         JSONArray var2 = new JSONArray(var3);
         this.dataMerger.register(0, var8);
         this.dataMerger.reset(1);
         Long var19 = null;

         for (int var20 = 0; var20 < var2.length(); var20++) {
            VersatileData var4 = this.parseData(var2.getJSONArray(var20));
            if (var4.time > this.importInfo.dateTo) {
               break;
            }

            this.dataMerger.storeToQueue(0, var4);
            var19 = var4.time;
         }

         this.dataMerger.tryToSaveQueue(var8);
         var8 = var19 == null ? this.importInfo.dateTo + var5 : var19 + var5;
         this.checkPaused();
         if (this.canceled && !this.stopping) {
            this.importInfo.dateTo = SQTime.correctDayEnd(var8);
            this.stopping = true;
         }

         this.evaluateSpeed(var17, var15, var3.getBytes().length, var10);
         Thread.sleep(250L);
         if (this.dataMerger.getReader() != null) {
            this.dataMerger.readTill(var8, false);
         }
      }
   }

   private long getEndOfTime(long var1, int var3, String var4) throws Exception {
      String var5 = var4.substring(0, 1);
      int var6 = 0;

      try {
         var6 = Integer.parseInt(var4.substring(1));
      } catch (Exception var9) {
         throw new Exception(L.t("Invalid timeframe format. Cannot parse time units count.", new Object[0]));
      }

      switch (var5) {
         case "M":
            return SQTime.addMinutes(var1, var6 * var3);
         case "H":
            return SQTime.addHours(var1, var6 * var3);
         case "D":
            return SQTime.addDays(var1, var6 * var3);
         default:
            throw new Exception(L.t("Invalid timeframe format. Prefix '%s' not recognized.", new Object[]{var5}));
      }
   }

   private long getMinimalDate(String var1) {
      String var2;
      try {
         var2 = new String(HttpApacheManager.getInstance().getData("https://api.poloniex.com/markets/" + var1));
      } catch (Exception var5) {
         Log.error("Error while downloading symbol info", var5);
         throw new RuntimeException("Error while downloading symbol info");
      }

      JSONArray var3 = new JSONArray(var2);
      JSONObject var4 = var3.getJSONObject(0);
      return var4.getLong("visibleStartTime");
   }

   private VersatileData parseData(JSONArray var1) {
      VersatileData var2 = new VersatileData();
      var2.time = var1.getLong(12);
      var2.open = var1.getDouble(2);
      var2.high = var1.getDouble(1);
      var2.low = var1.getDouble(0);
      var2.close = var1.getDouble(3);
      var2.volume = var1.getDouble(4);
      return var2;
   }

   public JSONArray availableTimeframes() {
      return new JSONArray().put("M5").put("M15").put("M30").put("H2").put("H4").put("D1");
   }

   public String convertTimeframe(String var1) throws Exception {
      String var2 = var1.substring(0, 1);
      int var3 = 0;

      try {
         var3 = Integer.parseInt(var1.substring(1));
      } catch (Exception var6) {
         throw new Exception(L.t("Invalid timeframe format. Cannot parse time units count.", new Object[0]));
      }

      switch (var2) {
         case "M":
            return "MINUTE_" + var3;
         case "H":
            return "HOUR_" + var3;
         case "D":
            return "DAY_" + var3;
         default:
            throw new Exception(L.t("Invalid timeframe format. Prefix '%s' not recognized.", new Object[]{var2}));
      }
   }

   public boolean checkSymbolExists(String var1) {
      try {
         String var2 = new String(HttpApacheManager.getInstance().getData("https://api.poloniex.com/markets/" + var1));
         new JSONArray(var2);
         return true;
      } catch (Exception var3) {
         return false;
      }
   }

   public SymbolInfo getSymbolInfo(String var1) {
      try {
         String var2 = new String(HttpApacheManager.getInstance().getData("https://api.poloniex.com/markets/" + var1));
         JSONArray var3 = new JSONArray(var2);
         JSONObject var4 = var3.getJSONObject(0).getJSONObject("symbolTradeLimit");
         SymbolInfo var5 = new SymbolInfo();
         int var6 = var4.getInt("priceScale");
         double var7 = BigDecimal.ONE.movePointLeft(var6).doubleValue();
         var5.setTickSize(var7);
         var5.setTickStep(var7);
         return var5;
      } catch (Exception var9) {
         throw new RuntimeException("Error while fetching symbol info", var9);
      }
   }
}
