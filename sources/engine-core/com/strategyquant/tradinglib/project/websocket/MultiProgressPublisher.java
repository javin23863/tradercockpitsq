package com.strategyquant.tradinglib.project.websocket;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiProgressPublisher extends SynchronizedWebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(MultiProgressPublisher.class);
   private HashMap<String, MultiProgressListener> listeners = new HashMap<>();
   private List<String> listenersForRemove = new LinkedList<>();
   private DataToSend dataToSend;
   private JSONArray jsonData;
   private String lastDataHash;
   private String appCode;

   public MultiProgressPublisher(String var1, String var2) {
      SQWebSocketManager.getInstance().addAppPublisher(var2, this);
      this.appCode = var2;
      this.jsonData = new JSONArray();
      this.dataToSend = new DataToSend(var1, this.jsonData);
   }

   public synchronized MultiProgressListener createListener(String var1) {
      if (!this.listeners.containsKey(var1)) {
         this.listeners.put(var1, this.performCreateListener(var1));
      }

      MultiProgressListener var2 = this.listeners.get(var1);
      var2.reset();
      return var2;
   }

   public synchronized MultiProgressListener getListener(String var1) {
      return !this.listeners.containsKey(var1) ? null : this.listeners.get(var1);
   }

   public synchronized void removeListener(String var1) {
      this.listenersForRemove.add(var1);
   }

   protected MultiProgressListener performCreateListener(String var1) {
      return new MultiProgressListener(var1, this);
   }

   public synchronized void setProgress(String var1, int var2) {
      MultiProgressListener var3 = this.createListener(var1);
      JSONObject var4 = var3.jsonData;
      var4.put("pct", var2);
   }

   public synchronized void setProgress(String var1, int var2, String var3) {
      MultiProgressListener var4 = this.createListener(var1);
      this.setProgress(var1, var2);
      JSONObject var5 = var4.jsonData;
      var5.put("message", var3);
   }

   public synchronized void setMessage(String var1, String var2) {
      MultiProgressListener var3 = this.createListener(var1);
      JSONObject var4 = var3.jsonData;
      var4.put("message", var2);
   }

   public synchronized void setCustomValue(String var1, String var2, Object var3) {
      MultiProgressListener var4 = this.createListener(var1);
      JSONObject var5 = var4.jsonData;
      var5.put(var2, var3);
   }

   public synchronized void addStringCustomValue(String var1, String var2, String var3, boolean var4) {
      MultiProgressListener var5 = this.createListener(var1);
      JSONObject var6 = var5.jsonData;
      if (var6.has(var2)) {
         String var7 = (String)var6.get(var2);
         var3 = var4 ? var3 + var7 : var7 + var3;
      }

      var6.put(var2, var3);
   }

   public synchronized void clearAllCustomValues(String var1) {
      for (MultiProgressListener var3 : this.listeners.values()) {
         JSONObject var4 = var3.jsonData;
         var4.remove(var1);
      }
   }

   @Override
   protected synchronized DataToSend getData() {
      for (int var2 = this.jsonData.length() - 1; var2 >= 0; var2--) {
         this.jsonData.remove(var2);
      }

      for (String var3 : this.listeners.keySet()) {
         JSONObject var1 = this.listeners.get(var3).getData();
         if (var1 != null) {
            this.jsonData.put(var1);
         }
      }

      if (this.lastDataHash != null && this.jsonData.toString().equals(this.lastDataHash)) {
         return null;
      }

      this.lastDataHash = this.jsonData.toString();
      return this.dataToSend;
   }

   public void reset() {
      this.lastDataHash = null;
   }

   @Override
   public void resetLastData() {
      this.lastDataHash = null;
   }

   public void onError(String var1) {
      WebSocketNotification.sendError(var1, this.appCode);
   }

   @Override
   protected void handleConfirmBroadcast() {
      super.handleConfirmBroadcast();
      synchronized (this) {
         for (String var3 : this.listenersForRemove) {
            this.listeners.remove(var3);
         }

         this.listenersForRemove.clear();
      }
   }
}
