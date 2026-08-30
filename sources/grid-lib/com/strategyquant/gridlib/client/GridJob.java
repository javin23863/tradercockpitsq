package com.strategyquant.gridlib.client;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class GridJob<T> implements Callable<T>, Serializable {
   private static final long serialVersionUID = 1L;
   public static final int blocking_flag = 1;
   private String jobId;
   private Map<String, Serializable> parameters;
   private int flags;
   private String jobGroupId;
   private int priority;

   public GridJob(String var1, int var2, Map<String, Serializable> var3) {
      this.jobId = var1;
      this.flags = var2;
      this.parameters = var3;
   }

   public String getJobId() {
      return this.jobId;
   }

   public void messageReceived(GridMessage var1) {
   }

   public Map<String, Serializable> getParameters() {
      return this.parameters;
   }

   public void destroy() {
   }

   public boolean isBlocking() {
      return (this.flags & 1) != 0;
   }

   public int getPriority() {
      return this.priority;
   }

   public void setPriority(int var1) {
      this.priority = var1;
   }

   public int getFlags() {
      return this.flags;
   }

   public void sendProgress(int var1) {
      SQGrid.getGridClient().sendProgress(this.getJobId(), this.jobGroupId, var1);
   }

   public String getJobGroupId() {
      return this.jobGroupId;
   }

   public void setJobGroupId(String var1) {
      this.jobGroupId = var1;
   }
}
