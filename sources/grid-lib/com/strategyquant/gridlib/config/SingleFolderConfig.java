package com.strategyquant.gridlib.config;

public class SingleFolderConfig {
   private String folder;
   private boolean solidFolder;

   public SingleFolderConfig() {
   }

   public SingleFolderConfig(String var1, boolean var2) {
      this.folder = var1;
      this.solidFolder = var2;
   }

   public String getFolder() {
      return this.folder;
   }

   public boolean isSolidFolder() {
      return this.solidFolder;
   }

   public void setFolder(String var1) {
      this.folder = var1;
   }

   public void setSolidFolder(boolean var1) {
      this.solidFolder = var1;
   }
}
