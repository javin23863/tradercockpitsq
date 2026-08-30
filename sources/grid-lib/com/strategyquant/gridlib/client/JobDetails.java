package com.strategyquant.gridlib.client;

import java.io.Serializable;

public class JobDetails implements Serializable {
   private String id;
   private long duration;
   private String exception;

   public JobDetails(String var1) {
      this.id = var1;
   }

   public String getJobID() {
      return this.id;
   }

   public String getException() {
      return this.exception;
   }

   public void setException(String var1) {
      this.exception = var1;
   }

   public long getDuration() {
      return this.duration;
   }

   public void setDuration(long var1) {
      this.duration = var1;
   }

   public boolean isSuccess() {
      return this.exception == null;
   }
}
