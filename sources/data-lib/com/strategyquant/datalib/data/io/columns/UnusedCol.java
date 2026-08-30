package com.strategyquant.datalib.data.io.columns;

public class UnusedCol extends DefaultCol {
   public UnusedCol() {
      super("Unused", "UNUSED");
   }

   @Override
   public int getType() {
      return 8;
   }

   @Override
   public int getDataType() {
      return 10;
   }
}
