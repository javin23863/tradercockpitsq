package com.strategyquant.tradinglib.project.websocket;

import org.json.JSONObject;

public class DataManagerDataProgress extends MultiProgressPublisher {
   private static DataManagerDataProgress instance;

   public static DataManagerDataProgress get() {
      if (instance == null) {
         instance = new DataManagerDataProgress();
      }

      return instance;
   }

   public DataManagerDataProgress() {
      super("DMDataProgresses", "SQMANAGER");
   }

   @Override
   protected MultiProgressListener performCreateListener(String var1) {
      return new DataManagerProgressListener(var1, this);
   }

   @Override
   protected void handleConfirmBroadcast() {
      super.handleConfirmBroadcast();
      this.clearAllCustomValues("log");
   }

   public MultiProgressListener createListener(String var1, String var2) {
      MultiProgressListener var3 = super.createListener(var1);
      JSONObject var4 = var3.jsonData;
      var4.put("actionLink", var2);
      return var3;
   }
}
