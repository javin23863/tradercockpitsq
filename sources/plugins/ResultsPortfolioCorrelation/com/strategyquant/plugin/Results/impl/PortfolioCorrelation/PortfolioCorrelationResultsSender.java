package com.strategyquant.plugin.Results.impl.PortfolioCorrelation;

import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioCorrelationResultsSender extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioCorrelationResultsSender.class);
   private DataToSend toSend;
   private JSONObject data = null;

   public PortfolioCorrelationResultsSender() {
      this.toSend = new DataToSend();
      this.toSend.setName("portfolioCorrelation");
      SQWebSocketManager.getInstance().addAppPublisher("RESULTS", this);
   }

   public DataToSend getData() {
      if (this.data != null) {
         this.toSend.setDataObject(new JSONObject(this.data.toString()));
         this.data = null;
         return this.toSend;
      } else {
         return null;
      }
   }

   public void sendData(JSONObject var1) {
      this.data = var1;
   }

   public void resetLastData() {
   }

   public void progress(int var1) {
      if (this.data == null) {
         this.data = new JSONObject();
      }

      this.data.put("progress", var1);
   }
}
