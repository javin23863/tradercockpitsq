package com.strategyquant.gridlib.stat;

import java.util.HashMap;
import java.util.Map;

public class ComputingStats {
   private long computedTasks = 0L;
   private long resentTasks = 0L;
   private Map<String, Statistic> groupStats = new HashMap<>();
   private Map<String, Statistic> nodeStats = new HashMap<>();

   public long getComputedTasks() {
      return this.computedTasks;
   }

   public long getResentTasks() {
      return this.resentTasks;
   }

   public Statistic getStatisticForNode(String var1) {
      synchronized (this.nodeStats) {
         Statistic var3 = this.nodeStats.get(var1);
         if (var3 == null) {
            var3 = new Statistic();
            this.nodeStats.put(var1, var3);
         }

         return var3;
      }
   }

   public Statistic getStatisticForGroup(String var1) {
      synchronized (this.groupStats) {
         Statistic var3 = this.groupStats.get(var1);
         if (var3 == null) {
            var3 = new Statistic();
            this.groupStats.put(var1, var3);
         }

         return var3;
      }
   }

   public void removeStatisticsForNode(String var1) {
      synchronized (this.nodeStats) {
         this.nodeStats.remove(var1);
      }
   }

   public void jobComputed(String var1, String var2, long var3) {
      this.getStatisticForGroup(var1).taskComputed(var3);
      this.getStatisticForNode(var2).taskComputed(var3);
      this.computedTasks++;
   }

   public void jobFailed(String var1, String var2) {
      this.getStatisticForGroup(var1).taskFailed();
      this.getStatisticForNode(var2).taskFailed();
      this.resentTasks++;
   }
}
