package com.strategyquant.tradinglib.project.websocket;

public class DataManagerProgressListener extends MultiProgressListener {
   public DataManagerProgressListener(String var1, MultiProgressPublisher var2) {
      super(var1, var2);
   }

   public void addLog(String var1) {
      this.publisher.addStringCustomValue(this.key, "log", var1, true);
   }

   public void setDataType(String var1) {
      this.publisher.setCustomValue(this.key, "dataType", var1);
   }

   public void setDownloadType(String var1) {
      this.publisher.setCustomValue(this.key, "downloadType", var1);
   }

   public void setSpeed(String var1) {
      this.publisher.setCustomValue(this.key, "speed", var1);
   }

   public void setEstimation(String var1) {
      this.publisher.setCustomValue(this.key, "estimation", var1);
   }
}
