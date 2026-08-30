package com.strategyquant.datalib.data.io.columns;

public class LowCol extends DefaultCol {
   public LowCol() {
      super("Low", "LOW");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 6;
   }
}
