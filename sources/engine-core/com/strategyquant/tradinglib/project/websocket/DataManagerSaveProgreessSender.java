package com.strategyquant.tradinglib.project.websocket;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManagerSaveProgreessSender extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(DataManagerSaveProgreessSender.class);
   private DataToSend toSend;
   private JSONObject data = null;
   private static DataManagerSaveProgreessSender instance;

   public static DataManagerSaveProgreessSender getInstance() {
      if (instance == null) {
         instance = new DataManagerSaveProgreessSender();
      }

      return instance;
   }

   private DataManagerSaveProgreessSender() {
      this.toSend = new DataToSend();
      this.toSend.setName("DMDataSave");
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
