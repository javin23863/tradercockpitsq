package com.strategyquant.tradinglib.project;

public class GoToTaskException extends Exception {
   public String taskName;

   public GoToTaskException(String var1) {
      this.taskName = var1;
   }
}
