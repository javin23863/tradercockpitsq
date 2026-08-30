package com.strategyquant.plugin.CodeEditor.impl.IndicatorTester;

import com.strategyquant.indicatorTester.IndicatorTesterConfig;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONObject;

public class DownloadProgressPublisher extends SynchronizedWebSocketPublisher implements IProgressListener {
   private static DownloadProgressPublisher instance;
   private DataToSend info = new DataToSend("TestsDownloadStatus");
   private JSONObject status = new JSONObject();
   private String lastData = "";
   private StampedLock lock = new StampedLock();

   private DownloadProgressPublisher() {
      instance = this;
      this.info.setDataObject(this.status);
      SQWebSocketManager.getInstance().addAppPublisher("SQEDITOR", this);
   }

   public static DownloadProgressPublisher get() {
      return instance == null ? new DownloadProgressPublisher() : instance;
   }

   protected DataToSend getData() {
      long var1 = this.lock.readLock();

      try {
         String var3 = this.status.toString();
         if (!this.lastData.equals(var3)) {
            this.lastData = var3;
            return this.info;
         } else {
            return null;
         }
      } finally {
         this.lock.unlock(var1);
      }
   }

   public void setMessage(String var1) {
      long var2 = this.lock.writeLock();

      try {
         this.status.put("info", var1);
         this.status.put("error", false);
         this.status.put("xmlConfig", false);
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void onProgress(double var1) {
      long var3 = this.lock.writeLock();

      try {
         this.status.put("percent", (int)var1);
         if (var1 != 100.0) {
            this.status.put("xmlConfig", false);
            this.status.put("filePath", false);
         } else {
            this.status.put("xmlConfig", XMLUtil.elementToString(IndicatorTesterConfig.getConfig()));
            this.status.put("filePath", IndicatorTesterConfig.defaultConfigFilePath);
         }
      } finally {
         this.lock.unlock(var3);
      }
   }

   public void onError(String var1) {
      long var2 = this.lock.writeLock();

      try {
         this.status.put("error", var1);
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void resetLastData() {
      this.lastData = "";
   }

   public void onStart() {
   }

   public void onFinish() {
   }

   public void onConfirm(String var1) {
   }

   public void setStep(int var1) {
   }

   public void onPause() {
   }

   public void onContinue() {
   }
}
