package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.sqxbusiness.BuildData;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildResult;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import com.strategyquant.lib.sqxbusiness.MQLMarketLoggable;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQXBusinessBuildReporter implements IGridMessageListener, MQLMarketLoggable {
   private static final Logger Log = LoggerFactory.getLogger(SQXBusinessBuildReporter.class);
   private static final DataToSend wsData = new DataToSend("buildUpdates");
   private static SQXBusinessBuildReporter instance;
   private static ArrayList<String> allGroupIDs = new ArrayList<>();
   private static HashMap<String, HashMap<String, BuildData>> allBuilds = new HashMap<>();
   private static StampedLock groupIDsLock = new StampedLock();
   private static StampedLock buildsLock = new StampedLock();

   private SQXBusinessBuildReporter() {
      (new Thread() {
         @Override
         public void run() {
            while (true) {
               try {
                  sleep(1000L);
               } catch (Exception var2) {
               }

               SQXBusinessBuildReporter.this.sendWSUpdates();
            }
         }
      }).start();
   }

   public static SQXBusinessBuildReporter get() {
      if (instance == null) {
         instance = new SQXBusinessBuildReporter();
      }

      return instance;
   }

   public void messageReceived(GridMessage var1) {
      try {
         if (SQProject.isMemoryProtectionUsed()) {
            MemoryUsageChecker.checkAvailableMemory();
         }
      } catch (OutOfMemoryError var68) {
         Log.error("Out of memory error occurred. Stopping all jobs...");
         long var3 = groupIDsLock.readLock();

         try {
            for (int var5 = 0; var5 < allGroupIDs.size(); var5++) {
               String var6 = allGroupIDs.get(var5);
               Log.info("Stopping jobGroupID " + var6 + "...");
               SQGrid.getGridClient().stop(var6);
            }
         } finally {
            groupIDsLock.unlock(var3);
         }
      }

      JobDetails var2 = var1.getJobDetails();
      if (var1.getMessageID() == 1) {
         try {
            if (var2.isSuccess()) {
               MQLMarketBuildResult var69 = (MQLMarketBuildResult)var1.getData();
               long var4 = buildsLock.writeLock();

               try {
                  BuildData var78 = getBuildData(var2.getJobID());
                  if (var69.exception != null) {
                     var78.addLogLine("Compilation failed - " + var69.exception.getMessage());
                     var78.status = 5;
                     var78.changed = true;
                  } else {
                     var78.addLogLine("Compilation done");
                     var78.status = 2;
                     var78.changed = true;
                  }
               } finally {
                  buildsLock.unlock(var4);
               }
            } else {
               long var70 = buildsLock.writeLock();

               try {
                  BuildData var74 = getBuildData(var2.getJobID());
                  var74.addLogLine("Compilation failed - " + var2.getException());
                  var74.status = 5;
                  var74.changed = true;
               } finally {
                  buildsLock.unlock(var70);
               }
            }
         } catch (Exception var66) {
            Log.error("Error while processing job message", var66);
         }
      } else if (var1.getMessageID() == 4) {
         long var71 = buildsLock.writeLock();

         try {
            BuildData var75 = getBuildData(var2.getJobID());
            var75.addLogLine("Compilation stopped");
            var75.status = 3;
            var75.changed = true;
         } finally {
            buildsLock.unlock(var71);
         }
      } else if (var1.getMessageID() == 2) {
         long var72 = buildsLock.writeLock();

         try {
            BuildData var76 = getBuildData(var2.getJobID());
            var76.addLogLine("Compilation paused");
            var76.status = 4;
            var76.changed = true;
         } finally {
            buildsLock.unlock(var72);
         }
      } else if (var1.getMessageID() == 3) {
         long var73 = buildsLock.writeLock();

         try {
            BuildData var77 = getBuildData((String)var1.getData());
            var77.addLogLine("Compilation resumed");
            var77.status = 1;
            var77.changed = true;
         } finally {
            buildsLock.unlock(var73);
         }
      }
   }

   public static void registerMessageListener(String var0) {
      long var1 = groupIDsLock.writeLock();

      try {
         if (!allGroupIDs.contains(var0)) {
            allGroupIDs.add(var0);
         }
      } finally {
         groupIDsLock.unlock(var1);
      }

      SQGrid.getGridClient().registerMessageListener(var0, get());
   }

   public static void addNewBuildData(String var0, String var1, String var2, String var3, boolean var4) {
      long var5 = buildsLock.writeLock();

      try {
         if (!allBuilds.containsKey(var0)) {
            allBuilds.put(var0, new HashMap<>());
         }

         allBuilds.get(var0).put(var2, new BuildData(var0, var1, var2, var3, var4));
      } catch (Exception var11) {
         Log.error("Cannot add build data. ProjectName: " + var0 + ", jobID: " + var2, var11);
      } finally {
         buildsLock.unlock(var5);
      }
   }

   public static void printToLog(String var0, String var1) {
      String var2 = MQLMarketConst.getProjectNameFromJobID(var0);
      long var3 = buildsLock.writeLock();

      try {
         BuildData var5 = allBuilds.get(var2).get(var0);
         var5.addLogLine(var1);
      } catch (Exception var9) {
         Log.error("Cannot add message to log. ProjectName: " + var2 + ", jobID: " + var0, var9);
      } finally {
         buildsLock.unlock(var3);
      }
   }

   public static void clearLog(String var0) {
      String var1 = MQLMarketConst.getProjectNameFromJobID(var0);
      long var2 = buildsLock.writeLock();

      try {
         BuildData var4 = allBuilds.get(var1).get(var0);
         var4.clearLog();
      } catch (Exception var8) {
         Log.error("Cannot clear build log. ProjectName: " + var1 + ", jobID: " + var0, var8);
         throw var8;
      } finally {
         buildsLock.unlock(var2);
      }
   }

   public static void setStatus(String var0, int var1) {
      String var2 = MQLMarketConst.getProjectNameFromJobID(var0);
      long var3 = buildsLock.writeLock();

      try {
         BuildData var5 = allBuilds.get(var2).get(var0);
         var5.status = var1;
         var5.changed = true;
      } catch (Exception var9) {
         Log.error("Cannot set build status. ProjectName: " + var2 + ", jobID: " + var0, var9);
      } finally {
         buildsLock.unlock(var3);
      }
   }

   private static JSONArray getBuildReports() {
      long var0 = buildsLock.writeLock();

      try {
         JSONArray var2 = new JSONArray();

         for (String var4 : allBuilds.keySet()) {
            HashMap var5 = allBuilds.get(var4);

            for (String var7 : var5.keySet()) {
               BuildData var8 = (BuildData)var5.get(var7);
               if (var8.changed) {
                  var2.put(var8.toJSON());
               }

               var8.changed = false;
            }
         }

         return var2;
      } catch (Exception var12) {
         Log.error("Cannot get build reports", var12);
         return null;
      } finally {
         buildsLock.unlock(var0);
      }
   }

   private static BuildData getBuildData(String var0) {
      String var1 = MQLMarketConst.getProjectNameFromJobID(var0);
      return allBuilds.get(var1).get(var0);
   }

   private void sendWSUpdates() {
      JSONArray var1 = getBuildReports();
      if (var1.length() != 0) {
         wsData.setDataObject(var1);
         SQWebSocketManager.addToDataQueue(wsData, new String[]{"SQXBUSINESS"});
      }
   }

   public void _printToLog(String var1, String var2) {
      printToLog(var1, var2);
   }
}
