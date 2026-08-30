package com.strategyquant.plugin.CryptoExchange.impl.Binance;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import java.io.IOException;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Takacs")
@Name(name = "Binance")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing Binance data")
@PluginImplementation
public class CryptoExchangeBinancePlugin extends Exchange {
   private JSONArray availableSymbols = null;

   public CryptoExchangeBinancePlugin() {
      (new Thread() {
         @Override
         public void run() {
            try {
               CryptoExchangeBinancePlugin.this.getSymbols();
            } catch (Exception var2) {
            }
         }
      }).start();
   }

   public String getName() {
      return "Binance";
   }

   public JSONArray getSymbols() throws Exception {
      if (this.availableSymbols != null) {
         return this.availableSymbols;
      }

      JSONArray var1 = new JSONArray();
      String var2 = new String(HttpApacheManager.getInstance().getData("https://www.binance.com/api/v1/ticker/allBookTickers"));
      JSONArray var3 = new JSONArray(var2);

      for (int var4 = 0; var4 < var3.length(); var4++) {
         var1.put(var3.getJSONObject(var4).getString("symbol"));
      }

      this.availableSymbols = var1;
      return this.availableSymbols;
   }

   public IExchange clone() {
      return new CryptoExchangeBinancePlugin();
   }

   public void download() throws Exception {
      long var5 = TimeframeManager.getMillis(this.dataInfo.timeframe);
      String var7 = this.convertTimeframe(this.dataInfo.timeframe);
      int var8 = 0;
      long var9 = this.importInfo.dateFrom > 0L ? this.importInfo.dateFrom : 0L;
      var9 = this.dataMerger.checkDate(var9);
      JsonFactory var13 = new JsonFactory();

      while (var9 <= this.importInfo.dateTo) {
         String var1 = "https://api.binance.com/api/v3/klines?symbol=" + this.dataInfo.uSymbol + "&interval=" + var7 + "&limit=1000&startTime=" + var9;
         long var14 = this.dataMerger.getPerformedDays();
         long var16 = System.currentTimeMillis();
         this.dataMerger.register(0, var9 / 1000L);
         this.dataMerger.reset(1);
         String var3 = this.doDownload(var1);
         long var11 = System.currentTimeMillis() - var16;
         JsonParser var18 = var13.createParser(var3);
         var18.nextToken();
         VersatileData var4 = null;

         while (var18.currentToken() != null) {
            var18.nextToken();
            if (var18.currentToken() == JsonToken.END_ARRAY) {
               break;
            }

            var4 = this.parseData2(var18);
            if (var4.time > this.importInfo.dateTo) {
               break;
            }

            this.dataMerger.storeToQueue(0, var4);
            var18.nextToken();
         }

         if (var4 == null) {
            break;
         }

         this.dataMerger.tryToSaveQueue(var9);
         var9 = var4.time;
         var9 += var5;
         this.checkPaused();
         if (this.canceled && !this.stopping) {
            this.importInfo.dateTo = SQTime.correctDayEnd(var9);
            this.stopping = true;
         }

         this.evaluateSpeed(var16, var14, var3.getBytes().length, var11);
         if (++var8 % 25 == 0) {
            Thread.sleep(2000L);
         }

         if (this.dataMerger.getReader() != null) {
            this.dataMerger.readTill(var9, false);
         }
      }
   }

   private VersatileData parseData2(JsonParser var1) throws IOException {
      VersatileData var2 = new VersatileData();
      var2.time = var1.nextLongValue(-1L);
      var2.open = Double.valueOf(var1.nextTextValue());
      var2.high = Double.valueOf(var1.nextTextValue());
      var2.low = Double.valueOf(var1.nextTextValue());
      var2.close = Double.valueOf(var1.nextTextValue());
      var2.volume = Double.valueOf(var1.nextTextValue());
      var1.nextToken();
      var1.nextToken();
      var1.nextToken();
      var1.nextToken();
      var1.nextToken();
      var1.nextToken();
      return var2;
   }

   private VersatileData parseData(JSONArray var1) {
      VersatileData var2 = new VersatileData();
      var2.time = var1.getLong(0);
      var2.open = var1.getDouble(1);
      var2.high = var1.getDouble(2);
      var2.low = var1.getDouble(3);
      var2.close = var1.getDouble(4);
      var2.volume = var1.getDouble(5);
      return var2;
   }

   public JSONArray availableTimeframes() {
      return new JSONArray().put("M1").put("M3").put("M5").put("M15").put("M30").put("H1").put("H2").put("H4").put("H6").put("H8").put("H12").put("D1");
   }

   public String convertTimeframe(String var1) throws Exception {
      String var2 = var1.substring(0, 1);
      int var3 = 0;

      try {
         var3 = Integer.parseInt(var1.substring(1));
      } catch (Exception var6) {
         throw new Exception(L.t("Invalid timeframe format. Cannot parse time units count.", new Object[]{true}));
      }

      switch (var2) {
         case "M":
            return var3 + "m";
         case "H":
            return var3 + "h";
         case "D":
            return var3 + "d";
         default:
            throw new Exception(L.t("Invalid timeframe format. Prefix '%s' not recognized.", new Object[]{var2}));
      }
   }

   public boolean checkSymbolExists(String var1) {
      try {
         String var2 = new String(HttpApacheManager.getInstance().getData("https://api.binance.com/api/v1/ticker/price?symbol=" + var1));
         JSONObject var3 = new JSONObject(var2);
         var3.getString("price");
         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   public SymbolInfo getSymbolInfo(String var1) {
      String var2;
      try {
         var2 = new String(HttpApacheManager.getInstance().getData("https://api.binance.com/api/v3/exchangeInfo"));
      } catch (IOException var6) {
         throw new RuntimeException("Error while fetching symbol info", var6);
      }

      JSONObject var3 = new JSONObject(var2);
      JSONArray var4 = var3.getJSONArray("symbols");
      return this.findSymbolInfo(var4, var1);
   }

   private SymbolInfo findSymbolInfo(JSONArray var1, String var2) {
      SymbolInfo var3 = new SymbolInfo();

      for (int var4 = 0; var4 < var1.length(); var4++) {
         JSONObject var5 = var1.getJSONObject(var4);
         if (var2.equals(var5.getString("symbol"))) {
            this.fillSymbolInfo(var3, var5);
            break;
         }
      }

      return var3;
   }

   private void fillSymbolInfo(SymbolInfo var1, JSONObject var2) {
      JSONArray var3 = var2.getJSONArray("filters");

      for (int var4 = 0; var4 < var3.length(); var4++) {
         JSONObject var5 = var3.getJSONObject(var4);
         String var6 = var5.getString("filterType");
         if ("PRICE_FILTER".equalsIgnoreCase(var6)) {
            var1.setTickSize(Double.valueOf(var5.getString("tickSize")));
         } else if ("LOT_SIZE".equalsIgnoreCase(var6)) {
            var1.setTickStep(Double.valueOf(var5.getString("stepSize")));
         }
      }
   }
}
