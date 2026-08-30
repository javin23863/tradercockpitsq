package com.strategyquant.tradinglib.project.websocket;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManagerAddProgressSender extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(DataManagerAddProgressSender.class);
   private DataToSend toSend;
   private JSONObject data = null;
   private static DataManagerAddProgressSender instance;

   public static DataManagerAddProgressSender getInstance() {
      if (instance == null) {
         instance = new DataManagerAddProgressSender();
      }

      return instance;
   }

   private DataManagerAddProgressSender() {
      this.toSend = new DataToSend();
      this.toSend.setName("DMDataAdd");
      SQWebSocketManager.getInstance().addAppPublisher("SQMANAGER", this);
   }

   @Override
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

   @Override
   public void resetLastData() {
   }
}
