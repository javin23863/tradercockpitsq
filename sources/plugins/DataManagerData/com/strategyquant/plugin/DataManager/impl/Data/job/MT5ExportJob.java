package com.strategyquant.plugin.DataManager.impl.Data.job;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT5ExportJob extends GridJob<Void> {
   private static final Logger LOG = LoggerFactory.getLogger(MT5ExportJob.class);
   private MT5Exporter exporter = new MT5Exporter();
   private IProgressListener listener;
   private String connection;
   private String symbol;
   private String timeframe;
   private String filePath;
   private long fromDate;
   private long toDate;
   private String spreadType;
   private double spreadPips;
   private double spreadPoints;
   private String targetTimezone;

   public MT5ExportJob(
      String var1,
      IProgressListener var2,
      String var3,
      String var4,
      String var5,
      String var6,
      double var7,
      double var9,
      String var11,
      long var12,
      long var14,
      String var16
   ) {
      super(var1, 0, null);
      this.listener = var2;
      this.connection = var3;
      this.symbol = var4;
      this.timeframe = var5;
      this.filePath = var11;
      this.fromDate = var12;
      this.toDate = var14;
      this.spreadType = var6;
      this.spreadPoints = var7;
      this.spreadPips = var9;
      this.targetTimezone = var16;
   }

   public Void call() throws Exception {
      try {
         this.listener.setMessage(L.t("Starting export to MT5...", new Object[0]));
         this.exporter
            .exportData(
               this.listener,
               this.connection,
               this.symbol,
               this.timeframe,
               this.spreadType,
               this.spreadPoints,
               this.spreadPips,
               this.filePath,
               this.fromDate,
               this.toDate,
               this.targetTimezone
            );
      } catch (Exception var2) {
         LOG.error("Error while exporting data", var2);
         this.listener.onError(var2.getMessage());
      }

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
