package com.strategyquant.plugin.DataManager.impl.Connections;

import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionInfoSender extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(ConnectionInfoSender.class);
   private static ConnectionInfoSender instance;
   private boolean stop = true;

   public ConnectionInfoSender() {
      SQWebSocketManager.getInstance().addAppPublisher("SQMANAGER", this);
      instance = this;
   }

   public static ConnectionInfoSender getInstance() {
      if (instance == null) {
         new ConnectionInfoSender();
      }

      return instance;
   }

   public void start() {
      this.stop = false;
   }

   public void stop() {
      this.stop = true;
   }

   public DataToSend getData() {
      return !this.stop ? WSDataObjects.getConnections() : null;
   }

   public void resetLastData() {
   }
}
