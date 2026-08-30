package com.strategyquant.tradinglib.project.websocket;

public interface WebSocketPublisher {
   DataToSend getDataToSend() throws Exception;

   void confirmBroadcast();

   void resetLastData();
}
