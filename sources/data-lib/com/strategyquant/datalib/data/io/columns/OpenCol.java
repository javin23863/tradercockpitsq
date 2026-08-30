package com.strategyquant.datalib.data.io.columns;

public class OpenCol extends DefaultCol {
   public OpenCol() {
      super("Open", "OPEN");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 4;
   }
}
