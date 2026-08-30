package com.strategyquant.plugin.DataManager.impl.Data;

import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import org.json.JSONObject;

public class ImportInfoPublisher extends SynchronizedWebSocketPublisher {
   private DataToSend toSend;
   private volatile boolean newData = true;
   private String infoMessage = "";
   private int step;
   private static ImportInfoPublisher instance;

   public ImportInfoPublisher() {
      this.toSend = new DataToSend();
      this.toSend.setName("dataImportProgress");
      this.toSend.setDataObject(new JSONObject().put("progress", 0));
      SQWebSocketManager.getInstance().addAppPublisher("SQMANAGER", this);
      instance = this;
   }

   private static ImportInfoPublisher get() {
      if (instance == null) {
         new ImportInfoPublisher();
      }

      return instance;
   }

   public static void setProgress(int var0) {
      synchronized (get().toSend) {
         JSONObject var2 = get().toSend.getDataObject();
         var2.remove("error");
         var2.remove("confirm");
         var2.remove("confirmLabel");
         var2.put("step", instance.step);
         var2.put("message", instance.infoMessage);
         var2.put("progress", var0);
         get().newData = true;
      }
   }

   public static void setError(String var0) {
      synchronized (get().toSend) {
         JSONObject var2 = get().toSend.getDataObject();
         var2.remove("progress");
         var2.remove("confirm");
         var2.remove("confirmLabel");
         var2.remove("step");
         var2.put("message", instance.infoMessage);
         var2.put("error", var0);
         get().newData = true;
      }
   }

   public static void setConfirmation(String var0, String var1) {
      synchronized (get().toSend) {
         JSONObject var3 = get().toSend.getDataObject();
         var3.remove("progress");
         var3.remove("error");
         var3.remove("step");
         var3.put("message", instance.infoMessage);
         var3.put("confirm", var1);
         var3.put("confirmLabel", var0);
         get().newData = true;
      }
   }

   public static void setMessage(String var0) {
      get().infoMessage = var0;
   }

   public DataToSend getData() {
      synchronized (this.toSend) {
         if (this.newData) {
            this.newData = false;
            return this.toSend;
         } else {
            return null;
         }
      }
   }

   public void resetLastData() {
      this.newData = true;
   }

   public static void setStep(int var0) {
      get().step = var0;
   }
}
