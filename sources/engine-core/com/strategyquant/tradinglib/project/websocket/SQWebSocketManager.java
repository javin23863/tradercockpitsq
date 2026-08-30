package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.console.CLILogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQWebSocketManager {
   private static final Logger Log = LoggerFactory.getLogger(SQWebSocketManager.class);
   private static LinkedHashMap<String, SQWebSocket> sockets = new LinkedHashMap<>();
   private static SQWebSocketManager instance;
   public HashMap<String, ArrayList<WebSocketPublisher>> appPublishers = new HashMap<>();
   private HashMap<String, ArrayList<DataToSend>> dataToSend = new HashMap<>();
   ArrayList<DataToSend> tempData = new ArrayList<>();
   private StampedLock lock = new StampedLock();

   public SQWebSocketManager() {
      if (!MainApp.runInConsole()) {
         Log.info("WebSocketManager initialized.");
      }

      (new Thread() {
         @Override
         public void run() {
            while (true) {
               try {
                  Thread.sleep(1000L);
               } catch (Exception var2) {
               }

               if (MainApp.AppLoaded) {
                  SQWebSocketManager.this.sendData();
               }
            }
         }
      }).start();
      instance = this;
   }

   public void sendData() {
      try {
         long var1 = this.lock.writeLock();

         try {
            if (MainApp.runInConsoleWithoutActiveWebserver()) {
               for (String var4 : this.appPublishers.keySet()) {
                  ArrayList var5 = this.appPublishers.get(var4);
                  if (var5 != null) {
                     for (int var6 = 0; var6 < var5.size(); var6++) {
                        WebSocketPublisher var7 = (WebSocketPublisher)var5.get(var6);
                        synchronized (var7) {
                           this.logCmdMessage(var7.getDataToSend());
                           ((WebSocketPublisher)var5.get(var6)).confirmBroadcast();
                        }
                     }
                  }
               }
            } else {
               this.sendCommonMessages();
               if (!MainApp.checkProduct("AlgoWizard") && !MainApp.checkProduct("QDM") && !MainApp.checkProduct("BACKTESTNODE")) {
                  this.sendProjectsUpdates();
               }
            }
         } finally {
            this.lock.unlock(var1);
         }
      } catch (Throwable var16) {
         Log.error("WebSocketManager error", var16);
      }
   }

   public void logCmdMessage(DataToSend... var1) {
      for (int var2 = 0; var2 < var1.length; var2++) {
         DataToSend var3 = var1[var2];
         if (var3 != null) {
            if (var3.getDataObject() != null) {
               this.printCmdMessage(var3.getDataObject());
            } else if (var3.getDataArray() != null) {
               for (int var4 = 0; var4 < var3.getDataArray().length(); var4++) {
                  this.printCmdMessage((JSONObject)var3.getDataArray().get(var4));
               }
            }
         }
      }
   }

   private void printCmdMessage(JSONObject var1) {
      if (var1.has("logStr")) {
         String var2 = "";
         if (var1.has("key")) {
            var2 = var2 + var1.getString("key") + ", ";
         }

         if (var1.has("pct")) {
            if (var1.getInt("pct") < 0) {
               var2 = var2 + "ERROR: ";
               var2 = var2 + var1.getString("logStr") + ", ";
            } else {
               var2 = var2 + var1.getString("logStr") + ", ";
               var2 = var2 + var1.getInt("pct") + "%, ";
            }
         } else {
            var2 = var2 + var1.getString("logStr") + ", ";
         }

         var2 = var2.substring(0, var2.length() - 2);
         CLILogger.log(var2);
      }
   }

   private void sendCommonMessages() {
      HashMap var1 = new HashMap();

      for (String var3 : sockets.keySet()) {
         try {
            this.tempData.clear();
            String var4 = this.normalizeSocketName(var3);
            String var5 = (String)var1.get(var4);
            if (var5 == null) {
               ArrayList var6 = this.appPublishers.get(var4);
               if (var6 != null) {
                  for (int var7 = 0; var7 < var6.size(); var7++) {
                     WebSocketPublisher var8 = (WebSocketPublisher)var6.get(var7);
                     synchronized (var8) {
                        this.tempData.add(var8.getDataToSend());
                        var8.confirmBroadcast();
                     }
                  }
               }

               ArrayList var13 = this.dataToSend.get(var4);
               if (var13 != null) {
                  this.tempData.addAll(var13);
               }

               var5 = this.stringifyMessage(this.tempData);
               var1.put(var4, var5);
            }

            if (!var5.equals("{}")) {
               sockets.get(var3).sendMessage(var5);
            }
         } catch (Exception var12) {
            Log.error("Cannot send common message to socket '" + var3 + "'. ", var12);
         }
      }

      this.dataToSend.clear();
   }

   private String normalizeSocketName(String var1) {
      String var2 = var1;
      int var3 = var1.indexOf("(");
      if (var3 > 0) {
         var2 = var1.substring(0, var3);
      }

      return var2;
   }

   private void sendProjectsUpdates() {
      List var1 = ProjectEngine.list();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         SQProject var3 = (SQProject)var1.get(var2);
         this.sendProjectUpdates(var3);
      }
   }

   private void sendProjectUpdates(SQProject var1) {
      try {
         DataToSend var2 = var1.publisher.getDataToSend();
         var1.publisher.confirmBroadcast();
         if (var2 == null) {
            return;
         }

         String var3 = this.stringifyMessage(var2);

         for (String var5 : sockets.keySet()) {
            SQWebSocket var6 = sockets.get(var5);

            try {
               String var7 = this.normalizeSocketName(var5);
               if (getProductCode(var1.getName()).equals(var7)) {
                  var6.sendMessage(var3);
               }
            } catch (Exception var8) {
               Log.error("Cannot send project update to websocket '" + var5 + "'. ", var8);
            }
         }
      } catch (Exception var9) {
         Log.error("Cannot send update for project '" + var1.getName() + "'. ", var9);
      }
   }

   public static String getProductCode(String var0) {
      switch (var0) {
         case "Builder":
            return "BUILDER";
         case "Retester":
            return "RETESTER";
         case "Optimizer":
            return "OPTIMIZER";
         case "PortfolioMaster":
            return "PORTFOLIOMASTER";
         case "PortfolioComposer":
            return "PORTFOLIOCOMPOSER";
         default:
            return "TASKMANAGER";
      }
   }

   public static String getProjectName(String var0) {
      switch (var0) {
         case "BUILDER":
            return "Builder";
         case "RETESTER":
            return "Retester";
         case "OPTIMIZER":
            return "Optimizer";
         case "PORTFOLIOMASTER":
            return "PortfolioMaster";
         case "PORTFOLIOCOMPOSER":
            return "PortfolioComposer";
         default:
            return null;
      }
   }

   private String stringifyMessage(DataToSend... var1) {
      JSONObject var2 = new JSONObject();

      for (int var3 = 0; var3 < var1.length; var3++) {
         DataToSend var4 = var1[var3];
         if (var4 != null) {
            if (var4.getDataObject() != null) {
               var2.put(var4.getName(), var4.getDataObject());
            } else if (var4.getDataArray() != null) {
               var2.put(var4.getName(), var4.getDataArray());
            }
         }
      }

      return var2.toString();
   }

   private String stringifyMessage(ArrayList<DataToSend> var1) {
      JSONObject var2 = new JSONObject();

      for (int var3 = 0; var3 < var1.size(); var3++) {
         DataToSend var4 = (DataToSend)var1.get(var3);
         if (var4 != null) {
            if (var4.getDataObject() != null) {
               var2.put(var4.getName(), var4.getDataObject());
            } else if (var4.getDataArray() != null) {
               var2.put(var4.getName(), var4.getDataArray());
            }
         }
      }

      return var2.toString();
   }

   public static SQWebSocketManager getInstance() {
      if (instance == null) {
         new SQWebSocketManager();
      }

      return instance;
   }

   public static void addToDataQueue(DataToSend var0, String... var1) {
      getInstance()._addToDataQueue(var0, var1);
   }

   public void addClient(SQWebSocket var1) {
      long var2 = this.lock.writeLock();

      try {
         sockets.put(var1.getUniqueId(), var1);
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void removeClient(String var1) {
      long var2 = this.lock.writeLock();

      try {
         sockets.remove(var1);
      } finally {
         this.lock.unlock(var2);
      }
   }

   public boolean containsClient(String var1) {
      long var2 = this.lock.readLock();

      try {
         return sockets.containsKey(var1);
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void addAppPublisher(String var1, WebSocketPublisher var2) {
      long var3 = this.lock.writeLock();

      try {
         ArrayList var5 = this.appPublishers.get(var1);
         if (var5 == null) {
            var5 = new ArrayList();
            var5.add(var2);
            this.appPublishers.put(var1, var5);
         } else {
            var5.add(var2);
         }
      } finally {
         this.lock.unlock(var3);
      }
   }

   public void removeAppPublisher(String var1, WebSocketPublisher var2) {
      long var3 = this.lock.writeLock();

      try {
         ArrayList var5 = this.appPublishers.get(var1);
         if (var5 != null) {
            var5.remove(var2);
         } else {
            Log.error("Publisher for app '" + var1 + "' doesn't exist");
         }
      } finally {
         this.lock.unlock(var3);
      }
   }

   public void resetPublishersLastData(String var1) {
      long var2 = this.lock.writeLock();

      try {
         ArrayList var4 = this.appPublishers.get(getProductCode(var1));
         if (var4 != null) {
            for (int var5 = 0; var5 < var4.size(); var5++) {
               ((WebSocketPublisher)var4.get(var5)).resetLastData();
            }

            return;
         }
      } catch (Exception var9) {
         Log.error("Cannot reset last data of publishers of project " + var1, var9);
         return;
      } finally {
         this.lock.unlock(var2);
      }
   }

   public void _addToDataQueue(DataToSend var1, String... var2) {
      long var3 = this.lock.writeLock();

      try {
         for (int var5 = 0; var5 < var2.length; var5++) {
            String var6 = var2[var5];
            ArrayList var7 = this.dataToSend.get(var6);
            if (var7 == null) {
               var7 = new ArrayList();
               var7.add(var1);
               this.dataToSend.put(var6, var7);
            } else {
               var7.add(var1);
            }
         }
      } finally {
         this.lock.unlock(var3);
      }
   }

   public boolean checkWebSocketsConnected() {
      long var1 = this.lock.readLock();

      try {
         for (String var4 : sockets.keySet()) {
            if (!var4.endsWith(")")) {
               return true;
            }
         }

         return false;
      } finally {
         this.lock.unlock(var1);
      }
   }

   public void sendProjectDataImmediately(SQProject var1) {
      long var2 = this.lock.readLock();

      try {
         this.sendProjectUpdates(var1);
      } finally {
         this.lock.unlock(var2);
      }
   }
}
