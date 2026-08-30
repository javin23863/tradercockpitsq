package com.strategyquant.datalib.data.io.columns;

public class BidCol extends DefaultCol {
   public BidCol() {
      super("Bid", "BID");
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public int getDataType() {
      return 3;
   }
}
