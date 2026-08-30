package com.strategyquant.tradinglib.project;

import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserConfirm {
   private static final Logger Log = LoggerFactory.getLogger(UserConfirm.class);
   protected String projectName;
   private DataToSend confirmData;
   private Thread waitingThread;
   private boolean confirmed = false;
   private volatile boolean resolved = false;

   public UserConfirm(String var1) {
      this.projectName = var1;
      this.confirmData = new DataToSend("confirmation", new JSONObject());
   }

   public void awaitConfirmation(String var1, String var2) throws Exception {
      this.awaitConfirmation(var1, var2, null);
   }

   public void awaitConfirmation(String var1, String var2, JSONObject var3) throws Exception {
      this.confirmData.getDataObject().put("projectName", this.projectName);
      this.confirmData.getDataObject().put("title", var1);
      this.confirmData.getDataObject().put("message", var2);
      if (var3 != null) {
         this.confirmData.getDataObject().put("additionalData", var3);
      } else {
         this.confirmData.getDataObject().remove("additionalData");
      }

      SQWebSocketManager.addToDataQueue(this.confirmData, SQWebSocketManager.getProductCode(this.projectName));
      if (this.waitingThread != null && this.waitingThread.isAlive()) {
         throw new Exception("Confirmation in progress");
      }

      this.resolved = false;
      this.waitingThread = new Thread() {
         @Override
         public void run() {
            while (!UserConfirm.this.resolved) {
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
      } catch (InterruptedException var5) {
         Log.error("Waiting for confirmation interrupted.", var5);
      }
   }

   public void setConfirmed(boolean var1) {
      this.confirmed = var1;
      this.resolved = true;
   }

   public boolean isConfirmed() {
      return this.confirmed;
   }

   public void setProjectName(String var1) {
      this.projectName = var1;
   }
}
