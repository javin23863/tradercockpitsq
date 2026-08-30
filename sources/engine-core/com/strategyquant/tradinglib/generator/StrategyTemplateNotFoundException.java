package com.strategyquant.tradinglib.generator;

public class StrategyTemplateNotFoundException extends Exception {
   private String templateFile = null;
   private String buildTaskName = null;

   public StrategyTemplateNotFoundException(String var1, String var2) {
      super(String.format("Strategy template file '%s' for task name '%s' was not found.", var1, var2));
      this.templateFile = var1;
      this.buildTaskName = var2;
   }

   public String getTemplateFile() {
      return this.templateFile;
   }

   public String getTaskName() {
      return this.buildTaskName;
   }
}
