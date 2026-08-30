package com.strategyquant.wizard.desktop.indicators;

public class SParameter {
   public String name;
   public String type;
   public String value;
   public int index;

   public SParameter(String var1, String var2, String var3, int var4) {
      this.name = var1;
      this.type = var2;
      this.value = var3;
      this.index = var4;
      this.adjustParameterData();
   }

   private void adjustParameterData() {
      if (this.type.contains("bool")) {
         this.type = "boolean";
      }

      if (this.value == null) {
         this.value = "0";
         if (this.type.equals("string")) {
            this.value = "";
         }

         if (this.type.contains("bool")) {
            this.value = "false";
         }

         if (this.type.equals("color")) {
            this.value = "Red";
         }
      } else if (this.type.contains("bool")) {
         this.value = this.value.toLowerCase();
      }
   }
}
