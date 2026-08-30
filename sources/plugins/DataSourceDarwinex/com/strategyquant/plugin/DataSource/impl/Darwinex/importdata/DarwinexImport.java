package com.strategyquant.plugin.DataSource.impl.Darwinex.importdata;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexImport {
   public static final Logger Log = LoggerFactory.getLogger(DarwinexImport.class);
   private static final String IMPORT_DARWINEX_JOB = "DarwinexImportJob_";
   private static DarwinexImport instance;

   public static synchronized DarwinexImport get() {
      if (instance == null) {
         instance = new DarwinexImport();
      }

      return instance;
   }

   public void stop(String var1) {
      String var2 = "DarwinexImportJob_" + var1;
      SQGrid.getGridClient().stop(var2);
   }

   public void pause(String var1) {
      String var2 = "DarwinexImportJob_" + var1;
      SQGrid.getGridClient().pause(var2);
   }

   public void restart(String var1) {
      String var2 = "DarwinexImportJob_" + var1;
      SQGrid.getGridClient().restart(var2);
   }

   public void importData(ImportInfo var1, String var2, DataManagerProgressListener var3) {
      try {
         if (SQGrid.getGridClient().countRunningJobs("DarwinexImportJob_" + var1.symbol) != 0L) {
            throw new RuntimeException(L.t("Import from Darwinex is already running.", new Object[0]));
         }

         DataManagerDataProgress.get().setProgress(var1.symbol, 0, L.t("Waiting...", new Object[0]));
         String var4 = "DarwinexImportJob_" + var1.symbol;
         DarwinexImportJob var5 = new DarwinexImportJob(var1, var2, var4);
         if (var3 != null) {
            var5.setProgressListener(var3);
         }

         ArrayList var6 = new ArrayList(1);
         var6.add(var5);
         GridClient var7 = SQGrid.getGridClient();
         var7.executeOnGrid(var4, var6);
      } catch (Exception var8) {
         Log.error("", var8);
         if (var3 != null) {
            var3.onError("Error - " + var8.getMessage());
         }

         throw new RuntimeException(var8);
      }
   }
}
