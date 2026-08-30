package com.strategyquant.datalib.data.io.columns;

public class AskCol extends DefaultCol {
   public AskCol() {
      super("Ask", "ASK");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 2;
   }
}
