package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.pluginlib.program.Program;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 1048576)
public class SQWebSocket {
   private static final Logger Log = LoggerFactory.getLogger(SQWebSocket.class);
   private final CountDownLatch closeLatch;
   private Session session;
   private String productName;

   public SQWebSocket() {
      if (Log.isDebugEnabled()) {
         Log.debug("WebSocket initialized 1.");
      }

      this.closeLatch = new CountDownLatch(1);
   }

   @OnWebSocketConnect
   public void onConnect(Session var1) {
      String var2 = var1.getRemoteAddress().toString();
      if (Log.isDebugEnabled()) {
         Log.debug("Incoming connection from {}, protocol version: {}", var2, var1.getProtocolVersion());
      }

      this.session = var1;
   }

   public String getUniqueId() {
      return this.productName;
   }

   @OnWebSocketMessage
   public void onMessage(String var1) {
      Log.debug("Got message: '" + var1 + "'");
      this.processAction(new JSONObject(var1));
   }

   @OnWebSocketError
   public void onError(Session var1, Throwable var2) {
      Log.debug("Websocket error", var2);
   }

   @OnWebSocketClose
   public void onClose(int var1, String var2) {
      if (Log.isDebugEnabled()) {
         Log.debug("Connection closed. Product name: " + this.productName + ", status code: " + var1 + ", reason: " + var2);
      }

      this.closeLatch.countDown();
      SQWebSocketManager.getInstance().removeClient(this.productName);
   }

   public boolean awaitClose(int var1, TimeUnit var2) throws InterruptedException {
      return this.closeLatch.await(var1, var2);
   }

   public void sendMessage(String var1) throws Exception {
      if (this.session != null && this.session.isOpen()) {
         this.session.getRemote().sendString(var1);
      }
   }

   private void processAction(JSONObject var1) {
      String var2 = var1.getString("action");
      if (var2 != null) {
         switch (var2) {
            case "setup":
               this.onSetup(var1);
               return;
            case "subscribe":
               this.onSubscribe(var1);
               return;
            case "unsubscribe":
               this.onUnsubscribe(var1);
               return;
            case "resolveCharts":
               try {
                  String var5 = var1.getString("format");
                  if (var5.equals("html")) {
                     Program.get("SaverHTML").call("resolveCharts", new Object[]{var1});
                  } else if (var5.equals("pdf")) {
                     Program.get("SaverPDF").call("resolveCharts", new Object[]{var1});
                  }
               } catch (Exception var6) {
                  Log.error("Cannot generate charts. Exc.", var6);
               }

               return;
         }
      }
   }

   private void onSetup(JSONObject var1) {
      String var2 = var1.getString("app");
      String var3 = var2;
      if (var2 == null) {
         Log.error("Websocket setup error - Product name is null");
      } else {
         for (int var4 = 2; SQWebSocketManager.getInstance().containsClient(var3); var4++) {
            var3 = var2 + "(" + var4 + ")";
         }

         this.productName = var3;
         SQWebSocketManager.getInstance().addClient(this);
         if (Log.isDebugEnabled()) {
            Log.debug("Connection opened. Product name: " + var3);
         }
      }
   }

   private void onSubscribe(JSONObject var1) {
      if (var1.has("projectName") && var1.has("channel")) {
         String var2 = var1.getString("projectName");
         String var3 = var1.getString("channel");
         ProjectFeeds.subscribe(var2, var3);
         SQWebSocketManager.getInstance().resetPublishersLastData(var2);
      } else {
         Log.error("Websocket subscription error - Product: '" + this.productName + "', JSON data: '" + var1.toString() + "'");
      }
   }

   private void onUnsubscribe(JSONObject var1) {
      if (var1.has("projectName")) {
         String var2 = var1.getString("projectName");
         String var3 = var1.getString("channel");
         if (var2 != null && var3 != null) {
            ProjectFeeds.unsubscribe(var2, var3);
         } else {
            Log.error("Websocket unsubscription error - Project name: '" + var2 + "', channel: '" + var3 + "'");
         }
      }
   }
}
