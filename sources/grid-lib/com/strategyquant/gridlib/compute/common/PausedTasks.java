package com.strategyquant.gridlib.compute.common;

import java.io.Serializable;
import java.util.List;

public class PausedTasks<T extends Serializable> {
   private List<JobQueue.JobInfo<T>> resultWaiting;
   private List<JobQueue.JobInfo<T>> resultRunning;

   public PausedTasks(List<JobQueue.JobInfo<T>> var1, List<JobQueue.JobInfo<T>> var2) {
      this.resultWaiting = var1;
      this.resultRunning = var2;
   }

   public List<JobQueue.JobInfo<T>> getRunningTasks() {
      return this.resultRunning;
   }
}
