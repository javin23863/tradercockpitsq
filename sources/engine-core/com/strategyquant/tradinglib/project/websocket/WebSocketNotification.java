package com.strategyquant.tradinglib.project.websocket;

import org.json.JSONObject;

public class WebSocketNotification {
   public static void send(String var0, String var1, String... var2) {
      JSONObject var3 = new JSONObject();
      var3.put("title", var0);
      var3.put("message", var1);
      SQWebSocketManager.addToDataQueue(new DataToSend("notification", var3), var2);
   }

   public static void sendError(String var0, String... var1) {
      JSONObject var2 = new JSONObject();
      var2.put("error", var0);
      SQWebSocketManager.addToDataQueue(new DataToSend("notification", var2), var1);
   }

   public static void sendSuccess(String var0, String... var1) {
      JSONObject var2 = new JSONObject();
      var2.put("success", var0);
      SQWebSocketManager.addToDataQueue(new DataToSend("notification", var2), var1);
   }
}
