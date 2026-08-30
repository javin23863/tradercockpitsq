package com.strategyquant.tradinglib.dukascopy.job;

import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;
import com.strategyquant.tradinglib.jobs.SimpleDownloadJob;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadWithFlexibileSpeedJob extends SimpleDownloadJob {
   private static final long serialVersionUID = 1L;
   public static final Logger Log = LoggerFactory.getLogger(DownloadWithFlexibileSpeedJob.class);
   private static final int TRY_COUNT = 5;
   private static final int TRY_COUNT_503 = 20;
   private DownloadRateCorrector corrector;

   public DownloadWithFlexibileSpeedJob(String var1, int var2, Map<String, Serializable> var3, DownloadRateCorrector var4) {
      super(var1, var2, var3);
      this.corrector = var4;
   }

   @Override
   protected DownloadResultDto doDownload(HttpClient var1, HttpGet var2) throws Exception {
      int var3 = -1;
      HttpResponse var4 = null;
      int var5 = 0;
      int var6 = 0;
      int var7 = 1;
      String var8 = null;
      boolean var9 = false;
      long var10 = System.nanoTime();

      while (var5 < 5 && var6 < 20) {
         try {
            synchronized (this.corrector) {
               this.corrector.waitBeforeNextDownload();
               var4 = var1.execute(var2);
               var3 = var4.getStatusLine().getStatusCode();
               if (var3 == 503 || var3 == 500 || var3 == 504) {
                  this.corrector.downloadFailed();
                  var6++;
               } else if (var3 == 200) {
                  this.corrector.downloadSuccess();
               } else {
                  var5++;
               }
            }

            if (var3 == 200) {
               long var28 = System.nanoTime() - var10;
               return this.handleSuccessDownload(var4, var28);
            }
         } catch (InterruptedException var22) {
            var9 = true;
            break;
         } catch (UnknownHostException var23) {
            Log.error("Unknown host: " + var2.getURI() + ". Try " + var7, var23);
            var8 = "The server address could not be resolved. This may be due to a DNS or network issue.";
            var5++;
         } catch (Exception var24) {
            Log.error("Error while downloading:" + var2.getURI() + ". Try " + var7, var24);
            var5++;
         } finally {
            var2.releaseConnection();
            var7++;
         }
      }

      if (!var9) {
         Log.error("Download failed:" + var2.getURI());
      }

      DownloadResultDto var29 = DownloadResultDto.failed(var3);
      var29.setErrorMessage(var8);
      return var29;
   }
}
