package com.strategyquant.plugin.DataManager.impl.Data.job;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneToTimezoneJob extends GridJob<Void> {
   private static final Logger Log = LoggerFactory.getLogger(CloneToTimezoneJob.class);
   private DataCloner cloner = new DataCloner();
   private IProgressListener listener;
   private volatile boolean cancel;
   private String symbol;
   private String toSymbol;
   private String toTimezone;
   private int shiftHours;
   private boolean removeWeekends;
   private TickerDto ticker;

   public CloneToTimezoneJob(String var1, String var2, String var3, String var4, int var5, boolean var6, TickerDto var7, MultiProgressListener var8) {
      super(var1, 0, null);
      this.listener = var8;
      this.symbol = var2;
      this.toSymbol = var3;
      this.toTimezone = var4;
      this.shiftHours = var5;
      this.removeWeekends = var6;
      this.ticker = var7;
   }

   public Void call() throws Exception {
      this.listener.setMessage(L.t("Starting clone...", new Object[0]));
      this.listener.onProgress(1.0);

      try {
         DataInfo var1 = DataManager.getDataInfo("History", this.symbol);
         this.cloner.createSymbol(this.toSymbol, var1);
         this.sendDataUpdate();
         this.cloner.cloneToTimezone(this.symbol, this.toSymbol, this.toTimezone, this.shiftHours, this.removeWeekends, this.ticker, this.listener);
         this.sendDataUpdate();
         this.listener.onProgress(100.0);
         this.listener.setMessage(this.cancel ? L.t("Stopped", new Object[0]) : L.t("Completed", new Object[0]));
      } catch (Exception var2) {
         Log.error("Error while performing cloning", var2);
         this.listener.onError(L.t("Error while cloning data: " + var2.getMessage(), new Object[0]));
      }

      return null;
   }

   private void sendDataUpdate() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(this.toSymbol, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var2) {
         Log.error("Cannot send websocket data update. ", var2);
      }
   }

   public void messageReceived(GridMessage var1) {
      super.messageReceived(var1);
      switch (var1.getMessageID()) {
         case 2:
            this.cloner.pause();
            break;
         case 3:
            this.cloner.restart();
            break;
         case 4:
            this.cancel = true;
            this.cloner.cancel();
      }
   }
}
