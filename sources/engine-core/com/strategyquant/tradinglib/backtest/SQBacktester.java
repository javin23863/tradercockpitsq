package com.strategyquant.tradinglib.backtest;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQBacktester implements IBacktester {
   private static final Logger Log = LoggerFactory.getLogger(SQBacktester.class);
   private static StampedLock lock = new StampedLock();
   private static final int lockTimeout = 2;
   private static long stamp = 0L;
   private static SQBacktester instance;
   private static HashMap<String, String> jobToBacktestMap = new HashMap<>();
   private static HashMap<String, ResultsGroup> resultsMap = new HashMap<>();
   private static String serverUrl = "http://localhost";
   private static int serverPort = 8080;
   private GridClient gridClient;
   private String jobGroupID;
   private long jobCount;
   private Thread timeoutThread;
   DataToSend dataProgress = new DataToSend("backtestProgress", new JSONObject());
   DataToSend dataMessage = new DataToSend("backtestMessage", new JSONObject());

   private SQBacktester() {
      this.jobGroupID = UUID.randomUUID().toString();
      this.jobCount = 0L;
      this.gridClient = SQGrid.getGridClient();
      this.gridClient.registerMessageListener(this.jobGroupID, new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            SQBacktester.this.processMessage(var1);
         }
      });
   }

   public static SQBacktester get() {
      if (instance == null) {
         instance = new SQBacktester();
      }

      return instance;
   }

   @Override
   public void run(String var1, ChartSetup var2, Map<String, Serializable> var3) throws Exception {
      long var4 = lock.writeLock();

      try {
         this.dataProgress.setDataObject(new JSONObject());
         this.dataProgress.getDataObject().put("backtestID", var1);
         this.dataProgress.getDataObject().put("percent", 0);
         String var6 = String.format(var3.get("StrategyName") + "_%d", this.jobCount++);
         jobToBacktestMap.put(var6, var1);
         this.gridClient.executeOnGrid(this.jobGroupID, new SQBacktesterJob(var6, var3, new SQBacktester.JobLastEventListener(var1)));
      } finally {
         lock.unlock(var4);
      }
   }

   protected synchronized void processMessage(GridMessage var1) {
      long var2 = lock.writeLock();

      try {
         JobDetails var4 = var1.getJobDetails();
         String var5 = var4 != null ? var4.getJobID() : var1.getCustomID();
         if (jobToBacktestMap.containsKey(var5)) {
            String var6 = jobToBacktestMap.get(var5);
            this.dataProgress.setDataObject(new JSONObject());
            this.dataProgress.getDataObject().put("backtestID", var6);
            if (var1.getMessageID() == 6) {
               int var7 = (Integer)var1.getData();
               this.dataProgress.getDataObject().put("percent", var7);
               SQWebSocketManager.addToDataQueue(this.dataProgress, "AlgoWizard");
            } else if (var1.getMessageID() == 1) {
               var4 = var1.getJobDetails();
               if (this.timeoutThread != null && this.timeoutThread.isAlive()) {
                  this.timeoutThread.interrupt();
               }

               if (var4.isSuccess()) {
                  BacktestResult var17 = (BacktestResult)var1.getData();

                  try {
                     ResultsGroup var8 = var17.getResult();
                     if (var8 == null) {
                        throw new Exception(var17.getDismissalMessage());
                     }

                     resultsMap.put(var6, var8.clone());
                     ResultsPanelData.load(var8, this.dataProgress.getDataObject(), new JSONObject());
                     this.dataProgress.getDataObject().put("percent", 100);
                     this.dataProgress.getDataObject().put("nodeURL", serverUrl + ":" + serverPort);
                     SQWebSocketManager.addToDataQueue(this.dataProgress, "AlgoWizard");
                  } catch (Exception var13) {
                     Log.error("Backtest failed", var13);
                     this.dataProgress.getDataObject().put("error", var13.getMessage() != null ? var13.getMessage() : "null");
                     SQWebSocketManager.addToDataQueue(this.dataProgress, "AlgoWizard");
                  }

                  return;
               } else {
                  Log.error("Backtest failed", var4.getException());
                  this.dataProgress.getDataObject().put("error", getFirstLine(var4.getException()));
                  SQWebSocketManager.addToDataQueue(this.dataProgress, "AlgoWizard");
                  return;
               }
            }

            return;
         }

         Log.error("Unrecognized backtest by jobID " + var5);
      } catch (Exception var14) {
         Log.error("Exception while processing message", var14);
         return;
      } finally {
         lock.unlock(var2);
      }
   }

   private static String getFirstLine(String var0) {
      return var0 == null ? "NullPointer - see log" : var0.split("\n")[0];
   }

   @Override
   public void stop(String var1) throws Exception {
      lock();

      try {
         Log.info("Stopping backtest with id " + var1 + "...");
         String var2 = this.findJobIDByBacktestID(var1);
         if (var2 == null) {
            throw new Exception(L.t("Job not found for backtestID %s", new Object[]{var1}));
         }

         this.gridClient.sendMessage(this.jobGroupID, var2, new GridMessage(5, "StopBacktest"));
      } finally {
         lock.unlock(stamp);
      }
   }

   private String findJobIDByBacktestID(String var1) {
      for (Entry var3 : jobToBacktestMap.entrySet()) {
         if (((String)var3.getValue()).equals(var1)) {
            return (String)var3.getKey();
         }
      }

      return null;
   }

   private static void lock() throws Exception {
      stamp = lock.tryWriteLock(2L, TimeUnit.SECONDS);
      if (stamp == 0L) {
         throw new Exception(L.t("Cannot acquire lock. Stamp is 0", new Object[0]));
      }
   }

   public static ResultsGroup getResultsGroup(String var0) throws Exception {
      if (var0 == null) {
         throw new Exception(L.t("backtestID cannot be null.", new Object[0]));
      } else if (var0.trim().isEmpty()) {
         throw new Exception(L.t("backtestID is not set.", new Object[0]));
      } else if (!resultsMap.containsKey(var0)) {
         throw new Exception(L.t("Backtest not found for id %s", new Object[]{var0}));
      } else {
         return resultsMap.get(var0);
      }
   }

   @Override
   public void setServerInfo(String var1, int var2) {
      serverUrl = var1;
      serverPort = var2;
   }

   private class JobLastEventListener implements ILastEventListener {
      private String backtestID;

      public JobLastEventListener(String nullx) {
         this.backtestID = nullx;
      }

      @Override
      public void setLastEvent(String var1) {
         SQBacktester.this.dataMessage.setDataObject(new JSONObject());
         SQBacktester.this.dataMessage.getDataObject().put("backtestID", this.backtestID);
         SQBacktester.this.dataMessage.getDataObject().put("message", var1);
         SQWebSocketManager.addToDataQueue(SQBacktester.this.dataMessage, "AlgoWizard");
      }
   }
}
