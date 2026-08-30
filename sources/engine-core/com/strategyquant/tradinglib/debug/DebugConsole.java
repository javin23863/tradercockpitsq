package com.strategyquant.tradinglib.debug;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugConsole extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(DebugConsole.class);
   private static DebugConsole instance = null;
   private static final int MAX_LOG_ENTRIES = 1000;
   JSONArray data = new JSONArray();
   DataToSend toSend = new DataToSend("DebugConsoleLog");

   private DebugConsole() {
      SQWebSocketManager.getInstance().addAppPublisher("DEBUGCONSOLE", this);
   }

   public static DebugConsole getInstance() {
      if (instance == null) {
         instance = new DebugConsole();
      }

      return instance;
   }

   public static void log(String var0, String var1) {
      getInstance()._log(var0, var1);
   }

   private void _log(String var1, String var2) {
      if (this.data.length() < 1000) {
         JSONObject var3 = new JSONObject();
         var3.put("time", SQTime.getLogTime(false, false));
         var3.put("category", var1);
         var3.put("message", var2);
         this.data.put(var3);
      }
   }

   @Override
   public void resetLastData() {
   }

   @Override
   protected DataToSend getData() {
      if (this.data.length() > 0) {
         this.toSend.setDataObject(this.data);
         this.data = new JSONArray();
         return this.toSend;
      } else {
         return null;
      }
   }
}
