package com.strategyquant.tradinglib.yahoo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahooHelper {
   private static final String CRUMB_URL = "https://query2.finance.yahoo.com/v1/test/getcrumb";
   public static final Logger Log = LoggerFactory.getLogger(YahooHelper.class);
   private Random random = new Random();
   private String[] AGENTS = new String[]{
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0"
   };

   public String getCrumb(String var1, String var2) throws ClientProtocolException, IOException {
      return this.doDownloadWithCookies("https://query2.finance.yahoo.com/v1/test/getcrumb", var1, var2);
   }

   public String download(String var1) throws Exception {
      int var2 = this.random.nextInt(this.AGENTS.length);
      String var3 = this.AGENTS[var2];
      String var4 = this.getCookies(var3, var2);
      String var5 = this.getCrumb(var3, var4);
      var1 = var1 + "&crumb=" + var5;
      return this.doDownloadWithCookies(var1, var3, var4);
   }

   private String getCookies(String var1, int var2) throws Exception {
      try {
         CloseableHttpClient var3;
         label55: {
            var3 = HttpClientBuilder.create().build();

            String var12;
            try {
               HttpGet var4 = new HttpGet("https://fc.yahoo.com");
               var4.addHeader("User-Agent", var1);
               CloseableHttpResponse var5 = var3.execute(var4);
               Header[] var6 = var5.getAllHeaders();
               int var7 = var6.length;
               int var8 = 0;

               while (true) {
                  if (var8 >= var7) {
                     break label55;
                  }

                  Header var9 = var6[var8];
                  if (var9.getName().equalsIgnoreCase("Set-Cookie")) {
                     String var10 = var9.getValue();
                     String[] var11 = var10.split(";");
                     var12 = var11[0];
                     break;
                  }

                  var8++;
               }
            } catch (Throwable var14) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }
               }

               throw var14;
            }

            if (var3 != null) {
               var3.close();
            }

            return var12;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Exception var15) {
         Log.error("Error while getting cookie", var15);
         throw new Exception("Error while getting cookie");
      }

      throw new Exception("Error while getting cookie");
   }

   private String doDownloadWithCookies(String var1, String var2, String var3) throws IOException, ClientProtocolException {
      CloseableHttpClient var4 = HttpClientBuilder.create().build();

      String var11;
      try {
         HttpGet var5 = new HttpGet(var1);
         var5.addHeader("Cookie", var3);
         var5.addHeader("User-Agent", var2);
         CloseableHttpResponse var6 = var4.execute(var5);
         HttpEntity var7 = var6.getEntity();
         int var8 = var6.getStatusLine().getStatusCode();
         if (var8 != 200) {
            Log.error("Error while downloading: " + var1 + ". Error code:" + var8);
         }

         InputStream var9 = var7.getContent();
         byte[] var10 = IOUtils.toByteArray(var9);
         var11 = new String(var10);
      } catch (Throwable var13) {
         if (var4 != null) {
            try {
               var4.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (var4 != null) {
         var4.close();
      }

      return var11;
   }
}
