package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.lib.utils.IProgressListener;
import org.json.JSONObject;

public class MultiProgressListener implements IProgressListener {
   public static final String SYMBOL_TOTAL = "TotalProgressSymbol";
   protected String key;
   public JSONObject jsonData;
   protected MultiProgressPublisher publisher;
   private String lastMessage;
   private String lastJson;

   public MultiProgressListener(String var1, MultiProgressPublisher var2) {
      this.key = var1;
      this.publisher = var2;
      this.jsonData = new JSONObject();
      this.jsonData.put("key", var1);
   }

   public void setMessage(String var1) {
      this.publisher.setMessage(this.key, var1);
   }

   public void onStart() {
   }

   public void onProgress(double var1) {
      this.publisher.setProgress(this.key, (int)var1);
   }

   public void onPause() {
      this.publisher.setCustomValue(this.key, "paused", true);
   }

   public void onContinue() {
      this.publisher.setCustomValue(this.key, "paused", false);
   }

   public void onFinish() {
      this.publisher.setProgress(this.key, 100);
   }

   public void onError(String var1) {
      this.publisher.setMessage(this.key, var1);
      this.publisher.setProgress(this.key, -1);
      this.publisher.onError(var1);
   }

   public void onConfirm(String var1) {
   }

   public void setStep(int var1) {
   }

   public void reset() {
      this.publisher.reset();
   }

   public JSONObject getData() {
      String var1 = this.jsonData.has("message") ? this.jsonData.getString("message") : null;
      if (var1 == null || this.lastMessage != null && var1.equals(this.lastMessage)) {
         this.jsonData.remove("logStr");
      } else {
         this.jsonData.put("logStr", var1);
      }

      this.lastMessage = var1;
      String var2 = this.jsonData.toString();
      boolean var3 = var2.equals(this.lastJson);
      this.lastJson = var2;
      if (var3) {
         if ("TotalProgressSymbol".equals(this.key) && this.jsonData.has("pct")) {
            double var4 = this.jsonData.getDouble("pct");
            if (var4 < 100.0) {
               return this.jsonData;
            }
         }

         return null;
      } else {
         return this.jsonData;
      }
   }
}
