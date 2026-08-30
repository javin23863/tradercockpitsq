package com.strategyquant.tradinglib.historyData;

import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpApacheManager {
   private static final Logger Log = LoggerFactory.getLogger(HttpApacheManager.class);
   private static HttpApacheManager INSTANCE = new HttpApacheManager();
   private boolean ignoreSSL = false;

   public static HttpApacheManager getInstance() {
      return INSTANCE;
   }

   private HttpApacheManager() {
      File var1 = new File(MainApp.getDataPath() + "/ignoressl.txt");
      Log.debug("Checking file in location: {}" + var1.getAbsolutePath());
      this.ignoreSSL = var1.exists();
      Log.debug("Initializing http manager with strict mode: " + !this.ignoreSSL);
   }

   public HttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
      return this.getClient(null);
   }

   public HttpClient getClient(Integer var1) {
      try {
         HttpClientBuilder var2 = HttpClientBuilder.create();
         if (this.ignoreSSL) {
            SSLContextBuilder var3 = new SSLContextBuilder();
            var3.loadTrustMaterial(null, (var0, var1x) -> true);
            SSLConnectionSocketFactory var4 = new SSLConnectionSocketFactory(var3.build(), NoopHostnameVerifier.INSTANCE);
            var2 = HttpClients.custom().setSSLSocketFactory(var4);
         }

         if (var1 == null) {
            var1 = 40;
         }

         RequestConfig var6 = RequestConfig.custom()
            .setConnectTimeout(var1 * 1000)
            .setConnectionRequestTimeout(var1 * 1000)
            .setSocketTimeout(4 * var1 * 1000)
            .build();
         var2.setDefaultRequestConfig(var6);
         return var2.build();
      } catch (Exception var5) {
         throw new RuntimeException("Error while creating http client", var5);
      }
   }

   public byte[] getData(String var1) throws ClientProtocolException, IOException {
      return this.getData(var1, null);
   }

   public byte[] getData(String var1, Integer var2) throws ClientProtocolException, IOException {
      CloseableHttpClient var3 = (CloseableHttpClient)this.getClient(var2);

      byte[] var8;
      try {
         CloseableHttpResponse var4 = var3.execute(new HttpGet(var1));
         InputStream var5 = var4.getEntity().getContent();

         try {
            int var6 = var4.getStatusLine().getStatusCode();
            byte[] var7 = IOUtils.toByteArray(var5);
            if (var6 >= 400) {
               throw new HttpApacheManager.HttpException("Request error: GET, " + var1 + " - status code:" + var6, var1, var6, var7);
            }

            var8 = var7;
         } catch (Throwable var11) {
            if (var5 != null) {
               try {
                  var5.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (var5 != null) {
            var5.close();
         }
      } catch (Throwable var12) {
         if (var3 != null) {
            try {
               var3.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }
         }

         throw var12;
      }

      if (var3 != null) {
         var3.close();
      }

      return var8;
   }

   public static class HttpException extends RuntimeException {
      private static final long serialVersionUID = 1L;
      private int code;
      private byte[] resultBytes;
      private String url;

      public HttpException(String var1, String var2, int var3, byte[] var4) {
         super(var1);
         this.url = var2;
         this.code = var3;
         this.resultBytes = var4;
      }

      public String getUrl() {
         return this.url;
      }

      public int getCode() {
         return this.code;
      }

      public byte[] getResultBytes() {
         return this.resultBytes;
      }
   }
}
