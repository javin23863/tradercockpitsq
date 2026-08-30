package com.strategyquant.plugin.DataSource.impl.TD;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.plugin.DataSource.impl.TD.job.ImportFileJob;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TDDataManager {
   public static final Logger Log = LoggerFactory.getLogger(TDDataManager.class);
   private static final String IMPORT_TDDATA_JOB = "ImportTDDataJob_";
   private static TDDataManager instance;

   public static synchronized TDDataManager get() {
      if (instance == null) {
         instance = new TDDataManager();
      }

      return instance;
   }

   public void stopTDImport(String var1) {
      String var2 = "ImportTDDataJob_" + var1;
      SQGrid.getGridClient().stop(var2);
   }

   public void pauseTDImport(String var1) {
      String var2 = "ImportTDDataJob_" + var1;
      SQGrid.getGridClient().pause(var2);
   }

   public void restartTDImport(String var1) {
      String var2 = "ImportTDDataJob_" + var1;
      SQGrid.getGridClient().restart(var2);
   }

   public void importTDData(ImportInfo var1, String var2, DataManagerProgressListener var3) {
      try {
         if (SQGrid.getGridClient().countRunningJobs("ImportTDDataJob_" + var1.symbol) != 0L) {
            throw new RuntimeException(L.t("Import from Dukascopy is already running.", new Object[0]));
         }

         DataManagerDataProgress.get().setProgress(var1.symbol, 0, L.t("Waiting...", new Object[0]));
         String var4 = "ImportTDDataJob_" + var1.symbol;
         ImportFileJob var5 = new ImportFileJob(var1, var2, var4);
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
