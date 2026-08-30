package com.strategyquant.datalib.data.io.columns;

public class CloseCol extends DefaultCol {
   public static final String NAME = "Close";

   public CloseCol() {
      super("Close", "CLOSE");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 7;
   }
}
