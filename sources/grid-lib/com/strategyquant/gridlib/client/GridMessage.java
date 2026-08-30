package com.strategyquant.gridlib.client;

import java.io.Serializable;

public class GridMessage implements Serializable {
   private static final long serialVersionUID = 1L;
   public static final int JobFinished = 1;
   public static final int JobPause = 2;
   public static final int JobRestart = 3;
   public static final int JobStop = 4;
   public static final int Custom = 5;
   public static final int ProgressUpdated = 6;
   private int messageID;
   private String customID;
   private Serializable data;
   private JobDetails jobDetails;

   public GridMessage() {
   }

   public GridMessage(int var1) {
      this(var1, null, null);
   }

   public GridMessage(int var1, String var2) {
      this(var1, var2, null);
   }

   public GridMessage(int var1, String var2, Serializable var3) {
      this.messageID = var1;
      this.customID = var2;
      this.data = var3;
   }

   public int getMessageID() {
      return this.messageID;
   }

   public String getCustomID() {
      return this.customID;
   }

   public Serializable getData() {
      return this.data;
   }

   public JobDetails getJobDetails() {
      return this.jobDetails;
   }

   public void setJobDetails(JobDetails var1) {
      this.jobDetails = var1;
   }
}
