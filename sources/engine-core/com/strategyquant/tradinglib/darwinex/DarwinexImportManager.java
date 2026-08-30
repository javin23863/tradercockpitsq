package com.strategyquant.tradinglib.darwinex;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexImportManager {
   public static final Logger Log = LoggerFactory.getLogger(DarwinexImportManager.class);
   private Map<String, DarwinexDownloadJob> downloadJobs = new HashMap<>();
   private static ReentrantLock lock = new ReentrantLock();
   private static DarwinexImportManager instance;

   public static synchronized DarwinexImportManager get() {
      if (instance == null) {
         try {
            lock.lock();
            if (instance == null) {
               instance = new DarwinexImportManager();
            }
         } finally {
            lock.unlock();
         }
      }

      return instance;
   }

   private DarwinexImportManager() {
      this.prepareDownloadListener();
   }

   private void prepareDownloadListener() {
      SQGrid.getGridClient().registerMessageListener("DarwinexDownloadPerformerGroup", new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            if (var1.getMessageID() == 1) {
               String var2 = var1.getJobDetails().getJobID();
               if (var2.endsWith("cdn")) {
                  var2 = var2.substring(0, var2.length() - 4);
               }

               int var3 = var2.lastIndexOf("_");
               String var4 = var2.substring(0, var3);
               DarwinexDownloadJob var5 = null;

               try {
                  DarwinexImportManager.lock.lock();
                  var5 = DarwinexImportManager.this.downloadJobs.get(var4);
               } finally {
                  DarwinexImportManager.lock.unlock();
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
      final GridClient var6 = SQGrid.getGridClient();
      if (var6.isRegisteredMessageListener(var1)) {
         throw new RuntimeException(L.t("Downloading of symbol %s is already running.", new Object[]{var3.symbol}));
      }

      DataManagerDataProgress.get().setProgress(var3.symbol, 0, L.t("Waiting...", new Object[0]));

      try {
         var6.registerMessageListener(var1, new IGridMessageListener() {
            public void messageReceived(GridMessage var1x) {
               if (var1x.getMessageID() == 1) {
                  try {
                     DarwinexImportManager.lock.lock();
                     var6.removeMessageListener(var1);
                     DarwinexImportManager.this.downloadJobs.remove(var1);
                  } finally {
                     DarwinexImportManager.lock.unlock();
                  }

                  if (var5 != null) {
                     var5.messageReceived(var1x);
                  }
               }
            }
         });
         DarwinexDownloadJob var7 = new DarwinexDownloadJob(var2, var1, var3);
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
