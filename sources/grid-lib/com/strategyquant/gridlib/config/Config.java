package com.strategyquant.gridlib.config;

import com.strategyquant.gridlib.message.sync.FolderConfigs;

public class Config {
   private boolean useJavaSerialization;
   private boolean useAffinity;
   private boolean compressData;
   private String coreUsage;
   private String nodeId;
   private int finishedJobsInDesc;
   private String serverLocation;
   private FolderConfigs folderConfigs = new FolderConfigs();
   private String[] foldersWithJars;
   private boolean adjustTopSizeOfCores;

   public Config() {
      this.useJavaSerialization = true;
      this.compressData = true;
      this.useAffinity = true;
      this.coreUsage = "-1";
      this.finishedJobsInDesc = 10;
   }

   public Config(String var1) {
      this();
      this.serverLocation = var1;
   }

   public int getFinishedJobsInDesc() {
      return this.finishedJobsInDesc;
   }

   public void setFinishedJobsInDesc(int var1) {
      this.finishedJobsInDesc = var1;
   }

   public String getServerLocation() {
      return this.serverLocation;
   }

   public void setUseJavaSerialization(boolean var1) {
      this.useJavaSerialization = var1;
   }

   public boolean isUseJavaSerialization() {
      return this.useJavaSerialization;
   }

   public void setServerLocation(String var1) {
      this.serverLocation = var1;
   }

   public String[] getFoldersWithJars() {
      return this.foldersWithJars;
   }

   public void setFoldersWithJars(String[] var1) {
      this.foldersWithJars = var1;
   }

   public FolderConfigs getFolderConfigs() {
      return this.folderConfigs;
   }

   public void setFolderConfigs(FolderConfigs var1) {
      this.folderConfigs = var1;
   }

   public boolean isCompressData() {
      return this.compressData;
   }

   public void setCompressData(boolean var1) {
      this.compressData = var1;
   }

   public String getCoreUsage() {
      return this.coreUsage;
   }

   public void setCoreUsage(String var1) {
      this.coreUsage = var1;
   }

   public boolean isUseAffinity() {
      return this.useAffinity;
   }

   public void setUseAffinity(boolean var1) {
      this.useAffinity = var1;
   }

   public String getNodeId() {
      return this.nodeId;
   }

   public void setNodeId(String var1) {
      this.nodeId = var1;
   }

   public boolean isAdjustTopSizeOfCores() {
      return this.adjustTopSizeOfCores;
   }

   public void setAdjustTopSizeOfCores(boolean var1) {
      this.adjustTopSizeOfCores = var1;
   }
}
