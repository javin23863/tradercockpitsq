package com.strategyquant.gridlib.sync.updater;

import java.util.LinkedList;

public class HandleHashResult {
   private boolean needUpdate;
   private LinkedList<String> filesToUpdate;

   public HandleHashResult(boolean var1, LinkedList<String> var2) {
      this.needUpdate = var1;
      this.filesToUpdate = var2;
   }

   public boolean isNeedUpdate() {
      return this.needUpdate;
   }

   public LinkedList<String> getFilesToUpdate() {
      return this.filesToUpdate;
   }
}
