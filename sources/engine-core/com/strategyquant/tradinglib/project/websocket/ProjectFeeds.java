package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.tradinglib.project.ProjectEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.StampedLock;

public class ProjectFeeds {
   private static HashMap<String, ArrayList<String>> feedMap = new HashMap<>();
   private static StampedLock lock = new StampedLock();
   private static String activeProductCode = null;

   public static void setActiveProduct(String var0) {
      long var1 = lock.writeLock();

      try {
         activeProductCode = var0;
      } finally {
         lock.unlock(var1);
      }
   }

   public static void subscribe(String var0, String var1) {
      long var2 = lock.writeLock();

      try {
         if (feedMap.containsKey(var0)) {
            ArrayList var4 = feedMap.get(var0);
            if (!var4.contains(var1)) {
               var4.add(var1);
            }
         } else {
            ArrayList var8 = new ArrayList();
            var8.add(var1);
            feedMap.put(var0, var8);
         }
      } finally {
         lock.unlock(var2);
      }
   }

   public static void unsubscribe(String var0, String var1) {
      long var2 = lock.writeLock();

      try {
         if (feedMap.containsKey(var0)) {
            ArrayList var4 = feedMap.get(var0);
            var4.remove(var1);
         }
      } finally {
         lock.unlock(var2);
      }
   }

   public static void unsubscribeAll(String var0) {
      long var1 = lock.writeLock();

      try {
         if (feedMap.containsKey(var0)) {
            feedMap.get(var0).clear();
         }
      } finally {
         lock.unlock(var1);
      }
   }

   public static ArrayList<String> getChannels(String var0) {
      long var1 = lock.readLock();

      try {
         return feedMap.get(var0);
      } finally {
         lock.unlock(var1);
      }
   }

   public static boolean isSubscribed(String var0, String var1) {
      long var2 = lock.readLock();

      try {
         if (!checkProduct(var0)) {
            return false;
         }

         ArrayList var4 = feedMap.get(var0);
         return var4 == null ? false : var4.contains(var1);
      } finally {
         lock.unlock(var2);
      }
   }

   private static boolean checkProduct(String var0) {
      if (activeProductCode == null) {
         return true;
      }

      String var1 = SQWebSocketManager.getProjectName(activeProductCode);
      return var1 != null && var1.equals(var0) ? true : activeProductCode.equals("TASKMANAGER") && !ProjectEngine.isDefaulProject(var0);
   }
}
