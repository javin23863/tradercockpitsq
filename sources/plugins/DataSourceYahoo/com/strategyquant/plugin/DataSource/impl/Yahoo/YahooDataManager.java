package com.strategyquant.plugin.DataSource.impl.Yahoo;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.yahoo.YahooHelper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahooDataManager {
   public static final Logger Log = LoggerFactory.getLogger(YahooDataManager.class);
   private static YahooDataManager instance;

   public static synchronized YahooDataManager get() {
      if (instance == null) {
         instance = new YahooDataManager();
      }

      return instance;
   }

   public String addData(String var1, String var2, byte var3, String var4, String var5) throws Exception {
      double var6 = 1.0;
      double var8 = 0.01;
      double var10 = 0.01;
      double var12 = 0.0;
      double var14 = 0.0;
      String var16 = var1 + var2;
      String var17 = var1;

      while (DataManager.checkDataExists("History", var16)) {
         var16 = DataManager.generateName(var16);
      }

      if (!InstrumentManager.checkInstrumentExists(var17)) {
         InstrumentManager.addInstrument(
            var17,
            "Yahoo instrument",
            var6,
            var8,
            var10,
            var12,
            var14,
            "<Method type=\"None\" use=\"true\"><Params/></Method>",
            (byte)1,
            null,
            null,
            null,
            null,
            1.0,
            0.0
         );
      } else {
         InstrumentInfo var18 = InstrumentManager.getInstrumentInfo(var17);
         if (var18.tickSize != var8 || var18.tickStep != var10) {
            while (InstrumentManager.checkInstrumentExists(var17)) {
               var17 = DataManager.generateName(var17);
            }

            InstrumentManager.addInstrument(
               var17,
               "Yahoo instrument",
               var6,
               var8,
               var10,
               var12,
               var14,
               "<Method type=\"None\" use=\"true\"><Params/></Method>",
               (byte)1,
               null,
               null,
               null,
               null,
               1.0,
               0.0
            );
         }
      }

      DataManager.addData("History", var16, var17, 1, 3, var1, var4 + ", " + var5);
      return var16;
   }

   private String getCookieCrumb() throws Exception {
      String var1 = null;
      Object var2 = null;

      try {
         CloseableHttpClient var3 = HttpClientBuilder.create().build();
         HttpGet var4 = new HttpGet("https://finance.yahoo.com/quote/SPY");
         HttpResponse var5 = var3.execute(var4);
         if (var5.getStatusLine().getStatusCode() != 200) {
            throw new Exception(L.t("HTML status code - %d", new Object[]{var5.getStatusLine().getStatusCode()}));
         }

         InputStream var6 = var5.getEntity().getContent();
         InputStreamReader var7 = new InputStreamReader(var6, StandardCharsets.UTF_8);
         BufferedReader var8 = new BufferedReader(var7);
         String var9 = "\"crumb\":\"";
         String var10 = null;

         while ((var10 = var8.readLine()) != null) {
            System.out.println(var10);
            if (var10.contains(var9)) {
               int var11 = var10.indexOf("\"crumb\":\"") + var9.length();
               var1 = var10.substring(var11, var10.indexOf("\"", var11));
               break;
            }
         }

         var8.close();
      } catch (Error var12) {
         throw new Exception(L.t("Cannot get Yahoo cookie crumb.", new Object[0]), var12);
      } catch (Exception var13) {
         throw new Exception(L.t("Cannot get Yahoo cookie crumb.", new Object[0]), var13);
      }

      if (var1 == null) {
         throw new Exception(L.t("Cannot get Yahoo cookies / crumb.", new Object[0]));
      } else {
         return var1;
      }
   }

   public YahooSymbol downloadInfo(String var1) throws Exception {
      YahooSymbol var2 = new YahooSymbol();
      var2.symbol = var1;
      String var3 = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + URLEncoder.encode(var1);

      String var4;
      try {
         YahooHelper var5 = new YahooHelper();
         var4 = var5.download(var3);
      } catch (Exception var12) {
         throw new Exception(L.t("Symbol '%s' not found.", new Object[]{var1}));
      }

      try {
         JSONObject var13 = new JSONObject(var4);
         JSONObject var6 = var13.getJSONObject("quoteResponse");
         JSONArray var7 = var6.getJSONArray("result");
         if (var7.length() == 0) {
            throw new Exception(L.t("Symbol '%s' not found.", new Object[]{var1}));
         }

         JSONObject var8 = var7.getJSONObject(0);

         try {
            var2.name = var8.getString("shortName");
         } catch (Exception var10) {
         }

         var2.exchange = var8.getString("exchange");
         return var2;
      } catch (Exception var11) {
         Log.error("Cannot parse symbol '" + var1 + "' details from: " + var4, var11);
         throw new Exception(L.t("Symbol '%s' not found.", new Object[]{var1}));
      }
   }
}
