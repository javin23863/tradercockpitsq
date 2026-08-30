package com.strategyquant.plugin.CryptoExchange.impl.BinanceCoinM;

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
import java.time.Duration;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;

@Name(name = "BinanceCoinM")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing Binance Coin-M data")
@PluginImplementation
public class CryptoExchangeBinanceCoinMPlugin extends Exchange {
   private static final long MAX_BARS_PER_REQUEST = 1500L;
   private static final long MAX_RANGE_MS = Duration.ofDays(200L).toMillis();
   private String exchangeInfo;
   private JSONArray availableSymbols = null;

   public CryptoExchangeBinanceCoinMPlugin() {
      (new Thread() {
         @Override
         public void run() {
            try {
               CryptoExchangeBinanceCoinMPlugin.this.getSymbols();
            } catch (Exception var2) {
            }
         }
      }).start();
   }

   public String getName() {
      return "Binance Coin-M";
   }

   public JSONArray getSymbols() throws Exception {
      if (this.availableSymbols != null) {
         return this.availableSymbols;
      }

      JSONArray var1 = new JSONArray();
      String var2 = new String(HttpApacheManager.getInstance().getData("https://dapi.binance.com/dapi/v1/exchangeInfo"));
      JSONObject var3 = new JSONObject(var2);
      JSONArray var4 = var3.getJSONArray("symbols");

      for (int var5 = 0; var5 < var4.length(); var5++) {
         var1.put(var4.getJSONObject(var5).getString("symbol"));
      }

      this.availableSymbols = var1;
      return this.availableSymbols;
   }

   public IExchange clone() {
      return new CryptoExchangeBinanceCoinMPlugin();
   }

   public void download() throws Exception {
      long var5 = TimeframeManager.getMillis(this.dataInfo.timeframe);
      String var7 = this.convertTimeframe(this.dataInfo.timeframe);
      int var8 = 0;
      long var9 = this.importInfo.dateFrom > 0L ? this.importInfo.dateFrom : this.getSymbolInfo(this.dataInfo.uSymbol).getDateFrom();
      var9 = this.dataMerger.checkDate(var9);
      long var13 = 1500L * var5;
      long var15 = Math.min(var13, MAX_RANGE_MS);
      JsonFactory var17 = new JsonFactory();

      while (var9 <= this.importInfo.dateTo) {
         long var18 = var9 + var15;
         String var1 = "https://dapi.binance.com/dapi/v1/klines?symbol="
            + this.dataInfo.uSymbol
            + "&interval="
            + var7
            + "&limit=1500&"
            + this.getStartEndParams(var9, var18);
         long var20 = this.dataMerger.getPerformedDays();
         long var22 = System.currentTimeMillis();
         this.dataMerger.register(0, var9 / 1000L);
         this.dataMerger.reset(1);
         String var3 = this.doDownload(var1);
         long var11 = System.currentTimeMillis() - var22;
         JsonParser var24 = var17.createParser(var3);
         var24.nextToken();
         VersatileData var4 = null;

         while (var24.currentToken() != null) {
            var24.nextToken();
            if (var24.currentToken() == JsonToken.END_ARRAY) {
               break;
            }

            var4 = this.parseData2(var24);
            if (var4.time > this.importInfo.dateTo) {
               break;
            }

            this.dataMerger.storeToQueue(0, var4);
            var24.nextToken();
         }

         if (var4 == null) {
            break;
         }

         this.dataMerger.tryToSaveQueue(var9);
         var9 = var4.time + var5;
         this.checkPaused();
         if (this.canceled && !this.stopping) {
            this.importInfo.dateTo = SQTime.correctDayEnd(var9);
            this.stopping = true;
         }

         this.evaluateSpeed(var22, var20, var3.getBytes().length, var11);
         if (++var8 % 25 == 0) {
            Thread.sleep(2000L);
         }

         if (this.dataMerger.getReader() != null) {
            this.dataMerger.readTill(var9, false);
         }
      }
   }

   private String getStartEndParams(long var1, long var3) {
      return var3 >= System.currentTimeMillis() ? "startTime=" + var1 : "startTime=" + var1 + "&endTime=" + var3;
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
      return true;
   }

   private synchronized String getExchangeInfo() throws ClientProtocolException, IOException {
      if (this.exchangeInfo != null) {
         return this.exchangeInfo;
      }

      this.exchangeInfo = new String(HttpApacheManager.getInstance().getData("https://dapi.binance.com/dapi/v1/exchangeInfo"));
      return this.exchangeInfo;
   }

   public SymbolInfo getSymbolInfo(String var1) {
      String var2;
      try {
         var2 = this.getExchangeInfo();
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
            var1.setTickStep(var1.getTickSize());
         }
      }

      var1.setDateFrom(var2.getLong("onboardDate"));
   }
}
