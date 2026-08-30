package com.strategyquant.gridlib.compute;

import java.io.Serializable;

public class JobResult<T extends Serializable> implements Serializable {
   private static final long serialVersionUID = 1L;
   private boolean success;
   private boolean stopped;
   private T data;
   private long duration;
   private String errorMessage;

   public T getData() {
      return this.data;
   }

   public void setData(T var1) {
      this.data = (T)var1;
   }

   public boolean isSuccess() {
      return this.success;
   }

   public void setSuccess(boolean var1) {
      this.success = var1;
   }

   public long getDuration() {
      return this.duration;
   }

   public void setDuration(long var1) {
      this.duration = var1;
   }

   public String getErrorMessage() {
      return this.errorMessage;
   }

   public void setErrorMessage(String var1) {
      this.errorMessage = var1;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public void setStopped(boolean var1) {
      this.stopped = var1;
   }
}
