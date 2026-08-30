package com.strategyquant.datalib.data;

public interface BatchProgressController {
   boolean isCancel();

   void updateProgress(int var1, int var2, String var3) throws Exception;

   void finished();
}
