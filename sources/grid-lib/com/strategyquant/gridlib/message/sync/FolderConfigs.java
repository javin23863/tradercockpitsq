package com.strategyquant.gridlib.message.sync;

import com.strategyquant.gridlib.config.SingleFolderConfig;
import java.util.LinkedList;
import java.util.List;

public class FolderConfigs {
   private String dataFolderRoot;
   private List<SingleFolderConfig> folderConfig = new LinkedList<>();

   public String getDataFolderRoot() {
      return this.dataFolderRoot;
   }

   public void setDataFolderRoot(String var1) {
      this.dataFolderRoot = var1;
   }

   public List<SingleFolderConfig> getFolderConfig() {
      return this.folderConfig;
   }

   public void setFolderConfig(List<SingleFolderConfig> var1) {
      this.folderConfig = var1;
   }
}
