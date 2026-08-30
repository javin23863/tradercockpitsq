package com.strategyquant.plugin.CryptoExchange.impl.Bitfinex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.exchange.Exchange;
import com.strategyquant.tradinglib.exchange.IExchange;
import com.strategyquant.tradinglib.exchange.SymbolInfo;
import com.strategyquant.tradinglib.historyData.HttpApacheManager;
import java.io.IOException;
import java.math.BigDecimal;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Takacs")
@Name(name = "Bitfinex")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing Bitfinex data")
@PluginImplementation
public class CryptoExchangeBitfinexPlugin extends Exchange {
   private JSONArray availableSymbols = null;

   public CryptoExchangeBitfinexPlugin() {
      (new Thread() {
         @Override
         public void run() {
            try {
               CryptoExchangeBitfinexPlugin.this.getSymbols();
            } catch (Exception var2) {
            }
         }
      }).start();
   }

   public String getName() {
      return "Bitfinex";
   }

   public JSONArray getSymbols() throws Exception {
      if (this.availableSymbols != null) {
         return this.availableSymbols;
      }

      String var1 = new String(HttpApacheManager.getInstance().getData("https://api.bitfinex.com/v2/conf/pub:list:pair:exchange"));
      this.availableSymbols = new JSONArray(var1).getJSONArray(0);
      return this.availableSymbols;
   }

   public IExchange clone() {
      return new CryptoExchangeBitfinexPlugin();
   }

   public void download() throws Exception {
      String var3 = null;
      long var5 = TimeframeManager.getMillis(this.dataInfo.timeframe);
      String var7 = this.convertTimeframe(this.dataInfo.timeframe);
      long var8 = this.importInfo.dateFrom > 0L ? this.importInfo.dateFrom : 0L;
      var8 = this.dataMerger.checkDate(var8);
      JsonFactory var12 = new JsonFactory();

      while (var8 <= this.importInfo.dateTo) {
         String var1 = "https://api.bitfinex.com/v2/candles/trade:" + var7 + ":t" + this.dataInfo.uSymbol + "/hist?limit=10000&sort=1&start=" + var8;
         long var13 = this.dataMerger.getPerformedDays();
         long var15 = System.currentTimeMillis();
         this.dataMerger.register(0, var8 / 1000L);
         this.dataMerger.reset(1);
         var3 = this.doDownload(var1);
         long var10 = System.currentTimeMillis() - var15;
         JsonParser var17 = var12.createParser(var3);
         var17.nextToken();
         VersatileData var4 = null;

         while (var17.currentToken() != null) {
            var17.nextToken();
            if (var17.currentToken() == JsonToken.END_ARRAY) {
               break;
            }

            var4 = this.parseData2(var17);
            if (var4.time > this.importInfo.dateTo) {
               break;
            }

            this.dataMerger.storeToQueue(0, var4);
            var17.nextToken();
         }

         if (var4 == null) {
            break;
         }

         this.dataMerger.tryToSaveQueue(var8);
         var8 = var4.time;
         var8 += var5;
         if (this.canceled && !this.stopping) {
            this.importInfo.dateTo = SQTime.correctDayEnd(var8);
            this.stopping = true;
         }

         this.checkPaused();
         this.evaluateSpeed(var15, var13, var3.getBytes().length, var10);
         Thread.sleep(2000L);
         if (this.dataMerger.getReader() != null) {
            this.dataMerger.readTill(var8, false);
         }
      }
   }

   private VersatileData parseData(JSONArray var1) {
      VersatileData var2 = new VersatileData();
      var2.time = var1.getLong(0);
      var2.open = var1.getDouble(1);
      var2.close = var1.getDouble(2);
      var2.high = var1.getDouble(3);
      var2.low = var1.getDouble(4);
      var2.volume = var1.getDouble(5);
      return var2;
   }

   private VersatileData parseData2(JsonParser var1) throws IOException {
      VersatileData var2 = new VersatileData();
      var2.time = var1.nextLongValue(-1L);
      var1.nextValue();
      var2.open = var1.getNumberValue().doubleValue();
      var1.nextValue();
      var2.close = var1.getNumberValue().doubleValue();
      var1.nextValue();
      var2.high = var1.getNumberValue().doubleValue();
      var1.nextValue();
      var2.low = var1.getNumberValue().doubleValue();
      var1.nextValue();
      var2.volume = var1.getNumberValue().doubleValue();
      return var2;
   }

   public JSONArray availableTimeframes() {
      return new JSONArray().put("M1").put("M5").put("M15").put("M30").put("H1").put("H3").put("H6").put("H12").put("D1");
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
            return var3 + "m";
         case "H":
            return var3 + "h";
         case "D":
            return var3 + "D";
         default:
            throw new Exception(L.t("Invalid timeframe format. Prefix '%s' not recognized.", new Object[]{var2}));
      }
   }

   public boolean checkSymbolExists(String var1) {
      try {
         String var2 = SQUtils.httpGet("https://api.bitfinex.com/v1/pubticker/" + var1);
         JSONObject var3 = new JSONObject(var2);
         var3.getString("last_price");
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   public SymbolInfo getSymbolInfo(String var1) {
      try {
         String var2 = SQUtils.httpGet("https://api.bitfinex.com/v1/symbols_details");
         JSONArray var3 = new JSONArray(var2);
         SymbolInfo var4 = new SymbolInfo();

         for (int var5 = 0; var5 < var3.length(); var5++) {
            JSONObject var6 = var3.getJSONObject(var5);
            String var7 = var6.getString("pair");
            if (var1.equalsIgnoreCase(var7)) {
               int var8 = var6.getInt("price_precision");
               double var9 = BigDecimal.ONE.movePointLeft(var8).doubleValue();
               var4.setTickSize(var9);
               var4.setTickStep(var9);
               break;
            }
         }

         return var4;
      } catch (Exception var11) {
         throw new RuntimeException("Error while fetching symbol info", var11);
      }
   }
}
