package com.strategyquant.gridlib.stat;

public class Statistic {
   private long computedTasks = 0L;
   private long resentTasks = 0L;
   private long shortestTaskTime = 0L;
   private long longestTaskTime = 0L;
   private long averageTaskTime = 0L;
   private long totalDuration = 0L;

   public long getComputedTasks() {
      return this.computedTasks;
   }

   public long getResentTasks() {
      return this.resentTasks;
   }

   public long getShortestTaskTime() {
      return this.shortestTaskTime;
   }

   public long getLongestTaskTime() {
      return this.longestTaskTime;
   }

   public long getAverageTaskTime() {
      return this.averageTaskTime;
   }

   public long getTotalDuration() {
      return this.totalDuration;
   }

   public synchronized void taskFailed() {
      this.resentTasks++;
   }

   public void taskComputed(long var1) {
      this.computedTasks++;
      if (var1 > this.longestTaskTime) {
         this.longestTaskTime = var1;
      }

      if (var1 < this.shortestTaskTime || this.shortestTaskTime == 0L) {
         this.shortestTaskTime = var1;
      }

      this.totalDuration += var1;
      this.averageTaskTime = this.totalDuration / this.computedTasks;
   }
}
