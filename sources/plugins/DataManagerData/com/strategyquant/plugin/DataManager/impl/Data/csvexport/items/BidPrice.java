package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class BidPrice extends AbstractItem {
   public static final String Key = "Bid";
   public static final String Header = "Bid";

   public BidPrice() {
      super("Bid price", "Bid", "Bid");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.bid, var3);
   }

   @Override
   public AbstractItem clone() {
      return new BidPrice();
   }
}
