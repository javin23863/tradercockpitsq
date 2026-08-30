package com.strategyquant.datalib.data.io.columns;

public abstract class DefaultCol {
   private String name;
   private String code;

   public DefaultCol(String var1, String var2) {
      this.name = var1;
      this.code = var2;
   }

   public String getName() {
      return this.name;
   }

   public String getCode() {
      return this.code;
   }

   public String getClassName() {
      return this.getClass().getSimpleName();
   }

   public abstract int getType();

   public abstract int getDataType();
}
