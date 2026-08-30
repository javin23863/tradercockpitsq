package com.strategyquant.plugin.CryptoExchange.impl.CoinbasePro;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Takacs")
@Name(name = "Coinbase")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "Server API for managing Coinbase Pro data")
@PluginImplementation
public class CryptoExchangeCoinbaseProPlugin extends Exchange {
   private JSONArray availableSymbols = null;

   public CryptoExchangeCoinbaseProPlugin() {
      (new Thread() {
         @Override
         public void run() {
            try {
               CryptoExchangeCoinbaseProPlugin.this.getSymbols();
            } catch (Exception var2) {
            }
         }
      }).start();
   }

   public String getName() {
      return "Coinbase Pro";
   }

   public JSONArray getSymbols() throws Exception {
      if (this.availableSymbols != null) {
         return this.availableSymbols;
      }

      String var1 = new String(HttpApacheManager.getInstance().getData("https://api.exchange.coinbase.com/products"));
      JSONArray var2 = new JSONArray();
      JSONArray var3 = new JSONArray(var1);

      for (int var4 = 0; var4 < var3.length(); var4++) {
         JSONObject var5 = var3.getJSONObject(var4);
         boolean var6 = var5.getBoolean("trading_disabled");
         if (!var6) {
            var2.put(var5.getString("id"));
         }
      }

      this.availableSymbols = var2;
      return this.availableSymbols;
   }

   public IExchange clone() {
      return new CryptoExchangeCoinbaseProPlugin();
   }

   private long getEarliestTime() throws Exception {
      long var1 = 0L;
      long var3 = 1000L;
      long var5 = TimeframeManager.getMillis("D290");

      while (var1 == 0L) {
         long[] var7 = this.getFutureEarliestAndLargestTime(var3);
         if (var7[0] > 0L) {
            var1 = var7[0];
         }

         var3 += var5;
         Thread.sleep(333L);
      }

      return var1;
   }

   private long[] getFutureEarliestAndLargestTime(long var1) throws Exception {
      SimpleDateFormat var3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      long var4 = TimeframeManager.getMillis("D290");
      long var6 = 0L;
      long var8 = 0L;
      Date var10 = new Date();
      var10.setTime(var1);
      String var11 = "https://api.exchange.coinbase.com/products/" + this.dataInfo.uSymbol + "/candles?granularity=86400&start=" + var3.format(var10);
      var10.setTime(var1 + var4);
      var11 = var11 + "&end=" + var3.format(var10);
      String var12 = this.doDownload(var11);
      JSONArray var13 = new JSONArray(var12);
      if (var13.length() > 0) {
         VersatileData var14 = this.parseData(var13.getJSONArray(var13.length() - 1));
         VersatileData var15 = this.parseData(var13.getJSONArray(0));
         var6 = var14.time;
         var8 = var15.time;
      }

      return new long[]{var6, var8};
   }

