package com.strategyquant.tradinglib.jobs;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDownloadJob extends GridJob<DownloadResultDto> {
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(SimpleDownloadJob.class);
   private static final int TIMEOUT = 30;
   public static final String URL_PARAM = "url";

   public SimpleDownloadJob(String var1, int var2, Map<String, Serializable> var3) {
      super(var1, var2, var3);
   }

   public DownloadResultDto call() throws Exception {
      String var1 = (String)this.getParameters().get("url");
      RequestConfig var2 = RequestConfig.custom().setConnectTimeout(30000).setConnectionRequestTimeout(30000).setSocketTimeout(120000).build();
      CloseableHttpClient var3 = HttpClientBuilder.create().setDefaultRequestConfig(var2).build();
      HttpGet var4 = new HttpGet(var1);
      return this.doDownload(var3, var4);
   }

   protected DownloadResultDto doDownload(HttpClient var1, HttpGet var2) throws Exception {
      int var3 = -1;

      try {
         long var4 = System.nanoTime();
         HttpResponse var6 = var1.execute(var2);
         var3 = var6.getStatusLine().getStatusCode();
         if (var3 == 200) {
            long var7 = System.nanoTime() - var4;
            return this.handleSuccessDownload(var6, var7);
         }
      } catch (Exception var13) {
         Log.error("Error while downloading:" + var2.getURI(), var13);
      } finally {
         var2.releaseConnection();
      }

      return DownloadResultDto.failed(var3);
   }

   protected byte[] getResponseData(HttpResponse var1) throws IllegalStateException, IOException {
      HttpEntity var2 = var1.getEntity();
      InputStream var3 = var2.getContent();
      return IOUtils.toByteArray(var3);
   }

   protected DownloadResultDto handleSuccessDownload(HttpResponse var1, long var2) throws IllegalStateException, IOException {
      byte[] var4 = this.getResponseData(var1);
      return DownloadResultDto.success(var4, var2);
   }
}
