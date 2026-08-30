package com.strategyquant.gridlib.compute.common;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskGroupMap {
   private Map<Integer, Set<String>> groupMap = new Int2ObjectOpenHashMap();
   private Map<Integer, ExecuteOptions> groupLimits = new Int2ObjectOpenHashMap();
   private Map<Integer, Integer> runningInGroups = new Int2IntOpenHashMap();

   public Set<String> getJobsForGroup(String var1) {
      synchronized (this.groupMap) {
         int var3 = var1.hashCode();
         Set var4 = this.groupMap.get(var3);
         if (var4 == null) {
            var4 = new HashSet();
            this.groupMap.put(var3, var4);
         }

         return var4;
      }
   }

   public void add(String var1, String var2, ExecuteOptions var3) {
      synchronized (this.groupMap) {
         this.getJobsForGroup(var2).add(var1);
         this.groupLimits.put(var2.hashCode(), var3);
      }
   }

   public boolean remove(String var1, String var2) {
      synchronized (this.groupMap) {
         this.finished(var2);
         Set var4 = this.getJobsForGroup(var2);
         if (var4 == null) {
            return false;
         } else {
            var4.remove(var1);
            if (var4.isEmpty()) {
               this.removeGroup(var2);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public ExecuteOptions getOptions(String var1) {
      synchronized (this.groupMap) {
         int var3 = var1.hashCode();
         return this.groupLimits.get(var3);
      }
   }

   private void removeGroup(String var1) {
      int var2 = var1.hashCode();
      this.groupMap.remove(var2);
      this.groupLimits.remove(var2);
      this.runningInGroups.remove(var2);
   }

   public int getRunningCount(String var1) {
      synchronized (this.groupMap) {
         Integer var3 = this.runningInGroups.get(var1.hashCode());
         return var3 == null ? 0 : var3;
      }
   }

   public void executed(String var1) {
      synchronized (this.groupMap) {
         int var3 = var1.hashCode();
         Integer var4 = this.runningInGroups.get(var3);
         this.runningInGroups.put(var1.hashCode(), var4 == null ? 1 : var4 + 1);
      }
   }

   public void finished(String var1) {
      synchronized (this.groupMap) {
         int var3 = var1.hashCode();
         Integer var4 = this.runningInGroups.get(var3);
         this.runningInGroups.put(var1.hashCode(), var4 == null ? 0 : var4 - 1);
      }
   }

   public boolean hasTasks(String var1) {
      synchronized (this.groupMap) {
         return this.getRunningCount(var1) > 0;
      }
   }
}
