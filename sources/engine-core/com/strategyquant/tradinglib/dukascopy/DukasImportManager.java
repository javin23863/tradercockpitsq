package com.strategyquant.tradinglib.dukascopy;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.dukascopy.job.DownloadDukascopySymbolJob;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DukasImportManager {
   private static final String IMPORT_FILE_DUKASCOPY_JOB = "ImportFileDukascopyJob_";
   public static final String DOWNLOAD_DUKASCOPY_SYMBOL_JOB = "DownloadDukascopySymbolJob_";
   public static final Logger Log = LoggerFactory.getLogger(DukasImportManager.class);
   private Map<String, DownloadDukascopySymbolJob> downloadJobs = new HashMap<>();
   private static ReentrantLock lock = new ReentrantLock();
   private static DukasImportManager instance;

   public static synchronized DukasImportManager get() {
      if (instance == null) {
         try {
            lock.lock();
            if (instance == null) {
               instance = new DukasImportManager();
            }
         } finally {
            lock.unlock();
         }
      }

      return instance;
   }

   private DukasImportManager() {
      this.prepareDukasDownloadListener();
   }

   private void prepareDukasDownloadListener() {
      SQGrid.getGridClient().registerMessageListener("DukascopyDownloadPerformerGroup", new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            if (var1.getMessageID() == 1) {
               String var2 = var1.getJobDetails().getJobID();
               if (var2.endsWith("cdn")) {
                  var2 = var2.substring(0, var2.length() - 4);
               }

               int var3 = var2.lastIndexOf("_");
               String var4 = var2.substring(0, var3);
               DownloadDukascopySymbolJob var5 = null;

               try {
                  DukasImportManager.lock.lock();
                  var5 = DukasImportManager.this.downloadJobs.get(var4);
               } finally {
                  DukasImportManager.lock.unlock();
               }

               if (var5 == null) {
                  return;
               }

               var5.handleDownloadedMessage(var1);
            }
         }
      });
   }

   public GridJob importData(final String var1, String var2, ImportInfo var3, DataManagerProgressListener var4, final IGridMessageListener var5) {
      if (SQGrid.getGridClient().countRunningJobs("ImportFileDukascopyJob_" + var3.symbol) != 0L) {
         throw new RuntimeException(L.t("Import from TickDownloader is already running.", new Object[0]));
      }

      final GridClient var6 = SQGrid.getGridClient();
      if (var6.isRegisteredMessageListener(var1)) {
         throw new RuntimeException(L.t("Downloading of symbol '%s' is already running.", new Object[]{var3.symbol}));
      }

      DataManagerDataProgress.get().setProgress(var3.symbol, 0, L.t("Waiting...", new Object[0]));

      try {
         var6.registerMessageListener(var1, new IGridMessageListener() {
            public void messageReceived(GridMessage var1x) {
               if (var1x.getMessageID() == 1) {
                  try {
                     DukasImportManager.lock.lock();
                     var6.removeMessageListener(var1);
                     DukasImportManager.this.downloadJobs.remove(var1);
                  } finally {
                     DukasImportManager.lock.unlock();
                  }

                  if (var5 != null) {
                     var5.messageReceived(var1x);
                  }
               }
            }
         });
         DownloadDukascopySymbolJob var7 = new DownloadDukascopySymbolJob(var2, var1, var3);
         var7.setProgressListener(var4);

         try {
            lock.lock();
            this.downloadJobs.put(var1, var7);
         } finally {
            lock.unlock();
         }

         return var7;
      } catch (Exception var12) {
         Log.error("", var12);
         if (var4 != null) {
            var4.onError("Error - " + var12.getMessage());
         }

         throw new RuntimeException(var12);
      }
   }
}
