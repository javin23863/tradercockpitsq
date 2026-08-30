package com.strategyquant.plugin.DataManager.impl.Data.job;

import com.strategyquant.datalib.metatrader4.Mt4SymbolProperties;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.mt4.Mt4Exporter;

public class MT4ExportJob extends GridJob<Void> {
   private Mt4Exporter exporter;
   private IProgressListener listener;

   public MT4ExportJob(
      String var1,
      String var2,
      String var3,
      String var4,
      long var5,
      long var7,
      String var9,
      String var10,
      Mt4SymbolProperties var11,
      String var12,
      String var13,
      String var14,
      IProgressListener var15
   ) {
      super(var1, 0, null);
      this.listener = var15;
      this.exporter = new Mt4Exporter(var2, var3, var4, var5, var7, var9, var10, var11, var12, var13, var14, var15);
   }

   public Void call() throws Exception {
      this.listener.setMessage(L.t("Starting export to MT4...", new Object[0]));
      this.listener.onProgress(1.0);
      this.exporter.export();
      this.listener.onProgress(100.0);
      this.listener.setMessage(this.exporter.isCanceled() ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      return null;
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.exporter.pause();
            break;
         case 3:
            this.exporter.restart();
            break;
         case 4:
            this.exporter.cancel();
      }
   }
}
