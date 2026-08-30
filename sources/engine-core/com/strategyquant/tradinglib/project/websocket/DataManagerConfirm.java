package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.lib.L;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManagerConfirm {
   private static final Logger Log = LoggerFactory.getLogger(DataManagerConfirm.class);
   public static final String ActionOverwriteAll = "overwrite_all";
   public static final String ActionOverwrite = "overwrite";
   public static final String ActionSkip = "skip";
   public static final String ActionCancel = "cancel";
   private DataToSend confirmData;
   private Thread waitingThread;
   private String confirmedAction = null;
   private static DataManagerConfirm instance = null;

   public static DataManagerConfirm get() {
      if (instance == null) {
         instance = new DataManagerConfirm();
      }

      return instance;
   }

   public DataManagerConfirm() {
      JSONObject var1 = new JSONObject();
      var1.put(
         "options",
         new JSONArray()
            .put(new JSONObject().put("title", L.t("Overwrite All", new Object[0])).put("key", "overwrite_all"))
            .put(new JSONObject().put("title", L.t("Overwrite", new Object[0])).put("key", "overwrite"))
            .put(new JSONObject().put("title", L.t("Skip", new Object[0])).put("key", "skip"))
            .put(new JSONObject().put("title", L.t("Cancel import", new Object[0])).put("key", "cancel"))
      );
      this.confirmData = new DataToSend("DMConfirmation", var1);
   }

   public void awaitConfirmation(String var1, String var2) throws Exception {
      this.confirmData.getDataObject().put("title", var1);
      this.confirmData.getDataObject().put("message", var2);
      SQWebSocketManager.addToDataQueue(this.confirmData, "SQMANAGER");
      if (this.waitingThread != null && this.waitingThread.isAlive()) {
         throw new Exception("Confirmation in progress");
      }

      this.confirmedAction = null;
      this.waitingThread = new Thread() {
         @Override
         public void run() {
            while (DataManagerConfirm.this.confirmedAction == null) {
               try {
                  Thread.sleep(50L);
               } catch (Exception var2x) {
               }
            }
         }
      };
      this.waitingThread.start();

      try {
         this.waitingThread.join();
      } catch (InterruptedException var4) {
         Log.error("Waiting for confirmation interrupted.", var4);
      }
   }

   public void setConfirmedAction(String var1) {
      this.confirmedAction = var1;
   }

   public String getConfirmedAction() {
      return this.confirmedAction;
   }
}