   public void download() throws Exception {
      String var3 = null;
      long var5 = 0L;
      long var7 = 0L;
      long var9 = TimeframeManager.getMillis(this.dataInfo.timeframe);
      String var11 = this.convertTimeframe(this.dataInfo.timeframe);
      long var12 = this.importInfo.dateFrom > 0L ? this.importInfo.dateFrom : this.getEarliestTime();
      var12 = this.dataMerger.checkDate(var12);
      Date var14 = new Date();
      SimpleDateFormat var15 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      long var16 = 0L;
      String var20 = System.getProperty("https.protocols");
      System.setProperty("https.protocols", "TLSv1.2");

      while (var12 <= this.importInfo.dateTo) {
         var14.setTime(var12);
         String var1 = "https://api.exchange.coinbase.com/products/"
            + this.dataInfo.uSymbol
            + "/candles?granularity="
            + var11
            + "&start="
            + var15.format(var14);
         long var21 = this.dataMerger.getPerformedDays();
         long var23 = System.currentTimeMillis();
         this.dataMerger.register(0, var12 / 1000L);
         this.dataMerger.reset(1);
         var14.setTime(var12 + 299L * var9);
         var1 = var1 + "&end=" + var15.format(var14);

         try {
            var3 = this.doDownload(var1);
         } catch (Exception var26) {
            if (var26.getMessage().contains("429")) {
               throw new Exception(L.t("Rate limit exceeded.", new Object[0]));
            }

            throw var26;
         }

         JSONArray var2 = new JSONArray(var3);
         if (var2.length() == 0) {
            Thread.sleep(333L);
            long[] var25 = this.getFutureEarliestAndLargestTime(var12 + 299L * var9);
            var7 = var25[0];
            var5 = var25[1];
            if (var12 > var5) {
               break;
            }

            var12 = var7 - 1000L;
         } else {
            long var18 = System.currentTimeMillis() - var23;

            for (int var33 = var2.length() - 1; var33 >= 0; var33--) {
               VersatileData var4 = this.parseData(var2.getJSONArray(var33));
               if (var16 == 0L && var4.time > this.importInfo.dateTo) {
                  Log.warn(
                     "Date from for this cryptocurrency is not correct, this exchange doesn't have history this long. Please enter the correct date from."
                  );
               } else {
                  if (var4.time > this.importInfo.dateTo) {
                     break;
                  }

                  this.dataMerger.storeToQueue(0, var4);
               }
            }

            this.dataMerger.tryToSaveQueue(var12);
            var12 = var2.getJSONArray(0).getLong(0) * 1000L;
            var12 += var9;
            var16++;
            this.evaluateSpeed(var23, var21, var3.getBytes().length, var18);
            if (this.dataMerger.getReader() != null) {
               this.dataMerger.readTill(var12, false);
            }
         }

         if (this.canceled && !this.stopping) {
            this.importInfo.dateTo = SQTime.correctDayEnd(var12);
            this.stopping = true;
         }

         this.checkPaused();
         Thread.sleep(333L);
      }

      if (var20 == null) {
         System.clearProperty("https.protocols");
      } else {
         System.setProperty("https.protocols", var20);
      }
   }

   private VersatileData parseData(JSONArray var1) {
      VersatileData var2 = new VersatileData();
      var2.time = var1.getLong(0) * 1000L;
      var2.low = var1.getDouble(1);
      var2.high = var1.getDouble(2);
      var2.open = var1.getDouble(3);
      var2.close = var1.getDouble(4);
      var2.volume = var1.getDouble(5);
      return var2;
   }

   public JSONArray availableTimeframes() {
      return new JSONArray().put("M1").put("M5").put("M15").put("H1").put("H6").put("D1");
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
            return String.valueOf(var3 * 60);
         case "H":
            return String.valueOf(var3 * 3600);
         case "D":
            return String.valueOf(var3 * 86400);
         default:
            throw new Exception(L.t("Invalid timeframe format. Prefix '%s' not recognized.", new Object[]{var2}));
      }
   }

   public boolean checkSymbolExists(String var1) {
      String var2 = System.getProperty("https.protocols");

      try {
         System.setProperty("https.protocols", "TLSv1.2");
         String var3 = new String(HttpApacheManager.getInstance().getData("https://api.exchange.coinbase.com/products/" + var1 + "/ticker"));
         JSONObject var4 = new JSONObject(var3);
         var4.getString("price");
      } catch (Exception var8) {
         return false;
      } finally {
         if (var2 == null) {
            System.clearProperty("https.protocols");
         } else {
            System.setProperty("https.protocols", var2);
         }
      }

      return true;
   }

   public SymbolInfo getSymbolInfo(String var1) {
      String var2 = "https://api.exchange.coinbase.com/products/" + var1;

      String var3;
      try {
         var3 = this.doDownload(var2);
      } catch (Exception var7) {
         throw new RuntimeException("Error while downloading symbol info", var7);
      }

      JSONObject var4 = new JSONObject(var3);
      String var5 = var4.getString("quote_increment");
      SymbolInfo var6 = new SymbolInfo();
      var6.setTickSize(Double.valueOf(var5));
      var6.setTickStep(Double.valueOf(var5));
      return var6;
   }
}
