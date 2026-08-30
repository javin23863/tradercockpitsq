package com.strategyquant.plugin.DataManager.impl.Data.csvexport;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.format.Format;

public class CsvExportJob extends GridJob<Void> {
   private CsvExporter exporter = new CsvExporter();
   private IProgressListener listener;
   private String connection;
   private String symbol;
   private String timeframe;
   private String session;
   private String filePath;
   private long fromDate;
   private long toDate;
   private Format format;
   private String targetTimezone;

   public CsvExportJob(
      String var1, IProgressListener var2, String var3, String var4, String var5, String var6, String var7, long var8, long var10, Format var12, String var13
   ) {
      super(var1, 0, null);
      this.listener = var2;
      this.connection = var3;
      this.symbol = var4;
      this.timeframe = var5;
      this.session = var6;
      this.filePath = var7;
      this.fromDate = var8;
      this.toDate = var10;
      this.format = var12;
      this.targetTimezone = var13;
   }

   public Void call() throws Exception {
      this.listener.setMessage(L.t("Starting Export to CSV...", new Object[0]));
      this.exporter
         .exportData(
            this.listener,
            this.connection,
            this.symbol,
            this.timeframe,
            this.session,
            this.filePath,
            this.fromDate,
            this.toDate,
            this.format,
            this.targetTimezone
         );
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
