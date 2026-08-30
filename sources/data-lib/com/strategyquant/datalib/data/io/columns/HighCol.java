package com.strategyquant.datalib.data.io.columns;

public class HighCol extends DefaultCol {
   public HighCol() {
      super("High", "HIGH");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 5;
   }
}
