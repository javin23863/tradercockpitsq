package com.strategyquant.tradinglib.exception;

public class TaskErrorInfo {
   public String strategyName;
   public String exception;

   public TaskErrorInfo(String var1, String var2) {
      this.strategyName = var1;
      this.exception = var2;
   }
}
