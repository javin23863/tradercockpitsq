package com.strategyquant.tradinglib.project;

public class ProjectConditionField {
   public static final String DATABANK = "databank";
   public static final String NUMBER = "number";
   public static final String DURATION = "duration";
   public static final String COMPARATOR = "comparator";
   private String type;
   private String name;
   private String defaultValue;

   public ProjectConditionField(String var1, String var2, String var3) {
      this.type = var1;
      this.name = var2;
      this.defaultValue = var3;
   }

   public String getType() {
      return this.type;
   }

   public String getName() {
      return this.name;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }
}
