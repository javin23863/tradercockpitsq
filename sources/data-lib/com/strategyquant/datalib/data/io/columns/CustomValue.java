package com.strategyquant.datalib.data.io.columns;

public class CustomValue extends DefaultCol {
   public CustomValue(int var1) {
      super("Value" + var1, "Value" + var1);
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 9999;
   }
}
